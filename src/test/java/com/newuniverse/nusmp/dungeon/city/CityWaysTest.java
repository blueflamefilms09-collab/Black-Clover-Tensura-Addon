package com.newuniverse.nusmp.dungeon.city;

import com.newuniverse.nusmp.dungeon.city.CityPlan.Kind;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Op;
import com.newuniverse.nusmp.dungeon.city.CityPlan.Pal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The ways into a Golden City: found where a cave comes close, never a leak for the flooded caves, none where there is only rock. */
class CityWaysTest {
    private static final int Y0 = -64, H = 40, MARGIN = 24;
    private static final byte NATURAL = 0, BLOCK = 1, AIR = 2;

    private interface Cave { boolean at(int x, int y, int z); }

    /** What the plan makes of each cell of its map cell: untouched, a block, or open (carved, air, water). */
    private static byte[][][] grid(CityPlan p) {
        int x0 = p.cellX * CityPlan.CELL, z0 = p.cellZ * CityPlan.CELL;
        byte[][][] g = new byte[CityPlan.CELL][H][CityPlan.CELL];
        for (Op o : p.ops) {
            byte v = o.kind() == Kind.CARVE || o.pal() == Pal.AIR || o.pal() == Pal.WATER ? AIR : BLOCK;
            for (int y = o.y1(); y <= o.y2(); y++)
                for (int x = o.x1(); x <= o.x2(); x++)
                    for (int z = o.z1(); z <= o.z2(); z++) g[x - x0][y - Y0][z - z0] = v;
        }
        return g;
    }

    private static byte plan(CityPlan p, byte[][][] g, int x, int y, int z) {
        int gx = x - p.cellX * CityPlan.CELL, gy = y - Y0, gz = z - p.cellZ * CityPlan.CELL;
        if (gx < 0 || gx >= CityPlan.CELL || gy < 0 || gy >= H || gz < 0 || gz >= CityPlan.CELL) return NATURAL;
        return g[gx][gy][gz];
    }

    private static CityWays.Probe world(CityPlan p, byte[][][] g, Cave cave) {
        return (x, y, z) -> {
            byte b = plan(p, g, x, y, z);
            if (b != NATURAL) return b == AIR ? CityWays.OPEN : CityWays.SOLID;
            return cave.at(x, y, z) ? CityWays.OPEN : CityWays.SOLID;
        };
    }

    /** A cavern beyond the east end of the district, 6 blocks off its tip. */
    private static Cave eastCavern(CityPlan p) {
        return (x, y, z) -> x >= p.centreX + p.semiX + 6 && Math.abs(z - p.centreZ) <= 45 && y >= -58 && y <= -34;
    }

    /** A made-up field of caves: about two cells in five are open, in lumps of 5 blocks. */
    private static Cave field(int seed) {
        return (x, y, z) -> CityRng.hash(seed, Math.floorDiv(x, 5), Math.floorDiv(z, 5), Math.floorDiv(y, 5) + 100) % 100 < 40;
    }

    /**
     * Counts the cells where the flooded caves could flow into the city: a cell open inside (the vault, or a passage up to its door) next
     * to a cell open outside (a cave, or rock cut for a way). A door cell holds no water, so it separates the two.
     */
    private static int leaks(CityPlan p, byte[][][] g, Cave cave, List<CityWays.Way> ways) {
        int n = CityPlan.CELL + 2 * MARGIN;
        byte[][][] c = new byte[n][H][n];                 // 0 rock, 1 open inside, 2 open outside, 3 door, 4 plan block
        int x0 = p.cellX * CityPlan.CELL - MARGIN, z0 = p.cellZ * CityPlan.CELL - MARGIN;
        for (int x = 0; x < n; x++)
            for (int y = 0; y < H; y++)
                for (int z = 0; z < n; z++) {
                    byte b = plan(p, g, x + x0, y + Y0, z + z0);
                    if (b == AIR) c[x][y][z] = 1;
                    else if (b == BLOCK) c[x][y][z] = 4;
                    else if (cave.at(x + x0, y + Y0, z + z0)) c[x][y][z] = 2;
                }
        for (CityWays.Way w : ways) {
            int doorAt = Math.abs(w.doorX() - w.fromX()) + Math.abs(w.doorZ() - w.fromZ());
            for (int i = 0; i < w.length(); i++) {
                int x = w.fromX() + w.dx() * i - x0, z = w.fromZ() + w.dz() * i - z0;
                for (int y = p.floorY + 1; y <= p.floorY + 2; y++) {
                    if (i < doorAt) c[x][y - Y0][z] = 1;
                    else if (i == doorAt) c[x][y - Y0][z] = 3;
                    else c[x][y - Y0][z] = 2;
                }
            }
        }
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        int leaks = 0;
        for (int x = 0; x < n; x++)
            for (int y = 0; y < H; y++)
                for (int z = 0; z < n; z++) {
                    if (c[x][y][z] != 1) continue;
                    for (int[] d : dirs) {
                        int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                        if (nx >= 0 && nx < n && ny >= 0 && ny < H && nz >= 0 && nz < n && c[nx][ny][nz] == 2) leaks++;
                    }
                }
        return leaks;
    }

