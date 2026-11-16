package com.newuniverse.nusmp.vfx;

/**
 * Why (docs/vfx_roblox_pass.md): the pure half of the "Roblox style" VFX toolkit ({@code vfx/client/RobloxStyle}, {@code RobloxFx}). Roblox VFX artists build "GIF" effects out of a few layered textured meshes
 * (scrolling-UV swooshes, expanding rings, cones) and flip-book sprites, timed anticipation -> burst -> dissipate with ease curves, instead of piles of camera-facing sprites. This class holds what needs no game
 * type, so it is unit-tested: the client switch ({@code vfx.robloxStyle}, mirrored from the CLIENT config {@code nusmp-roblox-vfx-client.toml}), the texture list, the ease and timing curves, the flip-book cell maths, the
 * width taper of a ribbon, a deterministic hash, and the quad cost of every primitive (so the worst case per effect is a number the tests pin against the old effect's, not a guess).
 *
 * <p>Nothing here is kept between frames or allocates: every method is a plain function of its arguments.
 */
public final class RobloxMath {
    private RobloxMath() {}

    /** The CLIENT config file (looks only: nothing on the server depends on it). Pinned by ConfigFileNamesTest through the literal in RobloxStyleConfig. */
    public static final String CONFIG_FILE = "nusmp-roblox-vfx-client.toml";
    public static final String PREFIX = "rbx_";
    /** Every texture the generator ({@code tools/gen_roblox_vfx_textures.py}) makes, in its order. */
    public static final String[] TEXTURE_NAMES = {"swoosh", "bolt", "swirl", "ring", "decal_ring", "glow", "streak", "flip_burst", "flip_fire", "rock"};
    /** The flip-book sheets: name -> columns x rows (cells are square). */
    public static final String[] FLIPBOOKS = {"flip_burst", "flip_fire"};
    public static final int FLIP_COLS = 4, FLIP_ROWS = 4;

    /** The engine's own cap for one effect (VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION / 4). */
    public static final int ENGINE_MAX_QUADS = 100;

    public static String fileName(String name) { return PREFIX + name; }

    // ------------------------------------------------------------------ the switch
    /** true (default): the effects listed in {@link #SHAPES} are drawn with the toolkit; false: exactly the old layers (a client setting, looks only). */
    private static volatile boolean enabled = true;

    public static boolean enabled() { return enabled; }

    public static void setEnabled(boolean on) { enabled = on; }

    /** The shapes the toolkit redraws (each keeps its old drawing in its own layer, selectable by the setting). Order is the doc's. */
    public static final VfxShape[] SHAPES = {VfxShape.MAGIC_CIRCLE_EXPLOSION, VfxShape.KI_BEAM, VfxShape.KI_SHOCKWAVE, VfxShape.KI_BLAST, VfxShape.LIGHTNING_SPEAR, VfxShape.EARTH_SPIKES,
            VfxShape.ANTI_MAGIC_SLASH, VfxShape.WIND_TORNADO, VfxShape.ICE_FX3, VfxShape.WATER_BURST};

    public static boolean owns(VfxShape s) {
        for (VfxShape o : SHAPES) if (o == s) return true;
        return false;
    }

    // ------------------------------------------------------------------ curves (t = 0..1)
    public static float clamp01(float t) { return t < 0f ? 0f : Math.min(t, 1f); }

    /** (t - a) / (b - a), clamped: the progress of a sub-phase that runs from a to b. */
    public static float phase(float t, float a, float b) { return b <= a ? (t >= b ? 1f : 0f) : clamp01((t - a) / (b - a)); }

