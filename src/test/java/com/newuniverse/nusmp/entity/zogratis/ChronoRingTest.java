package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lucius's snapshot ring: bounded, newest-wins, rewinds as far as it remembers, forgets an undone future. */
class ChronoRingTest {
    private static ChronoRing filled(int capacity, int snaps, int every) {
        ChronoRing r = new ChronoRing(capacity);
        for (int i = 0; i < snaps; i++) r.add(i * every, i, 64, -i, 100 - i);
        return r;
    }

    @Test
    void startsEmptyAndAnswersNothing() {
        ChronoRing r = new ChronoRing(8);
        assertTrue(r.isEmpty());
        assertNull(r.newest());
        assertNull(r.oldest());
        assertNull(r.at(100));
        assertNull(r.rewind(100, 40));
    }

    @Test
    void keepsTheNewestAndOverwritesTheOldest() {
        ChronoRing r = filled(5, 12, 2);                       // ticks 0..22, only the last five remain
        assertEquals(5, r.size());
        assertEquals(22, r.newest().tick());
        assertEquals(14, r.oldest().tick());
        assertNull(r.at(12), "older than anything remembered");
        assertEquals(14, r.at(15).tick(), "the newest one at or before the tick");
        assertEquals(16, r.at(16).tick());
        assertEquals(22, r.at(1000).tick());
    }

    @Test
    void rewindGoesAsFarBackAsItRemembers() {
        ChronoRing r = filled(10, 10, 2);                      // ticks 0..18
        assertEquals(8, r.rewind(18, 10).tick(), "10 ticks before 18");
        assertEquals(0, r.rewind(18, 18).tick());
        assertEquals(0, r.rewind(18, 500).tick(), "farther than it remembers: the oldest moment");
        assertEquals(18, r.rewind(18, 0).tick(), "no ticks back is now");
        assertEquals(18, r.rewind(18, -5).tick(), "a negative number is treated as zero");
    }

    @Test
    void anOlderTickMeansTimeWentBackwardsAndDropsTheNewer() {
        ChronoRing r = filled(10, 10, 2);                      // ticks 0..18
        r.add(9, 1, 2, 3, 50);                                 // time ran back to tick 9
        assertEquals(9, r.newest().tick());
        assertEquals(6, r.size() , "ticks 0,2,4,6,8 and the new 9");
        assertEquals(8, r.at(9 - 1).tick());
    }

    @Test
    void truncateAfterForgetsTheUndoneFuture() {
        ChronoRing r = filled(10, 10, 2);
        r.truncateAfter(10);
        assertEquals(10, r.newest().tick());
        assertEquals(6, r.size());
        r.truncateAfter(-1);
        assertTrue(r.isEmpty());
        r.add(100, 1, 1, 1, 1);
        assertEquals(1, r.size(), "usable again after being emptied");
    }

    @Test
    void clearEmptiesTheRing() {
        ChronoRing r = filled(4, 4, 1);
        r.clear();
        assertTrue(r.isEmpty());
        assertNull(r.newest());
        r.add(5, 0, 0, 0, 1);
        assertEquals(5, r.oldest().tick());
    }

    @Test
    void capacityHasAFloorOfTwo() {
        assertEquals(2, new ChronoRing(0).capacity());
        assertEquals(2, new ChronoRing(-5).capacity());
    }

    @Test
    void rewoundHealthNeverLowersAndIsCapped() {
        assertEquals(500f, ChronoRing.rewoundHealth(500f, 400f, 1000f, 0.1), "a rewind to a worse moment keeps the better health");
        assertEquals(500f, ChronoRing.rewoundHealth(500f, 500f, 1000f, 0.1));
        assertEquals(600f, ChronoRing.rewoundHealth(500f, 900f, 1000f, 0.1), "at most 10% of the maximum back");
        assertEquals(560f, ChronoRing.rewoundHealth(500f, 560f, 1000f, 0.1), "less than the cap: what was remembered");
        assertEquals(1000f, ChronoRing.rewoundHealth(990f, 2000f, 1000f, 0.5), "never past the maximum");
        assertEquals(500f, ChronoRing.rewoundHealth(500f, 900f, 1000f, 0.0), "a zero cap heals nothing");
        assertEquals(500f, ChronoRing.rewoundHealth(500f, 900f, 1000f, -1.0), "a negative cap is zero");
    }

    @Test
    void theCapacityTheBossUsesCoversSixSeconds() {
        int cap = ChronoMath.ringCapacity();
        ChronoRing r = new ChronoRing(cap);
        for (long t = 0; t < 1000; t += ChronoMath.SNAP_EVERY) r.add(t, 0, 0, 0, 1);
        assertTrue(r.newest().tick() - r.oldest().tick() >= 120, "six seconds of memory");
        assertTrue(r.newest().tick() - r.rewind(998, ChronoMath.rewindTicks(100)).tick() <= ChronoMath.rewindTicks(100) + ChronoMath.SNAP_EVERY);
    }
}
