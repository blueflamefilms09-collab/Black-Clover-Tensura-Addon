package com.newuniverse.nusmp.client.scalecast;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.scalecast.ScaleLod;
import com.newuniverse.nusmp.scalecast.ScaleMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: draws the Scale Magic balance scale ({@link ScaleMesh}, the same geometry tools/scalecast/preview_scale.py shows) into the game: the solid passes (cutout, or alpha blended while the
 * Scale fades), the glow passes (the etched runes of the beam, the halo, the glows of the jewel and the pans, additive through {@link SafeAdditive} only, so no transparent corner can show as
 * a square), the weighing line and the seal ring under the weighed foe. Render thread only, no allocation per frame (the sinks are reused, every position is plain floats). Textures:
 * assets/nusmp/textures/entity/scalecast (tools/scalecast/gen_scale_textures.py).
 */
public final class ScaleRender {
    private ScaleRender() {}

    private static final ResourceLocation GOLD_TEX = AuraRender.tex("entity/scalecast/gold"), ENAMEL_TEX = AuraRender.tex("entity/scalecast/enamel"), CHAIN_TEX = AuraRender.tex("entity/scalecast/chain"),
            GEM_TEX = AuraRender.tex("entity/scalecast/gem"), RUNES_TEX = AuraRender.tex("entity/scalecast/glow_runes"), HALO_TEX = AuraRender.tex("entity/scalecast/glow_halo"),
            RING_TEX = AuraRender.tex("entity/scalecast/glow_ring"), SOFT_TEX = AuraRender.tex("particle/glow");
    public static final int FB = PropDraw.FULL_BRIGHT, TEAL = 0x63BDB5, ORB_YOU = 0x7FE8DC, ORB_FOE = 0xFF7040, JEWEL = 0xA8FFF0, GOLD = 0xFFD76A;
    private static final PoseSink SOLID = new PoseSink(), GLOW = new PoseSink();

