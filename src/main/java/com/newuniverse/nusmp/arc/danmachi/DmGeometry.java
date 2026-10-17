package com.newuniverse.nusmp.arc.danmachi;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: every attack of the four DanMachi monsters is one of a few shapes on the floor (a cone in front, a ring round the body, a lane, a set of marks, a
 * spoke of lanes, a sweeping breath), and the question "is this foe inside the shape?" must give the same answer to the telegraph that draws it and to the
 * strike that lands, or the fight feels unfair. This is that one answer, pure (plain numbers on the horizontal plane: x and z, never a game type), so it can
 * be unit tested and mutation checked. Yaw follows Minecraft: 0 faces +z, -90 faces +x.
 */
public final class DmGeometry {
    private DmGeometry() {}

    /** The Minecraft yaw in degrees of a horizontal direction, in the usual range from -180 (inclusive) to 180. */
    public static double yawOf(double dx, double dz) {
        double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        return ((yaw + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }

    /** The horizontal unit direction {x, z} of a Minecraft yaw in degrees. */
    public static double[] dirOfYaw(double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new double[]{-Math.sin(r), Math.cos(r)};
    }

    /** A vector turned by {@code radians} in the x-z plane (the same turn as {@code Vec3.rotateY} in the Zogratis fans: positive turns from +x towards +z). */
    public static double[] rotate(double dx, double dz, double radians) {
        double c = Math.cos(radians), s = Math.sin(radians);
        return new double[]{dx * c - dz * s, dx * s + dz * c};
    }

    /** The unit vector of (dx, dz), or {@code fallback} (also a unit vector) when it is shorter than a thousandth of a block. */
    public static double[] unit(double dx, double dz, double[] fallback) {
        double len = Math.hypot(dx, dz);
        return len < 1e-3 ? fallback : new double[]{dx / len, dz / len};
    }

    /** The angle in degrees (0 to 180) between two horizontal vectors; 0 when either is too short to have a direction. */
    public static double angleBetweenDeg(double ax, double az, double bx, double bz) {
        double la = Math.hypot(ax, az), lb = Math.hypot(bx, bz);
        if (la < 1e-6 || lb < 1e-6) return 0;
        double cos = (ax * bx + az * bz) / (la * lb);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
    }

    /** The smallest difference in degrees (0 to 180) between two yaws. */
    public static double yawDiffDeg(double a, double b) {
        return Math.abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0);
    }

    /**
     * Is a body whose centre is at (dx, dz) from the origin inside a cone facing the unit vector (fx, fz)? The cone reaches {@code reach} blocks and opens
     * {@code halfAngleDeg} to each side; {@code bodyRadius} widens both (a big body is hit by the edge of a cone that only touches it). Anything within a
     * block and a quarter of the origin is always inside (nobody dodges a blow by standing on the attacker).
     */
    public static boolean inCone(double dx, double dz, double fx, double fz, double halfAngleDeg, double reach, double bodyRadius) {
        double dist = Math.hypot(dx, dz);
        if (dist > reach + bodyRadius) return false;
        if (dist <= 1.25) return true;
        double slack = Math.toDegrees(Math.asin(Math.min(1.0, bodyRadius / dist)));
        return angleBetweenDeg(dx, dz, fx, fz) <= halfAngleDeg + slack;
    }

    /** How far along the unit direction (fx, fz) the point (px, pz) lies. */
    public static double along(double px, double pz, double fx, double fz) { return px * fx + pz * fz; }

    /** How far to the side of the line along (fx, fz) the point (px, pz) lies (absolute). */
    public static double lateral(double px, double pz, double fx, double fz) { return Math.abs(px * fz - pz * fx); }

    /**
     * Is a body at (px, pz) from the lane's origin inside a lane that runs {@code length} blocks along (fx, fz) and is {@code width} blocks wide?
     * {@code bodyRadius} widens the lane on every side.
     */
    public static boolean inLane(double px, double pz, double fx, double fz, double length, double width, double bodyRadius) {
        double a = along(px, pz, fx, fz);
        return a >= -bodyRadius && a <= length + bodyRadius && lateral(px, pz, fx, fz) <= width / 2.0 + bodyRadius;
    }

    /** Is a body at {@code dist} from a ring wave's centre inside the wave when the wave's front is at {@code waveRadius} and it is {@code thick} blocks thick? */
    public static boolean ringHit(double dist, double waveRadius, double thick) { return Math.abs(dist - waveRadius) <= thick; }

    /** The front of a ring wave {@code age} ticks after it began: it starts {@code start} blocks out and travels {@code speed} blocks a tick. */
    public static double waveRadius(int age, double start, double speed) { return start + Math.max(0, age) * speed; }

    /** Ticks a ring wave needs to cross {@code reach} blocks (and a little past it). */
    public static int waveTicks(double reach, double start, double speed, double thick) { return (int) Math.ceil((reach + thick - start) / speed) + 1; }

    /** Yaw offsets in radians of {@code count} projectiles fanned over {@code spreadDeg} degrees, centred on the aim (one projectile flies straight). */
    public static double[] fan(int count, double spreadDeg) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        if (n == 1) return out;
        double total = Math.toRadians(Math.max(0.0, spreadDeg)), step = total / (n - 1);
        for (int i = 0; i < n; i++) out[i] = (i - (n - 1) / 2.0) * step;
        return out;
    }

