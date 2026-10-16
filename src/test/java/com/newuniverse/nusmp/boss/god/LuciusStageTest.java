package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The three stages: what a fatal blow means, the one-refill-per-stage ids, and the virtual bar the power curve reads. */
class LuciusStageTest {
    @Test
    void aFreshFightIsStageOneAndAFatalBlowStartsTheNextStage() {
        LuciusStage s = new LuciusStage();
        assertEquals(1, s.stage());
        assertEquals(LuciusStage.Fall.NEXT_STAGE, s.onFatal(false));
        assertTrue(s.advance());
        assertEquals(2, s.stage());
        assertEquals(LuciusStage.Fall.NEXT_STAGE, s.onFatal(false));
        assertTrue(s.advance());
        assertTrue(s.last());
        assertEquals(LuciusStage.Fall.REAL_DEATH, s.onFatal(false), "the last stage's fatal blow is a real death");
        assertFalse(s.advance(), "there is no fourth stage");
        assertEquals(3, s.stage());
    }

    @Test
    void aRealKillEndsTheFightInAnyStage() {
        LuciusStage s = new LuciusStage();
        assertEquals(LuciusStage.Fall.REAL_DEATH, s.onFatal(true));
    }

    @Test
    void everyStageHasItsOwnRefillIdSoTheSameRefillCannotFireTwice() {
        assertEquals("luciusStage1", LuciusStage.refillMode(1));
        assertEquals("luciusStage2", LuciusStage.refillMode(2));
        assertNotEquals(LuciusStage.refillMode(1), LuciusStage.refillMode(2));
        assertEquals("luciusStage3", LuciusStage.refillMode(9), "clamped");
        assertEquals("luciusStage1", LuciusStage.refillMode(-4), "clamped");
    }

    @Test
    void theActFollowsTheStage() {
        assertEquals(1, LuciusStage.act(1));
        assertEquals(2, LuciusStage.act(2));
        assertEquals(3, LuciusStage.act(3));
        assertEquals(3, LuciusStage.act(7));
        assertEquals(1, LuciusStage.act(0));
    }

    @Test
    void theVirtualBarIsContinuousAcrossAStageChange() {
        float max = 4800f;
        assertEquals(3 * max, LuciusStage.virtualHealth(1, max, max));
        float endOfOne = LuciusStage.virtualHealth(1, 0f, max), startOfTwo = LuciusStage.virtualHealth(2, max, max);
        assertEquals(endOfOne, startOfTwo);
        assertEquals(LuciusStage.virtualHealth(2, 0f, max), LuciusStage.virtualHealth(3, max, max));
        assertEquals(0f, LuciusStage.virtualHealth(3, 0f, max));
        assertEquals(3 * max, LuciusStage.virtualMax(max));
    }

    @Test
    void overallFractionRunsFromOneToZeroAcrossAllStages() {
        float max = 100f;
        assertEquals(1f, LuciusStage.overallFraction(1, 100f, max), 1e-6f);
        assertEquals(2f / 3f, LuciusStage.overallFraction(1, 0f, max), 1e-6f);
        assertEquals(0.5f, LuciusStage.overallFraction(2, 50f, max), 1e-6f);
        assertEquals(0f, LuciusStage.overallFraction(3, 0f, max), 1e-6f);
        assertEquals(0f, LuciusStage.overallFraction(1, 5f, 0f), "no maximum, no fraction");
    }

    @Test
    void restoreMakesGarbageSafeAndResetStartsOver() {
        LuciusStage s = new LuciusStage();
        s.restore(99);
        assertEquals(3, s.stage());
        s.restore(-5);
        assertEquals(1, s.stage());
        s.restore(2);
        s.reset();
        assertEquals(1, s.stage());
    }

    @Test
    void labelsAndArsenals() {
        assertEquals("Stage 2/3", LuciusStage.label(2));
        assertEquals("Time and Soul", LuciusStage.arsenal(1));
        assertEquals("Gravity and Blood", LuciusStage.arsenal(2));
        assertTrue(LuciusStage.arsenal(3).contains("Space"));
    }
}
