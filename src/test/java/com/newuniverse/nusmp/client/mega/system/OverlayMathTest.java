package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the holographic window's look is decided by a handful of arithmetic helpers (bar segments, the countdown, the glitch and cursor timing, the scanline
 * creep, the anchor positions at any scale, the alpha scaling) and a toast's animation by one more class. Nobody can see an off-by-one there until the game
 * runs, so each rule is pinned here: the slide ends before the typing does, the text is complete before the fade begins, alpha stays in 0..1, the bar never
 * shows full until the quest is, and the panel never leaves the screen.
 */
class OverlayMathTest {
    // ---------------------------------------------------------------- the progress bar
    @Test void oneSegmentPerUnitUpToTwelve() {
        assertEquals(5, OverlayMath.totalSegments(5));
        assertEquals(12, OverlayMath.totalSegments(12));
        assertEquals(12, OverlayMath.totalSegments(40));
        assertEquals(1, OverlayMath.totalSegments(0));
        assertEquals(1, OverlayMath.totalSegments(-9));
        for (int p = 0; p <= 5; p++) assertEquals(p, OverlayMath.litSegments(p, 5), "progress " + p);
    }

    @Test void aLongBarIsProportionalButNeverFullBeforeTheEnd() {
        assertEquals(0, OverlayMath.litSegments(0, 40));
        assertEquals(1, OverlayMath.litSegments(1, 40), "any progress lights at least one segment");
        assertEquals(2, OverlayMath.litSegments(7, 40));
        assertEquals(11, OverlayMath.litSegments(39, 40), "one short of the end is not full");
        assertEquals(12, OverlayMath.litSegments(40, 40));
        assertEquals(12, OverlayMath.litSegments(99, 40), "past the target is full");
        for (int p = 0; p < 40; p++) assertTrue(OverlayMath.litSegments(p, 40) < 12, "progress " + p);
        for (int p = 1; p < 40; p++) assertTrue(OverlayMath.litSegments(p, 40) >= OverlayMath.litSegments(p - 1, 40), "monotone at " + p);
    }

    @Test void aSingleUnitQuestIsEmptyUntilDone() {
        assertEquals(0, OverlayMath.litSegments(0, 1));
        assertEquals(1, OverlayMath.litSegments(1, 1));
    }

    // ---------------------------------------------------------------- the clock
    @Test void aCountdownIsMinutesAndSecondsRoundedUp() {
        assertEquals("5:00", OverlayMath.countdown(6000));
        assertEquals("4:59", OverlayMath.countdown(5980));
        assertEquals("0:01", OverlayMath.countdown(1), "one tick left is still a second");
        assertEquals("0:00", OverlayMath.countdown(0));
        assertEquals("0:00", OverlayMath.countdown(-40));
        assertEquals("2:30", OverlayMath.countdown(3000));
        assertEquals("0:10", OverlayMath.countdown(181), "9.05 seconds shows as 10");
    }

    // ---------------------------------------------------------------- the animation clock
    @Test void theHeaderGlitchesForTwoShortFlickersEveryCycle() {
        assertTrue(OverlayMath.glitch(0f));
        assertTrue(OverlayMath.glitch(0.5f));
        assertFalse(OverlayMath.glitch(0.8f));
        assertFalse(OverlayMath.glitch(1.2f));
        assertTrue(OverlayMath.glitch(1.5f));
        assertFalse(OverlayMath.glitch(2.0f));
        assertFalse(OverlayMath.glitch(75f));
        assertTrue(OverlayMath.glitch(150f), "the next cycle");
        assertTrue(OverlayMath.glitch(300.4f));
        int frames = 0;
        for (int i = 0; i < 1500; i++) if (OverlayMath.glitch(i * 0.1f)) frames++;
        assertTrue(frames > 0 && frames < 60, "glitch time over ten cycles " + frames);
        assertFalse(OverlayMath.glitch(-75f), "a negative clock wraps");
    }

    @Test void theCursorBlinksHalfASecond() {
        assertTrue(OverlayMath.blink(0f));
        assertTrue(OverlayMath.blink(9.9f));
        assertFalse(OverlayMath.blink(10f));
        assertFalse(OverlayMath.blink(19.9f));
        assertTrue(OverlayMath.blink(20f));
    }

