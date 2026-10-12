package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.Devil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Dark Disciple numbers, the damage arithmetic and the devil palette. */
class DiscipleStatsTest {
    @Test
    void statsGrowWithTheShareUpToFortyPercent() {
        double lo = PowerCurve.discipleShare(20), hi = PowerCurve.discipleShare(100);
        assertEquals(0.08, lo, 1e-9);
        assertEquals(DiscipleStats.MAX_SHARE, hi, 1e-9);
        assertTrue(DiscipleStats.health(hi) > DiscipleStats.health(lo));
        assertTrue(DiscipleStats.attack(hi) > DiscipleStats.attack(lo));
        assertTrue(DiscipleStats.speed(hi) > DiscipleStats.speed(lo));
        assertTrue(DiscipleStats.armor(hi) > DiscipleStats.armor(lo));
        assertTrue(DiscipleStats.abilityCooldown(hi) < DiscipleStats.abilityCooldown(lo));
        assertTrue(DiscipleStats.abilityDamage(hi) > DiscipleStats.abilityDamage(lo));
        assertEquals(300, DiscipleStats.health(hi), 1e-9);
        assertEquals(60, DiscipleStats.health(0), 1e-9);
        assertEquals(DiscipleStats.health(hi), DiscipleStats.health(5.0), 1e-9, "more than 40% is capped");
        assertEquals(1.0, DiscipleStats.fill(hi), 1e-9);
        assertEquals(0.0, DiscipleStats.fill(-1), 1e-9);
    }

    @Test
    void speedStaysBelowAPlayerSprint() {
        assertTrue(DiscipleStats.speed(DiscipleStats.MAX_SHARE) <= 0.30 + 1e-9);
    }

    @Test
    void victimFactorScalesWithEpButIsCapped() {
        assertEquals(1.0, DamageMath.victimFactor(0), 1e-9);
        assertEquals(1.0, DamageMath.victimFactor(1000), 1e-9);
        assertEquals(1.5, DamageMath.victimFactor(10_000), 1e-9);
        assertEquals(2.0, DamageMath.victimFactor(100_000), 1e-9);
        assertEquals(3.0, DamageMath.victimFactor(10_000_000), 1e-9);
        assertEquals(3.0, DamageMath.victimFactor(1e15), 1e-9, "capped at x3");
        double prev = 0;
        for (double ep = 1; ep < 1e12; ep *= 3) {
            double f = DamageMath.victimFactor(ep);
            assertTrue(f >= prev);
            prev = f;
        }
    }

    @Test
    void rawDamageMultipliesTheParts() {
        assertEquals(10 * 0.6 * 1.0 * 1.0, DamageMath.raw(10, 0.6, 1.0, 100), 1e-9);
        assertEquals(10 * 1.6 * 1.5 * 3.0, DamageMath.raw(10, 1.6, 1.5, 1e8), 1e-9);
        assertEquals(0, DamageMath.raw(-4, 1.6, 1.5, 1e8), 1e-9);
    }

    @Test
    void everyDevilOfTheFamilyHasItsOwnColour() {
        int[] colours = {DevilStyle.color(Devil.LUCIFERO), DevilStyle.color(Devil.MEGICULA), DevilStyle.color(Devil.BEELZEBUB), DevilStyle.color(Devil.ASTAROTH)};
        for (int i = 0; i < colours.length; i++)
            for (int j = i + 1; j < colours.length; j++) assertNotEquals(colours[i], colours[j]);
        assertEquals(DevilStyle.NEUTRAL, DevilStyle.color(null));
        assertEquals(DevilStyle.NEUTRAL, DevilStyle.color(Devil.LIEBE));
        assertEquals("Gravity", DevilStyle.magic(Devil.LUCIFERO));
        assertEquals("Curse-Warding", DevilStyle.magic(Devil.MEGICULA));
        assertEquals("Spatial", DevilStyle.magic(Devil.BEELZEBUB));
        assertEquals("Time", DevilStyle.magic(Devil.ASTAROTH));
        assertEquals(1f, DevilStyle.red(0xFF0000), 1e-6);
        assertEquals(0f, DevilStyle.green(0xFF0000), 1e-6);
        assertEquals(1f, DevilStyle.blue(0x0000FF), 1e-6);
        for (Devil d : new Devil[]{Devil.LUCIFERO, Devil.MEGICULA, Devil.BEELZEBUB, Devil.ASTAROTH}) assertNotEquals(DevilStyle.color(d), DevilStyle.accent(d));
    }
}
