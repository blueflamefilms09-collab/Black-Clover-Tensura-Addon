package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules;
import com.newuniverse.nusmp.spiritdark.SpiritDarkLod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the Spirit of Darkness, apart from the entity glue ({@link SpiritDarkPropPainter}), so the headless preview (tools/render_preview, example SpiritDarkProp) draws exactly what
 * the game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame (the specs are built once, the passes are a few model draws).
 *
 * <p>Two forms: the smoky shadow-wisp companion (about 0.9 block tall at {@link #COMPANION_SCALE}) and the powered form, a miniature aristocratic demon (about 2.3 blocks at scale 1). Each is drawn
 * from its own level of detail (a body model on its own copy of the sheet), then the FACE (a tiny model of the head's plates with the sheet of the current mood: seven moods, no extra quads), then,
 * on the two nearer levels, the glow-only model and the face's glow sheet additive and full bright (the eyes, the grin, the runes, the wing seams, the smoke wisps and the ring on the ground). The
 * cost of every level is {@link SpiritDarkLod#cost}.</p>
 */
public final class SpiritDarkDraw {
    private SpiritDarkDraw() {}

    /** A level of detail (or the glow-only model): its own model and its own copy of the sheet (a lower level reads a smaller one), the animation file of the full model, no glow pass. */
    private static GeoSpec lod(GeoSpec base, String modelId) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + ".png"), null);
    }

    /** The face model of a form with the sheet of a mood: the model is the same for all moods, only the texture differs. */
    private static GeoSpec face(GeoSpec base, String form, String model, String sheet, String mood) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/spirit_dark_" + form + "_" + model + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/spirit_dark_" + form + "_" + sheet + "_" + mood + ".png"), null);
    }

    private static final GeoSpec COMPANION = GeoSpec.of("spirit_dark", "companion"), POWERED = GeoSpec.of("spirit_dark", "powered");
    private static final GeoSpec[] COMPANION_LOD = {COMPANION.noGlow(), lod(COMPANION, "spirit_dark_companion_lod1"), lod(COMPANION, "spirit_dark_companion_lod2")};
    private static final GeoSpec[] POWERED_LOD = {POWERED.noGlow(), lod(POWERED, "spirit_dark_powered_lod1"), lod(POWERED, "spirit_dark_powered_lod2")};
    private static final GeoSpec COMPANION_FX = lod(COMPANION, "spirit_dark_companion_fx"), POWERED_FX = lod(POWERED, "spirit_dark_powered_fx");
    private static final GeoSpec[] COMPANION_FACE = new GeoSpec[DarkMoodRules.Face.values().length], COMPANION_FACE_GLOW = new GeoSpec[COMPANION_FACE.length];
    private static final GeoSpec[] POWERED_FACE = new GeoSpec[COMPANION_FACE.length], POWERED_FACE_GLOW = new GeoSpec[COMPANION_FACE.length];

    static {
        for (DarkMoodRules.Face f : DarkMoodRules.Face.values()) {
            COMPANION_FACE[f.ordinal()] = face(COMPANION, "companion", "face", "face", f.id);
            COMPANION_FACE_GLOW[f.ordinal()] = face(COMPANION, "companion", "faceglow", "faceglow", f.id);
            POWERED_FACE[f.ordinal()] = face(POWERED, "powered", "face", "face", f.id);
            POWERED_FACE_GLOW[f.ordinal()] = face(POWERED, "powered", "faceglow", "faceglow", f.id);
        }
    }

    /** The models are built in 1/16 block units: the companion is drawn at 0.6 (about 0.9 block with its horns and tail), the powered form at 1. */
    public static final float COMPANION_SCALE = 0.6f, POWERED_SCALE = 1.0f;
    /** The height of the companion's head blob centre in model units: the anchor point is that centre, so the model is lowered by it. */
    public static final float COMPANION_CENTRE = 17f;
    static final int FULL_BRIGHT = 0xF000F0;

    /**
     * One spirit in the pose's space (its anchor at the origin, facing +z): the body at the level of detail, the face of the mood, then the glow-only model and the face glow on the two nearer
     * levels. {@code fade} is 0..1 (translucent while it forms and fades); {@code clip} and {@code secs} pick the animation; {@code face} is a {@link DarkMoodRules.Face} ordinal.
     */
    public static void spirit(PoseStack pose, MultiBufferSource buffers, boolean powered, int lod, String clip, float secs, float age, float fade, int light, int face) {
        if (lod >= SpiritDarkLod.CULL || fade <= 0.01f) return;
        float s = powered ? POWERED_SCALE : COMPANION_SCALE;
        int f = Mth.clamp(face, 0, COMPANION_FACE.length - 1);
        pose.pushPose();
        if (!powered) pose.translate(0f, -COMPANION_CENTRE / 16f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.Layer layer = fade < 0.99f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT;
        int white = ((int) (Mth.clamp(fade, 0f, 1f) * 255f + 0.5f) << 24) | 0xFFFFFF;
        GeoDraw.paint(pose, buffers, (powered ? POWERED_LOD : COMPANION_LOD)[lod], clip, secs, GeoDraw.Space.PROP, layer, light, white, 0);
        GeoDraw.paint(pose, buffers, (powered ? POWERED_FACE : COMPANION_FACE)[f], clip, secs, GeoDraw.Space.PROP, layer, light, white, 0);
        if (lod <= SpiritDarkLod.HALF) {
            float pulse = 0.82f + 0.18f * Mth.sin(age * 0.35f);
            int glow = SealDraw.argb(Mth.clamp(fade, 0f, 1f) * pulse, 0xFFFFFF);
            GeoDraw.paint(pose, buffers, powered ? POWERED_FX : COMPANION_FX, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FULL_BRIGHT, glow, 0);
            GeoDraw.paint(pose, buffers, (powered ? POWERED_FACE_GLOW : COMPANION_FACE_GLOW)[f], clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FULL_BRIGHT, SealDraw.argb(Mth.clamp(fade, 0f, 1f), 0xFFFFFF), 0);
        }
        pose.popPose();
    }
}
