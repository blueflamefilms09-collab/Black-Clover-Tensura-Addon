package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.MonarchFight.Hazard;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Blood-Red Commander's three fast weapon arts, as things set going in the world: the Crimson Cross, the Blood-Red Lunge and the
 * Executioner's Arc (docs/living_dungeon.md section 8). They are the bread and butter of the fight: quick (a wind-up of 12 to 20 ticks), huge,
 * and every one is drawn in red on the floor from the first tick of its wind-up, so the whole of it can be read before it lands. Each has a
 * counter: step off the lines of the cross (or jump them), leave the lane of the lunge, jump the arc or get outside its radius. Each is a
 * {@link Hazard} like the Commander's other moves, and damage goes through {@link MonarchAttacks#strike} (a bounded share of the victim's
 * health, nobody struck twice inside the grace). The numbers are in {@link MonarchRules}, the geometry in {@link MonarchShapes} and
 * {@link MonarchLunge}; this class is only the glue to the world. All of it runs on the server thread and keeps no player or entity beyond the
 * few ticks of one casting.
 */
final class MonarchArts {
    private MonarchArts() {}

    /** The blood of the Commander as an ARGB colour for the mod's VFX. */
    private static final int BLOOD = 0xFFE0182C;
    /** The mark on the floor: dark red while there is time, hot red in the last 8 ticks, white in the last 4. */
    private static final DustParticleOptions MARK = new DustParticleOptions(new Vector3f(0.85f, 0.04f, 0.08f), 1.3f),
            HOT = new DustParticleOptions(new Vector3f(1.0f, 0.32f, 0.22f), 1.4f), WHITE = new DustParticleOptions(new Vector3f(1.0f, 0.92f, 0.92f), 1.3f);
    /** How far apart the dots of a mark are, and the most in one row of one line: a long lane costs no more than a short one. */
    private static final double ROW_SPACING = 1.8;
    private static final int ROW_MAX = 16;

    private static DustParticleOptions tint(long left) { return left <= 4 ? WHITE : left <= 8 ? HOT : MARK; }

    private static void dots(ServerLevel l, DustParticleOptions dust, List<double[]> points, double y) {
        for (double[] q : points) l.sendParticles(dust, q[0], y, q[1], 1, 0, 0, 0, 0);
    }

    /** Pushes along (ux, uz), which need not be a unit vector, the way a blow shoves; no damage. */
    private static void push(ServerPlayer p, double ux, double uz, double power, double lift) {
        double d = Math.max(0.1, Math.hypot(ux, uz));
        p.push(ux / d * power, lift, uz / d * power);
        p.hurtMarked = true;
    }

    // ---------------------------------------------------------------- Crimson Cross
    /**
     * Two long red lines crossing in an X, drawn on the floor under the target (not under the Commander) for 18 ticks, then cut. In the second
     * phase a second cross, turned 45 degrees, begins 20 ticks after the first; in the third there are three in a row. Each cross is centred on
     * where the target stands when ITS mark begins, so standing still is punished and stepping to the wedge between the lines (or jumping the
     * cut) is not.
     */
    static Hazard cross(ServerLevel level, Mob boss, MonarchFight f, ServerPlayer target, long now) {
        return new Cross(level, boss, f, target, now);
    }

    private static final class Cross implements Hazard {
        final int n;
        final UUID who;
        final double heading;
        final long start, end;
        final double[] cx, cz, cy, ang;
        final boolean[] begun, done;
        double seenX, seenY, seenZ;

        Cross(ServerLevel l, Mob boss, MonarchFight f, ServerPlayer target, long now) {
            n = MonarchRules.crosses(f.phase);
            who = target.getUUID();
            seenX = target.getX();
            seenY = target.getY();
            seenZ = target.getZ();
            heading = MonarchShapes.angle(boss.getX(), boss.getZ(), seenX, seenZ);
            start = now;
            end = now + MonarchRules.activeUntil(MonarchMove.CROSS_SLASH, f.phase);
            cx = new double[n];
            cz = new double[n];
            cy = new double[n];
            ang = new double[n];
            begun = new boolean[n];
            done = new boolean[n];
            begin(l, 0);
        }

