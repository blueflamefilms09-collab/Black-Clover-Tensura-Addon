package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preview stub of PlayerModel (1.21.1): the player's model parts with the game's cube sizes, pivots and 64x64 skin UV layout (head / hat,
 * body / jacket, right and left arm / sleeve, right and left leg / pants, wide or slim arms), the overlay layers inflated like the game's
 * (hat 0.5, the other overlays 0.25), the overlays copying their base part's pose in setupAnim. Its render type is entityTranslucent.
 */
public class PlayerModel<T extends LivingEntity> extends HumanoidModel<T> {
    public final ModelPart leftSleeve;
    public final ModelPart rightSleeve;
    public final ModelPart leftPants;
    public final ModelPart rightPants;
    public final ModelPart jacket;
    private final ModelPart ear;
    private final ModelPart cloak;
    private final boolean slim;

    public PlayerModel(ModelPart root, boolean slim) {
        super(root, RenderType::entityTranslucent);
        this.slim = slim;
        this.ear = root.getChild("ear");
        this.cloak = root.getChild("cloak");
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftPants = root.getChild("left_pants");
        this.rightPants = root.getChild("right_pants");
        this.jacket = root.getChild("jacket");
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return List.of(body, rightArm, leftArm, rightLeg, leftLeg, hat, leftPants, rightPants, leftSleeve, rightSleeve, jacket);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.jacket.copyFrom(this.body);
    }

    public void setAllVisible(boolean visible) {
        this.head.visible = visible;
        this.hat.visible = visible;
        this.body.visible = visible;
        this.rightArm.visible = visible;
        this.leftArm.visible = visible;
        this.rightLeg.visible = visible;
        this.leftLeg.visible = visible;
        this.leftPants.visible = visible;
        this.rightPants.visible = visible;
        this.leftSleeve.visible = visible;
        this.rightSleeve.visible = visible;
        this.jacket.visible = visible;
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        ModelPart part = this.getArm(side);
        if (this.slim) {
            float f = 0.5F * (float) (side == HumanoidArm.RIGHT ? 1 : -1);
            part.x += f;
            part.translateAndRotate(poseStack);
            part.x -= f;
        } else {
            part.translateAndRotate(poseStack);
        }
    }

    // ---------------------------------------------------------------- the mesh (PlayerModel.createMesh + HumanoidModel.createMesh, 64x64)
    private static ModelPart box(int u, int v, float x, float y, float z, float w, float h, float d, float grow, PartPose pose) {
        ModelPart.Cube cube = new ModelPart.Cube(u, v, x, y, z, w, h, d, grow, grow, grow, false, 64.0F, 64.0F, EnumSet.allOf(Direction.class));
        ModelPart part = new ModelPart(List.of(cube), new LinkedHashMap<>());
        part.loadPose(pose);
        part.setInitialPose(pose);
        return part;
    }

    /** Builds the part tree the constructor reads (not a game API: the game bakes it from a LayerDefinition). */
    public static ModelPart createRoot(boolean slim) {
        Map<String, ModelPart> m = new LinkedHashMap<>();
        m.put("head", box(0, 0, -4, -8, -4, 8, 8, 8, 0.0F, PartPose.offset(0, 0, 0)));
        m.put("hat", box(32, 0, -4, -8, -4, 8, 8, 8, 0.5F, PartPose.offset(0, 0, 0)));
        m.put("body", box(16, 16, -4, 0, -2, 8, 12, 4, 0.0F, PartPose.offset(0, 0, 0)));
        m.put("right_arm", box(40, 16, slim ? -2 : -3, -2, -2, slim ? 3 : 4, 12, 4, 0.0F, PartPose.offset(-5, 2, 0)));
        m.put("left_arm", box(32, 48, -1, -2, -2, slim ? 3 : 4, 12, 4, 0.0F, PartPose.offset(5, 2, 0)));
        m.put("right_leg", box(0, 16, -2, 0, -2, 4, 12, 4, 0.0F, PartPose.offset(-1.9F, 12, 0)));
        m.put("left_leg", box(16, 48, -2, 0, -2, 4, 12, 4, 0.0F, PartPose.offset(1.9F, 12, 0)));
        m.put("ear", box(24, 0, -3, -6, -1, 6, 6, 1, 0.0F, PartPose.ZERO));
        m.put("cloak", box(0, 0, -5, 0, -1, 10, 16, 1, 0.0F, PartPose.ZERO));
        m.put("left_sleeve", box(48, 48, -1, -2, -2, slim ? 3 : 4, 12, 4, 0.25F, PartPose.offset(5, 2, 0)));
        m.put("right_sleeve", box(40, 32, slim ? -2 : -3, -2, -2, slim ? 3 : 4, 12, 4, 0.25F, PartPose.offset(-5, 2, 0)));
        m.put("left_pants", box(0, 48, -2, 0, -2, 4, 12, 4, 0.25F, PartPose.offset(1.9F, 12, 0)));
        m.put("right_pants", box(0, 32, -2, 0, -2, 4, 12, 4, 0.25F, PartPose.offset(-1.9F, 12, 0)));
        m.put("jacket", box(16, 32, -4, 0, -2, 8, 12, 4, 0.25F, PartPose.ZERO));
        return new ModelPart(List.of(), m);
    }
}
