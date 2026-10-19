package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: page 3 of the creation screen shows the Avatar being designed, live, turning slowly, while no Avatar entity exists yet. This draws it without
 * any entity: one baked player model (kept, rebuilt only after a model reload), the same composed skin and the same 3D parts the world renderer uses
 * ({@link AvatarSkinCache}, {@link AvatarParts}), in the GUI's own pose, lit like the inventory figure. No per-frame model or texture is created.
 */
final class AvatarPreview {
    private AvatarPreview() {}

    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/avatar/eyes_glow.png");
    private static PlayerModel<LivingEntity> model;
    private static EntityModelSet baked;
    /** The preview's hair keeps its own springs (no entity moves it: it sways on the clock). */
    private static final com.newuniverse.nusmp.client.playermodel.HairState HAIR = com.newuniverse.nusmp.client.playermodel.HairState.create();

    private static PlayerModel<LivingEntity> model() {
        EntityModelSet set = Minecraft.getInstance().getEntityModels();
        if (model == null || baked != set) {
            model = new PlayerModel<>(set.bakeLayer(ModelLayers.PLAYER), false);
            model.young = false;
            baked = set;
        }
        return model;
    }

    /**
     * @param cx the horizontal middle and feetY the ground line in GUI pixels, pxPerBlock the size of one block, spin the turn in degrees
     * @param z a depth in front of the panel
     */
    static void draw(GuiGraphics g, int cx, int feetY, float pxPerBlock, AppearanceData look, String raceId, float spin, float age) {
        PlayerModel<LivingEntity> m = model();
        MultiBufferSource.BufferSource bs = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack pose = g.pose();
        g.flush();
        pose.pushPose();
        float sc = pxPerBlock * look.scale();
        pose.translate(cx, feetY - 1.501f * sc, 150f);          // the model's origin is at the neck, 1.5 blocks above the feet
        pose.scale(sc, sc, sc);
        pose.mulPose(Axis.YP.rotationDegrees(180f + spin));      // the model faces -z; the viewer is on +z
        Lighting.setupForEntityInInventory();
        int light = PropDraw.FULL_BRIGHT;
        ResourceLocation skin = AvatarSkinCache.skin(look, raceId);
        m.renderToBuffer(pose, bs.getBuffer(RenderType.entityCutoutNoCull(skin)), light, OverlayTexture.NO_OVERLAY, -1);
        AvatarParts.drawHair(pose, bs, light, m, look, HAIR, age, false);
        AvatarParts.draw(pose, bs, light, m, look, age / 20f, 1f, 0.5f + 0.5f * Mth.sin(age * 0.11f));
        m.renderToBuffer(pose, bs.getBuffer(AuraRender.additive(EYES)), light, OverlayTexture.NO_OVERLAY, 0xFF000000 | (look.eyeRgb() & 0xFFFFFF));
        bs.endBatch();
        pose.popPose();
        g.flush();
        Lighting.setupFor3DItems();
    }
}
