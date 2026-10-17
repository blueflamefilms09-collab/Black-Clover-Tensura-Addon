package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.SplittableRandom;

/**
 * Why: the owner's brief asks for 3D Wave Function Collapse to populate the carved spaces of the Hub (trial chambers upwards in the Holy Spire,
 * caverns downwards in the Abyssal Chasm). This is a real solver, not a random scatter: tiles with six face sockets and an explicit symmetric
 * compatibility table, a grid of cells where each cell is EXCLUDED (the noise says it is rock or outside the volume: nothing is solved there),
 * FREE (to be solved) or FIXED (pre-collapsed to a given tile: the guaranteed path and the entrances), lowest-entropy-first collapse with a seeded
 * tie-break, constraint propagation over a work queue, and CONTRADICTION handling in three escalating layers: (1) bounded chronological
 * backtracking (undo the last collapse through a trail and ban that tile there), (2) when a try runs out of backtracks, a RESTART with the next
 * seed (up to {@code maxTries}), (3) when every try failed, a documented deterministic FALLBACK that fills every free cell with the caller's
 * "plain" tile, so a bad tileset can degrade a landmark but can never stop world generation or make it loop.
 *
 * <p>Design choices and their reasons. Domains are one {@code long} bit mask per cell (a tileset has at most 64 tiles; the real ones have about a
 * dozen), so a propagation step is a handful of bit operations and the largest real grid solves in milliseconds. The trail records (cell, old
 * domain) pairs, so undoing a collapse restores exactly what propagation changed without copying the grid. Entropy is cached per cell and refreshed
 * only when a domain changes. The solver is deterministic for a given (tileset, problem, seed): the random source is a {@link SplittableRandom},
 * logarithms use {@link StrictMath}, and nothing depends on hash order, so a world regenerated on another machine gets the identical interior.
 * The x axis may WRAP (the Hub's landmarks are rings of angular sectors). A face whose neighbour is outside the grid or EXCLUDED is checked against
 * the problem's per-direction "edge" socket masks instead of a neighbour (the caller decides there which sockets are acceptable). A FREE cell may also be
 * restricted to a subset of the tiles ({@link Problem#allowOnly}, {@link Problem#forbid}): that is how a doorway tile is kept off every cell the caller did not
 * pre-collapse on purpose.
 * Pure: no Minecraft import.
 */
public final class Wfc3D {
    private Wfc3D() {}

    /** Directions: +x, -x, +y, -y, +z, -z. The opposite of {@code d} is {@code d ^ 1}. */
    public static final int PX = 0, NX = 1, PY = 2, NY = 3, PZ = 4, NZ = 5;
    public static final int[] DX = {1, -1, 0, 0, 0, 0}, DY = {0, 0, 1, -1, 0, 0}, DZ = {0, 0, 0, 0, 1, -1};

    public static int opposite(int dir) { return dir ^ 1; }

    /** Cell states of a {@link Problem}. */
    public static final byte EXCLUDED = 0, FREE = 1, FIXED = 2;

    /** One tile: a name, a weight (how likely relative to the others) and one socket id (0..31) per face, indexed by direction. */
    public static final class Tile {
        public final String name;
        public final double weight;
        public final int[] sockets;

        public Tile(String name, double weight, int px, int nx, int py, int ny, int pz, int nz) {
            if (!(weight > 0)) throw new IllegalArgumentException("tile weight must be positive: " + name);
            this.name = name;
            this.weight = weight;
            this.sockets = new int[] {px, nx, py, ny, pz, nz};
            for (int s : sockets) if (s < 0 || s > 31) throw new IllegalArgumentException("socket id out of 0..31 in tile " + name);
        }
    }

