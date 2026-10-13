package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The crack of a wall birth runs through the ten vanilla stages, only ever rising, and is redrawn every fourth tick. */
class BirthTimingTest {
    @Test
    void theCrackRisesFromZeroToNineAndNeverFalls() {
        for (int delay : new int[]{1, 20, 33, 40, 70, 99, 120, 500}) {
            int last = -1;
            for (int age = 0; age <= BirthTiming.clamp(delay); age++) {
                int s = BirthTiming.stage(age, delay);
                assertTrue(s >= 0 && s <= 9, "stage " + s);
                assertTrue(s >= last, "monotonic at age " + age + " delay " + delay);
                last = s;
            }
            assertEquals(0, BirthTiming.stage(0, delay));
            assertEquals(9, BirthTiming.stage(BirthTiming.clamp(delay) - 1, delay), "the last tick before the wall gives way shows stage 9");
        }
    }

    @Test
    void theCrackGrowsInStepWithTheDelay() {
        for (int delay : new int[]{20, 40, 70, 100, 120}) {
            for (int age = 0; age < delay; age++) assertEquals((int) Math.min(9, Math.floor(10.0 * age / delay)), BirthTiming.stage(age, delay), "age " + age + " of " + delay);
        }
        assertEquals(4, BirthTiming.stage(49, 100));
        assertEquals(5, BirthTiming.stage(50, 100));
        assertEquals(9, BirthTiming.stage(90, 100));
        assertEquals(8, BirthTiming.stage(89, 100));
    }

    @Test
    void everyStageIsShownAtSomePoint() {
        boolean[] shown = new boolean[10];
        for (int age = 0; age < 70; age += BirthTiming.CRACK_EVERY) shown[BirthTiming.stage(age, 70)] = true;
        for (int s = 0; s < 10; s++) assertTrue(shown[s], "stage " + s + " is drawn on a 70 tick birth");
    }

    @Test
    void delaysAreHeldBetweenTwentyAndOneTwenty() {
        assertEquals(20, BirthTiming.clamp(-5));
        assertEquals(20, BirthTiming.clamp(0));
        assertEquals(70, BirthTiming.clamp(70));
        assertEquals(120, BirthTiming.clamp(9999));
    }

    @Test
    void theCrackIsRedrawnEveryFourthTick() {
        int n = 0;
        for (int age = 0; age < 40; age++) if (BirthTiming.crackDue(age)) n++;
        assertEquals(10, n);
        assertTrue(BirthTiming.crackDue(0));
        assertFalse(BirthTiming.crackDue(3));
        assertFalse(BirthTiming.crackDue(-4));
    }

    @Test
    void theWallGivesWayWhenTheDelayHasRunOut() {
        assertFalse(BirthTiming.done(69, 70));
        assertTrue(BirthTiming.done(70, 70));
        assertFalse(BirthTiming.done(10, 5), "a delay below the shortest is raised to it: 20 ticks");
        assertFalse(BirthTiming.done(19, 5));
        assertTrue(BirthTiming.done(20, 5));
    }
}
