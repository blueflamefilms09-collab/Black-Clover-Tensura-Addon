package com.newuniverse.nusmp.avatar.inventory.client;

import com.newuniverse.nusmp.avatar.inventory.AvatarInventoryMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Why: binds the Avatar inventory menu to its screen. Kept in its own class so the screen (a client-only class) is never loaded on a dedicated server: the menu
 * registration calls this only when the game runs as a client.
 */
public final class AvatarInventoryClient {
    private AvatarInventoryClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(AvatarInventoryMenus.INVENTORY.get(), AvatarInventoryScreen::new));
    }
}
