package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 11: the pure rules of the ten blades. */
class Wave11PureTest {
    @Test
    void theDayIsTwentyFourThousandTicks() {
        assertEquals(0, Wave11Pure.dayOf(0));
        assertEquals(0, Wave11Pure.dayOf(23999));
        assertEquals(1, Wave11Pure.dayOf(24000));
        assertEquals(0, Wave11Pure.dayOf(-5), "a negative time is day zero");
    }

    @Test
    void theTripolarTableIsTwoTwoTwo() {
        int lash = 0, plain = 0, fav = 0;
        for (int d = 1; d <= 6; d++) {
            switch (Wave11Pure.mood(d)) {
                case LASHING -> lash++;
                case PLAIN -> plain++;
                default -> fav++;
            }
        }
        assertEquals(2, lash);
        assertEquals(2, plain);
        assertEquals(2, fav);
        assertTrue(Wave11Pure.moodFactor(Wave11Pure.Mood.LASHING) < 1f);
        assertEquals(1f, Wave11Pure.moodFactor(Wave11Pure.Mood.PLAIN));
        assertTrue(Wave11Pure.moodFactor(Wave11Pure.Mood.FAVOURED) > 1f);
    }

    @Test
    void theLashNeverKills() {
        assertEquals(7f, Wave11Pure.lash(7, 20f));
        assertEquals(2f, Wave11Pure.lash(10, 3f));
        assertEquals(0f, Wave11Pure.lash(10, 1f));
        assertEquals(0f, Wave11Pure.lash(10, 0.2f));
    }

    @Test
    void theGeasFailsBelowFifty() {
        for (int d = 50; d <= 100; d++) assertEquals(Wave11Pure.Demand.NONE, Wave11Pure.geas(d));
        int n = 0, b = 0, f = 0;
        for (int d = 1; d < 50; d++) {
            switch (Wave11Pure.geas(d)) {
                case BLOODLUST -> b++;
                case FASTING -> f++;
                default -> n++;
            }
        }
        assertEquals(0, n);
        assertTrue(b > 0 && f > 0, "both demands come up");
        assertFalse(Wave11Pure.bloodlustOverdue(100, 0, false), "no deadline, no debt");
        assertFalse(Wave11Pure.bloodlustOverdue(100, 50, true), "satisfied");
        assertTrue(Wave11Pure.bloodlustOverdue(100, 50, false));
    }

    @Test
    void manyDaggersAreTwoToEightAndPlayersTakeFewer() {
        assertEquals(2, Wave11Pure.copies(1, 1));
        assertEquals(8, Wave11Pure.copies(4, 4));
        assertEquals(8, Wave11Pure.copies(9, 9), "dice are clamped");
        assertEquals(3, Wave11Pure.hitsAgainst(8, true));
        assertEquals(8, Wave11Pure.hitsAgainst(8, false));
        assertEquals(2, Wave11Pure.hitsAgainst(2, true));
        assertTrue(Wave11Pure.misses(3));
        assertFalse(Wave11Pure.misses(2));
    }

    @Test
    void theBreathTableHasEightKinds() {
        assertEquals("acid", Wave11Pure.breath(1));
        assertEquals("thunder", Wave11Pure.breath(8));
        assertEquals("acid", Wave11Pure.breath(0));
        assertEquals("thunder", Wave11Pure.breath(99));
        assertEquals(8, Wave11Pure.BREATH.length);
    }

    @Test
    void biancaHoldsSixCylindersOfThree() {
        assertEquals(18, Wave11Pure.DRUM);
        int d = Wave11Pure.DRUM, shots = 0;
        while (Wave11Pure.canVolley(d)) { d = Wave11Pure.afterVolley(d); shots++; }
        assertEquals(6, shots, "six volleys of three");
        assertEquals(0, d);
        assertEquals(2, Wave11Pure.afterVolley(2), "a part-empty drum fires a plain bolt");
        assertEquals(18, Wave11Pure.reloaded(0, 64));
        assertEquals(4, Wave11Pure.reloaded(14, 64));
        assertEquals(0, Wave11Pure.reloaded(18, 64));
        assertEquals(5, Wave11Pure.reloaded(0, 5));
        assertEquals(0, Wave11Pure.reloaded(-3, -3));
    }
}
