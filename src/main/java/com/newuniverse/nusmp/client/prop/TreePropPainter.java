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
 * 1.93.0 Tree Magic: how the props of PropKind.TREE_1 look (GeoLite models, tools/gen_tree_rebuild_geo.py; the server side is prop.TreeProps). The variant is the low byte of the param, the
 * animation phase (bits 8-9) and the ticks since the action began (bits 10-15) are written by the server:
 * <ul>
 *   <li>variant 0, the Heartwood Guardian (tree_guardian): clip rise for its first 22 ticks, then idle, and slam from the start of a slam to its end (the fists land at 14 ticks, 0.7 s into the
 *       clip); it grows in over 6 ticks and sinks out over the last 10. Passes: 1. a growth-ring sigil on the ground under it, additive (RenderType.eyes), full bright; 2. the model, cutout
 *       (entityCutoutNoCull) with its glow map additive on top (GeoDraw's own second pass).</li>
 *   <li>variant 1, the Root Lance (tree_lance): clip thrust over its first 12 ticks then idle; the model rises out of the ground (scale in y from 0.3 to 1 over 5 ticks) and sinks out over the last 8.
 *       Passes: the model, cutout + glow.</li>
 * </ul>
 * {@link #model} is the shared drawing call of the six nature painters. Every texture exists (entity/tree_*.png and their _glow maps, particle/tree_fx_ring.png).
 */
public final class TreePropPainter {
    private TreePropPainter() {}

    private static final GeoSpec GUARDIAN = GeoSpec.of("tree", "guardian"), LANCE = GeoSpec.of("tree", "lance");
    private static ResourceLocation ring;

    /** Called once on the client by PropPainterRegistry (the five sibling painters are listed next to it in PropPainterRegistry). */
    public static void register() {
        ring = AuraRender.tex("particle/tree_fx_ring");
        PropPainters.register(PropKind.TREE_1, TreePropPainter::paint);
    }

    // ================================================================ shared
    /** The fade of a prop over its life: grows in over `in` ticks and out over the last `out`. */
    public static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return Mth.clamp(life / in, 0f, 1f) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    public static int phase(MagicPropEntity e) { return (e.param() >> 8) & 3; }

    /** The ticks since the current action began, with the partial tick (0 when idle). */
    public static float since(MagicPropEntity e, float partial) { return phase(e) == 0 ? 0f : ((e.param() >> 10) & 63) + partial; }

    /**
     * One model of a prop: scaled (sx, sy, sz) about the prop's feet, lowered by yOff blocks, drawn on the layer with its glow map in glowRgb scaled by glowK (0 = no glow pass).
     */
    public static void model(PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String clip, float seconds, GeoDraw.Layer layer, int light, float sx, float sy, float sz, float yOff,
                             int argb, int glowArgb) {
        pose.pushPose();
        pose.translate(0f, yOff, 0f);
        pose.scale(sx, sy, sz);
        GeoDraw.paint(pose, buffers, spec, clip, seconds, GeoDraw.Space.PROP, layer, light, argb, glowArgb);
        pose.popPose();
    }

    public static int argb(float a, int rgb) { return TreeDraw.argb(a, rgb); }

    /** A sigil lying on the ground under the prop (additive, full bright). */
    public static void sigil(PoseStack pose, MultiBufferSource buffers, ResourceLocation tex, float radius, float spin, int rgb, float alpha) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(tex));
        pose.pushPose();
        pose.translate(0f, 0.03f, 0f);
        TreeDraw.flat(pose, vc, radius, spin, TreeDraw.argb(alpha, rgb), TreeDraw.FB);
        pose.popPose();
    }

    // ================================================================ Tree
    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 6f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale();
        int variant = e.param() & 0xFF;
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        if (variant == 0) {
            sigil(pose, buf, ring, 2.4f * s * k, age * 1.2f, 0xFFC866, 0.55f * k);
            float life = e.life() + partial;
            String clip;
            float clock;
            if (phase(e) == 1) { clip = "slam"; clock = since(e, partial) / 20f; }
            else if (life < 22f) { clip = "rise"; clock = life / 20f; }
            else { clip = "idle"; clock = age / 20f; }
            float sc = 0.72f * s * (0.6f + 0.4f * k);
            model(pose, buf, GUARDIAN, clip, clock, GeoDraw.Layer.CUTOUT, light, sc, sc, sc, 0f, 0xFFFFFFFF, argb(k * (0.65f + 0.35f * pulse), 0xFFFFFF));
        } else {
            float life = e.life() + partial;
            float rise = Mth.clamp(life / 5f, 0f, 1f);
            String clip = life < 12f ? "thrust" : "idle";
            float sc = 0.9f * s * k;
            model(pose, buf, LANCE, clip, life < 12f ? life / 20f : age / 20f, GeoDraw.Layer.CUTOUT, light, sc, sc * (0.3f + 0.7f * rise), sc, 0f, 0xFFFFFFFF, argb(k * pulse, 0xFFFFFF));
        }
    }
}
