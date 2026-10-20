package com.newuniverse.nusmp.labyrinth.mega.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code /dungeon tp} to a floor that does not exist yet must QUEUE the road's growth instead of failing, and must also end, with the reason, when the road can never reach the
 * floor. Those decisions are {@link TpRules#decide}, pure, so each case of the walk is pinned here: arrive when the floor stands, wait while the target or the newest floor is being
 * built, queue the first floor of an empty road, queue the next floor of a finished tail, and give up (failed floor, size limit, expansion off, no progress for three minutes, a
 * lost link) with the right reason.
 */
public class TpRulesTest {
    private static final long IDLE = 0L;

    /** A road of {@code tail + 1} finished floors (indices 0 to tail) and a target at {@code target}. */
    private static TpRules.View road(int tail, int target, long idle) {
        boolean targetExists = target <= tail;
        return new TpRules.View(false, tail, true, false, false, target, targetExists, targetExists, false, tail + 1, 1000, true, idle);
    }

    @Test
    public void aFloorThatStandsIsReached() {
        assertEquals(TpRules.Step.ARRIVE, TpRules.decide(road(10, 10, IDLE)));
        assertEquals(TpRules.Step.ARRIVE, TpRules.decide(road(10, 0, IDLE)));
        assertEquals(TpRules.Step.ARRIVE, TpRules.decide(road(10, 4, TpRules.GIVE_UP_TICKS * 10)), "a floor that stands is reached however long the road was idle");
    }

    @Test
    public void aFloorBeyondTheTailQueuesTheNextFloorOfTheRoad() {
        assertEquals(TpRules.Step.QUEUE_NEXT, TpRules.decide(road(10, 11, IDLE)));
        assertEquals(TpRules.Step.QUEUE_NEXT, TpRules.decide(road(10, 999, IDLE)));
        assertEquals(TpRules.Step.QUEUE_NEXT, TpRules.decide(road(0, 5, IDLE)));
    }

    @Test
    public void anEmptyRoadQueuesItsFirstFloor() {
        TpRules.View empty = new TpRules.View(true, -1, false, false, false, 7, false, false, false, 0, 1000, true, IDLE);
        assertEquals(TpRules.Step.QUEUE_FIRST, TpRules.decide(empty));
        TpRules.View noGrowth = new TpRules.View(true, -1, false, false, false, 7, false, false, false, 0, 1000, false, IDLE);
        assertEquals(TpRules.Step.FAIL_OFF, TpRules.decide(noGrowth));
        TpRules.View full = new TpRules.View(true, -1, false, false, false, 7, false, false, false, 1000, 1000, true, IDLE);
        assertEquals(TpRules.Step.FAIL_LIMIT, TpRules.decide(full));
    }

    @Test
    public void whileSomethingIsBeingBuiltTheRequestWaits() {
        TpRules.View tailBuilding = new TpRules.View(false, 10, false, false, false, 20, false, false, false, 11, 1000, true, IDLE);
        assertEquals(TpRules.Step.WAIT, TpRules.decide(tailBuilding), "the newest floor is still generating");
        TpRules.View nextQueued = new TpRules.View(false, 10, true, false, true, 20, false, false, false, 12, 1000, true, IDLE);
        assertEquals(TpRules.Step.WAIT, TpRules.decide(nextQueued), "the floor after the tail is already queued");
        TpRules.View targetBuilding = new TpRules.View(false, 20, false, false, false, 20, true, false, false, 21, 1000, true, IDLE);
        assertEquals(TpRules.Step.WAIT, TpRules.decide(targetBuilding), "the target itself is being generated");
    }

    @Test
    public void waitingEndsAfterThreeMinutesWithoutProgress() {
        long limit = TpRules.GIVE_UP_TICKS;
        assertEquals(20L * 60L * 3L, limit);
        TpRules.View building = new TpRules.View(false, 10, false, false, false, 20, false, false, false, 11, 1000, true, limit);
        assertEquals(TpRules.Step.WAIT, TpRules.decide(building), "exactly at the limit it still waits");
        TpRules.View stuck = new TpRules.View(false, 10, false, false, false, 20, false, false, false, 11, 1000, true, limit + 1);
        assertEquals(TpRules.Step.FAIL_STUCK, TpRules.decide(stuck));
        TpRules.View targetStuck = new TpRules.View(false, 20, false, false, false, 20, true, false, false, 21, 1000, true, limit + 1);
        assertEquals(TpRules.Step.FAIL_STUCK, TpRules.decide(targetStuck));
        TpRules.View linkLost = new TpRules.View(false, 10, true, false, true, 20, false, false, false, 12, 1000, true, limit + 1);
        assertEquals(TpRules.Step.FAIL_STUCK, TpRules.decide(linkLost));
    }

    @Test
    public void aFailedFloorOnTheWayEndsTheRequestWithThatReason() {
        TpRules.View tailFailed = new TpRules.View(false, 10, false, true, false, 20, false, false, false, 11, 1000, true, IDLE);
        assertEquals(TpRules.Step.FAIL_FAILED, TpRules.decide(tailFailed));
        TpRules.View targetFailed = new TpRules.View(false, 20, false, false, false, 20, true, false, true, 21, 1000, true, IDLE);
        assertEquals(TpRules.Step.FAIL_FAILED, TpRules.decide(targetFailed));
    }

    @Test
    public void theSizeLimitAndTheSwitchStopTheGrowth() {
        TpRules.View atLimit = new TpRules.View(false, 10, true, false, false, 20, false, false, false, 1000, 1000, true, IDLE);
        assertEquals(TpRules.Step.FAIL_LIMIT, TpRules.decide(atLimit));
        TpRules.View justUnder = new TpRules.View(false, 10, true, false, false, 20, false, false, false, 999, 1000, true, IDLE);
        assertEquals(TpRules.Step.QUEUE_NEXT, TpRules.decide(justUnder));
        TpRules.View off = new TpRules.View(false, 10, true, false, false, 20, false, false, false, 11, 1000, false, IDLE);
        assertEquals(TpRules.Step.FAIL_OFF, TpRules.decide(off));
        TpRules.View offButStands = new TpRules.View(false, 10, true, false, false, 5, true, true, false, 11, 1000, false, IDLE);
        assertEquals(TpRules.Step.ARRIVE, TpRules.decide(offButStands), "a floor that stands is reached even when the dungeon cannot grow");
        TpRules.View fullButStands = new TpRules.View(false, 10, true, false, false, 5, true, true, false, 1000, 1000, true, IDLE);
        assertEquals(TpRules.Step.ARRIVE, TpRules.decide(fullButStands));
    }

    @Test
    public void aTargetBehindTheTailThatDoesNotExistIsALostLink() {
        TpRules.View lost = new TpRules.View(false, 10, true, false, false, 4, false, false, false, 11, 1000, true, IDLE);
        assertEquals(TpRules.Step.FAIL_STUCK, TpRules.decide(lost), "floor 4 should stand under a tail at 10; asking the generator for more would not help");
    }

    @Test
    public void onlyTheFailuresAreFailures() {
        for (TpRules.Step s : TpRules.Step.values()) {
            boolean expected = s.name().startsWith("FAIL_");
            assertEquals(expected, s.failed(), s.name());
        }
        assertFalse(TpRules.Step.ARRIVE.failed());
        assertFalse(TpRules.Step.WAIT.failed());
        assertTrue(TpRules.Step.FAIL_STUCK.failed());
    }

    @Test
    public void theRemainingFloorsCountFromTheTailOrFromNothing() {
        assertEquals(0, TpRules.remaining(false, 10, 10));
        assertEquals(0, TpRules.remaining(false, 10, 3));
        assertEquals(5, TpRules.remaining(false, 10, 15));
        assertEquals(16, TpRules.remaining(true, -1, 15), "an empty road: floors 0 to 15 are all still to build");
        assertEquals(16, TpRules.remaining(true, 99, 15), "the tail is ignored when the road is empty");
    }

    @Test
    public void aWalkOfOneFloorAtATimeEndsOnTheTargetAndNeverAsksTwiceForTheSameFloor() {
        // a road of three floors (0..2) and a target at 6: the walk queues 3, 4, 5, 6 in turn, one at a time, and arrives
        int tail = 2, target = 6, queued = 0;
        boolean building = false;
        for (int round = 0; round < 100; round++) {
            boolean targetExists = target <= tail;
            TpRules.View v = new TpRules.View(false, tail, !building, false, false, target, targetExists, targetExists && !building, false, tail + 1, 1000, true, IDLE);
            TpRules.Step step = TpRules.decide(v);
            if (step == TpRules.Step.ARRIVE) {
                assertEquals(4, queued, "floors 3 to 6 were queued once each");
                return;
            }
            if (step == TpRules.Step.QUEUE_NEXT) {
                assertFalse(building, "the planner never queues while a floor is building");
                tail++;
                queued++;
                building = true;
            } else {
                assertEquals(TpRules.Step.WAIT, step);
                building = false;                                       // the generator finished the floor it was working on
            }
        }
        assertTrue(false, "the walk did not end");
    }
}
