package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's rule is that deeper is stronger and smarter, in both directions, and that is a property of plain arithmetic, so it is pinned here without a world: the bonuses never fall
 * as the depth grows, never pass their ceilings, are the same for plus and minus, are zero on the first floor; the mind level steps up exactly at the thresholds; a bad threshold list is
 * refused; the decisions of the behaviours answer as designed. The game side (attribute modifiers, goals) is exercised by the in-game self-test {@code enc.depth}.
 */
class DepthRulesTest {
    @Test
    void deeperIsNeverWeaker() {
        DepthRules.Strength prev = DepthRules.strength(1);
        for (int d = 2; d <= 3000; d++) {
            DepthRules.Strength s = DepthRules.strength(d);
            assertTrue(s.health() >= prev.health() && s.damage() >= prev.damage() && s.speed() >= prev.speed() && s.armor() >= prev.armor()
                    && s.knockback() >= prev.knockback() && s.follow() >= prev.follow(), "a bonus fell at depth " + d);
            prev = s;
        }
    }

    @Test
    void firstFloorIsPlainAndTheSecondAlreadyHarder() {
        assertEquals(DepthRules.Strength.NONE, DepthRules.strength(1));
        assertTrue(DepthRules.strength(2).health() > 0.0 && DepthRules.strength(2).damage() > 0.0);
    }

    @Test
    void everyCurveHasACeiling() {
        DepthRules.Strength s = DepthRules.strength(Integer.MAX_VALUE);
        assertTrue(s.health() <= DepthRules.HEALTH_MAX && s.damage() <= DepthRules.DAMAGE_MAX && s.speed() <= DepthRules.SPEED_MAX && s.armor() <= DepthRules.ARMOR_MAX
                && s.knockback() <= DepthRules.KNOCKBACK_MAX && s.follow() <= DepthRules.FOLLOW_MAX);
        assertTrue(s.health() > DepthRules.HEALTH_MAX * 0.99, "the curve really approaches its ceiling");
        assertTrue(DepthRules.strength(1000).health() < DepthRules.HEALTH_MAX);
    }

    @Test
    void upAndDownAreEquallyDeep() {
        for (int f : new int[]{2, 9, 10, 57, 300, 1000}) assertEquals(DepthRules.depthOf(f), DepthRules.depthOf(-f));
        assertEquals(DepthRules.strength(DepthRules.depthOf(300)), DepthRules.strength(DepthRules.depthOf(-300)));
        assertEquals(1, DepthRules.depthOf(0));
        assertEquals(Integer.MAX_VALUE, DepthRules.depthOf(Integer.MIN_VALUE));
    }

    @Test
    void theDesignedNumbersAtTheLandmarkFloors() {
        // a sector is ten floors: floor 10 is noticeably but not wildly harder, floor 100 clearly, floor 1000 a wall
        assertEquals(0.12, DepthRules.strength(10).health(), 0.01);
        assertEquals(0.99, DepthRules.strength(100).health(), 0.01);
        assertEquals(3.1, DepthRules.strength(1000).health(), 0.1);
    }

    @Test
    void scalingByAShareAndByTheConfigScale() {
        DepthRules.Strength s = DepthRules.strength(100);
        assertEquals(s.health() * 0.5, s.times(0.5).health(), 1e-9);
        assertEquals(DepthRules.Strength.NONE, s.times(0.0));
        assertEquals(DepthRules.Strength.NONE, s.times(Double.NaN));
        assertEquals(DepthRules.Strength.NONE, s.times(-3.0));
    }

    @Test
    void mindStepsUpAtTheThresholds() {
        int[] t = DepthRules.parseThresholds(DepthRules.DEFAULT_MIND_FLOORS);
        assertTrue(java.util.Arrays.equals(new int[]{6, 16, 40, 100, 250, 500}, t));
        assertEquals(0, DepthRules.mind(1, t));
        assertEquals(0, DepthRules.mind(5, t));
        assertEquals(1, DepthRules.mind(6, t));
        assertEquals(2, DepthRules.mind(16, t));
        assertEquals(3, DepthRules.mind(40, t));
        assertEquals(4, DepthRules.mind(100, t));
        assertEquals(5, DepthRules.mind(250, t));
        assertEquals(6, DepthRules.mind(500, t));
        assertEquals(6, DepthRules.mind(5000, t));
        int last = 0;
        for (int d = 1; d < 1200; d++) {
            int m = DepthRules.mind(d, t);
            assertTrue(m >= last, "the mind fell at depth " + d);
            last = m;
        }
    }

