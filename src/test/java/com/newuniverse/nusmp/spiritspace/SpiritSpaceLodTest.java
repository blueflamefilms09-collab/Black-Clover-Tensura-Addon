package com.newuniverse.nusmp.spiritspace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The client cost rules: exact quads per level of detail (the numbers of the doc), the distance bands, the shared frame budget, and that the viewer's own close spirit is never degraded by it. */
class SpiritSpaceLodTest {
    @Test
    void theExactQuadsPerLevelAreTheNumbersOfTheDoc() {
        // orb: a UV sphere (rings x segments) per shell, the rift ring (geo) and the halo sprite
        assertEquals(96, SpiritSpaceMesh.sphereQuads(0));
        assertEquals(60, SpiritSpaceMesh.sphereQuads(1));
        assertEquals(24, SpiritSpaceMesh.sphereQuads(2));
        assertEquals(490, SpiritSpaceMesh.orbQuads(0), "96 x 4 shells + 105 + 1");
        assertEquals(241, SpiritSpaceMesh.orbQuads(1), "60 x 3 shells + 60 + 1");
        assertEquals(66, SpiritSpaceMesh.orbQuads(2), "24 x 2 shells + 18");
        // wisp: a tube per shell, the fins, the head (geo) and its glow-only model
        assertEquals(192, SpiritSpaceMesh.tubeQuads(0));
        assertEquals(96, SpiritSpaceMesh.tubeQuads(1));
        assertEquals(32, SpiritSpaceMesh.tubeQuads(2));
        assertEquals(800, SpiritSpaceMesh.wispQuads(0), "192 x 3 + 72 fins + 115 head + 37 glow");
        assertEquals(368, SpiritSpaceMesh.wispQuads(1), "96 x 2 + 48 fins + 91 head + 37 glow");
        assertEquals(71, SpiritSpaceMesh.wispQuads(2), "32 + 39 head");
        assertEquals(120, SpiritSpaceMesh.portalQuads());
        assertEquals(SpiritSpaceMesh.SPHERE_RINGS.length, SpiritSpaceMesh.LEVELS);
    }

    @Test
    void theBudgetsOfTheSpecHold() {
        assertTrue(SpiritSpaceMesh.orbQuads(0) <= 500 && SpiritSpaceMesh.wispQuads(0) <= 900, "full detail up to about 500 and 900");
        assertTrue(SpiritSpaceMesh.orbQuads(1) <= 250 && SpiritSpaceMesh.wispQuads(1) <= 400, "level 1 about 250 and 400");
        assertTrue(SpiritSpaceMesh.orbQuads(2) <= 100 && SpiritSpaceMesh.wispQuads(2) <= 100, "level 2 at most 100");
        for (boolean wisp : new boolean[]{false, true}) {
            assertTrue(SpiritSpaceLod.cost(wisp, 0) > SpiritSpaceLod.cost(wisp, 1) && SpiritSpaceLod.cost(wisp, 1) > SpiritSpaceLod.cost(wisp, 2));
            assertEquals(0, SpiritSpaceLod.cost(wisp, SpiritSpaceLod.CULL));
        }
        assertEquals(3200, SpiritSpaceLod.FRAME_QUADS);
        assertEquals(64.0, SpiritSpaceLod.ORB_RANGE[2]);
        assertEquals(112.0, SpiritSpaceLod.WISP_RANGE[2]);
    }

    @Test
    void theLevelFollowsTheDistanceAndTheEffectQuality() {
        assertEquals(SpiritSpaceLod.FULL, SpiritSpaceLod.level(false, 15 * 15, 1f));
        assertEquals(SpiritSpaceLod.HALF, SpiritSpaceLod.level(false, 20 * 20, 1f));
        assertEquals(SpiritSpaceLod.LOW, SpiritSpaceLod.level(false, 50 * 50, 1f));
        assertEquals(SpiritSpaceLod.CULL, SpiritSpaceLod.level(false, 65 * 65, 1f), "an orb is culled beyond 64 blocks");
        assertEquals(SpiritSpaceLod.LOW, SpiritSpaceLod.level(true, 100 * 100, 1f));
        assertEquals(SpiritSpaceLod.CULL, SpiritSpaceLod.level(true, 113 * 113, 1f), "a wisp is culled beyond 112 blocks");
        assertEquals(SpiritSpaceLod.HALF, SpiritSpaceLod.level(false, 10 * 10, 0.5f), "a weaker machine gets the lower levels sooner");
        assertEquals(SpiritSpaceLod.level(false, 20 * 20, 1f), SpiritSpaceLod.level(false, 20 * 20, 7f), "the quality is clamped to 1");
        assertEquals(SpiritSpaceLod.level(false, 20 * 20, 0.5f), SpiritSpaceLod.level(false, 20 * 20, -3f), "and to 0.5");
    }

