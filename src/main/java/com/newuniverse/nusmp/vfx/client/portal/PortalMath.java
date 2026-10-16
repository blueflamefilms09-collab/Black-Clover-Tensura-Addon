package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: every number that shapes the 3D portal (how it opens and closes, where the nested shells sit, how they slide when you look
 * through at an angle, how fast the horizon swirls) lives here as plain float maths, so it is the same in the game, the headless
 * preview and the unit tests, and the layer code only has to place vertices.
 */
public final class PortalMath {
    private PortalMath() {}

    /** Ticks the gate takes to open and to collapse. */
    public static final int OPEN_TICKS = 12, CLOSE_TICKS = 8;

    private static float clamp01(float x) { return x < 0 ? 0 : x > 1 ? 1 : x; }
    private static float easeOutCubic(float t) { float u = 1 - clamp01(t); return 1 - u * u * u; }
    private static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.2f, c3 = c1 + 1, u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    /** 0..1 progress of the closing collapse for a gate that lives {@code duration} ticks, 0 while it is not closing yet. */
    public static float closing(float age, float duration) {
        return clamp01((age - (duration - CLOSE_TICKS)) / CLOSE_TICKS);
    }

    /**
     * Horizontal opening of the gate (multiplies half the width). Opening: a thin vertical slit first, then it widens (slightly
     * overshooting at the end). Closing: the width collapses first, so the gate ends as a line and then a point.
     */
    public static float openW(float age, float duration) {
        float c = closing(age, duration);
        if (c > 0) return 1 - easeOutCubic(c / 0.7f);
        return easeOutBack(clamp01((age / OPEN_TICKS - 0.25f) / 0.75f));
    }

    /** Vertical opening (multiplies half the height): the slit grows tall first, and shrinks last. */
    public static float openH(float age, float duration) {
        float c = closing(age, duration);
        if (c > 0) return 1 - easeOutCubic((c - 0.3f) / 0.7f);
        return easeOutCubic(age / (OPEN_TICKS * 0.6f));
    }

    /** Overall brightness while opening and closing (a gate that is not open yet is faint). */
    public static float fade(float age, float duration) {
        float open = clamp01(age / 4f), c = closing(age, duration);
        return open * (1 - c * c);
    }

    // ------------------------------------------------------------------ depth shells

    /** Scale of shell i of n (0 = nearest the opening): they shrink towards the far end of the tunnel. */
    public static float shellScale(int i, int n) {
        return 1f - 0.78f * (i + 1) / (n + 1);
    }

    /** How deep behind the opening shell i sits, in portal radii. */
    public static float shellDepth(int i) {
        return 0.4f * (i + 1);
    }

    /**
     * Parallax of a shell: where a point {@code depth} behind the middle of the opening shows up on the opening's plane for a viewer
     * at (vx, vy, vz) in the gate's own axes (x right, y up, z towards the front; same units as depth). The line from the viewer to
     * the point crosses the plane at {@code v.xy * depth / (|vz| + depth)}, so a viewer far to the side sees the deep shells slide
     * towards him, and one looking straight in sees them centred. The result is limited to {@code maxX}, {@code maxY} (an ellipse)
     * so the shells never leave the opening.
     *
     * @param out receives {x, y}
     */
    public static void parallax(float vx, float vy, float vz, float depth, float maxX, float maxY, float[] out) {
        float z = Math.max(Math.abs(vz), 0.25f);
        float k = depth / (z + depth);
        float x = vx * k, y = vy * k;
        if (maxX > 1e-6f && maxY > 1e-6f) {
            float e = (x / maxX) * (x / maxX) + (y / maxY) * (y / maxY);
            if (e > 1f) { float s = (float) (1.0 / Math.sqrt(e)); x *= s; y *= s; }
        } else { x = 0; y = 0; }
        out[0] = x; out[1] = y;
    }

    // ------------------------------------------------------------------ swirl, rim

    /** Extra turn (in turns of the texture, 0..1 = one full lap) the swirl adds at radius r (0 centre .. 1 rim) after t ticks: inner parts spin faster. */
    public static float swirlTurns(float r, float ticks) {
        return ticks * 0.012f * (1.7f - clamp01(r));
    }

    /** Deterministic hash to [0, 1) for jitter that must be the same on every client. */
    public static float hash(long seed, int a, int b) {
        long h = seed * 0x9E3779B97F4A7C15L + a * 0xC2B2AE3D27D4EB4FL + b * 0x165667B19E3779F9L;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return (h & 0xFFFFFFL) / (float) 0x1000000;
    }

    /** Radial jitter (-1..1) of one rim-lightning point; changes about ten times a second so the arcs crawl instead of sliding. */
    public static float boltJitter(long seed, int bolt, int point, float ticks) {
        int step = (int) Math.floor(ticks * 0.5f);
        return hash(seed + step * 31L, bolt, point) * 2f - 1f;
    }

    /** Whether rim bolt {@code bolt} is lit in the current flicker step. */
    public static boolean boltLit(long seed, int bolt, float ticks) {
        int step = (int) Math.floor(ticks * 0.5f);
        return hash(seed ^ 0x5DEECE66DL, bolt, step) < 0.55f;
    }

    /** Glyph (0..3) of rune segment i. */
    public static int glyph(long seed, int i) {
        return (int) (hash(seed, i, 77) * 4f) & 3;
    }
}
