package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the short windows of the Katana (a parry, an evade, the phantom blade, a riposte, the quiet before a Keen Opening) are all "until tick T" or a small
 * counter, and each must open, answer true until its tick and not after, close when used, and never give more than it promised: the phantom blade gives exactly
 * its four cuts and never two within its gap, a riposte is used up by asking.
 */
class KatanaSessionTest {
    @Test
    void theParryAndTheEvadeOpenForTheirTicksOnly() {
        KatanaSession s = new KatanaSession();
        assertFalse(s.counterOpen(0));
        s.openCounter(100, KatanaNumbers.PARRY_TICKS);
        assertTrue(s.counterOpen(100));
        assertTrue(s.counterOpen(100 + KatanaNumbers.PARRY_TICKS - 1));
        assertFalse(s.counterOpen(100 + KatanaNumbers.PARRY_TICKS), "exclusive end");
        s.openCounter(200, KatanaNumbers.PARRY_TICKS);
        s.closeCounter();
        assertFalse(s.counterOpen(201), "closed when used");
        s.openEvade(300, KatanaNumbers.EVADE_TICKS);
        assertTrue(s.evadeOpen(300 + KatanaNumbers.EVADE_TICKS - 1));
        assertFalse(s.evadeOpen(300 + KatanaNumbers.EVADE_TICKS));
        s.openEvade(400, 8);
        s.closeEvade();
        assertFalse(s.evadeOpen(401));
    }

    @Test
    void thePhantomBladeGivesExactlyItsCutsWithTheGapBetween() {
        KatanaSession s = new KatanaSession();
        assertFalse(s.takeEcho(0), "closed");
        s.openDouble(1000, KatanaNumbers.DOUBLE_TICKS, KatanaNumbers.ECHO_MAX);
        assertTrue(s.doubleOpen(1000));
        int given = 0;
        long t = 1000;
        for (int i = 0; i < 40; i++, t += KatanaNumbers.ECHO_GAP) if (s.takeEcho(t)) given++;
        assertEquals(KatanaNumbers.ECHO_MAX, given, "exactly four cuts");
        assertEquals(0, s.echoesLeft());
        assertFalse(s.doubleOpen(t), "spent");
    }

    @Test
    void thePhantomBladeNeverStrikesTwiceWithinItsGapOrAfterItsTime() {
        KatanaSession s = new KatanaSession();
        s.openDouble(0, KatanaNumbers.DOUBLE_TICKS, 4);
        assertTrue(s.takeEcho(10));
        assertFalse(s.takeEcho(10 + KatanaNumbers.ECHO_GAP - 1), "inside the gap");
        assertTrue(s.takeEcho(10 + KatanaNumbers.ECHO_GAP));
        assertFalse(s.takeEcho(KatanaNumbers.DOUBLE_TICKS), "after its time");
        assertEquals(2, s.echoesLeft(), "refused strikes cost nothing");
        s.openDouble(500, 20, 0);
        assertFalse(s.doubleOpen(500), "a blade with no cuts is not open");
    }

    @Test
    void aRiposteIsUsedUpByAsking() {
        KatanaSession s = new KatanaSession();
        assertFalse(s.takeRiposte(0));
        s.openRiposte(100, KatanaNumbers.RIPOSTE_TICKS);
        assertTrue(s.takeRiposte(130));
        assertFalse(s.takeRiposte(131), "used up");
        s.openRiposte(200, KatanaNumbers.RIPOSTE_TICKS);
        assertFalse(s.takeRiposte(200 + KatanaNumbers.RIPOSTE_TICKS), "too late");
    }

    @Test
    void quietTimeIsCountedFromTheLastAction() {
        KatanaSession s = new KatanaSession();
        assertTrue(s.quiet(5, KatanaNumbers.OPENING_IDLE), "a wielder who never acted is quiet");
        s.acted(1000);
        assertFalse(s.quiet(1000 + KatanaNumbers.OPENING_IDLE - 1, KatanaNumbers.OPENING_IDLE));
        assertTrue(s.quiet(1000 + KatanaNumbers.OPENING_IDLE, KatanaNumbers.OPENING_IDLE));
        s.acted(2000);
        s.chain().record("a", 2000);
        s.reset();
        assertTrue(s.quiet(2001, KatanaNumbers.OPENING_IDLE), "reset forgets the action");
        assertEquals(0, s.chain().level(2001));
    }
}
