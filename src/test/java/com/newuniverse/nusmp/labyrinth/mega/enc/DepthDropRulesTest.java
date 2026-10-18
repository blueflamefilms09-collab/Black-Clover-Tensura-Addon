package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "the drops get better the deeper you go, up or down" is a property of four curves, pinned here without a world: more depth never pays less, the first floor is plain, every curve is
 * capped, up and down are the same, a stack that cannot stack is left alone, a stack never passes its maximum, and the server owner's scale and ceilings behave.
 */
class DepthDropRulesTest {
    private static final DepthDropRules.Params P = DepthDropRules.DEFAULTS;

    @Test
    void theFirstFloorIsPlain() {
        assertEquals(0.0, DepthDropRules.bonusRolls(1, P), 1e-12);
        assertEquals(0.0, DepthDropRules.countBonus(1, P), 1e-12);
        assertEquals(0.0f, DepthDropRules.luckBonus(1, P), 1e-12f);
        assertEquals(1.0, DepthDropRules.xpMultiplier(1, P), 1e-12);
        assertEquals(0, DepthDropRules.extraRolls(1, P, 0.0));
        assertEquals(5, DepthDropRules.boostedCount(5, 64, 1, P, 0.0));
        assertEquals(100, DepthDropRules.boostedXp(100, 1, P));
    }

    @Test
    void deeperNeverPaysLess() {
        double rolls = 0, count = 0, xp = 1;
        float luck = 0;
        for (int d = 1; d <= 3000; d++) {
            double r = DepthDropRules.bonusRolls(d, P), c = DepthDropRules.countBonus(d, P), x = DepthDropRules.xpMultiplier(d, P);
            float l = DepthDropRules.luckBonus(d, P);
            assertTrue(r >= rolls && c >= count && x >= xp && l >= luck, "a curve fell at depth " + d);
            rolls = r; count = c; xp = x; luck = l;
        }
    }

    @Test
    void everyCurveIsCapped() {
        int d = Integer.MAX_VALUE;
        assertTrue(DepthDropRules.bonusRolls(d, P) <= P.rollsMax() && DepthDropRules.bonusRolls(d, P) > P.rollsMax() * 0.99);
        assertTrue(DepthDropRules.countBonus(d, P) <= P.countMax());
        assertTrue(DepthDropRules.luckBonus(d, P) <= P.luckMax());
        assertTrue(DepthDropRules.xpMultiplier(d, P) <= 1.0 + P.xpMax());
    }

    @Test
    void theDesignedLandmarks() {
        assertEquals(0.12, DepthDropRules.bonusRolls(10, P), 0.01);      // a sector in: one kill in eight leaves one more roll
        assertEquals(1.49, DepthDropRules.bonusRolls(200, P), 0.02);     // the end of tier 1: one and a half extra rolls
        assertEquals(2.5, DepthDropRules.bonusRolls(1000, P), 0.05);
        assertEquals(1.0 + 3.0 * 99.0 / 399.0, DepthDropRules.xpMultiplier(100, P), 1e-9);
    }

    @Test
    void extraRollsAreTheIntegerPartPlusAChanceOfOneMore() {
        double b = DepthDropRules.bonusRolls(200, P);         // 1.49
        assertEquals(1, DepthDropRules.extraRolls(200, P, 0.99));
        assertEquals(2, DepthDropRules.extraRolls(200, P, 0.0));
        int more = 0;
        for (int i = 0; i < 10000; i++) more += DepthDropRules.extraRolls(200, P, i / 10000.0);
        assertEquals(b * 10000, more, 10.0);
    }

    @Test
    void stacksGrowByTheBonusAndNeverPastTheirMaximum() {
        int d = 401;                                           // count bonus 0.5 (x = 400 = half)
        assertEquals(0.5, DepthDropRules.countBonus(d, P), 1e-9);
        assertEquals(4 + 2, DepthDropRules.boostedCount(4, 64, d, P, 0.99));         // +2.0 exactly
        assertEquals(3 + 1, DepthDropRules.boostedCount(3, 64, d, P, 0.99));         // +1.5: the half is a coin toss (lost)
        assertEquals(3 + 2, DepthDropRules.boostedCount(3, 64, d, P, 0.0));          // (won)
        assertEquals(64, DepthDropRules.boostedCount(60, 64, d, P, 0.0));            // capped at the stack size
        assertEquals(1, DepthDropRules.boostedCount(1, 1, d, P, 0.0));               // a sword does not stack: left alone
        assertEquals(0, DepthDropRules.boostedCount(0, 64, d, P, 0.0));
    }

    @Test
    void upAndDownAreTheSame() {
        for (int f : new int[]{3, 40, 250, 999}) {
            assertEquals(DepthDropRules.bonusRolls(DepthRules.depthOf(f), P), DepthDropRules.bonusRolls(DepthRules.depthOf(-f), P), 0.0);
            assertEquals(DepthDropRules.xpMultiplier(DepthRules.depthOf(f), P), DepthDropRules.xpMultiplier(DepthRules.depthOf(-f), P), 0.0);
        }
    }

    @Test
    void theOwnersScaleAndBadValues() {
        DepthDropRules.Params off = new DepthDropRules.Params(3, 1, 3, 3, 0.0);
        assertEquals(0.0, DepthDropRules.bonusRolls(1000, off), 0.0);
        assertEquals(1.0, DepthDropRules.xpMultiplier(1000, off), 0.0);
        assertEquals(7, DepthDropRules.boostedCount(7, 64, 1000, off, 0.0));
        DepthDropRules.Params half = new DepthDropRules.Params(3, 1, 3, 3, 0.5);
        assertEquals(DepthDropRules.bonusRolls(500, P) / 2.0, DepthDropRules.bonusRolls(500, half), 1e-9);
        DepthDropRules.Params bad = new DepthDropRules.Params(Double.NaN, -4, 1e9, Double.NaN, Double.NaN);
        assertEquals(0.0, bad.rollsMax(), 0.0);
        assertEquals(0.0, bad.countMax(), 0.0);
        assertEquals(20.0, bad.luckMax(), 0.0);
        assertFalse(DepthDropRules.xpMultiplier(1000, bad) > 1.0);
    }

    @Test
    void experienceIsRoundedAndNeverNegative() {
        assertEquals(0, DepthDropRules.boostedXp(0, 500, P));
        assertEquals(-3, DepthDropRules.boostedXp(-3, 500, P));
        assertTrue(DepthDropRules.boostedXp(5, 1000, P) > 5);
    }
}
