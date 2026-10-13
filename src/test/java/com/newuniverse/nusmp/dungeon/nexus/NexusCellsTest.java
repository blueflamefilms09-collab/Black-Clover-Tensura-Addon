package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Finding the nearest cell that holds a Nexus, which cells those are, and the keys of the saved data. */
class NexusCellsTest {
    @Test
    void aNexusCellIsACoreCellOrAnArenaAndNothingElse() {
        int core = 0, arena = 0, plain = 0;
        for (int cx = -100; cx < 100; cx++) {
            for (int cz = -100; cz < 100; cz++) {
                boolean isCore = DungeonTier.isCityCell(cx, cz), isArena = DungeonTier.arenaOf(cx, cz) != DungeonTier.ARENA_NONE;
                assertEquals(isCore || isArena, NexusCells.isNexusCell(cx, cz), cx + "," + cz);
                assertFalse(isCore && isArena, "a core cell is never an arena: " + cx + "," + cz);
                if (isCore) core++;
                else if (isArena) arena++;
                else plain++;
            }
        }
        assertTrue(core > 0 && arena > 0 && plain > 0, core + " core, " + arena + " arena, " + plain + " plain");
    }

    @Test
    void theNearestNexusCellMayBeAnArenaAndTheCoreFilterStillFindsTheCore() {
        int[] anyNear = NexusCells.nearest(0, 0, 6, NexusCells::isNexusCell);
        int[] coreNear = NexusCells.nearest(0, 0, 6, DungeonTier::isCityCell);
        assertNotNull(anyNear);
        assertNotNull(coreNear);
        assertTrue(NexusCells.isNexusCell(anyNear[0], anyNear[1]));
        assertTrue(DungeonTier.isCityCell(coreNear[0], coreNear[1]));
        double dAny = Math.hypot(NexusZone.centre(anyNear[0]), NexusZone.centre(anyNear[1])), dCore = Math.hypot(NexusZone.centre(coreNear[0]), NexusZone.centre(coreNear[1]));
        assertTrue(dAny <= dCore + 1e-9, "the nearest Nexus of any kind is never farther than the nearest core cell");
    }

    @Test
    void anArenaIsOnlyBuiltForAPlayerNearItInHeight() {
        assertTrue(NexusCells.arenaInReach(40, 40));
        assertTrue(NexusCells.arenaInReach(40 + NexusCells.ARENA_REACH_Y, 40));
        assertTrue(NexusCells.arenaInReach(40 - NexusCells.ARENA_REACH_Y, 40));
        assertFalse(NexusCells.arenaInReach(40 + NexusCells.ARENA_REACH_Y + 1, 40));
        assertFalse(NexusCells.arenaInReach(40 - NexusCells.ARENA_REACH_Y - 1, 40));
        assertFalse(NexusCells.arenaInReach(-52, 41), "a player in the Core's layers does not build the pit above them");
        assertFalse(NexusCells.arenaInReach(Integer.MIN_VALUE, Integer.MAX_VALUE), "no overflow");
        assertEquals(70, NexusCells.ARENA_REACH_Y);
    }

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
