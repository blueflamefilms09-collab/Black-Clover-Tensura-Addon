package com.newuniverse.nusmp.boss.bars;

/**
 * Why: the numbers of the custom bar's geometry and motion in one pure place, so the painter only places quads and the tests (and the python mock-up, whose layout block in {@code _index.json}
 * is compared with these constants) pin the shapes. Units are GUI pixels of the bar at scale 1; the art is stored at twice that.
 */
public final class BarLayoutMath {
    private BarLayoutMath() {}

    /** The widget: 256 x 42 (name plate, frame, fill window) plus the under-row for the wind-up and the small texts. */
    public static final int WIDTH = 256, FRAME_H = 42, FULL_H = 54, COMPACT_H = 42;
    /** The step from one bar to the next (the bar and a gap). */
    public static final int STEP = 58, COMPACT_STEP = 46;
    /** The fill window: x, y, width, height. */
    public static final int WIN_X = 38, WIN_Y = 25, WIN_W = 196, WIN_H = 10;
    /** The socket (portrait / emblem): centre and radius. */
    public static final int SOCK_X = 18, SOCK_Y = 24, SOCK_R = 15;
    /** The name plate: x, y, width, height. */
    public static final int PLATE_X = 40, PLATE_Y = 2, PLATE_W = 192, PLATE_H = 17;
    /** The cast bar under the window and the row of small texts under it. */
    public static final int CAST_Y = 43, CAST_H = 3, ROW_Y = 47;
    /** The atlas of one style: size and the y of its three layers (back, frame, glow); each layer is 84 high (2x the widget). */
    public static final int ATLAS_W = 512, ATLAS_H = 268, LAYER_H = 84;
    public static final int[] LAYER_Y = {0, 92, 184};
    public static final int EMBLEM_COLS = 8, EMBLEM_CELL = 32, FX_W = 64, FX_ROW_H = 16;

    public static final int ENTER_TICKS = 24, DEATH_TICKS = 36, DYING_KEEP_TICKS = 60;

    /** Vertical position cursor rule: bars are laid down until the next would start below this share of the screen height (a bit more than vanilla's third: our bars are taller). */
    public static final float MAX_SCREEN_SHARE = 0.45f;

    /** Few bosses: the full bar; three or more at once: compact bars (no under-row) so the stack stays short. */
    public static boolean compact(int bars) { return bars >= 3; }

    public static int step(boolean compact) { return compact ? COMPACT_STEP : STEP; }

    public static boolean fits(int y, int guiHeight) { return y < guiHeight * MAX_SCREEN_SHARE; }

    /** The pixels of the fill for a health share 0..1: at least one while anything is left, the whole window at 1. */
    public static int fillPixels(float hp) {
        float p = Math.max(0f, Math.min(1f, hp));
        int w = Math.round(p * WIN_W);
        return p > 0f && w < 1 ? 1 : w;
    }

    /** The x (relative to the window) of a phase marker at {@code percent} of health. */
    public static int markerX(int percent) { return Math.round(Math.max(0, Math.min(100, percent)) * WIN_W / 100f); }

    /** Whole percent for the text: a bar with anything left never reads 0. */
    public static int percent(float hp) {
        float p = Math.max(0f, Math.min(1f, hp));
        int v = Math.round(p * 100f);
        return p > 0f && v == 0 ? 1 : v;
    }

    /** Ease-out for the entrance, 0..1. */
    public static float easeOut(float k) {
        float c = Math.max(0f, Math.min(1f, k));
        return 1f - (1f - c) * (1f - c) * (1f - c);
    }

    /** The share of the entrance done at {@code age} ticks. */
    public static float entrance(double age) { return easeOut((float) (age / ENTER_TICKS)); }

    /** Low health pulse: 0 above 25 %, a growing red pulse below it, faster the lower the health. Returns 0..1. */
    public static float lowPulse(float hp, double time) {
        if (hp <= 0f || hp >= 0.25f) return 0f;
        double rate = 0.18 + (0.25 - hp) * 1.4;                                       // radians per tick
        return (float) (0.5 + 0.5 * Math.sin(time * rate * 2.0)) * (0.35f + 0.65f * (1f - hp / 0.25f));
    }

    /** The slow breathing of the frame glow, 0.55..1; faster when the boss is enraged. */
    public static float glowPulse(double time, boolean enraged) { return (float) (0.775 + 0.225 * Math.sin(time * (enraged ? 0.45 : 0.16))); }

    /** The offset in pixels of one of {@code cols} strips of a shattering bar at progress {@code k} (0..1): flies out for half the time, comes back for the rest; deterministic per strip. */
    public static float shatterDx(int col, int cols, float k, long seed) {
        float out = k < 0.5f ? k * 2f : 2f - k * 2f;
        float dir = (col - (cols - 1) / 2f) / Math.max(1f, cols / 2f);
        return dir * 26f * out * (0.7f + 0.6f * unit(mix(seed + col * 0x9E3779B97F4A7C15L)));
    }

    public static float shatterDy(int col, float k, long seed) {
        float t = Math.min(1f, k * 1.4f);
        return 18f * t * t * (0.5f + unit(mix(seed ^ (col * 0xC2B2AE3D27D4EB4FL))));
    }

    /** SplitMix64 finaliser. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static float unit(long h) { return (h >>> 40) / (float) (1L << 24); }

    /** How many dots of a ring of {@code dots} are still lit when {@code ticksLeft} of {@code total} remain. */
    public static int ringDots(int dots, int ticksLeft, int total) {
        if (total <= 0) return 0;
        return Math.max(0, Math.min(dots, (int) Math.ceil(dots * (ticksLeft / (double) total))));
    }
}
