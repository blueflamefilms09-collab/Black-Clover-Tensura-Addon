package com.newuniverse.nusmp.client.relics.worn.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The emissive pass of a worn armour relic: vanilla's own outer-armour model (the one the helmet, chest piece and boots are drawn with) is drawn once more, posed
 * like the wearer, over the glow channel the painter made ({@code ArmorPainter#paintGlow}), with RenderType.eyes. The model is the same size as the armour, so the
 * glowing pixels land exactly on the painted ones and shine over them at full brightness. Client thread only.
 */
public final class ArmorGlow {
    private ArmorGlow() {}

    private static HumanoidModel<AbstractClientPlayer> model;
    private static boolean failed;

    /** Draws the glow of one armour relic of {@code slot} (head, chest or feet) with the texture {@code glowTex}, at brightness {@code k} (about 0.6 .. 1). */
    public static void draw(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> parent, RelicSlot slot, ResourceLocation glowTex, float k) {
        if (failed) return;
        if (model == null) {
            try {
                model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            } catch (RuntimeException ex) {
                failed = true;
                LogUtils.getLogger().error("[nusmp] the armour glow model could not be baked; armour relics do not glow", ex);
                return;
            }
        }
        parent.copyPropertiesTo(model);
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> model.head.visible = true;
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> { return; }
        }
        int v = Math.min(255, Math.max(0, (int) (255f * k)));
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(glowTex)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFF000000 | v << 16 | v << 8 | v);
    }

    /** Forgets the baked model (a resource reload re-bakes the layers). */
    public static void clear() {
        model = null;
        failed = false;
    }
}
