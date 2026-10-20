package com.newuniverse.nusmp.client.relics.djinn.tiny;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Tiny Djinn: its size, the level-of-detail limits, the cap, the shoulder anchor, the spring, the facing, the dash and the exit spiral. */
class TinyDjinnMathTest {
    @Test
    void theFigureIsAboutHalfABlockTall() {
        float k = TinyDjinnMath.modelScale(false);
        float blocks = TinyDjinnMath.MODEL_HEIGHT_PX / 16f * k;
        assertEquals(0.45f, blocks, 0.001f, "0.35 to 0.5 block: the chibi of the brief");
        assertTrue(blocks >= 0.35f && blocks <= 0.5f);
        assertTrue(TinyDjinnMath.modelScale(true) < k, "smaller in first person");
        assertEquals(k * TinyDjinnMath.FIRST_PERSON_SCALE, TinyDjinnMath.modelScale(true), 1e-6f);
    }

    @Test
    void levelOfDetailLimitsAreFortyEightAndTwentyFour() {
        assertTrue(TinyDjinnMath.inRange(48.0 * 48.0));
        assertFalse(TinyDjinnMath.inRange(48.0 * 48.0 + 0.01), "skipped beyond 48 blocks");
        assertTrue(TinyDjinnMath.withGlow(24.0 * 24.0));
        assertFalse(TinyDjinnMath.withGlow(24.0 * 24.0 + 0.01), "no additive glow beyond 24 blocks");
        assertTrue(TinyDjinnMath.withName(16.0 * 16.0));
        assertFalse(TinyDjinnMath.withName(17.0 * 17.0));
        assertTrue(TinyDjinnMath.GLOW < TinyDjinnMath.FAR);
    }

    @Test
    void theConfiguredCapIsClamped() {
        assertEquals(16, TinyDjinnMath.DEFAULT_MAX);
        assertEquals(0, TinyDjinnMath.clampMax(-5));
        assertEquals(16, TinyDjinnMath.clampMax(16));
        assertEquals(64, TinyDjinnMath.clampMax(1000));
    }

    @Test
    void theCapKeepsTheNearest() {
        double[] d2 = {400, 4, 100, 9, 25};
        boolean[] keep = new boolean[5];
        assertEquals(2, TinyDjinnMath.cap(d2, 5, 2, keep));
        assertFalse(keep[0]);
        assertTrue(keep[1]);
        assertFalse(keep[2]);
        assertTrue(keep[3]);
        assertFalse(keep[4]);
        assertEquals(5, TinyDjinnMath.cap(d2, 5, 9, keep), "a cap above the count keeps all");
        assertEquals(0, TinyDjinnMath.cap(d2, 5, 0, keep), "a cap of zero keeps none");
        for (boolean k : keep) assertFalse(k);
    }

    @Test
    void theCapBreaksTiesByIndexAndNeverKeepsMoreThanAsked() {
        double[] d2 = {9, 9, 9, 9, 9, 9};
        boolean[] keep = new boolean[6];
        assertEquals(3, TinyDjinnMath.cap(d2, 6, 3, keep));
        assertTrue(keep[0] && keep[1] && keep[2]);
        assertFalse(keep[3] || keep[4] || keep[5]);
        // a big crowd: exactly max stay, and they are the nearest
        double[] many = new double[40];
        boolean[] k2 = new boolean[40];
        for (int i = 0; i < 40; i++) many[i] = (i * 17 % 40) + 1.0;
        assertEquals(16, TinyDjinnMath.cap(many, 40, 16, k2));
        for (int i = 0; i < 40; i++) assertEquals(many[i] <= 16.0, k2[i], "distance " + many[i]);
    }

    @Test
    void theAnchorIsOnTheOwnersRightBehindAndAboveTheShoulder() {
        float[] out = new float[3];
        // facing south (yaw 0): forward +z, the owner's right is -x
        TinyDjinnMath.anchor(out, 10, 64, 20, 0f, 1.8f, false, false);
        assertTrue(out[0] < 10f - 0.4f && out[0] > 10f - 0.6f, "to the right: " + out[0]);
        assertTrue(out[2] < 20f, "behind: " + out[2]);
        assertTrue(out[1] > 64f + 1.7f && out[1] < 64f + 2.0f, "above the shoulder");
        // facing west (yaw 90): forward -x, the owner's right is -z
        TinyDjinnMath.anchor(out, 0, 0, 0, 90f, 1.8f, false, false);
        assertTrue(out[2] < -0.4f, "right is -z when facing west: " + out[2]);
        assertTrue(out[0] > 0f, "behind is +x when facing west: " + out[0]);
        // the offset has the same length for every yaw
        for (float yaw = 0; yaw < 360; yaw += 37) {
            TinyDjinnMath.anchor(out, 0, 0, 0, yaw, 1.8f, false, false);
            double horiz = Math.sqrt(out[0] * out[0] + out[2] * out[2]);
            assertEquals(Math.sqrt(0.5 * 0.5 + 0.3 * 0.3), horiz, 1e-4, "yaw " + yaw);
        }
    }

