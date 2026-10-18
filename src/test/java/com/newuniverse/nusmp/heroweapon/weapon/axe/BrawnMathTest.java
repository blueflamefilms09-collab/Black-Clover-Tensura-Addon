package com.newuniverse.nusmp.heroweapon.weapon.axe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: Brawn is the Axe's build-up and the ammunition of its ultimate, so its arithmetic is pinned: stacks are held after the last gain, then fade by the clock (half as fast
 * with Radiance), never go below zero or above eight, and what each stack gives is bounded.
 */
class BrawnMathTest {
    @Test
    void stacksAreHeldThenFadeOneByOne() {
        assertEquals(6, BrawnMath.current(6, 1000, 1000, false));
        assertEquals(6, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD, false), "held for the whole hold");
        assertEquals(6, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD + BrawnMath.STEP - 1, false), "the first stack goes a step after the hold");
        assertEquals(5, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD + BrawnMath.STEP, false));
        assertEquals(4, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD + 2 * BrawnMath.STEP, false));
        assertEquals(0, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD + 6 * BrawnMath.STEP, false));
        assertEquals(0, BrawnMath.current(6, 1000, 1000 + 100000, false), "never below zero");
    }

    @Test
    void radianceMakesThemFadeHalfAsFast() {
        long later = 1000 + BrawnMath.HOLD + 2 * BrawnMath.STEP;
        assertEquals(4, BrawnMath.current(6, 1000, later, false));
        assertEquals(5, BrawnMath.current(6, 1000, later, true));
        assertEquals(6, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD + BrawnMath.STEP_SLOW - 1, true));
        assertEquals(5, BrawnMath.current(6, 1000, 1000 + BrawnMath.HOLD + BrawnMath.STEP_SLOW, true));
    }

    @Test
    void gainingAddsToWhatIsLeftAndStopsAtTheLimit() {
        assertEquals(3, BrawnMath.gain(0, 0, 500, 3, false));
        assertEquals(4, BrawnMath.gain(3, 500, 510, 1, false));
        assertEquals(BrawnMath.MAX, BrawnMath.gain(7, 500, 510, 5, false));
        long faded = 500 + BrawnMath.HOLD + 2 * BrawnMath.STEP;
        assertEquals(2, BrawnMath.gain(3, 500, faded, 1, false), "three faded to one, plus the new one is two");
    }

    @Test
    void whatEachStackGivesIsBounded() {
        assertEquals(0.0, BrawnMath.meleeBonus(0), 1e-9);
        assertEquals(0.24, BrawnMath.meleeBonus(8), 1e-9);
        assertEquals(0.24, BrawnMath.meleeBonus(80), 1e-9, "clamped to eight");
        assertEquals(1.16, BrawnMath.skillMult(8), 1e-9);
        assertEquals(0.32, BrawnMath.knockbackResistance(8), 1e-9);
        assertEquals(0.08, BrawnMath.speedBonus(8), 1e-9);
        assertEquals(1.0, BrawnMath.skillMult(-3), 1e-9);
        assertTrue(BrawnMath.meleeBonus(8) <= 0.25 && BrawnMath.knockbackResistance(8) <= 0.6);
    }

    @Test
    void shineBurstNeedsFiveAndGrowsWithBrawn() {
        assertFalse(BrawnMath.canShine(4));
        assertTrue(BrawnMath.canShine(5));
        assertEquals(25.0f, BrawnMath.shineRaw(5), 1e-4);
        assertEquals(31.6f, BrawnMath.shineRaw(8), 1e-4);
        assertEquals(31.6f, BrawnMath.shineRaw(12), 1e-4, "clamped to eight");
        assertTrue(BrawnMath.shineRaw(8) <= 32f);
    }
}
