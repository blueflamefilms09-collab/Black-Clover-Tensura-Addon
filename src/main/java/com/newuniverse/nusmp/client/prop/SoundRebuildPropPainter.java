package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Sound Magic: how the two props look (GeoLite models of tools/gen_sound_rebuild_geo.py; the behaviours are prop.SoundRebuildProps).
 * SOUND_1, the speaker stack: sound_speaker (cutout, glow map additive on top); clip "pulse" while param bit 8 is set (the woofer thumps, the cabinet shakes) and "idle"
 * otherwise; on every pulse a ring of particle/sound_wave runs out of the woofer along the ground and a ring facing out of the cone (additive, full bright). It grows
 * in over 8 ticks and shrinks out over the last 8.
 * SOUND_2, the tuning-fork bolt: sound_fork (cutout, glow additive) in its "strike" clip, laid along the flight (pitched by the entity's x rotation), with a waveform ribbon
 * behind it (particle/sound_beam_v is for the VFX beam; the painter uses the horizontal particle/sound_wave rings instead): three rings that trail it, additive.
 * Pass order: the model first, every additive pass after it. Every texture exists (particle/sound_*.png, entity/sound_*).
 */
public final class SoundRebuildPropPainter {
    private SoundRebuildPropPainter() {}

    private static final GeoSpec SPEAKER = GeoSpec.of("sound", "speaker"), FORK = GeoSpec.of("sound", "fork");
    private static final int FB = PropDraw.FULL_BRIGHT, CYAN = 0x70C8F0, PALE = 0xD8F4FF;
    private static ResourceLocation wave, shock, glow;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        wave = AuraRender.tex("particle/sound_wave");
        shock = AuraRender.tex("particle/sound_shock");
        glow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.SOUND_1, SoundRebuildPropPainter::speaker);
        PropPainters.register(PropKind.SOUND_2, SoundRebuildPropPainter::fork);
    }

    private static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return SealDraw.smooth(life / in) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    private static void speaker(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 8f, 8f);
        if (k <= 0.01f) return;
        boolean pulse = (e.param() & 0x100) != 0;
        float s = e.scale() * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, SPEAKER, pulse ? "pulse" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF,
                SealDraw.argb(Mth.clamp(k * (pulse ? 1f : 0.7f), 0f, 1f), 0xFFFFFF));
        pose.popPose();
        if (pulse) {
            float ph = Mth.clamp(((e.life() + partial) % 30f) / 8f, 0f, 1f);
            VfxConsumerHolder.run(buf, wave, vc -> {
                pose.pushPose();
                pose.translate(0f, 0.05f, 0f);
                SealDraw.flat(pose, vc, (0.6f + 2.4f * ph) * s, age * 0.6f, SealDraw.argb(0.75f * k * (1f - ph), CYAN), FB);
                pose.popPose();
                SealDraw.plane(pose, vc, 0f, 1.0f * s, -0.6f * s, (0.3f + 1.2f * ph) * s, 0f, 0f, 0f, (0.3f + 1.2f * ph) * s, 0f, true, SealDraw.argb(0.8f * k * (1f - ph), PALE), FB);
            });
        }
    }

    private static void fork(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 2f, 2f);
        if (k <= 0.01f) return;
        float s = e.scale() * 0.7f * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f + e.getXRot()));               // the model stands on its stem: lay it along the flight (+z)
        pose.translate(0f, -0.6f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, FORK, "strike", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SealDraw.argb(0.9f * k, 0xFFFFFF));
        pose.popPose();
        VfxConsumerHolder.run(buf, wave, vc -> {
            for (int j = 0; j < 3; j++) {
                float z = -(0.5f + 0.7f * j) * s, r = (0.35f + 0.12f * j + 0.05f * Mth.sin(age * 0.8f + j)) * s;
                SealDraw.plane(pose, vc, 0f, 0f, z, r, 0f, 0f, 0f, r, 0f, false, SealDraw.argb(0.7f * k * (1f - 0.25f * j), j == 1 ? PALE : CYAN), FB);
                SealDraw.plane(pose, vc, 0f, 0f, z, r, 0f, 0f, 0f, r, 0f, true, SealDraw.argb(0.7f * k * (1f - 0.25f * j), j == 1 ? PALE : CYAN), FB);
            }
        });
        VfxConsumerHolder.run(buf, glow, vc -> SealDraw.glow(pose, vc, 0f, 0f, 0.2f * s, 0.5f * s, age * 0.2f, SealDraw.argb(0.5f * k, CYAN), FB));
    }

    /** Takes an additive consumer for a texture and runs the drawing (kept in one place so the passes stay after the model). */
    private static final class VfxConsumerHolder {
        static void run(MultiBufferSource buf, ResourceLocation tex, java.util.function.Consumer<VertexConsumer> draw) {
            draw.accept(buf.getBuffer(AuraRender.additive(tex)));
        }
    }
}
