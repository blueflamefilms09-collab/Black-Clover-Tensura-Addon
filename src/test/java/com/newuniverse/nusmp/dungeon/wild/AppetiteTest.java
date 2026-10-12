package com.newuniverse.nusmp.dungeon.wild;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The hunger clock of the Living Dungeon's wildlife: one point per 100 ticks, starvation at zero, no storm after an absence. */
class AppetiteTest {
    @Test
    void hungerFallsOnePerHundredTicks() {
        Appetite a = new Appetite(70);
        assertFalse(a.advance(1000L, false));
        assertEquals(70, a.hunger(), "the first look only starts the clock");
        a.advance(1099L, false);
        assertEquals(70, a.hunger());
        a.advance(1100L, false);
        assertEquals(69, a.hunger());
        a.advance(1500L, false);
        assertEquals(65, a.hunger());
    }

    @Test
    void aLegendaryCreatureBurnsThreeTimesSlower() {
        Appetite a = new Appetite(70);
        a.advance(0L, true);
        a.advance(299L, true);
        assertEquals(70, a.hunger());
        a.advance(300L, true);
        assertEquals(69, a.hunger());
    }

    @Test
    void anAbsenceDoesNotStarveItAtOnce() {
        Appetite a = new Appetite(50);
        a.advance(0L, false);
        a.advance(100L, false);
        assertEquals(49, a.hunger());
        a.advance(100L + Appetite.AWAY + 1, false);
        assertEquals(49, a.hunger(), "it was out of reach: the clock restarts");
        a.advance(100L + Appetite.AWAY + 101, false);
        assertEquals(48, a.hunger());
    }

    @Test
    void aClockThatRunsBackwardsRestarts() {
        Appetite a = new Appetite(50);
        a.advance(5000L, false);
        a.advance(100L, false);
        assertEquals(50, a.hunger());
        a.advance(200L, false);
        assertEquals(49, a.hunger());
    }

    @Test
    void starvationComesEveryFortyTicksAtZeroAndFeedingStopsIt() {
        Appetite a = new Appetite(0);
        assertFalse(a.advance(0L, false), "the first look at an empty stomach only starts the clock");
        assertFalse(a.advance(39L, false));
        assertTrue(a.advance(40L, false));
        assertFalse(a.advance(60L, false));
        assertTrue(a.advance(80L, false));
        a.feed(50);
        assertEquals(50, a.hunger());
        assertFalse(a.advance(120L, false));
        assertFalse(a.advance(160L, false));
        assertEquals(49, a.hunger(), "fed: it loses a point again only after a hundred ticks");
    }

    @Test
    void feedingIsCappedAndIgnoresNegatives() {
        Appetite a = new Appetite(90);
        a.feed(50);
        assertEquals(100, a.hunger());
        a.feed(-20);
        assertEquals(100, a.hunger());
        assertTrue(a.full());
        a.set(40);
        assertTrue(a.hungry());
        a.set(-4);
        assertEquals(0, a.hunger());
    }

    @Test
    void aVeryLongGapNeverTakesMoreThanTheWholeScale() {
        Appetite a = new Appetite(100);
        a.advance(0L, false);
        a.advance(Appetite.AWAY, false);
        assertEquals(90, a.hunger());
    }
}
