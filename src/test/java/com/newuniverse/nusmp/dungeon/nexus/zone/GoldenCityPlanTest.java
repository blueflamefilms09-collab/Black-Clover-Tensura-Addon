package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Golden City as planned: its size, its sealing, its Treasury and gate, its ways in, its pools, and that a person can walk it. */
class GoldenCityPlanTest {
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {-3, 5}, {7, -4}, {-12, -9}, {20, 31}, {-1, -1}, {3, 2}};
    /** The heavier tests, which build a block grid of the whole garden and the city, look at these only. */
    private static final int[][] GRID_CELLS = {{0, 0}, {-3, 5}, {20, 31}, {-1, -1}};

    // ---------------------------------------------------------------- the plan itself
    @Test
    void everyCellGivesAnInOrderAndDeterministicPlan() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            assertSame(NexusZone.GOLDEN_CITY, p.zone);
            assertTrue(p.insideCell(), "inside its core cell: " + c[0] + "," + c[1]);
            assertFalse(p.opsStrayFromFootprint(), "no op writes outside the footprint's chunks");
            assertEquals(4, p.phaseCount(), "carve; foundation, shell and ground; structures; details");
            assertTrue(p.topY > p.floorY);
            assertEquals(NexusZone.centre(c[0]), p.centreX());
            assertEquals(NexusZone.centre(c[1]), p.centreZ());
            assertEquals(Footprint.Shape.RECT, p.footprint.shape());
            assertEquals(GoldenGrid.HALF, p.footprint.halfX());
            assertEquals(CityPlan.of(c[0], c[1]).floorY, p.floorY, "the garden stands on the city's floor");
            assertSame(p, GoldenCityPlan.of(c[0], c[1]), "cached");
            ZonePlan again = GoldenCityPlan.fresh(c[0], c[1]);
            assertEquals(p.ops, again.ops, "the same ops every time");
            assertEquals(p.allSpawns(), again.allSpawns());
            assertEquals(p.entrances, again.entrances);
        }
    }

    @Test
    void theCellsDiffer() {
        Set<String> seen = new HashSet<>();
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            seen.add(g.cityU + "," + g.cityV + "," + g.city.semiX + "," + g.floorY + "," + g.pools.size() + "," + g.sites.size());
        }
        assertTrue(seen.size() >= 6, "the gardens are not all alike: " + seen);
    }

    @Test
    void theOpCountIsBoundedAndEveryOpStaysInside() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            assertTrue(p.ops.size() < 60_000, "ops: " + p.ops.size());
            for (NexusOp o : p.ops) {
                assertTrue(o.x1() >= p.centreX() - GoldenGrid.HALF && o.x2() <= p.centreX() + GoldenGrid.HALF, "x of " + o);
                assertTrue(o.z1() >= p.centreZ() - GoldenGrid.HALF && o.z2() <= p.centreZ() + GoldenGrid.HALF, "z of " + o);
                assertTrue(o.y1() >= p.floorY - GoldenGrid.FOUNDATION && o.y2() <= p.topY, "y of " + o);
            }
        }
    }

    @Test
    void theNexusSpotsAreAllThere() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            CityPlan city = GoldenGrid.of(c[0], c[1]).city;
            assertNotNull(p.centralRelic, "exactly one central pedestal");
            assertEquals(new ZonePlan.Spot(city.centreX, city.floorY - 4, city.centreZ), p.centralRelic, "directly under the altar");
            assertEquals(city.altarX, p.centralRelic.x());
            assertEquals(city.altarZ, p.centralRelic.z());
            assertEquals(4, p.minorRelics.size());
            Set<ZonePlan.Spot> all = new HashSet<>(p.minorRelics);
            all.add(p.centralRelic);
            assertEquals(5, all.size(), "five different pedestals for the five relics");
            assertEquals(1, p.gates.size(), "one mana-locked gate to the Treasury");
            assertTrue(p.entrances.size() >= 3, "ways in from the caves: " + p.entrances.size());
            assertEquals(p.entrances.size(), p.spawns(ZonePlan.ROLE_ARRIVAL).size(), "somewhere to set a rescued player down at each");
            assertTrue(p.spawns(ZonePlan.ROLE_GUARD).size() >= 2 && p.spawns(ZonePlan.ROLE_GUARD).size() <= 4, "the old city's guards");
            assertTrue(p.spawns("prey").size() >= 40, "places for the herds: " + p.spawns("prey").size());
            assertTrue(p.spawns("apex").size() >= 40, "places for the predators: " + p.spawns("apex").size());
            for (int i = 0; i < 5; i++) assertTrue(GoldenCityPlan.centralRelicIndex(c[0], c[1], 5) >= 0 && GoldenCityPlan.centralRelicIndex(c[0], c[1], 5) < 5);
            assertEquals(0, GoldenCityPlan.centralRelicIndex(c[0], c[1], 0));
        }
        Set<Integer> which = new HashSet<>();
        for (int x = -10; x < 10; x++) for (int z = -10; z < 10; z++) which.add(GoldenCityPlan.centralRelicIndex(x, z, 5));
        assertEquals(5, which.size(), "every relic is the central one somewhere");
    }

    // ---------------------------------------------------------------- the built garden
    @Test
    void theCavernIsSealedAgainstTheFloodedCavesAllAround() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
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
                int[] p = queue.poll();
                for (int[] k : d) {
                    int x = p[0] + k[0], y = p[1] + k[1], z = p[2] + k[2];
                    if (!r.in(x, y, z) || flooded[r.index(x, y, z)] || !r.open(x, y, z)) continue;
                    flooded[r.index(x, y, z)] = true;
                    queue.add(new int[]{x, y, z});
                }
            }
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.g(u, v)) continue;
                    for (int y = g.floorY + 1; y <= g.floorY + g.ceil(u, v); y++) {
                        assertFalse(flooded[r.index(g.cx + u, y, g.cz + v)], "a leak into the garden at " + u + "," + y + "," + v + " of cell " + c[0] + "," + c[1]);
                    }
                }
            }
        }
    }

    @Test
    void theFloorHoldsFromBelowToo() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            for (int u = -GoldenGrid.REACH - GoldenGrid.WALL; u <= GoldenGrid.REACH + GoldenGrid.WALL; u++) {
                for (int v = -GoldenGrid.REACH - GoldenGrid.WALL; v <= GoldenGrid.REACH + GoldenGrid.WALL; v++) {
                    if (!g.garden(u, v)) continue;
                    for (int y = g.floorY - GoldenGrid.FOUNDATION; y <= g.floorY - 3; y++) {
                        if (g.inCity(u, v, 0) && y >= g.floorY - 5) continue;                      // the Treasury is cut here
                        assertFalse(r.open(g.cx + u, y, g.cz + v), "no hole under the garden at " + u + "," + y + "," + v);
                    }
                }
            }
        }
    }

    @Test
    void aPersonCanWalkFromEveryGateToEveryGateAndAcrossTheGarden() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            ZonePlan.Spot from = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            boolean[] seen = r.reach(from.x(), from.y(), from.z());
            for (ZonePlan.Spot e : p.entrances) assertTrue(r.reached(seen, e.x(), e.y(), e.z()), "the gate at " + e + " is reached on foot from " + from);
            for (ZonePlan.Spot e : p.spawns(ZonePlan.ROLE_ARRIVAL)) assertTrue(r.reached(seen, e.x(), e.y(), e.z()), "the arrival spot " + e);
            int path = 0, pathReached = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (g.coverAt(u, v) != GoldenGrid.PATH) continue;
                    path++;
                    if (r.reached(seen, g.cx + u, g.floorY + 1, g.cz + v)) pathReached++;
                }
            }
            assertEquals(path, pathReached, "every cell of every path can be walked to");
            for (String role : new String[]{"prey", "apex"}) {
                int ok = 0;
                List<ZonePlan.Spot> spots = p.spawns(role);
                for (ZonePlan.Spot s : spots) if (r.reached(seen, s.x(), s.y(), s.z())) ok++;
                assertTrue(ok >= spots.size() * 0.97, role + " places reached on foot: " + ok + " of " + spots.size());
            }
        }
    }

    @Test
    void thePathsAreOpenCellsTwoHighAndNothingGrowsOnThem() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int n = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (g.coverAt(u, v) != GoldenGrid.PATH) continue;
                    n++;
                    boolean ladder = false;
                    for (int[] i : g.ivy) if (i[0] == u && i[1] == v && i[3] == 1) ladder = true;
                    for (int y = g.floorY + 1; y <= g.floorY + 3; y++) {
                        String cell = r.at(g.cx + u, y, g.cz + v);
                        assertTrue(cell.equals("minecraft:air") || (ladder && cell.equals("minecraft:vine")), "a path cell is open at " + u + "," + y + "," + v + ": " + cell);
                    }
                    String ground = r.at(g.cx + u, g.floorY, g.cz + v);
                    assertTrue(ground.equals("minecraft:dirt_path") || ground.equals("minecraft:coarse_dirt") || ground.equals("minecraft:ochre_froglight"), "path ground: " + ground);
                }
            }
            assertTrue(n > 150, "a promenade and spokes: " + n);
        }
    }

    @Test
    void theWaterStaysWhereItWasPut() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            Set<String> streams = new HashSet<>();
            for (GoldenGrid.Fall w : g.falls) {
                for (int y = g.floorY + 1; y <= g.floorY + w.height(); y++) streams.add(w.u() + "," + y + "," + w.v());
            }
            int water = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.garden(u, v) || g.inCity(u, v, 0)) continue;
                    for (int y = g.floorY - 3; y <= g.floorY + 60; y++) {
                        if (!r.at(g.cx + u, y, g.cz + v).equals("minecraft:water")) continue;
                        water++;
                        boolean stream = streams.contains(u + "," + y + "," + v);
                        String below = r.at(g.cx + u, y - 1, g.cz + v);
                        assertTrue(!r.open(g.cx + u, y - 1, g.cz + v) || below.equals("minecraft:water"), "water over nothing at " + u + "," + y + "," + v);
                        if (stream) continue;
                        int[][] side = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                        for (int[] k : side) {
                            String n = r.at(g.cx + u + k[0], y, g.cz + v + k[1]);
                            assertTrue(!r.open(g.cx + u + k[0], y, g.cz + v + k[1]) || n.equals("minecraft:water"), "water would run off at " + u + "," + y + "," + v + " into " + n);
                        }
                    }
                }
            }
            assertTrue(water > 100, "pools and falls: " + water);
        }
    }

    @Test
    void everyWaterfallLandsInAPoolAndTheSpringSitsInTheWall() {
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            assertEquals(2, g.falls.size(), "two waterfalls in cell " + c[0] + "," + c[1]);
            for (GoldenGrid.Fall w : g.falls) {
                assertTrue(g.g(w.u(), w.v()), "the column of the fall is garden air");
                assertEquals(GoldenGrid.POOL, g.coverAt(w.u(), w.v()), "it lands in a pool");
                assertEquals(0, g.groundAt(w.u(), w.v()), "at the pool's level");
                assertFalse(g.g(w.srcU(), w.srcV()), "the spring is in the wall");
                assertEquals(1, g.shellAt(w.srcU(), w.srcV()));
                int garden = 0;
                int[][] side = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] k : side) if (g.g(w.srcU() + k[0], w.srcV() + k[1])) garden++;
                assertEquals(1, garden, "the spring touches the air on one side only");
                assertTrue(w.height() >= 12 && w.height() <= g.ceil(w.u(), w.v()) - 4, "height " + w.height());
            }
        }
    }

    // ---------------------------------------------------------------- the city and its Treasury
    @Test
    void theCityStandsOnTheSameFloorUnderADomeThatClearsItsRoof() {
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            CityPlan city = g.city;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.inCity(u, v, 4)) continue;
                    if (Math.abs(u) > GoldenGrid.REACH || Math.abs(v) > GoldenGrid.REACH) continue;
                    assertTrue(g.g(u, v), "the city and its promenade are inside the cavern at " + u + "," + v);
                    if (g.inCity(u, v, 0)) assertTrue(g.floorY + g.ceil(u, v) >= CityPlan.CAP_Y + 5, "the dome clears the city's roof by 5 at " + u + "," + v);
                }
            }
            assertTrue(g.inCity(g.cityU, g.cityV, 0));
            assertEquals(g.cx + g.cityU, city.centreX);
        }
    }

    @Test
    void theTreasuryIsReachedByAStairAndTheGateIsTheOnlyWayIn() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            CityPlan city = GoldenGrid.of(c[0], c[1]).city;
            ZonePlan.Spot head = GoldenTreasury.stairHead(city);
            boolean[] seen = r.reach(head.x(), head.y(), head.z());
            ZonePlan.Spot central = p.centralRelic;
            assertTrue(r.reached(seen, central.x(), central.y(), central.z()), "the central pedestal is reached from the stair");
            for (ZonePlan.Spot m : p.minorRelics) assertTrue(r.reached(seen, m.x(), m.y(), m.z()), "the minor pedestal " + m);
            ZonePlan.Spot gate = p.gates.get(0);
            assertEquals("minecraft:air", r.at(gate.x(), gate.y(), gate.z()), "the doorway is open until the gate block is set");
            assertEquals("minecraft:air", r.at(gate.x(), gate.y() + 1, gate.z()));
            assertFalse(r.open(gate.x(), gate.y() + 2, gate.z()), "the doorway is two high");
            r.set(gate.x(), gate.y(), gate.z(), "nusmp:mana_gate");
            r.set(gate.x(), gate.y() + 1, gate.z(), "nusmp:mana_gate");
            boolean[] shut = r.reach(head.x(), head.y(), head.z());
            assertFalse(r.reached(shut, central.x(), central.y(), central.z()), "with the gate shut the Treasury cannot be reached");
            for (ZonePlan.Spot m : p.minorRelics) assertFalse(r.reached(shut, m.x(), m.y(), m.z()), "nor the minor pedestal " + m);
            assertTrue(r.reached(shut, gate.x(), gate.y(), gate.z() - 1), "but the foot of the stair and the tunnel up to the gate can");
        }
    }

    @Test
    void theHallLiesUnderTheAltarAndNoWaterIsNearIt() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            CityPlan city = GoldenGrid.of(c[0], c[1]).city;
            int f = city.floorY;
            for (int x = city.centreX - 8; x <= city.centreX + 8; x++) {
                for (int z = city.centreZ - 18; z <= city.centreZ + 8; z++) {
                    for (int y = f - 5; y <= f - 1; y++) {
                        if (!r.at(x, y, z).equals("minecraft:air")) continue;
                        int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
                        for (int[] k : d) assertNotEquals("minecraft:water", r.at(x + k[0], y + k[1], z + k[2]), "water beside the Treasury at " + (x - city.centreX) + "," + y + "," + (z - city.centreZ));
                    }
                }
            }
            assertEquals("city:ALTAR", r.at(city.altarX, city.altarY, city.altarZ), "the altar is right where it was");
            for (int y = f - 4; y <= f - 1; y++) assertEquals("minecraft:air", r.at(city.centreX, y, city.centreZ), "the hall is open above the central pedestal");
            assertTrue(city.floorY - 5 >= -59, "the hall's floor stays above the bedrock");
        }
    }

    @Test
    void nothingOfTheGardenIsLeftInsideTheOldCity() {
        Set<String> garden = Set.of("nusmp:golden_leaves", "minecraft:stripped_oak_log", "minecraft:mushroom_stem", "minecraft:red_mushroom_block", "minecraft:brown_mushroom_block",
                "minecraft:shroomlight", "minecraft:flowering_azalea_leaves", "minecraft:azalea_leaves", "minecraft:cave_vines", "minecraft:cave_vines_plant",
                "minecraft:grass_block", "minecraft:moss_block", "minecraft:podzol");
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.inCity(u, v, 0)) continue;
                    for (int y = g.floorY - 3; y <= CityPlan.CAP_Y; y++) {
                        assertFalse(garden.contains(r.at(g.cx + u, y, g.cz + v)), "a plant of the garden inside the city at " + u + "," + y + "," + v + ": " + r.at(g.cx + u, y, g.cz + v));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- the life of the garden
    @Test
    void theCavernHasItsGatesAndTheirPassagesAreShellStone() {
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            assertTrue(g.gates.size() >= 2, "at least two gates in cell " + c[0] + "," + c[1]);
            Set<Integer> sides = new HashSet<>();
            for (GoldenGrid.Gate gate : g.gates) {
                assertTrue(sides.add(gate.side()), "one gate a side");
                assertTrue(g.g(gate.u(), gate.v()), "the gate's last cell is air");
                for (int d = 1; d <= GoldenGrid.WALL; d++) {
                    int u = GoldenGrid.sideU(gate.side(), gate.tP() + d, gate.w()), v = GoldenGrid.sideV(gate.side(), gate.tP() + d, gate.w());
                    assertEquals(d, g.shellAt(u, v), "the passage runs through the shell, layer " + d);
                    for (int l = -1; l <= 1; l += 2) {
                        int lu = GoldenGrid.sideU(gate.side(), gate.tP() + d, gate.w() + l), lv = GoldenGrid.sideV(gate.side(), gate.tP() + d, gate.w() + l);
                        assertTrue(g.shellAt(lu, lv) >= 1, "the shell is thick beside the passage");
                    }
                }
                assertEquals(-1, g.shellAt(GoldenGrid.sideU(gate.side(), gate.tP() + GoldenGrid.WALL + 1, gate.w()), GoldenGrid.sideV(gate.side(), gate.tP() + GoldenGrid.WALL + 1, gate.w())),
                        "the cell beyond the door is outside the garden, where the way to a cave can start");
                assertTrue(gate.tP() - gate.tRing() >= 2, "a clear approach");
                assertTrue(Math.abs(g.cx + gate.u() - g.cx) <= GoldenGrid.REACH + 1);
            }
        }
    }

    @Test
    void theGroundIsNeverSteeperThanAStepAndEveryPoolIsOnTheFlat() {
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int[][] side = {{1, 0}, {0, 1}};
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.g(u, v)) continue;
                    for (int[] k : side) {
                        if (g.g(u + k[0], v + k[1]) && g.isRoof(u, v) == g.isRoof(u + k[0], v + k[1])) assertTrue(Math.abs(g.groundAt(u, v) - g.groundAt(u + k[0], v + k[1])) <= 1, "a step too high at " + u + "," + v);
                    }
                    byte kind = g.coverAt(u, v);
                    int base = g.isRoof(u, v) ? g.roofBase : 0;
                    if (kind == GoldenGrid.POOL || kind == GoldenGrid.SHORE || kind == GoldenGrid.PATH) assertEquals(base, g.groundAt(u, v), "flat under water and paths");
                    if (g.inCity(u, v, 6) && !g.isRoof(u, v)) assertEquals(0, g.groundAt(u, v), "flat round the city");
                    assertTrue(g.groundAt(u, v) - base <= (g.isRoof(u, v) ? 3 : 8));
                    if (g.isRoof(u, v)) assertEquals(g.roofBase, g.groundAt(u, v) - (g.groundAt(u, v) - g.roofBase), "the roof garden lies on the roof");
                }
            }
        }
    }

    @Test
    void theBigThingsKeepClearOfPathsPoolsAndEachOther() {
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            assertTrue(g.sites.size() >= 15, "trees and mushrooms: " + g.sites.size());
            int giants = 0;
            for (GoldenGrid.Site s : g.sites) {
                if (s.kind() == GoldenGrid.GIANT_MUSHROOM) giants++;
                for (int du = -s.r(); du <= s.r(); du++) {
                    for (int dv = -s.r(); dv <= s.r(); dv++) {
                        if (du * du + dv * dv > s.r() * s.r()) continue;
                        assertTrue(g.g(s.u() + du, s.v() + dv), "the crown or cap stays in the cavern");
                    }
                }
                int t = s.trunk();
                for (int du = -t - 1; du <= t + 1; du++) {
                    for (int dv = -t - 1; dv <= t + 1; dv++) {
                        if (du * du + dv * dv > (t + 1) * (t + 1) + 1) continue;
                        byte k = g.coverAt(s.u() + du, s.v() + dv);
                        assertTrue(k == GoldenGrid.FIXED, "the foot of a trunk is fixed ground, not a path or a pool: " + k);
                    }
                }
                assertTrue(s.ht() >= 2 && g.groundAt(s.u(), s.v()) + s.ht() + s.ry() + 2 <= g.ceil(s.u(), s.v()), "it fits under the ceiling");
            }
            assertTrue(giants >= 3, "mushrooms the size of houses: " + giants);
            for (int i = 0; i < g.sites.size(); i++) {
                for (int j = i + 1; j < g.sites.size(); j++) {
                    GoldenGrid.Site a = g.sites.get(i), b = g.sites.get(j);
                    assertTrue(GoldenGrid.dist(a.u() - b.u(), a.v() - b.v()) >= a.trunk() + b.trunk() + 2, "trunks do not meet");
                }
            }
            for (GoldenGrid.Grove gr : g.groves) {
                for (int du = -1; du <= 1; du++) {
                    for (int dv = -1; dv <= 1; dv++) {
                        assertEquals(g.groundAt(gr.u(), gr.v()), g.groundAt(gr.u() + du, gr.v() + dv), "a grove stands on level ground");
                        assertFalse(g.path[gr.u() + du + GoldenGrid.OFF][gr.v() + dv + GoldenGrid.OFF]);
                    }
                }
                assertTrue(g.dPath[gr.u() + GoldenGrid.OFF][gr.v() + GoldenGrid.OFF] >= 3, "a grove keeps off the paths");
            }
        }
    }

    @Test
    void theTreesHangVinesWithinReachOfTheirCanopies() {
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            for (GoldenGrid.Vine v : g.vines) {
                assertTrue(v.max() >= 2 && v.max() <= 11);
                assertTrue(g.g(v.u(), v.v()));
                assertTrue(!g.inCity(v.u(), v.v(), 1) || g.isRoof(v.u(), v.v()), "no vine over the city but the roof garden's own");
                assertNotEquals(GoldenGrid.PATH, g.coverAt(v.u(), v.v()), "no vine over a path");
                assertNotEquals(GoldenGrid.POOL, g.coverAt(v.u(), v.v()));
                assertTrue(v.anchor() - 1 - v.max() >= g.groundAt(v.u(), v.v()) + 1, "the vine's end is above the ground");
            }
        }
    }

    @Test
    void theCeilingGrowthNeverHangsAboveAWaterfall() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            for (GoldenGrid.Fall w : g.falls) {
                int top = g.floorY + g.ceil(w.u(), w.v());
                for (NexusOp o : p.ops) {
                    if (o.block().startsWith("minecraft:cave_vines") || o.block().equals("minecraft:glow_lichen") || o.block().equals("minecraft:spore_blossom") || o.block().equals("minecraft:hanging_roots")) {
                        boolean hits = o.x1() <= g.cx + w.u() && o.x2() >= g.cx + w.u() && o.z1() <= g.cz + w.v() && o.z2() >= g.cz + w.v() && o.y2() >= g.floorY + w.height() - 1 && o.y1() <= top;
                        assertFalse(hits, "something hangs in the fall's column: " + o);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- the roof garden
    @Test
    void theRoofOfTheOldCityIsAGardenOfItsOwnWithPlantsAPondAndAHedge() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int roof = 0, ponds = 0, plants = 0, hedge = 0, trees = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.isRoof(u, v)) continue;
                    roof++;
                    int top = g.floorY + g.groundAt(u, v);
                    String block = r.at(g.cx + u, top, g.cz + v);
                    String below = r.at(g.cx + u, g.floorY + g.roofBase - 1, g.cz + v);
                    assertTrue(below.startsWith("city:"), "the roof's own stone is under the soil at " + u + "," + v + ": " + below);
                    if (g.coverAt(u, v) == GoldenGrid.POOL) {
                        ponds++;
                        assertEquals("minecraft:water", block);
                        assertEquals(g.roofBase, g.groundAt(u, v), "a pond lies at the level of the soil");
                    } else {
                        assertFalse(r.open(g.cx + u, top, g.cz + v), "soil at " + u + "," + v + ": " + block);
                    }
                    String above = r.at(g.cx + u, top + 1, g.cz + v);
                    if (g.coverAt(u, v) != GoldenGrid.FIXED && g.coverAt(u, v) != GoldenGrid.POOL && !above.equals("minecraft:air")) plants++;
                    if (g.isRoofEdge(u, v) && above.equals("minecraft:flowering_azalea_leaves")) hedge++;
                    if (r.at(g.cx + u, top + 5, g.cz + v).startsWith("nusmp:golden") || above.equals("minecraft:stripped_oak_log")) trees++;
                }
            }
            assertTrue(roof > 2500, "a roof garden of some size: " + roof);
            assertTrue(ponds >= 20, "a pond: " + ponds);
            assertTrue(plants > roof / 4, "plants on the roof: " + plants + " of " + roof);
            assertTrue(hedge > 40, "a hedge round it: " + hedge);
            long onRoof = g.sites.stream().filter(s -> g.isRoof(s.u(), s.v())).count();
            assertTrue(onRoof >= 8, "trees and mushrooms on the roof: " + onRoof);
        }
    }

    @Test
    void thePondsOnTheRoofHoldTheirWater() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int water = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.isRoof(u, v)) continue;
                    for (int y = g.floorY + g.roofBase; y <= g.floorY + g.roofBase + 6; y++) {
                        if (!r.at(g.cx + u, y, g.cz + v).equals("minecraft:water")) continue;
                        water++;
                        assertFalse(r.open(g.cx + u, y - 1, g.cz + v) && !r.at(g.cx + u, y - 1, g.cz + v).equals("minecraft:water"), "water over nothing");
                        int[][] side = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                        for (int[] k : side) {
                            String n = r.at(g.cx + u + k[0], y, g.cz + v + k[1]);
                            assertTrue(!r.open(g.cx + u + k[0], y, g.cz + v + k[1]) || n.equals("minecraft:water"), "roof water would run off at " + u + "," + v + " into " + n);
                        }
                    }
                }
            }
            assertTrue(water > 20, "roof ponds: " + water);
        }
    }

    @Test
    void anIvyLadderClimbsTheWallToTheRoofAndAPersonCanWalkTheWholeRoofGarden() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            ZonePlan.Spot from = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            boolean[] seen = r.reach(from.x(), from.y(), from.z());
            long ladders = g.ivy.stream().filter(i -> i[3] == 1).count();
            assertTrue(ladders >= 6, "ladders up the wall: " + ladders);
            int stand = 0, reached = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.isRoof(u, v) || g.coverAt(u, v) == GoldenGrid.FIXED || g.coverAt(u, v) == GoldenGrid.POOL) continue;
                    int y = g.floorY + g.groundAt(u, v) + 1;
                    if (!r.standable(g.cx + u, y, g.cz + v)) continue;
                    stand++;
                    if (r.reached(seen, g.cx + u, y, g.cz + v)) reached++;
                }
            }
            assertTrue(stand > 2000, "places to stand on the roof: " + stand);
            assertTrue(reached >= stand * 0.95, "the roof garden can be walked after the climb: " + reached + " of " + stand);
        }
    }

    @Test
    void theIvyClingsToAWallAndLeavesThePromenadeOpen() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int[] dx = {0, 1, 0, -1}, dz = {-1, 0, 1, 0};
            assertFalse(g.ivy.isEmpty());
            for (int[] i : g.ivy) {
                assertTrue(g.g(i[0], i[1]) && g.coverAt(i[0], i[1]) == GoldenGrid.PATH, "ivy stands on the promenade's cells beside the wall");
                for (int y = g.floorY + 1; y < g.floorY + GoldenGrid.IVY_FROM; y++) {
                    String cell = r.at(g.cx + i[0], y, g.cz + i[1]);
                    assertTrue(cell.equals("minecraft:air") || (i[3] == 1 && cell.equals("minecraft:vine")), "the way stays open to head height (a ladder's ivy is passable): " + cell);
                }
                for (int y = g.floorY + (i[3] == 1 ? 1 : GoldenGrid.IVY_FROM); y <= g.floorY + g.roofBase; y++) {
                    if (!r.at(g.cx + i[0], y, g.cz + i[1]).equals("minecraft:vine")) {
                        assertEquals(0, i[3], "a ladder is whole");
                        continue;
                    }
                    for (int k = 0; k < 4; k++) {
                        if ((i[2] & (1 << k)) == 0) continue;
                        assertFalse(r.open(g.cx + i[0] + dx[k], y, g.cz + i[1] + dz[k]), "the ivy has a wall to cling to at " + i[0] + "," + y + "," + i[1] + " face " + k);
                    }
                }
                if (i[3] == 1) assertEquals("minecraft:vine", r.at(g.cx + i[0], g.floorY + g.roofBase, g.cz + i[1]), "a ladder reaches the roof's soil");
            }
        }
    }

    @Test
    void noOneWalksOffTheRoofExceptDownAnIvyLadder() {
        for (int[] c : GRID_CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int fenced = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.isRoofEdge(u, v)) continue;
                    int top = g.floorY + g.groundAt(u, v);
                    if (r.at(g.cx + u, top + 1, g.cz + v).equals("minecraft:flowering_azalea_leaves")) fenced++;
                }
            }
            long edge = 0;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) if (g.isRoofEdge(u, v)) edge++;
            long gaps = edge - fenced;
            assertTrue(gaps <= 9L * g.ivy.stream().filter(i -> i[3] == 1).count(), "the hedge is open only at the ladders: " + gaps + " gaps of " + edge);
            assertTrue(fenced > edge * 0.6, "and closed elsewhere: " + fenced + " of " + edge);
        }
    }
}
