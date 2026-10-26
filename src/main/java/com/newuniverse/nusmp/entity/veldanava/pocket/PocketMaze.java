package com.newuniverse.nusmp.entity.veldanava.pocket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

/**
 * Why: the pocket dimension of Dimensional Labyrinth (Israfil) is a maze whose walls keep moving, and the rule that makes it fair is simple and testable on its own: the maze is always a
 * perfect maze (every cell reachable from every other by exactly one way), and a shift opens one closed wall and closes one wall on the loop that opening made, so it is NEVER cut in two and
 * never gains a second way round. This class holds only that: the grid, the walls, the shift and the distances. No game classes, no world, no clock (a unit test runs it as it is).
 *
 * <p>Geometry (blocks, relative to the maze's corner): a cell is {@link #PITCH} blocks wide with a wall line every {@link #PITCH} blocks, so a corridor is {@code PITCH - 1} = 3 blocks wide
 * and the whole maze is {@code cells * PITCH + 1} blocks across. An edge is a wall between two cells: {@code edge = (row * cells + column) * 2 + dir}, dir 0 = the wall on the east of that cell,
 * dir 1 = the wall on the south of it. Cost: generating or shifting is O(cells squared), at most 64 cells.
 */
public final class PocketMaze {
    public static final int PITCH = 4;

    private final int n;
    private final boolean[] open;        // by edge id

    public PocketMaze(int cells) {
        if (cells < 2 || cells > 16) throw new IllegalArgumentException("cells " + cells);
        this.n = cells;
        this.open = new boolean[cells * cells * 2];
    }

    /** A random perfect maze (depth-first, so the ways are long and winding), the same for the same seed. */
    public static PocketMaze generate(int cells, long seed) {
        PocketMaze m = new PocketMaze(cells);
        Random rnd = new Random(seed);
        boolean[] seen = new boolean[cells * cells];
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        stack.push(0);
        seen[0] = true;
        int[] order = {0, 1, 2, 3};
        while (!stack.isEmpty()) {
            int cur = stack.peek();
            for (int i = 3; i > 0; i--) { int j = rnd.nextInt(i + 1); int t = order[i]; order[i] = order[j]; order[j] = t; }
            boolean moved = false;
            for (int d : order) {
                int next = m.neighbour(cur, d);
                if (next < 0 || seen[next]) continue;
                seen[next] = true;
                m.open[m.edgeBetween(cur, next)] = true;
                stack.push(next);
                moved = true;
                break;
            }
            if (!moved) stack.pop();
        }
        return m;
    }

    // ---------------------------------------------------------------- the grid
    public int cells() { return n; }

    /** Blocks across, walls included. */
    public int size() { return n * PITCH + 1; }

    public int cellCount() { return n * n; }

    public int edgeCount() { return open.length; }

    public int cell(int column, int row) { return row * n + column; }

    public int column(int cell) { return cell % n; }

    public int row(int cell) { return cell / n; }

    /** The cell next to this one in a direction (0 east, 1 south, 2 west, 3 north), or -1 at the edge of the maze. */
    public int neighbour(int cell, int dir) {
        int c = column(cell), r = row(cell);
        return switch (dir) {
            case 0 -> c + 1 < n ? cell + 1 : -1;
            case 1 -> r + 1 < n ? cell + n : -1;
            case 2 -> c > 0 ? cell - 1 : -1;
            default -> r > 0 ? cell - n : -1;
        };
    }

    /** The edge between two neighbouring cells. */
    public int edgeBetween(int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        return lo * 2 + (hi - lo == 1 ? 0 : 1);
    }

    /** True for an edge that exists inside the maze (the east edge of the last column and the south edge of the last row do not). */
    public boolean valid(int edge) {
        if (edge < 0 || edge >= open.length) return false;
        int cell = edge / 2;
        return (edge & 1) == 0 ? column(cell) + 1 < n : row(cell) + 1 < n;
    }

    /** The two cells an edge lies between. */
    public int[] ends(int edge) {
        int cell = edge / 2;
        return new int[]{cell, (edge & 1) == 0 ? cell + 1 : cell + n};
    }

    public boolean isOpen(int edge) { return valid(edge) && open[edge]; }

    public int openCount() {
        int k = 0;
        for (int e = 0; e < open.length; e++) if (valid(e) && open[e]) k++;
        return k;
    }

    // ---------------------------------------------------------------- distances
    /** Steps from a cell to every cell through open walls (-1 for a cell that cannot be reached). */
    public int[] distances(int from) {
        int[] dist = new int[n * n];
        java.util.Arrays.fill(dist, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        dist[from] = 0;
        q.add(from);
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (int d = 0; d < 4; d++) {
                int next = neighbour(cur, d);
                if (next < 0 || dist[next] >= 0 || !open[edgeBetween(cur, next)]) continue;
                dist[next] = dist[cur] + 1;
                q.add(next);
            }
        }
        return dist;
    }

    /** The cell furthest (in steps) from a cell. */
    public int farthest(int from) {
        int[] dist = distances(from);
        int best = from;
        for (int c = 0; c < dist.length; c++) if (dist[c] > dist[best]) best = c;
        return best;
    }

