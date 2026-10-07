package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Barrier Magic: the custom spell logic (voodoo regalia, domes, deflection, the barrier cannon). */
final class BarrierArts {
    private BarrierArts() {}

    private static final VfxShape FX1 = VfxShape.BARRIER_FX1, FX2 = VfxShape.BARRIER_FX2, FX3 = VfxShape.BARRIER_FX3;

    private static boolean ally(ServerPlayer p, LivingEntity e) {
        return e == p || e.isAlliedTo(p);
    }

    private static boolean skip(LivingEntity e) {
        return !e.isAlive() || e.isSpectator() || (e instanceof Player pl && pl.isCreative());
    }

    // ------------------------------------------------------------------ Voodoo Regalia
    /** Four barrier weapons (sword, axe, arrow, trident) fly in a fan; the rest of the barrier stays in the hand for a few seconds. */
    static boolean regalia(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        LivingEntity mark = GrimoireBook.target(p, 24);
        b.castCircle(p, 1f);
        for (int k = 0; k < 4; k++) {
            final int kind = k;
            Vec3 d = dir.add(side.scale((k - 1.5) * 0.12)).normalize();
            double speed = kind == 2 ? 2.4 : kind == 1 ? 1.3 : 1.7;
            b.vfx(p, FX1, start, start.add(d.scale(speed * 20)), 16, (kind == 1 ? 1.2f : 0.9f) * s);
            SpellRuntime.bolt(p, start, d.scale(speed), 0.5 * s, 22, kind == 0 || kind == 2, kind == 3 ? mark : null, (bolt, t) -> {
                switch (kind) {
                    case 0 -> { b.hurt(i, p, t, mode, 6f); Nullification.bleed(t, 0.04); }                                  // sword: cuts the magicule pool
                    case 1 -> { b.hurt(i, p, t, mode, 9f); EnergyBridge.effect(t, "fragility", 80, 0); t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0)); }   // axe: breaks guard
                    case 2 -> b.hurt(i, p, t, mode, 5f);                                                                      // arrow: fast, pierces
                    default -> {                                                                                              // trident: heaves them up
                        b.hurt(i, p, t, mode, 7f);
                        t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
                        t.hurtMarked = true;
                    }
                }
            }, (bolt, at) -> b.vfx(p, FX3, at, at, 16, 0.7f));
        }
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + 140);
        tag.putFloat("EmpowerBonus", 3f);
        return true;
    }

    // ------------------------------------------------------------------ Deflecting Pane
    /** For 8 s a barrier turns every hostile projectile and spell bolt that comes within 4.5 blocks back on its sender. */
    static boolean deflect(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel lv = p.serverLevel();
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(lv, FX2, p, p.position(), b.color, 160, 4.5f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        SpellRuntime.zone(lv, 160, 2, age -> {
            if (p.isRemoved() || !p.isAlive()) return;
            Vec3 at = p.position().add(0, 1, 0);
            int n = 0;
            for (Projectile pr : lv.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(4.5))) {
                if (n++ >= 24) break;
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && ally(p, o))) continue;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.1));
                pr.setOwner(p);
                pr.hurtMarked = true;
                b.vfx(p, FX3, pr.position(), pr.position(), 10, 0.6f);
            }
            if (SpellRuntime.dissolveBolts(lv, at, 4.5, p) > 0) b.vfx(p, FX3, at, at, 12, 0.9f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Hexagon Wall
    /** A pane wall of seven by four tessellated barrier cells, 12 s. */
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        Vec3 base = p.position().add(dir.scale(2.5));
        ServerLevel lv = p.serverLevel();
        int placed = 0;
        for (int w = -3; w <= 3; w++) for (int h = 0; h < 4; h++) {
            BlockPos pos = BlockPos.containing(base.add(side.scale(w)).add(0, h, 0));
            if (lv.isLoaded(pos) && SpellRuntime.tempBlock(lv, pos, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 240)) placed++;
        }
        if (placed == 0) { GrimoireBook.fail(p, "No room for a wall."); return false; }
        b.castCircle(p, 0.9f);
        b.vfx(p, FX2, base, base, 80, 3.5f);
        b.vfx(p, FX3, base.add(side.scale(-3)).add(0, 1.5, 0), base.add(side.scale(3)).add(0, 1.5, 0), 20, 1f);
        return true;
    }

    // ------------------------------------------------------------------ Barrier Cannon
    /** A cannon of barrier plates fires a beam: damage, knock back, magic jamming, and it shatters hostile barriers where it ends. */
    static boolean cannon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f);
        double len = 26 * GrimoireBook.size(i, p);
        Vec3 end = a.add(dir.scale(len));
        var wall = p.level().clip(new net.minecraft.world.level.ClipContext(a, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) end = wall.getLocation();
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.1)) {
            if (skip(t)) continue;
            b.hurt(i, p, t, mode, 14f);
            Vec3 push = t.position().subtract(p.position()).normalize();
            t.knockback(BalanceLaw.isBoss(t) ? 0.3 : 1.3, -push.x, -push.z);
            EnergyBridge.effect(t, "silence", BalanceLaw.isBoss(t) ? 20 : 80, 0);
        }
        Nullification.shatterBarriers(p.serverLevel(), end, 3.5, p);
        b.castCircle(p, 0.9f);
        b.vfx(p, FX1, a, end, 18, 1.4f);
        b.vfx(p, FX3, end, end, 24, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Spatial Lock
    /** The barrier is tuned against Spatial Magic: foes around you cannot cast, are anchored in place and lose their ultimate skills for a moment. */
    static boolean lock(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 7 * GrimoireBook.size(i, p);
        List<LivingEntity> ts = GrimoireBook.around(p, p.position(), r);
        if (ts.isEmpty()) { GrimoireBook.fail(p, "No one to lock down."); return false; }
        for (LivingEntity t : ts) {
            if (skip(t)) continue;
            boolean boss = BalanceLaw.isBoss(t);
            b.hurt(i, p, t, mode, 3f);
            EnergyBridge.effect(t, "silence", boss ? 40 : 160, 0);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? 20 : 100, 2));
            Nullification.interfere(t, boss ? 3 : 8, 0.7f, 0.5f);
        }
        b.castCircle(p, 1.2f);
        b.vfx(p, FX2, p.position(), p.position(), 60, (float) r);
        b.vfx(p, FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 24, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Barrier Armour
    /** Barrier material wraps you like a bandage: wounds close, poison and fire are sealed out, and a shell of absorption forms; allies near you get a thinner coat. */
    static boolean armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        BalanceLaw.heal(p, 6f);
        p.clearFire();
        p.removeEffect(MobEffects.POISON);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
        for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(6))) {
            if (a != p && a.isAlliedTo(p)) a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0));
        }
        b.castCircle(p, 1f);
        b.vfx(p, FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 30, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Barrier Reclaim
    /** The barriers you have made are drawn back into your body: cleanses, heals and powers you up for 30 s. */
    static boolean reclaim(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        for (var m : new java.util.ArrayList<>(p.getActiveEffects())) if (!m.getEffect().value().isBeneficial()) p.removeEffect(m.getEffect());
        BalanceLaw.heal(p, p.getMaxHealth() * 0.25f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        b.castCircle(p, 1.4f);
        b.vfx(p, FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 36, 1.6f);
        return true;
    }

    // ------------------------------------------------------------------ Voodoo Shangrila / Kingdom
    /** The hex dome: foes inside stay in, foes outside stay out, hostile shots break on it, allies pass freely and are guarded. */
    static boolean shangrila(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return dome(b, i, p, mode, 7, 240, false);
    }

    /** The grand dome: the same law over a wider ground, with barrier weapons falling on the foes locked inside. */
    static boolean kingdom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return dome(b, i, p, mode, 11, 300, true);
    }

    private static boolean dome(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, double base, int ticks, boolean kingdom) {
        ServerLevel lv = p.serverLevel();
        double r = base * Math.min(1.3f, GrimoireBook.size(i, p));
        Vec3 c = p.position();
        Set<UUID> locked = new HashSet<>();
        for (LivingEntity t : GrimoireBook.around(p, c, r)) if (!skip(t)) locked.add(t.getUUID());
        b.castCircle(p, kingdom ? 1.8f : 1.4f);
        b.vfx(p, FX2, c, c, ticks, (float) r);
        b.vfx(p, FX3, c.add(0, 1, 0), c.add(0, 2, 0), 30, (float) r / 4f);
        SpellRuntime.zone(lv, ticks, 2, age -> {
            if (p.isRemoved()) return;
            AABB box = new AABB(c, c).inflate(r + 4, r + 2, r + 4);
            int n = 0;
            for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, box)) {
                if (skip(e) || n++ >= 48) continue;
                double dx = e.getX() - c.x, dz = e.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz);
                boolean inside = d <= r && e.getY() - c.y <= r;
                if (ally(p, e)) {
                    if (inside && age % 20 == 0) {
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, kingdom ? 1 : 0));
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0));
                        if (kingdom) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 0));
                    }
                    continue;
                }
                if (inside) locked.add(e.getUUID());
                double gain = BalanceLaw.isBoss(e) ? 0.25 : 0.7;
                Vec3 out = d < 1e-3 ? Vec3.ZERO : new Vec3(dx / d, 0, dz / d);
                if (locked.contains(e.getUUID())) {
                    if (inside && age % 20 == 0) {
                        EnergyBridge.effect(e, "silence", 50, 0);
                        if (kingdom) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
                    }
                    if (d > r - 1.0) {                                                      // the wall: back inside
                        e.setDeltaMovement(out.scale(-gain).add(0, 0.08, 0));
                        e.hurtMarked = true;
                        if (d > r + 0.5) e.teleportTo(c.x + out.x * (r - 1.5), e.getY(), c.z + out.z * (r - 1.5));
                    }
                } else if (d < r + 1.5) {                                                   // not part of the prisoners: out
                    e.setDeltaMovement(out.scale(gain).add(0, 0.1, 0));
                    e.hurtMarked = true;
                }
            }
            for (Projectile pr : lv.getEntitiesOfClass(Projectile.class, box)) {            // shots break on the shell
                if (pr.getOwner() instanceof LivingEntity o && ally(p, o)) continue;
                double dx = pr.getX() - c.x, dz = pr.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(d - r) < 1.5) { b.vfx(p, FX3, pr.position(), pr.position(), 10, 0.5f); pr.discard(); }
            }
            if (kingdom && age % 20 == 0) {                                                 // barrier weapons fall on the prisoners
                int hit = 0;
                for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                    if (skip(t) || hit++ >= 3) break;
                    Vec3 top = new Vec3(t.getX(), c.y + Math.min(r, 8), t.getZ());
                    b.hurt(i, p, t, mode, 6f);
                    b.vfx(p, FX1, top, t.getBoundingBox().getCenter(), 10, 0.9f);
                    b.vfx(p, FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 14, 0.7f);
                }
            }
        });
        return true;
    }
}
