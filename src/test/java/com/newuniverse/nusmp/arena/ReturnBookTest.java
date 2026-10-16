package com.newuniverse.nusmp.arena;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The return positions: a round trip through plain strings, garbage, the size bound. */
class ReturnBookTest {
    @Test
    void anEntryRoundTripsThroughItsString() {
        ReturnBook.Entry e = new ReturnBook.Entry("minecraft:the_nether", -1234.5, 64.0, 99.25, 135.5f, -12.5f);
        ReturnBook.Entry back = ReturnBook.Entry.decode(e.encode());
        assertEquals(e, back);
    }

    @Test
    void aBookRoundTripsThroughStringsForTheSavedData() {
        ReturnBook b = new ReturnBook();
        UUID a = UUID.randomUUID(), c = UUID.randomUUID();
        b.put(a, new ReturnBook.Entry("minecraft:overworld", 1, 2, 3, 4, 5));
        b.put(c, new ReturnBook.Entry("nusmp:dream", -1e7, 300.125, 0.5, -90f, 0f));
        Map<String, String> saved = b.toStrings();
        ReturnBook loaded = new ReturnBook();
        loaded.fromStrings(saved);
        assertEquals(2, loaded.size());
        assertEquals(b.get(a), loaded.get(a));
        assertEquals(b.get(c), loaded.get(c));
    }

    @Test
    void takingAnEntryRemovesIt() {
        ReturnBook b = new ReturnBook();
        UUID a = UUID.randomUUID();
        b.put(a, new ReturnBook.Entry("minecraft:overworld", 1, 2, 3, 4, 5));
        assertTrue(b.has(a));
        assertNotNull(b.take(a));
        assertFalse(b.has(a));
        assertNull(b.take(a));
    }

    @Test
    void aSecondEntryReplacesTheFirst() {
        ReturnBook b = new ReturnBook();
        UUID a = UUID.randomUUID();
        b.put(a, new ReturnBook.Entry("minecraft:overworld", 1, 2, 3, 4, 5));
        b.put(a, new ReturnBook.Entry("minecraft:overworld", 9, 9, 9, 0, 0));
        assertEquals(1, b.size());
        assertEquals(9, b.get(a).x());
    }

    @Test
    void garbageIsDroppedNotThrown() {
        assertNull(ReturnBook.Entry.decode(null));
        assertNull(ReturnBook.Entry.decode(""));
        assertNull(ReturnBook.Entry.decode("minecraft:overworld|1|2|3|4"));
        assertNull(ReturnBook.Entry.decode("minecraft:overworld|a|2|3|4|5"));
        assertNull(ReturnBook.Entry.decode("|1|2|3|4|5"));
        assertNull(ReturnBook.Entry.decode("minecraft:overworld|NaN|2|3|4|5"));
        assertNull(ReturnBook.Entry.decode("minecraft:overworld|1|Infinity|3|4|5"));
        ReturnBook b = new ReturnBook();
        b.fromStrings(Map.of("not-a-uuid", "minecraft:overworld|1|2|3|4|5", UUID.randomUUID().toString(), "junk"));
        assertEquals(0, b.size());
        b.fromStrings(null);
        assertEquals(0, b.size());
        b.put(null, new ReturnBook.Entry("a:b", 1, 2, 3, 4, 5));
        b.put(UUID.randomUUID(), null);
        assertEquals(0, b.size());
    }

    @Test
    void theBookIsBoundedAndTheOldestGoesFirst() {
        ReturnBook b = new ReturnBook();
        UUID first = UUID.randomUUID();
        b.put(first, new ReturnBook.Entry("minecraft:overworld", 0, 0, 0, 0, 0));
        for (int i = 0; i < ReturnBook.MAX; i++) b.put(UUID.randomUUID(), new ReturnBook.Entry("minecraft:overworld", i, 0, 0, 0, 0));
        assertEquals(ReturnBook.MAX, b.size());
        assertFalse(b.has(first));
    }
}