    /** {@code count} horizontal unit directions {x, z}, evenly spaced round the circle, the first at {@code phase} radians from +x. */
    public static double[][] spokes(int count, double phase) {
        int n = Math.max(1, count);
        double[][] out = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = phase + i * Math.PI * 2 / n;
            out[i] = new double[]{Math.cos(a), Math.sin(a)};
        }
        return out;
    }

    /**
     * Is a body at (px, pz) from the middle of a set of spokes inside one of them? Each spoke is a lane of {@code length} and {@code width} from the middle
     * (a body within the first block is in all of them). The gaps between the spokes are the safe places, and they are wide enough to stand in when
     * {@link #spokeGap} says so.
     */
    public static boolean inAnySpoke(double px, double pz, double[][] spokes, double length, double width, double bodyRadius) {
        if (Math.hypot(px, pz) <= 1.0) return true;
        for (double[] s : spokes) if (inLane(px, pz, s[0], s[1], length, width, bodyRadius)) return true;
        return false;
    }

    /** The width in blocks of the gap between two neighbouring spokes of {@code count} at {@code distance} from the middle (arc length minus the lane width). */
    public static double spokeGap(int count, double distance, double laneWidth) { return Math.PI * 2 * distance / Math.max(1, count) - laneWidth; }

    /**
     * Where {@code n} marks go: the first on the target at (tx, tz), then on the other foes ({@code foes} as {x, z} pairs in priority order), then on rings
     * round the target, every mark at least 1.6 radii from every other so they never stack. At most {@code n} marks come back, each {x, z}.
     */
    public static List<double[]> markPlan(int n, double tx, double tz, double[][] foes, double radius, double phase) {
        int want = Math.max(0, n);
        List<double[]> out = new ArrayList<>();
        if (want == 0) return out;
        double apart = radius * 1.6;
        out.add(new double[]{tx, tz});
        if (foes != null) {
            for (double[] f : foes) {
                if (out.size() >= want) break;
                if (f == null || f.length < 2 || near(out, f[0], f[1], apart)) continue;
                out.add(new double[]{f[0], f[1]});
            }
        }
        for (int ring = 1; ring <= 3 && out.size() < want; ring++) {
            double r = radius * 2.2 * ring + 0.5;
            int slots = Math.max(3, (int) Math.floor(Math.PI * 2 * r / (radius * 2.2)));
            for (int k = 0; k < slots && out.size() < want; k++) {
                double a = phase + (k + 0.5 * (ring - 1)) * Math.PI * 2 / slots;
                double x = tx + Math.cos(a) * r, z = tz + Math.sin(a) * r;
                if (!near(out, x, z, apart)) out.add(new double[]{x, z});
            }
        }
        return out;
    }

    private static boolean near(List<double[]> marks, double x, double z, double minDist) {
        for (double[] m : marks) {
            double dx = m[0] - x, dz = m[1] - z;
            if (dx * dx + dz * dz < minDist * minDist) return true;
        }
        return false;
    }
}
