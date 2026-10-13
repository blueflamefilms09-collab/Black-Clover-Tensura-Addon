package com.newuniverse.nusmp.client.floor.goliath;

import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderer of the Goliath. Register {@link #register} on the mod bus on the client. */
public final class GoliathClient {
    private GoliathClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(GoliathEntities.GOLIATH.get(), GoliathRenderer::new);
    }
}
