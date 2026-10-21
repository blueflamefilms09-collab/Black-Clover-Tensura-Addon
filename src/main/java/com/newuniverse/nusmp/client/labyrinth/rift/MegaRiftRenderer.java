package com.newuniverse.nusmp.client.labyrinth.rift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Why: the look of a MEGA-RARE tear, which must not be mistaken for the ordinary ones: bigger (four and a half blocks tall, two across), more violent, and in the colours of the tomb behind it (the
 * rift carries its pocket's accent and sky colours, synced from the server). It is drawn as two crossed, FIXED planes (not billboards: a fixed plane is what lets the core show parallax) with five
 * layers. (1) A soft glow behind, in the tomb's accent. (2) The CORE: a skybox-style window, a triangle fan over the jagged outline ({@link TearGeometry}) drawn as up to three layers of one
 * generated nebula texture, each layer deeper behind the plane and shifted by the view ray ({@code offset = (point - camera) / |camera depth| * layer depth}), so the sky inside the tear slides at three rates
 * as the viewer walks round it: depth, without a shader; tinted by the tomb's SKY colour, brighter at the middle. (3) The RIM, a bright ragged outline in the accent colour. (4) The STRANDS: forked
 * jagged strands of light leaping from the edge, each point wobbled every two ticks by a hash of integers (no random object is made), growing with the tear's opening and fading as it gutters. (5) The
 * SHIMMER: a faint haze of scrolling ripple texture a little wider than the tear; honestly an additive-looking ripple, not a screen-space refraction (that needs a shader pass, listed in the docs). The tear is
 * drawn with one render type (translucent emissive, double sided, full bright) so every layer is lit the same and seen from both sides. THE BUDGET: {@link TearGeometry#BUDGET} vertices in a frame, fewer
 * with distance ({@link TearGeometry#lod}: full under 18 blocks, middle under 40, far under 84, nothing beyond), the mesh is cached by seed, and nothing is allocated per frame but the pose stack's own.
 * The textures are made by tools/gen_rift_textures.py (reproducible: a fixed seed, tileable by construction). The ambient crackle is played by the rift's client tick (sound id {@code nusmp:rift.crackle}).
 */
public final class MegaRiftRenderer {
    private MegaRiftRenderer() {}

    public static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/misc/rift/core.png");
    public static final ResourceLocation CRACK = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/misc/rift/crack.png");
    public static final ResourceLocation SHIMMER = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/misc/rift/shimmer.png");
    public static final ResourceLocation SPARK = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/misc/rift/spark.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png");

    /** Depth behind the plane (blocks), texture tile size (blocks) and brightness of the three core layers. */
    private static final float[] DEPTH = {0.20f, 0.55f, 1.10f}, TILE = {3.2f, 4.6f, 6.8f}, LIGHT = {1.0f, 0.72f, 0.5f};
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    /** The plane's yaw, in degrees, as a function of the seed: so two tears of a tomb are not parallel. */
    static float yaw(long seed) { return (TearGeometry.hash(seed, 7, 3) * 0.5f + 0.5f) * 180f; }

    // scratch for the strand wobble (client thread only): the wobbled point of the last call
    private static float px, py;

    public static void render(BlockEntityRenderDispatcher dispatcher, DimensionalRiftBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers) {
        float open = RiftRules.openness(be.age(), be.left());
        if (open <= 0.02f || dispatcher == null || dispatcher.camera == null) return;
        Camera camera = dispatcher.camera;
        Vec3 cp = camera.getPosition();
        double wx = cp.x - (be.getBlockPos().getX() + 0.5), wy = cp.y - (be.getBlockPos().getY() + TearGeometry.CENTRE_Y), wz = cp.z - (be.getBlockPos().getZ() + 0.5);
        int lod = TearGeometry.lod(wx * wx + wy * wy + wz * wz);
        if (lod == TearGeometry.NONE) return;
        TearGeometry g = TearGeometry.of(be.seed());
        float t = be.age() + partial;
        int accent = be.accent(), sky = be.sky();
        float ar = (accent >> 16 & 255) / 255f, ag = (accent >> 8 & 255) / 255f, ab = (accent & 255) / 255f;
        float sr = (sky >> 16 & 255) / 255f, sg = (sky >> 8 & 255) / 255f, sb = (sky & 255) / 255f;
        float flicker = 0.9f + 0.1f * (float) Math.sin(t * 1.9f) + 0.05f * TearGeometry.hash(be.seed(), (int) (t * 0.5f), 1);
        float scale = open * (1f + 0.025f * (float) Math.sin(t * 0.37f));
        int tick2 = (int) (t * 0.5f);

        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);

        // (1) the soft glow behind, a camera-facing quad
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(wx, wz)));
        pose.translate(0, TearGeometry.CENTRE_Y * scale, -0.05);
        glowQuad(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)), 2.3f * scale, ar, ag, ab, 0.42f * open * flicker);
        pose.popPose();

        int planes = TearGeometry.planes(lod);
        for (int p = 0; p < planes; p++) {
            float deg = yaw(be.seed()) + 90f * p;
            double rad = Math.toRadians(deg), cos = Math.cos(rad), sin = Math.sin(rad);
            float lx = (float) (wx * cos - wz * sin), lz = (float) (wx * sin + wz * cos), ly = (float) wy + TearGeometry.CENTRE_Y;     // the camera in the plane's own frame (x across, y up from the floor, z out of the plane)
            float alz = Math.max(1.0f, Math.abs(lz));
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(deg));
            PoseStack.Pose last = pose.last();
            Matrix4f m = last.pose();
            // (2) the core: up to three layers, deeper = shifted more by the view ray
            int layers = TearGeometry.layers(lod);
            for (int l = 0; l < layers; l++) {
                VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(CORE));
                core(vc, last, m, g, scale, l, lx, ly, alz, t, sr, sg, sb, open * (0.92f - 0.2f * l) * flicker);
            }
            // (3) the rim and (4) the strands, with the crack texture's glow
            VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(SPARK));
            if (TearGeometry.rim(lod)) rim(glow, last, m, g, scale, ar, ag, ab, open * flicker);
            strands(glow, last, m, g, lod, scale, tick2, ar, ag, ab, open);
            // (5) the shimmer
            if (TearGeometry.shimmer(lod)) shimmer(buffers.getBuffer(RenderType.entityTranslucentEmissive(SHIMMER)), last, m, scale, t, sr, sg, sb, 0.2f * open);
            pose.popPose();
        }
        // 1.107.0: reality shards drifting round the fracture (RealityShards: budgeted by the same level of detail, no allocation)
        RealityShards.render(pose, buffers, be.seed(), t, 0f, TearGeometry.CENTRE_Y * scale, 0f, TearGeometry.RX * scale, TearGeometry.RY * scale, Math.min(1f, ar * 0.6f + 0.4f), Math.min(1f, ag * 0.6f + 0.4f), Math.min(1f, ab * 0.6f + 0.4f), open * flicker, wx * wx + wy * wy + wz * wz);
        pose.popPose();
    }

    // ---------------------------------------------------------------- the layers
    private static void vertex(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(last, 0f, 0f, 1f);
    }

    /** One layer of the core: the outline's triangle fan, the texture shifted by the view ray through each point to the layer's depth. */
    private static void core(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, TearGeometry g, float scale, int layer, float lx, float ly, float alz, float t, float sr, float sg, float sb, float alpha) {
        float depth = DEPTH[layer], tile = TILE[layer], br = LIGHT[layer];
        float scrollU = t * 0.004f * (layer + 1), scrollV = -t * 0.0025f * (layer + 1);
        float cx = 0f, cy = TearGeometry.CENTRE_Y * scale;
        float cu = (cx + (cx - lx) / alz * depth) / tile + scrollU, cv = (cy + (cy - ly) / alz * depth) / tile + scrollV;
        float inner = Math.min(1f, 0.55f + 0.5f * br), outer = 0.28f * br + 0.12f;
        int n = TearGeometry.N;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float x0 = g.ox[i] * scale, y0 = g.oy[i] * scale, x1 = g.ox[j] * scale, y1 = g.oy[j] * scale;
            float u0 = (x0 + (x0 - lx) / alz * depth) / tile + scrollU, v0 = (y0 + (y0 - ly) / alz * depth) / tile + scrollV;
            float u1 = (x1 + (x1 - lx) / alz * depth) / tile + scrollU, v1 = (y1 + (y1 - ly) / alz * depth) / tile + scrollV;
            float z = -0.012f * (layer + 1);
            vertex(vc, last, m, cx, cy, z, cu, cv, sr * inner, sg * inner, sb * inner, alpha);
            vertex(vc, last, m, x0, y0, z, u0, v0, sr * outer, sg * outer, sb * outer, alpha);
            vertex(vc, last, m, x1, y1, z, u1, v1, sr * outer, sg * outer, sb * outer, alpha);
            vertex(vc, last, m, x1, y1, z, u1, v1, sr * outer, sg * outer, sb * outer, alpha);
        }
    }

    private static void rim(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, TearGeometry g, float scale, float r, float gg, float b, float a) {
        int n = TearGeometry.N;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            segment(vc, last, m, g.ox[i] * scale, g.oy[i] * scale, g.ox[j] * scale, g.oy[j] * scale, 0.07f * scale + 0.015f, r, gg, b, a, 0.02f);
        }
    }

    private static void strands(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, TearGeometry g, int lod, float scale, int tick2, float r, float gg, float b, float open) {
        int count = TearGeometry.strands(lod), segs = TearGeometry.strandSegments(lod);
        for (int s = 0; s < count; s++) {
            float pulse = 0.6f + 0.4f * TearGeometry.hash(g.seed, s, tick2 + 11);
            float grow = Math.min(1f, open * 1.4f) * pulse;
            if (grow <= 0.05f) continue;
            wobble(g, s, 0, tick2, scale, grow);
            float x0 = px, y0 = py;
            for (int k = 1; k <= segs; k++) {
                wobble(g, s, k, tick2, scale, grow);
                float x1 = px, y1 = py, fade = 1f - (k - 1f) / segs * 0.7f;
                segment(vc, last, m, x0, y0, x1, y1, g.sw[s] * (1f - 0.55f * k / segs) * (0.8f + 0.5f * pulse), r, gg, b, (0.5f + 0.5f * fade) * grow, 0.03f);
                x0 = x1;
                y0 = y1;
            }
        }
    }

    /** The wobbled point k of strand s at this half-tick, into {@link #px} and {@link #py}: the base point scaled by the opening, moved by a hash that grows along the strand. */
    private static void wobble(TearGeometry g, int s, int k, int tick2, float scale, float grow) {
        int idx = s * TearGeometry.STRAND_POINTS + k;
        float bx = g.sx[idx], by = g.sy[idx];
        float ext = k == 0 ? 1f : grow;                                         // a strand reaches out as the tear opens; its root stays on the rim
        float rx = g.sx[s * TearGeometry.STRAND_POINTS], ry = g.sy[s * TearGeometry.STRAND_POINTS];
        float x = rx + (bx - rx) * ext, y = ry + (by - ry) * ext;
        float amp = 0.17f * k / (float) (TearGeometry.STRAND_POINTS - 1);
        x += TearGeometry.hash(g.seed, s * 16 + k, tick2) * amp;
        y += TearGeometry.hash(g.seed, s * 16 + k + 7, tick2) * amp;
        px = x * scale;
        py = y * scale;
    }

    private static void shimmer(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, float scale, float t, float r, float g, float b, float a) {
        float hw = 1.6f * scale, y0 = -0.3f * scale, y1 = (TearGeometry.CENTRE_Y * 2f + 0.3f) * scale;
        float u0 = t * 0.011f, v0 = -t * 0.017f, du = 2.0f * hw / 3.0f, dv = (y1 - y0) / 3.0f;
        vertex(vc, last, m, -hw, y0, 0.04f, u0, v0 + dv, r, g, b, a);
        vertex(vc, last, m, hw, y0, 0.04f, u0 + du, v0 + dv, r, g, b, a);
        vertex(vc, last, m, hw, y1, 0.04f, u0 + du, v0, r, g, b, a);
        vertex(vc, last, m, -hw, y1, 0.04f, u0, v0, r, g, b, a);
    }

    /** A thin quad from (x0, y0) to (x1, y1) in the plane, textured with the soft glow so a chain of them is a glowing line. */
    private static void segment(VertexConsumer vc, PoseStack.Pose last, Matrix4f m, float x0, float y0, float x1, float y1, float thick, float r, float g, float b, float a, float z) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 1e-5f) return;
        float nx = -dy / l * thick, ny = dx / l * thick;
        vertex(vc, last, m, x0 - nx, y0 - ny, z, 0f, 0f, r, g, b, a);
        vertex(vc, last, m, x0 + nx, y0 + ny, z, 0f, 1f, r, g, b, a);
        vertex(vc, last, m, x1 + nx, y1 + ny, z, 1f, 1f, r, g, b, a);
        vertex(vc, last, m, x1 - nx, y1 - ny, z, 1f, 0f, r, g, b, a);
    }

    private static void glowQuad(PoseStack pose, VertexConsumer vc, float s, float r, float g, float b, float a) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float hs = s * 1.6f;
        vertex(vc, last, m, -s, -hs, 0f, 0f, 1f, r, g, b, a);
        vertex(vc, last, m, s, -hs, 0f, 1f, 1f, r, g, b, a);
        vertex(vc, last, m, s, hs, 0f, 1f, 0f, r, g, b, a);
        vertex(vc, last, m, -s, hs, 0f, 0f, 0f, r, g, b, a);
    }
}
