package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Summoner's canon pass (docs/summoner_rework.md) follows the Hell Mode wiki: type order and ranks, Summoning levels, command ratios and limits, named summons. */
class SummonerCanonRulesTest {
    @Test
    void theNineTypesOpenAtTheWikisLevelsWithTheWikisFirstRanks() {
        SummonerRules.Family[] f = SummonerRules.Family.values();
        assertEquals(9, f.length);
        int[] levels = {1, 1, 2, 3, 4, 5, 6, 7, 8};
        char[] first = {'H', 'H', 'G', 'F', 'E', 'D', 'C', 'B', 'A'};
        for (int i = 0; i < f.length; i++) {
            assertEquals(levels[i], f[i].level, f[i].label);
            assertEquals(first[i], SummonerRules.rankLetter(f[i].firstRank()), f[i].label);
        }
        assertEquals("Insect + Beast", SummonerRules.Family.BIRD.recipe);
        assertEquals("Spirit + Stone", SummonerRules.Family.DRAGON.recipe);
        assertEquals(70, SummonerRules.Family.DRAGON.mp);
        assertEquals('S', SummonerRules.rankLetter(99), "the rank list ends at S");
    }

    @Test
    void summoningLevelRunsOneToSevenAndModesOpenWithIt() {
        assertEquals(1, SummonerRules.summoningLevel(0.0));
        assertEquals(1, SummonerRules.summoningLevel(0.14));
        assertEquals(2, SummonerRules.summoningLevel(1.0 / 7.0));
        assertEquals(7, SummonerRules.summoningLevel(1.0));
        assertEquals(7, SummonerRules.summoningLevel(5.0), "clamped");
        assertEquals(1, SummonerRules.summoningLevel(-1.0), "clamped");
        // the wiki: Synthesis and Bird at 2, Strengthening and Grass at 3, Stone and Storage at 4, Fish 5, Awakening and Spirit 6, Dragon and Deputize 7
        assertEquals(1, SummonerRules.canonLevel(SummonerRules.BEAST));
        assertEquals(1, SummonerRules.canonLevel(SummonerRules.INSECT));
        assertEquals(1, SummonerRules.canonLevel(SummonerRules.RECALL));
        assertEquals(2, SummonerRules.canonLevel(SummonerRules.SYNTHESIS));
        assertEquals(2, SummonerRules.canonLevel(SummonerRules.BIRD));
        assertEquals(3, SummonerRules.canonLevel(SummonerRules.STRENGTHEN));
        assertEquals(3, SummonerRules.canonLevel(SummonerRules.SPIRIT));
        assertEquals(4, SummonerRules.canonLevel(SummonerRules.STONE));
        assertEquals(4, SummonerRules.canonLevel(SummonerRules.STORAGE));
        assertEquals(5, SummonerRules.canonLevel(SummonerRules.FISH));
        assertEquals(6, SummonerRules.canonLevel(SummonerRules.AWAKENING));
        assertEquals(6, SummonerRules.canonLevel(SummonerRules.SPIRIT_CAST));
        assertEquals(7, SummonerRules.canonLevel(SummonerRules.DRAGON));
        assertEquals(7, SummonerRules.canonLevel(SummonerRules.DEPUTIZE));
        assertFalse(SummonerRules.unlockedCanon(SummonerRules.STONE, 0.4, false, false));
        assertTrue(SummonerRules.unlockedCanon(SummonerRules.STONE, 3.0 / 7.0, false, false));
        assertTrue(SummonerRules.unlockedCanon(SummonerRules.DEPUTIZE, 0.0, true, false));
        assertTrue(SummonerRules.unlockedCanon(SummonerRules.BIRD, 0.0, false, true), "a holder carried over from Hawk Eye keeps the Bird card");
        assertEquals(43, SummonerRules.canonUnlockPercent(SummonerRules.STONE));
    }

