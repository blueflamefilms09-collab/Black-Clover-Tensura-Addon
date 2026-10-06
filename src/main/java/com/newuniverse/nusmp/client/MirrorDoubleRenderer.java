package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.entity.MirrorDoubleEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.UUID;

/**
 * 0.42: a Real Double is drawn as its owner, from the mirror world: the owner's own skin, mirrored left to right (the wiki: "the
 * clone has inverted outer features because it is the mirrored version of the user"), holding the owner's weapon, with a
 * shimmering violet glass sheen over the whole body.
 */
public class MirrorDoubleRenderer extends LivingEntityRenderer<MirrorDoubleEntity, PlayerModel<MirrorDoubleEntity>> {
    public MirrorDoubleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        addLayer(new Sheen(this));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.MIRROR_DOUBLE.get(), MirrorDoubleRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(MirrorDoubleEntity e) {
        UUID owner = e.getOwnerUUID();
        if (owner == null) return DefaultPlayerSkin.getDefaultTexture();
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(owner);
        return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(owner).texture();
    }

    /** Mirrored left to right, at player scale. */
    @Override
    protected void scale(MirrorDoubleEntity e, PoseStack pose, float partial) {
        pose.scale(-0.9375f, 0.9375f, 0.9375f);
    }

    @Override
    protected boolean shouldShowName(MirrorDoubleEntity e) { return false; }

    /** The glass: the model again, full-bright and translucent violet, pulsing. */
    static final class Sheen extends RenderLayer<MirrorDoubleEntity, PlayerModel<MirrorDoubleEntity>> {
        Sheen(MirrorDoubleRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, MirrorDoubleEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            float pulse = 0.5f + 0.5f * Mth.sin(age * 0.15f);
            int alpha = (int) (60 + 50 * pulse);
            int color = (alpha << 24) | 0xB89CFF;
            ResourceLocation tex = getTextureLocation(e);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(tex)), 0xF000F0, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
