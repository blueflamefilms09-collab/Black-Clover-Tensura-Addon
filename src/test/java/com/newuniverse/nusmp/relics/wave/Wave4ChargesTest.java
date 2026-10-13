package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 4: the charge pools of the Arcane Glass of Seeing and the Pendant of the Dreamer. */
class Wave4ChargesTest {
    @Test
    void dayIndexTurnsOverAtDawnAndAtMidnight() {
        // game time 0 is the morning (tick 0 of day 0); dawn is tick 23000, midnight tick 18000 of the day
        assertEquals(0, Wave4Charges.dayIndex(0, Wave4Charges.DAWN_OFFSET));
        assertEquals(0, Wave4Charges.dayIndex(22999, Wave4Charges.DAWN_OFFSET));
        assertEquals(1, Wave4Charges.dayIndex(23000, Wave4Charges.DAWN_OFFSET), "the day turns at dawn");
        assertEquals(1, Wave4Charges.dayIndex(24000 + 22999, Wave4Charges.DAWN_OFFSET));
        assertEquals(0, Wave4Charges.dayIndex(17999, Wave4Charges.MIDNIGHT_OFFSET));
        assertEquals(1, Wave4Charges.dayIndex(18000, Wave4Charges.MIDNIGHT_OFFSET), "the day turns at midnight");
        assertEquals(2, Wave4Charges.dayIndex(24000 + 18000, Wave4Charges.MIDNIGHT_OFFSET));
    }

    @Test
    void aFreshAmuletIsFullAndDoesNotOweDays() {
        Wave4Charges.State s = Wave4Charges.fresh(6, 10);
        assertEquals(6, s.charges());
        assertEquals(s, Wave4Charges.refill(s, 10, 6, 6), "same day: nothing owed");
        assertEquals(6, Wave4Charges.refill(s, 11, 6, 4).charges(), "never above the maximum");
    }

    @Test
    void recoveryComesOncePerNewDayAndRespectsTheCap() {
        Wave4Charges.State s = new Wave4Charges.State(1, 5, false);
        Wave4Charges.State a = Wave4Charges.refill(s, 6, 6, 4);
        assertEquals(5, a.charges());
        assertEquals(6, a.day());
        assertEquals(a, Wave4Charges.refill(a, 6, 6, 4), "looking again the same day gives nothing more");
        assertEquals(6, Wave4Charges.refill(a, 7, 6, 4).charges(), "capped at 6");
        // many days away still gives one recovery, not one per day (the rest happens at the next dawn)
        assertEquals(5, Wave4Charges.refill(s, 50, 6, 4).charges());
    }

    @Test
    void aCrackedGemSkipsOneRecoveryOnly() {
        Wave4Charges.State s = Wave4Charges.crack(new Wave4Charges.State(0, 3, false));
        assertTrue(s.cracked());
        Wave4Charges.State a = Wave4Charges.refill(s, 4, 6, 5);
        assertEquals(0, a.charges(), "the cracked dawn gives nothing");
        assertFalse(a.cracked());
        assertEquals(5, Wave4Charges.refill(a, 5, 6, 5).charges(), "the next dawn works again");
    }

    @Test
    void aClockSetBackDoesNotOweDays() {
        Wave4Charges.State s = new Wave4Charges.State(2, 100, false);
        Wave4Charges.State a = Wave4Charges.refill(s, 4, 6, 6);
        assertEquals(2, a.charges());
        assertEquals(4, a.day());
    }

    @Test
    void spendingAndTheLastChargeCrackRoll() {
        Wave4Charges.State s = new Wave4Charges.State(3, 0, false);
        assertTrue(Wave4Charges.canSpend(s, 3));
        assertFalse(Wave4Charges.canSpend(s, 4));
        assertFalse(Wave4Charges.canSpend(s, 0));
        assertTrue(Wave4Charges.emptiesOn(s, 3));
        assertFalse(Wave4Charges.emptiesOn(s, 2));
        assertEquals(0, Wave4Charges.spend(s, 3).charges());
        assertEquals(0, Wave4Charges.spend(s, 9).charges(), "never negative");
        assertTrue(Wave4Charges.cracks(1));
        for (int d = 2; d <= 20; d++) assertFalse(Wave4Charges.cracks(d));
        assertFalse(Wave4Charges.emptiesOn(new Wave4Charges.State(0, 0, false), 1), "an empty amulet cannot be emptied again");
    }

    @Test
    void recoveryDiceStayInRange() {
        for (int d = -2; d <= 9; d++) {
            int g = Wave4Charges.glassRecovery(d);
            assertTrue(g >= 3 && g <= 6, "1d4+2 is 3 to 6, got " + g);
            int m = Wave4Charges.dreamerRecovery(d);
            assertTrue(m >= 1 && m <= 6, "1d6 is 1 to 6, got " + m);
        }
        assertEquals(3, Wave4Charges.glassRecovery(1));
        assertEquals(6, Wave4Charges.glassRecovery(4));
    }

    @Test
    void oncePerSpanReadiness() {
        assertTrue(Wave4Charges.ready(-1, 0, 24000), "never used");
        assertFalse(Wave4Charges.ready(100, 200, 24000));
        assertTrue(Wave4Charges.ready(100, 24100, 24000));
        assertFalse(Wave4Charges.ready(100, 24099, 24000));
        assertTrue(Wave4Charges.ready(5000, 100, 24000), "a stamp from the future (a restored world) is void");
        assertEquals(1, Wave4Charges.minutesLeft(0, 23999, 24000));
        assertEquals(20, Wave4Charges.minutesLeft(0, 0, 24000));
    }

    @Test
    void theMindBlastIsBoundedAndHalvedBySave() {
        float low = Wave4Charges.mindBlast(1, 1, 1, false, false);
        float high = Wave4Charges.mindBlast(6, 6, 6, false, false);
        assertEquals(3.0, low, 1e-4);
        assertEquals(12.0, high, 1e-4);
        assertEquals(6.0, Wave4Charges.mindBlast(6, 6, 6, true, false), 1e-4);
        assertEquals(6.0, Wave4Charges.mindBlast(6, 6, 6, false, true), 1e-4, "a player takes at most 6");
        assertEquals(3.0, Wave4Charges.mindBlast(1, 1, 1, false, true), 1e-4);
        assertEquals(Wave4Charges.mindBlast(1, 1, 1, false, false), Wave4Charges.mindBlast(-5, 0, 1, false, false), 1e-4, "dice are clamped to 1..6");
    }
}
