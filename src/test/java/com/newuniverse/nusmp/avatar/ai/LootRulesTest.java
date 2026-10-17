package com.newuniverse.nusmp.avatar.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The looting rules: what an Avatar may pick up, what it wants most, how far a Merchant looks, and what it hands to its creator. */
class LootRulesTest {
    private static LootRules.Drop drop(int rarity, boolean upgrade, boolean valuable, boolean stack, boolean treasure, int age, boolean byPlayer, double dist) {
        return new LootRules.Drop(rarity, upgrade, valuable, stack, treasure, age, byPlayer, dist);
    }

    private static LootRules.Drop plain(double dist) { return drop(0, false, false, false, false, 100, false, dist); }

    // ---------------------------------------------------------------- may it take it
    @Test
    void neverAnItemAPlayerJustDropped() {
        assertFalse(LootRules.allowed(drop(0, false, false, false, false, 59, true, 2), 60, true), "dropped 59 ticks ago by a player");
        assertTrue(LootRules.allowed(drop(0, false, false, false, false, 60, true, 2), 60, true), "after three seconds it is fair game");
        assertTrue(LootRules.allowed(drop(0, false, false, false, false, 5, false, 2), 60, true), "a mob's drop has no grace");
    }

    @Test
    void fullPocketsOnlyTakeUpgradesAndRareFinds() {
        assertFalse(LootRules.allowed(plain(2), 60, false));
        assertTrue(LootRules.allowed(drop(0, true, false, false, false, 100, false, 2), 60, false), "an armour upgrade is always worth the swap");
        assertTrue(LootRules.allowed(drop(2, false, false, false, false, 100, false, 2), 60, false), "so is a rare item");
        assertTrue(LootRules.allowed(plain(2), 60, true));
    }

    // ---------------------------------------------------------------- how much it wants it
    @Test
    void rarityValuablesAndUpgradesRaisePriority() {
        double common = LootRules.priority(plain(4), false);
        double rare = LootRules.priority(drop(2, false, false, false, false, 100, false, 4), false);
        double epic = LootRules.priority(drop(3, false, false, false, false, 100, false, 4), false);
        double upgrade = LootRules.priority(drop(0, true, false, false, false, 100, false, 4), false);
        double stack = LootRules.priority(drop(0, false, false, true, false, 100, false, 4), false);
        assertTrue(rare > common && epic > rare, "rarity ladder");
        assertTrue(upgrade > common && stack > common);
    }

    @Test
    void distanceLowersPriority() {
        assertTrue(LootRules.priority(plain(2), false) > LootRules.priority(plain(12), false));
        assertTrue(LootRules.priority(plain(-5), false) > 0.0, "a bad distance never breaks it");
    }

    @Test
    void aMerchantWantsRareDropsMoreButOrdinaryOnesAsMuchAsAnyone() {
        LootRules.Drop rare = drop(2, false, false, false, false, 100, false, 6);
        assertEquals(LootRules.priority(rare, false) * 2.5, LootRules.priority(rare, true), 1e-9);
        assertEquals(LootRules.priority(plain(6), false), LootRules.priority(plain(6), true), 1e-9);
    }

    // ---------------------------------------------------------------- how far it looks
    @Test
    void aMerchantSeesRareDropsFromFarAway() {
        LootRules.Drop farRare = drop(3, false, false, false, false, 100, false, 20);
        LootRules.Drop farPlain = plain(20);
        assertTrue(LootRules.withinReach(farRare, 8, 24, true));
        assertFalse(LootRules.withinReach(farRare, 8, 24, false), "anyone else only sees 8 blocks");
        assertFalse(LootRules.withinReach(farPlain, 8, 24, true), "a Merchant's long sight is for rare drops only");
        assertTrue(LootRules.isRare(drop(0, false, false, true, false, 0, false, 1)), "a stack of valuables is rare");
        assertTrue(LootRules.isRare(drop(0, false, false, false, true, 0, false, 1)), "so is a relic or a magic stone");
        assertFalse(LootRules.isRare(plain(1)));
    }

