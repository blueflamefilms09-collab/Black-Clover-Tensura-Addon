package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the line router is the only thing that decides what the connections of a tree look like, and the rules it promises (one bend when a clean one-bend path exists, the exit and
 * entry on the facing edges, never through an unrelated node when a clean path exists, always axis-aligned) are exactly what a hand-made layout would silently break. These cases pin
 * each promise, including the two fallbacks, and a sweep over a whole grid of positions checks the promises hold for every pair and not only for the cases someone thought of.
 */
class LineRouterTest {
    private static final int H = 11;                                      // half a node box

    private static int[] r(int px, int py, int cx, int cy, List<int[]> obstacles) { return LineRouter.route(px, py, cx, cy, H, obstacles); }

    private static int[] box(int cx, int cy) { return new int[]{cx - H, cy - H, cx + H, cy + H}; }

    @Test
    void sameRowIsOneStraightHorizontalPiece() {
        Asserts.ints(new int[]{100 + H, 50, 200 - H, 50}, r(100, 50, 200, 50, List.of()));
        Asserts.ints(new int[]{200 - H, 50, 100 + H, 50}, r(200, 50, 100, 50, List.of()), "a child to the left leaves by the left edge");
    }

    @Test
    void sameColumnIsOneStraightVerticalPiece() {
        Asserts.ints(new int[]{60, 40 + H, 60, 120 - H}, r(60, 40, 60, 120, List.of()));
        Asserts.ints(new int[]{60, 120 - H, 60, 40 + H}, r(60, 120, 60, 40, List.of()));
    }

    @Test
    void differentRowAndColumnIsOneBendHorizontalFirst() {
        int[] p = r(50, 100, 130, 60, List.of());                         // child up and to the right
        Asserts.ints(new int[]{50 + H, 100, 130, 100, 130, 60 + H}, p, "out of the right edge, along the row, up into the child's bottom");
        int[] q = r(50, 100, 130, 140, List.of());                        // child down and to the right
        Asserts.ints(new int[]{50 + H, 100, 130, 100, 130, 140 - H}, q);
        int[] left = r(130, 100, 50, 140, List.of());                     // child down and to the left
        Asserts.ints(new int[]{130 - H, 100, 50, 100, 50, 140 - H}, left);
    }

    @Test
    void anObstacleOnTheHorizontalRunMakesItBendVerticalFirst() {
        List<int[]> obstacles = List.of(box(90, 100));                    // a node sitting on the parent's row, between parent and child
        int[] p = r(50, 100, 130, 60, obstacles);
        Asserts.ints(new int[]{50, 100 - H, 50, 60, 130 - H, 60}, p, "up out of the top edge, along the child's row, into the child's left edge");
        assertTrue(LineRouter.clear(p, obstacles));
    }

    @Test
    void whenBothSingleBendShapesAreBlockedItGoesThroughTheGapBetweenTheColumns() {
        List<int[]> obstacles = List.of(box(110, 100), box(70, 60));      // one on the far end of each single-bend path, so the middle column stays free
        int[] p = r(50, 100, 130, 60, obstacles);
        assertEquals(8, p.length, "three pieces");
        assertEquals(p[2], p[4], "the middle piece is vertical");
        assertTrue(p[2] > 50 + H && p[2] < 130 - H, "and sits between the two boxes");
        assertTrue(LineRouter.clear(p, obstacles));
    }

    @Test
    void whenNothingIsClearItKeepsTheFirstChoiceInsteadOfFailing() {
        List<int[]> wall = List.of(new int[]{-1000, -1000, 1000, 1000});
        Asserts.ints(new int[]{50 + H, 100, 130, 100, 130, 60 + H}, r(50, 100, 130, 60, wall));
    }

    @Test
    void theSameCellGivesAPointNotAnException() {
        Asserts.ints(new int[]{70, 70, 70, 70}, r(70, 70, 70, 70, List.of()));
    }

    @Test
    void everyPairOnAGridGivesAnAxisAlignedLineWithoutZeroLengthPieces() {
        List<int[]> centres = new ArrayList<>();
        for (int gx = 0; gx < 4; gx++) for (int gy = 0; gy < 4; gy++) centres.add(new int[]{23 + gx * TreeLayout.CELL_X, 23 + gy * TreeLayout.CELL_Y});
        for (int[] a : centres) {
            for (int[] b : centres) {
                if (a == b) continue;
                List<int[]> others = new ArrayList<>();
                for (int[] c : centres) if (c != a && c != b) others.add(box(c[0], c[1]));
                int[] p = r(a[0], a[1], b[0], b[1], others);
                for (int i = 0; i + 3 < p.length; i += 2) {
                    boolean horizontal = p[i + 1] == p[i + 3], vertical = p[i] == p[i + 2];
                    assertTrue(horizontal || vertical, "axis-aligned");
                    assertFalse(horizontal && vertical, "no zero-length piece between " + a[0] + "," + a[1] + " and " + b[0] + "," + b[1]);
                }
                Rect from = new Rect(a[0] - H - 1, a[1] - H - 1, 2 * H + 2, 2 * H + 2), to = new Rect(b[0] - H - 1, b[1] - H - 1, 2 * H + 2, 2 * H + 2);
                assertTrue(from.contains(p[0], p[1]), "starts on the parent's box");
                assertTrue(to.contains(p[p.length - 2], p[p.length - 1]), "ends on the child's box");
            }
        }
    }
}
