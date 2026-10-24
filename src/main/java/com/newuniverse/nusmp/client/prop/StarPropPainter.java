package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.star.StarRules;
import com.newuniverse.nusmp.vfx.StarV2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Star Magic nodes (docs/vfx_remake/star_ingame.md): the two props. Look: A "anime cel" for the star (a flat gold-white four-point star that steps between two sizes every two ticks, a warm halo, two twinkles,
 * a ground sigil of rings and four small stars, a faint column down to it) and for the Quartile Scutum pane (a flat blue glare pane with a white border and a four-point star on each corner); B "volumetric
 * glow" for a star seen from afar (a soft bloom star). Palette: a PLACEHOLDER (gold-white and starlight blue).
 *
 * <p>Cost (rule 11): a star is {@link StarV2#NODE_QUADS_NEAR} quads near (less than {@value StarV2#NODE_FAR_BLOCKS} blocks and among the first {@value StarV2#NODE_NEAR_PER_FRAME} drawn this frame),
 * {@link StarV2#NODE_QUADS_FAR} quads far, nothing beyond {@value StarV2#NODE_MAX_BLOCKS} blocks; a pane is at most {@link StarV2#PANE_QUADS} quads. With {@code vfx.element.star = V1} a star is one plain glow
 * and a pane one flat quad. Nothing is allocated per frame; every texture is resolved once.
 */
public final class StarPropPainter {
    private StarPropPainter() {}

    private static final int FB = PropDraw.FULL_BRIGHT, GOLD = 0xFFE08A;
    private static ResourceLocation star4, spark, sigil, paneTex, glow, bstar;
    private static float lastPartial = -1f;
    private static int drawnNear;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        star4 = AuraRender.tex("particle/" + StarV2.fileName("a_star4"));
        spark = AuraRender.tex("particle/" + StarV2.fileName("a_spark"));
        sigil = AuraRender.tex("particle/" + StarV2.fileName("a_sigil"));
        paneTex = AuraRender.tex("particle/" + StarV2.fileName("a_pane"));
        glow = AuraRender.tex("particle/" + StarV2.fileName("b_glow"));
        bstar = AuraRender.tex("particle/" + StarV2.fileName("b_star"));
        PropPainters.register(PropKind.STAR_NODE, StarPropPainter::node);
        PropPainters.register(PropKind.STAR_PANE, StarPropPainter::pane);
    }

    private static float inOut(MagicPropEntity e, float age, float in, float out) {
        return Math.min(Mth.clamp(age / in, 0f, 1f), Mth.clamp((e.maxLife() - age) / out, 0f, 1f));
    }

    private static void node(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = e.getX() - cam.x, dy = e.getY() - cam.y, dz = e.getZ() - cam.z;
        if (partial != lastPartial) { lastPartial = partial; drawnNear = 0; }
        int quads = StarV2.nodeQuads(Math.sqrt(dx * dx + dy * dy + dz * dz), drawnNear);
        if (quads == 0) return;
        boolean reduced = StarV2.reducedFlashes();
        float a = inOut(e, age, 6f, 24f);
        if (!reduced && e.maxLife() - age < 60f && ((int) (age / 3f) & 1) == 1) a *= 0.55f;      // the last three seconds: it blinks (not with reduced flashes)
        SpatialArmourKit.camera(pose);
        if (!StarV2.v2Look()) {                                                  // V1: one plain glow
            SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.additive(glow)), 0f, 0f, 0f, 0.9f, 0f, SpatialArmourKit.argb(0.9f * a, GOLD), FB);
            return;
        }
        if (quads == StarV2.NODE_QUADS_FAR) {                                    // far: the B bloom star and a halo
            SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.additive(bstar)), 0f, 0f, 0f, 1.1f, 0f, SpatialArmourKit.argb(0.95f * a, 0xFFFFFF), FB);
            SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.additive(glow)), 0f, 0f, 0f, 1.3f, 0f, SpatialArmourKit.argb(0.4f * a, GOLD), FB);
            return;
        }
        drawnNear++;
        int step = reduced ? 0 : ((int) (age / 2f) & 1);                         // cel timing: two sizes, swapped every two ticks
        float s = (step == 0 ? 0.62f : 0.55f) * (0.85f + 0.15f * Mth.clamp(age / 6f, 0f, 1f));
        float tw = StarV2.twinkle(age, 0.5f, 0.3f, reduced);
        SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.additive(glow)), 0f, 0f, 0f, 1.15f, 0f, SpatialArmourKit.argb(0.45f * a * tw, GOLD), FB);        // the faint warm halo
        SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.translucent(star4)), 0f, 0f, 0f, s, 0f, SpatialArmourKit.argb(a, 0xFFFFFF), FB);              // the star
        VertexConsumer sv = buffers.getBuffer(AuraRender.translucent(spark));
        float roll = StarV2.spin(age * 0.06f, reduced);
        SpatialArmourKit.glow(pose, sv, 0.38f, 0.30f, 0f, 0.20f * tw, roll, SpatialArmourKit.argb(0.9f * a, 0xFFFFFF), FB);
        SpatialArmourKit.glow(pose, sv, -0.32f, -0.26f, 0f, 0.16f * tw, -roll, SpatialArmourKit.argb(0.8f * a, 0xFFFFFF), FB);
        double drop = StarRules.nodeDrop(e.param());
        if (drop >= 1.0) {
            float d = (float) drop;
            SpatialArmourKit.streak(pose, buffers.getBuffer(AuraRender.additive(glow)), 0f, -0.2f, 0f, 0f, -d + 0.1f, 0f, 0.07f, SpatialArmourKit.argb(0.22f * a, 0xBFDFFF), FB);   // the column down to the point beneath it
            pose.pushPose();
            pose.translate(0f, -d + 0.04f, 0f);
            SpatialArmourKit.flat(pose, buffers.getBuffer(AuraRender.translucent(sigil)), 1.5f, StarV2.spin(age * 1.2f, reduced), SpatialArmourKit.argb(0.85f * a, 0xFFFFFF), FB);   // the ground sigil (two quads: both sides)
            pose.popPose();
        }
    }

    private static void pane(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param();
        float hw = (float) StarRules.paneWidth(param) * 0.5f, hh = (float) StarRules.paneHeight(param) * 0.5f;
        if (hw <= 0.1f || hh <= 0.1f) return;
        boolean reduced = StarV2.reducedFlashes();
        float a = inOut(e, age, 5f, 10f);
        float pct = StarRules.panePercent(param) / 100f;
        float k = a * (0.45f + 0.55f * pct);                                      // it fades as it takes damage
        if (!StarV2.v2Look()) {                                                  // V1: one flat glow quad
            SpatialArmourKit.plane(pose, buffers.getBuffer(AuraRender.additive(glow)), 0f, 0f, 0f, hw, 0f, 0f, 0f, hh, 0f, false, SpatialArmourKit.argb(0.35f * k, 0xBFDFFF), FB);
            return;
        }
        VertexConsumer pv = buffers.getBuffer(AuraRender.translucent(paneTex));
        SpatialArmourKit.plane(pose, pv, 0f, 0f, 0f, hw, 0f, 0f, 0f, hh, 0f, false, SpatialArmourKit.argb(0.9f * k, 0xFFFFFF), FB);
        SpatialArmourKit.plane(pose, pv, 0f, 0f, 0f, hw, 0f, 0f, 0f, hh, 0f, true, SpatialArmourKit.argb(0.9f * k, 0xFFFFFF), FB);
        SpatialArmourKit.camera(pose);
        VertexConsumer cv = buffers.getBuffer(AuraRender.translucent(star4));
        float tw = StarV2.twinkle(age, 0.4f, 0.3f, reduced);
        float cs = 0.55f * tw * Math.min(1f, 0.6f + hw * 0.1f);
        for (int i = 0; i < 4; i++) SpatialArmourKit.glow(pose, cv, (i & 1) == 0 ? -hw : hw, (i & 2) == 0 ? -hh : hh, 0f, cs, 0f, SpatialArmourKit.argb(a, 0xFFFFFF), FB);
    }

}
