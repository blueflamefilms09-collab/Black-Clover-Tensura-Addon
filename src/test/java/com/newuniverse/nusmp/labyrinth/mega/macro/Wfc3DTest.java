package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the solver is the most intricate pure class of the Hub and its failure modes are silent (an interior that is subtly inconsistent, a
 * generation that loops). These tests check its OUTPUT with a checker written independently of the solver's own bookkeeping (it re-reads the tiles'
 * sockets and the tileset's matching table), pin determinism, the fixed cells, the mask, the wrap-around, the contradiction handling (unsolvable
 * grids end in the documented fallback, infeasible fixed cells are detected), and fuzz the whole machinery on random tilesets so that the
 * backtracking and restart paths are really executed.
 */
public class Wfc3DTest {

    /** Independent adjacency checker. Returns the number of violated faces (0 = a consistent assembly). */
    static int violations(Wfc3D.Tileset ts, Wfc3D.Problem p, Wfc3D.Result r) {
        int bad = 0;
        for (int z = 0; z < p.sz; z++) for (int y = 0; y < p.sy; y++) for (int x = 0; x < p.sx; x++) {
            int t = r.tileAt(x, y, z);
            if (p.stateAt(x, y, z) == Wfc3D.EXCLUDED) { if (t != -1) bad++; continue; }
            if (t < 0) { bad++; continue; }
            for (int d = 0; d < 6; d++) {
                int nx = x + Wfc3D.DX[d], ny = y + Wfc3D.DY[d], nz = z + Wfc3D.DZ[d];
                if (p.wrapX) nx = (nx + p.sx) % p.sx;
                boolean outside = nx < 0 || nx >= p.sx || ny < 0 || ny >= p.sy || nz < 0 || nz >= p.sz;
                int mine = ts.tile(t).sockets[d];
                if (outside || p.stateAt(nx, ny, nz) == Wfc3D.EXCLUDED) {
                    if ((p.edgeMask[d] >> mine & 1) == 0) bad++;
                    continue;
                }
                int u = r.tileAt(nx, ny, nz);
                if (u < 0 || !ts.socketsMatch(mine, ts.tile(u).sockets[Wfc3D.opposite(d)])) bad++;
            }
        }
        return bad;
    }

    /** Two tiles, A (every face socket 0) and B (every face socket 1); nothing matches across. */
    static Wfc3D.Tileset uniformPair() {
        return new Wfc3D.Tileset(new Wfc3D.Tile[] {
                new Wfc3D.Tile("A", 1, 0, 0, 0, 0, 0, 0),
                new Wfc3D.Tile("B", 1, 1, 1, 1, 1, 1, 1)}, null);
    }

    /** Alternating tiles: W (+x 0, -x 1) and K (+x 1, -x 0); y and z faces use socket 2 and match themselves. */
    static Wfc3D.Tileset alternating() {
        return new Wfc3D.Tileset(new Wfc3D.Tile[] {
                new Wfc3D.Tile("W", 1, 0, 1, 2, 2, 2, 2),
                new Wfc3D.Tile("K", 1, 1, 0, 2, 2, 2, 2)}, null);
    }

    @Test
    public void aTrivialTilesetFillsTheGridConsistently() {
        Wfc3D.Tileset ts = uniformPair();
        Wfc3D.Problem p = new Wfc3D.Problem(5, 4, 3, false);
        for (int z = 0; z < 3; z++) for (int y = 0; y < 4; y++) for (int x = 0; x < 5; x++) p.setFree(x, y, z);
        Wfc3D.Result r = Wfc3D.solve(ts, p, 99L, 4, 0);
        assertFalse(r.fallbackUsed, "no fallback for a trivially solvable grid");
        assertEquals(0, violations(ts, p, r));
        int first = r.tileAt(0, 0, 0);
        for (int z = 0; z < 3; z++) for (int y = 0; y < 4; y++) for (int x = 0; x < 5; x++) assertEquals(first, r.tileAt(x, y, z), "uniform tiles only match themselves");
    }

    @Test
    public void everyAdjacentPairIsCompatibleOnAnAlternatingRing() {
        Wfc3D.Tileset ts = alternating();
        for (int sx : new int[] {2, 4, 6, 8, 10}) {
            Wfc3D.Problem p = new Wfc3D.Problem(sx, 3, 2, true);
            for (int z = 0; z < 2; z++) for (int y = 0; y < 3; y++) for (int x = 0; x < sx; x++) p.setFree(x, y, z);
            Wfc3D.Result r = Wfc3D.solve(ts, p, 5L, 4, 0);
            assertFalse(r.fallbackUsed, "an even ring alternates: " + sx);
            assertEquals(0, violations(ts, p, r), "ring of " + sx);
            for (int x = 0; x < sx; x++) assertTrue(r.tileAt(x, 0, 0) != r.tileAt((x + 1) % sx, 0, 0), "neighbours alternate at " + x);
        }
    }

