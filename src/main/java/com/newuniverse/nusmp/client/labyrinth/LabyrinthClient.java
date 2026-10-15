package com.newuniverse.nusmp.client.labyrinth;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the client half of {@link com.newuniverse.nusmp.labyrinth.LabyrinthSystem}: one line in the {@code Dist.CLIENT} section of the mod
 * class calls every Labyrinth package's client init (renderers, dimension sky, block-entity renderers). Kept apart from the common init so
 * no client class is ever loaded on a dedicated server (the same split as {@code client/dungeon/DungeonSky} against {@code dungeon/}).
 */
public final class LabyrinthClient {
    private LabyrinthClient() {}

    public static void init(IEventBus modBus) {
        com.newuniverse.nusmp.client.labyrinth.gate.GateClient.init(modBus);                               // C: the Labyrinth's sea-of-clouds sky
        com.newuniverse.nusmp.client.labyrinth.trial.TrialClient.init(modBus);                             // E1: barrier and archway renderers
        com.newuniverse.nusmp.client.labyrinth.seal.SealClient.init(modBus);                               // E2: Floor Warden renderer, altar
        com.newuniverse.nusmp.client.labyrinth.vault.VaultClient.init(modBus);                             // D: pedestal renderer
    }
}
