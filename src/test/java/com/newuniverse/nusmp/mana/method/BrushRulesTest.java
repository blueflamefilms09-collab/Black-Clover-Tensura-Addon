package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 3.6.0, the Brush Rune: caps, lifetimes, the trigger geometry on a floor, a wall and a ceiling, the staggered checks, the picture's frame. */
class BrushRulesTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;

    // ---------------------------------------------------------------- caps
    @Test
    void thePerCasterAndTheGlobalCapsBothHold() {
        assertTrue(BrushRules.allowed(0, 0, 3, 24));
        assertTrue(BrushRules.allowed(2, 23, 3, 24));
        assertFalse(BrushRules.allowed(3, 0, 3, 24), "the caster has painted three");
        assertFalse(BrushRules.allowed(0, 24, 3, 24), "the server has 24");
        assertTrue(BrushRules.allowed(0, 0, 0, 24), "a setting of 0 is clamped to one");
        assertFalse(BrushRules.allowed(1, 0, 0, 24));
    }

    @Test
    void theSettingsAreClampedToTheirRanges() {
        assertEquals(1, BrushRules.clampPerCaster(-9));
        assertEquals(BrushRules.MAX_PER_CASTER, BrushRules.clampPerCaster(900));
        assertEquals(BrushRules.MIN_GLOBAL, BrushRules.clampGlobal(0));
        assertEquals(BrushRules.MAX_GLOBAL, BrushRules.clampGlobal(100000));
        assertEquals(BrushRules.MIN_SECONDS, BrushRules.seconds(1));
        assertEquals(BrushRules.MAX_SECONDS, BrushRules.seconds(100000));
        assertFalse(BrushRules.allowed(BrushRules.MAX_PER_CASTER, 0, 1000, 1000), "even a huge setting cannot pass the hard maximum");
        assertFalse(BrushRules.allowed(0, BrushRules.MAX_GLOBAL, 1000, 1000));
    }

    // ---------------------------------------------------------------- lifetimes
    @Test
    void theLifeIsHalfForATapAndWholeForAFullHold() {
        assertEquals(300, BrushRules.lifeTicks(RuneCharge.of(0, MIN, MAX), 30));
        assertEquals(600, BrushRules.lifeTicks(RuneCharge.of(MAX, MIN, MAX), 30));
        assertEquals(BrushRules.MAX_SECONDS * 20, BrushRules.lifeTicks(RuneCharge.of(MAX, MIN, MAX), 99999));
        assertTrue(BrushRules.lifeTicks(RuneCharge.of(0, MIN, MAX), BrushRules.MIN_SECONDS) >= 20);
        long last = 0;
        for (int h = 0; h <= RuneCharge.HOLD_CAP; h++) {
            int t = BrushRules.lifeTicks(RuneCharge.of(h, MIN, MAX), 30);
            assertTrue(t >= last, "a longer hold never lives shorter");
            last = t;
        }
    }

    @Test
    void anArrayLivesUntilItsEndTickAndIsArmedAfterItsDrawingGrace() {
        assertTrue(BrushRules.alive(99, 100));
        assertFalse(BrushRules.alive(100, 100));
        assertFalse(BrushRules.armed(1000 + BrushRules.ARM_TICKS - 1, 1000));
        assertTrue(BrushRules.armed(1000 + BrushRules.ARM_TICKS, 1000));
    }

    @Test
    void theChecksAreStaggeredByIdSoNotEveryArrayLooksInTheSameTick() {
        int[] looking = new int[BrushRules.CHECK_EVERY];
        for (int id = 1; id <= 24; id++) {
            int fired = 0;
            for (long t = 1000; t < 1000 + BrushRules.CHECK_EVERY; t++) {
                if (BrushRules.due(t, id)) { fired++; looking[(int) (t - 1000)]++; }
            }
            assertEquals(1, fired, "every array looks once in every " + BrushRules.CHECK_EVERY + " ticks (id " + id + ")");
        }
        for (int n : looking) assertTrue(n <= 24 / BrushRules.CHECK_EVERY + 1, "no tick takes more than its share: " + n);
    }

    @Test
    void theQueryBudgetBeatsTheWorstCaseOfEveryArrayAtOnce() {
        assertTrue(BrushRules.DEFAULT_GLOBAL / BrushRules.CHECK_EVERY <= BrushRules.QUERIES_PER_TICK, "the default 24 arrays average 6 queries a tick, under the budget of 8");
    }

    // ---------------------------------------------------------------- spacing
    @Test
    void anArrayKeepsItsDistanceFromTheCastersOthers() {
        assertTrue(BrushRules.tooClose(0, 0, 0, 1.0, 0, 0));
        assertFalse(BrushRules.tooClose(0, 0, 0, BrushRules.MIN_SPACING + 0.01, 0, 0));
        assertTrue(BrushRules.tooClose(5, 5, 5, 5, 5, 5));
    }

    // ---------------------------------------------------------------- the trigger
    @Test
    void standingInAFloorArrayTriggersIt() {
        double r = 1.5;
        assertTrue(BrushRules.triggered(0, 64, 0, 0, 1, 0, 0.5, 64.0, 0.5, r), "feet on the floor inside the radius");
        assertTrue(BrushRules.triggered(0, 64, 0, 0, 1, 0, 0.0, 65.0, 0.0, r), "a head above the array still counts: the nearest point of the body is used by the server");
        assertFalse(BrushRules.triggered(0, 64, 0, 0, 1, 0, 2.0, 64.0, 0.0, r), "outside the radius in the plane");
        assertFalse(BrushRules.triggered(0, 64, 0, 0, 1, 0, 0.0, 66.5, 0.0, r), "a flyer high above is beyond the reach out of the surface");
        assertFalse(BrushRules.triggered(0, 64, 0, 0, 1, 0, 0.0, 63.0, 0.0, r), "below the floor is not in the array");
    }

    @Test
    void standingInFrontOfAWallArrayTriggersIt() {
        double r = 1.5;
        // a wall facing east (normal +x), painted at x = 10
        assertTrue(BrushRules.triggered(10, 65, 0, 1, 0, 0, 11.0, 65.0, 0.5, r));
        assertFalse(BrushRules.triggered(10, 65, 0, 1, 0, 0, 9.0, 65.0, 0.0, r), "behind the wall");
        assertFalse(BrushRules.triggered(10, 65, 0, 1, 0, 0, 14.0, 65.0, 0.0, r), "too far out from the wall");
        assertFalse(BrushRules.triggered(10, 65, 0, 1, 0, 0, 11.0, 65.0, 3.0, r), "outside the radius along the wall");
        // a wall facing north (normal -z)
        assertTrue(BrushRules.triggered(0, 65, 10, 0, 0, -1, 0.0, 65.5, 9.0, r));
    }

    @Test
    void aCeilingArrayTriggersForWhoStandsBelowIt() {
        assertTrue(BrushRules.triggered(0, 70, 0, 0, -1, 0, 0.5, 69.0, 0.0, 1.5));
        assertFalse(BrushRules.triggered(0, 70, 0, 0, -1, 0, 0.5, 71.0, 0.0, 1.5), "above the ceiling");
    }

    @Test
    void theNearestPointOfABoxIsTheClampOnEachAxis() {
        assertEquals(2.0, BrushRules.nearest(0.0, 2.0, 3.0), 0.0);
        assertEquals(3.0, BrushRules.nearest(9.0, 2.0, 3.0), 0.0);
        assertEquals(2.5, BrushRules.nearest(2.5, 2.0, 3.0), 0.0);
    }

    @Test
    void theRadiusGrowsWithTheChargeInsideItsRange() {
        double tap = BrushRules.radius(RuneCharge.of(0, MIN, MAX)), full = BrushRules.radius(RuneCharge.of(MAX, MIN, MAX));
        assertEquals(BrushRules.RADIUS_MIN, tap, 1e-9);
        assertEquals(BrushRules.RADIUS_MAX, full, 1e-9);
        double old = BrushRules.radius(Charge.LEGACY);
        assertTrue(old > tap && old < full);
    }

    // ---------------------------------------------------------------- the picture
    @Test
    void theFacesHaveUnitNormalsInTheOrderOfMinecraftsDirections() {
        double[][] want = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        double[] n = new double[3];
        for (int f = 0; f < 6; f++) {
            BrushRules.normal(f, n);
            assertArrayEquals(want[f], n, 0.0, "face " + f);
        }
        BrushRules.normal(99, n);
        assertArrayEquals(new double[]{0, 1, 0}, n, 0.0, "anything unknown is up");
    }

    @Test
    void thePictureFacesAgainstTheSurfaceNormalAndIsUnit() {
        double[] look = new double[3];
        BrushRules.lookFor(1, 0, 0, 33.0, look);
        assertEquals(-1.0, look[0], 1e-9);
        assertEquals(0.0, look[1], 1e-9);
        BrushRules.lookFor(0, 1, 0, 90.0, look);
        assertEquals(1.0, Math.sqrt(look[0] * look[0] + look[1] * look[1] + look[2] * look[2]), 1e-9);
        assertTrue(look[1] < -0.99, "a floor rune is seen from above");
        double[] lean0 = new double[3], lean1 = new double[3];
        BrushRules.lookFor(0, 1, 0, 0.0, lean0);
        BrushRules.lookFor(0, 1, 0, 180.0, lean1);
        assertTrue(Math.abs(lean0[2] - lean1[2]) > 1e-6, "the caster's yaw turns the glyphs upright toward the caster");
    }

    @Test
    void theNumbersForThePacketRoundTrip() {
        assertEquals(15, BrushRules.tenths(1.5));
        assertEquals(255, BrushRules.tenths(999.0));
        assertEquals(0, BrushRules.tenths(-1.0));
        for (double yaw : new double[]{-180, -90, 0, 45, 90, 179.9, 270}) {
            int b = BrushRules.yawByte(yaw);
            assertTrue(b >= 0 && b <= 255);
            double back = BrushRules.yawDegrees(b), want = ((yaw % 360) + 360) % 360;
            double diff = Math.abs(back - want);
            assertTrue(Math.min(diff, 360 - diff) <= 360.0 / 256 + 1e-9, "yaw " + yaw + " came back as " + back);
        }
    }

    @Test
    void thePictureScaleFitsTheTriggerRadiusInsideSaneLimits() {
        float s = BrushRules.pictureScale(1.5, 1.5);
        assertTrue(s > 0.8f && s < 1.1f);
        assertEquals(1f, BrushRules.pictureScale(1.5, 0.0), 0f);
        assertTrue(BrushRules.pictureScale(100.0, 0.1) <= 3.0f);
        assertTrue(BrushRules.pictureScale(0.01, 5.0) >= 0.3f);
    }

    @Test
    void thePaintedRunesDrawnAtOnceAreCappedAndFarOnesAreSkipped() {
        assertEquals(8, BrushRules.MAX_PAINTED_DRAWN);
        assertTrue(BrushRules.PAINT_DISTANCE <= 64.0,"a painted rune is only ever sent to players inside the packet range");
    }
}
