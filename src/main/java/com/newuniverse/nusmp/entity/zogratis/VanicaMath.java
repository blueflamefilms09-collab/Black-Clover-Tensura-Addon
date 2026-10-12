package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The arithmetic behind Vanica's moves (pure, no Minecraft types, unit tested): lance fans, blood drain and its limiter, the
 * Blood Beasts' numbers, how a rotting field spreads (radius over time, the ring of columns each step reaches), the curse's levels,
 * where the Crimson Rain falls and the Crimson Crown's thorns stand, and the little geometry the scythe, whip and dash share.
 * Everything that depends on her power takes the percent (20 .. 80, 100 at the Gates) like {@link PowerCurve} does.
 */
public final class VanicaMath {
    private VanicaMath() {}

    // ---------------------------------------------------------------- tunables
    public static final int MAX_LANCES = 6, LANCE_LIFE = 24, LANCE_STAGGER = 2, ROT_CAP = 360, HARD_BEAST_CAP = 14, SPAWN_TICKS = 24;
    public static final double LANCE_SPEED = 1.6, LANCE_REACH = 0.9, DASH_MAX = 22, DASH_WIDTH = 1.8, WHIP_LENGTH = 13, WHIP_WIDTH = 1.3,
            WHIP_STOP = 2.2, SCYTHE_REACH = 5.2, SCYTHE_HALF_ANGLE = Math.toRadians(75), DRAIN_PER_HIT_CAP = 0.01, DRAIN_WINDOW_CAP = 0.04;
    public static final int DRAIN_WINDOW = 40;
    /** The Crimson Rain: each spot has this radius, the scatter starts this far from the target and spots keep this far apart. */
    public static final double RAIN_RADIUS = 2.3, RAIN_RING = 4.2, RAIN_SPACING = 3.4;

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }

    /** 0 at the opening power, 1 at the Gates: how much of the devil's power is drawn. */
    public static double fill(int power) { return clamp((power - PowerCurve.OPENING) / (double) (PowerCurve.FULL - PowerCurve.OPENING), 0, 1); }

    private static boolean gates(int power) { return PowerCurve.gatesOpen(power); }

    // ---------------------------------------------------------------- blood lances
    /** Lances in one volley: the power's volley size plus one, and one more at the Gates ("blood lances multiply"). */
    public static int lanceCount(int castSlots, boolean gates) {
        return (int) clamp(castSlots + 1 + (gates ? 1 : 0), 2, MAX_LANCES);
    }

    /** Angles (radians, left to right) of a fan of 'n' lances: 9 degrees apart, centred on the aim. */
    public static double[] lanceFan(int n) {
        int c = (int) clamp(n, 1, MAX_LANCES);
        double[] out = new double[c];
        for (int i = 0; i < c; i++) out[i] = (i - (c - 1) / 2.0) * Math.toRadians(9);
        return out;
    }

    // ---------------------------------------------------------------- blood drain
    /** The share of the damage she deals that comes back to her as health: 15% at the opening, 40% at the Gates. */
    public static double drainShare(int power) { return 0.15 + 0.25 * fill(power); }

    /** What one hit of 'dealt' damage heals her: the share of it, at most 1% of her max health (2% in a frenzy, when the share doubles too). */
    public static double drainHeal(double dealt, double maxHealth, int power, boolean frenzy) {
        double f = frenzy ? 2 : 1;
        return Math.max(0, Math.min(dealt * drainShare(power) * f, maxHealth * DRAIN_PER_HIT_CAP * f));
    }

    /** Caps what drain may return in any {@link #DRAIN_WINDOW} ticks: 4% of her max health (8% in a frenzy), so a many-hit move never refills her. */
    public static final class Drain {
        private long windowStart = Long.MIN_VALUE / 2;
        private double used;

        /** How much of 'want' she may take at tick 'now'. */
        public double take(long now, double want, double maxHealth, boolean frenzy) {
            if (now - windowStart >= DRAIN_WINDOW) {
                windowStart = now;
                used = 0;
            }
            double room = Math.max(0, maxHealth * DRAIN_WINDOW_CAP * (frenzy ? 2 : 1) - used);
            double got = Math.max(0, Math.min(want, room));
            used += got;
            return got;
        }

        public void reset() {
            windowStart = Long.MIN_VALUE / 2;
            used = 0;
        }
    }

    // ---------------------------------------------------------------- blood beasts
    /** Most Blood Beasts a boss keeps alive from ordinary summons: 2 at the opening, +1 per step, 8 at the Gates. */
    public static int beastCap(int power) { return gates(power) ? 8 : 2 + PowerCurve.stage(power); }

    /** Beasts one Blood Beasts move brings: the volley size plus one, never over the cap. */
    public static int beastsPerCast(int castSlots, int power) { return Math.min(beastCap(power), Math.max(1, castSlots + 1)); }

    /** Beasts of the Crimson Crown's stampede: extra to the ordinary cap, and with a full cap alive still under {@link #HARD_BEAST_CAP} in all. */
    public static int stampedeCount(int power) { return gates(power) ? 6 : 2 + PowerCurve.stage(power); }

    /** How many of a stampede of 'want' may come while 'alive' beasts of the boss stand: never over {@link #HARD_BEAST_CAP} in all. */
    public static int stampedeRoom(int want, int alive) { return Math.max(0, Math.min(want, HARD_BEAST_CAP - Math.max(0, alive))); }

    public static double beastHealth(int power, boolean stampede) {
        return (28 + 92 * fill(power)) * (gates(power) ? 1.35 : 1.0) * (stampede ? 0.6 : 1.0);
    }

    public static double beastAttack(int power) { return (4 + 4 * fill(power)) * (gates(power) ? 1.2 : 1.0); }
    public static double beastSpeed(int power, boolean stampede) { return 0.31 + 0.07 * fill(power) + (stampede ? 0.05 : 0); }
    public static double beastArmor(int power) { return 2 + 6 * fill(power); }
    /** Ticks between two bites. */
    public static int biteCooldown(int power) { return (int) Math.round(24 - 8 * fill(power)); }
    /** Ticks of bleeding (Wither) one bite leaves. */
    public static int bleedTicks(int power) { return (int) Math.round(60 + 60 * fill(power)); }
    /** Ticks between two pounces. */
    public static int pounceCooldown(int power) { return (int) Math.round(90 - 30 * fill(power)); }
    /** Ticks a beast lives: a minute, or ten seconds for the Crown's stampede. */
    public static int beastLife(boolean stampede) { return stampede ? 220 : 1200; }

    // ---------------------------------------------------------------- the rotting field
    /** Blocks the rot has reached 'age' ticks into a field that grows to 'max' blocks over 'spread' ticks (fast at first, then it settles). */
    public static double rotRadius(int age, double max, int spread) {
        double t = clamp(age / (double) Math.max(1, spread), 0, 1);
        double ease = 1 - (1 - t) * (1 - t);
        return Math.min(max, 1.5 + (max - 1.5) * ease);
    }

    /** Ticks a field takes to reach its full size: three seconds at the opening, under a second and a half at the Gates. */
    public static int spreadTicks(int power) { return (int) Math.round(60 - 32 * fill(power)); }

    /** The full radius of a rotting field. */
    public static double rotMaxRadius(int power) { return Math.min(16, (6.5 + PowerCurve.stage(power)) * (gates(power) ? 1.3 : 1.0)); }

    /** How long a rotting field lasts. */
    public static int rotTicks(int power) { return 160 + 20 * PowerCurve.stage(power); }

    /**
     * The block columns (dx, dz) with r0 &lt; distance &lt;= r1 from the centre, nearest first. Pass r0 below 0 to include the centre.
     * Each step of a spreading field takes the band between the old radius and the new one, so a column is visited once.
     */
    public static List<int[]> band(double r0, double r1) {
        List<int[]> out = new ArrayList<>();
        if (r1 <= 0 && r0 >= 0) return out;
        int n = (int) Math.ceil(Math.max(0, r1));
        double lo = r0 < 0 ? -1 : r0 * r0, hi = r1 * r1;
        for (int dx = -n; dx <= n; dx++)
            for (int dz = -n; dz <= n; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 > lo && d2 <= hi) out.add(new int[]{dx, dz});
            }
        out.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        return out;
    }

    // ---------------------------------------------------------------- the decay curse
    /** The curse's level: 1 below 60% power, 2 from 60%, 3 at the Gates. */
    public static int curseLevel(int power) { return gates(power) ? 3 : power >= PowerCurve.INFINITE_MANA_FROM ? 2 : 1; }

    /** Ticks a brand lasts. */
    public static int curseTicks(int power) { return 200 + 20 * PowerCurve.stage(power); }

    /** Weakness amplifier: Weakness I at level 1, Weakness II from level 2. */
    public static int weaknessAmp(int level) { return level <= 1 ? 0 : 1; }

    /** What is left of a heal while branded: 60% at level 1, 40%, then 25%. */
    public static double healFactor(int level) { return level <= 1 ? 0.6 : level == 2 ? 0.4 : 0.25; }

    /** Radius of the sigil under a brand. */
    public static double sigilRadius(int level) { return 4.0 + 0.5 * clamp(level, 1, 3); }

    // ---------------------------------------------------------------- where things fall and stand
    /**
     * The spots of the Crimson Rain: one on the target, one on each other foe (nearest first, as given), the rest scattered round the
     * target on a golden-angle spiral from 'ring' blocks out. No two are closer than 'spacing'; fewer than 'count' come back when
     * they do not fit. Each spot is {x, z}.
     */
    public static List<double[]> planSpots(int count, double tx, double tz, double[][] others, double ring, double spacing, double phase) {
        List<double[]> out = new ArrayList<>();
        if (count <= 0) return out;
        out.add(new double[]{tx, tz});
        if (others != null)
            for (double[] o : others) {
                if (out.size() >= count) break;
                if (apart(out, o[0], o[1], spacing)) out.add(new double[]{o[0], o[1]});
            }
        for (int i = 0, tries = 0; out.size() < count && tries < 6 * count; i++, tries++) {
            double a = phase + i * 2.399963229728653, r = ring + 1.4 * (i % 3);
            double x = tx + Math.cos(a) * r, z = tz + Math.sin(a) * r;
            if (apart(out, x, z, spacing)) out.add(new double[]{x, z});
        }
        return out;
    }

    private static boolean apart(List<double[]> spots, double x, double z, double spacing) {
        for (double[] s : spots) if ((s[0] - x) * (s[0] - x) + (s[1] - z) * (s[1] - z) < spacing * spacing) return false;
        return true;
    }

    /** Thorns of the Crimson Crown: 8 at the opening of the ultimate, up to 14 at the Gates. */
    public static int thornCount(int power) { return gates(power) ? 14 : 8 + (int) Math.round(4 * fill(power)); }

    /** 'count' points evenly round a circle of 'radius' blocks, starting at angle 'phase'. Each point is {x, z}. */
    public static List<double[]> thornRing(int count, double cx, double cz, double radius, double phase) {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = phase + i * Math.PI * 2 / Math.max(1, count);
            out.add(new double[]{cx + Math.cos(a) * radius, cz + Math.sin(a) * radius});
        }
        return out;
    }

    /** Radius of the Crimson Crown's thorn ring. */
    public static double crownRadius(int power) { return gates(power) ? 13 : 10.5; }

    // ---------------------------------------------------------------- geometry
    /** Is the offset (dx, dz) inside a flat fan facing (fx, fz): within 'reach' (+ a body margin) and within 'halfAngle' of the facing? Close in (under 1.2) always counts. */
    public static boolean inCone(double dx, double dz, double fx, double fz, double halfAngle, double reach, double margin) {
        double d = Math.hypot(dx, dz);
        if (d > reach + margin) return false;
        if (d < 1.2) return true;
        double f = Math.hypot(fx, fz);
        if (f < 1e-6) return true;
        return (dx * fx + dz * fz) / (d * f) >= Math.cos(halfAngle);
    }

    /** Distance from the point (px, pz) to the segment a-b. */
    public static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az, len2 = vx * vx + vz * vz;
        if (len2 < 1e-9) return Math.hypot(px - ax, pz - az);
        double t = clamp(((px - ax) * vx + (pz - az) * vz) / len2, 0, 1);
        return Math.hypot(px - (ax + vx * t), pz - (az + vz * t));
    }

    /** Where along the segment a-b (0 at a, 1 at b) the point (px, pz) projects: used to find the first foe on a line. */
    public static double alongSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az, len2 = vx * vx + vz * vz;
        return len2 < 1e-9 ? 0 : clamp(((px - ax) * vx + (pz - az) * vz) / len2, 0, 1);
    }

    /**
     * Horizontal speed (blocks per tick, 0.3 to 1.5) of the whip's yank for a victim 'distance' blocks from the boss. A victim thrown with
     * a quarter-block hop travels about 6.9 times its launch speed (eight airborne ticks at 0.91 drag, then the slide on the ground), so it
     * comes to rest near {@link #WHIP_STOP} blocks from her instead of flying past her.
     */
    public static double yankSpeed(double distance) { return clamp((distance - WHIP_STOP) / 6.9, 0.3, 1.5); }

    /** How far the dash goes toward a target 'distance' away: it stops 1.6 blocks short, at least 3 and at most {@link #DASH_MAX}. */
    public static double dashLength(double distance) { return clamp(distance - 1.6, 3, DASH_MAX); }
}