    @Test
    void theSharedBudgetDegradesTheOthersNotTheOwnersCloseSpirit() {
        SpiritSpaceLod.QuadBudget b = new SpiritSpaceLod.QuadBudget();
        long frame = SpiritSpaceLod.frameKey(1000, 0.25f);
        int fullWisps = SpiritSpaceLod.FRAME_QUADS / SpiritSpaceLod.cost(true, 0);
        for (int i = 0; i < fullWisps; i++) assertEquals(SpiritSpaceLod.FULL, SpiritSpaceLod.fit(true, SpiritSpaceLod.FULL, false, b, frame));
        int next = SpiritSpaceLod.fit(true, SpiritSpaceLod.FULL, false, b, frame);
        assertTrue(next > SpiritSpaceLod.FULL, "once the budget is used up the next one is drawn at a lower level");
        assertEquals(SpiritSpaceLod.FULL, SpiritSpaceLod.fit(true, SpiritSpaceLod.FULL, true, b, frame), "the owner's own close wisp is never degraded");
        assertEquals(SpiritSpaceLod.FULL, SpiritSpaceLod.fit(false, SpiritSpaceLod.FULL, true, b, frame));
        assertTrue(b.used() > SpiritSpaceLod.FRAME_QUADS - 1, "the owner's forms are charged to the budget");
        assertEquals(SpiritSpaceLod.CULL, SpiritSpaceLod.fit(true, SpiritSpaceLod.CULL, true, b, frame));
        SpiritSpaceLod.QuadBudget c = new SpiritSpaceLod.QuadBudget();
        long a = SpiritSpaceLod.frameKey(5, 0f);
        assertTrue(c.take(a, 3000, 3200));
        assertFalse(c.take(a, 300, 3200));
        assertTrue(c.take(SpiritSpaceLod.frameKey(5, 0.5f), 300, 3200), "a new frame starts a fresh budget");
        assertNotEquals(SpiritSpaceLod.frameKey(5, 0.5f), SpiritSpaceLod.frameKey(5, 0.25f));
    }

    @Test
    void theWorstFrameIsBoundedNoMatterHowManySpiritsAreVisible() {
        SpiritSpaceLod.QuadBudget b = new SpiritSpaceLod.QuadBudget();
        long frame = SpiritSpaceLod.frameKey(77, 0f);
        int drawn = 0;
        for (int i = 0; i < 48; i++) if (SpiritSpaceLod.fit(false, SpiritSpaceLod.FULL, false, b, frame) != SpiritSpaceLod.CULL) drawn++;
        for (int i = 0; i < 6; i++) if (SpiritSpaceLod.fit(true, SpiritSpaceLod.FULL, false, b, frame) != SpiritSpaceLod.CULL) drawn++;
        assertTrue(drawn > 0);
        assertTrue(b.used() <= SpiritSpaceLod.FRAME_QUADS, "48 orbs and 6 wisps at once still fit the frame budget: " + b.used());
        assertTrue(SpiritSpaceLod.worstFrameQuads() <= 3200 + 800);
    }

    @Test
    void theMeshTablesAreConsistentAndTheSpineIsAsLongAndAsWideAsTheDocSays() {
        assertEquals(SpiritSpaceMesh.sphereXyz(0).length, (SpiritSpaceMesh.SPHERE_RINGS[0] + 1) * (SpiritSpaceMesh.SPHERE_SEGS[0] + 1) * 3);
        float[] xyz = SpiritSpaceMesh.sphereXyz(1);
        for (int i = 0; i < xyz.length; i += 3) assertEquals(1.0, Math.sqrt(xyz[i] * xyz[i] + xyz[i + 1] * xyz[i + 1] + xyz[i + 2] * xyz[i + 2]), 1e-5, "unit vectors");
        float[] tail = new float[3], head = new float[3];
        SpiritSpaceMesh.spine(tail, 0f, 0f);
        SpiritSpaceMesh.spine(head, 1f, 0f);
        assertEquals(SpiritSpaceMesh.WISP_LENGTH, head[2] - tail[2], 1e-5, "3.2 blocks from tail to neck");
        assertTrue(SpiritSpaceMesh.WISP_LENGTH >= 2.5f && SpiritSpaceMesh.WISP_LENGTH <= 3.5f);
        float max = 0f;
        for (int i = 0; i <= 100; i++) max = Math.max(max, SpiritSpaceMesh.bodyRadius(i / 100f));
        assertEquals(SpiritSpaceMesh.WISP_RADIUS, max, 1e-3);
        assertEquals(SpiritSpaceMesh.WISP_NECK, SpiritSpaceMesh.bodyRadius(1f), 1e-5);
        assertTrue(SpiritSpaceMesh.bodyRadius(0f) < 0.03f, "a thin tail tip");
        assertTrue(SpiritSpaceMesh.CORE_RADIUS * 2 >= 0.5f && SpiritSpaceMesh.CORE_RADIUS * 2 <= 0.8f, "the orb is 0.5 to 0.8 blocks");
        float[] mid = new float[3], coiled = new float[3];
        SpiritSpaceMesh.spine(mid, 0.2f, 33f, 0f);
        SpiritSpaceMesh.spine(coiled, 0.2f, 33f, 1f);
        assertTrue(Math.abs(coiled[2] - mid[2]) > 0.1f, "a coil pulls the body in along its length");
        for (int i = 0; i <= 24; i++) {
            float t = i / 24f;
            assertTrue(SpiritSpaceMesh.finWidth(t) >= 0f && SpiritSpaceMesh.finWidth(t) < 0.5f);
            assertTrue(SpiritSpaceMesh.crestHeight(i, 24) >= 0.04f && SpiritSpaceMesh.crestHeight(i, 24) < 0.3f);
        }
    }
}
