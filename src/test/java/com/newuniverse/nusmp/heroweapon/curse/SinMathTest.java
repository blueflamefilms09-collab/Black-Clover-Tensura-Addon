package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.Sin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's rule is that every sin's downside is bounded and none can kill. These tests are that rule as arithmetic: whatever the health, the maximum
 * health, the intensity or the config, a drain stops at its floor (and the floor is never under one heart), a hunger drain stops at its floor, a slow stops at
 * 60%, a pull points at the foe and stops at its range, and a heal is only ever reduced, never raised.
 */
class SinMathTest {
    private static final Sin[] SINS = Sin.values();

    @Test
    void wrathNeverDrainsBelowItsFloorAndTheFloorIsNeverUnderOneHeart() {
        for (double max : new double[]{1.0, 2.0, 6.0, 20.0, 40.0, 1000.0}) {
            for (double share : new double[]{0.0, 0.1, 0.3, 0.9, 1.0, 5.0, -2.0}) {
                double floor = SinMath.wrathFloor(max, share);
                assertTrue(floor <= max + 1e-9, "the floor is never above the maximum: max " + max + " share " + share);
                for (double hp = max; hp >= 0.0; hp -= max / 7.0) {
                    for (double want : new double[]{0.0, 0.5, 3.0, 50.0, 1.0e9}) {
                        double take = SinMath.drain(hp, floor, want);
                        assertTrue(take >= 0.0, "a drain never gives health");
                        assertTrue(take <= want + 1e-9, "a drain never takes more than wanted");
                        double left = hp - take;
                        if (hp >= SinMath.ONE_HEART) assertTrue(left >= SinMath.ONE_HEART - 1e-9, "never below one heart: hp " + hp + " floor " + floor + " want " + want);
                        else assertEquals(hp, left, 1e-9, "a wielder under one heart is not drained at all");
                    }
                }
            }
        }
    }

    @Test
    void wrathDrainFollowsTheShareWithAMinimum() {
        assertEquals(0.5, SinMath.wrathWant(20.0, 0.02, 0.5, 1.0), 1e-9, "20 health: the minimum");
        assertEquals(2.0, SinMath.wrathWant(100.0, 0.02, 0.5, 1.0), 1e-9, "100 health: two percent");
        assertEquals(4.0, SinMath.wrathWant(100.0, 0.02, 0.5, 2.0), 1e-9, "intensity doubles it");
        assertEquals(0.0, SinMath.wrathWant(100.0, 0.02, 0.5, 0.0), 1e-9, "intensity 0 switches it off");
        assertEquals(6.0, SinMath.wrathFloor(20.0, 0.30), 1e-9);
        assertEquals(2.0, SinMath.wrathFloor(20.0, 0.0), 1e-9, "at least one heart");
        assertEquals(1.0, SinMath.wrathFloor(1.0, 0.3), 1e-9, "never above the maximum");
    }

    @Test
    void healsAreOnlyEverReduced() {
        for (Sin sin : SINS) {
            for (double amount : new double[]{0.1, 1.0, 4.0, 40.0}) {
                for (double k : new double[]{0.0, 0.5, 1.0, 3.0, 9.0, -1.0}) {
                    float f = SinMath.healScale(sin, amount, k, 0.3, 0.6, 1.0);
                    assertTrue(f >= 0f && f <= 1f, sin + " " + amount + " " + k + " -> " + f);
                }
            }
        }
        assertEquals(0.7f, SinMath.healScale(Sin.WRATH, 1.0, 1.0, 0.3, 0.6, 1.0), 1e-6f, "Wrath's flames cut every heal");
        assertEquals(0.7f, SinMath.healScale(Sin.WRATH, 30.0, 1.0, 0.3, 0.6, 1.0), 1e-6f);
        assertEquals(1.0f, SinMath.healScale(Sin.PRIDE, 1.0, 1.0, 0.3, 0.6, 1.0), 1e-6f, "natural recovery is not help");
        assertEquals(0.4f, SinMath.healScale(Sin.PRIDE, 4.0, 1.0, 0.3, 0.6, 1.0), 1e-6f, "a potion is help");
        assertEquals(1.0f, SinMath.healScale(Sin.GLUTTONY, 4.0, 1.0, 0.3, 0.6, 1.0), 1e-6f);
        assertEquals(1.0f, SinMath.healScale(Sin.LUST, 4.0, 3.0, 0.3, 0.6, 1.0), 1e-6f);
        assertEquals(0.0f, SinMath.healScale(Sin.PRIDE, 4.0, 3.0, 0.3, 0.6, 1.0), 1e-6f, "never below zero");
    }

