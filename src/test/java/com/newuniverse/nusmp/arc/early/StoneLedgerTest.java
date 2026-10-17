package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Mars's long fight is cracking his stones in time, so the count, the window, the double count of anti-magic and the save/restore are proven. */
class StoneLedgerTest {
    @Test
    void threeStonesAreSetAndEachAddsATenth() {
        StoneLedger s = new StoneLedger(3);
        assertEquals(3, s.intact());
        assertEquals(0, s.broken());
        assertEquals(1.3, s.amplification(), 1e-9);
        assertFalse(s.exposed(0));
        assertEquals(StoneLedger.Event.NONE, s.hit(0, 500, false), "blows count only while a stone shows");
    }

    @Test
    void aShowingStoneCracksUnderEnoughDamage() {
        StoneLedger s = new StoneLedger(3);
        assertTrue(s.surface(100, 240, 100));
        assertTrue(s.exposed(100));
        assertFalse(s.surface(101, 240, 100), "one stone shows at a time");
        assertEquals(StoneLedger.Event.NONE, s.hit(110, 60, false));
        assertEquals(0.6, s.progress(110), 1e-9);
        assertEquals(StoneLedger.Event.BROKEN, s.hit(120, 45, false));
        assertEquals(2, s.intact());
        assertEquals(1.2, s.amplification(), 1e-9);
        assertFalse(s.exposed(121), "a cracked stone is gone");
        assertEquals(StoneLedger.Event.NONE, s.hit(122, 500, false), "and nothing is showing now");
    }

    @Test
    void antiMagicCountsDoubleBecauseAStoneIsAMagicTool() {
        StoneLedger s = new StoneLedger(3);
        s.surface(0, 240, 100);
        assertEquals(StoneLedger.Event.NONE, s.hit(1, 30, true));
        assertEquals(StoneLedger.Event.BROKEN, s.hit(2, 20, true), "30 + 20 doubled is 100");
    }

    @Test
    void aStoneThatIsNotCrackedInTimeSinksBackAndKeepsItsTenth() {
        StoneLedger s = new StoneLedger(3);
        s.surface(0, 240, 100);
        s.hit(100, 90, false);
        assertFalse(s.exposed(240), "the window ran out");
        assertEquals(3, s.intact());
        assertEquals(1.3, s.amplification(), 1e-9);
        assertEquals(StoneLedger.Event.NONE, s.hit(241, 500, false));
        assertTrue(s.surface(300, 240, 100), "the next window starts from nothing");
        assertEquals(0.0, s.progress(300), 1e-9);
        assertEquals(StoneLedger.Event.BROKEN, s.hit(301, 100, false));
    }

    @Test
    void whenAllAreCrackedNothingSurfacesAgain() {
        StoneLedger s = new StoneLedger(2);
        for (int i = 0; i < 2; i++) {
            assertTrue(s.surface(i * 1000, 240, 10));
            assertEquals(StoneLedger.Event.BROKEN, s.hit(i * 1000 + 1, 10, false));
        }
        assertTrue(s.allBroken());
        assertEquals(1.0, s.amplification(), 1e-9);
        assertFalse(s.surface(5000, 240, 10));
    }

    @Test
    void resetAndRestoreAreClamped() {
        StoneLedger s = new StoneLedger(3);
        s.surface(0, 240, 10);
        s.hit(1, 10, false);
        assertEquals(2, s.intact());
        s.reset();
        assertEquals(3, s.intact());
        assertFalse(s.exposed(2));
        s.restore(1);
        assertEquals(1, s.intact());
        s.restore(9);
        assertEquals(3, s.intact());
        s.restore(-4);
        assertEquals(0, s.intact());
    }
}
