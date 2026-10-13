package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Spatial Surge: its clock, and every rule that keeps a throw a cost in seconds and never a danger. */
class SurgePlanTest {
    private static final long QUIET_FROM = CollapsePlan.TOTAL - SurgePlan.QUIET;

    /** A surge that happened: when and what. */
    private record Event(long at, SurgePlan.Act act) {}

    private static List<Event> drive(SurgePlan.Clock c, long from, long to, int stride, boolean exempt, boolean anchored, boolean gate) {
        List<Event> out = new ArrayList<>();
        for (long t = from; t < to; t += stride) {
            SurgePlan.Act a = c.step(t, exempt, anchored, gate);
            if (a != SurgePlan.Act.NONE) out.add(new Event(t, a));
        }
        return out;
    }

    private static List<Long> throwsOf(List<Event> events) {
        List<Long> out = new ArrayList<>();
        for (Event e : events) if (e.act() == SurgePlan.Act.WARP) out.add(e.at());
        return out;
    }

    // ---------------------------------------------------------------- the clock
    @Test
    void aPlayerIsThrownEveryTwelveToTwentySecondsAndNotBeforeAGrace() {
        for (long salt = 0; salt < 400; salt++) {
            SurgePlan.Clock c = new SurgePlan.Clock(salt * 7919 + 3);
            List<Long> at = throwsOf(drive(c, 0, CollapsePlan.TOTAL + 100, 4, false, false, false));
            assertFalse(at.isEmpty(), "somebody who stays in is thrown now and then, salt " + salt);
            assertTrue(at.get(0) >= SurgePlan.GRACE + SurgePlan.MIN_GAP, "not before the grace and a gap: " + at.get(0));
            assertTrue(at.get(0) <= SurgePlan.GRACE + SurgePlan.MAX_GAP + 8, "and not long after it: " + at.get(0));
            for (int i = 1; i < at.size(); i++) {
                long gap = at.get(i) - at.get(i - 1);
                assertTrue(gap >= SurgePlan.MIN_GAP, "at least twelve seconds apart: " + gap);
                assertTrue(gap <= SurgePlan.MAX_GAP + 8, "at most twenty seconds apart (and the stride of a look): " + gap);
            }
            assertTrue(at.size() >= 6 && at.size() <= 12, "a handful in three minutes: " + at.size());
            for (long t : at) assertTrue(t < QUIET_FROM, "none in the last twenty seconds: " + t);
        }
        assertEquals(200, SurgePlan.GRACE, "ten seconds to read the room before the first");
        assertEquals(240, SurgePlan.MIN_GAP, "twelve seconds");
        assertEquals(400, SurgePlan.MAX_GAP, "twenty seconds");
        assertEquals(40, SurgePlan.WARNING, "two seconds of tear");
        assertEquals(400L, SurgePlan.QUIET, "twenty seconds");
        assertEquals(60, SurgePlan.NAUSEA, "three seconds of nausea");
    }

    @Test
    void everyPlayerHasAClockOfTheirOwnSoTheTearsDoNotOpenAllAtOnce() {
        java.util.Set<Long> firsts = new java.util.HashSet<>();
        for (long salt = 0; salt < 200; salt++) {
            SurgePlan.Clock c = new SurgePlan.Clock(CollapsePlan.mix(42, salt));
            firsts.add(throwsOf(drive(c, 0, 2000, 4, false, false, false)).get(0));
        }
        assertTrue(firsts.size() >= 30, "different players are thrown at different moments: " + firsts.size() + " distinct first throws in 200");
        long a = throwsOf(drive(new SurgePlan.Clock(1), 0, 3000, 4, false, false, false)).get(0);
        long b = throwsOf(drive(new SurgePlan.Clock(2), 0, 3000, 4, false, false, false)).get(0);
        long c = throwsOf(drive(new SurgePlan.Clock(3), 0, 3000, 4, false, false, false)).get(0);
        assertTrue(a != b || b != c, "three clocks are not one");
    }

    @Test
    void theSameClockAndTheSameLooksGiveTheSameSurges() {
        for (long salt : new long[]{0, 1, -5, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE}) {
            List<Event> x = drive(new SurgePlan.Clock(salt), 17, 3700, 4, false, false, false);
            List<Event> y = drive(new SurgePlan.Clock(salt), 17, 3700, 4, false, false, false);
            assertEquals(x, y, "deterministic for salt " + salt);
            assertFalse(x.isEmpty());
        }
    }

