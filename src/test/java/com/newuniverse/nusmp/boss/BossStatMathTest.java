package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of the Black Clover boss buff (3.64.0): who counts, the clamp, the health modifier, the damage and its one-blow ceiling, the kept share of the bar. */
class BossStatMathTest {
    @Test
    void onlyABossOfTheBlackCloverWorkCounts() {
        assertTrue(BossStatMath.applies("Black Clover", true));
        assertTrue(BossStatMath.applies("black clover", true), "case does not matter");
        assertFalse(BossStatMath.applies("Black Clover", false), "an NPC is not a boss");
        assertFalse(BossStatMath.applies("DanMachi", true));
        assertFalse(BossStatMath.applies("The Seven Deadly Sins", true));
        assertFalse(BossStatMath.applies("Tensura", true));
        assertFalse(BossStatMath.applies(null, true));
        assertFalse(BossStatMath.applies("", true));
    }

    @Test
    void theMultiplierIsClampedAndOneMeansOff() {
        assertEquals(3.0, BossStatMath.clamp(3.0), 1e-12);
        assertEquals(1.0, BossStatMath.clamp(0.2), 1e-12);
        assertEquals(1.0, BossStatMath.clamp(-4), 1e-12);
        assertEquals(1.0, BossStatMath.clamp(Double.NaN), 1e-12);
        assertEquals(BossStatMath.MAX, BossStatMath.clamp(1.0e9), 1e-12);
        assertEquals(BossStatMath.MAX, BossStatMath.clamp(Double.POSITIVE_INFINITY), 1e-12);
    }

    @Test
    void threeTimesTheHealthIsAModifierOfPlusTwo() {
        assertEquals(2.0, BossStatMath.healthModifier(3.0), 1e-12);
        assertEquals(0.0, BossStatMath.healthModifier(1.0), 1e-12);
        assertEquals(0.0, BossStatMath.healthModifier(Double.NaN), 1e-12);
        // ADD_MULTIPLIED_TOTAL: 1700 x (1 + 2.0) = 5100, and another system's x1.3 multiplies on top
        assertEquals(5100.0, 1700.0 * (1.0 + BossStatMath.healthModifier(3.0)), 1e-9);
        assertEquals(5100.0 * 1.3, 1700.0 * (1.0 + BossStatMath.healthModifier(3.0)) * 1.3, 1e-9);
    }

    @Test
    void aHitIsTripledButNeverAboveHalfOfTheVictimsMaximum() {
        assertEquals(30f, BossStatMath.damage(10f, 3.0, 1000f), 1e-4f);
        assertEquals(10f, BossStatMath.damage(10f, 1.0, 1000f), 1e-4f, "off");
        assertEquals(0f, BossStatMath.damage(0f, 3.0, 1000f), 0f);
        assertEquals(-2f, BossStatMath.damage(-2f, 3.0, 1000f), 0f, "nothing is made of a non-hit");
        // a 40% hit tripled would be 120%: it is held at half of the maximum
        assertEquals(500f, BossStatMath.damage(400f, 3.0, 1000f), 1e-3f);
        // a hit that was already above the ceiling is not lowered
        assertEquals(700f, BossStatMath.damage(700f, 3.0, 1000f), 1e-3f);
        // nothing is made a one-shot from full health by the buff
        for (float hit = 1f; hit < 400f; hit += 7f)
            assertTrue(BossStatMath.damage(hit, 3.0, 1000f) <= Math.max(hit, 500f) + 1e-3f);
        // an unknown maximum falls back to a plain multiplication
        assertEquals(30f, BossStatMath.damage(10f, 3.0, 0f), 1e-4f);
    }

    @Test
    void theBarKeepsItsShareWhenTheMaximumMoves() {
        assertEquals(5100f, BossStatMath.keepShare(1700f, 1700f, 5100f), 1e-3f, "a full spawn stays full");
        assertEquals(1500f, BossStatMath.keepShare(500f, 1700f, 5100f), 1e-3f, "a wounded boss stays as wounded");
        assertEquals(500f, BossStatMath.keepShare(1500f, 5100f, 1700f), 1e-3f, "the setting turned down");
        assertEquals(10f, BossStatMath.keepShare(10f, 0f, 30f), 0f, "no old maximum: nothing is guessed");
        assertTrue(BossStatMath.keepShare(99999f, 100f, 300f) <= 300f, "never above the new maximum");
    }
}
