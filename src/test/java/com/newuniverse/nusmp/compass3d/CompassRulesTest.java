package com.newuniverse.nusmp.compass3d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the 3D Compass Magic's decisions (prop word, where the compass floats, where its needle points, fades, caps, the marching route, the seal's flight) are pure, so they are pinned here
 * (docs/magic/compass_3d.md).
 */
class CompassRulesTest {
    @Test
    void theMainPropWordRoundTrips() {
        for (int mode = 0; mode < 4; mode++) {
            for (int fade : new int[]{0, 1, 7, 14, 62}) {
                int w = CompassRules.packMain(200f, 35f, mode, fade);
                assertEquals(mode, CompassRules.mode(w));
                assertEquals(fade, CompassRules.fadeLeft(w));
                assertEquals(200f, CompassRules.aimYaw(w), 1.5f);
                assertEquals(35f, CompassRules.aimPitch(w), 1.5f);
                assertEquals(0, CompassRules.variant(w), "the variant of the compass itself is 0");
                assertTrue(w >= 0, "the word stays a positive int (bit 31 unused)");
            }
        }
        assertEquals(0, CompassRules.fadeLeft(CompassRules.packMain(0, 0, 0, 0)));
        assertEquals(62, CompassRules.fadeLeft(CompassRules.packMain(0, 0, 0, 500)), "a fade longer than the field holds is clamped, not wrapped");
    }

    @Test
    void theAimYawWrapsAndThePitchIsClamped() {
        assertEquals(CompassRules.aimYaw(CompassRules.packMain(10f, 0, 0, 0)), CompassRules.aimYaw(CompassRules.packMain(370f, 0, 0, 0)), 1e-3f);
        assertEquals(CompassRules.aimYaw(CompassRules.packMain(350f, 0, 0, 0)), CompassRules.aimYaw(CompassRules.packMain(-10f, 0, 0, 0)), 1e-3f);
        assertEquals(90f, CompassRules.aimPitch(CompassRules.packMain(0, 400f, 0, 0)), 1e-3f);
        assertEquals(-90f, CompassRules.aimPitch(CompassRules.packMain(0, -400f, 0, 0)), 1e-3f);
    }

    @Test
    void theSpellPropWordCarriesVariantAndIndex() {
        for (int v = CompassRules.V_NEEDLE; v <= CompassRules.V_ROUTE; v++) {
            int w = CompassRules.packProp(v, 19);
            assertEquals(v, CompassRules.variant(w));
            assertEquals(19, CompassRules.index(w));
            assertTrue(CompassRules.knownVariant(w));
        }
        assertFalse(CompassRules.knownVariant(CompassRules.packProp(9, 0)));
    }

    @Test
    void theCompassFloatsAtTheCastersRightAndBehind() {
        // yaw 0 faces +z: the right hand is -x, behind is -z
        double[] p = CompassRules.mainPosition(10, 64, 20, 0f);
        assertEquals(10 - CompassRules.SIDE, p[0], 1e-9);
        assertEquals(64 + CompassRules.UP, p[1], 1e-9);
        assertEquals(20 - CompassRules.BACK, p[2], 1e-9);
        // yaw 90 faces -x: the right hand is -z, behind is +x
        p = CompassRules.mainPosition(0, 0, 0, 90f);
        assertEquals(CompassRules.BACK, p[0], 1e-9);
        assertEquals(-CompassRules.SIDE, p[2], 1e-9);
        // the distance to the caster does not depend on the yaw
        for (float yaw = -180; yaw <= 180; yaw += 37.5f) {
            double[] q = CompassRules.mainPosition(3, 0, -4, yaw);
            double dx = q[0] - 3, dz = q[2] + 4;
            assertEquals(Math.hypot(CompassRules.SIDE, CompassRules.BACK), Math.hypot(dx, dz), 1e-9);
        }
    }

    @Test
    void theCompassTurnsOutwardFromTheCasterAndApproachTakesTheShortWay() {
        assertEquals(30f + (float) CompassRules.OUT_YAW, CompassRules.mainYaw(30f), 1e-6f);
        assertEquals(180f, CompassRules.approachDegrees(170f, -170f, 0.5f), 1e-3f, "from 170 to -170 the short way is +20, so half of it is 180");
        assertEquals(10f, CompassRules.approachDegrees(0f, 20f, 0.5f), 1e-4f);
        assertEquals(20f, CompassRules.approachDegrees(0f, 20f, 3f), 1e-4f, "k is clamped to 1");
    }

    @Test
    void theNeedlePointsAtTheDirectionInTheCompassPlane() {
        // a compass with yaw 0: the model's x axis is world +x, y is up, the front is +z
        assertEquals(0f, CompassRules.needleAngle(0, 1, 0, 0f), 1e-4f, "straight up = north up");
        assertEquals(-90f, CompassRules.needleAngle(1, 0, 0, 0f), 1e-4f, "world +x is the model's right: the needle (pointing +y at 0) turns clockwise by -90 about z");
        assertEquals(90f, CompassRules.needleAngle(-1, 0, 0, 0f), 1e-4f);
        assertEquals(180f, Math.abs(CompassRules.needleAngle(0, -1, 0, 0f)), 1e-4f);
        assertEquals(0f, CompassRules.needleAngle(0, 0, 5, 0f), 1e-6f, "straight out of the face has no direction in the plane");
        // turn the compass by 90 degrees: the model's x axis is now world +z
        assertEquals(-90f, CompassRules.needleAngle(0, 0, 1, 90f), 1e-3f);
        // the same direction seen from a compass turned the other way round gives the mirror
        assertEquals(CompassRules.needleAngle(1, 1, 0, 0f), -CompassRules.needleAngle(-1, 1, 0, 0f), 1e-4f);
    }

