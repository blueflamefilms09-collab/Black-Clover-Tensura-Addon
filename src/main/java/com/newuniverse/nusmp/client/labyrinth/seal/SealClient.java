package com.newuniverse.nusmp.client.labyrinth.seal;

import com.newuniverse.nusmp.labyrinth.seal.SealRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: the client half of the seals: one listener on the mod bus that hands the Floor Warden its renderer. Kept in {@code client/} and
 * reached only through {@code client.labyrinth.LabyrinthClient.init} so no renderer class is loaded on a dedicated server (the split of
 * {@code client/dungeon/DungeonMobsClient} against {@code dungeon/}). The doors and the altar are plain JSON models and need nothing here.
 */
public final class SealClient {
    private SealClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(SealClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(SealRegistry.FLOOR_WARDEN.get(), FloorWardenRenderer::new);
        e.registerEntityRenderer(com.newuniverse.nusmp.labyrinth.mega.guard.GuardRegistry.GUARDIAN.get(), FloorWardenRenderer::new);   // 1.54.0: the sector Guardian wears the Warden's model, larger
    }
}