    /**
     * Writes the quads of ONE layer into ONE vertex consumer through the pose stack's current transform. One consumer at a time on purpose: the shared buffer source ends the batch of the
     * render type it held before when another type is asked for, so a consumer must be written to right after it was fetched and never kept for later.
     */
    private static final class PoseSink implements ScaleMesh.Sink {
        PoseStack pose;
        VertexConsumer vc;
        int only, argb = 0xFFFFFFFF, light;

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
     * Draws the Scale at the pose's origin, model space (the beam is two blocks across at scale 1: scale the pose before). {@code angleDeg} turns the beam about z (negative = the right pan,
     * the foe's, goes down); {@code bobL} and {@code bobR} lift each pan (blocks of model space); {@code loadC} and {@code loadF} (0..1) size the orbs of weight in the caster's and the foe's
     * pan; {@code alpha} fades the whole thing (below 1 the solid passes are alpha blended); {@code glow} 0..2 scales the glow passes. {@link SealDraw#camera} must have been called in
     * this pose (the cards face the camera).
     */
    public static void scale(PoseStack pose, MultiBufferSource buf, int light, float angleDeg, float bobL, float bobR, float loadC, float loadF, float alpha, float glow, int lod) {
        if (alpha <= 0.01f || lod >= ScaleLod.CULLED) return;
        boolean blend = alpha < 0.98f, full = lod == ScaleLod.FULL, orbs = lod <= ScaleLod.MEDIUM;
        float rad = angleDeg * Mth.DEG_TO_RAD, ca = Mth.cos(rad), sa = Mth.sin(rad);
        float hx = ScaleMesh.HOOK_X, hy = ScaleMesh.HOOK_Y, drop = ScaleMesh.CHAIN_LEN;
        float lx = -hx * ca - hy * sa, ly = -hx * sa + hy * ca - drop + bobL;                 // the caster's pan hangs from the left hook
        float rx = hx * ca - hy * sa, ry = hx * sa + hy * ca - drop + bobR;                   // the foe's pan from the right one
        int tint = SealDraw.argb(alpha, 0xFFFFFF);
        // gold: beam and gauge needle (turned), stand, the outside of the pans
        PoseSink s = SOLID.init(pose, SealDraw.lift(light, 7), tint);
        s.use(buf.getBuffer(blend ? AuraRender.translucent(GOLD_TEX) : AuraRender.cutout(GOLD_TEX)), ScaleMesh.GOLD);
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(angleDeg));
        ScaleMesh.beam(s, lod);
        ScaleMesh.pointer(s, lod);
        pose.popPose();
        ScaleMesh.column(s, lod);
        pose.pushPose();
        pose.translate(lx, ly, 0f);
        ScaleMesh.panGold(s, lod);
        pose.popPose();
        pose.pushPose();
        pose.translate(rx, ry, 0f);
        ScaleMesh.panGold(s, lod);
        pose.popPose();
        // the enamel of the pans
        s.use(buf.getBuffer(blend ? AuraRender.translucent(ENAMEL_TEX) : AuraRender.cutout(ENAMEL_TEX)), ScaleMesh.ENAMEL);
        pose.pushPose();
        pose.translate(lx, ly, 0f);
        ScaleMesh.panEnamel(s, lod);
        pose.popPose();
        pose.pushPose();
        pose.translate(rx, ry, 0f);
        ScaleMesh.panEnamel(s, lod);
        pose.popPose();
        // the chains
        s.use(buf.getBuffer(blend ? AuraRender.translucent(CHAIN_TEX) : AuraRender.cutout(CHAIN_TEX)), ScaleMesh.CHAIN);
        pose.pushPose();
        pose.translate(lx, ly, 0f);
        ScaleMesh.chains(s, lod);
        pose.popPose();
        pose.pushPose();
        pose.translate(rx, ry, 0f);
        ScaleMesh.chains(s, lod);
        pose.popPose();
        // the jewel and the orbs of weight (one texture, tinted per piece)
        if (orbs) {
            s.use(buf.getBuffer(blend ? AuraRender.translucent(GEM_TEX) : AuraRender.cutout(GEM_TEX)), ScaleMesh.GEM);
            s.argb = SealDraw.argb(alpha, JEWEL);
            ScaleMesh.gem(s, lod);
            if (loadC > 0f) orb(pose, s, lx, ly, loadC, SealDraw.argb(alpha, ORB_YOU), lod);
            if (loadF > 0f) orb(pose, s, rx, ry, loadF, SealDraw.argb(alpha, ORB_FOE), lod);                // no foe weighed: its pan is empty
        }
        // glow: the etched runes of the beam shine (additive)
        float g = Mth.clamp(glow, 0f, 2f) * alpha;
        if (full && g > 0.02f) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(angleDeg));
            pose.translate(0f, 0f, 0.003f);
            PoseSink a = GLOW.init(pose, FB, SealDraw.argb(Mth.clamp(0.85f * g, 0f, 1f), 0xFFFFFF));
            ScaleMesh.beamGlow(a.use(buf.getBuffer(SafeAdditive.additive(RUNES_TEX)), ScaleMesh.RUNES), lod);
            pose.popPose();
        }
        // soft glows: the jewel and the load of each pan
        if (orbs && g > 0.02f) {
            VertexConsumer vc = buf.getBuffer(SafeAdditive.additive(SOFT_TEX));
            SealDraw.glow(pose, vc, 0f, 0f, 0.24f, 0.26f * (0.9f + 0.1f * g), 0f, SealDraw.argb(Mth.clamp(0.85f * alpha, 0f, 1f), TEAL), FB);
            if (loadC > 0f) SealDraw.glow(pose, vc, lx, ly + 0.02f, 0f, 0.30f + 0.28f * loadC, 0f, SealDraw.argb(Mth.clamp((0.18f + 0.40f * loadC) * alpha, 0f, 1f), ORB_YOU), FB);
            if (loadF > 0f) SealDraw.glow(pose, vc, rx, ry + 0.02f, 0f, 0.30f + 0.28f * loadF, 0f, SealDraw.argb(Mth.clamp((0.18f + 0.40f * loadF) * alpha, 0f, 1f), ORB_FOE), FB);
        }
    }

    /** One orb of weight resting in a pan whose middle is (px, py): radius grows with the load. */
    private static void orb(PoseStack pose, PoseSink s, float px, float py, float load, int argb, int lod) {
        float rho = 0.075f + 0.115f * Mth.clamp(load, 0f, 1f);
        pose.pushPose();
        pose.translate(px, py - 0.125f + rho * 0.85f, 0f);
        pose.scale(rho, rho, rho);
        s.argb = argb;
        ScaleMesh.orb(s, lod);
        pose.popPose();
    }

    // ================================================================ cards
    /**
     * The halo behind the Scale (a card in the Scale's plane, always on the side away from the camera; call after {@link SealDraw#camera} in this pose): twelve soft spokes and a ring, a
     * little brighter while the aura is on. k = opacity 0..1.
     */
    public static void halo(PoseStack pose, MultiBufferSource buf, float age, float k, boolean aura) {
        if (k <= 0.02f) return;
        float side = SealDraw.CAM[8] < 0f ? 1f : -1f;
        float z = -0.28f * side, spin = age * 0.006f, c = Mth.cos(spin) * 1.75f, s = Mth.sin(spin) * 1.75f;
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.17f);
        VertexConsumer vc = buf.getBuffer(SafeAdditive.additive(HALO_TEX));
        SealDraw.plane(pose, vc, 0f, -0.15f, z, c, s, 0f, -s, c, 0f, side < 0f, SealDraw.argb(Mth.clamp((aura ? 0.62f : 0.40f) * k * pulse, 0f, 1f), 0xFFFFFF), FB);
    }

    /** A flat ring of the seal-ring texture lying on the ground at the pose's origin (turn nothing before: it is drawn in the xz plane), radius in blocks. */
    public static void groundRing(PoseStack pose, MultiBufferSource buf, float radius, float rotRad, float alpha, int rgb) {
        if (alpha <= 0.01f) return;
        VertexConsumer vc = buf.getBuffer(SafeAdditive.additive(RING_TEX));
        float c = Mth.cos(rotRad) * radius, s = Mth.sin(rotRad) * radius;
        SealDraw.plane(pose, vc, 0f, 0f, 0f, c, 0f, s, -s, 0f, c, false, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
        SealDraw.plane(pose, vc, 0f, 0f, 0f, c, 0f, s, -s, 0f, c, true, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
    }

    /** A ribbon of the soft glow texture from the tail to the head, camera facing: the line along which the Scale measures its foe. */
    public static void line(PoseStack pose, MultiBufferSource buf, float tx, float ty, float tz, float hx, float hy, float hz, float halfWidth, float alpha, int rgb) {
        if (alpha <= 0.01f) return;
        SealDraw.ribbon(pose, buf.getBuffer(SafeAdditive.additive(SOFT_TEX)), tx, ty, tz, hx, hy, hz, halfWidth, 0f, 1f, SealDraw.argb(Mth.clamp(alpha, 0f, 1f), rgb), FB);
    }
}
