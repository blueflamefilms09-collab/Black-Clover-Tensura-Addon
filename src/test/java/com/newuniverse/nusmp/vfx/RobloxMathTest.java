package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/vfx_roblox_pass.md): the Roblox-style toolkit cannot be seen here, so the maths it is built from is pinned: the ease and timing curves (a burst is continuous and ends on 0, a shock ring is fast then slow),
 * the flip-book cell maths (every frame is its own cell, row by row), the ribbon taper, the deterministic hash (every client sees the same sparks), the quad costs of the primitives, and the shape list.
 */
class RobloxMathTest {
    private static final float EPS = 1e-5f;

    @AfterEach
    void restore() { RobloxMath.setEnabled(true); }

    @Test
    void curvesRunFromZeroToOneAndAreClamped() {
        for (float[] f : new float[][]{{RobloxMath.easeOut(0f), 0f}, {RobloxMath.easeOut(1f), 1f}, {RobloxMath.easeIn(0f), 0f}, {RobloxMath.easeIn(1f), 1f}, {RobloxMath.smooth(0f), 0f}, {RobloxMath.smooth(1f), 1f},
                {RobloxMath.shockRadius(0f), 0f}, {RobloxMath.shockRadius(1f), 1f}, {RobloxMath.easeOutBack(0f), 0f}, {RobloxMath.easeOutBack(1f), 1f}}) {
            assertEquals(f[1], f[0], EPS);
        }
        assertEquals(0f, RobloxMath.easeOut(-3f), EPS);
        assertEquals(1f, RobloxMath.easeOut(7f), EPS);
        assertEquals(0.5f, RobloxMath.smooth(0.5f), EPS);
        assertTrue(RobloxMath.easeOut(0.3f) > 0.3f && RobloxMath.easeIn(0.3f) < 0.3f);
    }

    @Test
    void aShockRingIsFastThenSlow() {
        assertEquals(0.9375f, RobloxMath.shockRadius(0.5f), EPS);
        assertTrue(RobloxMath.shockRadius(0.5f) > RobloxMath.easeOut(0.5f), "quartic is quicker out of the blocks than cubic");
        float prev = -1f;
        for (int i = 0; i <= 20; i++) {
            float v = RobloxMath.shockRadius(i / 20f);
            assertTrue(v >= prev, "monotonic");
            prev = v;
        }
    }

    @Test
    void easeOutBackOvershootsThenSettlesOnOne() {
        float max = 0f;
        for (int i = 0; i <= 100; i++) max = Math.max(max, RobloxMath.easeOutBack(i / 100f));
        assertTrue(max > 1.05f && max < 1.2f, "a small pop, not a bounce: " + max);
    }

    @Test
    void phaseMapsASubRangeAndIsSafeOnAnEmptyRange() {
        assertEquals(0f, RobloxMath.phase(0.1f, 0.2f, 0.6f), EPS);
        assertEquals(0.5f, RobloxMath.phase(0.4f, 0.2f, 0.6f), EPS);
        assertEquals(1f, RobloxMath.phase(0.9f, 0.2f, 0.6f), EPS);
        assertEquals(1f, RobloxMath.phase(0.5f, 0.3f, 0.3f), EPS);
        assertEquals(0f, RobloxMath.phase(0.2f, 0.3f, 0.3f), EPS);
    }

    @Test
    void theBurstEnvelopeDrawsInBurstsAndDissipates() {
        float a = 0.1f, p = 0.4f;
        assertEquals(0f, RobloxMath.burstEnvelope(0f, a, p), EPS);
        assertEquals(0.2f, RobloxMath.burstEnvelope(a, a, p), EPS, "anticipation ends at 0.2");
        assertEquals(0.2f, RobloxMath.burstEnvelope(a - 1e-4f, a, p), 1e-3f, "continuous at the anticipation join");
        assertEquals(1f, RobloxMath.burstEnvelope(p, a, p), EPS);
        assertEquals(1f, RobloxMath.burstEnvelope(p - 1e-4f, a, p), 1e-3f, "continuous at the peak");
        assertEquals(0f, RobloxMath.burstEnvelope(1f, a, p), EPS, "ends on nothing");
        for (int i = 0; i <= 100; i++) {
            float v = RobloxMath.burstEnvelope(i / 100f, a, p);
            assertTrue(v >= 0f && v <= 1f, "stays in 0..1");
        }
    }

    @Test
    void fadeInOutHoldsInTheMiddle() {
        assertEquals(0f, RobloxMath.fadeInOut(0f, 0.2f, 0.2f), EPS);
        assertEquals(1f, RobloxMath.fadeInOut(0.5f, 0.2f, 0.2f), EPS);
        assertEquals(0f, RobloxMath.fadeInOut(1f, 0.2f, 0.2f), EPS);
        assertEquals(1f, RobloxMath.fadeInOut(0f, 0f, 0f), EPS);
    }