    @Test
    void theNeedleSwingSettlesOnItsGoal() {
        assertEquals(10f, CompassRules.needleSwing(10f, 80f, 0f), 1e-3f, "at age 0 the needle has not moved");
        assertEquals(80f, CompassRules.needleSwing(10f, 80f, 60f), 0.01f, "after a second it has settled");
        boolean overshoot = false;
        for (float a = 0; a < 20; a += 0.5f) if (CompassRules.needleSwing(0f, 100f, a) > 100.5f) overshoot = true;
        assertTrue(overshoot, "it rings past the goal once, like a real needle");
        // the short way round
        assertEquals(190f, CompassRules.needleSwing(170f, -170f, 60f), 0.05f, "170 to -170 is +20 the short way: it ends at 190, the same direction as -170");
    }

    @Test
    void thePopInOvershootsAndThenSettlesAtOne() {
        assertEquals(0f, CompassRules.popIn(0f, 10f), 1e-5f);
        assertEquals(1f, CompassRules.popIn(10f, 10f), 1e-5f);
        assertEquals(1f, CompassRules.popIn(99f, 10f), 1e-5f);
        float max = 0;
        for (float t = 0; t <= 10; t += 0.25f) max = Math.max(max, CompassRules.popIn(t, 10f));
        assertTrue(max > 1.02f && max < 1.15f, "a small overshoot: " + max);
    }

    @Test
    void theOpacityFadesInThenOutAndStaysInsideZeroToOne() {
        int calm = CompassRules.packMain(0, 0, 0, 0);
        assertEquals(0f, CompassRules.opacity(0f, calm, 0f), 1e-6f);
        assertEquals(1f, CompassRules.opacity(CompassRules.FADE_IN, calm, 0f), 1e-6f);
        assertEquals(1f, CompassRules.opacity(500f, calm, 0.5f), 1e-6f);
        int fading = CompassRules.packMain(0, 0, 0, CompassRules.FADE);
        assertEquals(1f, CompassRules.opacity(500f, fading, 0f), 1e-6f, "just began to fade");
        int nearly = CompassRules.packMain(0, 0, 0, 1);
        assertTrue(CompassRules.opacity(500f, nearly, 0f) < 0.1f);
        float last = 2f;
        for (int left = CompassRules.FADE; left >= 1; left--) {
            float o = CompassRules.opacity(500f, CompassRules.packMain(0, 0, 0, left), 0f);
            assertTrue(o >= 0f && o <= 1f && o <= last + 1e-6f, "monotone fade at " + left);
            last = o;
        }
    }

    @Test
    void theRouteHasMarchingDashesThatStayOnTheRoute() {
        assertEquals(2, CompassRules.dashes(0.5));
        assertEquals(14, CompassRules.dashes(500));
        int n = CompassRules.dashes(13);
        assertEquals(10, n);
        for (float t = 0; t < 200; t += 7.3f) {
            for (int k = 0; k < n; k++) {
                float[] d = CompassRules.dash(k, n, t);
                assertTrue(d[0] >= 0f && d[1] <= 1f && d[0] <= d[1], "dash " + k + " at " + t + ": " + d[0] + " " + d[1]);
            }
        }
        // it marches: the first dash moves toward the target as time passes
        assertTrue(CompassRules.dash(0, n, 10f)[0] > CompassRules.dash(0, n, 0f)[0]);
    }

    @Test
    void theSealFliesFromTheCompassAndLandsOnTheFoe() {
        assertEquals(0.0, CompassRules.ease(0, CompassRules.SEAL_FLIGHT), 1e-9);
        assertEquals(1.0, CompassRules.ease(CompassRules.SEAL_FLIGHT, CompassRules.SEAL_FLIGHT), 1e-9);
        assertEquals(1.0, CompassRules.ease(CompassRules.SEAL_FLIGHT + 50, CompassRules.SEAL_FLIGHT), 1e-9);
        double last = -1;
        for (int t = 0; t <= CompassRules.SEAL_FLIGHT; t++) {
            double e = CompassRules.ease(t, CompassRules.SEAL_FLIGHT);
            assertTrue(e >= last, "monotone");
            last = e;
        }
    }

    @Test
    void theCapsLeaveRoomOnlyWhileUnderThem() {
        assertEquals(CompassRules.CAP_NEEDLES, CompassRules.room(0, CompassRules.CAP_NEEDLES));
        assertEquals(0, CompassRules.room(CompassRules.CAP_NEEDLES, CompassRules.CAP_NEEDLES));
        assertEquals(0, CompassRules.room(99, CompassRules.CAP_NEEDLES), "never negative");
        // the biggest spell (Compass Rose: 10 foes x 2 needles) fits the needle cap, and its seals fit the seal cap
        assertTrue(20 <= CompassRules.CAP_NEEDLES);
        assertTrue(10 <= CompassRules.CAP_SEALS);
        assertTrue(CompassRules.LINGER > CompassRules.FADE * 4);
    }
}
