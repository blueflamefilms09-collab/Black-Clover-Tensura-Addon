package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Lotus's smoke dose: the number, the ladder, the ways to lower it. */
class DoseLedgerTest {
    private final UUID a = new UUID(1, 1), b = new UUID(1, 2);

    @Test
    void theDoseClimbsAndFallsBetweenZeroAndTen() {
        DoseLedger l = new DoseLedger();
        assertEquals(0, l.dose(a));
        assertEquals(3, l.add(a, 3));
        assertEquals(10, l.add(a, 50), "never over ten");
        assertEquals(9, l.add(a, -1));
        assertEquals(0, l.add(a, -50), "never under zero");
        assertTrue(l.ids().isEmpty(), "a clean foe is not kept on record");
        assertEquals(5, l.set(b, 5));
        assertEquals(2, l.set(b, 2));
        l.forget(b);
        assertEquals(0, l.dose(b));
        l.add(a, 4);
        l.clear();
        assertEquals(0, l.dose(a));
    }

    @Test
    void theLadderIsFixed() {
        for (int d = 0; d <= 2; d++) assertEquals(-1, DoseLedger.weakness(d), "no weakness under 3");
        assertEquals(0, DoseLedger.weakness(3));
        assertEquals(0, DoseLedger.weakness(5));
        assertEquals(1, DoseLedger.weakness(6));
        assertEquals(1, DoseLedger.weakness(10));
        for (int d = 0; d <= 5; d++) assertEquals(-1, DoseLedger.slowness(d), "no slowness under 6");
        assertEquals(0, DoseLedger.slowness(6));
        assertEquals(0, DoseLedger.slowness(7));
        assertEquals(1, DoseLedger.slowness(8));
        assertFalse(DoseLedger.falls(9));
        assertTrue(DoseLedger.falls(10));
        assertTrue(DoseLedger.AFTER_FALL < DoseLedger.MAX && DoseLedger.AFTER_FALL >= 3, "the stumble leaves the foe weakened but not at the brink");
    }

    @Test
    void theBloomSpendsTheDose() {
        assertEquals(0f, DoseLedger.bloom(2, 2.2f), "a trace of smoke is not worth a bloom");
        assertEquals(6.6f, DoseLedger.bloom(3, 2.2f), 1e-4f);
        assertEquals(22f, DoseLedger.bloom(10, 2.2f), 1e-4f);
    }

    @Test
    void aCloudBeatsTheTrickleAndTheTrickleNeedsSightAndItsBeat() {
        assertEquals(2, DoseLedger.gain(2, true, true));
        assertEquals(1, DoseLedger.gain(1, false, false), "standing in a garden is enough");
        assertEquals(1, DoseLedger.gain(0, true, true), "the aura's beat in his sight");
        assertEquals(0, DoseLedger.gain(0, true, false), "between beats");
        assertEquals(0, DoseLedger.gain(0, false, true), "out of his sight");
    }
}
