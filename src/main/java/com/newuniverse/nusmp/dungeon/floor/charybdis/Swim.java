package com.newuniverse.nusmp.dungeon.floor.charybdis;

/**
 * How the Charybdis Spawn moves (pure, unit tested). A floating water mob has no walkable path, so the boss is steered by hand: every tick
 * the boss tells this class where it is and where its target is, and gets back a velocity to set. The three behaviours:
 * <ul>
 *   <li><b>Orbit</b>: a slow lap round the arena point (the hover point above the dais) at a radius of {@value #ORBIT_MIN} to {@value #ORBIT_MAX}
 *       blocks that breathes in and out, bobbing {@value #BAND} blocks above and below the arena point (2 to 8 blocks above the floor).</li>
 *   <li><b>Engage</b>: a target within {@value #CHASE_RANGE} blocks of the boss is approached (to {@value #STANDOFF} blocks, close enough for its
 *       bite to land and no closer) and shadowed, so the moves that need the boss close have something to hit. Farther, it orbits again.</li>
 *   <li><b>Lunge</b>: the Beak Lunge, a fixed dive along a line the wind-up drew.</li>
 * </ul>
 * It never leaves the {@value #LEASH}-block leash round the arena point, never leaves the vertical window, brakes to a stop while the boss casts,
 * and when it has been stuck against stonework for a few seconds it swims for the open orbit instead of the target (and, much later, asks to
 * be put back on the orbit).
 */
public final class Swim {
    public static final double ORBIT_MIN = 8, ORBIT_MAX = 14, BAND = 3, LEASH = 23, BELOW = 5.5, ABOVE = 12;
    public static final double ORBIT_SPEED = 0.14, CHASE_SPEED = 0.21, ACCEL = 0.012, STANDOFF = 2.5, CHASE_RANGE = 14, ARRIVE = 4.0;
    public static final int LUNGE_TICKS = 14;
    public static final double LUNGE_SPEED = 0.9;
    /** The lap rate (radians a tick): one lap in about 40 seconds. */
    private static final double OMEGA = 0.0075;
    /** Stuck for this many ticks: swim for the open orbit; this many: ask to be put back. */
    public static final int STUCK_SWERVE = 60, STUCK_TELEPORT = 200, SWERVE_TICKS = 80;

    private final double cx, cy, cz, dir, phase;
    private double vx, vy, vz;
    private int lungeLeft;
    private double lx, ly, lz;
    private int stuck;
    private long swerveUntil = Long.MIN_VALUE;
    private double sx = Double.NaN, sy, sz;

