package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Monarch's Vault as planned: its size, its sealing, its Treasury and gate, its ways in, what stands in it. */
class MonarchVaultPlanTest {
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {-3, 5}, {7, -4}, {-12, -9}, {20, 31}, {-1, -1}, {3, 2}};
    /** The heavier tests, which build a block grid of the whole hall, look at these only: one for each side the Treasury can be on. */
    private static final int[][] GRID_CELLS = {{0, 0}, {1, 0}, {-3, 5}, {20, 31}};

    // ---------------------------------------------------------------- a stand-in world to build the plan into
    /** The plan's ops applied to a block grid whose every cell starts as air (the worst case: a cave all round), to check the hall is sealed and shaped. */
    private static final class Raster {
        final int x0, y0, z0, sx, sy, sz;
        final int[] cells;
        final Map<String, Integer> ids = new HashMap<>();
        final List<String> names = new java.util.ArrayList<>();

        Raster(ZonePlan p) { this(p, p.ops.size()); }

        /** The grid with only the first `count` ops applied. */
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

        /** Things that are in a cell but do not close it. */
        boolean open(int x, int y, int z) {
            String n = at(x, y, z);
            return n.equals("minecraft:air") || n.endsWith("_carpet") || n.endsWith("lantern") || n.equals("minecraft:chain") || n.endsWith("banner")
                    || n.endsWith("skull") || n.equals("minecraft:candle") || n.equals("minecraft:amethyst_cluster");
        }

        /** Every cell reachable from (x, y, z) through open cells, with the cells in `shut` counted as closed. */
        BitSet flood(int x, int y, int z, BitSet shut) {
            BitSet seen = new BitSet(cells.length);
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[]{x, y, z});
            seen.set(index(x, y, z));
            int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] k : d) {
                    int nx = c[0] + k[0], ny = c[1] + k[1], nz = c[2] + k[2];
                    if (!in(nx, ny, nz)) continue;
                    int i = index(nx, ny, nz);
                    if (seen.get(i) || !open(nx, ny, nz) || shut.get(i)) continue;
                    seen.set(i);
                    q.add(new int[]{nx, ny, nz});
                }
            }
            return seen;
        }
    }

    // ---------------------------------------------------------------- shape
    @Test
    void theHallProfileIsADomeWithinTheDimension() {
        assertEquals(MonarchVaultPlan.R, MonarchVaultPlan.reach(1), "the foot of the wall is at full radius");
        assertEquals(MonarchVaultPlan.R, MonarchVaultPlan.reach(26));
        assertTrue(MonarchVaultPlan.reach(27) < MonarchVaultPlan.R, "above that the dome closes in");
        assertTrue(MonarchVaultPlan.reach(MonarchVaultPlan.HEIGHT) > 0);
        assertEquals(-1, MonarchVaultPlan.reach(MonarchVaultPlan.HEIGHT + 1));
        assertEquals(-1, MonarchVaultPlan.reach(0));
        for (int h = 2; h <= MonarchVaultPlan.HEIGHT; h++) assertTrue(MonarchVaultPlan.reach(h) <= MonarchVaultPlan.reach(h - 1), "the ceiling only comes down toward the wall");
        assertEquals(MonarchVaultPlan.HEIGHT, MonarchVaultPlan.columnTop(0, 0));
        assertEquals(26, MonarchVaultPlan.columnTop(44, 0), "at the wall the ceiling is 26 above the floor");
        assertEquals(0, MonarchVaultPlan.columnTop(45, 0), "outside the hall there is no air");
        for (int x = -44; x <= 44; x += 4) for (int z = -44; z <= 44; z += 4) {
            if (x * x + z * z > 44 * 44) continue;
            int top = MonarchVaultPlan.columnTop(x, z);
            assertTrue(top >= 26 && top <= MonarchVaultPlan.HEIGHT, x + "," + z + " -> " + top);
        }
    }

    @Test
    void eachCellGetsOneDeterministicPlanInsideItsCellAndTheDimension() {
        Set<String> seen = new HashSet<>();
        for (int[] c : CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            ZonePlan again = MonarchVaultPlan.of(c[0], c[1]);
            assertEquals(p.ops, again.ops, "the same cell always gives the same hall");
            assertEquals(p.centralRelic, again.centralRelic);
            assertSame(NexusZone.MONARCH_VAULT, p.zone);
            assertTrue(p.insideCell(), "footprint and edge stay in the 128-block cell: " + c[0] + "," + c[1]);
            assertFalse(p.opsStrayFromFootprint(), "no op reaches a chunk the footprint does not cover");
            assertEquals(Footprint.Shape.OVAL, p.footprint.shape());
            assertEquals(MonarchVaultPlan.OUTER, p.footprint.halfX());
            assertEquals(p.footprint.halfX(), p.footprint.halfZ());
            assertEquals(MonarchVaultPlan.centreX(c[0], c[1]), p.centreX());
            assertEquals(MonarchVaultPlan.centreZ(c[0], c[1]), p.centreZ());
            assertTrue(Math.abs(p.centreX() - NexusZone.centre(c[0])) <= MonarchVaultPlan.JITTER);
            assertTrue(Math.abs(p.centreZ() - NexusZone.centre(c[1])) <= MonarchVaultPlan.JITTER);
            assertEquals(MonarchVaultPlan.floorY(c[0], c[1]), p.floorY);
            assertTrue(p.floorY >= -57 && p.floorY <= -55, "floor " + p.floorY);
            assertTrue(p.floorY < -40, "the floor lies in the deepest tier (y < -40)");
            assertEquals(p.floorY + MonarchVaultPlan.HEIGHT + MonarchVaultPlan.ROOF, p.topY);
            assertTrue(p.topY < 127 && p.floorY - MonarchVaultPlan.FOUNDATION >= -64, "inside the dimension -64..127");
            assertEquals(4, p.phaseCount(), "carve, floor and shell, structures, details");
            assertTrue(p.ops.size() > 2000 && p.ops.size() < 60000, "ops: " + p.ops.size());
            int cell0x = c[0] * NexusZone.CELL, cell0z = c[1] * NexusZone.CELL;
            for (NexusOp o : p.ops) {
                assertTrue(o.y1() >= -64 && o.y2() <= 127, "op outside the dimension: " + o);
                assertTrue(o.y1() >= p.floorY - MonarchVaultPlan.FOUNDATION && o.y2() <= p.topY, "op outside the hall's height: " + o);
                assertTrue(o.x1() >= cell0x && o.x2() < cell0x + NexusZone.CELL && o.z1() >= cell0z && o.z2() < cell0z + NexusZone.CELL, "op outside its cell: " + o);
                assertTrue(o.x1() >= p.footprint.minX() && o.x2() <= p.footprint.maxX() && o.z1() >= p.footprint.minZ() && o.z2() <= p.footprint.maxZ(), "op outside the footprint: " + o);
                assertTrue(o.block().matches("[a-z0-9_]+:[a-z0-9_]+"), o.block());
            }
            seen.add(p.centreX() + "," + p.centreZ() + "," + p.floorY);
        }
        assertTrue(seen.size() > 4, "different cells give different halls");
    }

    @Test
    void nothingHereCanBeLooted() {
        String[] holders = {"chest", "barrel", "shulker", "hopper", "dropper", "dispenser", "furnace", "smoker", "lectern", "jukebox", "crafter", "brewing", "bookshelf", "spawner"};
        for (int[] c : CELLS) {
            for (NexusOp o : MonarchVaultPlan.of(c[0], c[1]).ops) {
                for (String h : holders) assertFalse(o.block().contains(h), "a block that holds items: " + o.block());
            }
        }
    }

    // ---------------------------------------------------------------- what the plan marks
    @Test
    void thePlanMarksItsRelicsGateEntrancesAndBoss() {
        for (int[] c : CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            assertNotNull(p.centralRelic, "exactly one central pedestal");
            assertEquals(4, p.minorRelics.size());
            assertEquals(1, p.gates.size());
            assertEquals(3, p.entrances.size(), "a sealed gate on every side but the Treasury's");
            assertEquals(1, p.spawns(ZonePlan.ROLE_BOSS).size());
            assertEquals(1, p.spawns(ZonePlan.ROLE_ARRIVAL).size());
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            assertEquals(p.centreX(), boss.x());
            assertEquals(p.centreZ(), boss.z());
            assertEquals(p.floorY + MonarchVaultPlan.DAIS_TOP + 1, boss.y(), "the boss stands on top of the dais");
            Set<ZonePlan.Spot> all = new HashSet<>(p.minorRelics);
            all.add(p.centralRelic);
            assertEquals(5, all.size(), "five different pedestals");
            int[] t = MonarchVaultPlan.treasuryBounds(c[0], c[1]);
            for (ZonePlan.Spot s : all) {
                assertTrue(s.x() >= t[0] && s.x() <= t[3] && s.z() >= t[2] && s.z() <= t[5], "pedestal inside the Treasury: " + s);
                assertEquals(p.floorY + 2, s.y());
            }
            for (ZonePlan.Spot s : p.entrances) {
                assertTrue(p.footprint.contains(s.x(), s.z()));
                assertEquals(p.floorY + 1, s.y());
            }
            assertTrue(p.contains(p.gates.get(0).x(), p.gates.get(0).y(), p.gates.get(0).z(), 0));
        }
    }

    @Test
    void theTreasurySitsBehindItsGateAndNowhereElseLeadsIn() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            assertTrue(w.open(boss.x(), boss.y(), boss.z()));
            for (int k = 0; k < 4; k++) assertTrue(w.open(boss.x(), boss.y() + k, boss.z()), "headroom above the boss");
            assertFalse(w.open(boss.x(), boss.y() - 1, boss.z()), "something to stand on");

            ZonePlan.Spot gate = p.gates.get(0);
            BitSet shut = new BitSet();
            shut.set(w.index(gate.x(), gate.y(), gate.z()));
            shut.set(w.index(gate.x(), gate.y() + 1, gate.z()));
            assertTrue(w.open(gate.x(), gate.y(), gate.z()) && w.open(gate.x(), gate.y() + 1, gate.z()), "the doorway is left open for the gate");
            assertFalse(w.open(gate.x(), gate.y() + 2, gate.z()), "and it is two high");

            BitSet closed = w.flood(boss.x(), boss.y(), boss.z(), shut);
            BitSet opened = w.flood(boss.x(), boss.y(), boss.z(), new BitSet());
            ZonePlan.Spot central = p.centralRelic;
            assertTrue(w.open(central.x(), central.y(), central.z()), "the pedestal's own cell is free");
            assertFalse(w.open(central.x(), central.y() - 1, central.z()), "and has a plinth under it");
            assertFalse(closed.get(w.index(central.x(), central.y(), central.z())), "with the gate shut the central relic cannot be reached");
            assertTrue(opened.get(w.index(central.x(), central.y(), central.z())), "with the gate open it can be");
            for (ZonePlan.Spot m : p.minorRelics) {
                assertFalse(closed.get(w.index(m.x(), m.y(), m.z())), "shut: no minor relic can be reached " + m);
                assertTrue(opened.get(w.index(m.x(), m.y(), m.z())), "open: every minor relic can " + m);
                assertFalse(w.open(m.x(), m.y() - 1, m.z()));
            }
        }
    }

    @Test
    void theHallIsSealedAgainstTheCavesRoundIt() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            // a cave (or a flooded cave) on every side, above and below: start in the far corner of the volume
            BitSet outside = w.flood(w.x0, w.y0, w.z0, new BitSet());
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            assertFalse(outside.get(w.index(boss.x(), boss.y(), boss.z())), "the shell has a leak at cell " + c[0] + "," + c[1]);
            // every cell of the hall's air is behind the shell too
            int air = 0;
            for (int h = 1; h <= MonarchVaultPlan.HEIGHT; h++) {
                int r = MonarchVaultPlan.reach(h);
                for (int x = -r; x <= r; x += 3) {
                    for (int z = -r; z <= r; z += 3) {
                        if (x * x + z * z > r * r) continue;
                        if (w.open(p.centreX() + x, p.floorY + h, p.centreZ() + z)) {
                            air++;
                            assertFalse(outside.get(w.index(p.centreX() + x, p.floorY + h, p.centreZ() + z)), "leak at " + x + "," + h + "," + z);
                        }
                    }
                }
            }
            assertTrue(air > 5000, "the hall is hollow: " + air);
            // the floor under the whole hall is solid
            for (int x = -43; x <= 43; x += 5) for (int z = -43; z <= 43; z += 5) {
                if (x * x + z * z > 43 * 43) continue;
                assertFalse(w.open(p.centreX() + x, p.floorY, p.centreZ() + z), "floor at " + x + "," + z);
            }
        }
    }

    @Test
    void everyAisleAndEntranceIsReachableFromTheDais() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            ZonePlan.Spot gate = p.gates.get(0);
            BitSet shut = new BitSet();
            shut.set(w.index(gate.x(), gate.y(), gate.z()));
            shut.set(w.index(gate.x(), gate.y() + 1, gate.z()));
            BitSet seen = w.flood(boss.x(), boss.y(), boss.z(), shut);
            for (ZonePlan.Spot e : p.entrances) {
                assertTrue(seen.get(w.index(e.x(), e.y(), e.z())), "entrance " + e);
                assertTrue(seen.get(w.index(e.x(), e.y() + 1, e.z())), "with room to stand " + e);
            }
            ZonePlan.Spot a = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            assertTrue(seen.get(w.index(a.x(), a.y(), a.z())), "arrival " + a);
        }
    }

    // ---------------------------------------------------------------- the ways in
    @Test
    void theHallsAreTheSealedGatesOnTheOtherThreeSides() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            List<MonarchVaultPlan.Hall> halls = MonarchVaultPlan.halls(c[0], c[1]);
            assertEquals(3, halls.size());
            int ts = MonarchVaultPlan.treasurySide(c[0], c[1]);
            Set<Integer> sides = new HashSet<>();
            Raster w = new Raster(p);
            for (MonarchVaultPlan.Hall h : halls) {
                assertNotEquals(ts, h.side(), "no gate on the Treasury's side");
                assertTrue(sides.add(h.side()));
                assertEquals(1, Math.abs(h.dx()) + Math.abs(h.dz()), "a straight way, along an axis");
                assertEquals(MonarchVaultPlan.GATE_DOOR, Math.abs(h.doorX() - p.centreX()) + Math.abs(h.doorZ() - p.centreZ()));
                assertEquals(MonarchVaultPlan.GATE_FIRST, Math.abs(h.firstX() - p.centreX()) + Math.abs(h.firstZ() - p.centreZ()));
                assertEquals(h.doorX(), h.firstX() + h.dx() * (MonarchVaultPlan.GATE_DOOR - MonarchVaultPlan.GATE_FIRST));
                assertEquals(h.doorZ(), h.firstZ() + h.dz() * (MonarchVaultPlan.GATE_DOOR - MonarchVaultPlan.GATE_FIRST));
                assertTrue(p.footprint.contains(h.doorX(), h.doorZ()), "the door stands inside the footprint");
                assertFalse(p.footprint.contains(h.doorX() + h.dx() * 2, h.doorZ() + h.dz() * 2), "and the rock beyond it is outside");
                for (int y : new int[]{p.floorY + 1, p.floorY + 2}) {
                    for (int u = MonarchVaultPlan.GATE_FIRST; u <= MonarchVaultPlan.GATE_DOOR; u++) {
                        if (u == MonarchVaultPlan.GATE_FIRST + 1) continue;                      // the last cell of the hall's air, a pocket behind the sealed face
                        int x = p.centreX() + (h.firstX() - p.centreX()) * u / MonarchVaultPlan.GATE_FIRST, z = p.centreZ() + (h.firstZ() - p.centreZ()) * u / MonarchVaultPlan.GATE_FIRST;
                        assertFalse(w.open(x, y, z), "the passage is shut in the plan: " + x + "," + y + "," + z);
                    }
                    assertTrue(w.open(h.firstX() - h.dx(), y, h.firstZ() - h.dz()), "and the porch in front of it is open");
                    assertTrue(w.open(h.firstX() - 2 * h.dx(), y, h.firstZ() - 2 * h.dz()));
                }
            }
        }
    }

    // ---------------------------------------------------------------- frames
    @Test
    void theSideFramesAgreeWithTheOpCollector() {
        for (MonarchVaultPlan.Side s : MonarchVaultPlan.SIDES) {
            NexusOps o = new NexusOps(1000, -2000);
            o.frame(0, 0, s.swap(), s.su(), s.sv());
            int n = 0;
            for (int lx = -7; lx <= 7; lx += 7) {
                for (int lz = -5; lz <= 45; lz += 10) {
                    o.put("minecraft:stone", lx, 10, lz);
                    NexusOp op = o.list().get(n++);
                    assertEquals(1000 + s.wx(lx, lz), op.x1(), "side " + s.index());
                    assertEquals(-2000 + s.wz(lx, lz), op.z1(), "side " + s.index());
                }
            }
            assertEquals(0, s.wx(0, 1) * s.wx(0, 1) + s.wz(0, 1) * s.wz(0, 1) - 1, "one step outward is one step");
            assertEquals(s.opposite(), (s.index() + 2) % 4);
            MonarchVaultPlan.Side opp = MonarchVaultPlan.SIDES[s.opposite()];
            assertEquals(-s.wx(0, 5), opp.wx(0, 5));
            assertEquals(-s.wz(0, 5), opp.wz(0, 5));
        }
        // 0 north (-z), 1 east (+x), 2 south (+z), 3 west (-x)
        assertEquals(-1, MonarchVaultPlan.SIDES[0].wz(0, 1));
        assertEquals(1, MonarchVaultPlan.SIDES[1].wx(0, 1));
        assertEquals(1, MonarchVaultPlan.SIDES[2].wz(0, 1));
        assertEquals(-1, MonarchVaultPlan.SIDES[3].wx(0, 1));
    }

    @Test
    void theGridTestsCoverEverySideTheTreasuryCanBeOn() {
        Set<Integer> sides = new HashSet<>();
        for (int[] c : GRID_CELLS) sides.add(MonarchVaultPlan.treasurySide(c[0], c[1]));
        assertEquals(Set.of(0, 1, 2, 3), sides);
    }

    @Test
    void theTreasuryAndTheGrandEntranceFaceEachOther() {
        Set<Integer> sides = new HashSet<>();
        for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z += 7) sides.add(MonarchVaultPlan.treasurySide(x, z));
        assertEquals(Set.of(0, 1, 2, 3), sides, "any side can hold the Treasury");
        for (int[] c : CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            int ts = MonarchVaultPlan.treasurySide(c[0], c[1]);
            MonarchVaultPlan.Side t = MonarchVaultPlan.SIDES[ts];
            ZonePlan.Spot gate = p.gates.get(0);
            assertEquals(p.centreX() + t.wx(0, MonarchVaultPlan.TREASURY_NEAR), gate.x());
            assertEquals(p.centreZ() + t.wz(0, MonarchVaultPlan.TREASURY_NEAR), gate.z());
            ZonePlan.Spot arrival = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            assertEquals(p.centreX() - t.wx(0, 40), arrival.x(), "the arrival is at the grand entrance, across the hall from the Treasury");
            assertEquals(p.centreZ() - t.wz(0, 40), arrival.z());
        }
    }

    // ---------------------------------------------------------------- the dressing
    @Test
    void goldBonesAndArtifactsAreHeapedUp() {
        Set<String> kinds = new HashSet<>();
        for (int[] c : CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            int gold = 0, raw = 0, gilded = 0, bone = 0, mounds = 0;
            for (NexusOp o : p.ops) {
                kinds.add(o.block());
                long v = o.volume();
                switch (o.block()) {
                    case "minecraft:gold_block" -> gold += (int) Math.min(v, 100000);
                    case "minecraft:raw_gold_block" -> raw++;
                    case "minecraft:gilded_blackstone" -> gilded++;
                    case "minecraft:bone_block" -> bone++;
                    default -> { }
                }
            }
            assertTrue(gold > 800, "heaps of gold: " + gold);
            assertTrue(raw > 20 && gilded > 20 && bone > 20, raw + " " + gilded + " " + bone);
        }
        for (String k : new String[]{"minecraft:decorated_pot", "minecraft:enchanting_table", "minecraft:anvil", "minecraft:purple_banner", "minecraft:black_banner",
                "minecraft:lantern", "minecraft:soul_lantern", "minecraft:skeleton_skull", "minecraft:wither_skeleton_skull", "minecraft:candle",
                "minecraft:chain", "minecraft:obsidian", "minecraft:crying_obsidian"}) {
            assertTrue(kinds.contains(k), "missing " + k);
        }
    }

    @Test
    void theDetailsAreSetDownOnAirNeverOnTheStructures() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            int details = p.phaseStart(3);
            Raster w = new Raster(p, details);
            int clashes = 0;
            StringBuilder first = new StringBuilder();
            for (int i = details; i < p.ops.size(); i++) {
                NexusOp o = p.ops.get(i);
                if (o.kind() == NexusOp.Kind.CARVE || o.block().equals(NexusOp.AIR)) continue;
                for (int y = o.y1(); y <= o.y2(); y++) for (int z = o.z1(); z <= o.z2(); z++) for (int x = o.x1(); x <= o.x2(); x++) {
                    String before = w.at(x, y, z);
                    if (!before.equals("minecraft:air") && y > p.floorY && !before.equals(o.block())) {
                        clashes++;
                        if (first.length() == 0) first.append(before).append(" under ").append(o).append(" at ").append(x - p.centreX()).append(",").append(y - p.floorY).append(",").append(z - p.centreZ());
                    }
                }
            }
            assertEquals(0, clashes, "the details overwrite structures at cell " + c[0] + "," + c[1] + ": " + first);
        }
    }

    @Test
    void thePedestalsHaveRoomAbove() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = MonarchVaultPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            List<ZonePlan.Spot> spots = new java.util.ArrayList<>(p.minorRelics);
            spots.add(p.centralRelic);
            for (ZonePlan.Spot s : spots) for (int k = 0; k < 4; k++) assertTrue(w.open(s.x(), s.y() + k, s.z()), "room above the pedestal " + s + " +" + k);
            ZonePlan.Spot g = p.gates.get(0);
            assertFalse(w.open(g.x(), g.y() - 1, g.z()), "the doorway has a sill");
        }
    }

    @Test
    void relicChoiceIsStableAndInRange() {
        for (int[] c : CELLS) {
            int a = MonarchVaultPlan.centralRelicIndex(c[0], c[1], 5);
            assertTrue(a >= 0 && a < 5);
            assertEquals(a, MonarchVaultPlan.centralRelicIndex(c[0], c[1], 5));
        }
        assertEquals(0, MonarchVaultPlan.centralRelicIndex(1, 1, 0));
        Set<Integer> picks = new HashSet<>();
        for (int x = -10; x < 10; x++) for (int z = -10; z < 10; z++) picks.add(MonarchVaultPlan.centralRelicIndex(x, z, 5));
        assertEquals(5, picks.size(), "every relic can be the central one");
    }
}
