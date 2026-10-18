package com.newuniverse.nusmp.heroweapon.weapon.axe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the Axe's execution is a bounded BONUS, never a kill command: only a wounded non-boss is eligible, a player needs to be lower than a mob and gets less, and the result is
 * clamped to the cap of the technique's kind. Each edge is pinned.
 */
class ExecuteMathTest {
    @Test
    void aWoundedMobIsEligibleAtAQuarterOfItsHealth() {
        assertTrue(ExecuteMath.eligible(5f, 20f, false, false), "exactly a quarter");
        assertTrue(ExecuteMath.eligible(1f, 20f, false, false));
        assertFalse(ExecuteMath.eligible(5.1f, 20f, false, false), "just above a quarter");
    }

    @Test
    void aPlayerNeedsToBeLowerThanAMob() {
        assertTrue(ExecuteMath.eligible(4f, 20f, false, true), "exactly a fifth");
        assertFalse(ExecuteMath.eligible(5f, 20f, false, true), "a quarter is not enough for a player");
    }

    @Test
    void aBossAndTheDeadAreNeverEligible() {
        assertFalse(ExecuteMath.eligible(1f, 500f, true, false));
        assertFalse(ExecuteMath.eligible(0f, 20f, false, false));
        assertFalse(ExecuteMath.eligible(5f, 0f, false, false));
    }

    @Test
    void theBonusIsSmallerOnAPlayerAndClampedToTheCap() {
        assertEquals(9f * 2.2f, ExecuteMath.raw(9f, 1.0, true, false, 20f), 1e-4);
        assertEquals(9f * 1.4f, ExecuteMath.raw(9f, 1.0, true, true, 20f), 1e-4);
        assertEquals(9f, ExecuteMath.raw(9f, 1.0, false, false, 20f), 1e-4, "no bonus when not eligible");
        assertEquals(20f, ExecuteMath.raw(9f, 1.16, true, false, 20f), 1e-4, "clamped");
        assertEquals(7.5f, ExecuteMath.raw(9f, 1.0, false, false, 7.5f), 1e-4, "the cap also holds without a bonus");
        assertTrue(ExecuteMath.multiplier(false) > ExecuteMath.multiplier(true));
    }
}
