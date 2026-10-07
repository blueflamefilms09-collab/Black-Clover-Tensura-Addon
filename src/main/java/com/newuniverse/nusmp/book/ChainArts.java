package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Chain Magic: chains that pierce, drag, cage and bind. */
final class ChainArts {
    private ChainArts() {}

    private static final VfxShape CAST = VfxShape.CHAIN_FX1, ZONE = VfxShape.CHAIN_FX2, BURST = VfxShape.CHAIN_FX3;

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.35, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    /** Drags the target towards a point (bosses barely move). */
    static void pull(LivingEntity t, Vec3 to, double strength) {
        double k = BalanceLaw.isBoss(t) ? strength * 0.25 : strength;
        Vec3 d = to.subtract(t.position());
        double len = Math.max(0.5, d.length());
        Vec3 v = d.scale(k / len);
        t.setDeltaMovement(v.x, Math.min(0.5, v.y * 0.5 + 0.2), v.z);
        t.hurtMarked = true;
    }

    /** Rider: the hit target is dragged towards the caster. */
    static ElementBook.Rider drag(double strength) { return (t, p) -> pull(t, p.position(), strength); }

    /** Rider for the kunai chains: the wound bleeds magicules and the target is slowed. */
    static ElementBook.Rider barbs() { return (t, p) -> { Nullification.bleed(t, 0.02); slow(t, 30, 1); }; }

