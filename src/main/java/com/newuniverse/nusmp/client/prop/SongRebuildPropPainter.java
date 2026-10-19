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
 * 1.93.0 Song Magic: how the two props look (GeoLite models of tools/gen_song_rebuild_geo.py; the behaviours are prop.SongRebuildProps).
 * SONG_1 param 0, the note bolt: song_note (cutout, glow additive) in its fast "strike" spin, with a trail of staff lines behind it (a ribbon of particle/song_staff,
 * additive, full bright) and a camera-facing note glow. Param 1, the metronome: song_metronome (cutout, glow) swinging its pendulum once a second, a ring of
 * particle/song_wave on the ground that pulses with every beat (additive, flat).
 * SONG_2, the choir spirit: song_choir, translucent (a spirit: alpha blended, the glow map additive on top), clip "sing" while param bit 8 is set and "idle"
 * otherwise; it fades in over 10 ticks and out over the last 10; two note glints orbit its head while it sings (additive, camera-facing).
 * Pass order: model passes first (solid / translucent), then every additive pass. Every texture exists (particle/song_*.png, entity/song_*).
 */
public final class SongRebuildPropPainter {
    private SongRebuildPropPainter() {}

    private static final GeoSpec NOTE = GeoSpec.of("song", "note"), CHOIR = GeoSpec.of("song", "choir"), METRO = GeoSpec.of("song", "metronome");
    private static final int FB = PropDraw.FULL_BRIGHT, ROSE = 0xFFB4E8, PALE = 0xFFE8F8;
    private static ResourceLocation staff, wave, glow, noteTex;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        staff = AuraRender.tex("particle/song_staff");
        wave = AuraRender.tex("particle/song_wave");
        glow = AuraRender.tex("particle/glow");
        noteTex = AuraRender.tex("particle/song_note");
        PropPainters.register(PropKind.SONG_1, SongRebuildPropPainter::one);
        PropPainters.register(PropKind.SONG_2, SongRebuildPropPainter::choir);
    }

    private static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return SealDraw.smooth(life / in) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    private static void one(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        if ((e.param() & 0xFF) == 1) metronome(e, partial, age, pose, buf, light);
        else note(e, partial, age, pose, buf, light);
    }

    // ================================================================ the note bolt
    private static void note(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 2f, 2f);
        if (k <= 0.01f) return;
        float s = e.scale() * 0.75f * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.translate(0f, -0.35f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, NOTE, "strike", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SealDraw.argb(0.8f * k, 0xFFFFFF));
        pose.popPose();
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(staff));                       // the staff trail: five lines streaming back along the flight
        for (int side = -1; side <= 1; side += 2)
            SealDraw.ribbon(pose, vc, 0f, 0f, -0.2f * s, side * 0.05f * s, 0.04f * side * s, -2.4f * s, 0.2f * s, 1f, 0f, SealDraw.argb(0.8f * k, ROSE), FB);
        vc = buf.getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, vc, 0f, 0f, 0f, 0.55f * s, age * 0.2f, SealDraw.argb(0.5f * k, ROSE), FB);
        SealDraw.glow(pose, vc, 0f, 0f, 0f, 0.22f * s, -age * 0.3f, SealDraw.argb(0.9f * k, PALE), FB);
    }

    // ================================================================ the metronome
    private static void metronome(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 8f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale() * (0.5f + 0.5f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, METRO, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, SealDraw.argb(1f, 0xFFFFFF), SealDraw.argb(0.8f * k, 0xFFFFFF));
        pose.popPose();
        float beat = (age / 20f) % 1f, pulse = 1f - beat;
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(wave));
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        float r = 6f * e.scale() * (0.35f + 0.65f * beat);
        SealDraw.flat(pose, vc, r, age * 1.2f, SealDraw.argb(0.7f * k * pulse, ROSE), FB);
        SealDraw.flat(pose, vc, 6f * e.scale() * 0.5f, -age * 0.8f, SealDraw.argb(0.25f * k, PALE), FB);
        pose.popPose();
    }

    // ================================================================ the choir spirit
    private static void choir(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 10f, 10f);
        if (k <= 0.01f) return;
        boolean sing = (e.param() & 0x100) != 0;
        float s = e.scale() * (0.7f + 0.3f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, CHOIR, sing ? "sing" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, SealDraw.lift(light, 8),
                SealDraw.argb(0.9f * k, 0xFFFFFF), SealDraw.argb(Mth.clamp(k * (sing ? 1f : 0.6f), 0f, 1f), 0xFFFFFF));
        pose.popPose();
        if (sing) {
            VertexConsumer vc = buf.getBuffer(AuraRender.additive(noteTex));
            for (int j = 0; j < 2; j++) {
                float a = age * 0.2f + j * Mth.PI;
                SealDraw.glow(pose, vc, Mth.cos(a) * 0.5f * s, (2.15f + 0.12f * j) * s, Mth.sin(a) * 0.5f * s, 0.16f * s, 0f, SealDraw.argb(0.9f * k, j == 0 ? ROSE : PALE), FB);
            }
        }
    }
}
