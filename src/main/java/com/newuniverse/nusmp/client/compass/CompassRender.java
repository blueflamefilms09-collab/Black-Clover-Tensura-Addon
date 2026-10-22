package com.newuniverse.nusmp.client.compass;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.compass3d.CompassLod;
import com.newuniverse.nusmp.compass3d.CompassMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: draws the 3D Compass Magic models ({@link CompassMesh}, the same geometry tools/compass/preview_compass3d.py shows) into the game: the solid passes (cutout, or alpha blended while the
 * compass fades), the glow passes (the dial's gold lines, the rose and the needle, additive through {@link SafeAdditive}, so no transparent corner can show as a square), the glass dome (translucent),
 * the spiky yellow aura card that always sits on the far side of the compass from the camera, the glint that runs round the rim, and the field ring of the guard mode. Render thread only. Textures:
 * assets/nusmp/textures/entity/compass3d (tools/compass/gen_compass3d_textures.py).
 */
public final class CompassRender {
    private CompassRender() {}

    private static final ResourceLocation RIM = AuraRender.tex("entity/compass3d/rim"), FACE = AuraRender.tex("entity/compass3d/face"), FACE_GLOW = AuraRender.tex("entity/compass3d/face_glow"),
            STAR = AuraRender.tex("entity/compass3d/star"), NEEDLE = AuraRender.tex("entity/compass3d/needle"), GLASS = AuraRender.tex("entity/compass3d/glass"),
            AURA = AuraRender.tex("entity/compass3d/aura"), GLINT = AuraRender.tex("entity/compass3d/glint"), SEAL_RING = AuraRender.tex("entity/compass3d/seal_ring"),
            SOFT = AuraRender.tex("particle/glow");
    private static final ResourceLocation[] LAYER_TEX = {RIM, FACE, STAR, NEEDLE, GLASS};
    public static final int FB = PropDraw.FULL_BRIGHT, GOLD = 0xFFC94A, PALE = 0xFFF0B0;
    private static final PoseSink SOLID = new PoseSink(), GLOW = new PoseSink();

    /**
     * Writes the quads of ONE layer into ONE vertex consumer through the pose stack's current transform. One consumer at a time on purpose: the shared buffer source ends the batch of the
     * render type it held before when another type is asked for, so a consumer must be written to right after it was fetched and never kept for later.
     */
    private static final class PoseSink implements CompassMesh.Sink {
        PoseStack pose;
        VertexConsumer vc;
        int only, argb = 0xFFFFFFFF, light;

        /** 1.112.0 (lag audit): the two sinks below are reused (the render thread draws one compass at a time); a new sink per pass was 3 to 4 objects per compass per frame. */
        PoseSink init(PoseStack pose, int light, int argb) {
            this.pose = pose;
            this.light = light;
            this.argb = argb;
            this.vc = null;
            return this;
        }

        PoseSink use(VertexConsumer c, int layer) { this.vc = c; this.only = layer; return this; }

        @Override
        public void quad(int layer, float[] d, int o) {
            if (layer != only || vc == null) return;
            PoseStack.Pose p = pose.last();
            float nx = d[o + 20], ny = d[o + 21], nz = d[o + 22];
            int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
            for (int k = 0; k < 4; k++) {
                int q = o + k * 5;
                vc.addVertex(p, d[q], d[q + 1], d[q + 2]).setColor(r, g, b, a).setUv(d[q + 3], d[q + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
            }
        }
    }

    /**
     * Draws the compass at the pose's origin, model space radius 1 (scale the pose before). needleDeg turns the needle about the model's z axis (0 = north up). alpha 0..1 fades the whole thing
     * (below 1 the solid passes are alpha blended); glow 0..2 scales the glow passes. Passes in order: rim, dial, rose, needle (solid), the glow passes (additive), the glass (translucent).
     */
    public static void compass(PoseStack pose, MultiBufferSource buf, int light, float needleDeg, float alpha, float glow, boolean needle, boolean glass) {
        compass(pose, buf, light, needleDeg, alpha, glow, needle, glass, CompassLod.FULL);
    }

    /**
     * The same at a level of detail ({@link CompassLod}): FULL is the pass list above; MEDIUM has a third of the segments, no glass and only the dial's lines glowing (7 render types instead
     * of 11); LOW is the dial disc and a gold ring card standing for the rim (2 render types); CULLED draws nothing.
     */
    public static void compass(PoseStack pose, MultiBufferSource buf, int light, float needleDeg, float alpha, float glow, boolean needle, boolean glass, int lod) {
        if (alpha <= 0.01f || lod >= CompassLod.CULLED) return;
        boolean blend = alpha < 0.98f;
        PoseSink s = SOLID.init(pose, SealDraw.lift(light, 7), SealDraw.argb(alpha, 0xFFFFFF));
        if (lod >= CompassLod.LOW) {
            CompassMesh.bodyLayer(s.use(buf.getBuffer(blend ? AuraRender.translucent(FACE) : AuraRender.cutout(FACE)), CompassMesh.FACE), lod, CompassMesh.FACE);
            pose.pushPose();
            pose.translate(0f, 0f, 0.02f);
            ring(pose, buf, 1.02f, 0f, 0.55f * alpha, GOLD);
            pose.popPose();
            return;
        }
        for (int l = CompassMesh.RIM; l <= CompassMesh.STAR; l++) CompassMesh.bodyLayer(s.use(buf.getBuffer(blend ? AuraRender.translucent(LAYER_TEX[l]) : AuraRender.cutout(LAYER_TEX[l])), l), lod, l);
        if (needle) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(needleDeg));
            CompassMesh.needle(s.use(buf.getBuffer(blend ? AuraRender.translucent(NEEDLE) : AuraRender.cutout(NEEDLE)), CompassMesh.NEEDLE), lod);
            pose.popPose();
        }
        // glow passes: the dial's gold lines shine, the rose and the needle a little (full bright, additive)
        float g = Mth.clamp(glow, 0f, 2f) * alpha;
        if (g > 0.02f) {
            pose.pushPose();
            pose.translate(0f, 0f, 0.004f);
            PoseSink a = GLOW.init(pose, FB, SealDraw.argb(Mth.clamp(0.85f * g, 0f, 1f), 0xFFFFFF));
            CompassMesh.bodyLayer(a.use(buf.getBuffer(SafeAdditive.additive(FACE_GLOW)), CompassMesh.FACE), lod, CompassMesh.FACE);
            if (lod == CompassLod.FULL) {                                                // the rose and the needle only glow while the compass is near
                a.argb = SealDraw.argb(Mth.clamp(0.20f * g, 0f, 1f), 0xFFFFFF);
                CompassMesh.bodyLayer(a.use(buf.getBuffer(SafeAdditive.additive(STAR)), CompassMesh.STAR), lod, CompassMesh.STAR);
                if (needle) {
                    a.argb = SealDraw.argb(Mth.clamp(0.28f * g, 0f, 1f), 0xFFFFFF);
                    pose.mulPose(Axis.ZP.rotationDegrees(needleDeg));
                    CompassMesh.needle(a.use(buf.getBuffer(SafeAdditive.additive(NEEDLE)), CompassMesh.NEEDLE), lod);
                }
            }
            pose.popPose();
        }
        if (glass && lod == CompassLod.FULL)
            CompassMesh.glass(SOLID.init(pose, SealDraw.lift(light, 7), SealDraw.argb(alpha, 0xFFFFFF)).use(buf.getBuffer(AuraRender.translucent(GLASS)), CompassMesh.GLASS), lod);
    }

    /** A flying arrow-needle along +z at the pose's origin (model units; scale the pose): solid, plus a soft additive pass of the same shape. */
    public static void arrow(PoseStack pose, MultiBufferSource buf, int light, float alpha, float glow) { arrow(pose, buf, light, alpha, glow, CompassLod.FULL); }

    /** The arrow-needle at a level of detail: FULL the whole model and its additive twin; MEDIUM the model without its tail ring and no twin; LOW nothing (the painter's streak stands for it). */
    public static void arrow(PoseStack pose, MultiBufferSource buf, int light, float alpha, float glow, int lod) {
        if (alpha <= 0.01f || lod >= CompassLod.LOW) return;
        PoseSink s = SOLID.init(pose, SealDraw.lift(light, 9), SealDraw.argb(alpha, 0xFFFFFF));
        CompassMesh.arrow(s.use(buf.getBuffer(alpha < 0.98f ? AuraRender.translucent(NEEDLE) : AuraRender.cutout(NEEDLE)), CompassMesh.NEEDLE), lod);
        if (glow > 0.02f && lod == CompassLod.FULL) {
            PoseSink a = GLOW.init(pose, FB, SealDraw.argb(Mth.clamp(0.35f * glow * alpha, 0f, 1f), 0xFFFFFF));
            CompassMesh.arrow(a.use(buf.getBuffer(SafeAdditive.additive(NEEDLE)), CompassMesh.NEEDLE), lod);
        }
    }

    /** A unit route segment ({@link CompassMesh#bar}) or arrow head ({@link CompassMesh#chevron}) at the pose, with a faint additive twin. */
    public static void routePiece(PoseStack pose, MultiBufferSource buf, boolean chevron, float alpha) { routePiece(pose, buf, chevron, alpha, true); }

    /** The same; {@code twin} false leaves the additive twin out (a route drawn at a lower level of detail). */
    public static void routePiece(PoseStack pose, MultiBufferSource buf, boolean chevron, float alpha, boolean twin) {
        if (alpha <= 0.01f) return;
        PoseSink s = SOLID.init(pose, FB, SealDraw.argb(alpha, 0xFFFFFF));
        s.use(buf.getBuffer(AuraRender.translucent(NEEDLE)), CompassMesh.NEEDLE);
        if (chevron) CompassMesh.chevron(s); else CompassMesh.bar(s);
        if (!twin) return;
        PoseSink a = GLOW.init(pose, FB, SealDraw.argb(0.4f * alpha, 0xFFFFFF));
        a.use(buf.getBuffer(SafeAdditive.additive(NEEDLE)), CompassMesh.NEEDLE);
        if (chevron) CompassMesh.chevron(a); else CompassMesh.bar(a);
    }

    // ================================================================ cards
    /**
     * The spiky yellow aura behind the compass (a card in the compass plane, always on the side away from the camera; call after {@link SealDraw#camera} in this pose), turning slowly,
     * and the glint running round the rim. k = opacity 0..1, pulse = how bright the aura breathes.
     */
    public static void aura(PoseStack pose, MultiBufferSource buf, float age, float k, float strength) {
        if (k <= 0.02f) return;
        float side = SealDraw.CAM[8] < 0f ? 1f : -1f;                      // the camera looks along CAM[6..8]: toward -z means it sits in front of the face
        float z = -0.30f * side, spin = age * 0.012f, c = Mth.cos(spin) * 2.15f, s = Mth.sin(spin) * 2.15f;
        VertexConsumer vc = buf.getBuffer(SafeAdditive.additive(AURA));
        float pulse = 0.82f + 0.18f * Mth.sin(age * 0.23f);
        SealDraw.plane(pose, vc, 0f, 0f, z, c, s, 0f, -s, c, 0f, side < 0f, SealDraw.argb(Mth.clamp(0.8f * k * strength * pulse, 0f, 1f), 0xFFFFFF), FB);
        vc = buf.getBuffer(SafeAdditive.additive(SOFT));
        SealDraw.glow(pose, vc, 0f, 0f, z * 0.5f, 1.7f, 0f, SealDraw.argb(Mth.clamp(0.22f * k * strength, 0f, 1f), GOLD), FB);
    }

    /** The glint: a small four point flare that runs round the rim (front lip), with a second, fainter one half a turn behind. */
    public static void glint(PoseStack pose, MultiBufferSource buf, float age, float k) {
        if (k <= 0.02f) return;
        VertexConsumer vc = buf.getBuffer(SafeAdditive.additive(GLINT));
        for (int j = 0; j < 2; j++) {
            float a = age * 0.035f + j * Mth.PI + 0.7f;
            float flick = 0.55f + 0.45f * Mth.sin(age * 0.31f + j * 2f);
            SealDraw.glow(pose, vc, Mth.cos(a) * 0.93f, Mth.sin(a) * 0.93f, 0.15f, (j == 0 ? 0.22f : 0.13f) * (0.8f + 0.4f * flick), age * 0.02f,
                    SealDraw.argb(Mth.clamp((j == 0 ? 1f : 0.6f) * flick * k, 0f, 1f), 0xFFFFFF), FB);
        }
    }

    /** A ring card (the seal ring texture) in the plane facing +z of this pose: the guard field round the compass, the seal under a foe (turn the pose before). */
    public static void ring(PoseStack pose, MultiBufferSource buf, float radius, float rotRad, float alpha, int rgb) {
        if (alpha <= 0.01f) return;
        VertexConsumer vc = buf.getBuffer(SafeAdditive.additive(SEAL_RING));
        float c = Mth.cos(rotRad) * radius, s = Mth.sin(rotRad) * radius;
        SealDraw.plane(pose, vc, 0f, 0f, 0f, c, s, 0f, -s, c, 0f, false, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
        SealDraw.plane(pose, vc, 0f, 0f, 0f, c, s, 0f, -s, c, 0f, true, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
    }

    /** A camera facing soft glow (call SealDraw.camera in this pose first). */
    public static void softGlow(PoseStack pose, MultiBufferSource buf, float x, float y, float z, float half, float alpha, int rgb) {
        if (alpha <= 0.01f) return;
        SealDraw.glow(pose, buf.getBuffer(SafeAdditive.additive(SOFT)), x, y, z, half, 0f, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
    }

    /** A ribbon (the trail of a needle, the line of a route) of the soft glow texture from the tail to the head, camera facing. */
    public static void trail(PoseStack pose, MultiBufferSource buf, float tx, float ty, float tz, float hx, float hy, float hz, float halfWidth, float alpha, int rgb) {
        if (alpha <= 0.01f) return;
        SealDraw.ribbon(pose, buf.getBuffer(SafeAdditive.additive(SOFT)), tx, ty, tz, hx, hy, hz, halfWidth, 0f, 1f, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
    }
}
