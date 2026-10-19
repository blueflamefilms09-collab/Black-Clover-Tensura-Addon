package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Mirror Magic: the summon models, GeoLite models (tools/gen_mirror_geo.py) with their glow maps additive; the glass makes them TRANSLUCENT models.
 * <ul>
 *   <li>MIRROR_1, the standing ornate mirror (oval, round, arch or diamond by param): it unfolds from a line over 10 ticks (clip open), shimmers (idle) and fades over the
 *       last 8; a ripple ring turns on the ground under it, a bright glint runs over the glass and a soft light stands in front of it (additive, camera-facing).</li>
 *   <li>MIRROR_2, the blade-edged mirror: a kite of glass with a white edge, spinning about its length as it flies (param 0) or turned point down as it falls (param 1),
 *       with a streak behind it and a glint on its edge.</li>
 * </ul>
 */
public final class MirrorPropPainter {
    private MirrorPropPainter() {}

    private static final GeoSpec[] SHAPES = {GeoSpec.of("mirror", "oval"), GeoSpec.of("mirror", "round"), GeoSpec.of("mirror", "arch"), GeoSpec.of("mirror", "diamond")};
    private static final GeoSpec BLADE = GeoSpec.of("mirror", "blade");
    private static final int VIOLET = 0xB89CFF, SILVER = 0xE6EEFF, FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation ripple, glint, pane, beam;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        ripple = AuraRender.tex("particle/mirror_reflex_ripple");
        glint = AuraRender.tex("particle/mirror_reflex_glint");
        pane = AuraRender.tex("particle/mirror_reflex_pane");
        beam = AuraRender.tex("particle/mirror_reflex_beam");
        PropPainters.register(PropKind.MIRROR_1, MirrorPropPainter::standing);
        PropPainters.register(PropKind.MIRROR_2, MirrorPropPainter::blade);
    }

    private static void standing(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int shape = e.param() & 3;
        float k = 0.55f * e.scale();
        float on = Mth.clamp(age / 10f, 0f, 1f), out = Mth.clamp((e.maxLife() - age) / 8f, 0f, 1f), a = Math.min(on, out);
        if (a <= 0.01f) return;
        String clip = age < 10f ? "open" : "idle";
        pose.pushPose();
        // the ripple on the ground under the mirror (the prop's position is the middle of the glass, 1.5 model blocks up)
        pose.pushPose();
        pose.translate(0f, -1.5f * k + 0.03f, 0f);
        SpatialArmourKit.flat(pose, buffers.getBuffer(AuraRender.additive(ripple)), 1.1f * k * (0.6f + 0.4f * on), age * 0.4f, SpatialArmourKit.argb(0.6f * a, VIOLET), FB);
        pose.popPose();
        pose.scale(k, k, k);
        pose.translate(0f, -1.5f, 0f);
        GeoDraw.paint(pose, buffers, SHAPES[shape], clip, age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light,
                SpatialArmourKit.argb(a, 0xFFFFFF), SpatialArmourKit.argb(0.9f * a * (0.8f + 0.2f * Mth.sin(age * 0.2f)), SILVER));
        // a glint running over the glass and a soft light standing in front (the glass looks into the mirror world)
        SpatialArmourKit.camera(pose);
        float ph = (age * 0.03f) % 1f;
        SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.additive(glint)), (-0.5f + ph) * 1.3f * (shape == 3 ? 0.6f : 1f), 1.5f + (0.5f - ph) * 1.8f, -0.1f, 0.5f * Mth.sin(ph * Mth.PI),
                age * 0.03f, SpatialArmourKit.argb(0.9f * a, SILVER), FB);
        pose.popPose();
        pose.pushPose();
        pose.scale(k, k, k);
        pose.translate(0f, -1.5f, 0f);
        pose.translate(0f, 1.5f, -0.2f);
        SpatialArmourKit.plane(pose, buffers.getBuffer(AuraRender.additive(pane)), 0f, 0f, 0f, 0.75f, 0f, 0f, 0f, 1.35f, 0f, true, SpatialArmourKit.argb(0.16f * a, VIOLET), FB);
        pose.popPose();
    }

    private static void blade(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean falling = e.param() == 1;
        float k = 0.55f * e.scale();
        float on = Mth.clamp(age / 3f, 0f, 1f), out = Mth.clamp((e.maxLife() - age) / 3f, 0f, 1f), a = Math.min(on, out);
        if (a <= 0.01f) return;
        pose.pushPose();
        if (falling) pose.mulPose(Axis.XP.rotationDegrees(-90f));            // the point (the model's front, -z) looks down
        pose.scale(k * a, k * a, k * a);
        pose.translate(0f, -1f, 0f);
        GeoDraw.paint(pose, buffers, BLADE, "spin", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, SpatialArmourKit.argb(a, 0xFFFFFF),
                SpatialArmourKit.argb(0.95f * a, SILVER));
        SpatialArmourKit.camera(pose);
        SpatialArmourKit.streak(pose, buffers.getBuffer(AuraRender.additive(beam)), 0f, 1f, 2.6f, 0f, 1f, 0.2f, 0.4f, SpatialArmourKit.argb(0.5f * a, VIOLET), FB);
        SpatialArmourKit.glow(pose, buffers.getBuffer(AuraRender.additive(glint)), 0f, 1f, -1.5f, 0.45f * (0.7f + 0.3f * Mth.sin(age * 0.8f)), age * 0.2f, SpatialArmourKit.argb(0.9f * a, SILVER), FB);
        pose.popPose();
    }
}
