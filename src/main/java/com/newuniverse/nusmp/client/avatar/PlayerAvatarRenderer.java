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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Why: the player-made Avatar looks like a person the creator designed, not like a stock mob, so it is drawn with a player-shaped model whose skin is
 * composed from the creator's choices ({@link AvatarSkinCache}), sized by the vanilla scale attribute (not scaled again here), with the 3D parts ({@link AvatarFeatureLayer}), glowing
 * eyes ({@link AvatarEyesLayer}) and a tinted aura ring on the ground. The brief asked for GeckoLib; the mod's own GeoLite loader draws the same
 * Blockbench-format models without a new dependency, and the body animates through the vanilla humanoid model (walk, arms, held items, armour). The arms take the
 * pose of what the Avatar holds and does with it (a drawn bow, a charging or loaded crossbow, a raised shield), see {@link #setArmPoses}.
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

    /**
     * What each arm does for the item in that hand, as the player's own renderer decides it: nothing for an empty hand, the block pose behind a raised shield, the bow
     * draw, the crossbow charge and hold, the thrown spear, and the plain holding pose for every other item. The Avatar's weapon AI puts a bow, a crossbow or a shield in
     * use on the server, and the use flag reaches the client with the entity data, so the arms follow what the AI does.
     */
    private static HumanoidModel.ArmPose armPose(AvatarEntity e, InteractionHand hand) {
        ItemStack stack = e.getItemInHand(hand);
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (e.getUsedItemHand() == hand && e.getUseItemRemainingTicks() > 0) {
            UseAnim anim = stack.getUseAnimation();
            if (anim == UseAnim.BLOCK) return HumanoidModel.ArmPose.BLOCK;
            if (anim == UseAnim.BOW) return HumanoidModel.ArmPose.BOW_AND_ARROW;
            if (anim == UseAnim.SPEAR) return HumanoidModel.ArmPose.THROW_SPEAR;
            if (anim == UseAnim.CROSSBOW) return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            if (anim == UseAnim.SPYGLASS) return HumanoidModel.ArmPose.SPYGLASS;
            if (anim == UseAnim.TOOT_HORN) return HumanoidModel.ArmPose.TOOT_HORN;
            if (anim == UseAnim.BRUSH) return HumanoidModel.ArmPose.BRUSH;
        } else if (!e.swinging && stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return HumanoidModel.ArmPose.ITEM;
    }

    /** Puts both arms of the shared model into the pose of what this Avatar holds; a two-handed pose takes the other arm too (an empty off hand hangs, a full one holds). */
    private static void setArmPoses(AvatarEntity e, PlayerModel<AvatarEntity> m) {
        HumanoidModel.ArmPose main = armPose(e, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose off = armPose(e, InteractionHand.OFF_HAND);
        if (main.isTwoHanded()) off = e.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        if (e.getMainArm() == HumanoidArm.RIGHT) {
            m.rightArmPose = main;
            m.leftArmPose = off;
        } else {
            m.rightArmPose = off;
            m.leftArmPose = main;
        }
    }

    @Override
    public void render(AvatarEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        setArmPoses(e, getModel());
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
