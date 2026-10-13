package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** When the Dungeon looks at a player's room, and what the look turns into. */
class RoomGateTest {
    private static final long LONG_AGO = Long.MIN_VALUE / 2;

    private static RoomGate.Decision decide(long room, long handled, long now, long nextTry, boolean births, long lastBirth, boolean suppressed, int near, int cap) {
        return RoomGate.decide(room, handled, now, nextTry, births, lastBirth, () -> suppressed, () -> near, cap);
    }

    @Test
    void aRoomThatWasDealtWithIsNotLookedAtAgain() {
        assertEquals(RoomGate.Verdict.SKIP, decide(5, 5, 1000, 0, true, LONG_AGO, false, 0, 28).verdict());
    }

    @Test
    void whileTheWaitLastsNothingIsAsked() {
        AtomicInteger asked = new AtomicInteger();
        RoomGate.Decision d = RoomGate.decide(6, 5, 1000, 1500, true, LONG_AGO, () -> { asked.incrementAndGet(); return false; }, () -> { asked.incrementAndGet(); return 0; }, 28);
        assertEquals(RoomGate.Verdict.SKIP, d.verdict());
        assertEquals(0, asked.get());
    }

    @Test
    void aFloorWithNoBirthsCallsTheRoomDealtWith() {
        assertEquals(RoomGate.Verdict.HANDLED, decide(6, 5, 1000, 0, false, LONG_AGO, false, 0, 28).verdict());
    }

    @Test
    void aPlayerWhoDrewABirthAMomentAgoWaitsOutTheGap() {
        RoomGate.Decision d = decide(6, 5, 1100, 0, true, 1000, false, 0, 28);
        assertEquals(RoomGate.Verdict.WAIT, d.verdict());
        assertEquals(1200, d.nextTry(), "200 ticks after the last birth");
        assertEquals(RoomGate.Verdict.ENTER, decide(6, 5, 1200, 0, true, 1000, false, 0, 28).verdict(), "the gap is over");
        assertEquals(200, RoomGate.GAP);
    }

    @Test
    void aCollapseUnderWayCallsTheRoomDealtWith() {
        assertEquals(RoomGate.Verdict.HANDLED, decide(6, 5, 5000, 0, true, LONG_AGO, true, 0, 28).verdict());
    }

    @Test
    void tooManyBornMobsAboutMakeTheWallsWait() {
        RoomGate.Decision d = decide(6, 5, 5000, 0, true, LONG_AGO, false, 28, 28);
        assertEquals(RoomGate.Verdict.WAIT, d.verdict());
        assertEquals(5040, d.nextTry());
        assertEquals(RoomGate.Verdict.ENTER, decide(6, 5, 5000, 0, true, LONG_AGO, false, 27, 28).verdict());
    }

    @Test
    void theCostlyQuestionsAreAskedOnlyWhenTheCheapOnesLeaveTheRoomStanding() {
        AtomicInteger asked = new AtomicInteger();
        RoomGate.decide(6, 5, 1100, 0, true, 1000, () -> { asked.incrementAndGet(); return false; }, () -> { asked.incrementAndGet(); return 0; }, 28);
        assertEquals(0, asked.get(), "inside the gap");
        RoomGate.decide(6, 5, 5000, 0, true, LONG_AGO, () -> { asked.incrementAndGet(); return true; }, () -> { asked.incrementAndGet(); return 0; }, 28);
        assertEquals(1, asked.get(), "a collapse ends it before the count");
        RoomGate.decide(6, 5, 5000, 0, true, LONG_AGO, () -> { asked.incrementAndGet(); return false; }, () -> { asked.incrementAndGet(); return 0; }, 28);
        assertEquals(3, asked.get());
    }

    @Test
    void aBirthMakesTheRoomDealtWithAndStartsTheGap() {
        RoomGate.After a = RoomGate.after(RoomGate.Result.BIRTHED, 0, 7000);
        assertTrue(a.handled() && a.birthed());
        assertEquals(0, a.nextTry());
    }

    @Test
    void aRoomWhoseChunkIsNotLoadedIsLookedAtAgainSoon() {
        RoomGate.After a = RoomGate.after(RoomGate.Result.LATER, 0, 7000);
        assertFalse(a.handled() || a.birthed());
        assertEquals(7040, a.nextTry());
    }

    @Test
    void aPopulatedRoomWithNoWallTheRightDistanceAwayIsTriedThreeTimesAndThenLeft() {
        long now = 9000;
        int tries = 0;
        RoomGate.After a = RoomGate.after(RoomGate.Result.NOWHERE, tries, now);
        assertFalse(a.handled());
        assertEquals(now + 60, a.nextTry());
        tries = a.tries();
        a = RoomGate.after(RoomGate.Result.NOWHERE, tries, now + 60);
        assertFalse(a.handled());
        tries = a.tries();
        a = RoomGate.after(RoomGate.Result.NOWHERE, tries, now + 120);
        assertTrue(a.handled(), "the third look is the last");
        assertEquals(3, a.tries());
        assertFalse(a.birthed());
    }

    @Test
    void aBarrenOrAlreadyBornRoomIsDealtWith() {
        RoomGate.After a = RoomGate.after(RoomGate.Result.DONE, 1, 100);
        assertTrue(a.handled());
        assertFalse(a.birthed());
        assertEquals(0, a.nextTry());
    }

    @Test
    void aRoomBirthsAgainOnlyWhenTheTimeHasPassedAndNoneOfItsMobsLives() {
        long born = 10_000;
        int thirty = 30;
        assertFalse(RoomGate.rebirthDue(born, thirty, born + 30 * 1200 - 1, () -> 0), "not yet");
        assertTrue(RoomGate.rebirthDue(born, thirty, born + 30 * 1200, () -> 0));
        assertFalse(RoomGate.rebirthDue(born, thirty, born + 40 * 1200, () -> 1), "one of its mobs still lives");
        assertFalse(RoomGate.rebirthDue(born, 0, born + 1_000_000, () -> 0), "0 minutes: rooms never refill");
        AtomicInteger asked = new AtomicInteger();
        RoomGate.rebirthDue(born, thirty, born + 5, () -> { asked.incrementAndGet(); return 0; });
        RoomGate.rebirthDue(born, 0, born + 1_000_000, () -> { asked.incrementAndGet(); return 0; });
        assertEquals(0, asked.get(), "the mobs are not counted when the time has not passed or the setting is off");
    }
}
