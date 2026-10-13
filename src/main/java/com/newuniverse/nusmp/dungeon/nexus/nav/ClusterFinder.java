package com.newuniverse.nusmp.dungeon.nexus.nav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the Dungeon's life is thickest, for the Anti-Bird Compass (docs/living_dungeon.md section 3): given the biomass of the loaded
 * chunks round a holder, finds the nearest <b>localized biomass cluster</b>. A chunk's weight is its biomass above {@link #FLOOR} (the
 * 20 a chunk that was never simulated holds counts for nothing); its density is its own weight plus those of its eight neighbours, the
 * edge ones at 0.7 and the corners at 0.5. A cluster is a chunk that is the peak of its neighbourhood and dense enough
 * ({@link #MIN_DENSITY}); of the clusters at least half as dense as the densest, the nearest to the holder wins, and the compass points
 * at the weighted centre of the nine chunks round its peak. Pure.
 */
public final class ClusterFinder {
    private ClusterFinder() {}

    /** The biomass that counts for nothing, the least density a cluster has, and how dense (of the densest) a cluster must be to be worth the walk. */
    public static final float FLOOR = 20f, MIN_DENSITY = 30f;
    public static final double STRONG = 0.5;

    /** One loaded chunk's biomass. */
    public record Sample(int cx, int cz, float biomass) {}

    /** A cluster: the block position (x, z) of its weighted centre, its density, and the chunk at its peak. */
    public record Cluster(double x, double z, float density, int peakCx, int peakCz) {}

    private static double kernel(int dx, int dz) {
        int n = Math.abs(dx) + Math.abs(dz);
        return n == 0 ? 1.0 : n == 1 ? 0.7 : 0.5;
    }

    private static long key(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL); }

    /** The nearest strong cluster to the holder's chunk, or null if there is no cluster at all. */
    public static Cluster find(int holderCx, int holderCz, Collection<Sample> samples) {
        Map<Long, Float> weight = new HashMap<>();
        List<int[]> cells = new ArrayList<>();
        for (Sample s : samples) {
            float w = Float.isNaN(s.biomass()) ? 0f : Math.max(0f, s.biomass() - FLOOR);
            if (weight.put(key(s.cx(), s.cz()), w) == null) cells.add(new int[]{s.cx(), s.cz()});
        }
        Map<Long, Float> density = new HashMap<>();
        for (int[] c : cells) {
            float d = 0f;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    Float w = weight.get(key(c[0] + dx, c[1] + dz));
                    if (w != null) d += (float) (w * kernel(dx, dz));
                }
            density.put(key(c[0], c[1]), d);
        }
        List<int[]> peaks = new ArrayList<>();
        float best = 0f;
        for (int[] c : cells) {
            float d = density.get(key(c[0], c[1]));
            if (weight.get(key(c[0], c[1])) <= 0f || d < MIN_DENSITY) continue;
            boolean peak = true;
            for (int dx = -1; dx <= 1 && peak; dx++)
                for (int dz = -1; dz <= 1 && peak; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Float o = density.get(key(c[0] + dx, c[1] + dz));
                    if (o == null) continue;
                    if (o > d || (o == d && (c[0] + dx < c[0] || (c[0] + dx == c[0] && c[1] + dz < c[1])))) peak = false;   // a denser neighbour wins; of equals the lowest (x, z) does
                }
            if (!peak) continue;
            peaks.add(c);
            best = Math.max(best, d);
        }
        int[] pick = null;
        long pickDist = Long.MAX_VALUE;
        float pickDensity = 0f;
        for (int[] c : peaks) {
            float d = density.get(key(c[0], c[1]));
            if (d < best * STRONG) continue;
            long dx = c[0] - holderCx, dz = c[1] - holderCz, dist = dx * dx + dz * dz;
            if (pick == null || dist < pickDist || (dist == pickDist && (d > pickDensity || (d == pickDensity && (c[0] < pick[0] || (c[0] == pick[0] && c[1] < pick[1])))))) {
                pick = c;
                pickDist = dist;
                pickDensity = d;
            }
        }
        if (pick == null) return null;
        double sx = 0, sz = 0, sw = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                Float w = weight.get(key(pick[0] + dx, pick[1] + dz));
                if (w == null || w <= 0f) continue;
                double m = w * kernel(dx, dz);
                sx += m * ((pick[0] + dx) * 16 + 8);
                sz += m * ((pick[1] + dz) * 16 + 8);
                sw += m;
            }
        if (sw <= 0) return null;
        return new Cluster(sx / sw, sz / sw, pickDensity, pick[0], pick[1]);
    }
}
