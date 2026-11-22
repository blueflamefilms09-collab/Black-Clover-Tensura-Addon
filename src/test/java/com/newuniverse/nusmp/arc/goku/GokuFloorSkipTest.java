package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 4.102.0: the 1% floor he is held at between forms, and the smart skip against a very strong player. */
class GokuFloorSkipTest {
    @Test void floorIsOnePercentButNeverBelowOneHealth() {
        assertEquals(30f, GokuLadder.floorHp(3000f, 0.01), 1e-3f);
        assertEquals(1f, GokuLadder.floorHp(50f, 0.01), 1e-3f);        // 0.5 -> never below 1
        assertEquals(1f, GokuLadder.floorHp(3000f, 0.0), 1e-3f);       // 0 = the old 1 health
        assertEquals(30f, GokuLadder.floorHp(3000f, Double.NaN), 1e-3f);
        assertEquals(1500f, GokuLadder.floorHp(3000f, 9.0), 1e-3f);    // capped at half
    }

    @Test void noSkipWhenThePlayerIsNotAboveHim() {
        assertEquals(0, GokuLadder.skip(1e12, 2e12, 3));
        assertEquals(0, GokuLadder.skip(2e12, 2e12, 3));
        assertEquals(0, GokuLadder.skip(Double.NaN, 2e12, 3));
        assertEquals(0, GokuLadder.skip(1e15, 0, 3));
        assertEquals(0, GokuLadder.skip(1e15, 1e12, 0));               // smart off
    }

    @Test void oneSkipPerDoublingCappedAtTheSetting() {
        assertEquals(0, GokuLadder.skip(1.9e12, 1e12, 3));
        assertEquals(1, GokuLadder.skip(2e12, 1e12, 3));
        assertEquals(2, GokuLadder.skip(4.5e12, 1e12, 3));
        assertEquals(3, GokuLadder.skip(1e15, 1e12, 3));
        assertEquals(9, GokuLadder.skip(1e15, 1e12, 9));            // 1000x = 9.96 doublings, capped at 9
    }

    @Test void targetIsNextPlusSkipNeverAboveTopNeverDown() {
        assertEquals(1, GokuLadder.target(0, GokuLadder.CLASSIC_LAST, 1e12, 1e12, 3));
        assertEquals(4, GokuLadder.target(0, GokuLadder.CLASSIC_LAST, 1e15, 1e12, 3));
        assertEquals(9, GokuLadder.target(8, GokuLadder.CLASSIC_LAST, 1e18, 1e12, 3));
        assertEquals(9, GokuLadder.target(9, GokuLadder.CLASSIC_LAST, 1e18, 1e12, 3));
    }
}
