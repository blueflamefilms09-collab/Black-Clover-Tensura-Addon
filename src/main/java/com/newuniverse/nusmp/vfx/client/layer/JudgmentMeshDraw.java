package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * 3.21.0: draws a {@link JudgmentModels.Mesh} through the VFX vertex buffer, far faces first, all through the one material atlas (alpha-blended: the buffer has no depth write, so the faces that look at the camera are sorted
 * and drawn back to front, like {@code SolidParts}). One instance per layer; the arrays inside are allocated once, nothing is allocated per frame, per model or per face. Render thread only (the layer draws one effect at a
 * time). A model that does not fit the buffer's remaining budget is not drawn at all (never half a hand): {@link #draw} then returns 0 and the layer falls back to its flat sprite.
 */
public final class JudgmentMeshDraw {
    private final JudgmentModels.Posed posed = new JudgmentModels.Posed();
    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();

    /** The pose and the axes of the next model (set them, then call {@link #draw}). */
    public JudgmentModels.Posed posed() { return posed; }

    /**
     * Poses and draws {@code mesh} at the camera-relative point (ox, oy, oz); the axes and the pose come from {@link #posed()}.
     *
     * @return the quads drawn (0 when the model was fully culled, faded out or does not fit the budget)
     */
    public int draw(VfxVertexBuffer buf, ResourceLocation atlas, JudgmentModels.Mesh mesh, float ox, float oy, float oz, int tint, float heat) {
        if ((tint >>> 24) < 4) return 0;
        int n = posed.prepare(mesh, ox, oy, oz, tint, heat);
        if (n == 0 || !buf.hasBudget(n * 4)) return 0;
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
        return n;
    }
}
