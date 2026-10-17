package com.newuniverse.nusmp.avatar.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The easing curves and the module slide: ends exactly where it should, never shows two pages, can be reversed in flight without a jump. */
class ForgeMotionTest {
    @Test
    void theCurvesStartAtZeroEndAtOneAndStayInRange() {
        for (var f : new java.util.function.DoubleUnaryOperator[]{
                t -> ForgeMotion.easeOutCubic((float) t), t -> ForgeMotion.easeInOutCubic((float) t), t -> ForgeMotion.easeOutBack((float) t)}) {
            assertEquals(0.0, f.applyAsDouble(0.0), 1.0e-6);
            assertEquals(1.0, f.applyAsDouble(1.0), 1.0e-6);
            assertEquals(0.0, f.applyAsDouble(-3.0), 1.0e-6, "below the range is clamped");
            assertEquals(1.0, f.applyAsDouble(9.0), 1.0e-6, "above the range is clamped");
        }
        for (int i = 0; i <= 100; i++) {
            float t = i / 100f;
            assertTrue(ForgeMotion.easeOutCubic(t) <= 1.0f && ForgeMotion.easeOutCubic(t) >= t - 1.0e-6f, "ease out is ahead of linear");
            assertTrue(ForgeMotion.easeInOutCubic(t) >= 0f && ForgeMotion.easeInOutCubic(t) <= 1f);
        }
        assertEquals(0.5f, ForgeMotion.easeInOutCubic(0.5f), 1.0e-6f);
    }

    @Test
    void easeOutCubicNeverGoesBackwards() {
        float prev = -1f;
        for (int i = 0; i <= 200; i++) {
            float v = ForgeMotion.easeOutCubic(i / 200f);
            assertTrue(v >= prev);
            prev = v;
        }
    }

    @Test
    void easeOutBackOvershootsAndComesBack() {
        float max = 0f;
        for (int i = 0; i <= 100; i++) max = Math.max(max, ForgeMotion.easeOutBack(i / 100f));
        assertTrue(max > 1.0f && max < 1.2f, "a small overshoot, was " + max);
    }

    @Test
    void aGlideArrivesExactlyAndNotBefore() {
        ForgeMotion.Glide g = new ForgeMotion.Glide(5f);
        assertTrue(g.done());
        assertEquals(5f, g.value(), 0f);
        g.to(15f, 10f);
        assertFalse(g.done());
        for (int i = 0; i < 9; i++) g.tick(1f);
        assertFalse(g.done());
        assertTrue(g.value() < 15f);
        g.tick(1f);
        assertTrue(g.done());
        assertEquals(15f, g.value(), 0f);
        g.tick(5f);
        assertEquals(15f, g.value(), 0f, "stays put");
    }

    @Test
    void aGlideReversedInFlightContinuesFromWhereItIs() {
        ForgeMotion.Glide g = new ForgeMotion.Glide(0f);
        g.to(10f, 10f);
        g.tick(4f);
        float mid = g.value();
        assertTrue(mid > 0f && mid < 10f);
        g.to(0f, 10f);
        assertEquals(mid, g.value(), 1.0e-5f, "no jump when the target changes");
        g.tick(10f);
        assertEquals(0f, g.value(), 0f);
    }

    @Test
    void aGlideIgnoresNegativeAndZeroTime() {
        ForgeMotion.Glide g = new ForgeMotion.Glide(0f);
        g.to(1f, 0f);                    // a zero length is lifted to one tick
        g.tick(-5f);
        assertFalse(g.done());
        g.tick(0.5f);
        assertFalse(g.done());
        g.tick(0.5f);
        assertTrue(g.done());
    }

    @Test
    void aHoverRisesWhileHoveredFallsAfterwardsAndStaysInRange() {
        ForgeMotion.Hovers h = new ForgeMotion.Hovers();
        assertEquals(0f, h.get("a"), 0f);
        float prev = 0f;
        for (int i = 0; i < 40; i++) {
            float v = h.step("a", true, 1f, 0.3f);
            assertTrue(v >= prev && v <= 1f);
            prev = v;
        }
        assertEquals(1f, h.get("a"), 0f, "snaps to 1 once it is close");
        for (int i = 0; i < 40; i++) {
            float v = h.step("a", false, 1f, 0.3f);
            assertTrue(v <= prev && v >= 0f);
            prev = v;
        }
        assertEquals(0f, h.get("a"), 0f);
        assertEquals(0f, h.step("b", false, 1f, 0.3f), 0f, "an untouched key stays 0");
    }

