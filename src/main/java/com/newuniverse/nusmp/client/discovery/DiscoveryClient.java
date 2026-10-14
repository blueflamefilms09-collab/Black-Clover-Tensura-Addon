package com.newuniverse.nusmp.client.discovery;

import com.newuniverse.nusmp.core.discovery.DiscoveryCache;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only hook of the discovery system: the cache is wiped when the player leaves a world, so the next one starts clean. */
public final class DiscoveryClient {
    private DiscoveryClient() {}

    private static boolean done;

    public static synchronized void init() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> DiscoveryCache.clear());
    }
}
