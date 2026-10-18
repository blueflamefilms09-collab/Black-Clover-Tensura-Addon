package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@link BookLook} is what makes a Skill Book of a line the client has not unlocked show the right cover, colour and name, and a wrong value there is silent in game (a grey cover
 * and a blank tooltip look "fine" to a tester who never saw the right one). This pins the pure parts: the table is parsed as the generator writes it, a book and a line id find the same
 * entry, the franchise property is exactly (index + 1) / steps (the model's overrides are generated with the same numbers and half-step thresholds, see BookArtAssetsTest), an unknown
 * book or franchise gives 0 (the generic cover), the colour comes from the known route first, then the table, then the fall-back, and a hand-damaged table never throws.
 */
class BookLookTest {
    private static final String SAMPLE = """
            {"franchises": ["exiled_heavy_knight", "tensura", "fairy_tail"], "prop_steps": 20,
             "lines": {
               "fire_dragon_slayer": {"book": "nusmp:fire_dragon_slayer_book", "name": "Fire Dragon Slayer", "book_name": "Heart of the Fire Dragon", "color": "#E8541C", "franchise": "fairy_tail"},
               "oath_of_the_heavy_armor": {"book": "nusmp:heavy_armor_book", "name": "Oath of the Heavy Armor", "book_name": "Oath of the Heavy Armor", "color": "#8E9AAF", "franchise": "exiled_heavy_knight"},
               "no_franchise": {"book": "nusmp:no_franchise_book", "name": "Plain", "color": "#102030", "franchise": ""}
             }}""";

    private static BookLook.Index sample() { return BookLook.parse(JsonParser.parseString(SAMPLE).getAsJsonObject()); }

    @Test
    void aBookAndALineIdFindTheSameEntry() {
        BookLook.Index idx = sample();
        assertEquals(3, idx.size());
        BookLook.Entry byBook = idx.byBook("nusmp:heavy_armor_book");
        assertNotNull(byBook, "the book id differs from the line id for this line: the table must be searchable by the BOOK id, which is what the stack carries");
        assertSame(byBook, idx.byLine("oath_of_the_heavy_armor"));
        assertSame(byBook, idx.byLine("nusmp:oath_of_the_heavy_armor"));
        assertEquals(0x8E9AAF, byBook.color());
        assertEquals("Oath of the Heavy Armor", byBook.bookName());
        assertNull(idx.byBook("nusmp:nothing_book"));
        assertNull(idx.byBook(null));
        assertNull(idx.byLine(null));
    }

    @Test
    void theFranchisePropertyIsIndexPlusOneOverSteps() {
        BookLook.Index idx = sample();
        assertEquals(1 / 20f, idx.franchiseValue("exiled_heavy_knight"), 1e-6);
        assertEquals(2 / 20f, idx.franchiseValue("tensura"), 1e-6);
        assertEquals(3 / 20f, idx.franchiseValue("fairy_tail"), 1e-6);
        assertEquals(0f, idx.franchiseValue("unknown"));
        assertEquals(0f, idx.franchiseValue(""));
        assertEquals(0f, idx.franchiseValue(null));
    }

    @Test
    void theHalfStepThresholdsOfTheModelNeverMissAFranchise() {
        // the model overrides use thresholds at (i + 1) / steps - 0.5 / steps and the game compares with >=, the last match wins: each value must satisfy its own threshold and fail the next one
        BookLook.Index idx = sample();
        int steps = idx.steps();
        for (int i = 0; i < idx.franchises().size(); i++) {
            float v = idx.franchiseValue(idx.franchises().get(i));
            float own = (i + 1f) / steps - 0.5f / steps, next = (i + 2f) / steps - 0.5f / steps;
            assertTrue(v >= own, "franchise " + i + " must satisfy its own override");
            assertTrue(v < next, "franchise " + i + " must not satisfy the next override");
        }
    }

    @Test
    void stepsAreNeverFewerThanTheFranchisesNeed() {
        JsonObject o = JsonParser.parseString("{\"franchises\":[\"a\",\"b\",\"c\"],\"prop_steps\":2,\"lines\":{}}").getAsJsonObject();
        BookLook.Index idx = BookLook.parse(o);
        assertTrue(idx.steps() >= 4, "three franchises need index+1 up to 3 and a step above it: " + idx.steps());
        assertTrue(idx.franchiseValue("c") <= 1f, "a property value above 1 would be clamped by the game and select the wrong cover");
    }

    @Test
    void theColourComesFromTheRouteThenTheTableThenTheFallback() {
        BookLook.set(sample());
        assertEquals(0x112233, BookLook.colorOf("nusmp:fire_dragon_slayer_book", 0x112233, 0x8A8A9A), "the client's own route wins");
        assertEquals(0xE8541C, BookLook.colorOf("nusmp:fire_dragon_slayer_book", -1, 0x8A8A9A), "no route on the client: the shipped table");
        assertEquals(0x8A8A9A, BookLook.colorOf("nusmp:nothing_book", -1, 0x8A8A9A), "neither: the fall-back");
        assertEquals(0xABCDEF, BookLook.colorOf("x", 0x01ABCDEF, 0), "a route colour with stray high bits is cut to RGB");
    }

    @Test
    void theFranchiseOfAKnownRouteIsTheFallbackWhenTheTableHasNoEntry() {
        BookLook.set(sample());
        assertEquals(3 / 20f, BookLook.franchiseValue("nusmp:fire_dragon_slayer_book", null), 1e-6);
        assertEquals(2 / 20f, BookLook.franchiseValue("nusmp:a_datapack_book", "tensura"), 1e-6, "a datapack line of a known franchise takes the franchise's cover");
        assertEquals(2 / 20f, BookLook.franchiseValue("nusmp:no_franchise_book", "tensura"), 1e-6, "an entry with no franchise defers to the route's");
        assertEquals(0f, BookLook.franchiseValue("nusmp:a_datapack_book", null));
        assertEquals(0f, BookLook.franchiseValue(null, null));
    }

    @Test
    void aDamagedTableNeverThrows() {
        BookLook.Index a = BookLook.parse(JsonParser.parseString("{}").getAsJsonObject());
        assertEquals(0, a.size());
        BookLook.Index b = BookLook.parse(JsonParser.parseString("{\"franchises\":[1,{},\"x\"],\"lines\":{\"a\":5,\"b\":{\"book\":\"\"},\"c\":{\"book\":\"nusmp:c_book\",\"color\":\"zzz\"},\"d\":{\"book\":\"nusmp:d_book\",\"color\":\"12\"}}}").getAsJsonObject());
        assertEquals(2, b.size(), "a line with no book, and one that is not an object, are skipped");
        assertEquals(0xFFFFFF, b.byBook("nusmp:c_book").color(), "a bad colour is white");
        assertEquals(0xFFFFFF, b.byBook("nusmp:d_book").color());
        assertEquals("c", b.byBook("nusmp:c_book").name(), "a missing name falls back to the line id");
        assertEquals(2, b.franchises().size(), "primitive entries of the franchise list are kept as text, the object is dropped");
    }

    @Test
    void theEmptyTableAnswersHarmlessly() {
        BookLook.set(BookLook.Index.EMPTY);                       // the other tests install a table: start from nothing
        assertEquals(0f, BookLook.franchiseValue("nusmp:anything", null));
        assertNull(BookLook.byBook("nusmp:anything"));
        assertEquals(7, BookLook.colorOf("nusmp:anything", -1, 7));
    }
}
