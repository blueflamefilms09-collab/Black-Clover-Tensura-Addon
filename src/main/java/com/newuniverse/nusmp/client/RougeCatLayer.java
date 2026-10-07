package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.RougeCat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 0.49: Rouge, the red-thread cat, sitting on her summoner's head (see book.RougeCat). */
public class RougeCatLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/rouge_cat.png");
    private final RougeCatModel cat;

    public RougeCatLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, RougeCatModel cat) {
        super(parent);
        this.cat = cat;
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) { e.registerLayerDefinition(RougeCatModel.LAYER, RougeCatModel::createLayer); }

    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        RougeCatModel model = new RougeCatModel(e.getEntityModels().bakeLayer(RougeCatModel.LAYER));
        for (PlayerSkin.Model skin : e.getSkins())
            if (e.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new RougeCatLayer(r, model));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        if (!RougeCat.onHead(p.getId(), p.level().getGameTime())) return;
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        pose.translate(0, -0.5f + Mth.sin(age * 0.1f) * 0.01f, 0);           // the top of the head (y points down here)
        float s = 0.55f;
        pose.scale(s, s, s);
        pose.translate(0, -1.5f, 0.05f);                                      // the cat's paws (model y 24) onto the head
        cat.animate(age);
        cat.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }
}
