package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.ai.WeaponKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** When a found piece of gear replaces the worn one, and when it never does. */
class GearScoreTest {
    private static GearScore.Armor armor(int def, double tough, int ench, int left, int max) { return new GearScore.Armor(def, tough, 0.0, ench, left, max); }

    // ---------------------------------------------------------------- armour
    @Test
    void nothingWornTakesAnyRealArmour() {
        assertTrue(GearScore.betterArmor(armor(2, 0, 0, 100, 100), null));
        assertTrue(GearScore.betterArmor(armor(2, 0, 0, 100, 100), armor(0, 0, 0, 0, 0)));
        assertFalse(GearScore.betterArmor(armor(0, 0, 0, 100, 100), null), "a piece worth nothing is not worn");
        assertFalse(GearScore.betterArmor(null, null));
    }

    @Test
    void ironBeatsLeatherAndNetheriteBeatsIron() {
        GearScore.Armor leather = armor(1, 0, 0, 80, 80), iron = armor(2, 0, 0, 165, 165), dia = armor(3, 2, 0, 363, 363), neth = armor(3, 3, 0, 407, 407);
        assertTrue(GearScore.betterArmor(iron, leather));
        assertFalse(GearScore.betterArmor(leather, iron));
        assertTrue(GearScore.betterArmor(neth, dia));
    }

    @Test
    void aSlightImprovementDoesNotTriggerASwap() {
        GearScore.Armor a = armor(5, 0, 0, 100, 100), b = armor(5, 0, 0, 100, 100);
        assertFalse(GearScore.betterArmor(b, a), "equal pieces never trade places");
        assertFalse(GearScore.betterArmor(armor(5, 0, 0, 100, 100), armor(5, 0, 0, 98, 100)), "within the 5% margin");
    }

    @Test
    void aNearlyBrokenPieceIsNeverEquipped() {
        assertFalse(GearScore.betterArmor(armor(8, 3, 4, 3, 407), armor(1, 0, 0, 80, 80)), "3 of 407 durability left");
    }

    @Test
    void enchantmentsAndWearAreCounted() {
        assertTrue(GearScore.armor(armor(3, 0, 8, 100, 100)) > GearScore.armor(armor(3, 0, 0, 100, 100)));
        assertTrue(GearScore.armor(armor(3, 0, 0, 100, 100)) > GearScore.armor(armor(3, 0, 0, 20, 100)));
        assertEquals(0.0, GearScore.armor(null), 1e-9);
        assertEquals(1.0, armor(1, 0, 0, 0, 0).wear(), 1e-9, "an item that cannot wear out is as good as new");
    }

    // ---------------------------------------------------------------- weapons
    @Test
    void aBetterSwordReplacesAWorseOne() {
        GearScore.Weapon wood = new GearScore.Weapon(4, 0, 59, 59), diamond = new GearScore.Weapon(7, 0, 1561, 1561);
        assertTrue(GearScore.betterWeapon(diamond, wood));
        assertFalse(GearScore.betterWeapon(wood, diamond));
        assertFalse(GearScore.betterWeapon(diamond, diamond));
        assertTrue(GearScore.betterWeapon(wood, null));
    }

    @Test
    void sharpnessCounts() {
        GearScore.Weapon plain = new GearScore.Weapon(7, 0, 100, 100), sharp = new GearScore.Weapon(7, 5, 100, 100);
        assertTrue(GearScore.betterWeapon(sharp, plain));
    }

    @Test
    void theMainHandKeepsItsKindWhenSomethingElseIsFound() {
        // empty hand: takes a sword, takes a focus, refuses a shield and a bow without arrows
        assertTrue(GearScore.equipMainHand(WeaponKind.NONE, 0, WeaponKind.MELEE, 4, false));
        assertTrue(GearScore.equipMainHand(WeaponKind.NONE, 0, WeaponKind.FOCUS, 0, false));
        assertFalse(GearScore.equipMainHand(WeaponKind.NONE, 0, WeaponKind.SHIELD, 0, true));
        assertFalse(GearScore.equipMainHand(WeaponKind.NONE, 0, WeaponKind.BOW, 5, false));
        assertTrue(GearScore.equipMainHand(WeaponKind.NONE, 0, WeaponKind.BOW, 5, true));
        // a swordsman keeps its sword when it finds a bow, but swaps for a better sword
        assertFalse(GearScore.equipMainHand(WeaponKind.MELEE, 4, WeaponKind.BOW, 9, true));
        assertTrue(GearScore.equipMainHand(WeaponKind.MELEE, 4, WeaponKind.MELEE, 7, true));
        assertFalse(GearScore.equipMainHand(WeaponKind.MELEE, 7, WeaponKind.MELEE, 7, true));
        // a mage keeps its staff
        assertFalse(GearScore.equipMainHand(WeaponKind.FOCUS, 0, WeaponKind.MELEE, 9, true));
        assertFalse(GearScore.equipMainHand(WeaponKind.NONE, 0, WeaponKind.NONE, 0, true));
    }
}
