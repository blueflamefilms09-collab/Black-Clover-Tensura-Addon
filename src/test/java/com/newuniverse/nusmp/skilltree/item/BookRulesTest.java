package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.skilltree.item.BookRules.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: reading a book is where a player's valuable thing is either used or wasted, so every branch of the decision is pinned with a name that says what it
 * protects: a book whose route does not exist is blank and keeps the book; a route not yet known is unlocked; a known route turns a copy into Skill Points only when
 * the server setting allows it AND the route says the copy is worth something, otherwise the book is refused and kept; only the two outcomes that really did
 * something consume. For the SP Book: the stack's own amount beats the kind's default, a missing amount falls back to the kind, a crafted negative amount can never
 * remove points, a silly huge amount is capped, a stack with no kind and no amount is blank, and the gamble goes through the jack-in-the-box table with the stack's
 * amount as its floor.
 */
class BookRulesTest {
    private static Dice fixed(double v) { return () -> v; }

    @Test
    void anUnknownRouteIsBlankWhateverElseIsTrue() {
        for (boolean unlocked : new boolean[] {false, true})
            for (boolean sp : new boolean[] {false, true})
                assertEquals(Outcome.BLANK, BookRules.skillBook(false, unlocked, sp, 3));
    }

    @Test
    void aNewRouteIsUnlocked() {
        for (boolean sp : new boolean[] {false, true})
            for (int dup : new int[] {0, 3}) assertEquals(Outcome.UNLOCK, BookRules.skillBook(true, false, sp, dup));
    }

    @Test
    void aKnownRouteBecomesPointsOnlyWhenAllowedAndWorthSomething() {
        assertEquals(Outcome.DUPLICATE_SP, BookRules.skillBook(true, true, true, 3));
        assertEquals(Outcome.DUPLICATE_REFUSED, BookRules.skillBook(true, true, false, 3), "the server refuses duplicates");
        assertEquals(Outcome.DUPLICATE_REFUSED, BookRules.skillBook(true, true, true, 0), "a copy worth nothing is not eaten");
        assertEquals(Outcome.DUPLICATE_REFUSED, BookRules.skillBook(true, true, true, -2));
    }

    @Test
    void onlyTheTwoOutcomesThatDidSomethingConsume() {
        assertTrue(BookRules.consumes(Outcome.UNLOCK));
        assertTrue(BookRules.consumes(Outcome.DUPLICATE_SP));
        assertFalse(BookRules.consumes(Outcome.BLANK));
        assertFalse(BookRules.consumes(Outcome.DUPLICATE_REFUSED));
    }

    @Test
    void theStacksOwnAmountBeatsTheKindsDefault() {
        assertEquals(2, BookRules.spBookAmount(SpKind.BEGINNER_ARCHERY, BookRules.NO_AMOUNT, fixed(0.5)));
        assertEquals(5, BookRules.spBookAmount(SpKind.MAGIC_BOOSTER, BookRules.NO_AMOUNT, fixed(0.5)));
        assertEquals(10, BookRules.spBookAmount(SpKind.GREATER_PRIMER, BookRules.NO_AMOUNT, fixed(0.5)));
        assertEquals(7, BookRules.spBookAmount(SpKind.MAGIC_BOOSTER, 7, fixed(0.5)), "the shop can sell a stack with another number");
        assertEquals(0, BookRules.spBookAmount(SpKind.MAGIC_BOOSTER, 0, fixed(0.5)), "an explicit zero is a spent book");
    }

    @Test
    void aStackWithNoKindUsesItsAmountAndWithNothingIsBlank() {
        assertEquals(4, BookRules.spBookAmount(null, 4, fixed(0.5)));
        assertEquals(0, BookRules.spBookAmount(null, BookRules.NO_AMOUNT, fixed(0.5)));
        assertEquals(0, BookRules.spBookAmount(null, 0, fixed(0.5)));
    }

    @Test
    void aCraftedNegativeAmountNeverRemovesPoints() {
        assertEquals(0, BookRules.spBookAmount(null, -50, fixed(0.5)));
        assertEquals(5, BookRules.spBookAmount(SpKind.MAGIC_BOOSTER, -50, fixed(0.5)), "a negative amount reads as missing: the kind's own number");
    }

    @Test
    void aHugeAmountIsCapped() {
        assertEquals(BookRules.MAX_AMOUNT, BookRules.spBookAmount(null, Integer.MAX_VALUE, fixed(0.5)));
        assertEquals(BookRules.MAX_AMOUNT, BookRules.spBookAmount(SpKind.GREATER_PRIMER, 1_000_000, fixed(0.5)));
    }

    @Test
    void theGambleRollsWithTheStacksAmountAsItsFloor() {
        int[] i = {0};
        double[] v = {0.5, 0.0};
        Dice lowest = () -> v[Math.min(i[0]++, 1)];
        assertEquals(1, BookRules.spBookAmount(SpKind.JACK_IN_THE_BOX, 1, lowest));
        i[0] = 0;
        assertEquals(3, BookRules.spBookAmount(SpKind.JACK_IN_THE_BOX, 3, lowest), "a floor of 3 lifts the lowest roll");
        i[0] = 0;
        assertEquals(1, BookRules.spBookAmount(SpKind.JACK_IN_THE_BOX, BookRules.NO_AMOUNT, lowest), "no amount: the kind's floor of 1");
        assertEquals(20, BookRules.spBookAmount(SpKind.JACK_IN_THE_BOX, 1, fixed(0.0)), "the jackpot");
    }
}
