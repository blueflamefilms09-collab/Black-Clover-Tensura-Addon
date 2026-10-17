package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Specimen thread: every way it can end, the order they win in, and the geometry of "on the thread". */
class TetherTest {
    private static Tether.End run(Tether t, int ticks, boolean crossed, boolean blocked, boolean anti, boolean burst, double dist) {
        Tether.End e = Tether.End.HOLD;
        for (int i = 0; i < ticks; i++) e = t.tick(crossed, blocked, anti, burst, dist);
        return e;
    }

    @Test
    void aQuietThreadHoldsUntilItsLifeRunsOut() {
        Tether t = new Tether(160);
        assertEquals(Tether.End.HOLD, run(t, 159, false, false, false, false, 12));
        assertFalse(t.over());
        assertEquals(Tether.End.TIMEOUT, t.tick(false, false, false, false, 12));
        assertTrue(t.over());
        assertEquals(Tether.End.TIMEOUT, t.tick(true, true, true, true, 1), "an ended thread stays ended");
    }

    @Test
    void anAllyMustStandOnItForSixConsecutiveTicksToCutIt() {
        Tether t = new Tether(160);
        run(t, Tether.CUT_TICKS - 1, true, false, false, false, 12);
        assertFalse(t.over());
        t.tick(false, false, false, false, 12);                               // stepped off: the count starts over
        run(t, Tether.CUT_TICKS - 1, true, false, false, false, 12);
        assertFalse(t.over(), "five more is still not six in a row");
        assertEquals(Tether.End.CUT, t.tick(true, false, false, false, 12));
    }

    @Test
    void theTimingsAreTheOnesTheDocumentationPromises() {
        assertEquals(6, Tether.CUT_TICKS, "a third of a second standing on the thread");
        assertEquals(30, Tether.SIGHT_TICKS, "a second and a half without a line of sight");
        assertEquals(28.0, Tether.MAX_DISTANCE, 1e-9);
        assertEquals(2.4, Tether.REACH, 1e-9);
        assertEquals(1.15, TetherMath.THREAD_HALF_WIDTH, 1e-9);
    }

    @Test
    void aWallBetweenThemLosesItAfterThirtyTicks() {
        Tether t = new Tether(160);
        run(t, Tether.SIGHT_TICKS - 1, false, true, false, false, 12);
        assertFalse(t.over());
        assertEquals(Tether.End.LOST_SIGHT, t.tick(false, true, false, false, 12));
        Tether u = new Tether(160);
        run(u, 20, false, true, false, false, 12);
        run(u, 1, false, false, false, false, 12);                            // the line cleared once: the count starts over
        run(u, 20, false, true, false, false, 12);
        assertFalse(u.over());
    }

    @Test
    void antiMagicAndBurstEndItAtOnceAndAntiMagicWinsTheTie() {
        assertEquals(Tether.End.ANTI_MAGIC, new Tether(160).tick(false, false, true, false, 12));
        assertEquals(Tether.End.BURST, new Tether(160).tick(false, false, false, true, 12));
        assertEquals(Tether.End.ANTI_MAGIC, new Tether(160).tick(true, true, true, true, 1), "anti-magic outranks the rest, cut, burst, distance");
        assertEquals(Tether.End.BURST, new Tether(160).tick(true, true, false, true, 1));
    }

    @Test
    void distanceEndsItBothWays() {
        assertEquals(Tether.End.TOO_FAR, new Tether(160).tick(false, false, false, false, Tether.MAX_DISTANCE + 0.1));
        assertEquals(Tether.End.HOLD, new Tether(160).tick(false, false, false, false, Tether.MAX_DISTANCE));
        assertEquals(Tether.End.REACHED, new Tether(160).tick(false, false, false, false, Tether.REACH));
        assertEquals(Tether.End.HOLD, new Tether(160).tick(false, false, false, false, Tether.REACH + 0.1));
    }

    @Test
    void thePullGrowsButNeverBeatsWalking() {
        double last = 0;
        for (int age = 0; age <= 160; age += 8) {
            double p = TetherMath.pullSpeed(age, 160);
            assertTrue(p >= last - 1e-12, "never weaker later");
            assertTrue(p < TetherMath.WALK, "a specimen can always walk away: " + p);
            assertTrue(p <= TetherMath.MAX_PULL + 1e-12);
            last = p;
        }
        assertEquals(0.03, TetherMath.pullSpeed(0, 160), 1e-9);
        assertEquals(TetherMath.MAX_PULL, TetherMath.pullSpeed(160, 160), 1e-9);
        assertEquals(TetherMath.MAX_PULL, TetherMath.pullSpeed(9999, 160), 1e-9, "clamped");
        assertEquals(0.03, TetherMath.pullSpeed(-5, 160), 1e-9);
        Tether t = new Tether(100);
        assertEquals(0.03, t.pull(), 1e-9);
        run(t, 50, false, false, false, false, 12);
        assertTrue(t.pull() > 0.03 && t.pull() < TetherMath.MAX_PULL, "half way along: " + t.pull());
    }

    @Test
    void distanceToASegmentIsMeasuredToTheSegmentNotTheLine() {
        assertEquals(3.0, TetherMath.distToSegment(5, 3, 0, 0, 10, 0), 1e-9, "beside the middle");
        assertEquals(5.0, TetherMath.distToSegment(-3, 4, 0, 0, 10, 0), 1e-9, "past the start: the distance to the end point");
        assertEquals(5.0, TetherMath.distToSegment(13, 4, 0, 0, 10, 0), 1e-9, "past the far end");
        assertEquals(5.0, TetherMath.distToSegment(3, 4, 0, 0, 0, 0), 1e-9, "a segment of no length is a point");
        assertEquals(0.5, TetherMath.along(5, 9, 0, 0, 10, 0), 1e-9);
        assertEquals(0.0, TetherMath.along(-5, 0, 0, 0, 10, 0), 1e-9);
        assertEquals(1.0, TetherMath.along(50, 0, 0, 0, 10, 0), 1e-9);
    }

    @Test
    void onlyTheMiddleOfALongThreadCanBeStoodOnToCutIt() {
        // a thread along x from 0 to 12
        assertTrue(TetherMath.cuts(6, 0.5, 0, 0, 12, 0), "the middle, half a block off");
        assertFalse(TetherMath.cuts(6, 1.3, 0, 0, 12, 0), "wider than the thread");
        assertFalse(TetherMath.cuts(1.5, 0, 0, 0, 12, 0), "next to the owner is not on the thread");
        assertFalse(TetherMath.cuts(10.5, 0, 0, 0, 12, 0), "next to the specimen is not on the thread");
        assertTrue(TetherMath.cuts(2.0, 0, 0, 0, 12, 0), "two blocks in is");
        assertFalse(TetherMath.cuts(2.5, 0, 0, 0, 4, 0), "a thread shorter than 4.5 blocks has no middle to stand on");
    }
}
