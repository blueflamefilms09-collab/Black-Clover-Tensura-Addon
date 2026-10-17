package com.newuniverse.nusmp.arc.arena;

import org.junit.jupiter.api.Test;

import static com.newuniverse.nusmp.arc.arena.ChronicleSigilRules.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the sigil carries a party into a fight; these tests pin who may use it where (the Hub near the lectern to go, an arena to come home), the refusals (a blow
 * in the last five seconds, no boss, a fight already running, the arenas switched off) and the two channel times.
 */
class ChronicleSigilRulesTest {
    private static final long NEVER = 1_000_000L;

    @Test
    void atTheLecternWithABossAndNoRecentBlowTheSigilGoes() {
        Decision d = decide(true, Place.HUB, true, false, NEVER, true);
        assertTrue(d.allowed());
        assertEquals(Action.ENTER, d.action());
        assertEquals(ENTER_TICKS, d.channelTicks());
    }

    @Test
    void awayFromTheLecternOrElsewhereItDoesNothing() {
        assertEquals(Denial.TOO_FAR, decide(true, Place.HUB, false, false, NEVER, true).denial());
        assertEquals(Denial.WRONG_PLACE, decide(true, Place.ELSEWHERE, true, false, NEVER, true).denial());
        assertNull(decide(true, Place.ELSEWHERE, true, false, NEVER, true).action());
    }

    @Test
    void aRecentBlowStopsItAndTheWindowEndsAtTheLimit() {
        assertEquals(Denial.RECENTLY_HURT, decide(true, Place.HUB, true, false, 0, true).denial());
        assertEquals(Denial.RECENTLY_HURT, decide(true, Place.HUB, true, false, HURT_WINDOW - 1, true).denial());
        assertTrue(decide(true, Place.HUB, true, false, HURT_WINDOW, true).allowed());
    }

    @Test
    void withoutABossOrWithAFightUnderWayItRefuses() {
        assertEquals(Denial.NOTHING_TO_FIGHT, decide(true, Place.HUB, true, false, NEVER, false).denial());
        assertEquals(Denial.FIGHT_IN_PROGRESS, decide(true, Place.HUB, true, true, NEVER, true).denial());
    }

    @Test
    void anArenaThatIsRestingIsNotEnteredAndAFightOutranksTheRest() {
        assertEquals(Denial.RESTING, decide(true, Place.HUB, true, false, NEVER, true, true).denial());
        assertEquals(Denial.FIGHT_IN_PROGRESS, decide(true, Place.HUB, true, true, NEVER, true, true).denial(), "a running fight is the more urgent word");
        assertTrue(decide(true, Place.HUB, true, false, NEVER, true, false).allowed());
        assertEquals(Action.LEAVE, decide(true, Place.ARENA, false, false, NEVER, false, true).action(), "resting never keeps anybody in");
    }

    @Test
    void insideAnArenaItGoesHomeAndTakesLongerWhileTheFightRuns() {
        Decision quiet = decide(true, Place.ARENA, false, false, NEVER, false);
        assertEquals(Action.LEAVE, quiet.action());
        assertEquals(LEAVE_TICKS, quiet.channelTicks());
        Decision fight = decide(true, Place.ARENA, false, true, 0, true);
        assertEquals(Action.LEAVE, fight.action(), "leaving is never refused for a blow: it is the way out");
        assertEquals(LEAVE_FIGHT_TICKS, fight.channelTicks());
        assertTrue(LEAVE_FIGHT_TICKS > LEAVE_TICKS);
    }

    @Test
    void switchedOffMeansNothingWorksAnywhere() {
        for (Place p : Place.values()) {
            Decision d = decide(false, p, true, false, NEVER, true);
            assertEquals(Denial.SWITCHED_OFF, d.denial(), p.name());
            assertFalse(d.allowed());
        }
    }

    @Test
    void theTimingsAreSane() {
        assertTrue(ENTER_TICKS >= 40 && ENTER_TICKS <= 100);
        assertTrue(COOLDOWN_TICKS >= 200);
        assertTrue(HURT_WINDOW >= 60);
    }
}
