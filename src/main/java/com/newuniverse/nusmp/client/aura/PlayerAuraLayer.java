package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

import java.util.Map;

/** 0.53: draws every running {@link com.newuniverse.nusmp.aura.Aura} of a player through its registered painter, in enum order. */
public class PlayerAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public PlayerAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        var running = PlayerAuraClient.running(p.getId());
        if (running == null || p.isSpectator()) return;
        float now = p.level().getGameTime() + partial;
        for (Map.Entry<com.newuniverse.nusmp.aura.Aura, long[]> e : running.entrySet()) {
            long[] w = e.getValue();
            if (now >= w[1]) continue;
            AuraPainter painter = PlayerAuraClient.painter(e.getKey());
            if (painter == null) continue;
            float t = now - w[0], left = w[1] - now, total = Math.max(1f, w[1] - w[0]);
            float fade = Mth.clamp(Math.min(t / 8f, left / 8f), 0f, 1f);
            pose.pushPose();
            try {
                painter.paint(new AuraContext(p, getParentModel(), pose, buffers, light, partial, age, t, left, total, (int) w[2], limbSwing, limbSwingAmount, netHeadYaw, headPitch, fade));
            } finally {
                pose.popPose();
            }
        }
    }
}