    @Test
    public void anOddRingCannotAlternateSoTheDocumentedFallbackFillsItWithThePlainTile() {
        Wfc3D.Tileset ts = alternating();
        Wfc3D.Problem p = new Wfc3D.Problem(5, 1, 1, true);
        for (int x = 0; x < 5; x++) p.setFree(x, 0, 0);
        Wfc3D.Result r = Wfc3D.solve(ts, p, 1L, 3, 1);
        assertTrue(r.fallbackUsed, "an odd ring is unsolvable");
        assertFalse(r.infeasible, "arc consistency alone cannot see an odd cycle");
        assertEquals(3, r.restarts, "every allowed try failed first");
        for (int x = 0; x < 5; x++) assertEquals(1, r.tileAt(x, 0, 0), "the fallback is the plain tile everywhere");
    }

    @Test
    public void fixedCellsAreRespectedAndForcePropagation() {
        Wfc3D.Tileset ts = alternating();
        Wfc3D.Problem p = new Wfc3D.Problem(8, 2, 2, true);
        for (int z = 0; z < 2; z++) for (int y = 0; y < 2; y++) for (int x = 0; x < 8; x++) p.setFree(x, y, z);
        p.setFixed(3, 1, 1, 1); // a K
        Wfc3D.Result r = Wfc3D.solve(ts, p, 11L, 4, 0);
        assertFalse(r.fallbackUsed);
        assertEquals(1, r.tileAt(3, 1, 1));
        assertEquals(0, r.tileAt(2, 1, 1), "left of a K must be a W");
        assertEquals(0, r.tileAt(4, 1, 1), "right of a K must be a W");
        assertEquals(1, r.tileAt(5, 1, 1));
        assertEquals(0, violations(ts, p, r));
        // the same grid with a fixed cell that contradicts another fixed cell is infeasible from the start, detected without any search
        Wfc3D.Problem q = new Wfc3D.Problem(8, 1, 1, true);
        for (int x = 0; x < 8; x++) q.setFree(x, 0, 0);
        q.setFixed(1, 0, 0, 1);
        q.setFixed(2, 0, 0, 1);
        Wfc3D.Result s = Wfc3D.solve(ts, q, 3L, 10, 0);
        assertTrue(s.infeasible && s.fallbackUsed, "two K side by side");
        assertEquals(1, s.restarts, "an infeasible problem stops after the first try");
        assertEquals(1, s.tileAt(1, 0, 0));
        assertEquals(1, s.tileAt(2, 0, 0));
    }

    @Test
    public void excludedCellsAreNotSolvedAndDoNotConstrain() {
        Wfc3D.Tileset ts = alternating();
        Wfc3D.Problem p = new Wfc3D.Problem(10, 1, 1, false);
        for (int x = 0; x < 10; x++) if (x != 4 && x != 5) p.setFree(x, 0, 0);
        p.setFixed(0, 0, 0, 0);
        p.setFixed(6, 0, 0, 0); // across the hole these two are only consistent because the hole does not constrain
        Wfc3D.Result r = Wfc3D.solve(ts, p, 8L, 4, 0);
        assertFalse(r.fallbackUsed);
        assertEquals(-1, r.tileAt(4, 0, 0));
        assertEquals(-1, r.tileAt(5, 0, 0));
        assertEquals(-1, r.tileAt(-1, 0, 0));
        assertEquals(-1, r.tileAt(10, 0, 0));
        assertEquals(0, violations(ts, p, r));
        assertEquals(0, r.tileAt(2, 0, 0), "W K W alternation from the fixed W at 0");
        assertEquals(1, r.tileAt(3, 0, 0));
        assertEquals(1, r.tileAt(7, 0, 0));
    }

