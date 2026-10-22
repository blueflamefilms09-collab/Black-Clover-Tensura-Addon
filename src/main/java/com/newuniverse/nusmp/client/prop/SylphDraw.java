package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.sylph.SylphLod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the Sylph (docs/magic/sylph.md), apart from the entity glue ({@link SylphPropPainter}) and the Spirit Lord body ({@code client.ElementalSpiritRenderer}), so the headless
 * preview (tools/render_preview, example SylphProp) draws exactly what the game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame.
 *
 * <p>One Sylph is: the body (cutout: the girl, her dress, her hair; three levels of detail), the four dragonfly wings (long, narrow, tapered, translucent, painted with a vein network, plus an
 * additive iridescent sheen sheet of several phases that is shown by shifting V and sweeps slowly over the membrane: {@link #sheen}), the glow-only model (the irises of the shown face and the hem of the skirt, additive), and a halo (one camera-facing square). The face that is shown, the pose, the laugh and the sway
 * all come from the clip: the models are posed by bone rotation only.</p>
 */
public final class SylphDraw {
    private SylphDraw() {}

    private static GeoSpec lod(GeoSpec base, String modelId, boolean glow) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + ".png"),
                glow ? ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + "_glow.png") : null);
    }

    private static final GeoSpec SMALL = GeoSpec.of("sylph", "small"), POWERED = GeoSpec.of("sylph", "powered");
    private static final GeoSpec[] SMALL_LOD = {SMALL, lod(SMALL, "sylph_small_lod1", false), lod(SMALL, "sylph_small_lod2", false)};
    private static final GeoSpec[] POWERED_LOD = {POWERED, lod(POWERED, "sylph_powered_lod1", false), lod(POWERED, "sylph_powered_lod2", false)};
    private static final GeoSpec SMALL_WINGS = lod(SMALL, "sylph_small_wings", true), POWERED_WINGS = lod(POWERED, "sylph_powered_wings", true);
    private static final GeoSpec SMALL_FX = lod(SMALL, "sylph_small_fx", false), POWERED_FX = lod(POWERED, "sylph_powered_fx", false);
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/sylph_halo.png");
    static final int FB = PropDraw.FULL_BRIGHT;
    /** The models are built in 1/16 block units; the small fairy is drawn at this scale (about 0.5 of a block seated, 0.6 standing), the powered form at its own. */
    public static final float SMALL_SCALE = 0.24f, POWERED_SCALE = 0.36f;
    /** The height (model units) of the middle of each body: the anchor of a Sylph is the middle of her body. */
    static final float ANCHOR_SMALL = 20f, ANCHOR_POWERED = 30f;
    /** The colour of the halo (a soft green). */
    static final int HALO_RGB = 0x6CFF6C;

    /**
     * The sheen of the wings: the glow sheet holds {@link SylphLod#SHEEN_PHASES} phases of a pastel rainbow gradient; the phase follows the clock ({@link SylphLod#SHEEN_TICKS_PER_PHASE} ticks each),
     * and on the full level the phase and the next one are drawn with weights that cross-fade, so the sheen drifts smoothly; on the half level the nearest phase only; none on the lowest level.
     * Additive, through {@link SafeAdditive} (alpha-weighted: the sheet is black under every transparent texel).
     */
    private static void sheen(PoseStack pose, MultiBufferSource b, GeoSpec wings, String clip, float secs, float age, float fade, int lod) {
        if (lod >= SylphLod.LOW) return;
        int n = SylphLod.SHEEN_PHASES;
        float x = (age / SylphLod.SHEEN_TICKS_PER_PHASE) % n;
        int k0 = (int) x;
        float w = x - k0;
        RenderType rt = SafeAdditive.additive(wings.glow());
        if (lod == SylphLod.FULL) {
            passSheen(pose, b, wings, rt, clip, secs, SealDraw.argb(fade * 0.95f * (1f - w), 0xFFFFFF), k0, n);
            passSheen(pose, b, wings, rt, clip, secs, SealDraw.argb(fade * 0.95f * w, 0xFFFFFF), (k0 + 1) % n, n);
        } else {
            passSheen(pose, b, wings, rt, clip, secs, SealDraw.argb(fade * 0.95f, 0xFFFFFF), w < 0.5f ? k0 : (k0 + 1) % n, n);
        }
    }

    private static void passSheen(PoseStack pose, MultiBufferSource b, GeoSpec spec, RenderType rt, String clip, float secs, int argb, int k, int n) {
        if ((argb >>> 24) < 3) return;
        GeoModelData m = GeoModels.model(spec.model());
        GeoAnim a = clip == null ? null : GeoModels.animations(spec.animations());
        GeoAnim.Clip c = a == null ? null : a.clip(clip);
        GeoDraw.draw(pose, SheenUv.over(b.getBuffer(rt), k, n), m, c, secs, GeoDraw.Space.PROP, FB, OverlayTexture.NO_OVERLAY, argb);
    }

    /**
     * One Sylph in the pose's space (her anchor at the origin, facing +z; the anchor is the middle of her body). {@code lod} 0..2 picks the body, {@code fade} is 0..1 (translucent while she
     * forms and fades), {@code clip} and {@code secs} pick the animation, {@code age} the clock of the glows.
     */
    public static void sylph(PoseStack pose, MultiBufferSource buffers, boolean powered, int lod, String clip, float secs, float age, float fade, int light) {
        if (lod >= SylphLod.CULL || fade <= 0.01f) return;
        MultiBufferSource b = WaterPropDraw.lazy(buffers);
        float s = (powered ? POWERED_SCALE : SMALL_SCALE) * (0.85f + 0.15f * fade);
        pose.pushPose();
        pose.translate(0f, -(powered ? ANCHOR_POWERED : ANCHOR_SMALL) * s / 16f, 0f);                    // the model's feet are at its origin: the anchor is its middle
        pose.scale(s, s, s);
        int white = ((int) (fade * 255f + 0.5f) << 24) | 0xFFFFFF;
        GeoSpec wings = powered ? POWERED_WINGS : SMALL_WINGS;
        pass(pose, b, wings, AuraRender.translucent(wings.texture()), clip, secs, light, white, true);                              // the membrane and its veins
        sheen(pose, b, wings, clip, secs, age, fade, lod);                                                                          // the iridescent sheen sweeping over it
        GeoSpec body = (powered ? POWERED_LOD : SMALL_LOD)[lod];
        pass(pose, b, body, fade < 0.99f ? AuraRender.translucent(body.texture()) : AuraRender.cutout(body.texture()), clip, secs, light, white, true);
        if (lod <= SylphLod.HALF) GeoDraw.paint(pose, b, powered ? POWERED_FX : SMALL_FX, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FB,
                SealDraw.argb(fade * (0.80f + 0.20f * Mth.sin(age * 0.17f)), 0xFFFFFF), 0);
        pose.popPose();
        if (lod <= SylphLod.HALF) halo(pose, b, powered, age, fade);
    }

    /** One pass of a model of the Sylph with a render type; {@code flat} draws it with an upward normal on every face (see {@link FlatLit}: no diffuse shading, the painted colours show). */
    private static void pass(PoseStack pose, MultiBufferSource b, GeoSpec spec, RenderType rt, String clip, float secs, int light, int argb, boolean flat) {
        GeoModelData m = GeoModels.model(spec.model());
        GeoAnim a = clip == null ? null : GeoModels.animations(spec.animations());
        GeoAnim.Clip c = a == null ? null : a.clip(clip);
        VertexConsumer vc = b.getBuffer(rt);
        GeoDraw.draw(pose, flat ? FlatLit.over(vc) : vc, m, c, secs, GeoDraw.Space.PROP, light, OverlayTexture.NO_OVERLAY, argb);
    }

    /** The soft green halo behind her: one camera-facing additive square at her middle, breathing slowly. */
    private static void halo(PoseStack pose, MultiBufferSource b, boolean powered, float age, float fade) {
        float pulse = 0.88f + 0.12f * Mth.sin(age * 0.09f);
        float half = (powered ? 1.5f : 0.5f) * pulse;
        pose.pushPose();
        SealDraw.camera(pose);
        VertexConsumer vc = b.getBuffer(AuraRender.additive(HALO));
        SealDraw.glow(pose, vc, 0f, 0f, 0f, half, age * 0.01f, SealDraw.argb((powered ? 0.55f : 0.42f) * fade, HALO_RGB), FB);
        SealDraw.glow(pose, vc, 0f, 0f, 0f, half * 0.55f, -age * 0.012f, SealDraw.argb((powered ? 0.42f : 0.30f) * fade, 0xDFFFD0), FB);
        pose.popPose();
    }
}
