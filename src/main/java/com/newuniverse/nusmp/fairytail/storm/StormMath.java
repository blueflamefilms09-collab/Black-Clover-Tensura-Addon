package com.newuniverse.nusmp.fairytail.storm;

/**
 * Why: the server runs a storm and every nearby client draws it, and the two must agree on WHERE things are without the server sending each drop. So everything that is a function
 * of (seed, age) lives here, with no game class in it: where the cell is, how big it is, which hail stones fall where and when they land, how far a gust front has run, the forked
 * path of a bolt. The server uses the answers to hurt things, the client uses the same answers to draw them, and the unit tests hold both to the same numbers. This class also holds
 * every limit of the storm (the per-target hit caps, the foe cap, the cloud height), so no number of a storm is a literal buried in a tick method.
 */
public final class StormMath {
    private StormMath() {}

    // ------------------------------------------------------------------------------------------------------------------ limits
    /** The cloud base hangs this high over the ground point of the cell. */
    public static final double CLOUD_HEIGHT = 9.0;
    /** A storm hurts and moves at most this many creatures at a time. */
    public static final int MAX_FOES = 24;
    /** The cost floor: a share of a tiny Aura pool is never less than this. */
    public static final double MIN_COST = 12.0;
    /** Ticks a cell takes to build up and to die away. */
    public static final int FADE_IN = 14, FADE_OUT = 24;
    /** The first bolt waits for the cloud to build. */
    public static final int BOLT_LEAD = 20;
    /** Hail: a volley every {@code HAIL_PERIOD} ticks, a stone falls {@code HAIL_FALL} ticks, at most {@code HAIL_MAX_PER_VOLLEY} stones in a volley. */
    public static final int HAIL_PERIOD = 5, HAIL_FALL = 6, HAIL_MAX_PER_VOLLEY = 4;
    /** Per cell and per creature: the most bolts, hail stones and rain/funnel pulses that may land on it, and the ticks between two of each. */
    public static final int MAX_BOLTS_PER_TARGET = 3, MAX_HAIL_PER_TARGET = 8, MAX_PULSES_PER_TARGET = 6;
    public static final int BOLT_GAP = 12, HAIL_GAP = 6, PULSE_GAP = 20;
    /** The radius where a bolt hurts around its mark, and the share of the bolt's damage the ones next to the first take. */
    public static final double BOLT_SPLASH = 2.4, BOLT_SECONDARY = 0.6;
    /** A hail stone hurts what is this close to where it lands. */
    public static final double HAIL_RADIUS = 1.35;
    /** The calm eye of a big cell. */
    public static final double EYE_RADIUS = 3.2;
    /** A gust ring leaves the middle every this many ticks and is this wide. */
    public static final int GUST_PERIOD = 50;
    public static final double GUST_WIDTH = 2.6;
    /** Highest speed (blocks per tick) a storm gives a creature, up or sideways. */
    public static final double MAX_PUSH = 1.3, MAX_LIFT = 0.55;
    /** The effect packets of one cast, and the farthest a cell is placed from the caster whatever the spell says. */
    public static final double MAX_AIM = 32.0;

    // ------------------------------------------------------------------------------------------------------------------ random
    /** A deterministic 0..1 number for (seed, i, salt): the same on the server and on every client. */
    public static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31;
        x *= 0x7FB5D329728EA185L;
        x ^= x >>> 27;
        x *= 0x81DADEF4BC2DD44DL;
        x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    // ------------------------------------------------------------------------------------------------------------------ seed packing
    /** The seed of an effect: 48 random bits, the weather parts in bits 48..55 and the spell in bits 56..59. */
    public static long packSeed(long random, int mask, int spellOrdinal) {
        return (random & 0xFFFFFFFFFFFFL) | ((long) (mask & 0xFF) << 48) | ((long) (spellOrdinal & 0xF) << 56);
    }

    public static int maskOf(long seed) { return (int) ((seed >>> 48) & 0xFF); }

    public static int spellOf(long seed) { return (int) ((seed >>> 56) & 0xF); }

