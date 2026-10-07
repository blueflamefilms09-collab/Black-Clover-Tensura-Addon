package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.entity.GrimoireDaemonEntity;
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
 * 0.52: a Grimoire Daemon (docs/zagred_boss_gdd.md 5.1) in cuboids. One model for all tiers: a floating tattered robe and hood,
 * thin arms holding a grimoire out in front that snaps open and flips its pages while it casts; horns from the greater tier,
 * two pairs of membrane wings for the arch tier, or just a glyph slab for the Overwrite rule stones. The texture layout is the
 * part table in tools/gen_daemon_textures.py (128x128).
 */
public class GrimoireDaemonModel extends EntityModel<GrimoireDaemonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_daemon"), "main");

    private final ModelPart root, robeLow, robeMid, robeTop, head, hoodTip, armR, armL, hornR, hornL, book, coverL, coverR, pages,
            wingR, wingL, wing2R, wing2L, slab;

    public GrimoireDaemonModel(ModelPart modelRoot) {
        root = modelRoot.getChild("root");
        robeLow = root.getChild("robe_low");
        robeMid = root.getChild("robe_mid");
        robeTop = root.getChild("robe_top");
        head = root.getChild("head");
        hoodTip = head.getChild("hood_tip");
        hornR = head.getChild("horn_r");
        hornL = head.getChild("horn_l");
        armR = root.getChild("arm_r");
        armL = root.getChild("arm_l");
        book = root.getChild("book");
        coverL = book.getChild("cover_l");
        coverR = book.getChild("cover_r");
        pages = book.getChild("pages");
        wingR = root.getChild("wing_r");
        wingL = root.getChild("wing_l");
        wing2R = root.getChild("wing2_r");
        wing2L = root.getChild("wing2_l");
        slab = root.getChild("slab");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f));
        r.addOrReplaceChild("robe_low", CubeListBuilder.create().texOffs(0, 32).addBox(-6f, -8f, -4f, 12f, 8f, 8f), PartPose.offset(0f, 24f, 0f));
        r.addOrReplaceChild("robe_mid", CubeListBuilder.create().texOffs(0, 16).addBox(-5f, -8f, -3.5f, 10f, 8f, 7f), PartPose.offset(0f, 16f, 0f));
        r.addOrReplaceChild("robe_top", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -8f, -3f, 8f, 8f, 6f), PartPose.offset(0f, 8f, 0f));
        PartDefinition h = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 50).addBox(-3.5f, -7f, -3.5f, 7f, 7f, 7f), PartPose.offset(0f, 0f, 0f));
        h.addOrReplaceChild("hood_tip", CubeListBuilder.create().texOffs(40, 0).addBox(-1f, -4f, -1f, 2f, 4f, 2f), PartPose.offsetAndRotation(0f, -7f, 1f, -0.5f, 0f, 0f));
        h.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(40, 8).addBox(-1f, -5f, -1f, 2f, 5f, 2f), PartPose.offsetAndRotation(-3f, -6f, -1f, -0.2f, 0f, -0.5f));
        h.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(40, 8).mirror().addBox(-1f, -5f, -1f, 2f, 5f, 2f), PartPose.offsetAndRotation(3f, -6f, -1f, -0.2f, 0f, 0.5f));
        r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5f, 0f, -1.5f, 3f, 9f, 3f), PartPose.offsetAndRotation(-5.5f, 1f, 0f, -0.9f, 0f, 0f));
        r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-1.5f, 0f, -1.5f, 3f, 9f, 3f), PartPose.offsetAndRotation(5.5f, 1f, 0f, -0.9f, 0f, 0f));
        PartDefinition b = r.addOrReplaceChild("book", CubeListBuilder.create(), PartPose.offset(0f, 6f, -8f));
        b.addOrReplaceChild("cover_l", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(0f, -6f, -0.5f, 6f, 12f, 1f), PartPose.offset(0f, 0f, 0f));
        b.addOrReplaceChild("cover_r", CubeListBuilder.create().texOffs(64, 0).addBox(-6f, -6f, -0.5f, 6f, 12f, 1f), PartPose.offset(0f, 0f, 0f));
        b.addOrReplaceChild("pages", CubeListBuilder.create().texOffs(64, 16).addBox(-5f, -5f, 0.5f, 10f, 10f, 1f), PartPose.offset(0f, 0f, 0f));
        r.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(64, 32).addBox(-14f, -11f, -0.5f, 14f, 22f, 1f), PartPose.offsetAndRotation(-3f, 4f, 3f, 0f, 0.6f, 0f));
        r.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(64, 32).mirror().addBox(0f, -11f, -0.5f, 14f, 22f, 1f), PartPose.offsetAndRotation(3f, 4f, 3f, 0f, -0.6f, 0f));
        r.addOrReplaceChild("wing2_r", CubeListBuilder.create().texOffs(64, 32).addBox(-14f, -11f, -0.5f, 14f, 22f, 1f), PartPose.offsetAndRotation(-3f, 12f, 3f, 0f, 0.9f, 0f));
        r.addOrReplaceChild("wing2_l", CubeListBuilder.create().texOffs(64, 32).mirror().addBox(0f, -11f, -0.5f, 14f, 22f, 1f), PartPose.offsetAndRotation(3f, 12f, 3f, 0f, -0.9f, 0f));
        r.addOrReplaceChild("slab", CubeListBuilder.create().texOffs(0, 70).addBox(-4f, -8f, -1.5f, 8f, 16f, 3f), PartPose.offset(0f, 12f, 0f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(GrimoireDaemonEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        int tier = e.tier();
        boolean stone = tier == GrimoireDaemonEntity.STONE;
        boolean horns = tier == GrimoireDaemonEntity.GREATER || tier == GrimoireDaemonEntity.ARCH, wings = tier == GrimoireDaemonEntity.ARCH;
        for (ModelPart p : new ModelPart[]{robeLow, robeMid, robeTop, head, armR, armL, book}) p.visible = !stone;
        slab.visible = stone;
        hornR.visible = hornL.visible = horns && !stone;
        wingR.visible = wingL.visible = wing2R.visible = wing2L.visible = wings;
        float bob = Mth.sin(age * 0.1f + e.getId()) * 1.5f;
        root.y += bob;
        if (stone) { slab.yRot = age * 0.05f; slab.y += Mth.sin(age * 0.07f) * 1.5f; return; }
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        robeLow.zRot = Mth.sin(age * 0.12f) * 0.05f;
        robeMid.zRot = Mth.sin(age * 0.12f + 0.6f) * 0.03f;
        hoodTip.xRot += Mth.sin(age * 0.2f) * 0.1f;
        boolean casting = e.castLeft() > 0;
        float open = casting ? 1.25f + Mth.sin(age * 1.3f) * 0.3f : 0.55f + Mth.sin(age * 0.1f) * 0.1f;     // the book snaps open and flaps
        coverL.yRot = -open;
        coverR.yRot = open;
        pages.yRot = casting ? Mth.sin(age * 1.7f) * 0.5f : Mth.sin(age * 0.2f) * 0.05f;                    // pages flipping frantically
        book.y += Mth.sin(age * 0.13f) * 0.8f;
        float raise = casting ? -1.5f : -0.9f;
        armR.xRot = raise;
        armL.xRot = raise;
        if (wings) {
            float flap = Mth.sin(age * 0.15f) * 0.3f;
            wingR.yRot += flap;
            wingL.yRot -= flap;
            wing2R.yRot += flap * 0.8f;
            wing2L.yRot -= flap * 0.8f;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        root.render(pose, vc, light, overlay, color);
    }
}
