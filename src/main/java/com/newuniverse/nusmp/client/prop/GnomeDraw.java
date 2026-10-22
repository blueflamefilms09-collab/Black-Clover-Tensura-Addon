package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.gnome.GnomeLod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the Gnome (the Earth Spirit, docs/magic/gnome.md), apart from the entity glue ({@link GnomePropPainter}), so the headless preview (tools/render_preview, example GnomeProp) draws exactly
 * what the game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame (the specs are built once, in static fields).
 *
 * <p>Per Gnome, per frame: the colour model of its level of detail (the quads of {@link GnomeLod#BODY}) and, on the two nearer levels, the glow-only model (the eyes, the carved magic circle, the rune
 * cuffs, the crystal veins; {@link GnomeLod#FX}) drawn additive and full bright with its own half size glow sheet. The dust of the rise and of the slam is made of custom particles sent by the painter.</p>
 */
public final class GnomeDraw {
    private GnomeDraw() {}

    /** A level of detail (or the glow-only model): its own model and its own copy of the sheet (a lower level reads a smaller one), the animation file of the full model, no glow pass. */
    private static GeoSpec lod(GeoSpec base, String modelId) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + ".png"), null);
    }

    private static final GeoSpec COMP = GeoSpec.of("gnome", "companion"), FULL = GeoSpec.of("gnome", "full");
    private static final GeoSpec[][] BODY = {{COMP, lod(COMP, "gnome_companion_lod1"), lod(COMP, "gnome_companion_lod2")}, {FULL, lod(FULL, "gnome_full_lod1"), lod(FULL, "gnome_full_lod2")}};
    private static final GeoSpec[][] FX = {{lod(COMP, "gnome_companion_fx"), lod(COMP, "gnome_companion_fx_lod1")}, {lod(FULL, "gnome_full_fx"), lod(FULL, "gnome_full_fx_lod1")}};
    /** The clip names, in the order of {@code GnomeRules.CLIP_*}. */
    public static final String[] CLIPS = {"idle", "walk", "rise", "cast", "sink"};

    /**
     * One Gnome in the pose's space (its feet at the origin, facing +z): the body at the level of detail and the glow-only model on the two nearer levels. {@code form} is
     * {@link GnomeLod#COMPANION} or {@link GnomeLod#FULL}; {@code fade} is 0..1 (translucent while it forms and fades); {@code clip} and {@code secs} pick the animation.
     */
    public static void gnome(PoseStack pose, MultiBufferSource buffers, int form, int lod, String clip, float secs, float age, float fade, int light) {
        if (lod >= GnomeLod.CULL || fade <= 0.01f) return;
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        float s = form == GnomeLod.FULL ? GnomeLod.FULL_SCALE : GnomeLod.COMPANION_SCALE;
        pose.pushPose();
        pose.scale(s, s, s);
        int white = ((int) (fade * 255f + 0.5f) << 24) | 0xFFFFFF;
        GeoDraw.paint(pose, b, BODY[form][lod], clip, secs, GeoDraw.Space.PROP, fade < 0.99f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, light, white, 0);
        if (lod <= GnomeLod.LOD_HALF) {                                                               // the eyes, the circle, the runes, the veins: additive, full bright, a slow pulse
            float pulse = 0.82f + 0.18f * Mth.sin(age * 0.09f) + 0.06f * Mth.sin(age * 0.37f);
            GeoDraw.paint(pose, b, FX[form][lod], clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT, SealDraw.argb(fade * pulse, 0xFFFFFF), 0);
        }
        pose.popPose();
    }
}
