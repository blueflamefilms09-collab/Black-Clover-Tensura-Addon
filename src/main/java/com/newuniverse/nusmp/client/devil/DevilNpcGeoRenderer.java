package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.devil.DevilCanonRules;
import com.newuniverse.nusmp.devil.DevilNpc;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * Why: the summoning figure as the real, full-size devil (docs/devil_trial_model.md, "Canon looks"). It replaces the earlier figure, a player-model body that was baby-sized (the game's player model starts with
 * the child proportions unless told otherwise) with the adult wings and horns drawn on top of it, so a small child stood under a pair of wings. This figure is the same GeckoLib model, sheet, glow and rank scale
 * as the trial devil ({@link DevilTrialGeoRenderer}: the devil's canon model, or the generic brute for an unknown devil or with {@code bossLooks.devilCanonLooks} off), so its wings and horns are bones of the
 * body and can not float. The state of the figure picks the pose: it climbs out of the circle (sunk, fading in, the {@code rise} clip), stands (idle), roars while it makes its offer (the {@code roar} clip),
 * and merges into the signer (shrinks, fades, slides into their chest) or dissolves (fades). Chosen by {@link DevilNpcRenderer} while the client option {@code bossLooks.devilRitualFigure} is on (default).
 *
 * <p>Cost per figure per frame (rule 11): one model draw (at most 1 100 quads for the biggest canon model) and one glow pass while it is not faded out; nothing beyond 64 blocks; at most {@code npc.max} (default 8)
 * figures exist. Colours come from a table made once, the glow layer is skipped while the figure is transparent, nothing is allocated per frame but the game's own rotation helper (one per figure). Not seen in a
 * running game by its author.
 */
public final class DevilNpcGeoRenderer extends GeoEntityRenderer<DevilNpc> {
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.devil.npc.geo");
    private static final double CULL = 64.0;
    private static final int STEPS = 32;
    /** White at 0..STEPS of full opacity, made once: the tint that fades the model without making an object per frame. */
    private static final Color[] FADES = new Color[STEPS + 1];

    static {
        for (int i = 0; i <= STEPS; i++) FADES[i] = new Color(((i * 255 / STEPS) << 24) | 0xFFFFFF);
    }

    public DevilNpcGeoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(DevilNpc e) { return DevilTrialGeoRenderer.modelOf(e.devilId()); }
            @Override public ResourceLocation getTextureResource(DevilNpc e) { return DevilTrialGeoRenderer.textureOf(e.devilId()); }
            @Override public ResourceLocation getAnimationResource(DevilNpc e) { return DevilTrialGeoRenderer.animationOf(e.devilId()); }
        });
        this.shadowRadius = 0f;
        addRenderLayer(new FadingGlow(this));
    }

    /** The figure may be taller than its box: only the distance culls it. */
    @Override
    public boolean shouldRender(DevilNpc e, Frustum frustum, double x, double y, double z) { return e.distanceToSqr(x, y, z) < CULL * CULL; }

    private static float fadeOf(DevilNpc e, float partial) { return DevilCanonRules.fade(e.state(), e.stateAge() + partial); }

    @Override
    public Color getRenderColor(DevilNpc e, float partial, int light) { return FADES[DevilCanonRules.fadeLevel(fadeOf(e, partial), STEPS)]; }

    /** Translucent only while it fades (the cut-out type draws the full-opacity figure the cheap way). */
    @Override
    public RenderType getRenderType(DevilNpc e, ResourceLocation texture, MultiBufferSource buffers, float partial) {
        return fadeOf(e, partial) < 0.99f ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
    }

    /** The game's yaw of the figure (set toward the caller when it was made); the library's own turn ignores it for a plain entity. A Bedrock model's front is -z, hence 180 - yaw. */
    @Override
    protected void applyRotations(DevilNpc e, PoseStack ps, float ageInTicks, float rotationYaw, float partial, float nativeScale) {
        ps.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partial, e.yRotO, e.getYRot())));
    }

    @Override
    public void render(DevilNpc e, float entityYaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        int state = e.state();
        float t = e.stateAge() + partial;
        if (DevilCanonRules.fade(state, t) <= 0.01f) return;
        long t0 = PerfProbe.begin();
        try {
            float s = DevilCanonRules.rankScale(e.devilId()) * DevilCanonRules.shrink(state, t);
            float ox = 0f, oy = 0f, oz = 0f;
            float k = DevilCanonRules.slide(state, t);
            if (k > 0f && e.owner() != null) {                                                  // merging: slides into the signer's chest
                Player owner = e.level().getPlayerByUUID(e.owner());
                if (owner != null) {
                    ox = (float) (Mth.lerp(partial, owner.xo, owner.getX()) - Mth.lerp(partial, e.xo, e.getX())) * k;
                    oy = (float) (Mth.lerp(partial, owner.yo, owner.getY()) + 0.9 - Mth.lerp(partial, e.yo, e.getY())) * k;
                    oz = (float) (Mth.lerp(partial, owner.zo, owner.getZ()) - Mth.lerp(partial, e.zo, e.getZ())) * k;
                }
            }
            ps.pushPose();
            ps.translate(ox, oy - DevilCanonRules.sink(state, t), oz);
            ps.scale(s, s, s);
            super.render(e, entityYaw, partial, ps, buffers, light);
            ps.popPose();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** The glow mask of the sheet (eyes, brands, gems), drawn at the figure's own opacity so a fading figure does not leave bright eyes behind. */
    private static final class FadingGlow extends AutoGlowingGeoLayer<DevilNpc> {
        FadingGlow(DevilNpcGeoRenderer r) { super(r); }

        @Override
        public void render(PoseStack ps, DevilNpc e, BakedGeoModel model, RenderType rt, MultiBufferSource buffers, VertexConsumer buffer, float partial, int light, int overlay) {
            float a = fadeOf(e, partial);
            if (a >= 0.99f) {
                super.render(ps, e, model, rt, buffers, buffer, partial, light, overlay);
                return;
            }
            if (a <= 0.02f) return;
            RenderType glow = getRenderType(e, buffers);
            if (glow != null)
                getRenderer().reRender(model, ps, buffers, e, glow, buffers.getBuffer(glow), partial, 15728640, OverlayTexture.NO_OVERLAY, FADES[DevilCanonRules.fadeLevel(a, STEPS)].argbInt());
        }
    }
}
