package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the traits remember players (marks, the Karma mark, the steam and lock gaps, the Balance's window). On a public server that memory must be bounded and must expire on its own, so
 * these tests drive {@link TierState} by hand with game ticks and check the caps, the expiry and the windows.
 */
class TierStateTest {
    private static UUID id(int i) { return UUID.nameUUIDFromBytes(("player-" + i).getBytes()); }

    @Test
    void marksCountUpToTheCapAndExpireAfterTheirTime() {
        TierState st = new TierState();
        UUID a = id(1);
        assertEquals(0, st.marks(a, 0));
        assertEquals(1, st.addMark(a, 100));
        assertEquals(2, st.addMark(a, 110));
        assertEquals(3, st.addMark(a, 120));
        assertEquals(TierTrait.MARK_MAX, st.addMark(a, 130), "a fourth mark does not go over the cap");
        assertEquals(TierTrait.MARK_MAX, st.marks(a, 130 + TierTrait.MARK_TICKS - 1));
        assertEquals(0, st.marks(a, 130 + TierTrait.MARK_TICKS), "the marks run out MARK_TICKS after the last one");
        assertEquals(1, st.addMark(a, 130 + TierTrait.MARK_TICKS + 5), "an expired set starts again from one");
    }

    @Test
    void aFiredMarkIsCleared() {
        TierState st = new TierState();
        st.addMark(id(1), 10);
        st.clearMarks(id(1));
        assertEquals(0, st.marks(id(1), 11));
    }

    @Test
    void karmaLastsItsTimeAndCanBeCleared() {
        TierState st = new TierState();
        assertFalse(st.karma(id(1), 0));
        st.markKarma(id(1), 50);
        assertTrue(st.karma(id(1), 50 + TierTrait.KARMA_TICKS - 1));
        assertFalse(st.karma(id(1), 50 + TierTrait.KARMA_TICKS));
        st.markKarma(id(2), 50);
        st.clearKarma(id(2));
        assertFalse(st.karma(id(2), 51));
    }

    @Test
    void steamAndLockAreRateLimitedPerPlayer() {
        TierState st = new TierState();
        assertTrue(st.steamReady(id(1), 100));
        assertFalse(st.steamReady(id(1), 100 + TierTrait.STEAM_GAP_TICKS - 1), "second fire inside the gap");
        assertTrue(st.steamReady(id(2), 100 + 1), "another player has their own gap");
        assertTrue(st.steamReady(id(1), 100 + TierTrait.STEAM_GAP_TICKS));
        assertTrue(st.lockReady(id(1), 100));
        assertFalse(st.lockReady(id(1), 100 + TierTrait.LOCK_GAP_TICKS - 1));
        assertTrue(st.lockReady(id(1), 100 + TierTrait.LOCK_GAP_TICKS));
    }

    @Test
    void everyMapIsCappedAtMaxTrackedWhateverTheNumberOfPlayers() {
        TierState st = new TierState();
        for (int i = 0; i < TierTrait.MAX_TRACKED * 5; i++) {
            st.addMark(id(i), 100 + i);
            st.markKarma(id(i), 100 + i);
            st.steamReady(id(i), 100 + i);
            st.lockReady(id(i), 100 + i);
        }
        assertTrue(st.size() <= 4 * TierTrait.MAX_TRACKED, "held " + st.size());
        // the newest players are the ones kept (the oldest entry is the one evicted)
        int newest = TierTrait.MAX_TRACKED * 5 - 1;
        assertEquals(1, st.marks(id(newest), 100 + newest));
        assertTrue(st.karma(id(newest), 100 + newest));
        assertEquals(0, st.marks(id(0), 100 + newest), "the oldest mark was evicted");
    }

    @Test
    void pruneDropsEverythingThatRanOutAndKeepsTheRest() {
        TierState st = new TierState();
        st.addMark(id(1), 0);
        st.markKarma(id(2), 0);
        st.steamReady(id(3), 0);
        st.lockReady(id(4), 0);
        st.markKarma(id(5), 1000);
        st.prune(TierTrait.MARK_TICKS + 1);
        assertEquals(1, st.size(), "only the late karma mark is left");
        assertTrue(st.karma(id(5), TierTrait.MARK_TICKS + 1));
    }

    @Test
    void forgetAndClearEmptyTheMemory() {
        TierState st = new TierState();
        st.addMark(id(1), 0);
        st.markKarma(id(1), 0);
        st.steamReady(id(1), 0);
        st.lockReady(id(1), 0);
        st.openWindow(0, 20);
        st.addWindow(id(1), 5f, 1);
        st.forget(id(1));
        assertEquals(0, st.size());
        st.addMark(id(2), 0);
        st.startMirror(0);
        st.startGuard(0);
        st.startFrenzy(0);
        st.fusionSeen(2);
        st.clear();
        assertEquals(0, st.size());
        assertFalse(st.mirror(1));
        assertFalse(st.guard(1));
        assertFalse(st.frenzy(1));
        assertEquals(0, st.fusionSeen());
        assertFalse(st.windowOpen(1));
    }

    @Test
    void stancesRunForTheirTimeAndNeverShrinkWhenStartedAgain() {
        TierState st = new TierState();
        st.startMirror(100);
        assertTrue(st.mirror(100 + TierTrait.MIRROR_TICKS - 1));
        assertFalse(st.mirror(100 + TierTrait.MIRROR_TICKS));
        st.startMirror(150);
        st.startMirror(120);
        assertTrue(st.mirror(150 + TierTrait.MIRROR_TICKS - 1), "an earlier start never shortens a running stance");
        st.startGuard(0);
        assertTrue(st.guard(TierTrait.UNTOUCHABLE_TICKS - 1));
        assertFalse(st.guard(TierTrait.UNTOUCHABLE_TICKS));
        st.startFrenzy(0);
        assertTrue(st.frenzy(TierTrait.FRENZY_TICKS - 1));
        assertFalse(st.frenzy(TierTrait.FRENZY_TICKS));
    }

    @Test
    void theBalanceWindowOpensOnceCountsOnlyInsideAndNamesTheTopDealer() {
        TierState st = new TierState();
        st.addWindow(id(1), 10f, 5);
        assertTrue(st.closeWindow().empty(), "damage outside the window is not counted");
        st.openWindow(100, 30);
        st.openWindow(110, 30);                                                // a second telegraph of the same wind-up does not restart it
        assertTrue(st.windowOpen(100 + 30 + TierTrait.BALANCE_SLACK_TICKS - 1));
        assertFalse(st.windowOpen(100 + 30 + TierTrait.BALANCE_SLACK_TICKS));
        st.addWindow(id(1), 12f, 120);
        st.addWindow(id(2), 4f, 125);
        st.addWindow(id(1), 4f, 126);
        st.addWindow(id(3), 99f, 100 + 30 + TierTrait.BALANCE_SLACK_TICKS + 1);   // too late
        TierState.Window w = st.closeWindow();
        assertEquals(id(1), w.top());
        assertEquals(16f, w.topDamage(), 1e-4);
        assertEquals(20f, w.total(), 1e-4);
        assertEquals(2, w.fighters());
        assertEquals(0.8f, w.share(), 1e-4);
        assertFalse(st.windowOpen(121), "closing the window shuts it");
        assertTrue(st.closeWindow().empty());
        assertNull(st.closeWindow().top());
    }

    @Test
    void theWindowKeepsAtMostMaxTrackedPlayers() {
        TierState st = new TierState();
        st.openWindow(0, 100);
        for (int i = 0; i < TierTrait.MAX_TRACKED * 3; i++) st.addWindow(id(i), 1f, 1);
        assertEquals(TierTrait.MAX_TRACKED, st.closeWindow().fighters());
    }

    @Test
    void aSecondWindowStartsClean() {
        TierState st = new TierState();
        st.openWindow(0, 20);
        st.addWindow(id(1), 50f, 1);
        // the move was interrupted: the window runs out by itself and the next one must not inherit its damage
        st.openWindow(500, 20);
        st.addWindow(id(2), 3f, 501);
        TierState.Window w = st.closeWindow();
        assertEquals(1, w.fighters());
        assertEquals(id(2), w.top());
        assertEquals(3f, w.total(), 1e-4);
    }

    @Test
    void fusionSeenNeverGoesNegative() {
        TierState st = new TierState();
        st.fusionSeen(-3);
        assertEquals(0, st.fusionSeen());
        st.fusionSeen(2);
        assertEquals(2, st.fusionSeen());
    }
}
