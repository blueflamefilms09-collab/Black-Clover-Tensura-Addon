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
 * 1.93.0 Water Magic: the painters of the summons. WATER_1 = the Sea Dragon (water_sea_dragon: 48 cubes, a head with horns and a frill, nine segments swimming in a wave, a fish tail),
 * WATER_2 = the Aqua Javelin / the Tide Lance (water_javelin, spiral rings turning round the shaft). Both are translucent (water), their glow maps additive, and each gets additive
 * sprites round it (spray and drops shed along the dragon, a stream behind the lance).
 * <pre>
 *   pass                        render type           why
 *   model                       translucent (glass)   water is see-through; a prop that forms or fades is translucent anyway
 *   glow map                    additive, full bright the pearl seams and the crystal edges
 *   spray, drops, stream        additive, full bright camera-facing sprites (never the model)
 * </pre>
 * Variant 1 of WATER_2 (the Tide Lance of the Valkyrie armour) is the javelin 1.4 times as big and tinted towards pearl.
 */
public final class WaterPropPainter {
    private WaterPropPainter() {}

    private static final GeoSpec DRAGON = GeoSpec.of("water", "sea_dragon"), JAVELIN = GeoSpec.of("water", "javelin");
    private static final ResourceLocation SPRAY = AuraRender.tex("particle/waterfx_spray"), DROP = AuraRender.tex("particle/waterfx_drop"),
            STREAM = AuraRender.tex("particle/waterfx_stream"), RIPPLE = AuraRender.tex("particle/waterfx_ripple");
    private static final int FB = PropDraw.FULL_BRIGHT, GLOW = 0x5CE6FF, PALE = 0xCFF8FF;

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.WATER_1, WaterPropPainter::dragon);
        PropPainters.register(PropKind.WATER_2, WaterPropPainter::javelin);
    }

    private static void dragon(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        int lit = SealDraw.lift(light, 8);
        pose.pushPose();
        WaterPropDraw.pitch(e, pose, partial, 1.125f * s);
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, DRAGON, "swim", age / 20f, true, f * 0.92f, lit, GLOW, 0.8f + 0.2f * Mth.sin(age * 0.3f));
        SealDraw.camera(pose);
        VertexConsumer spray = buffers.getBuffer(AuraRender.additive(SPRAY));
        VertexConsumer drop = buffers.getBuffer(AuraRender.additive(DROP));
        SealDraw.glow(pose, spray, 0f, 1.125f, 2.9f, 0.55f, age * 0.05f, SealDraw.argb(0.5f * f, PALE), FB);                    // the spray in front of the muzzle
        for (int i = 0; i < 16; i++) {                                                                                       // drops shed along the body, thrown up and falling back
            float ph = WaterPropDraw.frac(age * 0.05f * (1f + 0.25f * (i % 3)) + i * 0.173f), z = 1.5f - 5.2f * (i / 16f), side = Mth.sin(i * 2.7f);
            float y = 1.125f + 0.55f * Mth.sin(ph * Mth.PI) * (0.6f + 0.4f * Mth.cos(i)) + 0.2f * (1f - ph), x = side * (0.45f + 0.25f * ph);
            SealDraw.glow(pose, drop, x, y, z - 0.4f * ph, 0.07f + 0.04f * Mth.sin(i * 1.3f), 0f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.9f * f, PALE), FB);
        }
        pose.popPose();
    }

    private static void javelin(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), big = (e.param() & 15) == 1 ? 1.4f : 1f, s = WaterPropDraw.size(e, f) * big;
        if (f <= 0.01f) return;
        int lit = SealDraw.lift(light, 8);
        pose.pushPose();
        WaterPropDraw.pitch(e, pose, partial, 1.0f * s);
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, JAVELIN, "fly", age / 20f, true, f * 0.95f, lit, (e.param() & 15) == 1 ? PALE : GLOW, 1.0f);
        SealDraw.camera(pose);
        VertexConsumer stream = buffers.getBuffer(AuraRender.additive(STREAM));
        for (int k = 0; k < 3; k++) {                                                                                        // the stream the lance leaves, three ribbons winding
            float a0 = age * 0.4f + k * 2.1f;
            float x0 = Mth.cos(a0) * 0.12f, y0 = 1.0f + Mth.sin(a0) * 0.12f;
            SealDraw.ribbon(pose, stream, x0, y0, -1.4f, x0 * 2f, y0 + (Mth.sin(a0 + 1f)) * 0.1f, -4.6f, 0.13f, 0f, 1f, SealDraw.argb(0.7f * f, k == 1 ? PALE : GLOW), FB);
        }
        VertexConsumer ripple = buffers.getBuffer(AuraRender.additive(RIPPLE));
        SealDraw.glow(pose, ripple, 0f, 1.0f, 1.2f, 0.4f, age * 0.1f, SealDraw.argb(0.5f * f, PALE), FB);                      // the pressure ring at the head
        pose.popPose();
    }
}
