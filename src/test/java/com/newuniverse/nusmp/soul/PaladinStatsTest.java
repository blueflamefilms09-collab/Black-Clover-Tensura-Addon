package com.newuniverse.nusmp.soul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The owner's numbers (x2 health, +60% attack, +8 armour, +4 toughness, +15% speed), the strength scalar and the two hit caps. */
class PaladinStatsTest {
    @Test
    void theOwnersNumbersAtStrengthOne() {
        PaladinStats.Mods m = PaladinStats.mods(1.0);
        assertEquals(1.0, m.health(), 1e-9);
        assertEquals(0.6, m.attack(), 1e-9);
        assertEquals(8.0, m.armor(), 1e-9);
        assertEquals(4.0, m.toughness(), 1e-9);
        assertEquals(0.15, m.speed(), 1e-9);
        assertTrue(m.knockback() > 0 && m.knockback() <= 1.0);
    }

    @Test
    void theStrengthScalesEverythingAndIsClamped() {
        PaladinStats.Mods half = PaladinStats.mods(0.5);
        assertEquals(0.5, half.health(), 1e-9);
        assertEquals(4.0, half.armor(), 1e-9);
        assertEquals(0.0, PaladinStats.mods(-3).health(), 1e-9);
        assertEquals(3.0, PaladinStats.mods(99).health(), 1e-9);
        assertEquals(1.0, PaladinStats.clampStrength(Double.NaN), 1e-9);
        assertTrue(PaladinStats.mods(3.0).knockback() <= 1.0, "knockback resistance never goes past immune");
    }

    @Test
    void aBlowOnAPlayerIsHeldToAQuarterOfTheirHealth() {
        assertEquals(5f, PaladinStats.capHit(50f, 20, true, false), 1e-4f);
        assertEquals(3f, PaladinStats.capHit(3f, 20, true, false), 1e-4f);
    }

    @Test
    void aBlowOnABossIsHeldToTwoPercent() {
        assertEquals(20f, PaladinStats.capHit(500f, 1000, false, true), 1e-4f);
        assertEquals(20f, PaladinStats.capHit(500f, 1000, true, true), 1e-4f, "a boss player-sized or not: the lower cap wins");
    }

    @Test
    void anythingElseIsUntouched() {
        assertEquals(500f, PaladinStats.capHit(500f, 20, false, false), 1e-4f);
        assertEquals(0f, PaladinStats.capHit(-4f, 20, false, false), 1e-4f);
    }

    @Test
    void aMobWithNoAttackAttributeStillStrikes() {
        assertEquals(3.0 * 1.6, PaladinStats.fallbackDamage(1.0), 1e-9);
        assertEquals(3.0, PaladinStats.fallbackDamage(0.0), 1e-9);
    }
}
