package com.newuniverse.nusmp.heroweapon.weapon.whip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: a tether sets a velocity, and a velocity that is too big or points nowhere can fling a body out of the world or into the ground. The speeds must grow with the distance but
 * stay clamped, the lift must stay inside its range, a close foe must not be pulled, and a direction with no length must give no motion.
 */
class TetherMathTest {
    @Test
    void thePullGrowsWithTheDistanceButIsClamped() {
        assertEquals(TetherMath.PULL_MIN, TetherMath.pullSpeed(0.0), 1e-9);
        assertTrue(TetherMath.pullSpeed(6.0) > TetherMath.pullSpeed(3.0));
        assertEquals(TetherMath.PULL_MAX, TetherMath.pullSpeed(100.0), 1e-9);
        assertEquals(TetherMath.PULL_MIN, TetherMath.pullSpeed(-5.0), 1e-9, "a negative distance is nonsense, not a reverse pull");
        for (double d = 0; d < 40; d += 0.5) {
            double v = TetherMath.pullSpeed(d);
            assertTrue(v >= TetherMath.PULL_MIN && v <= TetherMath.PULL_MAX, "distance " + d);
        }
    }

    @Test
    void aCloseFoeIsNotPulled() {
        assertFalse(TetherMath.needsPull(1.0));
        assertFalse(TetherMath.needsPull(TetherMath.PULL_STOP + TetherMath.PULL_MARGIN));
        assertTrue(TetherMath.needsPull(TetherMath.PULL_STOP + TetherMath.PULL_MARGIN + 0.01));
    }

    @Test
    void theSwingIsClampedAndNeedsAMarkFarEnough() {
        assertFalse(TetherMath.canSwing(2.9));
        assertTrue(TetherMath.canSwing(3.0));
        for (double d = 0; d < 60; d += 0.5) {
            double v = TetherMath.swingSpeed(d);
            assertTrue(v >= TetherMath.SWING_MIN && v <= TetherMath.SWING_MAX, "distance " + d);
            for (double dy = -30; dy <= 30; dy += 3) {
                double lift = TetherMath.swingLift(dy, d);
                assertTrue(lift >= TetherMath.LIFT_MIN && lift <= TetherMath.LIFT_MAX, "dy " + dy + " distance " + d);
            }
        }
        assertTrue(TetherMath.swingLift(10, 10) > TetherMath.swingLift(-10, 10), "a mark above needs more lift than one below");
    }

    @Test
    void theDirectionIsNormalisedAndADegenerateOneGivesNothing() {
        double[] v = TetherMath.toward(3.0, 4.0, 1.0);
        assertEquals(0.6, v[0], 1e-9);
        assertEquals(0.8, v[1], 1e-9);
        double[] none = TetherMath.toward(0.0, 0.0, 1.5);
        assertEquals(0.0, none[0], 1e-12);
        assertEquals(0.0, none[1], 1e-12);
        double[] tiny = TetherMath.toward(1.0E-9, -1.0E-9, 1.5);
        assertEquals(0.0, tiny[0], 1e-12);
        double[] far = TetherMath.toward(-500.0, 0.0, 1.5);
        assertEquals(-1.5, far[0], 1e-9);
    }
}
