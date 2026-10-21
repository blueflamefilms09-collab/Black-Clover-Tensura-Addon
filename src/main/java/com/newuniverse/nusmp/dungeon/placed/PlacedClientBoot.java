package com.newuniverse.nusmp.dungeon.placed;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Why: the one client-only piece of the player-placed exemption, kept in its own class so the dedicated server never loads a client event class: the mirror
 * of the server's marks is forgotten when the player leaves a world (another world's positions mean nothing). Called only on the physical client.
 */
public final class PlacedClientBoot {
    private PlacedClientBoot() {}

    public static void init(IEventBus gameBus) {
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> PlacedMirror.clear());
    }
}
