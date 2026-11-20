package com.newuniverse.nusmp.devil.mastery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the numbers of the Devil Awakening skill (docs/devil_mastery.md) are the whole balance of the contract; they are pinned here with no game: the curve is monotone, starts at 10 %, never passes 100 %, full power takes 25 to 40 hours of active play and far more of idle play. */
class MasteryRulesTest {
    // the "active" player of the docs: an arts every 15 s, a kill every 20 s, a blow a few seconds
    private static final double CASTS = 4, KILLS = 3, HITS = 20;

    @Test
    void startsAtTenPercentAndTiersOpenAtTheTable() {
        assertEquals(10f, MasteryRules.START);
        assertEquals(0, MasteryRules.tier(10f));
        assertEquals(0, MasteryRules.tier(24.9f));
        assertEquals(1, MasteryRules.tier(25f));
        assertEquals(2, MasteryRules.tier(50f));
        assertEquals(3, MasteryRules.tier(75f));
        assertEquals(4, MasteryRules.tier(100f));
        assertEquals(25f, MasteryRules.nextAt(10f));
        assertEquals(100f, MasteryRules.nextAt(75f));
        assertEquals(-1f, MasteryRules.nextAt(100f));
    }

    @Test
    void gainIsMonotoneBoundedAndNeverGoesBack() {
        float m = MasteryRules.START;
        for (int i = 0; i < 100_000 && m < MasteryRules.MAX; i++) {
            float n = MasteryRules.gain(m, 60, 1.0);
            assertTrue(n >= m, "the mastery fell at " + m);
            assertTrue(n <= MasteryRules.MAX);
            m = n;
        }
        assertEquals(MasteryRules.MAX, m, 0.01f);
        assertEquals(MasteryRules.MAX, MasteryRules.gain(100f, 1000, 10.0));
        float capped = 10f;
        for (int i = 0; i < 100_000; i++) capped = MasteryRules.gain(capped, 120, 10.0, 30f);
        assertEquals(30f, capped, "a server maximum is respected");
        assertEquals(40f, MasteryRules.gain(40f, -5, 1.0), "a negative use takes nothing away");
        assertEquals(MasteryRules.START, MasteryRules.clamp(Float.NaN));
    }

    @Test
    void eachPercentCostsMoreAsTheMasteryGrows() {
        double prev = 0;
        for (float m = 10f; m <= 100f; m += 5f) {
            double s = MasteryRules.secondsPerPercent(m, 1.0);
            assertTrue(s >= prev, "diminishing returns broke at " + m);
            prev = s;
        }
        assertEquals(MasteryRules.BASE_SECONDS_PER_PERCENT, MasteryRules.secondsPerPercent(10f, 1.0), 1e-9);
        assertEquals(MasteryRules.BASE_SECONDS_PER_PERCENT * (1 + MasteryRules.SLOWDOWN), MasteryRules.secondsPerPercent(100f, 1.0), 1e-9);
        assertTrue(MasteryRules.secondsPerPercent(50f, 5.0) < MasteryRules.secondsPerPercent(50f, 1.0), "a higher rate is faster");
        assertEquals(MasteryRules.secondsPerPercent(50f, 10.0), MasteryRules.secondsPerPercent(50f, 1000.0), 1e-9, "the rate is bounded");
    }

    @Test
    void oneHugeUseCannotJumpTheBar() {
        float after = MasteryRules.gain(10f, 1.0e9, 1.0);
        assertTrue(after - 10f <= MasteryRules.MAX_SINGLE / MasteryRules.secondsPerPercent(10f, 1.0) + 0.01, "one use added " + (after - 10f) + " percent");
    }

    @Test
    void fullMasteryTakesTwentyFiveToFortyHoursOfActivePlay() {
        double h = MasteryRules.hoursToMax(1.0, CASTS, KILLS, HITS, 1.0);
        assertTrue(h >= 25.0 && h <= 40.0, "active play reaches 100 % in " + h + " h");
    }

