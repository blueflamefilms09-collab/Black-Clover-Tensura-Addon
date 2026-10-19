package com.newuniverse.nusmp.relics.djinn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Blows in a row, per bearer (pure): Bloodlust grows with each blow that comes soon after the last, Drumbeat lands on every third. The table is
 * bounded ({@link #MAX} bearers; a full table is pruned of the stale, and cleared if all are fresh), forgotten on logout and when the server stops,
 * and a game clock that went backwards simply starts a new streak.
 */
public final class BlowStreak {
    public static final int MAX = 128;

    private final Map<UUID, long[]> table = new HashMap<>();             // {tick of the last blow, blows so far in the streak}

    public int size() { return table.size(); }

    /** Registers a blow at game time {@code now}; returns how many blows in a row came before it (0 for the first of a streak). */
    public int hit(UUID id, long now) {
        long[] e = table.get(id);
        int before = 0;
        if (e != null && now >= e[0] && now - e[0] <= TraitRules.STREAK_WINDOW) before = (int) Math.min(1000, e[1]);
        if (e == null) {
            if (table.size() >= MAX) prune(now);
            e = new long[2];
            table.put(id, e);
        }
        e[0] = now;
        e[1] = before + 1;
        return before;
    }

    /** Drops every streak that has run out. If the table is still full the oldest half goes. */
    public void prune(long now) {
        Iterator<long[]> it = table.values().iterator();
        while (it.hasNext()) {
            long[] e = it.next();
            if (now < e[0] || now - e[0] > TraitRules.STREAK_WINDOW) it.remove();
        }
        if (table.size() >= MAX) table.clear();
    }

    public void forget(UUID id) { table.remove(id); }

    public void clear() { table.clear(); }
}