    @Test
    void sneakingLowersAndAFirstPersonFigureSitsLowAndOut() {
        float[] up = new float[3], sneak = new float[3], first = new float[3];
        TinyDjinnMath.anchor(up, 0, 0, 0, 0f, 1.8f, false, false);
        TinyDjinnMath.anchor(sneak, 0, 0, 0, 0f, 1.8f, true, false);
        TinyDjinnMath.anchor(first, 0, 0, 0, 0f, 1.8f, false, true);
        assertTrue(sneak[1] < up[1], "a sneaking owner lowers it");
        assertTrue(first[1] < up[1], "first person: low, out of the view");
        assertTrue(first[0] < up[0], "first person: further to the right");
        assertTrue(first[2] > up[2], "first person: not behind the camera's eye");
    }

    @Test
    void theSpringSettlesOnItsTargetWithoutOvershootingWhenCriticallyDamped() {
        float[] pos = {0, 0, 0}, vel = {0, 0, 0}, target = {3, -2, 1};
        float maxX = 0;
        for (int i = 0; i < 400; i++) {
            TinyDjinnMath.spring(pos, vel, target, 9f, 1f, 1f / 60f);
            maxX = Math.max(maxX, pos[0]);
        }
        assertEquals(3f, pos[0], 0.01f);
        assertEquals(-2f, pos[1], 0.01f);
        assertEquals(1f, pos[2], 0.01f);
        assertTrue(maxX <= 3.001f, "critical damping does not overshoot: " + maxX);
    }

    @Test
    void anUnderdampedSpringOvershootsAndStillSettles() {
        float[] pos = {0, 0, 0}, vel = {0, 0, 0}, target = {1, 0, 0};
        float maxX = 0;
        for (int i = 0; i < 600; i++) {
            TinyDjinnMath.spring(pos, vel, target, 12f, 0.3f, 1f / 60f);
            maxX = Math.max(maxX, pos[0]);
        }
        assertTrue(maxX > 1.05f, "zeta 0.3 swings past the target: " + maxX);
        assertEquals(1f, pos[0], 0.01f);
    }

    @Test
    void theSpringIsStableForAnyFrameLengthAndSnapsAfterAGap() {
        float[] pos = {0, 0, 0}, vel = {0, 0, 0}, target = {5, 5, 5};
        TinyDjinnMath.spring(pos, vel, target, 30f, 0.9f, 0.4f);                    // a very long frame
        for (int k = 0; k < 3; k++) assertTrue(Math.abs(pos[k]) < 50f && !Float.isNaN(pos[k]), "no blow-up: " + pos[k]);
        float[] p2 = {0, 0, 0}, v2 = {1, 1, 1};
        TinyDjinnMath.spring(p2, v2, target, 9f, 0.8f, 2.0f);                       // not drawn for two seconds
        assertEquals(5f, p2[0]);
        assertEquals(0f, v2[0]);
        float[] p3 = {1, 1, 1}, v3 = {0, 0, 0};
        TinyDjinnMath.spring(p3, v3, target, 9f, 0.8f, 0f);                         // a zero step does nothing
        assertEquals(1f, p3[0]);
        TinyDjinnMath.spring(p3, v3, target, 9f, 0.8f, -1f);
        assertEquals(1f, p3[0]);
    }

