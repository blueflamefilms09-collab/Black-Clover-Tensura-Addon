package com.newuniverse.nusmp.butoh;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.butoh.DancePassive.Action.*;
import static org.junit.jupiter.api.Assertions.*;

/** The passive dance rules: on by default, off when mana or the body fails, back only above the restart share. */
class DancePassiveTest {
    private static final double R = DancePassive.DEFAULT_RESTART;

    @Test
    void startsByItselfWhenWantedAndPaid() {
        assertEquals(START, DancePassive.decide(false, true, false, 0.9, R));
    }

    @Test
    void doesNotStartBelowTheRestartShare() {
        assertEquals(NONE, DancePassive.decide(false, true, false, 0.14, R));
        assertEquals(START, DancePassive.decide(false, true, false, 0.15, R));
        assertEquals(NONE, DancePassive.decide(false, true, false, 0.0, 0.0));
    }

    @Test
    void aRunningDanceIsNotStoppedByALowShare() {
        assertEquals(NONE, DancePassive.decide(true, true, false, 0.01, R));
    }

    @Test
    void restingOrLosingTheSkillStopsIt() {
        assertEquals(STOP, DancePassive.decide(true, false, false, 1.0, R));
        assertEquals(NONE, DancePassive.decide(false, false, false, 1.0, R));
    }

    @Test
    void aBlockedBodyStopsAndKeepsItOffThenReArms() {
        assertEquals(STOP, DancePassive.decide(true, true, true, 1.0, R));
        assertEquals(NONE, DancePassive.decide(false, true, true, 1.0, R));
        assertEquals(START, DancePassive.decide(false, true, false, 1.0, R));
    }

    @Test
    void noFlickerAcrossTheThreshold() {
        // a dance that dropped at 5% waits; it comes back at 15% and then holds while the share falls again
        boolean on = false;
        int starts = 0;
        for (double share : new double[] {0.05, 0.10, 0.14, 0.16, 0.12, 0.08, 0.14}) {
            DancePassive.Action a = DancePassive.decide(on, true, false, share, R);
            if (a == START) { on = true; starts++; }
        }
        assertEquals(1, starts);
        assertTrue(on);
    }

    @Test
    void idleCostsHalfByDefaultAndMovingCostsAll() {
        assertEquals(0.5, DancePassive.drainFactor(true, 0.5), 1e-9);
        assertEquals(1.0, DancePassive.drainFactor(false, 0.5), 1e-9);
        assertEquals(1.0, DancePassive.drainFactor(true, 7.0), 1e-9);
        assertEquals(0.0, DancePassive.drainFactor(true, -1.0), 1e-9);
    }

    @Test
    void activityMakesTheDancerNotIdleForAWindow() {
        DancePassive.Activity a = new DancePassive.Activity();
        assertTrue(a.idle(100));
        a.observe(100, 0.04, false);
        assertFalse(a.idle(100));
        assertFalse(a.idle(100 + DancePassive.IDLE_WINDOW_TICKS - 1));
        assertTrue(a.idle(100 + DancePassive.IDLE_WINDOW_TICKS));
        a.observe(200, 0.0, true);                                        // a fight counts though the feet are still
        assertFalse(a.idle(205));
        a.observe(300, 0.0001, false);                                    // jitter is not movement
        assertTrue(a.idle(300));
    }
}
