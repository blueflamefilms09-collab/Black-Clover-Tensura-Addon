package com.newuniverse.nusmp.dungeon.floor.goliath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of the Goliath's rising: who wakes it, the eased climb, the shove and the cracking. */
class GoliathRiseTest {
    @Test
    void aPlayerNearTheSealAtItsHeightWakesIt() {
        assertTrue(GoliathRise.triggers(0, 0, 0));
        assertTrue(GoliathRise.triggers(18, 0, 0));
        assertFalse(GoliathRise.triggers(18.1, 0, 0));
        assertTrue(GoliathRise.triggers(10, 12, 10));
        assertFalse(GoliathRise.triggers(0, 12.1, 0), "a gallery's height is in reach, the cave far below is not");
        assertFalse(GoliathRise.triggers(0, -40, 0));
        assertFalse(GoliathRise.triggers(13, 0, 13), "18.4 blocks away along the floor");
    }

    @Test
    void theClimbIsEasedAndMonotonic() {
        assertEquals(0f, GoliathRise.progress(-5), 0f);
        assertEquals(0f, GoliathRise.progress(0), 0f);
        assertEquals(1f, GoliathRise.progress(GoliathRise.CLIMB_TICKS), 0f);
        assertEquals(1f, GoliathRise.progress(GoliathRise.CLIMB_TICKS + 50), 0f);
        assertEquals(0.5f, GoliathRise.progress(GoliathRise.CLIMB_TICKS / 2), 1e-5);
        float last = -1f;
        for (int t = 0; t <= GoliathRise.CLIMB_TICKS; t++) {
            float p = GoliathRise.progress(t);
            assertTrue(p >= last, "never goes back down");
            assertTrue(p - Math.max(0f, last) <= 0.02f, "no tick moves the body more than two percent of its height: " + t);
            last = p;
        }
        assertTrue(GoliathRise.progress(8) < 0.03f, "the stone gives slowly at first");
    }

    @Test
    void theShoveIsStrongestAtTheSealAndNeverMoreThanTheCap() {
        assertEquals(0.0, GoliathRise.shove(GoliathRise.SHOVE_RANGE), 0.0);
        assertEquals(0.0, GoliathRise.shove(20), 0.0);
        assertEquals(0.0, GoliathRise.shove(-1), 0.0);
        assertEquals(0.0, GoliathRise.shove(Double.NaN), 0.0);
        double prev = Double.MAX_VALUE;
        for (double d = 0; d < GoliathRise.SHOVE_RANGE; d += 0.25) {
            double k = GoliathRise.shove(d);
            assertTrue(k > 0 && k <= GoliathRise.SHOVE_MAX + 0.15 + 1e-9, "d=" + d);
            assertTrue(k <= prev);
            prev = k;
        }
    }

    @Test
    void theCrackingBuildsUpAndStaysInRange() {
        assertTrue(GoliathRise.crackBits(0) < GoliathRise.crackBits(GoliathRise.CRACK_TICKS));
        assertEquals(GoliathRise.crackBits(GoliathRise.CRACK_TICKS), GoliathRise.crackBits(GoliathRise.CRACK_TICKS + 99));
        assertEquals(GoliathRise.crackBits(0), GoliathRise.crackBits(-4));
        assertEquals(0.5f, GoliathRise.crackPitch(0), 1e-6);
        assertEquals(0.8f, GoliathRise.crackPitch(GoliathRise.CRACK_TICKS), 1e-6);
        assertEquals(60, GoliathRise.CRACK_TICKS, "three seconds");
        assertEquals(80, GoliathRise.CLIMB_TICKS);
    }

    @Test
    void theSynchedValueIsClamped() {
        assertEquals(1f, GoliathRise.clampRise(Float.NaN));
        assertEquals(0f, GoliathRise.clampRise(-3f));
        assertEquals(1f, GoliathRise.clampRise(7f));
        assertEquals(0.4f, GoliathRise.clampRise(0.4f));
    }
}
