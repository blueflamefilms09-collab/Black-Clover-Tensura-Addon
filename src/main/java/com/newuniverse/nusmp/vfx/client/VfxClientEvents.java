package com.newuniverse.nusmp.vfx.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/** Wires the VFX system into the client. Called once from the mod constructor on the client. */
public final class VfxClientEvents {
    private VfxClientEvents() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new VfxDefinitions()));
        modBus.addListener(FxRenderer::registerShaders);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> { VfxManager.get().tick(); FxRenderer.tick(); });
        NeoForge.EVENT_BUS.addListener(VfxShake::onCameraAngles);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { VfxManager.get().clear(); FxRenderer.clear(); });
        NeoForge.EVENT_BUS.addListener(VfxClientEvents::onRenderStage);
    }

    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        VfxManager.get().render(event.getCamera(), new Matrix4f(event.getModelViewMatrix()), partial);
        FxRenderer.render(event.getCamera(), new Matrix4f(event.getModelViewMatrix()), partial);
    }
}
