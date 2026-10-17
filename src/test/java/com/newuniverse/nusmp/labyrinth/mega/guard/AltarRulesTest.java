package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.guard.AltarRules.Act;
import com.newuniverse.nusmp.labyrinth.mega.guard.AltarRules.Look;
import com.newuniverse.nusmp.labyrinth.mega.guard.AltarRules.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Guardian Altar's machine (IDLE, ACTIVE, DONE) decides when the boss appears, when a fight is abandoned or reset and when it counts as won, and a
 * wrong branch there is a boss that never comes, a vanished boss counted as a victory or a core forged without a kill; this test walks every branch of
 * {@link AltarRules} with no game running.
 */
class AltarRulesTest {
    private static final long ABANDON = AltarRules.abandonTicks(120);

    /** A look with sensible defaults, tweaked per test. */
    private static Look look(State s, boolean near, boolean wide, boolean found, boolean dead, boolean reported, long now, long since, long idle) {
        return new Look(s, near, wide, true, found, dead, reported, true, true, now, since, idle, ABANDON);
    }

    @Test
    void anIdleAltarSummonsWhenAPlayerComesCloseAndTheSectorIsReady() {
        assertEquals(Act.SUMMON, AltarRules.decide(look(State.IDLE, true, true, false, false, false, 100, 0, -1)));
        assertEquals(Act.NONE, AltarRules.decide(look(State.IDLE, false, true, false, false, false, 100, 0, -1)), "close means within the approach radius, not the wide one");
        assertEquals(Act.NONE, AltarRules.decide(look(State.IDLE, false, false, false, false, false, 100, 0, -1)));
        Look generating = new Look(State.IDLE, true, true, false, false, false, false, true, true, 100, 0, -1, ABANDON);
        assertEquals(Act.NONE, AltarRules.decide(generating), "a sector that is still being built hosts no fight");
    }

    @Test
    void aDeadBossFinishesTheFight() {
        assertEquals(Act.FINISH, AltarRules.decide(look(State.ACTIVE, true, true, true, true, false, 500, 100, 400)));
        assertEquals(Act.FINISH, AltarRules.decide(look(State.ACTIVE, true, true, false, false, true, 500, 100, 400)), "the Guardian reported its death and is already gone");
        assertEquals(Act.FINISH, AltarRules.decide(look(State.ACTIVE, false, false, true, true, false, 500, 100, 0)), "a death counts even when nobody is near any more");
    }

    @Test
    void aLiveBossKeepsTheFightGoingWhileTheyAreNear() {
        assertEquals(Act.NONE, AltarRules.decide(look(State.ACTIVE, true, true, true, false, false, 500, 100, 500)));
        assertEquals(Act.NONE, AltarRules.decide(look(State.ACTIVE, false, true, true, false, false, 500, 100, 500)), "wide radius still counts as present");
    }

    @Test
    void aPartyThatLeavesForTwoMinutesAbandonsTheFight() {
        long idle = 1000;
        assertEquals(Act.NONE, AltarRules.decide(look(State.ACTIVE, false, false, true, false, false, idle + ABANDON - 1, 100, idle)), "one tick short");
        assertEquals(Act.RESET, AltarRules.decide(look(State.ACTIVE, false, false, true, false, false, idle + ABANDON, 100, idle)));
        assertEquals(Act.NONE, AltarRules.decide(look(State.ACTIVE, false, true, true, false, false, idle + ABANDON * 3, 100, idle)), "someone within the wide radius: not abandoned");
        assertEquals(Act.NONE, AltarRules.decide(look(State.ACTIVE, false, false, true, false, false, idle + ABANDON * 3, 100, -1)), "never seen anybody: the clock has not started");
    }

    @Test
    void aBossThatVanishedWithoutDyingIsAResetNeverAVictory() {
        // not found, past the grace, its last spot loaded: it is gone, start over (the Peak Altar would finish here)
        assertEquals(Act.RESET, AltarRules.decide(look(State.ACTIVE, true, true, false, false, false, 100 + AltarRules.SPAWN_GRACE_TICKS + 1, 100, 100)));
        // within the grace: it may simply not have joined the world yet
        assertEquals(Act.NONE, AltarRules.decide(look(State.ACTIVE, true, true, false, false, false, 100 + AltarRules.SPAWN_GRACE_TICKS, 100, 100)));
        // its chunk is unloaded: proves nothing
        Look unloaded = new Look(State.ACTIVE, true, true, true, false, false, false, true, false, 10_000, 100, 10_000, ABANDON);
        assertEquals(Act.NONE, AltarRules.decide(unloaded));
        // no last position known: nothing to wait for
        Look unknown = new Look(State.ACTIVE, true, true, true, false, false, false, false, false, 10_000, 100, 10_000, ABANDON);
        assertEquals(Act.RESET, AltarRules.decide(unknown));
    }

    @Test
    void aSpentAltarDoesNothingMore() {
        assertEquals(Act.NONE, AltarRules.decide(look(State.DONE, true, true, false, false, false, 9999, 0, 0)));
        assertEquals(Act.NONE, AltarRules.decide(look(State.DONE, true, true, true, true, true, 9999, 0, 0)));
    }

    @Test
    void theIdleClockOnlyAdvancesWhileSomeoneIsNear() {
        assertEquals(500L, AltarRules.idleSince(100L, true, 500L));
        assertEquals(100L, AltarRules.idleSince(100L, false, 500L));
        assertEquals(-1L, AltarRules.idleSince(-1L, false, 500L));
    }

    @Test
    void anActiveAltarWithoutABossIdStartsOver() {
        assertEquals(State.IDLE, AltarRules.sanitize(State.ACTIVE, false));
        assertEquals(State.ACTIVE, AltarRules.sanitize(State.ACTIVE, true));
        assertEquals(State.DONE, AltarRules.sanitize(State.DONE, false));
        assertEquals(State.IDLE, AltarRules.sanitize(State.IDLE, false));
    }

    @Test
    void theAbandonTimeIsAtLeastOnePoll() {
        assertEquals(2400L, AltarRules.abandonTicks(120));
        assertEquals(AltarRules.POLL_TICKS, AltarRules.abandonTicks(0));
        assertEquals(AltarRules.POLL_TICKS, AltarRules.abandonTicks(-5));
    }
}
