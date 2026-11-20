package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.playermodel.PlayerModelOptions;
import com.newuniverse.nusmp.devil.phase2.DevilTrialEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: draws the devil of the trial fight (docs/devil_contract_phase2.md). Two looks, switched live by the CLIENT option {@code bossLooks.devilTrialModel}:
 * <ul>
 * <li>on (default): the real model, {@link DevilTrialGeoRenderer} (docs/devil_trial_model.md): a horned brute in the devil's own palette with glow, wings, tail and three clips;</li>
 * <li>off: the earlier stand-in this class always was, kept as it was (rule 2): the vanilla humanoid model with the zombie skin, lit by the glowing flag the server sets.</li>
 * </ul>
 * Both renderers are made once; the choice is one boolean read per frame per trial devil (at most {@code trial.max}, default 4, exist). No allocation per frame.
 */
public final class DevilTrialRenderer extends EntityRenderer<DevilTrialEntity> {
    private static final ResourceLocation OLD_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    private final DevilTrialGeoRenderer model;
    private final Old old;

    public DevilTrialRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new DevilTrialGeoRenderer(ctx);
        this.old = new Old(ctx);
    }

    private EntityRenderer<DevilTrialEntity> current() { return PlayerModelOptions.devilTrialModel() ? model : old; }

    @Override
    public void render(DevilTrialEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        current().render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(DevilTrialEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return current().shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public ResourceLocation getTextureLocation(DevilTrialEntity entity) {
        return PlayerModelOptions.devilTrialModel() ? DevilTrialGeoRenderer.textureOf(entity.devilId()) : OLD_TEXTURE;
    }

    /** The stand-in of phase 2, unchanged: the vanilla humanoid model with the zombie skin. */
    private static final class Old extends HumanoidMobRenderer<DevilTrialEntity, HumanoidModel<DevilTrialEntity>> {
        Old(EntityRendererProvider.Context ctx) { super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f); }

        @Override
        public ResourceLocation getTextureLocation(DevilTrialEntity entity) { return OLD_TEXTURE; }
    }
}
