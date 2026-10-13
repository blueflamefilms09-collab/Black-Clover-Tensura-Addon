package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Finding the nearest core cell, and the keys of the saved data. */
class NexusCellsTest {
    @Test
    void theNearestCellIsMeasuredToTheMiddleOfTheCell() {
        int[] near = NexusCells.nearest(100, 100, 3, (cx, cz) -> (cx == 0 && cz == 0) || (cx == 1 && cz == 1));
        assertArrayEquals(new int[]{0, 0}, near, "(100, 100) is nearer the middle (64, 64) than the middle (192, 192)");
        int[] other = NexusCells.nearest(140, 140, 3, (cx, cz) -> (cx == 0 && cz == 0) || (cx == 1 && cz == 1));
        assertArrayEquals(new int[]{1, 1}, other);
    }

    @Test
    void noCellInRangeIsNull() {
        assertNull(NexusCells.nearest(0, 0, 2, (cx, cz) -> cx == 9));
        assertNull(NexusCells.nearest(0, 0, 4, (cx, cz) -> false));
        assertArrayEquals(new int[]{2, 0}, NexusCells.nearest(0, 0, 2, (cx, cz) -> cx == 2 && cz == 0));
    }

    @Test
    void negativeColumnsFindTheirOwnCell() {
        assertArrayEquals(new int[]{-1, -1}, NexusCells.nearest(-1, -1, 0, (cx, cz) -> true));
        assertArrayEquals(new int[]{-2, 0}, NexusCells.nearest(-129, 5, 0, (cx, cz) -> true));
    }

    @Test
    void keysRoundTrip() {
        for (int[] c : new int[][]{{0, 0}, {-1, -1}, {5, -9}, {-300, 77}, {123456, -654321}, {Integer.MAX_VALUE / 200, Integer.MIN_VALUE / 200}}) {
            long key = NexusCells.key(c[0], c[1]);
            assertEquals(c[0], NexusCells.cellX(key));
            assertEquals(c[1], NexusCells.cellZ(key));
        }
        assertNotEquals(NexusCells.key(1, 2), NexusCells.key(2, 1));
    }
}
