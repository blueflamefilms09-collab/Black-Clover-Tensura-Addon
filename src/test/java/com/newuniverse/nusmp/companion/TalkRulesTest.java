package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.companion.talk.TalkRules;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the talk screen's limits (id length, option count, reserved prefixes, reach, what a pick may be) decide what a hostile client can make the server do; they are pinned here. */
class TalkRulesTest {
    @Test
    void anIdIsAtMostFortyCharacters() {
        assertNull(TalkRules.idProblem("a".repeat(40), false));
        assertNotNull(TalkRules.idProblem("a".repeat(41), false));
        assertNotNull(TalkRules.idProblem("a".repeat(41), true));
        assertNotNull(TalkRules.idProblem("", false));
        assertNotNull(TalkRules.idProblem(null, false));
    }

    @Test
    void theOptionLimitIsFortyAndTheTabsAreFour() {
        assertEquals(40, TalkRules.MAX_OPTIONS);
        assertEquals(40, TalkRules.MAX_ID);
        assertEquals(4, TalkRules.TABS);
        assertTrue(TalkRules.validTab(0) && TalkRules.validTab(3));
        assertFalse(TalkRules.validTab(4) || TalkRules.validTab(-1));
        assertEquals(TalkRules.TALK, TalkRules.cleanTab(99));
        assertEquals(TalkRules.POUCH, TalkRules.cleanTab(3));
    }

    @Test
    void theReservedPrefixesBelongToTheSystem() {
        assertTrue(TalkRules.reserved("k:gift"));
        assertTrue(TalkRules.reserved("g:panel"));
        assertTrue(TalkRules.reserved("x:close"));
        assertFalse(TalkRules.reserved("t:self"));
        assertFalse(TalkRules.reserved("kk:x"));
        assertNotNull(TalkRules.idProblem("k:mine", false), "a companion's own topic may not use k:");
        assertNotNull(TalkRules.idProblem("g:mine", false));
        assertNotNull(TalkRules.idProblem("x:mine", false));
        assertNull(TalkRules.idProblem("k:gift:3", true), "the bond system may");
        assertNull(TalkRules.idProblem("t:self", false));
    }

    @Test
    void anIdWithSpacesControlCharactersOrColourCodesIsRefused() {
        assertNotNull(TalkRules.idProblem("a b", false));
        assertNotNull(TalkRules.idProblem("a\nb", false));
        assertNotNull(TalkRules.idProblem("a§cb", false));
        assertNotNull(TalkRules.idProblem("a\u007fb", false));
        assertNull(TalkRules.idProblem("t:topic_1.a-b", false));
    }

    @Test
    void reachIsEightBlocks() {
        assertTrue(TalkRules.inReach(64.0));
        assertFalse(TalkRules.inReach(64.01));
        assertTrue(TalkRules.inReach(0));
    }

    @Test
    void aPickMustBeOnTheLastViewAndWellFormed() {
        Set<String> offered = Set.of("t:self", "k:five");
        assertTrue(TalkRules.acceptPick("t:self", offered));
        assertTrue(TalkRules.acceptPick("k:five", offered));
        assertFalse(TalkRules.acceptPick("t:other", offered));
        assertFalse(TalkRules.acceptPick("", offered));
        assertFalse(TalkRules.acceptPick(null, offered));
        assertFalse(TalkRules.acceptPick("t:self", null));
        assertFalse(TalkRules.acceptPick("a".repeat(41), Set.of("a".repeat(41))), "an over-long id is refused even if somehow offered");
    }

    @Test
    void textIsCut() {
        assertEquals(TalkRules.MAX_TEXT, TalkRules.cut("x".repeat(5000)).length());
        assertEquals("", TalkRules.cut(null));
        assertEquals("hi", TalkRules.cut("hi"));
    }
}
