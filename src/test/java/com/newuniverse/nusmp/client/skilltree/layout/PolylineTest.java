package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the dashed look of a sealed route and the "line fills toward the node" animation of a purchase are both arithmetic along a polyline, and both fail quietly (a dash that
 * restarts at every corner, a fill that overshoots the node, an off-by-one that leaves a one-pixel gap). The pieces are pinned on straight lines and on a line with a bend.
 */
class PolylineTest {
    private static final int[] L_SHAPE = {0, 0, 10, 0, 10, 6};            // 10 across then 6 down: length 16

    @Test
    void lengthAddsThePieces() {
        assertEquals(16, Polyline.length(L_SHAPE));
        assertEquals(0, Polyline.length(new int[]{3, 3, 3, 3}));
        assertEquals(0, Polyline.length(new int[]{3, 3}));
    }

    @Test
    void prefixCutsTheLineAtAFractionOfItsLength() {
        Asserts.ints(new int[]{0, 0, 0, 0}, Polyline.prefix(L_SHAPE, 0f));
        Asserts.ints(new int[]{0, 0, 8, 0}, Polyline.prefix(L_SHAPE, 0.5f));            // 8 of 16 along the first piece
        Asserts.ints(new int[]{0, 0, 10, 0, 10, 1}, Polyline.prefix(L_SHAPE, 0.6875f)); // 11 of 16: the whole first leg, then 1 down the second
    }

    @Test
    void prefixOfOneOrMoreIsTheWholeLineAndNeverLonger() {
        Asserts.ints(L_SHAPE, Polyline.prefix(L_SHAPE, 1f));
        Asserts.ints(L_SHAPE, Polyline.prefix(L_SHAPE, 7f));
        int[] copy = Polyline.prefix(L_SHAPE, 1f);
        copy[0] = 99;
        assertEquals(0, L_SHAPE[0], "a copy, not the input");
        for (float f = 0f; f <= 1.0001f; f += 0.05f) assertTrue(Polyline.length(Polyline.prefix(L_SHAPE, f)) <= 16);
    }

    @Test
    void prefixLengthMatchesTheFraction() {
        for (int percent = 0; percent <= 100; percent += 5) {
            int got = Polyline.length(Polyline.prefix(L_SHAPE, percent / 100f));
            assertEquals(Math.round(16 * percent / 100f), got, "at " + percent + "%");
        }
    }

    @Test
    void dashesOnAStraightLineFollowThePattern() {
        int[] line = {0, 0, 20, 0};
        Asserts.ints(new int[]{0, 0, 4, 0, 6, 0, 10, 0, 12, 0, 16, 0, 18, 0, 20, 0}, Polyline.dashes(line, 4, 2, 0));
    }

    @Test
    void thePhaseSlidesThePattern() {
        int[] line = {0, 0, 12, 0};
        Asserts.ints(new int[]{0, 0, 3, 0, 5, 0, 9, 0, 11, 0, 12, 0}, Polyline.dashes(line, 4, 2, 1));
        Asserts.ints(Polyline.dashes(line, 4, 2, 0), Polyline.dashes(line, 4, 2, 6), "a whole period changes nothing");
        Asserts.ints(Polyline.dashes(line, 4, 2, 0), Polyline.dashes(line, 4, 2, -6), "and neither does a negative whole period");
    }

    private static int dashLength(int[] dashes) {
        int drawn = 0;
        for (int i = 0; i + 3 < dashes.length; i += 4) drawn += Math.abs(dashes[i + 2] - dashes[i]) + Math.abs(dashes[i + 3] - dashes[i + 1]);
        return drawn;
    }

    @Test
    void thePatternContinuesAcrossABend() {
        // pattern positions 0-3 dash, 4-5 gap. The first leg is 10 long (dashes 0-4 and 6-10); the second leg starts at position 10, which is a gap
        // position, so its dash starts 2 pixels down (position 12) instead of restarting at the corner.
        int[] dashes = Polyline.dashes(L_SHAPE, 4, 2, 0);
        Asserts.ints(new int[]{0, 0, 4, 0, 6, 0, 10, 0, 10, 2, 10, 6}, dashes);
        assertEquals(12, dashLength(dashes));
    }

    @Test
    void aGapOfZeroIsASolidLineAndAnEmptyLineHasNoDashes() {
        assertEquals(16, dashLength(Polyline.dashes(L_SHAPE, 4, 0, 0)));
        assertEquals(0, Polyline.dashes(new int[]{5, 5, 5, 5}, 4, 2, 0).length);
    }
}