    /** True when every cell can be reached from every other (the maze is in one piece). */
    public boolean connected() {
        for (int d : distances(0)) if (d < 0) return false;
        return true;
    }

    /** True when it is a perfect maze: in one piece and with exactly one way between two cells, which is the same as having cells - 1 open walls. */
    public boolean perfect() { return connected() && openCount() == n * n - 1; }

    // ---------------------------------------------------------------- shifting
    /** One shift: a closed wall that opens and an open wall that closes. */
    public record Change(int opened, int closed) {}

    /**
     * Plans one shift without doing it: a closed wall is picked at random and would open (each wall is tried at most once); that makes one loop, and one wall of the loop for which {@code mayClose} says yes would close. Returns null
     * when none of the twelve tries finds a wall that may close (for example every wall of the loop has someone standing in it). Bounded: twelve tries, each one BFS over at most 64 cells.
     */
    public Change plan(Random rnd, IntPredicate mayClose) {
        List<Integer> closed = new ArrayList<>();
        for (int e = 0; e < open.length; e++) if (valid(e) && !open[e]) closed.add(e);
        for (int attempt = 0; attempt < 12 && !closed.isEmpty(); attempt++) {
            int edge = closed.remove(rnd.nextInt(closed.size()));
            int[] ab = ends(edge);
            List<Integer> loop = path(ab[0], ab[1]);
            List<Integer> choices = new ArrayList<>();
            for (int e : loop) if (mayClose.test(e)) choices.add(e);
            if (choices.isEmpty()) continue;
            return new Change(edge, choices.get(rnd.nextInt(choices.size())));
        }
        return null;
    }

    /** Does a planned shift: the wall opens and the other closes. The maze stays perfect. */
    public void apply(Change c) {
        if (!valid(c.opened()) || !valid(c.closed()) || open[c.opened()] || !open[c.closed()]) throw new IllegalStateException("stale change " + c);
        open[c.opened()] = true;
        open[c.closed()] = false;
    }

    /** The open walls on the way between two cells (a perfect maze has exactly one way). */
    private List<Integer> path(int from, int to) {
        int[] parent = new int[n * n];
        java.util.Arrays.fill(parent, -2);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        parent[from] = -1;
        q.add(from);
        while (!q.isEmpty() && parent[to] == -2) {
            int cur = q.poll();
            for (int d = 0; d < 4; d++) {
                int next = neighbour(cur, d);
                if (next < 0 || parent[next] != -2 || !open[edgeBetween(cur, next)]) continue;
                parent[next] = cur;
                q.add(next);
            }
        }
        List<Integer> edges = new ArrayList<>();
        for (int c = to; parent[c] >= 0; c = parent[c]) edges.add(edgeBetween(c, parent[c]));
        return edges;
    }

    // ---------------------------------------------------------------- blocks
    /**
     * True when the column of blocks at (x, z) (relative to the maze's corner, 0 .. size - 1) is wall: the outer wall, a pillar where the wall lines cross, or a wall between two cells that is
     * closed. Anything else is a corridor.
     */
    public boolean solid(int x, int z) {
        int s = size();
        if (x < 0 || z < 0 || x >= s || z >= s) return true;
        int mx = x % PITCH, mz = z % PITCH;
        if (mx != 0 && mz != 0) return false;
        if (mx == 0 && mz == 0) return true;
        if (mx == 0) {                                                   // a wall line that runs north-south: the east wall of the cell to its west
            int column = x / PITCH - 1, row = z / PITCH;
            return column < 0 || column >= n - 1 || !open[edgeId(column, row, 0)];
        }
        int column = x / PITCH, row = z / PITCH - 1;                     // a wall line that runs east-west: the south wall of the cell above it
        return row < 0 || row >= n - 1 || !open[edgeId(column, row, 1)];
    }

    private int edgeId(int column, int row, int dir) { return (row * n + column) * 2 + dir; }

    /** The three block columns a wall fills, as {x0, z0, x1, z1} (inclusive, relative to the corner). */
    public int[] gap(int edge) {
        int cell = edge / 2, c = column(cell), r = row(cell);
        if ((edge & 1) == 0) return new int[]{(c + 1) * PITCH, r * PITCH + 1, (c + 1) * PITCH, r * PITCH + PITCH - 1};
        return new int[]{c * PITCH + 1, (r + 1) * PITCH, c * PITCH + PITCH - 1, (r + 1) * PITCH};
    }

    /** The cell that the block column at (x, z) belongs to (wall lines belong to the cell before them), or -1 outside the maze. */
    public int cellAt(double x, double z) {
        int c = (int) Math.floor(x / PITCH), r = (int) Math.floor(z / PITCH);
        if (x < 0 || z < 0 || c >= n || r >= n) return -1;
        return cell(c, r);
    }

    /** The middle of a cell's floor, in blocks from the corner. */
    public double[] centre(int cell) {
        return new double[]{column(cell) * PITCH + PITCH / 2.0 + 0.5, row(cell) * PITCH + PITCH / 2.0 + 0.5};
    }
}
