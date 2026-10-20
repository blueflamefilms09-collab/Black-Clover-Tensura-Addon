package com.newuniverse.nusmp.vfx.client;

import java.util.Locale;

/**
 * Why: the performance report ({@code /nusmp perf report}) should say what the effect system did, not only how long it took: how many effects were running, how many were drawn, how many the
 * distance level of detail thinned out, how many were culled for being too far to see or skipped because the vertex budget of the frame was used up, and how many vertices went to the GPU.
 * Plain counters written by {@link VfxManager#render} and {@link FxRenderer#render} on the render thread (the numbers of the LAST frame, plus a running total of culled and skipped), read by the
 * client command on the same thread. No game classes.
 */
public final class VfxStats {
    private VfxStats() {}

    private static int active, drawn, thinned, culled, skipped, vertices, fxActive, fxDrawn, fxCulled;
    private static long totalCulled, totalSkipped, frames;

    /** The effect layers' last frame. */
    static void frame(int activeNow, int drawnNow, int thinnedNow, int culledNow, int skippedNow, int verticesNow) {
        active = activeNow;
        drawn = drawnNow;
        thinned = thinnedNow;
        culled = culledNow;
        skipped = skippedNow;
        vertices = verticesNow;
        totalCulled += culledNow;
        totalSkipped += skippedNow;
        frames++;
    }

    /** The FX pieces' last frame. */
    static void fxFrame(int activeNow, int drawnNow, int culledNow) {
        fxActive = activeNow;
        fxDrawn = drawnNow;
        fxCulled = culledNow;
        totalCulled += culledNow;
    }

    public static void reset() {
        active = drawn = thinned = culled = skipped = vertices = fxActive = fxDrawn = fxCulled = 0;
        totalCulled = totalSkipped = frames = 0;
    }

    /** One text line for the report. */
    public static String line() {
        return String.format(Locale.ROOT, "effects last frame: %d running, %d drawn (%d thinned by distance), %d culled, %d over the vertex budget, %d vertices; "
                        + "fx pieces: %d running, %d drawn, %d culled; since reset: %d culled, %d over budget in %d frames",
                active, drawn, thinned, culled, skipped, vertices, fxActive, fxDrawn, fxCulled, totalCulled, totalSkipped, frames);
    }
}