    @Test
    void modeIndexesAreAppendedAndTheBaseNeverListsUltimateCards() {
        assertEquals(9, SummonerRules.MODES, "the 4.5.0 count is kept");
        assertEquals(17, SummonerRules.MODES_CANON);
        for (int mode = SummonerRules.MODES; mode < SummonerRules.MODES_CANON; mode++) assertTrue(mode >= 9);
        assertFalse(SummonerRules.listedCanon(SummonerRules.ANGEL, false));
        assertTrue(SummonerRules.listedCanon(SummonerRules.ANGEL, true));
        assertFalse(SummonerRules.listedCanon(SummonerRules.LEGION, false));
        assertTrue(SummonerRules.listedCanon(SummonerRules.STORAGE, false));
        assertFalse(SummonerRules.listedCanon(SummonerRules.MODES_CANON, true));
    }

    @Test
    void namedSummonsSitOnTheirWikiRanks() {
        assertEquals("Wally", SummonerRules.canonName(SummonerRules.Family.STONE, 3));
        assertEquals("Hawk", SummonerRules.canonName(SummonerRules.Family.BIRD, 3));
        assertEquals("Maria", SummonerRules.canonName(SummonerRules.Family.SPIRIT, 5));
        assertEquals("Merus", SummonerRules.canonName(SummonerRules.Family.ANGEL, 7));
        assertNull(SummonerRules.canonName(SummonerRules.Family.STONE, 4), "Stone D has no name on the wiki");
        // a stone card starts as Wally (Stone E) and climbs D, Steely (C), Mira (B); a beast card keeps the plain letter
        assertEquals("Wally", SummonerRules.cardLabel(SummonerRules.Family.STONE, 0, true));
        assertEquals("Stone D", SummonerRules.cardLabel(SummonerRules.Family.STONE, 1, true));
        assertEquals("Steely", SummonerRules.cardLabel(SummonerRules.Family.STONE, 2, true));
        assertEquals("Mira", SummonerRules.cardLabel(SummonerRules.Family.STONE, 3, true));
        assertEquals("Stone E", SummonerRules.cardLabel(SummonerRules.Family.STONE, 0, false));
        assertEquals("Beast H", SummonerRules.cardLabel(SummonerRules.Family.BEAST, 0, false));
        assertEquals("Beast G", SummonerRules.cardLabel(SummonerRules.Family.BEAST, 1, false));
    }

    @Test
    void strengtheningKeepsTheWikisSixStepsAndRatios() {
        assertArrayEquals(new int[]{10, 20, 50, 100, 200, 500}, SummonerRules.STRENGTH_STEPS);
        assertEquals(1, SummonerRules.strengthenLevel(3.0 / 7.0 * 0.0, false), "level 1 when it opens");
        assertEquals(1, SummonerRules.strengthenLevel(2.0 / 7.0, false));
        assertEquals(5, SummonerRules.strengthenLevel(1.0, false));
        assertEquals(6, SummonerRules.strengthenLevel(0.0, true));
        for (int l = 2; l <= 6; l++) assertTrue(SummonerRules.strengthenBonus(l) > SummonerRules.strengthenBonus(l - 1));
        assertEquals(SummonerRules.strengthenBonus(6), SummonerRules.strengthenBonus(60), "clamped");
        assertEquals(5 * SummonerRules.strengthenBonus(1), SummonerRules.strengthenBonus(3), 1e-12, "+10 -> +50 is five times");
        assertEquals(10 * SummonerRules.strengthenBonus(1), SummonerRules.strengthenBonus(4), 1e-12);
        assertEquals(0.02, SummonerRules.strengthenBonus(1), 1e-12);
        assertEquals(1.0, SummonerRules.strengthenBonus(6), 1e-12);
    }

