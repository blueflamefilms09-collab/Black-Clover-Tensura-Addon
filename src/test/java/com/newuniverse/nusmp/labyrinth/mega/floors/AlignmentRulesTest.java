package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Directional Lock is the one rule of this feature that can lock a player out of half the game, or fail to lock them at all, so every transition
 * of {@link AlignmentRules} is pinned here move by move in the owner's words: the first floor of a road only chooses, clearing it locks, a locked player
 * may use their own road and never the other, going back to Floor 0 changes nothing, and a reset gives everything back. The loopholes are pinned too (a
 * dip into the other road before the lock, a floor cleared twice, an operator on the wrong road), and so is the save: the five values the codec stores
 * must rebuild the same state, and a damaged save must load into a valid one.
 */
class AlignmentRulesTest {
    private static final Polarity H = Polarity.HOLY, A = Polarity.ABYSSAL;

    /** The tracker's move: ask what a step from {@code previous} to {@code current} does, then apply it the way {@code FloorTracker} does. Returns true when it newly locked. */
    private static boolean step(PlayerDungeonAlignment s, int previous, int current, int lockAtFloor, long now) {
        boolean clears = AlignmentRules.clearsPrevious(s, previous, current);
        AlignmentRules.onFloorEntered(s, current);
        return clears && AlignmentRules.onFloorCleared(s, previous, lockAtFloor, now);
    }

    // ------------------------------------------------------------------------------------------------ who may take a road
    @Test void mayEnterFollowsTheSixLineTruthTable() {
        Polarity[] roads = {null, H, A};
        for (Polarity chosen : roads) {
            for (boolean locked : new boolean[] {false, true}) {
                for (Polarity wanted : roads) {
                    for (boolean enabled : new boolean[] {false, true}) {
                        boolean expected;
                        if (!enabled) expected = true;                       // the lock is switched off
                        else if (!locked) expected = true;                   // nobody is bound yet
                        else if (chosen == null) expected = true;            // a lock without a road cannot bind
                        else if (wanted == null) expected = true;            // Floor 0 is not a road
                        else expected = chosen == wanted;                    // bound: only their own road
                        assertEquals(expected, AlignmentRules.mayEnter(chosen, locked, wanted, enabled), "chosen " + chosen + " locked " + locked + " wanted " + wanted + " enabled " + enabled);
                    }
                }
            }
        }
    }

    @Test void shouldCastOutIsTheSameRuleSeenFromInsideTheDungeon() {
        assertTrue(AlignmentRules.shouldCastOut(H, true, A, true), "locked Holy, standing in the Abyss");
        assertTrue(AlignmentRules.shouldCastOut(A, true, H, true), "locked Abyssal, standing in the Holy floors");
        assertFalse(AlignmentRules.shouldCastOut(H, true, H, true), "on their own road");
        assertFalse(AlignmentRules.shouldCastOut(H, false, A, true), "not locked yet");
        assertFalse(AlignmentRules.shouldCastOut(H, true, null, true), "in a gap between sectors nothing can be decided");
        assertFalse(AlignmentRules.shouldCastOut(H, true, A, false), "the lock is off");
        assertFalse(AlignmentRules.shouldCastOut(null, true, A, true), "a lock without a road");
    }

