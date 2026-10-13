package com.newuniverse.nusmp.entity.avatar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Devil Avatar's arithmetic: the lift, the slam, the flips of the field, the volley, the step, and what every telegraph leaves to leave it by. */
class AvatarMathTest {
    private static final double EPS = 1e-9;

    // ---------------------------------------------------------------- Ceiling Pull
    @Test
    void aResistingFoeGetsHalfThePush() {
        assertEquals(1.0, AvatarMath.resist(false, false));
        assertEquals(0.5, AvatarMath.resist(true, false));
        assertEquals(0.5, AvatarMath.resist(false, true));
        assertEquals(0.5, AvatarMath.resist(true, true), "they do not stack: a half is the most that can be held back");
    }

    @Test
    void theLiftIsTenBlocksAtMostAndNeverReachesTheRoof() {
        assertEquals(10.0, AvatarMath.liftHeight(34, 1.0), EPS, "a tall hall gives the full lift");
        assertEquals(5.0, AvatarMath.liftHeight(34, 0.5), EPS, "a Mana Zone holder is lifted half as high");
        for (double gap = 0; gap <= 40; gap += 0.25) {
            double rise = AvatarMath.liftHeight(gap, 1.0);
            assertTrue(rise == 0 || rise >= AvatarMath.LIFT_MIN, "no pointless nudge at gap " + gap);
            assertTrue(rise <= AvatarMath.LIFT_MAX + EPS);
            if (rise > 0) assertTrue(rise + AvatarMath.LEVITATION_TICKS * 0.05 + 0.5 <= gap + EPS, "the head and the Levitation's drift clear the roof at gap " + gap);
        }
        assertEquals(0.0, AvatarMath.liftHeight(4.9, 1.0), EPS, "a low ceiling gives no lift");
        assertEquals(1.5, AvatarMath.liftHeight(5.0, 1.0), EPS, "and the least worth lifting starts right there");
        assertEquals(0.0, AvatarMath.liftHeight(-3.0, 1.0), EPS);
        assertTrue(AvatarMath.CEILING_CLEARANCE >= AvatarMath.LEVITATION_TICKS * 0.05 + 0.5, "the clearance covers the worst drift of the Levitation");
    }

    @Test
    void theClimbIsFastThenEasesAndFinishesInTime() {
        double prev = Double.MAX_VALUE;
        for (double rem = 10; rem >= 0.3; rem -= 0.1) {
            double v = AvatarMath.liftSpeed(rem);
            assertTrue(v >= AvatarMath.LIFT_SPEED_MIN - EPS && v <= AvatarMath.LIFT_SPEED_MAX + EPS);
            assertTrue(v <= prev + EPS, "the climb never speeds up as it nears the top");
            prev = v;
        }
        for (double rise = AvatarMath.LIFT_MIN; rise <= AvatarMath.LIFT_MAX + EPS; rise += 0.5) {
            double y = 0;
            int ticks = 0;
            while (rise - y > 0.3 && ticks < 200) {
                y += AvatarMath.liftSpeed(rise - y);
                ticks++;
            }
            assertTrue(ticks <= AvatarMath.LIFT_TICKS, "a lift of " + rise + " blocks takes " + ticks + " ticks, over the " + AvatarMath.LIFT_TICKS + " the zone runs");
            assertTrue(y < rise + AvatarMath.LIFT_SPEED_MAX, "and it does not overshoot by more than a step");
        }
    }

    @Test
    void theLevitationHoldsTheFallIsGentleAndTheRainComesWhileTheyHang() {
        assertEquals(60, AvatarMath.LEVITATION_TICKS, "3 s of Levitation");
        assertEquals(AvatarMath.LEVITATION_TICKS + 120, AvatarMath.SLOW_FALL_TICKS, "then 6 s of Slow Falling");
        int last = -1;
        for (int at : AvatarMath.RAIN_AT) {
            assertTrue(at > last + 10, "volleys more than the invulnerability frames apart");
            assertTrue(at > 8 && at < AvatarMath.LEVITATION_TICKS, "the rain falls while they still hang");
            last = at;
        }
        assertTrue(AvatarMath.RAIN_MAX_BOLTS <= 6);
    }