    @Test void theScanlinesCreepAndWrap() {
        assertEquals(0, OverlayMath.scanOffset(0f, 4));
        assertEquals(1, OverlayMath.scanOffset(3f, 4));
        assertEquals(3, OverlayMath.scanOffset(9f, 4));
        assertEquals(0, OverlayMath.scanOffset(12f, 4));
        for (float t = -50f; t < 400f; t += 0.7f) {
            int o = OverlayMath.scanOffset(t, 4);
            assertTrue(o >= 0 && o < 4, "t=" + t + " gave " + o);
        }
        assertEquals(0, OverlayMath.scanOffset(100f, 0), "a spacing of 0 is treated as 1");
    }

    // ---------------------------------------------------------------- placement
    @Test void thePanelSitsInTheChosenCornerAndNeverOffScreen() {
        assertEquals(6, OverlayMath.originX(0, 400, 180, 6));
        assertEquals(6, OverlayMath.originX(2, 400, 180, 6));
        assertEquals(400 - 180 - 6, OverlayMath.originX(1, 400, 180, 6));
        assertEquals(400 - 180 - 6, OverlayMath.originX(3, 400, 180, 6));
        assertEquals(6, OverlayMath.originY(0, 300, 90, 6));
        assertEquals(6, OverlayMath.originY(1, 300, 90, 6));
        assertEquals(300 - 90 - 6, OverlayMath.originY(2, 300, 90, 6));
        assertEquals(300 - 90 - 6, OverlayMath.originY(3, 300, 90, 6));
        assertEquals(0, OverlayMath.originX(1, 100, 180, 6), "a panel wider than the screen sits at the left edge, not past it");
        assertEquals(0, OverlayMath.originY(3, 50, 90, 6));
    }

    @Test void theScaleIsClampedToItsRange() {
        assertEquals(1.0f, OverlayMath.clampScale(1.0));
        assertEquals(0.5f, OverlayMath.clampScale(0.01));
        assertEquals(2.5f, OverlayMath.clampScale(9.0), "the top of the range is 2.5 since the mouse resize (it was 2.0)");
        assertEquals(2.0f, OverlayMath.clampScale(2.0), "the old top is still a valid size");
        assertEquals(1.0f, OverlayMath.clampScale(Double.NaN));
    }

    // ---------------------------------------------------------------- colour
    @Test void alphaScalingKeepsTheHue() {
        assertEquals(0x3DE0FF, OverlayMath.scaleAlpha(0xFF3DE0FF, 0.5f) & 0xFFFFFF);
        assertEquals(128, OverlayMath.scaleAlpha(0xFF3DE0FF, 0.5f) >>> 24);
        assertEquals(0, OverlayMath.scaleAlpha(0xFF3DE0FF, 0f) >>> 24);
        assertEquals(255, OverlayMath.scaleAlpha(0xFF3DE0FF, 5f) >>> 24, "clamped above");
        assertEquals(0, OverlayMath.scaleAlpha(0xFF3DE0FF, -5f) >>> 24, "clamped below");
        assertEquals(0xAA, OverlayMath.withAlpha(0x06222E, 0.667f) >>> 24 & 0xFF);
        assertEquals(0x06222E, OverlayMath.withAlpha(0x06222E, 0.667f) & 0xFFFFFF);
    }

    @Test void aNearlyTransparentColourIsNotDrawnBecauseTheGameWouldDrawItOpaque() {
        assertFalse(OverlayMath.drawable(0x03FFFFFF));
        assertFalse(OverlayMath.drawable(0x07FFFFFF));
        assertTrue(OverlayMath.drawable(0x08FFFFFF));
        assertTrue(OverlayMath.drawable(0xFFFFFFFF));
    }

