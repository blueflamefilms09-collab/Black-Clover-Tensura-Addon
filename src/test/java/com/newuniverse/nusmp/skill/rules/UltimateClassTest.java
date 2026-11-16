package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Ultimate classes (docs/ultimate_classes.md) price, cap and gate 17 skills; the math is pure, so it is pinned here: the columns and their order (append only), the clamps every config value
 * passes through, the class-capped EP scaling and the combined cap with the late-game factor, the price (never below the flat price, proportional late game), the milestones and the mastery rescale.
 */
class UltimateClassTest {
    @Test
    void columnsAndOrder() {
        assertArrayEquals(new UltimateClass[]{UltimateClass.LORD, UltimateClass.KING, UltimateClass.GOD, UltimateClass.DRAGON}, UltimateClass.values(), "append only");
        assertEquals(8.0, UltimateClass.LORD.epCap);
        assertEquals(10.0, UltimateClass.KING.epCap);
        assertEquals(12.0, UltimateClass.GOD.epCap);
        assertEquals(10_000, UltimateClass.LORD.maxMastery);
        assertEquals(25_000, UltimateClass.KING.maxMastery);
        assertEquals(50_000, UltimateClass.GOD.maxMastery);
        assertEquals(0.70, UltimateClass.KING.shredBelow);
        assertEquals(0.60, UltimateClass.GOD.killBelow);
        assertEquals(0.0, UltimateClass.LORD.killBelow);
        for (UltimateClass c : UltimateClass.values()) {
            assertTrue(c.epCap >= 1 && c.epCap <= UltimateClass.EP_CAP_MAX, c + " epCap");
            assertEquals(c.epCap, UltimateClass.clampEpCap(c.epCap), c + " default passes its clamp");
            assertEquals(c.costPct, UltimateClass.clampCostPct(c.costPct));
            assertEquals(c.costRef, UltimateClass.clampCostRef(c.costRef));
            assertEquals(c.maxMastery, UltimateClass.clampMaxMastery(c.maxMastery));
            assertEquals(c.killBelow, UltimateClass.clampRatio(c.killBelow));
            assertEquals(c.key(), c.name().toLowerCase());
        }
        assertTrue(UltimateClass.KING.costPct > UltimateClass.LORD.costPct && UltimateClass.GOD.costPct > UltimateClass.KING.costPct, "a higher class costs a larger share");
    }

    @Test
    void clamps() {
        assertEquals(12.0, UltimateClass.clampEpCap(99));
        assertEquals(1.0, UltimateClass.clampEpCap(-5));
        assertEquals(12.0, UltimateClass.clampEpCap(Double.NaN));
        assertEquals(50.0, UltimateClass.clampCostPct(1e9));
        assertEquals(0.0, UltimateClass.clampCostPct(Double.NaN));
        assertEquals(1.0, UltimateClass.clampCostRef(0));
        assertEquals(100, UltimateClass.clampMaxMastery(1));
        assertEquals(1_000_000, UltimateClass.clampMaxMastery(Integer.MAX_VALUE));
        assertEquals(0.95, UltimateClass.clampRatio(2));
        assertEquals(1.0, UltimateClass.clampLateShare(0));
        assertEquals(6.0, UltimateClass.clampLateShare(100));
    }

    @Test
    void epScaleIsTheOldCurveCappedByTheClass() {
        assertEquals(1.0, UltimateClass.epScale(500, 12));
        assertEquals(1.0, UltimateClass.epScale(Double.NaN, 12));
        assertEquals(3.5, UltimateClass.epScale(10_000, 12), 1e-9);
        assertEquals(6.0, UltimateClass.epScale(100_000, 12), 1e-9);
        assertEquals(8.5, UltimateClass.epScale(1_000_000, 12), 1e-9);
        assertEquals(8.0, UltimateClass.epScale(1_000_000, UltimateClass.LORD.epCap), 1e-9, "Lord stops at x8");
        assertEquals(10.0, UltimateClass.epScale(1e9, UltimateClass.KING.epCap), 1e-9);
        assertEquals(12.0, UltimateClass.epScale(Double.POSITIVE_INFINITY, 12), 1e-9);
    }

    @Test
    void combinedCapWithTheLateGameFactor() {
        // the top of every class with the defaults: epCap x 3
        assertEquals(24.0, UltimateClass.totalScale(1e12, 8, 6, 3), 1e-9);
        assertEquals(30.0, UltimateClass.totalScale(1e12, 10, 6, 3), 1e-9);
        assertEquals(36.0, UltimateClass.totalScale(1e12, 12, 6, 3), 1e-9);
        // never above the old unclassified top (12 x 6), whatever the settings
        assertTrue(UltimateClass.totalScale(1e12, 99, 99, 99) <= 72.0 + 1e-9);
        // below the cap it is just the product; a bad late factor counts as x1
        assertEquals(6.0 * 2.25, UltimateClass.totalScale(100_000, 12, 2.25, 3), 1e-9);
        assertEquals(6.0, UltimateClass.totalScale(100_000, 12, Double.NaN, 3), 1e-9);
        assertEquals(1.0, UltimateClass.totalScale(0, 12, 0.5, 3), 1e-9);
    }