    @Test
    void flipBookCellsAreRowByRowAndEveryFrameIsItsOwnCell() {
        assertEquals(0, RobloxMath.flipFrame(0f, 16));
        assertEquals(15, RobloxMath.flipFrame(1f, 16));
        assertEquals(15, RobloxMath.flipFrame(5f, 16));
        assertEquals(0, RobloxMath.flipFrame(-1f, 16));
        assertEquals(0.5f, RobloxMath.flipBlend(0.5f, 16), EPS);
        assertEquals(0f, RobloxMath.flipBlend(1f, 16), EPS);
        float[] c = new float[4];
        RobloxMath.flipCell(5, RobloxMath.FLIP_COLS, RobloxMath.FLIP_ROWS, c);
        assertArrayEq(new float[]{0.25f, 0.25f, 0.5f, 0.5f}, c);
        RobloxMath.flipCell(0, 4, 4, c);
        assertArrayEq(new float[]{0f, 0f, 0.25f, 0.25f}, c);
        RobloxMath.flipCell(15, 4, 4, c);
        assertArrayEq(new float[]{0.75f, 0.75f, 1f, 1f}, c);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int f = 0; f < 16; f++) {
            RobloxMath.flipCell(f, 4, 4, c);
            assertTrue(seen.add(c[0] + "," + c[1]), "frame " + f + " reuses a cell");
            assertTrue(c[0] >= 0f && c[1] >= 0f && c[2] <= 1f && c[3] <= 1f && c[2] > c[0] && c[3] > c[1]);
        }
    }

    @Test
    void theFlipBookFramesCoverTheWholeLifeInOrder() {
        int prev = -1;
        for (int i = 0; i <= 100; i++) {
            int f = RobloxMath.flipFrame(i / 100f, 16);
            assertTrue(f >= prev && f - prev <= 1, "no frame skipped: " + prev + " -> " + f);
            prev = f;
        }
    }

    @Test
    void aRibbonTapersFromTailToHead() {
        assertEquals(0.1f, RobloxMath.taper(0f, 0.1f, 0.5f, 1f), EPS);
        assertEquals(0.5f, RobloxMath.taper(1f, 0.1f, 0.5f, 1f), EPS);
        assertEquals(0.3f, RobloxMath.taper(0.5f, 0.1f, 0.5f, 1f), EPS);
        assertTrue(RobloxMath.taper(0.5f, 0.1f, 0.5f, 2f) < 0.3f, "pow > 1 stays thin then swells");
        assertTrue(RobloxMath.taper(0.5f, 0.1f, 0.5f, 0.5f) > 0.3f, "pow < 1 is fat quickly");
    }

    @Test
    void theHashIsDeterministicInRangeAndSpreads() {
        assertEquals(RobloxMath.hash(42L, 3, 7), RobloxMath.hash(42L, 3, 7));
        assertNotEquals(RobloxMath.hash(42L, 3, 7), RobloxMath.hash(43L, 3, 7));
        assertNotEquals(RobloxMath.hash(42L, 3, 7), RobloxMath.hash(42L, 4, 7));
        assertNotEquals(RobloxMath.hash(42L, 3, 7), RobloxMath.hash(42L, 3, 8));
        float sum = 0f;
        for (int i = 0; i < 1000; i++) {
            float h = RobloxMath.hash(12345L, i, 1);
            assertTrue(h >= 0f && h < 1f, "in [0,1): " + h);
            sum += h;
        }
        assertTrue(Math.abs(sum / 1000f - 0.5f) < 0.06f, "roughly uniform: mean " + sum / 1000f);
    }

    @Test
    void segFollowsTheDetailLikeTheEngineAndNeverDropsBelowTheMinimum() {
        assertEquals(16, RobloxMath.seg(16, 8, 1f));
        assertEquals(8, RobloxMath.seg(16, 8, 0.5f));
        assertEquals(8, RobloxMath.seg(16, 8, 0.1f));
        assertEquals(8, RobloxMath.seg(16, 8, 0f));
    }

    @Test
    void theQuadCostsOfThePrimitivesAreTheirGeometry() {
        assertEquals(1, RobloxMath.swooshQuads(2));
        assertEquals(7, RobloxMath.swooshQuads(8));
        assertEquals(0, RobloxMath.swooshQuads(1));
        assertEquals(0, RobloxMath.swooshQuads(0));
        assertEquals(12, RobloxMath.ringQuads(12));
        assertEquals(20, RobloxMath.coneQuads(10, 2));
        assertEquals(1, RobloxMath.flipbookQuads(false));
        assertEquals(2, RobloxMath.flipbookQuads(true));
        assertEquals(3, RobloxMath.coreShellQuads(true));
        assertEquals(2, RobloxMath.coreShellQuads(false));
    }

    @Test
    void theShapeListIsTenDistinctShapesAndTheSwitchDefaultsOn() {
        assertEquals(10, RobloxMath.SHAPES.length);
        assertEquals(10, new java.util.HashSet<>(java.util.Arrays.asList(RobloxMath.SHAPES)).size());
        assertTrue(RobloxMath.owns(VfxShape.KI_BEAM) && RobloxMath.owns(VfxShape.WATER_BURST) && RobloxMath.owns(VfxShape.MAGIC_CIRCLE_EXPLOSION));
        int owned = 0;
        for (VfxShape v : VfxShape.values()) if (RobloxMath.owns(v)) owned++;
        assertEquals(10, owned, "owns() is exactly the list");
        assertTrue(RobloxMath.enabled());
        RobloxMath.setEnabled(false);
        assertFalse(RobloxMath.enabled());
    }

    private static void assertArrayEq(float[] want, float[] got) {
        for (int i = 0; i < want.length; i++) assertEquals(want[i], got[i], EPS, "index " + i);
    }
}