    @Test
    void aTearShowsForAtLeastTwoSecondsAndEndsInOneThrowOrAClose() {
        for (long salt = 0; salt < 300; salt++) {
            for (int stride : new int[]{1, 2, 4, 7}) {
                SurgePlan.Clock c = new SurgePlan.Clock(salt);
                List<Event> events = drive(c, 0, 3700, stride, false, false, false);
                long openedAt = -1;
                int spirals = 0;
                for (Event e : events) {
                    switch (e.act()) {
                        case TEAR -> {
                            assertEquals(-1, openedAt, "a tear is not opened twice");
                            openedAt = e.at();
                            spirals = 0;
                        }
                        case SPIRAL -> {
                            assertTrue(openedAt >= 0, "a spiral only while a tear is open");
                            spirals++;
                        }
                        case WARP -> {
                            assertTrue(openedAt >= 0, "a throw only from an open tear");
                            assertTrue(e.at() - openedAt >= SurgePlan.WARNING, "two seconds of warning at the least: " + (e.at() - openedAt) + " at stride " + stride);
                            assertTrue(e.at() - openedAt <= SurgePlan.WARNING + stride, "and no longer than needed: " + (e.at() - openedAt));
                            if (stride < SurgePlan.WARNING) assertTrue(spirals > 0, "it is shown while it opens");
                            openedAt = -1;
                        }
                        case CLOSE, HELD -> openedAt = -1;
                        default -> { }
                    }
                }
            }
        }
    }

    @Test
    void aLateLookStillGivesAFullWarning() {
        SurgePlan.Clock c = new SurgePlan.Clock(9);
        assertEquals(SurgePlan.Act.NONE, c.step(0, false, false, false));
        long due = c.due();
        assertTrue(due >= SurgePlan.GRACE + SurgePlan.MIN_GAP);
        SurgePlan.Act first = c.step(due + 500, false, false, false);                 // the server stalled for twenty-five seconds
        assertEquals(SurgePlan.Act.TEAR, first);
        assertEquals(SurgePlan.Act.SPIRAL, c.step(due + 504, false, false, false), "not thrown at once");
        assertEquals(SurgePlan.Act.SPIRAL, c.step(due + 500 + SurgePlan.WARNING - 1, false, false, false));
        assertEquals(SurgePlan.Act.WARP, c.step(due + 500 + SurgePlan.WARNING, false, false, false));
    }

    @Test
    void nothingIsThrownInTheLastTwentySecondsAndAnOpenTearClosesUnused() {
        assertTrue(SurgePlan.quiet(QUIET_FROM) && !SurgePlan.quiet(QUIET_FROM - 1));
        assertTrue(SurgePlan.quiet(CollapsePlan.TOTAL + 50));
        for (long salt = 0; salt < 400; salt++) {
            SurgePlan.Clock c = new SurgePlan.Clock(salt);
            for (long t = 0; t < CollapsePlan.TOTAL + 200; t += 3) {
                SurgePlan.Act a = c.step(t, false, false, false);
                if (t >= QUIET_FROM) assertTrue(a == SurgePlan.Act.NONE || a == SurgePlan.Act.CLOSE, "no tear and no throw at " + t + ": " + a);
                if (a == SurgePlan.Act.TEAR) assertTrue(c.due() < QUIET_FROM, "no tear whose throw would fall in the last twenty seconds: due " + c.due());
                if (a == SurgePlan.Act.WARP) assertTrue(t < QUIET_FROM);
            }
        }
        // a tear that is open when the quiet time begins is closed, not thrown
        boolean closedOne = false;
        for (long salt = 0; salt < 3000 && !closedOne; salt++) {
            SurgePlan.Clock c = new SurgePlan.Clock(salt);
            for (long t = 0; t < QUIET_FROM; t += 4) c.step(t, false, false, false);
            if (c.open()) {
                assertEquals(SurgePlan.Act.CLOSE, c.step(QUIET_FROM, false, false, false), "the tear that was open when the quiet began");
                assertFalse(c.open());
                closedOne = true;
            }
        }
    }

    @Test
    void nobodyInCreativeOrSpectatingIsEverThrownAndAReturnerGetsAFreshGrace() {
        SurgePlan.Clock c = new SurgePlan.Clock(77);
        assertTrue(drive(c, 0, 3600, 4, true, false, false).isEmpty(), "an exempt player is never touched");
        c.step(100, false, false, false);
        long due = c.due();
        assertTrue(due >= 100 + SurgePlan.GRACE + SurgePlan.MIN_GAP, "the clock starts when they come back");
        // exempt in the middle of a tear: it closes, and the schedule starts over
        SurgePlan.Clock d = new SurgePlan.Clock(5);
        long t = 0;
        while (!d.open() && t < 2000) d.step(t += 4, false, false, false);
        assertTrue(d.open(), "a tear opened");
        assertEquals(SurgePlan.Act.CLOSE, d.step(t + 4, true, false, false));
        assertFalse(d.open());
        assertEquals(SurgePlan.Act.NONE, d.step(t + 8, true, false, false));
        assertEquals(SurgePlan.Act.NONE, d.step(t + 12, false, false, false), "the first look after: a grace");
        long again = d.due();
        assertTrue(again >= t + 12 + SurgePlan.GRACE + SurgePlan.MIN_GAP);
    }

