package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Time Stasis Traps: three at most (was six), twelve seconds, armed after a 24-tick warning, one catch per player per twelve seconds. */
class TrapLedgerTest {
    @Test
    void threeAtOnceAndNoFourth() {
        TrapLedger l = new TrapLedger();
        assertEquals(3, TrapLedger.MAX_TRAPS);
        for (int i = 0; i < 3; i++) assertNotNull(l.lay(i * 10, 0, 0, 0, TrapLedger.LIFE_TICKS));
        assertNull(l.lay(100, 0, 0, 0, TrapLedger.LIFE_TICKS));
        assertEquals(3, l.liveCount(10));
    }

    @Test
    void aTrapLivesTwelveSecondsThenMakesRoom() {
        TrapLedger l = new TrapLedger();
        for (int i = 0; i < 3; i++) l.lay(i * 10, 0, 0, 0, TrapLedger.LIFE_TICKS);
        assertEquals(3, l.liveCount(239));
        assertEquals(0, l.liveCount(240));
        assertNotNull(l.lay(0, 0, 0, 240, TrapLedger.LIFE_TICKS));
    }

    @Test
    void aTrapIsHarmlessUntilItsWarningIsOver() {
        assertEquals(24, TrapLedger.ARM_TICKS, "the warning is 24 ticks since 1.46.0");
        TrapLedger l = new TrapLedger();
        l.lay(0, 64, 0, 100, TrapLedger.LIFE_TICKS);
        assertNull(l.catchAt("p", 0.5, 64, 0, 100 + TrapLedger.ARM_TICKS - 1));
        assertNotNull(l.catchAt("p", 0.5, 64, 0, 100 + TrapLedger.ARM_TICKS));
    }

    @Test
    void aCatchNeedsTheCircleAndTheHeight() {
        TrapLedger l = new TrapLedger();
        l.lay(0, 64, 0, 0, TrapLedger.LIFE_TICKS);
        assertNull(l.catchAt("p", 3.0, 64, 0, 50), "outside the ring");
        assertNull(l.catchAt("p", 0.0, 70, 0, 50), "far above it");
        assertNotNull(l.catchAt("p", 1.0, 65, 1.0, 50));
    }

    @Test
    void aCatchUsesUpTheTrapAndStartsThePlayersCooldown() {
        TrapLedger l = new TrapLedger();
        l.lay(0, 64, 0, 0, TrapLedger.LIFE_TICKS);
        l.lay(0.5, 64, 0, 0, TrapLedger.LIFE_TICKS);
        assertNotNull(l.catchAt("p", 0, 64, 0, 50));
        assertEquals(1, l.liveCount(50));
        assertNull(l.catchAt("p", 0, 64, 0, 60), "the same player is not caught again for twelve seconds");
        assertNotNull(l.catchAt("q", 0, 64, 0, 60), "another player is");
        l.lay(0, 64, 0, 100, TrapLedger.LIFE_TICKS);
        assertNull(l.catchAt("p", 0, 64, 0, 289), "one tick before the twelve seconds (240 ticks from the catch at 50) are over");
        assertNotNull(l.catchAt("p", 0, 64, 0, 290));
    }

    @Test
    void nearFindsATrapOnTheSameDodger() {
        TrapLedger l = new TrapLedger();
        l.lay(10, 64, 10, 0, TrapLedger.LIFE_TICKS);
        assertTrue(l.near(11, 10, 5, 3.0));
        assertFalse(l.near(30, 30, 5, 3.0));
        assertFalse(l.near(10, 10, 5000, 3.0), "expired");
    }

    @Test
    void lifeIsBoundedSoNoTrapIsImmortal() {
        TrapLedger l = new TrapLedger();
        TrapLedger.Trap t = l.lay(0, 0, 0, 0, Integer.MAX_VALUE);
        assertTrue(t.dieAt() <= TrapLedger.LIFE_TICKS * 2L);
        TrapLedger.Trap tiny = l.lay(50, 0, 0, 0, 1);
        assertTrue(tiny.dieAt() > tiny.armedAt(), "always armed for at least one tick");
    }

    @Test
    void clearEmptiesEverything() {
        TrapLedger l = new TrapLedger();
        l.lay(0, 0, 0, 0, 100);
        l.catchAt("p", 0, 0, 0, 30);
        l.clear();
        assertEquals(0, l.liveCount(1));
        l.lay(0, 0, 0, 40, 100);
        assertNotNull(l.catchAt("p", 0, 0, 0, 70), "the player cooldown was cleared too");
    }
}
