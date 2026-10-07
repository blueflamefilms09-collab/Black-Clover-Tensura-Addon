package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.book.PaintStudio;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.PaintedConstructEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.44: a living illustration drawn as what it is, solid wet paint. A painted hound (its own quadruped model) or a painted
 * humanoid (einherjar; the giant is the same body at twice the size), textured in brushstrokes of its paint's colour
 * (tools/gen_paint_construct_textures.py), with a glowing wet shell that pulses over it. As it appears it rises from flat on the
 * ground to full height, so it reads as the drawing standing up off the canvas.
 */
public class PaintedConstructRenderer extends MobRenderer<PaintedConstructEntity, EntityModel<PaintedConstructEntity>> {
    public static final ModelLayerLocation BEAST_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("nusmp", "painted_beast"), "main");

    private static final ResourceLocation[][] TEX = new ResourceLocation[2][PaintStudio.Paint.values().length];
    static {
        for (PaintStudio.Paint p : PaintStudio.Paint.values()) {
            String n = p.name().toLowerCase(java.util.Locale.ROOT);
            TEX[0][p.ordinal()] = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/painted/beast_" + n + ".png");
            TEX[1][p.ordinal()] = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/painted/humanoid_" + n + ".png");
        }
    }

    private final EntityModel<PaintedConstructEntity> beast, humanoid;
    private final ItemInHandRenderer items;

    public PaintedConstructRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        this.humanoid = this.model;
        this.beast = new BeastModel<>(ctx.bakeLayer(BEAST_LAYER));
        this.items = ctx.getItemInHandRenderer();
        addLayer(new Gloss(this));
        addLayer(new Blade(this));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.PAINTED_CONSTRUCT.get(), PaintedConstructRenderer::new);
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(BEAST_LAYER, BeastModel::createLayer);
    }

    @Override
    public void render(PaintedConstructEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        PaintedConstructEntity.Kind k = e.kind();
        this.model = k == PaintedConstructEntity.Kind.BEAST ? beast : humanoid;
        if (k == PaintedConstructEntity.Kind.BEAST && beast instanceof BeastModel<PaintedConstructEntity> bm) bm.setGenome(e.genome());
        this.shadowRadius = k == PaintedConstructEntity.Kind.GIANT ? 1.0f : 0.5f;
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PaintedConstructEntity e) {
        return TEX[e.kind() == PaintedConstructEntity.Kind.BEAST ? 0 : 1][e.paint().ordinal()];
    }

    /** Rising off the canvas: flat and wide at first, springing up to full height over 16 ticks. The giant is twice the size. */
    @Override
    protected void scale(PaintedConstructEntity e, PoseStack pose, float partial) {
        float t = Mth.clamp((e.tickCount + partial) / 16f, 0, 1);
        float c = 1.70158f, u = t - 1, rise = 1 + (c + 1) * u * u * u + c * u * u;              // ease-out-back
        float k = switch (e.kind()) { case GIANT -> 2f; case KNIGHT -> 0.9375f; default -> PaintedConstructEntity.geneScale(e.genome()); };
        float wide = 1 + 0.3f * (1 - t);
        pose.scale(k * wide, k * Math.max(0.04f, rise), k * wide);
    }

    @Override
    protected boolean shouldShowName(PaintedConstructEntity e) { return false; }

    /** The wet shell: the body again, full-bright and translucent, a little larger, its gloss pulsing. */
    static final class Gloss extends RenderLayer<PaintedConstructEntity, EntityModel<PaintedConstructEntity>> {
        private final PaintedConstructRenderer parent;
        Gloss(PaintedConstructRenderer parent) { super(parent); this.parent = parent; }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, PaintedConstructEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            float pulse = 0.5f + 0.5f * Mth.sin(age * 0.2f);
            int rgb = e.paint().color & 0xFFFFFF;
            int r = Math.min(255, ((rgb >> 16) & 255) + 70), g = Math.min(255, ((rgb >> 8) & 255) + 70), b = Math.min(255, (rgb & 255) + 70);
            int color = ((int) (55 + 55 * pulse) << 24) | (r << 16) | (g << 8) | b;
            pose.pushPose();
            pose.translate(0, 0.75, 0);
            pose.scale(1.05f, 1.03f, 1.05f);
            pose.translate(0, -0.75, 0);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(parent.getTextureLocation(e))),
                    0xF000F0, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
        }
    }

    /** The einherjar's sword (and the giant's), held as a player holds one. */
    static final class Blade extends RenderLayer<PaintedConstructEntity, EntityModel<PaintedConstructEntity>> {
        private static final ItemStack SWORD = new ItemStack(Items.IRON_SWORD);
        private final PaintedConstructRenderer parent;
        Blade(PaintedConstructRenderer parent) { super(parent); this.parent = parent; }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, PaintedConstructEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            if (e.kind() != PaintedConstructEntity.Kind.KNIGHT || !(getParentModel() instanceof HumanoidModel<?> hm)) return;
            pose.pushPose();
            hm.translateToHand(HumanoidArm.RIGHT, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(1 / 16f, 0.125f, -0.625f);
            parent.items.renderItem(e, SWORD, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffers, light);
            pose.popPose();
        }
    }

    /**
     * The painted beast. 0.49: one body with every part a painted animal can have; the entity's genome
     * (PaintedConstructEntity#genome) shows the head, body build, tail and extras it was drawn with - a pure wolf, panther, bear,
     * boar, griffin, dragon or lion, or a chimera mixing them. QuadrupedModel animates the legs and head.
     */
    static final class BeastModel<T extends Entity> extends QuadrupedModel<T> {
        private final ModelPart[] heads = new ModelPart[6], tails = new ModelPart[3];
        private final ModelPart wings, mane, spikes;

        BeastModel(ModelPart root) {
            super(root, false, 4f, 4f, 2f, 2f, 24);
            String[] h = {"wolf_face", "cat_face", "bear_face", "boar_face", "beak", "dragon_face"};
            for (int k = 0; k < h.length; k++) heads[k] = head.getChild(h[k]);
            ModelPart b = root.getChild("body");
            tails[0] = b.getChild("tail"); tails[1] = b.getChild("tail_bushy"); tails[2] = b.getChild("tail_spiked");
            wings = b.getChild("wings"); mane = root.getChild("head").getChild("mane"); spikes = b.getChild("spikes");
        }

        /** Shows the parts of this genome (see PaintedConstructEntity#genome). */
        void setGenome(int g) {
            int head = PaintedConstructEntity.geneHead(g), tail = PaintedConstructEntity.geneTail(g);
            for (int k = 0; k < heads.length; k++) heads[k].visible = k == head;
            for (int k = 0; k < tails.length; k++) tails[k].visible = k == tail;
            wings.visible = PaintedConstructEntity.geneWings(g);
            mane.visible = PaintedConstructEntity.geneMane(g);
            spikes.visible = PaintedConstructEntity.geneSpikes(g);
        }

        @Override
        public void setupAnim(T e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
            super.setupAnim(e, limbSwing, limbAmount, age, yaw, pitch);
            float beat = Mth.sin(age * 0.3f) * 0.35f;
            wings.getChild("wing_r").zRot = 0.5f + beat;
            wings.getChild("wing_l").zRot = -0.5f - beat;
            for (ModelPart tl : tails) tl.yRot = Mth.sin(age * 0.15f) * 0.3f;
        }

        static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3f, -3f, -5f, 6, 6, 6),
                    PartPose.offset(0f, 10f, -7f));
            // wolf: long muzzle, pointed ears (the 0.44 hound)
            head.addOrReplaceChild("wolf_face", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5f, 0f, -8f, 3, 3, 3)
                    .texOffs(24, 0).addBox(-3f, -5f, -2f, 2, 2, 1).texOffs(24, 0).addBox(1f, -5f, -2f, 2, 2, 1), PartPose.ZERO);
            // panther / lion: short flat muzzle, tall pointed ears, whisker pads
            head.addOrReplaceChild("cat_face", CubeListBuilder.create().texOffs(0, 12).addBox(-2f, 0.5f, -6.5f, 4, 2, 2)
                    .texOffs(24, 0).addBox(-3f, -6f, -2f, 2, 3, 1).texOffs(24, 0).addBox(1f, -6f, -2f, 2, 3, 1), PartPose.ZERO);
            // bear: wide blunt muzzle, round ears
            head.addOrReplaceChild("bear_face", CubeListBuilder.create().texOffs(0, 12).addBox(-2f, -0.5f, -7.5f, 4, 3, 3)
                    .texOffs(24, 0).addBox(-3.5f, -4.5f, -1f, 2, 2, 1).texOffs(24, 0).addBox(1.5f, -4.5f, -1f, 2, 2, 1), PartPose.ZERO);
            // boar: snout and upturned tusks
            head.addOrReplaceChild("boar_face", CubeListBuilder.create().texOffs(0, 12).addBox(-2f, 0f, -8f, 4, 3, 3)
                    .texOffs(24, 4).addBox(-2.5f, -1f, -8f, 1, 2, 1).texOffs(24, 4).addBox(1.5f, -1f, -8f, 1, 2, 1), PartPose.ZERO);
            // griffin: hooked beak and a crest
            head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(0, 12).addBox(-1f, -0.5f, -8f, 2, 2, 3)
                    .texOffs(0, 12).addBox(-1f, 1.5f, -8f, 2, 1, 1).texOffs(24, 0).addBox(-0.5f, -6f, -3f, 1, 3, 4), PartPose.ZERO);
            // dragon: long snout and two swept horns
            head.addOrReplaceChild("dragon_face", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5f, 0.5f, -9f, 3, 2, 4)
                    .texOffs(24, 0).addBox(-2.5f, -6f, 0f, 1, 4, 1).texOffs(24, 0).addBox(1.5f, -6f, 0f, 1, 4, 1), PartPose.ZERO);
            head.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(0, 20).addBox(-4.5f, -4.5f, -1f, 9, 9, 3), PartPose.ZERO);
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 20).addBox(-3.5f, -3.5f, -7f, 7, 7, 14),
                    PartPose.offset(0f, 12f, 0f));
            body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(36, 0).addBox(-1f, 0f, 0f, 2, 2, 8),     // children: QuadrupedModel only draws its own parts
                    PartPose.offsetAndRotation(0f, -2.5f, 6.5f, -0.6f, 0f, 0f));
            body.addOrReplaceChild("tail_bushy", CubeListBuilder.create().texOffs(36, 0).addBox(-1.5f, -0.5f, 0f, 3, 3, 7),
                    PartPose.offsetAndRotation(0f, -2.5f, 6.5f, -0.9f, 0f, 0f));
            body.addOrReplaceChild("tail_spiked", CubeListBuilder.create().texOffs(36, 0).addBox(-1f, 0f, 0f, 2, 2, 10)
                    .texOffs(24, 0).addBox(-0.5f, -1.5f, 3f, 1, 2, 1).texOffs(24, 0).addBox(-0.5f, -1.5f, 7f, 1, 2, 1),
                    PartPose.offsetAndRotation(0f, -2.5f, 6.5f, -0.3f, 0f, 0f));
            PartDefinition wings = body.addOrReplaceChild("wings", CubeListBuilder.create(), PartPose.offset(0f, -3.5f, -2f));
            wings.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 41).addBox(-12f, 0f, -3f, 12, 0, 8), PartPose.offset(-3f, 0f, 0f));
            wings.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 41).mirror().addBox(0f, 0f, -3f, 12, 0, 8), PartPose.offset(3f, 0f, 0f));
            body.addOrReplaceChild("spikes", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5f, -6f, -5f, 1, 3, 1).texOffs(24, 0).addBox(-0.5f, -6f, -1f, 1, 3, 1)
                    .texOffs(24, 0).addBox(-0.5f, -6f, 3f, 1, 3, 1), PartPose.ZERO);
            CubeListBuilder leg = CubeListBuilder.create().texOffs(44, 20).addBox(-1f, 0f, -1f, 2, 9, 2);
            root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2f, 15f, 5f));
            root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2f, 15f, 5f));
            root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2f, 15f, -5f));
            root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2f, 15f, -5f));
            return LayerDefinition.create(mesh, 64, 64);
        }
    }
}
