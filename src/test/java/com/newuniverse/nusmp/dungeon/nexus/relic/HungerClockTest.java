package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Glutton's Chalice's Symbiotic Hunger clock (docs/living_dungeon.md section 7). */
class HungerClockTest {
    @Test
    void threeInGameDaysStarveIt() {
        assertEquals(72000L, HungerClock.STARVE_AFTER);
        assertEquals(HungerClock.Phase.FED, HungerClock.phase(0));
        assertEquals(HungerClock.Phase.FED, HungerClock.phase(47999));
        assertEquals(HungerClock.Phase.STIRRING, HungerClock.phase(48000));
        assertEquals(HungerClock.Phase.HUNGRY, HungerClock.phase(60000));
        assertEquals(HungerClock.Phase.HUNGRY, HungerClock.phase(71999));
        assertEquals(HungerClock.Phase.STARVING, HungerClock.phase(72000));
        assertEquals(HungerClock.Phase.STARVING, HungerClock.phase(10_000_000L));
    }

    @Test
    void nothingIsTakenBeforeThreeDays() {
        for (long s = 0; s < 72000; s += 20) {
            assertEquals(0, HungerClock.foodLoss(s));
            assertEquals(0.0, HungerClock.magiculeLoss(s), 0.0);
        }
    }

    @Test
    void onceStarvingItTakesFoodEveryFourthSweepAndMagiculesEverySweep() {
        int food = 0;
        for (int sweep = 0; sweep < 100; sweep++) {
            long s = 72000L + sweep * 20L;
            food += HungerClock.foodLoss(s);
            assertEquals(HungerClock.MAGICULE_SHARE, HungerClock.magiculeLoss(s), 1e-12);
        }
        assertEquals(25, food, "one point every four seconds");
    }

    @Test
    void aDayLaterItFeedsHarder() {
        assertEquals(0.005, HungerClock.magiculeLoss(72000L + 23999), 1e-12);
        assertEquals(0.01, HungerClock.magiculeLoss(72000L + 24000), 1e-12);
    }

    @Test
    void feedingResetsTheClock() {
        long fedAt = 1000;
        long now = 1000 + 80000;
        assertEquals(HungerClock.Phase.STARVING, HungerClock.phase(now, fedAt));
        fedAt = HungerClock.fed(now);
        assertEquals(HungerClock.Phase.FED, HungerClock.phase(now, fedAt));
        assertEquals(0, HungerClock.foodLoss(HungerClock.since(now, fedAt)));
    }

    @Test
    void aClockThatRanBackCountsAsFed() {
        assertEquals(0L, HungerClock.since(100, 5000));
        assertEquals(HungerClock.Phase.FED, HungerClock.phase(100, 5000));
    }

    @Test
    void daysForTheTooltip() {
        assertEquals(0.0, HungerClock.days(0), 1e-9);
        assertEquals(1.0, HungerClock.days(24000), 1e-9);
        assertEquals(3.0, HungerClock.days(72000), 1e-9);
    }
}