        /** The mark of cross k begins: on the target where it stands now (else where it was last seen). */
        private void begin(ServerLevel l, int k) {
            if (l.getPlayerByUUID(who) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() && !p.isCreative()) {
                seenX = p.getX();
                seenY = p.getY();
                seenZ = p.getZ();
            }
            cx[k] = seenX;
            cz[k] = seenZ;
            cy[k] = MonarchAttacks.groundY(l, seenX, seenY, seenZ);
            ang[k] = heading + Math.PI / 4 + k * MonarchRules.CROSS_TURN;
            begun[k] = true;
            MonarchAttacks.sound(l, cx[k], cy[k], cz[k], SoundEvents.WITHER_SHOOT, 2.0f, 1.5f);
            mark(l, k, MonarchRules.CROSS_WINDUP);
            for (double[] ln : MonarchShapes.crossLines(cx[k], cz[k], ang[k], MonarchRules.CROSS_HALF_LEN))
                MonarchFx.chains(l, new Vec3(ln[0], cy[k] + 0.3, ln[1]), new Vec3(ln[2], cy[k] + 0.3, ln[3]), MonarchRules.CROSS_WINDUP, 0.7f);
        }

        private void mark(ServerLevel l, int k, long left) {
            DustParticleOptions dust = tint(left);
            for (double[] ln : MonarchShapes.crossLines(cx[k], cz[k], ang[k], MonarchRules.CROSS_HALF_LEN)) {
                dots(l, dust, MonarchShapes.strip(ln[0], ln[1], ln[2], ln[3], MonarchRules.CROSS_HALF_WIDTH, ROW_SPACING, ROW_MAX), cy[k] + 0.12);
            }
        }

        @Override
        public boolean tick(ServerLevel l, Mob b, MonarchFight fight, long t) {
            for (int k = 0; k < n; k++) {
                long begin = start + (long) k * MonarchRules.CROSS_GAP, cutAt = begin + MonarchRules.CROSS_WINDUP;
                if (!begun[k] && t >= begin) begin(l, k);
                if (!begun[k] || done[k]) continue;
                if (t < cutAt) {
                    if ((cutAt - t) % 4 == 0) mark(l, k, cutAt - t);
                } else {
                    cut(l, b, fight, k, t);
                    done[k] = true;
                }
            }
            return done[n - 1] || t > end;
        }

        /** The lines are cut: a flash along each, a burst at the middle, and anyone standing on a line (not in the air) is struck once. */
        private void cut(ServerLevel l, Mob b, MonarchFight fight, int k, long t) {
            final double x = cx[k], y = cy[k], z = cz[k];
            b.swing(InteractionHand.MAIN_HAND);
            MonarchAttacks.sound(l, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0f, 0.5f);
            MonarchAttacks.sound(l, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, 3.0f, 0.6f);
            for (double[] ln : MonarchShapes.crossLines(x, z, ang[k], MonarchRules.CROSS_HALF_LEN)) {
                VfxSpawn.send(l, VfxShape.BLOOD_FX1, new Vec3(ln[0], y + 0.7, ln[1]), new Vec3(ln[2], y + 0.7, ln[3]), BLOOD, 10, 1.2f);
                MonarchFx.cut(l, new Vec3(ln[0], y + 0.5, ln[1]), new Vec3(ln[2], y + 0.5, ln[3]), 1.0f, 12);
                for (int i = 0; i <= 6; i++) {
                    double u = i / 6.0;
                    l.sendParticles(ParticleTypes.SWEEP_ATTACK, ln[0] + (ln[2] - ln[0]) * u, y + 0.6, ln[1] + (ln[3] - ln[1]) * u, 1, 0, 0, 0, 0);
                }
            }
            l.sendParticles(ParticleTypes.CRIT, x, y + 0.5, z, 24, 0.6, 0.3, 0.6, 0.4);
            for (ServerPlayer p : MonarchAttacks.foesNear(l, x, z, MonarchRules.CROSS_HALF_LEN + 2.0)) {
                if (!MonarchShapes.crossHits(p.getX(), p.getZ(), x, z, ang[k], MonarchRules.CROSS_HALF_LEN, MonarchRules.CROSS_HALF_WIDTH + 0.3)) continue;
                if (!MonarchShapes.ringHurts(p.onGround(), p.getY(), y, MonarchRules.CROSS_REACH)) continue;
                if (MonarchAttacks.strike(l, b, fight, p, MonarchMove.CROSS_SLASH, t)) MonarchAttacks.shove(p, x, z, 0.35, 0.3);
            }
        }
    }