    @Test
    void nobodyNearARootGateIsEverThrown() {
        for (long salt = 0; salt < 200; salt++) {
            SurgePlan.Clock c = new SurgePlan.Clock(salt);
            List<Event> ev = drive(c, 0, 3700, 4, false, false, true);
            for (Event e : ev) assertTrue(e.act() != SurgePlan.Act.WARP && e.act() != SurgePlan.Act.TEAR && e.act() != SurgePlan.Act.SPIRAL, "a player at a gate: " + e);
        }
        // a gate that comes near while the tear is open closes it, and so does standing at one at the moment of the throw
        SurgePlan.Clock c = new SurgePlan.Clock(12);
        long t = 0;
        while (!c.open() && t < 2000) c.step(t += 4, false, false, false);
        assertTrue(c.open());
        assertEquals(SurgePlan.Act.SPIRAL, c.step(t + 4, false, false, false));
        assertEquals(SurgePlan.Act.CLOSE, c.step(t + 8, false, false, true), "the player reached the gate");
        assertFalse(c.open());
        SurgePlan.Clock last = new SurgePlan.Clock(12);
        long u = 0;
        while (!last.open() && u < 2000) last.step(u += 4, false, false, false);
        long throwAt = last.due();
        for (long k = u + 4; k < throwAt; k += 4) assertEquals(SurgePlan.Act.SPIRAL, last.step(k, false, false, false));
        assertEquals(SurgePlan.Act.CLOSE, last.step(throwAt, false, false, true), "at the very moment of the throw, a gate at hand wins");
        // away from the gate again, it is tried again soon (not a whole gap later)
        long retried = -1;
        for (long k = throwAt + 4; k < throwAt + 200 && retried < 0; k += 4) if (last.step(k, false, false, false) == SurgePlan.Act.TEAR) retried = k;
        assertTrue(retried > 0 && retried - throwAt <= SurgePlan.RETRY + 8, "tried again after a short wait: " + (retried - throwAt));
    }

    @Test
    void theAnchorHoldsAndIsToldSoNowAndThenButNeverThrown() {
        for (long salt = 0; salt < 200; salt++) {
            SurgePlan.Clock c = new SurgePlan.Clock(salt);
            List<Event> ev = drive(c, 0, 3700, 4, false, true, false);
            int held = 0;
            long lastHeld = Long.MIN_VALUE;
            for (Event e : ev) {
                assertTrue(e.act() == SurgePlan.Act.HELD, "an anchored player only hears that the Anchor holds: " + e);
                if (lastHeld != Long.MIN_VALUE) assertTrue(e.at() - lastHeld >= SurgePlan.MIN_GAP, "not every look: " + (e.at() - lastHeld));
                lastHeld = e.at();
                held++;
            }
            assertTrue(held >= 4 && held <= 12, "told now and then: " + held);
        }
        // a Band put on while the tear is open closes it
        SurgePlan.Clock c = new SurgePlan.Clock(31);
        long t = 0;
        while (!c.open() && t < 2000) c.step(t += 4, false, false, false);
        assertTrue(c.open());
        assertEquals(SurgePlan.Act.HELD, c.step(t + 4, false, true, false));
        assertFalse(c.open());
    }

    @Test
    void neverTwoThrowsWithinTwelveSecondsWhateverHappensToTheSchedule() {
        for (long seed = 0; seed < 300; seed++) {
            Random r = new Random(seed);
            SurgePlan.Clock c = new SurgePlan.Clock(seed * 31 + 1);
            long t = r.nextInt(300), lastWarp = Long.MIN_VALUE / 2;
            int throwsMade = 0;
            while (t < CollapsePlan.TOTAL + 100) {
                boolean exempt = r.nextInt(60) == 0, anchored = r.nextInt(25) == 0, gate = r.nextInt(18) == 0;
                SurgePlan.Act a = c.step(t, exempt, anchored, gate);
                if (a == SurgePlan.Act.WARP) {
                    assertTrue(t - lastWarp >= SurgePlan.MIN_GAP, "seed " + seed + ": " + (t - lastWarp));
                    assertTrue(t < QUIET_FROM, "seed " + seed);
                    assertFalse(exempt || anchored || gate, "never at the moment of an exemption");
                    lastWarp = t;
                    assertEquals(t, c.lastWarp(), "the clock remembers its last throw");
                    throwsMade++;
                }
                if (a == SurgePlan.Act.TEAR || a == SurgePlan.Act.SPIRAL) assertFalse(exempt || anchored || gate, "no tear under the exempt: " + a);
                t += 1 + r.nextInt(9);
            }
            assertTrue(throwsMade <= 12, "a bounded number: " + throwsMade);
        }
    }