    /**
     * A tileset: the tiles and which socket ids match. Two faces that touch must carry sockets that match; a socket always matches itself, further
     * pairs are given explicitly and are symmetric by construction ("a matches b" means "b matches a").
     */
    public static final class Tileset {
        private final Tile[] tiles;
        private final boolean[][] match = new boolean[32][32];
        /** allow[t][d] = bit mask of the tiles that may sit on side d of tile t. */
        private final long[][] allow;

        public Tileset(Tile[] tiles, int[][] extraMatchingPairs) {
            if (tiles.length == 0 || tiles.length > 64) throw new IllegalArgumentException("a tileset has 1..64 tiles");
            this.tiles = tiles.clone();
            for (int i = 0; i < 32; i++) match[i][i] = true;
            if (extraMatchingPairs != null) for (int[] pr : extraMatchingPairs) { match[pr[0]][pr[1]] = true; match[pr[1]][pr[0]] = true; }
            allow = new long[tiles.length][6];
            for (int t = 0; t < tiles.length; t++)
                for (int d = 0; d < 6; d++) {
                    long m = 0;
                    for (int u = 0; u < tiles.length; u++)
                        if (match[tiles[t].sockets[d]][tiles[u].sockets[opposite(d)]]) m |= 1L << u;
                    allow[t][d] = m;
                }
        }

        public int size() { return tiles.length; }

        public Tile tile(int i) { return tiles[i]; }

        public int indexOf(String name) {
            for (int i = 0; i < tiles.length; i++) if (tiles[i].name.equals(name)) return i;
            throw new IllegalArgumentException("no tile named " + name);
        }

        /** Do two socket ids match (symmetric)? */
        public boolean socketsMatch(int a, int b) { return match[a][b]; }

        /** May tile {@code other} sit on side {@code dir} of tile {@code tile}? */
        public boolean compatible(int tile, int dir, int other) { return (allow[tile][dir] >> other & 1L) != 0; }
    }

    /** The grid to solve: a state per cell, fixed tiles, whether x wraps, and which sockets are acceptable on a face that touches nothing. */
    public static final class Problem {
        public final int sx, sy, sz;
        public final boolean wrapX;
        private final byte[] state;
        private final int[] fixedTile;
        /** Per cell, the bit mask of the tiles the cell may take (a FREE cell only). Default: every tile. */
        private final long[] allowed;
        /** Per direction, the bit mask of socket ids acceptable on a face whose neighbour is outside the grid or EXCLUDED. Default: all. */
        public final int[] edgeMask = new int[] {-1, -1, -1, -1, -1, -1};

        public Problem(int sx, int sy, int sz, boolean wrapX) {
            if (sx < 1 || sy < 1 || sz < 1) throw new IllegalArgumentException("grid must be at least 1x1x1");
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.wrapX = wrapX;
            this.state = new byte[sx * sy * sz];
            this.fixedTile = new int[sx * sy * sz];
            this.allowed = new long[sx * sy * sz];
            java.util.Arrays.fill(this.allowed, -1L);
        }

        public int index(int x, int y, int z) { return (z * sy + y) * sx + x; }

        public int cells() { return state.length; }

        public byte stateAt(int x, int y, int z) { return state[index(x, y, z)]; }

        public void setFree(int x, int y, int z) { state[index(x, y, z)] = FREE; }

        public void setExcluded(int x, int y, int z) { state[index(x, y, z)] = EXCLUDED; }

        public void setFixed(int x, int y, int z, int tile) { int i = index(x, y, z); state[i] = FIXED; fixedTile[i] = tile; }

        public int fixedTileAt(int x, int y, int z) { return fixedTile[index(x, y, z)]; }

        /**
         * Restricts a FREE cell to the tiles in {@code tileMask} (bit i = tile i). Why: some tiles must exist only where the caller pre-collapsed a
         * cell (a doorway onto a staircase landing, the way in from outside): letting the solver choose them anywhere else would open doors onto a drop.
         */
        public void allowOnly(int x, int y, int z, long tileMask) { allowed[index(x, y, z)] = tileMask; }

