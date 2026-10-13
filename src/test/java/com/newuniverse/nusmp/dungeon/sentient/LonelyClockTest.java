package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A born mob that no player has come within 96 blocks of for three minutes goes back into the walls. */
class LonelyClockTest {
    @Test
    void aPlayerNearEndsTheWait() {
        assertEquals(0, LonelyClock.next(2000, true, 100));
        assertEquals(100, LonelyClock.next(0, false, 100));
        assertEquals(300, LonelyClock.next(200, false, 100));
        assertEquals(200, LonelyClock.next(200, false, -50), "a pass never runs backwards");
    }

    @Test
    void threeMinutesAloneIsTheLimit() {
        assertEquals(3600, LonelyClock.ALONE_TICKS);
        int alone = 0, passes = 0;
        while (!LonelyClock.expired(alone)) {
            alone = LonelyClock.next(alone, false, 100);
            passes++;
        }
        assertEquals(36, passes);
        assertFalse(LonelyClock.expired(3599));
        assertTrue(LonelyClock.expired(3600));
    }

    @Test
    void aPlayerPassingByResetsIt() {
        int alone = 0;
        for (int i = 0; i < 30; i++) alone = LonelyClock.next(alone, false, 100);
        assertFalse(LonelyClock.expired(alone));
        alone = LonelyClock.next(alone, true, 100);
        for (int i = 0; i < 30; i++) alone = LonelyClock.next(alone, false, 100);
        assertFalse(LonelyClock.expired(alone));
    }
}
