package com.newuniverse.nusmp.client.meteor;

import com.newuniverse.nusmp.meteor.MeteorEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client side of the shared meteor: the renderer. Called from {@code MeteorBoot} behind a client side check. */
public final class MeteorClient {
    private MeteorClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(MeteorEntities.METEOR.get(), MeteorRenderer::new));
    }
}
