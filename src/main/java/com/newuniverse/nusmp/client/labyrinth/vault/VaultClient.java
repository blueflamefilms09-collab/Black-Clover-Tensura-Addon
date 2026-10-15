package com.newuniverse.nusmp.client.labyrinth.vault;

import com.newuniverse.nusmp.labyrinth.vault.VaultRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: the client half of the vault: the Relic Pedestal's block entity renderer. Registered through {@code EntityRenderersEvent.RegisterRenderers}
 * on the mod bus (NeoForge's one entry point for block entity renderers); a single {@link #init} so {@code LabyrinthClient} can call it in order
 * with the other Labyrinth client inits. Kept apart from {@code VaultRegistry} so the common code never touches a client class.
 */
public final class VaultClient {
    private VaultClient() {}

    public static void init(IEventBus modBus) { modBus.addListener(VaultClient::onRegisterRenderers); }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(VaultRegistry.PEDESTAL_BE.get(), RelicPedestalRenderer::new);
    }
}