    /** Scorpio Chain: a barbed chain pierces the target, hauls it in and stings with poison. */
    static boolean scorpio(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "Nothing to hook."); return false; }
        b.castCircle(p, 0.7f);
        b.vfx(p, CAST, hand(p), mid(t), 10, 1f);
        SpellRuntime.later(p.serverLevel(), 6, () -> {
            if (!t.isAlive()) return;
            pull(t, p.position(), Math.min(1.7, 0.4 + t.distanceTo(p) * 0.12));
            b.hurt(i, p, t, mode, 9f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            slow(t, 40, 1);
            b.vfx(p, BURST, mid(t), mid(t), 20, 0.8f);
        });
        return true;
    }

    /** Dance of the Pitless Viper: chains with snake heads strike the target from five sides, one after another. */
    static boolean viper(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null) { GrimoireBook.fail(p, "Nothing to strike."); return false; }
        b.castCircle(p, 1f);
        for (int n = 0; n < 5; n++) {
            final int k = n;
            double a = k * Math.PI * 2 / 5 + p.getYRot() * 0.0174533;
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                if (!t.isAlive()) return;
                Vec3 target = mid(t);
                Vec3 origin = target.add(Math.cos(a) * 3.5, 0.6 + (k % 2) * 1.3, Math.sin(a) * 3.5);
                b.vfx(p, CAST, origin, target, 8, 0.9f);
                SpellRuntime.later(p.serverLevel(), 5, () -> {
                    if (!t.isAlive()) return;
                    b.hurt(i, p, t, mode, 5f);
                    Nullification.bleed(t, 0.01);
                    if (k == 4) {
                        t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                        slow(t, 60, 2);
                        b.vfx(p, BURST, mid(t), mid(t), 22, 1f);
                    }
                });
            });
        }
        return true;
    }

    /** Heavy Chain: a weighted chain is hurled at the aim point and slams down, crushing everything under it. */
    static boolean heavy(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = GrimoireBook.aim(p, 20);
        double r = 3.5 * GrimoireBook.size(i, p);
        b.castCircle(p, 0.9f);
        b.vfx(p, CAST, hand(p), at, 9, 1.2f);
        SpellRuntime.later(p.serverLevel(), 8, () -> {
            for (LivingEntity t : GrimoireBook.around(p, at, r)) {
                b.hurt(i, p, t, mode, 13f);
                slow(t, 80, 3);
                EnergyBridge.effect(t, "burden", 80, 1);
                Vec3 v = t.getDeltaMovement();
                t.setDeltaMovement(v.x * 0.2, Math.min(v.y, 0) - 0.8, v.z * 0.2);
                t.hurtMarked = true;
            }
            b.vfx(p, BURST, at, at.add(0, 1, 0), 24, 1.3f);
        });
        return true;
    }

    /** Grapple Chain: the chain bites into the aim point (or a foe) and reels the caster across. */
    static boolean grapple(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 28);
        Vec3 point;
        if (t != null) point = mid(t);
        else {
            HitResult h = p.pick(28, 1f, false);
            if (h.getType() == HitResult.Type.MISS) { GrimoireBook.fail(p, "Nothing to anchor the chain to."); return false; }
            point = h.getLocation();
        }
        Vec3 d = point.subtract(p.getEyePosition());
        double dist = d.length();
        if (dist < 3) { GrimoireBook.fail(p, "Too close to anchor."); return false; }
        b.castCircle(p, 0.7f);
        b.vfx(p, CAST, hand(p), point, 12, 1f);
        Vec3 v = d.scale(Math.min(2.6, 0.9 + dist * 0.09) / dist);
        p.setDeltaMovement(v.x, v.y + 0.25, v.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0));
        if (t != null) SpellRuntime.later(p.serverLevel(), 6, () -> {
            if (!t.isAlive()) return;
            b.hurt(i, p, t, mode, 5f);
            slow(t, 40, 1);
            b.vfx(p, BURST, mid(t), mid(t), 18, 0.7f);
        });
        return true;
    }

    /** Chain Aegis: chains circle you, strike what comes close and tear enemy shots out of the air. */
    static boolean aegis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 3.5 * GrimoireBook.size(i, p);
        int ticks = 160;
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        VfxSpawn.sendFollowing(p.serverLevel(), ZONE, p, p.position(), b.color, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                b.hurt(i, p, t, mode, 3f);
                slow(t, 40, 2);
            }
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.discard();
            }
        });
        return true;
    }

    /** Chain Cage: a ring of chains rises around the aim point and drags everything that tries to leave back inside. */
    static boolean cage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 4.5 * GrimoireBook.size(i, p);
        if (!GrimoireBook.control(p)) return false;
        int ticks = 100;
        b.castCircle(p, 1.2f);
        b.vfx(p, ZONE, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r + 2)) {
                boolean soft = t instanceof Player || BalanceLaw.isBoss(t);
                if (soft && age > (BalanceLaw.isBoss(t) ? 20 : 60)) continue;
                Vec3 flat = c.subtract(t.position()).multiply(1, 0, 1);
                if (flat.length() > r - 0.8 && flat.length() > 0.01) {
                    Vec3 v = flat.normalize().scale(0.45);
                    t.setDeltaMovement(v.x, t.getDeltaMovement().y, v.z);
                    t.hurtMarked = true;
                }
                if (flat.length() <= r + 0.5) {
                    slow(t, 30, 2);
                    if (age % 20 == 0) b.hurt(i, p, t, mode, 3f);
                }
            }
        });
        return true;
    }

    /** Magic Binding Iron Chain Formation: chains coil round the torso and anchor in the ground; the target cannot move or use magic. */
    static boolean ironFormation(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "Nothing to bind."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 140);
        Vec3 c = mid(t);
        b.castCircle(p, 1.4f);
        b.vfx(p, CAST, hand(p), c, 10, 1.2f);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            b.vfx(p, CAST, c, t.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5), 14, 0.8f);
        }
        b.vfx(p, ZONE, t.position(), t.position().add(0, 1, 0), ticks, (float) Math.max(1.8, t.getBbWidth() + 1.5));
        b.vfx(p, BURST, c, c, 26, 1.2f);
        slow(t, ticks, 9);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        EnergyBridge.effect(t, "silence", ticks, 0);
        if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", t.level().getGameTime() + ticks);
        Nullification.jamAll(t, Math.max(1, ticks / 20), 0.6f);
        b.hurt(i, p, t, mode, 10f);
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            if (!t.isAlive()) return;
            t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
            t.hurtMarked = true;
            if (age % 20 == 0 && age > 0) {
                EnergyBridge.drain(t, p, 0.02);
                EnergyBridge.burnAura(t, 0.03);
            }
        });
        return true;
    }

    /** Chain Reckoning: chains from every side run through up to six foes, drain their magicules and mend the caster. */
    static boolean reckoning(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 12 * GrimoireBook.size(i, p);
        List<LivingEntity> foes = GrimoireBook.around(p, p.position(), r).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(p))).limit(6).toList();
        if (foes.isEmpty()) { GrimoireBook.fail(p, "No one in reach."); return false; }
        b.castCircle(p, 1.5f);
        b.vfx(p, ZONE, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        for (LivingEntity t : foes) {
            b.vfx(p, CAST, hand(p), mid(t), 12, 1f);
            b.hurt(i, p, t, mode, 8f);
            slow(t, 100, 3);
            EnergyBridge.drain(t, p, 0.08);
            EnergyBridge.burnAura(t, 0.1);
            BalanceLaw.heal(p, 1.5f);
            b.vfx(p, BURST, mid(t), mid(t), 20, 0.8f);
        }
        return true;
    }
}
