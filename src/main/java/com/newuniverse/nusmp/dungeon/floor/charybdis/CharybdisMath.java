package com.newuniverse.nusmp.dungeon.floor.charybdis;

/**
 * The geometry of the Charybdis Spawn's moves (pure, unit tested): the fan of scales, the arc of the Tentacle Sweep, the whirlpool's push, the
 * circle of the Tidal Crush, where the megalodons come out of the wall and how the leviathan rises. Distances are blocks, angles radians
 * unless a name says degrees.
 */
public final class CharybdisMath {
    private CharybdisMath() {}

    // ---------------------------------------------------------------- the fan of scales
    /** The angle between two neighbouring scales of a fan (8.5 degrees): a nine-scale fan is 68 degrees across. */
    public static final double FAN_STEP = Math.toRadians(8.5);

    /** The yaw offsets of a fan of 'count' scales, centred on the aim (0 in the middle for an odd count). */
    public static double[] fan(int count) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = (i - (n - 1) / 2.0) * FAN_STEP;
        return out;
    }

    /** The pitch offset of scale i of a fan: neighbouring scales alternate a little up and down, so a fan is not a flat sheet to swim under. */
    public static double fanPitch(int i, int count) {
        if (count < 5) return 0;
        return (i % 2 == 0 ? 1 : -1) * Math.toRadians(3.0) * (i == (count - 1) / 2 ? 0 : 1);
    }

    /**
     * The unit direction {x, y, z} of (ax, ay, az) turned by 'yawOffset' about the vertical axis and tipped by 'pitchOffset' (radians, positive
     * up). A zero vector aims level along +x. This is how one scale of a fan is aimed from the centre line.
     */
    public static double[] aim(double ax, double ay, double az, double yawOffset, double pitchOffset) {
        double flat = Math.sqrt(ax * ax + az * az);
        double yaw = flat < 1e-9 ? yawOffset : Math.atan2(az, ax) + yawOffset;
        double pitch = Math.atan2(ay, flat) + pitchOffset;
        pitch = Math.max(-Math.PI / 2 + 1e-6, Math.min(Math.PI / 2 - 1e-6, pitch));
        double cp = Math.cos(pitch);
        return new double[]{Math.cos(yaw) * cp, Math.sin(pitch), Math.sin(yaw) * cp};
    }

    /** (x, z) turned by 'angle' about the vertical axis. */
    public static double[] rotate(double x, double z, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new double[]{x * c - z * s, x * s + z * c};
    }

    // ---------------------------------------------------------------- the whirlpool
    /** The inward push (blocks a tick, added every second tick) at the rim of a whirlpool and the sideways push that makes it turn. Gentle: a swimmer outswims it. */
    public static final double WHIRL_PULL = 0.018, WHIRL_SWIRL = 0.012;
    public static final int WHIRL_TICKS = 80, MAELSTROM_TICKS = 100, WHIRL_EVERY = 2, SLOW_TICKS = 60;
    public static final double WHIRL_RADIUS = 6.0, MAELSTROM_RADIUS = 7.5, WHIRL_HEIGHT = 7.0;

    /**
     * The velocity a whirlpool of the given radius adds to a foe at offset (dx, dz) from its middle: {ax, az}. Strongest at the rim (the pull
     * fades towards the middle so nobody is shaken about at the centre), turning anticlockwise as seen from above; nothing outside the radius.
     */
    public static double[] whirlpool(double dx, double dz, double radius) {
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > radius || radius <= 0) return new double[]{0, 0};
        if (d < 1e-6) return new double[]{0, 0};
        double edge = Math.max(0.15, Math.min(1.0, d / radius));
        double ix = -dx / d, iz = -dz / d;                          // towards the middle
        double tx = -dz / d, tz = dx / d;                           // anticlockwise
        return new double[]{ix * WHIRL_PULL * edge + tx * WHIRL_SWIRL * edge, iz * WHIRL_PULL * edge + tz * WHIRL_SWIRL * edge};
    }

    // ---------------------------------------------------------------- the sweep, the crush, the lunge
    /** The Tentacle Sweep: reach from the boss's middle, half the arc (100 degrees: a 200 degree sweep), and the vertical slack. */
    public static final double SWEEP_REACH = 7.5, SWEEP_HALF_ARC = Math.toRadians(100), SWEEP_HEIGHT = 4.0;
    /** The Tidal Crush: the circle's radius, how high above the floor it crushes, and the slam it gives. */
    public static final double CRUSH_RADIUS = 3.2, CRUSH_HEIGHT = 10.0, CRUSH_SLAM = -0.55;
    /** The Beak Lunge: how near to its path a foe is struck, once. */
    public static final double LUNGE_REACH = 2.4;

    /** Is (px, pz) within 'reach' of (ox, oz) and inside the arc of half-width 'halfArc' round the horizontal direction (dirX, dirZ)? */
    public static boolean inArc(double ox, double oz, double dirX, double dirZ, double px, double pz, double reach, double halfArc) {
        double vx = px - ox, vz = pz - oz, d = Math.sqrt(vx * vx + vz * vz);
        if (d > reach) return false;
        double l = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (d < 1e-6 || l < 1e-9) return true;                       // on top of the boss: in the arc
        double cos = (vx * dirX + vz * dirZ) / (d * l);
        return Math.acos(Math.max(-1, Math.min(1, cos))) <= halfArc;
    }

    /** Is (px, pz) inside the circle of radius r round (cx, cz)? */
    public static boolean inDisc(double cx, double cz, double px, double pz, double r) {
        double dx = px - cx, dz = pz - cz;
        return dx * dx + dz * dz <= r * r;
    }

    // ---------------------------------------------------------------- the megalodons
    /** How many megalodons one cast brings when 'alive' are already out: two, but never past the cap of four. */
    public static int minionsToSummon(int alive) {
        return Math.max(0, Math.min(CharybdisTable.MINIONS_PER_CAST, CharybdisTable.MINION_CAP - Math.max(0, alive)));
    }

    /**
     * Where the megalodons come out of the wall: 'count' angles round the basin's middle, spread evenly across 'spread' radians centred on
     * 'towards' (the angle from the middle to the player they are sent after).
     */
    public static double[] wallAngles(int count, double towards, double spread) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = n == 1 ? towards : towards + (i - (n - 1) / 2.0) * spread / (n - 1);
        return out;
    }

    /** The point at 'radius' from (cx, cz) in the direction 'angle': {x, z}. */
    public static double[] onRing(double cx, double cz, double angle, double radius) {
        return new double[]{cx + Math.cos(angle) * radius, cz + Math.sin(angle) * radius};
    }

    // ---------------------------------------------------------------- rising
    /** The height the leviathan has risen to when 'left' of 'total' ticks of the rise remain: from startY up by the rise, easing in and out. */
    public static double riseY(double startY, int left, int total) {
        double p = total <= 0 ? 1 : 1.0 - Math.max(0, Math.min(total, left)) / (double) total;
        return startY + CharybdisTable.RISE_BLOCKS * p * p * (3 - 2 * p);
    }
}
