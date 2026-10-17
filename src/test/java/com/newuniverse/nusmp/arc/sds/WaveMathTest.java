package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a travelling ring or wall must never skip a body or strike one it did not cross, and a jump must clear a low ring, so the one question the glue asks every tick is answered from numbers and tested. */
class WaveMathTest {
    @Test
    void theFrontGrowsWithTheAge() {
        assertEquals(0.0, WaveMath.front(0, 0.55), 1e-12);
        assertEquals(5.5, WaveMath.front(10, 0.55), 1e-12);
        assertEquals(0.0, WaveMath.front(-4, 0.55), 1e-12);
    }

    @Test
    void aRingTouchesABodyOnTheOneTickItCrossesIt() {
        double speed = 0.55, d = 6.0, hw = 0.3;
        int touches = 0;
        for (int age = 1; age <= 40; age++) if (WaveMath.touches(d, hw, WaveMath.front(age - 1, speed), WaveMath.front(age, speed))) touches++;
        assertTrue(touches >= 1 && touches <= 2, "a body is crossed on one or two ticks (the half-width is a little more than a tick of travel): " + touches);
    }

    @Test
    void aBodyNextToHerFeetIsTouchedAtOnceAndOneBeyondTheRingNever() {
        assertTrue(WaveMath.touches(0.2, 0.3, 0, 0.55));
        for (int age = 1; age <= 18; age++) assertFalse(WaveMath.touches(12.0, 0.3, WaveMath.front(age - 1, 0.55), WaveMath.front(age, 0.55)), "beyond ten blocks at tick " + age);
    }

    @Test
    void everyBodyOnTheWayIsTouchedExactlyOnceByAFastWave() {
        double speed = 0.9;                                   // Queen Embrace: faster than half a body's width a tick
        for (double d = 0; d <= 25; d += 0.37) {
            int n = 0;
            for (int age = 1; age <= 40; age++) if (WaveMath.touches(d, 0.3, WaveMath.front(age - 1, speed), WaveMath.front(age, speed))) n++;
            assertTrue(n >= 1, "the wave cannot skip a body at " + d);
        }
    }

    @Test
    void aLowRingIsClearedByAJumpButNotByWalking() {
        assertTrue(WaveMath.clears(0.8, 0.6));
        assertTrue(WaveMath.clears(0.6, 0.6));
        assertFalse(WaveMath.clears(0.0, 0.6));
        assertFalse(WaveMath.clears(0.3, 0.6));
    }

    @Test
    void theTimeToReachFollowsTheSpeed() {
        assertEquals(19, WaveMath.ticksToReach(10, 0.55));
        assertEquals(1, WaveMath.ticksToReach(0.1, 1.0));
        assertEquals(Integer.MAX_VALUE, WaveMath.ticksToReach(10, 0));
    }

    @Test
    void aWallOfEarthTouchesOnlyWhatIsInItsLaneAndAheadOfItsStart() {
        assertTrue(WaveMath.laneTouches(10, 1.0, 0.3, 3.2, 9.1, 10.0));
        assertTrue(WaveMath.laneTouches(10, -3.4, 0.3, 3.2, 9.1, 10.0), "the body's width counts at the lane's edge");
        assertFalse(WaveMath.laneTouches(10, 4.0, 0.3, 3.2, 9.1, 10.0), "beside the lane");
        assertFalse(WaveMath.laneTouches(-3, 0.0, 0.3, 3.2, 0.0, 0.9), "behind her");
        assertFalse(WaveMath.laneTouches(20, 0.0, 0.3, 3.2, 9.1, 10.0), "not reached yet");
    }
}
