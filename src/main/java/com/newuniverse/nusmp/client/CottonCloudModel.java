package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 0.52: Charmy's cotton cloud: a flat-bottomed heap of 18 cotton puffs (texture in tools/gen_cotton_textures.py, 64x64). */
public class CottonCloudModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("nusmp", "cotton_cloud"), "main");

    /** x, y, z of each puff (px); the first 7 are big (12x6x12), the rest small (8x5x8). */
    static final float[][] PUFFS = {
            {0, 0, 0}, {11.5f, 0, 0}, {5.75f, 0, 9.96f}, {-5.75f, 0, 9.96f}, {-11.5f, 0, 0}, {-5.75f, 0, -9.96f}, {5.75f, 0, -9.96f},
            {0, -5, 0}, {6, -4, 5}, {-6, -4, 5}, {6, -4, -5}, {-6, -4, -5},
            {15, -1, 4}, {-15, -1, -4}, {4, -1, 15}, {-4, -1, -15}, {13, -2, -8}, {-13, -2, 8}};

    private final ModelPart root;
    private final ModelPart[] puffs = new ModelPart[PUFFS.length];

    public CottonCloudModel(ModelPart modelRoot) {
        root = modelRoot.getChild("root");
        for (int i = 0; i < puffs.length; i++) puffs[i] = root.getChild("p" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f));
        for (int i = 0; i < PUFFS.length; i++) {
            float[] p = PUFFS[i];
            CubeListBuilder cube = i < 7 ? CubeListBuilder.create().texOffs(0, 0).addBox(-6f, -3f, -6f, 12f, 6f, 12f)
                    : CubeListBuilder.create().texOffs(0, 20).addBox(-4f, -2.5f, -4f, 8f, 5f, 8f);
            r.addOrReplaceChild("p" + i, cube, PartPose.offset(p[0], p[1], p[2]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The puffs rise and settle out of step, like boiling cotton. */
    public void animate(float age) {
        root.getAllParts().forEach(ModelPart::resetPose);
        for (int i = 0; i < puffs.length; i++) {
            puffs[i].y += Mth.sin(age * 0.08f + i * 1.7f) * 0.8f;
            puffs[i].x += Mth.sin(age * 0.05f + i) * 0.4f;
        }
    }

    public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) { root.render(pose, vc, light, overlay, color); }
}
