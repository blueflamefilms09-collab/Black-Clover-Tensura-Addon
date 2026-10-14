package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The counter of Dead End Rainbow: seven marks within the window close it; it never chains. */
class DeadEndRainbowTest {
    @Test
    void sevenMarksInsideTheWindowClose() {
        DeadEndRainbow<String> r = new DeadEndRainbow<>();
        long t = 0;
        for (int i = 1; i <= 6; i++) {
            DeadEndRainbow.Outcome o = r.hit("a", t);
            assertEquals(i, o.count());
            assertFalse(o.closes());
            t += 20;
        }
        DeadEndRainbow.Outcome last = r.hit("a", t);
        assertTrue(last.closes());
        assertEquals(7, last.count());
        assertEquals(0, r.count("a", t), "the stack is spent");
    }

    @Test
    void slowHitsNeverReachSeven() {
        DeadEndRainbow<String> r = new DeadEndRainbow<>();
        for (int i = 0; i < 100; i++) assertFalse(r.hit("a", i * 100L).closes(), "one blow every five seconds never stacks (window 12 s)");
        assertTrue(r.count("a", 9900) <= 3);
    }

    @Test
    void aFlurryOfOneMoveIsASingleMark() {
        DeadEndRainbow<String> r = new DeadEndRainbow<>();
        for (int i = 0; i < 40; i++) assertFalse(r.hit("a", 100 + i / 4).closes(), "forty hits within ten ticks");
        assertEquals(1, r.count("a", 110));
    }

    @Test
    void afterItClosesTheVictimIsSafeForTheCooldown() {
        DeadEndRainbow<String> r = new DeadEndRainbow<>();
        long closeAt = 0;
        for (int i = 0; i < 7; i++) { closeAt = i * 20L; r.hit("a", closeAt); }
        long safeUntil = closeAt + LuciusNumbers.RAINBOW_COOLDOWN_TICKS;
        assertTrue(r.safe("a", closeAt + 1));
        for (long t = closeAt + 1; t < safeUntil; t += 15) {
            DeadEndRainbow.Outcome o = r.hit("a", t);
            assertFalse(o.closes(), "no chain at " + t);
            assertEquals(0, o.count());
        }
        assertFalse(r.safe("a", safeUntil));
        assertEquals(1, r.hit("a", safeUntil).count(), "stacking starts over");
    }

    @Test
    void victimsAreIndependentAndTheBookIsBounded() {
        DeadEndRainbow<Integer> r = new DeadEndRainbow<>();
        for (int v = 0; v < 200; v++) r.hit(v, 0);
        assertTrue(r.size() <= LuciusNumbers.RAINBOW_TRACKED);
        r.clear();
        assertEquals(0, r.size());
        r.hit(1, 0);
        r.hit(2, 0);
        assertEquals(1, r.count(1, 0));
        assertEquals(1, r.count(2, 0));
        r.forget(1);
        assertEquals(0, r.count(1, 0));
        assertEquals(1, r.count(2, 0));
    }

    @Test
    void theNumbersAreTheOwnersSeven() {
        assertEquals(7, LuciusNumbers.RAINBOW_HITS);
        assertTrue(LuciusNumbers.RAINBOW_TELL_TICKS >= 8, "the closing is telegraphed for at least 8 ticks");
    }
}
