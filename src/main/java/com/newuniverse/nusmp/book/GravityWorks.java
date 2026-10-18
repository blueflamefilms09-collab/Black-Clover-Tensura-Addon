package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.prop.GravitySwordParam;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 Gravity Magic rebuild (Black Clover: Dante Zogratis's Gravity Magic; the wiki: lower gravity to float, raise it to slam, add weight to a blow, compress
 * stone into a sword, make a small black hole). The five old pages keep their ids, order and numbers; they gain the Demon King's regalia (the temporary armour,
 * client.aura.GravityAura, five seconds after a cast), the own effects (GRAVITY_FX1..3) and the summon models ({@link PropKind#GRAVITY_1} the singularity orb,
 * {@link PropKind#GRAVITY_2} the compressed rock sword). Gravity Sphere is now a bolt carrying the orb model. Four pages are appended:
 * <ul>
 *   <li><b>Heavy Infighting: Gladiator</b> (wiki): three slams in a row on one foe, 8 x EP scale each, the ground answering every one.</li>
 *   <li><b>Evil God's Pressure Craft</b> (wiki): a rock is pulled out of the ground, compressed to a great sword that rises, turns to the target and is launched:
 *       26 x EP scale along its path, 30 percent of armour ignored, piercing.</li>
 *   <li><b>Lightened Step</b> (wiki: lowering gravity lets one float): you and the allies within 4 blocks float, leap and run light for 8 s.</li>
 *   <li><b>Weighted Blows</b> (wiki: weight on a weapon adds damage): 15 s of melee blows that land 40 percent (+5 per EP step) harder and press the foe down.</li>
 * </ul>
 * Gravity Singularity, the black hole itself, stays a forbidden ritual and not a page (as the book always said); the orb is its model and Gravity Sphere its small form.
 */
public final class GravityWorks {
    private GravityWorks() {}

    static final String K_WEIGHT = "nusmp_gravity_weight_until";
    private static final int VIOLET = 0xFFB45AFF;

    static void fx(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) { VfxSpawn.send(level, shape, from, to, VIOLET, ticks, power); }

    public static void initHooks() {
        AttributeEvents.incoming(e -> {
            if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p) return;
            if (p.getPersistentData().getLong(K_WEIGHT) <= p.level().getGameTime() || e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) return;
            LivingEntity t = e.getEntity();
            e.setAmount(e.getAmount() * (1.4f + 0.05f * EnergyBridge.modifier(p)));
            Vec3 v = t.getDeltaMovement();
            t.setDeltaMovement(v.x * 0.5, Math.min(v.y, 0) - 0.6, v.z * 0.5);
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            fx(p.serverLevel(), VfxShape.GRAVITY_FX3, t.position(), t.position().add(0, 1, 0), 14, 0.6f);
        });
        AttributeEvents.logout(p -> p.getPersistentData().remove(K_WEIGHT));
    }

    static BookPage.Cast armoured(BookPage.Cast c, int style) { return SpatialArmourCast.armoured(c, Aura.GRAVITY, style); }

    // ================================================================ the old pages with the rebuild's effects
    /** Heavy Infighting: the old slam, with its crush. */
    static BookPage.Cast infighting() {
        return (b, i, p, mode) -> {
            LivingEntity t = GrimoireBook.target(p, 6);
            if (!GravityBook.infighting(b, i, p, mode)) return false;
            if (t != null) fx(p.serverLevel(), VfxShape.GRAVITY_FX3, t.position(), t.position().add(0, 1, 0), 26, 1.0f * EnergyBridge.scale(p));
            return true;
        };
    }

    /** Presence of the Demon King: the old domain, with the well drawn under the caster for its whole six seconds. */
    static BookPage.Cast presence() {
        return (b, i, p, mode) -> {
            if (!GravityBook.presence(b, i, p, mode)) return false;
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GRAVITY_FX2, p, p.position(), VIOLET, 120, 8f);
            return true;
        };
    }

    /** Gravity Sphere: a bolt of the singularity orb (11 x EP scale, slow III for two seconds, a downward pull), the crush where it ends. */
    static boolean sphere(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), start = eye.add(dir.scale(0.9));
        float pw = EnergyBridge.scale(p) * GrimoireBook.size(i, p);
        MagicPropEntity orb = MagicProps.spawn(sl, PropKind.GRAVITY_1, start.subtract(0, 0.5 * pw, 0), p.getYRot(), pw, 40, 1, p);
        Vec3 vel = dir.scale(1.2);
        if (orb != null) orb.setDeltaMovement(vel);
        fx(sl, VfxShape.GRAVITY_FX1, eye.add(0, -0.25, 0), start.add(dir.scale(10)), 10, 0.7f * pw);
        SpellRuntime.bolt(p, start, vel, 1.1, 22, false, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 11f * EnergyBridge.scale(p));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            t.setDeltaMovement(t.getDeltaMovement().x, Math.min(t.getDeltaMovement().y, 0) - 0.8, t.getDeltaMovement().z);
            t.hurtMarked = true;
        }, (bolt, at) -> {
            if (orb != null) orb.expire();
            fx(sl, VfxShape.GRAVITY_FX3, at, at.add(0, 1, 0), 24, 0.9f * pw);
        });
        b.castCircle(p, 0.8f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    /** Gravity Well: the old well, with the well effect drawn on the aimed floor. */
    static BookPage.Cast well(BookPage.Cast old) {
        return (b, i, p, mode) -> {
            if (!old.cast(b, i, p, mode)) return false;
            Vec3 c = GrimoireBook.aim(p, 24);
            fx(p.serverLevel(), VfxShape.GRAVITY_FX2, c, c.add(0, 1, 0), 100, 5f * GrimoireBook.size(i, p));
            return true;
        };
    }

    /** Crushing Weight: the old crush, with its shock drawn on the aimed floor. */
    static BookPage.Cast crush(BookPage.Cast old) {
        return (b, i, p, mode) -> {
            if (!old.cast(b, i, p, mode)) return false;
            Vec3 c = GrimoireBook.aim(p, 24);
            fx(p.serverLevel(), VfxShape.GRAVITY_FX3, c, c.add(0, 1, 0), 28, 6f * GrimoireBook.size(i, p) / 2.6f);
            return true;
        };
    }

    // ================================================================ new pages (appended through GravityExt)
    /** Heavy Infighting: Gladiator: three slams. */
    static boolean gladiator(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 8);
        if (t == null) { GrimoireBook.fail(p, "Too far to fight."); return false; }
        ServerLevel sl = p.serverLevel();
        float pw = EnergyBridge.scale(p);
        for (int k = 0; k < 3; k++) {
            int kk = k;
            SpellRuntime.later(sl, k * 5, () -> {
                if (!t.isAlive() || t.distanceToSqr(p) > 144) return;
                t.setDeltaMovement(t.getDeltaMovement().x, -1.6, t.getDeltaMovement().z);
                t.hurtMarked = true;
                b.hurt(i, p, t, mode, 8f * pw);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                fx(sl, VfxShape.GRAVITY_FX3, t.position(), t.position().add(0, 1, 0), 22, 0.8f * pw);
                sl.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 0.6f + 0.15f * kk);
            });
        }
        fx(sl, VfxShape.GRAVITY_FX1, p.getEyePosition().add(0, -0.25, 0), t.getBoundingBox().getCenter(), 10, 0.8f * pw);
        b.castCircle(p, 0.8f);
        return true;
    }

    /** Evil God's Pressure Craft: the rock sword. */
    static boolean pressureCraft(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        Vec3 flat = p.getViewVector(1f).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        float pw = EnergyBridge.scale(p) * (0.9f + 0.1f * GrimoireBook.size(i, p));
        Vec3 base = p.position().add(flat.scale(2.6));
        Vec3 aim = GrimoireBook.aim(p, 32);
        MagicPropEntity sword = MagicProps.spawn(sl, PropKind.GRAVITY_2, base, p.getYRot(), pw, 70, GravitySwordParam.of(GravitySwordParam.FORMING, 90f), p);
        if (sword == null) return false;
        fx(sl, VfxShape.GRAVITY_FX2, base, base.add(0, 1, 0), 36, 2.4f);
        b.castCircle(p, 1.2f);
        sl.playSound(null, p.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
        SpellRuntime.zone(sl, 36, 1, age -> {
            if (!sword.isAlive()) return;
            Vec3 centre = sword.position().add(0, 0.9 * pw, 0);
            Vec3 to = aim.subtract(centre);
            double len = Math.max(0.5, to.length());
            float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z)), pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(to.y / len, -1.0, 1.0)));
            if (age >= 24 && age < 32) {                                                    // hover: turn to the target
                float k = (age - 24) / 8f;
                sword.setYRot(Mth.rotLerp(k, sword.getYRot(), yaw));
                sword.setParam(GravitySwordParam.of(GravitySwordParam.HOVER, Mth.lerp(k, 90f, pitch)));
            } else if (age == 32) {                                                         // launch
                sword.setYRot(yaw);
                Vec3 vel = to.normalize().scale(1.6);
                sword.setParam(GravitySwordParam.of(GravitySwordParam.LAUNCHED, pitch));
                sword.setDeltaMovement(vel);
                fx(sl, VfxShape.GRAVITY_FX1, centre, centre.add(to.normalize().scale(12)), 10, 1.0f * pw);
                SpellRuntime.bolt(p, centre, vel, 1.3, 20, true, null, (bolt, t) -> {
                    float raw = 26f * EnergyBridge.scale(p);
                    raw *= EnergyBridge.armourBypass(t, p.damageSources().generic(), raw, 0.3f);
                    b.hurt(i, p, t, mode, raw);
                    fx(sl, VfxShape.GRAVITY_FX3, t.position(), t.position().add(0, 1, 0), 22, 0.9f * pw);
                }, (bolt, at) -> {
                    sword.expire();
                    fx(sl, VfxShape.GRAVITY_FX3, at, at.add(0, 1, 0), 26, 1.1f * pw);
                });
                sl.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2f, 0.5f);
            }
        });
        return true;
    }

    /** Lightened Step: float, leap and run light, for you and the allies by you. */
    static boolean lightenedStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> who = new ArrayList<>();
        who.add(p);
        for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(4), x -> x != p && x.isAlive() && x.isAlliedTo(p))) who.add(a);
        for (LivingEntity e : who) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
            e.addEffect(new MobEffectInstance(MobEffects.JUMP, 160, 2));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
        }
        fx(p.serverLevel(), VfxShape.GRAVITY_FX2, p.position(), p.position().add(0, 1, 0), 40, 2.5f);
        b.castCircle(p, 1f);
        return true;
    }

    /** Weighted Blows: 15 s of heavier melee. */
    static boolean weightedBlows(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.getPersistentData().putLong(K_WEIGHT, p.level().getGameTime() + 300);
        SpatialArmourCast.on(p, Aura.GRAVITY, 300, 1);
        fx(p.serverLevel(), VfxShape.GRAVITY_FX3, p.position(), p.position().add(0, 1, 0), 20, 0.9f);
        b.castCircle(p, 1f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.9f, 0.5f);
        return true;
    }
}
