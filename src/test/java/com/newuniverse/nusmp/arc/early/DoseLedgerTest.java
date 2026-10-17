package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Lotus's slow burn is only fair if the number is readable and can always be lowered: the ladder, the cap of his gaze, and the way down are fixed here. */
class DoseLedgerTest {
    private final UUID a = new UUID(1, 1), b = new UUID(1, 2);

    @Test
    void theDoseClimbsAndFallsBetweenZeroAndTen() {
        DoseLedger l = new DoseLedger();
        assertEquals(0, l.dose(a));
        assertEquals(3, l.add(a, 3));
        assertEquals(10, l.add(a, 50), "never over ten");
        assertEquals(9, l.add(a, -1));
        assertEquals(0, l.add(a, -50), "never under zero");
        assertTrue(l.ids().isEmpty(), "a clean foe is not kept on record");
        assertEquals(5, l.set(b, 5));
        assertEquals(2, l.set(b, 2));
        l.forget(b);
        assertEquals(0, l.dose(b));
        l.add(a, 4);
        l.clear();
        assertEquals(0, l.dose(a));
    }

    @Test
    void theLadderIsFixed() {
        for (int d = 0; d <= 2; d++) assertEquals(-1, DoseLedger.weakness(d), "no weakness under 3");
        assertEquals(0, DoseLedger.weakness(3));
        assertEquals(0, DoseLedger.weakness(5));
        assertEquals(1, DoseLedger.weakness(6));
        assertEquals(1, DoseLedger.weakness(10));
        for (int d = 0; d <= 5; d++) assertEquals(-1, DoseLedger.slowness(d), "no slowness under 6");
        assertEquals(0, DoseLedger.slowness(6));
        assertEquals(0, DoseLedger.slowness(7));
        assertEquals(1, DoseLedger.slowness(8));
        assertFalse(DoseLedger.falls(9));
        assertTrue(DoseLedger.falls(10));
        assertTrue(DoseLedger.AFTER_FALL < DoseLedger.MAX && DoseLedger.AFTER_FALL >= 3, "the stumble leaves the foe weakened but not at the brink");
    }

    @Test
    void theBloomSpendsTheDose() {
        assertEquals(0f, DoseLedger.bloom(2, 2.2f), "a trace of smoke is not worth a bloom");
        assertEquals(6.6f, DoseLedger.bloom(3, 2.2f), 1e-4f);
        assertEquals(22f, DoseLedger.bloom(10, 2.2f), 1e-4f);
    }

    @Test
    void aCloudClimbsToTheTopAndNeverPastIt() {
        int d = 0;
        for (int sec = 0; sec < 40; sec++) d = DoseLedger.step(d, DoseLedger.MAX, false, false, false);
        assertEquals(DoseLedger.MAX, d);
        d = 0;
        for (int sec = 0; sec < 30; sec++) {
            d = DoseLedger.step(d, 1, true, sec % 5 == 0, sec % 3 == 0);
            assertTrue(d >= 0 && d <= DoseLedger.MAX);
        }
        assertEquals(10, d, "a garden gives a dose a second, so ten seconds in it is enough");
        assertEquals(DoseLedger.MAX, DoseLedger.step(9, 3, false, false, false), "a bloom adds three and stops at ten");
    }

    @Test
    void hisGazeAloneTakesAFoeToTheCapAndHoldsThere() {
        int d = 0;
        int firstWeak = -1;
        for (int sec = 0; sec < 120; sec++) {
            d = DoseLedger.step(d, 0, true, sec % 5 == 0, sec % 3 == 0);
            assertTrue(d <= DoseLedger.SIGHT_CAP, "his gaze alone never gives more than the cap, at second " + sec);
            if (firstWeak < 0 && DoseLedger.weakness(d) >= 0) firstWeak = sec;
        }
        assertEquals(DoseLedger.SIGHT_CAP, d, "a foe he watches for two minutes is at the cap");
        assertTrue(firstWeak > 0 && firstWeak <= 15, "the weakness arrives within a quarter of a minute of being watched, at second " + firstWeak);
        assertEquals(-1, DoseLedger.slowness(d), "the cap is under the slowness step: the gaze weakens, it never slows");
        assertTrue(DoseLedger.weakness(d) >= 0, "the cap is over the weakness step: the gaze does weaken");
    }

    @Test
    void breakingHisLineOfSightIsTheCounterAndItWorks() {
        int d = DoseLedger.SIGHT_CAP;
        for (int sec = 0; sec < 15; sec++) d = DoseLedger.step(d, 0, false, sec % 5 == 0, sec % 3 == 0);
        assertEquals(0, d, "out of his sight and out of the smoke the dose is gone in fifteen seconds");
        int e = 9;
        for (int sec = 0; sec < 30; sec++) e = DoseLedger.step(e, 0, true, sec % 5 == 0, sec % 3 == 0);
        assertEquals(DoseLedger.SIGHT_CAP, e, "a foe who left the smoke but not his eye sinks back to the cap, no further");
    }

    @Test
    void theStepNeverLeavesTheLadder() {
        for (int d = -3; d <= 14; d++) {
            for (int cloud = 0; cloud <= 3; cloud++) {
                for (int mask = 0; mask < 8; mask++) {
                    int n = DoseLedger.step(d, cloud, (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
                    assertTrue(n >= 0 && n <= DoseLedger.MAX, "dose " + d + " cloud " + cloud + " mask " + mask + " gave " + n);
                }
            }
        }
        assertEquals(0, DoseLedger.step(0, 0, false, true, true), "nothing to lose, nothing gained out of sight");
        assertEquals(1, DoseLedger.step(0, 0, true, true, true), "his aura beat in his sight, even on a decay beat");
        assertEquals(5, DoseLedger.step(5, 0, true, true, true), "at the cap the gaze holds it");
    }
}
