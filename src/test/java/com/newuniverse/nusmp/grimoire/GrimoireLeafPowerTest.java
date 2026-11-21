package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The leaf ladder: three-leaf is the baseline, each step up is far stronger, off and bad numbers read as x1. */
class GrimoireLeafPowerTest {
    private static double f(GrimoireCover c) { return GrimoireLeafPower.factor(c, true, 2.5, 6.0, 10.0, 14.0); }

    @Test
    void theLadderRises() {
        assertEquals(1.0, f(GrimoireCover.THREE_LEAF));
        assertEquals(2.5, f(GrimoireCover.FOUR_LEAF));
        assertEquals(6.0, f(GrimoireCover.FIVE_LEAF));
        assertEquals(10.0, f(GrimoireCover.BLACK_MAGIC));
        assertEquals(14.0, f(GrimoireCover.GOD_TIER));
    }

    @Test
    void everyTierFourCoverGetsTheFourLeafStep() {
        for (GrimoireCover c : new GrimoireCover[]{GrimoireCover.DOUBLE_SPADE, GrimoireCover.TWO_HEART, GrimoireCover.FIVE_SIDED}) assertEquals(2.5, f(c), c.name());
        assertEquals(6.0, f(GrimoireCover.TRIPLE_SPADE));
    }

    @Test
    void tierThreeCoversAreTheBaseline() {
        for (GrimoireCover c : GrimoireCover.values()) if (c.tier <= 3) assertEquals(1.0, f(c), c.name());
    }

    @Test
    void theWholeLadderNeverFallsAndTheCoversKeepTheirOwnNumber() {
        double last = 0;
        for (GrimoireCover c : new GrimoireCover[]{GrimoireCover.THREE_LEAF, GrimoireCover.FOUR_LEAF, GrimoireCover.FIVE_LEAF, GrimoireCover.BLACK_MAGIC, GrimoireCover.GOD_TIER}) {
            double total = f(c) * c.damage;
            assertTrue(total > last, c.name());
            last = total;
        }
        assertTrue(f(GrimoireCover.FOUR_LEAF) * GrimoireCover.FOUR_LEAF.damage > 2.0 * f(GrimoireCover.THREE_LEAF) * GrimoireCover.THREE_LEAF.damage, "a massive step, not 5%");
    }

    @Test
    void offAndBadNumbersAreOne() {
        assertEquals(1.0, GrimoireLeafPower.factor(GrimoireCover.GOD_TIER, false, 2.5, 6.0, 10.0, 14.0));
        assertEquals(1.0, GrimoireLeafPower.factor(null, true, 2.5, 6.0, 10.0, 14.0));
        assertEquals(1.0, GrimoireLeafPower.factor(GrimoireCover.FOUR_LEAF, true, Double.NaN, 6.0, 10.0, 14.0));
        assertEquals(1.0, GrimoireLeafPower.factor(GrimoireCover.FOUR_LEAF, true, 0.2, 6.0, 10.0, 14.0));
    }
}
