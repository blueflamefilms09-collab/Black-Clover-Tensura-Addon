package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: light is one of the three things the dice read, and its edges (what counts as dark, what counts as lit, what a torch in hand is worth) decide which
 * table entries a party meets. These tests pin the limits, the modifier curve and the held-light rule.
 */
class LightRulesTest {
    private static LightRules.Light of(int light, boolean held) {
        return LightRules.evaluate(new int[] {light}, new boolean[] {held});
    }

    @Test
    void theModifierIsZeroInTheMiddleAndRisesWithBrightness() {
        assertEquals(0, LightRules.mod(8));
        assertEquals(0, LightRules.mod(7));
        assertEquals(0, LightRules.mod(9));
        assertEquals(1, LightRules.mod(10));
        assertEquals(2, LightRules.mod(12));
        assertEquals(3, LightRules.mod(15));
        assertEquals(-1, LightRules.mod(6));
        assertEquals(-2, LightRules.mod(4));
        assertEquals(-3, LightRules.mod(0));
        for (int b = 0; b < 15; b++) assertTrue(LightRules.mod(b) <= LightRules.mod(b + 1), "never falls as the light rises at " + b);
    }

    @Test
    void theModifierStaysInsideThreeEitherWayWhateverIsPassed() {
        assertEquals(3, LightRules.mod(99));
        assertEquals(-3, LightRules.mod(-5));
    }

    @Test
    void aPartyInTheDarkWithNothingLitIsDarkAndNotLit() {
        LightRules.Light l = of(2, false);
        assertTrue(l.dark());
        assertFalse(l.lit());
        assertEquals(-2, l.mod());
        assertEquals(2, l.brightness());
    }

    @Test
    void aTorchInHandMakesADarkRoomABrightParty() {
        LightRules.Light l = of(0, true);
        assertFalse(l.dark());
        assertTrue(l.lit());
        assertEquals(LightRules.HELD_LIGHT, l.brightness());
        assertEquals(2, l.mod());
    }

    @Test
    void aPartyInAnOrdinaryDimRoomIsNeitherDarkNorLit() {
        LightRules.Light l = of(9, false);
        assertFalse(l.dark());
        assertFalse(l.lit());
        assertEquals(0, l.mod());
    }

    @Test
    void theBrightestMemberDecidesForTheWholeParty() {
        LightRules.Light l = LightRules.evaluate(new int[] {1, 2, 15}, new boolean[] {false, false, false});
        assertTrue(l.lit());
        assertFalse(l.dark());
        assertEquals(15, l.brightness());
        LightRules.Light holder = LightRules.evaluate(new int[] {1, 1, 1}, new boolean[] {false, true, false});
        assertTrue(holder.lit(), "one member with a lantern lights the party for a hunter");
    }

    @Test
    void theLimitsAreInclusive() {
        assertTrue(of(LightRules.DARK_LIMIT, false).dark());
        assertFalse(of(LightRules.DARK_LIMIT + 1, false).dark());
        assertTrue(of(LightRules.LIT_LIMIT, false).lit());
        assertFalse(of(LightRules.LIT_LIMIT - 1, false).lit());
    }

    @Test
    void anEmptyPartyOrMissingArraysAreHandled() {
        LightRules.Light none = LightRules.evaluate(new int[0], new boolean[0]);
        assertEquals(0, none.mod());
        assertFalse(none.dark());
        assertFalse(none.lit());
        assertEquals(0, LightRules.evaluate(null, null).mod());
        LightRules.Light shortHeld = LightRules.evaluate(new int[] {3, 3}, new boolean[] {false});
        assertTrue(shortHeld.dark());
        LightRules.Light noHeld = LightRules.evaluate(new int[] {3}, null);
        assertTrue(noHeld.dark());
    }

    @Test
    void outOfRangeLightValuesAreClamped() {
        assertEquals(15, LightRules.evaluate(new int[] {40}, null).brightness());
        assertEquals(0, LightRules.evaluate(new int[] {-7}, null).brightness());
    }
}