    public static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }

    public static float easeOut(float t) { float u = 1f - clamp01(t); return 1f - u * u * u; }

    public static float easeIn(float t) { t = clamp01(t); return t * t * t; }

    /** Fast then slow: the shock ring that shows the area at once and then lingers (quartic ease-out). */
    public static float shockRadius(float t) { float u = 1f - clamp01(t); return 1f - u * u * u * u; }

    /** A small overshoot on the way out (a pop), ending exactly on 1. */
    public static float easeOutBack(float t) { t = clamp01(t); float c = 1.70158f, u = t - 1f; return 1f + (c + 1f) * u * u * u + c * u * u; }

    /**
     * The three-beat timing of a burst: anticipation (0..{@code anticipation}: a slow draw-in, 0 -> 0.2), burst ({@code anticipation}..{@code peak}: fast ease-out up to 1), dissipate ({@code peak}..1: eased back to 0).
     * Returns the strength 0..1 (the size / brightness envelope). Continuous at both joins.
     */
    public static float burstEnvelope(float t, float anticipation, float peak) {
        t = clamp01(t);
        if (t < anticipation) return 0.2f * smooth(phase(t, 0f, anticipation));
        if (t < peak) return 0.2f + 0.8f * easeOut(phase(t, anticipation, peak));
        return 1f - smooth(phase(t, peak, 1f));
    }

    /** Fade in over {@code in}, hold, fade out over the last {@code out} (fractions of 1), smooth. */
    public static float fadeInOut(float t, float in, float out) {
        float a = in <= 0f ? 1f : smooth(t / in), b = out <= 0f ? 1f : smooth((1f - t) / out);
        return Math.min(a, b);
    }

    // ------------------------------------------------------------------ flip-book
    /** The frame shown at progress t of a sheet of {@code frames} frames (0..frames-1). */
    public static int flipFrame(float t, int frames) { return Math.min(frames - 1, Math.max(0, (int) (clamp01(t) * (frames - 1)))); }

    /** The fraction between this frame and the next (for the soft cross-fade). */
    public static float flipBlend(float t, int frames) { float f = clamp01(t) * (frames - 1); return f - (int) f; }

    /** UV rectangle of cell {@code frame} (row by row from the top-left) of a {@code cols x rows} sheet: out = {u0, v0, u1, v1}. */
    public static void flipCell(int frame, int cols, int rows, float[] out) {
        int c = frame % cols, r = frame / cols;
        out[0] = (float) c / cols;
        out[1] = (float) r / rows;
        out[2] = (float) (c + 1) / cols;
        out[3] = (float) (r + 1) / rows;
    }

    // ------------------------------------------------------------------ ribbons
    /** Width of a ribbon at s (0 = tail, 1 = head): tail + (head - tail) * s^pow. pow 1 = linear, > 1 = stays thin then swells at the head, < 1 = fat quickly. */
    public static float taper(float s, float tailW, float headW, float pow) {
        float k = pow == 1f ? s : (float) Math.pow(clamp01(s), pow);
        return tailW + (headW - tailW) * k;
    }

    /** A deterministic number 0..1 from a seed and two indices (the same on every client). */
    public static float hash(long seed, int i, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL + k * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (float) ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Segments for a base count at a detail level (the same rule as {@code VfxRenderContext.seg}). */
    public static int seg(int full, int min, float detail) { return Math.max(min, Math.round(full * detail)); }

    // ------------------------------------------------------------------ quad cost of every primitive (what the budget tests add up)
    /** A ribbon through n path points. */
    public static int swooshQuads(int points) { return Math.max(0, points - 1); }

    /** One shock ring of {@code segs} segments. */
    public static int ringQuads(int segs) { return segs; }

    /** A cone / vortex of sides x rings. */
    public static int coneQuads(int sides, int rings) { return sides * rings; }

    /** A flip-book sprite: 1 quad, or 2 with the cross-fade. */
    public static int flipbookQuads(boolean crossfade) { return crossfade ? 2 : 1; }

    /** Core + shell glow: the shell (alpha) when {@code shell}, the halo and the hot core (additive). */
    public static int coreShellQuads(boolean shell) { return shell ? 3 : 2; }

    public static final int DECAL_QUADS = 1, STREAK_QUADS = 1, SIDE_RIBBON_QUADS = 1;

    // ------------------------------------------------------------------ worst-case quads per effect (index = SHAPES)
    /**
     * The most quads one toolkit effect writes at full detail, measured with the preview harness on a half-tick sweep of the whole life (tools/vfx_preview/scenes.d/roblox.json; KI_BLAST at power 8, the largest volley) plus
     * a little headroom. {@code ctx.seg} thins every ring, cone and ribbon by distance and the particle setting, and the engine caps one effect at {@link #ENGINE_MAX_QUADS} on top.
     */
    public static final int[] MAX_QUADS = {56, 68, 66, 24, 18, 24, 50, 62, 58, 50};
    /** The same measure of the old drawing of each shape (the before picture), same order. The toolkit is cheaper everywhere except EARTH_SPIKES (3D stone spikes instead of 11 flat quads). */
    public static final int[] OLD_QUADS = {69, 69, 70, 26, 18, 11, 83, 96, 58, 85};
}
