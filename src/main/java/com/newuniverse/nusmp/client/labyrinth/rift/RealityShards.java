package com.newuniverse.nusmp.client.labyrinth.rift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * Why: the rifts of the spec are "massive cracks in space-time" with "floating, shattering reality shards that slowly drift and disintegrate". This draws them, for both renderers
 * ({@link DimensionalRiftRenderer} and {@link MegaRiftRenderer}). A shard is a four-faced sliver (a tetrahedron stretched along one axis) lit like a crystal: each face takes its brightness from
 * the angle it makes with a fixed light, the colour is the rift's own colour lifted towards white on the bright faces, and the faces are drawn additively with the same render type as the rift's
 * arcs ({@code RenderType.lightning()}: position and colour only, full bright, so it needs no texture and no light lookup). Where the shards are and what they look like at a moment is
 * {@link ShardMath}; this class turns that into vertices. THE BUDGET: at most {@link ShardMath#BUDGET} vertices a frame, fewer with distance ({@link TearGeometry#lod}: 14 shards near, 8 in the
 * middle, 3 far, none beyond). NO ALLOCATION: the rotation is computed into local floats, the vertices of the piece into one scratch array that lives as long as the class, and the face
 * normals into locals (the renderer runs on the client thread only, so the scratch arrays are safe). A shard that breaks is drawn as two halves moving apart.
 */
public final class RealityShards {
    private RealityShards() {}

    /** The sliver, a unit shape: apex up, three feet below. Scaled by the shard's size. */
    private static final float[] SHAPE = {
            0.00f, 1.00f, 0.00f,
            0.38f, -0.50f, 0.22f,
            -0.32f, -0.40f, 0.30f,
            0.00f, -0.45f, -0.40f};
    /** Faces as vertex triples, and the fixed light direction (normalised: 0.35, 0.8, 0.49). */
    private static final int[] FACES = {0, 1, 2, 0, 3, 1, 0, 2, 3, 1, 3, 2};
    private static final float LX = 0.35f, LY = 0.80f, LZ = 0.49f;

    /** Scratch: the four world-space (tear-space) vertices of the piece being drawn, and the state of the shard. */
    private static final float[] V = new float[12], S = new float[ShardMath.OUT_LEN];