    // ------------------------------------------------------------------------------------------------ the owner's story, step by step
    @Test void theFirstFloorOnlyChoosesAndClearingItLocks() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertTrue(s.isDefault());
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), A, true), "a fresh player may use either road");
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), H, true));

        assertFalse(step(s, 0, 1, 1, 100), "arriving at Floor +1 does not lock");
        assertEquals(H, s.chosen());
        assertFalse(s.locked());
        assertEquals(1, s.deepestHoly());
        assertEquals(0, s.deepestAbyssal());
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), A, true), "before the lock the second road is still open");
        assertFalse(s.isDefault());

        assertTrue(step(s, 1, 2, 1, 250), "moving on from Floor +1 clears it and locks");
        assertEquals(H, s.chosen());
        assertTrue(s.locked());
        assertEquals(250L, s.lockedAt());
        assertEquals(2, s.deepestHoly());
        assertFalse(AlignmentRules.mayEnter(s.chosen(), s.locked(), A, true), "after the lock the second road is shut");
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), H, true), "their own road stays open");
        assertFalse(step(s, 2, 3, 1, 300), "going deeper once locked locks nothing new");
        assertEquals(250L, s.lockedAt(), "and does not move the lock time");
    }

    @Test void theAbyssalRoadMirrorsTheHolyOne() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertFalse(step(s, 0, -1, 1, 5));
        assertEquals(A, s.chosen());
        assertEquals(1, s.deepestAbyssal());
        assertTrue(step(s, -1, -2, 1, 6));
        assertTrue(s.locked());
        assertFalse(AlignmentRules.mayEnter(s.chosen(), s.locked(), H, true));
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), A, true));
        assertEquals(0, s.deepestHoly());
    }

    @Test void goingBackToFloorZeroNeverChangesAnything() {
        PlayerDungeonAlignment fresh = new PlayerDungeonAlignment();
        PlayerDungeonAlignment open = new PlayerDungeonAlignment();
        step(open, 0, 1, 1, 1);
        PlayerDungeonAlignment locked = new PlayerDungeonAlignment();
        step(locked, 0, 1, 1, 1);
        step(locked, 1, 2, 1, 9);
        for (PlayerDungeonAlignment s : new PlayerDungeonAlignment[] {fresh, open, locked}) {
            PlayerDungeonAlignment before = s.copy();
            assertFalse(AlignmentRules.onFloorEntered(s, 0), "entering Floor 0 reports no change");
            assertFalse(AlignmentRules.clearsPrevious(s, 2, 0), "retreating to Floor 0 clears nothing");
            assertFalse(AlignmentRules.clearsPrevious(s, 0, 1), "arriving from Floor 0 clears nothing");
            assertFalse(AlignmentRules.onFloorCleared(s, 0, 1, 77), "Floor 0 is not a floor of any road");
            assertFalse(AlignmentRules.isNewDeepest(s, 0));
            assertEquals(before, s, "the state is exactly as it was");
        }
        assertTrue(locked.locked(), "a retreat does not open the lock");
    }

    @Test void aFloorIsClearedOnlyOncePerPlayerAndRoad() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertFalse(AlignmentRules.clearsPrevious(s, 0, 1), "arriving from outside the dungeon clears nothing");
        AlignmentRules.onFloorEntered(s, 1);
        assertTrue(AlignmentRules.clearsPrevious(s, 1, 2), "+1 to +2 clears +1");
        AlignmentRules.onFloorEntered(s, 2);
        assertFalse(AlignmentRules.clearsPrevious(s, 2, 1), "stepping back down clears nothing");
        AlignmentRules.onFloorEntered(s, 1);
        assertFalse(AlignmentRules.clearsPrevious(s, 1, 2), "+1 was cleared already, walking it again is not a second clear");
        AlignmentRules.onFloorEntered(s, 2);
        assertTrue(AlignmentRules.clearsPrevious(s, 2, 3), "+2 to +3 clears +2 for the first time");
        assertFalse(AlignmentRules.clearsPrevious(s, 1, 1), "no move, no clear");
    }

    @Test void aJumpClearsOnlyTheFloorTheyLeft() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        AlignmentRules.onFloorEntered(s, 1);
        assertTrue(AlignmentRules.clearsPrevious(s, 1, 4), "an operator's jump from +1 to +4 leaves +1");
        AlignmentRules.onFloorEntered(s, 4);
        assertEquals(4, s.deepestHoly());
        assertFalse(AlignmentRules.clearsPrevious(s, 2, 3), "+2 and +3 were never reached after that: below the deepest, so no event");
        assertFalse(AlignmentRules.clearsPrevious(s, 1, -1), "from one road to the other is no clear");
        assertFalse(AlignmentRules.clearsPrevious(s, -1, 1));
    }

    @Test void theSectorBoundaryIsJustAnotherStep() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        AlignmentRules.onFloorEntered(s, 10);
        assertTrue(AlignmentRules.clearsPrevious(s, 10, 11), "the arena (+10) is cleared by walking into the next sector's Landing (+11)");
    }

    @Test void theLockCanWaitForADeeperFloor() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertFalse(step(s, 0, 1, 3, 1));
        assertFalse(step(s, 1, 2, 3, 2), "+1 cleared but the lock waits for floor 3");
        assertFalse(s.locked());
        assertFalse(step(s, 2, 3, 3, 3), "+2 cleared, still below 3");
        assertTrue(step(s, 3, 4, 3, 4), "+3 cleared: now it locks");
        assertEquals(4L, s.lockedAt());
        // a lockAtFloor of 0 or less reads as 1
        PlayerDungeonAlignment t = new PlayerDungeonAlignment();
        step(t, 0, 1, 0, 1);
        assertTrue(step(t, 1, 2, 0, 2));
    }

    // ------------------------------------------------------------------------------------------------ the loopholes
    @Test void aDipIntoTheOtherRoadDoesNotEscapeTheLock() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        step(s, 0, 1, 1, 1);                                    // steps onto +1 and retreats without moving on
        assertEquals(H, s.chosen());
        AlignmentRules.onFloorEntered(s, -1);                   // later takes the other road (still open)
        assertEquals(A, s.chosen(), "while open, the road follows the last floor entered");
        assertEquals(1, s.deepestHoly(), "the dip is remembered per road");
        assertEquals(1, s.deepestAbyssal());
        assertTrue(step(s, -1, -2, 1, 50), "clearing -1 locks them to the Abyssal road, it must not slip past the lock");
        assertEquals(A, s.chosen());
        assertTrue(s.locked());
        assertFalse(AlignmentRules.mayEnter(s.chosen(), s.locked(), H, true), "and now the Holy road is shut although they dipped into it first");
    }

    @Test void aLockedPlayerOnTheWrongRoadKeepsTheirLock() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        step(s, 0, 1, 1, 1);
        step(s, 1, 2, 1, 2);
        assertTrue(s.locked());
        assertTrue(AlignmentRules.onFloorEntered(s, -3), "an operator or a teleport puts them on the Abyssal road: the deepest is recorded");
        assertEquals(3, s.deepestAbyssal());
        assertEquals(H, s.chosen(), "but the road they are bound to does not change");
        assertTrue(s.locked());
        assertFalse(AlignmentRules.onFloorCleared(s, -3, 1, 99), "and clearing a floor there does not re-lock or re-aim the lock");
        assertEquals(H, s.chosen());
        assertEquals(2L, s.lockedAt());
    }

    @Test void aFloorOfTheOtherRoadDoesNotLockAPlayerWhoseRoadWasSetByHand() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertTrue(AlignmentRules.choose(s, H));
        assertFalse(AlignmentRules.onFloorCleared(s, -1, 1, 5), "an operator chose Holy: clearing an Abyssal floor is not a commitment");
        assertFalse(s.locked());
        assertTrue(AlignmentRules.onFloorCleared(s, 1, 1, 6), "clearing Holy is");
    }

    @Test void theLockOnlyEverClosesOnce() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertTrue(AlignmentRules.onFloorCleared(s, 1, 1, 10));
        assertFalse(AlignmentRules.onFloorCleared(s, 5, 1, 20), "already locked: no newly locked event");
        assertEquals(10L, s.lockedAt());
    }

    @Test void newDeepestIsTrueOnlyForARoadsNewRecord() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertTrue(AlignmentRules.isNewDeepest(s, 1));
        assertTrue(AlignmentRules.isNewDeepest(s, -1));
        AlignmentRules.onFloorEntered(s, 2);
        assertFalse(AlignmentRules.isNewDeepest(s, 1));
        assertFalse(AlignmentRules.isNewDeepest(s, 2));
        assertTrue(AlignmentRules.isNewDeepest(s, 3));
        assertTrue(AlignmentRules.isNewDeepest(s, -1), "the other road has its own record");
    }

    // ------------------------------------------------------------------------------------------------ reset and the operator's hands
    @Test void resetGivesEverythingBack() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        step(s, 0, 1, 1, 1);
        step(s, 1, 2, 1, 2);
        AlignmentRules.onFloorEntered(s, -4);
        AlignmentRules.reset(s);
        assertTrue(s.isDefault());
        assertNull(s.chosen());
        assertFalse(s.locked());
        assertEquals(0, s.deepestHoly());
        assertEquals(0, s.deepestAbyssal());
        assertEquals(0L, s.lockedAt());
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), A, true), "the second road is open again");
        assertEquals(new PlayerDungeonAlignment(), s);
    }

    @Test void theOperatorCanChooseLockAndUnlock() {
        PlayerDungeonAlignment s = new PlayerDungeonAlignment();
        assertFalse(AlignmentRules.lock(s, 5), "a lock needs a road");
        assertTrue(AlignmentRules.choose(s, A));
        assertFalse(AlignmentRules.choose(s, A), "choosing the same road changes nothing");
        assertFalse(s.locked(), "choosing does not lock");
        assertTrue(AlignmentRules.lock(s, 5));
        assertFalse(AlignmentRules.lock(s, 6), "already locked");
        assertEquals(5L, s.lockedAt());
        assertFalse(AlignmentRules.mayEnter(s.chosen(), s.locked(), H, true));
        assertTrue(AlignmentRules.choose(s, H), "an operator may re-aim a lock");
        assertTrue(s.locked());
        assertTrue(AlignmentRules.unlock(s));
        assertFalse(AlignmentRules.unlock(s), "already open");
        assertEquals(H, s.chosen(), "unlocking keeps the road");
        assertEquals(0L, s.lockedAt());
        assertTrue(AlignmentRules.mayEnter(s.chosen(), s.locked(), A, true));
        assertTrue(AlignmentRules.choose(s, null), "forgetting the road is allowed");
        assertNull(s.chosen());
    }

    // ------------------------------------------------------------------------------------------------ the save
    @Test void theFiveSavedValuesRebuildTheSameState() {
        PlayerDungeonAlignment fresh = new PlayerDungeonAlignment();
        PlayerDungeonAlignment open = new PlayerDungeonAlignment();
        step(open, 0, 1, 1, 1);
        PlayerDungeonAlignment holy = new PlayerDungeonAlignment();
        step(holy, 0, 1, 1, 1);
        step(holy, 1, 2, 1, 1234567890123L);
        AlignmentRules.onFloorEntered(holy, -7);
        PlayerDungeonAlignment abyssal = new PlayerDungeonAlignment();
        step(abyssal, 0, -1, 1, 1);
        step(abyssal, -1, -2, 1, 42);
        for (PlayerDungeonAlignment s : new PlayerDungeonAlignment[] {fresh, open, holy, abyssal}) {
            PlayerDungeonAlignment back = PlayerDungeonAlignment.of(s.chosenId(), s.locked(), s.deepestHoly(), s.deepestAbyssal(), s.lockedAt());
            assertEquals(s, back, "round trip of " + s);
            assertEquals(s, s.copy());
            assertEquals(s.hashCode(), back.hashCode());
        }
        assertEquals("", fresh.chosenId());
        assertEquals("holy", holy.chosenId());
        assertEquals("abyssal", abyssal.chosenId());
        assertTrue(fresh.isDefault() && !open.isDefault() && !holy.isDefault());
    }

    @Test void aDamagedSaveLoadsIntoAValidState() {
        assertNull(PlayerDungeonAlignment.of("sideways", false, 0, 0, 0).chosen(), "an unknown road is none");
        assertNull(PlayerDungeonAlignment.of(null, false, 0, 0, 0).chosen());
        assertEquals(H, PlayerDungeonAlignment.of("HOLY", false, 0, 0, 0).chosen(), "the id is read without regard to case");
        assertEquals(0, PlayerDungeonAlignment.of("holy", false, -5, -9, 0).deepestHoly(), "a negative distance is 0");
        assertEquals(0, PlayerDungeonAlignment.of("holy", false, -5, -9, 0).deepestAbyssal());
        PlayerDungeonAlignment noRoad = PlayerDungeonAlignment.of("", true, 3, 0, 99);
        assertFalse(noRoad.locked(), "a lock without a road is no lock, a damaged save must never trap anyone");
        assertEquals(0L, noRoad.lockedAt());
        PlayerDungeonAlignment open = PlayerDungeonAlignment.of("holy", false, 3, 0, 99);
        assertEquals(0L, open.lockedAt(), "a lock time without a lock is dropped");
        assertEquals(0L, PlayerDungeonAlignment.of("holy", true, 3, 0, -4).lockedAt(), "a negative time is 0");
        assertTrue(PlayerDungeonAlignment.of("holy", true, 3, 0, 99).locked());
        assertNotEquals(PlayerDungeonAlignment.of("holy", true, 3, 0, 99), PlayerDungeonAlignment.of("holy", false, 3, 0, 99));
    }

    @Test void equalityLooksAtEveryField() {
        PlayerDungeonAlignment base = PlayerDungeonAlignment.of("holy", true, 3, 2, 99);
        assertEquals(base, PlayerDungeonAlignment.of("holy", true, 3, 2, 99));
        assertNotEquals(base, PlayerDungeonAlignment.of("abyssal", true, 3, 2, 99));                       // the road
        assertNotEquals(base, PlayerDungeonAlignment.of("holy", false, 3, 2, 99));                         // the lock
        assertNotEquals(base, PlayerDungeonAlignment.of("holy", true, 4, 2, 99));                          // the Holy record
        assertNotEquals(base, PlayerDungeonAlignment.of("holy", true, 3, 3, 99));                          // the Abyssal record
        assertNotEquals(base, PlayerDungeonAlignment.of("holy", true, 3, 2, 100));                         // the time of the lock
        assertNotEquals(base, null);
    }

    @Test void deepestIsReadPerRoadAndNullHasNone() {
        PlayerDungeonAlignment s = PlayerDungeonAlignment.of("holy", false, 12, 3, 0);
        assertEquals(12, s.deepest(H));
        assertEquals(3, s.deepest(A));
        assertEquals(0, s.deepest(null));
    }
}
