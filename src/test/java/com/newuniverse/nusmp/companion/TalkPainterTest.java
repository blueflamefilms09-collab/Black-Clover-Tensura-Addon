package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.client.companion.TalkCanvas;
import com.newuniverse.nusmp.client.companion.TalkFrame;
import com.newuniverse.nusmp.client.companion.TalkLayout;
import com.newuniverse.nusmp.client.companion.TalkPainter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the painter is pure code behind a three-call canvas, so the wrap, the cut and what is drawn (and how much) can be checked without a game. */
class TalkPainterTest {
    /** A canvas that counts: six pixels a character, and it records the fills and the texts. */
    private static final class Rec implements TalkCanvas {
        final List<int[]> fills = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        final List<Integer> textColours = new ArrayList<>();

        @Override public void fill(int x1, int y1, int x2, int y2, int argb) { fills.add(new int[]{x1, y1, x2, y2, argb}); }

        @Override public void text(String s, int x, int y, int argb) { texts.add(s); textColours.add(argb); }

        @Override public int textWidth(String s) { return s.length() * 6; }
    }

    private static TalkFrame frame(int tab, List<TalkFrame.Row> rows, int scroll, int rapport) {
        return new TalkFrame("Charmy Pappitson", "Cotton and Food Mage", 0xF4EAD0, "Oh, hello! Did you bring snacks?", new String[]{"Talk", "Interact", "Bond", "Pouch"}, tab, rows, scroll, -1, -1, false,
                rapport, "a friend", "Stance: Guard", 0, 1, List.of("x", "", "", "", "", "", "", "", ""), -1, "Close", "");
    }

    private static List<TalkFrame.Row> rows(int n, int blockedFrom) {
        List<TalkFrame.Row> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new TalkFrame.Row("Row number " + i, i >= blockedFrom));
        return out;
    }

    @Test
    void wrapBreaksOnSpacesAndKeepsEveryWord() {
        Rec c = new Rec();
        List<String> lines = TalkPainter.wrap(c, "one two three four five six", 60, 10);       // ten characters a line
        for (String l : lines) assertTrue(l.length() * 6 <= 60, "'" + l + "' is too wide");
        assertEquals("one two three four five six", String.join(" ", lines));
    }

    @Test
    void wrapCutsAWordWiderThanTheLine() {
        Rec c = new Rec();
        List<String> lines = TalkPainter.wrap(c, "abcdefghijklmnopqrstuvwxyz", 60, 10);
        assertTrue(lines.size() >= 3);
        for (String l : lines) assertTrue(l.length() * 6 <= 60);
        assertEquals("abcdefghijklmnopqrstuvwxyz", String.join("", lines));
    }

    @Test
    void wrapStopsAtTheLineLimitWithAnEllipsis() {
        Rec c = new Rec();
        List<String> lines = TalkPainter.wrap(c, "word ".repeat(100), 60, 3);
        assertEquals(3, lines.size());
        assertTrue(lines.get(2).endsWith("..."));
        assertTrue(lines.get(2).length() * 6 <= 60);
    }

    @Test
    void wrapOfNothingIsNothing() {
        Rec c = new Rec();
        assertTrue(TalkPainter.wrap(c, "", 60, 3).isEmpty());
        assertTrue(TalkPainter.wrap(c, null, 60, 3).isEmpty());
        assertTrue(TalkPainter.wrap(c, "   ", 60, 3).isEmpty());
        assertTrue(TalkPainter.wrap(c, "a", 60, 0).isEmpty());
    }

    @Test
    void fitNeverExceedsTheWidth() {
        Rec c = new Rec();
        assertEquals("short", TalkPainter.fit(c, "short", 60));
        String cut = TalkPainter.fit(c, "a very long label indeed", 60);
        assertTrue(cut.length() * 6 <= 60 && cut.endsWith("..."));
        assertEquals("", TalkPainter.fit(c, null, 60));
    }

    @Test
    void mixOfTwoColoursIsBetweenThem() {
        assertEquals(0x000000, TalkPainter.mix(0, 0xFFFFFF, 0f));
        assertEquals(0xFFFFFF, TalkPainter.mix(0, 0xFFFFFF, 1f));
        assertEquals(0x7F7F7F, TalkPainter.mix(0, 0xFFFFFF, 0.5f));
    }

    @Test
    void thePaintDrawsTheHeaderTheTabsTheWordsAndOnlyTheVisibleRows() {
        Rec c = new Rec();
        TalkLayout L = TalkLayout.compute(427, 240, 0);
        TalkPainter.paint(c, L, frame(0, rows(40, 99), 0, -1));
        assertTrue(c.texts.contains("Charmy Pappitson") && c.texts.contains("Cotton and Food Mage"));
        for (String tab : new String[]{"Talk", "Interact", "Bond", "Pouch"}) assertTrue(c.texts.contains(tab), tab);
        assertTrue(c.texts.contains("Oh, hello! Did you bring snacks?"));
        assertTrue(c.texts.contains("Close"));
        int drawnRows = 0;
        for (String t : c.texts) if (t.startsWith("Row number")) drawnRows++;
        assertEquals(L.visibleRows, drawnRows, "only the visible rows are drawn, not all 40");
        assertTrue(c.texts.size() < 40, "far fewer text draws than rows");
        assertTrue(c.fills.size() < 120, "a bounded number of fills: " + c.fills.size());
    }

    @Test
    void scrollingShowsLaterRows() {
        Rec c = new Rec();
        TalkLayout L = TalkLayout.compute(427, 240, 0);
        TalkPainter.paint(c, L, frame(0, rows(30, 99), 10, -1));
        assertTrue(c.texts.contains("Row number 10"));
        assertFalse(c.texts.contains("Row number 9"));
        assertFalse(c.texts.contains("Row number " + (10 + L.visibleRows)));
    }

    @Test
    void aClosedRowIsDimmedAndTheBondTabShowsRapport() {
        Rec c = new Rec();
        TalkLayout L = TalkLayout.compute(427, 240, 2);
        TalkPainter.paint(c, L, frame(2, rows(3, 1), 0, 12));
        int open = c.textColours.get(c.texts.indexOf("Row number 0")), closed = c.textColours.get(c.texts.indexOf("Row number 1"));
        assertEquals(TalkPainter.INK, open);
        assertEquals(TalkPainter.BLOCKED, closed);
        boolean tier = false;
        for (String t : c.texts) if (t.contains("a friend") && t.contains("12")) tier = true;
        assertTrue(tier, "the tier and the number are written under the bar: " + c.texts);
        assertTrue(c.texts.contains("Stance: Guard"));
    }

    @Test
    void nothingIsDrawnOutsideThePanelEdge() {
        Rec c = new Rec();
        TalkLayout L = TalkLayout.compute(427, 240, 3);
        TalkPainter.paint(c, L, frame(3, rows(20, 99), 0, 30));
        for (int[] f : c.fills) {
            assertTrue(f[0] >= L.panel[0] - 1 && f[1] >= L.panel[1] - 1 && f[2] <= L.panel[0] + L.panel[2] + 1 && f[3] <= L.panel[1] + L.panel[3] + 1, "a fill sticks out of the panel: " + java.util.Arrays.toString(f));
        }
    }
}
