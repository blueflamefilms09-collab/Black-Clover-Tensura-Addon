package com.newuniverse.nusmp.client.ui.theme;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Registers the parts of the anime look that are not screens the mod opens itself: the page-unlock notification (a GUI layer and a client tick
 * listener), the overlay on Tensura's ability windows (off unless ui.tensuraOverlay is on), the Codex's resource-pack reload listener and the relic tooltip colours. Called once from the client section of the mod constructor:
 * {@code UiThemeClient.init(modEventBus)}. Every part checks {@link UiTheme#anime()} when it draws, so the config switch takes effect at once.
 */
public final class UiThemeClient {
    private UiThemeClient() {}

    public static void init(IEventBus modBus) {
        PageUnlockOverlay.init(modBus);
        com.newuniverse.nusmp.client.ui.gs.GsNotices.init(modBus);                       // 2.x: the alert crest of the great_spirit look (draws nothing in the other themes)
        TensuraScreenTheme.init();
        com.newuniverse.nusmp.client.ui.codex.CodexClient.init(modBus);                  // 1.37.0: the data-driven Codex reloads with the resource packs
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.relics.RelicTooltip::onColor);
        com.newuniverse.nusmp.client.ui.tooltip.UnifiedTooltip.init(modBus);              // 1.110.0: every item of the mod gets the relic tooltip's layout (docs/ui/unified_tooltips.md)
    }
}
