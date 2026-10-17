package com.newuniverse.nusmp.arc.sds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: Echo of Memory is Gowther's Rewrite Light turned into a ground attack: he remembers where each foe stood a second, two seconds, three seconds ago and strikes
 * those places. The answer is the one every fight teaches: do not stand still. To do it he needs a short memory of where each foe has been, which is this class: a few
 * samples per entity (one every few ticks, as many as the trail's length needs), the position at "so many ticks ago" taken from the nearest sample at or before that
 * moment, and nothing kept for an entity that was not seen for a while. Pure: positions are numbers and an entity is an int.
 */
public final class EchoTrail {
    private final int length;
    private final Map<Integer, ArrayDeque<double[]>> trails = new HashMap<>();

    /** A trail that remembers {@code lengthTicks} ticks (at least 1). */
    public EchoTrail(int lengthTicks) { this.length = Math.max(1, lengthTicks); }

    /** Notes where {@code id} stands at {@code now} (callers sample every few ticks; a repeat within the same tick replaces the last sample). */
    public void record(int id, long now, double x, double y, double z) {
        ArrayDeque<double[]> t = trails.computeIfAbsent(id, k -> new ArrayDeque<>());
        if (!t.isEmpty() && (long) t.peekLast()[0] == now) t.pollLast();
        t.addLast(new double[]{now, x, y, z});
        while (!t.isEmpty() && now - (long) t.peekFirst()[0] > length) t.pollFirst();
    }

    /** Where {@code id} stood {@code age} ticks before {@code now} ({x, y, z}), or null when the trail does not reach back that far. */
    public double[] at(int id, long now, int age) {
        ArrayDeque<double[]> t = trails.get(id);
        if (t == null || t.isEmpty()) return null;
        long want = now - Math.max(0, age);
        double[] best = null;
        for (double[] s : t) {
            if ((long) s[0] <= want) best = s;
            else break;
        }
        return best == null ? null : new double[]{best[1], best[2], best[3]};
    }

    /** The echoes of {@code id}: the positions at each of {@code ages}, skipping the ages the trail does not reach. */
    public List<double[]> echoes(int id, long now, int[] ages) {
        List<double[]> out = new ArrayList<>();
        for (int age : ages) {
            double[] p = at(id, now, age);
            if (p != null) out.add(p);
        }
        return out;
    }

    /** Forgets everyone who has not been seen since {@code now - length} (so an entity that left costs nothing). */
    public void prune(long now) {
        trails.values().removeIf(t -> t.isEmpty() || now - (long) t.peekLast()[0] > length);
    }

    public void forget(int id) { trails.remove(id); }

    public void clear() { trails.clear(); }

    public int tracked() { return trails.size(); }
}
