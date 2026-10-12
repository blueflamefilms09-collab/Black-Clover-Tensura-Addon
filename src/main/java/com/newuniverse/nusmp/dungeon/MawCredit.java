package com.newuniverse.nusmp.dungeon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who has been cutting the roots of which open Maw ({@link MawReward}): a small in-memory table keyed by the Maw, every credit with an
 * end time so that a Maw closed some other way (its time ran out, an admin) cannot leave entries behind. Pure.
 */
public final class MawCredit {
    private final Map<String, Map<UUID, Long>> credits = new HashMap<>();

    /** {@code who} cut a root of the Maw named {@code maw} at game time {@code now}; the credit holds until {@code until}. */
    public void record(String maw, UUID who, long now, long until) {
        sweep(now);
        credits.computeIfAbsent(maw, k -> new HashMap<>()).merge(who, until, Long::max);
    }

    /** Everyone credited for this Maw whose credit is still alive at {@code now}, in no promised order. The Maw is forgotten. */
    public Set<UUID> take(String maw, long now) {
        Set<UUID> out = new LinkedHashSet<>();
        Map<UUID, Long> mine = credits.remove(maw);
        if (mine == null) return out;
        for (Map.Entry<UUID, Long> e : mine.entrySet()) if (e.getValue() > now) out.add(e.getKey());
        return out;
    }

    private void sweep(long now) {
        for (Iterator<Map<UUID, Long>> it = credits.values().iterator(); it.hasNext(); ) {
            Map<UUID, Long> mine = it.next();
            mine.values().removeIf(until -> until <= now);
            if (mine.isEmpty()) it.remove();
        }
    }

    /** How many Maws are remembered (for tests). */
    public int mawCount() { return credits.size(); }

    public void clear() { credits.clear(); }
}
