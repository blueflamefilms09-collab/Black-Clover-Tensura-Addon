package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.celestial.CelestialKey;
import com.newuniverse.nusmp.celestial.CelestialRules;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Why: what the twelve zodiac spirits and the four silver ones actually DO when they stand in the world. The spirit is a prop ({@code PropKind.CELESTIAL_1}); its behaviour
 * ({@code celestial.CelestialProps}) calls {@link #cast} once when the spirit has finished stepping out of its circle and {@link #sustain} every tick after that for the
 * abilities that last (the volley, the sandstorm, the tide, the field, the shelter, the song). Every hit goes through {@link SkillDamage} and so through the balance law
 * (damage scaled by the caster's EP and the skill's mastery, a player never takes more than the PvP hit cap), a boss takes {@link CelestialRules#BOSS_DAMAGE_FACTOR} of a
 * blow and a shortened hard control, and hard control is rationed by {@link BalanceLaw#beginControl}. The numbers are the {@link CelestialKey} table. The class is in the
 * skill package for the package-private helpers; nothing here is a skill.
 */
public final class CelestialArts {
    private CelestialArts() {}

    public static final String RAW = "cel_raw", MS = "cel_ms", BOND = "cel_bd", CTL = "cel_ctl", HIT = "cel_hit";

    /** Writes the cast numbers onto a freshly spawned spirit prop. */
    public static void stamp(MagicPropEntity e, CelestialKey key, int bond, double mastery, boolean control) {
        CompoundTag d = e.getPersistentData();
        d.putFloat(RAW, key.power * (1f + 0.05f * Math.max(0, Math.min(CelestialRules.MAX_BOND, bond))));
        d.putFloat(MS, (float) mastery);
        d.putInt(BOND, bond);
        d.putBoolean(CTL, control);
    }

    private static double ms(MagicPropEntity e) { return e.getPersistentData().getFloat(MS); }

    private static float raw(MagicPropEntity e) { return e.getPersistentData().getFloat(RAW); }

    private static boolean control(MagicPropEntity e) { return e.getPersistentData().getBoolean(CTL); }

    private static Vec3 forward(MagicPropEntity e) {
        double yaw = Math.toRadians(e.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private static Vec3 chest(MagicPropEntity e) { return e.position().add(0, 1.1 * e.scale(), 0); }

    /** One hit of {@code mult} times the spirit's blow, with the boss cut. Returns the damage dealt. */
    private static float hit(MagicPropEntity e, ServerPlayer p, LivingEntity t, float mult) {
        float r = CelestialRules.vsBoss(raw(e) * mult, BalanceLaw.isBoss(t));
        return SkillDamage.hit(p, t, r, ms(e), SkillDamage.Kind.MAGIC);
    }

    private static void push(LivingEntity t, Vec3 dir, double horizontal, double up) {
        double k = BalanceLaw.isBoss(t) ? 0.15 : 1.0;
        t.setDeltaMovement(t.getDeltaMovement().add(dir.x * horizontal * k, up * k, dir.z * horizontal * k));
        t.hurtMarked = true;
    }

    /** Hard control on a foe: the ticks are cut for a boss, shortened by the balance law's caps, and only applied when the caster may control now. */
    private static void bind(MagicPropEntity e, CelestialKey key, LivingEntity t, int base, int slowAmp) {
        if (!control(e)) return;
        int ticks = BalanceLaw.controlTicks(t, CelestialRules.controlTicks(key, base, BalanceLaw.isBoss(t)));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, slowAmp));
    }

    private static LivingEntity nearest(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> foes = SkillTargets.foes(p, c, r);
        return foes.isEmpty() ? null : foes.get(0);
    }

    private static void line(ServerLevel sl, VfxShape s, Vec3 a, Vec3 b, CelestialKey key, int dur, float power) {
        SkillFx.shape(sl, s, a, b, 0xFF000000 | key.rgb, dur, power);
    }

    private static void ring(ServerLevel sl, Vec3 at, double r, CelestialKey key) { SkillFx.ring(sl, at, r, key.rgb); }

    // ================================================================ the cast (once)
    /** The spirit's signature move, at the moment it is fully out of its circle. */
    public static void cast(MagicPropEntity e, ServerPlayer p, CelestialKey key) {
        ServerLevel sl = p.serverLevel();
        Vec3 from = chest(e), dir = forward(e);
        Vec3 reachTo = from.add(dir.scale(key.reach));
        switch (key) {
            case AQUARIUS -> {
                line(sl, VfxShape.FX_WATER_CRASH, from, reachTo, key, 24, 1.4f);
                line(sl, VfxShape.WATER_SPLASH, from, reachTo, key, 24, 1.0f);
                SkillFx.sound(sl, from, SoundEvents.PLAYER_SPLASH, 1.4f, 0.6f);
                for (LivingEntity t : SkillTargets.foesAlong(p, from, reachTo, 1.6)) {
                    hit(e, p, t, 1f);
                    push(t, dir, 1.1, 0.3);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                for (LivingEntity a : SkillTargets.alliesAround(p, from.add(dir.scale(key.reach * 0.5)), key.reach * 0.6)) a.clearFire();
            }
            case TAURUS -> {
                Vec3 at = e.position().add(dir.scale(2.0 * e.scale()));
                line(sl, VfxShape.FX_EARTH_SPIKES, at, at.add(0, 1, 0), key, 24, 1.3f);
                ring(sl, at, key.reach, key);
                SkillFx.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 0.7f);
                for (LivingEntity t : SkillTargets.foes(p, at, key.reach)) {
                    hit(e, p, t, 1f);
                    push(t, t.position().subtract(at).multiply(1, 0, 1).normalize(), 0.7, 0.35);
                    bind(e, key, t, 40, 4);
                }
            }
            case LEO -> {
                line(sl, VfxShape.DIVINE_RAY, from, reachTo, key, 26, 1.6f);
                line(sl, VfxShape.LIGHT_FLARE, from, from.add(dir), key, 18, 1.2f);
                SkillFx.sound(sl, from, SoundEvents.BEACON_POWER_SELECT, 1.4f, 1.5f);
                for (LivingEntity t : SkillTargets.foesAlong(p, from, reachTo, 1.3)) {
                    float mult = t.isInvertedHealAndHarm() ? 1.5f : 1f;          // the undead burn worse in holy light
                    hit(e, p, t, mult);
                    if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                }
            }
            case VIRGO -> {
                Vec3 at = e.position();
                line(sl, VfxShape.SEAL_CHAINS, at, at.add(0, 1, 0), key, 40, 1.2f);
                for (LivingEntity t : SkillTargets.foes(p, at, key.reach)) {
                    hit(e, p, t, 1f);
                    bind(e, key, t, 80, 7);
                    line(sl, VfxShape.SEAL_CHAINS, t.position(), t.position().add(0, 1, 0), key, 30, 0.8f);
                    if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                }
                SkillFx.sound(sl, at, SoundEvents.CHAIN_PLACE, 1.2f, 0.7f);
            }
            case SAGITTARIUS -> SkillFx.sound(sl, from, SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.0f);   // the arrows fly in sustain()
            case GEMINI -> {
                ring(sl, e.position(), 2.5, key);
                line(sl, VfxShape.MIRROR_DOUBLE, from, from.add(0, 0.5, 0), key, 30, 1.0f);
                SkillFx.sound(sl, from, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.6f);
            }
            case CANCER -> SkillFx.sound(sl, from, SoundEvents.SHEEP_SHEAR, 1.2f, 0.9f);                       // the snips come in sustain()
            case SCORPIO -> {
                ring(sl, e.position().add(dir.scale(3)), key.reach, key);
                SkillFx.sound(sl, from, SoundEvents.SAND_BREAK, 1.4f, 0.6f);
            }
            case CAPRICORN -> SkillFx.sound(sl, from, SoundEvents.GOAT_LONG_JUMP, 1.4f, 0.7f);                // the charge runs in sustain()
            case PISCES -> {
                ring(sl, e.position(), 3.0, key);
                SkillFx.sound(sl, from, SoundEvents.DOLPHIN_PLAY, 1.2f, 1.2f);
            }
            case ARIES -> {
                float soak = (float) Math.min(40.0, 4.0 * Math.sqrt(EnergyBridge.power(p) * 3.0) * (1 + e.getPersistentData().getInt(BOND) * 0.1));
                for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), key.reach)) {
                    a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
                    a.setAbsorptionAmount(Math.max(a.getAbsorptionAmount(), soak));
                    line(sl, VfxShape.DIVINE_HALO, a.position(), a.position().add(0, 1, 0), key, 30, 0.9f);
                }
                ring(sl, p.position(), key.reach, key);
                SkillFx.sound(sl, from, SoundEvents.WOOL_PLACE, 1.2f, 0.6f);
            }
            case LIBRA -> {
                line(sl, VfxShape.GRAVITY_WELL, e.position(), e.position().add(0, 1, 0), key, 40, 1.3f);
                SkillFx.sound(sl, from, SoundEvents.ANVIL_PLACE, 0.8f, 0.5f);
            }
            case HOROLOGIUM -> {
                ring(sl, e.position(), key.reach, key);
                SkillFx.sound(sl, from, SoundEvents.BELL_BLOCK, 1.2f, 0.7f);
            }
            case CRUX -> {
                line(sl, VfxShape.STAR_MAGIC, from.add(0, 4, 0), from, key, 30, 1.3f);
                for (LivingEntity t : SkillTargets.foes(p, p.position(), key.reach)) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
                for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), key.reach)) {
                    a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0));
                    a.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0));
                }
                SkillFx.sound(sl, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.6f);
            }
            case LYRA -> SkillFx.sound(sl, from, SoundEvents.NOTE_BLOCK_HARP.value(), 1.4f, 1.2f);
            case NIKORA -> {
                LivingEntity t = nearest(p, e.position(), key.reach);
                if (t != null) {
                    Vec3 d = t.position().subtract(e.position()).multiply(1, 0, 1);
                    d = d.lengthSqr() < 1e-4 ? dir : d.normalize();
                    e.setPos(t.getX() - d.x * 0.8, e.getY(), t.getZ() - d.z * 0.8);
                    hit(e, p, t, 1f);
                    push(t, d, 0.5, 0.2);
                    line(sl, VfxShape.STAR_MAGIC, from, t.position().add(0, 0.5, 0), key, 14, 0.8f);
                }
                BalanceLaw.heal(p, 2f);
                SkillFx.sound(sl, from, SoundEvents.WOLF_PANT, 1.0f, 1.8f);
            }
            default -> { }
        }
    }

    // ================================================================ the long abilities (every tick after the cast)
    /** {@code t} is the number of ticks since the cast. */
    public static void sustain(MagicPropEntity e, ServerPlayer p, CelestialKey key, int t) {
        ServerLevel sl = p.serverLevel();
        Vec3 from = chest(e), dir = forward(e);
        switch (key) {
            case SAGITTARIUS -> {                                                                    // seven arrows, one every 4 ticks, each at a different foe
                if (t % 4 != 0 || t / 4 >= 7) return;
                List<LivingEntity> foes = SkillTargets.foes(p, e.position(), key.reach);
                if (foes.isEmpty()) return;
                LivingEntity tg = foes.get((t / 4) % foes.size());
                line(sl, VfxShape.STAR_MAGIC, from, tg.getBoundingBox().getCenter(), key, 10, 0.7f);
                SkillFx.sound(sl, from, SoundEvents.ARROW_SHOOT, 0.9f, 1.3f);
                hit(e, p, tg, 1f);
            }
            case GEMINI -> {                                                                         // the twins are the bait: foes walk to them, then they burst
                if (t % 10 == 0) for (Mob m : sl.getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(key.reach), x -> x.getTarget() == p && !BalanceLaw.isBoss(x))) {
                    m.setTarget(null);
                    m.getNavigation().moveTo(e.getX(), e.getY(), e.getZ(), 1.1);
                }
                if (t == e.maxLife() - 30) {                                                          // the burst, a moment before they go
                    ring(sl, e.position(), key.reach, key);
                    line(sl, VfxShape.MIRROR_SHATTER, e.position(), e.position().add(0, 1, 0), key, 20, 1.2f);
                    SkillFx.sound(sl, from, SoundEvents.GLASS_BREAK, 1.2f, 1.1f);
                    for (LivingEntity x : SkillTargets.foes(p, e.position(), key.reach)) { hit(e, p, x, 1.5f); push(x, x.position().subtract(e.position()).multiply(1, 0, 1).normalize(), 0.6, 0.3); }
                }
            }
            case CANCER -> {                                                                         // four snips, 4 ticks apart
                if (t % 4 != 0 || t / 4 >= 4) return;
                for (LivingEntity x : SkillTargets.foes(p, e.position(), key.reach)) {
                    hit(e, p, x, 0.4f);
                    line(sl, VfxShape.SLASH_BLADES, x.position().add(0, 0.8, 0), x.position().add(0.5, 1.1, 0), key, 8, 0.6f);
                    if (t == 12) {                                                                    // the last snip leaves the wound and shears the armour
                        EnergyBridge.effect(x, "fragility", 120, 0);
                        x.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    }
                }
            }
            case SCORPIO -> {                                                                        // the sandstorm
                if (t > 80 || t % 10 != 0) return;
                Vec3 c = e.position().add(dir.scale(3));
                line(sl, VfxShape.SAND_MAGIC, c, c.add(0, 2, 0), key, 14, 1.3f);
                for (LivingEntity x : SkillTargets.foes(p, c, key.reach)) {
                    hit(e, p, x, 0.3f);
                    if (!BalanceLaw.isBoss(x)) x.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                    x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
            case CAPRICORN -> {                                                                      // ten ticks of charge, one block a tick
                if (t >= 10) return;
                CompoundTag d = e.getPersistentData();
                int[] seen = d.getIntArray(HIT);
                e.setPos(e.getX() + dir.x, e.getY(), e.getZ() + dir.z);
                SkillFx.sound(sl, e.position(), SoundEvents.GOAT_STEP, 0.8f, 0.7f);
                for (LivingEntity x : SkillTargets.foes(p, e.position().add(0, 0.5, 0), 2.3)) {
                    boolean done = false;
                    for (int id : seen) if (id == x.getId()) done = true;
                    if (done) continue;
                    hit(e, p, x, 1f);
                    push(x, dir, 0.9, 0.5);
                    int[] more = java.util.Arrays.copyOf(seen, seen.length + 1);
                    more[seen.length] = x.getId();
                    seen = more;
                    d.putIntArray(HIT, seen);
                }
            }
            case PISCES -> {
                if (t % 10 == 0 && t < 100) {
                    LivingEntity x = nearest(p, e.position(), key.reach);
                    if (x != null) {
                        line(sl, VfxShape.WATER_SPLASH, from, x.getBoundingBox().getCenter(), key, 10, 0.8f);
                        hit(e, p, x, 0.8f);
                    }
                }
                if (t % 20 == 0 && t < 100) {
                    for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), key.reach)) {
                        BalanceLaw.heal(a, (float) (1.5 * BalanceLaw.epScale(p)));
                        line(sl, VfxShape.WATER_RING, a.position(), a.position().add(0, 1, 0), key, 14, 0.7f);
                    }
                }
            }
            case ARIES -> {
                if (t % 20 == 0 && t < 140) for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), key.reach))
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0));
            }
            case LIBRA -> {                                                                          // the scales tip: foes are weighed down, friends are light
                if (t % 10 != 0 || t > 140) return;
                for (LivingEntity x : SkillTargets.foes(p, e.position(), key.reach)) {
                    x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, BalanceLaw.isBoss(x) ? 0 : 2));
                    if (!BalanceLaw.isBoss(x)) x.setDeltaMovement(x.getDeltaMovement().add(0, -0.25, 0));
                    x.hurtMarked = true;
                    hit(e, p, x, 0.2f);
                }
                for (LivingEntity a : SkillTargets.alliesAround(p, e.position(), key.reach)) {
                    a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0));
                    a.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 1));
                }
                if (t % 20 == 0) ring(sl, e.position(), key.reach, key);
            }
            case HOROLOGIUM -> {                                                                     // the shelter: friends mend, foes are kept out
                if (t % 5 != 0 || t > 150) return;
                for (LivingEntity a : SkillTargets.alliesAround(p, e.position(), key.reach)) {
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1));
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1));
                }
                for (LivingEntity x : SkillTargets.foes(p, e.position(), key.reach)) {
                    Vec3 out = x.position().subtract(e.position()).multiply(1, 0, 1);
                    if (out.lengthSqr() < 1e-4) out = dir;
                    push(x, out.normalize(), BalanceLaw.isBoss(x) ? 0.5 : 0.35, 0.05);
                }
                if (t % 20 == 0) ring(sl, e.position(), key.reach, key);
            }
            case LYRA -> {
                if (t % 20 != 0 || t > 100) return;
                line(sl, VfxShape.SONG_NOTES, from, from.add(0, 1.5, 0), key, 24, 1.2f);
                for (LivingEntity x : SkillTargets.foes(p, e.position(), key.reach)) {
                    bind(e, key, x, 60, 6);
                    x.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    if (x instanceof Mob m && !BalanceLaw.isBoss(x)) m.setTarget(null);
                }
                for (LivingEntity a : SkillTargets.alliesAround(p, e.position(), key.reach)) {
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                    BalanceLaw.heal(a, 1f);
                }
            }
            default -> { }
        }
    }

    /** True for the spirits that keep acting after {@link #cast} for the given number of ticks (the prop stays at least that long). */
    public static int actionTicks(CelestialKey key) {
        return switch (key) {
            case SAGITTARIUS -> 30;
            case CANCER -> 18;
            case SCORPIO -> 90;
            case CAPRICORN -> 14;
            case PISCES, LYRA -> 100;
            case LIBRA, HOROLOGIUM -> 150;
            case ARIES -> 140;
            default -> 20;
        };
    }

    /** The Tensura-side effect on a foe that the balance law has cleared (kept for the self-test: silences a foe for the given ticks). */
    public static void silence(LivingEntity t, int ticks) { EnergyBridge.effect(t, "silence", ticks, 0); }

    /** The speed attribute is used by the Star Dress; exposed so the dress and the arts agree on the attribute. */
    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> speedAttribute() { return Attributes.MOVEMENT_SPEED; }

    /** Foes of a point sorted nearest first (for the king's summon). */
    public static List<LivingEntity> foesNear(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> f = SkillTargets.foes(p, c, r);
        f.sort(Comparator.comparingDouble(x -> x.distanceToSqr(c)));
        return f;
    }

    /** A hit for the king-summon prop (the same damage rules). */
    public static float kingHit(ServerPlayer p, LivingEntity t, float raw, double mastery) {
        return SkillDamage.hit(p, t, CelestialRules.vsBoss(raw, BalanceLaw.isBoss(t)), mastery, SkillDamage.Kind.MAGIC);
    }

    /** Allies of a point (for the king's blessing and the summon). */
    public static List<LivingEntity> alliesNear(ServerPlayer p, Vec3 c, double r) { return SkillTargets.alliesAround(p, c, r); }

    /** Removes harmful effects (the king's blessing). */
    public static void cleanse(LivingEntity t) { SkillUtil.cleanse(t); }
}
