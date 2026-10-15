package com.newuniverse.nusmp.labyrinth.dnd.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the resource clock's balance (how long the dark is safe, how hunger scales) is checked as numbers: warning edge, recovery, wrap handling, no free hunger. */
class GloomClockTest {
    @Test void darkClimbsToWarningThenGloom() {
        GloomClock c = new GloomClock();
        GloomClock.State s = GloomClock.State.SAFE;
        int warnAt = -1, gloomAt = -1;
        for (int i = 1; i <= 20; i++) {
            s = c.step(true, 20);
            if (s == GloomClock.State.WARNING && warnAt < 0) warnAt = i;
            if (s == GloomClock.State.GLOOM && gloomAt < 0) gloomAt = i;
        }
        assertEquals(15, warnAt);
        assertEquals(20, gloomAt);
    }

    @Test void lightRecoversTwiceAsFast() {
        GloomClock c = new GloomClock();
        for (int i = 0; i < 10; i++) c.step(true, 20);
        assertEquals(10, c.darkSeconds());
        c.step(false, 20);
        assertEquals(8, c.darkSeconds());
        for (int i = 0; i < 10; i++) c.step(false, 20);
        assertEquals(0, c.darkSeconds());
        assertEquals(GloomClock.State.SAFE, c.step(false, 20));
    }

    @Test void darknessIsCappedSoTheWayBackIsBounded() {
        GloomClock c = new GloomClock();
        for (int i = 0; i < 500; i++) c.step(true, 20);
        assertEquals(40, c.darkSeconds());
        for (int i = 0; i < 10; i++) c.step(false, 20);
        assertEquals(20, c.darkSeconds());
        assertEquals(GloomClock.State.WARNING, c.step(false, 20));
        assertEquals(18, c.darkSeconds());
    }

    @Test void warningEdgeFiresOnce() {
        GloomClock c = new GloomClock();
        assertTrue(c.warningEdge(GloomClock.State.WARNING, GloomClock.State.SAFE));
        assertFalse(c.warningEdge(GloomClock.State.WARNING, GloomClock.State.WARNING));
        assertFalse(c.warningEdge(GloomClock.State.GLOOM, GloomClock.State.WARNING));
    }

    @Test void darkNeedsLowLightAndNoLightInHand() {
        assertTrue(GloomClock.isDark(4, 4, false));
        assertFalse(GloomClock.isDark(5, 4, false));
        assertFalse(GloomClock.isDark(0, 4, true));
    }

    @Test void hungerExtraScalesWithSpendingAndNeverInventsAny() {
        GloomClock c = new GloomClock();
        assertEquals(0f, c.extraExhaustion(1.0f, 1.5));
        assertEquals(0f, c.extraExhaustion(1.0f, 1.5));
        assertEquals(0.1f, c.extraExhaustion(1.2f, 1.5), 1e-4f);
        c.settle(1.3f);
        assertEquals(0f, c.extraExhaustion(1.3f, 1.5));
        assertEquals(0f, c.extraExhaustion(1.6f, 1.0), "drain 1.0 is vanilla");
    }

    @Test void hungerCounterWrapIsCounted() {
        GloomClock c = new GloomClock();
        c.extraExhaustion(3.8f, 2.0);
        assertEquals(0.4f, c.extraExhaustion(0.2f, 2.0), 1e-4f);
    }

    @Test void starvingAtThreeShanks() {
        assertTrue(GloomClock.starving(6));
        assertFalse(GloomClock.starving(7));
    }
}
