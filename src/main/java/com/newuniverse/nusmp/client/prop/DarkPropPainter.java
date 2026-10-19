package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Dark Magic: the painters of the summons. DARK_1 = the Black Blade (dark_black_blade, variants 0 idle / 2 chopping) and the Crescent (dark_crescent, variant 1); DARK_2 = the Black
 * Moon (dark_black_moon, variant 0) and the Black Hole (dark_black_hole, variant 1). Solid obsidian (cutout) with the violet glow maps additive, plus camera-facing sprites: black
 * smoke (alpha blended, dark, so it hides rather than glows), violet cut streaks and tendrils, the moon's corona and the hole's accretion disc.
 * <pre>
 *   pass                        render type           why
 *   model                       cutout (translucent while it forms / fades)   solid black plates with alpha holes (the shadow flames)
 *   glow map                    additive, full bright the violet edge and inlay
 *   smoke                       translucent, lit       black smoke must darken what is behind it
 *   streaks, corona, disc       additive, full bright camera-facing sprites
 * </pre>
 */
public final class DarkPropPainter {
    private DarkPropPainter() {}

    private static final GeoSpec BLADE = GeoSpec.of("dark", "black_blade"), CRESCENT = GeoSpec.of("dark", "crescent"), MOON = GeoSpec.of("dark", "black_moon"), HOLE = GeoSpec.of("dark", "black_hole");
    private static final ResourceLocation WISP = AuraRender.tex("particle/darkrb_wisp"), SLASH = AuraRender.tex("particle/darkrb_slash"), TENDRIL = AuraRender.tex("particle/darkrb_tendril"),
            MOON_TEX = AuraRender.tex("particle/darkrb_moon"), HOLE_TEX = AuraRender.tex("particle/darkrb_hole"), RUNE = AuraRender.tex("particle/darkrb_rune");
    private static final int FB = PropDraw.FULL_BRIGHT, GLOW = 0xB070FF, PALE = 0xE0B8FF;

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.DARK_1, DarkPropPainter::blade);
        PropPainters.register(PropKind.DARK_2, DarkPropPainter::orb);
    }

    private static void blade(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        int v = (e.param() & 15);
        pose.pushPose();
        WaterPropDraw.pitch(e, pose, partial, 1.0f * s);
        pose.scale(s, s, s);
        if (v == 1) {                                                                                  // the crescent
            WaterPropDraw.model(pose, buffers, CRESCENT, "idle", age / 20f, false, f, light, GLOW, 0.9f + 0.1f * Mth.sin(age * 0.5f));
            SealDraw.camera(pose);
            VertexConsumer slash = buffers.getBuffer(AuraRender.additive(SLASH));
            for (int k = 0; k < 3; k++)                                                               // violet streaks trailing from the two points and the middle
                SealDraw.ribbon(pose, slash, (k - 1) * 1.55f, 1.0f, -0.4f * Math.abs(k - 1) + 0.0f, (k - 1) * 2.3f, 1.0f, -0.4f * Math.abs(k - 1) - 3.2f, 0.17f, 0f, 1f, SealDraw.argb(0.65f * f, k == 1 ? PALE : GLOW), FB);
        } else {                                                                                       // the black blade
            WaterPropDraw.model(pose, buffers, BLADE, v == 2 ? "slash" : "idle", age / 20f, false, f, light, GLOW, 0.9f + 0.1f * Mth.sin(age * 0.5f));
            SealDraw.camera(pose);
            VertexConsumer wisp = buffers.getBuffer(AuraRender.translucent(WISP));
            VertexConsumer tendril = buffers.getBuffer(AuraRender.additive(TENDRIL));
            int lit = SealDraw.lift(light, 4);
            for (int i = 0; i < 8; i++) {                                                             // black smoke boiling off the blade, violet tendrils licking along it
                float ph = WaterPropDraw.frac(age * 0.03f + i * 0.127f), z = 2.2f - i * 0.38f;
                SealDraw.glow(pose, wisp, 0.12f * Mth.sin(i * 2.3f), 1.0f + 0.25f + 0.5f * ph, z, 0.2f + 0.14f * ph, i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.75f * f, 0x0A0612), lit);
                if (i % 2 == 0) SealDraw.ribbon(pose, tendril, 0f, 1.1f, z, 0.1f * Mth.sin(age * 0.3f + i), 1.1f + 0.5f * ph, z - 0.5f, 0.05f, 0f, 1f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.8f * f, GLOW), FB);
            }
        }
        pose.popPose();
    }

    private static void orb(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        boolean hole = (e.param() & 15) == 1;
        float cy = hole ? 1.25f : 1.5f;
        pose.pushPose();
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, hole ? HOLE : MOON, "idle", age / 20f, false, f, light, GLOW, 0.8f + 0.2f * Mth.sin(age * 0.2f));
        pose.popPose();
        pose.pushPose();
        pose.scale(s, s, s);
        SealDraw.camera(pose);
        VertexConsumer add = buffers.getBuffer(AuraRender.additive(hole ? HOLE_TEX : MOON_TEX));
        float pulse = 0.7f + 0.3f * Mth.sin(age * 0.17f);
        SealDraw.glow(pose, add, 0f, cy, 0f, hole ? 1.9f : 1.7f, age * (hole ? 0.04f : 0.008f), SealDraw.argb((hole ? 0.55f : 0.3f) * f * pulse, GLOW), FB);
        VertexConsumer wisp = buffers.getBuffer(AuraRender.translucent(WISP));
        int lit = SealDraw.lift(light, 4);
        for (int i = 0; i < 10; i++) {                                                                // shadow falling off the moon / being pulled into the hole
            float ph = WaterPropDraw.frac(age * 0.02f + i * 0.1f), a = i * 2.399f + age * (hole ? 0.08f : 0.02f);
            float r = hole ? 2.2f * (1f - ph) + 0.3f : 1.1f + 0.5f * ph, y = hole ? cy + 0.3f * Mth.sin(a * 2f) : cy - 1.0f * ph;
            SealDraw.glow(pose, wisp, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.22f * (hole ? 1f - 0.5f * ph : 1f), a, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.7f * f, 0x0A0612), lit);
        }
        pose.popPose();
    }
}
