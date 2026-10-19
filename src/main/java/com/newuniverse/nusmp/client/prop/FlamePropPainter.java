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
 * 1.93.0 Flame Magic: the painters of the summons. FLAME_1 = the Flame Lion of Leo Rugiens (flame_lion: 43 cubes, a mane of twelve flame wedges, a galloping cycle, plays the idle
 * breathing once it has stopped), FLAME_2 = the Phoenix (flame_phoenix: wings of flame feathers that beat, a crest, a fan of long tail feathers). Solid (cutout) with the glow maps additive,
 * plus camera-facing flame ribbons and embers: flames licking up the lion's back and trailing from its tail, fire left on the ground under its paws, a trail of flame behind the phoenix.
 * <pre>
 *   pass                        render type           why
 *   model                       cutout (translucent while it forms / fades)   solid fur with alpha holes (the mane and the flame tuft)
 *   glow map                    additive, full bright the fur's embers, the mane, the feathers
 *   flames, embers, ground fire additive, full bright camera-facing sprites and flat rings on the ground (never the model)
 * </pre>
 */
public final class FlamePropPainter {
    private FlamePropPainter() {}

    private static final GeoSpec LION = GeoSpec.of("flame", "lion"), PHOENIX = GeoSpec.of("flame", "phoenix");
    private static final ResourceLocation FLAME = AuraRender.tex("particle/flamefx_flame"), EMBER = AuraRender.tex("particle/flamefx_ember"), BURST = AuraRender.tex("particle/flamefx_burst"),
            RING = AuraRender.tex("particle/flamefx_ring"), SMOKE = AuraRender.tex("particle/flamefx_smoke");
    private static final int FB = PropDraw.FULL_BRIGHT, BODY = 0xFF5A14, CORE = 0xFFA838, GLOW = 0xFF8A2A, HOT = 0xFFD070;

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.FLAME_1, FlamePropPainter::lion);
        PropPainters.register(PropKind.FLAME_2, FlamePropPainter::phoenix);
    }

    private static void flame(PoseStack pose, VertexConsumer vc, float bx, float by, float bz, float tx, float ty, float tz, float hw, float k, float t) {
        float sway = Mth.sin(t * 0.37f) * 0.05f;
        SealDraw.ribbon(pose, vc, bx, by, bz, tx + sway, ty, tz + sway, hw, 0f, 1f, SealDraw.argb(0.7f * k, BODY), FB);
        SealDraw.ribbon(pose, vc, bx, by, bz, tx + sway * 0.5f, by + (ty - by) * 0.72f, tz + sway * 0.5f, hw * 0.5f, 0f, 0.8f, SealDraw.argb(0.8f * k, CORE), FB);
    }

    private static void lion(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        int stop = (e.param() >> 8) & 1023;
        boolean running = stop == 0 || e.life() + partial < stop;
        pose.pushPose();
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, LION, running ? "run" : "idle", age / 20f, false, f, light, GLOW, 0.85f + 0.2f * Mth.sin(age * 0.6f));
        SealDraw.camera(pose);
        VertexConsumer flame = buffers.getBuffer(AuraRender.additive(FLAME));
        VertexConsumer ember = buffers.getBuffer(AuraRender.additive(EMBER));
        for (int i = 0; i < 6; i++) {                                                                  // flames licking up the lion's back and off the haunches
            float z = 0.5f - i * 0.28f, fl = 0.5f + 0.5f * Mth.sin(age * 0.45f + i * 1.9f);
            flame(pose, flame, 0.12f * Mth.sin(i * 2.3f), 1.5f, z, 0.04f * Mth.sin(i), 1.5f + 0.34f + 0.16f * fl, z - (running ? 0.3f : 0.05f), 0.11f, f, age + i * 3f);
        }
        for (int i = 0; i < 8; i++) {                                                                  // embers streaming off behind
            float ph = WaterPropDraw.frac(age * 0.05f + i * 0.125f);
            SealDraw.glow(pose, ember, (Mth.sin(i * 2.1f)) * 0.35f, 1.0f + 0.9f * ph, -1.0f - ph * 2.2f * (running ? 1f : 0.3f), 0.07f * (1f - ph) + 0.03f, age * 0.2f + i,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * f, i % 2 == 0 ? HOT : GLOW), FB);
        }
        VertexConsumer ring = buffers.getBuffer(AuraRender.additive(RING));
        pose.pushPose();                                                                              // fire on the ground under the paws
        pose.translate(0f, 0.03f, 0f);
        SealDraw.flat(pose, ring, 0.85f * (0.9f + 0.1f * Mth.sin(age * 0.4f)), age * 1.2f, SealDraw.argb(0.55f * f, GLOW), FB);
        pose.popPose();
        pose.popPose();
    }

    private static void phoenix(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        pose.pushPose();
        WaterPropDraw.pitch(e, pose, partial, 1.375f * s);
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, PHOENIX, "fly", age / 20f, false, f, light, GLOW, 0.9f + 0.2f * Mth.sin(age * 0.5f));
        SealDraw.camera(pose);
        VertexConsumer flame = buffers.getBuffer(AuraRender.additive(FLAME));
        VertexConsumer ember = buffers.getBuffer(AuraRender.additive(EMBER));
        VertexConsumer smoke = buffers.getBuffer(AuraRender.additive(SMOKE));
        for (int k = 0; k < 4; k++) {                                                                  // the trail: four long flames streaming back from the tail
            float fl = 0.5f + 0.5f * Mth.sin(age * 0.4f + k * 1.7f), x = (k - 1.5f) * 0.18f;
            flame(pose, flame, x, 1.375f, -0.9f, x * 1.6f + 0.05f * Mth.sin(age * 0.3f + k), 1.375f - 0.12f - 0.08f * fl, -3.3f - 0.5f * fl, 0.16f, f, age + k * 4f);
        }
        for (int i = 0; i < 12; i++) {
            float ph = WaterPropDraw.frac(age * 0.045f + i * 0.083f);
            SealDraw.glow(pose, ember, Mth.sin(i * 2.4f) * (0.4f + 1.0f * ph), 1.375f + Mth.cos(i * 1.3f) * 0.4f * ph, -0.8f - ph * 3.4f, 0.08f * (1f - ph) + 0.03f, age * 0.2f + i,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * f, i % 2 == 0 ? HOT : GLOW), FB);
        }
        SealDraw.glow(pose, smoke, 0f, 1.375f, 0f, 1.1f, age * 0.02f, SealDraw.argb(0.22f * f, GLOW), FB);                         // a warm halo round the body
        pose.popPose();
    }
}
