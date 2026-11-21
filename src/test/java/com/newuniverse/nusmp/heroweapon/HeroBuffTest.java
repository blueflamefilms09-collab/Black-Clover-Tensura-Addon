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

    @Test void animeFactorGrowsWithTheLevelAndHoldsItsBounds() {
        assertEquals(2.0, HeroBuff.anime(2.0, 1.0, 0), 1e-9);
        assertEquals(3.0, HeroBuff.anime(2.0, 1.0, 50), 1e-9);
        assertEquals(4.0, HeroBuff.anime(2.0, 1.0, 100), 1e-9);
        assertEquals(4.0, HeroBuff.anime(2.0, 1.0, 5000), 1e-9);
        assertEquals(2.0, HeroBuff.anime(2.0, 1.0, -9), 1e-9);
        assertEquals(2.0, HeroBuff.anime(2.0, 0.0, 100), 1e-9);
    }

    @Test void squashKeepsOnlyAShareOfTheRaise() {
        assertEquals(1.0, HeroBuff.squash(1.0, 0.02), 1e-12);
        assertEquals(1.0, HeroBuff.squash(0.3, 0.02), 1e-12);
        assertEquals(1.0, HeroBuff.squash(Double.NaN, 0.02), 1e-12);
        assertEquals(1.22, HeroBuff.squash(12.0, 0.02), 1e-9);
        assertEquals(12.0, HeroBuff.squash(12.0, 1.0), 1e-9, "share 1 = the old behaviour");
        assertEquals(12.0, HeroBuff.squash(12.0, 7.0), 1e-9, "a share above 1 is held at 1");
        assertEquals(1.0, HeroBuff.squash(12.0, -3.0), 1e-9);
    }

    /** The owner's report: a hero weapon hit for about 10 000 at its base. With the default share a Legendary blow (base ~142) stays in the low hundreds. */
    @Test void aLegendaryBlowIsNoLongerThousands() {
        double share = 0.02, base = 142.0;
        double lvl0 = base * HeroBuff.squash(4.0 * 1.5 * HeroBuff.anime(2.0, 1.0, 0), share) * HeroBuff.squash(com.newuniverse.nusmp.power.PowerRules.ramp(25.0 * 1.5, 0, 0.05), share);
        double lvl100 = base * HeroBuff.squash(4.0 * 1.5 * HeroBuff.anime(2.0, 1.0, 100), share) * HeroBuff.squash(com.newuniverse.nusmp.power.PowerRules.ramp(25.0 * 1.5, 100, 0.05), share);
        assertTrue(lvl0 < 250, "level 0 blow " + lvl0);
        assertTrue(lvl100 < 500 && lvl100 > lvl0, "level 100 blow " + lvl100);
        double old0 = base * (4.0 * 1.5 * HeroBuff.anime(2.0, 1.0, 0)) * com.newuniverse.nusmp.power.PowerRules.ramp(25.0 * 1.5, 0, 0.05);
        assertTrue(old0 > 4000, "the old level 0 blow was " + old0);
    }
}
