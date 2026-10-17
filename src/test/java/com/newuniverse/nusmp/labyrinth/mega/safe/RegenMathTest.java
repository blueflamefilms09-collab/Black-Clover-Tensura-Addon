package com.newuniverse.nusmp.labyrinth.mega.safe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "no easy passive regeneration" is two numbers that must not drift: natural heals (at most one heart-half per call, no Regeneration
 * effect) are scaled, everything else is not, and the mana regeneration attribute is multiplied by exactly the configured scale. A wrong sign
 * here would either heal players faster than vanilla or leave potions and rests crippled, and neither shows up until someone plays for an hour.
 */
class RegenMathTest {
    @Test void aNaturalHealIsScaled() {
        assertEquals(0.25f, RegenMath.scaleHeal(1.0f, 0.25, false), 1e-6f);
        assertEquals(0.125f, RegenMath.scaleHeal(0.5f, 0.25, false), 1e-6f);
        assertEquals(0.5f, RegenMath.scaleHeal(1.0f, 0.5, false), 1e-6f);
    }

    @Test void vanillaScaleChangesNothing() {
        assertEquals(1.0f, RegenMath.scaleHeal(1.0f, 1.0, false), 1e-6f);
        assertEquals(1.0f, RegenMath.scaleHeal(1.0f, 7.0, false), 1e-6f, "a scale above 1 never speeds regeneration up");
    }

    @Test void scaleZeroCancelsTheHeal() {
        assertEquals(0f, RegenMath.scaleHeal(1.0f, 0.0, false), 1e-6f);
        assertEquals(0f, RegenMath.scaleHeal(0.7f, -2.0, false), 1e-6f, "a negative scale is treated as zero");
    }

    @Test void aTinyResultRoundsToNothing() {
        assertEquals(0f, RegenMath.scaleHeal(0.01f, 0.25, false), 1e-6f);
    }

    @Test void theRegenerationEffectIsExempt() {
        assertEquals(1.0f, RegenMath.scaleHeal(1.0f, 0.25, true), 1e-6f);
    }

    @Test void bigHealsAreNotNaturalRegeneration() {
        assertEquals(4.0f, RegenMath.scaleHeal(4.0f, 0.25, false), 1e-6f, "an instant-health potion");
        assertEquals(1.01f, RegenMath.scaleHeal(1.01f, 0.25, false), 1e-6f);
        assertEquals(19.0f, RegenMath.scaleHeal(19.0f, 0.0, false), 1e-6f, "a rest's own heal");
    }

    @Test void garbageComesBackUnchanged() {
        assertEquals(0f, RegenMath.scaleHeal(0f, 0.25, false), 1e-6f);
        assertEquals(-1f, RegenMath.scaleHeal(-1f, 0.25, false), 1e-6f);
        assertEquals(1.0f, RegenMath.scaleHeal(1.0f, Double.NaN, false), 1e-6f);
    }

    @Test void theModifierMultipliesByExactlyTheScale() {
        assertEquals(-0.75, RegenMath.modifierAmount(0.25), 1e-9);
        assertEquals(-1.0, RegenMath.modifierAmount(0.0), 1e-9);
        assertEquals(0.0, RegenMath.modifierAmount(1.0), 1e-9);
        assertEquals(0.0, RegenMath.modifierAmount(3.0), 1e-9, "no bonus above vanilla");
        assertEquals(-1.0, RegenMath.modifierAmount(-3.0), 1e-9, "no regeneration below zero");
        assertEquals(0.0, RegenMath.modifierAmount(Double.NaN), 1e-9);
        // ADD_MULTIPLIED_TOTAL: value * (1 + amount)
        assertEquals(0.25, 1.0 * (1.0 + RegenMath.modifierAmount(0.25)), 1e-9);
    }

    @Test void theThrottleAppliesOnlyInTheDungeonOutsideSafeZonesToNonExemptPlayers() {
        assertTrue(RegenMath.applies(true, false, false, 0.25));
        assertFalse(RegenMath.applies(false, false, false, 0.25), "not in the Mega-Dungeon");
        assertFalse(RegenMath.applies(true, true, false, 0.25), "a Landing is a place to catch your breath");
        assertFalse(RegenMath.applies(true, false, true, 0.25), "creative and spectator are exempt");
        assertFalse(RegenMath.applies(true, false, false, 1.0), "scale 1 is vanilla");
        assertTrue(RegenMath.applies(true, false, false, 0.0));
        assertFalse(RegenMath.applies(true, false, false, Double.NaN));
    }
}
