package com.newuniverse.nusmp.fairytail.storm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Why: this is what keeps a storm from being a damage faucet. One storm cell keeps one of these: for every creature it has hurt, how many bolts, hail stones and rain or funnel
 * pulses have landed on it and when the last of each did. A new hit is allowed only if the kind's gap has passed and the kind's cap for this creature has not been reached
 * ({@link StormMath#MAX_BOLTS_PER_TARGET} and friends), so however many foes, however long the storm, one creature takes at most 3 bolts, 8 stones and 6 pulses from one storm. The book is
 * bounded (64 creatures; the oldest entry goes first) and pure, so the rule is unit tested. Not thread safe: it belongs to the server thread.
 */
public final class StormHits {
    public enum Kind {
        BOLT(StormMath.MAX_BOLTS_PER_TARGET, StormMath.BOLT_GAP), HAIL(StormMath.MAX_HAIL_PER_TARGET, StormMath.HAIL_GAP), PULSE(StormMath.MAX_PULSES_PER_TARGET, StormMath.PULSE_GAP);

        final int cap, gap;

        Kind(int cap, int gap) { this.cap = cap; this.gap = gap; }

        public int cap() { return cap; }

        public int gap() { return gap; }
    }

    public static final int MAX_ENTRIES = 64;

    private static final class Entry {
        final int[] count = new int[Kind.values().length];
        final long[] last = {Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2};
        long touched;
    }

    private final Map<Integer, Entry> book = new HashMap<>();

    /** Asks to land one hit of this kind on creature {@code id} at tick {@code now}; true (and recorded) when it is allowed. */
    public boolean tryHit(int id, Kind kind, long now) {
        Entry e = book.get(id);
        if (e == null) {
            if (book.size() >= MAX_ENTRIES) dropOldest();
            e = new Entry();
            book.put(id, e);
        }
        int k = kind.ordinal();
        if (e.count[k] >= kind.cap || now - e.last[k] < kind.gap) return false;
        e.count[k]++;
        e.last[k] = now;
        e.touched = now;
        return true;
    }

    /** How many hits of this kind have landed on the creature. */
    public int count(int id, Kind kind) {
        Entry e = book.get(id);
        return e == null ? 0 : e.count[kind.ordinal()];
    }

    public int size() { return book.size(); }

    private void dropOldest() {
        Iterator<Map.Entry<Integer, Entry>> it = book.entrySet().iterator();
        int worst = Integer.MIN_VALUE;
        long oldest = Long.MAX_VALUE;
        while (it.hasNext()) {
            Map.Entry<Integer, Entry> m = it.next();
            if (m.getValue().touched < oldest) {
                oldest = m.getValue().touched;
                worst = m.getKey();
            }
        }
        if (worst != Integer.MIN_VALUE) book.remove(worst);
    }
}
