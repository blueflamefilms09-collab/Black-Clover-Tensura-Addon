package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cruel Sun and Zenith Cleave: the burst grows with the sun, can always be walked out of, and cover, a shield and a burst of damage all answer it. */
class CruelSunMathTest {
    private static EscanorTable.Spec cruel() { return EscanorTable.byId(EscanorTable.CRUEL_SUN); }

    @Test
    void theBurstGrowsWithTheSun() {
        assertEquals(9.38, CruelSunMath.radius(0.72, false), 1e-9);
        assertEquals(10.5, CruelSunMath.radius(1.0, false), 1e-9);
        assertEquals(12.0, CruelSunMath.radius(1.0, true), 1e-9);
        assertTrue(CruelSunMath.radius(0.9, true) > CruelSunMath.radius(0.9, false));
        assertTrue(CruelSunMath.damage(1.0) > CruelSunMath.damage(0.72));
        assertEquals(33.6, CruelSunMath.damage(1.0), 1e-3f);
        assertTrue(CruelSunMath.damage(0.72) > EscanorTable.SMASH_DAMAGE * 1.3, "the sun is his heaviest blow");
        assertTrue(CruelSunMath.scorchTicks(1.0) > CruelSunMath.scorchTicks(0.0));
        assertEquals(CruelSunMath.radius(1.0, false) * 0.55, CruelSunMath.scorchRadius(CruelSunMath.radius(1.0, false)), 1e-9);
    }

    @Test
    void itCanAlwaysBeWalkedOutOfInTheTimeTheTellGives() {
        // the wind-up (the framework scales it by 0.75 at the Gates), the flight and the ring on the ground are the warning
        int warning = Math.round(cruel().windUpTicks() * 0.75f) + CruelSunMath.FLIGHT;
        for (double s = 0.72; s <= 1.0; s += 0.02) {
            assertTrue(CruelSunMath.walkable(CruelSunMath.radius(s, true), warning), "walkable at sun " + s);
        }
        assertFalse(CruelSunMath.walkable(40, warning), "and the check really checks");
    }

    @Test
    void coverAndAShieldCutItAndTheyDoNotStack() {
        assertEquals(1f, CruelSunMath.factor(false, false), 0f);
        assertEquals(CruelSunMath.COVER_FACTOR, CruelSunMath.factor(true, false), 0f);
        assertEquals(CruelSunMath.SHIELD_FACTOR, CruelSunMath.factor(false, true), 0f);
        assertEquals(Math.min(CruelSunMath.COVER_FACTOR, CruelSunMath.SHIELD_FACTOR), CruelSunMath.factor(true, true), 0f, "the better of the two, never both");
        assertTrue(CruelSunMath.COVER_FACTOR < 0.5f && CruelSunMath.SHIELD_FACTOR < 0.5f);
    }

    @Test
    void aBurstOfDamageDuringTheWindUpBreaksTheSun() {
        assertFalse(CruelSunMath.broken(0.079));
        assertTrue(CruelSunMath.broken(0.08));
        assertTrue(CruelSunMath.broken(0.3));
        // the framework lets a party take at most 6% of his health a second off him (and 1.5x that in the final phase): 8% is reachable in the wind-up
        double perSecond = 0.06 * 1.5;
        assertTrue(perSecond * cruel().windUpTicks() * 0.75 / 20.0 > CruelSunMath.INTERRUPT, "reachable inside the wind-up even at the Gates");
        assertTrue(0.06 * cruel().windUpTicks() * 0.75 / 20.0 > CruelSunMath.INTERRUPT * 0.99, "and reachable before the Gates for a party at the cap");
    }

    @Test
    void zenithCleaveIsBiggerThanCruelSunInReachAndNotInDamage() {
        assertTrue(CruelSunMath.ZENITH_REACH > CruelSunMath.radius(1.0, true), "the shock runs further than the sun burns");
        assertTrue(CruelSunMath.ZENITH_CENTRE_DAMAGE > CruelSunMath.ZENITH_WAVE_DAMAGE);
        assertTrue(CruelSunMath.ZENITH_CENTRE < 5, "the slam itself is small; the wave is jumped");
        EscanorTable.Spec z = EscanorTable.byId(EscanorTable.ZENITH);
        assertEquals(100, z.minPower());
        assertTrue(z.windUpTicks() >= 56);
    }
}
