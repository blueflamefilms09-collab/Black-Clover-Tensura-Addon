package com.newuniverse.nusmp.compat.create;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: a Create trap is only a trap where it cannot be walked around, so it is placed in a CHOKE: a stretch of corridor exactly three cells wide with solid wall at least two
 * cells deep on both sides. This finds such a stretch in a grid of columns, and nothing else about the world: {@link Grid#open} says whether a column is a walkable lane
 * cell (solid floor, free headroom), {@link Grid#wall} whether it is solid wall that the machinery may replace. A corridor of the dungeon is three wide with walls three
 * thick, so its straight stretches qualify and a side doorway, a junction or a hall does not (the wall test fails beside it), which is exactly the keep-out the traps need.
 * Pure (no Minecraft types).
 */
public final class LaneFinder {
    private LaneFinder() {}

    public interface Grid {
        /** Is the column a lane cell: solid floor under it and free headroom above it? */
        boolean open(int x, int z);

        /** Is the column solid wall the machinery may replace (floor row up to knee height)? */
        boolean wall(int x, int z);
    }

    /**
     * A chosen stretch. {@code originX / originZ} is the cell of local (0, 0, 0): the lane's centre line at the start of the strip in the direction of travel;
     * {@code forward} is the direction of travel as an index (0 north = -z, 1 east = +x, 2 south = +z, 3 west = -x); {@code runLength} is the whole free stretch it was cut from.
     */
    public record Site(int originX, int originZ, int forward, int length, int runLength) {}

    private record Run(boolean alongZ, int fixed, int start, int len) {}

    /**
     * The stretches of the region that can hold a strip of {@code length} cells, best first, as candidate {@link Site}s (empty when there is none). {@code minX..maxX},
     * {@code minZ..maxZ} is the region to look in (a room's box). Longer runs come first (a strip is cut from the middle of its run so the ends keep clear), then lower
     * coordinates; {@code flip} turns the direction of travel round (so a seed can choose which end the trap is entered from).
     */
    public static List<Site> find(Grid g, int minX, int maxX, int minZ, int maxZ, int length, boolean flip) {
        List<Run> runs = new ArrayList<>();
        // lanes running along z: the centre line is a fixed x
        for (int x = minX + 4; x <= maxX - 4; x++) {
            int start = Integer.MIN_VALUE;
            for (int z = minZ; z <= maxZ + 1; z++) {
                boolean ok = z <= maxZ && valid(g, x, z, true);
                if (ok && start == Integer.MIN_VALUE) start = z;
                if (!ok && start != Integer.MIN_VALUE) {
                    if (z - start >= length) runs.add(new Run(true, x, start, z - start));
                    start = Integer.MIN_VALUE;
                }
            }
        }
        // lanes running along x: the centre line is a fixed z
        for (int z = minZ + 4; z <= maxZ - 4; z++) {
            int start = Integer.MIN_VALUE;
            for (int x = minX; x <= maxX + 1; x++) {
                boolean ok = x <= maxX && valid(g, z, x, false);
                if (ok && start == Integer.MIN_VALUE) start = x;
                if (!ok && start != Integer.MIN_VALUE) {
                    if (x - start >= length) runs.add(new Run(false, z, start, x - start));
                    start = Integer.MIN_VALUE;
                }
            }
        }
        runs.sort(Comparator.comparingInt((Run r) -> -r.len()).thenComparingInt(Run::fixed).thenComparingInt(Run::start).thenComparing(r -> r.alongZ()));
        List<Site> out = new ArrayList<>();
        for (Run r : runs) {
            int first = r.start() + (r.len() - length) / 2;                     // the strip sits in the middle of its run
            if (r.alongZ()) {
                out.add(flip ? new Site(r.fixed(), first + length - 1, 0, length, r.len()) : new Site(r.fixed(), first, 2, length, r.len()));
            } else {
                out.add(flip ? new Site(first + length - 1, r.fixed(), 3, length, r.len()) : new Site(first, r.fixed(), 1, length, r.len()));
            }
        }
        return out;
    }

    /** The centre line (c, t) is valid when the three lane columns are open and the three wall columns on each side (two used by the machinery, the outer shell as margin) are wall. */
    private static boolean valid(Grid g, int c, int t, boolean alongZ) {
        for (int d = -1; d <= 1; d++) if (!(alongZ ? g.open(c + d, t) : g.open(t, c + d))) return false;
        for (int d = 2; d <= 2 + TrapRules.WALL_DEPTH; d++) {
            if (!(alongZ ? g.wall(c - d, t) && g.wall(c + d, t) : g.wall(t, c - d) && g.wall(t, c + d))) return false;
        }
        return true;
    }

    /** The world offset of a local cell (dx right, dz forward) for a forward index (0 north, 1 east, 2 south, 3 west): {x, z}. Right is the clockwise neighbour of forward. */
    public static int[] toWorld(int originX, int originZ, int forward, int dx, int dz) {
        int fx = fx(forward), fz = fz(forward);
        int rx = -fz, rz = fx;
        return new int[] {originX + rx * dx + fx * dz, originZ + rz * dx + fz * dz};
    }

    public static int fx(int forward) { return switch (Math.floorMod(forward, 4)) { case 1 -> 1; case 3 -> -1; default -> 0; }; }

    public static int fz(int forward) { return switch (Math.floorMod(forward, 4)) { case 0 -> -1; case 2 -> 1; default -> 0; }; }
}
