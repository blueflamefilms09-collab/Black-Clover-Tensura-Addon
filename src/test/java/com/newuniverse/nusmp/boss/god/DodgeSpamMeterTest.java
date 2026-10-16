package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Three dodges in six seconds is spam; fewer, or slower, is not. */
class DodgeSpamMeterTest {
    @Test
    void threeInSixSecondsFires() {
        DodgeSpamMeter m = new DodgeSpamMeter();
        assertFalse(m.record(0));
        assertFalse(m.record(40));
        assertTrue(m.record(100));
    }

    @Test
    void theSixSecondWindowIsOneHundredTwentyTicks() {
        DodgeSpamMeter m = new DodgeSpamMeter();
        m.record(0);
        m.record(60);
        assertTrue(m.record(120), "the first is exactly 120 ticks old: still inside");
        DodgeSpamMeter late = new DodgeSpamMeter();
        late.record(0);
        late.record(60);
        assertFalse(late.record(121), "one tick too late: the first fell out");
    }

    @Test
    void slowDodgesNeverFire() {
        DodgeSpamMeter m = new DodgeSpamMeter();
        for (int i = 0; i < 50; i++) assertFalse(m.record(i * 70L));
    }

    @Test
    void afterItFiresItStartsFromZero() {
        DodgeSpamMeter m = new DodgeSpamMeter();
        m.record(0);
        m.record(10);
        assertTrue(m.record(20));
        assertEquals(0, m.count(20));
        assertFalse(m.record(30));
        assertFalse(m.record(40));
        assertTrue(m.record(50), "three fresh dodges are needed for the next trap");
    }

    @Test
    void countIgnoresOldEntries() {
        DodgeSpamMeter m = new DodgeSpamMeter();
        m.record(0);
        m.record(50);
        assertEquals(2, m.count(100));
        assertEquals(1, m.count(160));
        assertEquals(0, m.count(400));
    }

    @Test
    void aHugeBurstDoesNotOverflowTheBuffer() {
        DodgeSpamMeter m = new DodgeSpamMeter();
        int fired = 0;
        for (int i = 0; i < 100; i++) if (m.record(i)) fired++;
        assertEquals(33, fired);
    }
}