    @Test
    void commandTiersAreOneTwoFourAndLimitedLikeTheWiki() {
        double s = SummonerRules.commandBonus(SummonerRules.CMD_SOLDIER);
        assertEquals(2 * s, SummonerRules.commandBonus(SummonerRules.CMD_GENERAL), 1e-12);
        assertEquals(4 * s, SummonerRules.commandBonus(SummonerRules.CMD_KING), 1e-12);
        assertEquals(0.0, SummonerRules.commandBonus(SummonerRules.CMD_NONE));
        assertEquals(3, SummonerRules.MAX_GENERALS);
        assertEquals(1, SummonerRules.MAX_KINGS);
        assertEquals(SummonerRules.CMD_GENERAL, SummonerRules.nextCommand(SummonerRules.CMD_NONE, 2, 0, false));
        assertEquals(SummonerRules.CMD_GENERAL, SummonerRules.nextCommand(SummonerRules.CMD_SOLDIER, 0, 0, false));
        assertEquals(-1, SummonerRules.nextCommand(SummonerRules.CMD_NONE, 3, 0, true), "a kind has three Generals at most");
        assertEquals(SummonerRules.CMD_KING, SummonerRules.nextCommand(SummonerRules.CMD_GENERAL, 1, 0, true));
        assertEquals(SummonerRules.CMD_NONE, SummonerRules.nextCommand(SummonerRules.CMD_GENERAL, 1, 0, false), "no Kingnification: back to plain");
        assertEquals(SummonerRules.CMD_NONE, SummonerRules.nextCommand(SummonerRules.CMD_GENERAL, 1, 1, true), "one King a kind");
        assertEquals(SummonerRules.CMD_NONE, SummonerRules.nextCommand(SummonerRules.CMD_KING, 0, 0, true));
    }

    @Test
    void awakeningWaitsADayAndShortensWithTheTiers() {
        int day = SummonerRules.awakeningCooldown(SummonerRules.CMD_NONE);
        assertEquals(24000, day);
        assertEquals(day / 4, SummonerRules.awakeningCooldown(SummonerRules.CMD_SOLDIER));   // 24 h -> 6 h
        assertEquals(day / 8, SummonerRules.awakeningCooldown(SummonerRules.CMD_GENERAL));   // 3 h
        assertEquals(day / 24, SummonerRules.awakeningCooldown(SummonerRules.CMD_KING));     // 1 h
    }

    @Test
    void pricesKeepTheWikisMpRatios() {
        double card = 500.0;
        double synth = SummonerRules.synthesisCost(card), strength = SummonerRules.canonStrengthenCost(card), awaken = SummonerRules.awakeningCost(card);
        assertEquals(2.0, strength / synth, 1e-9, "Strengthening 10 MP : Synthesis 5 MP");
        assertEquals(10.0, awaken / strength, 1e-9, "Awakening 100 MP : Strengthening 10 MP");
        assertEquals(card, SummonerRules.typeCost(card, SummonerRules.Family.INSECT), 1e-9);
        assertTrue(SummonerRules.typeCost(card, SummonerRules.Family.DRAGON) > SummonerRules.typeCost(card, SummonerRules.Family.SPIRIT));
        assertTrue(SummonerRules.typeCost(card, SummonerRules.Family.SPIRIT) > SummonerRules.typeCost(card, SummonerRules.Family.FISH));
    }

    @Test
    void theCastsAreBoundedAndTheUltimateIsStronger() {
        assertEquals(8, SummonerRules.ANGEL_TARGETS);
        assertTrue(SummonerRules.dragonDamage(Tier.ULTIMATE) > SummonerRules.dragonDamage(Tier.BASE));
        assertTrue(SummonerRules.angelDamage(Tier.BASE) > SummonerRules.dragonDamage(Tier.BASE));
        assertTrue(SummonerRules.insectRange(Tier.ULTIMATE) <= 48.0 && SummonerRules.dragonRange(Tier.ULTIMATE) <= 48.0 && SummonerRules.spiritRange() <= 48.0 && SummonerRules.fishRange() <= 48.0);
        assertEquals(100 * 20, SummonerRules.castCooldown(100, SummonerRules.ANGEL));
        assertEquals(100, SummonerRules.castCooldown(100, SummonerRules.BEAST));
    }
}
