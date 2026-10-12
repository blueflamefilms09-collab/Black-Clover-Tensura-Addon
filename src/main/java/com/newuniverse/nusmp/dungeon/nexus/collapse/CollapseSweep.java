package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order in which the columns of a collapsing Nexus come down: every column of the footprint once, the farthest from the exits first
 * and the nearest last, so the ruin falls from the far side toward the way out and not at random. The columns within the corridor radius
 * of an exit come after all the others ("spared"): the rolling fall never reaches them, only the final wipe does. Pure and deterministic:
 * the same footprint and exits always give the same order, which is what lets a restart carry on from a saved cursor.
 */
public final class CollapseSweep {
    private final int[] xs, zs;
    private final int open;
    /** The index of the column at (minX + i, minZ + j) is grid[i * depth + j], or -1 where the footprint has none. */
    private final int minX, minZ, width, depth;
    private final int[] grid;

    private CollapseSweep(int[] xs, int[] zs, int open) {
        this.xs = xs;
        this.zs = zs;
        this.open = open;
        int loX = Integer.MAX_VALUE, hiX = Integer.MIN_VALUE, loZ = Integer.MAX_VALUE, hiZ = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            loX = Math.min(loX, xs[i]);
            hiX = Math.max(hiX, xs[i]);
            loZ = Math.min(loZ, zs[i]);
            hiZ = Math.max(hiZ, zs[i]);
        }
        this.minX = xs.length == 0 ? 0 : loX;
        this.minZ = xs.length == 0 ? 0 : loZ;
        this.width = xs.length == 0 ? 0 : hiX - loX + 1;
        this.depth = xs.length == 0 ? 0 : hiZ - loZ + 1;
        this.grid = new int[width * depth];
        java.util.Arrays.fill(grid, -1);
        for (int i = 0; i < xs.length; i++) grid[(xs[i] - minX) * depth + (zs[i] - minZ)] = i;
    }

    /**
     * @param fp       the ground that collapses
     * @param margin   blocks round the footprint that are swept too (the edge counts as inside)
     * @param exitXs   the exits' x and z (equal lengths); with none, the middle of the footprint stands in for the exit
     * @param radius   columns whose flat distance to an exit is at most this are spared
     */
    public static CollapseSweep build(Footprint fp, int margin, int[] exitXs, int[] exitZs, int radius) {
        int n = Math.min(exitXs == null ? 0 : exitXs.length, exitZs == null ? 0 : exitZs.length);
        int[] ex = new int[Math.max(1, n)], ez = new int[Math.max(1, n)];
        if (n == 0) {
            ex[0] = fp.centreX();
            ez[0] = fp.centreZ();
        } else {
            System.arraycopy(exitXs, 0, ex, 0, n);
            System.arraycopy(exitZs, 0, ez, 0, n);
        }
        long r2 = (long) radius * radius;
        List<long[]> far = new ArrayList<>(), near = new ArrayList<>();                       // {distance squared, x, z}
        for (int x = fp.minX() - margin; x <= fp.maxX() + margin; x++) {
            for (int z = fp.minZ() - margin; z <= fp.maxZ() + margin; z++) {
                if (!fp.contains(x, z, margin)) continue;
                long best = Long.MAX_VALUE;
                for (int i = 0; i < ex.length; i++) {
                    long dx = (long) x - ex[i], dz = (long) z - ez[i];
                    best = Math.min(best, dx * dx + dz * dz);
                }
                (best <= r2 && n > 0 ? near : far).add(new long[]{best, x, z});
            }
        }
        Comparator<long[]> farthestFirst = (a, b) -> a[0] != b[0] ? Long.compare(b[0], a[0]) : a[1] != b[1] ? Long.compare(a[1], b[1]) : Long.compare(a[2], b[2]);
        far.sort(farthestFirst);
        near.sort(farthestFirst);
        int total = far.size() + near.size();
        int[] xs = new int[total], zs = new int[total];
        int i = 0;
        for (long[] c : far) {
            xs[i] = (int) c[1];
            zs[i++] = (int) c[2];
        }
        for (long[] c : near) {
            xs[i] = (int) c[1];
            zs[i++] = (int) c[2];
        }
        return new CollapseSweep(xs, zs, far.size());
    }

    /** All columns, swept and spared. */
    public int size() { return xs.length; }

    /** How many columns the rolling fall may sweep: the first `open` of the order; the rest are the corridors. */
    public int open() { return open; }

    public int x(int i) { return xs[i]; }

    public int z(int i) { return zs[i]; }

    public boolean spared(int i) { return i >= open; }

    /** Where column (x, z) stands in the order, or -1 if the footprint has no such column. */
    public int indexOf(int x, int z) {
        int i = x - minX, j = z - minZ;
        if (i < 0 || j < 0 || i >= width || j >= depth) return -1;
        return grid[i * depth + j];
    }
}
