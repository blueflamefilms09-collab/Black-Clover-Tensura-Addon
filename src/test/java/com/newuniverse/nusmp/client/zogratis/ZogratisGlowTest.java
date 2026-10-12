package com.newuniverse.nusmp.client.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The glow, flash, fade and hover numbers of the Zogratis bodies. */
class ZogratisGlowTest {
    private static float mean(int power, int mode, float progress) {
        double sum = 0;
        int n = 600;
        for (int t = 0; t < n; t++) sum += ZogratisGlow.level(power, t, mode, progress);
        return (float) (sum / n);
    }

    @Test
    void theGlowRisesWithThePowerDrawnAndStaysInRange() {
        float last = -1f;
        for (int power : new int[]{20, 40, 60, 80, 100}) {
            float m = mean(power, ZogratisGlow.NORMAL, 0f);
            assertTrue(m > last, "brighter at " + power);
            last = m;
            for (int t = 0; t < 400; t++) {
                for (int mode = 0; mode <= 4; mode++) {
                    float v = ZogratisGlow.level(power, t, mode, (t % 61) / 60f);
                    assertTrue(v >= 0f && v <= 1f, "in range: power " + power + " mode " + mode);
                }
            }
        }
        assertTrue(ZogratisGlow.level(Integer.MAX_VALUE, 3f, 0, Float.NaN) <= 1f);
        assertTrue(ZogratisGlow.level(-50, 3f, 0, -2f) >= 0f);
    }

    @Test
    void castingSwellsAndExposedStarves() {
        assertTrue(mean(60, ZogratisGlow.CASTING, 1f) > mean(60, ZogratisGlow.NORMAL, 0f));
        assertTrue(mean(60, ZogratisGlow.ULTIMATE, 1f) > mean(60, ZogratisGlow.CASTING, 1f));
        assertTrue(mean(60, ZogratisGlow.EXPOSED, 0f) < 0.4f);
        assertTrue(mean(100, ZogratisGlow.TRANSFORMING, 0.5f) > 0.9f);
    }

    @Test
    void theFlashPeaksInTheMiddleOfTheTransformation() {
        assertEquals(1f, ZogratisGlow.flash(0.5f), 1e-6);
        assertEquals(0f, ZogratisGlow.flash(0.3f));
        assertEquals(0f, ZogratisGlow.flash(0.7f));
        assertEquals(0f, ZogratisGlow.flash(0f));
        assertEquals(0f, ZogratisGlow.flash(1f));
        assertEquals(ZogratisGlow.flash(0.45f), ZogratisGlow.flash(0.55f), 1e-5, "symmetric");
        assertTrue(ZogratisGlow.flash(0.45f) > ZogratisGlow.flash(0.4f));
    }

    @Test
    void aDyingBodyHoldsThenFades() {
        assertEquals(1f, ZogratisGlow.deathFade(0f, 20f));
        assertEquals(1f, ZogratisGlow.deathFade(12f, 20f));
        assertTrue(ZogratisGlow.deathFade(16f, 20f) < 1f && ZogratisGlow.deathFade(16f, 20f) > 0f);
        assertEquals(0f, ZogratisGlow.deathFade(20f, 20f), 1e-6);
        assertEquals(0f, ZogratisGlow.deathFade(99f, 20f), 1e-6);
        assertEquals(0f, ZogratisGlow.deathFade(5f, 0f), 1e-6, "a zero span does not divide by zero: it is over at once");
        assertEquals(1f, ZogratisGlow.deathFade(0f, 0f), 1e-6);
    }

    @Test
    void hoverRisesAfterTheNewFormAppears() {
        assertEquals(0f, ZogratisGlow.hoverRamp(false, 0f, false));
        assertEquals(0f, ZogratisGlow.hoverRamp(true, 0.9f, false));
        assertEquals(0f, ZogratisGlow.hoverRamp(true, 0.4f, true));
        assertEquals(0f, ZogratisGlow.hoverRamp(true, 0.5f, true), 1e-6);
        assertEquals(1f, ZogratisGlow.hoverRamp(true, 0.8f, true), 1e-6);
        assertEquals(1f, ZogratisGlow.hoverRamp(false, 0f, true));
        assertTrue(ZogratisGlow.hoverRamp(true, 0.65f, true) > 0f && ZogratisGlow.hoverRamp(true, 0.65f, true) < 1f);
    }

    @Test
    void colourHelpers() {
        assertEquals(0xFF000000, ZogratisGlow.grey(0f));
        assertEquals(0xFFFFFFFF, ZogratisGlow.grey(1f));
        assertEquals(0xFF808080, ZogratisGlow.grey(0.5f));
        assertEquals(0xFF000000, ZogratisGlow.grey(Float.NaN));
        assertEquals(0xFF5B2A8C, ZogratisGlow.scaled(0x5B2A8C, 1f));
        assertEquals(0xFF000000, ZogratisGlow.scaled(0x5B2A8C, 0f));
        assertEquals(0x80123456, ZogratisGlow.withAlpha(0x123456, 0.5f));
        assertEquals(0x00123456, ZogratisGlow.withAlpha(0xFF123456, -1f));
        assertEquals(0x000000, ZogratisGlow.mix(0x000000, 0xFFFFFF, 0f));
        assertEquals(0xFFFFFF, ZogratisGlow.mix(0x000000, 0xFFFFFF, 1f));
        assertEquals(0x808080, ZogratisGlow.mix(0x000000, 0xFFFFFF, 0.5f));
    }
}
