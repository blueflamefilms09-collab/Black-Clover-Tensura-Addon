package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestMiragePlan.Fake;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestMiragePlan.Gateway;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestMiragePlan.Layout;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Tempest Mirage as planned: its size, its sealing, its Treasury and gate, its ways in, its fakes, what stands in it, and that a person can walk it. */
class TempestMiragePlanTest {
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {-3, 5}, {7, -4}, {-12, -9}, {20, 31}, {-1, -1}, {3, 2}};
    /** The heavier tests, which build a block grid of the whole village, look at these only. */
    private static final int[][] GRID_CELLS = {{0, 0}, {-3, 5}, {20, 31}, {-1, -1}};

    /** Blocks a creature walks through and that water would wash away: the plant life, carpets, hanging things. */
    private static final Set<String> OPEN = Set.of("minecraft:air", "minecraft:short_grass", "minecraft:fern", "minecraft:moss_carpet", "minecraft:poppy", "minecraft:dandelion",
            "minecraft:blue_orchid", "minecraft:allium", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:red_mushroom", "minecraft:brown_mushroom",
            "minecraft:pink_petals", "minecraft:vine", "minecraft:red_carpet", "minecraft:lantern", "minecraft:chain", "minecraft:cave_vines", "minecraft:cave_vines_plant",
            "minecraft:hanging_roots", "minecraft:spore_blossom", "minecraft:red_wall_banner", "minecraft:red_banner");

    // ---------------------------------------------------------------- a stand-in world to build the plan into
    /** The plan's ops applied to a block grid whose every cell starts as air (the worst case: a cave all round). */
    private static final class Raster {
        final int x0, y0, z0, sx, sy, sz;
        final int[] cells;
        final Map<String, Integer> ids = new HashMap<>();
        final List<String> names = new java.util.ArrayList<>();

        Raster(ZonePlan p) { this(p, p.ops.size()); }

        Raster(ZonePlan p, int count) {
            x0 = p.footprint.minX() - 4;
            z0 = p.footprint.minZ() - 4;
            y0 = p.floorY - 8;
            sx = p.footprint.maxX() + 4 - x0 + 1;
            sz = p.footprint.maxZ() + 4 - z0 + 1;
            sy = p.topY + 4 - y0 + 1;
            cells = new int[sx * sy * sz];
            id("minecraft:air");
            for (int i = 0; i < count; i++) apply(p.ops.get(i));
        }

        int id(String name) {
            return ids.computeIfAbsent(name, n -> {
                names.add(n);
                return names.size() - 1;
            });
        }

        int index(int x, int y, int z) { return ((y - y0) * sz + (z - z0)) * sx + (x - x0); }

        boolean in(int x, int y, int z) { return x >= x0 && x < x0 + sx && y >= y0 && y < y0 + sy && z >= z0 && z < z0 + sz; }

        String at(int x, int y, int z) { return in(x, y, z) ? names.get(cells[index(x, y, z)]) : "minecraft:air"; }

        void set(int x, int y, int z, String name) { cells[index(x, y, z)] = id(name); }

        void apply(NexusOp o) {
            int block = o.kind() == NexusOp.Kind.CARVE ? 0 : id(o.block());
            for (int y = o.y1(); y <= o.y2(); y++) {
                for (int z = o.z1(); z <= o.z2(); z++) {
                    for (int x = o.x1(); x <= o.x2(); x++) {
                        assertTrue(in(x, y, z), "op reaches outside the volume: " + o);
                        int i = index(x, y, z);
                        if (o.kind() == NexusOp.Kind.FILL_AIR && cells[i] != 0) continue;
                        cells[i] = block;
                    }
                }
            }
        }

        boolean open(int x, int y, int z) { return OPEN.contains(at(x, y, z)); }
    }

    // ---------------------------------------------------------------- the plan itself
    @Test
    void everyCellGivesAnInOrderAndDeterministicPlan() {
        for (int[] c : CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            assertSame(NexusZone.TEMPEST_MIRAGE, p.zone);
            assertTrue(p.insideCell(), "inside its core cell: " + c[0] + "," + c[1]);
            assertFalse(p.opsStrayFromFootprint(), "no op writes outside the footprint's chunks");
            assertEquals(4, p.phaseCount(), "carve; shell and floor; structures; details");
            assertTrue(p.topY > p.floorY);
            assertEquals(TempestMiragePlan.centreX(c[0], c[1]), p.centreX());
            assertEquals(TempestMiragePlan.centreZ(c[0], c[1]), p.centreZ());
            assertTrue(Math.abs(p.centreX() - NexusZone.centre(c[0])) <= TempestMiragePlan.JITTER);
            assertTrue(Math.abs(p.centreZ() - NexusZone.centre(c[1])) <= TempestMiragePlan.JITTER);
            assertTrue(p.floorY >= -57 && p.floorY <= -55);
            ZonePlan again = TempestMiragePlan.of(c[0], c[1]);
            assertSame(p, again, "cached");
        }
    }

    @Test
    void theSamePlanComesOutEveryTime() throws Exception {
        java.lang.reflect.Method build = TempestMiragePlan.class.getDeclaredMethod("build", int.class, int.class);
        build.setAccessible(true);
        for (int[] c : new int[][]{{0, 0}, {5, -7}}) {
            Layout a = (Layout) build.invoke(null, c[0], c[1]), b = (Layout) build.invoke(null, c[0], c[1]);
            assertEquals(a.plan().ops, b.plan().ops, "the ops are the same");
            assertEquals(a.fakes().size(), b.fakes().size());
            assertEquals(a.plan().allSpawns(), b.plan().allSpawns());
            assertEquals(a.gateways(), b.gateways());
            assertEquals(a.hallSide(), b.hallSide());
            for (int i = 0; i < a.fakes().size(); i++) assertEquals(a.fakes().get(i), b.fakes().get(i));
        }
    }

    @Test
    void theCellsDiffer() {
        Set<String> seen = new HashSet<>();
        for (int[] c : CELLS) {
            ZonePlan p = TempestMiragePlan.of(c[0], c[1]);
            seen.add((p.centreX() - NexusZone.centre(c[0])) + "," + (p.centreZ() - NexusZone.centre(c[1])) + "," + p.floorY + "," + TempestMiragePlan.hallSide(c[0], c[1]));
        }
        assertTrue(seen.size() >= 4, "the villages are not all alike: " + seen);
        Set<Integer> sides = new HashSet<>();
        for (int x = -10; x < 10; x++) for (int z = -10; z < 10; z++) sides.add(TempestMiragePlan.hallSide(x, z));
        assertEquals(4, sides.size(), "the hall faces every side somewhere");
    }

    @Test
    void theOpCountIsBounded() {
        for (int[] c : CELLS) {
            ZonePlan p = TempestMiragePlan.of(c[0], c[1]);
            assertTrue(p.ops.size() < 40_000, "ops: " + p.ops.size());
            long cells = 0;
            for (NexusOp o : p.ops) cells += o.volume();
            assertTrue(cells < 1_500_000L, "cells visited: " + cells);
        }
    }

    @Test
    void everyOpLiesInsideTheFootprintAndItsHeights() {
        for (int[] c : CELLS) {
            ZonePlan p = TempestMiragePlan.of(c[0], c[1]);
            for (NexusOp o : p.ops) {
                assertTrue(o.y1() >= p.floorY - TempestMiragePlan.FOUNDATION && o.y2() <= p.topY, "height of " + o);
                for (int z = o.z1(); z <= o.z2(); z++) {
                    // a box is a row (or a column): its two ends are enough, the footprint is convex
                    assertTrue(p.footprint.contains(o.x1(), z) && p.footprint.contains(o.x2(), z), "op leaves the footprint: " + o);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the Treasury, its gate and the relics
    @Test
    void oneCentralPedestalFourMinorOnesOneGate() {
        for (int[] c : CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            assertNotNull(p.centralRelic, "a central relic: its taking collapses the Nexus");
            assertEquals(4, p.minorRelics.size());
            assertEquals(1, p.gates.size());
            Set<ZonePlan.Spot> all = new HashSet<>(p.minorRelics);
            all.add(p.centralRelic);
            assertEquals(5, all.size(), "five different pedestals");
            int[] t = l.treasury();
            for (ZonePlan.Spot s : all) {
                assertTrue(s.x() >= t[0] && s.x() <= t[3] && s.y() >= t[1] && s.y() <= t[4] && s.z() >= t[2] && s.z() <= t[5], "pedestal in the Treasury: " + s);
                assertEquals(p.floorY + 2, s.y(), "on a plinth one above the floor");
            }
            ZonePlan.Spot gate = p.gates.get(0);
            assertEquals(p.floorY + 1, gate.y());
            assertFalse(gate.x() >= t[0] && gate.x() <= t[3] && gate.z() >= t[2] && gate.z() <= t[5], "the gate is in the wall, not in the room");
            int centralIndex = TempestMiragePlan.centralRelicIndex(c[0], c[1], 5);
            assertTrue(centralIndex >= 0 && centralIndex < 5);
        }
        assertEquals(0, TempestMiragePlan.centralRelicIndex(0, 0, 0));
        Set<Integer> picks = new HashSet<>();
        for (int x = -10; x < 10; x++) for (int z = -10; z < 10; z++) picks.add(TempestMiragePlan.centralRelicIndex(x, z, 5));
        assertEquals(5, picks.size(), "every relic is the central one somewhere");
    }

    // ---------------------------------------------------------------- sealing and shape
    private static boolean airRegion(int u, int y, int v, int f) {
        int h = y - f;
        if (h < 0 || h > TempestMiragePlan.HEIGHT) return false;
        if (h == 0) return Footprint.inside(u, v, TempestMiragePlan.A, TempestMiragePlan.B);
        return Footprint.inside(u, v, TempestMiragePlan.airX(h), TempestMiragePlan.airZ(h));
    }

    @Test
    void theCavernIsSealedAgainstEverythingOutsideIt() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = TempestMiragePlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            // flood from the outside, through everything water would pass: it must not reach the floor of the village
            boolean[] seen = new boolean[r.cells.length];
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            int[] start = {r.x0, r.y0 + 1, r.z0};
            queue.add(start);
            seen[r.index(start[0], start[1], start[2])] = true;
            int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            int reached = 0;
            while (!queue.isEmpty()) {
                int[] q = queue.poll();
                reached++;
                for (int[] d : dirs) {
                    int x = q[0] + d[0], y = q[1] + d[1], z = q[2] + d[2];
                    if (!r.in(x, y, z) || seen[r.index(x, y, z)] || !r.open(x, y, z)) continue;
                    seen[r.index(x, y, z)] = true;
                    queue.add(new int[]{x, y, z});
                }
            }
            assertTrue(reached > 1000, "the flood did get going outside");
            for (int dx = -40; dx <= 40; dx += 8) {
                for (int dz = -36; dz <= 36; dz += 8) {
                    if (!airRegion(dx, p.floorY + 5, dz, p.floorY)) continue;
                    assertFalse(seen[r.index(p.centreX() + dx, p.floorY + 5, p.centreZ() + dz)], "the flood reached the inside at " + dx + "," + dz);
                }
            }
            assertFalse(seen[r.index(p.centreX(), p.floorY + 1, p.centreZ())]);
            assertFalse(seen[r.index(p.centreX(), p.topY - 5, p.centreZ())], "not through the roof");
        }
    }

    @Test
    void theAirIsClearedAndTheShellIsSolid() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = TempestMiragePlan.of(c[0], c[1]);
            int f = p.floorY;
            Raster carved = new Raster(p, p.phaseStart(1));
            for (int h = 1; h <= TempestMiragePlan.HEIGHT; h++) {
                int a = TempestMiragePlan.airX(h), b = TempestMiragePlan.airZ(h);
                assertEquals("minecraft:air", carved.at(p.centreX() + a, f + h, p.centreZ()), "the air's edge is cleared at layer " + h);
                assertEquals("minecraft:air", carved.at(p.centreX(), f + h, p.centreZ() + b));
            }
            Raster built = new Raster(p, p.phaseStart(2));
            for (int h = 1; h <= TempestMiragePlan.HEIGHT; h++) {
                int a = TempestMiragePlan.airX(h);
                assertNotEquals("minecraft:air", built.at(p.centreX() + a + 1, f + h, p.centreZ()), "wall beyond the air at layer " + h);
                assertNotEquals("minecraft:air", built.at(p.centreX() + a + TempestMiragePlan.WALL, f + h, p.centreZ()), "wall is " + TempestMiragePlan.WALL + " thick");
            }
            for (int h = TempestMiragePlan.HEIGHT + 1; h <= TempestMiragePlan.HEIGHT + TempestMiragePlan.ROOF; h++) assertNotEquals("minecraft:air", built.at(p.centreX(), f + h, p.centreZ()), "roof");
            for (int d = 1; d <= TempestMiragePlan.FOUNDATION; d++) assertNotEquals("minecraft:air", built.at(p.centreX(), f - d, p.centreZ()), "foundation");
            assertNotEquals("minecraft:air", built.at(p.centreX(), f, p.centreZ()), "floor");
        }
    }

    @Test
    void everythingBuiltInsideStandsInTheAirOrIsTheGatesPlugOrTheCeilingsSkin() {
        for (int[] c : GRID_CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            int f = p.floorY, cx = p.centreX(), cz = p.centreZ();
            for (int i = p.phaseStart(2); i < p.ops.size(); i++) {
                NexusOp o = p.ops.get(i);
                if (o.block().equals(NexusOp.AIR) || o.kind() == NexusOp.Kind.CARVE) continue;
                boolean skin = o.block().equals("minecraft:moss_block") || o.block().equals("minecraft:shroomlight");
                for (int y = o.y1(); y <= o.y2(); y++) {
                    for (int z = o.z1(); z <= o.z2(); z++) {
                        for (int x = o.x1(); x <= o.x2(); x++) {
                            if (airRegion(x - cx, y, z - cz, f)) continue;
                            if (skin && airRegion(x - cx, y - 1, z - cz, f)) continue;
                            assertTrue(inPlug(l, x, y, z), "op " + o + " builds in the shell at " + x + "," + y + "," + z);
                        }
                    }
                }
            }
        }
    }

    /** Is the cell part of a gate's plug: within two of the passage's line, and in the shell beyond the air's edge? */
    private static boolean inPlug(Layout l, int x, int y, int z) {
        int f = l.plan().floorY;
        if (y < f + 1 || y > f + 6) return false;
        for (Gateway g : l.gateways()) {
            int along = (x - g.firstX()) * g.dx() + (z - g.firstZ()) * g.dz();
            int across = Math.abs((x - g.firstX()) * g.dz() + (z - g.firstZ()) * g.dx());
            if (along >= 0 && along <= TempestMiragePlan.WALL - 1 && across <= 2) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the gates and the ways in
    @Test
    void fourGatesOneOnEachSideSealedByAPlugAndThenTheShellThenTheDoor() {
        for (int[] c : GRID_CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            assertEquals(4, l.gateways().size());
            assertEquals(4, p.entrances.size());
            Raster r = new Raster(p);
            Set<Integer> sides = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                Gateway g = l.gateways().get(i);
                sides.add(g.side());
                assertEquals(1, Math.abs(g.dx()) + Math.abs(g.dz()), "an axis direction");
                int y = p.floorY + 1;
                assertTrue(p.footprint.contains(g.firstX(), g.firstZ()), "the first cell is in the footprint");
                assertTrue(p.footprint.contains(g.doorX(), g.doorZ()), "the door's cell is in the footprint");
                assertFalse(p.footprint.contains(g.doorX() + g.dx(), g.doorZ() + g.dz()), "the cell outside the door is outside it");
                assertEquals(TempestMiragePlan.WALL - TempestMiragePlan.GATE_FIRST, Math.abs(g.doorX() - g.firstX()) + Math.abs(g.doorZ() - g.firstZ()), "the door ends the shell");
                for (int k = 0; k <= TempestMiragePlan.WALL - TempestMiragePlan.GATE_FIRST; k++) {
                    int x = g.firstX() + g.dx() * k, z = g.firstZ() + g.dz() * k;
                    assertNotEquals("minecraft:air", r.at(x, y, z), "sealed at " + k);
                    assertNotEquals("minecraft:air", r.at(x, y + 1, z), "sealed above at " + k);
                }
                ZonePlan.Spot e = p.entrances.get(i);
                assertEquals(y, e.y());
                assertEquals("minecraft:air", r.at(e.x(), y, e.z()), "the entrance's feet cell is open");
                assertEquals("minecraft:air", r.at(e.x(), y + 1, e.z()));
                assertEquals(g.firstX() - 2 * g.dx(), e.x());
                assertEquals(g.firstZ() - 2 * g.dz(), e.z());
                assertEquals("minecraft:air", r.at(g.firstX() - g.dx(), y, g.firstZ() - g.dz()), "the cell before the plug is the archway's end");
            }
            assertEquals(Set.of(0, 1, 2, 3), sides);
        }
    }

    // ---------------------------------------------------------------- spots
    @Test
    void everySpotStandsOnTheFloorInTheOpen() {
        for (int[] c : GRID_CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            Raster r = new Raster(p);
            int y = p.floorY + 1;
            assertEquals(30, p.spawns(ZonePlan.ROLE_SWARM).size(), "a swarm's worth of places");
            assertEquals(4, p.spawns(ZonePlan.ROLE_GUARD).size());
            assertEquals(4, p.spawns(ZonePlan.ROLE_ELITE).size());
            assertEquals(1, p.spawns("rally").size());
            assertEquals(1, p.spawns(ZonePlan.ROLE_ARRIVAL).size());
            assertTrue(p.spawns(ZonePlan.ROLE_BOSS).isEmpty(), "no boss: the central relic collapses it");
            for (Map.Entry<String, List<ZonePlan.Spot>> e : p.allSpawns().entrySet()) {
                for (ZonePlan.Spot s : e.getValue()) {
                    assertEquals(y, s.y(), e.getKey());
                    assertTrue(p.footprint.contains(s.x(), s.z(), -TempestMiragePlan.WALL), e.getKey() + " is inside the air");
                    assertNotEquals("minecraft:air", r.at(s.x(), y - 1, s.z()), e.getKey() + " has a floor");
                    assertFalse(r.at(s.x(), y - 1, s.z()).equals("minecraft:water"), e.getKey() + " does not stand in a well");
                    assertTrue(r.open(s.x(), y, s.z()), e.getKey() + " feet cell " + s + " is " + r.at(s.x(), y, s.z()));
                    assertTrue(r.open(s.x(), y + 1, s.z()), e.getKey() + " head cell is " + r.at(s.x(), y + 1, s.z()));
                }
            }
            assertTrue(l.standard().y() == y);
            assertEquals(l.rally(), p.spawns("rally").get(0));
        }
    }

    @Test
    void aPersonCanWalkFromEveryGateToTheRallyPointTheTreasuryAndEverySpot() {
        for (int[] c : GRID_CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            Raster r = new Raster(p);
            for (ZonePlan.Spot start : p.entrances) {
                boolean[] seen = walk(r, start.x(), start.y(), start.z());
                String where = " from the entrance at " + start;
                assertTrue(seen[r.index(l.rally().x(), l.rally().y(), l.rally().z())], "the rally point" + where);
                ZonePlan.Spot gate = p.gates.get(0);
                assertTrue(seen[r.index(gate.x(), gate.y(), gate.z())], "the Treasury's doorway" + where);
                for (ZonePlan.Spot pedestal : p.minorRelics) assertTrue(seen[r.index(pedestal.x(), pedestal.y() - 1, pedestal.z())] || reachesNeighbour(seen, r, pedestal), "a minor pedestal" + where);
                assertTrue(reachesNeighbour(seen, r, p.centralRelic), "the central pedestal" + where);
                for (Map.Entry<String, List<ZonePlan.Spot>> e : p.allSpawns().entrySet()) {
                    for (ZonePlan.Spot s : e.getValue()) assertTrue(seen[r.index(s.x(), s.y(), s.z())], e.getKey() + " spot " + s + where);
                }
                for (ZonePlan.Spot other : p.entrances) assertTrue(seen[r.index(other.x(), other.y(), other.z())], "another entrance" + where);
            }
        }
    }

    /** Can a person stand beside the pedestal: next to it on the floor, or on the plinth it stands on? */
    private static boolean reachesNeighbour(boolean[] seen, Raster r, ZonePlan.Spot pedestal) {
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            if (seen[r.index(pedestal.x() + d[0], pedestal.y() - 1, pedestal.z() + d[1])] || seen[r.index(pedestal.x() + d[0], pedestal.y(), pedestal.z() + d[1])]) return true;
        }
        return false;
    }

    /** Every cell a person can stand in that a walk from (x, y, z) reaches: through open cells, one step up or down at a time, two high. */
    private static boolean[] walk(Raster r, int x, int y, int z) {
        boolean[] seen = new boolean[r.cells.length];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{x, y, z});
        seen[r.index(x, y, z)] = true;
        while (!queue.isEmpty()) {
            int[] q = queue.poll();
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = q[0] + d[0], nz = q[2] + d[1];
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = q[1] + dy;
                    if (!r.in(nx, ny, nz) || seen[r.index(nx, ny, nz)] || !stand(r, nx, ny, nz)) continue;
                    if (dy == 1 && !r.open(q[0], q[1] + 2, q[2])) continue;
                    if (dy == -1 && !r.open(nx, ny + 2, nz)) continue;
                    seen[r.index(nx, ny, nz)] = true;
                    queue.add(new int[]{nx, ny, nz});
                }
            }
        }
        return seen;
    }

    private static boolean stand(Raster r, int x, int y, int z) {
        return r.open(x, y, z) && r.open(x, y + 1, z) && !r.open(x, y - 1, z) && !r.at(x, y - 1, z).equals("minecraft:water");
    }

    // ---------------------------------------------------------------- the fakes
    @Test
    void theFakesAreSelfContainedAndDissolveIntoOpenGround() {
        for (int[] c : GRID_CELLS) {
            Layout l = TempestMiragePlan.layout(c[0], c[1]);
            ZonePlan p = l.plan();
            assertEquals(3, l.fakes().size(), "three houses are fakes");
            Raster full = new Raster(p);
            Set<NexusOp> all = new HashSet<>(p.ops);
            for (int i = 0; i < l.fakes().size(); i++) {
                Fake f = l.fakes().get(i);
                assertEquals(i, f.id());
                assertFalse(f.ops().isEmpty());
                assertTrue(f.minY() == p.floorY + 1, "the fake starts above the floor");
                for (NexusOp o : f.ops()) {
                    assertTrue(all.contains(o), "a fake's op is part of the plan");
                    assertTrue(o.y1() > p.floorY, "a fake never touches the floor: " + o);
                    assertTrue(o.x1() >= f.minX() && o.x2() <= f.maxX() && o.z1() >= f.minZ() && o.z2() <= f.maxZ() && o.y2() <= f.maxY(), "inside the bounds: " + o);
                }
                assertNotEquals("minecraft:air", full.at(f.markX(), f.markY(), f.markZ()), "the mark is a block of the roof");
                boolean covered = false;
                for (NexusOp o : f.ops()) {
                    if (f.markX() >= o.x1() && f.markX() <= o.x2() && f.markY() >= o.y1() && f.markY() <= o.y2() && f.markZ() >= o.z1() && f.markZ() <= o.z2() && !o.block().equals(NexusOp.AIR)) covered = true;
                }
                assertTrue(covered, "the mark is made by the fake's own ops");
                assertEquals(5, f.ambush().size());
                // take the fake away: its ambush cells must be open with a floor
                Raster gone = new Raster(p);
                for (NexusOp o : f.ops()) {
                    for (int y = o.y1(); y <= o.y2(); y++) for (int z = o.z1(); z <= o.z2(); z++) for (int x = o.x1(); x <= o.x2(); x++) gone.set(x, y, z, "minecraft:air");
                }
                for (ZonePlan.Spot s : f.ambush()) {
                    assertEquals(p.floorY + 1, s.y());
                    assertTrue(f.near(s.x(), s.z(), 0.0), "inside the fake's bounds");
                    assertTrue(gone.open(s.x(), s.y(), s.z()) && gone.open(s.x(), s.y() + 1, s.z()), "open once it has dissolved: " + s);
                    assertNotEquals("minecraft:air", gone.at(s.x(), s.y() - 1, s.z()), "with a floor under it");
                }
                assertTrue(f.near(f.doorX() + 0.5, f.doorZ() + 0.5, TempestRules.TRIGGER), "the door's front arms it");
                for (int j = i + 1; j < l.fakes().size(); j++) {
                    Fake g = l.fakes().get(j);
                    assertTrue(f.maxX() < g.minX() || g.maxX() < f.minX() || f.maxZ() < g.minZ() || g.maxZ() < f.minZ(), "two fakes never overlap");
                }
            }
        }
    }

    @Test
    void aFakeIsRebuiltByItsOwnOpsToWhatItWas() {
        Layout l = TempestMiragePlan.layout(0, 0);
        ZonePlan p = l.plan();
        Raster full = new Raster(p);
        Fake f = l.fakes().get(0);
        Raster raster = new Raster(p);
        for (NexusOp o : f.ops()) for (int y = o.y1(); y <= o.y2(); y++) for (int z = o.z1(); z <= o.z2(); z++) for (int x = o.x1(); x <= o.x2(); x++) raster.set(x, y, z, "minecraft:air");
        assertEquals("minecraft:air", raster.at(f.markX(), f.markY(), f.markZ()));
        for (NexusOp o : f.ops()) raster.apply(o);
        for (int y = f.minY(); y <= f.maxY(); y++) {
            for (int z = f.minZ(); z <= f.maxZ(); z++) {
                for (int x = f.minX(); x <= f.maxX(); x++) {
                    String a = full.at(x, y, z), b = raster.at(x, y, z);
                    if (a.equals(b)) continue;
                    // the only differences allowed are the growth on the outside of the house, which is not the fake's
                    assertTrue(OPEN.contains(a) || OPEN.contains(b) || a.equals("minecraft:air"), "cell " + x + "," + y + "," + z + " is " + a + " but rebuilt " + b);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the queries
    @Test
    void theDomeIsMonotoneAndTheQueriesAgree() {
        int last = Integer.MAX_VALUE;
        for (int h = 0; h <= TempestMiragePlan.HEIGHT; h++) {
            assertTrue(TempestMiragePlan.airX(h) <= last, "the dome only narrows");
            last = TempestMiragePlan.airX(h);
            assertTrue(TempestMiragePlan.airZ(h) > 0 && TempestMiragePlan.airX(h) > 0);
        }
        assertEquals(TempestMiragePlan.A, TempestMiragePlan.airX(0));
        assertEquals(TempestMiragePlan.HEIGHT, TempestMiragePlan.columnTop(0, 0));
        assertEquals(0, TempestMiragePlan.columnTop(TempestMiragePlan.A + 5, 0));
        assertTrue(TempestMiragePlan.columnTop(TempestMiragePlan.A, 0) >= TempestMiragePlan.WALL_H);
        assertTrue(TempestMiragePlan.insideFloor(0, 0, 0));
        assertFalse(TempestMiragePlan.insideFloor(TempestMiragePlan.A + 1, 0, 0));
        assertTrue(TempestMiragePlan.insideFloor(TempestMiragePlan.A, 0, 0));
        assertTrue(TempestMiragePlan.insideFloor(TempestMiragePlan.A + 1, 0, 1));
        for (Layout l : new Layout[]{TempestMiragePlan.layout(0, 0)}) {
            int[] t = TempestMiragePlan.treasuryBounds(0, 0);
            assertArrayEquals(l.treasury(), t);
            assertTrue(t[0] <= t[3] && t[1] <= t[4] && t[2] <= t[5]);
        }
        assertEquals(TempestMiragePlan.B, TempestMiragePlan.reach(0));
        assertEquals(TempestMiragePlan.A, TempestMiragePlan.reach(1));
        assertEquals(TempestMiragePlan.B, TempestMiragePlan.reach(2));
        assertEquals(TempestMiragePlan.A, TempestMiragePlan.reach(3));
    }

    @Test
    void theSidesMapTheWayTheOpsFrameDoes() {
        // local +z is outward: the side's step must point away from the middle on its own side
        assertEquals(-1, TempestMiragePlan.SIDES[0].dz());
        assertEquals(1, TempestMiragePlan.SIDES[1].dx());
        assertEquals(1, TempestMiragePlan.SIDES[2].dz());
        assertEquals(-1, TempestMiragePlan.SIDES[3].dx());
        for (TempestMiragePlan.Side s : TempestMiragePlan.SIDES) assertEquals(1, Math.abs(s.dx()) + Math.abs(s.dz()));
    }
}
