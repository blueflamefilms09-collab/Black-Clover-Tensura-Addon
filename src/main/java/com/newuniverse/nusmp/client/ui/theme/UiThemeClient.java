package com.newuniverse.nusmp.client.ui.theme;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Registers the parts of the anime look that are not screens the mod opens itself: the page-unlock notification (a GUI layer and a client tick
 * listener), the overlay on Tensura's ability windows and the relic tooltip colours. Called once from the client section of the mod constructor:
 * {@code UiThemeClient.init(modEventBus)}. Every part checks {@link UiTheme#anime()} when it draws, so the config switch takes effect at once.
 */
public final class UiThemeClient {
    private UiThemeClient() {}

    public static void init(IEventBus modBus) {
        PageUnlockOverlay.init(modBus);
        TensuraScreenTheme.init();
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.relics.RelicTooltip::onColor);
    }
}
