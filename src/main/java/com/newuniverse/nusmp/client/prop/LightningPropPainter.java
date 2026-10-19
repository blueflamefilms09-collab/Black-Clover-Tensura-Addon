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
 * 1.93.0 Lightning Magic: the painters of the summons. LIGHTNING_1 = the Thunderbird (lightning_thunderbird: 34 cubes, wings of flat bolt feathers that beat, a crest of bolts, a fan of tail
 * feathers), LIGHTNING_2 = the Thunder Lance (lightning_lance: a gold-and-white spear with a coil of electrodes turning round the shaft; variant 1 = black lightning, iron-grey with a red
 * glow). Solid enamel (cutout) with the glow maps additive, and flickering arcs: jagged ribbons that are re-rolled every three ticks, sparks, a flash at the head.
 * <pre>
 *   pass                        render type           why
 *   model                       cutout (translucent while it forms / fades)   solid enamel with alpha holes
 *   glow map                    additive, full bright the bolt inlay
 *   arcs, sparks, flash         additive, full bright camera-facing sprites that re-roll every 3 ticks
 * </pre>
 */
public final class LightningPropPainter {
    private LightningPropPainter() {}

    private static final GeoSpec BIRD = GeoSpec.of("lightning", "thunderbird"), LANCE = GeoSpec.of("lightning", "lance");
    private static final ResourceLocation ARC = AuraRender.tex("particle/lightningfx_arc"), SPARK = AuraRender.tex("particle/lightningfx_spark"), FLASH = AuraRender.tex("particle/lightningfx_flash"),
            RING = AuraRender.tex("particle/lightningfx_ring");
    private static final int FB = PropDraw.FULL_BRIGHT;

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.LIGHTNING_1, LightningPropPainter::bird);
        PropPainters.register(PropKind.LIGHTNING_2, LightningPropPainter::lance);
    }

    private static float roll(MagicPropEntity e, float age, int i, int k) { return SealDraw.rnd(e.seed() + (int) (age / 3f), i, k); }

    private static void bird(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        int glow = 0x6AB4FF, pale = 0xE8F6FF;
        pose.pushPose();
        WaterPropDraw.pitch(e, pose, partial, 1.375f * s);
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, BIRD, "fly", age / 20f, false, f, light, glow, 0.8f + 0.3f * Mth.sin(age * 1.3f));
        SealDraw.camera(pose);
        VertexConsumer arc = buffers.getBuffer(AuraRender.additive(ARC));
        VertexConsumer spark = buffers.getBuffer(AuraRender.additive(SPARK));
        for (int i = 0; i < 5; i++) {                                                                  // arcs jumping from the wing tips, the tail and the crest
            if (roll(e, age, i, 0) < 0.3f) continue;
            float sx = (roll(e, age, i, 1) < 0.5f ? -1f : 1f), x0 = sx * (0.9f + 0.9f * roll(e, age, i, 2)), z0 = -0.2f - 0.5f * roll(e, age, i, 3);
            SealDraw.ribbon(pose, arc, x0, 1.375f + 0.35f * (roll(e, age, i, 4) - 0.5f), z0, x0 + sx * 0.5f * roll(e, age, i, 5), 1.375f + 0.9f * (roll(e, age, i, 6) - 0.5f),
                    z0 - 0.9f * roll(e, age, i, 7), 0.07f, 0f, 1f, SealDraw.argb(0.95f * f, i % 2 == 0 ? pale : glow), FB);
        }
        for (int i = 0; i < 10; i++) {                                                                 // sparks streaming off behind
            float ph = WaterPropDraw.frac(age * 0.07f + i * 0.1f);
            SealDraw.glow(pose, spark, (roll(e, age, i, 9) - 0.5f) * 0.9f, 1.375f + (roll(e, age, i, 10) - 0.5f) * 0.5f, -0.6f - ph * 3.0f, 0.09f * (1f - ph) + 0.03f, age * 0.3f + i,
                    SealDraw.argb((1f - ph) * f, i % 3 == 0 ? pale : glow), FB);
        }
        VertexConsumer flash = buffers.getBuffer(AuraRender.additive(FLASH));
        SealDraw.glow(pose, flash, 0f, 1.5f, 0.9f, 0.3f + 0.1f * roll(e, age, 3, 3), age * 0.2f, SealDraw.argb(0.7f * f, pale), FB);
        pose.popPose();
    }

    private static void lance(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        buffers = WaterPropDraw.lazy(buffers);
        float f = WaterPropDraw.fade(e, partial), s = WaterPropDraw.size(e, f);
        if (f <= 0.01f) return;
        boolean black = (e.param() & 15) == 1;
        int glow = black ? 0xFF3A3A : 0x6AB4FF, pale = black ? 0xFF9090 : 0xE8F6FF;
        pose.pushPose();
        WaterPropDraw.pitch(e, pose, partial, 1.0f * s);
        pose.scale(s, s, s);
        WaterPropDraw.model(pose, buffers, LANCE, "fly", age / 20f, false, f, light, glow, 0.85f + 0.3f * Mth.sin(age * 1.1f));
        SealDraw.camera(pose);
        VertexConsumer arc = buffers.getBuffer(AuraRender.additive(ARC));
        for (int i = 0; i < 4; i++) {                                                                  // arcs crawling along the shaft
            if (roll(e, age, i, 0) < 0.25f) continue;
            float z0 = 1.2f - 0.5f * i - 0.6f * roll(e, age, i, 1), a = roll(e, age, i, 2) * Mth.TWO_PI;
            SealDraw.ribbon(pose, arc, Mth.cos(a) * 0.15f, 1.0f + Mth.sin(a) * 0.15f, z0, Mth.cos(a + 1.5f) * 0.3f, 1.0f + Mth.sin(a + 1.5f) * 0.3f, z0 - 0.9f, 0.06f, 0f, 1f,
                    SealDraw.argb(0.95f * f, i % 2 == 0 ? pale : glow), FB);
        }
        VertexConsumer flash = buffers.getBuffer(AuraRender.additive(FLASH));
        SealDraw.glow(pose, flash, 0f, 1.0f, 2.1f, 0.35f + 0.1f * roll(e, age, 7, 1), age * 0.25f, SealDraw.argb(0.8f * f, pale), FB);        // the point of the head
        VertexConsumer ring = buffers.getBuffer(AuraRender.additive(RING));
        SealDraw.glow(pose, ring, 0f, 1.0f, 1.2f, 0.45f, age * 0.3f, SealDraw.argb(0.55f * f, glow), FB);
        pose.popPose();
    }
}
