package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * 1.93.0: what the painters of the Water / Dark / Lightning / Flame summons share: the grow-in and fade-out over the prop's life, the turn to face the way it flies (yaw is the
 * renderer's, pitch is added here about the model's centre), and one call that draws a geo model (solid while fully there, translucent while it fades) and its glow map.
 */
public final class WaterPropDraw {
    private WaterPropDraw() {}

    /** 0..1: grows over the first 6 ticks of the prop's life, fades over the last 8. */
    public static float fade(MagicPropEntity e, float partial) {
        float t = e.life() + partial, left = e.maxLife() - t;
        return Mth.clamp(Math.min(t / 6f, left / 8f), 0f, 1f);
    }

    /** The size the painter draws at: the prop's scale, 70 percent of it while the prop is forming or fading. */
    public static float size(MagicPropEntity e, float fade) { return e.scale() * (0.7f + 0.3f * fade); }

    /** Pitches the pose about the model's centre (centre in blocks, already multiplied by the scale), by the entity's pitch. */
    public static void pitch(MagicPropEntity e, PoseStack pose, float partial, float centre) {
        float pitch = Mth.rotLerp(partial, e.xRotO, e.getXRot());
        if (Math.abs(pitch) < 0.01f) return;
        pose.translate(0f, centre, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.translate(0f, -centre, 0f);
    }

    /**
     * Draws the model with its glow map. {@code glass}: always translucent. The colour is white times the fade; the glow is {@code glowRgb} at {@code glowK} times the fade.
     */
    public static void model(PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String clip, float seconds, boolean glass, float fade, int light, int glowRgb, float glowK) {
        int white = ((int) (Mth.clamp(fade, 0f, 1f) * 255f + 0.5f) << 24) | 0xFFFFFF;
        int glow = ((int) (Mth.clamp(fade * glowK, 0f, 1f) * 255f + 0.5f) << 24) | (glowRgb & 0xFFFFFF);
        boolean translucent = glass || fade < 0.99f;
        GeoDraw.paint(pose, buffers, spec, clip, seconds, GeoDraw.Space.PROP, translucent ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, light, white, glow);
    }

    /**
     * The buffers a painter should use: a source hands out one live vertex builder at a time, and a painter that holds several consumers (a model, then ribbons, then sprites) would die
     * with "Not building!" on the second one; the lazy wrapper asks for the builder again at every vertex (client.aura.LazyBuffers).
     */
    public static MultiBufferSource lazy(MultiBufferSource b) { return b instanceof com.newuniverse.nusmp.client.aura.LazyBuffers ? b : new com.newuniverse.nusmp.client.aura.LazyBuffers(b); }

    public static float frac(float x) { return x - (float) Math.floor(x); }
}
