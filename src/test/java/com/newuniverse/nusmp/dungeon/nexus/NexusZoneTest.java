package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The zones of the Dungeon's Nexuses: the pick of the four core zones per cell is an even, stable hash that server and clients agree on,
 * and since 1.22 the cells that are floor boss arenas take their arena zone instead, without moving any core cell's zone.
 */
class NexusZoneTest {
    private static Map<NexusZone, Integer> count() {
        Map<NexusZone, Integer> n = new EnumMap<>(NexusZone.class);
        for (NexusZone z : NexusZone.values()) n.put(z, 0);
        return n;
    }

    /** The zone of 34 core cells as the 1.21.0 formula (values()[hash % 4], with the four original zones) gave it: {cellX, cellZ, zone}. */
    private static final Object[][] CORE_1_21_0 = {
            {1, -14, "TEMPEST_MIRAGE"}, {-7, -12, "MONARCH_VAULT"}, {13, -10, "UNDERWORLD_GATE"}, {-12, -9, "MONARCH_VAULT"}, {8, -9, "UNDERWORLD_GATE"},
            {-13, -8, "MONARCH_VAULT"}, {-11, -8, "UNDERWORLD_GATE"}, {3, -8, "TEMPEST_MIRAGE"}, {-10, -7, "MONARCH_VAULT"}, {6, -7, "GOLDEN_CITY"},
            {1, -6, "GOLDEN_CITY"}, {-4, -5, "UNDERWORLD_GATE"}, {0, -5, "MONARCH_VAULT"}, {8, -5, "TEMPEST_MIRAGE"}, {12, -5, "UNDERWORLD_GATE"},
            {-6, -3, "GOLDEN_CITY"}, {5, -2, "MONARCH_VAULT"}, {-4, -1, "TEMPEST_MIRAGE"}, {12, -1, "TEMPEST_MIRAGE"}, {-13, 0, "GOLDEN_CITY"},
            {-10, 1, "TEMPEST_MIRAGE"}, {-2, 1, "MONARCH_VAULT"}, {9, 4, "UNDERWORLD_GATE"}, {-13, 6, "MONARCH_VAULT"}, {3, 6, "TEMPEST_MIRAGE"},
            {5, 6, "UNDERWORLD_GATE"}, {-3, 8, "UNDERWORLD_GATE"}, {-2, 9, "TEMPEST_MIRAGE"}, {-7, 12, "GOLDEN_CITY"}, {1, 12, "GOLDEN_CITY"},
            {9, 12, "UNDERWORLD_GATE"}, {-13, 14, "GOLDEN_CITY"}, {3, 14, "TEMPEST_MIRAGE"}, {5, 14, "UNDERWORLD_GATE"}};

    /** The same for 24 cells that are neither core cells nor arenas (the hash is also asked for plain columns, e.g. by the collapse of a pocket). */
    private static final Object[][] PLAIN_1_21_0 = {
            {-5, -9, "TEMPEST_MIRAGE"}, {2, -8, "TEMPEST_MIRAGE"}, {-7, -7, "MONARCH_VAULT"}, {1, -7, "TEMPEST_MIRAGE"}, {-8, -6, "UNDERWORLD_GATE"},
            {8, -6, "MONARCH_VAULT"}, {-1, -5, "TEMPEST_MIRAGE"}, {7, -5, "GOLDEN_CITY"}, {-2, -4, "MONARCH_VAULT"}, {6, -4, "MONARCH_VAULT"},
            {5, -3, "GOLDEN_CITY"}, {-5, -1, "UNDERWORLD_GATE"}, {3, -1, "UNDERWORLD_GATE"}, {2, 0, "UNDERWORLD_GATE"}, {-7, 1, "UNDERWORLD_GATE"},
            {9, 1, "MONARCH_VAULT"}, {0, 2, "MONARCH_VAULT"}, {7, 3, "MONARCH_VAULT"}, {-2, 4, "GOLDEN_CITY"}, {6, 4, "MONARCH_VAULT"},
            {5, 5, "GOLDEN_CITY"}, {-6, 8, "GOLDEN_CITY"}, {2, 8, "TEMPEST_MIRAGE"}, {-7, 9, "GOLDEN_CITY"}};

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
    void everyCoreCellKeepsTheZoneItHadInVersion1_21_0() {
        assertEquals(34, CORE_1_21_0.length);
        for (Object[] c : CORE_1_21_0) {
            int cx = (Integer) c[0], cz = (Integer) c[1];
            assertTrue(DungeonTier.isCityCell(cx, cz), "a core cell: " + cx + "," + cz);
            assertSame(NexusZone.valueOf((String) c[2]), NexusZone.of(cx, cz), "core cell " + cx + "," + cz);
        }
        java.util.Set<NexusZone> seen = java.util.EnumSet.noneOf(NexusZone.class);
        for (Object[] c : CORE_1_21_0) seen.add(NexusZone.valueOf((String) c[2]));
        assertEquals(java.util.EnumSet.copyOf(java.util.Arrays.asList(NexusZone.coreZones())), seen, "the table holds all four zones");
    }

