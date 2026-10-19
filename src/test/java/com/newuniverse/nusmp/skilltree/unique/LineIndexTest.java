package com.newuniverse.nusmp.skilltree.unique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the registry of unique skills is filled from the shipped table at start-up, and a registry that differs between the server and a client refuses the connection, so the parsing and the order of the table
 * are the first thing to pin. The tests prove that the table is read in id order whatever the file order, that a broken entry is skipped and never an error, that the arts and passives of an entry
 * are kept in order and are immutable, that colours and defaults are filled in, and that the REAL shipped table loads, has one skill per book with a valid unique path, and contains no removed magic.
 */
class LineIndexTest {
    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    @Test
    void theTableIsReadInIdOrderWhateverTheFileOrder() {
        List<LineIndex.Line> lines = LineIndex.parse(json("{\"lines\":{\"zeta\":{\"name\":\"Z\"},\"alpha\":{\"name\":\"A\"},\"mid_1\":{\"name\":\"M\"}}}"));
        assertEquals(List.of("alpha", "mid_1", "zeta"), lines.stream().map(LineIndex.Line::bare).toList());
    }

    @Test
    void aBrokenEntryIsSkippedAndNeverThrows() {
        List<LineIndex.Line> lines = LineIndex.parse(json("{\"lines\":{\"Bad Id\":{\"name\":\"x\"},\"fine\":{\"name\":\"F\"},\"not_object\":5,\"UPPER\":{}}}"));
        assertEquals(List.of("fine"), lines.stream().map(LineIndex.Line::bare).toList());
        assertTrue(LineIndex.parse(null).isEmpty());
        assertTrue(LineIndex.parse(json("{}")).isEmpty());
        assertTrue(LineIndex.parse(json("{\"lines\":[1,2]}")).isEmpty());
    }

    @Test
    void anEntryKeepsItsArtsAndPassivesInOrderAndGetsDefaults() {
        LineIndex.Line l = LineIndex.parse(json("{\"lines\":{\"x\":{\"name\":\"X\",\"color\":\"#102030\",\"franchise\":\"tensura\",\"arts\":[\"B\",\"A\"],\"passives\":[\"P\"],\"desc\":\"d\",\"book_lore\":\"l\"}}}")).get(0);
        assertEquals(List.of("B", "A"), l.arts());
        assertEquals(List.of("P"), l.passives());
        assertEquals(0x102030, l.color());
        assertEquals("nusmp:x", l.routeId());
        assertEquals("nusmp:x_book", l.book(), "the book id defaults to <line>_book");
        assertEquals("X", l.bookName(), "the book name defaults to the line name");
        assertEquals("line_x", l.skillPath());
        boolean threw = false;
        try {
            l.arts().add("C");
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assertTrue(threw, "the arts are immutable");
    }

    @Test
    void aBadColourIsWhite() {
        assertEquals(0xFFFFFF, LineIndex.color("nonsense"));
        assertEquals(0xFFFFFF, LineIndex.color("#12"));
        assertEquals(0xABCDEF, LineIndex.color("abcdef"));
    }

    @Test
    void theIdHelpersRoundTrip() {
        assertEquals("nusmp:x", LineIndex.routeId("x"));
        assertEquals("other:x", LineIndex.routeId("other:x"));
        assertEquals("x", LineIndex.bare("nusmp:x"));
        assertEquals("x", LineIndex.bare("x"));
        assertTrue(LineIndex.validBare("a_1"));
        assertTrue(!LineIndex.validBare("A") && !LineIndex.validBare("") && !LineIndex.validBare(null) && !LineIndex.validBare("a b"));
    }

    @Test
    void lookupsByBookAndByLine() {
        List<LineIndex.Line> lines = LineIndex.parse(json("{\"lines\":{\"x\":{\"name\":\"X\",\"book\":\"nusmp:tome\"},\"y\":{\"name\":\"Y\"}}}"));
        assertEquals("x", LineIndex.byBook(lines, "nusmp:tome").bare());
        assertEquals("y", LineIndex.byBook(lines, "nusmp:y_book").bare());
        assertNull(LineIndex.byBook(lines, "nusmp:none"));
        assertNull(LineIndex.byBook(lines, ""));
        assertEquals("y", LineIndex.byBare(lines, "nusmp:y").bare());
        assertEquals("x", LineIndex.byBare(lines, "x").bare());
        assertNull(LineIndex.byBare(lines, null));
    }

    @Test
    void theShippedTableLoadsOneValidBookPerLineWithNoRemovedMagic() {
        List<LineIndex.Line> lines = LineIndex.shipped();
        assertTrue(lines.size() >= 200, "the shipped books: " + lines.size());
        assertEquals("", LineIndex.problem());
        Set<String> paths = new HashSet<>(), books = new HashSet<>();
        List<String> sorted = new ArrayList<>();
        for (LineIndex.Line l : lines) {
            assertTrue(LineIndex.validBare(l.bare()), l.bare());
            assertTrue(paths.add(l.skillPath()), "two books share the skill path " + l.skillPath());
            assertTrue(books.add(l.book()), "two books share the book id " + l.book());
            assertTrue(l.skillPath().startsWith("line_"));
            assertTrue(!LegacyIds.isRemovedLine(l.bare()), l.bare() + " is a removed magic");
            assertTrue(!LegacyIds.isRemovedBook(l.book()), l.book() + " is a removed magic's book");
            assertTrue(!l.name().isEmpty() && !l.desc().isEmpty(), l.bare() + " has a name and a description");
            sorted.add(l.bare());
        }
        List<String> copy = new ArrayList<>(sorted);
        java.util.Collections.sort(copy);
        assertEquals(copy, sorted, "the registry order is the id order on every machine");
        assertNotNull(LineIndex.byBare(lines, "lady_luck"));
        assertTrue(LineIndex.known("nusmp:oath_of_the_heavy_armor"));
        assertTrue(!LineIndex.known("fire_dragon_slayer"));
    }
}