    @Test
    void aClockThatRunsBackwardsOrJumpsFarCannotBreakIt() {
        SurgePlan.Clock c = new SurgePlan.Clock(1);
        assertEquals(SurgePlan.Act.NONE, c.step(500, false, false, false));
        assertEquals(SurgePlan.Act.NONE, c.step(10, false, false, false), "time went back: it just waits");
        assertEquals(SurgePlan.Act.NONE, c.step(CollapsePlan.TOTAL * 10, false, false, false), "far past the end: quiet");
        assertFalse(c.open());
        SurgePlan.Clock d = new SurgePlan.Clock(2);
        assertEquals(SurgePlan.Act.NONE, d.step(Long.MAX_VALUE / 4, false, false, false));
    }

    // ---------------------------------------------------------------- where a throw may end
    @Test
    void aThrowMayCostAtMostTenBlocksOfDistanceToTheNearestExit() {
        assertTrue(SurgePlan.costsLittle(30.0, 40.0), "exactly ten farther is allowed");
        assertFalse(SurgePlan.costsLittle(30.0, 40.01));
        assertTrue(SurgePlan.costsLittle(30.0, 5.0), "any amount nearer is fine");
        assertTrue(SurgePlan.costsLittle(0.0, 10.0));
        assertFalse(SurgePlan.costsLittle(0.0, 10.5));
        assertFalse(SurgePlan.costsLittle(30.0, Double.NaN), "a broken number is refused");
        assertFalse(SurgePlan.costsLittle(Double.NaN, 30.0));
        assertTrue(SurgePlan.costsLittle(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), "no exit is known: nothing to be farther from");
        assertEquals(10, SurgePlan.MAX_FARTHER);
    }

    @Test
    void theDistanceToTheNearestExitIsFlatAndFromTheMiddleOfTheBlock() {
        List<int[]> exits = List.of(new int[]{10, 5, 0}, new int[]{-30, -50, 40});
        assertEquals(0.0, SurgePlan.exitDistance(exits, 10.5, 0.5), 1e-9);
        assertEquals(10.0, SurgePlan.exitDistance(exits, 0.5, 0.5), 1e-9, "the height does not count");
        assertEquals(50.0, SurgePlan.exitDistance(List.of(new int[]{-30, 0, 40}), 0.5, 0.5), 1e-9, "a 30-40-50 triangle");
        assertEquals(5.0, SurgePlan.exitDistance(List.of(new int[]{3, 0, 4}), 0.5, 0.5), 1e-9, "a 3-4-5 triangle");
        assertEquals(Double.POSITIVE_INFINITY, SurgePlan.exitDistance(List.of(), 3, 4));
        assertEquals(Double.POSITIVE_INFINITY, SurgePlan.exitDistance(null, 3, 4));
    }

    @Test
    void aRootGateWithinFourBlocksIsNear() {
        List<int[]> gates = List.of(new int[]{100, 10, 100});
        assertTrue(SurgePlan.nearGate(gates, 100.5, 10.5, 100.5));
        assertTrue(SurgePlan.nearGate(gates, 100.5 + 4.0, 10.5, 100.5), "exactly four blocks is near");
        assertFalse(SurgePlan.nearGate(gates, 100.5 + 4.01, 10.5, 100.5));
        assertTrue(SurgePlan.nearGate(gates, 100.5, 10.5 + 4.0, 100.5), "up and down count too");
        assertFalse(SurgePlan.nearGate(gates, 100.5, 10.5 + 4.5, 100.5));
        assertTrue(SurgePlan.nearGate(gates, 102.5, 12.5, 100.5 + 1.0), "three blocks in a diagonal (3.0)");
        assertFalse(SurgePlan.nearGate(gates, 103.0, 13.0, 103.0), "five blocks in a diagonal");
        assertFalse(SurgePlan.nearGate(List.of(), 0, 0, 0));
        assertFalse(SurgePlan.nearGate(null, 0, 0, 0));
        assertTrue(SurgePlan.nearGate(List.of(new int[]{0, 0, 0}, new int[]{100, 10, 100}), 100.5, 10.5, 100.5), "any gate of several");
        assertEquals(4.0, SurgePlan.GATE_SAFE);
    }

