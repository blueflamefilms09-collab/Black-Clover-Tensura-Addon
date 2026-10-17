package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the one promise the Hub's landmarks make to the rest of the mod is "a player can walk from the road to the gate". The stair and the ramp are built
 * to be independent of the noise and the Wave Function Collapse, but "built to be" is a claim; this is the check. It is a breadth-first search over what a
 * walking player can do, run on the same pure model the generator reads: a STANDING STATE is a column and a height of the feet in half blocks (so a bottom
 * slab is a state half a block above the full block beside it); a state is standable when the block under the feet is solid or a bottom slab and the two cells
 * of the body are passable; a move goes to a neighbouring column (no diagonals: the strictest reading) and may rise or fall by at most one block (a jump, or the
 * half-block steps of the ramp), needing the cells above the start free when it rises a whole block. Anything else (a railing, water, a dripstone point, a
 * gap) is an obstacle. The search is bounded by a box so that open plains cannot make it run forever. The same class backs the self-test
 * {@code macro.model} in the real game and the unit tests, including a negative control that cuts the stair and must make the search fail.
 * Pure: no Minecraft import.
 */
public final class Walkability {
    private Walkability() {}

    /** Where the blocks come from: fills {@code out[y - HubModel.MIN_Y]} for the heights y0 to y1 of one column (the search only asks for the rows of its box). */
    @FunctionalInterface
    public interface ColumnSource {
        void fill(int x, int z, BlockKind[] out, int y0, int y1);
    }

    /** The outcome of a search: whether the target was reached, how many states were visited, and the number of moves of the shortest route. */
    public record Result(boolean reached, int visited, int steps) {}

    /** The search box: columns x0..x1, z0..z1 and block rows y0..y1 (inclusive). */
    public record Box(int x0, int x1, int z0, int z1, int y0, int y1) {}

    /**
     * Searches for a walk from the start cell (where the feet are: x, y, z, the first standable state of that column at that height or half a block above) to the
     * target cell the same way.
     */
    public static Result reach(ColumnSource src, Box box, int sx, int sy, int sz, int tx, int ty, int tz) {
        Search s = new Search(src, box);
        return s.run(sx, sy, sz, tx, ty, tz);
    }

    /** "No standing state": feet heights can be negative (the Chasm lies below y 0), so the sentinel is not -1. */
    private static final int NONE = Integer.MIN_VALUE;

    private static final class Search {
        final ColumnSource src;
        final Box box;
        final int w, d, h2;
        final BlockKind[] buf = new BlockKind[HubModel.HEIGHT];
        final Map<Integer, BlockKind[]> columns = new HashMap<>();

        Search(ColumnSource src, Box box) {
            this.src = src;
            this.box = box;
            this.w = box.x1 - box.x0 + 1;
            this.d = box.z1 - box.z0 + 1;
            this.h2 = 2 * (box.y1 - box.y0 + 2);
        }

        BlockKind[] column(int x, int z) {
            int key = (x - box.x0) * d + (z - box.z0);
            BlockKind[] c = columns.get(key);
            if (c == null) {
                src.fill(x, z, buf, Math.max(HubModel.MIN_Y, box.y0 - 1), Math.min(HubModel.MAX_Y, box.y1 + 2));
                c = new BlockKind[box.y1 - box.y0 + 4];
                for (int y = box.y0 - 1; y <= box.y1 + 2; y++) {
                    int i = y - (box.y0 - 1);
                    c[i] = y < HubModel.MIN_Y ? BlockKind.BEDROCK : y > HubModel.MAX_Y ? BlockKind.AIR : buf[y - HubModel.MIN_Y];
                }
                columns.put(key, c);
            }
            return c;
        }

        BlockKind at(BlockKind[] col, int y) {
            int i = y - (box.y0 - 1);
            return i < 0 ? BlockKind.BEDROCK : i >= col.length ? BlockKind.AIR : col[i];
        }

        /** Is a standing state with feet at {@code f} half blocks valid in this column? */
        boolean standable(BlockKind[] col, int f) {
            int support = Math.floorDiv(f - 1, 2); // the block row whose top the feet rest on
            BlockKind k = at(col, support);
            boolean onFull = (f & 1) == 0 && k.solid;
            boolean onSlab = (f & 1) != 0 && k.slab;
            if (!onFull && !onSlab) return false;
            return at(col, support + 1).passable && at(col, support + 2).passable;
        }

        /** The feet heights (half blocks) a player can stand at in this column, lowest first. */
        int[] states(BlockKind[] col) {
            int[] tmp = new int[16];
            int n = 0;
            for (int y = box.y0; y <= box.y1; y++) {
                BlockKind k = at(col, y);
                if (k.solid && standable(col, 2 * (y + 1))) { if (n == tmp.length) tmp = java.util.Arrays.copyOf(tmp, n * 2); tmp[n++] = 2 * (y + 1); }
                else if (k.slab && standable(col, 2 * y + 1)) { if (n == tmp.length) tmp = java.util.Arrays.copyOf(tmp, n * 2); tmp[n++] = 2 * y + 1; }
            }
            return java.util.Arrays.copyOf(tmp, n);
        }

        long key(int x, int z, int f) { return ((long) (x - box.x0) * d + (z - box.z0)) * h2 + (f - 2 * box.y0); }

        /** The standing state of a column nearest to the wanted feet cell, or {@link #NONE}. */
        int pick(int x, int y, int z) {
            int best = NONE, bestDist = Integer.MAX_VALUE;
            for (int f : states(column(x, z))) {
                int dist = Math.abs(f - 2 * y);
                if (dist < bestDist) { bestDist = dist; best = f; }
            }
            return bestDist <= 1 ? best : NONE;
        }

        Result run(int sx, int sy, int sz, int tx, int ty, int tz) {
            int f0 = pick(sx, sy, sz), f1 = pick(tx, ty, tz);
            if (f0 == NONE || f1 == NONE) return new Result(false, 0, -1);
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[] {sx, sz, f0, 0});
            seen.add(key(sx, sz, f0));
            int[] dxs = {1, -1, 0, 0}, dzs = {0, 0, 1, -1};
            while (!queue.isEmpty()) {
                int[] cur = queue.poll();
                int x = cur[0], z = cur[1], f = cur[2];
                if (x == tx && z == tz && f == f1) return new Result(true, seen.size(), cur[3]);
                BlockKind[] colA = column(x, z);
                for (int i = 0; i < 4; i++) {
                    int nx = x + dxs[i], nz = z + dzs[i];
                    if (nx < box.x0 || nx > box.x1 || nz < box.z0 || nz > box.z1) continue;
                    BlockKind[] colB = column(nx, nz);
                    for (int fb : states(colB)) {
                        int delta = fb - f;
                        if (delta > 2 || delta < -2) continue;
                        if (delta == 2) {
                            // a whole block up is a jump: the three cells above the start must be free
                            int support = Math.floorDiv(f - 1, 2);
                            if (!at(colA, support + 3).passable) continue;
                        }
                        long kk = key(nx, nz, fb);
                        if (seen.add(kk)) queue.add(new int[] {nx, nz, fb, cur[3] + 1});
                    }
                }
            }
            return new Result(false, seen.size(), -1);
        }
    }
}
