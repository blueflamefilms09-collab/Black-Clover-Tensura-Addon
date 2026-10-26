package com.newuniverse.nusmp.skilltree.unique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.item.BookLook;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the rarity and the background theme of a book ride along in the same shipped table the registry and the item look already read ({@code skill_books.json}), in two new fields of every line. Both
 * readers must take a table written BEFORE 3.26.0 (no such fields: common, no theme), a damaged value (an unknown rarity, a number where a word belongs: common, never an exception), and the new table,
 * and the 11-argument and 5-argument constructors of {@link LineIndex.Line} that older code and tests call must still compile and mean "common".
 */
class LineIndexRarityTest {
    private static JsonObject table(String extra) {
        return JsonParser.parseString("{\"franchises\":[\"tensura\"],\"prop_steps\":20,\"lines\":{\"a_line\":{\"book\":\"nusmp:a_line_book\",\"name\":\"A\",\"book_name\":\"Book A\",\"color\":\"#102030\",\"franchise\":\"tensura\""
                + extra + "}}}").getAsJsonObject();
    }

    @Test
    void aTableWithoutTheFieldsIsCommonWithNoTheme() {
        LineIndex.Line l = LineIndex.parse(table("")).get(0);
        assertSame(BookRarity.COMMON, l.rarity());
        assertEquals("", l.theme());
        BookLook.Entry e = BookLook.parse(table("")).byLine("a_line");
        assertSame(BookRarity.COMMON, e.rarity());
    }

    @Test
    void theNewFieldsAreRead() {
        JsonObject t = table(",\"rarity\":\"mythic\",\"theme\":\"magma\"");
        LineIndex.Line l = LineIndex.parse(t).get(0);
        assertSame(BookRarity.MYTHIC, l.rarity());
        assertEquals("magma", l.theme());
        assertSame(BookRarity.MYTHIC, BookLook.parse(t).byLine("a_line").rarity());
        assertSame(BookRarity.MYTHIC, BookLook.parse(t).byBook("nusmp:a_line_book").rarity());
    }

    @Test
    void aDamagedValueIsCommonAndNeverThrows() {
        for (String bad : new String[] {",\"rarity\":\"legendary\"", ",\"rarity\":5", ",\"rarity\":null", ",\"rarity\":\"\"", ",\"rarity\":[\"epic\"]", ",\"theme\":7"}) {
            LineIndex.Line l = LineIndex.parse(table(bad)).get(0);
            assertSame(BookRarity.COMMON, l.rarity(), bad);
            assertNotNull(l.theme(), bad);
            assertSame(BookRarity.COMMON, BookLook.parse(table(bad)).byLine("a_line").rarity(), bad);
        }
    }

    @Test
    void theOlderConstructorsStillMeanCommon() {
        LineIndex.Line l = new LineIndex.Line("a", "nusmp:a", "A", 0x102030, "tensura", "nusmp:a_book", "Book A", "d", "l", java.util.List.of(), java.util.List.of());
        assertSame(BookRarity.COMMON, l.rarity());
        assertEquals("", l.theme());
        BookLook.Entry e = new BookLook.Entry("a", "nusmp:a_book", "A", "Book A", 0x102030, "tensura");
        assertSame(BookRarity.COMMON, e.rarity());
        LineIndex.Line withNull = new LineIndex.Line("a", "nusmp:a", "A", 0x102030, "tensura", "nusmp:a_book", "Book A", "d", "l", java.util.List.of(), java.util.List.of(), null, null);
        assertSame(BookRarity.COMMON, withNull.rarity(), "a null rarity is common");
        assertEquals("", withNull.theme());
    }

    @Test
    void theModelValueOfABookIsTheRaritysOrZeroWhenUnknown() {
        assertEquals(0f, BookLook.rarityValue("nusmp:no_such_book_anywhere"), 0.0f);
        assertEquals(0f, BookLook.rarityValue(null), 0.0f);
        assertSame(BookRarity.COMMON, BookLook.rarityOf("nusmp:no_such_book_anywhere"));
        assertSame(BookRarity.COMMON, BookLook.rarityOf(null));
    }
}
