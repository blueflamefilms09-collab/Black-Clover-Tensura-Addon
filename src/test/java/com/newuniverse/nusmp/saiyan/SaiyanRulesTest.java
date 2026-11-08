package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Saiyan numbers (ranks, growth, bonuses, anti-farm weights, daily room) are plain arithmetic, so they are pinned here with no game. */
class SaiyanRulesTest {
    @Test
    void sixRanksStartWhereTheSpecSays() {
        assertEquals(SaiyanRules.Rank.LOW_CLASS, SaiyanRules.rankOf(100));
        assertEquals(SaiyanRules.Rank.LOW_CLASS, SaiyanRules.rankOf(999));
        assertEquals(SaiyanRules.Rank.ELITE, SaiyanRules.rankOf(1_000));
        assertEquals(SaiyanRules.Rank.SUPER_ELITE, SaiyanRules.rankOf(10_000));
        assertEquals(SaiyanRules.Rank.LEGENDARY, SaiyanRules.rankOf(100_000));
        assertEquals(SaiyanRules.Rank.DIVINE, SaiyanRules.rankOf(1_000_000));
        assertEquals(SaiyanRules.Rank.TRANSCENDENT, SaiyanRules.rankOf(10_000_000));
        assertEquals(SaiyanRules.Rank.TRANSCENDENT, SaiyanRules.Rank.byOrdinal(99));
        assertEquals("nusmp.saiyan.rank.super_elite", SaiyanRules.Rank.SUPER_ELITE.key());
    }

    @Test
    void growthIsALogScaleFromTheFirstToTheLastRank() {
        assertEquals(0.0, SaiyanRules.growth(100), 1e-9);
        assertEquals(0.0, SaiyanRules.growth(Double.NaN), 1e-9);
        assertEquals(0.2, SaiyanRules.growth(1_000), 1e-9);
        assertEquals(1.0, SaiyanRules.growth(10_000_000), 1e-9);
        assertEquals(1.0, SaiyanRules.growth(1.0e12), 1e-9);
    }

    @Test
    void aBonusGrowsWithBattlePowerAndNeverPassesItsCap() {
        assertEquals(0.35, SaiyanRules.bonus(0.35, 1.0, 1.5, 100), 1e-9);
        assertEquals(0.70, SaiyanRules.bonus(0.35, 1.0, 1.5, 10_000_000), 1e-9);
        assertEquals(0.5, SaiyanRules.bonus(5.0, 10.0, 0.5, SaiyanRules.BP_HARD_CAP), 1e-9);
        assertEquals(0.0, SaiyanRules.bonus(Double.NaN, 1.0, 1.0, 5000), 1e-9);
        SaiyanRules.Table base = new SaiyanRules.Table(0.35, 0.20, 0.30, 0.15, 0.25, 0.40, 0.30, 0.15, 0.20);
        SaiyanRules.Table caps = new SaiyanRules.Table(1.5, 0.5, 1.5, 0.5, 0.8, 2.0, 2.0, 0.8, 1.0);
        SaiyanRules.Table top = base.at(1.0, SaiyanRules.BP_HARD_CAP, caps);
        assertTrue(top.strength() <= 1.5 && top.speed() <= 0.5 && top.knockback() <= 0.8 && top.jump() <= 0.8);
        assertEquals(0.35, base.at(1.0, 100, caps).strength(), 1e-9);
    }

    @Test
    void strengthRaisesAndDurabilityLowersDamage() {
        assertEquals(1.35, SaiyanRules.damageFactor(0.35), 1e-9);
        assertEquals(1.0 / 1.3, SaiyanRules.takenFactor(0.30), 1e-9);
        assertEquals(1.0, SaiyanRules.damageFactor(-3), 1e-9);
    }

    @Test
    void aWeakCreatureTeachesNothingAndAnEqualOneTeachesFully() {
        assertEquals(0.0, SaiyanRules.difficultyWeight(10, 1000, 0.25), 1e-9);
        assertEquals(0.0, SaiyanRules.difficultyWeight(0, 1000, 0.25), 1e-9);
        assertEquals(1.0, SaiyanRules.difficultyWeight(1000, 1000, 0.25), 1e-9);
        assertTrue(SaiyanRules.difficultyWeight(500, 1000, 0.25) > 0.0 && SaiyanRules.difficultyWeight(500, 1000, 0.25) < 1.0);
        assertEquals(1.5, SaiyanRules.difficultyWeight(1.0e12, 100, 0.25), 1e-9);
    }

    @Test
    void theSameCreatureAgainAndAgainIsWorthLess() {
        assertEquals(1.0, SaiyanRules.repeatFactor(0, 5), 1e-9);
        assertEquals(0.5, SaiyanRules.repeatFactor(5, 5), 1e-9);
        assertTrue(SaiyanRules.repeatFactor(50, 5) < SaiyanRules.repeatFactor(5, 5));
        assertEquals(1.0, SaiyanRules.repeatFactor(50, 0), 1e-9);
    }

    @Test
    void theDayHasAFloorAndAShareAndTheKillGainIsCutAtWhatIsLeft() {
        assertEquals(20.0, SaiyanRules.dailyRoom(100, 0.10, 20, 0), 1e-9);
        assertEquals(100.0, SaiyanRules.dailyRoom(1000, 0.10, 20, 0), 1e-9);
        assertEquals(0.0, SaiyanRules.dailyRoom(1000, 0.10, 20, 500), 1e-9);
        assertEquals(3.0, SaiyanRules.killGain(1000, 0.01, 1.0, 1.0, 0.2, 3.0), 1e-9);
        assertEquals(12.0, SaiyanRules.killGain(1000, 0.01, 1.0, 1.0, 0.2, 50.0), 1e-9);
        assertEquals(0.0, SaiyanRules.killGain(1000, 0.01, 0.0, 1.0, 0.2, 50.0), 1e-9);
        assertEquals(0, SaiyanRules.dayOf(23_999));
        assertEquals(1, SaiyanRules.dayOf(24_000));
        assertEquals(0, SaiyanRules.dayOf(-5));
    }

    @Test
    void kiDamageStaminaAndTheAwakeningStayInBounds() {
        assertEquals(6.0, SaiyanRules.kiDamage(6, 1.5, 100), 1e-9);
        assertEquals(15.0, SaiyanRules.kiDamage(6, 1.5, 10_000_000), 1e-9);
        assertEquals(100.0, SaiyanRules.stamina(90, 50, 100), 1e-9);
        assertEquals(0.0, SaiyanRules.stamina(5, -50, 100), 1e-9);
        assertTrue(SaiyanRules.awakened(10, 10));
        assertTrue(!SaiyanRules.awakened(9, 10));
        assertTrue(SaiyanRules.victimPower(20, 3, 2, 0, 2.0, 0.05) > 0.0);
    }
}
