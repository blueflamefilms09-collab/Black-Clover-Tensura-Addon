package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.entity.avatar.AvatarEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderer of the Devil Avatar. Register it from the client section of NUSMP: {@code modEventBus.addListener(AvatarClient::register)}. */
public final class AvatarClient {
    private AvatarClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(AvatarEntities.DEVIL_AVATAR.get(), AvatarRenderer::new);
    }
}
