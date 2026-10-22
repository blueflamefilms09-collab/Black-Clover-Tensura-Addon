package com.newuniverse.nusmp.scalecast;

import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.VfxQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: rule 11 of CLAUDE.md (no lag on a public server): the Scale's level of detail, its behind-the-camera cull and its per-frame quad budget are pure rules, tested without the game.
 */
class ScaleLodTest {
    private static double sq(double d) { return d * d; }

    @Test
    void theLevelFollowsTheDistanceBands() {
        assertEquals(ScaleLod.FULL, ScaleLod.level(sq(4), 1f));
        assertEquals(ScaleLod.FULL, ScaleLod.level(sq(16), 1f));
        assertEquals(ScaleLod.MEDIUM, ScaleLod.level(sq(17), 1f));
        assertEquals(ScaleLod.MEDIUM, ScaleLod.level(sq(32), 1f));
        assertEquals(ScaleLod.LOW, ScaleLod.level(sq(33), 1f));
        assertEquals(ScaleLod.LOW, ScaleLod.level(sq(48), 1f));
        assertEquals(ScaleLod.CULLED, ScaleLod.level(sq(49), 1f));
        assertEquals(ScaleLod.CULLED, ScaleLod.level(Double.MAX_VALUE, 1f));
    }

    @Test
    void aWeakerMachineSwitchesEarlier() {
        assertEquals(ScaleLod.FULL, ScaleLod.level(sq(12), 1f));
        assertEquals(ScaleLod.FULL, ScaleLod.level(sq(12), 0.75f), "12 blocks is the edge of the 0.75 band");
        assertEquals(ScaleLod.MEDIUM, ScaleLod.level(sq(12.1), 0.75f));
        assertEquals(ScaleLod.LOW, ScaleLod.level(sq(17), 0.5f));
        assertEquals(ScaleLod.CULLED, ScaleLod.level(sq(25), 0.5f));
        assertEquals(ScaleLod.CULLED, ScaleLod.level(sq(37), 0.1f), "a silly detail is held to 0.25 of the bands");
        assertEquals(ScaleLod.FULL, ScaleLod.level(sq(16), 5f), "and never stretches them past the design");
    }

    @Test
    void aScaleWhollyBehindTheCameraIsSkipped() {
        assertTrue(ScaleLod.behind(-10));
        assertTrue(!ScaleLod.behind(-1));
        assertTrue(!ScaleLod.behind(5));
    }

    @Test
    void theFrameBudgetScalesWithTheQualityAndHasAFloor() {
        assertEquals(4000, ScaleLod.frameQuads(VfxQuality.HIGH));
        assertEquals(1400, ScaleLod.frameQuads(VfxQuality.MEDIUM));
        assertEquals(600, ScaleLod.frameQuads(VfxQuality.LOW));
    }

    @Test
    void aScaleThatDoesNotFitDropsALevelAndANearOneIsNeverLost() {
        int[] cost = ScaleLod.cost();
        assertTrue(cost[0] > cost[1] && cost[1] > cost[2] && cost[0] <= 1500);
        PerfBudget b = new PerfBudget("test", cost[0] + cost[1]);
        assertEquals(ScaleLod.FULL, ScaleLod.admit(b, 1, ScaleLod.FULL, sq(10), cost), "the first fits whole");
        assertEquals(ScaleLod.MEDIUM, ScaleLod.admit(b, 1, ScaleLod.FULL, sq(10), cost), "the second fits as medium");
        assertEquals(ScaleLod.LOW, ScaleLod.admit(new PerfBudget("tiny", cost[2]), 2, ScaleLod.FULL, sq(10), cost), "only the low one fits");
        PerfBudget none = new PerfBudget("none", 1);
        assertEquals(ScaleLod.CULLED, ScaleLod.admit(none, 3, ScaleLod.FULL, sq(20), cost), "far and nothing fits: culled");
        assertEquals(ScaleLod.LOW, ScaleLod.admit(none, 3, ScaleLod.FULL, sq(3), cost), "near and nothing fits: still drawn, low");
        assertEquals(ScaleLod.CULLED, ScaleLod.admit(b, 4, ScaleLod.CULLED, sq(1), cost), "a culled one stays culled");
        PerfBudget next = new PerfBudget("roll", cost[0]);
        assertEquals(ScaleLod.FULL, ScaleLod.admit(next, 5, ScaleLod.FULL, sq(10), cost));
        assertEquals(ScaleLod.CULLED, ScaleLod.admit(next, 5, ScaleLod.FULL, sq(30), cost), "the budget is spent this frame");
        assertEquals(ScaleLod.FULL, ScaleLod.admit(next, 6, ScaleLod.FULL, sq(10), cost), "and refills the next one");
    }

    @Test
    void theWorstCaseOfAFullFrameIsBoundedByTheBudgetNotByTheCasterCount() {
        int[] cost = ScaleLod.cost();
        PerfBudget b = new PerfBudget("frame", ScaleLod.frameQuads(VfxQuality.HIGH));
        int full = 0, drawn = 0;
        for (int i = 0; i < 200; i++) {                                                          // two hundred casters in view, all far enough to be culled when the budget is gone
            int l = ScaleLod.admit(b, 1, ScaleLod.FULL, sq(10 + i % 5), cost);
            if (l == ScaleLod.FULL) full++;
            if (l != ScaleLod.CULLED) drawn += cost[l];
        }
        assertTrue(drawn <= ScaleLod.frameQuads(VfxQuality.HIGH), "never more than the frame budget: " + drawn);
        assertTrue(full <= ScaleLod.frameQuads(VfxQuality.HIGH) / cost[0], "so at most " + ScaleLod.frameQuads(VfxQuality.HIGH) / cost[0] + " full ones: " + full);
    }
}
