package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HeroBuffTest {
    @Test void holyWeaponsGetTheExtraFactor() {
        assertTrue(HeroKind.SHIELD.isHoly());
        assertEquals(6.0, HeroBuff.damageFactor(HeroKind.SHIELD, 4.0, 1.5), 1e-9);
        assertEquals(4.0, HeroBuff.damageFactor(null, 4.0, 1.5), 1e-9);
    }

    @Test void cooldownHalvesAndNeverHitsZero() {
        assertEquals(50, HeroBuff.cooldown(100, 0.5));
        assertEquals(1, HeroBuff.cooldown(1, 0.05));
        assertEquals(1, HeroBuff.cooldown(-5, 0.5));
    }

    @Test void shieldTakesTheLowerFactor() {
        assertEquals(20f, HeroBuff.taken(HeroKind.SHIELD, 100f, 0.6, 0.2), 1e-4);
        assertEquals(60f, HeroBuff.taken(HeroKind.SHIELD, 100f, 0.6, 0.9), 1e-4);
    }

    @Test void reflectIsCapped() {
        assertEquals(30f, HeroBuff.reflect(40f, 0.75, 60.0), 1e-4);
        assertEquals(60f, HeroBuff.reflect(1000f, 0.75, 60.0), 1e-4);
        assertEquals(0f, HeroBuff.reflect(-5f, 0.75, 60.0), 1e-4);
    }
}