    @Test
    void aCavernBesideTheCityGivesWaysIntoItAndNoLeak() {
        for (int[] cell : new int[][]{{0, 0}, {3, -2}, {-5, 4}, {7, 7}}) {
            CityPlan p = CityPlan.of(cell[0], cell[1]);
            byte[][][] g = grid(p);
            Cave cave = eastCavern(p);
            List<CityWays.Way> ways = CityWays.choose(p, world(p, g, cave));
            assertFalse(ways.isEmpty(), "a cavern 6 blocks off the east end has ways in");
            assertTrue(ways.size() <= CityWays.MAX);
            for (CityWays.Way w : ways) {
                assertEquals(1, w.dx(), "the cavern is to the east");
                assertEquals(0, w.dz());
                assertTrue(p.contains(w.doorX(), w.doorZ()), "the door is the outermost cell of the district");
                assertFalse(p.contains(w.doorX() + 1, w.doorZ()));
                assertTrue(w.dug() >= 0 && w.dug() <= CityWays.REACH);
                assertEquals(w.doorX() - w.fromX() + w.dug() + 1, w.length());
                assertTrue(Math.abs(w.doorZ() - p.centreZ) > 6, "never at an avenue");
            }
            for (int i = 0; i < ways.size(); i++)
                for (int j = i + 1; j < ways.size(); j++)
                    assertTrue(Math.abs(ways.get(i).doorX() - ways.get(j).doorX()) + Math.abs(ways.get(i).doorZ() - ways.get(j).doorZ()) >= CityWays.APART, "ways keep apart");
            assertEquals(0, leaks(p, g, cave, ways), "the flooded cavern cannot reach the city");
        }
    }

    @Test
    void onlyRockOrUnloadedCavesGiveNoWay() {
        CityPlan p = CityPlan.of(1, 2);
        byte[][][] g = grid(p);
        assertTrue(CityWays.choose(p, world(p, g, (x, y, z) -> false)).isEmpty(), "rock all round");
        CityWays.Probe east = world(p, g, eastCavern(p));
        CityWays.Probe unloaded = (x, y, z) -> plan(p, g, x, y, z) == NATURAL ? CityWays.UNKNOWN : east.at(x, y, z);
        assertTrue(CityWays.choose(p, unloaded).isEmpty(), "chunks that are not loaded are never dug");
        CityWays.Probe walledIn = (x, y, z) -> p.contains(x, z) ? CityWays.SOLID : east.at(x, y, z);
        assertTrue(CityWays.choose(p, walledIn).isEmpty(), "a way must open into the city, not into a building");
    }

    @Test
    void aSmallPocketIsNotACave() {
        CityPlan p = CityPlan.of(2, 2);
        byte[][][] g = grid(p);
        Cave pocket = (x, y, z) -> x == p.centreX + p.semiX + 3 && Math.abs(z - p.centreZ) == 12 && y >= p.floorY + 1 && y <= p.floorY + 2;
        assertTrue(CityWays.choose(p, world(p, g, pocket)).isEmpty());
    }

    @Test
    void randomCavesNeverLeakAndTheChoiceIsDeterministic() {
        int found = 0;
        for (int i = 0; i < 16; i++) {
            CityPlan p = CityPlan.of(i * 5 - 20, 13 - i * 3);
            byte[][][] g = grid(p);
            Cave cave = field(i * 7 + 1);
            List<CityWays.Way> ways = CityWays.choose(p, world(p, g, cave));
            assertEquals(ways, CityWays.choose(p, world(p, g, cave)));
            assertTrue(ways.size() <= CityWays.MAX);
            assertEquals(0, leaks(p, g, cave, ways), "cell " + p.cellX + "," + p.cellZ);
            found += ways.size();
        }
        assertTrue(found > 0, "caves this common give some ways");
    }
}
