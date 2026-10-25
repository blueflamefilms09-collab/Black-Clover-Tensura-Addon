package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report 3.7.0, docs/magic/rades_cast_fix.md): the rule of what a numbered undead's renderer draws is pure, so it is tested here: the geo look when its model and texture are there,
 * the zombie stand-in when one is missing (so the body is never invisible), and nothing only when the owner's client switch {@code corpse.fallbackLook} is off (the old behaviour).
 */
class CorpseLookRulesTest {
    @Test
    void geoWhenModelAndTextureAreThere() {
        assertEquals(CorpseLookRules.Draw.GEO, CorpseLookRules.draw(true, true, true));
        assertEquals(CorpseLookRules.Draw.GEO, CorpseLookRules.draw(true, true, false), "the switch does not matter when the look loaded");
    }

    @Test
    void fallbackWhenOneThingIsMissing() {
        assertEquals(CorpseLookRules.Draw.FALLBACK, CorpseLookRules.draw(false, true, true));
        assertEquals(CorpseLookRules.Draw.FALLBACK, CorpseLookRules.draw(true, false, true));
        assertEquals(CorpseLookRules.Draw.FALLBACK, CorpseLookRules.draw(false, false, true));
    }

    @Test
    void nothingOnlyWhenTheFallbackIsSwitchedOff() {
        assertEquals(CorpseLookRules.Draw.NOTHING, CorpseLookRules.draw(false, true, false));
        assertEquals(CorpseLookRules.Draw.NOTHING, CorpseLookRules.draw(true, false, false));
        assertEquals(CorpseLookRules.Draw.NOTHING, CorpseLookRules.draw(false, false, false));
    }

    @Test
    void everyDrawHasAWordForTheLog() {
        for (CorpseLookRules.Draw d : CorpseLookRules.Draw.values()) {
            String s = CorpseLookRules.describe(d);
            assertNotNull(s);
            assertFalse(s.isBlank(), d + " has no description");
        }
        assertNotEquals(CorpseLookRules.describe(CorpseLookRules.Draw.GEO), CorpseLookRules.describe(CorpseLookRules.Draw.FALLBACK));
    }

    @Test
    void theLaterRegistrationWins() {
        assertTrue(CorpseLookRules.laterWins(2, 1), "the renderer registered second stays");
        assertFalse(CorpseLookRules.laterWins(1, 2));
        assertFalse(CorpseLookRules.laterWins(1, 1), "equal positions: the second argument");
    }
}
