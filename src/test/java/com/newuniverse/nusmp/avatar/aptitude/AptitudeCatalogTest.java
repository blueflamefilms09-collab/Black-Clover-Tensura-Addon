package com.newuniverse.nusmp.avatar.aptitude;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The aptitude record clamps its numbers and the catalog is replaced whole, first id wins. */
class AptitudeCatalogTest {
    @Test
    void recordClampsAndDefaults() {
        Aptitude a = new Aptitude("fire", null, null, 500, null, null);
        assertEquals("fire", a.name());
        assertEquals(Aptitude.Rarity.COMMON, a.rarity());
        assertEquals(90.0, a.taxPercent(), 1e-9);
        assertEquals("", a.magic());
        assertEquals(List.of(), a.skills());
    }

    @Test
    void catalogIsReplacedWholeAndTheFirstIdWins() {
        AptitudeCatalog.replaceAll(List.of(new Aptitude("fire", "Fire", Aptitude.Rarity.COMMON, 0, "fire", List.of()), new Aptitude("fire", "Other", Aptitude.Rarity.EPIC, 9, "", List.of())));
        assertEquals(1, AptitudeCatalog.size());
        assertEquals("Fire", AptitudeCatalog.byId("FIRE").name());
        AptitudeCatalog.replaceAll(null);
        assertEquals(0, AptitudeCatalog.size());
        assertNull(AptitudeCatalog.byId("fire"));
    }
}
