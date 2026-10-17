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
    void aStoneIsMarkedLongEnoughToStepAsideAndLandsInsideTheCollapse() {
        assertTrue(CatastropheRules.DEBRIS_TELEGRAPH_TICKS >= 10, "at least half a second to see it and move");
        assertTrue(CatastropheRules.DEBRIS_TELEGRAPH_TICKS <= 20, "but a scare, not a pause");
        assertEquals(54, CatastropheRules.debrisLandsAt(40));
        // a walking person (4.3 blocks a second) covers more than the stone's radius while it is marked: the aimed spot is escapable
        double walked = 4.3 * CatastropheRules.DEBRIS_TELEGRAPH_TICKS / 20.0;
        assertTrue(walked > CatastropheRules.DEBRIS_RADIUS + 1.0, "walked " + walked + " blocks, the stone's radius is " + CatastropheRules.DEBRIS_RADIUS);
        // the pulses are spaced by more than the time a stone takes, so two aimed stones never fall on the same person at once
        assertTrue(CatastropheRules.COLLAPSE_PULSE_EVERY > CatastropheRules.DEBRIS_TELEGRAPH_TICKS);
    }

    @Test
    void aStoneHurtsOnlyWhoStaysWithinItsRadius() {
        assertTrue(CatastropheRules.debrisHits(0, 0, 0));
        assertTrue(CatastropheRules.debrisHits(1.7, 0, 0));
        assertTrue(CatastropheRules.debrisHits(1.2, 2.9, 1.2));
        assertFalse(CatastropheRules.debrisHits(1.9, 0, 0), "two steps away is out of it");
        assertFalse(CatastropheRules.debrisHits(1.3, 0, 1.3), "the radius is round: the corner of a square is out");
        assertFalse(CatastropheRules.debrisHits(0, 3.1, 0), "a person on a gallery above or a floor below is not under it");
        assertFalse(CatastropheRules.debrisHits(0, -3.1, 0));
    }

    @Test
    void theCollapseNeverTakesMoreThanItsBudgetWhateverLandsOnAPerson() {
        float max = 20f, taken = 0f;
        int stones = 0;
        while (true) {
            float allowance = CatastropheRules.collapseAllowance(max, taken);
            assertTrue(allowance >= 0f, "never negative");
            if (allowance <= 0f) break;
            taken += Math.min(allowance, 7f);                                       // a heavy stone each time
            stones++;
            assertTrue(stones < 20, "the budget runs out");
        }
        assertEquals(12f, taken, 0.001f, "60 % of 20 hearts-halves: 12 health");
        assertTrue(taken < max, "a person who stood in every stone is still alive");
        assertEquals(0f, CatastropheRules.collapseAllowance(20f, 99f), "an over-spent budget gives nothing");
        assertEquals(CatastropheRules.collapseAllowance(20f, 0f), CatastropheRules.collapseAllowance(20f, -5f), 0.001f, "damage taken is never negative: a negative figure counts as none");
        assertTrue(CatastropheRules.collapseAllowance(0f, 0f) > 0f, "a nonsense health value still gives a small, positive allowance");
        for (float hp = 1f; hp <= 2000f; hp *= 1.7f) assertTrue(CatastropheRules.collapseAllowance(hp, 0f) < hp, "at " + hp + " maximum health the budget is below it");
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
