package com.newuniverse.nusmp.heroweapon.weapon.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the volume is the Book's loop (read every page of both chapters, then Libraria), so its rules are pinned: an empty volume is never complete, the
 * decks are rebuilt only when the pages really changed (a change of form to the same pages keeps the progress), a turn of the page turns the chapter read last,
 * the Grand Library forgives one unread page, and consuming the volume empties its progress.
 */
class VolumeTest {
    private static Volume base() {
        Volume v = new Volume();
        v.ensure(List.of("bird", "needles"), List.of("wall", "verse"));
        return v;
    }

    @Test
    void anEmptyVolumeIsNeverComplete() {
        Volume v = new Volume();
        assertFalse(v.complete(0));
        assertFalse(v.complete(5));
        assertEquals(0, v.pagesTotal());
        assertEquals("0/0", v.progress());
        assertNull(v.turn(), "nothing to turn");
    }

    @Test
    void readingEveryPageCompletesTheVolume() {
        Volume v = base();
        assertEquals(4, v.pagesTotal());
        v.offense().draw();
        v.noteRead(false);
        v.support().draw();
        v.noteRead(true);
        assertEquals("2/4", v.progress());
        assertFalse(v.complete(0));
        v.offense().draw();
        v.support().draw();
        assertTrue(v.complete(0));
        assertEquals("4/4", v.progress());
    }

    @Test
    void theGrandLibraryForgivesOnePage() {
        Volume v = base();
        v.offense().draw();
        v.offense().draw();
        v.support().draw();
        assertFalse(v.complete(0));
        assertTrue(v.complete(1), "one page short is complete for the Grand Library");
        v.consume();
        assertFalse(v.complete(1), "consuming the volume empties the progress");
        assertEquals(0, v.pagesRead());
        assertEquals(4, v.pagesTotal(), "but the pages stay");
    }

    @Test
    void theDecksAreRebuiltOnlyWhenThePagesChange() {
        Volume v = base();
        v.offense().draw();
        PageDeck kept = v.offense();
        v.ensure(List.of("bird", "needles"), List.of("wall", "verse"));
        assertSame(kept, v.offense(), "the same pages keep the same deck and its progress");
        assertEquals(1, v.offense().readCount());
        v.ensure(List.of("bird", "needles", "feathers"), List.of("wall", "verse"));
        assertEquals(0, v.offense().readCount(), "a new offence chapter starts unread");
        assertEquals(3, v.offense().size());
        assertEquals(2, v.support().size());
    }

    @Test
    void turningTheTurnsTheChapterReadLast() {
        Volume v = base();
        assertEquals("bird", v.turn(), "nothing read yet: the offence chapter");
        assertEquals("needles", v.offense().next());
        v.noteRead(true);
        assertEquals("wall", v.turn(), "after reading the support chapter, that one turns");
        assertEquals("verse", v.support().next());
        assertEquals(0, v.pagesRead(), "turning reads nothing");
        v.consume();
        assertEquals("bird", v.turn(), "a consumed volume goes back to the offence chapter");
    }
}
