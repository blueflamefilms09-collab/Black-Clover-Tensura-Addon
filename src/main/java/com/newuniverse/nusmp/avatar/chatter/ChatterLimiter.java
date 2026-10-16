package com.newuniverse.nusmp.avatar.chatter;

import com.newuniverse.nusmp.avatar.ChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a swarm of Avatars (or one in a long fight) must not flood chat. Two limits, both pure: a per-Avatar per-event cooldown, and a global budget
 * of lines per server tick. Time is a plain tick counter so the tests need no server.
 */
public final class ChatterLimiter {
    private final Map<Key, Long> next = new HashMap<>();
    private long budgetTick = Long.MIN_VALUE;
    private int budgetLeft;
    private long lastPrune;

    private record Key(UUID avatar, ChatEvent event) {}

    /**
     * Asks to speak. True (and the cooldown starts, one budget slot is used) when this Avatar's cooldown for the event is over and the tick still has
     * budget; false otherwise (nothing is consumed).
     */
    public boolean tryAcquire(UUID avatar, ChatEvent event, long tick, int cooldownTicks, int budgetPerTick) {
        if (tick != budgetTick) { budgetTick = tick; budgetLeft = budgetPerTick; }
        if (budgetLeft <= 0) return false;
        Key k = new Key(avatar, event);
        Long until = next.get(k);
        if (until != null && tick < until) return false;
        budgetLeft--;
        next.put(k, tick + Math.max(0, cooldownTicks));
        if (tick - lastPrune > 6000) prune(tick);
        return true;
    }

    /** Drops cooldowns that are over, so the map cannot grow without bound over a long session. */
    void prune(long tick) {
        lastPrune = tick;
        next.values().removeIf(t -> t <= tick);
    }

    public void forget(UUID avatar) { next.keySet().removeIf(k -> k.avatar().equals(avatar)); }

    public int size() { return next.size(); }
}
