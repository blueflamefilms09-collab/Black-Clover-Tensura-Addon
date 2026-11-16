package com.newuniverse.nusmp.labyrinth.mega.cores;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guide P2 (Telesma / Malice bridge, docs/guide_fixes_p2.md): the amount, the safe player name, the command text and the pacing, all pure. */
class QuenchedRulesTest {
    @Test
    void amountIsTheGainTimesTheFactorRoundedDownAndBounded() {
        assertEquals(0, QuenchedRules.amount(100, 0.0, 1000), "factor 0 = off");
        assertEquals(0, QuenchedRules.amount(0, 2.0, 1000));
        assertEquals(0, QuenchedRules.amount(-5, 2.0, 1000));
        assertEquals(0, QuenchedRules.amount(Double.NaN, 2.0, 1000));
        assertEquals(0, QuenchedRules.amount(Double.POSITIVE_INFINITY, 2.0, 1000));
        assertEquals(0, QuenchedRules.amount(0.4, 2.0, 1000), "under one rounds to nothing");
        assertEquals(5, QuenchedRules.amount(10, 0.5, 1000));
        assertEquals(7, QuenchedRules.amount(3.7, 2.0, 1000), "rounded down");
        assertEquals(1000, QuenchedRules.amount(1.0E12, 1.0, 1000), "one kill never pays more than the cap");
        assertEquals(0, QuenchedRules.amount(10, 1.0, 0));
    }

    @Test
    void onlyAVanillaPlayerNameIsWrittenIntoACommand() {
        assertTrue(QuenchedRules.safeName("Steve_01"));
        assertTrue(QuenchedRules.safeName("a"));
        assertFalse(QuenchedRules.safeName(""));
        assertFalse(QuenchedRules.safeName(null));
        assertFalse(QuenchedRules.safeName("seventeen_letters"));
        assertFalse(QuenchedRules.safeName("Bad Name"));
        assertFalse(QuenchedRules.safeName("x;say hi"));
        assertFalse(QuenchedRules.safeName("a\nb"));
    }

    @Test
    void theCommandIsTheTemplateFilledInOrNothing() {
        assertEquals("tni telesma add Steve 12", QuenchedRules.render("tni telesma add {player} {amount}", "Steve", 12));
        assertEquals("tni telesma add Steve 12", QuenchedRules.render("  /tni telesma add {player} {amount}  ", "Steve", 12), "one leading slash and the padding go");
        assertEquals("say {x} Steve 3 3", QuenchedRules.render("say {x} {player} {amount} {amount}", "Steve", 3), "every {amount} is filled, unknown braces stay");
        assertNull(QuenchedRules.render("", "Steve", 1));
        assertNull(QuenchedRules.render("   ", "Steve", 1));
        assertNull(QuenchedRules.render("/", "Steve", 1));
        assertNull(QuenchedRules.render(null, "Steve", 1));
        assertNull(QuenchedRules.render("say {amount}", "Steve", 0), "no amount, no command");
        assertNull(QuenchedRules.render("say {amount}", "Bad Name", 1), "an unsafe name is never written into a command");
        assertNull(QuenchedRules.render("say a\nsay b", "Steve", 1), "one line only");
        assertNull(QuenchedRules.render("say a\u0000b", "Steve", 1), "no control characters");
        assertNull(QuenchedRules.render("x".repeat(QuenchedRules.MAX_COMMAND + 1), "Steve", 1), "bounded length");
        assertEquals(QuenchedRules.MAX_COMMAND, QuenchedRules.render("x".repeat(QuenchedRules.MAX_COMMAND), "Steve", 1).length());
    }

    @Test
    void paymentsAreSpacedAndAnOddClockNeverBlocksForever() {
        assertTrue(QuenchedRules.due(-1, 0), "the first one runs");
        assertFalse(QuenchedRules.due(100, 100 + QuenchedRules.MIN_INTERVAL_TICKS - 1));
        assertTrue(QuenchedRules.due(100, 100 + QuenchedRules.MIN_INTERVAL_TICKS));
        assertTrue(QuenchedRules.due(1000, 5), "a clock that went backwards (a restarted world) allows it");
    }
}
