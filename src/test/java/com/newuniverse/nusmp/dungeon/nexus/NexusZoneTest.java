package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The four zones of the Variable Core: the pick per core cell is an even, stable hash that server and clients agree on. */
class NexusZoneTest {
    private static Map<NexusZone, Integer> count() {
        Map<NexusZone, Integer> n = new EnumMap<>(NexusZone.class);
        for (NexusZone z : NexusZone.values()) n.put(z, 0);
        return n;
    }

    @Test
    void theHashIsStableAndNeverNull() {
        for (int cx = -60; cx <= 60; cx++) {
            for (int cz = -60; cz <= 60; cz++) {
                NexusZone z = NexusZone.of(cx, cz);
                assertNotNull(z);
                assertSame(z, NexusZone.of(cx, cz), "the same cell gives the same zone");
            }
        }
    }

    @Test
    void theZonesAreEvenOverTheMap() {
        Map<NexusZone, Integer> n = count();
        int total = 0;
        for (int cx = -150; cx < 150; cx++) {
            for (int cz = -150; cz < 150; cz++) {
                n.merge(NexusZone.of(cx, cz), 1, Integer::sum);
                total++;
            }
        }
        for (NexusZone z : NexusZone.values()) {
            double share = n.get(z) / (double) total;
            assertEquals(0.25, share, 0.02, z + " holds about a quarter of all cells: " + share);
        }
    }

    @Test
    void theZonesAreEvenAmongTheCoreCellsThemselves() {
        Map<NexusZone, Integer> n = count();
        int core = 0;
        for (int cx = -300; cx < 300; cx++) {
            for (int cz = -300; cz < 300; cz++) {
                if (!DungeonTier.isCityCell(cx, cz)) continue;
                n.merge(NexusZone.of(cx, cz), 1, Integer::sum);
                core++;
            }
        }
        assertTrue(core > 50_000, "about 18% of 360000 cells are core cells: " + core);
        for (NexusZone z : NexusZone.values()) {
            double share = n.get(z) / (double) core;
            assertEquals(0.25, share, 0.02, z + " holds about a quarter of the core cells: " + share);
        }
    }

    @Test
    void neighbouringCellsDoNotShareAZone() {
        int same = 0, pairs = 0;
        for (int cx = -80; cx < 80; cx++) {
            for (int cz = -80; cz < 80; cz++) {
                pairs += 2;
                if (NexusZone.of(cx, cz) == NexusZone.of(cx + 1, cz)) same++;
                if (NexusZone.of(cx, cz) == NexusZone.of(cx, cz + 1)) same++;
            }
        }
        assertEquals(0.25, same / (double) pairs, 0.02, "neighbours match only by chance, about one in four");
    }

    @Test
    void aColumnTakesTheZoneOfItsCell() {
        for (int x = -400; x <= 400; x += 7) {
            for (int z = -400; z <= 400; z += 11) {
                assertSame(NexusZone.of(Math.floorDiv(x, 128), Math.floorDiv(z, 128)), NexusZone.at(x, z), x + "," + z);
            }
        }
        for (int cell = -3; cell <= 3; cell++) {
            NexusZone inside = NexusZone.at(cell * 128, 5);
            assertSame(inside, NexusZone.at(cell * 128 + 127, 5), "the whole cell is one zone");
            assertSame(NexusZone.of(cell, 0), inside);
            assertSame(NexusZone.of(cell - 1, 0), NexusZone.at(cell * 128 - 1, 5), "the column just before the edge belongs to the cell before");
        }
    }

    @Test
    void theCentreIsTheMiddleOfTheCell() {
        assertEquals(64, NexusZone.centre(0));
        assertEquals(-64, NexusZone.centre(-1));
        assertEquals(128 * 5 + 64, NexusZone.centre(5));
        for (int c = -9; c <= 9; c++) for (int d = -9; d <= 9; d++) assertSame(NexusZone.of(c, d), NexusZone.at(NexusZone.centre(c), NexusZone.centre(d)));
    }

    @Test
    void theCellIsAsWideAsTheDungeonTiersCoreCell() {
        assertEquals(DungeonTier.CITY_CELL, NexusZone.CELL, "the same 128-block grid");
    }
}
