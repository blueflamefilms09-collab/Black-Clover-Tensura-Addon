package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Curse-Warding Magic: the spells of CurseWardingBook that need their own logic (cleansing, moving and reflecting curses, the
 * thrall curse, Exploding Life, Decaying World, the cheat-death ward).
 */
public final class CurseWardingArts {
    private CurseWardingArts() {}

    private static final int COLOR = CurseWardingBook.COLOR;
    private static final String MIRROR = "nusmp_cw_mirror", UNDYING = "nusmp_cw_undying", DECAY = "nusmp_cw_decay";
    private static final String[] TENSURA_CURSES = {"fatal_poison", "magicule_poison", "corrosion", "infection", "fear", "burden", "silence", "fragility", "frost", "webbed", "paralysis"};
    private static boolean hooked;

    /** Registers the damage and tick hooks (mirror ward, cheat-death ward, the Decaying World ward). Called once from CurseWardingProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            long now = victim.level().getGameTime();
            var data = victim.getPersistentData();
            var src = e.getSource();
            if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(DamageTypes.THORNS)) return;
            if (data.getLong(UNDYING) > now && e.getAmount() >= victim.getHealth()) {
                data.putLong(UNDYING, 0L);
                float left = victim.getHealth() - 1f;
                if (left <= 0f) e.setCanceled(true); else e.setAmount(left);
                victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 2));
                victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));
                cleanse(victim);
                if (victim.level() instanceof net.minecraft.server.level.ServerLevel sl)
                    VfxSpawn.send(sl, VfxShape.CURSE_WARDING_FX3, victim.position().add(0, 1, 0), victim.position().add(0, 2, 0), COLOR, 30, 1.2f);
                return;
            }
            if (data.getLong(MIRROR) > now) {
                float before = e.getAmount();
                e.setAmount(before * 0.8f);
                if (src.getEntity() instanceof LivingEntity att && att != victim)
                    att.hurt(victim.damageSources().thorns(victim), Math.min(6f, before * 0.3f));
            }
            if (data.getLong(DECAY) > now) e.setAmount(e.getAmount() * 0.7f);
        });
        AttributeEvents.playerTick(p -> {
            long now = p.level().getGameTime();
            if (now % 5 != 0 || p.getPersistentData().getLong(MIRROR) <= now) return;
            reflectCurses(p);
        });
    }

    // ------------------------------------------------------------------ small helpers
    private static float k(ServerPlayer p, ManasSkillInstance i) { return GrimoireBook.size(i, p) * EnergyBridge.scale(p); }

    /** The harmful effects on t (a copy, safe to iterate while removing). */
    static List<MobEffectInstance> harmful(LivingEntity t) {
        List<MobEffectInstance> out = new ArrayList<>();
        for (MobEffectInstance e : t.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) out.add(e);
        return out;
    }

    /** Takes every curse off t: vanilla harmful effects, the Tensura poisons and curses, frost and fire. Returns how many came off. */
    static int cleanse(LivingEntity t) {
        int n = 0;
        for (MobEffectInstance e : harmful(t)) { t.removeEffect(e.getEffect()); n++; }
        for (String id : TENSURA_CURSES)
            n += BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", id)).map(h -> t.removeEffect(h) ? 1 : 0).orElse(0);
        if (t.getTicksFrozen() > 0) { t.setTicksFrozen(0); n++; }
        if (t.isOnFire()) { t.clearFire(); n++; }
        return n;
    }

    /** The caster and its friends within r of c (at most 12). */
    static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    private static List<LivingEntity> foes(ServerPlayer p, Vec3 c, double r, int max) {
        List<LivingEntity> out = GrimoireBook.around(p, c, r);
        return out.size() > max ? out.subList(0, max) : out;
    }

