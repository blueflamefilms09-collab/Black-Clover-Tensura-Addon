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
 * 1.93.0 Permeation Magic: the summon models, GeoLite models (tools/gen_permeation_geo.py), both see-through (TRANSLUCENT) with their glow maps additive:
 * <ul>
 *   <li>PERMEATION_1, the phase echo: the hollow ghost of bars and wisps the caster left standing; it appears in 3 ticks at 70 percent, then fades out over its life, bobbing
 *       and trailing its tail of wisps; a ring ripple runs out on the ground under it and bubbles of nothing rise from it.</li>
 *   <li>PERMEATION_2, the phase door: a doorway of thin bars with a membrane of wisps that opens over the first 12 ticks (clip open), idles, and shuts over the last 12 (clip
 *       close). The permeation_door texture is laid in the doorway's plane (additive, both sides) and ripples spread over it.</li>
 * </ul>
 */
public final class PermeationPropPainter {
    private PermeationPropPainter() {}

    private static final GeoSpec ECHO = GeoSpec.of("permeation", "echo"), DOOR = GeoSpec.of("permeation", "door");
    private static final int CYAN = 0x65D7E8, PALE = 0xDFFBFF, FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation ripple, door, mote;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        ripple = AuraRender.tex("particle/permeation_ripple");
        door = AuraRender.tex("particle/permeation_door");
        mote = AuraRender.tex("particle/permeation_mote");
        PropPainters.register(PropKind.PERMEATION_1, PermeationPropPainter::echo);
        PropPainters.register(PropKind.PERMEATION_2, PermeationPropPainter::door);
    }

    private static void echo(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float a = Mth.clamp(age / 3f, 0f, 1f) * 0.7f * (1f - e.lifeFrac());
        if (a <= 0.01f) return;
        float s = 0.9f * e.scale();
        pose.pushPose();
        pose.pushPose();
        pose.translate(0f, 0.03f, 0f);
        float wave = (age * 0.04f) % 1f;
        SpatialArmourKit.flat(pose, buffers.getBuffer(AuraRender.additive(ripple)), 0.5f + 1.1f * wave, 0f, SpatialArmourKit.argb(0.6f * a * (1f - wave), CYAN), FB);
        pose.popPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, ECHO, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, SpatialArmourKit.argb(a, 0xFFFFFF), SpatialArmourKit.argb(a * 1.2f, PALE));
        SpatialArmourKit.camera(pose);
        VertexConsumer mv = buffers.getBuffer(AuraRender.additive(mote));
        for (int i = 0; i < 4; i++) {
            float ph = (age * 0.03f + i * 0.25f) % 1f, an = i * 2.399f;
            SpatialArmourKit.glow(pose, mv, Mth.cos(an) * 0.5f, 0.2f + ph * 2.2f, Mth.sin(an) * 0.4f, 0.1f, 0f, SpatialArmourKit.argb(0.8f * a * Mth.sin(ph * Mth.PI), PALE), FB);
        }
        pose.popPose();
    }

    private static void door(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float k = 0.75f * e.scale();
        float left = e.maxLife() - age;
        String clip = age < 12f ? "open" : left < 12f ? "close" : "idle";
        float t = age < 12f ? age / 20f : left < 12f ? (12f - left) / 20f : age / 20f;
        float on = Math.min(Mth.clamp(age / 12f, 0f, 1f), Mth.clamp(left / 12f, 0f, 1f));
        pose.pushPose();
        pose.scale(k, k, k);
        GeoDraw.paint(pose, buffers, DOOR, clip, t, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, SpatialArmourKit.argb(0.6f + 0.4f * on, 0xFFFFFF), SpatialArmourKit.argb(on, PALE));
        // the doorway's plane (the front is +z): the door texture over the opening, both sides, and a ripple that spreads over it
        float hw = 0.82f * on, hh = 1.5f;
        VertexConsumer dv = buffers.getBuffer(AuraRender.additive(door));
        SpatialArmourKit.plane(pose, dv, 0f, 1.5f, 0.05f, hw, 0f, 0f, 0f, hh, 0f, false, SpatialArmourKit.argb(0.5f * on, CYAN), FB);
        SpatialArmourKit.plane(pose, dv, 0f, 1.5f, -0.05f, hw, 0f, 0f, 0f, hh, 0f, true, SpatialArmourKit.argb(0.5f * on, CYAN), FB);
        float ph = (age * 0.04f) % 1f;
        VertexConsumer rv = buffers.getBuffer(AuraRender.additive(ripple));
        float rr = 0.2f + 1.0f * ph;
        SpatialArmourKit.plane(pose, rv, 0f, 1.5f, 0.07f, rr, 0f, 0f, 0f, rr, 0f, false, SpatialArmourKit.argb(0.5f * on * (1f - ph), PALE), FB);
        SpatialArmourKit.plane(pose, rv, 0f, 1.5f, -0.07f, rr, 0f, 0f, 0f, rr, 0f, true, SpatialArmourKit.argb(0.5f * on * (1f - ph), PALE), FB);
        pose.popPose();
    }
}