    @Test
    void aHoverWithFastRateOrBigStepsNeverOvershoots() {
        ForgeMotion.Hovers h = new ForgeMotion.Hovers();
        assertEquals(1f, h.step("a", true, 100f, 0.9f), 0f);
        assertEquals(1f, h.step("a", true, 5f, 5f), 0f, "a rate over 1 is clamped");
        assertEquals(1f, h.step("a", true, -3f, 0.5f), 0f, "negative time changes nothing");
    }

    @Test
    void hoversOfKeysNobodyAsksAboutAnymoreAreForgotten() {
        ForgeMotion.Hovers h = new ForgeMotion.Hovers();
        for (int i = 0; i < 200; i++) h.step("cell" + i, false, 1f, 0.3f);
        assertEquals(200, h.size());
        for (int f = 0; f < 200; f++) h.endFrame();
        assertEquals(0, h.size(), "idle keys at 0 are dropped");
        h.step("kept", true, 50f, 0.5f);
        for (int f = 0; f < 200; f++) { h.step("kept", true, 1f, 0.5f); h.endFrame(); }
        assertEquals(1, h.size(), "a key still in use stays");
        h.clear();
        assertEquals(0, h.size());
    }

    @Test
    void aSlideShowsOnePageAtATimeAndSwitchesOnlyWhenFullyOut() {
        ForgeMotion.ModuleSlide s = new ForgeMotion.ModuleSlide(0);
        assertTrue(s.settled());
        assertEquals(0f, s.offset(), 0f);
        s.request(3);
        assertFalse(s.settled());
        int guard = 0;
        boolean sawSwitch = false;
        float last = 0f;
        while (!s.settled() && guard++ < 200) {
            int before = s.shown();
            s.tick(1f);
            if (s.shown() != before) {
                sawSwitch = true;
                assertEquals(0, before);
                assertEquals(3, s.shown());
                assertEquals(-1f, s.offset(), 1.0e-5f, "the page changes while the module is fully off to the left");
            }
            if (!sawSwitch) assertTrue(s.offset() <= last + 1.0e-6f, "sliding out only moves left");
            last = s.offset();
        }
        assertTrue(sawSwitch);
        assertTrue(s.settled());
        assertEquals(3, s.shown());
        assertEquals(0f, s.offset(), 0f);
        assertTrue(guard < 40, "a slide takes about 18 ticks, not " + guard);
    }

    @Test
    void askingForTheShownPageDoesNothing() {
        ForgeMotion.ModuleSlide s = new ForgeMotion.ModuleSlide(2);
        s.request(2);
        assertTrue(s.settled());
        s.tick(5f);
        assertEquals(2, s.shown());
        assertEquals(0f, s.offset(), 0f);
    }

    @Test
    void changingYourMindWhileTheModuleLeavesBringsItBackWithoutJumping() {
        ForgeMotion.ModuleSlide s = new ForgeMotion.ModuleSlide(1);
        s.request(4);
        for (int i = 0; i < 3; i++) s.tick(1f);
        float mid = s.offset();
        assertTrue(mid < 0f && mid > -1f);
        s.request(1);
        assertEquals(mid, s.offset(), 1.0e-5f, "no jump");
        assertEquals(1, s.shown());
        for (int i = 0; i < 40; i++) s.tick(1f);
        assertTrue(s.settled());
        assertEquals(1, s.shown());
        assertEquals(0f, s.offset(), 0f);
    }

    @Test
    void askingForAnotherPageWhileItSlidesInGoesOutAgainAndLandsOnTheLastRequest() {
        ForgeMotion.ModuleSlide s = new ForgeMotion.ModuleSlide(0);
        s.request(2);
        for (int i = 0; i < 12; i++) s.tick(1f);           // out (7 ticks) and well into the way in
        assertEquals(2, s.shown());
        s.request(4);
        for (int i = 0; i < 80; i++) s.tick(1f);
        assertTrue(s.settled());
        assertEquals(4, s.shown());
        assertEquals(4, s.wanted());
    }

    @Test
    void manyQuickRequestsEndOnTheLastOne() {
        ForgeMotion.ModuleSlide s = new ForgeMotion.ModuleSlide(0);
        int[] seq = {1, 2, 3, 4, 0, 2, 3, 1};
        for (int p : seq) { s.request(p); s.tick(1f); }
        for (int i = 0; i < 100; i++) s.tick(1f);
        assertEquals(1, s.shown());
        assertTrue(s.settled());
    }
}
