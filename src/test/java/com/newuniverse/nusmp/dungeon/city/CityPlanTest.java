package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.city.CityPlan.Building;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Kind;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Op;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Pal;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Golden City's layout (docs/living_dungeon.md section 7): deterministic, inside its cell, sealed, in order. */
class CityPlanTest {
    private static int[][] cells() {
        int[][] out = new int[60][];
        for (int i = 0; i < out.length; i++) out[i] = new int[]{(i * 7) % 23 - 11, (i * 13) % 19 - 9};
        out[0] = new int[]{0, 0};
        out[1] = new int[]{-1, -1};
        return out;
    }

    @Test
    void theSameCellGivesTheSameCity() {
        CityPlan a = CityPlan.of(3, -2), b = CityPlan.of(3, -2), c = CityPlan.of(4, -2);
        assertEquals(a.ops, b.ops);
        assertEquals(a.buildings, b.buildings);
        assertEquals(a.centreX, b.centreX);
        assertNotEquals(a.ops, c.ops, "another cell, another city");
        assertEquals(CityPlan.centreX(3, -2), a.centreX);
        assertEquals(CityPlan.centreZ(3, -2), a.centreZ);
    }

    @Test
    void theDistrictIsAnOvalInsideItsCell() {
        for (int[] cell : cells()) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            assertTrue(p.semiX >= 36 && p.semiX <= 48 && p.semiZ >= 36 && p.semiZ <= 48, "72 to 96 across");
            assertTrue(p.floorY >= -54 && p.floorY <= -52, "the floor is deep enough for the tier and clear of the bedrock");
            assertTrue(p.minX() >= cell[0] * CityPlan.CELL && p.maxX() < (cell[0] + 1) * CityPlan.CELL);
            assertTrue(p.minZ() >= cell[1] * CityPlan.CELL && p.maxZ() < (cell[1] + 1) * CityPlan.CELL);
            assertTrue(p.gateAxis == 0 || p.gateAxis == 1);
            assertTrue(p.contains(p.centreX, p.centreZ));
            assertFalse(p.contains(p.centreX + p.semiX + 1, p.centreZ));
        }
    }

    @Test
    void everyOpIsInsideTheCellAndTheDistrictAndTheHeightBand() {
        for (int[] cell : cells()) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            assertTrue(p.ops.size() >= 1500 && p.ops.size() <= 8000, "op count " + p.ops.size());
            for (Op o : p.ops) {
                assertTrue(o.x1() <= o.x2() && o.y1() <= o.y2() && o.z1() <= o.z2(), "corners are ordered: " + o);
                assertTrue(o.x1() >= cell[0] * CityPlan.CELL && o.x2() < (cell[0] + 1) * CityPlan.CELL, "x in the cell: " + o);
                assertTrue(o.z1() >= cell[1] * CityPlan.CELL && o.z2() < (cell[1] + 1) * CityPlan.CELL, "z in the cell: " + o);
                assertTrue(o.y1() >= -60 && o.y2() <= -30, "y in the band: " + o);
                assertTrue(o.kind() != Kind.BLOCK || o.volume() == 1, "a block is one block: " + o);
                for (int x : new int[]{o.x1(), o.x2()}) for (int z : new int[]{o.z1(), o.z2()}) assertTrue(p.contains(x, z), "inside the oval: " + o);
            }
        }
    }

    @Test
    void opsComeInOrderCarveFloorBuildingsDetails() {
        for (int[] cell : cells()) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            assertEquals(0, p.phaseStart(CityPlan.PHASE_CARVE));
            assertTrue(p.phaseStart(CityPlan.PHASE_FLOOR) > 0 && p.phaseStart(CityPlan.PHASE_BUILD) > p.phaseStart(CityPlan.PHASE_FLOOR)
                    && p.phaseStart(CityPlan.PHASE_DETAIL) > p.phaseStart(CityPlan.PHASE_BUILD) && p.phaseStart(4) > p.phaseStart(CityPlan.PHASE_DETAIL));
            assertEquals(p.ops.size(), p.phaseStart(4));
            for (int i = 0; i < p.ops.size(); i++) {
                boolean carve = p.ops.get(i).kind() == Kind.CARVE;
                assertEquals(i < p.phaseStart(CityPlan.PHASE_FLOOR), carve, "carving comes first and only first");
                assertEquals(p.phaseOf(i) == CityPlan.PHASE_CARVE, carve);
            }
            assertEquals(CityPlan.PHASE_DETAIL, p.phaseOf(p.ops.size() - 1));
        }
    }

    @Test
    void oneAltarOnTheDaisAndTwoToFourSentinels() {
        for (int[] cell : cells()) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            int altars = 0;
            for (Op o : p.ops) if (o.pal() == Pal.ALTAR) {
                altars++;
                assertEquals(1, o.volume());
                assertEquals(p.altarX, o.x1());
                assertEquals(p.altarY, o.y1());
                assertEquals(p.altarZ, o.z1());
            }
            assertEquals(1, altars, "exactly one altar");
            assertEquals(p.centreX, p.altarX);
            assertEquals(p.floorY + 3, p.altarY);
            assertTrue(p.sentinels.size() >= 2 && p.sentinels.size() <= 4);
        }
    }

    @Test
    void eightToFourteenBuildingsSomeRuinedSomeWhole() {
        Set<String> types = new HashSet<>();
        for (int[] cell : cells()) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            assertTrue(p.buildings.size() >= 8 && p.buildings.size() <= 14, "buildings " + p.buildings.size());
            int ruined = 0;
            for (Building b : p.buildings) {
                types.add(b.type());
                if (b.ruined()) ruined++;
                assertTrue(p.contains(b.x(), b.z()));
            }
            assertTrue(ruined >= 2 && p.buildings.size() - ruined >= 2, "some ruined, some whole");
        }
        assertTrue(types.contains("tower") && types.contains("hall") && types.contains("shrine") && types.contains("arch"), "every kind turns up: " + types);
    }

    @Test
    void twoGatesAtOppositeEndsOfTheGateAxis() {
        for (int[] cell : cells()) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            int axisHalf = p.gateAxis == 0 ? p.semiX : p.semiZ;
            int sigils = 0;
            for (Op o : p.ops) {
                if (o.pal() != Pal.SEA_LANTERN || o.kind() != Kind.BLOCK || o.y1() != p.floorY + 4) continue;
                int along = p.gateAxis == 0 ? o.x1() - p.centreX : o.z1() - p.centreZ, across = p.gateAxis == 0 ? o.z1() - p.centreZ : o.x1() - p.centreX;
                if (across == 0 && Math.abs(along) == axisHalf - 3) sigils++;
            }
            assertEquals(2, sigils, "a sigil on each sealed gate");
        }
    }

    @Test
    void theVaultIsSealedItsWaterStaysInTheMoatAndEveryoneHasSomewhereToStand() {
        for (int i = 0; i < 12; i++) {
            int[] cell = cells()[i * 5];
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            int x0 = cell[0] * CityPlan.CELL, z0 = cell[1] * CityPlan.CELL;
            int[][][] g = new int[128][31][128];
            for (Op o : p.ops)
                for (int y = o.y1(); y <= o.y2(); y++)
                    for (int x = o.x1(); x <= o.x2(); x++)
                        for (int z = o.z1(); z <= o.z2(); z++) g[x - x0][y + 60][z - z0] = 1 + o.pal().ordinal();
            int air = 1 + Pal.AIR.ordinal(), water = 1 + Pal.WATER.ordinal();
            int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
            int leaks = 0, waterLeaks = 0, airCells = 0;
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 31; y++) {
                    for (int z = 0; z < 128; z++) {
                        int here = g[x][y][z];
                        if (here != air && here != water) continue;
                        if (here == air) airCells++;
                        for (int[] d : dirs) {
                            if (here == water && d[1] > 0) continue;                       // water does not climb
                            int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                            int there = nx < 0 || nx > 127 || ny < 0 || ny > 30 || nz < 0 || nz > 127 ? 0 : g[nx][ny][nz];
                            if (there == 0) { if (here == air) leaks++; else waterLeaks++; }       // untouched stone or flooded cave next to it
                            else if (here == water && there == air) waterLeaks++;
                        }
                    }
                }
            }
            assertTrue(airCells > 40000, "a real cavern was carved: " + airCells);
            assertEquals(0, leaks, "no air cell touches the natural cave");
            assertEquals(0, waterLeaks, "the moat is closed");
            for (int[] s : p.sentinels) {
                assertEquals(air, g[s[0] - x0][s[1] + 60][s[2] - z0], "feet");
                assertEquals(air, g[s[0] - x0][s[1] + 61][s[2] - z0], "head");
                int below = g[s[0] - x0][s[1] + 59][s[2] - z0];
                assertTrue(below != 0 && below != air && below != water, "something to stand on");
            }
            assertEquals(1 + Pal.ALTAR.ordinal(), g[p.altarX - x0][p.altarY + 60][p.altarZ - z0]);
            assertTrue(g[p.altarX - x0][p.altarY + 59][p.altarZ - z0] != air, "the altar rests on the dais");
        }
    }

    @Test
    void theNearestCellSearchFindsTheClosestAndHonoursTheTest() {
        CityPlan.CellTest every = (cx, cz) -> true, none = (cx, cz) -> false, one = (cx, cz) -> cx == 2 && cz == -1;
        assertNull(CityPlan.nearest(0, 0, 5, none));
        int[] self = CityPlan.nearest(CityPlan.centreX(1, 1) + 2, CityPlan.centreZ(1, 1) - 3, 3, every);
        assertEquals(1, self[0]);
        assertEquals(1, self[1]);
        int[] far = CityPlan.nearest(0, 0, 5, one);
        assertEquals(2, far[0]);
        assertEquals(-1, far[1]);
    }

    @Test
    void ellipseHelpersAgreeWithEachOther() {
        for (int a = 5; a <= 20; a += 5) {
            for (int b = 4; b <= 20; b += 8) {
                for (int v = -b - 2; v <= b + 2; v++) {
                    int ext = CityPlan.extent(v, a, b);
                    if (ext < 0) {
                        assertFalse(CityPlan.inside(0, v, a, b));
                        continue;
                    }
                    assertTrue(CityPlan.inside(ext, v, a, b));
                    assertFalse(CityPlan.inside(ext + 1, v, a, b));
                    assertTrue(CityPlan.inside(-ext, v, a, b));
                }
            }
        }
    }
}
