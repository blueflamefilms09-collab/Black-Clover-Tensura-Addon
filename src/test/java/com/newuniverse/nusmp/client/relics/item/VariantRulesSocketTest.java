package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.29.0: the tier threshold of the elaborate look and the socket's emissive pass. */
class VariantRulesSocketTest {
    @Test
    void legendAndAboveAreElaborate() {
        assertFalse(VariantRules.elaborate(1));
        assertFalse(VariantRules.elaborate(4));
        assertTrue(VariantRules.elaborate(5));
        assertTrue(VariantRules.elaborate(6));
        assertTrue(VariantRules.elaborate(7));
    }

    @Test
    void theSocketPassBreathesInsideItsRange() {
        int lo = 255, hi = 0;
        for (int t = 0; t < 200; t++) {
            int a = VariantRules.socketAlpha(t);
            lo = Math.min(lo, a);
            hi = Math.max(hi, a);
        }
        assertTrue(lo >= 70 && lo <= 80, "low " + lo);
        assertTrue(hi >= 140 && hi <= 150, "high " + hi);
    }

    @Test
    void theArgbCarriesTheColourAndTheAlpha() {
        int argb = VariantRules.socketArgb(0xFF6A1A, 0f);
        assertEquals(110, argb >>> 24);
        assertEquals(0xFF6A1A, argb & 0xFFFFFF);
        assertEquals(0x123456, VariantRules.socketArgb(0xFF123456, 3f) & 0xFFFFFF);
    }

    @Test
    void theFiveLeafAndGenesisRulesAreUntouched() {
        VariantRules.Decision d = VariantRules.decide(new VariantRules.Inputs(false, false, 5, true, true));
        assertTrue(d.fiveLeaf());
        assertFalse(d.genesis());
        assertTrue(VariantRules.decide(new VariantRules.Inputs(true, false, 0, false, false)).genesis());
    }
}
