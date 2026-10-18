package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: four techniques of the Mirror depend on where the panes stand, so the bookkeeping is proved exactly: placing, expiry, the two-pane limit with eviction of the oldest, and the nearest lookup. */
class PaneBookTest {
    private static PaneBook.Pane put(PaneBook b, long now, double x) {
        List<PaneBook.Pane> out = b.makeRoom(now);
        for (PaneBook.Pane p : out) assertTrue(p.broken, "an evicted pane is marked broken");
        return b.place(now, x, 0, 0, 0, 0, 1, 1.7, 120, 3);
    }

    @Test
    void aPaneStandsUntilItsTimeIsUp() {
        PaneBook b = new PaneBook(2);
        PaneBook.Pane p = put(b, 100, 0);
        assertTrue(p.standing(100));
        assertTrue(p.standing(219));
        assertFalse(p.standing(220));
        assertEquals(1, b.live(150).size());
        assertEquals(0, b.live(220).size());
    }

    @Test
    void theThirdPaneEvictsTheOldestAndReturnsItForShattering() {
        PaneBook b = new PaneBook(2);
        PaneBook.Pane first = put(b, 100, 0);
        PaneBook.Pane second = put(b, 101, 10);
        List<PaneBook.Pane> evicted = b.makeRoom(102);
        assertEquals(1, evicted.size());
        assertSame(first, evicted.get(0));
        assertTrue(first.broken);
        assertFalse(second.broken);
        b.place(102, 20, 0, 0, 0, 0, 1, 1.7, 120, 3);
        assertEquals(2, b.live(103).size());
    }

    @Test
    void expiredPanesAreForgottenWithoutBeingReturned() {
        PaneBook b = new PaneBook(2);
        put(b, 0, 0);
        put(b, 1, 5);
        assertTrue(b.makeRoom(500).isEmpty(), "both timed out: nothing to shatter, nothing to evict");
        assertEquals(0, b.size(500));
    }

    @Test
    void aBrokenPaneNoLongerCounts() {
        PaneBook b = new PaneBook(2);
        PaneBook.Pane p = put(b, 0, 0);
        p.broken = true;
        assertEquals(0, b.live(10).size());
        assertNull(b.nearest(0, 0, 0, 10, 5.0));
    }

    @Test
    void theNearestPaneIsFoundWithinTheDistanceOnly() {
        PaneBook b = new PaneBook(2);
        PaneBook.Pane a = put(b, 0, 0);
        PaneBook.Pane c = put(b, 1, 10);
        assertSame(a, b.nearest(2, 0, 0, 5, 5.0));
        assertSame(c, b.nearest(9, 0, 0, 5, 5.0));
        assertNull(b.nearest(50, 0, 0, 5, 5.0));
    }

    @Test
    void theNormalIsStoredAsAUnitVector() {
        PaneBook b = new PaneBook(1);
        PaneBook.Pane p = b.place(0, 0, 0, 0, 0, 0, 5, 1.7, 10, 1);
        assertEquals(1.0, p.nz, 1e-9);
        PaneBook.Pane q = b.place(0, 0, 0, 0, 0, 0, 0, 1.7, 10, 1);
        assertEquals(1.0, q.nz, 1e-9, "a zero normal falls back to facing +z instead of dividing by zero");
    }

    @Test
    void clearForgetsEverything() {
        PaneBook b = new PaneBook(2);
        put(b, 0, 0);
        b.clear();
        assertEquals(0, b.size(1));
    }
}