    // ---------------------------------------------------------------- the toast
    @Test void theSlideStartsOutAndEndsInBeforeTheTypingIsDone() {
        assertEquals(0f, ToastTimeline.slide(0f), 1e-6f);
        assertEquals(0f, ToastTimeline.slide(-3f), 1e-6f);
        assertEquals(1f, ToastTimeline.slide(ToastTimeline.SLIDE_TICKS), 1e-6f);
        assertEquals(1f, ToastTimeline.slide(ToastTimeline.LIFE_TICKS), 1e-6f);
        float last = -1f;
        for (float a = 0f; a <= ToastTimeline.SLIDE_TICKS; a += 0.25f) {
            assertTrue(ToastTimeline.slide(a) >= last, "monotone at " + a);
            last = ToastTimeline.slide(a);
        }
        assertTrue(ToastTimeline.slide(ToastTimeline.SLIDE_TICKS / 2) > 0.5f, "an ease-out is past half-way at half the time");
        assertTrue(ToastTimeline.SLIDE_TICKS < ToastTimeline.typedAt(12));
    }

    @Test void theTextIsCompleteBeforeTheFadeBegins() {
        for (int len : new int[] {8, 20, 40, 80}) {
            assertTrue(ToastTimeline.typedAt(len) + 10f < ToastTimeline.LIFE_TICKS - ToastTimeline.FADE_TICKS, "a text of " + len + " characters is typed by " + ToastTimeline.typedAt(len));
        }
    }

    @Test void theTypewriterRevealsMonotonicallyAndNeverPastTheEnd() {
        assertEquals(0, ToastTimeline.visibleChars(0f, 20));
        assertEquals(0, ToastTimeline.visibleChars(ToastTimeline.TYPE_START, 20));
        assertEquals(0, ToastTimeline.visibleChars(-9f, 20));
        int last = 0;
        for (float a = 0f; a < ToastTimeline.LIFE_TICKS; a += 0.5f) {
            int n = ToastTimeline.visibleChars(a, 20);
            assertTrue(n >= last && n <= 20, "age " + a);
            last = n;
        }
        assertEquals(20, ToastTimeline.visibleChars(ToastTimeline.typedAt(20) + 0.01f, 20));
        assertEquals(0, ToastTimeline.visibleChars(50f, 0));
    }

    @Test void alphaStaysInRangeFadesInAndOutAndEndsAtZero() {
        assertEquals(0f, ToastTimeline.alpha(0f), 1e-6f);
        assertEquals(0f, ToastTimeline.alpha(-1f), 1e-6f);
        assertEquals(1f, ToastTimeline.alpha(2f), 1e-6f);
        assertEquals(1f, ToastTimeline.alpha(50f), 1e-6f);
        assertEquals(0f, ToastTimeline.alpha(ToastTimeline.LIFE_TICKS), 1e-6f);
        for (float a = -5f; a < ToastTimeline.LIFE_TICKS + 5f; a += 0.25f) {
            float v = ToastTimeline.alpha(a);
            assertTrue(v >= 0f && v <= 1f, "age " + a + " gave " + v);
        }
        float before = ToastTimeline.alpha(ToastTimeline.LIFE_TICKS - ToastTimeline.FADE_TICKS - 0.01f);
        float mid = ToastTimeline.alpha(ToastTimeline.LIFE_TICKS - ToastTimeline.FADE_TICKS / 2);
        assertEquals(1f, before, 1e-6f);
        assertEquals(0.5f, mid, 1e-3f);
    }

    @Test void aToastIsDoneExactlyWhenItsLifeIsOver() {
        assertFalse(ToastTimeline.done(0f));
        assertFalse(ToastTimeline.done(ToastTimeline.LIFE_TICKS - 0.01f));
        assertTrue(ToastTimeline.done(ToastTimeline.LIFE_TICKS));
        assertEquals(12.5f, ToastTimeline.age(112.5f, 100f), 1e-6f);
    }

    @Test void theCursorBlinksWhileTypingAndStopsAfterward() {
        int total = 20;
        boolean on = false, off = false;
        for (float a = 0f; a < ToastTimeline.typedAt(total); a += 0.5f) {
            if (ToastTimeline.cursor(a, total)) on = true;
            else off = true;
        }
        assertTrue(on && off, "it blinks");
        assertFalse(ToastTimeline.cursor(ToastTimeline.typedAt(total) + 30f, total));
        assertFalse(ToastTimeline.cursor(ToastTimeline.LIFE_TICKS, total));
    }
}
