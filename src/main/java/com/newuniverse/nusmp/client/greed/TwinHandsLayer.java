package com.newuniverse.nusmp.client.greed;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Why: third person, the second hand of the Black Sword Greed's gauntlet and dual-blade forms. Vanilla's hand layer draws the main-hand stack at the main arm; this draws the same stack at the OTHER arm, with the
 * left / right display context of that arm, so the item's renderer picks the left glove (or a second sword) and wears / holds it there. The pose is vanilla's own (ItemInHandLayer.renderArmWithItem). One item draw per
 * player holding a twin form with an empty off hand, none otherwise ({@link GreedHandsClient#drawsTwin}).
 */
final class TwinHandsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    TwinHandsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        if (!GreedHandsClient.drawsTwin(p)) return;
        HumanoidArm arm = p.getMainArm().getOpposite();
        boolean left = arm == HumanoidArm.LEFT;
        ItemDisplayContext ctx = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        pose.pushPose();
        getParentModel().translateToHand(arm, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.translate((left ? -1f : 1f) / 16f, 0.125f, -0.625f);
        Minecraft.getInstance().getItemRenderer().renderStatic(p, p.getMainHandItem(), ctx, left, pose, buffers, p.level(), light, OverlayTexture.NO_OVERLAY, p.getId() + ctx.ordinal());
        pose.popPose();
    }
}
