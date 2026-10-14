package com.newuniverse.nusmp.relics.djinn.weapon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Marked foes (Marchosias' Brand): a marked target takes {@code pct} more damage until a game time. At most {@link #CAP} marks exist at once (the
 * one that expires soonest is dropped to make room), so a crowd never makes the table grow (pure).
 */
public final class WeaponMarks {
    public static final int CAP = 64;

    private record Mark(long until, double pct) {}

    private final Map<UUID, Mark> marks = new HashMap<>();

    public int size() { return marks.size(); }

    /** Marks (or renews, never weakening a stronger mark that is still running) a target. */
    public void mark(UUID target, long now, int ticks, double pct) {
        if (ticks <= 0 || pct <= 0) return;
        prune(now);
        Mark old = marks.get(target);
        double p = Math.max(0.0, Math.min(1.0, pct));
        if (old != null && old.until > now) p = Math.max(p, old.pct);
        if (old == null && marks.size() >= CAP) dropSoonest();
        marks.put(target, new Mark(now + ticks, p));
    }

    /** The extra share of damage this target takes now (0 when unmarked or expired). */
    public double bonus(UUID target, long now) {
        Mark m = marks.get(target);
        if (m == null) return 0.0;
        if (m.until <= now) {
            marks.remove(target);
            return 0.0;
        }
        return m.pct;
    }

    public void prune(long now) {
        Iterator<Mark> it = marks.values().iterator();
        while (it.hasNext()) if (it.next().until <= now) it.remove();
    }

    public void clear() { marks.clear(); }

    private void dropSoonest() {
        UUID worst = null;
        long best = Long.MAX_VALUE;
        for (Map.Entry<UUID, Mark> e : marks.entrySet()) {
            if (e.getValue().until < best) {
                best = e.getValue().until;
                worst = e.getKey();
            }
        }
        if (worst != null) marks.remove(worst);
    }
}
