package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: saves and the mastery bits depend on the page ids and their order, so the four pages Transmutation Magic had before the 1.93.0 rebuild are pinned as the first four, in that order, and the four
 * new ones come after them with ids that never repeat; the numbers (spike count, armour cut, summon time, blade and flower counts) stay inside the bounds the balance law relies on.
 */
class TransmutationRulesTest {
    @Test
    void oldPagesKeepTheirIdsAndOrder() {
        assertEquals(List.of("iron_spikes", "magic_convert", "quagmire", "grand_transmutation"), TransmutationRules.OLD_IDS);
        assertEquals(List.of("alchemic_blades", "flowering", "transmuted_sentinel", "alchemist_armour"), TransmutationRules.NEW_IDS);
        var all = new HashSet<String>(TransmutationRules.OLD_IDS);
        all.addAll(TransmutationRules.NEW_IDS);
        assertEquals(8, all.size(), "a page id is used twice");
    }

    @Test
    void spikeCountIsBoundedAndGrowsWithSize() {
        assertEquals(9, TransmutationRules.spikeCount(1f));
        assertEquals(5, TransmutationRules.spikeCount(0.1f));
        assertEquals(16, TransmutationRules.spikeCount(40f));
        for (float s = 0.2f; s < 3f; s += 0.1f) assertTrue(TransmutationRules.spikeCount(s + 0.1f) >= TransmutationRules.spikeCount(s));
    }

    @Test
    void armourCutIsTwentyToThirtyFivePercent() {
        assertEquals(0.20f, TransmutationRules.armourCut(1.0), 1e-6);
        assertEquals(0.20f, TransmutationRules.armourCut(-5.0), 1e-6);
        assertEquals(0.35f, TransmutationRules.armourCut(12.0), 1e-6);
        double prev = 0;
        for (double ep = 1; ep <= 12; ep += 0.5) {
            float c = TransmutationRules.armourCut(ep);
            assertTrue(c >= prev);
            assertTrue(c >= 0.2f && c <= 0.35f);
            prev = c;
        }
    }

    @Test
    void summonsLastAtMostFiftyPercentLonger() {
        assertEquals(600, TransmutationRules.summonTicks(600, 1.0));
        assertEquals(900, TransmutationRules.summonTicks(600, 100.0));
        assertEquals(600, TransmutationRules.summonTicks(600, 0.2));
    }

    @Test
    void countsFollowTheModifier() {
        assertEquals(3, TransmutationRules.bladeCount(0));
        assertEquals(5, TransmutationRules.bladeCount(4));
        assertEquals(5, TransmutationRules.bladeCount(99));
        assertEquals(2, TransmutationRules.flowerCleanses(0));
        assertEquals(4, TransmutationRules.flowerCleanses(4));
        assertTrue(TransmutationRules.sentinelScale(1.0) >= 0.9f && TransmutationRules.sentinelScale(12.0) <= 1.06f + 1e-6);
        assertTrue(TransmutationRules.bladeHit(0.01f) > 0f && TransmutationRules.bladeHit(50f) <= 9f + 1e-6);
    }

    @Test
    void theArmourOutlastsItsShatterWindow() {
        assertTrue(TransmutationRules.SHORT_ARMOUR_TICKS >= 40, "the last piece must be on before the shatter window opens");
        assertTrue(TransmutationRules.ARMOUR_TICKS > TransmutationRules.SHORT_ARMOUR_TICKS);
    }
}
