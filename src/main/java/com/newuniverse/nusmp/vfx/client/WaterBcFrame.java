package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.WaterBc;
import com.newuniverse.nusmp.vfx.WaterBcMeshBuffer;
import com.newuniverse.nusmp.vfx.client.layer.WaterBcLayer;

import java.util.List;

/**
 * The per-frame plan of the water hybrid (docs/vfx_remake/water_ingame.md): once a frame, after {@link VfxManager} has worked out which effects are drawn at all, every water effect of the hybrid is
 * admitted nearest first ({@link WaterBc#admit}) against the per-frame ceilings of mesh and sprite quads, which gives each a tier (full mesh, light mesh, flat sprites, nothing). The painter asks
 * {@link #tierOf} for its effect, counts its sprites against {@link #spriteBudgetLeft}, and the mesh buffer ({@link WaterBcMeshBuffer#FRAME}) is capped at the same ceiling, so whatever the number of casters
 * the frame cost is bounded. All arrays are fixed (64 effects at most, as the manager's own limit), nothing is allocated per frame. Render thread only.
 */
public final class WaterBcFrame {
    private WaterBcFrame() {}

    private static final int MAX = 64;
    private static final VfxInstance[] INSTS = new VfxInstance[MAX];
    private static final double[] DIST = new double[MAX];
    private static final int[] KIND = new int[MAX], TIER = new int[MAX];
    private static final boolean[] DONE = new boolean[MAX];
    private static int count;
    private static int spriteLeft = WaterBc.FRAME_SPRITE_CEILING;

    /**
     * Plans the frame. {@code distance[i]}, {@code lod[i]} and {@code left[i]} are the manager's per-effect scratch of this frame (distance past the effect's reach, level of detail, left out by the vertex
     * allowance); {@code frameVertices} is the quality level's vertex allowance, which scales the water ceilings. Also empties the mesh buffer of the last frame.
     */
    public static void plan(List<VfxInstance> active, double[] distance, float[] lod, boolean[] left, int n, int frameVertices) {
        float scale = WaterBc.qualityScale(frameVertices);
        WaterBcMeshBuffer.FRAME.reset(WaterBc.meshCeiling(scale));
        spriteLeft = WaterBc.spriteCeiling(scale);
        for (int i = 0; i < count; i++) INSTS[i] = null;
        count = 0;
        if (!WaterBc.bcLook()) return;
        for (int i = 0; i < n && count < MAX; i++) {
            VfxInstance inst = active.get(i);
            if (!(inst.layer instanceof WaterBcLayer) || lod[i] <= 0f || left[i]) continue;
            int kind = WaterBcLayer.kindOf(inst);
            if (kind < 0) continue;
            INSTS[count] = inst;
            DIST[count] = distance[i];
            KIND[count] = kind;
            count++;
        }
        WaterBc.admit(DIST, KIND, count, TIER, DONE, scale);
    }

    /** The tier this frame's plan gave the effect; the full mesh when the effect is not in the plan (a frame drawn without the manager's plan, the preview), or the forced tier of a test. */
    public static int tierOf(VfxInstance inst) {
        for (int i = 0; i < count; i++) if (INSTS[i] == inst) return TIER[i];
        int forced = WaterBc.forcedTier();
        return forced >= 0 ? forced : WaterBc.LOD0;
    }

    /** Sprite quads the water effects may still add this frame (the ceiling minus what has been drawn). */
    public static int spriteBudgetLeft() { return spriteLeft; }

    /** Records sprite quads drawn. */
    public static void spent(int quads) { spriteLeft -= quads; }
}
