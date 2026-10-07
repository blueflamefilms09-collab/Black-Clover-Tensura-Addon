package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;

/** Preview stub of ArmedModel. */
public interface ArmedModel {
    void translateToHand(HumanoidArm side, PoseStack poseStack);
}
