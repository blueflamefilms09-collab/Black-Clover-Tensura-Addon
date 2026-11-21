package com.newuniverse.nusmp.dragonballs;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the overpowered wishes: the table, caps and counts, the item blocklist, the scatter guard, the weapon week, the rank target and the spoken vocabulary. The old mode must stay exactly as it was. */
class WishRulesOverpoweredTest {
    @Test
    void theOldModeIsExactlyTheOldTable() {
        for (BallSet s : BallSet.values()) {
            assertEquals(WishRules.forSet(s), WishRules.forSet(s, false));
            assertEquals(1, WishRules.powerLevels(BallSet.EARTH, false));
            assertEquals(2, WishRules.powerLevels(BallSet.SUPER, false));
            assertEquals(WishRules.coins(s), WishRules.coins(s, false));
            assertEquals(WishRules.wishCount(s, 50), WishRules.wishCount(s, 50, false));
        }
        assertEquals(3, WishRules.wishCount(BallSet.SUPER, 50, false));
        assertEquals(10, WishRules.powerCap(10, false));
        assertEquals(5, WishRules.powerCap(5, false));
        assertNull(WishRules.match(BallSet.SUPER, "become a high orc", false), "no race wish in the old mode");
        assertNull(WishRules.byId("race"));
        assertNull(WishRules.byId("rank", false));
    }

    @Test
    void overpoweredAddsRankAndRaceAndKeepsEveryOldWish() {
        for (BallSet s : BallSet.values()) {
            Set<String> ids = new HashSet<>();
            for (WishRules.Wish w : WishRules.forSet(s, true)) assertTrue(ids.add(w.id()), w.id());
            for (WishRules.Wish w : WishRules.ALL) assertTrue(ids.contains(w.id()), "kept: " + w.id());
            assertTrue(ids.contains("rank") && ids.contains("race"));
        }
        assertEquals(WishRules.ALL.size() + 2, WishRules.forSet(BallSet.EARTH, true).size(), "the weapon is open to Earth too");
    }

    @Test
    void theNewWishesAreMatchedFromTypedSentences() {
        assertEquals(WishRules.Kind.RACE, WishRules.match(BallSet.EARTH, "I wish to become a high orc", true).kind());
        assertEquals(WishRules.Kind.RACE, WishRules.match(BallSet.SUPER, "race demon lord", true).kind());
        assertEquals(WishRules.Kind.RANK, WishRules.match(BallSet.EARTH, "change my rate", true).kind());
        assertEquals(WishRules.Kind.RANK, WishRules.match(BallSet.SUPER, "promote me", true).kind());
        assertEquals(WishRules.Kind.SAIYAN, WishRules.match(BallSet.SUPER, "become a saiyan", true).kind(), "the Saiyan wish keeps its place before the race wish");
        assertEquals(WishRules.Kind.SKILL, WishRules.match(BallSet.SUPER, "skill eye of truth", true).kind());
        assertEquals(WishRules.Kind.WEAPON, WishRules.match(BallSet.EARTH, "I want a weapon", true).kind());
        assertNull(WishRules.match(BallSet.SUPER, "blah blah", true));
    }

    @Test
    void everyForbiddenWordIsStillRefused() {
        for (String w : List.of("make me immortal", "mind control him", "destroy the world", "kill everyone", "enslave them", "ban the server")) assertTrue(WishRules.forbidden(w), w);
        assertFalse(WishRules.forbidden("become a high orc"));
    }

    @Test
    void countsCapsAndAmountsAreBounded() {
        assertEquals(3, WishRules.wishCount(BallSet.EARTH, 3, true));
        assertEquals(5, WishRules.wishCount(BallSet.SUPER, 5, true));
        assertEquals(10, WishRules.wishCount(BallSet.SUPER, 500, true));
        assertEquals(1, WishRules.wishCount(BallSet.SUPER, 0, true));
        assertEquals(2, WishRules.powerLevels(BallSet.EARTH, true));
        assertEquals(4, WishRules.powerLevels(BallSet.SUPER, true));
        assertEquals(20, WishRules.powerCap(3, true));
        assertEquals(0, WishRules.addable(20, 4, WishRules.powerCap(3, true)));
        assertEquals(1_000_000L, WishRules.coins(BallSet.EARTH, true), "100 gold coins");
        assertEquals(10_000_000L, WishRules.coins(BallSet.SUPER, true), "1000 gold coins");
    }

    @Test
    void theReviveWindowIsUnlimitedOnlyWhileOverpowered() {
        long old = 10L * 24 * 3600 * 1000;
        assertTrue(WishRules.reviveInWindow(true, old, 10));
        assertFalse(WishRules.reviveInWindow(false, old, 10));
        assertTrue(WishRules.reviveInWindow(false, 5 * 60_000L, 10));
        assertFalse(WishRules.reviveInWindow(false, -1, 10));
    }

    @Test
    void theItemBlocklistRefusesOperatorAndCreativeOnlyItems() {
        for (String id : java.util.Arrays.asList("minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block", "minecraft:barrier", "minecraft:structure_block", "minecraft:jigsaw",
                "minecraft:light", "minecraft:debug_stick", "minecraft:bedrock", "minecraft:spawner", "minecraft:creeper_spawn_egg", "anime_multiverse:dragon_ball_earth_3", "somemod:admin_wand", "air", "", null)) assertTrue(WishRules.itemBlocked(id), String.valueOf(id));
        for (String id : List.of("minecraft:diamond", "minecraft:netherite_ingot", "minecraft:elytra", "tensura:magic_ore", "minecraft:enchanted_golden_apple")) assertFalse(WishRules.itemBlocked(id), id);
    }

