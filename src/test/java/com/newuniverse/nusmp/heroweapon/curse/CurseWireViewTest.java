package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.Sin;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: the server writes the curse's sync packet and the client reads it, and they must agree on how a sin is numbered and how the unlocked series are packed. The
 * packet can also arrive damaged, and the client must turn that into "nothing" and never an exception. These tests round-trip every sin, pack and unpack masks, and
 * feed the client's view hostile numbers.
 */
class CurseWireViewTest {
    @Test
    void everySinSurvivesTheRoundTripAndNoneIsZero() {
        assertEquals(0, CurseWire.toWire(null));
        assertNull(CurseWire.fromWire(0));
        for (Sin s : Sin.values()) {
            int w = CurseWire.toWire(s);
            assertTrue(w >= 1 && w <= Sin.values().length);
            assertEquals(s, CurseWire.fromWire(w));
        }
        assertNull(CurseWire.fromWire(-4));
        assertNull(CurseWire.fromWire(Sin.values().length + 1));
        assertNull(CurseWire.fromWire(255));
    }

    @Test
    void theMaskPacksTheSeries() {
        int mask = CurseWire.seriesMask(List.of("wrath", "envy", "not_a_sin"));
        assertTrue(CurseWire.hasBit(mask, Sin.WRATH));
        assertTrue(CurseWire.hasBit(mask, Sin.ENVY));
        assertFalse(CurseWire.hasBit(mask, Sin.PRIDE));
        assertFalse(CurseWire.hasBit(mask, null));
        assertEquals(0, CurseWire.seriesMask(List.of()));
        int all = CurseWire.seriesMask(List.of("wrath", "pride", "lust", "envy", "greed", "sloth", "gluttony"));
        for (Sin s : Sin.values()) assertTrue(CurseWire.hasBit(all, s));
        assertEquals(5, CurseWire.clamp(9, 0, 5));
        assertEquals(0, CurseWire.clamp(-9, 0, 5));
    }

    @Test
    void theClientViewTurnsTicksLeftIntoTimesAndClampsWhatArrives() {
        long now = 5000;
        CurseView.apply(640, 810, CurseWire.toWire(Sin.WRATH), CurseWire.toWire(Sin.PRIDE), 300, 900, CurseWire.seriesMask(List.of("wrath")), now);
        assertEquals(640, CurseView.despair());
        assertEquals(810, CurseView.corruption());
        assertEquals(Sin.WRATH, CurseView.offered());
        assertEquals(Sin.PRIDE, CurseView.active());
        assertTrue(CurseView.hasSeries(Sin.WRATH));
        assertFalse(CurseView.hasSeries(Sin.PRIDE));
        assertTrue(CurseView.locked(now + 299));
        assertFalse(CurseView.locked(now + 300));
        assertEquals(300, CurseView.overrunTicksLeft(now));
        assertEquals(0, CurseView.overrunTicksLeft(now + 400));
        assertTrue(CurseView.offerOpen(now + 899));
        assertFalse(CurseView.offerOpen(now + 900));
        assertEquals(900, CurseView.offerTicksLeft(now));
        assertTrue(CurseView.anything(now));
        // hostile numbers
        CurseView.apply(99999, -50, 77, 300, -5, 5, 0xFFFF, now);
        assertEquals(1000, CurseView.despair());
        assertEquals(0, CurseView.corruption());
        assertNull(CurseView.offered(), "a number no sin has is no sin");
        assertNull(CurseView.active());
        assertFalse(CurseView.locked(now));
        assertFalse(CurseView.offerOpen(now), "an offer with no sin is no offer");
        assertEquals((1 << Sin.values().length) - 1, CurseView.seriesMask(), "bits no sin has are dropped");
        CurseView.reset();
        assertEquals(0, CurseView.corruption());
        assertFalse(CurseView.anything(now));
    }
}
