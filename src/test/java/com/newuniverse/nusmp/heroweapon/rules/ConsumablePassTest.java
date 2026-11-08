package com.newuniverse.nusmp.heroweapon.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.rules.ConsumablePass.Kind;
import org.junit.jupiter.api.Test;

/** Why: the Legendary weapons swallowed every click, so no food or potion could be used from the off hand (3.63.0). This pins when the click passes on and when the weapon keeps it. */
class ConsumablePassTest {
    @Test
    void aDrinkInTheOffHandAlwaysGetsTheClick() {
        assertTrue(ConsumablePass.offhandFirst(true, false, Kind.DRINK, false));
    }

    @Test
    void aFoodGetsTheClickOnlyWhenThePlayerCanEatIt() {
        assertTrue(ConsumablePass.offhandFirst(true, false, Kind.FOOD, true));
        assertFalse(ConsumablePass.offhandFirst(true, false, Kind.FOOD, false), "a full stomach: the weapon keeps the click (its guard, its draw, its technique)");
    }

    @Test
    void sneakAndUseStaysTheWeaponsGesture() {
        assertFalse(ConsumablePass.offhandFirst(true, true, Kind.DRINK, true));
        assertFalse(ConsumablePass.offhandFirst(true, true, Kind.FOOD, true));
    }

    @Test
    void nothingUsableOrTheSettingOffChangesNothing() {
        assertFalse(ConsumablePass.offhandFirst(true, false, Kind.NONE, true));
        assertFalse(ConsumablePass.offhandFirst(true, false, null, true));
        assertFalse(ConsumablePass.offhandFirst(false, false, Kind.DRINK, true), "the setting off restores the old behaviour");
    }
}
