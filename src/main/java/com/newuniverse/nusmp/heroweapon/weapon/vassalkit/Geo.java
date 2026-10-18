package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

/**
 * Why: the five weapons built with this kit all ask the same geometric questions (is this point inside a cone, how far is it from a line, did a shot cross a pane
 * of glass since the last tick, which way do five fanned chains point), and those are exactly the places where a sign or an off-by-one hides until someone
 * plays. They are plain arithmetic on doubles here, with tests, and the weapons only feed them positions taken from the game. Yaw follows the game's
 * convention: yaw 0 looks along +z, yaw 90 along -x, so the horizontal direction of a yaw is (-sin, cos).
 */
public final class Geo {
    private Geo() {}

    /** The horizontal unit direction (x, z) of a yaw given in degrees (the game's convention). */
    public static double[] yawDir(double yawDeg) {
        double a = Math.toRadians(yawDeg);
        return new double[]{-Math.sin(a), Math.cos(a)};
    }

    /** The horizontal vector (x, z) turned by {@code deg} degrees: the same as adding {@code deg} to its yaw, so a positive angle turns it to the wielder's right and a negative one to the left. */
    public static double[] turn(double x, double z, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new double[]{x * c - z * s, x * s + z * c};
    }

    /** Directions of {@code count} lines spread evenly over {@code spreadDeg} degrees round the unit direction (x, z); one line points straight ahead when count is odd. */
    public static double[][] fan(double x, double z, int count, double spreadDeg) {
        int n = Math.max(1, count);
        double[][] out = new double[n][];
        double step = n == 1 ? 0 : spreadDeg / (n - 1), first = n == 1 ? 0 : -spreadDeg / 2;
        for (int i = 0; i < n; i++) out[i] = turn(x, z, first + step * i);
        return out;
    }

    /** The distance from point p to the segment a-b. */
    public static double segDist(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double t = segParam(px, py, pz, ax, ay, az, bx, by, bz);
        double qx = ax + (bx - ax) * t, qy = ay + (by - ay) * t, qz = az + (bz - az) * t;
        double dx = px - qx, dy = py - qy, dz = pz - qz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Where along the segment a-b (0 at a, 1 at b) the point nearest to p lies. */
    public static double segParam(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double abx = bx - ax, aby = by - ay, abz = bz - az;
        double len2 = abx * abx + aby * aby + abz * abz;
        if (len2 < 1e-12) return 0.0;
        double t = ((px - ax) * abx + (py - ay) * aby + (pz - az) * abz) / len2;
        return Math.max(0.0, Math.min(1.0, t));
    }

    /** True when the vector (dx, dy, dz) lies within {@code halfAngleDeg} of the look vector (lx, ly, lz); a zero vector counts as inside. */
    public static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double halfAngleDeg) {
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz), ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (dl < 1e-9) return true;
        if (ll < 1e-9) return false;
        double cos = (dx * lx + dy * ly + dz * lz) / (dl * ll);
        return cos >= Math.cos(Math.toRadians(halfAngleDeg)) - 1e-12;
    }

    /** The mirror image of velocity v off a surface with unit normal n: v - 2 (v.n) n. */
    public static double[] reflect(double vx, double vy, double vz, double nx, double ny, double nz) {
        double d = vx * nx + vy * ny + vz * nz;
        return new double[]{vx - 2 * d * nx, vy - 2 * d * ny, vz - 2 * d * nz};
    }

    /**
     * Did a thing that moved from p0 to p1 in one tick pass through a round pane of glass? The pane is a disc of radius {@code r} round the centre c, facing the unit
     * normal n, and counts as {@code thick} blocks thick on each side so that a fast arrow cannot skip it between two ticks. The thing must be moving against the
     * normal (towards the side the pane faces away from), so a shot already leaving the pane is let go.
     */
    public static boolean sweepsDisc(double cx, double cy, double cz, double nx, double ny, double nz, double r, double thick,
                                     double x0, double y0, double z0, double x1, double y1, double z1) {
        double mx = x1 - x0, my = y1 - y0, mz = z1 - z0;
        if (mx * nx + my * ny + mz * nz >= -1e-9) return false;               // not heading into the pane's face
        double d0 = (x0 - cx) * nx + (y0 - cy) * ny + (z0 - cz) * nz;
        double d1 = (x1 - cx) * nx + (y1 - cy) * ny + (z1 - cz) * nz;
        if (Math.min(d0, d1) > thick || Math.max(d0, d1) < -thick) return false;   // the slab is not touched this tick
        double t;
        if (d0 > thick && d1 < thick) t = (d0 - thick) / (d0 - d1);            // first touch of the front face of the slab
        else t = 0.0;                                                          // already inside the slab: judge where it is now
        if (t > 1.0) t = 1.0;
        double qx = x0 + mx * t - cx, qy = y0 + my * t - cy, qz = z0 + mz * t - cz;
        double along = qx * nx + qy * ny + qz * nz;
        double off2 = qx * qx + qy * qy + qz * qz - along * along;
        return off2 <= r * r;
    }

    /**
     * The speed (blocks per tick) of one tick of a pull that must stop {@code stopDist} blocks short of its goal: gentle near the goal so nothing overshoots, never
     * faster than {@code maxSpeed}, and zero once it has arrived.
     */
    public static double pullSpeed(double dist, double stopDist, double maxSpeed) {
        double left = dist - stopDist;
        return left <= 0 ? 0.0 : Math.min(maxSpeed, 0.2 + left * 0.35);
    }

    /**
     * The unit direction of (lx, ly, lz) turned {@code yawDeg} degrees to the right (negative: left) and raised {@code pitchDeg} degrees (negative: lowered). Used to
     * scatter notes and to fan lines round a look direction without drifting when the wielder looks up or down.
     */
    public static double[] aimOff(double lx, double ly, double lz, double yawDeg, double pitchDeg) {
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (len < 1e-9) return new double[]{0, 0, 1};
        double ux = lx / len, uy = ly / len, uz = lz / len;
        double[] h = turn(ux, uz, yawDeg);
        double pitch = Math.asin(Math.max(-1.0, Math.min(1.0, uy))) + Math.toRadians(pitchDeg);
        pitch = Math.max(-Math.PI / 2 + 1e-3, Math.min(Math.PI / 2 - 1e-3, pitch));
        double y = Math.sin(pitch), flat = Math.cos(pitch), hl = Math.sqrt(h[0] * h[0] + h[1] * h[1]);
        if (hl < 1e-9) return new double[]{flat, y, 0.0};                // looking straight up or down: any horizontal side will do
        return new double[]{h[0] / hl * flat, y, h[1] / hl * flat};
    }
}
