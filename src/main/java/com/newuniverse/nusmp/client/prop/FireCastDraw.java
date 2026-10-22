package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.firecast.FireCastLod;
import com.newuniverse.nusmp.firecast.FireCastRules;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the four fire casting props, apart from the entity glue ({@link FireCastPropPainter}), so the headless preview (tools/render_preview, example FireCastProp) draws exactly what the
 * game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame.
 *
 * <p>The costs, per call (the numbers of the doc): the small Salamander is 240 quads (full), 173 (half), 86 (low) plus 10 / 4 / 0 flame quads; the dragon 263 / 193 / 90 plus 24 / 10 / 0; the ball
 * {@link FireCastLod#BALL_QUADS} quads; the spiral at most {@code 3 * 26 - 2 + 2 = 78} quads ({@link FireCastLod#SPIRAL_MAX_POINTS} sprites, a ribbon between each two, a halo and a tip).</p>
 */
public final class FireCastDraw {
    private FireCastDraw() {}

    /** A level of detail (or the glow-only model): its own model and its own copy of the sheet (a lower level reads a smaller one), the animation file of the full model, no glow pass. */
    private static GeoSpec lod(GeoSpec base, String modelId) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + ".png"), null);
    }

    private static final GeoSpec SAL = GeoSpec.of("firecast", "salamander"), DRAGON = GeoSpec.of("firecast", "dragon");
    private static final GeoSpec[] SAL_LOD = {SAL, lod(SAL, "firecast_salamander_lod1"), lod(SAL, "firecast_salamander_lod2")};
    private static final GeoSpec[] DRAGON_LOD = {DRAGON, lod(DRAGON, "firecast_dragon_lod1"), lod(DRAGON, "firecast_dragon_lod2")};
    private static final GeoSpec SAL_FX = lod(SAL, "firecast_salamander_fx"), DRAGON_FX = lod(DRAGON, "firecast_dragon_fx");
    private static final ResourceLocation FLAME = AuraRender.tex("particle/flamefx_flame"), EMBER = AuraRender.tex("particle/flamefx_ember"), BURST = AuraRender.tex("particle/flamefx_burst");
    static final int FB = PropDraw.FULL_BRIGHT, BODY = 0xFF5A14, CORE = 0xFFA838, GLOW = 0xFF8A2A, HOT = 0xFFD070;
    /** The small form is drawn at this scale, the dragon at its own size (the models are built in 1/16 block units). */
    public static final float SMALL_SCALE = 0.14f, DRAGON_SCALE = 0.5f;

    /**
     * One Salamander in the pose's space (its anchor at the origin, facing +z): the body at the level of detail, the glow-only model (eyes, the furnace of the mouth, and the licks of flame on the wing tips and the tail's barb) on the two nearer levels.
     * {@code fade} is 0..1 (translucent while it forms and fades); {@code clip} and {@code secs} pick the animation.
     */
    public static void salamander(PoseStack pose, MultiBufferSource buffers, boolean dragon, int lod, String clip, float secs, float age, float fade, int light) {
        if (lod >= FireCastLod.CULL || fade <= 0.01f) return;
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        float s = (dragon ? DRAGON_SCALE : SMALL_SCALE) * (0.8f + 0.2f * fade);
        pose.pushPose();
        if (dragon) {
            pose.translate(0f, -1.07f * s, 0.47f * s);                                              // the body's centre on the anchor
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-26f + 4f * Mth.sin(age * 0.07f)));    // nose a little up, hanging in the air
        } else {
            pose.translate(0f, -0.01f, 0.02f);
        }
        pose.scale(s, s, s);
        GeoSpec body = (dragon ? DRAGON_LOD : SAL_LOD)[lod];
        int white = ((int) (fade * 255f + 0.5f) << 24) | 0xFFFFFF;
        GeoDraw.paint(pose, b, body, clip, secs, GeoDraw.Space.PROP, fade < 0.99f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, light, white, 0);
        if (lod <= FireCastLod.HALF) {                                                                // the slit eyes and the furnace of the mouth, additive, full bright
            GeoDraw.paint(pose, b, dragon ? DRAGON_FX : SAL_FX, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FB, SealDraw.argb(fade * (0.85f + 0.15f * Mth.sin(age * 0.5f)), 0xFFFFFF), 0);
        }
        pose.popPose();
    }

    /** Magna's tossed ball, centred on the origin: a halo, a bright core, licks of flame round it. */
    public static void ball(PoseStack pose, MultiBufferSource buffers, float age, float fade) {
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        pose.pushPose();
        pose.translate(0f, 0.04f * Mth.sin(age * 0.3f), 0f);
        SealDraw.camera(pose);
        float pulse = 0.9f + 0.1f * Mth.sin(age * 0.8f);
        VertexConsumer burst = b.getBuffer(AuraRender.additive(BURST));
        VertexConsumer flame = b.getBuffer(AuraRender.additive(FLAME));
        VertexConsumer ember = b.getBuffer(AuraRender.additive(EMBER));
        SealDraw.glow(pose, ember, 0f, 0f, 0f, 0.62f * pulse, age * 0.05f, SealDraw.argb(0.34f * fade, GLOW), FB);
        SealDraw.glow(pose, burst, 0f, 0f, 0f, 0.34f * pulse, age * 0.05f, SealDraw.argb(0.30f * fade, GLOW), FB);
        SealDraw.glow(pose, ember, 0f, 0f, 0f, 0.36f * pulse, age * 0.2f, SealDraw.argb(0.95f * fade, CORE), FB);
        SealDraw.glow(pose, ember, 0f, 0f, 0f, 0.2f, -age * 0.3f, SealDraw.argb(fade, HOT), FB);
        for (int i = 0; i < 5; i++) {                                                               // licks of flame round the sphere
            float a = i * 1.2566f + age * 0.15f, fl = 0.5f + 0.5f * Mth.sin(age * 0.5f + i * 2.1f);
            SealDraw.ribbon(pose, flame, 0.22f * Mth.cos(a), -0.1f, 0.22f * Mth.sin(a), 0.3f * Mth.cos(a), 0.3f + 0.16f * fl, 0.3f * Mth.sin(a), 0.1f, 0f, 1f, SealDraw.argb(0.7f * fade, BODY), FB);
        }
        pose.popPose();
    }

    /** Leopold's spiral, centred on the origin and running along the look (lx, ly, lz): a helix of flame sprites with a ribbon between each two, growing with the charge level (1..4). */
    public static void spiral(PoseStack pose, MultiBufferSource buffers, int level, float age, float fade, int pts, float lx, float ly, float lz) {
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        level = Mth.clamp(level, 1, FireCastRules.MAX_LEVEL);
        pose.pushPose();
        SealDraw.camera(pose);
        float ux = lz, uz = -lx, ul = Mth.sqrt(ux * ux + uz * uz);                                   // a basis across the look: u = look x up (level), v = look x u
        if (ul < 1e-3f) { ux = 1f; uz = 0f; ul = 1f; }
        ux /= ul; uz /= ul;
        float vx = ly * uz, vy = lz * ux - lx * uz, vz = -ly * ux;
        VertexConsumer flame = b.getBuffer(AuraRender.additive(FLAME));
        VertexConsumer ember = b.getBuffer(AuraRender.additive(EMBER));
        float len = 1.3f + 0.28f * level, rad = 0.30f + 0.09f * level;
        float px = 0f, py = 0f, pz = 0f;
        for (int i = 0; i < pts; i++) {
            float t = i / (float) (pts - 1);
            float ang = age * 0.55f + t * (5.2f + 1.4f * level), r = rad * (0.35f + 0.65f * t);
            float cx = lx * (t * len - len * 0.5f) + (ux * Mth.cos(ang) + vx * Mth.sin(ang)) * r;
            float cy = ly * (t * len - len * 0.5f) + (vy * Mth.sin(ang)) * r;
            float cz = lz * (t * len - len * 0.5f) + (uz * Mth.cos(ang) + vz * Mth.sin(ang)) * r;
            float size = (0.09f + 0.05f * level) * (1.0f - 0.55f * t);
            SealDraw.glow(pose, ember, cx, cy, cz, size, ang, SealDraw.argb(fade * (0.5f + 0.5f * t), t > 0.6f ? HOT : CORE), FB);
            if (i > 0) SealDraw.ribbon(pose, flame, px, py, pz, cx, cy, cz, size * 0.8f, 0f, 1f, SealDraw.argb(0.6f * fade, BODY), FB);
            px = cx; py = cy; pz = cz;
        }
        SealDraw.glow(pose, b.getBuffer(AuraRender.additive(BURST)), lx * len * 0.5f, ly * len * 0.5f, lz * len * 0.5f, 0.18f + 0.07f * level, age * 0.1f, SealDraw.argb(0.6f * fade, GLOW), FB);
        SealDraw.glow(pose, b.getBuffer(AuraRender.additive(BURST)), -lx * len * 0.5f, -ly * len * 0.5f, -lz * len * 0.5f, 0.1f + 0.04f * level, -age * 0.1f, SealDraw.argb(0.4f * fade, GLOW), FB);
        pose.popPose();
    }
}
