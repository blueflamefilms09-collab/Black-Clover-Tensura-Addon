package com.newuniverse.nusmp.client.playermodel;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Why: a player's renderer decides how each arm stands for what the hand holds (nothing, an item, a raised shield, a drawn bow, a charging or loaded crossbow, a spyglass, a horn, a brush, a thrown
 * spear); a mob's renderer does not. The kit's characters hold real items (an NPC given a sword, a boss whose AI draws a bow), so every body gets the player's rule: the state of the hand
 * comes from the item and from what the entity is using it for, and a two-handed pose takes the other arm too (an empty off hand hangs, a full one holds). Same decision as the Avatar's renderer
 * made for itself, now shared.
 */
public final class ArmPoses {
    private ArmPoses() {}

    public static HumanoidModel.ArmPose of(LivingEntity e, InteractionHand hand) {
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

    /** Puts both arms of the model into the pose of what this entity holds and does with it. */
    public static void apply(LivingEntity e, HumanoidModel<?> m) {
        HumanoidModel.ArmPose main = of(e, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose off = of(e, InteractionHand.OFF_HAND);
        if (main.isTwoHanded()) off = e.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        if (e.getMainArm() == HumanoidArm.RIGHT) {
            m.rightArmPose = main;
            m.leftArmPose = off;
        } else {
            m.rightArmPose = off;
            m.leftArmPose = main;
        }
    }
}
