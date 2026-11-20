package com.newuniverse.nusmp.client.devil;

import com.newuniverse.nusmp.devil.phase2.DevilTrialEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: draws the devil of the trial fight (docs/devil_contract_phase2.md). PLACEHOLDER: the vanilla humanoid model with the zombie skin, lit by the glowing flag the server sets (and by the boss bar with the devil's
 * name). It is not a made model of the devil: no Blender or Blockbench model of the trial devil exists yet (rule 14 is not met for it, said in the docs); the figure of the ritual ({@link DevilNpcRenderer}) is the
 * real look. Cost: one vanilla humanoid per trial, at most {@code trial.max} (default 4) exist.
 */
public final class DevilTrialRenderer extends HumanoidMobRenderer<DevilTrialEntity, HumanoidModel<DevilTrialEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public DevilTrialRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(DevilTrialEntity entity) { return TEXTURE; }
}
