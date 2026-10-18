package com.newuniverse.nusmp.skilltree.shop;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the shop's client packets are cheap to send and each one can change gold and items, so a modified client could fire hundreds per second (a duplication attempt, or just
 * lag). The rule is one shop action per player per server tick: the packet handler asks {@link #allow} first and silently drops a second packet in the same tick, which no human can
 * produce and any honest click does not need. The only state is the tick number of each player's last action, kept in a map that is emptied when a player leaves and when the server
 * stops, so it cannot grow without bound or leak into the next world. Server thread only (the handlers run on the main thread).
 */
public final class ShopGate {
    private ShopGate() {}

    private static final Map<UUID, Integer> LAST = new HashMap<>();

    /** True when this player has not acted in the current server tick; records the tick. */
    public static boolean allow(ServerPlayer p) {
        return allow(p.getUUID(), p.server.getTickCount());
    }

    /** The same rule on plain values (and the part the tests drive). */
    static boolean allow(UUID player, int tick) {
        Integer last = LAST.put(player, tick);
        return last == null || last != tick;
    }

    /** Forgets a player (logout). */
    public static void forget(UUID player) {
        LAST.remove(player);
    }

    /** Forgets everybody (server stopped). */
    public static void clear() {
        LAST.clear();
    }

    /** How many players are tracked (for the self-test). */
    static int tracked() { return LAST.size(); }
}