    @Test
    void aColumnIsRefusedOnceTheStoneHasSweptItOrIsAboutToAndNeverOutsideTheCollapse() {
        int open = 1000, cursor = 300, front = 420;
        assertFalse(SurgePlan.columnOk(-1, cursor, front, open), "the collapse has no such column");
        assertFalse(SurgePlan.columnOk(0, cursor, front, open), "swept");
        assertFalse(SurgePlan.columnOk(cursor - 1, cursor, front, open), "just swept");
        assertFalse(SurgePlan.columnOk(cursor, cursor, front, open), "next to fall");
        assertFalse(SurgePlan.columnOk(front - 1, cursor, front, open), "about to be swept within the warning ahead of the front");
        assertTrue(SurgePlan.columnOk(front, cursor, front, open), "the first column the front will not have reached");
        assertTrue(SurgePlan.columnOk(open - 1, cursor, front, open));
        assertTrue(SurgePlan.columnOk(open, 5, front, open), "a corridor stands until the wipe, whatever the cursor");
        assertTrue(SurgePlan.columnOk(open, open + 5, open + 5, open), "even if the cursor and the warning limit were past the swept columns");
        assertTrue(SurgePlan.columnOk(open + 40, cursor, front, open));
        assertTrue(SurgePlan.columnOk(5, 0, 0, open), "early on, before the front, every column is fine");
        assertFalse(SurgePlan.columnOk(5, 9, 0, open), "a cursor ahead of the warning limit still counts");
        assertTrue(SurgePlan.columnAllowed(front, cursor, front, open, 30.0, 40.0));
        assertFalse(SurgePlan.columnAllowed(front, cursor, front, open, 30.0, 41.0), "a good column that is too far from the way out");
        assertFalse(SurgePlan.columnAllowed(front - 1, cursor, front, open, 30.0, 10.0), "a good distance on ground that is going");
    }

    @Test
    void theFeetStandAboveTheFloorAndTheHeadUnderTheRoof() {
        int floor = -52, top = -13;
        assertFalse(SurgePlan.heightOk(floor, floor, top), "in the floor");
        assertTrue(SurgePlan.heightOk(floor + 1, floor, top));
        assertTrue(SurgePlan.heightOk(top - 1, floor, top), "feet at top-1, head at top");
        assertFalse(SurgePlan.heightOk(top, floor, top), "head above the roof line");
        assertFalse(SurgePlan.heightOk(floor - 6, floor, top), "in the rock under it: another cave");
        assertFalse(SurgePlan.heightOk(top + 6, floor, top), "above it: another cave");
    }

    @Test
    void aThrowLandsAtLeastSixBlocksFromTheOldSpotAndEightToTwentyFourAcross() {
        assertTrue(SurgePlan.farFromOld(6, 0, 0));
        assertTrue(SurgePlan.farFromOld(0, 6, 0));
        assertTrue(SurgePlan.farFromOld(-4, 4, 2), "exactly 36");
        assertFalse(SurgePlan.farFromOld(4, 4, 0));
        assertFalse(SurgePlan.farFromOld(3, 3, 3));
        assertFalse(SurgePlan.farFromOld(0, 0, 0));
        assertTrue(SurgePlan.spotAllowed(-40, -52, -13, 8, 0, 8));
        assertFalse(SurgePlan.spotAllowed(-40, -52, -13, 2, 0, 2), "too close to the old spot");
        assertFalse(SurgePlan.spotAllowed(-60, -52, -13, 8, 0, 8), "under the floor");
        assertTrue(SurgePlan.moveOk(8, 0));
        assertTrue(SurgePlan.moveOk(0, -24));
        assertTrue(SurgePlan.moveOk(16, 17), "23.3");
        assertFalse(SurgePlan.moveOk(17, 17), "24.04 is over");
        assertFalse(SurgePlan.moveOk(7, 0));
        assertFalse(SurgePlan.moveOk(17, 18));
        assertFalse(SurgePlan.moveOk(25, 0));
        assertFalse(SurgePlan.moveOk(Integer.MAX_VALUE, Integer.MAX_VALUE), "no overflow");
        assertEquals(8, SurgePlan.MIN_MOVE);
        assertEquals(24, SurgePlan.MAX_MOVE);
        assertEquals(6, SurgePlan.MIN_FROM_OLD);
    }

    // ---------------------------------------------------------------- a place the player can walk back from
    /** A ground that counts the questions it is asked. */
    private static final class CountingGround implements SurgePlan.Ground {
        final SurgePlan.Ground inner;
        int asked;

        CountingGround(SurgePlan.Ground inner) { this.inner = inner; }

        @Override
        public boolean stand(int x, int y, int z) {
            asked++;
            return inner.stand(x, y, z);
        }
    }

