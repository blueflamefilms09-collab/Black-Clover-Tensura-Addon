package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.gnome.GnomeLod;
import com.newuniverse.nusmp.gnome.GnomeRules;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the Gnome (the Earth Spirit, docs/magic/gnome.md), apart from the entity glue ({@link GnomePropPainter}), so the headless preview (tools/render_preview, example GnomeProp) draws exactly
 * what the game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame (the specs are built once, in static fields).
 *
 * <p>Per Gnome, per frame: the colour model of its level of detail (the quads of {@link GnomeLod#BODY}) and, on the two nearer levels, the glow-only model (the carved magic circle, the rune cuffs, the
 * crystal veins; {@link GnomeLod#FX}) drawn additive and full bright with its own half size glow sheet, and the FACE: one small plate on the head per expression (calm, amused, angry, sleepy, surprised,
 * stern; colour sheet cutout plus glow sheet additive, one quad each, {@link GnomeLod#FACE_QUADS}). The dust of the rise and of the slam is made of custom particles sent by the painter.</p>
 */
public final class GnomeDraw {
    private GnomeDraw() {}

    /**
     * A level of detail (or the glow-only model): its own model, the sheet {@code textureId} (a lower level reads a smaller copy), the animation file of its form, no glow pass. Both forms are painted on
     * ONE sheet, so the companion's models read the giant's textures (the same pixels): the game loads each sheet once, not twice.
     */
    private static GeoSpec lod(GeoSpec base, String modelId, String textureId) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + textureId + ".png"), null);
    }

    private static final GeoSpec COMP = GeoSpec.of("gnome", "companion"), FULL = GeoSpec.of("gnome", "full");
    private static final GeoSpec[][] BODY = {{lod(COMP, "gnome_companion", "gnome_full"), lod(COMP, "gnome_companion_lod1", "gnome_full_lod1"), lod(COMP, "gnome_companion_lod2", "gnome_full_lod2")},
            {FULL, lod(FULL, "gnome_full_lod1", "gnome_full_lod1"), lod(FULL, "gnome_full_lod2", "gnome_full_lod2")}};
    private static final GeoSpec[][] FX = {{lod(COMP, "gnome_companion_fx", "gnome_full_fx"), lod(COMP, "gnome_companion_fx_lod1", "gnome_full_fx_lod1")},
            {lod(FULL, "gnome_full_fx", "gnome_full_fx"), lod(FULL, "gnome_full_fx_lod1", "gnome_full_fx_lod1")}};
    /** The names of the expressions, in the order of {@code GnomeRules.FACE_*}; the plates are geo/gnome/gnome_<form>_face_<name>.geo.json. */
    public static final String[] FACES = {"calm", "amused", "angry", "sleepy", "surprised", "stern"};
    private static final GeoSpec[][] PLATE = new GeoSpec[2][FACES.length], PLATE_FX = new GeoSpec[2][FACES.length];

    static {
        GeoSpec[] bases = {COMP, FULL};
        String[] forms = {"companion", "full"};
        for (int f = 0; f < 2; f++)
            for (int x = 0; x < FACES.length; x++) {
                ResourceLocation model = ResourceLocation.fromNamespaceAndPath("nusmp", "geo/gnome/gnome_" + forms[f] + "_face_" + FACES[x] + ".geo.json");
                PLATE[f][x] = new GeoSpec(model, bases[f].animations(), ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/gnome_faces.png"), null);
                PLATE_FX[f][x] = new GeoSpec(model, bases[f].animations(), ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/gnome_faces_fx.png"), null);
            }
    }

    /** How strong the soft glow behind the eyes is for each expression (a sleepy Gnome's eyes are nearly out, an angry one's blaze). */
    private static final float[] EYE_GLOW = {1.0f, 1.0f, 1.3f, 0.25f, 1.15f, 0.9f};
    private static final ResourceLocation HALO = AuraRender.tex("particle/glow");
    /** The soft amber glow behind the eyes and over the magic circle (three camera-facing quads on the two nearer levels): where, in blocks of the drawn model, and how big (half size). {y, z} per form. */
    private static final float[][] EYES = {{105.8f * 0.165f / 16f, -28.1f * 0.165f / 16f}, {102f / 16f, -22.5f / 16f}}, CHEST = {{72f * 0.165f / 16f, -24f * 0.165f / 16f}, {72f / 16f, -24f / 16f}};
    private static final float[] EYE_HALF = {0.11f, 0.55f}, CHEST_HALF = {0.20f, 1.05f};
    private static final int AMBER = 0xFFB43C;
    /** The clip names, in the order of {@code GnomeRules.CLIP_*}. */
    public static final String[] CLIPS = {"idle", "walk", "rise", "cast", "sink", "punch", "stomp", "slam", "shrug", "guard", "pound", "doze", "kneel"};

    /**
     * One Gnome in the pose's space (its feet at the origin, facing +z): the body at the level of detail and the glow-only model on the two nearer levels. {@code form} is
     * {@link GnomeLod#COMPANION} or {@link GnomeLod#FULL}; {@code fade} is 0..1 (translucent while it forms and fades); {@code clip} and {@code secs} pick the animation; {@code face} is the
     * expression ({@code GnomeRules.FACE_*}).
     */
    public static void gnome(PoseStack pose, MultiBufferSource buffers, int form, int lod, String clip, float secs, float age, float fade, int light, int face) {
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
            int x = Math.max(0, Math.min(FACES.length - 1, face));                                    // the face: the plate on the head, then its glow
            GeoDraw.paint(pose, b, PLATE[form][x], clip, secs, GeoDraw.Space.PROP, fade < 0.99f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, light, white, 0);
            GeoDraw.paint(pose, b, PLATE_FX[form][x], clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT, SealDraw.argb(fade * pulse, 0xFFFFFF), 0);
        }
        if (lod <= GnomeLod.LOD_HALF && !CLIPS[GnomeRules.CLIP_RISE].equals(clip) && !CLIPS[GnomeRules.CLIP_SINK].equals(clip)) {      // underground (the rise, the sink) it has nothing to glow over
            float pulse = 0.55f + 0.2f * Mth.sin(age * 0.09f), eye = EYE_GLOW[Math.max(0, Math.min(FACES.length - 1, face))];
            SealDraw.camera(pose);
            VertexConsumer vc = b.getBuffer(AuraRender.additive(HALO));
            SealDraw.glow(pose, vc, 0f, EYES[form][0], EYES[form][1], EYE_HALF[form], 0f, SealDraw.argb(0.55f * fade * pulse * eye, AMBER), PropDraw.FULL_BRIGHT);
            SealDraw.glow(pose, vc, 0f, CHEST[form][0], CHEST[form][1], CHEST_HALF[form], age * 0.01f, SealDraw.argb(0.38f * fade * pulse, AMBER), PropDraw.FULL_BRIGHT);
            SealDraw.glow(pose, vc, 0f, CHEST[form][0], CHEST[form][1], CHEST_HALF[form] * 0.45f, -age * 0.02f, SealDraw.argb(0.45f * fade, 0xFFE9A0), PropDraw.FULL_BRIGHT);
        }
        pose.popPose();
    }
}
