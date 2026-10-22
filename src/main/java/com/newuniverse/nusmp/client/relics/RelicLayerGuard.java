package com.newuniverse.nusmp.client.relics;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.relics.WornOnce;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Why: the worn relic layer reached a player renderer only through one event ({@code EntityRenderersEvent.AddLayers}, once per renderer build), and only for a renderer that is a
 * {@code PlayerRenderer} by class. If another mod swaps the renderer a player is drawn with after that event, wraps it, or rebuilds it without the event, the layer is simply absent:
 * nothing is drawn and nothing says so. This is the safety net: the first time a player is about to be drawn by a renderer that never got the layer, it is attached then (once per
 * renderer, logged), provided the renderer draws a {@link PlayerModel}. A renderer that got the layer from the event is recorded by {@link #known} and never touched again.
 * Client render thread only.
 */
public final class RelicLayerGuard {
    private RelicLayerGuard() {}

    private static final Set<Object> KNOWN = Collections.newSetFromMap(new WeakHashMap<>());
    private static LivingEntityRenderer<?, ?> lastSeen;
    private static volatile int healed;
    private static volatile String lastHealed = "";

    /** Records a renderer that has the worn layer (added by the layer event). */
    public static void known(Object renderer) {
        synchronized (KNOWN) { KNOWN.add(renderer); }
    }

    /** How many renderers got the layer from this guard instead of the event, since the game started. */
    public static int healedCount() { return healed; }

    /** The class name of the last renderer the guard attached the layer to ("" if none). */
    public static String lastHealed() { return lastHealed; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void attachHumanoid(LivingEntityRenderer renderer) { renderer.addLayer(new HumanoidRelicLayer(renderer)); }

    /** Game-bus listener: {@code RenderLivingEvent.Pre}. */
    @SuppressWarnings("unchecked")
    public static void onPre(RenderLivingEvent.Pre<?, ?> e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer)) return;
        LivingEntityRenderer<?, ?> r = e.getRenderer();
        if (r == lastSeen) return;                                                       // 1.99.0: the renderer of the last player drawn is nearly always this one: no lock, no weak-map lookup per player per frame
        synchronized (KNOWN) {
            if (!KNOWN.add(r)) return;                                                   // already has it (the event added it, or the guard did)
        }
        lastSeen = r;
        if (!(r.getModel() instanceof PlayerModel<?>)) {
            if (r.getModel() instanceof HumanoidModel<?>) {                              // relics_wear: a body form of another mod (a humanoid model that is not a PlayerModel): the humanoid worn layer fits it
                try {
                    attachHumanoid(r);
                    healed++;
                    lastHealed = r.getClass().getName();
                    WornOnce.log(WornOnce.Cause.RENDERER_NOT_PLAYER_MODEL, r.getClass().getName());
                } catch (RuntimeException | LinkageError failed) {
                    LogUtils.getLogger().error("[nusmp] the humanoid worn relic layer could not be attached to {}", r.getClass().getName(), failed);
                }
                return;
            }
            WornOnce.log(WornOnce.Cause.RENDERER_NOT_HUMANOID, r.getClass().getName() + " (model " + r.getModel().getClass().getName() + ")");
            LogUtils.getLogger().warn("[nusmp] a player is drawn by {} whose model is neither a PlayerModel nor a HumanoidModel: worn relics cannot be attached to it", r.getClass().getName());
            return;
        }
        try {
            ((LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) r)
                    .addLayer(new RelicWornLayer((RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) r));
            healed++;
            lastHealed = r.getClass().getName();
            WornOnce.log(WornOnce.Cause.LAYER_LATE, lastHealed);
            LogUtils.getLogger().warn("[nusmp] the renderer {} drew a player without the worn relic layer (the layer event never reached it); the layer was attached at the first frame", lastHealed);
        } catch (RuntimeException | LinkageError failed) {
            LogUtils.getLogger().error("[nusmp] the worn relic layer could not be attached to {}", r.getClass().getName(), failed);
        }
    }
}
