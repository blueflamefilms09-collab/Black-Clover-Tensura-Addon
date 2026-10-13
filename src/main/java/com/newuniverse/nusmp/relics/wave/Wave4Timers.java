package com.newuniverse.nusmp.relics.wave;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 4: a small bounded table of "this entity is marked until game tick X" (the Weakening Gaze mark, a charmed mob, a charm
 * immunity, the Truesight window). Holds ids only, never entities; expired rows are dropped as they are met and when a row is
 * added, and a full table gives up the row that ends soonest, so it cannot grow. Pure Java.
 */
final class Wave4Timers {
    private final Map<UUID, Long> until = new HashMap<>();
    private final int cap;

    Wave4Timers(int cap) { this.cap = Math.max(1, cap); }

    /** Marks {@code id} until {@code untilTick}; a longer mark already there is kept. */
    void put(UUID id, long untilTick, long now) {
        if (id == null || untilTick <= now) return;
        purge(now);
        Long old = until.get(id);
        if (old != null) {
            if (untilTick > old) until.put(id, untilTick);
            return;
        }
        if (until.size() >= cap) dropSoonest();
        until.put(id, untilTick);
    }

    boolean active(UUID id, long now) {
        Long u = until.get(id);
        if (u == null) return false;
        if (u <= now) { until.remove(id); return false; }
        return true;
    }

    /** The tick the mark ends on, 0 when there is none. */
    long untilOf(UUID id) { Long u = until.get(id); return u == null ? 0L : u; }

    void remove(UUID id) { until.remove(id); }

    void clear() { until.clear(); }

    int size() { return until.size(); }

    /** A copy of the ids, for a bounded sweep. */
    java.util.List<UUID> ids() { return new java.util.ArrayList<>(until.keySet()); }

    private void purge(long now) {
        for (Iterator<Map.Entry<UUID, Long>> it = until.entrySet().iterator(); it.hasNext(); ) if (it.next().getValue() <= now) it.remove();
    }

    private void dropSoonest() {
        UUID worst = null;
        long at = Long.MAX_VALUE;
        for (Map.Entry<UUID, Long> e : until.entrySet()) if (e.getValue() < at) { at = e.getValue(); worst = e.getKey(); }
        if (worst != null) until.remove(worst);
    }
}