    private static boolean walk(SurgePlan.Ground g, int fx, int fy, int fz, int gx, int gy, int gz) { return SurgePlan.connected(g, fx, fy, fz, gx, gy, gz); }

    @Test
    void anOpenHallCanBeWalkedAcrossInEveryDirection() {
        SurgePlan.Ground hall = (x, y, z) -> y == 0 && Math.abs(x) <= 100 && Math.abs(z) <= 100;
        for (int[] to : new int[][]{{20, 0, 5}, {-17, 0, -17}, {0, 0, 24}, {8, 0, 0}, {-24, 0, 3}}) {
            assertTrue(walk(hall, 0, 0, 0, to[0], to[1], to[2]), "to " + java.util.Arrays.toString(to));
            assertTrue(walk(hall, to[0], to[1], to[2], 0, 0, 0), "and back");
        }
        assertTrue(walk(hall, 0, 0, 0, 1, 0, 1), "already there");
    }

    @Test
    void aSealedRoomCannotBeLeftButADoorwayLetsYouOut() {
        // a room x 0..5, z 0..5 with walls at x = 6 and 7, open floor outside from x = 8
        SurgePlan.Ground sealed = (x, y, z) -> y == 0 && ((x >= 0 && x <= 5 && z >= 0 && z <= 5) || (x >= 8 && x <= 60 && Math.abs(z) <= 60));
        assertFalse(walk(sealed, 2, 0, 2, 12, 0, 2), "from inside the sealed room to the hall");
        assertFalse(walk(sealed, 12, 0, 2, 2, 0, 2), "and from the hall into it");
        SurgePlan.Ground door = (x, y, z) -> sealed.stand(x, y, z) || (y == 0 && (x == 6 || x == 7) && z == 2);
        assertTrue(walk(door, 2, 0, 2, 12, 0, 2), "with a doorway it is one place");
        assertTrue(walk(door, 12, 0, 2, 2, 0, 2));
        SurgePlan.Ground shutGate = (x, y, z) -> door.stand(x, y, z) && !(x == 7 && z == 2);
        assertFalse(walk(shutGate, 2, 0, 2, 12, 0, 2), "a shut gate in the doorway seals it again");
    }

    @Test
    void aStepUpIsWalkedAndALongDropIsOneWay() {
        // y = 0 for x < 10, one block up for 10..19, then the floor drops by `drop` from x = 20
        for (int drop = 1; drop <= 5; drop++) {
            final int d = drop;
            SurgePlan.Ground g = (x, y, z) -> Math.abs(z) <= 20 && ((x < 10 && x >= -30 && y == 0) || (x >= 10 && x < 20 && y == 1) || (x >= 20 && x <= 50 && y == 1 - d));
            assertTrue(walk(g, 0, 0, 0, 15, 1, 0), "up the step");
            assertTrue(walk(g, 15, 1, 0, 0, 0, 0), "and down it");
            boolean down = walk(g, 0, 0, 0, 25, 1 - d, 0);
            assertEquals(d <= 3, down, "a drop of " + d + " is walked down only up to three blocks");
            assertEquals(d == 1, walk(g, 25, 1 - d, 0, 0, 0, 0), "a rise of " + d + " cannot be climbed, a rise of one is a step");
        }
    }

    @Test
    void anIslandAcrossAGapIsNotWalkable() {
        SurgePlan.Ground island = (x, y, z) -> y == 0 && Math.abs(z) <= 30 && ((x >= -30 && x <= 0) || (x >= 3 && x <= 40));
        assertFalse(walk(island, -5, 0, 0, 20, 0, 0), "a gap three blocks wide");
        assertFalse(walk(island, 20, 0, 0, -5, 0, 0));
        assertTrue(walk(island, 10, 0, 0, 20, 0, 0), "but each side is one place");
    }

    @Test
    void aShortWayRoundIsWalkedAndAWallPastTheBoxIsNot() {
        // a wall along x = 10 from z = -6 to z = 5, a gap at z = 6 and open floor beyond it
        SurgePlan.Ground around = (x, y, z) -> y == 0 && Math.abs(x) <= 100 && Math.abs(z) <= 100 && !(x == 10 && z >= -6 && z < 6);
        assertTrue(walk(around, 0, 0, 0, 20, 0, 0), "round the end of the wall: about thirty steps");
        assertTrue(walk(around, 20, 0, 0, 0, 0, 0));
        SurgePlan.Ground far = (x, y, z) -> y == 0 && Math.abs(x) <= 100 && Math.abs(z) <= 100 && !(x == 10 && z >= -60 && z <= 60);
        assertFalse(walk(far, 0, 0, 0, 20, 0, 0), "a wall that runs past the box of the search");
    }

