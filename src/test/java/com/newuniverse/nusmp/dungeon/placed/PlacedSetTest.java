package com.newuniverse.nusmp.dungeon.placed;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The memory of the player-placed blocks: packing, chunk index, the two caps (oldest first), clears, resizing, save order. */
class PlacedSetTest {
    @Test
    void packingRoundTripsEveryCornerOfTheWorld() {
        int[][] cells = {{0, 0, 0}, {5, 70, -9}, {-1, -64, -1}, {-30_000_000, -64, 29_999_999}, {29_999_999, 319, -30_000_000}, {16, 0, -16}, {-17, 100, 15}};
        for (int[] c : cells) {
            long k = PlacedSet.pack(c[0], c[1], c[2]);
            assertEquals(c[0], PlacedSet.unpackX(k), "x of " + c[0]);
            assertEquals(c[1], PlacedSet.unpackY(k), "y of " + c[1]);
            assertEquals(c[2], PlacedSet.unpackZ(k), "z of " + c[2]);
            long ck = PlacedSet.chunkOfKey(k);
            assertEquals(c[0] >> 4, PlacedSet.chunkX(ck));
            assertEquals(c[2] >> 4, PlacedSet.chunkZ(ck));
        }
    }

    @Test
    void packingMatchesTheBlockPosLayout() {
        // BlockPos.asLong: x in bits 38-63, z in bits 12-37, y in bits 0-11 (26, 26 and 12 bits); computed by hand here because this test has no game classes
        long expected = ((long) (123 & 0x3FFFFFF) << 38) | ((long) (-456 & 0x3FFFFFF) << 12) | (64 & 0xFFFL);
        assertEquals(expected, PlacedSet.pack(123, 64, -456));
    }

    @Test
    void addContainsRemove() {
        PlacedSet s = new PlacedSet();
        assertTrue(s.isEmpty());
        assertEquals(0, s.add(1, 2, 3));
        assertTrue(s.contains(1, 2, 3));
        assertFalse(s.contains(1, 2, 4));
        assertEquals(-1, s.add(1, 2, 3), "adding twice is reported, not duplicated");
        assertEquals(1, s.size());
        assertTrue(s.remove(1, 2, 3));
        assertFalse(s.remove(1, 2, 3));
        assertTrue(s.isEmpty());
        assertEquals(0, s.chunks().size(), "an emptied chunk leaves no index entry");
    }

    @Test
    void theGlobalCapDropsTheOldestAndKeepsTheNewest() {
        PlacedSet s = new PlacedSet(20, 20);
        List<Long> dropped = new ArrayList<>();
        for (int i = 0; i < 25; i++) s.addKey(PlacedSet.pack(i * 16, 70, 0), dropped::add);
        assertEquals(20, s.size());
        assertEquals(5, dropped.size());
        for (int i = 0; i < 5; i++) {
            assertFalse(s.contains(i * 16, 70, 0), "the oldest " + i + " is gone");
            assertEquals(PlacedSet.pack(i * 16, 70, 0), (long) dropped.get(i), "dropped in placing order");
        }
        assertTrue(s.contains(24 * 16, 70, 0), "the newest is kept");
        assertEquals(5, s.evictions());
        assertEquals(20, s.chunks().size(), "the chunk index followed the evictions");
    }

    @Test
    void thePerChunkCapOnlyTouchesItsOwnChunk() {
        PlacedSet s = new PlacedSet(1000, 16);
        for (int i = 0; i < 40; i++) s.add(i % 16, 70 + i / 16, 0);               // one chunk, 40 placements
        s.add(100, 70, 100);                                                       // another chunk
        assertEquals(16, s.chunkSize(0, 0));
        assertEquals(1, s.chunkSize(6, 6));
        assertFalse(s.contains(0, 70, 0), "the first block of the crowded chunk was dropped");
        assertTrue(s.contains(7, 72, 0), "the last block of the crowded chunk is kept");
        assertTrue(s.contains(100, 70, 100));
        assertEquals(17, s.size());
    }

