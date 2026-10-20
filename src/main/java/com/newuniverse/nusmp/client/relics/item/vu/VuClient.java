package com.newuniverse.nusmp.client.relics.item.vu;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one registration call of the visual upgrade of the Genesis weapons (client): {@code VuClient.init(modEventBus)} from the client section of the mod constructor, next to
 * {@code WeaponRenderer.init}. The config file ({@link VuOptions#SPEC}) is registered by the mod constructor like the other client files. It clears the mesh and effect caches
 * when the models are rebaked (a resource reload) and when the player leaves a world, and starts the particle tick ({@link VuFx}).
 */
public final class VuClient {
    private VuClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((ModelEvent.BakingCompleted e) -> {
            VuRenderer.clear();
            VuFx.clear();
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> VuFx.clear());
        VuFx.init();
    }
}
