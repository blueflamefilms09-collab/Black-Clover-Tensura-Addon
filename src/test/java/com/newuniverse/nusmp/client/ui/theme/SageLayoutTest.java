package com.newuniverse.nusmp.client.ui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Great-Sage text block's wrapping and typewriter bookkeeping. */
class SageLayoutTest {
    private static final SageLayout.Metrics SIX = s -> s.length() * 6;       // every character 6 px wide

    @Test
    void wrapsOnWordsToTheWidth() {
        String[] l = SageLayout.wrap("Analysis complete. Result: nothing was found here.", SIX, 60);   // 10 chars per line
        for (String line : l) assertTrue(SIX.width(line) <= 60, "'" + line + "' fits");
        assertEquals("Analysis complete. Result: nothing was found here.", String.join(" ", l), "no word lost or invented");
    }

    @Test
    void aWordWiderThanTheLineIsCut() {
        String[] l = SageLayout.wrap("Supercalifragilistic", SIX, 36);                                   // 6 chars per line
        for (String line : l) assertTrue(SIX.width(line) <= 36);
        assertEquals("Supercalifragilistic", String.join("", l));
    }

    @Test
    void emptyAndNewlineInputsAreSafe() {
        assertEquals(1, SageLayout.wrap("", SIX, 60).length);
        assertEquals(1, SageLayout.wrap(null, SIX, 60).length);
        String[] two = SageLayout.wrap("a\nb", SIX, 60);
        assertEquals(2, two.length);
        assertEquals("a", two[0]);
        assertEquals(1, SageLayout.wrap("hello", SIX, 0).length > 0 ? 1 : 0, "a zero width still returns lines");
    }

    @Test
    void typewriterRevealsLineByLine() {
        String[] lines = {"abc", "de", "fghi"};
        assertEquals(9, SageLayout.totalChars(lines));
        assertEquals(0, SageLayout.visibleInLine(lines, 0, 0));
        assertEquals(2, SageLayout.visibleInLine(lines, 0, 2));
        assertEquals(3, SageLayout.visibleInLine(lines, 0, 7));
        assertEquals(0, SageLayout.visibleInLine(lines, 1, 3));
        assertEquals(1, SageLayout.visibleInLine(lines, 1, 4));
        assertEquals(4, SageLayout.visibleInLine(lines, 2, 9));
        assertEquals(0, SageLayout.visibleInLine(lines, 7, 9), "a line that does not exist shows nothing");
    }

    @Test
    void theCursorSitsOnTheLineBeingTyped() {
        String[] lines = {"abc", "de", "fghi"};
        assertEquals(0, SageLayout.cursorLine(lines, 0));
        assertEquals(0, SageLayout.cursorLine(lines, 2));
        assertEquals(1, SageLayout.cursorLine(lines, 3));
        assertEquals(2, SageLayout.cursorLine(lines, 5));
        assertEquals(2, SageLayout.cursorLine(lines, 9), "all typed: the last line");
        assertEquals(0, SageLayout.cursorLine(new String[]{""}, 0));
    }

    @Test
    void clampKeepsShortTextAndCutsLongTextWithDots() {
        assertEquals("short text", SageLayout.clamp("short text", SIX, 120, 2));
        String longText = "one two three four five six seven eight nine ten eleven twelve thirteen";
        String c = SageLayout.clamp(longText, SIX, 60, 2);                                              // 10 chars per line
        String[] lines = SageLayout.wrap(c, SIX, 60);
        assertEquals(2, lines.length, c);
        assertTrue(c.endsWith("..."), c);
        for (String line : lines) assertTrue(SIX.width(line) <= 60, "'" + line + "' fits");
        assertEquals("", SageLayout.clamp(null, SIX, 60, 2));
        assertEquals(1, SageLayout.wrap(SageLayout.clamp(longText, SIX, 60, 0), SIX, 60).length, "at least one line");
    }
}
