package com.newuniverse.nusmp.client.floor.charybdis;

import com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderers of the Charybdis Basin: the leviathan and its magicule scales. Register {@link #register} on the mod bus, client side only. */
public final class CharybdisClient {
    private CharybdisClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CharybdisEntities.CHARYBDIS_SPAWN.get(), CharybdisSpawnRenderer::new);
        e.registerEntityRenderer(CharybdisEntities.MAGICULE_SCALE.get(), MagiculeScaleRenderer::new);
    }
}
