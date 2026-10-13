package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Which 128-block cells hold a floor boss arena (a Goliath's Pit, a Charybdis Basin): a stable hash, apart from the core cells and from each other. */
class DungeonArenaTest {
    /** The first ten pits and basins of the window x, z in -12..12, scanning x then z from the low side: the salts (19 and 23) are part of the saved world. */
    private static final int[][] PITS = {{-12, -12}, {-6, -11}, {-1, -11}, {4, -11}, {-6, -10}, {4, -9}, {1, -8}, {-9, -7}, {12, -7}, {-7, -6}};
    private static final int[][] BASINS = {{1, -12}, {6, -12}, {9, -11}, {3, -10}, {7, -10}, {0, -9}, {-12, -7}, {-4, -7}, {-3, -7}, {3, -7}};

    @Test
    void theClassesAreNamedAsTheyAreStored() {
        assertEquals(0, DungeonTier.ARENA_NONE);
        assertEquals(1, DungeonTier.ARENA_GOLIATH);
        assertEquals(2, DungeonTier.ARENA_CHARYBDIS);
    }

    @Test
    void theHashIsStableAndPinned() {
        for (int[] c : PITS) assertEquals(DungeonTier.ARENA_GOLIATH, DungeonTier.arenaOf(c[0], c[1]), "pit at " + c[0] + "," + c[1]);
        for (int[] c : BASINS) assertEquals(DungeonTier.ARENA_CHARYBDIS, DungeonTier.arenaOf(c[0], c[1]), "basin at " + c[0] + "," + c[1]);
        int pits = 0, basins = 0;
        for (int cz = -12; cz <= 12; cz++) {
            for (int cx = -12; cx <= 12; cx++) {
                int a = DungeonTier.arenaOf(cx, cz);
                assertEquals(a, DungeonTier.arenaOf(cx, cz), "the same cell gives the same answer");
                assertTrue(a >= 0 && a <= 2, "0, 1 or 2: " + a);
                if (a == 1) pits++;
                if (a == 2) basins++;
            }
        }
        assertTrue(pits >= PITS.length && basins >= BASINS.length);
    }

    @Test
    void aCoreCellIsNeverAnArena() {
        int core = 0;
        for (int cx = -150; cx < 150; cx++) {
            for (int cz = -150; cz < 150; cz++) {
                if (!DungeonTier.isCityCell(cx, cz)) continue;
                core++;
                assertEquals(DungeonTier.ARENA_NONE, DungeonTier.arenaOf(cx, cz), "core cell " + cx + "," + cz);
            }
        }
        assertTrue(core > 10_000, "plenty of core cells were looked at: " + core);
    }

    /** About 8% of the cells that are not core cells hold a pit and about 8% a basin (a little less: one cell cannot be both). */
    private static void density(int x0, int z0, int size) {
        long rest = 0, pit = 0, basin = 0, all = (long) size * size;
        for (int cx = x0; cx < x0 + size; cx++) {
            for (int cz = z0; cz < z0 + size; cz++) {
                if (DungeonTier.isCityCell(cx, cz)) continue;
                rest++;
                int a = DungeonTier.arenaOf(cx, cz);
                if (a == DungeonTier.ARENA_GOLIATH) pit++;
                if (a == DungeonTier.ARENA_CHARYBDIS) basin++;
            }
        }
        double pitShare = 100.0 * pit / rest, basinShare = 100.0 * basin / rest;
        assertTrue(pitShare >= 6 && pitShare <= 10, "pits are " + pitShare + "% of the cells that are not core cells at " + x0 + "," + z0);
        assertTrue(basinShare >= 6 && basinShare <= 10, "basins are " + basinShare + "% of the cells that are not core cells at " + x0 + "," + z0);
        assertTrue(100.0 * pit / all >= 5 && 100.0 * pit / all <= 10, "pits as a share of every cell: " + 100.0 * pit / all);
        assertTrue(100.0 * basin / all >= 5 && 100.0 * basin / all <= 10, "basins as a share of every cell: " + 100.0 * basin / all);
    }

    @Test
    void aboutEightPercentOfTheRemainingCellsHoldEachKind() {
        density(-100, -100, 200);                       // the 200 x 200 window round the origin
        density(300, -500, 200);
        density(-900, 700, 200);
    }

    @Test
    void theBasinHasARollOfItsOwnApartFromThePit() {
        long left = 0, basin = 0;
        for (int cx = -150; cx < 150; cx++) {
            for (int cz = -150; cz < 150; cz++) {
                if (DungeonTier.isCityCell(cx, cz)) continue;
                int a = DungeonTier.arenaOf(cx, cz);
                if (a == DungeonTier.ARENA_GOLIATH) continue;
                left++;
                if (a == DungeonTier.ARENA_CHARYBDIS) basin++;
            }
        }
        assertTrue(100.0 * basin / left > 6.5 && 100.0 * basin / left < 9.5, "of the cells that are neither core cells nor pits about 8% are basins: " + 100.0 * basin / left);
    }

    @Test
    void theNegativeCellsAreHashedLikeTheRest() {
        int found = 0;
        for (int cx = -40; cx < 0; cx++) for (int cz = -40; cz < 0; cz++) if (DungeonTier.arenaOf(cx, cz) != 0) found++;
        assertTrue(found > 100, "arenas exist in the negative quadrant: " + found);
    }
}
