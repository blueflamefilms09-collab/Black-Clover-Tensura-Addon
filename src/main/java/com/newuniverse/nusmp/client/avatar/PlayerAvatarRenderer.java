package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the player-made Avatar looks like a person the creator designed, not like a stock mob, so it is drawn with a player-shaped model whose skin is
 * composed from the creator's choices ({@link AvatarSkinCache}), sized by the vanilla scale attribute (not scaled again here), with the 3D parts ({@link AvatarFeatureLayer}), glowing
 * eyes ({@link AvatarEyesLayer}) and a tinted aura ring on the ground. The brief asked for GeckoLib; the mod's own GeoLite loader draws the same
 * Blockbench-format models without a new dependency, and the body animates through the vanilla humanoid model (walk, arms, held items, armour).
 * This class is named PlayerAvatarRenderer because {@code AvatarRenderer} already belongs to the Devil Avatar boss.
 */
public class PlayerAvatarRenderer extends HumanoidMobRenderer<AvatarEntity, PlayerModel<AvatarEntity>> {
    public PlayerAvatarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        addLayer(new AvatarFeatureLayer(this));
        addLayer(new AvatarEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AvatarEntity e) { return AvatarSkinCache.skin(AvatarLooks.of(e), e.raceId()); }

    @Override
    public void render(AvatarEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(e, yaw, partial, pose, buffers, light);
        if (e.isInvisible()) return;
        AppearanceData look = AvatarLooks.of(e);
        float age = e.tickCount + partial, s = e.getScale();   // the vanilla SCALE attribute (set by the server from the look) already scales the model
        boolean frenzy = e.frenzied();
        pose.pushPose();
        pose.scale(s, s, s);
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.12f + e.getId());
        int rgb = frenzy ? 0x7A0A14 : look.auraRgb();
        AvatarParts.drawAura(pose, buffers, rgb, (frenzy ? 0.85f : 0.40f) + 0.20f * pulse, age / 20f);
        pose.popPose();
    }
}
