package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.fx.RelicFxClient;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Wires the worn model library into the client: forgets the parsed models and the catalog on every resource reload (so a resource pack can replace them), and
 * starts the ambient particles. One call from {@code RelicClient.init}.
 */
public final class WornLibClient {
    private WornLibClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener((ResourceManagerReloadListener) mgr -> {
                    WornModels.clear();
                    WornLibrary.clear();
                    ArmorGlow.clear();
                    ArmorItems.clear();
                    com.newuniverse.nusmp.client.relics.vu.VuModels.clear();           // visual upgrade: the override table
                    com.newuniverse.nusmp.client.relics.vu.VuFxClient.clear();         // visual upgrade: the fx files
                    WornLibDraw.clearCustom();                                         // visual upgrade: the texture locations
                }));
        modBus.addListener(ArmorItemRenderer::registerExtensions);
        modBus.addListener(ArmorItemRenderer::registerModels);
        com.newuniverse.nusmp.client.relics.hand.HandClient.init(modBus);                  // 1.48.0: relics as held objects
        RelicFxClient.init();
        com.newuniverse.nusmp.client.relics.vu.VuFxClient.init();                           // visual upgrade: the particle layers and the draw animation clock
        com.newuniverse.nusmp.client.relics.fx.UpgradeFxClient.init();                     // 1.102.0: the ambient layers of the upgraded God relics (data: assets/nusmp/visual_upgrade)
    }
}
