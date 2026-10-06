package com.newuniverse.nusmp.core.magic.grimoire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The floating shelf's bob, opening ease, arc layout, facing and look-picking, all headless. */
class GrimoireShelfLayoutTest {
    private static final float EPS = 1e-5f;

    @Test
    void bookStartsBelowAndHasNoBobBeforeItsTurn() {
        GrimoireShelfLayout.Pose p = GrimoireShelfLayout.open(3, 5, 0f);
        assertEquals(0f, p.ease(), EPS);
        assertEquals(0f, p.bob(), EPS);
        assertEquals(GrimoireShelfLayout.RISE_FROM, p.up(), EPS);
    }

    @Test
    void arrivedBookSitsAtRestPlusBob() {
        int n = 5;
        for (int i = 0; i < n; i++) {
            float age = 200f + i;
            GrimoireShelfLayout.Pose p = GrimoireShelfLayout.open(i, n, age);
            GrimoireShelfLayout.Rest r = GrimoireShelfLayout.rest(i, n);
            float bob = 0.04f * (float) Math.sin(age * 0.12f + i * 7);
            assertEquals(1f, p.ease(), EPS);
            assertEquals(bob, p.bob(), EPS);
            assertEquals(-0.6f + (r.up() + 0.6f) * 1f + bob, p.up(), EPS);
            assertEquals(r.right(), p.right(), EPS);
            assertEquals(r.forward(), p.forward(), EPS);
        }
    }

    @Test
    void bobStaysWithinTwoAndAHalfCentimetres() {
        for (int i = 0; i < 12; i++) {
            for (float age = 0; age < 300; age += 0.37f) {
                float b = GrimoireShelfLayout.open(i, 12, age).bob();
                assertTrue(Math.abs(b) <= GrimoireShelfLayout.BOB_AMPLITUDE + EPS, "bob too big: " + b);
            }
        }
    }

    @Test
    void bobPeriodIsAbout52Ticks() {
        double period = 2 * Math.PI / GrimoireShelfLayout.BOB_SPEED;
        assertEquals(52.36, period, 0.01);
        float a = GrimoireShelfLayout.bob(2, 100f, 1f);
        float b = GrimoireShelfLayout.bob(2, (float) (100f + period), 1f);
        assertEquals(a, b, 1e-4f);
    }

    @Test
    void neighbouringBooksBobOutOfStep() {
        float age = 120f;
        assertNotEquals(GrimoireShelfLayout.bob(0, age, 1f), GrimoireShelfLayout.bob(1, age, 1f), 1e-3f);
    }

    @Test
    void easeRisesMonotonicallyFromZeroToOne() {
        float prev = 0f;
        for (float age = 0; age <= 40; age += 0.25f) {
            float e = GrimoireShelfLayout.ease(4, age);
            assertTrue(e >= prev - EPS && e >= 0f && e <= 1f, "ease out of order at " + age);
            prev = e;
        }
        assertEquals(0f, GrimoireShelfLayout.ease(4, 4 * GrimoireShelfLayout.STAGGER_TICKS), EPS);
        assertEquals(1f, prev, EPS);
    }

    @Test
    void arcIsSymmetricAndInFront() {
        int n = 5;
        for (int i = 0; i < n; i++) {
            GrimoireShelfLayout.Rest a = GrimoireShelfLayout.rest(i, n), b = GrimoireShelfLayout.rest(n - 1 - i, n);
            assertEquals(a.right(), -b.right(), EPS);
            assertEquals(a.forward(), b.forward(), EPS);
            assertTrue(a.forward() > 0f);
            assertEquals(GrimoireShelfLayout.RADIUS, Math.hypot(a.right(), a.forward()), 1e-4);
        }
        assertEquals(0f, GrimoireShelfLayout.rest(2, 5).right(), EPS);
    }

    @Test
    void moreThanOneRowStacksRowsDownwards() {
        int n = GrimoireShelfLayout.PER_ROW + 3;
        GrimoireShelfLayout.Rest top = GrimoireShelfLayout.rest(0, n);
        GrimoireShelfLayout.Rest bottom = GrimoireShelfLayout.rest(GrimoireShelfLayout.PER_ROW, n);
        assertEquals(GrimoireShelfLayout.ROW_GAP, top.up() - bottom.up(), EPS);
        assertEquals(0f, top.up() + bottom.up(), EPS);
    }

    @Test
    void worldOffsetFollowsMinecraftYaw() {
        // yaw 0 faces south (+Z), right hand is west (-X)
        assertEquals(1.0, GrimoireShelfLayout.worldDZ(0f, 1f, 0f), 1e-9);
        assertEquals(-1.0, GrimoireShelfLayout.worldDX(1f, 0f, 0f), 1e-9);
        // yaw 90 faces west (-X), right hand is north (-Z)
        assertEquals(-1.0, GrimoireShelfLayout.worldDX(0f, 1f, 90f), 1e-9);
        assertEquals(-1.0, GrimoireShelfLayout.worldDZ(1f, 0f, 90f), 1e-9);
    }

    @Test
    void faceYawTurnsFrontTowardsViewer() {
        assertEquals(0f, GrimoireShelfLayout.faceYaw(0, 1), EPS);
        assertEquals((float) (Math.PI / 2), GrimoireShelfLayout.faceYaw(1, 0), EPS);
        float yaw = GrimoireShelfLayout.faceYaw(-3, -4);
        assertEquals(-0.6, Math.sin(yaw), 1e-6);
        assertEquals(-0.8, Math.cos(yaw), 1e-6);
    }

    @Test
    void pickFindsTheBookOnTheLookRayOnly() {
        double[] centers = {0, 0, 2, 1, 0, 2, 0, 0, -2};
        assertEquals(0, GrimoireShelfLayout.pick(0, 0, 0, 0, 0, 1, centers, 0.3));
        double len = Math.sqrt(1 + 4);
        assertEquals(1, GrimoireShelfLayout.pick(0, 0, 0, 1 / len, 0, 2 / len, centers, 0.3));
        assertEquals(-1, GrimoireShelfLayout.pick(0, 0, 0, 0, 1, 0, centers, 0.3));
        assertEquals(2, GrimoireShelfLayout.pick(0, 0, 0, 0, 0, -1, centers, 0.3));
    }
}
