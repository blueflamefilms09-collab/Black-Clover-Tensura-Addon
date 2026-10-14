package com.newuniverse.nusmp.core.discovery;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one entry point of the discovery system. From the mod constructor, once (both sides):
 * {@code com.newuniverse.nusmp.core.discovery.Discovery.init(modEventBus, modContainer);}
 * and, in the client-only block, {@code com.newuniverse.nusmp.client.discovery.DiscoveryClient.init();}.
 * <p>
 * What it does: registers the player attachment and the {@code nusmp:discovery_sync} payload on the mod bus, the server config
 * {@code nusmp-discovery-server.toml} (key {@code discovery.showAll}), and the server tracker on the game bus.
 * See docs/discovery.md.
 */
public final class Discovery {
    private Discovery() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        DiscoveryAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(DiscoveryNet::registerPayloads);
        container.registerConfig(ModConfig.Type.SERVER, DiscoveryConfig.SPEC, "nusmp-discovery-server.toml");
        DiscoveryTracker.register(NeoForge.EVENT_BUS);
    }
}