    @Test
    void aWayRoundThatLeavesTheBoxOfTheSearchIsNotAWayBack() {
        // a one-block corridor: out along x = 0 to z = 50, across to x = 20 and back along it: about 120 steps, but past z = 40
        SurgePlan.Ground u = (x, y, z) -> y == 0 && ((x == 0 && z >= 0 && z <= 50) || (x == 20 && z >= 0 && z <= 50) || (z == 50 && x >= 0 && x <= 20));
        assertFalse(walk(u, 0, 0, 0, 20, 0, 0), "the search stays within forty blocks of where it began");
        SurgePlan.Ground shorter = (x, y, z) -> y == 0 && ((x == 0 && z >= 0 && z <= 30) || (x == 20 && z >= 0 && z <= 30) || (z == 30 && x >= 0 && x <= 20));
        assertTrue(walk(shorter, 0, 0, 0, 20, 0, 0), "the same corridor turning back at thirty blocks is a way");
    }

    @Test
    void theStartHasToBeStandableAndTheGoalNeedsOnlyBeNear() {
        SurgePlan.Ground floor = (x, y, z) -> y == 0;
        assertFalse(walk(floor, 0, 5, 0, 10, 0, 0), "nobody stands in the air");
        SurgePlan.Ground edge = (x, y, z) -> y == 0 && x >= 1;
        assertFalse(walk(edge, 0, 0, 0, 10, 0, 0), "the start is not a place to stand even though the next block is");
        assertTrue(walk(edge, 1, 0, 0, 10, 0, 0));
        assertFalse(walk(null, 0, 0, 0, 10, 0, 0));
        assertTrue(walk(floor, 0, 0, 0, 12, 3, 12), "a goal three blocks above the floor is near enough");
        assertTrue(walk(floor, 0, 0, 0, 12, 0, 12));
        assertEquals(1200, SurgePlan.WALK_BUDGET);
        assertEquals(3, SurgePlan.WALK_DROP);
        assertEquals(2, SurgePlan.WALK_NEAR);
    }

    @Test
    void theWalkIsBoundedWhateverTheWorldIs() {
        // the goal sits in a sealed bubble in an endless hall: the search must give up after its budget
        CountingGround endless = new CountingGround((x, y, z) -> y == 0 && !(Math.abs(x - 30) <= 3 && Math.abs(z) <= 3 && !(Math.abs(x - 30) <= 1 && Math.abs(z) <= 1)));
        assertFalse(walk(endless, 0, 0, 0, 30, 0, 0));
        assertTrue(endless.asked > 100, "it did look: " + endless.asked);
        assertTrue(endless.asked <= SurgePlan.WALK_BUDGET * 4 * 5 + 5, "and no more than its budget allows: " + endless.asked);
        CountingGround quick = new CountingGround((x, y, z) -> y == 0);
        assertTrue(walk(quick, 0, 0, 0, 24, 0, 0));
        assertTrue(quick.asked < 600, "a clear way is found quickly: " + quick.asked);
    }

