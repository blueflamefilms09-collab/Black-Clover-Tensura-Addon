package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Layout;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Para;
import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Row;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The relic tooltip never leaves the room it is given: wrapping, shrinking, paging. */
class RelicTooltipLayoutTest {
    static final RelicTooltipLayout.Metrics FONT = s -> s.length() * 6;

    static String words(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(i == 0 ? "" : " ").append("word").append(i % 10);
        return b.toString();
    }

    @Test
    void wrapNeverExceedsTheWidth() {
        for (int max : new int[]{60, 100, 180, 280}) {
            for (String line : RelicTooltipLayout.wrap(words(80), FONT, max)) assertTrue(FONT.width(line) <= max, line);
        }
    }

    @Test
    void wrapKeepsEveryWordInOrder() {
        String text = words(40);
        assertEquals(text, String.join(" ", RelicTooltipLayout.wrap(text, FONT, 120)));
    }

    @Test
    void aWordWiderThanTheLineIsCut() {
        List<String> rows = RelicTooltipLayout.wrap("supercalifragilisticexpialidocious", FONT, 60);
        assertTrue(rows.size() >= 3);
        for (String r : rows) assertTrue(FONT.width(r) <= 60);
        assertEquals("supercalifragilisticexpialidocious", String.join("", rows));
    }

    @Test
    void newlinesAndEmptyTextGiveRows() {
        assertEquals(List.of(""), RelicTooltipLayout.wrap("", FONT, 100));
        assertEquals(List.of(""), RelicTooltipLayout.wrap(null, FONT, 100));
        assertEquals(List.of("a", "", "b"), RelicTooltipLayout.wrap("a\n\nb", FONT, 100));
    }

    @Test
    void shortTextIsOnePageAtFullScale() {
        Layout l = RelicTooltipLayout.layout(List.of(new Para("A short line.", 0)), FONT, 280, 200, 10, 0.75f);
        assertEquals(1, l.pageCount());
        assertEquals(1f, l.scale(), 1e-4);
        assertTrue(l.width() <= 280);
        assertTrue(l.height() <= 200);
    }

    @Test
    void textShrinksBeforeItPages() {
        // at scale 1 it takes ~14 rows of 10 = 140 > 130, at 0.75 it takes fewer rows of 7.5 and fits
        Layout l = RelicTooltipLayout.layout(List.of(new Para(words(60), 0)), FONT, 140, 130, 10, 0.75f);
        assertEquals(1, l.pageCount());
        assertTrue(l.scale() < 1f && l.scale() >= 0.75f - 1e-4, "scale " + l.scale());
        assertTrue(l.height() <= 130, "height " + l.height());
        assertTrue(l.width() <= 140);
    }

    @Test
    void tooMuchTextPagesAtTheMinimumScaleAndEveryPageFits() {
        List<Para> paras = new ArrayList<>();
        for (int i = 0; i < 12; i++) { paras.add(new Para(words(50), 0)); paras.add(new Para("", 0)); }
        Layout l = RelicTooltipLayout.layout(paras, FONT, 200, 120, 10, 0.75f);
        assertTrue(l.pageCount() > 1, "pages " + l.pageCount());
        assertEquals(0.75f, l.scale(), 1e-4);
        assertTrue(l.height() <= 120, "height " + l.height());
        assertTrue(l.width() <= 200);
        int rows = 0;
        for (List<Row> p : l.pages()) {
            assertFalse(p.isEmpty());
            assertFalse(p.get(0).blank() && l.pages().indexOf(p) > 0, "a later page does not start blank");
            rows += p.size();
        }
        assertTrue(rows > 0);
        assertEquals("Page 2/" + l.pageCount(), RelicTooltipLayout.footer(1, l.pageCount()));
    }

    @Test
    void noWordIsLostWhenPaging() {
        String text = words(400);
        Layout l = RelicTooltipLayout.layout(List.of(new Para(text, 0)), FONT, 150, 90, 10, 0.75f);
        List<String> all = new ArrayList<>();
        for (List<Row> p : l.pages()) for (Row r : p) all.add(r.text());
        assertEquals(text, String.join(" ", all));
    }

    @Test
    void indentNarrowsTheLine() {
        Layout l = RelicTooltipLayout.layout(List.of(new Para(words(30), 1, 20)), FONT, 140, 400, 10, 0.75f);
        for (Row r : l.pages().get(0)) assertTrue(r.indent() + FONT.width(r.text()) <= 140);
    }

    @Test
    void pagesAreClamped() {
        assertEquals(0, RelicTooltipLayout.clampPage(-3, 4));
        assertEquals(3, RelicTooltipLayout.clampPage(9, 4));
        assertEquals(2, RelicTooltipLayout.clampPage(2, 4));
        assertEquals(0, RelicTooltipLayout.clampPage(2, 0));
    }
}
