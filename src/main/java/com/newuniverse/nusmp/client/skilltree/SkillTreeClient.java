package com.newuniverse.nusmp.client.skilltree;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one client entry point of the Skill Tree, called once from the client section of the mod constructor. It starts the client parts of the items and the shop so that the
 * constructor keeps its single line, and it starts the screen's own two hooks: the menu injection (a listener on the NeoForge event bus, because screen events are game events and
 * not mod-lifecycle events) and the cleanup on logout (the screen remembers the last route, the pan and zoom of every route and the icon cache in static maps so that reopening it
 * returns the player to where they were; those maps belong to one world and must not leak into the next, which may have other routes and items). The core system's own client hook,
 * which forgets the synced routes on logout, is {@code TreeCoreClient} and is started by the core; this class does not touch it. Owned by the GUI builder.
 */
public final class SkillTreeClient {
    private SkillTreeClient() {}

    public static void init(IEventBus modBus) {
        ItemsClient.init(modBus);
        ShopClient.init(modBus);
        MenuInjection.init();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            SkillTreeScreen.forget();
            MenuInjection.forget();
        });
    }
}
