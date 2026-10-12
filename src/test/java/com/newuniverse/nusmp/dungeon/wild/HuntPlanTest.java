package com.newuniverse.nusmp.dungeon.wild;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** One hunt of the Living Dungeon's wildlife as a state machine: chase, strike with a cooldown, finish, give up. */
class HuntPlanTest {
    @Test
    void chasesUntilInReachThenStrikesOnACooldown() {
        HuntPlan p = new HuntPlan();
        assertEquals(HuntPlan.Act.CHASE, p.step(2, 100.0, false, true, false));
        assertEquals(HuntPlan.Act.STRIKE, p.step(2, 1.0, true, true, false));
        for (int t = 2; t < HuntPlan.COOLDOWN; t += 2) assertEquals(HuntPlan.Act.HOLD, p.step(2, 1.0, true, true, false), "tick " + t);
        assertEquals(HuntPlan.Act.STRIKE, p.step(2, 1.0, true, true, false));
        assertEquals(2, p.strikes());
    }

    @Test
    void endsWhenThePreyIsGone() {
        HuntPlan p = new HuntPlan();
        p.step(2, 1.0, true, true, false);
        assertEquals(HuntPlan.Act.DONE, p.step(2, 0.0, false, false, false));
    }

    @Test
    void givesUpWhenFullFarTooLongOrLost() {
        assertEquals(HuntPlan.Act.GIVE_UP, new HuntPlan().step(2, 1.0, true, true, true), "full");
        assertEquals(HuntPlan.Act.GIVE_UP, new HuntPlan().step(2, HuntPlan.LOST_DISTANCE_SQ + 1, false, true, false), "got away");
        HuntPlan old = new HuntPlan();
        HuntPlan.Act last = HuntPlan.Act.CHASE;
        for (int t = 0; t <= HuntPlan.MAX_AGE + 2; t += 2) last = old.step(2, 100.0, false, true, false);
        assertEquals(HuntPlan.Act.GIVE_UP, last, "too long");
        HuntPlan stuck = new HuntPlan();
        for (int i = 0; i < HuntPlan.MAX_NO_PATH; i++) stuck.pathResult(false);
        assertEquals(HuntPlan.Act.GIVE_UP, stuck.step(2, 100.0, false, true, false), "no way to it");
    }

    @Test
    void aPathThatIsFoundForgivesTheFailures() {
        HuntPlan p = new HuntPlan();
        p.pathResult(false);
        p.pathResult(false);
        p.pathResult(true);
        p.pathResult(false);
        assertEquals(HuntPlan.Act.CHASE, p.step(2, 100.0, false, true, false));
    }

    @Test
    void pathRequestsComeEveryTenTicks() {
        HuntPlan p = new HuntPlan();
        assertTrue(p.wantsPath(), "the first one at once");
        int asked = 0;
        for (int t = 0; t < 100; t += 2) {
            p.step(2, 100.0, false, true, false);
            if (p.wantsPath()) asked++;
        }
        assertTrue(asked >= 9 && asked <= 11, "about one per ten ticks: " + asked);
    }
}
