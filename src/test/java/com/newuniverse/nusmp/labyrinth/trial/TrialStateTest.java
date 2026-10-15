package com.newuniverse.nusmp.labyrinth.trial;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.labyrinth.trial.TrialState.*;
import static org.junit.jupiter.api.Assertions.*;

/** Every transition of the trial's state machine. */
class TrialStateTest {
    private static final int RESET = 1200;

    @Test
    void idleWaitsForAPlayerWhoIsNotInADoorway() {
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, false, false, false, false, 0, 0, RESET));
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, true, true, false, false, 0, 0, RESET), "nobody is sealed inside a wall");
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(IDLE, true, false, false, false, 0, 0, RESET));
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, false, true, false, false, 0, 0, RESET));
    }

    @Test
    void aChamberWithDoorsActivatesOnlyOnceArmedByADoorway() {
        // hasRunes = true: being inside is not enough, someone must have come through a door
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, true, false, true, false, 0, 0, RESET), "arrived by the stair: not armed");
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, true, true, true, true, 0, 0, RESET), "in the doorway now: armed, but wait");
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(IDLE, true, false, true, true, 0, 0, RESET), "through the door and inside");
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, false, false, true, true, 0, 0, RESET), "armed but nobody inside");
        // no runes = no doors: the plain rule, armed or not
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(IDLE, true, false, false, false, 0, 0, RESET));
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(IDLE, true, false, false, true, 0, 0, RESET));
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, true, true, false, true, 0, 0, RESET), "the doorway rule still holds without arming");
    }

    @Test
    void armingNeverMattersOutsideIdle() {
        assertEquals(new Step(ACTIVE, Action.NONE), step(ACTIVE, true, false, true, false, 3, 0, RESET));
        assertEquals(new Step(CLEARED, Action.CLEAR), step(ACTIVE, true, false, true, false, 0, 0, RESET));
        assertEquals(new Step(IDLE, Action.RESET), step(ACTIVE, false, false, true, true, 3, RESET, RESET));
        assertEquals(new Step(CLEARED, Action.NONE), step(CLEARED, true, false, true, true, 0, 0, RESET));
    }

    @Test
    void idleIgnoresStaleCountersAndMobs() {
        assertEquals(new Step(IDLE, Action.NONE), step(IDLE, false, false, false, false, 5, 99999, RESET));
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(IDLE, true, false, false, false, 5, 99999, RESET));
    }

    @Test
    void activeClearsWhenEveryMobIsDead() {
        assertEquals(new Step(CLEARED, Action.CLEAR), step(ACTIVE, true, false, false, false, 0, 0, RESET));
        assertEquals(new Step(CLEARED, Action.CLEAR), step(ACTIVE, false, false, false, false, 0, 0, RESET), "even with nobody inside: the room opens");
        assertEquals(new Step(CLEARED, Action.CLEAR), step(ACTIVE, false, false, false, false, 0, 99999, RESET), "a clear beats a reset");
        assertEquals(new Step(CLEARED, Action.CLEAR), step(ACTIVE, true, true, false, false, -1, 0, RESET));
    }

    @Test
    void activeHoldsWhileMobsLiveAndPlayersStay() {
        assertEquals(new Step(ACTIVE, Action.NONE), step(ACTIVE, true, false, false, false, 3, 0, RESET));
        assertEquals(new Step(ACTIVE, Action.NONE), step(ACTIVE, true, true, false, false, 3, 0, RESET), "the doorway rule only matters before sealing");
        assertEquals(new Step(ACTIVE, Action.NONE), step(ACTIVE, true, false, false, false, 1, 99999, RESET), "a player inside means the counter is stale, never a reset");
    }

    @Test
    void activeResetsOnlyAfterTheRoomHasBeenEmptyLongEnough() {
        assertEquals(new Step(ACTIVE, Action.NONE), step(ACTIVE, false, false, false, false, 3, 0, RESET));
        assertEquals(new Step(ACTIVE, Action.NONE), step(ACTIVE, false, false, false, false, 3, RESET - 1, RESET));
        assertEquals(new Step(IDLE, Action.RESET), step(ACTIVE, false, false, false, false, 3, RESET, RESET));
        assertEquals(new Step(IDLE, Action.RESET), step(ACTIVE, false, false, false, false, 3, RESET * 10, RESET));
    }

    @Test
    void clearedIsForever() {
        assertEquals(new Step(CLEARED, Action.NONE), step(CLEARED, true, false, false, false, 0, 0, RESET));
        assertEquals(new Step(CLEARED, Action.NONE), step(CLEARED, false, false, false, false, 7, 99999, RESET));
        assertEquals(new Step(CLEARED, Action.NONE), step(CLEARED, true, true, false, false, 0, 0, RESET));
    }

    @Test
    void aFullTrialRunsIdleActiveCleared() {
        TrialState s = IDLE;
        Step a = step(s, true, false, false, false, 0, 0, RESET);
        assertEquals(Action.ACTIVATE, a.action());
        s = a.next();
        Step b = step(s, true, false, false, false, 4, 0, RESET);
        assertEquals(new Step(ACTIVE, Action.NONE), b);
        Step c = step(b.next(), true, false, false, false, 0, 0, RESET);
        assertEquals(new Step(CLEARED, Action.CLEAR), c);
        assertEquals(new Step(CLEARED, Action.NONE), step(c.next(), false, false, false, false, 0, RESET * 2, RESET));
    }

    @Test
    void anAbandonedTrialCanBeFoughtAgain() {
        Step r = step(ACTIVE, false, false, false, false, 2, RESET, RESET);
        assertEquals(Action.RESET, r.action());
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(r.next(), true, false, false, false, 0, 0, RESET));
        // with doors, the block entity clears the armed flag on a reset: the next party must come through a door again
        assertEquals(new Step(IDLE, Action.NONE), step(r.next(), true, false, true, false, 0, 0, RESET));
        assertEquals(new Step(ACTIVE, Action.ACTIVATE), step(r.next(), true, false, true, true, 0, 0, RESET));
    }

    @Test
    void aTrialThatSpawnedNothingOpensAtOnce() {
        assertEquals(new Step(CLEARED, Action.CLEAR), step(ACTIVE, true, false, false, false, 0, 0, RESET));
    }

    @Test
    void namesRoundTripAndUnknownIsIdle() {
        for (TrialState s : values()) assertEquals(s, byName(s.name()));
        assertEquals(ACTIVE, byName("active"));
        assertEquals(IDLE, byName("nonsense"));
        assertEquals(IDLE, byName(null));
        assertEquals(IDLE, byName(""));
    }
}
