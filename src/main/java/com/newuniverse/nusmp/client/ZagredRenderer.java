package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.47: Zagred, the Kotodama devil. A tall humanoid devil (x1.4) with swept-back horns and a tail, ink-black skin cut with
 * violet rune lines; the eyes and the runes glow (full-bright, pulsing faster in later phases). In phase 4 it holds its
 * otherworldly trident. Texture: tools/gen_kotodama_textures.py.
 */
public class ZagredRenderer extends MobRenderer<ZagredBossEntity, HumanoidModel<ZagredBossEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("nusmp", "zagred"), "main");
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/zagred.png");
    static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/zagred_glow.png");

    private final ItemInHandRenderer items;

    public ZagredRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(LAYER)), 0.8f);
        this.items = ctx.getItemInHandRenderer();
        addLayer(new Glow(this));
        addLayer(new Trident(this));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.ZAGRED.get(), ZagredRenderer::new);
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(LAYER, ZagredRenderer::createLayer);
    }

    /** The humanoid mesh plus horns (two segments each, curving back) and a tail, all children so HumanoidModel draws them. */
    static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "right" : "left";
            PartDefinition base = head.addOrReplaceChild(n + "_horn", CubeListBuilder.create().texOffs(0, 32).addBox(-1f, -5f, -1f, 2, 5, 2),
                    PartPose.offsetAndRotation(side * 3f, -7f, -1f, -0.35f, 0f, side * 0.45f));
            PartDefinition mid = base.addOrReplaceChild(n + "_horn_mid", CubeListBuilder.create().texOffs(8, 32).addBox(-0.75f, -4f, -0.75f, 1.5f, 4, 1.5f),
                    PartPose.offsetAndRotation(0f, -4.6f, 0f, -0.55f, 0f, side * -0.2f));
            mid.addOrReplaceChild(n + "_horn_tip", CubeListBuilder.create().texOffs(14, 32).addBox(-0.5f, -3f, -0.5f, 1, 3, 1),
                    PartPose.offsetAndRotation(0f, -3.7f, 0f, -0.6f, 0f, 0f));
        }
        PartDefinition body = root.getChild("body");
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40).addBox(-1f, 0f, 0f, 2, 2, 8),
                PartPose.offsetAndRotation(0f, 10f, 1.5f, -0.9f, 0f, 0f));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(20, 40).addBox(-0.5f, 0f, 0f, 1, 1, 6).texOffs(34, 40).addBox(-1.5f, -0.5f, 5f, 3, 2, 2),
                PartPose.offsetAndRotation(0f, 0.5f, 7.5f, 0.5f, 0f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ResourceLocation getTextureLocation(ZagredBossEntity e) { return TEX; }

    @Override
    protected void scale(ZagredBossEntity e, PoseStack pose, float partial) { pose.scale(1.4f, 1.4f, 1.4f); }

    /** Eyes and rune lines, full-bright, pulsing (faster each phase). */
    static final class Glow extends RenderLayer<ZagredBossEntity, HumanoidModel<ZagredBossEntity>> {
        Glow(ZagredRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ZagredBossEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            float pulse = 0.65f + 0.35f * Mth.sin(age * (0.08f + 0.04f * e.phase()));
            int v = (int) (255 * pulse);                                    // eyes() blends additively: dim by colour, not alpha
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
    }

    /** Phase 4: the otherworldly trident in its right hand. */
    static final class Trident extends RenderLayer<ZagredBossEntity, HumanoidModel<ZagredBossEntity>> {
        private final ZagredRenderer parent;
        private ItemStack stack;
        Trident(ZagredRenderer parent) { super(parent); this.parent = parent; }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ZagredBossEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            if (e.phase() < 4) return;
            if (stack == null) stack = new ItemStack(NUItems.OTHERWORLD_TRIDENT.get());
            pose.pushPose();
            getParentModel().translateToHand(HumanoidArm.RIGHT, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(1 / 16f, 0.125f, -0.625f);
            parent.items.renderItem(e, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffers, light);
            pose.popPose();
        }
    }
}
