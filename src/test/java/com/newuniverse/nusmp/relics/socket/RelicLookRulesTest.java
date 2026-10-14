package com.newuniverse.nusmp.relics.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure half of RelicLook: what a worn-relic layer is told. */
class RelicLookRulesTest {
    @Test
    void aSocketGivesItsElementAndColour() {
        RelicLookRules.Evolved e = RelicLookRules.evolved(5, "spirit:fire", 0x123456);
        assertEquals(5, e.tier());
        assertEquals("fire", e.socketElement());
        assertEquals(SocketElement.FIRE.rgb(), e.rgb());
    }

    @Test
    void noSocketGivesTheGradeColour() {
        RelicLookRules.Evolved e = RelicLookRules.evolved(3, "", 0x55FFFF);
        assertEquals("", e.socketElement());
        assertEquals(0x55FFFF, e.rgb());
        assertEquals("", RelicLookRules.evolved(3, "spirit:unknown", 0xAAAAAA).socketElement());
    }

    @Test
    void aDevilReadsAsItsLookElement() {
        assertEquals("fire", RelicLookRules.evolved(6, "devil:nahamah", 0).socketElement());
        assertEquals("dark", RelicLookRules.evolved(6, "devil:megicula", 0).socketElement());
        assertEquals(SocketCatalog.byId("devil:megicula").rgb(), RelicLookRules.evolved(6, "devil:megicula", 0).rgb());
    }

    @Test
    void theTierIsClampedAndLegendIsElaborate() {
        assertEquals(1, RelicLookRules.evolved(-4, "", 0).tier());
        assertEquals(7, RelicLookRules.evolved(40, "", 0).tier());
        assertFalse(RelicLookRules.elaborate(4));
        assertTrue(RelicLookRules.elaborate(5));
        assertTrue(RelicLookRules.elaborate(7));
    }
}