    @Test
    public void edgeMasksConstrainFacesThatTouchNothing() {
        // "open" faces +z/-z with socket 7, "closed" with socket 3; 7 and 3 match each other, so a stack may mix them freely,
        // but the edge mask says that a +z face that touches nothing must carry socket 3
        Wfc3D.Tileset ts = new Wfc3D.Tileset(new Wfc3D.Tile[] {
                new Wfc3D.Tile("open", 1, 0, 0, 0, 0, 7, 7),
                new Wfc3D.Tile("closed", 1, 0, 0, 0, 0, 3, 3)}, new int[][] {{7, 3}});
        Wfc3D.Problem p = new Wfc3D.Problem(4, 1, 2, false);
        for (int z = 0; z < 2; z++) for (int x = 0; x < 4; x++) p.setFree(x, 0, z);
        p.restrictEdge(Wfc3D.PZ, 3);
        for (long seed = 0; seed < 20; seed++) {
            Wfc3D.Result r = Wfc3D.solve(ts, p, seed, 4, 1);
            assertFalse(r.fallbackUsed, "solvable, seed " + seed);
            for (int x = 0; x < 4; x++) assertEquals(1, r.tileAt(x, 0, 1), "the top layer touches the outside on +z and must be closed");
            assertEquals(0, violations(ts, p, r), "seed " + seed);
        }
        // without the restriction the top layer is free to be open
        Wfc3D.Problem q = new Wfc3D.Problem(4, 1, 2, false);
        for (int z = 0; z < 2; z++) for (int x = 0; x < 4; x++) q.setFree(x, 0, z);
        boolean sawOpenOnTop = false;
        for (long seed = 0; seed < 20; seed++) for (int x = 0; x < 4; x++) if (Wfc3D.solve(ts, q, seed, 4, 1).tileAt(x, 0, 1) == 0) sawOpenOnTop = true;
        assertTrue(sawOpenOnTop, "the restriction is what keeps the top layer closed");
    }

    @Test
    public void theSameSeedGivesTheSameAssemblyAndOtherSeedsOtherOnes() {
        Wfc3D.Tileset ts = randomTileset(new SplittableRandom(77), 7, 3);
        Wfc3D.Problem p = new Wfc3D.Problem(6, 6, 6, false);
        for (int z = 0; z < 6; z++) for (int y = 0; y < 6; y++) for (int x = 0; x < 6; x++) p.setFree(x, y, z);
        Wfc3D.Result a = Wfc3D.solve(ts, p, 1234L, 8, 0), b = Wfc3D.solve(ts, p, 1234L, 8, 0);
        int differing = 0;
        for (int z = 0; z < 6; z++) for (int y = 0; y < 6; y++) for (int x = 0; x < 6; x++) {
            assertEquals(a.tileAt(x, y, z), b.tileAt(x, y, z), "same seed, same cell");
            if (a.tileAt(x, y, z) != Wfc3D.solve(ts, p, 1235L, 8, 0).tileAt(x, y, z)) differing++;
        }
        assertEquals(a.restarts, b.restarts);
        assertEquals(a.collapses, b.collapses);
        assertTrue(differing > 0, "another seed assembles another grid");
    }

    static Wfc3D.Tileset randomTileset(SplittableRandom rnd, int tiles, int sockets) {
        Wfc3D.Tile[] t = new Wfc3D.Tile[tiles];
        for (int i = 0; i < tiles; i++)
            t[i] = new Wfc3D.Tile("t" + i, 0.5 + rnd.nextInt(6), rnd.nextInt(sockets), rnd.nextInt(sockets), rnd.nextInt(sockets), rnd.nextInt(sockets), rnd.nextInt(sockets), rnd.nextInt(sockets));
        return new Wfc3D.Tileset(t, null);
    }

    @Test
    public void fuzzOnRandomTilesetsKeepsEverySolutionConsistentAndExercisesBacktracking() {
        SplittableRandom rnd = new SplittableRandom(2024);
        int solved = 0, fallbacks = 0, withBacktracks = 0, withRestarts = 0;
        for (int round = 0; round < 300; round++) {
            Wfc3D.Tileset ts = randomTileset(rnd, 4 + rnd.nextInt(8), 2 + rnd.nextInt(2));
            int sx = 3 + rnd.nextInt(5), sy = 2 + rnd.nextInt(4), sz = 1 + rnd.nextInt(4);
            Wfc3D.Problem p = new Wfc3D.Problem(sx, sy, sz, rnd.nextBoolean());
            for (int z = 0; z < sz; z++) for (int y = 0; y < sy; y++) for (int x = 0; x < sx; x++) if (rnd.nextInt(10) != 0) p.setFree(x, y, z);
            for (int k = 0; k < 2; k++) {
                int x = rnd.nextInt(sx), y = rnd.nextInt(sy), z = rnd.nextInt(sz);
                if (p.stateAt(x, y, z) != Wfc3D.EXCLUDED) p.setFixed(x, y, z, rnd.nextInt(ts.size()));
            }
            Wfc3D.Result r = Wfc3D.solve(ts, p, round * 31L, 6, 0);
            if (r.fallbackUsed) { fallbacks++; continue; }
            solved++;
            if (r.backtracks > 0) withBacktracks++;
            if (r.restarts > 0) withRestarts++;
            assertEquals(0, violations(ts, p, r), "round " + round + " produced an inconsistent assembly");
            for (int z = 0; z < sz; z++) for (int y = 0; y < sy; y++) for (int x = 0; x < sx; x++)
                if (p.stateAt(x, y, z) == Wfc3D.FIXED) assertEquals(p.fixedTileAt(x, y, z), r.tileAt(x, y, z), "fixed cell kept in round " + round);
        }
        assertTrue(solved > 40, "a fair share of random problems is solvable, solved=" + solved);
        assertTrue(fallbacks > 0, "some random problems are unsolvable and take the fallback, fallbacks=" + fallbacks);
        assertTrue(withBacktracks > 0, "the backtracking path was exercised, withBacktracks=" + withBacktracks);
        assertTrue(withRestarts > 0, "the restart path was exercised, withRestarts=" + withRestarts);
    }

