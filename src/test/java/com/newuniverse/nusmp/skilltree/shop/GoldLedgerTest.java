package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the purse is a long in a save file that commands, sales and purchases all change, so the cases that wreck money code are pinned at the very edges of the number
 * line: a negative stored value, the largest possible argument, the smallest possible argument (whose negation overflows), and products that would wrap.
 */
class GoldLedgerTest {
    @Test
    void capIsAQuarterOfTheLargestLong() {
        assertEquals(Long.MAX_VALUE / 4, GoldLedger.CAP);
        assertTrue(GoldLedger.CAP + GoldLedger.CAP < Long.MAX_VALUE, "two capped purses still fit in a long");
    }

    @Test
    void clampTurnsBadStoredValuesIntoALegalPurse() {
        assertEquals(0, GoldLedger.clamp(-1));
        assertEquals(0, GoldLedger.clamp(Long.MIN_VALUE));
        assertEquals(5, GoldLedger.clamp(5));
        assertEquals(GoldLedger.CAP, GoldLedger.clamp(Long.MAX_VALUE));
        assertEquals(GoldLedger.CAP, GoldLedger.clamp(GoldLedger.CAP));
    }

    @Test
    void addSaturatesAtBothEnds() {
        assertEquals(150, GoldLedger.add(100, 50));
        assertEquals(50, GoldLedger.add(100, -50));
        assertEquals(0, GoldLedger.add(100, -100));
        assertEquals(0, GoldLedger.add(100, -101), "never below zero");
        assertEquals(0, GoldLedger.add(100, Long.MIN_VALUE), "the negation of the smallest long overflows; it must still just empty the purse");
        assertEquals(GoldLedger.CAP, GoldLedger.add(100, Long.MAX_VALUE), "never above the cap");
        assertEquals(GoldLedger.CAP, GoldLedger.add(GoldLedger.CAP, 1));
        assertEquals(GoldLedger.CAP, GoldLedger.add(GoldLedger.CAP - 5, 5));
        assertEquals(GoldLedger.CAP, GoldLedger.add(GoldLedger.CAP - 5, 6));
        assertEquals(0, GoldLedger.add(-7, 0), "a corrupted negative purse is read as empty");
        assertEquals(10, GoldLedger.add(-7, 10));
    }

    @Test
    void takeNeverGoesBelowZeroAndIgnoresNegativeAmounts() {
        assertEquals(70, GoldLedger.take(100, 30));
        assertEquals(0, GoldLedger.take(100, 100));
        assertEquals(0, GoldLedger.take(100, 1_000));
        assertEquals(100, GoldLedger.take(100, 0));
        assertEquals(100, GoldLedger.take(100, -30), "a negative price pays nothing and gives nothing");
        assertEquals(0, GoldLedger.take(100, Long.MAX_VALUE));
    }

    @Test
    void canAffordIsExactAndRefusesNegativePrices() {
        assertTrue(GoldLedger.canAfford(100, 100));
        assertTrue(GoldLedger.canAfford(100, 0));
        assertFalse(GoldLedger.canAfford(100, 101));
        assertFalse(GoldLedger.canAfford(100, -1));
        assertFalse(GoldLedger.canAfford(-5, 1), "a corrupted negative purse affords nothing");
        assertTrue(GoldLedger.canAfford(Long.MAX_VALUE, GoldLedger.CAP), "an out-of-range stored purse is clamped, not trusted");
    }

    @Test
    void mulSaturatesInsteadOfWrapping() {
        assertEquals(600, GoldLedger.mul(200, 3));
        assertEquals(0, GoldLedger.mul(0, 5));
        assertEquals(0, GoldLedger.mul(5, 0));
        assertEquals(0, GoldLedger.mul(-5, 3));
        assertEquals(GoldLedger.CAP, GoldLedger.mul(Long.MAX_VALUE, 2));
        assertEquals(GoldLedger.CAP, GoldLedger.mul(GoldLedger.CAP, 2));
        assertEquals(GoldLedger.CAP, GoldLedger.mul(GoldLedger.CAP / 2 + 1, 2));
        assertEquals(GoldLedger.CAP - GoldLedger.CAP % 2, GoldLedger.mul(GoldLedger.CAP / 2, 2));
    }

    @Test
    void roomAndMaxCountDecideHowMuchMayBeSold() {
        assertEquals(GoldLedger.CAP, GoldLedger.room(0));
        assertEquals(0, GoldLedger.room(GoldLedger.CAP));
        assertEquals(10, GoldLedger.maxCount(100, 1_050));
        assertEquals(0, GoldLedger.maxCount(100, 99));
        assertEquals(0, GoldLedger.maxCount(0, 1_000), "a worthless item never fits");
        assertEquals(0, GoldLedger.maxCount(100, -5));
    }
}
