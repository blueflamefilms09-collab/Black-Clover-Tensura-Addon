package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.RougeCat;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.thread.RougeRest;
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
    /** The remodel: draw the art kit's Rouge on the head instead of the old cube cat (which stays in the tree, one switch away). */
    static final boolean KIT_CAT = true;
    static final float KIT_CAT_SCALE = 0.36f;
    private static final GeoSpec KIT_SPEC = GeoSpec.of("arc", "rouge");
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
        // the old head cat (RougeCat payload, used when thread.rougeEntity is off) or, from 1.46.0, the Rouge entity resting on this player's head
        if (!RougeCat.onHead(p.getId(), p.level().getGameTime()) && RougeRest.on(p.getUUID()) == null) return;
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        pose.translate(0, -0.5f + Mth.sin(age * 0.1f) * 0.01f, 0);           // the top of the head (y points down here)
        if (KIT_CAT) {                                                        // the remodel: the art kit's Rouge (tools/arc/char_rouge.py) sits on the head in her `sit` pose
            float k = KIT_CAT_SCALE;
            pose.scale(k, k, k);
            pose.translate(0, -1.5f, 0.05f);
            GeoDraw.paint(pose, buffers, KIT_SPEC, "sit", age / 20f, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0xFFB0B0B0);
            pose.popPose();
            return;
        }
        float s = 0.55f;
        pose.scale(s, s, s);
        pose.translate(0, -1.5f, 0.05f);                                      // the cat's paws (model y 24) onto the head
        cat.animate(age);
        cat.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }
}
