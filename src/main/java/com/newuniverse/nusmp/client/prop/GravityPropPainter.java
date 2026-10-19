package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.GravitySwordParam;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Gravity Magic: the summon models, both GeoLite models (tools/gen_gravity_geo.py) on the prop's feet (Space.PROP), solid CUTOUT with the glow map additive:
 * <ul>
 *   <li>GRAVITY_1, the singularity orb: the black core of stacked cubes, three accretion rings of different tilt and speed, orbiting lensing shards (clip idle,
 *       or fire while it flies: the core stretches along the flight). Over it, camera-facing: a black disc with a bright rim (gravity_disc, alpha blended, so the
 *       core reads as a hole in the air) and a lensing ring (gravity_lens, additive), and a dark pool of pull lines that crowd inwards.</li>
 *   <li>GRAVITY_2, the compressed rock sword: the layered blade with violet fractures, rubble pressed into the flats, a rough guard and grip, shards turning round it.
 *       Forming it rises with the ground cracking under it (gravity_crack on the ground, additive); hovering it bobs; launched (clip launch) it turns tip first along
 *       its pitch. The pitch is in the param (see GravitySwordParam).</li>
 * </ul>
 */
public final class GravityPropPainter {
    private GravityPropPainter() {}

    private static final GeoSpec ORB = GeoSpec.of("gravity", "orb"), SWORD = GeoSpec.of("gravity", "sword");
    private static final int VIOLET = 0xB45AFF, PALE = 0xE6C8FF, FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation disc, lens, crack, ring;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        disc = AuraRender.tex("particle/gravity_disc");
        lens = AuraRender.tex("particle/gravity_lens");
        crack = AuraRender.tex("particle/gravity_crack");
        ring = AuraRender.tex("particle/gravity_ring");
        PropPainters.register(PropKind.GRAVITY_1, GravityPropPainter::orb);
        PropPainters.register(PropKind.GRAVITY_2, GravityPropPainter::sword);
    }

    private static void orb(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = 0.5f * e.scale();
        boolean flying = (e.param() & 1) != 0;
        float on = Mth.clamp(age / 8f, 0f, 1f), out = Mth.clamp((e.maxLife() - age) / 8f, 0f, 1f), k = Math.min(on, out);
        pose.pushPose();
        pose.scale(s * k, s * k, s * k);
        GeoDraw.paint(pose, buffers, ORB, flying ? "fire" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SpatialArmourKit.argb(0.9f, PALE));
        // the hole in the air and its lensing ring, always facing the camera (centre of the core: 16 px up = 1 block)
        SpatialArmourKit.camera(pose);
        VertexConsumer dv = buffers.getBuffer(AuraRender.translucent(disc));
        SpatialArmourKit.glow(pose, dv, 0f, 1f, 0f, 1.12f, 0f, 0xFFFFFFFF, light);
        VertexConsumer lv = buffers.getBuffer(AuraRender.additive(lens));
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.3f);
        SpatialArmourKit.glow(pose, lv, 0f, 1f, 0f, 1.7f * pulse, age * 0.02f, SpatialArmourKit.argb(0.55f, VIOLET), FB);
        SpatialArmourKit.glow(pose, lv, 0f, 1f, 0f, 2.3f, -age * 0.015f, SpatialArmourKit.argb(0.25f, PALE), FB);
        pose.popPose();
    }

    private static void sword(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = 0.9f * e.scale();
        int stage = GravitySwordParam.stageOf(e.param());
        float pitch = GravitySwordParam.pitchOf(e.param());
        float on = Mth.clamp(age / 10f, 0f, 1f), out = Mth.clamp((e.maxLife() - age) / 8f, 0f, 1f);
        pose.pushPose();
        if (stage == GravitySwordParam.FORMING) {                                       // cracks in the ground under the rising stone
            pose.pushPose();
            pose.translate(0f, 0.03f, 0f);
            SpatialArmourKit.flat(pose, buffers.getBuffer(AuraRender.additive(crack)), 1.6f * s * on, 0f, SpatialArmourKit.argb(0.8f * on * out, VIOLET), FB);
            SpatialArmourKit.flat(pose, buffers.getBuffer(AuraRender.additive(ring)), 1.2f * s * (0.5f + 0.5f * on), age * 2f, SpatialArmourKit.argb(0.5f * on * out, PALE), FB);
            pose.popPose();
        }
        pose.translate(0f, 1f * s, 0f);                                            // the model's middle (16 px) on the prop's position
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        float k = Math.min(on, out);
        pose.scale(s * k, s * k, s * k);
        pose.translate(0f, -1f, 0f);
        GeoDraw.paint(pose, buffers, SWORD, stage == GravitySwordParam.LAUNCHED ? "launch" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF,
                SpatialArmourKit.argb((0.7f + 0.3f * Mth.sin(age * 0.25f)) * (stage == GravitySwordParam.FORMING ? 0.7f : 1f), PALE));
        pose.popPose();
    }
}
