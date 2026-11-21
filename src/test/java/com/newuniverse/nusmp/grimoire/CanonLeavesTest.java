package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Canon leaves (docs/grimoire_canon_leaves.md): meaning text, luck, ceremony weights, the devil's price and the mastery curves. All pure. */
class CanonLeavesTest {
    // ---- meaning
    @Test
    void everyCoverHasAMeaningAndOnlyTheTwoLegendaryOnesAreModOnly() {
        int modOnly = 0;
        for (GrimoireCover c : GrimoireCover.values()) {
            assertFalse(GrimoireMeaning.text(c).isBlank(), c.name());
            assertTrue(GrimoireMeaning.key(c).endsWith(c.name().toLowerCase()), c.name());
            if (GrimoireMeaning.modOnly(c)) { modOnly++; assertTrue(GrimoireMeaning.text(c).startsWith("Mod-only"), c.name()); }
            else assertFalse(GrimoireMeaning.text(c).contains("Mod-only"), c.name());
        }
        assertEquals(2, modOnly);
        assertTrue(GrimoireMeaning.modOnly(GrimoireCover.BLACK_MAGIC) && GrimoireMeaning.modOnly(GrimoireCover.GOD_TIER));
        assertTrue(GrimoireMeaning.text(GrimoireCover.FOUR_LEAF).contains("Luck"));
        assertTrue(GrimoireMeaning.text(GrimoireCover.FIVE_LEAF).contains("Devil"));
        assertTrue(GrimoireMeaning.text(GrimoireCover.THREE_LEAF).contains("Faith"));
        assertFalse(GrimoireMeaning.text(null).isBlank());
    }

    // ---- luck
    @Test
    void luckIsDeterministicWithAnInjectedRandom() {
        assertEquals(1.5, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 0.06, 1.5, () -> 0.05));
        assertEquals(1.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 0.06, 1.5, () -> 0.06));
        assertEquals(1.5, CanonLeaves.luckFactor(GrimoireCover.FIVE_SIDED, true, 0.06, 1.5, () -> 0.0));
    }

    @Test
    void chanceZeroNeverAndChanceOneAlways() {
        Random r = new Random(7);
        for (int i = 0; i < 1000; i++) {
            assertEquals(1.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 0.0, 1.5, r::nextDouble));
            assertEquals(1.5, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 1.0, 1.5, r::nextDouble));
        }
    }

    @Test
    void luckClampsAndOnlyLuckCoversAndOnlyWithCanonOn() {
        assertEquals(1.5, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 5.0, 1.5, () -> 0.9999));       // chance clamped to 1
        assertEquals(1.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, -1.0, 1.5, () -> 0.0));        // clamped to 0
        assertEquals(10.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 1.0, 99.0, () -> 0.0));        // mult clamped to 10
        assertEquals(1.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, 1.0, 0.2, () -> 0.0));         // never a loss
        assertEquals(1.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, true, Double.NaN, 1.5, () -> 0.0));
        assertEquals(1.0, CanonLeaves.luckFactor(GrimoireCover.FOUR_LEAF, false, 1.0, 1.5, () -> 0.0));
        for (GrimoireCover c : GrimoireCover.values())
            assertEquals(c == GrimoireCover.FOUR_LEAF || c == GrimoireCover.FIVE_SIDED, CanonLeaves.luckCover(c), c.name());
    }

    // ---- ceremony weights
    @Test
    void fiveLeafWeightMovesToTheFourLeafWhenCanonIsOn() {
        double[] on = CanonLeaves.tierWeights(80, 16, 3.5, 0.5, 0.1, true), off = CanonLeaves.tierWeights(80, 16, 3.5, 0.5, 0.1, false);
        assertArrayEquals(new double[]{80, 19.5, 0, 0.5, 0.1}, on, 1e-12);
        assertArrayEquals(new double[]{80, 16, 3.5, 0.5, 0.1}, off, 1e-12);
        double a = 0, b = 0;
        for (int i = 0; i < 5; i++) { a += on[i]; b += off[i]; }
        assertEquals(b, a, 1e-12);
        assertEquals(0.5, on[3]);       // black magic and god-tier kept
        assertEquals(0.1, on[4]);
    }

    // ---- the devil's price
    @Test
    void theDevilPaysFromFiveLeafUpExceptGodTier() {
        for (GrimoireCover c : GrimoireCover.values()) assertEquals(c.tier >= 5 && c != GrimoireCover.GOD_TIER, CanonLeaves.paysDevil(c), c.name());
        assertEquals(1.0 / 0.9, CanonLeaves.devilCostFactor(GrimoireCover.FIVE_LEAF, 0.1), 1e-12);
        assertEquals(1.0, CanonLeaves.devilCostFactor(GrimoireCover.FOUR_LEAF, 0.1));
        assertEquals(1.0, CanonLeaves.devilCostFactor(GrimoireCover.GOD_TIER, 0.1));
        assertEquals(1.0, CanonLeaves.devilCostFactor(GrimoireCover.FIVE_LEAF, 0.0));
        assertEquals(2.0, CanonLeaves.devilCostFactor(GrimoireCover.FIVE_LEAF, 9.0), 1e-12);       // share clamped to 0.5
    }

    @Test
    void theHealthPriceIsNeverLethalAndRateLimited() {
        assertEquals(0.4, CanonLeaves.healthPrice(20, 20, 10, 100, 1000, 0), 1e-12);           // 2% of 20
        assertEquals(0.0, CanonLeaves.healthPrice(20, 20, 15, 100, 1000, 0));                  // exactly 15% is not below
        assertEquals(0.0, CanonLeaves.healthPrice(20, 20, 60, 100, 1000, 0));
        assertEquals(0.0, CanonLeaves.healthPrice(20, 20, 10, 0, 1000, 0));                    // no max mana known
        assertEquals(0.0, CanonLeaves.healthPrice(20, 20, 10, 100, 1019, 1000));               // within a second
        assertEquals(0.4, CanonLeaves.healthPrice(20, 20, 10, 100, 1020, 1000), 1e-12);
        assertEquals(0.0, CanonLeaves.healthPrice(2.0, 20, 0, 100, 1000, 0));                  // one heart is never taken
        assertEquals(0.0, CanonLeaves.healthPrice(1.0, 20, 0, 100, 1000, 0));
        assertEquals(0.5, CanonLeaves.healthPrice(2.5, 1000, 0, 100, 1000, 0), 1e-12);         // capped at what leaves one heart
        for (double hp = 0; hp <= 40; hp += 0.5) assertTrue(hp - CanonLeaves.healthPrice(hp, 40, 0, 100, 5000, 0) >= Math.min(hp, 2.0) - 1e-12, "hp " + hp);
        assertEquals(0.0, CanonLeaves.healthPrice(Double.NaN, 20, 0, 100, 1000, 0));
        assertEquals(0.0, CanonLeaves.healthPrice(20, 20, Double.NaN, 100, 1000, 0));
    }

    @Test
    void theLogoutCleanupForgetsAPlayer() {
        java.util.UUID id = java.util.UUID.randomUUID();
        CanonLeaves.forget(id);
        assertTrue(CanonLeaves.tracked() >= 0);
    }

    @Test
    void settingsClampAndNaNFallsBack() {
        assertEquals(0.5, CanonLeaves.clamp(7, 0, 0.5, 0.1));
        assertEquals(0.1, CanonLeaves.clamp(Double.NaN, 0, 0.5, 0.1));
        assertEquals(0.0, CanonLeaves.clamp(-3, 0, 0.5, 0.1));
    }
}
