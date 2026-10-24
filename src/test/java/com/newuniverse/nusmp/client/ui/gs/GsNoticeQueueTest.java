package com.newuniverse.nusmp.client.ui.gs;

import com.newuniverse.nusmp.client.ui.gs.GsNoticeQueue.Kind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: a spamming server or a long fight must never build a pile of notices: 3 visible, 16 queued, 4 new per second, duplicates refused. */
class GsNoticeQueueTest {
    @Test
    void theLimitsAreTheOnesTheBudgetStates() {
        assertEquals(3, GsNoticeQueue.VISIBLE);
        assertEquals(16, GsNoticeQueue.QUEUED);
        assertEquals(4, GsNoticeQueue.PER_SECOND);
    }

    @Test
    void atMostThreeAreVisibleTheRestWait() {
        GsNoticeQueue q = new GsNoticeQueue();
        float t = 0f;
        for (int i = 0; i < 3; i++) { assertTrue(q.offer("k" + i, "text " + i, Kind.INFO, t)); t += 6f; }
        assertEquals(3, q.visibleCount());
        assertTrue(q.offer("k3", "text 3", Kind.INFO, t));
        assertEquals(3, q.visibleCount());
        assertEquals(1, q.queuedCount());
    }

    @Test
    void aWaitingNoticeShowsWhenASlotFrees() {
        GsNoticeQueue q = new GsNoticeQueue();
        q.offer("a", "a", Kind.INFO, 0f);
        q.offer("b", "b", Kind.INFO, 6f);
        q.offer("c", "c", Kind.INFO, 12f);
        q.offer("d", "d", Kind.INFO, 100f);
        q.update(100f);
        assertEquals(3, q.visibleCount());
        assertEquals(1, q.queuedCount(), "a, b and c are still alive at tick 100, d waits");
        q.update(GsNoticeQueue.LIFE + 1f);                                            // a has run out
        assertEquals(0, q.queuedCount());
        assertEquals(3, q.visibleCount());
        assertNotNull(q.slot(0));
        assertEquals("d", q.slot(0).text);
        assertEquals(GsNoticeQueue.LIFE + 1f, q.slot(0).shownAt);
    }

    @Test
    void everythingExpiresAndTheQueueEmpties() {
        GsNoticeQueue q = new GsNoticeQueue();
        q.offer("a", "a", Kind.OK, 0f);
        assertFalse(q.empty());
        q.update(GsNoticeQueue.LIFE);
        assertTrue(q.empty());
        assertNull(q.slot(0));
    }

    @Test
    void theQueueNeverHoldsMoreThanSixteenAndDropsTheOldestWaiting() {
        GsNoticeQueue q = new GsNoticeQueue();
        float t = 0f;
        int accepted = 0;
        for (int i = 0; i < 200; i++) {
            if (q.offer("key" + i, "text " + i, Kind.INFO, t)) accepted++;
            t += 5.5f;                                                                // four per second is the most that gets in; update() is not called, so nothing expires
        }
        assertTrue(q.visibleCount() <= GsNoticeQueue.VISIBLE);
        assertTrue(q.queuedCount() <= GsNoticeQueue.QUEUED, "queued " + q.queuedCount());
        assertEquals(GsNoticeQueue.QUEUED, q.queuedCount());
        assertTrue(q.dropped() > 0);
        assertEquals(accepted, q.visibleCount() + q.queuedCount() + q.dropped());
    }

    @Test
    void atMostFourNewNoticesAreAcceptedPerSecond() {
        GsNoticeQueue q = new GsNoticeQueue();
        int ok = 0;
        for (int i = 0; i < 40; i++) if (q.offer("k" + i, "t" + i, Kind.INFO, i * 0.4f)) ok++;      // 40 offers inside 16 ticks
        assertEquals(4, ok);
        assertEquals(36, q.refused());
        assertTrue(q.offer("later", "later", Kind.INFO, 20.5f), "a second later the window has moved on");
    }

    @Test
    void theRateWindowSlides() {
        GsNoticeQueue q = new GsNoticeQueue();
        int ok = 0;
        for (int i = 0; i < 100; i++) if (q.offer("k" + i, "t", Kind.INFO, i * 2f)) ok++;          // one offer every 2 ticks for 200 ticks = 10 s
        assertTrue(ok <= 4 * 10 + 4, "accepted " + ok);
        assertTrue(ok >= 4 * 9, "the limit must not block more than needed: accepted " + ok);
    }

    @Test
    void theSameKeyIsRefusedInsideTheDedupeWindowOnly() {
        GsNoticeQueue q = new GsNoticeQueue();
        assertTrue(q.offer("page:x", "first", Kind.OK, 0f));
        assertFalse(q.offer("page:x", "again", Kind.OK, 10f));
        assertFalse(q.offer("page:x", "again", Kind.OK, GsNoticeQueue.DEDUPE - 1f));
        assertTrue(q.offer("page:x", "much later", Kind.OK, GsNoticeQueue.DEDUPE + 1f));
        assertTrue(q.offer("page:y", "other key", Kind.OK, 12f));
    }

    @Test
    void blankTextIsRefusedAndLongTextIsCut() {
        GsNoticeQueue q = new GsNoticeQueue();
        assertFalse(q.offer("k", "   ", Kind.INFO, 0f));
        assertFalse(q.offer("k", null, Kind.INFO, 0f));
        assertTrue(q.offer(null, "x".repeat(500), Kind.WARN, 0f));
        assertEquals(GsNoticeQueue.MAX_TEXT, q.slot(0).text.length());
        assertEquals(Kind.WARN, q.slot(0).kind);
        assertTrue(q.offer("k2", "ok", null, 6f));
        assertEquals(Kind.INFO, q.slot(1).kind, "a missing kind is INFO");
    }

    @Test
    void clearForgetsEverythingIncludingTheDedupeAndRateMemory() {
        GsNoticeQueue q = new GsNoticeQueue();
        for (int i = 0; i < 4; i++) q.offer("k" + i, "t", Kind.INFO, i);
        q.clear();
        assertTrue(q.empty());
        assertTrue(q.offer("k0", "t", Kind.INFO, 5f), "after a logout the same key and the rate window start fresh");
    }

    @Test
    void theLookOfOneNoticeEasesInAndFadesOut() {
        assertEquals(0f, GsNoticeQueue.slide(0f), 1e-6);
        assertEquals(1f, GsNoticeQueue.slide(GsNoticeQueue.SLIDE_IN), 1e-6);
        assertEquals(1f, GsNoticeQueue.slide(500f), 1e-6);
        assertEquals(0f, GsNoticeQueue.alpha(0f), 1e-6);
        assertEquals(1f, GsNoticeQueue.alpha(GsNoticeQueue.LIFE / 2f), 1e-6);
        assertEquals(0f, GsNoticeQueue.alpha(GsNoticeQueue.LIFE), 1e-6);
        float last = 0f;
        for (float a = 0f; a <= GsNoticeQueue.SLIDE_IN; a += 0.5f) { float s = GsNoticeQueue.slide(a); assertTrue(s >= last); last = s; }
    }
}
