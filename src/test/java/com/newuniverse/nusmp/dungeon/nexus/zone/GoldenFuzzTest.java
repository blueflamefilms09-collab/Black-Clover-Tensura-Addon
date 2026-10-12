package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Golden City over cells picked at random: whatever the cell, the garden has its gates, its water, its trees and its ladders, and it stands sealed and can be walked. */
class GoldenFuzzTest {
    @Test
    void anyCellGivesAFullGarden() {
        Random rnd = new Random(42);
        for (int i = 0; i < 120; i++) {
            int cx = rnd.nextInt(2000) - 1000, cz = rnd.nextInt(2000) - 1000;
            String at = " in cell " + cx + "," + cz;
            GoldenGrid g = GoldenGrid.of(cx, cz);
            ZonePlan p = GoldenCityPlan.of(cx, cz);
            assertTrue(p.insideCell(), "inside the cell" + at);
            assertFalse(p.opsStrayFromFootprint(), "no stray op" + at);
            assertTrue(p.ops.size() < 60_000, "ops " + p.ops.size() + at);
            assertTrue(g.gates.size() >= 3, "gates " + g.gates.size() + at);
            assertTrue(g.falls.size() >= 1, "a waterfall" + at);
            assertTrue(g.sites.size() >= 25, "trees and mushrooms " + g.sites.size() + at);
            assertTrue(g.ivy.stream().filter(x -> x[3] == 1).count() >= 4, "ivy ladders" + at);
            assertTrue(g.stand.size() >= 80, "places for creatures " + g.stand.size() + at);
            assertTrue(g.groves.size() >= 3, "groves " + g.groves.size() + at);
            assertNotNull(p.centralRelic);
            assertEquals(4, p.minorRelics.size());
        }
    }

    @Test
    void anyBuiltGardenIsSealedAndCanBeWalkedFromGateToRoofToTreasury() {
        Random rnd = new Random(7);
        for (int i = 0; i < 8; i++) {
            int cx = rnd.nextInt(2000) - 1000, cz = rnd.nextInt(2000) - 1000;
            String at = " in cell " + cx + "," + cz;
            GoldenRaster r = GoldenRaster.built(cx, cz);
            GoldenGrid g = GoldenGrid.of(cx, cz);
            ZonePlan p = GoldenCityPlan.of(cx, cz);
            boolean[] flooded = new boolean[r.cells.length];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (int y = r.y0; y < r.y0 + r.sy; y++) {
                for (int z = r.z0; z < r.z0 + r.sz; z++) {
                    for (int x = r.x0; x < r.x0 + r.sx; x++) {
                        boolean edge = x == r.x0 || x == r.x0 + r.sx - 1 || z == r.z0 || z == r.z0 + r.sz - 1 || y == r.y0 || y == r.y0 + r.sy - 1;
                        if (edge && r.open(x, y, z)) {
                            flooded[r.index(x, y, z)] = true;
                            queue.add(new int[]{x, y, z});
                        }
                    }
                }
            }
            int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                for (int[] k : d) {
                    int x = c[0] + k[0], y = c[1] + k[1], z = c[2] + k[2];
                    if (!r.in(x, y, z) || flooded[r.index(x, y, z)] || !r.open(x, y, z)) continue;
                    flooded[r.index(x, y, z)] = true;
                    queue.add(new int[]{x, y, z});
                }
            }
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.g(u, v)) continue;
                    for (int y = g.floorY + 1; y <= g.floorY + g.ceil(u, v); y++) assertFalse(flooded[r.index(g.cx + u, y, g.cz + v)], "a leak at " + u + "," + y + "," + v + at);
                }
            }
            ZonePlan.Spot from = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            boolean[] seen = r.reach(from.x(), from.y(), from.z());
            for (ZonePlan.Spot e : p.entrances) assertTrue(r.reached(seen, e.x(), e.y(), e.z()), "a gate cannot be reached" + at);
            int path = 0, pathReached = 0, stand = 0, roofReached = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (g.coverAt(u, v) == GoldenGrid.PATH) {
                        path++;
                        if (r.reached(seen, g.cx + u, g.floorY + 1, g.cz + v)) pathReached++;
                    }
                    if (!g.isRoof(u, v) || g.coverAt(u, v) == GoldenGrid.FIXED || g.coverAt(u, v) == GoldenGrid.POOL) continue;
                    int y = g.floorY + g.groundAt(u, v) + 1;
                    if (!r.standable(g.cx + u, y, g.cz + v)) continue;
                    stand++;
                    if (r.reached(seen, g.cx + u, y, g.cz + v)) roofReached++;
                }
            }
            assertEquals(path, pathReached, "every path cell can be walked to" + at);
            assertTrue(roofReached >= stand * 0.95, "the roof garden can be reached by the ivy: " + roofReached + " of " + stand + at);
            ZonePlan.Spot head = GoldenTreasury.stairHead(g.city);
            boolean[] down = r.reach(head.x(), head.y(), head.z());
            assertTrue(r.reached(down, p.centralRelic.x(), p.centralRelic.y(), p.centralRelic.z()), "the Treasury can be reached from its stair" + at);
        }
    }
}
