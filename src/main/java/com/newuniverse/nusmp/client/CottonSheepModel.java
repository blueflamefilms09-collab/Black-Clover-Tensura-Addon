package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.entity.CottonSheepEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.52: Charmy's cotton sheep as in the anime: a tall, upright, fluffy body (three stacked tufts of wool), thick woolly arms and
 * legs, a pale long face with round pale eyes, framed in wool with a wool crown, droopy ears and curled ram horns. Cooks add a
 * chef's hat, a blue neckerchief and its tie. The part table is the one in tools/gen_cotton_textures.py (128x128).
 */
public class CottonSheepModel extends EntityModel<CottonSheepEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("nusmp", "cotton_sheep"), "main");

    private final ModelPart root, body, head, armR, armL, legR, legL, hat, band, tie;

    public CottonSheepModel(ModelPart modelRoot) {
        root = modelRoot.getChild("root");
        body = root.getChild("body");
        head = body.getChild("head");
        armR = body.getChild("arm_r");
        armL = body.getChild("arm_l");
        legR = root.getChild("leg_r");
        legL = root.getChild("leg_l");
        hat = head.getChild("hat");
        band = head.getChild("band");
        tie = head.getChild("tie");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f));
        r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 0).addBox(-3f, 0f, -3f, 6f, 8f, 6f), PartPose.offset(-3.6f, 16f, 0f));
        r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-3f, 0f, -3f, 6f, 8f, 6f), PartPose.offset(3.6f, 16f, 0f));
        PartDefinition b = r.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-6.5f, 8f, -4.5f, 13f, 8f, 9f)                                  // hips
                .texOffs(0, 36).addBox(-7.5f, 0f, -5f, 15f, 8f, 10f)                                   // belly
                .texOffs(0, 55).addBox(-7f, -7f, -4.5f, 14f, 7f, 9f), PartPose.offset(0f, 0f, 0f));    // chest
        b.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(30, 0).addBox(-3f, 0f, -3f, 6f, 12f, 6f), PartPose.offset(-9.5f, -5f, 0f));
        b.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-3f, 0f, -3f, 6f, 12f, 6f), PartPose.offset(9.5f, -5f, 0f));
        PartDefinition h = b.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-4f, -9f, -4f, 8f, 9f, 8f)                                      // the face and its wool frame
                .texOffs(64, 18).addBox(-4.5f, -12f, -4.5f, 9f, 3f, 9f)                                // the wool crown
                .texOffs(64, 31).addBox(-2f, -3f, -7f, 4f, 4f, 3f), PartPose.offset(0f, -7f, 0f));     // the long muzzle
        h.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(64, 39).addBox(-5f, -1f, -0.5f, 5f, 2f, 1f), PartPose.offsetAndRotation(-4f, -6f, 0f, 0f, 0f, 0.35f));
        h.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(64, 39).mirror().addBox(0f, -1f, -0.5f, 5f, 2f, 1f), PartPose.offsetAndRotation(4f, -6f, 0f, 0f, 0f, -0.35f));
        // the ram's horns: out, down, and curling back in
        h.addOrReplaceChild("horn_r", CubeListBuilder.create()
                .texOffs(64, 43).addBox(-7.5f, -9.5f, -1.5f, 3f, 3f, 3f)
                .texOffs(64, 43).addBox(-9f, -8f, -1.5f, 3f, 3f, 3f)
                .texOffs(64, 43).addBox(-7.5f, -5.5f, -1.5f, 3f, 3f, 3f), PartPose.offset(0f, 0f, 0f));
        h.addOrReplaceChild("horn_l", CubeListBuilder.create()
                .texOffs(64, 43).mirror().addBox(4.5f, -9.5f, -1.5f, 3f, 3f, 3f)
                .texOffs(64, 43).mirror().addBox(6f, -8f, -1.5f, 3f, 3f, 3f)
                .texOffs(64, 43).mirror().addBox(4.5f, -5.5f, -1.5f, 3f, 3f, 3f), PartPose.offset(0f, 0f, 0f));
        // the cook's things
        h.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(64, 50).addBox(-4f, -18f, -4f, 8f, 6f, 8f)
                .texOffs(64, 65).addBox(-5f, -23f, -5f, 10f, 5f, 10f), PartPose.offset(0f, 0f, 0f));
        h.addOrReplaceChild("band", CubeListBuilder.create().texOffs(64, 82).addBox(-5f, 0f, -4.5f, 10f, 2f, 9f), PartPose.offset(0f, 0f, 0f));
        h.addOrReplaceChild("tie", CubeListBuilder.create().texOffs(96, 0).addBox(-1.5f, 1f, -5.2f, 3f, 6f, 1f), PartPose.offset(0f, 0f, 0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(CottonSheepEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean cook = e.role() == CottonSheepEntity.COOK;
        hat.visible = band.visible = tie.visible = cook;
        float bob = Mth.sin(age * 0.12f + e.getId()) * 0.8f;
        body.y += bob;
        legR.xRot = 0f;
        legL.xRot = 0f;
        head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
        head.xRot = pitch * Mth.DEG_TO_RAD * 0.6f;
        switch (e.anim()) {
            case CottonSheepEntity.LEAP -> {                                           // arms thrown up and forward, legs tucked, leaning into the leap
                armR.xRot = armL.xRot = -2.5f;
                armR.zRot = 0.25f;
                armL.zRot = -0.25f;
                legR.xRot = -0.7f;
                legL.xRot = -0.3f;
                body.xRot = 0.25f;
                head.xRot -= 0.2f;
            }
            case CottonSheepEntity.HUG -> {                                            // wide arms squeezing in and out
                float squeeze = Mth.sin(age * 0.4f) * 0.15f;
                armR.xRot = armL.xRot = -1.35f;
                armR.yRot = 0.7f - squeeze;
                armL.yRot = -0.7f + squeeze;
                body.xRot = 0.1f;
            }
            case CottonSheepEntity.COOKING -> {                                        // flipping a pan in one hand and stirring with the other
                armR.xRot = -1.1f + Mth.sin(age * 0.5f) * 0.55f;
                armL.xRot = -1.0f + Mth.sin(age * 0.5f + Mth.PI) * 0.35f;
                armR.zRot = 0.2f;
                armL.zRot = -0.2f;
                body.yRot = Mth.sin(age * 0.1f) * 0.08f;
                head.zRot = Mth.sin(age * 0.2f) * 0.05f;
            }
            default -> {                                                               // standing, arms hanging a little out, swaying
                armR.zRot = 0.12f + Mth.sin(age * 0.09f) * 0.04f;
                armL.zRot = -0.12f - Mth.sin(age * 0.09f) * 0.04f;
                body.zRot = Mth.sin(age * 0.07f) * 0.02f;
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        root.render(pose, vc, light, overlay, color);
    }
}
