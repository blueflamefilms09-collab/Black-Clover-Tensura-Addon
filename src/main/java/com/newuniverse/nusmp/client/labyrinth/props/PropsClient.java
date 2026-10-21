package com.newuniverse.nusmp.client.labyrinth.props;

import com.newuniverse.nusmp.labyrinth.props.PropsRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: the client half of the dungeon props: the block entity renderer of the weapon rack, registered through {@code EntityRenderersEvent.RegisterRenderers} on the mod bus (the pattern of
 * {@code RiftClient}); kept apart from {@code PropsRegistry} so a dedicated server never loads a client class.
 */
public final class PropsClient {
    private PropsClient() {}

    public static void init(IEventBus modBus) { modBus.addListener(PropsClient::onRegisterRenderers); }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(PropsRegistry.RACK_BE.get(), WeaponRackRenderer::new);
    }
}
