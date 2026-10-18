package com.newuniverse.nusmp.compat.maps;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The no-spam rules of the automatic waypoint lines. */
class ShareLimiterTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a"), B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void atMostMaxPerWindowThenAgainAfterTheWindow() {
        ShareLimiter l = new ShareLimiter(3, 1200);
        assertTrue(l.tryAcquire(A, 0));
        assertTrue(l.tryAcquire(A, 10));
        assertTrue(l.tryAcquire(A, 20));
        assertFalse(l.tryAcquire(A, 30), "the fourth inside the minute is refused");
        assertFalse(l.wouldAllow(A, 30));
        assertFalse(l.tryAcquire(A, 1199));
        assertTrue(l.tryAcquire(A, 1200), "the first one has left the window");
        assertTrue(l.wouldAllow(A, 1210));
    }

    @Test
    void playersAreIndependent() {
        ShareLimiter l = new ShareLimiter(1, 100);
        assertTrue(l.tryAcquire(A, 0));
        assertFalse(l.tryAcquire(A, 1));
        assertTrue(l.tryAcquire(B, 1));
    }

    @Test
    void aRefusalTakesNothing() {
        ShareLimiter l = new ShareLimiter(1, 100);
        assertTrue(l.tryAcquire(A, 0));
        for (int i = 1; i < 50; i++) assertFalse(l.tryAcquire(A, i));
        assertTrue(l.tryAcquire(A, 100), "refusals did not extend the window");
    }

    @Test
    void aClockThatWentBackwardsStartsOver() {
        ShareLimiter l = new ShareLimiter(1, 100);
        assertTrue(l.tryAcquire(A, 5000));
        assertFalse(l.tryAcquire(A, 5001));
        assertTrue(l.tryAcquire(A, 10), "another world with a lower game time");
    }

    @Test
    void rememberTellsAPlaceOnceAndKeepsTheListShort() {
        List<String> known = new ArrayList<>();
        assertTrue(ShareLimiter.remember(known, "gate:a", 3));
        assertFalse(ShareLimiter.remember(known, "gate:a", 3));
        assertTrue(ShareLimiter.remember(known, "b", 3));
        assertTrue(ShareLimiter.remember(known, "c", 3));
        assertTrue(ShareLimiter.remember(known, "d", 3));
        assertEquals(List.of("b", "c", "d"), known, "the oldest is dropped");
        assertFalse(ShareLimiter.remember(known, null, 3));
    }

    @Test
    void zeroOrNegativeSettingsAreClamped() {
        ShareLimiter l = new ShareLimiter(0, 0);
        assertTrue(l.tryAcquire(A, 0));
        assertFalse(l.tryAcquire(A, 0));
    }
}
