package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * 1.93.0 Spatial Magic: the two summon models. Both are GeoLite models (tools/gen_spatial_geo.py) standing on the prop's feet (Space.PROP), drawn with the
 * solid CUTOUT render type and their glow map additive, then a few additive sprites over them:
 * <ul>
 *   <li>SPATIAL_1, the gate: the gold frame, a pearl ring and a void ring turning inside it and the lens of folded space. It opens over the first
 *       second (clip "open"), idles, and closes over the last second (clip "close"). A second pass lays the spatial_fold swirl in the gate's plane
 *       (additive, full bright, both sides) so the opening is bright from either side; a thin runic ring turns round the frame.</li>
 *   <li>SPATIAL_2, the rift: the standing tear with its jagged lips, a white seam and drifting fragments (clips open / idle / close), with the spatial_rift
 *       sprite behind it (additive) and a glow at its heart. param 1 (a collapse) draws it 1.6 times bigger and spins the fragments faster.</li>
 * </ul>
 */
public final class SpatialPropPainter {
    private SpatialPropPainter() {}

    private static final GeoSpec GATE = GeoSpec.of("spatial", "gate"), RIFT = GeoSpec.of("spatial", "rift");
    private static final int VIOLET = 0xB088FF, PALE = 0xE6DCFF, FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation fold, ring, rift, star;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        fold = AuraRender.tex("particle/spatial_fold");
        ring = AuraRender.tex("particle/spatial_ring");
        rift = AuraRender.tex("particle/spatial_rift");
        star = AuraRender.tex("particle/spatial_star");
        PropPainters.register(PropKind.SPATIAL_1, SpatialPropPainter::gate);
        PropPainters.register(PropKind.SPATIAL_2, SpatialPropPainter::rift);
    }

    /** The clip and its clock for a prop that opens in 20 ticks and closes in the last 20. */
    private static String clip(MagicPropEntity e, float age, float[] clock) {
        float left = e.maxLife() - age;
        if (age < 20f) { clock[0] = age / 20f; return "open"; }
        if (left < 20f) { clock[0] = Math.max(0f, (20f - left) / 20f); return "close"; }
        clock[0] = age / 20f;
        return "idle";
    }

    private static void gate(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = 0.75f * e.scale();
        float[] t = new float[1];
        String clip = clip(e, age, t);
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.2f);
        float on = Math.min(Mth.clamp(age / 20f, 0f, 1f), Mth.clamp((e.maxLife() - age) / 20f, 0f, 1f));
        on = on * on * on;
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, GATE, clip, t[0], GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SpatialArmourKit.argb(0.9f * pulse, PALE));
        // the swirl in the gate's plane (y up, the front +z): a quad centred 1.5 blocks up, both sides
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(fold));
        float r = 0.93f * on, a = age * 0.12f;
        float c = Mth.cos(a) * r, sn = Mth.sin(a) * r;
        SpatialArmourKit.plane(pose, vc, 0f, 1.5f, 0.02f, c, sn, 0f, -sn, c, 0f, false, SpatialArmourKit.argb(0.65f * on * pulse, VIOLET), FB);
        SpatialArmourKit.plane(pose, vc, 0f, 1.5f, -0.02f, c, sn, 0f, -sn, c, 0f, true, SpatialArmourKit.argb(0.65f * on * pulse, VIOLET), FB);
        VertexConsumer rv = buffers.getBuffer(AuraRender.additive(ring));
        float rr = 1.7f * on, b = -age * 0.05f;
        float c2 = Mth.cos(b) * rr, s2 = Mth.sin(b) * rr;
        SpatialArmourKit.plane(pose, rv, 0f, 1.5f, 0.05f, c2, s2, 0f, -s2, c2, 0f, false, SpatialArmourKit.argb(0.55f * on, VIOLET), FB);
        SpatialArmourKit.plane(pose, rv, 0f, 1.5f, -0.05f, c2, s2, 0f, -s2, c2, 0f, true, SpatialArmourKit.argb(0.55f * on, VIOLET), FB);
        pose.popPose();
    }

    private static void rift(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = 0.7f * e.scale() * (e.param() == 1 ? 1.6f : 1f);
        float[] t = new float[1];
        String clip = clip(e, age, t);
        float on = Math.min(Mth.clamp(age / 12f, 0f, 1f), Mth.clamp((e.maxLife() - age) / 12f, 0f, 1f));
        float flick = 0.75f + 0.25f * Mth.sin(age * 0.37f);
        pose.pushPose();
        pose.scale(s, s, s);
        // the sprite behind and in front: a lens of light the size of the tear (additive, full bright)
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(rift));
        float hw = 0.85f * on, hh = 1.65f * on;
        SpatialArmourKit.plane(pose, vc, 0f, 1.5f, 0.12f, hw, 0f, 0f, 0f, hh, 0f, false, SpatialArmourKit.argb(0.5f * on * flick, VIOLET), FB);
        SpatialArmourKit.plane(pose, vc, 0f, 1.5f, -0.12f, hw, 0f, 0f, 0f, hh, 0f, true, SpatialArmourKit.argb(0.5f * on * flick, VIOLET), FB);
        GeoDraw.paint(pose, buffers, RIFT, clip, t[0] * (e.param() == 1 ? 1.8f : 1f), GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF,
                SpatialArmourKit.argb(0.95f * flick, PALE));
        VertexConsumer sv = buffers.getBuffer(AuraRender.additive(star));
        float k = 0.35f * on * (0.7f + 0.3f * flick);
        SpatialArmourKit.plane(pose, sv, 0f, 1.5f, 0.15f, k, 0f, 0f, 0f, k * 2.4f, 0f, false, SpatialArmourKit.argb(0.8f * on, PALE), FB);
        pose.popPose();
    }
}
