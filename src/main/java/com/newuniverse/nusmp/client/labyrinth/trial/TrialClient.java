package com.newuniverse.nusmp.client.labyrinth.trial;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the client half of the trial chambers, kept so {@code client.labyrinth.LabyrinthClient} has one line per package and so a renderer
 * can be added here later without touching the common side. Today it registers nothing on purpose: the Magic Barrier is translucent
 * through its model's {@code render_type} (NeoForge's model key, no {@code ItemBlockRenderTypes} call needed since 1.19), the keystone's
 * three faces are blockstate variants, and every particle is a vanilla {@code animateTick} on the block, so no block entity renderer and
 * no custom particle exists to register. Nothing here loads a client class on a dedicated server.
 */
public final class TrialClient {
    private TrialClient() {}

    public static void init(IEventBus modBus) {}
}
