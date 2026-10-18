package com.newuniverse.nusmp.client.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every temporary armour of the mod now forges in and shatters out exactly like the Djinn Full Equip, so the shared arithmetic is pinned: 8 ticks to forge in after the stagger, fully on in the
 * middle, 12 ticks to shatter in the last 18, never back from gone, monotone both ways, and the offsets, alphas and sizes are the Djinn's own.
 */
class EquipAnimationTest {
    private static final float TOTAL = 60f;

    @Test
    void forgesInAfterItsDelayOverEightTicks() {
        assertEquals(0f, EquipAnimation.assembled(0f, TOTAL, 5f), 1e-6);
        assertEquals(0f, EquipAnimation.assembled(5f, TOTAL - 5f, 5f), 1e-6);
        assertEquals(0.5f, EquipAnimation.assembled(9f, TOTAL - 9f, 5f), 1e-6);
        assertEquals(1f, EquipAnimation.assembled(13f, TOTAL - 13f, 5f), 1e-6);
        float prev = 0f;
        for (float t = 0f; t <= 20f; t += 0.5f) { float a = EquipAnimation.assembled(t, TOTAL - t, 4f); assertTrue(a >= prev, "forge went backwards at " + t); prev = a; }
    }

    @Test
    void staysFullyOnInTheMiddle() {
        for (float t = 14f; t < TOTAL - EquipAnimation.SHATTER_WINDOW - 0.1f; t += 1f) assertEquals(1f, EquipAnimation.assembled(t, TOTAL - t, 8f), 1e-6, "t=" + t);
    }

    @Test
    void shattersOutInTheLastEighteenTicksAndStaysGone() {
        float prev = 1f;
        boolean negative = false;
        for (float left = EquipAnimation.SHATTER_WINDOW; left >= 0f; left -= 0.5f) {
            float a = EquipAnimation.assembled(TOTAL - left, left, 0f);
            if (a < 0f) negative = true;
            assertTrue(a <= prev + 1e-6f || a == 0f, "shatter went backwards at left=" + left);
            if (a == 0f) assertTrue(left < EquipAnimation.SHATTER_WINDOW - EquipAnimation.SHATTER_TICKS + 0.01f, "gone too early at left=" + left);
            prev = a;
        }
        assertTrue(negative);
        assertEquals(0f, EquipAnimation.assembled(TOTAL, 0f, 0f), 1e-6, "nothing is left when the aura ends");
    }

    @Test
    void aStaggeredPieceShattersLater() {
        float left = EquipAnimation.SHATTER_WINDOW - 6f;
        float early = EquipAnimation.assembled(TOTAL - left, left, 0f), late = EquipAnimation.assembled(TOTAL - left, left, 10f);
        assertTrue(early < 0f && early > -1f);
        assertTrue(late > early, "a piece with a delay of 10 is still more assembled than one with 0 at the same moment");
    }

    @Test
    void theDjinnsOwnNumbers() {
        assertEquals(1f, EquipAnimation.offset(0.0001f), 1e-3);
        assertEquals(0.25f, EquipAnimation.offset(0.5f), 1e-6);
        assertEquals(0f, EquipAnimation.offset(1f), 1e-6);
        assertEquals(1.4f, EquipAnimation.offset(-1f), 1e-6);
        assertEquals(0.35f, EquipAnimation.offset(-0.5f), 1e-6);
        assertEquals(0.5f, EquipAnimation.alpha(0.5f), 1e-6);
        assertEquals(0.5f, EquipAnimation.alpha(-0.5f), 1e-6);
        assertEquals(1f, EquipAnimation.alpha(1f), 1e-6);
        assertEquals(0f, EquipAnimation.alpha(0f), 1e-6);
        assertEquals(0.85f, EquipAnimation.scale(0.0001f), 1e-3);
        assertEquals(0.925f, EquipAnimation.scale(0.5f), 1e-6);
        assertEquals(1f, EquipAnimation.scale(1f), 1e-6);
        assertEquals(1.25f, EquipAnimation.scale(-1f), 1e-6);
    }
}
