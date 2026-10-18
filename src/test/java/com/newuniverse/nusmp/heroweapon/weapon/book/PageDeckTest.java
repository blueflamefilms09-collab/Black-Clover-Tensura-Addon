package com.newuniverse.nusmp.heroweapon.weapon.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the page deck is the Book's whole loop, so its rules are pinned: the pages come up in the order they are written and the deck wraps round, the next
 * and the one after are shown, turning a page skips it WITHOUT reading it, the deck is complete only when every page has really been read, a reset starts it
 * over, and an empty deck never throws and has nothing to read.
 */
class PageDeckTest {
    private static PageDeck deck() { return new PageDeck(List.of("a", "b", "c")); }

    @Test
    void pagesComeUpInOrderAndTheDeckWrapsRound() {
        PageDeck d = deck();
        List<String> order = new ArrayList<>();
        for (int i = 0; i < 7; i++) order.add(d.draw());
        assertEquals(List.of("a", "b", "c", "a", "b", "c", "a"), order);
        assertEquals(3, d.size());
        assertEquals("b", d.next());
        assertEquals("c", d.after());
    }

    @Test
    void nextAndAfterShowWhatIsComing() {
        PageDeck d = deck();
        assertEquals("a", d.next());
        assertEquals("b", d.after());
        d.draw();
        d.draw();
        assertEquals("c", d.next());
        assertEquals("a", d.after(), "the one after the last is the first");
    }

    @Test
    void aSkippedPageIsNotRead() {
        PageDeck d = deck();
        assertEquals("a", d.skip());
        assertEquals(0, d.readCount());
        assertFalse(d.isRead("a"));
        assertEquals("b", d.draw());
        assertEquals("c", d.draw());
        assertFalse(d.complete(), "a was skipped, so the deck is not complete");
        assertEquals("a", d.draw(), "the skipped page comes round again");
        assertTrue(d.complete());
        assertEquals(3, d.readCount());
    }

    @Test
    void completionNeedsEveryPageAndAResetStartsOver() {
        PageDeck d = deck();
        d.draw();
        d.draw();
        assertFalse(d.complete());
        assertTrue(d.isRead("a") && d.isRead("b") && !d.isRead("c"));
        d.draw();
        assertTrue(d.complete());
        d.resetProgress();
        assertFalse(d.complete());
        assertEquals(0, d.readCount());
        assertEquals("a", d.next(), "a reset goes back to the first page");
        assertFalse(d.isRead("zzz"), "an unknown page is not read");
    }

    @Test
    void anEmptyDeckHasNothingToReadAndNeverThrows() {
        PageDeck d = new PageDeck(List.of());
        assertNull(d.next());
        assertNull(d.after());
        assertNull(d.draw());
        assertNull(d.skip());
        assertEquals(0, d.readCount());
        assertTrue(d.complete(), "an empty deck has nothing left to read");
        d.resetProgress();
    }

    @Test
    void theDeckKeepsItsOwnCopyOfThePages() {
        List<String> source = new ArrayList<>(List.of("x", "y"));
        PageDeck d = new PageDeck(source);
        source.add("z");
        assertEquals(2, d.size());
        assertTrue(d.sameAs(List.of("x", "y")));
        assertFalse(d.sameAs(List.of("y", "x")), "order matters");
        assertEquals(List.of("x", "y"), d.copyOfPages());
    }
}
