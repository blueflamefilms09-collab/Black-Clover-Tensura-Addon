package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic behind Dante's Lucifero moves (pure, no Minecraft types, unit tested): the strength curve of a gravity field, the
 * fan of a rock sword volley, the wave of a stomp, the pull, and the Gravity Singularity (how many spheres, where their marks go, how
 * big the zone is, which cells of the ground a sphere bites, who can jump the collapse). {@code DanteMoves} applies the numbers to the
 * world; nothing here knows what a block or an entity is.
 */
public final class GravityMath {
    private GravityMath() {}

    // ---------------------------------------------------------------- the gravity field
    public static final double MIN_RADIUS = 2, MAX_RADIUS = 24, MIN_STRENGTH = 0.3, MAX_STRENGTH = 3.5;
    public static final int MIN_TICKS = 10, MAX_TICKS = 400, STEP = 4, RAMP = 16, MAX_VICTIMS = 32;

    public static double clampRadius(double r) { return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, r)); }
    public static int clampTicks(int t) { return Math.max(MIN_TICKS, Math.min(MAX_TICKS, t)); }
    public static double clampStrength(double s) { return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, s)); }

    /** How hard the field pulls at 'frac' of its radius (0 centre, 1 rim): 1 at the core, 0.35 at the rim, 0 outside (a gravity well, not a wall). */
    public static double weight(double frac) {
        if (frac < 0 || frac > 1) return frac < 0 ? 1.0 : 0.0;
        return 1.0 - 0.65 * frac * frac;
    }

    /** The field builds up over its first {@link #RAMP} ticks: 0.4 at once, 1 after the ramp, so being caught is never an instant lock. */
    public static double ramp(int age) { return Math.min(1.0, 0.4 + 0.6 * Math.max(0, age) / RAMP); }

    /** Slowness amplifier (0 = Slowness I ... 5 = Slowness VI, -1 = none) for a strength, a weight and a ramp. */
    public static int slowAmplifier(double strength, double weight, double ramp) {
        int a = (int) Math.round(strength * weight * ramp * 2.0) - 1;
        return Math.max(-1, Math.min(5, a));
    }

    /** Downward speed (blocks per tick, negative) a field forces on something inside it. */
    public static double sinkSpeed(double strength, double weight, double ramp) {
        return -(0.10 + 0.18 * strength * weight * ramp);
    }

    /** Base damage of one crush tick (the boss's damage law scales it afterwards). */
    public static double crushBase(double strength, double weight, double ramp) { return 1.2 * strength * weight * ramp; }

    // ---------------------------------------------------------------- rock swords
    public static final int MAX_SWORDS = 12;
    public static final double SWORD_SPEED = 1.25, SWORD_REACH = 0.55;
    public static final int SWORD_LIFE = 28, SWORD_STAGGER = 2;

    /** Swords in a volley at a power with 'slots' casts (3 at the opening, 5, 7, 9 ...). */
    public static int swordCount(int slots) { return Math.max(1, Math.min(MAX_SWORDS, 3 + 2 * (Math.max(1, slots) - 1))); }

    /** Yaw offsets in radians of the swords of a fan of 'count': centred on the aim, at most about 1.1 rad across. */
    public static double[] fan(int count) {
        int n = Math.max(1, Math.min(MAX_SWORDS, count));
        double[] out = new double[n];
        if (n == 1) return out;
        double total = Math.min(1.1, 0.16 * (n - 1)), step = total / (n - 1);
        for (int i = 0; i < n; i++) out[i] = (i - (n - 1) / 2.0) * step;
        return out;
    }

    // ---------------------------------------------------------------- stomp wave, spikes, pull
    public static final double WAVE_START = 0.5, WAVE_SPEED = 1.1, WAVE_THICK = 1.3, WAVE_JUMP = 0.75;

    public static double waveRadius(int age) { return WAVE_START + Math.max(0, age) * WAVE_SPEED; }

    /** Ticks a ring wave of the given reach lives. */
    public static int waveTicks(double reach) { return (int) Math.ceil((reach + WAVE_THICK - WAVE_START) / WAVE_SPEED) + 1; }

    /** Is something at 'dist' from the centre being passed by a wave of radius 'waveR' right now? */
    public static boolean waveHits(double dist, double waveR) { return Math.abs(dist - waveR) <= WAVE_THICK; }

    /** A low wave is jumped over by feet this far above the floor. */
    public static boolean jumpsWave(double feetAboveFloor) { return feetAboveFloor > WAVE_JUMP; }

    /** Ticks after the stone spikes start before they reach a point 'distance' blocks along the line (one block per two ticks). */
    public static int spikeDelay(double distance) { return (int) Math.max(0, Math.min(60, Math.round(distance * 2))); }

    /** The pull's speed (blocks per tick) for something 'dist' away, at a strength of 0.5 to 2: farther things are flung harder, never past 2.2. */
    public static double pullSpeed(double dist, double strength) {
        double s = Math.max(0.5, Math.min(2.0, strength));
        return Math.min(2.2, Math.min(1.1, 0.25 + 0.04 * Math.max(0, dist)) * s);
    }

    // ---------------------------------------------------------------- the Gravity Singularity
    /** Ticks from the spheres appearing to the collapse, and the last of them in which the pull drags foes in. */
    public static final int GATHER = 30, PULL = 18, MAX_MARKS = 5;
    /** How far past the zone the pull reaches. */
    public static final double PULL_REACH = 5;
    /** A foe whose feet are higher than this above the floor when the sphere collapses has jumped it. */
    public static final double JUMP_CLEARANCE = 1.1;

    /** Radius of the marked zone for a singularity power (1 = Dante at 80%; 0.5 .. 2): 6.25 at power 0.5, 7 at 1, 8.5 at 2. */
    public static double zoneRadius(double power) { return 5.5 + 1.5 * Math.max(0.5, Math.min(2.0, power)); }

    /** Base damage of the collapse (before the boss's damage law): 17 at power 0.5, 34 at 1, 68 at 2. */
    public static double collapseDamage(double power) { return 34.0 * Math.max(0.5, Math.min(2.0, power)); }

    /** How many spheres: none under 60% power, 2 up to 79, 3 up to 99, 4 once the Gates are open. */
    public static int markCount(int bossPower) {
        if (bossPower < 60) return 0;
        return bossPower >= PowerCurve.FULL ? 4 : bossPower >= 80 ? 3 : 2;
    }

    /** The factor on a collapse for someone who guards: a raised shield cuts 65%, anti-magic 60% (the better of the two applies). */
    public static double mitigation(boolean blocking, boolean antiMagic) {
        double f = 1.0;
        if (blocking) f = Math.min(f, 0.35);
        if (antiMagic) f = Math.min(f, 0.40);
        return f;
    }

    /** Did a foe whose feet are 'feetAboveFloor' above the floor jump the collapse? */
    public static boolean jumpsCollapse(double feetAboveFloor) { return feetAboveFloor > JUMP_CLEARANCE; }

    /**
     * Where the spheres go: the first on the primary target at (tx, tz), then on the other foes ('foes' = {x, z} pairs in priority order,
     * skipped when closer than one radius to a mark already placed), then the rest on a ring round the primary target at 2.35 radii
     * (turned by 'phase'), skipping any spot closer than one radius to a mark. Never more than 'n' marks, never more than
     * {@link #MAX_MARKS}. Each result is {x, z}.
     */
    public static List<double[]> planMarks(int n, double tx, double tz, double[][] foes, double radius, double phase) {
        int want = Math.max(0, Math.min(MAX_MARKS, n));
        List<double[]> out = new ArrayList<>();
        if (want == 0) return out;
        out.add(new double[]{tx, tz});
        if (foes != null) {
            for (double[] f : foes) {
                if (out.size() >= want) break;
                if (f == null || f.length < 2 || nearAny(out, f[0], f[1], radius)) continue;
                out.add(new double[]{f[0], f[1]});
            }
        }
        double ring = radius * 2.35 + 0.5;
        int slots = Math.max(1, want - 1);
        for (int k = 0; k < slots * 2 && out.size() < want; k++) {
            double a = phase + (k % slots + 0.5 * (k / slots)) * Math.PI * 2 / slots;
            double x = tx + Math.cos(a) * ring, z = tz + Math.sin(a) * ring;
            if (!nearAny(out, x, z, radius)) out.add(new double[]{x, z});
        }
        return out;
    }

    private static boolean nearAny(List<double[]> marks, double x, double z, double minDist) {
        for (double[] m : marks) {
            double dx = m[0] - x, dz = m[1] - z;
            if (dx * dx + dz * dz < minDist * minDist) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the bite
    public static final int CRATER_CAP = 420;

    /** Radius in blocks of the crater a sphere bites: 4 at power 0.5, 5 at 1, 6 at 2. */
    public static int craterRadius(double power) { return (int) Math.min(6, Math.round(3.5 + 1.5 * Math.max(0.5, Math.min(2.0, power)))); }

    /** How deep below the surface the bite reaches (a bowl, never a pit): about half the radius. */
    public static int craterDepth(int radius) { return Math.max(1, (radius + 1) / 2); }

    /**
     * The cells of the bite relative to the surface cell (the air block over the ground): a ball of 'radius' centred there, from
     * 'depth' below to 'radius' above, ordered from the centre outwards so the hole opens like a mouth. Deterministic. Each call
     * builds a fresh array (a sphere of radius 6 is a few hundred cells, and a singularity asks a handful of times).
     */
    public static int[][] crater(int radius, int depth) {
        int r = Math.max(1, Math.min(8, radius)), d = Math.max(1, Math.min(r, depth));
        List<int[]> cells = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -d; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++) {
                    int dist2 = dx * dx + dy * dy + dz * dz;
                    if (dist2 <= r * r) cells.add(new int[]{dist2, dy, dx, dz});
                }
        cells.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : a[1] != b[1] ? Integer.compare(a[1], b[1])
                : a[2] != b[2] ? Integer.compare(a[2], b[2]) : Integer.compare(a[3], b[3]));
        int[][] out = new int[cells.size()][];
        for (int i = 0; i < out.length; i++) {
            int[] c = cells.get(i);
            out[i] = new int[]{c[2], c[1], c[3]};
        }
        return out;
    }
}