    /**
     * Draws the shards of a tear. The pose is the tear's own frame (origin at the block's floor centre, axes aligned with the world); {@code cx, cy, cz} is the centre of the tear in it, {@code rx, ry} the half
     * width and half height of the tear, {@code r, g, b} the colour, {@code open} the tear's openness (0..1) and {@code distSq} the squared distance from the camera to the tear.
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, long seed, float tick, float cx, float cy, float cz, float rx, float ry, float r, float g, float b, float open, double distSq) {
        int n = ShardMath.count(TearGeometry.lod(distSq));
        if (n == 0 || open <= 0.05f) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < n; i++) {
            if (!ShardMath.state(seed, i, tick, rx, ry, S)) continue;
            float alpha = S[ShardMath.ALPHA] * open * 0.62f;
            float size = S[ShardMath.SIZE], split = S[ShardMath.SPLIT];
            float px = cx + S[ShardMath.X], py = cy + S[ShardMath.Y], pz = cz + S[ShardMath.Z];
            if (ShardMath.pieces(split) == 1) {
                piece(vc, m, px, py, pz, size, 1f, S[ShardMath.RX], S[ShardMath.RY], S[ShardMath.RZ], r, g, b, alpha);
            } else {
                // two halves, parting along the shard's own long axis as it breaks
                float away = split * size * 2.2f, half = size * (1f - 0.28f * split);
                float rad = (float) Math.toRadians(S[ShardMath.RZ]), ax = (float) Math.sin(rad), ay = (float) Math.cos(rad);
                piece(vc, m, px + ax * away, py + ay * away, pz, half, 1f, S[ShardMath.RX], S[ShardMath.RY], S[ShardMath.RZ], r, g, b, alpha);
                piece(vc, m, px - ax * away, py - ay * away, pz, half, -1f, S[ShardMath.RX] + 70f, S[ShardMath.RY] + 40f, S[ShardMath.RZ], r, g, b, alpha * 0.85f);
            }
        }
    }

    /** One four-faced sliver at (px, py, pz); {@code flip} -1 mirrors it (the second half of a broken shard). */
    private static void piece(VertexConsumer vc, Matrix4f m, float px, float py, float pz, float size, float flip, float rxDeg, float ryDeg, float rzDeg, float r, float g, float b, float a) {
        float cx = (float) Math.cos(Math.toRadians(rxDeg)), sx = (float) Math.sin(Math.toRadians(rxDeg));
        float cy = (float) Math.cos(Math.toRadians(ryDeg)), sy = (float) Math.sin(Math.toRadians(ryDeg));
        float cz = (float) Math.cos(Math.toRadians(rzDeg)), sz = (float) Math.sin(Math.toRadians(rzDeg));
        // the rotation matrix R = Rz * Ry * Rx
        float m00 = cz * cy, m01 = cz * sy * sx - sz * cx, m02 = cz * sy * cx + sz * sx;
        float m10 = sz * cy, m11 = sz * sy * sx + cz * cx, m12 = sz * sy * cx - cz * sx;
        float m20 = -sy, m21 = cy * sx, m22 = cy * cx;
        float mx = 0f, my = 0f, mz = 0f;
        for (int k = 0; k < 4; k++) {
            float x = SHAPE[k * 3] * flip * size, y = SHAPE[k * 3 + 1] * size, z = SHAPE[k * 3 + 2] * size;
            float wx = px + m00 * x + m01 * y + m02 * z, wy = py + m10 * x + m11 * y + m12 * z, wz = pz + m20 * x + m21 * y + m22 * z;
            V[k * 3] = wx;
            V[k * 3 + 1] = wy;
            V[k * 3 + 2] = wz;
            mx += wx * 0.25f;
            my += wy * 0.25f;
            mz += wz * 0.25f;
        }
        for (int f = 0; f < 4; f++) {
            int ia = FACES[f * 3] * 3, ib = FACES[f * 3 + 1] * 3, ic = FACES[f * 3 + 2] * 3;
            float ax = V[ia], ay = V[ia + 1], az = V[ia + 2];
            float ex = V[ib] - ax, ey = V[ib + 1] - ay, ez = V[ib + 2] - az;
            float fx = V[ic] - ax, fy = V[ic + 1] - ay, fz = V[ic + 2] - az;
            float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
            float fcx = (ax + V[ib] + V[ic]) / 3f - mx, fcy = (ay + V[ib + 1] + V[ic + 1]) / 3f - my, fcz = (az + V[ib + 2] + V[ic + 2]) / 3f - mz;
            boolean outward = nx * fcx + ny * fcy + nz * fcz >= 0f;                                  // keep the winding facing out of the sliver
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-8f) continue;
            float sign = outward ? 1f : -1f;
            float lit = Math.max(0f, sign * (nx * LX + ny * LY + nz * LZ) / len);
            float shade = 0.4f + 0.6f * lit;
            float cr = Math.min(1f, r * shade + 0.35f * lit), cg = Math.min(1f, g * shade + 0.35f * lit), cb = Math.min(1f, b * shade + 0.35f * lit), ca = a * (0.45f + 0.55f * lit);
            int jb = outward ? ib : ic, jc = outward ? ic : ib;
            vc.addVertex(m, ax, ay, az).setColor(cr, cg, cb, ca);
            vc.addVertex(m, V[jb], V[jb + 1], V[jb + 2]).setColor(cr, cg, cb, ca);
            vc.addVertex(m, V[jc], V[jc + 1], V[jc + 2]).setColor(cr, cg, cb, ca);
            vc.addVertex(m, V[jc], V[jc + 1], V[jc + 2]).setColor(cr, cg, cb, ca);
        }
    }
}
