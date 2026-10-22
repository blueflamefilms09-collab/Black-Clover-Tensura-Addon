package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.spiritlight.SpiritLightLod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the Spirit of Light's four props, apart from the entity glue ({@link SpiritLightPropPainter}), so the headless preview (tools/render_preview, example SpiritLightProp) draws exactly
 * what the game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame.
 *
 * <p>The costs, per call (the numbers of the doc, kept equal to the model files by SpiritLightLodTest): a form is its colour model plus, on the two nearer levels, its glow-only model (drawn additive, full
 * bright, its own soft textures); a beam is {@link SpiritLightLod#BEAM_QUADS} quads at most (a core ribbon, a glow ribbon, a thin laser ribbon and two sprites); a pulse ring is
 * {@link SpiritLightLod#PULSE_QUADS} quads (two flat rings and two crossing upright planes). Nothing here is a flat cut-out of an object: the sprites are soft glows and rings.</p>
 */
public final class SpiritLightDraw {
    private SpiritLightDraw() {}

    /** A level of detail (or the glow-only model): its own model and its own copy of the sheet (a lower level reads a smaller one), the animation file of the full model, no glow pass. */
    private static GeoSpec lod(GeoSpec base, String modelId) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + ".png"), null);
    }

    private static final GeoSpec CUB = GeoSpec.of("spiritlight", "cub"), SERAPH = GeoSpec.of("spiritlight", "seraph");
    private static final GeoSpec[] CUB_LOD = {CUB.noGlow(), lod(CUB, "spiritlight_cub_lod1"), lod(CUB, "spiritlight_cub_lod2")};
    private static final GeoSpec[] SERAPH_LOD = {SERAPH.noGlow(), lod(SERAPH, "spiritlight_seraph_lod1"), lod(SERAPH, "spiritlight_seraph_lod2")};
    private static final GeoSpec CUB_FX = lod(CUB, "spiritlight_cub_fx"), SERAPH_FX = lod(SERAPH, "spiritlight_seraph_fx");
    private static final ResourceLocation BEAM = AuraRender.tex("particle/spiritlight_beam"), RING = AuraRender.tex("particle/spiritlight_ring"), GLOW = AuraRender.tex("particle/spiritlight_glow"),
            SPARK = AuraRender.tex("particle/spiritlight_spark");
    static final int FB = PropDraw.FULL_BRIGHT, GOLD = 0xFFD86A, WARM = 0xFFE9A8, PALE = 0xFFF8DC, HALO = 0xFFC94A;
    /** The models are built in 1/16 block units at their own size; the cub is drawn at this scale (about 0.7 blocks sitting), the seraph at its own (about 2.2 blocks). */
    public static final float CUB_SCALE = 0.25f, SERAPH_SCALE = 0.46f;

    /**
     * One form in the pose's space (its anchor at the origin, facing +z): the colour model at the level of detail, then the glow-only model (halo, eyes, rings, core) on the two nearer levels.
     * {@code fade} is 0..1 (translucent while it forms and fades); {@code clip} and {@code secs} pick the animation.
     */
    public static void form(PoseStack pose, MultiBufferSource buffers, boolean seraph, int lod, String clip, float secs, float age, float fade, int light) {
        if (lod >= SpiritLightLod.CULL || fade <= 0.01f) return;
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        float s = (seraph ? SERAPH_SCALE : CUB_SCALE) * (0.8f + 0.2f * fade);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoSpec body = (seraph ? SERAPH_LOD : CUB_LOD)[lod];
        int white = ((int) (fade * 255f + 0.5f) << 24) | 0xFFFFFF;
        GeoDraw.paint(pose, b, body, clip, secs, GeoDraw.Space.PROP, fade < 0.99f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, light, white, 0);
        if (lod <= SpiritLightLod.HALF) {                                                             // the halo, the eyes, the rings and the core: additive, full bright
            float pulse = 0.86f + 0.14f * Mth.sin(age * (seraph ? 0.11f : 0.17f));
            GeoDraw.paint(pose, b, seraph ? SERAPH_FX : CUB_FX, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FB, SealDraw.argb(fade * pulse, 0xFFFFFF), 0);
        }
        pose.popPose();
    }

    /**
     * A beam in the pose's space: starts at the origin and runs {@code len} blocks along the unit vector (dx, dy, dz). {@code t} is 0..1 of its life. A bright thin laser inside a soft gold glow, a
     * flare at the muzzle and a spark where it ends; it opens fast and thins away. Five quads.
     */
    public static void beam(PoseStack pose, MultiBufferSource buffers, float len, float dx, float dy, float dz, float t, boolean powered, boolean stopped) {
        float open = SealDraw.smooth(t * 5f), thin = 1f - SealDraw.smooth((t - 0.35f) / 0.65f);
        float k = open * thin;
        if (k <= 0.01f || len < 0.2f) return;
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        pose.pushPose();
        SealDraw.camera(pose);
        float w = powered ? 1.35f : 1f;
        float ex = dx * len, ey = dy * len, ez = dz * len;
        VertexConsumer beam = b.getBuffer(AuraRender.additive(BEAM));
        SealDraw.ribbon(pose, beam, 0f, 0f, 0f, ex, ey, ez, 0.30f * w * (0.5f + 0.5f * k), 0f, 1f, SealDraw.argb(0.55f * k, HALO), FB);
        SealDraw.ribbon(pose, beam, 0f, 0f, 0f, ex, ey, ez, 0.13f * w * k, 0f, 1f, SealDraw.argb(0.95f * k, WARM), FB);
        SealDraw.ribbon(pose, beam, 0f, 0f, 0f, ex, ey, ez, 0.045f * w, 0f, 1f, SealDraw.argb(k, PALE), FB);
        VertexConsumer glow = b.getBuffer(AuraRender.additive(GLOW));
        SealDraw.glow(pose, glow, 0f, 0f, 0f, 0.36f * w * (1.2f - 0.4f * t), t * 2f, SealDraw.argb(0.85f * k, GOLD), FB);
        if (stopped) SealDraw.glow(pose, b.getBuffer(AuraRender.additive(SPARK)), ex, ey, ez, 0.32f * w * (0.6f + t), t * 3f, SealDraw.argb(0.9f * k, WARM), FB);
        pose.popPose();
    }

    /**
     * The Glowing pulse in the pose's space (the owner's feet at the origin): a ring runs out along the ground, a smaller one rises with it at chest height, and two upright rings cross it, so it reads as a
     * shell of light sweeping outward. {@code t} is 0..1 of its life, {@code radius} the full radius in blocks. Eight quads.
     */
    public static void pulse(PoseStack pose, MultiBufferSource buffers, float radius, float t, boolean powered) {
        float out = 1f - (1f - t) * (1f - t);
        float a = (1f - SealDraw.smooth((t - 0.2f) / 0.8f)) * SealDraw.smooth(t * 8f);
        if (a <= 0.01f || radius < 0.5f) return;
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        VertexConsumer ring = b.getBuffer(AuraRender.additive(RING));
        float r = radius * out;
        pose.pushPose();
        pose.translate(0f, 0.05f, 0f);
        SealDraw.flat(pose, ring, r, t * 40f, SealDraw.argb(0.85f * a, GOLD), FB);
        pose.translate(0f, 1.0f, 0f);
        SealDraw.flat(pose, ring, r * 0.62f, -t * 55f, SealDraw.argb(0.6f * a, WARM), FB);
        float h = r * 0.8f;
        int upright = SealDraw.argb(0.45f * a, HALO);
        SealDraw.plane(pose, ring, 0f, 0f, 0f, h, 0f, 0f, 0f, h, 0f, false, upright, FB);          // two upright rings crossing at right angles, each seen from both sides
        SealDraw.plane(pose, ring, 0f, 0f, 0f, h, 0f, 0f, 0f, h, 0f, true, upright, FB);
        SealDraw.plane(pose, ring, 0f, 0f, 0f, 0f, 0f, h, 0f, h, 0f, false, upright, FB);
        SealDraw.plane(pose, ring, 0f, 0f, 0f, 0f, 0f, h, 0f, h, 0f, true, upright, FB);
        pose.popPose();
    }
}
