package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 3.36.0: the cinematic plan is bounded (rule 11): a few rectangles a frame, always inside the screen, bystanders get less, a faded scene draws nothing. */
class LightFrameTest {
    private static final int[][] SIZES = {{427, 240}, {854, 480}, {960, 540}, {1920, 1080}, {320, 180}};

    @Test
    void neverMoreThanTheBudgetOfRectanglesAndAlwaysInsideTheScreen() {
        LightFrame f = new LightFrame();
        int most = 0;
        for (int[] sz : SIZES) {
            int w = sz[0], h = sz[1];
            for (boolean target : new boolean[]{true, false}) {
                for (float age = 0; age <= LightScene.CLIENT_HARD_LIMIT + 20; age += 0.25f) {
                    f.plan(age, w, h, target, 1f);
                    assertTrue(f.n <= LightFrame.MAX_QUADS, "age " + age + " has " + f.n);
                    most = Math.max(most, f.n);
                    for (int i = 0; i < f.n; i++) {
                        assertTrue(f.x0[i] >= 0 && f.y0[i] >= 0 && f.x1[i] <= w && f.y1[i] <= h, "quad " + i + " of age " + age + " leaves the screen " + w + "x" + h);
                        assertTrue(f.x1[i] > f.x0[i] || f.y1[i] > f.y0[i] || f.x1[i] == f.x0[i], "degenerate");
                        assertTrue((f.argb[i] >>> 24) > 0, "an invisible quad was kept");
                    }
                    assertTrue(Math.abs(f.shakeX) <= 8f && Math.abs(f.shakeY) <= 8f, "shake out of bounds at " + age);
                }
            }
        }
        assertTrue(most <= 16, "a frame never needs more than a dozen or so rectangles, it used " + most);
    }

    @Test
    void aBystanderGetsOnlyBarsAndCaptions() {
        LightFrame f = new LightFrame();
        for (int age = 0; age < LightScene.TOTAL_TICKS; age++) {
            f.plan(age, 854, 480, false, 1f);
            assertTrue(f.n <= 2, "bystander at " + age + " has " + f.n + " rectangles");
            assertEquals(0f, f.shakeX);
            assertEquals(0f, f.shakeY);
            assertFalse(f.page);
            assertEquals(LightFrame.CARD_NONE, f.card, "title cards are the target's");
        }
    }

    @Test
    void theNameIsWrittenOnlyDuringTheWritingBeatAndFinishesAtTheEnd() {
        LightFrame f = new LightFrame();
        f.plan(LightScene.CHIP_TICKS + 10, 854, 480, true, 1f);
        assertFalse(f.page);
        assertEquals(0f, f.write);
        f.plan(LightScene.CHIP_TICKS + LightScene.RAISE_TICKS + 40, 854, 480, true, 1f);
        assertTrue(f.page);
        assertEquals(0.5f, f.write, 0.01f);
        f.plan(LightScene.TOTAL_TICKS - 1, 854, 480, true, 1f);
        assertTrue(f.write > 0.98f);
        assertEquals(0, LightFrame.charsWritten(0f, 8));
        assertEquals(8, LightFrame.charsWritten(1f, 8));
        assertEquals(4, LightFrame.charsWritten(0.5f, 8));
        assertEquals(0, LightFrame.charsWritten(0.5f, 0));
    }

    @Test
    void aFadedSceneAndAnEmptyScreenDrawNothing() {
        LightFrame f = new LightFrame();
        f.plan(100, 854, 480, true, 0f);
        assertEquals(0, f.n);
        f.plan(100, 0, 0, true, 1f);
        assertEquals(0, f.n);
    }

    @Test
    void thePlanIsDeterministicSoTwoFramesOfTheSameTickAreIdentical() {
        LightFrame a = new LightFrame(), b = new LightFrame();
        a.plan(33, 854, 480, true, 1f);
        b.plan(33, 854, 480, true, 1f);
        assertEquals(a.n, b.n);
        for (int i = 0; i < a.n; i++) assertEquals(a.argb[i], b.argb[i]);
        assertEquals(a.shakeX, b.shakeX);
    }

    @Test
    void theCaptionsAndCardsFollowTheBeats() {
        LightFrame f = new LightFrame();
        f.plan(20, 854, 480, true, 1f);
        assertEquals(LightFrame.CAPTION_CHIP, f.caption);
        f.plan(40, 854, 480, true, 1f);
        assertEquals(LightFrame.CARD_CHIP, f.card);
        f.plan(90, 854, 480, true, 1f);
        assertEquals(LightFrame.CARD_PEN, f.card);
        f.plan(120, 854, 480, true, 1f);
        assertEquals(LightFrame.CARD_NOTE, f.card);
        assertEquals(LightFrame.CAPTION_WRITING, f.caption);
    }
}
