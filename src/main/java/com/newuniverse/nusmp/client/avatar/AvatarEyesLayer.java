package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the eyes are the one part of an Avatar that must glow in the dark: in the creator's eye colour normally, and a pulsing red while the Yandere
 * frenzy is on (the {@code IS_FRENZIED} flag, read through the entity). Drawn by redrawing the posed model with a texture that holds only the iris
 * pixels, additive and full bright, tinted by the colour; everything else in that texture is black and adds nothing.
 */
public class AvatarEyesLayer extends RenderLayer<AvatarEntity, PlayerModel<AvatarEntity>> {
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/avatar/eyes_glow.png");

    public AvatarEyesLayer(RenderLayerParent<AvatarEntity, PlayerModel<AvatarEntity>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AvatarEntity e, float limbSwing, float limbSwingAmount, float partial, float age,
                       float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;
        int argb = argb(AvatarLooks.of(e), e.frenzied(), age);
        getParentModel().renderToBuffer(pose, buffers.getBuffer(AuraRender.additive(EYES)), PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
    }

    /** The eye colour to draw: the creator's own, or a red that pulses when frenzied. */
    static int argb(AppearanceData look, boolean frenzied, float age) {
        if (frenzied) {
            int g = (int) (24 + 24 * (0.5f + 0.5f * Mth.sin(age * 0.5f)));
            return 0xFF000000 | (255 << 16) | (g << 8) | 16;
        }
        return 0xFF000000 | (look.eyeRgb() & 0xFFFFFF);
    }
}