    // ---------------------------------------------------------------- Floor Crush
    @Test
    void theAirIsTheWorstPlaceToBeWhenTheFloorCrushes() {
        assertEquals(1.0, AvatarMath.crushMultiplier(0), EPS);
        assertEquals(1.0, AvatarMath.crushMultiplier(0.75), EPS, "a jump's height still counts as the ground");
        assertEquals(1.7, AvatarMath.crushMultiplier(6.0), EPS);
        assertEquals(1.7, AvatarMath.crushMultiplier(40.0), EPS, "capped: bounded damage");
        double prev = 0;
        for (double h = 0; h <= 12; h += 0.25) {
            double m = AvatarMath.crushMultiplier(h);
            assertTrue(m >= prev - EPS && m >= 1.0 && m <= 1.7 + EPS);
            prev = m;
        }
        assertTrue(AvatarMath.crushMultiplier(AvatarMath.LIFT_MAX) > 1.6, "the foes the pull lifted suffer most");
    }

    @Test
    void theSlamGetsFasterWithHeightAndIsHalvedForAResistingFoe() {
        assertEquals(0.9, AvatarMath.slamSpeed(0, 1.0), EPS);
        assertEquals(1.4, AvatarMath.slamSpeed(10, 1.0), EPS);
        assertEquals(1.4, AvatarMath.slamSpeed(100, 1.0), EPS);
        assertEquals(0.45, AvatarMath.slamSpeed(0, 0.5), EPS);
        for (double h = 0; h < 20; h += 0.5) assertTrue(AvatarMath.slamSpeed(h + 0.5, 1.0) >= AvatarMath.slamSpeed(h, 1.0) - EPS);
    }

    @Test
    void theCrushPlacesMoreMarksAsPowerGrows() {
        assertEquals(1, AvatarMath.markCount(20));
        assertEquals(2, AvatarMath.markCount(40));
        assertEquals(2, AvatarMath.markCount(60));
        assertEquals(3, AvatarMath.markCount(80));
        assertEquals(4, AvatarMath.markCount(100));
        int prev = 0;
        for (int p = 20; p <= 100; p += 10) {
            assertTrue(AvatarMath.markCount(p) >= prev);
            prev = AvatarMath.markCount(p);
        }
    }