    @Test
    void gluttonyCannotStarveItsWielder() {
        assertEquals(0f, SinMath.foodExhaust(3, 3, 0.4, 1.0), 1e-6f, "at the floor nothing is added");
        assertEquals(0f, SinMath.foodExhaust(0, 3, 0.4, 1.0), 1e-6f);
        assertEquals(0.4f, SinMath.foodExhaust(4, 3, 0.4, 1.0), 1e-6f);
        assertEquals(0.8f, SinMath.foodExhaust(20, 3, 0.4, 2.0), 1e-6f);
        assertEquals(0f, SinMath.foodExhaust(20, 3, 0.4, 0.0), 1e-6f);
        assertEquals(0, SinMath.feed(20, 2), "a full stomach takes nothing");
        assertEquals(1, SinMath.feed(19, 2));
        assertEquals(2, SinMath.feed(10, 2));
        assertEquals(0, SinMath.feed(10, -3));
        // the whole walk: from a full stomach, second by second, the food never ends below the floor
        int food = 20;
        float exhaustion = 0f;
        for (int second = 0; second < 5000; second++) {
            exhaustion += SinMath.foodExhaust(food, 3, 0.4, 1.0);
            while (exhaustion >= 4.0f) {
                exhaustion -= 4.0f;
                food = Math.max(0, food - 1);
            }
        }
        assertEquals(3, food, "the curse stops at the floor and never reaches starvation");
    }

    @Test
    void slowsAreClamped() {
        assertEquals(0.20, SinMath.slow(0.20, 1.0), 1e-9);
        assertEquals(0.40, SinMath.slow(0.20, 2.0), 1e-9);
        assertEquals(SinMath.MAX_SLOW, SinMath.slow(0.50, 3.0), 1e-9, "never above the clamp");
        assertEquals(0.0, SinMath.slow(-0.5, 1.0), 1e-9);
        assertEquals(0.0, SinMath.slow(0.2, Double.NaN), 1e-9);
        assertEquals(0.0, SinMath.burden(0, 128, 0.25, 1.0), 1e-9, "no treasure, no burden");
        assertEquals(0.125, SinMath.burden(64, 128, 0.25, 1.0), 1e-9);
        assertEquals(0.25, SinMath.burden(128, 128, 0.25, 1.0), 1e-9);
        assertEquals(0.25, SinMath.burden(100000, 128, 0.25, 1.0), 1e-9, "more treasure than the full score does not slow more");
        assertEquals(0.0, SinMath.burden(50, 0, 0.25, 1.0), 1e-9, "a zero full score is no burden, not a division by zero");
    }

    @Test
    void treasureIsWeighedByItsWorth() {
        assertEquals(1, SinMath.treasureWeight("minecraft:diamond"));
        assertEquals(9, SinMath.treasureWeight("minecraft:diamond_block"));
        assertEquals(27, SinMath.treasureWeight("minecraft:netherite_block"));
        assertEquals(0, SinMath.treasureWeight("minecraft:cobblestone"));
        assertEquals(0, SinMath.treasureWeight(null));
        assertEquals(0, SinMath.treasureWeight("nusmp:legendary_shield"));
    }

    @Test
    void lustPullsTowardTheFoeAndOnlyWithinItsRange() {
        double[] v = SinMath.pull(0, 0, 10, 0, 3.0, 14.0, 0.14);
        assertNotNull(v);
        assertEquals(0.14, v[0], 1e-9);
        assertEquals(0.0, v[1], 1e-9);
        double[] diag = SinMath.pull(0, 0, -6, -8, 3.0, 14.0, 0.5);
        assertNotNull(diag);
        assertEquals(-0.3, diag[0], 1e-9);
        assertEquals(-0.4, diag[1], 1e-9);
        assertEquals(0.5, Math.hypot(diag[0], diag[1]), 1e-9, "the nudge has exactly the strength asked");
        assertNull(SinMath.pull(0, 0, 2, 0, 3.0, 14.0, 0.14), "already in reach");
        assertNull(SinMath.pull(0, 0, 15, 0, 3.0, 14.0, 0.14), "too far to feel it");
        assertNull(SinMath.pull(1, 1, 1, 1, 3.0, 14.0, 0.14), "exactly on top: no direction");
        assertNull(SinMath.pull(0, 0, 10, 0, 3.0, 14.0, 0.0), "no strength, no pull");
        assertNotNull(SinMath.pull(0, 0, 3, 0, 3.0, 14.0, 0.1), "the near edge is inside");
        assertNotNull(SinMath.pull(0, 0, 14, 0, 3.0, 14.0, 0.1), "the far edge is inside");
    }

    @Test
    void slothHoldsTheNextTechniqueBackForAShortWhile() {
        assertTrue(SinMath.slothLocked(1010, 1000, 30));
        assertTrue(SinMath.slothLocked(1029, 1000, 30));
        assertTrue(!SinMath.slothLocked(1030, 1000, 30));
        assertTrue(!SinMath.slothLocked(1010, 0, 30), "no technique yet");
        assertTrue(!SinMath.slothLocked(900, 1000, 30), "a clock that ran backwards is free");
    }

    @Test
    void envyPicksOneOfTheTechniques() {
        assertEquals(-1, SinMath.envyPick(0, 0.5));
        assertEquals(0, SinMath.envyPick(1, 0.99));
        List<Integer> seen = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int k = SinMath.envyPick(5, i / 100.0);
            assertTrue(k >= 0 && k < 5, "index " + k);
            if (!seen.contains(k)) seen.add(k);
        }
        assertEquals(5, seen.size(), "every technique can be chosen");
        assertEquals(4, SinMath.envyPick(5, 5.0));
        assertEquals(0, SinMath.envyPick(5, -3.0));
        assertEquals(0, SinMath.envyPick(5, Double.NaN));
    }
}
