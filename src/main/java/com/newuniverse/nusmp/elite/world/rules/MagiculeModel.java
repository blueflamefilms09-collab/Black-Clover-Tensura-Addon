package com.newuniverse.nusmp.elite.world.rules;

/**
 * Why: the "magicule density" of a chunk is the number every other part of the world engine reads (crystals grow with it, ores convert in it, mobs get touched by it, a calamity wakes in it).
 * The original source is not available, so this is the mod's own design, written as a pure model (no Minecraft types) so it can be unit tested and tuned in one place.
 *
 * <p>The stored value of a chunk is a number in 0..1 that is MOVED toward a target, never set: {@code target = dimension floor + biome term + crystal bonus + event}.
 * The event term is a signed bump that decays back to 0 (a fallen calamity, a harvested Mother Rock). Depth is not stored: it is added when the density is READ at a height
 * ({@link #effective}), because a chunk is a column and the deep part of it is richer than the surface of the same column.
 */
public final class MagiculeModel {
    private MagiculeModel() {}

    /** The floor every chunk of the allowed dimensions has, before biome and crystals. */
    public static final double DIMENSION_FLOOR = 0.10;
    /** The most the biome can add. */
    public static final double BIOME_SPAN = 0.30;
    /** The most nearby crystal growth can add, and how much one point of crystal score is worth. */
    public static final double CRYSTAL_CAP = 0.25, CRYSTAL_PER_POINT = 0.01;
    /** The most Mother Rocks can add. */
    public static final double MOTHER_CAP = 0.30;
    /** The event term is kept inside this range. */
    public static final double EVENT_MIN = -0.5, EVENT_MAX = 0.8;
    /** Depth: blocks under sea level for the full bonus, and the bonus itself. */
    public static final int DEPTH_FULL = 96;
    public static final double DEPTH_BONUS = 0.20;

    public static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        return v < 0.0 ? 0.0 : Math.min(1.0, v);
    }

    public static double clampEvent(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(EVENT_MIN, Math.min(EVENT_MAX, v));
    }

    /** What the biome adds, from a 0..1 biome factor (see the biome table). */
    public static double biomeTerm(double biomeFactor) { return BIOME_SPAN * clamp01(biomeFactor); }

    /** What crystal growth nearby adds: the crystal score of the chunk (buds weigh 1, 2, 3, clusters 5) and the Mother Rocks' own bonus. */
    public static double crystalBonus(int crystalScore, double motherBonus) {
        double c = Math.min(CRYSTAL_CAP, Math.max(0, crystalScore) * CRYSTAL_PER_POINT);
        double m = Math.min(MOTHER_CAP, Math.max(0.0, motherBonus));
        return c + m;
    }

    /** The value the density moves toward. */
    public static double target(double dimensionFloor, double biomeFactor, int crystalScore, double motherBonus, double event) {
        return clamp01(dimensionFloor + biomeTerm(biomeFactor) + crystalBonus(crystalScore, motherBonus) + clampEvent(event));
    }

    /** The extra density the deep part of a column has: 0 at and above sea level, the full bonus {@value #DEPTH_FULL} blocks under it. */
    public static double depthBonus(int y, int seaLevel) {
        int depth = seaLevel - y;
        if (depth <= 0) return 0.0;
        return DEPTH_BONUS * Math.min(1.0, depth / (double) DEPTH_FULL);
    }

    /** The density read at a height: the stored chunk value plus the depth bonus, 0..1. */
    public static double effective(double stored, int y, int seaLevel) { return clamp01(stored + depthBonus(y, seaLevel)); }

    /**
     * One update step: the stored value moves toward the target by at most {@code ratePerMinute * minutes} (rising and falling have their own rates, falling is faster so a
     * destroyed grove fades quickly, rising is slow so a calamity needs a long time). Never overshoots.
     */
    public static double step(double current, double target, double seconds, double risePerMinute, double fallPerMinute) {
        double cur = clamp01(current), tgt = clamp01(target);
        if (seconds <= 0.0) return cur;
        double minutes = seconds / 60.0;
        if (tgt > cur) return Math.min(tgt, cur + Math.max(0.0, risePerMinute) * minutes);
        if (tgt < cur) return Math.max(tgt, cur - Math.max(0.0, fallPerMinute) * minutes);
        return cur;
    }

    /** The event term decays toward 0 by {@code decayPerMinute * minutes}; it never crosses 0. */
    public static double decayEvent(double event, double seconds, double decayPerMinute) {
        double e = clampEvent(event);
        double d = Math.max(0.0, decayPerMinute) * Math.max(0.0, seconds) / 60.0;
        if (e > 0.0) return Math.max(0.0, e - d);
        if (e < 0.0) return Math.min(0.0, e + d);
        return 0.0;
    }

    // ---------------------------------------------------------------- wire format

    /** 0..1 to a byte value 0..255 (the sync sends this, not a double). */
    public static int quantize(double d) { return (int) Math.round(clamp01(d) * 255.0); }

    public static double dequantize(int q) { return Math.max(0, Math.min(255, q)) / 255.0; }

    /** Is the change worth a packet? The throttle of the sync: a change of at least {@code minDelta} of 255. */
    public static boolean needsSync(int lastSent, int now, int minDelta) {
        if (lastSent < 0) return true;
        return Math.abs(now - lastSent) >= Math.max(1, minDelta);
    }

    // ---------------------------------------------------------------- bands (names for the HUD and the commands)

    public enum Band {
        THIN("thin", 0.0), NORMAL("normal", 0.15), RICH("rich", 0.40), DENSE("dense", 0.65), OVERFLOWING("overflowing", 0.85);

        public final String id;
        public final double from;

        Band(String id, double from) {
            this.id = id;
            this.from = from;
        }

        public static Band of(double density) {
            double d = clamp01(density);
            Band b = THIN;
            for (Band x : values()) if (d >= x.from) b = x;
            return b;
        }
    }
}