    @Test
    void marksNeverStackAndAreCapped() {
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{0, 0});
        pts.add(new double[]{1, 1});
        pts.add(new double[]{10, 0});
        pts.add(new double[]{10.5, 0.2});
        pts.add(new double[]{0, 12});
        List<double[]> out = AvatarMath.spreadMarks(pts, 3.0, 8);
        assertEquals(3, out.size());
        assertArrayEquals(new double[]{0, 0}, out.get(0), "the first always stays");
        assertEquals(2, AvatarMath.spreadMarks(pts, 3.0, 2).size());
        assertEquals(1, AvatarMath.spreadMarks(pts, 100.0, 8).size());
        assertTrue(AvatarMath.spreadMarks(new ArrayList<>(), 3.0, 8).isEmpty());
    }

    // ---------------------------------------------------------------- Inversion Field
    @Test
    void theFieldFlipsEveryThirtyTicksForEightSeconds() {
        assertEquals(160, AvatarMath.FIELD_TICKS, "8 s");
        assertEquals(1, AvatarMath.inversionDir(0), "it begins by lifting");
        assertEquals(1, AvatarMath.inversionDir(29));
        assertEquals(-1, AvatarMath.inversionDir(30));
        assertEquals(-1, AvatarMath.inversionDir(59));
        assertEquals(1, AvatarMath.inversionDir(60));
        assertEquals(-1, AvatarMath.inversionDir(150));
        assertEquals(-1, AvatarMath.inversionDir(159));
        assertEquals(0, AvatarMath.inversionDir(160), "and it ends");
        assertEquals(0, AvatarMath.inversionDir(-1));
        int flips = 0;
        for (int a = 1; a < AvatarMath.FIELD_TICKS; a++) if (AvatarMath.inversionDir(a) != AvatarMath.inversionDir(a - 1)) flips++;
        assertEquals(5, flips, "five flips in eight seconds");
    }

    @Test
    void theFieldPushIsBoundedEasesInAndHalvesForAResistingFoe() {
        for (int a = 0; a < AvatarMath.FIELD_TICKS; a++) {
            double v = AvatarMath.inversionPush(a, 1.0);
            assertEquals(AvatarMath.inversionDir(a), Math.signum(v), "the push follows the direction at " + a);
            assertTrue(Math.abs(v) <= Math.max(AvatarMath.FIELD_UP_SPEED, AvatarMath.FIELD_DOWN_SPEED) + EPS);
            assertEquals(AvatarMath.inversionPush(a, 1.0) * 0.5, AvatarMath.inversionPush(a, 0.5), EPS, "a Mana Zone holder feels half of it at " + a);
        }
        assertEquals(0.0, AvatarMath.inversionPush(160, 1.0), EPS);
        assertTrue(Math.abs(AvatarMath.inversionPush(0, 1.0)) < Math.abs(AvatarMath.inversionPush(AvatarMath.FIELD_RAMP, 1.0)), "each flip eases in");
        assertTrue(Math.abs(AvatarMath.inversionPush(30, 1.0)) < Math.abs(AvatarMath.inversionPush(30 + AvatarMath.FIELD_RAMP, 1.0)));
        assertEquals(AvatarMath.FIELD_UP_SPEED, AvatarMath.inversionPush(AvatarMath.FIELD_RAMP, 1.0), EPS);
        assertEquals(-AvatarMath.FIELD_DOWN_SPEED, AvatarMath.inversionPush(30 + AvatarMath.FIELD_RAMP, 1.0), EPS);
    }

    @Test
    void aFoeLeftInTheFieldNeverClimbsPastItsCeilingAndAlwaysComesDown() {
        double y = 0, top = 0;
        for (int a = 0; a < AvatarMath.FIELD_TICKS; a++) {
            double v = AvatarMath.inversionPush(a, 1.0);
            if (v > 0 && y >= AvatarMath.FIELD_ABOVE - 3) v = 0;          // the glue stops the lift 3 blocks under the top of the field
            y = Math.max(0, y + v);
            top = Math.max(top, y);
        }
        assertTrue(top <= AvatarMath.FIELD_ABOVE, "bounded: " + top);
        assertTrue(top > 2, "and it does lift: " + top);
    }

    // ---------------------------------------------------------------- Hex Volley
    @Test
    void aVolleyHasFiveToNineBoltsAndGrowsWithPower() {
        assertEquals(5, AvatarMath.boltCount(1));
        assertEquals(6, AvatarMath.boltCount(2));
        assertEquals(7, AvatarMath.boltCount(3));
        assertEquals(9, AvatarMath.boltCount(4));
        assertEquals(5, AvatarMath.boltCount(0), "under the range it is the least");
        assertEquals(9, AvatarMath.boltCount(9), "over the range it is the most");
        for (int s = 1; s < 4; s++) assertTrue(AvatarMath.boltCount(s + 1) >= AvatarMath.boltCount(s));
    }

    @Test
    void aVolleyEndsOnTimeAndOneVictimTakesAtMostThreeBolts() {
        assertEquals((9 - 1) * AvatarMath.BOLT_STAGGER + AvatarMath.BOLT_LIFE + 2, AvatarMath.volleyTicks(9));
        assertTrue(AvatarMath.volleyTicks(5) < AvatarMath.volleyTicks(9));
        assertTrue(AvatarMath.volleyTicks(9) < 60, "a volley is over in under three seconds");
        assertEquals(3, AvatarMath.BOLT_MAX_HITS);
        assertTrue(AvatarMath.BOLT_MAX_HITS * 3.5 * 1.6 < 30, "three small hits are no execution");
    }

    @Test
    void theCurseOfAVolleyDeepensWithTheBoltsThatLanded() {
        assertEquals(0, AvatarMath.hexLevel(0, 100));
        assertEquals(1, AvatarMath.hexLevel(1, 20));
        assertEquals(1, AvatarMath.hexLevel(2, 20));
        assertEquals(1, AvatarMath.hexLevel(3, 40), "under 60% the curse is never deeper than level 1");
        assertEquals(2, AvatarMath.hexLevel(3, 60));
        assertEquals(2, AvatarMath.hexLevel(5, 80));
        assertEquals(3, AvatarMath.hexLevel(5, 100));
        assertEquals(3, AvatarMath.hexLevel(9, 100));
        for (int p : new int[]{20, 60, 100}) for (int h = 1; h < 9; h++) assertTrue(AvatarMath.hexLevel(h + 1, p) >= AvatarMath.hexLevel(h, p));
        assertEquals(60, AvatarMath.DREAD_TICKS, "the dread brand lasts 3 s");
    }

    // ---------------------------------------------------------------- Shadow Step and Void Claws
    @Test
    void theStepLandsBehindTheTarget() {
        double[] b = AvatarMath.behind(10, 10, 0f, 2.0);                    // facing +z: behind is -z
        assertEquals(10, b[0], 1e-6);
        assertEquals(8, b[1], 1e-6);
        b = AvatarMath.behind(0, 0, 90f, 2.0);                              // facing -x: behind is +x
        assertEquals(2, b[0], 1e-6);
        assertEquals(0, b[1], 1e-6);
        b = AvatarMath.behind(0, 0, 180f, 3.0);                             // facing -z: behind is +z
        assertEquals(0, b[0], 1e-6);
        assertEquals(3, b[1], 1e-6);
        b = AvatarMath.behind(0, 0, -90f, 1.0);
        assertEquals(-1, b[0], 1e-6);
        for (float yaw = -180; yaw <= 180; yaw += 15) {
            double[] p = AvatarMath.behind(5, -3, yaw, AvatarMath.SHADOW_BEHIND);
            assertEquals(AvatarMath.SHADOW_BEHIND, Math.hypot(p[0] - 5, p[1] + 3), 1e-9);
        }
    }

    @Test
    void theClawsSweepACone() {
        double half = AvatarMath.CLAW_HALF_ANGLE, reach = AvatarMath.CLAW_REACH;
        assertTrue(AvatarMath.inCone(0, 3, 0, 1, half, reach, 0.3), "straight ahead");
        assertTrue(AvatarMath.inCone(0, 0.1, 0, 1, half, reach, 0.3), "on top of it");
        assertFalse(AvatarMath.inCone(0, -3, 0, 1, half, reach, 0.3), "behind");
        assertFalse(AvatarMath.inCone(0, 7.5, 0, 1, half, reach, 0.3), "out of reach");
        assertTrue(AvatarMath.inCone(0, 6.2, 0, 1, half, reach, 0.3), "the body's width counts");
        assertTrue(AvatarMath.inCone(3, 1.5, 0, 1, half, reach, 0.3), "63 degrees off the line is inside 70");
        assertFalse(AvatarMath.inCone(4, 0.5, 0, 1, half, reach, 0.3), "83 degrees off is outside");
        assertFalse(AvatarMath.inCone(1, 1, 0, 0, half, reach, 0.3), "no facing, no cone");
    }

    // ---------------------------------------------------------------- escapes
    @Test
    void everyGroundMarkCanBeWalkedOutOfEvenAtTheGates() {
        int crushAtGates = Math.round(AvatarTable.byId(AvatarTable.FLOOR_CRUSH).windUpTicks() * 0.75f);
        assertTrue(AvatarMath.leavable(crushAtGates, AvatarMath.CRUSH_RADIUS, 0, AvatarMath.WALK),
                "the crush mark (" + AvatarMath.CRUSH_RADIUS + ") can be left from its middle in " + crushAtGates + " ticks");
        int stepAtGates = Math.round(AvatarTable.byId(AvatarTable.SHADOW_STEP).windUpTicks() * 0.75f);
        assertTrue(AvatarMath.leavable(stepAtGates, AvatarMath.SHADOW_STRIKE_RADIUS, AvatarMath.SHADOW_BEHIND, AvatarMath.WALK),
                "someone standing where the Avatar appears behind can step clear of its strike");
        assertFalse(AvatarMath.leavable(5, AvatarMath.CRUSH_RADIUS, 0, AvatarMath.WALK), "and the helper is not a rubber stamp");
        assertEquals(2, AvatarMath.ticksToCover(0.3, 0.2));
        assertEquals(0, AvatarMath.ticksToCover(-1, 0.2));
    }

    @Test
    void theCollapseGivesTheLiftedFoesTimeToSteerOutOfTheirMarks() {
        assertTrue(AvatarMath.ULT_MARK_AT < AvatarMath.ULT_CRUSH_AT && AvatarMath.ULT_CRUSH_AT < AvatarMath.ULT_NOVA_AT);
        int warn = AvatarMath.ULT_CRUSH_AT - AvatarMath.ULT_MARK_AT;
        assertTrue(warn >= 8, "the framework's minimum telegraph");
        assertTrue(AvatarMath.leavable(warn, AvatarMath.CRUSH_RADIUS, 0, AvatarMath.AIR_STRAFE), "steering in the air is enough to leave a mark");
        assertTrue(AvatarMath.ULT_CRUSH_AT < AvatarMath.LEVITATION_TICKS, "the crush lands while they still hang");
        assertTrue(AvatarMath.ULT_MARK_AT > AvatarMath.LIFT_TICKS, "the marks are drawn once the lift is over");
        assertTrue(AvatarMath.ULT_NOVA_AT <= 70, "the whole chain is over in about three seconds");
        assertTrue(AvatarMath.ULT_PULL_RADIUS > AvatarMath.PULL_RADIUS);
        assertEquals(14, AvatarMath.PULL_RADIUS, EPS, "the pull reaches 14 blocks");
        assertTrue(AvatarMath.ULT_MAX_MARKS >= 4 && AvatarMath.ULT_MAX_MARKS <= AvatarMath.PULL_MAX_VICTIMS);
    }

    @Test
    void theNovaGrowsAtTheGates() {
        assertEquals(10.0, AvatarMath.novaReach(false), EPS);
        assertEquals(12.0, AvatarMath.novaReach(true), EPS);
        assertTrue(AvatarMath.ULT_NOVA_REACH > AvatarMath.novaReach(true));
    }
}