    @Test
    void priceNeverBelowFlatAndProportionalLateGame() {
        // early game: the flat price
        assertEquals(4000, UltimateClass.price(4000, 10_000, 5, 4000), 1e-9);
        assertEquals(4000, UltimateClass.price(4000, 0, 5, 4000), 1e-9);
        // a cast of costRef costs costPct of the pool once that is more
        assertEquals(20_000, UltimateClass.price(4000, 400_000, 5, 4000), 1e-9);
        assertEquals(500_000, UltimateClass.price(4000, 10_000_000, 5, 4000), 1e-9);
        // other casts in proportion: a 600 dash stays cheaper than a 4,000 meteor
        assertEquals(3000, UltimateClass.price(600, 400_000, 5, 4000), 1e-9);
        assertTrue(UltimateClass.price(600, 1e7, 5, 4000) < UltimateClass.price(4000, 1e7, 5, 4000));
        // free stays free; zero percent = the flat price; bad input never NaN
        assertEquals(0.0, UltimateClass.price(0, 1e9, 5, 4000));
        assertEquals(1200, UltimateClass.price(1200, 1e9, 0, 4000), 1e-9);
        assertEquals(1200, UltimateClass.price(1200, Double.NaN, 5, 4000), 1e-9);
        assertEquals(1200, UltimateClass.price(1200, Double.POSITIVE_INFINITY, 5, 4000), 1e-9);
    }

    @Test
    void milestones() {
        assertEquals(0, UltimateClass.milestone(0));
        assertEquals(0, UltimateClass.milestone(-1));
        assertEquals(0, UltimateClass.milestone(Double.NaN));
        assertEquals(0, UltimateClass.milestone(0.2499));
        assertEquals(1, UltimateClass.milestone(0.25));
        assertEquals(2, UltimateClass.milestone(0.5));
        assertEquals(3, UltimateClass.milestone(0.75));
        assertEquals(3, UltimateClass.milestone(0.9999));
        assertEquals(4, UltimateClass.milestone(1.0));
        assertEquals(4, UltimateClass.milestone(7.0));
        assertEquals(1.0, UltimateClass.milestoneRange(0));
        assertEquals(1.2, UltimateClass.milestoneRange(1));
        assertFalse(UltimateClass.milestoneSecondEffect(1));
        assertTrue(UltimateClass.milestoneSecondEffect(2));
        assertEquals(1.0, UltimateClass.milestoneCost(2));
        assertEquals(0.8, UltimateClass.milestoneCost(3));
        assertEquals(0.75, UltimateClass.milestoneCooldown(4));
        assertEquals(1.0, UltimateClass.milestoneCooldown(2));
    }

    @Test
    void masteryRescaleKeepsTheFraction() {
        assertEquals(50_000, UltimateClass.rescaleMastery(10_000, 10_000, 50_000), 1e-9, "mastered stays mastered");
        assertEquals(12_500, UltimateClass.rescaleMastery(5_000, 10_000, 25_000), 1e-9);
        assertEquals(10_000, UltimateClass.rescaleMastery(50_000, 50_000, 10_000), 1e-9);
        assertEquals(10_000, UltimateClass.rescaleMastery(80_000, 50_000, 10_000), 1e-9, "never above the new max");
        assertEquals(-3, UltimateClass.rescaleMastery(-3, 10_000, 50_000), 1e-9, "still learning: untouched");
        assertEquals(700, UltimateClass.rescaleMastery(700, 10_000, 10_000), 1e-9);
        assertEquals(700, UltimateClass.rescaleMastery(700, 0, 10_000), 1e-9);
    }

    @Test
    void masteredUltimateTier() {
        assertEquals(Tier.ULTIMATE_MASTERED, Tier.of(true, true, true));
        assertEquals(Tier.ULTIMATE, Tier.of(true, true, false), "setting off = the old tier");
        assertEquals(Tier.ULTIMATE, Tier.of(false, true, true));
        assertEquals(Tier.MASTERED, Tier.of(true, false, true));
        assertTrue(Tier.ULTIMATE_MASTERED.ultimate() && Tier.ULTIMATE.ultimate() && !Tier.MASTERED.ultimate());
        assertEquals(3, Tier.ULTIMATE_MASTERED.pick(1, 2, 3), "reads like an Ultimate");
        assertEquals(4, Tier.ULTIMATE_MASTERED.pick(1, 2, 3, 4), "the mastered value");
        assertEquals(3, Tier.ULTIMATE.pick(1, 2, 3, 4));
        assertEquals(Tier.ULTIMATE_MASTERY_BONUS, Tier.ULTIMATE_MASTERED.masteryScale(false));
        assertTrue(Tier.ULTIMATE_MASTERED.atLeast(Tier.ULTIMATE));
        // the rules that gate on the Ultimate keep the mastered Ultimate's perks
        assertEquals(ChargeRules.maxCharge(Tier.ULTIMATE), ChargeRules.maxCharge(Tier.ULTIMATE_MASTERED));
        assertEquals(FireForceRules.modes(Tier.ULTIMATE), FireForceRules.modes(Tier.ULTIMATE_MASTERED));
        assertEquals(WillRules.modes(Tier.ULTIMATE), WillRules.modes(Tier.ULTIMATE_MASTERED));
        assertEquals(SummonRules.maxSummons(3, 6, Tier.ULTIMATE), SummonRules.maxSummons(3, 6, Tier.ULTIMATE_MASTERED));
        assertTrue(VectorRules.fallImmune(Tier.ULTIMATE_MASTERED));
    }
}
