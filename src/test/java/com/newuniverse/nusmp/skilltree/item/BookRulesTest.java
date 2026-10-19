package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.skilltree.item.BookRules.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: reading a book is where a player's valuable thing is either used or wasted, so every branch of the decision is pinned with a name that says what it protects: a book whose skill does not exist is
 * blank and kept; a skill not yet owned is learned and the book used up; a skill already owned refuses and keeps the book (a duplicate is never wasted); a creative reader keeps every book; an old book of a removed
 * magic is swapped, never blank; the swap picks only a book the reader does not own, and nothing when the reader owns them all.
 */
class BookRulesTest {
    private static Dice fixed(double v) { return () -> v; }

    @Test
    void aNewSkillIsGranted() {
        assertEquals(Outcome.GRANT, BookRules.skillBook(false, true, false));
    }

    @Test
    void anOwnedSkillIsRefusedAndTheBookIsKept() {
        assertEquals(Outcome.ALREADY_OWNED, BookRules.skillBook(false, true, true));
        assertFalse(BookRules.consumes(Outcome.ALREADY_OWNED));
        assertFalse(BookRules.usesUp(Outcome.ALREADY_OWNED, false));
    }

    @Test
    void aBookOfNoSkillIsBlankWhateverElseIsTrue() {
        assertEquals(Outcome.BLANK, BookRules.skillBook(false, false, false));
        assertEquals(Outcome.BLANK, BookRules.skillBook(false, false, true));
        assertFalse(BookRules.consumes(Outcome.BLANK));
    }

    @Test
    void aBookOfARemovedMagicIsSwappedNotBlank() {
        for (boolean known : new boolean[] {false, true})
            for (boolean owned : new boolean[] {false, true}) assertEquals(Outcome.REPLACE, BookRules.skillBook(true, known, owned));
        assertTrue(BookRules.consumes(Outcome.REPLACE));
    }

    @Test
    void onlyTheOutcomesThatDidSomethingConsumeAndCreativeKeepsTheBook() {
        assertTrue(BookRules.consumes(Outcome.GRANT));
        assertTrue(BookRules.usesUp(Outcome.GRANT, false));
        assertFalse(BookRules.usesUp(Outcome.GRANT, true), "a creative reader keeps the book");
        assertFalse(BookRules.usesUp(Outcome.REPLACE, true));
        for (Outcome o : Outcome.values()) assertFalse(BookRules.messageKey(o).isEmpty());
    }

    @Test
    void theSwapPicksOnlyABookTheReaderDoesNotOwn() {
        List<String> all = List.of("a", "b", "c", "d");
        Set<String> owned = Set.of("a", "c");
        for (double roll : new double[] {0.0, 0.3, 0.5, 0.99, 0.999999}) {
            String got = BookRules.pickUnowned(all, owned::contains, fixed(roll));
            assertTrue(got.equals("b") || got.equals("d"), "rolled " + roll + " and got " + got);
        }
        assertEquals("b", BookRules.pickUnowned(all, owned::contains, fixed(0.0)));
        assertEquals("d", BookRules.pickUnowned(all, owned::contains, fixed(0.99)));
    }

    @Test
    void theSwapGivesNothingWhenEverythingIsOwned() {
        assertNull(BookRules.pickUnowned(List.of("a", "b"), x -> true, fixed(0.5)));
        assertNull(BookRules.pickUnowned(List.of(), x -> false, fixed(0.5)));
    }

    @Test
    void aBrokenDiceNeverLeavesThePool() {
        List<String> all = List.of("a", "b");
        assertNotNull(BookRules.pickUnowned(all, x -> false, fixed(5.0)));
        assertNotNull(BookRules.pickUnowned(all, x -> false, fixed(-3.0)));
        assertNotNull(BookRules.pickUnowned(all, x -> false, fixed(Double.NaN)));
        assertNotNull(BookRules.pickUnowned(all, x -> false, null));
    }
}
