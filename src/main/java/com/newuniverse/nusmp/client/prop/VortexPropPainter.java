package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.AirPropWord;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Vortex Magic: how its summons look (VORTEX_1, variant in the low byte of param, see prop.AirPropWord).
 * <ul>
 *   <li>0, the maelstrom core: the geo model vortex_core (a glowing core in three gyroscope rings on three axes, five orbiting motes), its idle clip bobbing
 *       and spinning; a pale pulsing spiral lies on the ground under it, drawn additive 3 cm up, and the implode clip shrinks it over the last 10 ticks.</li>
 *   <li>1, the cyclone funnel: vortex_funnel (eight rings widening upwards, each turning against its neighbour), swelling out of the ground over 8 ticks and
 *       shrinking over the last 8; a dust spiral on the ground follows it.</li>
 * </ul>
 * Render types: models CUTOUT first (TRANSLUCENT while swelling / shrinking), glow maps ADDITIVE (GeoDraw), the ground spiral ADDITIVE full bright.
 */
public final class VortexPropPainter {
    private VortexPropPainter() {}

    private static final GeoSpec CORE = GeoSpec.of("vortex", "core"), FUNNEL = GeoSpec.of("vortex", "funnel");
    private static ResourceLocation spiral;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        spiral = AuraRender.tex("particle/vortex_spiral");
        PropPainters.register(PropKind.VORTEX_1, VortexPropPainter::paint);
    }

    private static void ground(PoseStack pose, MultiBufferSource buffers, float r, float rot, int argb) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(spiral));
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
        PropDraw.quad(pose, vc, -r, 0f, r, r, 0f, r, r, 0f, -r, -r, 0f, -r, 0f, 0f, 1f, 1f, argb, PropDraw.FULL_BRIGHT);
        pose.popPose();
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int v = AirPropWord.variant(e.param());
        float s = Math.max(0.3f, e.scale());
        float left = Math.max(1f, e.maxLife()) - age;
        float in = Mth.clamp(age / 8f, 0f, 1f), out = Mth.clamp(left / 8f, 0f, 1f), a = in * out;
        boolean solid = a >= 0.99f;
        int fade = (Mth.clamp((int) (a * 255f), 0, 255) << 24);
        if (v == 0) {
            ground(pose, buffers, 2.2f * s * a, age * 5f, fade | 0x7ADCEB);
            pose.pushPose();
            pose.translate(0f, 0.5f * s, 0f);
            float sc = s * 0.85f;
            boolean imp = left < 10f;
            pose.scale(sc, sc, sc);
            pose.translate(0f, -0.1f, 0f);
            GeoDraw.paint(pose, buffers, CORE, imp ? "implode" : "idle", imp ? (10f - left) / 20f : age / 20f, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                    solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, a), AuraRender.alpha(0xFFB4FFFF, (0.75f + 0.25f * Mth.sin(age * 0.25f)) * a));
            pose.popPose();
        } else {
            ground(pose, buffers, 2.6f * s * a, -age * 6f, fade | 0x59B8C9);
            pose.pushPose();
            float sc = s * 1.5f * (0.4f + 0.6f * a);
            pose.scale(sc, sc * (0.5f + 0.5f * a), sc);
            GeoDraw.paint(pose, buffers, FUNNEL, "idle", age / 20f, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                    solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, a), AuraRender.alpha(0xFFB4FFFF, 0.7f * a));
            pose.popPose();
        }
    }
}
