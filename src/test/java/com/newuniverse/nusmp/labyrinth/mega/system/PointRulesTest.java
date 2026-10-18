package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the stat screen sends REQUESTS from the client and the server must never trust them. These tests pin the rules that make that safe: an allocation only
 * moves points between the free pool and a stat (the total of the two never changes, whatever number a hostile packet carries), never past the cap, never more
 * than the pool holds, a negative request (taking a point back) works only when respec is allowed, and the reset gives every placed point back. A randomised
 * walk of thousands of requests checks the conservation and the cap after each one.
 */
class PointRulesTest {
    private static SystemState with(int free, int... pts) {
        SystemState s = new SystemState();
        s.setFree(free);
        for (int i = 0; i < pts.length; i++) s.setPoints(Stat.values()[i], pts[i]);
        return s;
    }

    private static int total(SystemState s) { return s.totalPoints() + s.free(); }

    @Test void aLegalRequestMovesPointsFromThePoolIntoTheStat() {
        SystemState s = with(10);
        assertEquals(3, PointRules.allocate(s, Stat.STR, 3, 30, true));
        assertEquals(3, s.points(Stat.STR));
        assertEquals(7, s.free());
    }

    @Test void aRequestIsCutToWhatThePoolHolds() {
        SystemState s = with(4);
        assertEquals(4, PointRules.allocate(s, Stat.AGI, 100, 30, true));
        assertEquals(0, s.free());
        assertEquals(0, PointRules.allocate(s, Stat.AGI, 1, 30, true), "an empty pool gives nothing");
    }

    @Test void aRequestIsCutToTheRoomUnderTheCapAndTheRestStaysInThePool() {
        SystemState s = with(50, 0, 0, 27);
        assertEquals(3, PointRules.allocate(s, Stat.VIT, 40, 30, true));
        assertEquals(30, s.points(Stat.VIT));
        assertEquals(47, s.free());
        assertEquals(0, PointRules.allocate(s, Stat.VIT, 1, 30, true), "at the cap nothing more goes in");
    }

    @Test void aCapLoweredSinceNeverTakesPointsAlreadyPlacedAndAddsNothing() {
        SystemState s = with(5, 25);
        assertEquals(0, PointRules.allocate(s, Stat.STR, 1, 10, true));
        assertEquals(25, s.points(Stat.STR), "points already placed are left alone");
        assertEquals(5, s.free());
    }

    @Test void hostileAmountsMoveNothing() {
        SystemState s = with(20, 5);
        for (int bad : new int[]{0, PointRules.MAX_AMOUNT + 1, -PointRules.MAX_AMOUNT - 1, Integer.MAX_VALUE, Integer.MIN_VALUE, 99999}) {
            assertEquals(0, PointRules.allocate(s, Stat.STR, bad, 30, true), "amount " + bad);
        }
        assertEquals(5, s.points(Stat.STR));
        assertEquals(20, s.free());
        assertEquals(0, PointRules.allocate(s, null, 3, 30, true), "no stat");
        assertEquals(0, PointRules.allocate(null, Stat.STR, 3, 30, true), "no state");
    }

    @Test void takingPointsBackNeedsRespecAndNeverTakesMoreThanThereAre() {
        SystemState s = with(0, 6);
        assertEquals(0, PointRules.allocate(s, Stat.STR, -2, 30, false), "respec off");
        assertEquals(6, s.points(Stat.STR));
        assertEquals(-2, PointRules.allocate(s, Stat.STR, -2, 30, true));
        assertEquals(4, s.points(Stat.STR));
        assertEquals(2, s.free());
        assertEquals(-4, PointRules.allocate(s, Stat.STR, -1000, 30, true), "only what is placed comes back");
        assertEquals(0, s.points(Stat.STR));
        assertEquals(6, s.free());
        assertEquals(0, PointRules.allocate(s, Stat.STR, -1, 30, true), "nothing placed, nothing to take");
    }

