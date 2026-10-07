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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon Fire Magic: the spell logic of the grimoire. The black flame is not fire damage: it hits as magic, so fire resistance and
 * fire immunity do nothing against it; what it leaves on a target is the demon burn (a pulsing, refreshing burn kept in the target's
 * persistent data), and it can be detonated by an Ignition Blast.
 */
final class DemonFireArts {
    private DemonFireArts() {}

    private static final String BURN = "nusmp_dfire_until";
    private static final int CAP = 14;

    // ---------------------------------------------------------------- helpers
    private static <T> List<T> cap(List<T> l) { return l.size() > CAP ? l.subList(0, CAP) : l; }

    private static boolean burning(LivingEntity t) { return t.getPersistentData().getLong(BURN) > t.level().getGameTime(); }

    private static void fx1(GrimoireBook b, ServerPlayer p, Vec3 from, Vec3 to, int ticks, float power) { b.vfx(p, VfxShape.DEMON_FIRE_FX1, from, to, ticks, power); }
    private static void fx2(GrimoireBook b, ServerPlayer p, Vec3 c, double radius, int ticks) { b.vfx(p, VfxShape.DEMON_FIRE_FX2, c, c, ticks, (float) radius); }
    private static void fx3(GrimoireBook b, ServerPlayer p, Vec3 c, float power) { b.vfx(p, VfxShape.DEMON_FIRE_FX3, c, c.add(0, 1, 0), 24, power); }

