package com.newuniverse.nusmp.client.relics.order;

import com.newuniverse.nusmp.relics.order.OrderEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderer of the God of Order. Register it from the client section of NUSMP: {@code modEventBus.addListener(OrderClient::register)}. */
public final class OrderClient {
    private OrderClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(OrderEntities.GOD_OF_ORDER.get(), GodOfOrderRenderer::new);
    }
}
