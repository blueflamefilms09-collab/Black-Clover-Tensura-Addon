package com.newuniverse.nusmp.client.ui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The page-unlock toast's timeline and the tracker that decides when to show it. */
class ToastTimelineTest {
    @Test
    void theToastSlidesInHoldsAndLeaves() {
        assertEquals(0f, ToastTimeline.slide(0f), 1e-6f);
        assertEquals(1f, ToastTimeline.slide(ToastTimeline.IN), 1e-6f);
        assertEquals(0f, ToastTimeline.leave(ToastTimeline.IN + ToastTimeline.HOLD), 1e-6f);
        assertEquals(1f, ToastTimeline.leave(ToastTimeline.TOTAL), 1e-6f);
        assertEquals(0f, ToastTimeline.alpha(0f), 1e-6f, "invisible at the start");
        assertEquals(1f, ToastTimeline.alpha(ToastTimeline.IN + 5f), 1e-6f, "fully visible while it holds");
        assertEquals(0f, ToastTimeline.alpha(ToastTimeline.TOTAL), 1e-6f, "gone at the end");
        assertFalse(ToastTimeline.done(ToastTimeline.TOTAL - 1f));
        assertTrue(ToastTimeline.done(ToastTimeline.TOTAL));
    }

    @Test
    void alphaOnlyEverFallsAfterItPeaks() {
        float prev = ToastTimeline.alpha(ToastTimeline.IN);
        for (float a = ToastTimeline.IN; a <= ToastTimeline.TOTAL; a += 0.5f) {
            float v = ToastTimeline.alpha(a);
            assertTrue(v <= prev + 1e-6f, "alpha at " + a);
            prev = v;
        }
    }

    @Test
    void theCircleBuildsOverAndSettlesAtOne() {
        assertEquals(0f, ToastTimeline.build(0f), 1e-6f);
        assertEquals(1f, ToastTimeline.build(ToastTimeline.BUILD), 1e-5f);
        assertEquals(1f, ToastTimeline.build(500f), 1e-5f);
        assertEquals(0f, ToastTimeline.textAge(3f), 0f, "the text waits until the panel is mostly in");
        assertEquals(10f, ToastTimeline.textAge(ToastTimeline.TEXT_START + 10f), 1e-5f);
    }

    @Test
    void joiningAWorldAnnouncesNothing() {
        PageUnlockTracker t = new PageUnlockTracker();
        assertTrue(t.update("Red|FIRE", java.util.List.of("a", "b", "c")).isEmpty(), "the first snapshot is the baseline");
        assertTrue(t.update("Red|FIRE", java.util.List.of("a", "b", "c")).isEmpty(), "nothing changed");
    }

    @Test
    void aNewPageIsAnnouncedOnceInOrder() {
        PageUnlockTracker t = new PageUnlockTracker();
        t.update("Red|FIRE", java.util.List.of("a"));
        assertEquals(java.util.List.of("b", "c"), t.update("Red|FIRE", java.util.List.of("a", "b", "c")));
        assertTrue(t.update("Red|FIRE", java.util.List.of("a", "b", "c")).isEmpty(), "not announced twice");
        assertEquals(java.util.List.of("d"), t.update("Red|FIRE", java.util.List.of("a", "b", "c", "d")));
    }

    @Test
    void aDifferentBookOrALogoutStartsOver() {
        PageUnlockTracker t = new PageUnlockTracker();
        t.update("Red|FIRE", java.util.List.of("a"));
        assertTrue(t.update("Blue|WATER", java.util.List.of("x", "y")).isEmpty(), "a different grimoire is a new baseline, not an unlock");
        assertEquals(java.util.List.of("z"), t.update("Blue|WATER", java.util.List.of("x", "y", "z")));
        t.reset();
        assertTrue(t.update("Blue|WATER", java.util.List.of("x", "y", "z", "w")).isEmpty(), "after a reset the first snapshot is a baseline again");
    }

    @Test
    void noGrimoireYetPrimesNothing() {
        PageUnlockTracker t = new PageUnlockTracker();
        assertTrue(t.update("", java.util.List.of()).isEmpty());
        assertTrue(t.update("|", java.util.List.of("a")).isEmpty(), "the first book is a baseline (its starter page is not a toast)");
        assertEquals(java.util.List.of("b"), t.update("|", java.util.List.of("a", "b")));
    }
}
