package com.newuniverse.nusmp.undine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The client cost rules of the Undine: the levels, the cull distances, the shared frame budget and the owner's never-dropped form. Pure. */
class UndineLodTest {
    private static double sq(double d) { return d * d; }

    @Test
    void theLevelsFollowTheDistanceBandsOfEachForm() {
        assertEquals(UndineLod.FULL, UndineLod.level(false, sq(5), 1f));
        assertEquals(UndineLod.FULL, UndineLod.level(false, sq(16), 1f));
        assertEquals(UndineLod.HALF, UndineLod.level(false, sq(30), 1f));
        assertEquals(UndineLod.LOW, UndineLod.level(false, sq(60), 1f));
        assertEquals(UndineLod.CULL, UndineLod.level(false, sq(64.1), 1f), "the companion is not drawn past 64 blocks");
        assertEquals(UndineLod.FULL, UndineLod.level(true, sq(30), 1f));
        assertEquals(UndineLod.HALF, UndineLod.level(true, sq(60), 1f));
        assertEquals(UndineLod.LOW, UndineLod.level(true, sq(110), 1f));
        assertEquals(UndineLod.CULL, UndineLod.level(true, sq(112.1), 1f), "the full form is not drawn past 112 blocks");
    }

    @Test
    void aWeakerEffectQualityGetsTheLowerLevelsSoonerButNeverBelowAHalf() {
        assertEquals(UndineLod.FULL, UndineLod.level(false, sq(12), 1f));
        assertEquals(UndineLod.HALF, UndineLod.level(false, sq(12), 0.5f));
        assertEquals(UndineLod.CULL, UndineLod.level(false, sq(40), 0.5f));
        assertEquals(UndineLod.level(false, sq(20), 0.1f), UndineLod.level(false, sq(20), 0.5f), "the quality factor is clamped to [0.5, 1]");
        assertEquals(UndineLod.level(false, sq(20), 7f), UndineLod.level(false, sq(20), 1f));
    }

    @Test
    void everyLevelStaysUnderItsBudgetAndEachLevelIsLighterThanTheOneBefore() {
        for (boolean full : new boolean[]{false, true}) {
            int[] cap = full ? UndineLod.FULL_CAP : UndineLod.COMPANION_CAP;
            for (int l = 0; l < 3; l++) assertTrue(UndineLod.cost(full, l) <= cap[l], (full ? "full" : "companion") + " level " + l + " costs " + UndineLod.cost(full, l) + ", the budget is " + cap[l]);
            assertTrue(UndineLod.cost(full, 1) < UndineLod.cost(full, 0));
            assertTrue(UndineLod.cost(full, 2) < UndineLod.cost(full, 1));
            assertEquals(0, UndineLod.cost(full, UndineLod.CULL));
        }
        assertTrue(UndineLod.cost(false, 2) <= 150 && UndineLod.cost(true, 2) <= 150, "the lowest level is at most 150 quads");
        assertEquals(900, UndineLod.COMPANION_CAP[0]);
        assertEquals(1100, UndineLod.FULL_CAP[0]);
        assertTrue(UndineLod.cost(true, 0) > UndineLod.cost(false, 0), "the full form adds the wave base");
        assertEquals(1, UndineLod.FACE_QUADS[0]);
        assertEquals(UndineLod.BODY_QUADS[0] + UndineLod.WATER_QUADS[0] + UndineLod.FX_QUADS[0] + UndineLod.FACE_QUADS[0], UndineLod.cost(false, 0), "the companion's worst case includes the expression card");
    }

    @Test
    void theFrameBudgetDegradesALevelAtATimeAndThenCulls() {
        UndineLod.QuadBudget b = new UndineLod.QuadBudget();
        long k = UndineLod.frameKey(100, 0.5f);
        int fullCost = UndineLod.cost(false, UndineLod.FULL), halfCost = UndineLod.cost(false, UndineLod.HALF), lowCost = UndineLod.cost(false, UndineLod.LOW);
        int drawnFull = 0;
        while (UndineLod.fit(false, UndineLod.FULL, b, k, false) == UndineLod.FULL) drawnFull++;
        assertEquals(UndineLod.FRAME_QUADS / fullCost, drawnFull, "as many full forms as the frame holds");
        assertTrue(b.used() <= UndineLod.FRAME_QUADS);
        // the form that did not fit at FULL was just asked: it got HALF or LOW or nothing; keep filling to see that the order is degrade then cull
        int guard = 0;
        int level;
        while ((level = UndineLod.fit(false, UndineLod.FULL, b, k, false)) != UndineLod.CULL && guard++ < 1000) assertTrue(level == UndineLod.HALF || level == UndineLod.LOW);
        assertEquals(UndineLod.CULL, UndineLod.fit(false, UndineLod.FULL, b, k, false), "when not even the lowest level fits the form is not drawn");
        assertTrue(b.used() + lowCost > UndineLod.FRAME_QUADS && halfCost > lowCost);
        // a new frame starts empty
        assertEquals(UndineLod.FULL, UndineLod.fit(false, UndineLod.FULL, b, UndineLod.frameKey(101, 0.5f), false));
    }

    @Test
    void theOwnersOwnCloseFormIsNeverDropped() {
        UndineLod.QuadBudget b = new UndineLod.QuadBudget();
        long k = UndineLod.frameKey(7, 0f);
        while (UndineLod.fit(true, UndineLod.FULL, b, k, false) != UndineLod.CULL) { /* fill the frame */ }
        assertEquals(UndineLod.CULL, UndineLod.fit(true, UndineLod.FULL, b, k, false));
        int level = UndineLod.fit(true, UndineLod.FULL, b, k, true);
        assertEquals(UndineLod.LOW, level, "the owner's own form degrades to the lowest level but is always drawn");
        assertTrue(UndineLod.ownerPriority(true, sq(5)));
        assertFalse(UndineLod.ownerPriority(true, sq(40)), "an owner's form that is far is an ordinary one");
        assertFalse(UndineLod.ownerPriority(false, sq(2)), "somebody else's form is never a priority");
    }

    @Test
    void theFrameKeyDiffersFromFrameToFrame() {
        assertNotEquals(UndineLod.frameKey(10, 0.25f), UndineLod.frameKey(10, 0.5f));
        assertNotEquals(UndineLod.frameKey(10, 0.25f), UndineLod.frameKey(11, 0.25f));
        assertEquals(3200, UndineLod.FRAME_QUADS);
    }
}