    @Test
    void theWalkAgreesWithAnExactSearchOnRandomRooms() {
        Random r = new Random(7);
        int reachable = 0, unreachable = 0;
        for (int round = 0; round < 300; round++) {
            boolean[][] open = new boolean[31][31];
            int density = 45 + r.nextInt(35);
            for (int x = 0; x < 31; x++) for (int z = 0; z < 31; z++) open[x][z] = r.nextInt(100) < density;
            SurgePlan.Ground g = (x, y, z) -> y == 0 && x >= 0 && x < 31 && z >= 0 && z < 31 && open[x][z];
            int sx = r.nextInt(31), sz = r.nextInt(31), tx = r.nextInt(31), tz = r.nextInt(31);
            if (!open[sx][sz]) continue;
            // the exact search: the same moves on one level, every cell of the room
            boolean[][] seen = new boolean[31][31];
            java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
            q.add(new int[]{sx, sz});
            seen[sx][sz] = true;
            boolean there = false;
            while (!q.isEmpty()) {
                int[] c = q.poll();
                if (Math.abs(c[0] - tx) <= SurgePlan.WALK_NEAR && Math.abs(c[1] - tz) <= SurgePlan.WALK_NEAR) there = true;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = c[0] + d[0], nz = c[1] + d[1];
                    if (nx >= 0 && nx < 31 && nz >= 0 && nz < 31 && open[nx][nz] && !seen[nx][nz]) {
                        seen[nx][nz] = true;
                        q.add(new int[]{nx, nz});
                    }
                }
            }
            assertEquals(there, walk(g, sx, 0, sz, tx, 0, tz), "round " + round + " from " + sx + "," + sz + " to " + tx + "," + tz);
            if (there) reachable++;
            else unreachable++;
        }
        assertTrue(reachable > 40 && unreachable > 40, "both kinds were tried: " + reachable + " reachable, " + unreachable + " not");
    }

    @Test
    void aThrowStaysOnTheLevelItBeganOn() {
        assertTrue(SurgePlan.spotAllowed(-40, -52, -13, 8, 0, 8));
        assertTrue(SurgePlan.spotAllowed(-40, -52, -13, 8, SurgePlan.MAX_LEVEL, 8));
        assertTrue(SurgePlan.spotAllowed(-40, -52, -13, 8, -SurgePlan.MAX_LEVEL, 8));
        assertFalse(SurgePlan.spotAllowed(-40, -52, -13, 8, SurgePlan.MAX_LEVEL + 1, 8), "a balcony five blocks up is another level");
        assertFalse(SurgePlan.spotAllowed(-40, -52, -13, 8, -SurgePlan.MAX_LEVEL - 1, 8), "and so is a pit five blocks down");
        assertEquals(4, SurgePlan.MAX_LEVEL);
    }

    // ---------------------------------------------------------------- the rules, together, on a real order
    /** The offsets the Warp Trap throws: 8 to 24 blocks at any angle (the vault's own are the same range). */
    private static List<int[]> offsets(Random r, int n) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double d = SurgePlan.MIN_MOVE + r.nextInt(SurgePlan.MAX_MOVE - SurgePlan.MIN_MOVE + 1), a = r.nextDouble() * Math.PI * 2;
            int dx = (int) Math.round(Math.cos(a) * d), dz = (int) Math.round(Math.sin(a) * d);
            if (SurgePlan.moveOk(dx, dz)) out.add(new int[]{dx, dz});
        }
        return out;
    }

    @Test
    void everyThrowTheRulesAllowEndsInsideTheCollapseOnGroundThatStandsAndCostsTenBlocksAtMost() {
        Footprint fp = Footprint.oval(0, 0, 47, 47);
        int[][] exitArr = {{43, 0}, {-43, 0}, {0, 43}};
        int[] ex = {43, -43, 0}, ez = {0, 0, 43};
        CollapseSweep order = CollapseSweep.build(fp, CollapseState.MARGIN, ex, ez, CollapsePlan.CORRIDOR_RADIUS);
        List<int[]> exits = new ArrayList<>();
        for (int[] e : exitArr) exits.add(new int[]{e[0], -51, e[1]});
        Random r = new Random(2024);
        int asked = 0, allowed = 0;
        for (long elapsed : new long[]{100, 1000, 1300, 1800, 2400, 2800, 3100}) {
            int open = order.open();
            int cursor = CollapsePlan.frontColumns(elapsed, open);                     // the sweep keeps up with its schedule
            int limit = CollapsePlan.warnLimit(elapsed, open);
            int playersWithAWay = 0, players = 0;
            for (int k = 0; k < 400; k++) {
                int px = r.nextInt(95) - 47, pz = r.nextInt(95) - 47;
                if (!fp.contains(px, pz)) continue;
                players++;
                double before = SurgePlan.exitDistance(exits, px + 0.5, pz + 0.5);
                boolean any = false;
                for (int[] o : offsets(r, 24)) {
                    int x = px + o[0], z = pz + o[1];
                    asked++;
                    if (!fp.contains(x, z)) continue;
                    double after = SurgePlan.exitDistance(exits, x + 0.5, z + 0.5);
                    int index = order.indexOf(x, z);
                    if (!SurgePlan.columnAllowed(index, cursor, limit, open, before, after)) continue;
                    allowed++;
                    any = true;
                    assertTrue(index >= 0 && fp.contains(x, z), "inside the collapse");
                    assertTrue(index >= open || index >= Math.max(cursor, limit), "not on ground the stone has swept or is about to: index " + index + " cursor " + cursor + " limit " + limit + " at " + elapsed);
                    assertTrue(after <= before + SurgePlan.MAX_FARTHER + 1e-9, "at most ten blocks farther from the nearest exit: " + before + " -> " + after);
                    assertTrue(SurgePlan.moveOk(o[0], o[1]));
                }
                if (any) playersWithAWay++;
            }
            assertTrue(players > 100);
            if (elapsed <= 2400) assertTrue(playersWithAWay >= players * 0.9, "most players can be thrown somewhere at " + elapsed + ": " + playersWithAWay + " of " + players);
            else assertTrue(playersWithAWay >= players * 0.3, "and a good many still late in the roll at " + elapsed + ": " + playersWithAWay + " of " + players);
        }
        assertTrue(allowed > 0 && allowed < asked, "the rules refuse some and allow some: " + allowed + " of " + asked);
    }
}
