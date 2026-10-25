package com.newuniverse.nusmp.client.veldanava;

import com.newuniverse.nusmp.entity.veldanava.VeldanavaEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderers of Veldanava: the boss (three bodies in one renderer) and the Will of the Creator. Client only: it is wired from the client branch of NUSMP. */
public final class VeldanavaClient {
    private VeldanavaClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(VeldanavaEntities.VELDANAVA.get(), VeldanavaRenderer::new);
        e.registerEntityRenderer(VeldanavaEntities.WILL.get(), VeldanavaOrbRenderer::new);
    }
}
