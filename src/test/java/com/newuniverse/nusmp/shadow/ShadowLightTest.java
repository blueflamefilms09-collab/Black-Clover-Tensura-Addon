package com.newuniverse.nusmp.shadow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Shadow Travel's light rule: below 4 at feet and head, sky light darkened by the time of day. */
class ShadowLightTest {
    @Test
    void thresholdIsBelowFourByDefault() {
        assertEquals(3, ShadowLight.DEFAULT_MAX_LIGHT);
        assertTrue(ShadowLight.isShadow(3, 0, 0, 3));
        assertFalse(ShadowLight.isShadow(4, 0, 0, 3));
        assertTrue(ShadowLight.isShadow(0, 0, 0, 3));
    }

    @Test
    void skyLightIsDarkenedByTheTimeOfDay() {
        assertEquals(15, ShadowLight.effectiveSky(15, 0), "clear noon: full sky light");
        assertEquals(4, ShadowLight.effectiveSky(15, 11), "midnight: 15 - 11");
        assertEquals(0, ShadowLight.effectiveSky(3, 11), "never below zero");
        assertFalse(ShadowLight.isShadow(0, 15, 0, 3), "open sky by day is not shadow");
        assertFalse(ShadowLight.isShadow(0, 15, 11, 3), "open sky at night is still 4: not shadow");
        assertTrue(ShadowLight.isShadow(0, 14, 11, 3), "one block under cover at night: 3, shadow");
        assertTrue(ShadowLight.isShadow(0, 3, 0, 3), "deep under cover by day");
    }

    @Test
    void theBrighterOfBlockAndSkyWins() {
        assertEquals(9, ShadowLight.brightness(9, 2, 0));
        assertEquals(12, ShadowLight.brightness(2, 12, 0));
        assertEquals(15, ShadowLight.brightness(99, 0, 0), "out of range input is clamped");
    }

    @Test
    void feetAndHeadBothMustBeDark() {
        assertTrue(ShadowLight.spotInShadow(0, 0, 3, 0, 0, 3));
        assertFalse(ShadowLight.spotInShadow(0, 0, 4, 0, 0, 3), "a bright head cell spoils it");
        assertFalse(ShadowLight.spotInShadow(5, 0, 0, 0, 0, 3), "a bright feet cell spoils it");
        assertFalse(ShadowLight.spotInShadow(0, 15, 0, 15, 0, 3));
    }

    @Test
    void theConfigThresholdIsClamped() {
        assertEquals(0, ShadowLight.clampMax(-5));
        assertEquals(7, ShadowLight.clampMax(40));
        assertFalse(ShadowLight.isShadow(1, 0, 0, -3), "a negative threshold behaves as 0");
        assertTrue(ShadowLight.isShadow(7, 0, 0, 99));
        assertFalse(ShadowLight.isShadow(8, 0, 0, 99));
    }
}