    @Test
    void movingOnlyOrIdlePlayIsMuchSlowerAndStandingStillGainsNothing() {
        double active = MasteryRules.hoursToMax(1.0, CASTS, KILLS, HITS, 1.0);
        double moving = MasteryRules.hoursToMax(1.0, 0, 0, 0, MasteryRules.afkFactor(100_000, true));
        double still = MasteryRules.hoursToMax(1.0, 0, 0, 0, MasteryRules.afkFactor(100_000, false));
        assertTrue(moving > active * 3, "walking about reached 100 % in " + moving + " h, active " + active + " h");
        assertTrue(still >= 400.0, "standing still never gets there (the model stops at 400 h), got " + still);
        assertEquals(0.0, MasteryRules.afkFactor(100_000, false));
        assertEquals(0.2, MasteryRules.afkFactor(100_000, true));
        assertEquals(1.0, MasteryRules.afkFactor(1200, false));
        assertEquals(1.0, MasteryRules.afkFactor(0, false));
    }

    @Test
    void theDailyCapBoundsAnDay() {
        assertEquals(360 * 60.0, MasteryRules.capLeft(0, 360), 1e-9);
        assertEquals(0.0, MasteryRules.capLeft(360 * 60.0, 360), 1e-9);
        assertEquals(0.0, MasteryRules.capLeft(1.0e9, 360), 1e-9);
        assertEquals(0.0, MasteryRules.capLeft(0, -5), 1e-9);
        // with the default cap a player needs several real days: the whole climb in effective seconds, over the cap of one day
        double eff = 0;
        float m = MasteryRules.START;
        while (m < MasteryRules.MAX) { eff += MasteryRules.secondsPerPercent(m, 1.0) * 0.5; m += 0.5f; }
        assertTrue(eff / (MasteryRules.DEFAULT_DAILY_MINUTES * 60.0) >= 3.0, "at least three days of the daily cap are needed, got " + eff / (MasteryRules.DEFAULT_DAILY_MINUTES * 60.0));
    }

    @Test
    void theAwakeningGrowsWithTheMasteryAndItsRestShrinks() {
        assertEquals(90, MasteryRules.durationSeconds(10f));
        assertEquals(300, MasteryRules.durationSeconds(100f));
        assertEquals(300, MasteryRules.cooldownSeconds(10f));
        assertEquals(120, MasteryRules.cooldownSeconds(100f));
        for (float m = 10f; m < 100f; m += 5f) {
            assertTrue(MasteryRules.durationSeconds(m + 5f) >= MasteryRules.durationSeconds(m));
            assertTrue(MasteryRules.cooldownSeconds(m + 5f) <= MasteryRules.cooldownSeconds(m));
            assertTrue(MasteryRules.artScale(m + 5f) > MasteryRules.artScale(m));
            assertTrue(MasteryRules.passiveScale(m + 5f) > MasteryRules.passiveScale(m));
        }
        assertEquals(1.0, MasteryRules.artScale(100f), 1e-9);
        assertEquals(1.0, MasteryRules.passiveScale(100f), 1e-9);
        assertTrue(MasteryRules.artScale(10f) < 0.4 && MasteryRules.passiveScale(10f) < 0.4, "at the start the power is a third");
        assertTrue(MasteryRules.withdrawCooldownSeconds(10f) < MasteryRules.cooldownSeconds(10f) && MasteryRules.withdrawCooldownSeconds(10f) >= 10);
        assertEquals(0, MasteryRules.aftermathSeconds(19f, 10f), "no fatigue under level 20");
        assertTrue(MasteryRules.aftermathSeconds(100f, 10f) > MasteryRules.aftermathSeconds(100f, 100f), "mastery eases the fatigue");
        assertTrue(MasteryRules.aftermathSeconds(100f, 10f) <= 30);
    }

    @Test
    void theCeilingIsTheMasteryUnlessLegacy() {
        assertEquals(10f, MasteryRules.ceiling(10f, false));
        assertEquals(55f, MasteryRules.ceiling(55f, false));
        assertEquals(100f, MasteryRules.ceiling(10f, true), "devil.legacyAssimilation restores the old reach of the level");
        assertEquals(100, MasteryRules.tenths(100f) / 10);
        assertEquals(100, MasteryRules.tenths(10f));
    }
}
