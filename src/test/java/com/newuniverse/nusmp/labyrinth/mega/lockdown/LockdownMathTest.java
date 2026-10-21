package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the gates a room passes before it closes are the whole of the anti-speedrun rule, and each refusal has a name so that "nothing locked" always has a reason. These
 * tests pin the ORDER of the gates (a safe room is refused before anything else about it is asked), that exactly one set of facts arms a room, how depth and a bigger
 * party raise a wave (never past HARD by themselves), that the share of a common kind that locks is a property of the room and not of the moment, and the geometry of
 * "deep enough inside".
 */
class LockdownMathTest {
    private static LockdownMath.Facts ok() {
        return new LockdownMath.Facts(true, false, false, true, true, 5, 2, 400, 100, false, false, 0L, true, true);
    }

    @Test
    void exactlyTheFreshChallengeRoomWithAPartyDeepInsideArms() {
        assertEquals(LockdownMath.Verdict.ARM, LockdownMath.decide(ok()));
        assertTrue(LockdownMath.decide(ok()).arms());
    }

    @Test
    void everyGateRefusesWithItsOwnName() {
        LockdownMath.Facts f = ok();
        assertEquals(LockdownMath.Verdict.OFF, LockdownMath.decide(new LockdownMath.Facts(false, false, false, true, true, 5, 2, 400, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.PEACEFUL, LockdownMath.decide(new LockdownMath.Facts(true, true, false, true, true, 5, 2, 400, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.SAFE_ROOM, LockdownMath.decide(new LockdownMath.Facts(true, false, true, true, true, 5, 2, 400, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.NOT_LOCKABLE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, false, true, 5, 2, 400, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.CHANCE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, false, 5, 2, 400, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.TOO_SHALLOW, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 1, 2, 400, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.TOO_SMALL, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 5, 2, 50, 100, false, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.CLEARED, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 5, 2, 400, 100, true, false, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.ACTIVE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 5, 2, 400, 100, false, true, 0L, true, true)));
        assertEquals(LockdownMath.Verdict.COOLDOWN, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 5, 2, 400, 100, false, false, 40L, true, true)));
        assertEquals(LockdownMath.Verdict.NOBODY, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 5, 2, 400, 100, false, false, 0L, false, true)));
        assertEquals(LockdownMath.Verdict.NOT_INSIDE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 5, 2, 400, 100, false, false, 0L, true, false)));
        assertEquals(LockdownMath.Verdict.ARM, LockdownMath.decide(f));
    }

    @Test
    void theGatesAreAskedInTheOrderTheDocSays() {
        // everything wrong at once: the first gate answers
        LockdownMath.Facts worst = new LockdownMath.Facts(false, true, true, false, false, 1, 9, 1, 9, true, true, 99L, false, false);
        assertEquals(LockdownMath.Verdict.OFF, LockdownMath.decide(worst));
        assertEquals(LockdownMath.Verdict.PEACEFUL, LockdownMath.decide(new LockdownMath.Facts(true, true, true, false, false, 1, 9, 1, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.SAFE_ROOM, LockdownMath.decide(new LockdownMath.Facts(true, false, true, false, false, 1, 9, 1, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.NOT_LOCKABLE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, false, false, 1, 9, 1, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.CHANCE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, false, 1, 9, 1, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.TOO_SHALLOW, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 1, 9, 1, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.TOO_SMALL, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 9, 9, 1, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.CLEARED, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 9, 9, 9, 9, true, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.ACTIVE, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 9, 9, 9, 9, false, true, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.COOLDOWN, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 9, 9, 9, 9, false, false, 99L, false, false)));
        assertEquals(LockdownMath.Verdict.NOBODY, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 9, 9, 9, 9, false, false, 0L, false, false)));
    }

    @Test
    void anUnknownFloorIsNeverTooShallow() {
        // depth 0 = the floor is not known: a room is not refused for a floor nobody could read
        assertEquals(LockdownMath.Verdict.ARM, LockdownMath.decide(new LockdownMath.Facts(true, false, false, true, true, 0, 5, 400, 100, false, false, 0L, true, true)));
    }

    @Test
    void depthAndAPartyRaiseTheWaveButNeverPastHard() {
        assertEquals(Band.STANDARD, LockdownMath.band(Band.STANDARD, 5, 1));
        assertEquals(Band.HARD, LockdownMath.band(Band.STANDARD, LockdownMath.DEEP_FLOOR, 1));
        assertEquals(Band.HARD, LockdownMath.band(Band.STANDARD, 5, LockdownMath.BIG_PARTY));
        assertEquals(Band.HARD, LockdownMath.band(Band.HARD, 100, 8), "depth and party together stop at HARD");
        assertEquals(Band.HARD, LockdownMath.band(Band.STANDARD, 100, 8));
        assertEquals(Band.AMBUSH, LockdownMath.band(Band.AMBUSH, 1, 1), "a data file may ask for an ambush-sized wave outright");
        assertEquals(Band.STANDARD, LockdownMath.band(null, 1, 1));
        assertEquals(Band.STANDARD, LockdownMath.band(Band.NOTHING, 1, 1));
    }

    @Test
    void wavesGrowFromFloorFortyAndStopAtThree() {
        assertEquals(1, LockdownMath.waves(1, 10));
        assertEquals(2, LockdownMath.waves(1, 40));
        assertEquals(3, LockdownMath.waves(3, 100));
        assertEquals(1, LockdownMath.waves(0, 1), "a wave count of nothing is one wave");
        assertTrue(LockdownMath.moreWaves(1, 2));
        assertFalse(LockdownMath.moreWaves(2, 2));
    }

    @Test
    void theShareOfAKindThatLocksIsAPropertyOfTheRoom() {
        for (int room = 0; room < 50; room++) assertEquals(LockdownMath.chanceOk(77L, room, 0.35), LockdownMath.chanceOk(77L, room, 0.35), "the same room always answers the same");
        assertTrue(LockdownMath.chanceOk(1L, 3, 1.0));
        assertFalse(LockdownMath.chanceOk(1L, 3, 0.0));
        int locked = 0, n = 4000;
        for (int room = 0; room < n; room++) if (LockdownMath.chanceOk(123456789L, room, 0.35)) locked++;
        assertTrue(Math.abs(locked / (double) n - 0.35) < 0.04, "about a third of the rooms lock, got " + locked + " of " + n);
        boolean differs = false;
        for (int room = 0; room < 64 && !differs; room++) differs = LockdownMath.chanceOk(1L, room, 0.5) != LockdownMath.chanceOk(2L, room, 0.5);
        assertTrue(differs, "another sector's seed picks other rooms");
    }

    @Test
    void deepInsideKeepsAPlayerOutOfTheDoorway() {
        // a room of 20 x 20 blocks at 0..19; the wall is two thick; the default inset is 4
        assertFalse(LockdownMath.deepInside(0, 0, 19, 19, 0.5, 10.0, 4), "in the doorway");
        assertFalse(LockdownMath.deepInside(0, 0, 19, 19, 3.9, 10.0, 4));
        assertTrue(LockdownMath.deepInside(0, 0, 19, 19, 4.0, 10.0, 4));
        assertTrue(LockdownMath.deepInside(0, 0, 19, 19, 10.0, 10.0, 4));
        assertTrue(LockdownMath.deepInside(0, 0, 19, 19, 15.99, 15.99, 4));
        assertFalse(LockdownMath.deepInside(0, 0, 19, 19, 16.0, 10.0, 4));
        assertFalse(LockdownMath.deepInside(0, 0, 7, 7, 4.0, 4.0, 4), "a room too small for the inset never locks");
    }

    @Test
    void roomKeysRoundTrip() {
        for (int[] p : new int[][] {{0, 0}, {3, 17}, {120, 5000}, {Integer.MAX_VALUE, 1}, {1, Integer.MAX_VALUE}}) {
            long k = LockdownMath.key(p[0], p[1]);
            assertEquals(p[0], LockdownMath.sectorOf(k));
            assertEquals(p[1], LockdownMath.roomOf(k));
        }
        assertNotEquals(LockdownMath.key(1, 2), LockdownMath.key(2, 1));
        assertEquals(20 * 30, LockdownMath.area(0, 0, 19, 29));
        assertEquals(0, LockdownMath.area(5, 5, 4, 4));
    }
}
