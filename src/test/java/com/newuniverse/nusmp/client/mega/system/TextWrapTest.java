package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a quest title is whatever a datapack author wrote, and the window has one fixed width. The wrap must never produce a line wider than the room, must not lose or
 * invent words, must cut a word that is wider than a whole line instead of overflowing, must stop at the line limit with an ellipsis that still fits, and must cope with the
 * blank and the odd (extra spaces, a null). Each of those is a way the window would look broken in play, so each is pinned here with a fixed-width "font" (6 pixels a
 * character).
 */
class TextWrapTest {
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;

    @Test void shortTextStaysOnOneLine() {
        assertEquals(List.of("Clear 5 rooms"), TextWrap.wrap("Clear 5 rooms", 200, SIX, 3));
    }

    @Test void wrapsOnSpacesAndEveryLineFits() {
        List<String> lines = TextWrap.wrap("Clear 5 rooms without taking magic damage", 150, SIX, 3);
        assertEquals(List.of("Clear 5 rooms without", "taking magic damage"), lines);
        for (String l : lines) assertTrue(SIX.applyAsInt(l) <= 150, l);
    }

    @Test void noWordIsLostOrInvented() {
        String text = "Survive the ambush: defeat 12 of the creatures that hunt you";
        List<String> lines = TextWrap.wrap(text, 120, SIX, 9);
        assertEquals(text, String.join(" ", lines));
        for (String l : lines) assertTrue(SIX.applyAsInt(l) <= 120, l);
    }

    @Test void aWordWiderThanTheLineIsCutBetweenCharacters() {
        List<String> lines = TextWrap.wrap("AAAAAAAAAAAAAAAAAAAAAAAAA end", 60, SIX, 9);
        for (String l : lines) assertTrue(SIX.applyAsInt(l) <= 60, l);
        assertEquals(List.of("AAAAAAAAAA", "AAAAAAAAAA", "AAAAA end"), lines);
    }

    @Test void tooManyLinesEndInAnEllipsisThatStillFits() {
        List<String> lines = TextWrap.wrap("one two three four five six seven eight nine ten eleven twelve", 60, SIX, 2);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith("..."), lines.get(1));
        assertTrue(SIX.applyAsInt(lines.get(1)) <= 60, lines.get(1));
        assertEquals("one two", lines.get(0).substring(0, 7));
    }

    @Test void blankNullAndSpacyTextsAreHandled() {
        assertTrue(TextWrap.wrap(null, 100, SIX, 3).isEmpty());
        assertTrue(TextWrap.wrap("", 100, SIX, 3).isEmpty());
        assertTrue(TextWrap.wrap("    ", 100, SIX, 3).isEmpty());
        assertEquals(List.of("a b c"), TextWrap.wrap("  a   b  c ", 100, SIX, 3));
    }

    @Test void silentLimitsNeverLoopOrThrow() {
        assertEquals(1, TextWrap.wrap("some text here", 100, SIX, 0).size(), "a limit of zero means one line");
        assertFalse(TextWrap.wrap("text", 0, SIX, 3).isEmpty(), "a room of zero still returns the text, one character a line");
        assertTrue(TextWrap.wrap("text", -5, SIX, 3).size() <= 3);
    }
}
