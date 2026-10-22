package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.sylph.SylphLod;

/**
 * Why: the one place that decides at which level of detail a Sylph is drawn this frame, shared by the prop painter (the linked small Sylph and the powered form) and the renderer of the
 * exclusive Sylph Spirit Lord, so that ALL Sylphs together share ONE quad budget per frame ({@link SylphLod#FRAME_QUADS}): the level follows the distance and the effect quality, and a Sylph
 * that does not fit is drawn at the next lower level. The viewer's own Sylph is never dropped. Render thread only; nothing is allocated.
 */
public final class SylphView {
    private SylphView() {}

    private static final SylphLod.QuadBudget BUDGET = new SylphLod.QuadBudget();

    /**
     * The level to draw a Sylph at, or {@link SylphLod#CULL}.
     * @param form    {@link SylphLod#SMALL} or {@link SylphLod#POWERED}
     * @param distSq  squared distance from the camera to her
     * @param own     true for the viewer's own Sylph (never dropped to nothing)
     */
    public static int lod(int form, double distSq, long gameTime, float partial, boolean own) {
        int wanted = SylphLod.level(form, distSq, PerfSettings.vfxQuality().detail);
        if (wanted == SylphLod.CULL) return SylphLod.CULL;
        return SylphLod.fit(form, wanted, BUDGET, SylphLod.frameKey(gameTime, partial), own);
    }
}
