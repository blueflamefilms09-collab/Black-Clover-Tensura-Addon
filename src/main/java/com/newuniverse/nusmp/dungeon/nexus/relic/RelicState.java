package com.newuniverse.nusmp.dungeon.nexus.relic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What the relics keep in memory about a player for a while: the Chalice's immunity, the Mantle's Pheromone Mark, whether the Mantle was
 * worn a second ago, and when each message was last shown. Server thread only. All of it is dropped on death, logout and a change of
 * dimension, so no temporary effect of a relic outlives its wearer's trip.
 */
final class RelicState {
    private RelicState() {}

    /** Game time the Chalice's atmospheric immunity ends. */
    static final Map<UUID, Long> IMMUNE = new HashMap<>();
    /** Game time the Pheromone Mark ends. */
    static final Map<UUID, Long> MARK = new HashMap<>();
    /** Was the Mantle worn at the last look? */
    static final Map<UUID, Boolean> MANTLE = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> NOTES = new HashMap<>();

    /** True (and starts the interval again) when {@code key} has not been shown to this player for {@code every} ticks. */
    static boolean due(UUID id, String key, long now, int every) {
        Map<String, Long> m = NOTES.computeIfAbsent(id, k -> new HashMap<>());
        Long last = m.get(key);
        if (last != null && now >= last && now - last < every) return false;
        m.put(key, now);
        return true;
    }

    static boolean immune(UUID id, long now) {
        Long u = IMMUNE.get(id);
        return u != null && DigestTable.active(now, u);
    }

    static boolean marked(UUID id, long now) {
        Long u = MARK.get(id);
        return u != null && Pheromone.active(now, u);
    }

    static void clear(UUID id) {
        IMMUNE.remove(id);
        MARK.remove(id);
        MANTLE.remove(id);
        NOTES.remove(id);
    }

    static void clearAll() {
        IMMUNE.clear();
        MARK.clear();
        MANTLE.clear();
        NOTES.clear();
    }
}