    // ---------------------------------------------------------------- Blood-Red Lunge
    /** The heights, from the one it stands at, at which the Commander may set its foot on the next sample of the lane: level, a step up, then drops of one to three. */
    private static final double[] STEPS = {0.0, 1.0, -1.0, -2.0, -3.0};

    /**
     * What the lunge asks of the world, for this body: a sample of the lane is good where a floor lies under it and the Commander's own box fits
     * (no wall, no low ceiling, no unloaded chunk). Nothing is ever placed or broken.
     */
    private static MonarchLunge.Ground groundFor(ServerLevel l, Mob b) {
        final AABB box = b.getBoundingBox().deflate(0.02);                          // a hair smaller, so a body standing flush on the floor is not taken for one inside it
        final double bx = b.getX(), by = b.getY(), bz = b.getZ();
        return (x, y, z) -> {
            if (!l.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4)) return Double.NaN;
            for (double step : STEPS) {
                double ny = y + step;
                if (!l.getBlockState(BlockPos.containing(x, ny - 0.5, z)).blocksMotion()) continue;
                if (l.noCollision(b, box.move(x - bx, ny - by, z - bz))) return ny;
            }
            return Double.NaN;
        };
    }

    /**
     * A red lane is marked from the Commander towards the target (as far past it as the cap and the ground allow) for 16 ticks; then it dashes
     * down the lane at two blocks a tick, hurting everything in the strip, and stops at its end, open to attack for the recovery. The lane is
     * planned once, before anything is drawn, as far as the ground carries it: it stops at the first wall or drop it would not take, and the
     * strip that is drawn is that lane, so the mark never lies. Returns null if there is no run to be had (a wall right in front of it): the
     * caller then chooses another art.
     */
    static Hazard lunge(ServerLevel level, Mob boss, MonarchFight f, ServerPlayer target, long now) {
        double dx = target.getX() - boss.getX(), dz = target.getZ() - boss.getZ(), d = Math.hypot(dx, dz);
        if (d < 0.5) {                                                              // right on top of it: along the way it is facing
            double yaw = Math.toRadians(boss.getYRot());
            dx = -Math.sin(yaw);
            dz = Math.cos(yaw);
            d = 1.0;
        }
        dx /= d;
        dz /= d;
        List<double[]> path = MonarchLunge.path(boss.getX(), boss.getY(), boss.getZ(), dx, dz, MonarchRules.lungeLength(d), groundFor(level, boss));
        if (MonarchLunge.length(path) < MonarchRules.LUNGE_MIN_RUN) return null;
        return new Lunge(level, path, dx, dz, now);
    }

    private static final class Lunge implements Hazard {
        final List<double[]> path;
        final double dx, dz, len;
        final long release;
        final Set<UUID> struck = new HashSet<>();
        double travelled;
        boolean loosed;
        double[] prev;

        Lunge(ServerLevel l, List<double[]> path, double dx, double dz, long now) {
            this.path = path;
            this.dx = dx;
            this.dz = dz;
            this.len = MonarchLunge.length(path);
            this.release = now + MonarchRules.LUNGE_WINDUP;
            this.prev = path.get(0);
            MonarchAttacks.sound(l, prev[0], prev[1], prev[2], SoundEvents.RAVAGER_ROAR, 3.0f, 0.8f);
            double[] stop = path.get(path.size() - 1);
            MonarchFx.chains(l, new Vec3(prev[0], prev[1] + 0.3, prev[2]), new Vec3(stop[0], stop[1] + 0.3, stop[2]), MonarchRules.LUNGE_WINDUP, 1.0f);
            MonarchFx.banners(l, stop[0], stop[1], stop[2], MonarchRules.LUNGE_HALF * 1.6, MonarchRules.LUNGE_WINDUP);
            mark(l, MonarchRules.LUNGE_WINDUP);
        }

        private void mark(ServerLevel l, long left) {
            DustParticleOptions dust = tint(left);
            for (double[] q : MonarchLunge.rows(path, dx, dz, MonarchRules.LUNGE_HALF, ROW_SPACING, ROW_MAX + 4)) l.sendParticles(dust, q[0], q[1] + 0.12, q[2], 1, 0, 0, 0, 0);
            double[] end = path.get(path.size() - 1);                              // a ring where it will stop
            MonarchAttacks.ring(l, dust, end[0], end[1] + 0.12, end[2], MonarchRules.LUNGE_HALF, 1.2, 0.0);
        }

        @Override
        public boolean tick(ServerLevel l, Mob b, MonarchFight fight, long t) {
            if (t < release) {
                if ((release - t) % 4 == 0) mark(l, release - t);
                return false;
            }
            if (!loosed) {
                loosed = true;
                b.swing(InteractionHand.MAIN_HAND);
                double[] a = path.get(0), z = path.get(path.size() - 1);
                MonarchAttacks.sound(l, a[0], a[1], a[2], SoundEvents.PLAYER_ATTACK_SWEEP, 4.0f, 0.5f);
                VfxSpawn.send(l, VfxShape.BLOOD_FX1, new Vec3(a[0], a[1] + 1.0, a[2]), new Vec3(z[0], z[1] + 1.0, z[2]), BLOOD, MonarchRules.lungeTicks(len) + 4, 1.6f);
                MonarchFx.cut(l, new Vec3(a[0], a[1] + 0.8, a[2]), new Vec3(z[0], z[1] + 0.8, z[2]), 1.5f, MonarchRules.lungeTicks(len) + 6);
            }
            travelled = Math.min(len, travelled + MonarchRules.LUNGE_SPEED);
            double[] to = MonarchLunge.at(path, travelled);
            b.moveTo(to[0], to[1], to[2], b.getYRot(), 0f);
            b.setDeltaMovement(Vec3.ZERO);
            strikeAlong(l, b, fight, to, t);
            l.sendParticles(HOT, to[0], to[1] + 0.4, to[2], 5, 0.8, 0.3, 0.8, 0.0);
            l.sendParticles(ParticleTypes.LARGE_SMOKE, to[0], to[1] + 0.3, to[2], 2, 0.6, 0.2, 0.6, 0.02);
            prev = to;
            if (travelled < len - 1e-6) return false;
            b.getNavigation().stop();                                              // the end of the lane: it stops and stands open to attack
            MonarchAttacks.sound(l, to[0], to[1], to[2], SoundEvents.WARDEN_ATTACK_IMPACT, 4.0f, 0.6f);
            l.sendParticles(ParticleTypes.EXPLOSION, to[0], to[1] + 0.4, to[2], 4, 1.2, 0.1, 1.2, 0.0);
            fight.frozenUntil = Math.min(fight.frozenUntil, t + MonarchRules.RECOVERY);
            return true;
        }

        /** Everyone in the strip between where it was and where it is now (so nobody is skipped however fast it goes) is struck once and thrown clear of the lane. */
        private void strikeAlong(ServerLevel l, Mob b, MonarchFight fight, double[] to, long t) {
            for (ServerPlayer p : MonarchAttacks.foesNear(l, to[0], to[2], 5.0 + MonarchRules.LUNGE_SPEED)) {
                if (struck.contains(p.getUUID()) || p.getY() < to[1] - 1.5 || p.getY() > to[1] + 3.5) continue;
                if (MonarchShapes.distToSegment(p.getX(), p.getZ(), prev[0], prev[2], to[0], to[2]) > MonarchRules.LUNGE_HALF + 0.3) continue;
                if (!MonarchAttacks.strike(l, b, fight, p, MonarchMove.LUNGE, t)) continue;
                struck.add(p.getUUID());
                double side = (p.getX() - to[0]) * -dz + (p.getZ() - to[2]) * dx;     // which side of the lane they are on
                double s = side >= 0 ? 1.0 : -1.0;
                push(p, -dz * s, dx * s, 0.9, 0.35);
            }
        }
    }

    // ---------------------------------------------------------------- Executioner's Arc
    /**
     * A huge half-circle (a fan of 200 degrees, radius 9 to 13 by phase) in front of the Commander, towards the target, is marked on the floor
     * for 20 ticks; then the blade sweeps across it from one edge to the other in five ticks. From the second phase on it sweeps back the
     * other way 14 ticks after the first cut began, the same mark flashing white to say so. A body is cut as the blade passes it, once per cut;
     * jumping the cut, or standing beyond the radius (or behind the Commander), is safe.
     */
    static Hazard sweep(ServerLevel level, Mob boss, MonarchFight f, ServerPlayer target, long now) {
        double heading = MonarchShapes.angle(boss.getX(), boss.getZ(), target.getX(), target.getZ());
        if (Math.hypot(target.getX() - boss.getX(), target.getZ() - boss.getZ()) < 0.5) heading = Math.toRadians(boss.getYRot() + 90.0);
        return new Sweep(level, boss, f, heading, now);
    }

    private static final class Sweep implements Hazard {
        final double cx, cy, cz, heading, radius;
        final int cuts;
        final long release, end;
        final List<Set<UUID>> struck = new ArrayList<>();
        boolean flashed;

        Sweep(ServerLevel l, Mob boss, MonarchFight f, double heading, long now) {
            this.cx = boss.getX();
            this.cy = boss.getY();
            this.cz = boss.getZ();
            this.heading = heading;
            this.radius = MonarchRules.sweepRadius(f.phase);
            this.cuts = MonarchRules.sweepCuts(f.phase);
            this.release = now + MonarchRules.SWEEP_WINDUP;
            this.end = now + MonarchRules.activeUntil(MonarchMove.SWEEP, f.phase);
            for (int k = 0; k < cuts; k++) struck.add(new HashSet<>());
            MonarchAttacks.sound(l, cx, cy, cz, SoundEvents.ENDER_DRAGON_FLAP, 3.0f, 0.7f);
            MonarchFx.banners(l, cx, cy, cz, radius, MonarchRules.SWEEP_WINDUP);
            mark(l, MonarchRules.SWEEP_WINDUP);
        }

        /** The rim of the fan, an inner arc and its two straight edges. At most about 80 dots. */
        private void mark(ServerLevel l, long left) {
            DustParticleOptions dust = tint(left);
            double half = MonarchRules.SWEEP_HALF_ANGLE, y = cy + 0.12;
            dots(l, dust, MonarchShapes.arc(cx, cz, heading, -half, half, radius, 1.4, 40), y);
            dots(l, dust, MonarchShapes.arc(cx, cz, heading, -half, half, radius * 0.55, 1.8, 24), y);
            for (int side = -1; side <= 1; side += 2) {
                double a = heading + side * half;
                MonarchAttacks.line(l, dust, cx + Math.cos(a) * 1.5, y, cz + Math.sin(a) * 1.5, cx + Math.cos(a) * radius, cz + Math.sin(a) * radius, 1.8);
            }
        }

        @Override
        public boolean tick(ServerLevel l, Mob b, MonarchFight fight, long t) {
            if (t < release) {
                if ((release - t) % 4 == 0) mark(l, release - t);
                return false;
            }
            for (int k = 0; k < cuts; k++) {
                long from = release + (long) k * MonarchRules.SWEEP_BACK, i = t - from;
                if (i < 0) {
                    if (k > 0 && !flashed && i >= -4) {                           // the reverse sweep is coming: the mark flashes white
                        flashed = true;
                        mark(l, 0);
                        MonarchAttacks.sound(l, cx, cy, cz, SoundEvents.ENDER_DRAGON_FLAP, 3.0f, 0.9f);
                    }
                    break;
                }
                if (i < MonarchRules.SWEEP_CUT) blade(l, b, fight, k, (int) i, t);
            }
            return t >= end;
        }

        /** One tick of a cut: the blade moves on, a slash is drawn along it, and whoever it passed over in this tick is struck. */
        private void blade(ServerLevel l, Mob b, MonarchFight fight, int k, int i, long t) {
            boolean reverse = k % 2 == 1;
            double half = MonarchRules.SWEEP_HALF_ANGLE;
            double a0 = MonarchShapes.bladeAt(i - 1, MonarchRules.SWEEP_CUT, half, reverse), a1 = MonarchShapes.bladeAt(i, MonarchRules.SWEEP_CUT, half, reverse);
            if (i == 0) {
                b.swing(InteractionHand.MAIN_HAND);
                MonarchAttacks.sound(l, cx, cy, cz, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0f, 0.5f);
                MonarchAttacks.sound(l, cx, cy, cz, SoundEvents.PLAYER_ATTACK_CRIT, 3.0f, 0.6f);
            }
            double ca = Math.cos(heading + a1), sa = Math.sin(heading + a1);
            for (double r = 2.0; r <= radius; r += 2.5) l.sendParticles(ParticleTypes.SWEEP_ATTACK, cx + ca * r, cy + 0.8, cz + sa * r, 1, 0, 0, 0, 0);
            MonarchAttacks.line(l, HOT, cx + ca * 1.5, cy + 0.6, cz + sa * 1.5, cx + ca * radius, cz + sa * radius, 1.5);
            if (i == 2) MonarchFx.cut(l, new Vec3(cx + ca * 1.5, cy + 0.7, cz + sa * 1.5), new Vec3(cx + ca * radius, cy + 0.7, cz + sa * radius), 1.3f, 12);
            if (i == MonarchRules.SWEEP_CUT - 1) VfxSpawn.send(l, VfxShape.BLOOD_FX3, new Vec3(cx + ca * radius, cy + 0.1, cz + sa * radius), new Vec3(cx + ca * radius, cy + 1.0, cz + sa * radius), BLOOD, 16, 0.9f);
            Set<UUID> seen = struck.get(k);
            for (ServerPlayer p : MonarchAttacks.foesNear(l, cx, cz, radius + 2.0)) {
                if (seen.contains(p.getUUID())) continue;
                if (!MonarchShapes.inSlice(p.getX(), p.getZ(), cx, cz, heading, a0, a1, radius, 0.3)) continue;
                if (!MonarchShapes.ringHurts(p.onGround(), p.getY(), cy, MonarchRules.SWEEP_REACH)) continue;
                if (MonarchAttacks.strike(l, b, fight, p, MonarchMove.SWEEP, t)) {
                    seen.add(p.getUUID());
                    MonarchAttacks.shove(p, cx, cz, 0.8, 0.3);
                }
            }
        }
    }
}
