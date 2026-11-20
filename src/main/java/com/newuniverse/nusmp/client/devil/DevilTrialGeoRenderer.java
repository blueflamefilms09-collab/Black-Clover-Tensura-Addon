package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.devil.DevilCanonRules;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.devil.phase2.DevilTrialEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Why: the real model of the trial devil (docs/devil_trial_model.md). With the canon looks each roster devil draws its own canon model ({@link DevilCanonAssets}, option {@code bossLooks.devilCanonLooks}, default on); the
 * description below is the generic brute, which stays the look of an unknown or pack devil and of every devil with the option off: the horned brute of {@code geo/entity/devil_trial.geo.json} (33 bones, 110 cubes, source {@code tools/blockbench/devil_trial.bbmodel}) with
 * one painted sheet per devil (the devil's own palette, {@code textures/entity/devil_trial_<id>.png}), a glow mask for the eyes, brand lines, gems and horn tips ({@code ..._glowmask.png}, drawn by GeckoLib's
 * auto-glow layer) and the clips idle / walk / attack. The devil's rank sets a small scale (the hitbox does not change). Chosen by {@link DevilTrialRenderer} while the client option
 * {@code bossLooks.devilTrialModel} is on (default); off = the earlier vanilla-humanoid stand-in.
 *
 * <p>Cost (rule 11): client draw only. At most {@code trial.max} (default 4) of these exist; 660 quads each plus one glow pass; the sheets are 256x256 and loaded when a devil is first drawn; no allocation per frame
 * (the texture locations are cached in a map bounded by the roster, the scale is a switch).
 */
public final class DevilTrialGeoRenderer extends GeoEntityRenderer<DevilTrialEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "geo/entity/devil_trial.geo.json");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "animations/entity/devil_trial.animation.json");
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/devil_trial.png");
    /** Sheet of each roster devil, filled on first use; the key is always a roster id, so it never holds more than the roster. */
    private static final java.util.Map<String, ResourceLocation> TEXTURES = new java.util.concurrent.ConcurrentHashMap<>();

    public DevilTrialGeoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(DevilTrialEntity e) { return modelOf(e.devilId()); }
            @Override public ResourceLocation getTextureResource(DevilTrialEntity e) { return textureOf(e.devilId()); }
            @Override public ResourceLocation getAnimationResource(DevilTrialEntity e) { return animationOf(e.devilId()); }
        });
        this.shadowRadius = 0.7f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** The geometry of a devil: its canon model (docs/devil_trial_model.md, "Canon looks") while {@code bossLooks.devilCanonLooks} is on, else the generic brute. Shared with the summoning figure. */
    static ResourceLocation modelOf(String id) { return DevilCanonAssets.model(id, MODEL); }

    /** The clips file of a devil (the canon file has the same clip names as the brute's). */
    static ResourceLocation animationOf(String id) { return DevilCanonAssets.animation(id, ANIMATION); }

    /** The painted sheet of a devil: its canon sheet, else the brute's sheet in its palette, or the default sheet for an empty or unknown id. */
    static ResourceLocation textureOf(String id) {
        ResourceLocation canon = DevilCanonAssets.texture(id, null);
        if (canon != null) return canon;
        WorldDevil d = WorldDevil.byId(id);
        if (d == null) return DEFAULT_TEXTURE;
        return TEXTURES.computeIfAbsent(d.id(), k -> ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/devil_trial_" + k + ".png"));
    }

    /** Scale by rank: a Supreme devil looms, a Low one is smaller. The hitbox stays the same for every rank. */
    static float scaleOf(String id) { return DevilCanonRules.rankScale(id); }

    @Override
    public void render(DevilTrialEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float s = scaleOf(entity.devilId());
        poseStack.pushPose();
        poseStack.scale(s, s, s);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
