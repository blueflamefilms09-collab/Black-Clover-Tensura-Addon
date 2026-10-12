package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The checked move list: unique ids, stable order, lookups by id and power. */
class MoveBookTest {
    static MoveSchedulerTest.Spec spec(String id, int minPower, boolean ultimate) {
        return new MoveSchedulerTest.Spec(id, minPower, 0, 30, 1, ultimate);
    }

    @Test
    void repeatsAndNullsAreDroppedFirstOneStays() {
        List<MoveSchedulerTest.Spec> raw = new ArrayList<>(Arrays.asList(spec("a", 20, false), null, spec("b", 40, false), spec("a", 60, true), spec("c", 60, true)));
        MoveBook<MoveSchedulerTest.Spec> book = MoveBook.of(raw);
        assertEquals(3, book.size());
        assertEquals(List.of("<null>", "a"), book.dropped());
        assertEquals(20, book.byId("a").minPower(), "the first 'a' stays");
        assertEquals(0, book.indexOf("a"));
        assertEquals(1, book.indexOf("b"));
        assertEquals(2, book.indexOf("c"));
        assertEquals(-1, book.indexOf("zzz"));
        assertNull(book.byId("zzz"));
        assertNull(book.get(-1));
        assertNull(book.get(3));
        assertSame(book.get(1), book.byId("b"));
    }

    @Test
    void usableByPowerAndUltimates() {
        MoveBook<MoveSchedulerTest.Spec> book = MoveBook.of(List.of(spec("a", 20, false), spec("b", 40, false), spec("u", 60, true)));
        assertEquals(1, book.usableAt(20).size());
        assertEquals(1, book.usableAt(39).size());
        assertEquals(2, book.usableAt(40).size());
        assertEquals(3, book.usableAt(100).size());
        assertEquals(List.of("u"), book.ultimates().stream().map(MoveSchedulerTest.Spec::id).toList());
    }

    @Test
    void theBookIsImmutableAndTheNullListIsEmpty() {
        MoveBook<MoveSchedulerTest.Spec> book = MoveBook.of(List.of(spec("a", 20, false)));
        assertThrows(UnsupportedOperationException.class, () -> book.moves().add(spec("x", 20, false)));
        assertEquals(0, MoveBook.<MoveSchedulerTest.Spec>of(null).size());
    }

    @Test
    void chooseReturnsTheMoveOrNothing() {
        MoveBook<MoveSchedulerTest.Spec> book = MoveBook.of(List.of(spec("a", 20, false)));
        MoveScheduler s = new MoveScheduler(1);
        s.reset(0);
        assertEquals("a", s.choose(book.moves(), MoveSchedulerTest.at(1000, 5, 20)).orElseThrow().id());
        assertTrue(s.choose(book.moves(), MoveSchedulerTest.at(1000, 500, 20)).isEmpty());
    }
}
