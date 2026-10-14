package com.newuniverse.nusmp.client.merge;

import com.newuniverse.nusmp.core.magic.merge.MergedRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 1.27.0: the client half of the merge generator. The definitions arrive in the sync packet (core.magic.merge.MergedSync) and are
 * read from MergedRegistry; this only keeps the baked meshes in step: they are rebuilt when the models reload and dropped, with the
 * definitions, when the player leaves a world.
 */
public final class MergedClient {
    private MergedClient() {}

    /** Call once on the client with the mod bus (the NUSMP client block). */
    public static void init(IEventBus modBus) {
        modBus.addListener((ModelEvent.BakingCompleted e) -> MergedMeshCache.clear());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { MergedRegistry.clear(); MergedMeshCache.clear(); });
    }
}
