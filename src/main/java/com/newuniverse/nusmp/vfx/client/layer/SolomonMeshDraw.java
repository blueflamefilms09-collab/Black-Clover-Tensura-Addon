package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * 3.2x.0: draws a {@link SolomonModels.Mesh} through the VFX vertex buffer, far faces first, all through the one material atlas (alpha-blended: the buffer has no depth write, so the faces that look at the camera are sorted
 * and drawn back to front, like {@code SolidParts}). One instance per layer; the arrays inside are allocated once, nothing is allocated per frame, per model or per face. Render thread only (the layer draws one effect at a
 * time). A model that does not fit the buffer's remaining budget is not drawn at all (never half a grimoire): {@link #draw} then returns 0 and the layer falls back to its flat sprite.
 *
 * <p>It also holds the small scratch arrays the layer needs to put a ring of models (the pillars) in far-to-near order, so the layer itself builds no array.
 */
public final class SolomonMeshDraw {
    /** The most models one ring can hold (the pillars). */
    public static final int RING_SLOTS = 8;

    private final SolomonModels.Posed posed = new SolomonModels.Posed();
    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
    private final float[] ringKey = new float[RING_SLOTS];
    private final int[] ringOrder = new int[RING_SLOTS];

    /** The pose and the axes of the next model (set them, then call {@link #draw}). */
    public SolomonModels.Posed posed() { return posed; }

    /**
     * Poses and draws {@code mesh} at the camera-relative point (ox, oy, oz); the axes and the pose come from {@link #posed()}. After the draw the part mask, the cell shift and the accent of the poser are reset.
     *
     * @return the quads drawn (0 when the model was fully culled, faded out or does not fit the budget)
     */
    public int draw(VfxVertexBuffer buf, ResourceLocation atlas, SolomonModels.Mesh mesh, float ox, float oy, float oz, int tint, float heat) {
        int drawn = 0;
        if ((tint >>> 24) >= 4) {
            int n = posed.prepare(mesh, ox, oy, oz, tint, heat);
            if (n > 0 && buf.hasBudget(n * 4)) {
                float[] pts = posed.pts, uv = posed.uv;
                for (int k = 0; k < n; k++) {
                    int i = posed.at(k), o = i * 12, u = i * 4;
                    p0.set(pts[o], pts[o + 1], pts[o + 2]);
                    p1.set(pts[o + 3], pts[o + 4], pts[o + 5]);
                    p2.set(pts[o + 6], pts[o + 7], pts[o + 8]);
                    p3.set(pts[o + 9], pts[o + 10], pts[o + 11]);
                    int c = posed.colour[i];
                    buf.quad(atlas, VfxBlend.ALPHA, p0, p1, p2, p3, uv[u], uv[u + 1], uv[u + 2], uv[u + 3], c, c);
                }
                drawn = n;
            }
        }
        posed.reset();
        return drawn;
    }

    /** Sets the distance key of slot {@code i} of the ring (squared distance to the camera of that model). */
    public void ringKey(int i, float distSq) { ringKey[i] = distSq; }

    /** Orders the first {@code n} slots far to near (the painter's order for models that overlap). */
    public void ringSort(int n) {
        int m = Math.min(n, RING_SLOTS);
        for (int i = 0; i < m; i++) ringOrder[i] = i;
        for (int i = 1; i < m; i++) {
            int v = ringOrder[i];
            float kv = ringKey[v];
            int j = i;
            while (j > 0 && ringKey[ringOrder[j - 1]] < kv) { ringOrder[j] = ringOrder[j - 1]; j--; }
            ringOrder[j] = v;
        }
    }

    /** The slot drawn {@code n}-th (after {@link #ringSort}). */
    public int ringAt(int n) { return ringOrder[n]; }
}
