package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Dungeon's memory of its rooms: bounded, forgetting the longest unseen, and saving as arrays. */
class RoomBookTest {
    @Test
    void aRoomIsRememberedAsRolled() {
        RoomBook b = new RoomBook();
        assertNull(b.lookup(7, 10));
        b.record(7, RoomBook.BARREN, 100);
        b.record(8, RoomBook.BORN, 200);
        assertEquals(RoomBook.BARREN, b.lookup(7, 300).state);
        assertEquals(RoomBook.BORN, b.lookup(8, 300).state);
        assertEquals(200, b.lookup(8, 300).lastBirth, "a birth stamps its time");
        assertEquals(0, b.lookup(7, 300).lastBirth, "a barren room never births");
        assertEquals(300, b.lookup(7, 300).lastSeen, "looking marks it seen");
        assertEquals(2, b.size());
    }

    @Test
    void recordingAgainKeepsOneRoom() {
        RoomBook b = new RoomBook();
        b.record(5, RoomBook.BORN, 100);
        b.record(5, RoomBook.BORN, 900);
        assertEquals(1, b.size());
        assertEquals(900, b.lookup(5, 900).lastBirth);
    }

    @Test
    void whenFullTheRoomLongestUnseenIsForgotten() {
        RoomBook b = new RoomBook(3);
        b.record(1, RoomBook.BARREN, 1);
        b.record(2, RoomBook.BARREN, 2);
        b.record(3, RoomBook.BARREN, 3);
        assertNotNull(b.lookup(1, 4), "seen again: it is now the newest");
        b.record(4, RoomBook.BORN, 5);
        assertEquals(3, b.size());
        assertNull(b.lookup(2, 6), "2 was the longest unseen");
        assertNotNull(b.lookup(1, 6));
        assertNotNull(b.lookup(3, 6));
        assertNotNull(b.lookup(4, 6));
    }

    @Test
    void theBookNeverHoldsMoreThanItsLimit() {
        RoomBook b = new RoomBook(500);
        for (int i = 0; i < 5000; i++) b.record(i, i % 2 == 0 ? RoomBook.BARREN : RoomBook.BORN, i);
        assertEquals(500, b.size());
        assertNotNull(b.lookup(4999, 6000));
        assertNull(b.lookup(0, 6000));
        assertEquals(100_000, RoomBook.MAX_ROOMS);
    }

    @Test
    void forgettingTakesTheRoomsThatPassTheTest() {
        RoomBook b = new RoomBook();
        for (int i = 0; i < 10; i++) b.record(i, RoomBook.BORN, i);
        assertEquals(4, b.forget(k -> k >= 3 && k <= 6));
        assertEquals(6, b.size());
        assertNull(b.lookup(4, 99));
        assertNotNull(b.lookup(7, 99));
        assertEquals(0, b.forget(k -> k > 1000));
    }

    @Test
    void countsByState() {
        RoomBook b = new RoomBook();
        b.record(1, RoomBook.BARREN, 1);
        b.record(2, RoomBook.BORN, 1);
        b.record(3, RoomBook.BORN, 1);
        assertEquals(1, b.count(RoomBook.BARREN));
        assertEquals(2, b.count(RoomBook.BORN));
    }

    @Test
    void theArraysRoundTripInTheSameOrder() {
        RoomBook b = new RoomBook();
        b.record(10, RoomBook.BORN, 100);
        b.record(-20, RoomBook.BARREN, 200);
        b.record(30, RoomBook.BORN, 300);
        b.lookup(10, 400);                                              // 10 is now the newest
        RoomBook.Saved s = b.toArrays();
        assertArrayEquals(new long[]{-20, 30, 10}, s.keys());
        RoomBook back = RoomBook.fromArrays(s.keys(), s.births(), s.seen(), s.states());
        assertEquals(3, back.size());
        assertEquals(b.keys(), back.keys());
        assertEquals(RoomBook.BORN, back.lookup(10, 500).state);
        assertEquals(100, back.lookup(10, 500).lastBirth);
        assertEquals(RoomBook.BARREN, back.lookup(-20, 500).state);
        assertEquals(0, back.lookup(-20, 500).lastBirth);
    }

    @Test
    void damagedArraysLoadWhatTheyCanAndNeverThrow() {
        RoomBook b = RoomBook.fromArrays(new long[]{1, 2, 3}, new long[]{0, 0}, new long[]{0, 0, 0}, new byte[]{RoomBook.BORN, 9, RoomBook.BARREN});
        assertEquals(1, b.size(), "two entries have data, one of them has a state that is neither");
        assertNotNull(b.lookup(1, 5));
        assertEquals(0, RoomBook.fromArrays(new long[0], new long[0], new long[0], new byte[0]).size());
    }
}
