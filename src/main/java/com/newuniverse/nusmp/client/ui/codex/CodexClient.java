package com.newuniverse.nusmp.client.ui.codex;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Why: the one wiring point of the data-driven Codex. Called from {@code UiThemeClient.init(modBus)} (already part of the client section of the
 * mod constructor), so the mod constructor needs no new line. The listener goes on the mod bus because {@code RegisterClientReloadListenersEvent}
 * is a mod-bus event.
 */
public final class CodexClient {
    private CodexClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener(new CodexReloadListener()));
    }
}