    @Test
    void aMerchantRunsForRareDropsOnly() {
        assertTrue(LootRules.speed(drop(2, false, false, false, false, 0, false, 5), true) > LootRules.speed(plain(5), true));
        assertEquals(LootRules.speed(plain(5), false), LootRules.speed(drop(2, false, false, false, false, 0, false, 5), false), 1e-9);
    }

    @Test
    void pickChoosesTheBestAllowedDropInReach() {
        List<LootRules.Drop> l = List.of(plain(3), drop(2, false, false, false, false, 100, false, 5), drop(3, false, false, false, false, 5, true, 2));
        assertEquals(1, LootRules.pick(l, 60, true, 8, 24, false), "the epic is a player's fresh drop: refused, so the rare one wins");
        assertEquals(-1, LootRules.pick(List.of(), 60, true, 8, 24, false));
        assertEquals(-1, LootRules.pick(List.of(plain(30)), 60, true, 8, 24, true), "out of reach");
        assertEquals(0, LootRules.pick(List.of(drop(2, false, false, false, false, 100, false, 20)), 60, true, 8, 24, true), "a Merchant goes for the far rare one");
    }

    // ---------------------------------------------------------------- handing over
    @Test
    void handOverNeedsTheSettingTheCreatorNearAndSomethingToGive() {
        LootRules.Pockets some = new LootRules.Pockets(2, 18, 3);
        assertFalse(LootRules.shouldHandOver(false, true, true, some));
        assertFalse(LootRules.shouldHandOver(true, false, true, some));
        assertFalse(LootRules.shouldHandOver(true, true, true, new LootRules.Pockets(2, 18, 0)));
        assertTrue(LootRules.shouldHandOver(true, true, true, some));
    }

    @Test
    void unaskedItOnlyHandsOverWhenFull() {
        assertFalse(LootRules.shouldHandOver(true, true, false, new LootRules.Pockets(10, 18, 3)));
        assertTrue(LootRules.shouldHandOver(true, true, false, new LootRules.Pockets(2, 18, 3)));
        assertTrue(LootRules.shouldHandOver(true, true, false, new LootRules.Pockets(0, 18, 1)));
    }

    @Test
    void handOverIndicesRespectTheLimit() {
        assertEquals(List.of(0, 2), LootRules.handOverIndices(List.of(true, false, true, true), 2));
        assertEquals(List.of(), LootRules.handOverIndices(List.of(false, false), 5));
    }

    // ---------------------------------------------------------------- what is valuable
    @Test
    void valuablesAreTheUsualSuspects() {
        for (String p : new String[]{"diamond", "emerald", "netherite_ingot", "nether_star", "totem_of_undying", "elytra", "heart_of_the_sea"})
            assertTrue(LootRules.isValuable(p), p);
        for (String p : new String[]{"dirt", "cobblestone", "rotten_flesh", "gold_ingot", ""}) assertFalse(LootRules.isValuable(p), p);
        assertFalse(LootRules.isValuable(null));
        assertTrue(LootRules.isValuable("DIAMOND"), "case does not matter");
    }

    @Test
    void goldIsOnlyValuableInNumbers() {
        assertTrue(LootRules.isValuableStack("gold_ingot", 8));
        assertFalse(LootRules.isValuableStack("gold_ingot", 7));
        assertFalse(LootRules.isValuableStack("dirt", 64));
        assertFalse(LootRules.isValuableStack(null, 64));
    }

    @Test
    void whatLiesInAGraveIsNeverTaken() {
        LootRules.Drop grave = new LootRules.Drop(3, true, true, true, true, 10_000, false, 2.0, true);
        assertFalse(LootRules.allowed(grave, 60, true), "not even a rare upgrade, not even with room, not even long after");
        LootRules.Drop same = new LootRules.Drop(3, true, true, true, true, 10_000, false, 2.0, false);
        assertTrue(LootRules.allowed(same, 60, true));
        LootRules.Drop old = new LootRules.Drop(0, false, false, false, false, 5, false, 2.0);
        assertFalse(old.grave(), "the short constructor is a plain drop");
    }
}
