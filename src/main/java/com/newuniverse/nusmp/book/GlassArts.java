package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Glass Magic: the spells of GlassBook that need their own logic. Everything here cuts: a glass hit leaves a stacking bleed
 * (up to 8 cuts on a target, each one a second of extra damage), Shatter pops the cuts for a burst, Verre Detection reveals.
 */
public final class GlassArts {
    private GlassArts() {}

    private static final int COLOR = GlassBook.COLOR;
    private static final int MAX_CUTS = 8;
    private static final String CUTS = "nusmp_glass_cuts", CUTS_UNTIL = "nusmp_glass_cuts_until", DOT_UNTIL = "nusmp_glass_dot_until",
            PRISM = "nusmp_glass_prism";
    /** True while a glass spell is dealing its own damage (the Verre Epee bonus must not feed on bleed ticks). */
    static boolean busy;
    private static boolean hooked;

    /** Registers the reflect hook of the prism barrier. Called once from GlassProps.init(). */
    public static synchronized void hooks() {
        if (hooked) return;
        hooked = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            if (victim.getPersistentData().getLong(PRISM) <= victim.level().getGameTime()) return;
            var src = e.getSource();
            if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(DamageTypes.THORNS)) return;
            float before = e.getAmount();
            e.setAmount(before * 0.6f);
            if (src.getDirectEntity() instanceof LivingEntity att && att != victim && !att.isAlliedTo(victim)) {
                att.hurt(victim.damageSources().thorns(victim), Math.min(20f, before * 0.35f));
                att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
        });
    }

    // ------------------------------------------------------------------ small helpers
    private static float k(GrimoireBook b, ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** Balance-law damage from a glass spell (flagged so the blade bonus ignores it). */
    static void hit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, LivingEntity t, float raw) {
        busy = true;
        try { b.hurt(i, p, t, mode, raw); } finally { busy = false; }
    }

    /** The live cut stacks on a target. */
    static int stacks(LivingEntity t) {
        var d = t.getPersistentData();
        return t.level().getGameTime() > d.getLong(CUTS_UNTIL) ? 0 : d.getInt(CUTS);
    }

    /**
     * Glass cuts: 'add' stacks of bleed on the target for 8 s. Each stack bleeds a second of damage every second (one bleed timer per
     * target), magicules leak out of the wound, and a deep wound (4+ cuts) makes the target fragile. Returns the stacks now.
     */
    static int cut(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, LivingEntity t, int add) {
        if (t == p || t.isAlliedTo(p)) return 0;
        var d = t.getPersistentData();
        long now = t.level().getGameTime();
        int n = Math.min(MAX_CUTS, stacks(t) + add);
        d.putInt(CUTS, n);
        d.putLong(CUTS_UNTIL, now + 160);
        Nullification.bleed(t, 0.008 * add);
        if (n >= 4) EnergyBridge.effect(t, "fragility", 60, 0);
        b.vfx(p, VfxShape.GLASS_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 14, 0.35f);
        if (now >= d.getLong(DOT_UNTIL)) {
            d.putLong(DOT_UNTIL, now + 200);
            SpellRuntime.zone(p.serverLevel(), 200, 20, age -> {
                if (!t.isAlive() || !p.isAlive()) return;
                int s = stacks(t);
                if (s > 0) hit(b, i, p, mode, t, 0.8f * s);
            });
        }
        return n;
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = v.multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    // ------------------------------------------------------------------ Shard Volley
    static boolean shardVolley(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        b.castCircle(p, 0.8f);
        for (int n = 0; n < 5; n++) {
            double off = (n - 2) * 0.1;
            Vec3 d = dir.add(side.scale(off)).add(0, (2 - Math.abs(n - 2)) * 0.02, 0).normalize();
            b.vfx(p, VfxShape.GLASS_FX1, start, start.add(d.scale(24)), 14, 0.6f);
            SpellRuntime.bolt(p, start, d.scale(1.7), 0.4 * s, 16, false, null, (bolt, t) -> {
                hit(b, i, p, mode, t, 3.5f);
                cut(b, i, p, mode, t, 1);
            }, (bolt, at) -> b.vfx(p, VfxShape.GLASS_FX3, at, at, 14, 0.4f));
        }
        return true;
    }

    // ------------------------------------------------------------------ Glass Lance
    static boolean glassLance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.GLASS_FX1, start, start.add(dir.scale(30)), 14, 1.3f);
        SpellRuntime.bolt(p, start, dir.scale(2.2), 0.6 * s, 14, true, null, (bolt, t) -> {
            hit(b, i, p, mode, t, 10f);
            cut(b, i, p, mode, t, 2);
            EnergyBridge.effect(t, "fragility", 80, 0);
        }, (bolt, at) -> b.vfx(p, VfxShape.GLASS_FX3, at, at, 20, 1.0f));
        return true;
    }

    // ------------------------------------------------------------------ Verre Epee: a glass blade, melee hits cut deep
    static boolean verreEpee(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        i.getOrCreateTag().putLong("GlassBladeUntil", p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GLASS_FX2, p, p.position(), COLOR, ticks, 1.5f);
        b.vfx(p, VfxShape.GLASS_FX3, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)).add(0, 1, 0), 28, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Pane Step: dash through a line of panes
    static boolean paneStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dir = flat(p.getViewVector(1f));
        Vec3 dest = null;
        for (double d = 9 * GrimoireBook.size(i, p); d >= 1; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to step."); return false; }
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.3)) {
            hit(b, i, p, mode, t, 5f);
            cut(b, i, p, mode, t, 1);
        }
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.GLASS_FX1, from.add(0, 1, 0), dest.add(0, 1, 0), 12, 1.1f);
        b.vfx(p, VfxShape.GLASS_FX3, from.add(0, 1, 0), from.add(0, 2, 0), 16, 0.7f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        b.vfx(p, VfxShape.GLASS_FX3, dest.add(0, 1, 0), dest.add(0, 2, 0), 16, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Verre Detection: panes spread around, light reflects between them
    static boolean verreDetection(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 24 * k(b, i, p);
        List<LivingEntity> found = GrimoireBook.around(p, p.position(), r);
        int n = 0;
        for (LivingEntity t : found) {
            if (n++ >= 40) break;
            t.removeEffect(MobEffects.INVISIBILITY);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
            b.vfx(p, VfxShape.GLASS_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 20, 0.5f);
        }
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.GLASS_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            Vec3 pane = p.position().add(Math.cos(a) * 5, 1.5, Math.sin(a) * 5);
            b.vfx(p, VfxShape.GLASS_FX1, pane, p.position().add(Math.cos(a) * r, 1.5, Math.sin(a) * r), 18, 0.5f);
        }
        p.displayClientMessage(Component.literal(found.isEmpty() ? "Verre Detection: no presence within " + (int) r + " blocks."
                : "Verre Detection: " + found.size() + " presence" + (found.size() == 1 ? "" : "s") + " revealed."), true);
        return true;
    }

    // ------------------------------------------------------------------ Prism Barrier: a reflective shield
    static boolean prismBarrier(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 160;
        p.getPersistentData().putLong(PRISM, p.level().getGameTime() + ticks);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GLASS_FX2, p, p.position(), COLOR, ticks, 2.4f);
        b.vfx(p, VfxShape.GLASS_FX3, p.position().add(0, 1, 0), p.position().add(0, 1.5, 0), 24, 1.0f);
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> {
            if (!p.isAlive() || p.level().getGameTime() >= p.getPersistentData().getLong(PRISM)) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(2.8))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.2));
                pr.setOwner(p);
                pr.hurtMarked = true;
                b.vfx(p, VfxShape.GLASS_FX3, pr.position(), pr.position(), 12, 0.5f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Glass Shatter: every cut pops
    static boolean shatter(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 aim = GrimoireBook.aim(p, 20);
        Vec3 c = aim.distanceTo(p.getEyePosition()) > 19.5 ? p.position() : aim;
        double r = 8 * s;
        List<LivingEntity> targets = GrimoireBook.around(p, c, r);
        int broken = Nullification.shatterBarriers(p.serverLevel(), c, Math.min(r, 8), p);
        if (targets.isEmpty() && broken == 0) { GrimoireBook.fail(p, "Nothing to shatter."); return false; }
        for (LivingEntity t : targets) {
            int n = stacks(t);
            hit(b, i, p, mode, t, 3f + 4f * n);
            if (n > 0) {
                var d = t.getPersistentData();
                d.putInt(CUTS, 0);
                d.putLong(CUTS_UNTIL, 0L);
                b.vfx(p, VfxShape.GLASS_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 22, 0.5f + 0.1f * n);
            }
        }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.GLASS_FX3, c.add(0, 1, 0), c.add(0, 2, 0), 26, 1.2f);
        b.vfx(p, VfxShape.GLASS_FX2, c, c.add(0, 1, 0), 24, (float) r);
        return true;
    }

    // ------------------------------------------------------------------ Shard Rain
    static boolean shardRain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 c = GrimoireBook.aim(p, 22);
        double r = 5.5 * s;
        int ticks = 100;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.GLASS_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            var rnd = p.getRandom();
            List<LivingEntity> in = GrimoireBook.around(p, c, r);
            for (int n = 0; n < Math.min(3, in.size()); n++) {
                LivingEntity t = in.get(rnd.nextInt(in.size()));
                Vec3 at = t.getBoundingBox().getCenter();
                b.vfx(p, VfxShape.GLASS_FX1, at.add(0, 8, 0), at, 8, 0.5f);
                hit(b, i, p, mode, t, 2.8f);
                cut(b, i, p, mode, t, 1);
            }
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
            Vec3 g = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            b.vfx(p, VfxShape.GLASS_FX1, g.add(0, 9, 0), g, 8, 0.5f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Verre Fleur: a flower of glass spikes rises from the ground
    static boolean verreFleur(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 aim = GrimoireBook.aim(p, 22);
        Vec3 c = aim.distanceTo(p.getEyePosition()) > 21.5 ? p.position().add(flat(p.getViewVector(1f)).scale(6)) : aim;
        double r = 5 * s;
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.GLASS_FX2, c, c.add(0, 1, 0), 40, (float) r);
        bloom(b, i, p, mode, c, r * 0.45, 6f, 2);
        SpellRuntime.later(p.serverLevel(), 8, () -> bloom(b, i, p, mode, c, r * 0.8, 7f, 2));
        SpellRuntime.later(p.serverLevel(), 16, () -> bloom(b, i, p, mode, c, r, 8f, 3));
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            b.vfx(p, VfxShape.GLASS_FX3, c.add(Math.cos(a) * r * 0.6, 0, Math.sin(a) * r * 0.6), c.add(Math.cos(a) * r * 0.6, 2, Math.sin(a) * r * 0.6), 26, 0.8f);
        }
        return true;
    }

    private static void bloom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 c, double r, float dmg, int cuts) {
        if (!p.isAlive()) return;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            hit(b, i, p, mode, t, dmg);
            cut(b, i, p, mode, t, cuts);
            t.setDeltaMovement(t.getDeltaMovement().add(0, BalanceLaw.isBoss(t) ? 0.1 : 0.5, 0));
            t.hurtMarked = true;
        }
        b.vfx(p, VfxShape.GLASS_FX3, c.add(0, 0.5, 0), c.add(0, 2.5, 0), 22, (float) Math.max(0.6, r / 4));
    }

    // ------------------------------------------------------------------ Le Chateau de Verre
    /** A castle of glass rises around you: a ring of panes with a gate in front, glass flowers that cut whoever is inside, shelter for friends. */
    static boolean chateau(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        Vec3 c = p.position();
        double r = 7 * s;
        int ticks = 200;
        Vec3 gate = flat(p.getViewVector(1f));
        var level = p.serverLevel();
        int steps = (int) Math.ceil(r * Math.PI * 2 * 1.4);
        for (int n = 0; n < steps; n++) {
            double a = n * Math.PI * 2 / steps;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            if (dir.dot(gate) > 0.93) continue;
            BlockPos base = BlockPos.containing(c.x + dir.x * r, c.y, c.z + dir.z * r);
            if (!level.isLoaded(base)) continue;
            for (int h = 0; h < 3; h++)
                SpellRuntime.tempBlock(level, base.above(h), Blocks.GLASS.defaultBlockState(), ticks);
        }
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.GLASS_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        b.vfx(p, VfxShape.GLASS_FX3, c.add(0, 0.5, 0), c.add(0, 3, 0), 36, 1.8f);
        SpellRuntime.zone(level, ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                hit(b, i, p, mode, t, 4f);
                cut(b, i, p, mode, t, 1);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0));
            }
            for (LivingEntity f : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                    x -> x.isAlive() && !x.isSpectator() && (x == p || x.isAlliedTo(p)) && x.distanceToSqr(c) <= r * r)) {
                f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0));
                f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0));
            }
            if (age % 20 == 0) {
                double a = age * 0.15;
                for (int k = 0; k < 3; k++) {
                    double ka = a + k * Math.PI * 2 / 3;
                    Vec3 at = c.add(Math.cos(ka) * r * 0.6, 1.5, Math.sin(ka) * r * 0.6);
                    b.vfx(p, VfxShape.GLASS_FX3, at, at.add(0, 1, 0), 20, 0.7f);
                }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Crystal Carapace (daily)
    static boolean carapace(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(b, i, p);
        long now = p.level().getGameTime();
        BalanceLaw.heal(p, p.getMaxHealth() * 0.3f);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
        p.getPersistentData().putLong(PRISM, now + 200);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 5 * s)) {
            hit(b, i, p, mode, t, 8f);
            cut(b, i, p, mode, t, 3);
            Vec3 out = flat(t.position().subtract(p.position()));
            t.setDeltaMovement(t.getDeltaMovement().add(out.scale(BalanceLaw.isBoss(t) ? 0.3 : 0.9)).add(0, 0.25, 0));
            t.hurtMarked = true;
        }
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.GLASS_FX2, p, p.position(), COLOR, 200, 2.6f);
        b.vfx(p, VfxShape.GLASS_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 32, 1.5f);
        return true;
    }
}
