package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

/**
 * 1.99.0 Great Spirits: the canon bodies of Sylph and Undine and the blaze of Salamander, drawn with {@link GeoDraw}. One static method per spirit so the game
 * ({@link ElementalSpiritRenderer}) and the headless render preview (tools/render_preview, scenes great_spirits) draw exactly the same thing. Models and generators:
 * <pre>
 *   Sylph      spirit_sylph_fairy (body, cutout)            + spirit_sylph_fairy_wings (honeycomb wings, translucent, iridescent glow)     tools/gen_sylph_fairy.py
 *   Undine     spirit_undine_mermaid (body, cutout)         + spirit_undine_mermaid_water (water that follows her, translucent)            tools/gen_undine_mermaid.py
 *   Salamander spirit_salamander (0.102, unchanged)         + spirit_salamander_blaze (flames, additive)                                   tools/gen_salamander_blaze.py
 * </pre>
 * The 0.102 models (spirit_sylph, spirit_sylph_wings, spirit_undine, spirit_undine_water) stay on disk; {@link SpiritLooks#CANON} picks which set the renderer draws.
 */
public final class GreatSpiritModels {
    private GreatSpiritModels() {}

    public static final GeoSpec SYLPH_FAIRY = GeoSpec.of("spirit", "sylph_fairy"), SYLPH_FAIRY_WINGS = GeoSpec.of("spirit", "sylph_fairy_wings"),
            UNDINE_MERMAID = GeoSpec.of("spirit", "undine_mermaid"), UNDINE_MERMAID_WATER = GeoSpec.of("spirit", "undine_mermaid_water"),
            SALAMANDER = GeoSpec.of("spirit", "salamander"), SALAMANDER_BLAZE = GeoSpec.of("spirit", "salamander_blaze");

    /**
     * Sylph, a tiny fairy: the body (cutout, a little glow in the eyes and the dress hem) and, over it, her four honeycomb wings (translucent) whose glow map takes the tint of a slowly
     * wandering thin-film hue. {@code anim} / {@code secs} drive both models (they share their clip names); the pose stack is at the spirit's feet.
     */
    public static void sylph(PoseStack pose, MultiBufferSource buffers, String anim, float secs, float age, int light) {
        pose.pushPose();
        float s = SpiritLooks.SYLPH_SCALE;
        pose.scale(s, s, s);
        float pulse = 0.62f + 0.14f * Mth.sin(age * 0.11f);
        GeoDraw.paint(pose, buffers, SYLPH_FAIRY, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SpiritLooks.greyArgb(pulse));
        GeoDraw.paint(pose, buffers, SYLPH_FAIRY_WINGS, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, 0xE8FFFFFF, SpiritLooks.filmArgb(age, 255));
        pose.popPose();
    }

    /** Undine, the giant mermaid: the body (cutout, glow of the scales, fins and shell trim) and the water that follows her (translucent, glowing). */
    public static void undine(PoseStack pose, MultiBufferSource buffers, String anim, float secs, float age, int light) {
        pose.pushPose();
        float s = SpiritLooks.UNDINE_SCALE;
        if (s != 1f) pose.scale(s, s, s);
        float pulse = 0.6f + 0.16f * Mth.sin(age * 0.09f);
        GeoDraw.paint(pose, buffers, UNDINE_MERMAID, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SpiritLooks.greyArgb(pulse));
        GeoDraw.paint(pose, buffers, UNDINE_MERMAID_WATER, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, 0xD8FFFFFF, SpiritLooks.greyArgb(pulse * 0.9f));
        pose.popPose();
    }

    /**
     * Salamander, MASSIVE: the unchanged 0.102 dragon body drawn 1.3 times bigger, and over it the blaze: the flames of its mane, horns, wing struts and tail, an additive layer that
     * shares the body's clips.
     */
    public static void salamander(PoseStack pose, MultiBufferSource buffers, String anim, float secs, float age, int light) {
        pose.pushPose();
        float s = SpiritLooks.SALAMANDER_SCALE;
        pose.scale(s, s, s);
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.12f);
        GeoDraw.paint(pose, buffers, SALAMANDER, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SpiritLooks.greyArgb(pulse));
        float blaze = 0.72f + 0.2f * Mth.sin(age * 0.23f) + 0.08f * Mth.sin(age * 0.71f);
        GeoDraw.paint(pose, buffers, SALAMANDER_BLAZE, anim, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, light, SpiritLooks.greyArgb(blaze * 0.5f), SpiritLooks.greyArgb(blaze * 0.32f));
        pose.popPose();
    }
}