    @Test
    void yawsTurnTheShortWay() {
        assertEquals(0f, TinyDjinnMath.yawTo(0, 1), 1e-4f, "south");
        assertEquals(90f, TinyDjinnMath.yawTo(-1, 0), 1e-4f, "west");
        assertEquals(-90f, TinyDjinnMath.yawTo(1, 0), 1e-4f, "east");
        assertEquals(180f, Math.abs(TinyDjinnMath.yawTo(0, -1)), 1e-4f, "north");
        assertEquals(-170f, TinyDjinnMath.wrap180(190f), 1e-4f);
        assertEquals(170f, TinyDjinnMath.wrap180(-190f), 1e-4f);
        assertEquals(10f, TinyDjinnMath.wrap180(370f), 1e-4f);
        assertEquals(180f, TinyDjinnMath.turnToward(160f, -170f, 20f), 1e-4f, "160 to -170 goes the short way up through 180");
        assertTrue(TinyDjinnMath.turnToward(160f, -170f, 5f) > 160f, "it turns up through 180, not down through 0");
        assertEquals(-170f, TinyDjinnMath.turnToward(160f, -170f, 90f), 1e-4f, "within reach it arrives");
        assertEquals(10f, TinyDjinnMath.turnToward(0f, 90f, 10f), 1e-4f);
        assertEquals(-10f, TinyDjinnMath.turnToward(0f, -90f, 10f), 1e-4f);
    }

    @Test
    void theDashGoesOutHoldsAndComesBack() {
        assertEquals(0f, TinyDjinnMath.dash(0f));
        assertEquals(0f, TinyDjinnMath.dash(-3f));
        assertEquals(1f, TinyDjinnMath.dash(TinyDjinnMath.DASH_OUT), 1e-5f);
        assertEquals(1f, TinyDjinnMath.dash(TinyDjinnMath.DASH_OUT + 1f), 1e-5f, "a beat at the foe");
        assertEquals(0f, TinyDjinnMath.dash(TinyDjinnMath.DASH_TOTAL));
        assertEquals(12f, TinyDjinnMath.DASH_TOTAL, "the same 0.6 s as the attack clip");
        float prev = 0f;
        for (float t = 0f; t <= TinyDjinnMath.DASH_OUT; t += 0.25f) {
            float v = TinyDjinnMath.dash(t);
            assertTrue(v >= prev - 1e-6f, "out is monotonic");
            prev = v;
        }
        for (float t = TinyDjinnMath.DASH_OUT + TinyDjinnMath.DASH_HOLD; t <= TinyDjinnMath.DASH_TOTAL; t += 0.25f) {
            float v = TinyDjinnMath.dash(t);
            assertTrue(v <= prev + 1e-6f, "back is monotonic");
            prev = v;
        }
    }

    @Test
    void theStrikePointStopsShortOfTheFoe() {
        float[] out = new float[3];
        TinyDjinnMath.strikePoint(out, new float[]{0, 0, 0}, new float[]{10, 0, 0}, 0.65f);
        assertEquals(9.35f, out[0], 1e-4f);
        TinyDjinnMath.strikePoint(out, new float[]{0, 0, 0}, new float[]{0.3f, 0, 0}, 0.65f);
        assertEquals(0f, out[0], 1e-4f, "a foe closer than the stop distance: it stays put");
        TinyDjinnMath.strikePoint(out, new float[]{1, 2, 3}, new float[]{1, 2, 3}, 0.65f);
        assertEquals(1f, out[0], 1e-4f);
        assertEquals(3f, out[2], 1e-4f);
    }

    @Test
    void theExitSpiralEndsAtTheVesselAndClosesItsRadius() {
        float[] from = {2, 1, 0}, to = {0, 1.2f, 0}, out = new float[3];
        TinyDjinnMath.spiral(out, from, to, 0f, 0.26f);
        assertEquals(2.26f, out[0], 1e-4f, "the start is at the figure, at the full radius round it");
        TinyDjinnMath.spiral(out, from, to, 1f, 0.26f);
        assertEquals(0f, out[0], 1e-4f);
        assertEquals(1.2f, out[1], 1e-4f);
        assertEquals(0f, out[2], 1e-4f);
        // it turns: the horizontal offset from the straight line changes sign on the way
        boolean positive = false, negative = false;
        for (float e = 0.05f; e < 0.95f; e += 0.05f) {
            TinyDjinnMath.spiral(out, from, to, e, 0.26f);
            if (out[2] > 0.01f) positive = true;
            if (out[2] < -0.01f) negative = true;
        }
        assertTrue(positive && negative, "it winds round the line");
        TinyDjinnMath.spiral(out, from, to, 7f, 0.26f);
        assertEquals(0f, out[0], 1e-4f, "e is clamped");
    }

    @Test
    void theBobStaysSmall() {
        for (float t = 0; t < 400; t += 3.3f) {
            float b = TinyDjinnMath.bob(t, 0.37f);
            assertTrue(Math.abs(b) <= 0.0451f);
        }
        assertTrue(TinyDjinnMath.bob(10f, 0f) != TinyDjinnMath.bob(10f, 0.5f), "figures are out of step");
    }
}