        /** Removes one tile from the choices of a cell. */
        public void forbid(int x, int y, int z, int tile) { allowed[index(x, y, z)] &= ~(1L << tile); }

        /** The tiles a cell may take (all bits set when unrestricted). */
        public long allowedAt(int x, int y, int z) { return allowed[index(x, y, z)]; }

        /** Restrict the sockets acceptable on faces that touch nothing in direction {@code dir} to the given ids. */
        public void restrictEdge(int dir, int... socketIds) {
            int m = 0;
            for (int s : socketIds) m |= 1 << s;
            edgeMask[dir] = m;
        }

        public int count(byte which) {
            int c = 0;
            for (byte b : state) if (b == which) c++;
            return c;
        }
    }

    /** The outcome: a tile index per cell ({@code -1} for EXCLUDED cells) and what it took to get there. */
    public static final class Result {
        public final int sx, sy, sz;
        private final int[] tiles;
        /** How many attempts failed before this result (0 = first try worked). */
        public final int restarts;
        /** True when every attempt failed (or the problem was infeasible) and the free cells were filled with the plain tile. */
        public final boolean fallbackUsed;
        /** True when the fixed cells and edge masks contradict each other already at the start (no seed can help). */
        public final boolean infeasible;
        public final long collapses, backtracks, nanos;

        Result(Problem p, int[] tiles, int restarts, boolean fallback, boolean infeasible, long collapses, long backtracks, long nanos) {
            this.sx = p.sx;
            this.sy = p.sy;
            this.sz = p.sz;
            this.tiles = tiles;
            this.restarts = restarts;
            this.fallbackUsed = fallback;
            this.infeasible = infeasible;
            this.collapses = collapses;
            this.backtracks = backtracks;
            this.nanos = nanos;
        }

        /** The tile at a cell, or -1 when the cell is EXCLUDED or outside the grid. */
        public int tileAt(int x, int y, int z) {
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return -1;
            return tiles[(z * sy + y) * sx + x];
        }
    }

    /**
     * Solve. {@code plainTile} is the tile the deterministic fallback uses for every FREE cell; it should be the one that is compatible with the
     * most (an all-solid tile). Never throws for a well-formed problem: the worst case is the fallback.
     */
    public static Result solve(Tileset ts, Problem p, long seed, int maxTries, int plainTile) {
        long t0 = System.nanoTime();
        if (plainTile < 0 || plainTile >= ts.size()) throw new IllegalArgumentException("plain tile out of range");
        int tries = Math.max(1, maxTries);
        long collapses = 0, backtracks = 0;
        int failed = 0;
        Run run = new Run(ts, p);
        for (int attempt = 0; attempt < tries; attempt++) {
            long s = seed + attempt * 0x9E3779B97F4A7C15L;
            boolean ok = run.attempt(s);
            collapses += run.collapses;
            backtracks += run.backtracks;
            if (ok) return new Result(p, run.extract(), failed, false, false, collapses, backtracks, System.nanoTime() - t0);
            failed++;
            if (run.infeasibleAtStart) break;
        }
        int[] out = new int[p.cells()];
        for (int i = 0; i < out.length; i++) {
            byte st = p.state[i];
            out[i] = st == EXCLUDED ? -1 : (st == FIXED ? p.fixedTile[i] : plainTile);
        }
        return new Result(p, out, failed, true, run.infeasibleAtStart, collapses, backtracks, System.nanoTime() - t0);
    }

    /** One solver instance: all the working arrays, reused between attempts. */
    private static final class Run {
        final Tileset ts;
        final Problem p;
        final int n, tcount;
        final int[] nbr;
        final long[] dom;
        final double[] ent, jitter;
        final long[] boundary = new long[6];
        final double[] w, wlogw;
        int[] trailCell = new int[256];
        long[] trailOld = new long[256];
        int tp;
        int[] decCell = new int[64], decTile = new int[64], decMark = new int[64];
        int dc;
        final int[] queue;
        final boolean[] inQueue;
        int qHead, qTail, qCount;
        long collapses, backtracks;
        long backtrackLimit;
        boolean infeasibleAtStart;
        SplittableRandom rng;

