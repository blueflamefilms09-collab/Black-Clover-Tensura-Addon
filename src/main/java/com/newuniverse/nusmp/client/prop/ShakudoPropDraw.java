package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.ShakudoPropState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0: the drawing helpers the prop painters of Shakudo, Scale, Spike, Nail and Hair Magic share. Every construct is one geo model (cutout, with its
 * glow map additive and full bright, drawn by {@link GeoDraw}) played with a clip chosen from the synced state word of {@link ShakudoPropState}
 * (idle / moving / the one-shot clips from the tick the state began), a soft ground shadow (translucent, 2 cm over the ground so nothing is coplanar)
 * and an optional additive ground glyph. Render order: shadow (translucent), model (cutout), glow map (additive), glyph (additive).
 */
public final class ShakudoPropDraw {
    private ShakudoPropDraw() {}

    private static final ResourceLocation SHADOW = AuraRender.tex("particle/glow");

    public static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    /** The state of the prop's synced word and the ticks since it began (with the partial tick). */
    public static int state(MagicPropEntity e) { return ShakudoPropState.state(e.param()); }

    public static float since(MagicPropEntity e, float partial) { return ShakudoPropState.since(e.param(), e.level().getGameTime()) + partial; }

    public static boolean moving(MagicPropEntity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return dx * dx + dz * dz > 1.0e-4;
    }

    /** A soft dark disc on the ground. */
    public static void shade(PoseStack pose, MultiBufferSource buffers, float r, float alpha, int light) {
        if (alpha <= 0.01f || r <= 0.01f) return;
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(SHADOW));
        int a = Mth.clamp((int) (alpha * 255f), 0, 255) << 24;
        PropDraw.quad(pose, vc, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, a | 0x101008, light);
    }

    /** A flat additive glyph on the ground (a seal, a crack decal), turned by rot radians, seen from both sides. */
    public static void glyph(PoseStack pose, MultiBufferSource buffers, ResourceLocation tex, float half, float rot, int argb) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(tex));
        pose.pushPose();
        pose.translate(0f, 0.03f, 0f);
        SealDraw.flat(pose, vc, half, rot * Mth.RAD_TO_DEG, argb, PropDraw.FULL_BRIGHT);
        pose.popPose();
    }

    /** The model with a clip (seconds), at a uniform scale, optionally lifted; glow colour rgb with the given strength 0..1. */
    public static void model(PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String clip, float seconds, float scale, float lift, int light, int glowRgb, float glow) {
        pose.pushPose();
        pose.translate(0f, lift, 0f);
        pose.scale(scale, scale, scale);
        GeoDraw.paint(pose, buffers, spec, clip, seconds, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFF000000 | glowRgb, Mth.clamp(glow, 0f, 1f)));
        pose.popPose();
    }

    /** 0 at the start of a life, 1 after {@code in} ticks. */
    public static float grow(float age, float in) { return smooth(age / in); }

    /** 1 until the last {@code out} ticks of the life, then down to 0. */
    public static float fade(MagicPropEntity e, float age, float out) { return smooth((Math.max(1f, e.maxLife()) - age) / out); }
}
