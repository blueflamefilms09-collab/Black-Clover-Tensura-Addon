package com.newuniverse.nusmp.boss.budget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leon study fix 1: the damage cap is a share of the BASE health, so the Black Clover x3 lengthens the shortest fight; the setting off is today's arithmetic exactly. */
class BaseHealthCapTest {
    private static float cap(float basis) { return BudgetPolicy.capPerSecond(basis, 0.06, 20.0, 1.0); }

    @Test
    void theBaseIsTheMaxHealthDividedByTheBlackCloverFactor() {
        assertEquals(1000f, BudgetPolicy.capBasis(3000f, 3.0, true), 1e-3f);
        assertEquals(2500f, BudgetPolicy.capBasis(2500f, 1.0, true), "no factor: unchanged");
        assertEquals(2500f, BudgetPolicy.capBasis(2500f, 0.5, true), "a factor under 1 is no buff");
        assertEquals(2500f, BudgetPolicy.capBasis(2500f, Double.NaN, true));
    }

    @Test
    void settingOffIsExactlyTheOldBehaviour() {
        assertEquals(3000f, BudgetPolicy.capBasis(3000f, 3.0, false));
        assertEquals(cap(3000f), BudgetPolicy.capPerSecond(BudgetPolicy.capBasis(3000f, 3.0, false), 0.06, 20.0, 1.0));
    }

    @Test
    void theShortestKillTakesThreeTimesAsLongWithTheBuff() {
        float baseHealth = 2000f;
        float buffed = baseHealth * 3f;
        float oldCap = cap(buffed);
        float newCap = cap(BudgetPolicy.capBasis(buffed, 3.0, true));
        assertEquals(oldCap, 3f * newCap, 0.01f, "the old cap tripled with the bar");
        assertEquals(cap(baseHealth), newCap, 1e-3f, "the new cap is the unbuffed boss's cap");
        double oldSeconds = buffed / oldCap, newSeconds = buffed / newCap;
        assertEquals(1.0 / 0.06, oldSeconds, 0.01, "about 17 s before");
        assertEquals(3.0 / 0.06, newSeconds, 0.01, "about 50 s after");
        assertTrue(newSeconds > 2.9 * oldSeconds);
    }

    @Test
    void anUnbuffedBossIsUntouched() {
        for (float hp : new float[]{50f, 1700f, 14400f}) assertEquals(cap(hp), cap(BudgetPolicy.capBasis(hp, 1.0, true)));
    }

    @Test
    void theMinimumStillLetsASmallBossDie() {
        float basis = BudgetPolicy.capBasis(90f, 3.0, true);       // base 30 health: 6% is 1.8, the minimum 20 wins, still under the basis
        assertEquals(20f, BudgetPolicy.capPerSecond(basis, 0.06, 20.0, 1.0), 1e-3f);
        assertEquals(10f, BudgetPolicy.capPerSecond(BudgetPolicy.capBasis(30f, 3.0, true), 0.06, 20.0, 1.0), 1e-3f, "never above the basis itself (10 health)");
    }
}