        Run(Tileset ts, Problem p) {
            this.ts = ts;
            this.p = p;
            this.n = p.cells();
            this.tcount = ts.size();
            this.nbr = new int[n * 6];
            this.dom = new long[n];
            this.ent = new double[n];
            this.jitter = new double[n];
            this.queue = new int[n + 1];
            this.inQueue = new boolean[n];
            this.w = new double[tcount];
            this.wlogw = new double[tcount];
            for (int t = 0; t < tcount; t++) {
                w[t] = ts.tile(t).weight;
                wlogw[t] = w[t] * StrictMath.log(w[t]);
            }
            for (int d = 0; d < 6; d++) {
                long m = 0;
                for (int t = 0; t < tcount; t++) if ((p.edgeMask[d] >> ts.tile(t).sockets[d] & 1) != 0) m |= 1L << t;
                boundary[d] = m;
            }
            for (int z = 0; z < p.sz; z++)
                for (int y = 0; y < p.sy; y++)
                    for (int x = 0; x < p.sx; x++) {
                        int i = p.index(x, y, z);
                        for (int d = 0; d < 6; d++) {
                            int nx = x + DX[d], ny = y + DY[d], nz = z + DZ[d];
                            if (p.wrapX && nx < 0) nx += p.sx;
                            if (p.wrapX && nx >= p.sx) nx -= p.sx;
                            boolean out = nx < 0 || nx >= p.sx || ny < 0 || ny >= p.sy || nz < 0 || nz >= p.sz;
                            nbr[i * 6 + d] = out ? -1 : p.index(nx, ny, nz);
                        }
                    }
        }

        boolean attempt(long seed) {
            rng = new SplittableRandom(seed);
            long all = tcount == 64 ? -1L : (1L << tcount) - 1;
            collapses = 0;
            backtracks = 0;
            tp = 0;
            dc = 0;
            qHead = qTail = qCount = 0;
            java.util.Arrays.fill(inQueue, false);
            backtrackLimit = Math.max(200L, 2L * p.count(FREE));
            for (int i = 0; i < n; i++) {
                jitter[i] = rng.nextDouble() * 1e-6;
                byte st = p.state[i];
                dom[i] = st == EXCLUDED ? 0L : (st == FIXED ? 1L << p.fixedTile[i] : all & p.allowed[i]);
            }
            for (int i = 0; i < n; i++) {
                if (p.state[i] == EXCLUDED) continue;
                for (int d = 0; d < 6; d++) {
                    int j = nbr[i * 6 + d];
                    if (j < 0 || p.state[j] == EXCLUDED) dom[i] &= boundary[d];
                }
                if (dom[i] == 0L) { infeasibleAtStart = true; return false; }
            }
            for (int i = 0; i < n; i++) refreshEntropy(i);
            for (int i = 0; i < n; i++) if (p.state[i] != EXCLUDED) push(i);
            if (!propagate()) { infeasibleAtStart = true; return false; }
            tp = 0; // the initial pruning is permanent: nothing below it can be undone
            while (true) {
                int cell = pickCell();
                if (cell < 0) return true;
                int tile = pickTile(cell);
                collapses++;
                pushDecision(cell, tile, tp);
                setDom(cell, 1L << tile);
                push(cell);
                if (!propagate() && !backtrack()) return false;
            }
        }

        int[] extract() {
            int[] out = new int[n];
            for (int i = 0; i < n; i++) out[i] = p.state[i] == EXCLUDED ? -1 : Long.numberOfTrailingZeros(dom[i]);
            return out;
        }

