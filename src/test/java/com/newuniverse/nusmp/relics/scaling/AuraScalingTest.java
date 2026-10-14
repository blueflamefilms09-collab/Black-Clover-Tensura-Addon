package com.newuniverse.nusmp.relics.scaling;

import com.newuniverse.nusmp.relics.scaling.AuraScaling.Aura;
import com.newuniverse.nusmp.relics.scaling.AuraScaling.Awakening;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The aura: the pool curve, the awakened bonus, the caps and the hit arithmetic. */
class AuraScalingTest {
    @Test
    void underTheBasePoolThereIsNoBonus() {
        assertEquals(0.0, AuraScaling.poolBonus(0), 0.0);
        assertEquals(0.0, AuraScaling.poolBonus(-5), 0.0);
        assertEquals(0.0, AuraScaling.poolBonus(AuraScaling.BASE_POOL), 0.0);
        assertEquals(0.0, AuraScaling.poolBonus(Double.NaN), 0.0);
        assertTrue(AuraScaling.compute(Awakening.NONE, false, 9_000, false).none(), "a player below the thresholds is unchanged");
    }

    @Test
    void eachDoublingAddsTheSameAndItEndsAtTheCap() {
        double one = AuraScaling.poolBonus(AuraScaling.BASE_POOL * 2);
        assertEquals(AuraScaling.PER_DOUBLING, one, 1e-9);
        assertEquals(AuraScaling.PER_DOUBLING * 2, AuraScaling.poolBonus(AuraScaling.BASE_POOL * 4), 1e-9);
        assertEquals(AuraScaling.PER_DOUBLING * 3, AuraScaling.poolBonus(AuraScaling.BASE_POOL * 8), 1e-9);
        double prev = 0;
        for (double pool = 10_001; pool < 1e12; pool *= 1.7) {
            double b = AuraScaling.poolBonus(pool);
            assertTrue(b >= prev && b <= AuraScaling.POOL_BONUS_CAP);
            prev = b;
        }
        assertEquals(AuraScaling.POOL_BONUS_CAP, AuraScaling.poolBonus(1e15), 1e-12);
        assertEquals(AuraScaling.POOL_BONUS_CAP, AuraScaling.poolBonus(Double.POSITIVE_INFINITY), 0.0);
    }

    @Test
    void awakeningAddsDamageAndIgnoresArmour() {
        Aura none = AuraScaling.compute(Awakening.NONE, false, 20_000, false);
        Aura seed = AuraScaling.compute(Awakening.SEEDED, false, 20_000, false);
        Aura awake = AuraScaling.compute(Awakening.AWAKENED, false, 20_000, false);
        assertTrue(none.damageMult() < seed.damageMult() && seed.damageMult() < awake.damageMult());
        assertEquals(0.0, none.armourIgnore(), 0.0);
        assertEquals(0.0, seed.armourIgnore(), 0.0);
        assertEquals(0.15, awake.armourIgnore(), 1e-9);
        assertEquals((1 + AuraScaling.PER_DOUBLING) * 1.20, awake.damageMult(), 1e-9);
    }

    @Test
    void theManaZoneAddsAndStaysUnderTheCaps() {
        Aura zone = AuraScaling.compute(Awakening.AWAKENED, true, 1e15, false);
        assertEquals(AuraScaling.MAX_DAMAGE_MULT, zone.damageMult(), 1e-9, "1.35 x 1.20 x 1.08 is over 1.60: capped");
        assertTrue(zone.armourIgnore() <= AuraScaling.MAX_IGNORE);
        assertEquals(0.20, AuraScaling.compute(Awakening.AWAKENED, true, 1, false).armourIgnore(), 1e-9);
    }

    @Test
    void bossesAreHeldToTighterCaps() {
        Aura normal = AuraScaling.compute(Awakening.AWAKENED, true, 1e15, false);
        Aura boss = AuraScaling.compute(Awakening.AWAKENED, true, 1e15, true);
        assertEquals(AuraScaling.BOSS_MAX_DAMAGE_MULT, boss.damageMult(), 1e-9);
        assertEquals(AuraScaling.BOSS_MAX_IGNORE, boss.armourIgnore(), 1e-9);
        assertTrue(boss.damageMult() < normal.damageMult() && boss.armourIgnore() < normal.armourIgnore());
        for (Awakening a : Awakening.values())
            for (double pool : new double[]{0, 1e4, 1e5, 1e7, 1e12}) {
                Aura x = AuraScaling.compute(a, true, pool, true);
                assertTrue(x.damageMult() >= 1.0 && x.damageMult() <= AuraScaling.BOSS_MAX_DAMAGE_MULT);
                assertTrue(x.armourIgnore() >= 0.0 && x.armourIgnore() <= AuraScaling.BOSS_MAX_IGNORE);
            }
    }

    @Test
    void aHitNeverGoesDownAndKeepsAPlayerLimit() {
        assertEquals(10f, AuraScaling.apply(10f, 1.0, 0f), 0f);
        assertEquals(15f, AuraScaling.apply(10f, 1.5, 0f), 1e-5f);
        assertEquals(12f, AuraScaling.apply(10f, 1.5, 12f), 0f, "a player's per-hit cap holds");
        assertEquals(20f, AuraScaling.apply(20f, 1.5, 12f), 0f, "a hit already over the limit is not lowered");
        assertEquals(10f, AuraScaling.apply(10f, 0.5, 0f), 0f, "a multiplier under 1 is read as 1");
    }

    @Test
    void armourKeepIsTheComplementOfTheIgnoredShare() {
        assertEquals(1.0f, AuraScaling.armourKeep(0.0), 0f);
        assertEquals(0.85f, AuraScaling.armourKeep(0.15), 1e-6f);
        assertEquals(0.0f, AuraScaling.armourKeep(3.0), 0f);
        assertEquals(1.0f, AuraScaling.armourKeep(-1.0), 0f);
    }
}
