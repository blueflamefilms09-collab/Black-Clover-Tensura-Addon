package com.newuniverse.nusmp.client.gui.bossbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The boss bar's shake, glitch, colour morph and shatter state machine: deterministic for a seed, bounded, allocation free. */
class BossBarAnimTest {
    private static final int GOLD = 0xE8B923, GOLD_LIGHT = 0xFFF0A0, PURPLE = 0x3B0F6B, VIOLET = 0xB07CFF;

    private static BossBarAnim fresh(long seed) {
        BossBarAnim a = new BossBarAnim(seed);
        a.update(100, 1f, 0, GOLD, GOLD_LIGHT, false);
        return a;
    }

    @Test
    void aBarThatJustAppearedDoesNothing() {
        BossBarAnim a = fresh(1);
        a.frame(100.0);
        assertEquals(0f, a.shakeX(), 0.0001);
        assertEquals(0f, a.shakeY(), 0.0001);
        assertEquals(0, a.sliceCount());
        assertFalse(a.shattering());
        assertEquals(GOLD, a.primary());
        assertEquals(1f, a.morph(), 0.0001);
    }

    @Test
    void aSlowDrainNeverShakes() {
        BossBarAnim a = fresh(2);
        float p = 1f;
        for (long t = 101; t < 400; t++) {
            p -= 0.003f;                                            // 3% per 10 ticks
            a.update(t, p, 0, GOLD, GOLD_LIGHT, false);
            a.frame(t + 0.5);
            assertFalse(a.shaking(), "tick " + t);
        }
    }

    @Test
    void aHeavyHitShakesForTwelveTicksWithinFourPixels() {
        BossBarAnim a = fresh(3);
        a.update(101, 0.90f, 0, GOLD, GOLD_LIGHT, false);           // -10% in one tick
        boolean moved = false, sliced = false, flashed = false;
        for (int i = 0; i < 12; i++) {
            a.frame(101 + i + 0.25);
            assertTrue(Math.abs(a.shakeX()) <= BossBarAnim.MAX_SHAKE_PX + 1e-4, "x " + a.shakeX());
            assertTrue(Math.abs(a.shakeY()) <= BossBarAnim.MAX_SHAKE_PX + 1e-4, "y " + a.shakeY());
            moved |= a.shakeX() != 0f || a.shakeY() != 0f;
            sliced |= a.sliceCount() > 0;
            flashed |= a.flash() > 0f;
        }
        assertTrue(moved && sliced && flashed);
        a.frame(101 + 11.5);
        assertTrue(a.shaking(), "still trembling in the last tick");
        a.frame(101 + 12.0);
        assertFalse(a.shaking(), "calm again after 12 ticks");
        assertEquals(0, a.sliceCount());
    }

    @Test
    void aHugeHitIsStillCappedAtFourPixels() {
        BossBarAnim a = fresh(12);
        a.update(101, 0.40f, 0, GOLD, GOLD_LIGHT, false);           // -60%
        float peak = 0;
        for (double t = 101; t < 113; t += 0.05) {
            a.frame(t);
            peak = Math.max(peak, Math.max(Math.abs(a.shakeX()), Math.abs(a.shakeY())));
            assertTrue(Math.abs(a.shakeX()) <= 4.0001f && Math.abs(a.shakeY()) <= 4.0001f);
        }
        assertTrue(peak > 3f, "a heavy hit uses the whole range: " + peak);
    }

    @Test
    void oneHitIsOneShakeNotAnEndlessOne() {
        BossBarAnim a = fresh(13);
        a.update(101, 0.80f, 0, GOLD, GOLD_LIGHT, false);
        for (long t = 102; t <= 125; t++) {
            a.update(t, 0.80f, 0, GOLD, GOLD_LIGHT, false);         // the health stays where it is
            a.frame(t + 0.5);
            if (t >= 113) assertFalse(a.shaking(), "tick " + t);
        }
    }