    @Test
    void removeChunkAndBox() {
        PlacedSet s = new PlacedSet();
        s.add(1, 70, 1);
        s.add(2, 71, 2);
        s.add(40, 70, 40);
        s.add(-3, 70, -3);
        assertEquals(2, s.removeChunk(0, 0));
        assertFalse(s.contains(1, 70, 1));
        assertTrue(s.contains(40, 70, 40));
        assertEquals(0, s.removeChunk(0, 0));
        assertEquals(1, s.removeBox(30, 60, 30, 50, 80, 50), "a box (corners in any order) takes the marks inside it");
        assertEquals(0, s.removeBox(50, 80, 50, 30, 60, 30));
        assertTrue(s.contains(-3, 70, -3));
        assertEquals(1, s.removeBox(-10, 0, -10, 0, 100, 0), "a box over negative chunks");
        assertTrue(s.isEmpty());
    }

    @Test
    void aBoxKeepsWhatIsAboveOrBelowIt() {
        PlacedSet s = new PlacedSet();
        s.add(5, 10, 5);
        s.add(5, 90, 5);
        assertEquals(0, s.removeBox(0, 40, 0, 15, 60, 15));
        assertEquals(2, s.size());
    }

    @Test
    void chunkKeysAreOldestFirstAndMatchTheSaveOrder() {
        PlacedSet s = new PlacedSet();
        s.add(3, 70, 3);
        s.add(1, 70, 1);
        s.add(2, 70, 2);
        long[] chunk = s.chunkKeys(0, 0);
        assertTrue(java.util.Arrays.equals(new long[]{PlacedSet.pack(3, 70, 3), PlacedSet.pack(1, 70, 1), PlacedSet.pack(2, 70, 2)}, chunk));
        assertTrue(java.util.Arrays.equals(chunk, s.toArray()));
        PlacedSet back = PlacedSet.fromArray(s.toArray(), PlacedSet.DEFAULT_CAP, PlacedSet.DEFAULT_PER_CHUNK);
        assertTrue(java.util.Arrays.equals(s.toArray(), back.toArray()));
    }

    @Test
    void loadingUnderASmallerCapKeepsTheNewest() {
        PlacedSet big = new PlacedSet(100, 100);
        for (int i = 0; i < 50; i++) big.add(i, 70, 0);
        PlacedSet small = PlacedSet.fromArray(big.toArray(), 20, 20);
        assertEquals(20, small.size());
        assertTrue(small.contains(49, 70, 0));
        assertFalse(small.contains(29, 70, 0));
        assertTrue(small.contains(30, 70, 0));
    }

    @Test
    void resizeDropsTheOldestThatNoLongerFit() {
        PlacedSet s = new PlacedSet(100, 100);
        for (int i = 0; i < 40; i++) s.add(i, 70, 0);
        assertEquals(0, s.resize(100, 100), "same caps: nothing to do");
        int dropped = s.resize(25, 25);
        assertEquals(15, dropped);
        assertEquals(25, s.size());
        assertFalse(s.contains(14, 70, 0));
        assertTrue(s.contains(15, 70, 0));
        assertTrue(s.contains(39, 70, 0));
        assertEquals(25, s.cap());
    }

    @Test
    void capsAreClamped() {
        assertEquals(PlacedSet.MIN_CAP, new PlacedSet(0, 0).cap());
        assertEquals(PlacedSet.MAX_CAP, new PlacedSet(Integer.MAX_VALUE, 5).cap());
        assertEquals(1, new PlacedSet(100, 0).perChunkCap());
        assertEquals(100, new PlacedSet(100, 5000).perChunkCap(), "a per-chunk cap above the whole cap is the whole cap");
    }

    @Test
    void replaceChunkMakesTheMirrorMatchTheServer() {
        PlacedSet mirror = new PlacedSet();
        mirror.add(1, 70, 1);
        mirror.add(2, 70, 2);
        mirror.add(100, 70, 100);
        mirror.replaceChunk(0, 0, new long[]{PlacedSet.pack(3, 70, 3)});
        assertFalse(mirror.contains(1, 70, 1));
        assertTrue(mirror.contains(3, 70, 3));
        assertTrue(mirror.contains(100, 70, 100), "another chunk is untouched");
        mirror.replaceChunk(0, 0, new long[0]);
        assertEquals(1, mirror.size());
    }

    @Test
    void anEmptySetAnswersFalseWithoutLooking() {
        PlacedSet s = new PlacedSet();
        assertFalse(s.contains(0, 0, 0));
        assertEquals(0, s.chunkKeys(3, 3).length);
        assertEquals(0, s.chunkSize(3, 3));
        assertEquals(0, s.removeBox(0, 0, 0, 100, 100, 100));
    }
}
