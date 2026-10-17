package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the per-cell tile restriction is what keeps a doorway tile from appearing on a cell nobody pre-collapsed (a door onto a sheer drop), so it is tested on
 * its own with the smallest tilesets that can show it: a restricted cell never takes a banned tile whatever the seed, the restriction propagates to its
 * neighbours, a restriction that leaves nothing is detected as infeasible without any search, and a fixed cell ignores the restriction (the caller pre-collapsed it
 * on purpose).
 */
public class Wfc3DRestrictTest {
    @Test
    public void aBannedTileNeverAppearsOnARestrictedCellWhateverTheSeed() {
        Wfc3D.Tileset ts = Wfc3DTest.randomTileset(new java.util.SplittableRandom(5), 6, 2);
        for (long seed = 0; seed < 60; seed++) {
            Wfc3D.Problem p = new Wfc3D.Problem(5, 5, 1, false);
            for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) p.setFree(x, y, 0);
            p.forbid(2, 2, 0, 0);
            p.forbid(2, 2, 0, 1);
            Wfc3D.Result r = Wfc3D.solve(ts, p, seed, 6, 2);
            if (r.fallbackUsed) continue;
            int t = r.tileAt(2, 2, 0);
            assertTrue(t != 0 && t != 1, "the restricted cell took a banned tile " + t + " with seed " + seed);
            assertEquals(0, Wfc3DTest.violations(ts, p, r));
        }
    }

    @Test
    public void allowOnlyForcesTheChoiceAndPropagatesToTheNeighbours() {
        Wfc3D.Tileset ts = Wfc3DTest.alternating();
        Wfc3D.Problem p = new Wfc3D.Problem(6, 1, 1, true);
        for (int x = 0; x < 6; x++) p.setFree(x, 0, 0);
        p.allowOnly(3, 0, 0, 1L << 1); // only K allowed at x = 3
        for (long seed = 0; seed < 20; seed++) {
            Wfc3D.Result r = Wfc3D.solve(ts, p, seed, 3, 0);
            assertFalse(r.fallbackUsed);
            assertEquals(1, r.tileAt(3, 0, 0));
            assertEquals(0, r.tileAt(2, 0, 0), "the neighbour of a K is a W");
            assertEquals(0, r.tileAt(4, 0, 0));
            assertEquals(1, r.tileAt(1, 0, 0));
        }
    }

    @Test
    public void aRestrictionThatLeavesNothingIsInfeasibleAndAFixedCellIgnoresIt() {
        Wfc3D.Tileset ts = Wfc3DTest.alternating();
        Wfc3D.Problem p = new Wfc3D.Problem(4, 1, 1, false);
        for (int x = 0; x < 4; x++) p.setFree(x, 0, 0);
        p.allowOnly(1, 0, 0, 0L);
        Wfc3D.Result r = Wfc3D.solve(ts, p, 1L, 5, 0);
        assertTrue(r.infeasible && r.fallbackUsed, "an empty choice is infeasible from the start");
        assertEquals(1, r.restarts, "and is not retried");

        Wfc3D.Problem q = new Wfc3D.Problem(4, 1, 1, false);
        for (int x = 0; x < 4; x++) q.setFree(x, 0, 0);
        q.allowOnly(1, 0, 0, 0L);
        q.setFixed(1, 0, 0, 0);
        Wfc3D.Result s = Wfc3D.solve(ts, q, 1L, 5, 0);
        assertFalse(s.fallbackUsed, "a pre-collapsed cell is the caller's decision and the restriction does not apply to it");
        assertEquals(0, s.tileAt(1, 0, 0));
        assertEquals(-1L, new Wfc3D.Problem(2, 2, 2, false).allowedAt(0, 0, 0), "by default every tile is allowed");
    }
}
