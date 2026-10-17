package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Udaeus's rib guard: blocked from one side for ever, cracked by flanks, broken by a few cracks. */
class RibGuardTest {
    @Test
    void theFirstBlowLandsInFullAndTheRibsTurnToIt() {
        RibGuard g = new RibGuard();
        RibGuard.Result r = g.onHit(0, 40.0, 20, true);
        assertEquals(RibGuard.Verdict.OPEN, r.verdict());
        assertEquals(1.0f, r.factor(), 1e-9);
        assertFalse(r.cracked());
        assertTrue(g.aimed());
        assertEquals(40.0, g.guardYaw(), 1e-9);
        // while they turn (14 ticks at the opening) everything lands in full, from anywhere
        assertEquals(RibGuard.Verdict.OPEN, g.onHit(5, 40.0, 20, true).verdict());
        assertEquals(RibGuard.Verdict.OPEN, g.onHit(13, -150.0, 20, true).verdict());
        assertEquals(0, g.cracks(), "no crack while the ribs turn");
    }

    @Test
    void oneSideIsBlockedForEver() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        for (long t = 20; t < 400; t += 7) {
            RibGuard.Result r = g.onHit(t, (t % 3 == 0 ? 30.0 : -30.0), 20, true);
            assertEquals(RibGuard.Verdict.BLOCKED, r.verdict(), "tick " + t);
            assertEquals(RibGuard.BLOCK_FACTOR, r.factor(), 1e-9);
        }
        assertEquals(0, g.cracks());
        assertFalse(g.staggered(401));
        assertEquals(RibGuard.Verdict.BLOCKED, g.onHit(500, 65.0, 20, true).verdict(), "right on the edge of the arc");
    }

    @Test
    void aFlankLandsInFullCracksARibAndTurnsTheGuard() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        RibGuard.Result r = g.onHit(30, 180.0, 20, true);
        assertEquals(RibGuard.Verdict.FLANK, r.verdict());
        assertEquals(1.0f, r.factor(), 1e-9);
        assertTrue(r.cracked());
        assertFalse(r.broke());
        assertEquals(1, g.cracks());
        assertEquals(180.0, g.guardYaw(), 1e-9);
        assertEquals(RibGuard.Verdict.OPEN, g.onHit(35, 0.0, 20, true).verdict(), "the old side is open while the ribs turn");
        assertEquals(RibGuard.Verdict.FLANK, g.onHit(50, 0.0, 20, true).verdict(), "and a flank again once they have settled on the new side");
        assertEquals(2, g.cracks());
    }

    @Test
    void twoFightersOnOppositeSidesBreakTheCageInAFewSeconds() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        long t = 20;
        boolean east = false;
        RibGuard.Result last = null;
        int blows = 0;
        while (blows < 40) {
            last = g.onHit(t, east ? 0.0 : 180.0, 20, true);
            east = !east;
            t += 16;                                              // a blow every 0.8 s, alternating sides
            blows++;
            if (last.broke()) break;
        }
        assertTrue(last.broke(), "the cage breaks");
        assertEquals(0, g.cracks(), "the count restarts after the break");
        assertTrue(blows <= 4, "three cracks need three flanks, at most four blows: " + blows);
        assertTrue(g.staggered(t));
        assertEquals(RibGuard.STAGGER_TICKS, g.staggerLeft(t - 16));
    }

    @Test
    void whileStaggeredEverythingHurtsMore() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        long t = 20;
        RibGuard.Result r;
        double[] yaws = {180, 0, 180};
        int i = 0;
        do {
            r = g.onHit(t, yaws[i++ % 3], 20, true);
            t += 16;
        } while (!r.broke());
        long broke = t - 16;
        assertEquals(RibGuard.Verdict.STAGGERED, g.onHit(broke + 10, 0.0, 20, true).verdict());
        assertEquals(RibGuard.STAGGER_FACTOR, g.onHit(broke + 10, 90.0, 20, true).factor(), 1e-9);
        assertEquals(RibGuard.Verdict.STAGGERED, g.onHit(broke + RibGuard.STAGGER_TICKS - 1, 0.0, 20, true).verdict());
        // the cage has mended: it begins again, facing nobody, and the first blow lands in full
        RibGuard.Result after = g.onHit(broke + RibGuard.STAGGER_TICKS + 1, 90.0, 20, true);
        assertEquals(RibGuard.Verdict.OPEN, after.verdict());
        assertEquals(0, g.cracks());
        assertFalse(g.staggered(broke + RibGuard.STAGGER_TICKS + 2));
    }

    @Test
    void crackFadeAfterAWhileWithoutAFlank() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        g.onHit(30, 180.0, 20, true);
        g.onHit(60, 0.0, 20, true);
        assertEquals(2, g.cracks());
        // a long quiet time, then one more flank: the old cracks are forgotten, this is the first
        RibGuard.Result r = g.onHit(60 + RibGuard.CRACK_MEMORY + 40, 180.0, 20, true);
        assertEquals(RibGuard.Verdict.FLANK, r.verdict());
        assertEquals(1, g.cracks());
        assertFalse(r.broke());
    }

    @Test
    void higherPowerReactsFasterAndTheGatesNeedAnotherCrack() {
        assertEquals(14, RibGuard.reaction(20));
        assertEquals(12, RibGuard.reaction(40));
        assertEquals(10, RibGuard.reaction(60));
        assertEquals(8, RibGuard.reaction(80));
        assertEquals(6, RibGuard.reaction(100));
        assertEquals(3, RibGuard.crackLimit(20));
        assertEquals(3, RibGuard.crackLimit(80));
        assertEquals(4, RibGuard.crackLimit(100));
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 100, true);
        long t = 10;
        double yaw = 180;
        int flanks = 0;
        while (true) {
            RibGuard.Result r = g.onHit(t, yaw, 100, true);
            yaw += 180;
            t += 8;
            if (r.cracked()) flanks++;
            if (r.broke()) break;
            assertTrue(t < 400, "it must break");
        }
        assertEquals(4, flanks, "four flanks at the Gates");
    }

    @Test
    void damageWithNoFighterBehindItIsNotBlockedAndCracksNothing() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        RibGuard.Result r = g.onHit(50, 0.0, 20, false);
        assertEquals(RibGuard.Verdict.OPEN, r.verdict());
        assertEquals(1.0f, r.factor(), 1e-9);
        assertEquals(0, g.cracks());
    }

    @Test
    void theCryShutsTheGuardWiderAndTighter() {
        RibGuard g = new RibGuard();
        g.onHit(0, 0.0, 20, true);
        assertEquals(RibGuard.Verdict.FLANK, g.onHit(30, 90.0, 20, true).verdict(), "90 degrees off is a flank when the guard is open");
        g.reset();
        g.onHit(0, 0.0, 20, true);
        g.close(10, 100);
        assertTrue(g.closed(50));
        RibGuard.Result shut = g.onHit(30, 90.0, 20, true);
        assertEquals(RibGuard.Verdict.BLOCKED, shut.verdict(), "closed, the arc is wider");
        assertEquals(RibGuard.CLOSED_FACTOR, shut.factor(), 1e-9);
        assertEquals(RibGuard.Verdict.FLANK, g.onHit(60, 180.0, 20, true).verdict(), "but the far side is still a flank: the counter is never gone");
        assertFalse(g.closed(111));
    }

    @Test
    void resetForgetsTheFight() {
        RibGuard g = new RibGuard();
        g.onHit(0, 10.0, 20, true);
        g.onHit(30, 190.0, 20, true);
        g.close(0, 500);
        g.reset();
        assertFalse(g.aimed());
        assertEquals(0, g.cracks());
        assertFalse(g.closed(1));
        assertFalse(g.staggered(1));
        assertEquals(RibGuard.Verdict.OPEN, g.onHit(5, 100.0, 20, true).verdict());
    }
}
