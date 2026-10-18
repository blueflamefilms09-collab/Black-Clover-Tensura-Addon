package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the routes a client holds are a copy of ONE server's data, so when the player leaves that world the copy must go, otherwise the next connection (to a server that does not have the mod, or
 * has other routes) would briefly show the old server's trees in tooltips and the creative tab. Leaving a world is a client-only event ({@code ClientPlayerNetworkEvent.LoggingOut}), so the
 * listener lives in a client class that the core starts only when the game is a client ({@code TreeCore.init} checks the dist, as the rest of the mod does for its client code), which keeps
 * a dedicated server from ever loading it. The sync handlers themselves need no client class (they only store into common classes), so this is the only client code of the core.
 */
public final class TreeCoreClient {
    private TreeCoreClient() {}

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> RouteRegistry.clearClient());
    }
}
