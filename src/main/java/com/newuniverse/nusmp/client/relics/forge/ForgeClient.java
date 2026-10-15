package com.newuniverse.nusmp.client.relics.forge;

import com.newuniverse.nusmp.relics.forge.ForgeRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Why: a menu type with no screen registered crashes the client the moment the table is opened, and a client class must never be named by common code,
 * so the one registration of the Relic Forge screen lives here, in the client package, and is called from the client-only branch of the mod
 * constructor next to the other client inits.
 */
public final class ForgeClient {
    private ForgeClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ForgeRegistry.MENU.get(), ForgeScreen::new));
    }
}