    /** A hit of black flame: balance-law damage, halved against bosses. */
    private static void hit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw) {
        b.hurt(i, p, t, mode, BalanceLaw.isBoss(t) ? raw * 0.5f : raw);
    }

    /** Puts the demon burn on a target: every second it takes 'pulse' damage (ignoring fire resistance) until 'ticks' run out. Re-applying extends it. */
    static void scorch(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, int ticks, float pulse) {
        boolean boss = BalanceLaw.isBoss(t);
        long now = t.level().getGameTime();
        var d = t.getPersistentData();
        boolean running = d.getLong(BURN) > now;
        d.putLong(BURN, Math.max(d.getLong(BURN), now + (boss ? Math.min(ticks, 60) : ticks)));
        t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
        if (!running) pulse(b, i, p, t, mode, boss ? pulse * 0.4f : pulse);
    }

    private static void pulse(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float dmg) {
        SpellRuntime.later(p.serverLevel(), 20, () -> {
            if (!t.isAlive() || p.isRemoved() || !burning(t)) return;
            b.hurt(i, p, t, mode, dmg);
            EnergyBridge.spirit(t, 1);
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
            Vec3 at = t.getBoundingBox().getCenter();
            b.vfx(p, VfxShape.DEMON_FIRE_FX3, at, at, 14, 0.35f);
            pulse(b, i, p, t, mode, dmg);
        });
    }

    private static void knock(LivingEntity t, Vec3 from, double k) {
        Vec3 d = t.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) return;
        d = d.normalize();
        t.knockback(k, -d.x, -d.z);
    }

    private static void lift(LivingEntity t, double y) {
        t.setDeltaMovement(t.getDeltaMovement().add(0, BalanceLaw.isBoss(t) ? y * 0.3 : y, 0));
        t.hurtMarked = true;
    }

    // ---------------------------------------------------------------- starters
    /** Five black flame bullets, one after another along your line of sight. */
    static boolean bullets(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 0.7f);
        for (int k = 0; k < 5; k++) {
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                if (!p.isAlive()) return;
                Vec3 start = p.getEyePosition().add(0, -0.15, 0), dir = p.getViewVector(1f);
                fx1(b, p, start, start.add(dir.scale(1.5 * 18)), 14, 0.6f);
                SpellRuntime.bolt(p, start, dir.scale(1.5), 0.4 * s, 18, false, null, (bolt, t) -> {
                    hit(b, i, p, t, mode, 3.5f);
                    scorch(b, i, p, t, mode, 60, 1.5f);
                }, (bolt, at) -> fx3(b, p, at, 0.4f));
            });
        }
        return true;
    }

    // ---------------------------------------------------------------- mid
    /** A beam into the soul: burns through barriers, bites spirit and aura, bleeds magicules, shreds resistance. */
    static boolean soulScorch(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null) { GrimoireBook.fail(p, "No one to burn."); return false; }
        Vec3 c = t.getBoundingBox().getCenter();
        b.castCircle(p, 0.8f);
        Nullification.shatterBarriers(p.serverLevel(), c, 3, p);
        hit(b, i, p, t, mode, 6f);
        EnergyBridge.spirit(t, 3);
        EnergyBridge.burnAura(t, 0.08);
        Nullification.bleed(t, 0.04);
        EnergyBridge.effect(t, "fragility", 100, 0);
        scorch(b, i, p, t, mode, 100, 2f);
        fx1(b, p, p.getEyePosition(), c, 12, 0.9f);
        fx3(b, p, c, 0.9f);
        return true;
    }

    /** A bolt that explodes: foes already under the demon burn are detonated for almost double damage. */
    static boolean ignitionBlast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.8f);
        fx1(b, p, start, start.add(dir.scale(1.3 * 20)), 20, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.3), 0.6 * s, 20, false, null, (bolt, t) -> {}, (bolt, at) -> {
            for (LivingEntity t : cap(GrimoireBook.around(p, at, 3.8 * s))) {
                float raw = 7f;
                if (burning(t)) { raw *= 1.8f; t.getPersistentData().putLong(BURN, 0); }
                hit(b, i, p, t, mode, raw);
                knock(t, at, 0.9);
                scorch(b, i, p, t, mode, 60, 1.5f);
            }
            fx3(b, p, at, 1.4f);
        });
        return true;
    }

    /** A pillar of black fire erupts under the aim point after a short warning ring. */
    static boolean pillar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 2.5 * s;
        b.castCircle(p, 0.8f);
        fx2(b, p, c, r, 18);
        SpellRuntime.later(p.serverLevel(), 18, () -> {
            for (LivingEntity t : cap(GrimoireBook.around(p, c, r))) {
                hit(b, i, p, t, mode, 10f);
                lift(t, 0.9);
                scorch(b, i, p, t, mode, 80, 2f);
            }
            fx1(b, p, c, c.add(0, 9, 0), 18, 1.0f);
            fx3(b, p, c, 1.2f);
        });
        return true;
    }

    /** A wall of flame rolls away from you in four surges. */
    static boolean wave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.position().add(0, 0.9, 0), dir = p.getViewVector(1f).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(0, 0, 1);
        final Vec3 d = dir.normalize();
        b.castCircle(p, 0.9f);
        fx1(b, p, a, a.add(d.scale(16 * s)), 16, 1.2f);
        for (int k = 0; k < 4; k++) {
            final int seg = k;
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                Vec3 from = a.add(d.scale(seg * 4 * s)), to = a.add(d.scale((seg + 1) * 4 * s));
                for (LivingEntity t : cap(GrimoireBook.along(p, from, to, 1.8))) {
                    hit(b, i, p, t, mode, 5f);
                    knock(t, a, 0.7);
                    scorch(b, i, p, t, mode, 60, 1.5f);
                }
                fx3(b, p, from.add(to).scale(0.5), 0.8f);
            });
        }
        return true;
    }

    /** Become flame and step through the fight: burns what you pass through and leaves a trail of fire. */
    static boolean flameStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dest = null;
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) { GrimoireBook.fail(p, "No room to move."); return false; }
        dir = dir.normalize();
        for (double dist = 10; dist >= 1; dist -= 0.5) {
            Vec3 c = from.add(dir.scale(dist));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to move."); return false; }
        for (LivingEntity t : cap(GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.3))) {
            hit(b, i, p, t, mode, 5f);
            scorch(b, i, p, t, mode, 60, 1.5f);
        }
        b.castCircle(p, 0.6f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        fx1(b, p, from.add(0, 1, 0), dest.add(0, 1, 0), 12, 1.0f);
        final Vec3 start = from, end = dest;
        for (int k = 0; k < 5; k++) fx2(b, p, start.add(end.subtract(start).scale(k / 4.0)), 1.2, 60);
        SpellRuntime.zone(p.serverLevel(), 60, 10, age -> {
            for (int k = 0; k < 5; k++) {
                Vec3 pt = start.add(end.subtract(start).scale(k / 4.0));
                for (LivingEntity t : cap(GrimoireBook.around(p, pt, 1.6))) {
                    hit(b, i, p, t, mode, 1.5f);
                    scorch(b, i, p, t, mode, 40, 1f);
                }
            }
        });
        return true;
    }

    /** The body turns to black flame: fire immunity, speed, a tougher frame, and a burning aura around you. */
    static boolean flameBody(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.1f);
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        p.setRemainingFireTicks(0);
        fx3(b, p, p.position().add(0, 1, 0), 1.0f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEMON_FIRE_FX2, p, p.position(), b.color, 200, 2.5f);
        SpellRuntime.zone(p.serverLevel(), 200, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : cap(GrimoireBook.around(p, p.position(), 3))) {
                hit(b, i, p, t, mode, 2f);
                scorch(b, i, p, t, mode, 40, 1f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- zones
    /** A black twister at the aim point: it drags everything in, shreds it and sets it on fire. */
    static boolean twister(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        double r = 5 * s;
        b.castCircle(p, 1.2f);
        fx2(b, p, c, r, 100);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            for (LivingEntity t : cap(GrimoireBook.around(p, c, r))) {
                if (!BalanceLaw.isBoss(t)) {
                    Vec3 in = c.subtract(t.position()).multiply(1, 0, 1);
                    if (in.lengthSqr() > 1e-4) in = in.normalize().scale(0.35);
                    t.setDeltaMovement(t.getDeltaMovement().scale(0.5).add(in.x, 0.15, in.z));
                    t.hurtMarked = true;
                }
                hit(b, i, p, t, mode, 2.5f);
                scorch(b, i, p, t, mode, 50, 1.2f);
            }
            if (age % 40 == 0) fx3(b, p, c, 1.0f);
        });
        return true;
    }

    /** A ring of black flame around you: burns what touches it and devours spells and arrows thrown at you. */
    static boolean veil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 3.5 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.DEMON_FIRE_FX2, p, p.position(), b.color, 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (!p.isAlive()) return;
            Nullification.shatterBarriers(p.serverLevel(), p.position(), r, p);
            for (LivingEntity t : cap(GrimoireBook.around(p, p.position(), r))) {
                hit(b, i, p, t, mode, 1.5f);
                scorch(b, i, p, t, mode, 40, 1f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- signature and daily
    /** Eight pillars of black fire fall across a wide ring, one after another. */
    static boolean apocalypse(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 28);
        double r = 10 * s;
        b.castCircle(p, 1.6f);
        Nullification.shatterBarriers(p.serverLevel(), c, r, p);
        fx2(b, p, c, r, 70);
        for (int k = 0; k < 8; k++) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2, dist = Math.sqrt(p.getRandom().nextDouble()) * r * 0.85;
            Vec3 pt = c.add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
            SpellRuntime.later(p.serverLevel(), 20 + k * 6, () -> {
                for (LivingEntity t : cap(GrimoireBook.around(p, pt, 3 * s))) {
                    hit(b, i, p, t, mode, 11f);
                    lift(t, 0.8);
                    EnergyBridge.spirit(t, 2);
                    scorch(b, i, p, t, mode, 120, 2.5f);
                }
                fx1(b, p, pt, pt.add(0, 10, 0), 18, 1.1f);
                fx3(b, p, pt, 1.2f);
            });
        }
        return true;
    }

    /** Once a day: the caster burns down to cinders and rises again, feeding on the foes caught in the blaze. */
    static boolean cinderRebirth(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 6 * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        int n = 0;
        for (LivingEntity t : cap(GrimoireBook.around(p, c, r))) {
            hit(b, i, p, t, mode, 12f);
            knock(t, c, 0.8);
            scorch(b, i, p, t, mode, 100, 2f);
            n++;
        }
        BalanceLaw.heal(p, p.getMaxHealth() * 0.2f + 2f * Math.min(n, 5));
        p.removeEffect(MobEffects.POISON);
        p.removeEffect(MobEffects.WITHER);
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        b.castCircle(p, 1.4f);
        fx2(b, p, c, r, 40);
        fx3(b, p, c.add(0, 1, 0), 1.6f);
        return true;
    }
}
