package com.newuniverse.nusmp.heroweapon.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 4.89.4: the pure rules that keep the hold-and-right-click items (the hourglass) working next to the weapons. */
class UtilityItemsTest {
    @Test
    void nameWordsAreWholeWordsOfThisModOnly() {
        assertTrue(UtilityItems.nameIsUtility("anime_multiverse", "zogratis_hourglass"));
        assertTrue(UtilityItems.nameIsUtility("anime_multiverse", "chronicle_sigil"));
        assertTrue(UtilityItems.nameIsUtility("anime_multiverse", "dragon_radar"));
        assertTrue(UtilityItems.nameIsUtility("anime_multiverse", "dragon_ball_earth_3"));
        assertTrue(UtilityItems.nameIsUtility("anime_multiverse", "vassal_seal_hammer"));
        assertTrue(UtilityItems.nameIsUtility("anime_multiverse", "calling_gem"));
        assertFalse(UtilityItems.nameIsUtility("anime_multiverse", "monkey_blade"), "a word inside another word is no match");
        assertFalse(UtilityItems.nameIsUtility("anime_multiverse", "genesis_demon_slayer"));
        assertFalse(UtilityItems.nameIsUtility("tensura", "magic_book"), "other mods' items are matched by the tag only");
        assertFalse(UtilityItems.nameIsUtility("anime_multiverse", null));
    }

    @Test
    void theTagExemptsOnlyWhileTheSettingIsOn() {
        assertTrue(UtilityItems.exempt(true, true));
        assertFalse(UtilityItems.exempt(false, true), "setting off = the old behaviour");
        assertFalse(UtilityItems.exempt(true, false));
    }

    @Test
    void theUseAnimationIsIgnoredOnlyForARealUtility() {
        assertTrue(UtilityItems.ignoreUseAnimation(true, true, false, false, 0.0), "the hourglass: BOW animation, no attack");
        assertFalse(UtilityItems.ignoreUseAnimation(false, true, false, false, 0.0), "setting off");
        assertFalse(UtilityItems.ignoreUseAnimation(true, false, false, false, 0.0), "not a utility");
        assertFalse(UtilityItems.ignoreUseAnimation(true, true, true, false, 0.0), "declares a weapon class");
        assertFalse(UtilityItems.ignoreUseAnimation(true, true, false, true, 0.0), "a relic keeps its class");
        assertFalse(UtilityItems.ignoreUseAnimation(true, true, false, false, WeaponClassifier.OTHER_WEAPON_ATTACK), "hits like a weapon");
    }

    @Test
    void anAnimationOnlyItemIsABowWithoutTheIgnoreAndNothingWithIt() {
        WeaponClassifier.Facts bowAnim = new WeaponClassifier.Facts(null, WeaponClassifier.BOW, 0.0);
        assertEquals(WeaponClass.BOW, WeaponClassifier.classify(bowAnim), "the old behaviour: the animation alone made the hourglass a bow");
        assertTrue(WeaponClassifier.rejects(com.newuniverse.nusmp.heroweapon.HeroKind.SWORD, WeaponClass.BOW));
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(new WeaponClassifier.Facts(null, 0, 0.0)), "with the animation ignored it is no weapon");
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(new WeaponClassifier.Facts(null, WeaponClassifier.ALLOWED | WeaponClassifier.BOW, 0.0)), "the tag flag wins");
    }

    @Test
    void theMainHandYieldsOnlyForAWeaponAndAReadyUtility() {
        assertTrue(UtilityItems.mainYields(true, false, true, false, true, true));
        assertFalse(UtilityItems.mainYields(false, false, true, false, true, true), "setting off = the weapon takes every click");
        assertFalse(UtilityItems.mainYields(true, true, true, false, true, true), "sneak + use stays the weapon's gesture");
        assertFalse(UtilityItems.mainYields(true, false, false, false, true, true), "a main hand that is not a weapon keeps its click");
        assertFalse(UtilityItems.mainYields(true, false, true, true, true, true), "a utility in the main hand keeps its click");
        assertFalse(UtilityItems.mainYields(true, false, true, false, false, true), "nothing to hand on to");
        assertFalse(UtilityItems.mainYields(true, false, true, false, true, false), "the off-hand item is on cooldown: the weapon keeps its click");
    }
}