    // ------------------------------------------------------------------------------------------------------------------ the cell in time
    /** The point the cell's ground centre is at after {@code age} ticks of {@code duration}: a straight slow drift from the start to the end point. */
    public static double[] center(double[] from, double[] to, double age, int duration) {
        double t = duration <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, age / duration));
        return new double[]{from[0] + (to[0] - from[0]) * t, from[1] + (to[1] - from[1]) * t, from[2] + (to[2] - from[2]) * t};
    }

    /** 0..1: builds up over {@link #FADE_IN} ticks, holds, dies away over {@link #FADE_OUT} ticks. */
    public static double envelope(double age, int duration) {
        if (age < 0 || age >= duration) return 0.0;
        double in = Math.min(1.0, age / FADE_IN), out = Math.min(1.0, (duration - age) / FADE_OUT);
        double e = Math.min(in, out);
        return e * e * (3 - 2 * e);
    }

    /** The radius the cell has at that age: it opens up while it builds and shrinks a little as it dies. */
    public static double radiusAt(double radius, double age, int duration) {
        double e = envelope(age, duration);
        return radius * (0.35 + 0.65 * e);
    }

    /** Where the drift of a cell ends: {@code driftPerSecond} blocks a second along the unit direction (dx, dz) for the whole duration. */
    public static double[] driftEnd(double[] from, double dx, double dz, double driftPerSecond, int duration) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-6) return new double[]{from[0], from[1], from[2]};
        double d = driftPerSecond * duration / 20.0;
        return new double[]{from[0] + dx / len * d, from[1], from[2] + dz / len * d};
    }

    // ------------------------------------------------------------------------------------------------------------------ hail
    /** How many stones a volley of a cell of this radius holds. */
    public static int hailPerVolley(double radius) {
        return Math.max(1, Math.min(HAIL_MAX_PER_VOLLEY, (int) Math.round(radius / 2.0)));
    }

    /** The volley whose stones LAND at this age, or -1 when none does. A stone of volley {@code v} starts falling at age {@code v * HAIL_PERIOD}. */
    public static int hailVolleyLanding(int age) {
        if (age < HAIL_FALL) return -1;
        int a = age - HAIL_FALL;
        return a % HAIL_PERIOD == 0 ? a / HAIL_PERIOD : -1;
    }

    /** The offset (x, z) from the cell centre where stone {@code i} of volley {@code v} lands: uniform over the disc, never at its very rim. */
    public static double[] hailStone(long seed, int volley, int i, double radius) {
        double r = Math.sqrt(hash(seed, volley * 8 + i, 11)) * radius * 0.92;
        double a = hash(seed, volley * 8 + i, 12) * Math.PI * 2;
        return new double[]{Math.cos(a) * r, Math.sin(a) * r};
    }

    // ------------------------------------------------------------------------------------------------------------------ bolts
    /** Whether the server throws a bolt at this age: after the lead, on the spell's beat, while the cell is not dying, and under the bolt cap of the spell. */
    public static boolean boltDue(int age, int duration, int every, int boltsSoFar, int maxBolts) {
        if (every <= 0 || maxBolts <= 0 || boltsSoFar >= maxBolts) return false;
        if (age < BOLT_LEAD || age > duration - 10) return false;
        return (age - BOLT_LEAD) % every == 0;
    }

    /** A random point of the cell's disc (offset from its centre) for a bolt that has no foe to find, never nearer than 15% of the radius from the middle. */
    public static double[] strayPoint(long seed, int n, double radius) {
        double r = (0.15 + 0.7 * Math.sqrt(hash(seed, n, 21))) * radius;
        double a = hash(seed, n, 22) * Math.PI * 2;
        return new double[]{Math.cos(a) * r, Math.sin(a) * r};
    }

    /**
     * The forked path of a bolt from the cloud to the ground: {@code segments + 1} points, the ends exactly at {@code from} and {@code to}, the points between pushed sideways by a
     * jitter that is largest in the middle. The same path for the same seed and {@code frame}; the client re-rolls {@code frame} a few times while the bolt flickers.
     */
    public static double[][] boltPath(long seed, int frame, double[] from, double[] to, int segments, double jitter) {
        int n = Math.max(2, segments);
        double[][] p = new double[n + 1][3];
        double dx = to[0] - from[0], dy = to[1] - from[1], dz = to[2] - from[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        // two unit vectors across the bolt: one horizontal, one that completes the frame
        double hx = len < 1.0e-6 ? 1 : -dz, hz = len < 1.0e-6 ? 0 : dx;
        double hl = Math.sqrt(hx * hx + hz * hz);
        if (hl < 1.0e-6) { hx = 1; hz = 0; hl = 1; }
        hx /= hl;
        hz /= hl;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double k = Math.sin(t * Math.PI) * jitter;
            double a = (hash(seed, frame * 64 + i, 31) - 0.5) * 2 * k, b = (hash(seed, frame * 64 + i, 32) - 0.5) * 2 * k;
            double x = from[0] + dx * t + hx * a, y = from[1] + dy * t + b * 0.35, z = from[2] + dz * t + hz * a;
            if (i == 0) { x = from[0]; y = from[1]; z = from[2]; }
            if (i == n) { x = to[0]; y = to[1]; z = to[2]; }
            p[i][0] = x;
            p[i][1] = y;
            p[i][2] = z;
        }
        return p;
    }

    // ------------------------------------------------------------------------------------------------------------------ fronts
    /** How far along its line the front of a gust or a surge has run at this age (blocks). Fast at first, slowing: it leaves the staff and spends itself. */
    public static double frontPos(double age, int duration, double length) {
        double t = duration <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, age / duration));
        double e = 1.0 - (1.0 - t) * (1.0 - t);
        return length * e;
    }

    /** True if the point (along, across) of a line effect is inside the band of the front: within {@code thick} behind and ahead of its head, and within {@code halfWidth} across. */
    public static boolean inFront(double along, double across, double head, double thick, double halfWidth) {
        return Math.abs(across) <= halfWidth && along >= head - thick && along <= head + thick * 0.4 && along >= 0;
    }

    /** Where the ring of gust number {@code n} of a cell is at this age (its radius), or a negative number when it has not left yet or has gone. */
    public static double gustRingRadius(double age, int n, double radius) {
        int born = n * GUST_PERIOD;
        int life = (int) (GUST_PERIOD * 0.9);
        double a = age - born;
        if (a < 0 || a > life) return -1.0;
        return radius * (0.1 + 0.9 * (a / (double) life));
    }

    // ------------------------------------------------------------------------------------------------------------------ push
    /** Limits a velocity change a storm gives (blocks per tick): sideways at most {@link #MAX_PUSH} in length, up at most {@link #MAX_LIFT}. */
    public static double[] limitPush(double vx, double vy, double vz) {
        double h = Math.sqrt(vx * vx + vz * vz);
        if (h > MAX_PUSH) {
            vx *= MAX_PUSH / h;
            vz *= MAX_PUSH / h;
        }
        vy = Math.max(-MAX_LIFT, Math.min(MAX_LIFT, vy));
        return new double[]{vx, vy, vz};
    }

    // ------------------------------------------------------------------------------------------------------------------ cost
    /** The Aura a cast costs: the share of the maximum, scaled by the mana law's multiplier, never below the floor. */
    public static double cost(double maxAura, double percent, double lawMultiplier) {
        return Math.max(MIN_COST, maxAura * percent / 100.0) * Math.max(0.0, lawMultiplier);
    }
}
