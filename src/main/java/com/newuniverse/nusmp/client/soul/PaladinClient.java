package com.newuniverse.nusmp.client.soul;

import com.newuniverse.nusmp.soul.PaladinNet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one client entry point of the Paladin look, so the lead wires a single line: the layer for every living-entity renderer
 * and both player skins (mod bus), the overlay for the renderers that cannot take a layer (game bus), and the log-out that empties
 * the two small client-side sets (the server's announcements and the ids drawn this frame) so nothing survives a world change.
 * Call {@code PaladinClient.register(modEventBus)} from the client-only block of the NUSMP constructor.
 */
public final class PaladinClient {
    private PaladinClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers e) -> PaladinLayer.addLayers(e));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> PaladinOverlay.onRender(e));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { PaladinNet.clear(); PaladinLayer.DRAWN.clear(); });
    }
}
