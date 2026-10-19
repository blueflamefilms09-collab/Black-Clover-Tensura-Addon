package com.newuniverse.nusmp.client.playermodel;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the vanilla {@link PlayerModel} animates itself (walk, swing, bow, crossbow, spyglass, shield, sneak, riding, swimming) for a {@code LivingEntity} of any kind, which is exactly what the
 * kit wants; what it lacks is a way to lay the repo's own poses (a cast, a talk, a slump, a power-up, a held weapon) over that animation. This subclass runs the vanilla animation first, then applies
 * the frame's {@link PoseState} to every part (final = vanilla + (target - vanilla) * blend + add) and re-copies the overlay layers (hat, jacket, sleeves, pants) from their base parts, which
 * vanilla did at the end of its own pass. With an empty state it is exactly the vanilla model.
 */
public class KitPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    /** The pose laid over the vanilla animation; the renderer fills it before every draw. */
    public final PoseState state = new PoseState();
    private final boolean slimArms;

    public KitPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.slimArms = slim;
    }

    public boolean slim() { return slimArms; }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (state.sitting) this.riding = true;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!state.any()) return;
        apply(PoseState.HEAD, head);
        apply(PoseState.BODY, body);
        apply(PoseState.ARM_R, rightArm);
        apply(PoseState.ARM_L, leftArm);
        apply(PoseState.LEG_R, rightLeg);
        apply(PoseState.LEG_L, leftLeg);
        hat.copyFrom(head);
        jacket.copyFrom(body);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
    }

    private void apply(int part, ModelPart p) {
        p.xRot = state.apply(part, 0, p.xRot);
        p.yRot = state.apply(part, 1, p.yRot);
        p.zRot = state.apply(part, 2, p.zRot);
    }

    /** The model part an anchor names. */
    public ModelPart part(PlayerLook.Anchor a) {
        return switch (a) {
            case HEAD -> head;
            case BODY -> body;
            case ARM_R -> rightArm;
            case ARM_L -> leftArm;
            case LEG_R -> rightLeg;
            case LEG_L -> leftLeg;
        };
    }

    /** The model part a hair part names. */
    public ModelPart part(HairStyleData.Part p) { return p == HairStyleData.Part.BODY ? body : head; }
}
