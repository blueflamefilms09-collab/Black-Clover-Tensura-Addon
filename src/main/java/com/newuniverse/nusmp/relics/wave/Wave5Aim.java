package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aiming for wave 5's rays, cones and bursts. Targets always come from {@link Fx#foesAround} (hostile or unallied, never creative or spectator
 * players, capped at 24), so every spell here is bounded and shares the core's idea of a foe.
 */
final class Wave5Aim {
    private Wave5Aim() {}

    /** The foe the wearer is looking at: nearest along the line of sight, within the width of its body and a little more. */
    static LivingEntity pickFoe(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestAlong = Double.MAX_VALUE;
        for (LivingEntity e : Fx.foesAround(p, range)) {
            if (e == p || !e.isAlive()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along <= 0.0 || along > range) continue;
            double perp = Math.sqrt(Math.max(0.0, to.lengthSqr() - along * along));
            if (perp > e.getBbWidth() * 0.5 + 0.9) continue;
            if (along < bestAlong && p.hasLineOfSight(e)) {
                best = e;
                bestAlong = along;
            }
        }
        return best;
    }

    /** Foes inside a cone in front of the wearer (minDot is the cosine of the half angle), nearest first, at most {@code cap}. */
    static List<LivingEntity> cone(ServerPlayer p, double range, double minDot, int cap) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : Fx.foesAround(p, range)) {
            if (e == p || !e.isAlive()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.01 || d > range) continue;
            if (to.scale(1.0 / d).dot(look) < minDot) continue;
            if (!p.hasLineOfSight(e)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
    }

    /** Foes within {@code radius} of a point (the burst of a fireball, the splash of acid). */
    static List<LivingEntity> foesNear(ServerPlayer p, Vec3 at, double radius) {
        double reach = Math.min(40.0, p.position().distanceTo(at) + radius + 1.0);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : Fx.foesAround(p, reach)) {
            if (e == p || !e.isAlive()) continue;
            if (e.getBoundingBox().getCenter().distanceTo(at) <= radius + e.getBbWidth() * 0.5) out.add(e);
        }
        return out;
    }

    /** Where a spell lands: the foe in the sights, else the first block along the look, else the end of the range. */
    static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity f = pickFoe(p, range);
        if (f != null) return f.getBoundingBox().getCenter();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult r = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? end : r.getLocation();
    }

    /** A line of particles (at most {@code points} of them) from one point to another. */
    static void beam(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions fx, int points) {
        int n = Math.max(2, Math.min(points, 32));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            Vec3 at = from.add(to.subtract(from).scale(t));
            l.sendParticles(fx, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** The start of a ray: just in front of the wearer's chest. */
    static Vec3 muzzle(ServerPlayer p) { return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.25, 0); }

    static void puff(ServerLevel l, Vec3 at, ParticleOptions fx, int count, double spread) {
        l.sendParticles(fx, at.x, at.y, at.z, Math.max(1, Math.min(count, 40)), spread, spread, spread, 0.02);
    }

    static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    /** One line on the action bar. */
    static void msg(ServerPlayer p, String text, ChatFormatting colour) {
        p.displayClientMessage(Component.literal(text).withStyle(colour), true);
    }
}
