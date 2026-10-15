package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave 1: targeting and particle helpers for the hand-held relics. Foes always come from {@link Fx#foesAround}, which already
 * leaves out allies, creative and spectator players and caps the list, so no ability here can pick a bystander.
 */
final class Wave1Aim {
    private Wave1Aim() {}

    /** The foe nearest the crosshair within {@code range} blocks (inside a narrow cone, in line of sight), or null. */
    static LivingEntity aimedFoe(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestScore = 0.0;
        for (LivingEntity e : Fx.foesAround(p, range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.05 || d > range) continue;
            double cos = to.dot(look) / d;
            if (cos < 0.93 || !p.hasLineOfSight(e)) continue;
            double score = cos - d / (range * 20.0);
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** A player or tamed pet near the crosshair (within {@code range}), or null: who a willing-target spell is meant for. */
    static LivingEntity aimedFriend(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestCos = 0.9;
        int seen = 0;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range))) {
            if (e == p || !e.isAlive() || ++seen > 40) continue;
            boolean friend = e instanceof Player || (e instanceof OwnableEntity o && o.getOwner() == p);
            if (!friend) continue;
            if (e instanceof ServerPlayer sp && sp.isSpectator()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.05 || d > range) continue;
            double cos = to.dot(look) / d;
            if (cos > bestCos) {
                bestCos = cos;
                best = e;
            }
        }
        return best;
    }

    /** Where the crosshair rests: the first block hit within {@code range}, or the point {@code range} blocks ahead. */
    static Vec3 point(ServerPlayer p, double range) {
        HitResult hr = p.pick(range, 1.0F, false);
        return hr.getLocation();
    }

    /** Foes within {@code r} blocks of a point (the list {@link Fx#foesAround} gives, narrowed to the point). */
    static List<LivingEntity> foesAt(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        double reach = p.position().distanceTo(c) + r;
        for (LivingEntity e : Fx.foesAround(p, Math.min(reach, 64.0))) {
            if (e.position().distanceToSqr(c) <= r * r || e.getBoundingBox().getCenter().distanceToSqr(c) <= r * r) out.add(e);
        }
        return out;
    }

    /** Distance from {@code q} to the segment a-b. */
    static double distToSegment(Vec3 a, Vec3 b, Vec3 q) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0e-9) return q.distanceTo(a);
        double t = Math.max(0.0, Math.min(1.0, q.subtract(a).dot(ab) / len2));
        return q.distanceTo(a.add(ab.scale(t)));
    }

    /** Radiant light hurts the undead more. */
    static boolean undead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    static float radiant(LivingEntity e, float base) {
        return undead(e) ? base * 1.5f : base;
    }

    // ---------------------------------------------------------------- particles (bounded: a few dozen per call)
    static void beam(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions fx, int steps) {
        int n = Math.max(2, Math.min(steps, 48));
        for (int i = 0; i <= n; i++) {
            Vec3 q = from.lerp(to, i / (double) n);
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(fx), q.x, q.y, q.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    static void column(ServerLevel l, Vec3 base, double height, ParticleOptions fx) {
        int n = (int) Math.max(2, Math.min(24, height * 3));
        for (int i = 0; i < n; i++) com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(fx), base.x, base.y + height * i / n, base.z, 1, 0.12, 0.0, 0.12, 0.0);
    }
}
