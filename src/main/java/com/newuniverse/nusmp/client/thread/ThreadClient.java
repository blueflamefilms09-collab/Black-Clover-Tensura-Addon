package com.newuniverse.nusmp.client.thread;

import com.newuniverse.nusmp.thread.ThreadEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: one place registers the client renderers of Vanessa's Thread Magic (Rouge and the trap node) so the lead wires a single
 * listener: {@code modEventBus.addListener(ThreadClient::register)} inside the client-only block of NUSMP. Rouge's model reuses the
 * existing {@code RougeCatModel.LAYER}, which {@code RougeCatLayer.layers} already registers, so no layer is registered here.
 */
public final class ThreadClient {
    private ThreadClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ThreadEntities.ROUGE.get(), RougeRenderer::new);
        e.registerEntityRenderer(ThreadEntities.THREAD_TRAP.get(), ThreadTrapRenderer::new);
    }
}