        /** Undo decisions until a banned alternative propagates cleanly. False when the decisions are used up or the backtrack budget is. */
        boolean backtrack() {
            while (dc > 0) {
                dc--;
                int cell = decCell[dc], tile = decTile[dc];
                undoTo(decMark[dc]);
                if (++backtracks > backtrackLimit) return false;
                long nd = dom[cell] & ~(1L << tile);
                if (nd == 0L) continue;
                setDom(cell, nd);
                push(cell);
                if (propagate()) return true;
            }
            return false;
        }

        void pushDecision(int cell, int tile, int mark) {
            if (dc == decCell.length) {
                decCell = java.util.Arrays.copyOf(decCell, dc * 2);
                decTile = java.util.Arrays.copyOf(decTile, dc * 2);
                decMark = java.util.Arrays.copyOf(decMark, dc * 2);
            }
            decCell[dc] = cell;
            decTile[dc] = tile;
            decMark[dc] = mark;
            dc++;
        }

        void setDom(int cell, long nd) {
            if (tp == trailCell.length) {
                trailCell = java.util.Arrays.copyOf(trailCell, tp * 2);
                trailOld = java.util.Arrays.copyOf(trailOld, tp * 2);
            }
            trailCell[tp] = cell;
            trailOld[tp] = dom[cell];
            tp++;
            dom[cell] = nd;
            refreshEntropy(cell);
        }

        void undoTo(int mark) {
            while (tp > mark) {
                tp--;
                int c = trailCell[tp];
                dom[c] = trailOld[tp];
                refreshEntropy(c);
            }
        }

        void push(int c) {
            if (inQueue[c]) return;
            inQueue[c] = true;
            queue[qTail] = c;
            qTail = qTail == queue.length - 1 ? 0 : qTail + 1;
            qCount++;
        }

        boolean propagate() {
            while (qCount > 0) {
                int c = queue[qHead];
                qHead = qHead == queue.length - 1 ? 0 : qHead + 1;
                qCount--;
                inQueue[c] = false;
                long dc0 = dom[c];
                for (int d = 0; d < 6; d++) {
                    int j = nbr[c * 6 + d];
                    if (j < 0 || p.state[j] == EXCLUDED) continue;
                    long a = 0L, m = dc0;
                    while (m != 0L) {
                        int t = Long.numberOfTrailingZeros(m);
                        m &= m - 1;
                        a |= ts.allow[t][d];
                    }
                    long nd = dom[j] & a;
                    if (nd != dom[j]) {
                        if (nd == 0L) { clearQueue(); return false; }
                        setDom(j, nd);
                        push(j);
                    }
                }
            }
            return true;
        }

        void clearQueue() {
            while (qCount > 0) {
                int c = queue[qHead];
                qHead = qHead == queue.length - 1 ? 0 : qHead + 1;
                qCount--;
                inQueue[c] = false;
            }
        }

        void refreshEntropy(int c) {
            long m = dom[c];
            if (p.state[c] != FREE || (m & (m - 1)) == 0L) { ent[c] = Double.POSITIVE_INFINITY; return; }
            double sw = 0, swl = 0;
            while (m != 0L) {
                int t = Long.numberOfTrailingZeros(m);
                m &= m - 1;
                sw += w[t];
                swl += wlogw[t];
            }
            ent[c] = StrictMath.log(sw) - swl / sw + jitter[c];
        }

        int pickCell() {
            int best = -1;
            double bestE = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                double e = ent[i];
                if (e < bestE) { bestE = e; best = i; }
            }
            return best;
        }

        int pickTile(int cell) {
            long m = dom[cell];
            double sw = 0;
            for (long q = m; q != 0L; q &= q - 1) sw += w[Long.numberOfTrailingZeros(q)];
            double r = rng.nextDouble() * sw;
            int last = -1;
            for (long q = m; q != 0L; q &= q - 1) {
                int t = Long.numberOfTrailingZeros(q);
                last = t;
                r -= w[t];
                if (r < 0) return t;
            }
            return last;
        }
    }
}
