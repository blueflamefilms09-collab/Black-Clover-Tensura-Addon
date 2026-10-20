package com.newuniverse.nusmp.particle.fx;

/**
 * Why: the curves and counters of the particle library, without a single game class, so they are unit-tested and so the particle class itself is only glue: the size over the life, the alpha
 * over the life, which animation frame a particle shows at an age, how many particles a request may spawn after the quality and the per-call cap, and the clamp that keeps a stray huge
 * velocity (a careless call site) from throwing a particle across the map.
 */
public final class FxMath {
    private FxMath() {}

    /** The largest velocity component (blocks per tick) a particle may have. */
    public static final double MAX_SPEED = 0.9;

    public static double clampSpeed(double v) {
        return v > MAX_SPEED ? MAX_SPEED : (v < -MAX_SPEED ? -MAX_SPEED : v);
    }

    /** The full width of the quad at life fraction {@code t} (0 at birth, 1 at death). */
    public static float sizeAt(FxProfile.Curve curve, float start, float end, float t) {
        t = t < 0f ? 0f : (t > 1f ? 1f : t);
        switch (curve) {
            case POP: {                                     // swells to 1.15x start in the first fifth, then glides to the end size
                if (t < 0.2f) return start * (0.35f + 0.8f * (t / 0.2f));
                float u = (t - 0.2f) / 0.8f;
                return lerp(start * 1.15f, end, u);
            }
            case EASE_OUT: {
                float u = 1f - (1f - t) * (1f - t);
                return lerp(start, end, u);
            }
            case PULSE:
                return lerp(start, end, t) * (1f + 0.18f * (float) Math.sin(t * 6.2831855f * 2f));
            case HOLD:                                      // keeps its size for three quarters of the life, then goes to the end size
                return t < 0.75f ? start : lerp(start, end, (t - 0.75f) / 0.25f);
            default:
                return lerp(start, end, t);
        }
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** The alpha at life fraction {@code t}: rises over the first {@code fadeIn}, falls over the last {@code fadeOut}. */
    public static float alphaAt(float fadeIn, float fadeOut, float t) {
        float a = 1f;
        if (fadeIn > 0f && t < fadeIn) a = Math.min(a, t / fadeIn);
        if (fadeOut > 0f && t > 1f - fadeOut) a = Math.min(a, (1f - t) / fadeOut);
        return a < 0f ? 0f : (a > 1f ? 1f : a);
    }

    /**
     * The animation frame (0-based) a particle shows. AGE: the frames are spread evenly over the life; LOOP: one step every {@code loopTicks} ticks, starting at a random {@code phase}, wrapping;
     * RANDOM: a fixed frame chosen at birth ({@code phase}).
     */
    public static int frameAt(FxProfile.Anim anim, int age, int life, int frames, int loopTicks, int phase) {
        if (frames <= 1) return 0;
        switch (anim) {
            case AGE: {
                int f = (int) ((long) age * frames / Math.max(1, life));
                return f >= frames ? frames - 1 : Math.max(f, 0);
            }
            case RANDOM:
                return Math.floorMod(phase, frames);
            default:
                return Math.floorMod(age / Math.max(1, loopTicks) + phase, frames);
        }
    }

    /**
     * How many particles a request for {@code wanted} may spawn: scaled by the quality's detail (1.0 high, 0.75 medium, 0.5 low; further reduced by {@code distanceDetail} on the client), never above
     * {@code perCallCap}, and at least one when anything was wanted and the detail is above zero.
     */
    public static int scaledCount(int wanted, float detail, float distanceDetail, int perCallCap) {
        if (wanted <= 0 || detail <= 0f || distanceDetail <= 0f) return 0;
        int n = Math.min(wanted, Math.max(1, perCallCap));
        float f = detail * distanceDetail;
        if (f >= 0.999f) return n;
        return Math.max(1, Math.round(n * f));
    }

    /** The per-call cap of a quality level (low / medium / high), so a 400-particle spell still costs a bounded packet on a weak machine. */
    public static int perCallCap(String qualityName) {
        if (qualityName == null) return 400;
        switch (qualityName.toLowerCase(java.util.Locale.ROOT)) {
            case "low": return 48;
            case "medium": return 140;
            default: return 400;
        }
    }
}
