package com.newuniverse.nusmp.labyrinth.mega.decennial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the Dungeon Guard holds every block of the dungeon and a hall's interactive elements are the one explicit exception, so BOTH sides must be able to ask "may this block be broken?": the
 * server to let the break through, and the client to start the crack animation at all (a client that predicts a refusal never starts one, see {@code DungeonGuard.predictsClient}). This holder is
 * side-neutral on purpose (no client class is loaded on a dedicated server): the server keeps the cells of the halls a player is near, and each client keeps the last list the server sent it
 * ({@link DecennialNet}). The two sets are separate so that a single-player world, where both sides share one JVM, never lets the client's copy overwrite the server's.
 */
public final class BreakableCells {
    private BreakableCells() {}

    private static final Set<Long> SERVER = ConcurrentHashMap.newKeySet();
    private static volatile Set<Long> client = Set.of();

    /** May this block be broken on the side of {@code level}? */
    public static boolean breakable(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        return level.isClientSide() ? client.contains(pos.asLong()) : SERVER.contains(pos.asLong());
    }

    // ---------------------------------------------------------------- server
    public static void serverAdd(long cell) { SERVER.add(cell); }

    public static void serverRemove(long cell) { SERVER.remove(cell); }

    public static int serverCount() { return SERVER.size(); }

    public static boolean serverHas(long cell) { return SERVER.contains(cell); }

    public static void serverClear() { SERVER.clear(); }

    // ---------------------------------------------------------------- client
    /** Replaces the client's list with what the server sent. */
    public static void clientSet(long[] cells) {
        Set<Long> s = ConcurrentHashMap.newKeySet();
        for (long c : cells) s.add(c);
        client = Collections.unmodifiableSet(s);
    }

    public static int clientCount() { return client.size(); }
}