    /** The curse proper: weakness and the Tensura fragility on t. */
    static void hex(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, amp));
        EnergyBridge.effect(t, "fragility", ticks, 0);
    }

    /** The nearest enemy of the caster within r of c, or null. */
    private static LivingEntity nearestFoe(ServerPlayer p, Vec3 c, double r, LivingEntity not) {
        return GrimoireBook.around(p, c, r).stream().filter(e -> e != not && !(e instanceof Player))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
    }

    private static void reflectCurses(ServerPlayer p) {
        List<MobEffectInstance> bad = harmful(p);
        if (bad.isEmpty()) return;
        LivingEntity to = p.getLastHurtByMob();
        if (to == null || !to.isAlive() || to == p || to.isAlliedTo(p) || to.distanceToSqr(p) > 24 * 24) to = nearestFoe(p, p.position(), 12, null);
        for (MobEffectInstance e : bad) {
            p.removeEffect(e.getEffect());
            if (to == null) continue;
            int ticks = e.isInfiniteDuration() ? 200 : Math.min(e.getDuration(), 200);
            to.addEffect(new MobEffectInstance(e.getEffect(), ticks, e.getAmplifier()));
        }
        VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_WARDING_FX1, p.position().add(0, 1.2, 0),
                (to != null ? to.getBoundingBox().getCenter() : p.position().add(0, 2.2, 0)), COLOR, 14, 0.8f);
    }

    // ------------------------------------------------------------------ Hex Eater: a bolt that steals a blessing
    static boolean hexEater(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX1, start, start.add(dir.scale(24)), 14, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.7), 0.5, 15, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 7f);
            hex(t, 100, 0);
            for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                if (e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) continue;
                t.removeEffect(e.getEffect());
                int ticks = e.isInfiniteDuration() ? 400 : Math.min(e.getDuration(), 400);
                p.addEffect(new MobEffectInstance(e.getEffect(), ticks, e.getAmplifier()));
                break;
            }
        }, (bolt, at) -> b.vfx(p, VfxShape.CURSE_WARDING_FX3, at, at, 20, 0.8f));
        return true;
    }

    // ------------------------------------------------------------------ Curse Cleanse
    static boolean cleanseAll(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(p, i);
        double r = 7 * s;
        int removed = 0;
        List<LivingEntity> fs = friends(p, p.position(), r);
        for (LivingEntity f : fs) {
            int n = cleanse(f);
            removed += n;
            if (n > 0) {
                BalanceLaw.heal(f, f.getMaxHealth() * 0.05f + 1.5f * Math.min(n, 4));
                VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_WARDING_FX3, f.position().add(0, 1, 0), f.position().add(0, 2, 0), COLOR, 22, 0.7f);
            }
        }
        if (removed == 0) { GrimoireBook.fail(p, "There is no curse to cleanse."); return false; }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX2, p.position(), p.position().add(0, 1, 0), 30, (float) r);
        return true;
    }

    // ------------------------------------------------------------------ Rune Step: a dash that leaves a binding rune trail
    static boolean runeStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        if (!ElementBook.dash(0f, 9, false, VfxShape.CURSE_WARDING_FX1, ElementBook.NONE).cast(b, i, p, mode)) return false;
        Vec3 to = p.position();
        cleanse(p);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
        b.vfx(p, VfxShape.CURSE_WARDING_FX2, from, from.add(0, 1, 0), 60, 1.8f);
        SpellRuntime.zone(p.serverLevel(), 60, 10, age -> {
            for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), to.add(0, 1, 0), 1.5)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                hex(t, 30, 0);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Malevolent Femcantation: the chanted curse that spreads
    static boolean malevolent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to curse."); return false; }
        boolean boss = BalanceLaw.isBoss(t);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 16, 1.0f);
        b.hurt(i, p, t, mode, 9f);
        EnergyBridge.spirit(t, boss ? 2 : 6);
        hex(t, 160, 1);
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
        Vec3 at = t.getBoundingBox().getCenter();
        for (LivingEntity o : foes(p, at, 4 * k(p, i), 8)) {
            if (o == t) continue;
            hex(o, 100, 0);
            o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        b.vfx(p, VfxShape.CURSE_WARDING_FX3, at, at, 24, 1.0f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX2, t.position(), t.position().add(0, 1, 0), 40, 4f * k(p, i));
        return true;
    }

    // ------------------------------------------------------------------ Curse Transfer: move the debuffs onto an enemy
    static boolean transfer(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No enemy to hand the curses to."); return false; }
        boolean boss = BalanceLaw.isBoss(t);
        int moved = 0;
        for (LivingEntity f : friends(p, p.position(), 8 * k(p, i))) {
            for (MobEffectInstance e : harmful(f)) {
                if (moved >= 6) break;
                f.removeEffect(e.getEffect());
                int ticks = e.isInfiniteDuration() ? 300 : Math.min(e.getDuration(), 400);
                t.addEffect(new MobEffectInstance(e.getEffect(), boss ? ticks / 2 : ticks, e.getAmplifier()));
                moved++;
            }
            if (moved > 0) b.vfx(p, VfxShape.CURSE_WARDING_FX1, f.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 14, 0.8f);
        }
        if (moved == 0) { GrimoireBook.fail(p, "There is no curse to move."); return false; }
        b.hurt(i, p, t, mode, 3f * moved);
        b.castCircle(p, 0.9f);
        Vec3 at = t.getBoundingBox().getCenter();
        b.vfx(p, VfxShape.CURSE_WARDING_FX3, at, at, 24, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Mirror Ward: curses reflect back, blows are thorned
    static boolean mirrorWard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong(MIRROR, p.level().getGameTime() + 240);
        reflectCurses(p);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CURSE_WARDING_FX2, p, p.position(), b.color, 240, 2.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_WARDING_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), b.color, 26, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Unbound Slave: the target obeys the caster
    static boolean unboundSlave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to bind."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 200);
        b.hurt(i, p, t, mode, 4f);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, BalanceLaw.isBoss(t) ? 1 : 3));
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 16, 1.0f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX2, t.position(), t.position().add(0, 1, 0), ticks, 2f);
        if (t instanceof Mob m) {
            SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
                if (!m.isAlive()) return;
                m.setTarget(nearestFoe(p, m.position(), 14, m));
            });
        } else {
            EnergyBridge.effect(t, "fear", ticks, 0);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        }
        return true;
    }

    // ------------------------------------------------------------------ Ward Circle: a sanctuary that absorbs curses and turns shots back
    static boolean wardCircle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(p, i);
        Vec3 c = GrimoireBook.aim(p, 14);
        double r = 5.5 * s;
        int ticks = 240;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        b.vfx(p, VfxShape.CURSE_WARDING_FX3, c, c.add(0, 1, 0), 26, 1.2f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            boolean pulse = age % 20 == 0;
            for (LivingEntity f : friends(p, c, r)) {
                if (pulse) {
                    int n = cleanse(f);
                    if (n > 0) BalanceLaw.heal(f, 1.5f * Math.min(n, 4));
                }
                f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0));
                f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0));
            }
            List<Projectile> shots = p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r),
                    pr -> pr.getOwner() != p && !(pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)) && pr.position().distanceToSqr(c) <= r * r);
            for (Projectile pr : shots.size() > 6 ? shots.subList(0, 6) : shots) {
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0));
                pr.setOwner(p);
                pr.hurtMarked = true;
                VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_WARDING_FX3, pr.position(), pr.position(), COLOR, 12, 0.5f);
            }
            if (age % 60 == 0 && age > 0) VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_WARDING_FX3, c, c.add(0, 1, 0), COLOR, 22, 0.9f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Decaying World: strands of runes that bind and unmake spells
    static boolean decayingWorld(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(p, i);
        double r = 7 * s;
        int ticks = 300;
        p.getPersistentData().putLong(DECAY, p.level().getGameTime() + ticks);
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CURSE_WARDING_FX2, p, p.position(), b.color, ticks, (float) r);
        b.vfx(p, VfxShape.CURSE_WARDING_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 30, 1.5f);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            Vec3 c = p.position();
            for (LivingEntity t : foes(p, c, r, 16)) {
                boolean boss = BalanceLaw.isBoss(t);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, boss ? 1 : 2));
                hex(t, 40, 0);
                EnergyBridge.effect(t, "silence", 30, 0);
                if (age % 30 == 0) b.hurt(i, p, t, mode, 2.5f);
            }
            if (age % 20 == 0) {
                Nullification.shatterBarriers(p.serverLevel(), c, r, p);
                SpellRuntime.dissolveBolts(p.serverLevel(), c, r, p);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Exploding Life: the bestowed devil power detonates
    static boolean explodingLife(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to detonate."); return false; }
        float s = k(p, i);
        boolean boss = BalanceLaw.isBoss(t);
        int charge = 50;
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.CURSE_WARDING_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 16, 1.2f);
        b.vfx(p, VfxShape.MANA_CHARGE, t.position(), t.position().add(0, 1, 0), charge, 1.2f);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, charge + 10, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, charge, boss ? 1 : 2));
        EnergyBridge.effect(t, "silence", charge, 0);
        Vec3[] last = {t.position()};
        SpellRuntime.zone(p.serverLevel(), charge, 10, age -> {
            if (!t.isAlive()) return;
            last[0] = t.position();
            Vec3 at = t.getBoundingBox().getCenter();
            b.vfx(p, VfxShape.CURSE_WARDING_FX3, at, at, 12, 0.4f + 0.12f * (age / 10));
        });
        SpellRuntime.later(p.serverLevel(), charge, () -> {
            boolean alive = t.isAlive();
            Vec3 c = (alive ? t.position() : last[0]).add(0, 1, 0);
            if (alive) {
                b.hurt(i, p, t, mode, boss ? 14f : 30f);
                EnergyBridge.spirit(t, boss ? 4 : 20);
            }
            double r = 5 * s;
            for (LivingEntity o : foes(p, c, r, 10)) {
                if (o == t) continue;
                b.hurt(i, p, o, mode, alive ? 14f : 8f);
                Vec3 out = o.position().subtract(c).multiply(1, 0, 1);
                out = out.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : out.normalize();
                o.setDeltaMovement(o.getDeltaMovement().add(out.scale(BalanceLaw.isBoss(o) ? 0.3 : 1.1)).add(0, 0.4, 0));
                o.hurtMarked = true;
            }
            b.vfx(p, VfxShape.CURSE_WARDING_FX3, c, c.add(0, 1, 0), 32, 1.6f);
            b.vfx(p, VfxShape.CURSE_WARDING_FX2, c.subtract(0, 1, 0), c, 30, (float) r);
            b.impact(p, c, 1.4f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Undying Curse: the first deadly blow leaves you alive
    static boolean undying(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong(UNDYING, p.level().getGameTime() + 1200);
        cleanse(p);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.CURSE_WARDING_FX2, p, p.position(), b.color, 80, 2.0f);
        VfxSpawn.send(p.serverLevel(), VfxShape.CURSE_WARDING_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), b.color, 30, 1.2f);
        return true;
    }
}