    @Test
    void theColumnsOfPlainCellsKeepTheirOldHashToo() {
        assertEquals(24, PLAIN_1_21_0.length);
        for (Object[] c : PLAIN_1_21_0) {
            int cx = (Integer) c[0], cz = (Integer) c[1];
            assertFalse(DungeonTier.isCityCell(cx, cz) || DungeonTier.arenaOf(cx, cz) != DungeonTier.ARENA_NONE, "neither a core cell nor an arena: " + cx + "," + cz);
            assertSame(NexusZone.valueOf((String) c[2]), NexusZone.of(cx, cz), "plain cell " + cx + "," + cz);
        }
    }

    @Test
    void theOriginalFourZonesKeepTheirOrdinals() {
        assertEquals(0, NexusZone.MONARCH_VAULT.ordinal());
        assertEquals(1, NexusZone.UNDERWORLD_GATE.ordinal());
        assertEquals(2, NexusZone.TEMPEST_MIRAGE.ordinal());
        assertEquals(3, NexusZone.GOLDEN_CITY.ordinal());
        assertEquals(4, NexusZone.GOLIATH_PIT.ordinal(), "the arenas were appended");
        assertEquals(5, NexusZone.CHARYBDIS_BASIN.ordinal());
        assertEquals(6, NexusZone.values().length);
        assertArrayEquals(new NexusZone[]{NexusZone.MONARCH_VAULT, NexusZone.UNDERWORLD_GATE, NexusZone.TEMPEST_MIRAGE, NexusZone.GOLDEN_CITY}, NexusZone.coreZones());
        assertEquals("The Goliath's Pit", NexusZone.GOLIATH_PIT.label);
        assertEquals("The Charybdis Basin", NexusZone.CHARYBDIS_BASIN.label);
    }

    @Test
    void anArenaIsNotACoreZone() {
        for (NexusZone z : NexusZone.coreZones()) {
            assertTrue(z.isCore(), z.name());
            assertFalse(z.isArena(), z.name());
        }
        for (NexusZone z : new NexusZone[]{NexusZone.GOLIATH_PIT, NexusZone.CHARYBDIS_BASIN}) {
            assertTrue(z.isArena(), z.name());
            assertFalse(z.isCore(), z.name());
        }
        NexusZone[] copy = NexusZone.coreZones();
        copy[0] = NexusZone.GOLIATH_PIT;
        assertSame(NexusZone.MONARCH_VAULT, NexusZone.coreZones()[0], "the list handed out is a copy");
    }

    @Test
    void anArenaCellTakesItsArenaZoneAndNoOtherCellDoes() {
        int pits = 0, basins = 0;
        for (int cx = -60; cx <= 60; cx++) {
            for (int cz = -60; cz <= 60; cz++) {
                NexusZone z = NexusZone.of(cx, cz);
                int arena = DungeonTier.arenaOf(cx, cz);
                assertEquals(arena == DungeonTier.ARENA_GOLIATH, z == NexusZone.GOLIATH_PIT, cx + "," + cz);
                assertEquals(arena == DungeonTier.ARENA_CHARYBDIS, z == NexusZone.CHARYBDIS_BASIN, cx + "," + cz);
                if (DungeonTier.isCityCell(cx, cz)) assertTrue(z.isCore(), "a core cell is never an arena: " + cx + "," + cz);
                if (z == NexusZone.GOLIATH_PIT) pits++;
                if (z == NexusZone.CHARYBDIS_BASIN) basins++;
            }
        }
        assertTrue(pits > 500 && basins > 500, pits + " pits and " + basins + " basins in 14641 cells");
    }

    @Test
    void theCoreZonesAreEvenOverThePlainCells() {
        Map<NexusZone, Integer> n = count();
        int total = 0;
        for (int cx = -150; cx < 150; cx++) {
            for (int cz = -150; cz < 150; cz++) {
                if (DungeonTier.arenaOf(cx, cz) != DungeonTier.ARENA_NONE) continue;
                n.merge(NexusZone.of(cx, cz), 1, Integer::sum);
                total++;
            }
        }
        for (NexusZone z : NexusZone.coreZones()) {
            double share = n.get(z) / (double) total;
            assertEquals(0.25, share, 0.02, z + " holds about a quarter of all cells that are not arenas: " + share);
        }
        assertEquals(0, n.get(NexusZone.GOLIATH_PIT));
        assertEquals(0, n.get(NexusZone.CHARYBDIS_BASIN));
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
        for (NexusZone z : NexusZone.coreZones()) {
            double share = n.get(z) / (double) core;
            assertEquals(0.25, share, 0.02, z + " holds about a quarter of the core cells: " + share);
        }
        assertEquals(0, n.get(NexusZone.GOLIATH_PIT) + n.get(NexusZone.CHARYBDIS_BASIN), "no core cell is an arena");
    }

    @Test
    void neighbouringCellsDoNotShareAZone() {
        int same = 0, pairs = 0;
        for (int cx = -80; cx < 80; cx++) {
            for (int cz = -80; cz < 80; cz++) {
                if (DungeonTier.arenaOf(cx, cz) != 0) continue;
                if (DungeonTier.arenaOf(cx + 1, cz) == 0) {
                    pairs++;
                    if (NexusZone.of(cx, cz) == NexusZone.of(cx + 1, cz)) same++;
                }
                if (DungeonTier.arenaOf(cx, cz + 1) == 0) {
                    pairs++;
                    if (NexusZone.of(cx, cz) == NexusZone.of(cx, cz + 1)) same++;
                }
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
