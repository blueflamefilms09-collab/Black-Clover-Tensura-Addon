package com.newuniverse.nusmp.heroweapon.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Why: every hero and vassal duration goes through Dur, so its clamp, its identity at 1.0 and its hard cap are what the whole setting rests on. */
class DurTest {
    @AfterEach
    void reset() { Dur.source(null); }

    @Test
    void identityAtOne() {
        for (int t : new int[] {1, 2, 7, 20, 40, 100, 399, 24000}) assertEquals(t, Dur.scale(t, 1.0));
    }

    @Test
    void scalesAndRounds() {
        assertEquals(200, Dur.scale(100, 2.0));
        assertEquals(50, Dur.scale(100, 0.5));
        assertEquals(3, Dur.scale(2, 1.25));
        assertEquals(1, Dur.scale(1, 0.25));
    }

    @Test
    void factorIsClamped() {
        assertEquals(Dur.scale(100, Dur.MAX), Dur.scale(100, 99.0));
        assertEquals(600, Dur.scale(100, 99.0));
        assertEquals(Dur.scale(100, Dur.MIN), Dur.scale(100, -3.0));
        assertEquals(25, Dur.scale(100, 0.0));
        assertEquals(100, Dur.scale(100, Double.NaN));
    }

    @Test
    void neverBelowOneTickOrAboveTwentyMinutes() {
        assertEquals(1, Dur.scale(1, Dur.MIN));
        assertEquals(24000, Dur.CAP);
        assertEquals(24000, Dur.scale(24000, 6.0));
        assertEquals(24000, Dur.scale(Integer.MAX_VALUE, 6.0));
        assertTrue(Dur.scale(20000, 6.0) <= Dur.CAP);
    }

    @Test
    void zeroAndNegativePassThrough() {
        assertEquals(0, Dur.scale(0, 2.0));
        assertEquals(-5, Dur.scale(-5, 2.0));
    }

    @Test
    void neutralUntilASourceIsInstalled() {
        Dur.source(null);
        assertEquals(1.0, Dur.factor());
        assertEquals(100, Dur.t(100));
        Dur.source(() -> 2.0);
        assertEquals(2.0, Dur.factor());
        assertEquals(200, Dur.t(100));
        Dur.source(() -> { throw new IllegalStateException("config not loaded"); });
        assertEquals(1.0, Dur.factor());
        Dur.source(() -> 50.0);
        assertEquals(Dur.MAX, Dur.factor());
    }

    @Test
    void taskIsHeldToFourHundred() {
        Dur.source(() -> 2.0);
        assertEquals(160, Dur.task(80));
        assertEquals(400, Dur.task(200));
        assertEquals(400, Dur.task(5000));
        assertEquals(400, Dur.t(200));
        Dur.source(() -> 1.0);
        assertEquals(200, Dur.task(200));
    }

    @Test
    void secondsScaleAndRoundUp() {
        Dur.source(() -> 2.0);
        assertEquals(8, Dur.seconds(4));
        assertEquals(2, Dur.seconds(1));
        Dur.source(() -> 0.25);
        assertEquals(1, Dur.seconds(1));
        Dur.source(() -> 1.0);
        assertEquals(3, Dur.seconds(3));
    }

    @Test
    void tooltipSeconds() {
        assertEquals("8", Dur.secs(160));
        assertEquals("2.5", Dur.secs(50));
        assertEquals("0.3", Dur.secs(5));
        assertEquals("1", Dur.secs(20));
    }
}
