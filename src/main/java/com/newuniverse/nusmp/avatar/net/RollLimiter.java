package com.newuniverse.nusmp.avatar.net;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the dice button is the one thing a client can spam on the creation screen, and every roll polls the creator's luck, so the server lets one
 * player roll once per gap (default half a second) and ignores the rest silently. Time is passed in, so the rule is tested without a clock.
 */
public final class RollLimiter {
    private final Map<UUID, Long> last = new HashMap<>();

    /** True (and remembers the time) when this player may act now. */
    public synchronized boolean allow(UUID player, long nowMillis, long gapMillis) {
        Long prev = last.get(player);
        if (prev != null && nowMillis - prev < gapMillis && nowMillis >= prev) return false;
        last.put(player, nowMillis);
        return true;
    }

    public synchronized void forget(UUID player) { last.remove(player); }

    public synchronized void clear() { last.clear(); }
}
