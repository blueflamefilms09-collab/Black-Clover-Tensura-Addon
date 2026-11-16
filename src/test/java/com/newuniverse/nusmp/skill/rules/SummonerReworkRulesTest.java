package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Summoner rework's unlock order, rank ceiling, mastery gain and old-skill carry-over flags must not regress (docs/summoner_rework.md). */
class SummonerReworkRulesTest {
    @Test
    void oldModeIndexesKeepTheirMeaning() {
        assertEquals(0, SummonerRules.BEAST);
        assertEquals(1, SummonerRules.STONE);
        assertEquals(2, SummonerRules.RECALL);
        assertEquals(3, SummonerRules.LEGION);
        assertEquals(4, SummonerRules.WRAITH);
        assertEquals(9, SummonerRules.MODES);
    }

    @Test
    void theBaseSummonerNeverListsTheUltimateCards() {
        assertFalse(SummonerRules.listed(SummonerRules.LEGION, false));
        assertFalse(SummonerRules.listed(SummonerRules.WRAITH, false));
        assertTrue(SummonerRules.listed(SummonerRules.LEGION, true));
        assertTrue(SummonerRules.listed(SummonerRules.SYNTHESIS, false));
        assertFalse(SummonerRules.listed(9, true));
        assertFalse(SummonerRules.listed(-1, true));
    }

    @Test
    void unlocksOpenInTheAnimeOrderAndTheUltimateHasEverything() {
        int[] order = {SummonerRules.BIRD, SummonerRules.STRENGTHEN, SummonerRules.SYNTHESIS, SummonerRules.SPIRIT};
        for (int i = 1; i < order.length; i++) assertTrue(SummonerRules.unlockAt(order[i]) > SummonerRules.unlockAt(order[i - 1]));
        assertTrue(SummonerRules.unlockAt(order[order.length - 1]) < SummonerRules.REFLECT_AT);
        assertEquals(0.0, SummonerRules.unlockAt(SummonerRules.BEAST));
        assertEquals(0.0, SummonerRules.unlockAt(SummonerRules.RECALL));
        assertFalse(SummonerRules.unlocked(SummonerRules.BIRD, 0.14, false, false));
        assertTrue(SummonerRules.unlocked(SummonerRules.BIRD, 0.15, false, false));
        assertTrue(SummonerRules.unlocked(SummonerRules.SPIRIT, 0.0, true, false));
        assertEquals(60, SummonerRules.unlockPercent(SummonerRules.SPIRIT));
        assertFalse(SummonerRules.reflects(0.5, false));
        assertTrue(SummonerRules.reflects(0.75, false));
        assertTrue(SummonerRules.reflects(0.0, true));
    }

    @Test
    void aHolderCarriedOverFromTheOldSkillKeepsTheirModeAtOnce() {
        int flags = SummonerRules.FORCE_BIRD;
        assertTrue(SummonerRules.forced(flags, SummonerRules.BIRD));
        assertFalse(SummonerRules.forced(flags, SummonerRules.SPIRIT));
        assertTrue(SummonerRules.unlocked(SummonerRules.BIRD, 0.0, false, true));
        assertFalse(SummonerRules.forced(flags, SummonerRules.STRENGTHEN), "only the two old skills carry a flag");
        assertEquals(0, SummonerRules.forceFlag(SummonerRules.BEAST));
    }

    @Test
    void synthesisIsBoundedByTheRankCeiling() {
        assertTrue(SummonerRules.maxRank(Tier.ULTIMATE) > SummonerRules.maxRank(Tier.MASTERED));
        assertTrue(SummonerRules.maxRank(Tier.MASTERED) > SummonerRules.maxRank(Tier.BASE));
        for (Tier t : Tier.values()) {
            assertTrue(SummonerRules.canSynthesize(0, t));
            assertFalse(SummonerRules.canSynthesize(SummonerRules.maxRank(t), t), "a top-rank summon cannot be combined further");
            assertFalse(SummonerRules.canSynthesize(-1, t));
        }
        assertEquals(1.0, SummonerRules.rankFactor(0));
        assertTrue(SummonerRules.rankFactor(1) > 1.0 && SummonerRules.rankFactor(1) < 2.0, "one stronger than either card but weaker than both together");
        assertEquals(SummonerRules.rankFactor(3), SummonerRules.rankFactor(99), "the factor is clamped");
    }

    @Test
    void masteryGainIsTheOldOnePointWhenTheReworkIsOffAndBoundedWhenOn() {
        assertEquals(1.0, SummonerRules.castGain(10_000, 1, 0.5, false));
        assertEquals(3.0, SummonerRules.castGain(10_000, 3, 0.5, false));
        assertEquals(50.0, SummonerRules.castGain(10_000, 1, 0.5, true), 1e-9);
        assertEquals(1.0, SummonerRules.castGain(10_000, 1, 0.0, true), "never below one point");
        assertTrue(SummonerRules.castGain(10_000, 3, 99.0, true) <= 10_000 * 0.05 * 3 + 1e-9, "the percent is clamped");
        assertEquals(1.0, SummonerRules.pulseGain(10_000, 0.0, true));
        assertEquals(1.0, SummonerRules.pulseGain(10_000, 5.0, false));
    }

    @Test
    void theReflectionAndTheBoostsAreSmallAndShort() {
        assertEquals(-1, SummonerRules.reflectLevel(0, true));
        assertEquals(0, SummonerRules.reflectLevel(1, false));
        assertEquals(0, SummonerRules.reflectLevel(9, false));
        assertEquals(1, SummonerRules.reflectLevel(4, true));
        assertTrue(SummonerRules.REFLECT_TICKS <= 100);
        assertTrue(SummonerRules.strengthenSeconds(Tier.ULTIMATE) > SummonerRules.strengthenSeconds(Tier.BASE));
        assertTrue(SummonerRules.STRENGTHEN_RANGE <= 64.0);
        assertTrue(SummonerRules.strengthenCost(500) < 500 && SummonerRules.synthesisCost(500) < 500);
        assertEquals(AuraRules.spiritInterval(), SummonerRules.spiritInterval());
        assertEquals(20, SummonerRules.sightInterval());
    }
}