    /** A swimmer round the arena point (cx, cy, cz); the seed picks the direction of the laps and where on the circle it starts. */
    public Swim(double cx, double cy, double cz, long seed) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.dir = (seed & 1L) == 0 ? 1 : -1;
        this.phase = ((seed >>> 1) & 1023L) / 1024.0 * Math.PI * 2;
    }

    public double vx() { return vx; }

    public double vy() { return vy; }

    public double vz() { return vz; }

    public boolean lunging() { return lungeLeft > 0; }

    /** Forgets the velocity (after a teleport, or when something else moved the boss). */
    public void halt() {
        vx = 0;
        vy = 0;
        vz = 0;
        lungeLeft = 0;
    }

    // ---------------------------------------------------------------- the orbit
    /** The point of the orbit at 'tick' ({x, y, z}). */
    public double[] orbitPoint(long tick) {
        double a = phase + dir * OMEGA * tick;
        double r = (ORBIT_MIN + ORBIT_MAX) / 2 + (ORBIT_MAX - ORBIT_MIN) / 2 * Math.sin(tick / 80.0 + phase);
        double h = BAND * Math.sin(tick / 60.0 + phase * 2);
        return new double[]{cx + Math.cos(a) * r, cy + h, cz + Math.sin(a) * r};
    }

    /** Keeps a point inside the leash and the vertical window (the arena point +/- the leash, from BELOW under it to ABOVE over it). */
    public double[] clamp(double x, double y, double z) {
        double dx = x - cx, dz = z - cz, d = Math.sqrt(dx * dx + dz * dz);
        if (d > LEASH) {
            dx *= LEASH / d;
            dz *= LEASH / d;
        }
        return new double[]{cx + dx, Math.max(cy - BELOW, Math.min(cy + ABOVE, y)), cz + dz};
    }

    // ---------------------------------------------------------------- the lunge
    /** Starts the Beak Lunge along (dx, dy, dz) (any length but zero). */
    public void lunge(double dx, double dy, double dz) {
        double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6) return;
        lx = dx / l;
        ly = dy / l;
        lz = dz / l;
        lungeLeft = LUNGE_TICKS;
    }

    /** The speed of the lunge when 'left' of its ticks are left: it gathers speed over four ticks, holds, and slows over the last four. */
    public static double lungeSpeed(int left) {
        if (left <= 0 || left > LUNGE_TICKS) return 0;
        int done = LUNGE_TICKS - left;
        if (done < 4) return LUNGE_SPEED * (done + 1) / 4.0;
        if (left <= 4) return LUNGE_SPEED - (LUNGE_SPEED - 0.25) * (5 - left) / 4.0;
        return LUNGE_SPEED;
    }

    /** How far the whole lunge carries it, in blocks, if nothing stops it. */
    public static double lungeLength() {
        double s = 0;
        for (int left = LUNGE_TICKS; left >= 1; left--) s += lungeSpeed(left);
        return s;
    }

    // ---------------------------------------------------------------- one tick
    /**
     * Works out this tick's velocity.
     * @param x,y,z      where the boss is now
     * @param hasTarget  whether there is a target, and where ({@code tx, ty, tz}: its middle)
     * @param hold       the boss is casting, recovering or in a cutscene: it brakes to a stop instead of swimming
     * @param speedScale the boss's own speed multiplier (the power's: 1.0 at the opening, 1.18 at the Gates)
     */
    public void step(double x, double y, double z, boolean hasTarget, double tx, double ty, double tz, boolean hold, double speedScale, long tick) {
        double scale = Math.max(0.3, Math.min(2.0, speedScale));
        if (lungeLeft > 0) {
            double s = lungeSpeed(lungeLeft--);
            vx = lx * s;
            vy = ly * s;
            vz = lz * s;
            return;
        }
        sample(x, y, z, tick, hold);
        if (hold) {
            brake();
            return;
        }
        boolean chase = hasTarget && tick >= swerveUntil;
        double dxT = tx - x, dyT = ty - y, dzT = tz - z;
        double dT = Math.sqrt(dxT * dxT + dyT * dyT + dzT * dzT);
        chase = chase && dT <= CHASE_RANGE;
        double[] want;
        double speed;
        if (chase) {
            double k = dT < 1e-6 ? 0 : Math.max(0, dT - STANDOFF) / dT;                // stops STANDOFF short of the target
            want = clamp(x + dxT * k, ty + 0.4, z + dzT * k);
            speed = CHASE_SPEED * scale;
        } else {
            double[] o = orbitPoint(tick);
            want = clamp(o[0], o[1], o[2]);
            speed = ORBIT_SPEED * scale;
        }
        double ex = want[0] - x, ey = want[1] - y, ez = want[2] - z, e = Math.sqrt(ex * ex + ey * ey + ez * ez);
        double go = e < 1e-6 ? 0 : speed * Math.min(1.0, e / ARRIVE);                   // slows to a stop on arrival
        double wvx = e < 1e-6 ? 0 : ex / e * go, wvy = e < 1e-6 ? 0 : ey / e * go, wvz = e < 1e-6 ? 0 : ez / e * go;
        vx = approach(vx, wvx);
        vy = approach(vy, wvy);
        vz = approach(vz, wvz);
    }

    private static double approach(double v, double want) {
        double d = want - v;
        return Math.abs(d) <= ACCEL ? want : v + Math.signum(d) * ACCEL;
    }

    private void brake() {
        vx = approach(vx, 0);
        vy = approach(vy, 0);
        vz = approach(vz, 0);
    }

    // ---------------------------------------------------------------- stuck
    private void sample(double x, double y, double z, long tick, boolean hold) {
        if (hold) {
            stuck = 0;
            sx = Double.NaN;
            return;
        }
        if (Double.isNaN(sx)) {
            sx = x;
            sy = y;
            sz = z;
            return;
        }
        if (tick % 20 != 0) return;
        double moved = Math.sqrt((x - sx) * (x - sx) + (y - sy) * (y - sy) + (z - sz) * (z - sz));
        boolean trying = Math.abs(vx) + Math.abs(vy) + Math.abs(vz) > 0.05;
        stuck = trying && moved < 0.5 ? stuck + 20 : 0;
        if (stuck >= STUCK_SWERVE && swerveUntil < tick) swerveUntil = tick + SWERVE_TICKS;
        sx = x;
        sy = y;
        sz = z;
    }

    /** Has the boss been stuck so long it should be put back on the orbit? */
    public boolean stuckTooLong() { return stuck >= STUCK_TELEPORT; }

    /** The boss was put back: the count starts again. */
    public void clearStuck() {
        stuck = 0;
        sx = Double.NaN;
        halt();
    }

    // ---------------------------------------------------------------- facing
    /** The yaw (degrees, Minecraft's convention: 0 looks along +z, 90 along -x) of the way (dx, dz). */
    public static float yawOf(double dx, double dz) { return (float) (Math.toDegrees(Math.atan2(-dx, dz))); }

    /** The pitch (degrees, positive looks down) of the way (dx, dy, dz). */
    public static float pitchOf(double dx, double dy, double dz) {
        double h = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.toDegrees(Math.atan2(dy, Math.max(h, 1e-9))));
    }

    /** Turns 'current' towards 'wanted' (degrees) the short way round by at most 'step'. */
    public static float turn(float current, float wanted, float step) {
        float d = wrap(wanted - current);
        if (d > step) d = step;
        if (d < -step) d = -step;
        return current + d;
    }

    /** An angle in degrees brought into [-180, 180). */
    public static float wrap(float degrees) {
        float r = degrees % 360f;
        if (r >= 180f) r -= 360f;
        if (r < -180f) r += 360f;
        return r;
    }
}