    @Test
    void anItemWishIsOneStackAndAtMost64() {
        assertEquals(64, WishRules.itemCount(0, 64));
        assertEquals(16, WishRules.itemCount(0, 16));
        assertEquals(1, WishRules.itemCount(0, 1), "a tool: one");
        assertEquals(10, WishRules.itemCount(10, 64));
        assertEquals(64, WishRules.itemCount(5000, 64));
        assertEquals(16, WishRules.itemCount(40, 16));
        assertEquals(64, WishRules.itemCount(99, 99), "never above 64 whatever the item says");
        assertEquals(64, WishRules.numberIn("give me 64 diamonds"));
        assertEquals(10, WishRules.numberIn("a stack of 10 apples"));
        assertEquals(0, WishRules.numberIn("diamond"));
        assertEquals(0, WishRules.numberIn("123456789 diamonds"), "a huge number is ignored");
    }

    @Test
    void theScatterHappensOnceAtTheFirstGrantedWishOnly() {
        assertTrue(WishRules.scatterNow(true, 1, false));
        assertFalse(WishRules.scatterNow(true, 0, false), "a refused wish does not scatter");
        assertFalse(WishRules.scatterNow(true, 1, true), "never twice");
        assertFalse(WishRules.scatterNow(true, 5, true));
        assertFalse(WishRules.scatterNow(false, 1, false), "the old mode scatters at the end of the ritual");
    }

    @Test
    void theFlashIsBoundedPerBallAndPerSet() {
        assertEquals(16, WishRules.FLASH_PER_BALL);
        assertEquals(8 * 16, WishRules.flashPerSet(8));
        assertEquals(DbRules.MAX_COUNT * 16, WishRules.flashPerSet(1000));
        assertEquals(0, WishRules.flashPerSet(-3));
    }

    @Test
    void theWeaponCanBeWishedOncePerRealWeek() {
        long now = 10 * WishRules.WEEK_MS;
        assertTrue(WishRules.weaponReady(0, now), "never wished");
        assertFalse(WishRules.weaponReady(now - 1000, now));
        assertTrue(WishRules.weaponReady(now - WishRules.WEEK_MS, now));
        assertTrue(WishRules.weaponReady(now + 5000, now), "a clock that went back never locks a player out");
        assertEquals(0, WishRules.weaponWaitMs(0, now));
        assertEquals(WishRules.WEEK_MS - 1000, WishRules.weaponWaitMs(now - 1000, now));
    }

    @Test
    void relicGradesStopAtGodUnlessGenesisIsAllowed() {
        assertTrue(WishRules.relicGradeAllowed(0, false));
        assertTrue(WishRules.relicGradeAllowed(5, false));
        assertFalse(WishRules.relicGradeAllowed(6, false));
        assertFalse(WishRules.relicGradeAllowed(7, false));
        assertTrue(WishRules.relicGradeAllowed(7, true));
    }

    @Test
    void theRankWishNeverLowersAndNeverPassesTheCeiling() {
        assertEquals(14, WishRules.rankCeiling(BallSet.EARTH));
        assertEquals(15, WishRules.rankCeiling(BallSet.SUPER));
        assertEquals(5, WishRules.rankTarget(BallSet.EARTH, 0, -1));
        assertEquals(14, WishRules.rankTarget(BallSet.EARTH, 12, -1));
        assertEquals(-1, WishRules.rankTarget(BallSet.EARTH, 14, -1), "nothing to raise");
        assertEquals(15, WishRules.rankTarget(BallSet.SUPER, 3, -1), "Super goes to its ceiling");
        assertEquals(-1, WishRules.rankTarget(BallSet.SUPER, 15, -1));
        assertEquals(-1, WishRules.rankTarget(BallSet.SUPER, 16, -1), "the Wizard King stays above");
        assertEquals(8, WishRules.rankTarget(BallSet.EARTH, 2, 8));
        assertEquals(14, WishRules.rankTarget(BallSet.EARTH, 2, 16), "a named rank is cut at the ceiling");
        assertEquals(-1, WishRules.rankTarget(BallSet.EARTH, 9, 4), "never lowers");
    }

    @Test
    void theSpokenVocabularyHoldsTheWishWordsAndTheLiveNamesAndIsBounded() {
        var oldRaces = WishRules.races;
        var oldSkills = WishRules.skills;
        try {
            WishRules.races = () -> WishNames.build(List.of("tensura:high_orc", "tensura:human"), null, null);
            WishRules.skills = () -> WishNames.build(List.of("tensura:eye_of_truth"), null, null);
            List<String> v = WishRules.spokenVocabulary();
            for (String w : List.of("race", "skill", "revive", "rank", "saiyan", "orc", "truth", "eye", "cancel")) assertTrue(v.contains(w), w);
            assertEquals(v.size(), new HashSet<>(v).size(), "no duplicates");
            for (String w : v) assertEquals(w, w.toLowerCase(java.util.Locale.ROOT));
            // a huge list is cut at 2000, the wish words first
            WishRules.skills = () -> {
                java.util.ArrayList<String> ids = new java.util.ArrayList<>();
                for (int i = 0; i < 5000; i++) ids.add("m:skill_word" + Integer.toString(i, 36) + "x");
                return WishNames.build(ids, null, null);
            };
            List<String> big = WishRules.spokenVocabulary();
            assertEquals(2000, big.size());
            assertTrue(big.contains("race"));
            // a list that is not ready yet adds nothing and does not throw
            WishRules.skills = () -> { throw new IllegalStateException("not ready"); };
            assertTrue(WishRules.spokenVocabulary().contains("race"));
        } finally {
            WishRules.races = oldRaces;
            WishRules.skills = oldSkills;
        }
    }
}
