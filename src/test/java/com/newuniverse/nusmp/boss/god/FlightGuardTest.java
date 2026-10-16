package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Anti-flight: grounding, the evidence-based restore, and the extra gravity curve. */
class FlightGuardTest {
    @Test
    void aFlyingPlayerIsGroundedOnTheFirstTick() {
        FlightGuard.Record r = new FlightGuard.Record();
        assertEquals(FlightGuard.Action.GROUND, r.tick(true, true, false, 100));
    }

    @Test
    void aPlayerWithoutFlightIsLeftAlone() {
        FlightGuard.Record r = new FlightGuard.Record();
        assertEquals(FlightGuard.Action.NONE, r.tick(false, false, false, 100));
        assertEquals(FlightGuard.Action.NONE, r.tick(false, false, false, 101));
        assertFalse(r.restore(500), "nothing to give back");
    }

    @Test
    void aSourceThatKeepsReassertingIsGroundedAgainAndRestoredOnLeaving() {
        FlightGuard.Record r = new FlightGuard.Record();
        r.tick(true, false, false, 0);
        for (long t = 20; t <= 2000; t += 20) {
            r.tick(false, false, false, t - 1);
            assertEquals(FlightGuard.Action.GROUND, r.tick(true, false, false, t), "re-asserted at " + t);
        }
        assertTrue(r.restore(2040), "the source was alive a moment ago");
    }

    @Test
    void aLongStayWhereTheSourceNeverCameBackIsNotRestored() {
        FlightGuard.Record r = new FlightGuard.Record();
        r.tick(true, false, false, 0);
        for (long t = 1; t < 1000; t++) r.tick(false, false, false, t);
        assertFalse(r.restore(1000), "a Djinn that timed out inside the arena must not become permanent flight");
    }

    @Test
    void aShortStayRestoresEvenWithoutAReassertion() {
        FlightGuard.Record r = new FlightGuard.Record();
        r.tick(true, false, false, 0);
        assertTrue(r.restore(150));
        assertFalse(r.restore(201));
    }

    @Test
    void aReassertionLongBeforeLeavingDoesNotCount() {
        FlightGuard.Record r = new FlightGuard.Record();
        r.tick(true, false, false, 0);
        r.tick(true, false, false, 300);
        assertFalse(r.restore(300 + FlightGuard.REGRANT_WINDOW + 1));
        assertTrue(r.restore(300 + FlightGuard.REGRANT_WINDOW));
    }

    @Test
    void inherentFlightIsAlwaysRestored() {
        FlightGuard.Record r = new FlightGuard.Record();
        r.tick(true, true, true, 0);
        assertTrue(r.restore(100000));
    }

    @Test
    void resetStartsOver() {
        FlightGuard.Record r = new FlightGuard.Record();
        r.tick(true, false, false, 0);
        r.reset();
        assertFalse(r.entered());
        assertFalse(r.restore(10));
        assertEquals(FlightGuard.Action.NONE, r.tick(false, false, false, 11));
    }

    @Test
    void extraGravityStartsAboveThreeBlocksAndStaysSmall() {
        assertEquals(0.0, FlightGuard.extraGravity(0.0));
        assertEquals(0.0, FlightGuard.extraGravity(3.0));
        assertTrue(FlightGuard.extraGravity(3.1) > 0.0);
        assertTrue(FlightGuard.extraGravity(6.0) > FlightGuard.extraGravity(4.0));
        assertEquals(0.12, FlightGuard.extraGravity(500.0), 1e-9);
        assertEquals(0.0, FlightGuard.extraGravity(Double.NaN));
    }
}
