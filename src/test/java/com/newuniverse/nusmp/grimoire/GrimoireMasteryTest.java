package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Mana and level matter (docs/grimoire_canon_leaves.md): finite, never below x1, monotone in the wielder's EP and in the cover's tier. */
class GrimoireMasteryTest {
    private static final double[] EPS = {0, 1, 500, 1e3, 3e3, 1e4, 1e5, 1e6, 1e7, 1e9, 1e15, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN, -5};

    @Test
    void neverBelowOneAndAlwaysFiniteAndWithinTheCaps() {
        for (GrimoireCover c : GrimoireCover.values()) for (double ep : EPS) {
            double cost = GrimoireMastery.costFactor(c, ep, 1.5), dmg = GrimoireMastery.damageFactor(c, ep, 0.12);
            assertTrue(Double.isFinite(cost) && cost >= 1.0 && cost <= 1.5, c + " " + ep + " cost " + cost);
            assertTrue(Double.isFinite(dmg) && dmg >= 1.0 && dmg <= 1.12 + 1e-12, c + " " + ep + " dmg " + dmg);
        }
    }

    @Test
    void costFallsAsTheWielderGrowsAndRisesWithTheCover() {
        for (GrimoireCover c : GrimoireCover.values()) {
            double last = Double.MAX_VALUE;
            for (double ep : new double[]{0, 1e3, 1e4, 1e5, 1e6, 1e7}) { double f = GrimoireMastery.costFactor(c, ep, 1.5); assertTrue(f <= last + 1e-12, c + " " + ep); last = f; }
        }
        double t3 = GrimoireMastery.costFactor(GrimoireCover.THREE_LEAF, 1e3, 1.5), t4 = GrimoireMastery.costFactor(GrimoireCover.FOUR_LEAF, 1e3, 1.5), t5 = GrimoireMastery.costFactor(GrimoireCover.FIVE_LEAF, 1e3, 1.5);
        double black = GrimoireMastery.costFactor(GrimoireCover.BLACK_MAGIC, 1e3, 1.5), god = GrimoireMastery.costFactor(GrimoireCover.GOD_TIER, 1e3, 1.5);
        assertEquals(1.0, t3);
        assertTrue(t3 < t4 && t4 < t5 && t5 < black && black < god);
    }

    @Test
    void bonusRisesWithTheWielderAndFallsWithTheCover() {
        double last = 0;
        for (double ep : new double[]{0, 1e3, 1e4, 1e5, 1e6, 1e7}) { double f = GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, ep, 0.12); assertTrue(f >= last, "" + ep); last = f; }
        assertTrue(GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e6, 0.12) > GrimoireMastery.damageFactor(GrimoireCover.FOUR_LEAF, 1e6, 0.12));
        assertEquals(1.0, GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e3, 0.12));
        assertEquals(1.0, GrimoireMastery.damageFactor(GrimoireCover.FIVE_LEAF, 1e3, 0.12));
    }

    @Test
    void aWielderAtTheBandPaysAndGainsNothingExtra() {
        assertEquals(1.0, GrimoireMastery.costFactor(GrimoireCover.FOUR_LEAF, 1e4, 1.5), 1e-12);
        assertEquals(1.0, GrimoireMastery.damageFactor(GrimoireCover.FOUR_LEAF, 1e4, 0.12), 1e-12);
    }

    @Test
    void theSettingsClampAndOffValuesGiveOne() {
        assertEquals(1.0, GrimoireMastery.costFactor(GrimoireCover.GOD_TIER, 0, 1.0));
        assertEquals(1.0, GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e9, 0.0));
        assertTrue(GrimoireMastery.costFactor(GrimoireCover.GOD_TIER, 0, 99) <= 3.0);
        assertTrue(GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e9, 99) <= 2.0);
        assertEquals(1.0, GrimoireMastery.costFactor(GrimoireCover.GOD_TIER, 0, Double.NaN));
        assertEquals(1.0, GrimoireMastery.damageFactor(GrimoireCover.THREE_LEAF, 1e9, Double.NaN));
        assertEquals(0.0, GrimoireMastery.band(-1));
        assertEquals(6.0, GrimoireMastery.band(Double.MAX_VALUE));
    }
}