    /** Three colours, neighbours must differ on every axis: arc consistency alone cannot solve it, so the search has to backtrack. */
    static Wfc3D.Tileset threeColours() {
        Wfc3D.Tile[] tiles = new Wfc3D.Tile[3];
        for (int c = 0; c < 3; c++) tiles[c] = new Wfc3D.Tile("c" + c, 1, c, 3 + c, 6 + c, 9 + c, 12 + c, 15 + c);
        java.util.List<int[]> pairs = new java.util.ArrayList<>();
        for (int k = 0; k < 3; k++) for (int c = 0; c < 3; c++) for (int d = 0; d < 3; d++) if (c != d) pairs.add(new int[] {k * 6 + c, k * 6 + 3 + d});
        return new Wfc3D.Tileset(tiles, pairs.toArray(new int[0][]));
    }

    @Test
    public void threeColouringNeedsBacktrackingAndTheBanMakesTheSearchConverge() {
        Wfc3D.Tileset ts = threeColours();
        long backtracksSingleTry = 0;
        int fallbacksSingleTry = 0;
        for (long seed = 0; seed < 200; seed++) {
            Wfc3D.Problem p = new Wfc3D.Problem(8, 6, 4, true);
            for (int z = 0; z < 4; z++) for (int y = 0; y < 6; y++) for (int x = 0; x < 8; x++) p.setFree(x, y, z);
            p.setFixed(0, 0, 0, 0);
            p.setFixed(1, 1, 1, 1);
            p.setFixed(2, 2, 2, 2);
            Wfc3D.Result one = Wfc3D.solve(ts, p, seed, 1, 0);
            backtracksSingleTry += one.backtracks;
            if (one.fallbackUsed) fallbacksSingleTry++; else assertEquals(0, violations(ts, p, one), "single try, seed " + seed);
            Wfc3D.Result six = Wfc3D.solve(ts, p, seed, 6, 0);
            assertFalse(six.fallbackUsed, "with restarts every seed is solved, seed " + seed);
            assertEquals(0, violations(ts, p, six), "six tries, seed " + seed);
        }
        // measured: 680 backtracks in total and 1 exhausted budget over these 200 single-try runs; a search that forgets what it already
        // tried (no ban of the failed tile) needs about 2700 and exhausts the budget about 7 times
        assertTrue(backtracksSingleTry > 0, "the grid really needs backtracking");
        assertTrue(backtracksSingleTry < 1500, "the ban keeps the search short, total backtracks " + backtracksSingleTry);
        assertTrue(fallbacksSingleTry <= 3, "single-try budget exhausted " + fallbacksSingleTry + " times");
    }

    @Test
    public void aLargeGridSolvesFast() {
        // the size of the biggest real landmark grid (about 28 x 22 x 4), with a tileset that always has the all-open tile
        Wfc3D.Tileset ts = alternating();
        Wfc3D.Problem p = new Wfc3D.Problem(28, 22, 4, true);
        for (int z = 0; z < 4; z++) for (int y = 0; y < 22; y++) for (int x = 0; x < 28; x++) p.setFree(x, y, z);
        long t0 = System.nanoTime();
        Wfc3D.Result r = Wfc3D.solve(ts, p, 3L, 4, 0);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertFalse(r.fallbackUsed);
        assertEquals(0, violations(ts, p, r));
        assertTrue(ms < 2000, "solved in " + ms + " ms");
    }
}
