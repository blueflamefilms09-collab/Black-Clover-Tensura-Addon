package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the ways out of a collapsing Nexus are: the spots the sweep is ordered from, the Root Gates stand at and the mutated guards come
 * to. A spot is {x, y, z}, the block the feet stand in. A standing Nexus gives its own entrances; a pocket (a collapse outside a Nexus)
 * tries a ring round its centre. Pure: the world only decides whether a candidate can be stood on.
 */
public final class CollapseExits {
    private CollapseExits() {}

    /** At most this many exits, and none closer than MIN_GAP blocks (flat) to another: a double doorway is one way out. */
    public static final int MAX = 8, MIN_GAP = 6;

    /**
     * The exits of a standing Nexus, from the spots its plan names: the entrances; with none, the arrival spots; with none, the rescue
     * point; with none of those, the middle of the footprint's floor. Duplicates and near-duplicates are dropped, at most MAX are kept,
     * and the answer is never empty.
     */
    public static List<int[]> fromPlan(List<int[]> entrances, List<int[]> arrivals, int[] rescue, Footprint fp, int floorY) {
        List<int[]> out = new ArrayList<>();
        take(out, entrances);
        if (out.isEmpty()) take(out, arrivals);
        if (out.isEmpty() && rescue != null && rescue.length >= 3) take(out, List.<int[]>of(rescue));
        if (out.isEmpty()) out.add(new int[]{fp.centreX(), floorY + 1, fp.centreZ()});
        return out;
    }

    private static void take(List<int[]> out, List<int[]> spots) {
        if (spots == null) return;
        for (int[] s : spots) {
            if (out.size() >= MAX) return;
            if (s != null && s.length >= 3 && farEnough(out, s[0], s[2], MIN_GAP)) out.add(new int[]{s[0], s[1], s[2]});
        }
    }

    /** Is (x, z) at least `gap` blocks (flat) from every spot already taken? */
    public static boolean farEnough(List<int[]> taken, int x, int z, int gap) {
        long g2 = (long) gap * gap;
        for (int[] t : taken) {
            long dx = Math.abs((long) t[0] - x), dz = Math.abs((long) t[2] - z);
            if (dx < gap && dz < gap && dx * dx + dz * dz < g2) return false;
        }
        return true;
    }

    /**
     * Candidate columns {x, z} on a ring of the given radius round (cx, cz), `count` of them evenly spread, the first at a compass step
     * chosen by `phase` (a number of any size: the same collapse always gives the same ring).
     */
    public static List<int[]> ring(int cx, int cz, int radius, int count, int phase) {
        List<int[]> out = new ArrayList<>();
        if (count <= 0 || radius <= 0) return out;
        double start = Math.floorMod(phase, count) * (2.0 * Math.PI / count) * 0.5;
        for (int i = 0; i < count; i++) {
            double a = start + i * (2.0 * Math.PI / count);
            out.add(new int[]{cx + (int) Math.round(Math.cos(a) * radius), cz + (int) Math.round(Math.sin(a) * radius)});
        }
        return out;
    }

    /** The index of the spot nearest to (x, z) in the flat plane, or -1 if there are none. */
    public static int nearest(List<int[]> spots, double x, double z) {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < spots.size(); i++) {
            int[] s = spots.get(i);
            double dx = s[0] + 0.5 - x, dz = s[2] + 0.5 - z, d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
