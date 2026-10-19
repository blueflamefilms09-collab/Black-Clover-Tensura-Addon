package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the emissive skin pass. A skin that has a glow map ({@code <skin>_glow.png}, painted where the character must shine: glowing eyes, cracks, runes, lit armour) is drawn a second time,
 * the same posed model, additive and full bright, in the glow colour times the frame's glow level, so a boss brightens as it draws power and starves when it is exposed. A look without
 * {@code glow}, a skin composed at run time, or a glow map the pack does not have draws nothing (an absent file is never an error).
 */
public class GlowLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    private final PlayerModelRenderer.Body<T> owner;

    public GlowLayer(PlayerModelRenderer.Body<T> owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        PlayerLook look = owner.look();
        if (look == null || e.isInvisible()) return;
        boolean flashOnly = !look.glow() || owner.playerBody().skin(e) != null;
        PlayerBody.Dynamic d = owner.dynamic();
        if (d.flash > 0.02f) {                      // the burst of colour over a power-up: the whole skin, additive
            int fa = Math.max(0, Math.min(255, Math.round(255f * Math.min(1f, d.flash))));
            getParentModel().renderToBuffer(pose, buffers.getBuffer(AuraRender.additive(getTextureLocation(e))), PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (fa << 24) | (d.flashRgb & 0xFFFFFF));
        }
        if (flashOnly) return;
        float level = d.glowLevel * (look.alpha() * d.alpha);
        if (level <= 0.01f) return;
        ResourceLocation tex = PlayerAssets.glowOf(look.skin());
        if (!PlayerAssets.exists(tex)) return;
        int rgb = d.glowRgb >= 0 ? d.glowRgb : look.glowRgb();
        int a = Math.max(0, Math.min(255, Math.round(255f * Math.min(1f, level))));
        getParentModel().renderToBuffer(pose, buffers.getBuffer(AuraRender.additive(tex)), PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (a << 24) | (rgb & 0xFFFFFF));
    }
}
