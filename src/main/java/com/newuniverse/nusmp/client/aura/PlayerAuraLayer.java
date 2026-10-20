package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.perf.PerfMath;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.perf.PerfSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * 0.53: draws every running {@link com.newuniverse.nusmp.aura.Aura} of a player through its registered painter, in enum order.
 *
 * <p>1.99.0 (performance pass, docs/performance_1_99.md): a level of detail by the distance from the camera, so a crowd of players with auras does not cost a frame each. Up to
 * {@code auraLodNear} blocks (48 by default) every running aura is painted exactly as before; between that and {@code auraLodFar} (96) only the {@code auraMidCount} most recently started
 * ones are; beyond it none (the player is a few pixels tall there, and vanilla stops drawing players at about 64 blocks anyway). The painted auras keep their enum order. The numbers are in
 * nusmp-performance-common.toml; the decision is the pure {@link PerfMath#auraCount}. The section {@code c.aura.layer} of the performance probe times the layer.
 */
public class PlayerAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.aura.layer");

    public PlayerAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        var running = PlayerAuraClient.running(p.getId());
        if (running == null || p.isSpectator()) return;
        float now = p.level().getGameTime() + partial;
        int live = 0;
        for (long[] w : running.values()) if (now < w[1]) live++;
        if (live == 0) return;
        long t0 = PerfProbe.begin();
        int allowed = PerfMath.auraCount(live, Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(p),
                PerfSettings.auraLodNear(), PerfSettings.auraLodFar(), PerfSettings.auraMidCount());
        if (allowed > 0) {
            long newest = allowed < live ? PerfMath.nthNewestStart(running.values(), now, allowed) : Long.MIN_VALUE;           // the start tick the allowed-th most recent aura has
            for (Map.Entry<com.newuniverse.nusmp.aura.Aura, long[]> e : running.entrySet()) {
                long[] w = e.getValue();
                if (now >= w[1]) continue;
                if (w[0] < newest) continue;                                                              // thinned out by the distance
                AuraPainter painter = PlayerAuraClient.painter(e.getKey());
                if (painter == null) continue;
                float t = now - w[0], left = w[1] - now, total = Math.max(1f, w[1] - w[0]);
                float fade = Mth.clamp(Math.min(t / 8f, left / 8f), 0f, 1f);
                pose.pushPose();
                try {
                    painter.paint(new AuraContext(p, getParentModel(), pose, new LazyBuffers(buffers), light, partial, age, t, left, total, (int) w[2], limbSwing, limbSwingAmount, netHeadYaw, headPitch, fade));
                } finally {
                    pose.popPose();
                }
            }
        }
        PerfProbe.end(SECTION, t0);
    }
}