    @Test
    void aBadThresholdListFallsBackToTheDefault() {
        int[] def = {6, 16, 40, 100, 250, 500};
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds(null)));
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds("")));
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds("1,2,3")));
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds("a,b,c,d,e,f")));
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds("6,6,40,100,250,500")));
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds("6,16,40,100,250,100")));
        assertTrue(java.util.Arrays.equals(def, DepthRules.parseThresholds("1,16,40,100,250,500")));
        assertTrue(java.util.Arrays.equals(new int[]{3, 5, 8, 13, 21, 34}, DepthRules.parseThresholds(" 3, 5, 8, 13, 21, 34 ")));
    }

    @Test
    void eachLevelUnlocksExactlyOneMoreBehaviour() {
        int count = 0;
        for (DepthRules.Behaviour b : DepthRules.Behaviour.values()) {
            assertEquals(count + 1, b.level());
            assertFalse(DepthRules.knows(count, b));
            assertTrue(DepthRules.knows(count + 1, b));
            count++;
        }
        assertEquals(DepthRules.MAX_MIND, DepthRules.Behaviour.values().length);
    }

    @Test
    void aSharperMindThinksSooner() {
        int prev = Integer.MAX_VALUE;
        for (int m = 0; m <= DepthRules.MAX_MIND; m++) {
            assertTrue(DepthRules.thinkEvery(m) <= prev && DepthRules.thinkEvery(m) >= 4);
            prev = DepthRules.thinkEvery(m);
        }
        assertEquals(20, DepthRules.thinkEvery(0));
    }

    @Test
    void theDecisionsOfTheBehaviours() {
        assertTrue(DepthRules.startRegroup(0.30, false));
        assertFalse(DepthRules.startRegroup(0.31, false));
        assertFalse(DepthRules.startRegroup(0.05, true), "only once in a life");
        assertFalse(DepthRules.endRegroup(0.4, 50));
        assertTrue(DepthRules.endRegroup(0.6, 50));
        assertTrue(DepthRules.endRegroup(0.1, 201), "a regroup cannot last for ever");
        assertTrue(DepthRules.isKited(9.0, 80, false));
        assertFalse(DepthRules.isKited(5.0, 80, false));
        assertFalse(DepthRules.isKited(9.0, 20, false));
        assertFalse(DepthRules.isKited(9.0, 80, true));
        assertTrue(DepthRules.alertRadius(2) >= 12.0 && DepthRules.alertRadius(6) > DepthRules.alertRadius(2) && DepthRules.alertRadius(6) <= 40.0);
        assertTrue(DepthRules.memoryTicks(6) > DepthRules.memoryTicks(1));
    }

    @Test
    void flankersTakeDifferentSides() {
        for (int n = 2; n <= 8; n++) {
            double minGap = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                double a = DepthRules.flankAngle(i, n), b = DepthRules.flankAngle(i + 1, n);
                minGap = Math.min(minGap, Math.abs(Math.sin((b - a) / 2.0)));
            }
            assertTrue(minGap > 0.0, "two flankers share a side with " + n + " in the pack");
        }
        assertEquals(0.0, DepthRules.flankAngle(0, 1), 1e-9);
        assertEquals(DepthRules.flankAngle(1, 4), DepthRules.flankAngle(5, 4), 1e-9);
    }

    @Test
    void theHunterPicksTheSoftestPlayer() {
        double tank = DepthRules.softness(40.0, 20.0), mage = DepthRules.softness(12.0, 0.0), wounded = DepthRules.softness(5.0, 15.0);
        assertTrue(mage < tank);
        assertEquals(1, DepthRules.softest(List.of(tank, mage, wounded)));
        assertEquals(0, DepthRules.softest(List.of(mage, mage)), "a tie keeps the nearer one");
        assertEquals(-1, DepthRules.softest(List.of()));
        assertEquals(0.0, DepthRules.softness(-4.0, -1.0), 1e-9);
    }
}
