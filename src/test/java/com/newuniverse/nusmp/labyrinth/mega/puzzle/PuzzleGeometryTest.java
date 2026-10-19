package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a puzzle that does not fit its room is a puzzle that never appears, and one that fills the floor is a wall. So: EVERY puzzle, at EVERY tier and party size, must fit the interior of
 * the gallery rooms (a 27 x 18 box has 23 x 14 usable blocks once the two-block wall margin is kept); no two cells share a block; pillars and chests stand at least two blocks apart (so there
 * is always a block-wide gap to walk through: a stage never walls a room in); the first group's row runs along x; and the boards grow with depth and party (the work to solve at tier 5 with a
 * full party is never less than at tier 1 for one delver, and is more for most puzzles).
 */
class PuzzleGeometryTest {
    @Test
    void everyPuzzleAtEveryDepthAndPartyFitsAGalleryInterior() {
        int maxW = 0, maxD = 0;
        for (String id : PuzzleCatalog.ids()) for (int tier = 1; tier <= 5; tier++) for (int party : new int[] {1, 2, 4, 8}) for (int s = 0; s < 4; s++) {
            PuzzleRule r = PuzzleCatalog.create(id, new Params(300L * tier + s, party, tier));
            long seed = 99L + s;
            assertTrue(PuzzleGeometry.fits(r.layout(), seed, 23, 14), id + " tier " + tier + " party " + party + " does not fit a 23 x 14 interior");
            PuzzleGeometry.Arrangement a = PuzzleGeometry.arrange(r.layout(), seed, false);
            maxW = Math.max(maxW, Math.min(a.w(), PuzzleGeometry.arrange(r.layout(), seed, true).w()));
            maxD = Math.max(maxD, Math.min(a.d(), PuzzleGeometry.arrange(r.layout(), seed, true).d()));
        }
        assertTrue(maxW <= 23 && maxD <= 14, maxW + " x " + maxD);
    }

    @Test
    void cellsNeverShareABlockAndPillarsKeepAGapToWalkThrough() {
        for (String id : PuzzleCatalog.ids()) for (int tier = 1; tier <= 5; tier += 2) {
            PuzzleRule r = PuzzleCatalog.create(id, new Params(17L + tier, 4, tier));
            for (boolean alongX : new boolean[] {false, true}) {
                PuzzleGeometry.Arrangement a = PuzzleGeometry.arrange(r.layout(), 5L, alongX);
                Set<Long> seen = new HashSet<>();
                int n = r.layout().total();
                for (int i = 0; i < n; i++) {
                    assertTrue(a.x()[i] >= 0 && a.x()[i] < a.w() && a.z()[i] >= 0 && a.z()[i] < a.d(), id + " cell " + i + " outside its box");
                    assertTrue(seen.add(((long) a.x()[i] << 32) ^ a.z()[i]), id + " two cells on one block");
                }
                for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                    Layout.Group gi = r.layout().groupOf(i), gj = r.layout().groupOf(j);
                    boolean ci = gi.role() == Layout.Role.PILLAR || gi.role() == Layout.Role.CHEST || r.height(i) > 1;
                    boolean cj = gj.role() == Layout.Role.PILLAR || gj.role() == Layout.Role.CHEST || r.height(j) > 1;
                    if (ci && cj) {
                        int dist = Math.max(Math.abs(a.x()[i] - a.x()[j]), Math.abs(a.z()[i] - a.z()[j]));
                        assertTrue(dist >= 2, id + ": solid cells " + i + " and " + j + " are adjacent");
                    }
                }
            }
        }
    }

    @Test
    void theFirstGroupRunsAlongXSoCompassArrowsAreNotRotated() {
        PuzzleRule r = PuzzleCatalog.create("gravity_maze", new Params(3, 1, 3));
        PuzzleGeometry.Arrangement a = PuzzleGeometry.arrange(r.layout(), 1L, false);
        Layout.Group maze = r.layout().groups().get(0);
        assertEquals(maze.cols(), maze.rows() == 0 ? 0 : maze.cols());
        for (int row = 0; row < maze.rows(); row++) for (int col = 1; col < maze.cols(); col++) {
            int i = row * maze.cols() + col;
            assertEquals(a.x()[i - 1] + maze.gap(), a.x()[i], "columns run along x");
            assertEquals(a.z()[i - 1], a.z()[i], "a row stays on one z");
        }
    }

    @Test
    void boardsGrowWithDepthAndPartyAndNeverShrink() {
        int grew = 0;
        for (String id : PuzzleCatalog.ids()) {
            double small = 0, big = 0;
            int n = 20;
            for (int s = 0; s < n; s++) {
                PuzzleRule a = PuzzleCatalog.create(id, new Params(50L + s, 1, 1)), b = PuzzleCatalog.create(id, new Params(50L + s, 8, 5));
                PuzzleSim sa = new PuzzleSim(a), sb = new PuzzleSim(b);
                assertTrue(sa.solve(900) && sb.solve(900), id);
                small += a.layout().total() + sa.steps + sa.t / 200.0;
                big += b.layout().total() + sb.steps + sb.t / 200.0;
            }
            assertTrue(big >= small * 0.98, id + ": work at tier 5 with a full party (" + big / n + ") is less than at tier 1 alone (" + small / n + ")");
            if (big > small * 1.05) grew++;
        }
        assertTrue(grew >= 22, "only " + grew + " puzzles get bigger with depth and party");
    }
}
