package com.newuniverse.nusmp.labyrinth.mega.crucible;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Every catastrophe is survivable by running out of the vault", stated as assertions on the clocks: a warning of at least a second before the
 * first collapse pulse, pulses spread wide enough to cross the hall between two, a flood that shows a countdown and stops once nobody is in it, a
 * summoning circle before the spirit wakes, and a spirit that is let go when everybody left.
 */
class CatastropheRulesTest {
    @Test
    void theDefaultCollapseWarnsTwoSecondsThenPulsesEveryThirtyTicks() {
        int total = CatastropheRules.collapseTotalTicks(8);
        assertEquals(160, total);
        assertEquals(40, CatastropheRules.collapseWarnTicks(total));
        List<Integer> pulses = new ArrayList<>();
        for (int t = 0; t < total + 50; t++) if (CatastropheRules.collapsePulseAt(t, total)) pulses.add(t);
        assertEquals(List.of(40, 70, 100, 130), pulses);
        assertEquals(4, CatastropheRules.collapsePulseCount(total));
    }

    @Test
    void everyCollapseLengthWarnsAtLeastASecondAndHasAPulse() {
        for (int seconds = 0; seconds <= 60; seconds++) {
            int total = CatastropheRules.collapseTotalTicks(seconds);
            assertTrue(total >= 60, "never under three seconds: " + seconds);
            assertTrue(CatastropheRules.collapseWarnTicks(total) >= 20, "a warning of at least a second: " + seconds);
            assertTrue(CatastropheRules.collapsePulseCount(total) >= 1, "at least one pulse: " + seconds);
            int previous = -1000;
            for (int t = 0; t < total; t++) {
                if (!CatastropheRules.collapsePulseAt(t, total)) continue;
                assertTrue(t - previous >= CatastropheRules.COLLAPSE_PULSE_EVERY || previous < 0, "pulses are spaced");
                assertTrue(t >= CatastropheRules.collapseWarnTicks(total), "no pulse during the warning");
                previous = t;
            }
        }
    }

    @Test
    void noPulseAfterTheEnd() {
        assertFalse(CatastropheRules.collapsePulseAt(160, 160));
        assertFalse(CatastropheRules.collapsePulseAt(190, 160));
        assertFalse(CatastropheRules.collapsePulseAt(0, 160));
    }

    @Test
    void theFloodCountsDownAndRenewsOnceASecond() {
        int total = CatastropheRules.floodTotalTicks(30);
        assertEquals(600, total);
        assertEquals(30, CatastropheRules.floodSecondsLeft(0, total));
        assertEquals(30, CatastropheRules.floodSecondsLeft(1, total), "rounded up");
        assertEquals(29, CatastropheRules.floodSecondsLeft(20, total));
        assertEquals(1, CatastropheRules.floodSecondsLeft(581, total));
        assertEquals(0, CatastropheRules.floodSecondsLeft(600, total));
        assertEquals(0, CatastropheRules.floodSecondsLeft(900, total), "never negative");
        assertTrue(CatastropheRules.floodPulseAt(0));
        assertTrue(CatastropheRules.floodPulseAt(40));
        assertFalse(CatastropheRules.floodPulseAt(41));
        assertEquals(100, CatastropheRules.floodTotalTicks(1), "never under five seconds");
    }

    @Test
    void theFloodEndsEarlyOnlyAfterItsGraceAndOnlyWhenEmpty() {
        assertFalse(CatastropheRules.floodEndsEarly(0, 0), "the chooser has just turned round");
        assertFalse(CatastropheRules.floodEndsEarly(59, 0));
        assertTrue(CatastropheRules.floodEndsEarly(60, 0));
        assertTrue(CatastropheRules.floodEndsEarly(300, 0));
        assertFalse(CatastropheRules.floodEndsEarly(300, 1));
        assertFalse(CatastropheRules.floodEndsEarly(300, 4));
    }

    @Test
    void theSpiritWakesAfterTheSummoningCircle() {
        assertFalse(CatastropheRules.spiritAwake(0));
        assertFalse(CatastropheRules.spiritAwake(59));
        assertTrue(CatastropheRules.spiritAwake(60));
        assertEquals(60, CatastropheRules.SPIRIT_SUMMON_TICKS);
    }

    @Test
    void theSpiritIsLetGoTenMinutesAfterTheLastPlayerLeft() {
        assertFalse(CatastropheRules.spiritAbandoned(1000, 1000, 10));
        assertFalse(CatastropheRules.spiritAbandoned(1000 + 11999, 1000, 10));
        assertTrue(CatastropheRules.spiritAbandoned(1000 + 12000, 1000, 10));
        assertTrue(CatastropheRules.spiritAbandoned(1000 + 1200, 1000, 1));
        assertTrue(CatastropheRules.spiritAbandoned(5000, 1000, 0), "a minutes value below 1 is read as 1");
    }

    @Test
    void aMissingSpiritIsDeadOnlyWhenItsGroundIsLoaded() {
        assertFalse(CatastropheRules.spiritGone(true, true));
        assertFalse(CatastropheRules.spiritGone(true, false));
        assertTrue(CatastropheRules.spiritGone(false, true), "gone from loaded ground: it died or was removed");
        assertFalse(CatastropheRules.spiritGone(false, false), "an unloaded chunk may simply be asleep");
    }
}
