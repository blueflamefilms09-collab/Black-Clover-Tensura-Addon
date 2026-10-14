package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The render-time scale clamp for weapons whose model json has not been through tools/relics/display_presets.py. */
class OversizeGuardTest {
    @Test
    void anOrdinaryWeaponIsNeverTouched() {
        assertEquals(1f, OversizeGuard.factor(1.2f, 0.68f));
        assertEquals(1f, OversizeGuard.factor(1.4f, 0.68f));
        assertEquals(1f, OversizeGuard.factor(1.69f, 0.68f), "just under the limit: 1.15 blocks apparent");
    }

    @Test
    void aPresetWeaponStaysUnderTheLimit() {
        // the manifest's rows: length x scale <= 1.0 blocks apparent, below 1.15
        assertEquals(1f, OversizeGuard.factor(2.47f, 0.385f));
        assertEquals(1f, OversizeGuard.factor(1.47f, 0.672f));
    }

    @Test
    void aLongWeaponAtTheHandheldScaleIsBroughtToTheLimit() {
        float f = OversizeGuard.factor(2.47f, 0.68f);                           // 1.68 blocks apparent
        assertTrue(f < 1f);
        assertEquals(OversizeGuard.APPARENT_MAX * OversizeGuard.TOLERANCE, 2.47f * 0.68f * f, 1e-4);
    }

    @Test
    void theClampHasAFloorAndIgnoresNonsense() {
        assertEquals(OversizeGuard.MIN_FACTOR, OversizeGuard.factor(10f, 0.68f));
        assertEquals(1f, OversizeGuard.factor(0f, 0.68f));
        assertEquals(1f, OversizeGuard.factor(2f, 0f));
        assertEquals(1f, OversizeGuard.factor(Float.NaN, 0.68f));
    }
}
