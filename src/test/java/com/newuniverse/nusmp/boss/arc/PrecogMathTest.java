package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Precognition: everything is phased out except the four ways the owner allows, and nothing when it is off. */
class PrecogMathTest {
    @Test
    void aLoneSlowAttackerIsPhasedOutEveryTime() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        long now = 100;
        for (int i = 0; i < 10; i++, now += 25) assertEquals(PrecogMath.Verdict.PHASED, t.onHit(now, 7, false, false, true), "hit " + i);
        assertEquals(10, t.phasedCount());
    }

    @Test
    void twoAttackersWithinSixTicksBothLand() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(100, 1, false, false, true));
        assertEquals(PrecogMath.Verdict.MULTI_ATTACKER, t.onHit(104, 2, false, false, true));
        assertEquals(PrecogMath.Verdict.MULTI_ATTACKER, t.onHit(106, 1, false, false, true), "the first attacker's next blow is in sequence with the second");
    }

    @Test
    void twoAttackersSevenTicksApartAreNotSimultaneous() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(100, 1, false, false, true));
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(107, 2, false, false, true));
    }

    @Test
    void theFourthFastBlowOfOneAttackerLands() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(100, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(106, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(112, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.RAPID, t.onHit(118, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.RAPID, t.onHit(124, 3, false, false, true), "while the pace holds, every blow lands");
    }

    @Test
    void fourBlowsSpreadOverMoreThanTwentyTicksStayPhased() {
        PrecogMath.Tracker u = new PrecogMath.Tracker();
        assertEquals(PrecogMath.Verdict.PHASED, u.onHit(100, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.PHASED, u.onHit(110, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.PHASED, u.onHit(120, 3, false, false, true));
        assertEquals(PrecogMath.Verdict.PHASED, u.onHit(130, 3, false, false, true), "four blows in thirty ticks is not very fast");
    }

    @Test
    void antiMagicAndSynchronizedRelicsAlwaysLand() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        assertEquals(PrecogMath.Verdict.ANTI_MAGIC, t.onHit(100, 1, true, false, true));
        assertEquals(PrecogMath.Verdict.SYNCED_RELICS, t.onHit(200, 1, false, true, true));
        assertEquals(PrecogMath.Verdict.ANTI_MAGIC, t.onHit(300, 1, true, true, true), "anti-magic is reported first");
        assertEquals(0, t.phasedCount());
    }

    @Test
    void noAttackerLandsAndOffLands() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        assertEquals(PrecogMath.Verdict.NO_ATTACKER, t.onHit(100, -1, false, false, true));
        assertEquals(PrecogMath.Verdict.OFF, t.onHit(101, 5, false, false, false));
        assertTrue(PrecogMath.Verdict.OFF.lands() && PrecogMath.Verdict.NO_ATTACKER.lands());
        assertFalse(PrecogMath.Verdict.PHASED.lands());
    }

    @Test
    void blowsWhilePrecognitionIsOffAreNotRemembered() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        for (int i = 0; i < 6; i++) t.onHit(100 + i, 3, false, false, false);
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(110, 3, false, false, true), "the history starts when precognition returns");
    }

    @Test
    void oldHitsAreForgotten() {
        PrecogMath.Tracker t = new PrecogMath.Tracker();
        t.onHit(100, 1, false, false, true);
        assertEquals(PrecogMath.Verdict.PHASED, t.onHit(100 + PrecogMath.FORGET_AFTER + 5, 2, false, false, true));
    }

    @Test
    void syncedRelicsRule() {
        assertFalse(PrecogMath.synchronizedRelics(1));
        assertTrue(PrecogMath.synchronizedRelics(2));
        assertTrue(PrecogMath.synchronizedRelics(3));
    }
}
