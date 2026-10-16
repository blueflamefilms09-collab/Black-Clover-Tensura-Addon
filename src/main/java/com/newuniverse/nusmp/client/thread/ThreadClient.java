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
        // 1.46.0: Rouge can be picked (for a name tag) only while the local player holds one
        com.newuniverse.nusmp.thread.RougeRest.namingHand = () -> {
            var pl = net.minecraft.client.Minecraft.getInstance().player;
            return pl != null && (pl.getMainHandItem().is(net.minecraft.world.item.Items.NAME_TAG) || pl.getOffhandItem().is(net.minecraft.world.item.Items.NAME_TAG));
        };
        e.registerEntityRenderer(ThreadEntities.ROUGE.get(), RougeRenderer::new);
        e.registerEntityRenderer(ThreadEntities.THREAD_TRAP.get(), ThreadTrapRenderer::new);
    }
}
