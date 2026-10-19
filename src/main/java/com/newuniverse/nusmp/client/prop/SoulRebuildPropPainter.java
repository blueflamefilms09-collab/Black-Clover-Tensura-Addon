package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Soul Magic: how the two props look (GeoLite models of tools/gen_soul_rebuild_geo.py; the behaviours are prop.SoulRebuildProps).
 * SOUL_1, the soul wisp: soul_wisp, translucent (a spirit: alpha blended, its glow map additive on top), clip "attack" while param bit 8 is set (lunge, jaw wide) and "idle"
 * otherwise; a soul flame glows in its chest and, while it lunges, a flame ribbon streams behind it (particle/soul_flame, additive, camera-facing). It fades in over
 * 10 ticks and out over the last 10; a wisp of the Soul Ascension (bit 9) swells and brightens over its last 20 ticks until it bursts.
 * SOUL_2, the soul-flame orb: soul_orb (cutout, glow additive) laid along the flight (pitched by the entity's x rotation) with its tail behind it, three flame sprites
 * trailing and a glow (additive). Pass order: the model first, every additive pass after it. Every texture exists (particle/soul_*.png, entity/soul_*).
 */
public final class SoulRebuildPropPainter {
    private SoulRebuildPropPainter() {}

    private static final GeoSpec WISP = GeoSpec.of("soul", "wisp"), ORB = GeoSpec.of("soul", "orb");
    private static final int FB = PropDraw.FULL_BRIGHT, TEAL = 0x72D9CC, PALE = 0xE6FFFA;
    private static ResourceLocation flame, glow, wispTex;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        flame = AuraRender.tex("particle/soul_flame");
        wispTex = AuraRender.tex("particle/soul_wisp");
        glow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.SOUL_1, SoulRebuildPropPainter::wisp);
        PropPainters.register(PropKind.SOUL_2, SoulRebuildPropPainter::orb);
    }

    private static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return SealDraw.smooth(life / in) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    private static void wisp(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 10f, (e.param() & 0x200) != 0 ? 1f : 10f);
        if (k <= 0.01f) return;
        boolean attack = (e.param() & 0x100) != 0, ascend = (e.param() & 0x200) != 0;
        float swell = ascend ? 1f + 0.35f * SealDraw.smooth((e.life() + partial - (e.maxLife() - 20f)) / 20f) : 1f;
        float s = e.scale() * (0.7f + 0.3f * k) * swell;
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, WISP, attack ? "attack" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, SealDraw.lift(light, 8),
                SealDraw.argb(0.9f * k, 0xFFFFFF), SealDraw.argb(Mth.clamp(k * (attack || ascend ? 1f : 0.65f), 0f, 1f), 0xFFFFFF));
        pose.popPose();
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, vc, 0f, 1.2f * s, 0f, (0.55f + 0.08f * Mth.sin(age * 0.2f)) * s, 0f, SealDraw.argb(0.45f * k, TEAL), FB);
        if (attack || ascend) {
            VertexConsumer fl = buf.getBuffer(AuraRender.additive(flame));
            for (int j = 0; j < 3; j++)
                SealDraw.glow(pose, fl, Mth.sin(age * 0.7f + j) * 0.2f * s, (0.9f + 0.4f * j) * s, -(0.3f + 0.35f * j) * s, 0.28f * s, Mth.sin(age * 0.3f + j) * 0.3f, SealDraw.argb(0.85f * k, j == 1 ? PALE : TEAL), FB);
        }
    }

    private static void orb(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 2f, 2f);
        if (k <= 0.01f) return;
        float s = e.scale() * 0.7f * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0f, -0.5f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, ORB, "strike", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SealDraw.argb(0.95f * k, 0xFFFFFF));
        pose.popPose();
        VertexConsumer fl = buf.getBuffer(AuraRender.additive(flame));
        for (int j = 0; j < 3; j++) {
            float z = -(0.6f + 0.55f * j) * s;
            SealDraw.glow(pose, fl, Mth.sin(age * 0.5f + j * 2f) * 0.18f * s, Mth.sin(age * 0.4f + j) * 0.12f * s, z, (0.55f - 0.1f * j) * s, Mth.sin(age * 0.3f + j) * 0.3f, SealDraw.argb(0.8f * k * (1f - 0.2f * j), j == 0 ? PALE : TEAL), FB);
        }
        VertexConsumer g = buf.getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, g, 0f, 0f, 0f, 0.9f * s, 0f, SealDraw.argb(0.45f * k, TEAL), FB);
        VertexConsumer w = buf.getBuffer(AuraRender.additive(wispTex));
        SealDraw.glow(pose, w, 0f, 0.1f * s, 0f, 0.55f * s, Mth.sin(age * 0.2f) * 0.2f, SealDraw.argb(0.7f * k, PALE), FB);
    }
}