    @Test void resetGivesEveryPlacedPointBackOnlyWhenAllowed() {
        SystemState s = with(3, 4, 5, 6, 7, 8);
        assertEquals(0, PointRules.reset(s, false));
        assertEquals(30, s.totalPoints());
        assertEquals(30, PointRules.reset(s, true));
        assertEquals(0, s.totalPoints());
        assertEquals(33, s.free());
        assertEquals(0, PointRules.reset(s, true), "a second reset moves nothing");
        assertEquals(0, PointRules.reset(null, true));
    }

    @Test void thePoolHasACeilingAndAResetNeverDestroysAPoint() {
        SystemState s = with(SystemState.FREE_MAX - 3, 10, 0, 0, 0, 0);
        int before = total(s);
        assertEquals(3, PointRules.reset(s, true));
        assertEquals(SystemState.FREE_MAX, s.free());
        assertEquals(7, s.points(Stat.STR), "the points that did not fit stay placed");
        assertEquals(before, total(s));
    }

    @Test void roomCountsWhatTheStatsCanStillTakeMinusWhatThePoolAlreadyHolds() {
        assertEquals(150, PointRules.room(new int[5], 0, 30));
        assertEquals(140, PointRules.room(new int[5], 10, 30));
        assertEquals(0, PointRules.room(new int[]{30, 30, 30, 30, 30}, 0, 30), "all full: no room");
        assertEquals(0, PointRules.room(new int[]{29, 30, 30, 30, 30}, 1, 30), "one open slot, already in the pool");
        assertEquals(1, PointRules.room(new int[]{29, 30, 30, 30, 30}, 0, 30));
        assertEquals(120, PointRules.room(new int[]{50, 0, 0, 0, 0}, 0, 30), "a stat above a lowered cap counts as full, not as negative room");
        assertEquals(0, PointRules.room(new int[5], 999, 30));
    }

    @Test void maxAddableIsTheSmallerOfThePoolAndTheRoom() {
        SystemState s = with(8, 25);
        assertEquals(5, PointRules.maxAddable(s, Stat.STR, 30));
        assertEquals(8, PointRules.maxAddable(s, Stat.AGI, 30));
        assertEquals(0, PointRules.maxAddable(s, Stat.STR, 20), "over a lowered cap");
        assertEquals(0, PointRules.maxAddable(with(0), Stat.STR, 30));
    }

    @Test void anyWalkOfRandomRequestsConservesPointsAndKeepsTheCap() {
        Random rnd = new Random(42);
        SystemState s = with(60, 3, 0, 9, 0, 1);
        int sum = total(s);
        int cap = 20;
        for (int i = 0; i < 20000; i++) {
            int kind = rnd.nextInt(10);
            boolean respec = rnd.nextBoolean();
            if (kind == 0) PointRules.reset(s, respec);
            else PointRules.allocate(s, Stat.values()[rnd.nextInt(Stat.COUNT)], rnd.nextInt(2400) - 1200, cap, respec);
            assertEquals(sum, total(s), "the points of the pool and the stats add up after request " + i);
            for (Stat st : Stat.values()) assertTrue(s.points(st) <= Math.max(cap, 9), "cap held for " + st + " after request " + i);
            assertTrue(s.free() >= 0);
        }
    }

    @Test void effectiveCapIsClampedToTheHardCap() {
        assertEquals(30, PointRules.effectiveCap(30));
        assertEquals(SystemState.HARD_CAP, PointRules.effectiveCap(99999));
        assertEquals(0, PointRules.effectiveCap(-5));
    }

    @Test void theModeReadsLeniently() {
        assertEquals(PointMode.CHOOSE, PointMode.of("choose"));
        assertEquals(PointMode.CHOOSE, PointMode.of(null));
        assertEquals(PointMode.CHOOSE, PointMode.of("whatever"));
        assertEquals(PointMode.RANDOM, PointMode.of("random"));
        assertEquals(PointMode.RANDOM, PointMode.of("  RANDOM "));
        assertEquals("random", PointMode.RANDOM.id());
        assertEquals(PointMode.RANDOM, PointMode.ofCode(PointMode.RANDOM.ordinal()));
        assertEquals(PointMode.CHOOSE, PointMode.ofCode(99));
        assertEquals(PointMode.CHOOSE, PointMode.ofCode(-1));
        assertEquals(PointMode.CHOOSE, PointMode.values()[0], "choose is the default and the first entry");
    }
}
