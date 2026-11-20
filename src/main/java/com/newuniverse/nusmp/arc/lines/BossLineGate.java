package com.newuniverse.nusmp.arc.lines;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: keeps a boss from talking all the time (docs/boss_redesign_goku_sinbad.md, cost). Pure logic, no Minecraft type: per boss UUID it remembers when each group last spoke, a rotating counter per
 * group (so the same line is not said twice in a row) and when the boss last said anything, and answers {@link #allow}. At most {@link #MAX_BOSSES} bosses are tracked (the least recently used goes first)
 * and each holds at most one entry per group, so the memory is bounded; {@link #forget} drops a boss when it is removed. Cost: a few map lookups per line, no allocation per tick.
 */
public final class BossLineGate {
    /** Two lines of the same boss are at least this many ticks apart, unless the group is a priority one. */
    public static final long MIN_GAP = 40L;
    public static final int MAX_BOSSES = 16;

    private static final class State {
        final Map<String, Long> last = new HashMap<>();
        final Map<String, Integer> counter = new HashMap<>();
        long lastAny = Long.MIN_VALUE / 2;
    }

    private final Map<UUID, State> bosses = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, State> e) { return size() > MAX_BOSSES; }
    };

    /** Whether the boss may say a line of this group at {@code now}; when true it is recorded as said. */
    public synchronized boolean allow(UUID boss, String group, long now) {
        State s = bosses.computeIfAbsent(boss, k -> new State());
        long cd = BossLines.cooldown(group);
        Long last = s.last.get(group);
        if (last != null && cd > 0 && now >= last && now - last < cd) return false;
        if (!BossLines.priority(group) && now >= s.lastAny && now - s.lastAny < MIN_GAP) return false;
        s.last.put(group, now);
        s.lastAny = now;
        return true;
    }

    /** The next rotation number of a group for this boss (0, 1, 2, ...). */
    public synchronized int next(UUID boss, String group) {
        State s = bosses.computeIfAbsent(boss, k -> new State());
        int n = s.counter.getOrDefault(group, 0);
        s.counter.put(group, n + 1);
        return n;
    }

    public synchronized void forget(UUID boss) { bosses.remove(boss); }

    public synchronized void clear() { bosses.clear(); }

    public synchronized int tracked() { return bosses.size(); }
}
