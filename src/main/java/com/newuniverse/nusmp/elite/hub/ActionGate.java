package com.newuniverse.nusmp.elite.hub;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a client can send a request packet as fast as it likes, and a purchase or a hand-in is an action with consequences (coins move), so at most ONE request per player per tick is let through;
 * the rest are dropped. This is the same guard the Black Market uses. It keeps the last tick each player acted on, per server run (cleared on stop and on logout).
 */
final class ActionGate {
    private ActionGate() {}

    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** True when this is the first request of the player in this game tick. */
    static synchronized boolean allow(ServerPlayer p) {
        long now = p.serverLevel().getGameTime();
        Long last = LAST.put(p.getUUID(), now);
        return last == null || last != now;
    }

    static synchronized void forget(UUID id) { LAST.remove(id); }

    static synchronized void clear() { LAST.clear(); }
}