    @Test
    void theShakeDecays() {
        BossBarAnim a = fresh(4);
        a.update(101, 0.80f, 0, GOLD, GOLD_LIGHT, false);
        float early = 0, late = 0;
        for (double t = 101; t < 104; t += 0.1) { a.frame(t); early = Math.max(early, Math.max(Math.abs(a.shakeX()), Math.abs(a.shakeY()))); }
        for (double t = 109; t < 112; t += 0.1) { a.frame(t); late = Math.max(late, Math.max(Math.abs(a.shakeX()), Math.abs(a.shakeY()))); }
        assertTrue(early > late * 3, early + " vs " + late);
    }

    @Test
    void theSlicesStayInsideTheFillAndChangeEveryTick() {
        BossBarAnim a = fresh(5);
        a.update(101, 0.85f, 0, GOLD, GOLD_LIGHT, false);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) {
            a.frame(101 + i);
            assertTrue(a.sliceCount() >= 1 && a.sliceCount() <= BossBarAnim.MAX_SLICES);
            StringBuilder sb = new StringBuilder();
            for (int s = 0; s < a.sliceCount(); s++) {
                assertTrue(a.sliceY(s) >= 0 && a.sliceH(s) >= 1 && a.sliceY(s) + a.sliceH(s) <= BossBarAnim.FILL_ROWS, "rows");
                assertTrue(a.sliceDx(s) != 0 && Math.abs(a.sliceDx(s)) <= 10, "dx " + a.sliceDx(s));
                sb.append(a.sliceY(s)).append(',').append(a.sliceH(s)).append(',').append(a.sliceDx(s)).append(';');
            }
            seen.add(sb.toString());
        }
        assertTrue(seen.size() >= 3, "the glitch is new every tick: " + seen);
    }

    @Test
    void theSameSeedGivesTheSameFrames() {
        BossBarAnim a = fresh(77), b = fresh(77), c = fresh(78);
        boolean differs = false;
        for (BossBarAnim x : new BossBarAnim[]{a, b, c}) x.update(101, 0.80f, 1, PURPLE, VIOLET, true);
        for (int i = 0; i < 40; i++) {
            double t = 101 + i * 0.75;
            a.frame(t); b.frame(t); c.frame(t);
            assertEquals(a.shakeX(), b.shakeX(), 0.0);
            assertEquals(a.shakeY(), b.shakeY(), 0.0);
            assertEquals(a.sliceCount(), b.sliceCount());
            assertEquals(a.primary(), b.primary());
            for (int k = 0; k < BossBarAnim.COLUMNS; k++) { assertEquals(a.colDx(k), b.colDx(k), 0.0); assertEquals(a.colDy(k), b.colDy(k), 0.0); }
            differs |= a.colDx(3) != c.colDx(3) || a.sliceCount() != c.sliceCount();
        }
        assertTrue(differs, "another seed gives another show");
    }

    @Test
    void aPhaseChangeMorphsTheColourOverFortyTicks() {
        BossBarAnim a = fresh(6);
        a.update(110, 1f, 4, PURPLE, VIOLET, true);                 // the Gates open: gold turns deep purple
        a.frame(110.0);
        assertEquals(GOLD, a.primary(), "starts on the old colour");
        a.frame(130.0);
        int mid = a.primary();
        assertNotEquals(GOLD, mid);
        assertNotEquals(PURPLE, mid);
        assertTrue(a.morph() > 0.4f && a.morph() < 0.6f, "halfway at 20 ticks: " + a.morph());
        a.frame(150.0);
        assertEquals(PURPLE, a.primary());
        assertEquals(VIOLET, a.secondary());
        assertEquals(1f, a.morph(), 0.0001);
    }

    @Test
    void aSecondMorphStartsFromTheColourOnScreen() {
        BossBarAnim a = fresh(7);
        a.update(110, 1f, 3, PURPLE, VIOLET, false);
        a.frame(125.0);
        int shown = a.primary();
        a.update(125, 1f, 4, 0x123456, 0x654321, false);
        a.frame(125.0);
        assertEquals(shown, a.primary(), "no jump when the target changes mid-morph");
    }

    @Test
    void aPhaseChangeShattersThenReformsInThirtyTicks() {
        BossBarAnim a = fresh(8);
        a.update(110, 1f, 1, GOLD, GOLD_LIGHT, false);
        float peak = 0;
        for (int i = 0; i < 30; i++) {
            a.frame(110 + i);
            assertTrue(a.shattering() || i == 0, "tick " + i);
            for (int k = 0; k < BossBarAnim.COLUMNS; k++) peak = Math.max(peak, Math.abs(a.colDx(k)));
            assertTrue(a.shatterAlpha() <= 1f && a.shatterAlpha() >= 0.5f);
        }
        assertTrue(peak > 10f, "the columns fly apart: " + peak);
        a.frame(110.0);
        for (int k = 0; k < BossBarAnim.COLUMNS; k++) { assertEquals(0f, a.colDx(k), 0.001); assertEquals(0f, a.colDy(k), 0.001); }
        a.frame(110 + 15.0);
        float spread = 0;
        for (int k = 0; k < BossBarAnim.COLUMNS; k++) spread = Math.max(spread, Math.abs(a.colDx(k)));
        assertTrue(spread > 10f, "widest in the middle");
        a.frame(110 + 29.99);
        for (int k = 0; k < BossBarAnim.COLUMNS; k++) { assertEquals(0f, a.colDx(k), 0.5); assertEquals(0f, a.colDy(k), 0.5); }
        a.frame(110 + 30.0);
        assertFalse(a.shattering());
        for (int k = 0; k < BossBarAnim.COLUMNS; k++) { assertEquals(0f, a.colDx(k), 0.0); assertEquals(0f, a.colDy(k), 0.0); }
        assertEquals(1f, a.shatterAlpha(), 0.0);
    }

    @Test
    void theColumnsFlyApartAndFall() {
        BossBarAnim a = fresh(9);
        a.update(110, 1f, 2, GOLD, GOLD_LIGHT, false);
        a.frame(125.0);                                             // the widest point
        assertTrue(a.colDx(0) < 0 && a.colDx(BossBarAnim.COLUMNS - 1) > 0, "outer columns go outward");
        float lowest = -1000;
        for (int k = 0; k < BossBarAnim.COLUMNS; k++) {
            assertTrue(Math.abs(a.colDx(k)) <= 40.001f && Math.abs(a.colDy(k)) <= 24.001f, "bounded");
            lowest = Math.max(lowest, a.colDy(k));
        }
        assertTrue(lowest > 0, "something has fallen below its place");
    }

    @Test
    void sameValuesAgainAreIgnoredAndGapsAreHandled() {
        BossBarAnim a = fresh(10);
        a.update(101, 0.97f, 0, GOLD, GOLD_LIGHT, false);
        a.update(101, 0.10f, 0, GOLD, GOLD_LIGHT, false);           // the same tick again: ignored
        a.frame(101.0);
        assertFalse(a.shaking());
        a.update(500, 0.10f, 0, GOLD, GOLD_LIGHT, false);           // a long gap (the bar was off screen): history restarts from the last value
        a.frame(500.0);
        assertTrue(a.shaking(), "the fall since the last sample counts even after a long gap");
        a.update(501, 0.10f, 0, GOLD, GOLD_LIGHT, false);
        a.update(520, 0.10f, 0, GOLD, GOLD_LIGHT, false);
        a.frame(520.0);
        assertFalse(a.shaking());
    }

    @Test
    void healingAndFullBarsNeverShake() {
        BossBarAnim a = new BossBarAnim(11);
        a.update(1, 0.2f, 0, GOLD, GOLD_LIGHT, false);
        for (long t = 2; t < 60; t++) {
            a.update(t, Math.min(1f, 0.2f + 0.05f * (t - 1)), 0, GOLD, GOLD_LIGHT, false);
            a.frame(t);
            assertFalse(a.shaking());
        }
    }
}
