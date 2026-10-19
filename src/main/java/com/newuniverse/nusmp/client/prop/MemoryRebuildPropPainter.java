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
 * 1.93.0 Memory Magic: how the two props look (GeoLite models of tools/gen_memory_rebuild_geo.py; the behaviours are prop.MemoryRebuildProps).
 * MEMORY_1, the memory echo: memory_echo, translucent (an afterimage: alpha blended with a flicker, its glow map additive on top), clip "strike" while param bit 8 is set and
 * "idle" otherwise; every time it blinks (its position jumps) a flash of particle/memory_spark opens on it (additive, camera-facing); it fades in over 8 ticks and out over the
 * last 10. MEMORY_2, the recollection orb: memory_orb (cutout, glow additive) laid along the flight with three small photo frames orbiting it (they are part of the model) and a
 * trail of frames (particle/memory_frame, additive, camera-facing) and sparks behind it. Pass order: the model first, every additive pass after it. Every texture exists
 * (particle/memory_*.png, entity/memory_*).
 */
public final class MemoryRebuildPropPainter {
    private MemoryRebuildPropPainter() {}

    private static final GeoSpec ECHO = GeoSpec.of("memory", "echo"), ORB = GeoSpec.of("memory", "orb");
    private static final int FB = PropDraw.FULL_BRIGHT, LAV = 0xBCA8FF, PALE = 0xF0EAFF;
    private static ResourceLocation frame, spark, glow;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        frame = AuraRender.tex("particle/memory_frame");
        spark = AuraRender.tex("particle/memory_spark");
        glow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.MEMORY_1, MemoryRebuildPropPainter::echo);
        PropPainters.register(PropKind.MEMORY_2, MemoryRebuildPropPainter::orb);
    }

    private static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return SealDraw.smooth(life / in) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    private static void echo(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 8f, 10f);
        if (k <= 0.01f) return;
        boolean strike = (e.param() & 0x100) != 0;
        float flick = 0.82f + 0.18f * Mth.sin(age * 1.3f) * Mth.sin(age * 0.37f);
        double jump = Math.abs(e.getX() - e.xo) + Math.abs(e.getZ() - e.zo);
        float s = e.scale() * 0.95f * (0.8f + 0.2f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, ECHO, strike ? "strike" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, SealDraw.lift(light, 9),
                SealDraw.argb(0.85f * k * flick, 0xFFFFFF), SealDraw.argb(Mth.clamp(k * (0.6f + 0.3f * flick), 0f, 1f), 0xFFFFFF));
        pose.popPose();
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(spark));
        if (jump > 0.5) SealDraw.glow(pose, vc, 0f, 1.0f * s, 0f, 1.6f * s, age * 0.5f, SealDraw.argb(0.9f * k, PALE), FB);        // the blink
        for (int j = 0; j < 3; j++) {
            float ph = (age * 0.02f + j * 0.33f) % 1f;
            SealDraw.glow(pose, vc, Mth.sin(j * 2.1f + age * 0.05f) * 0.5f * s, (0.2f + ph * 2.0f) * s, Mth.cos(j * 2.1f) * 0.3f * s, 0.12f * s, age * 0.1f + j, SealDraw.argb(0.8f * k * Mth.sin(ph * Mth.PI), LAV), FB);
        }
    }

    private static void orb(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 2f, 2f);
        if (k <= 0.01f) return;
        float s = e.scale() * 0.8f * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0f, -0.5f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, ORB, "strike", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SealDraw.argb(0.95f * k, 0xFFFFFF));
        pose.popPose();
        VertexConsumer fr = buf.getBuffer(AuraRender.additive(frame));
        for (int j = 0; j < 3; j++) {
            float z = -(0.9f + 0.6f * j) * s;
            SealDraw.glow(pose, fr, Mth.sin(age * 0.3f + j * 2f) * 0.1f * s, Mth.sin(age * 0.2f + j) * 0.1f * s, z, (0.62f - 0.12f * j) * s, 0.3f * (j - 1) + Mth.sin(age * 0.2f) * 0.1f, SealDraw.argb(0.7f * k * (1f - 0.25f * j), j == 1 ? PALE : LAV), FB);
        }
        VertexConsumer g = buf.getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, g, 0f, 0f, 0f, 1.0f * s, 0f, SealDraw.argb(0.4f * k, LAV), FB);
        VertexConsumer sp = buf.getBuffer(AuraRender.additive(spark));
        SealDraw.glow(pose, sp, 0f, 0f, 0f, 0.5f * s, age * 0.3f, SealDraw.argb(0.8f * k, PALE), FB);
    }
}
