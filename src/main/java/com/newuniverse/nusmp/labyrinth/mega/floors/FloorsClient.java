package com.newuniverse.nusmp.labyrinth.mega.floors;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: {@link ClientAlignment} is filled by the {@code nusmp:alignment_sync} packet, and a client that leaves one server and joins another that does not
 * run this module would otherwise keep showing the first server's lock (no packet arrives to replace it). This is the one client-only hook of the module,
 * the same shape as the discovery system's: it wipes the holder when the player leaves a world, so the next one starts "not synced" and every road reads as
 * free. It is OPTIONAL (the holder is also overwritten by the first packet of every login on a server that has the module) and CLIENT ONLY: it names a
 * client event, so the lead calls {@link #init} from the mod constructor's {@code Dist.CLIENT} block, next to the other client initialisers, and nothing
 * on a dedicated server ever loads this class.
 */
public final class FloorsClient {
    private FloorsClient() {}

    private static boolean done;

    public static synchronized void init() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientAlignment.clear());
    }
}
