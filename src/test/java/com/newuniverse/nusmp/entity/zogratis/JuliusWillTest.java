package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.entity.zogratis.JuliusWill.Event.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Two Souls scheduler: Julius surfaces once per threshold, for eight seconds, and the windows never run together. */
class JuliusWillTest {
    private static final int D = JuliusWill.DURATION;

    @Test
    void thresholdsAreOrderedAndSitBetweenTheStepsOfThePowerCurve() {
        float prev = 1f;
        for (float t : JuliusWill.THRESHOLDS) {
            assertTrue(t < prev, "descending");
            prev = t;
        }
        assertTrue(JuliusWill.THRESHOLDS.length >= 3);
        assertEquals(160, JuliusWill.DURATION, "eight seconds");
        assertTrue(JuliusWill.THRESHOLDS[JuliusWill.THRESHOLDS.length - 1] < 0.25f, "the last stand is in the Gates form");
    }

    @Test
    void nothingHappensWhileHealthy() {
        JuliusWill j = new JuliusWill();
        for (long t = 0; t < 1000; t += 7) assertEquals(NONE, j.update(1.0f, t, true));
        assertEquals(0, j.surfaced());
        assertFalse(j.active(500));
    }

    @Test
    void surfacesOnceAtTheFirstThresholdAndEndsEightSecondsLater() {
        JuliusWill j = new JuliusWill();
        assertEquals(NONE, j.update(0.76f, 100, true));
        assertEquals(SURFACE, j.update(0.74f, 110, true));
        assertTrue(j.active(111));
        assertEquals(D, j.left(110));
        assertEquals(0, j.lastIndex());
        assertEquals(NONE, j.update(0.74f, 200, true), "still open");
        assertEquals(D - 90, j.left(200));
        assertEquals(NONE, j.update(0.74f, 110 + D - 1, true));
        assertEquals(END, j.update(0.74f, 110 + D, true));
        assertFalse(j.active(110 + D));
        assertEquals(0, j.left(110 + D));
        assertEquals(NONE, j.update(0.74f, 110 + D + 1, true), "the same threshold does not open twice");
        assertEquals(1, j.surfaced());
    }

    @Test
    void healingBackUpDoesNotRearmAThreshold() {
        JuliusWill j = new JuliusWill();
        assertEquals(SURFACE, j.update(0.7f, 0, true));
        assertEquals(END, j.update(0.9f, D, true));
        assertEquals(NONE, j.update(0.9f, D + 500, true));
        assertEquals(NONE, j.update(0.7f, D + 600, true), "crossing it again does nothing");
        assertEquals(1, j.surfaced());
    }

    @Test
    void eachThresholdOpensItsOwnWindow() {
        JuliusWill j = new JuliusWill();
        long now = 0;
        float[] hp = {0.7f, 0.5f, 0.3f, 0.1f};
        for (int i = 0; i < hp.length; i++) {
            assertEquals(SURFACE, j.update(hp[i], now, true), "window " + i);
            assertEquals(i, j.lastIndex());
            now += D;
            assertEquals(END, j.update(hp[i], now, true));
            now += JuliusWill.PAUSE + 5;
        }
        assertEquals(4, j.surfaced());
        assertEquals(NONE, j.update(0.01f, now + 1000, true), "nothing left to open");
    }

    @Test
    void aHitThroughTwoThresholdsOpensOneWindowAndSpendsBoth() {
        JuliusWill j = new JuliusWill();
        assertEquals(SURFACE, j.update(0.5f, 0, true), "0.75 and 0.55 crossed at once");
        assertEquals(1, j.lastIndex(), "named by the lower one");
        assertEquals(END, j.update(0.5f, D, true));
        assertEquals(NONE, j.update(0.5f, D + 500, true), "the higher one was spent with it, not queued");
        assertEquals(SURFACE, j.update(0.3f, D + 600, true));
        assertEquals(2, j.lastIndex());
    }

    @Test
    void aThresholdCrossedInsideAWindowWaitsForTheEndAndThePause() {
        JuliusWill j = new JuliusWill();
        assertEquals(SURFACE, j.update(0.7f, 0, true));
        assertEquals(NONE, j.update(0.5f, 50, true), "burst down during the window: it waits");
        assertEquals(END, j.update(0.5f, D, true));
        assertEquals(NONE, j.update(0.5f, D + 10, true), "the pause");
        assertEquals(NONE, j.update(0.5f, D + JuliusWill.PAUSE - 1, true));
        assertEquals(SURFACE, j.update(0.5f, D + JuliusWill.PAUSE, true), "then the waiting window opens");
        assertEquals(1, j.lastIndex());
        assertEquals(2, j.surfaced());
    }

    @Test
    void windowsNeverOverlap() {
        JuliusWill j = new JuliusWill();
        boolean open = false;
        long lastEnd = -1000;
        for (long t = 0; t < 4000; t++) {
            float hp = Math.max(0.01f, 1f - t / 2500f);
            JuliusWill.Event e = j.update(hp, t, true);
            if (e == SURFACE) {
                assertFalse(open, "a window opened inside a window at " + t);
                assertTrue(t - lastEnd >= JuliusWill.PAUSE || lastEnd < 0, "pause kept at " + t);
                open = true;
            } else if (e == END) {
                assertTrue(open);
                open = false;
                lastEnd = t;
            }
            assertEquals(open, j.active(t), "state agrees with the events at " + t);
        }
        assertTrue(j.surfaced() >= 3);
    }

    @Test
    void staysShutWhileTheBossCannotTakeIt() {
        JuliusWill j = new JuliusWill();
        assertEquals(NONE, j.update(0.5f, 100, false), "the Gates' transformation");
        assertEquals(NONE, j.update(0.5f, 160, false));
        assertEquals(SURFACE, j.update(0.5f, 161, true), "opens once it can, for the lowest threshold crossed");
        assertEquals(1, j.lastIndex());
    }

    @Test
    void resetRearmsEverything() {
        JuliusWill j = new JuliusWill();
        j.update(0.05f, 0, true);
        j.reset();
        assertEquals(0, j.surfaced());
        assertEquals(-1, j.lastIndex());
        assertEquals(SURFACE, j.update(0.7f, 10, true));
        assertEquals(0, j.lastIndex());
    }

    @Test
    void restoreBringsBackTheArmedThresholdsAndTheOpenWindow() {
        JuliusWill j = new JuliusWill();
        j.update(0.7f, 0, true);
        int mask = j.armedMask();
        assertEquals(0b1110, mask, "the first threshold is spent");
        JuliusWill k = new JuliusWill();
        k.restore(mask, 60, 1000);
        assertTrue(k.active(1000));
        assertEquals(60, k.left(1000));
        assertEquals(END, k.update(0.7f, 1060, true));
        assertEquals(NONE, k.update(0.7f, 1100, true), "the first threshold stays spent after a reload");
        k.restore(mask, 0, 2000);
        assertFalse(k.active(2000));
    }

    @Test
    void fractionLeftRunsFromOneToZero() {
        JuliusWill j = new JuliusWill();
        j.update(0.7f, 100, true);
        assertEquals(1f, j.fractionLeft(100), 1e-6);
        assertEquals(0.5f, j.fractionLeft(100 + D / 2), 1e-6);
        assertEquals(0f, j.fractionLeft(100 + D), 1e-6);
    }
}
