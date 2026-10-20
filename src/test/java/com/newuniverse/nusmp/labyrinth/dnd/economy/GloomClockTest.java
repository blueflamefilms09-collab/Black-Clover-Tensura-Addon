package com.newuniverse.nusmp.labyrinth.dnd.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the resource clock's balance (how long the dark is safe) is checked as numbers: warning edge, recovery, the cap; and since 1.99.0 that the hunger half stays removed. */
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

    /** 1.99.0, the hunger ban: the clock has no hunger meter any more (no exhaustion multiplier, no "starving" line); only the gloom is left. */
    @Test void theHungerHalfIsGoneForGood() {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (java.lang.reflect.Method m : GloomClock.class.getDeclaredMethods()) names.add(m.getName());
        for (String banned : new String[]{"extraExhaustion", "settle", "starving"}) assertFalse(names.contains(banned), banned + " must not come back");
        for (java.lang.reflect.Field f : GloomClock.class.getDeclaredFields()) assertFalse(f.getName().toLowerCase().contains("exhaust"), f.getName());
    }
}
