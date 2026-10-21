package com.newuniverse.nusmp.client.labyrinth.rift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Why: the rift is drawn entirely here, because a vertical tear in the air is not a block shape. It is three layers, back to front. (1) The window: a pointed-oval lens, 2.6 blocks tall and 1.3
 * wide at its belly, drawn with a "skybox" render type so it shows another place and not a flat colour: the End's star field for a shortcut (the way on), the mod's own crimson void for a
 * pocket (a hidden realm), the End gateway's beam field for the return portal and the exit; the lens turns about the vertical axis to face the camera so it is never seen edge-on. (2) The rim and
 * the arcs: a bright outline of the lens and, re-rolled every three ticks from the rift's seed, jagged electrical arcs that leap from the rim into the air, in the colour of the kind of rift
 * (cyan, violet, gold, green), drawn with the vanilla lightning render type (additive, full bright). (3) A soft glow quad behind everything. The size follows {@link RiftRules#openness} (it opens
 * over a second and a half, holds, then gutters in its last four seconds), and an unstable shortcut (its floor still forming) is drawn dimmer with fewer arcs so it reads as "not ready". The
 * aura particles are Light and Dark Rukh, spawned by the block entity's client tick (the only particles the mod may use). No per-frame allocation except the pose stack's own.
 */
public final class DimensionalRiftRenderer implements BlockEntityRenderer<DimensionalRiftBlockEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png");
    private static final float HEIGHT = 2.6f, WIDTH = 0.65f;
    private static final int SEGMENTS = 14;

    private final BlockEntityRenderDispatcher dispatcher;

    public DimensionalRiftRenderer(BlockEntityRendererProvider.Context ctx) { this.dispatcher = ctx.getBlockEntityRenderDispatcher(); }

    @Override
    public boolean shouldRenderOffScreen(DimensionalRiftBlockEntity be) { return true; }

    @Override
    public void render(DimensionalRiftBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.isExclusive()) {                                                                   // 1.99.0: a mega-rare tear has a look of its own (MegaRiftRenderer); every other tear is drawn as before
            MegaRiftRenderer.render(dispatcher, be, partial, pose, buffers);
            return;
        }
        float open = RiftRules.openness(be.age(), be.left());
        if (open <= 0.02f) return;
        float t = be.age() + partial;
        float dim = be.stable() ? 1f : 0.55f;
        RiftType type = be.type();
        int rgb = type.rgb();
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;

        double camDist2 = 1.0e9, camRel = 0.0;                                                    // 1.107.0: the viewer's distance and height, for the parallax layers and the shards
        float camAngle = 0f;
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        if (dispatcher != null && dispatcher.camera != null) {
            Vec3 cam = dispatcher.camera.getPosition();
            double dx = cam.x - (be.getBlockPos().getX() + 0.5), dz = cam.z - (be.getBlockPos().getZ() + 0.5);
            camAngle = (float) Math.atan2(dx, dz);
            camRel = cam.y - (be.getBlockPos().getY() + HEIGHT * 0.5);
            camDist2 = dx * dx + dz * dz + camRel * camRel;
            pose.mulPose(Axis.YP.rotation(camAngle));
        }
        float h = HEIGHT * open, w = WIDTH * open * (0.96f + 0.04f * (float) Math.sin(t * 0.3));

        // (3) the glow behind everything
        pose.pushPose();
        pose.translate(0, h * 0.5, -0.02);
        glow(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)), 1.2f * open, r, g, b, 0.34f * dim);
        pose.popPose();

        // (1) the window onto another place
        RenderType window = type == RiftType.POCKET ? NURenderTypes.demonVoid() : type == RiftType.SHORTCUT ? RenderType.endPortal() : RenderType.endGateway();
        lens(buffers.getBuffer(window), pose.last().pose(), w * 0.94f, h * 0.94f, 0.05f * h, 0f);
        RiftParallax.render(pose, buffers, TearGeometry.lod(camDist2), w * 0.94f, h * 0.94f, 0.05f * h, camAngle, (float) camRel, t, r, g, b, 0.5f * open * dim);       // 1.107.0: two masked layers that slide with the viewing angle

        // (2) the rim and the arcs
        VertexConsumer bolt = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        rim(bolt, m, w, h, 0.05f * h, 0.045f, r, g, b, 0.9f * dim);
        RandomSource rnd = RandomSource.create(be.seed() * 31L + (long) (t / 3f));
        int arcs = be.stable() ? 7 : 3;
        for (int i = 0; i < arcs; i++) arc(bolt, m, rnd, w, h, 0.05f * h, r, g, b, dim, open);
        pose.popPose();

        // 1.107.0: reality shards, in the world frame (not turned to the camera): slivers torn loose by the tear that drift, turn, split and fade
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        RealityShards.render(pose, buffers, be.seed(), t, 0f, HEIGHT * 0.5f * open + 0.05f * h, 0f, 0.5f + WIDTH * open, HEIGHT * 0.5f * open, r, g, b, open * dim, camDist2);
        pose.popPose();
    }

    /** The lens: a pointed oval, as quads from bottom to top, both windings so it shows from either side. */
    private static void lens(VertexConsumer vc, Matrix4f m, float w, float h, float y0, float z) {
        for (int i = 0; i < SEGMENTS; i++) {
            float v0 = i / (float) SEGMENTS, v1 = (i + 1) / (float) SEGMENTS;
            float w0 = width(w, v0), w1 = width(w, v1), a = y0 + h * v0, b = y0 + h * v1;
            vc.addVertex(m, -w0, a, z);
            vc.addVertex(m, w0, a, z);
            vc.addVertex(m, w1, b, z);
            vc.addVertex(m, -w1, b, z);
            vc.addVertex(m, -w1, b, z);
            vc.addVertex(m, w1, b, z);
            vc.addVertex(m, w0, a, z);
            vc.addVertex(m, -w0, a, z);
        }
    }

    /** Half width of the lens at height fraction v (0 at both points, {@code w} at the belly). */
    private static float width(float w, float v) { return w * (float) Math.pow(Math.sin(Math.PI * Math.min(1f, Math.max(0f, v))), 0.8); }

    private static void rim(VertexConsumer vc, Matrix4f m, float w, float h, float y0, float thick, float r, float g, float b, float a) {
        for (int side = -1; side <= 1; side += 2)
            for (int i = 0; i < SEGMENTS; i++) {
                float v0 = i / (float) SEGMENTS, v1 = (i + 1) / (float) SEGMENTS;
                strip(vc, m, side * width(w, v0), y0 + h * v0, side * width(w, v1), y0 + h * v1, thick, r, g, b, a);
            }
    }

    /** One jagged arc from a point on the rim out into the air. */
    private static void arc(VertexConsumer vc, Matrix4f m, RandomSource rnd, float w, float h, float y0, float r, float g, float b, float dim, float open) {
        float v = 0.1f + rnd.nextFloat() * 0.8f, side = rnd.nextBoolean() ? 1f : -1f;
        float x = side * width(w, v), y = y0 + h * v;
        float len = (0.5f + rnd.nextFloat() * 0.9f) * open, dir = side * (0.4f + rnd.nextFloat() * 0.9f), up = (rnd.nextFloat() - 0.5f) * 1.2f;
        int n = 5;
        for (int s = 0; s < n; s++) {
            float k = (s + 1) / (float) n;
            float nx = x + dir * len / n + (rnd.nextFloat() - 0.5f) * 0.16f, ny = y + up * len / n + (rnd.nextFloat() - 0.5f) * 0.16f;
            float a = (1f - k * 0.8f) * dim;
            strip(vc, m, x, y, nx, ny, 0.03f * (1f - k * 0.5f), r, g, b, a);
            x = nx;
            y = ny;
        }
    }

    /** A thin camera-facing quad from (x0, y0) to (x1, y1) in the rift's plane (the plane already faces the camera). */
    private static void strip(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float thick, float r, float g, float b, float a) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 1e-5f) return;
        float nx = -dy / l * thick, ny = dx / l * thick;
        vc.addVertex(m, x0 - nx, y0 - ny, 0.01f).setColor(r, g, b, a);
        vc.addVertex(m, x0 + nx, y0 + ny, 0.01f).setColor(r, g, b, a);
        vc.addVertex(m, x1 + nx, y1 + ny, 0.01f).setColor(r, g, b, a);
        vc.addVertex(m, x1 - nx, y1 - ny, 0.01f).setColor(r, g, b, a);
    }

    private static void glow(PoseStack pose, VertexConsumer vc, float s, float r, float g, float b, float a) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        int light = LightTexture.FULL_BRIGHT;
        float hs = s * 1.5f;
        vc.addVertex(m, -s, -hs, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, -hs, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, hs, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, -s, hs, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
    }
}
