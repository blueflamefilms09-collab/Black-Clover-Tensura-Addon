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
 * 0.59 Beast Magic: how this magic's props look.
 * <ul>
 *   <li>BEAST_1, the spirit beast: the geo model of its variant (param bits 0..1: 0 beast_spirit_beast, the lion-wolf; 1 beast_spirit_bear;
 *       2 beast_spirit_rhino) with the clip of its state: appear (first 10 ticks), vanish (last 10), attack (param bits 2.. = the attack tick),
 *       run (it moved this tick) or idle. Passes: the model CUTOUT (TRANSLUCENT while it fades in or out), its glow map ADDITIVE, then
 *       camera-facing flame tongues (beast_flame, ADDITIVE) over the mane and the tail, embers behind it when it runs, and a ground sigil
 *       (beast_sigil, ADDITIVE, flat) that opens when it appears.</li>
 *   <li>BEAST_2, the claw rake: the beast_claw gashes (ADDITIVE, camera-facing) that hang in the air for 14 ticks where the beast bit.</li>
 * </ul>
 * Everything is a few thousand vertices at most and allocates nothing per frame.
 */
public final class BeastPropPainter {
    private BeastPropPainter() {}

    private static final GeoSpec[] SPECS = {GeoSpec.of("beast", "spirit_beast"), GeoSpec.of("beast", "spirit_bear"), GeoSpec.of("beast", "spirit_rhino")};
    private static final float[] BASE = {1.25f, 1.2f, 1.25f};                   // the model's size next to the prop's scale
    private static final float[] MANE_Y = {1.32f, 1.34f, 1.36f};                // flame crown height over the feet, at scale 1
    private static final float[] MANE_Z = {0.62f, 0.66f, 0.7f};
    private static final float[] TAIL_Z = {-1.45f, -1.0f, -1.1f};
    private static final int FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation flame, licks, sigil, claw, glint;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        flame = AuraRender.tex("particle/beast_flame");
        licks = AuraRender.tex("particle/beast_licks");
        sigil = AuraRender.tex("particle/beast_sigil");
        claw = AuraRender.tex("particle/beast_claw");
        glint = AuraRender.tex("particle/beast_glint");
        PropPainters.register(PropKind.BEAST_1, BeastPropPainter::beast);
        PropPainters.register(PropKind.BEAST_2, BeastPropPainter::rake);
    }

    // ------------------------------------------------------------------ the spirit beast
    private static void beast(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = Mth.clamp(param & 3, 0, 2), phase = param >> 2;
        float sc = e.scale();
        int life = e.life(), left = e.maxLife() - life;
        String clip;
        float t;
        float fade = 1f;
        if (life < 10) { clip = "appear"; t = (life + partial) / 20f; fade = Mth.clamp((life + partial) / 5f, 0f, 1f); }
        else if (left < 10) { clip = "vanish"; t = Mth.clamp((10 - left + partial) / 20f, 0f, 0.5f); fade = Mth.clamp((left - partial) / 7f, 0f, 1f); }
        else if (phase > 0) { clip = "attack"; t = (phase - 1 + partial) / 20f; }
        else {
            float dx = (float) (e.getX() - e.xo), dz = (float) (e.getZ() - e.zo);
            float sp = Mth.sqrt(dx * dx + dz * dz);
            if (sp > 0.05f) { clip = "run"; t = age / 20f * Mth.clamp(sp / 0.28f, 0.75f, 1.4f); }
            else { clip = "idle"; t = age / 20f; }
        }
        boolean running = "run".equals(clip);
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.25f);
        int lit = BeastDraw.lift(light, 9);
        pose.pushPose();
        pose.scale(sc * BASE[variant], sc * BASE[variant], sc * BASE[variant]);
        GeoDraw.Layer layer = fade < 0.999f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT;
        GeoDraw.paint(pose, buffers, SPECS[variant], clip, t, GeoDraw.Space.PROP, layer, lit, AuraRender.alpha(0xFFFFFFFF, fade),
                AuraRender.alpha(0xFFFFC890, (0.38f + 0.16f * pulse) * fade));
        pose.popPose();

        // additive: flames over the mane and tail tip, embers behind a running beast, the ground sigil
        BeastDraw.camera(pose);
        VertexConsumer fv = buffers.getBuffer(AuraRender.additive(flame));
        float k = sc * BASE[variant];
        float my = MANE_Y[variant] * k, mz = MANE_Z[variant] * k;
        float grow = "appear".equals(clip) ? 1f + 1.6f * (1f - Mth.clamp(t / 0.5f, 0f, 1f)) : "vanish".equals(clip) ? 1f + 2.2f * (t / 0.5f) : 1f;
        for (int i = 0; i < 4; i++) {
            float sw = Mth.sin(age * 0.17f + i * 1.7f);
            float h = (0.55f + 0.12f * Mth.sin(age * 0.3f + i * 2.3f)) * k * grow;
            BeastDraw.tongue(pose, fv, (i - 1.5f) * 0.2f * k + sw * 0.05f * k, my + 0.1f * k, mz - (i % 2) * 0.12f * k, 0.2f * k * grow, h, 1f,
                    AuraRender.alpha(0xFFFFFFFF, Mth.clamp(0.55f * fade * (running ? 1.2f : 1f), 0f, 1f)), FB);
        }
        float tailSwing = Mth.sin(age * (running ? 0.5f : 0.15f)) * 0.12f * k;
        BeastDraw.tongue(pose, fv, tailSwing, 0.75f * k, TAIL_Z[variant] * k, 0.22f * k * grow, 0.62f * k * grow, 1f, AuraRender.alpha(0xFFFFFFFF, 0.6f * fade), FB);
        if (running) {
            VertexConsumer lv = buffers.getBuffer(AuraRender.additive(licks));
            for (int i = 0; i < 5; i++) {
                float ph = (age * 0.09f + i * 0.2f) % 1f;
                float px = (BeastDraw.rnd(e.seed(), i, 1) - 0.5f) * 0.8f * k, pz = -(0.5f + ph * 1.6f) * k;
                float lu = (i % 4) * 0.25f;
                BeastDraw.cell(pose, lv, px, (0.12f + ph * 0.4f) * k, pz, 0.14f * k * (1f - ph * 0.5f), lu, AuraRender.alpha(0xFFFFFFFF, Mth.clamp(0.7f * (1f - ph) * fade, 0f, 1f)), FB);
            }
        }
        if (life < 22) {
            float f = life / 22f;
            VertexConsumer gv = buffers.getBuffer(AuraRender.additive(sigil));
            pose.pushPose();
            pose.translate(0f, 0.04f, 0f);
            BeastDraw.flat(pose, gv, (1.1f + 1.0f * f) * sc, age * 6f, AuraRender.alpha(0xFFFFB050, (1f - f) * 0.9f), FB);
            pose.popPose();
        }
        if (left < 12) {
            VertexConsumer gv = buffers.getBuffer(AuraRender.additive(glint));
            for (int i = 0; i < 6; i++) {
                float a = i * 1.047f + age * 0.2f, r = (0.5f + (12 - left) * 0.08f) * k;
                BeastDraw.glow(pose, gv, Mth.cos(a) * r, 0.5f * k + (12 - left) * 0.06f * k, Mth.sin(a) * r, 0.2f * k, age * 0.3f, AuraRender.alpha(0xFFFFC060, 0.9f * (left / 12f)), FB);
            }
        }
    }

    // ------------------------------------------------------------------ the claw rake
    private static void rake(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float f = Mth.clamp((e.life() + partial) / Math.max(1, e.maxLife()), 0f, 1f);
        float a = (1f - f * f) * Mth.clamp(e.life() / 2f, 0.2f, 1f);
        float half = e.scale() * (0.6f + 0.35f * Mth.sqrt(f));
        float rot = ((e.seed() & 15) - 8) * 0.07f;
        BeastDraw.camera(pose);
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(claw));
        BeastDraw.glow(pose, vc, 0f, 0f, 0f, half, rot, AuraRender.alpha(0xFFFFB050, a), FB);
        BeastDraw.glow(pose, vc, 0f, 0f, 0f, half * 0.8f, rot + 0.05f, AuraRender.alpha(0xFFFF8A30, a * 0.55f), FB);
    }
}
