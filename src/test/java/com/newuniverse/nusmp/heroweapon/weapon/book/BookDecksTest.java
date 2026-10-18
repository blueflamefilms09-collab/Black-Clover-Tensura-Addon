package com.newuniverse.nusmp.heroweapon.weapon.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: which pages a form's chapters hold is decided by its passives, and a wielder's plan must survive a change of form, so the order is pinned: the offence
 * chapter always opens with the bird and the needles and gains the feathers last, the support chapter always opens with the wall and the verse and gains the
 * cage last, and a passive that is not a deck passive changes nothing.
 */
class BookDecksTest {
    @Test
    void theBaseChaptersHoldTwoPagesEach() {
        assertEquals(List.of("bird", "needles"), BookDecks.offense(Set.of()));
        assertEquals(List.of("wall", "verse"), BookDecks.support(Set.of()));
        assertEquals(List.of("bird", "needles"), BookDecks.offense(Set.of(BookNumbers.P_CHAPTERS, BookNumbers.P_REVISED)));
        assertEquals(List.of("wall", "verse"), BookDecks.support(Set.of(BookNumbers.P_CHAPTERS, BookNumbers.P_GRAND)));
    }

    @Test
    void thePassivesAddTheirPageLast() {
        assertEquals(List.of("bird", "needles", "feathers"), BookDecks.offense(Set.of(BookNumbers.P_DECK_FEATHERS)));
        assertEquals(List.of("wall", "verse", "cage"), BookDecks.support(Set.of(BookNumbers.P_DECK_CAGE)));
        assertEquals(List.of("bird", "needles"), BookDecks.offense(Set.of(BookNumbers.P_DECK_CAGE)), "the cage passive does not touch the offence chapter");
        assertEquals(List.of("wall", "verse"), BookDecks.support(Set.of(BookNumbers.P_DECK_FEATHERS)), "nor the feather passive the support chapter");
    }

    @Test
    void everyPageBelongsToExactlyOneChapter() {
        List<String> off = BookDecks.offense(Set.of(BookNumbers.P_DECK_FEATHERS)), sup = BookDecks.support(Set.of(BookNumbers.P_DECK_CAGE));
        for (String p : off) assertTrue(BookDecks.isOffense(p), p);
        for (String p : sup) assertFalse(BookDecks.isOffense(p), p);
        assertEquals(6, off.size() + sup.size());
        assertEquals(BookText.pageIds(), List.of(off.get(0), off.get(1), off.get(2), sup.get(0), sup.get(1), sup.get(2)), "the text names the six pages in reading order");
    }
}
