package com.newuniverse.nusmp.client.labyrinth.rift;

import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: the client half of the rifts: the block entity renderer of the Dimensional Rift, registered through {@code EntityRenderersEvent.RegisterRenderers} on the mod bus (the pattern of
 * {@code VaultClient}); kept apart from {@code RiftRegistry} so a dedicated server never loads a client class.
 */
public final class RiftClient {
    private RiftClient() {}

    public static void init(IEventBus modBus) { modBus.addListener(RiftClient::onRegisterRenderers); }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(RiftRegistry.RIFT_BE.get(), DimensionalRiftRenderer::new);
    }
}
