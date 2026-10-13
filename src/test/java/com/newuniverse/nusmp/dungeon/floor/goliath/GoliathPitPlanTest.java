package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.newuniverse.nusmp.dungeon.DungeonTier;
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

/** The Goliath's Pit as planned: its size, its sealing, its Treasury and gate, its ways in, what stands in it. */
class GoliathPitPlanTest {
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {-3, 5}, {7, -4}, {-12, -9}, {20, 31}, {-1, -1}, {3, 2}, {-5, 0}, {-5, -2}, {-6, -4}, {-6, 0}};
    /** The heavier tests, which build a block grid of the whole pit, look at these only: one for each side the Treasury can be on (north, east, south, west). */
    private static final int[][] GRID_CELLS = {{-5, 0}, {-5, -2}, {-6, -4}, {-6, 0}};

    // ---------------------------------------------------------------- a stand-in world to build the plan into
    /** The plan's ops applied to a block grid whose every cell starts as air (the worst case: a cave all round), to check the pit is sealed and shaped. */
    private static final class Raster {
        final int x0, y0, z0, sx, sy, sz;
        final int[] cells;
        final Map<String, Integer> ids = new HashMap<>();
        final List<String> names = new java.util.ArrayList<>();

        Raster(ZonePlan p) {
            x0 = p.footprint.minX() - 4;
            z0 = p.footprint.minZ() - 4;
            y0 = p.floorY - 8;
            sx = p.footprint.maxX() + 4 - x0 + 1;
            sz = p.footprint.maxZ() + 4 - z0 + 1;
            sy = p.topY + 4 - y0 + 1;
            cells = new int[sx * sy * sz];
            id("minecraft:air");
            for (NexusOp o : p.ops) apply(o);
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
            return n.equals("minecraft:air") || n.endsWith("_carpet") || n.endsWith("lantern") || n.equals("minecraft:chain");
        }

        /** Can feet stand in this cell: it and the one above are free of anything solid (a carpet and a chain do not stop a body), and the one below is solid. */
        boolean standable(int x, int y, int z) {
            return free(x, y, z) && free(x, y + 1, z) && solid(x, y - 1, z);
        }

        boolean free(int x, int y, int z) {
            String n = at(x, y, z);
            return n.equals("minecraft:air") || n.endsWith("_carpet") || n.equals("minecraft:chain");
        }

        boolean solid(int x, int y, int z) { return !open(x, y, z) && !at(x, y, z).equals("minecraft:stone_brick_wall"); }

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

        /**
         * The standing places a walker reaches from (x, y, z): one block up or down or level to a neighbouring column (four directions), or a drop of
         * up to three blocks, never through a closed cell. `shut` holds cells that count as closed (the gate).
         */
        Set<Long> walk(int x, int y, int z, BitSet shut) {
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[]{x, y, z});
            seen.add(key(x, y, z));
            int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] k : d) {
                    int nx = c[0] + k[0], nz = c[2] + k[1];
                    for (int dy = 1; dy >= -3; dy--) {
                        int ny = c[1] + dy;
                        if (!in(nx, ny, nz) || !standable(nx, ny, nz) || shut.get(index(nx, ny, nz)) || shut.get(index(nx, ny + 1, nz))) continue;
                        if (dy == 1 && !free(c[0], c[1] + 2, c[2])) continue;                           // a step up needs the head room over the starting cell too
                        if (seen.add(key(nx, ny, nz))) q.add(new int[]{nx, ny, nz});
                        break;
                    }
                }
            }
            return seen;
        }

        static long key(int x, int y, int z) { return ((long) (x + 5000) << 40) | ((long) (y + 5000) << 20) | (z + 5000); }
    }

    // ---------------------------------------------------------------- shape
    @Test
    void thePitProfileIsADomeWithinTheDimensionAndTheUpperTier() {
        assertEquals(GoliathPitPlan.R, GoliathPitPlan.reach(1), "the foot of the wall is at full radius");
        assertEquals(GoliathPitPlan.R, GoliathPitPlan.reach(21));
        assertTrue(GoliathPitPlan.reach(22) < GoliathPitPlan.R, "above that the dome closes in");
        assertTrue(GoliathPitPlan.reach(GoliathPitPlan.HEIGHT) > 0);
        assertEquals(-1, GoliathPitPlan.reach(GoliathPitPlan.HEIGHT + 1));
        assertEquals(-1, GoliathPitPlan.reach(0));
        for (int h = 2; h <= GoliathPitPlan.HEIGHT; h++) assertTrue(GoliathPitPlan.reach(h) <= GoliathPitPlan.reach(h - 1), "the ceiling only comes down toward the wall");
        assertEquals(GoliathPitPlan.HEIGHT, GoliathPitPlan.columnTop(0, 0));
        assertEquals(21, GoliathPitPlan.columnTop(28, 0), "at the wall the ceiling is 21 above the floor");
        assertEquals(0, GoliathPitPlan.columnTop(29, 0), "outside the pit there is no air");
        assertTrue(GoliathPitPlan.HEIGHT >= 28 && GoliathPitPlan.HEIGHT <= 32, "a ceiling 28 to 32 blocks over the floor");
        assertTrue(2 * GoliathPitPlan.R >= 54 && 2 * GoliathPitPlan.OUTER <= 66, "about sixty blocks across");
        for (int x = -28; x <= 28; x += 3) for (int z = -28; z <= 28; z += 3) {
            if (x * x + z * z > 28 * 28) continue;
            int top = GoliathPitPlan.columnTop(x, z);
            assertTrue(top >= 21 && top <= GoliathPitPlan.HEIGHT, x + "," + z + " -> " + top);
        }
    }

    @Test
    void eachCellGetsOneDeterministicPlanInsideItsCellAndTheUpperTier() {
        Set<String> seen = new HashSet<>();
        for (int[] c : CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            ZonePlan again = GoliathPitPlan.of(c[0], c[1]);
            assertEquals(p.ops, again.ops, "the same cell always gives the same pit");
            assertEquals(p.centralRelic, again.centralRelic);
            assertSame(NexusZone.GOLIATH_PIT, p.zone);
            assertTrue(p.insideCell(), "footprint and edge stay in the 128-block cell: " + c[0] + "," + c[1]);
            assertFalse(p.opsStrayFromFootprint(), "no op reaches a chunk the footprint does not cover");
            assertEquals(Footprint.Shape.OVAL, p.footprint.shape());
            assertEquals(GoliathPitPlan.OUTER, p.footprint.halfX());
            assertEquals(p.footprint.halfX(), p.footprint.halfZ());
            assertTrue(p.footprint.halfX() >= 28 && p.footprint.halfX() <= 32, "footprint radius 28 to 32");
            assertEquals(GoliathPitPlan.centreX(c[0], c[1]), p.centreX());
            assertEquals(GoliathPitPlan.centreZ(c[0], c[1]), p.centreZ());
            assertTrue(Math.abs(p.centreX() - NexusZone.centre(c[0])) <= GoliathPitPlan.JITTER);
            assertTrue(Math.abs(p.centreZ() - NexusZone.centre(c[1])) <= GoliathPitPlan.JITTER);
            assertEquals(GoliathPitPlan.floorY(c[0], c[1]), p.floorY);
            assertTrue(p.floorY >= 40 && p.floorY <= 42, "floor " + p.floorY);
            assertTrue(p.floorY >= DungeonTier.UPPER_Y + GoliathPitPlan.FOUNDATION, "the floor lies in the Upper Catacombs (y of 24 and up)");
            assertEquals(p.floorY + GoliathPitPlan.HEIGHT + GoliathPitPlan.ROOF, p.topY);
            assertTrue(p.topY < 100, "the roof stays under y 100: " + p.topY);
            assertTrue(p.topY < 127 && p.floorY - GoliathPitPlan.FOUNDATION >= -64, "inside the dimension -64..127");
            assertEquals(4, p.phaseCount(), "carve, floor and shell, structures, details");
            assertTrue(p.ops.size() > 2000 && p.ops.size() < 60000, "ops: " + p.ops.size());
            int cell0x = c[0] * NexusZone.CELL, cell0z = c[1] * NexusZone.CELL;
            for (NexusOp o : p.ops) {
                assertTrue(o.y1() >= -64 && o.y2() <= 127, "op outside the dimension: " + o);
                assertTrue(o.y1() >= p.floorY - GoliathPitPlan.FOUNDATION && o.y2() <= p.topY, "op outside the pit's height: " + o);
                assertTrue(o.x1() >= cell0x && o.x2() < cell0x + NexusZone.CELL && o.z1() >= cell0z && o.z2() < cell0z + NexusZone.CELL, "op outside its cell: " + o);
                assertTrue(o.x1() >= p.footprint.minX() && o.x2() <= p.footprint.maxX() && o.z1() >= p.footprint.minZ() && o.z2() <= p.footprint.maxZ(), "op outside the footprint: " + o);
                assertTrue(o.block().matches("[a-z0-9_]+:[a-z0-9_]+"), o.block());
            }
            seen.add(p.centreX() + "," + p.centreZ() + "," + p.floorY);
        }
        assertTrue(seen.size() > 4, "different cells give different pits");
    }

    @Test
    void everyArenaCellOfTheMapHasAPitThatFits() {
        int checked = 0;
        for (int cx = -40; cx < 40; cx++) {
            for (int cz = -40; cz < 40; cz++) {
                if (DungeonTier.arenaOf(cx, cz) != DungeonTier.ARENA_GOLIATH) continue;
                ZonePlan p = GoliathPitPlan.of(cx, cz);
                assertTrue(p.insideCell(), cx + "," + cz);
                assertFalse(p.opsStrayFromFootprint(), cx + "," + cz);
                assertTrue(p.floorY >= 40 && p.floorY <= 42);
                checked++;
            }
        }
        assertTrue(checked > 100, "pits were looked at: " + checked);
    }

    @Test
    void nothingHereCanBeLooted() {
        String[] holders = {"chest", "barrel", "shulker", "hopper", "dropper", "dispenser", "furnace", "smoker", "lectern", "jukebox", "crafter", "brewing", "bookshelf", "spawner"};
        for (int[] c : CELLS) {
            for (NexusOp o : GoliathPitPlan.of(c[0], c[1]).ops) {
                for (String h : holders) assertFalse(o.block().contains(h), "a block that holds items: " + o.block());
            }
        }
    }

    @Test
    void noBlockOfThePlanFallsOrFlows() {
        String[] loose = {"sand", "gravel", "concrete_powder", "water", "lava", "anvil", "dragon_egg", "tnt", "fire", "torch", "redstone", "piston"};
        for (int[] c : CELLS) {
            for (NexusOp o : GoliathPitPlan.of(c[0], c[1]).ops) {
                for (String h : loose) assertFalse(o.block().contains(h), "a block that would fall, flow or react: " + o.block());
            }
        }
    }

    // ---------------------------------------------------------------- what the plan marks
    @Test
    void thePlanMarksItsRelicsGateEntrancesAndBoss() {
        for (int[] c : CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            assertNotNull(p.centralRelic, "exactly one central pedestal");
            assertEquals(4, p.minorRelics.size());
            assertEquals(1, p.gates.size());
            assertEquals(3, p.entrances.size(), "a sealed gate on every side but the Treasury's");
            assertEquals(1, p.spawns(ZonePlan.ROLE_BOSS).size());
            assertEquals(1, p.spawns(ZonePlan.ROLE_ARRIVAL).size());
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            assertEquals(p.centreX(), boss.x());
            assertEquals(p.centreZ(), boss.z());
            assertEquals(p.floorY + 1, boss.y(), "the boss stands on the floor, on the seal");
            Set<ZonePlan.Spot> all = new HashSet<>(p.minorRelics);
            all.add(p.centralRelic);
            assertEquals(5, all.size(), "five different pedestals");
            int[] t = GoliathPitPlan.treasuryBounds(c[0], c[1]);
            for (ZonePlan.Spot s : all) {
                assertTrue(s.x() >= t[0] && s.x() <= t[3] && s.z() >= t[2] && s.z() <= t[5], "pedestal inside the Treasury: " + s);
                assertEquals(p.floorY + 2, s.y());
            }
            for (ZonePlan.Spot s : p.entrances) {
                assertTrue(p.footprint.contains(s.x(), s.z()));
                assertEquals(p.floorY + 1, s.y());
            }
            assertTrue(p.contains(p.gates.get(0).x(), p.gates.get(0).y(), p.gates.get(0).z(), 0));
            int[] seal = GoliathPitPlan.sealCentre(c[0], c[1]);
            assertArrayEquals(new int[]{p.centreX(), p.floorY, p.centreZ()}, seal);
        }
    }

    @Test
    void theSealIsNineBlocksOfBedrockWithObsidianCracks() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            int bedrock = 0;
            for (int dz = -6; dz <= 6; dz++) {
                for (int dx = -6; dx <= 6; dx++) {
                    boolean disc = dx * dx + dz * dz <= GoliathPitPlan.DISC * GoliathPitPlan.DISC;
                    String top = w.at(p.centreX() + dx, p.floorY, p.centreZ() + dz), under = w.at(p.centreX() + dx, p.floorY - 1, p.centreZ() + dz);
                    if (disc) {
                        assertEquals("minecraft:bedrock", top, "the seal at " + dx + "," + dz);
                        assertEquals("minecraft:bedrock", under);
                        bedrock++;
                    } else {
                        assertNotEquals("minecraft:bedrock", top, "bedrock only inside the seal: " + dx + "," + dz);
                    }
                }
            }
            assertEquals(49, bedrock, "a disc nine blocks across");
            for (int dx = -4; dx <= 4; dx++) assertTrue(w.at(p.centreX() + dx, p.floorY, p.centreZ()).equals("minecraft:bedrock"), "nine blocks across the middle");
            int obsidian = 0, crying = 0;
            for (NexusOp o : p.ops) {
                if (o.y1() != p.floorY || o.y2() != p.floorY) continue;
                if (o.block().equals("minecraft:obsidian")) obsidian += o.volume();
                if (o.block().equals("minecraft:crying_obsidian")) crying += o.volume();
            }
            assertTrue(obsidian > 20 && crying > 5, "cracks of obsidian and crying obsidian: " + obsidian + " and " + crying);
            for (int[] k : GoliathPitPlan.cracks(0x1234L)) assertTrue(k[0] * k[0] + k[1] * k[1] <= 26 * 26, "a crack stays well inside the floor");
            for (int k = 1; k <= 3; k++) {
                for (int dy = 0; dy <= 3; dy++) assertTrue(w.open(p.centreX(), p.floorY + 1 + dy, p.centreZ()), "headroom over the seal for the giant");
            }
        }
    }

    @Test
    void theTreasurySitsBehindItsGateAndNowhereElseLeadsIn() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            assertTrue(w.open(boss.x(), boss.y(), boss.z()));
            for (int k = 0; k < 8; k++) assertTrue(w.open(boss.x(), boss.y() + k, boss.z()), "headroom above the boss");
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
    void thePitIsSealedAgainstTheCavesRoundIt() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            // a cave (or a flooded cave) on every side, above and below: start in the far corner of the volume
            BitSet outside = w.flood(w.x0, w.y0, w.z0, new BitSet());
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            assertFalse(outside.get(w.index(boss.x(), boss.y(), boss.z())), "the shell has a leak at cell " + c[0] + "," + c[1]);
            int air = 0;
            for (int h = 1; h <= GoliathPitPlan.HEIGHT; h++) {
                int r = GoliathPitPlan.reach(h);
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
            assertTrue(air > 4000, "the pit is hollow: " + air);
            // the floor under the whole pit is solid, and so is the foundation under it
            for (int x = -27; x <= 27; x += 3) for (int z = -27; z <= 27; z += 3) {
                if (x * x + z * z > 27 * 27) continue;
                assertFalse(w.open(p.centreX() + x, p.floorY, p.centreZ() + z), "floor at " + x + "," + z);
                for (int k = 1; k <= GoliathPitPlan.FOUNDATION; k++) assertFalse(w.open(p.centreX() + x, p.floorY - k, p.centreZ() + z), "foundation at " + x + "," + z);
            }
        }
    }

    @Test
    void everyEntranceAndTheArrivalAreReachableAndTheFloorIsOneFight() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            ZonePlan.Spot gate = p.gates.get(0);
            BitSet shut = new BitSet();
            shut.set(w.index(gate.x(), gate.y(), gate.z()));
            shut.set(w.index(gate.x(), gate.y() + 1, gate.z()));
            Set<Long> walked = w.walk(boss.x(), boss.y(), boss.z(), shut);
            for (ZonePlan.Spot e : p.entrances) {
                assertTrue(w.standable(e.x(), e.y(), e.z()), "an entrance can be stood on " + e);
                assertTrue(walked.contains(Raster.key(e.x(), e.y(), e.z())), "entrance " + e + " is a walk from the seal");
            }
            ZonePlan.Spot a = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            assertTrue(w.standable(a.x(), a.y(), a.z()), "the arrival can be stood on " + a);
            assertTrue(walked.contains(Raster.key(a.x(), a.y(), a.z())), "arrival " + a);
            int[] t = GoliathPitPlan.treasuryBounds(c[0], c[1]);
            ZonePlan.Spot central = p.centralRelic;
            assertFalse(walked.contains(Raster.key(central.x(), central.y(), central.z())), "the Treasury is not part of the walk while its gate is shut");
            Set<Long> through = w.walk(boss.x(), boss.y(), boss.z(), new BitSet());
            boolean inside = false;
            for (int x = t[0]; x <= t[3]; x++) for (int z = t[2]; z <= t[5]; z++) if (through.contains(Raster.key(x, t[1], z))) inside = true;
            assertTrue(inside, "with the gate open one can walk into the Treasury");
        }
    }

    @Test
    void theRampsLeadFromTheFloorUpToTheFirstDeck() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            ZonePlan.Spot boss = p.spawns(ZonePlan.ROLE_BOSS).get(0);
            Set<Long> walked = w.walk(boss.x(), boss.y(), boss.z(), new BitSet());
            int deckStanding = p.floorY + GoliathPitPlan.TIER1 + 1;
            int reached = 0;
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    boolean up = false;
                    for (int d = 24; d <= 26 && !up; d++) {
                        int x = p.centreX() + sx * (int) Math.round(d / Math.sqrt(2)), z = p.centreZ() + sz * (int) Math.round(d / Math.sqrt(2));
                        if (walked.contains(Raster.key(x, deckStanding, z))) up = true;
                    }
                    if (up) reached++;
                }
            }
            assertEquals(4, reached, "all four ramps climb to the first deck, a block at a time, at cell " + c[0] + "," + c[1]);
            int onDeck = 0;
            for (long k : walked) if (((k >> 20) & 0xFFFFF) - 5000 == deckStanding) onDeck++;
            assertTrue(onDeck > 100, "a good stretch of the first deck can be walked: " + onDeck);
        }
    }

    // ---------------------------------------------------------------- the ways in
    @Test
    void theHallsAreTheSealedGatesOnTheOtherThreeSides() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            List<GoliathPitPlan.Hall> halls = GoliathPitPlan.halls(c[0], c[1]);
            assertEquals(3, halls.size());
            int ts = GoliathPitPlan.treasurySide(c[0], c[1]);
            Set<Integer> sides = new HashSet<>();
            Raster w = new Raster(p);
            for (GoliathPitPlan.Hall h : halls) {
                assertNotEquals(ts, h.side(), "no gate on the Treasury's side");
                assertTrue(sides.add(h.side()));
                assertEquals(1, Math.abs(h.dx()) + Math.abs(h.dz()), "a straight way, along an axis");
                assertEquals(GoliathPitPlan.GATE_DOOR, Math.abs(h.doorX() - p.centreX()) + Math.abs(h.doorZ() - p.centreZ()));
                assertEquals(GoliathPitPlan.GATE_FIRST, Math.abs(h.firstX() - p.centreX()) + Math.abs(h.firstZ() - p.centreZ()));
                assertEquals(h.doorX(), h.firstX() + h.dx() * (GoliathPitPlan.GATE_DOOR - GoliathPitPlan.GATE_FIRST));
                assertEquals(h.doorZ(), h.firstZ() + h.dz() * (GoliathPitPlan.GATE_DOOR - GoliathPitPlan.GATE_FIRST));
                assertTrue(p.footprint.contains(h.doorX(), h.doorZ()), "the door stands inside the footprint");
                assertFalse(p.footprint.contains(h.doorX() + h.dx() * 2, h.doorZ() + h.dz() * 2), "and the rock beyond it is outside");
                for (int y : new int[]{p.floorY + 1, p.floorY + 2}) {
                    for (int u = GoliathPitPlan.GATE_FIRST; u <= GoliathPitPlan.GATE_DOOR; u++) {
                        int x = p.centreX() + (h.firstX() - p.centreX()) * u / GoliathPitPlan.GATE_FIRST, z = p.centreZ() + (h.firstZ() - p.centreZ()) * u / GoliathPitPlan.GATE_FIRST;
                        assertFalse(w.open(x, y, z), "the passage is shut in the plan: " + x + "," + y + "," + z);
                    }
                    assertTrue(w.open(h.firstX() - h.dx(), y, h.firstZ() - h.dz()), "and the porch in front of it is open");
                    assertTrue(w.open(h.firstX() - 2 * h.dx(), y, h.firstZ() - 2 * h.dz()));
                }
                ZonePlan.Spot entrance = null;
                for (ZonePlan.Spot e : p.entrances) if (e.x() == h.firstX() - h.dx() && e.z() == h.firstZ() - h.dz()) entrance = e;
                assertNotNull(entrance, "the plan's entrance is the cell in front of the sealed face");
            }
        }
    }

    // ---------------------------------------------------------------- frames
    @Test
    void theSideFramesAgreeWithTheOpCollectorAndTheirOwnInverse() {
        for (GoliathPitPlan.Side s : GoliathPitPlan.SIDES) {
            NexusOps o = new NexusOps(1000, -2000);
            o.frame(0, 0, s.swap(), s.su(), s.sv());
            int n = 0;
            for (int lx = -7; lx <= 7; lx += 7) {
                for (int lz = -5; lz <= 45; lz += 10) {
                    o.put("minecraft:stone", lx, 10, lz);
                    NexusOp op = o.list().get(n++);
                    assertEquals(1000 + s.wx(lx, lz), op.x1(), "side " + s.index());
                    assertEquals(-2000 + s.wz(lx, lz), op.z1(), "side " + s.index());
                    assertEquals(lx, s.lx(s.wx(lx, lz), s.wz(lx, lz)), "the inverse gives the local x back on side " + s.index());
                    assertEquals(lz, s.lz(s.wx(lx, lz), s.wz(lx, lz)), "and the local z on side " + s.index());
                }
            }
            assertEquals(0, s.wx(0, 1) * s.wx(0, 1) + s.wz(0, 1) * s.wz(0, 1) - 1, "one step outward is one step");
            assertEquals(s.opposite(), (s.index() + 2) % 4);
            GoliathPitPlan.Side opp = GoliathPitPlan.SIDES[s.opposite()];
            assertEquals(-s.wx(0, 5), opp.wx(0, 5));
            assertEquals(-s.wz(0, 5), opp.wz(0, 5));
        }
        // 0 north (-z), 1 east (+x), 2 south (+z), 3 west (-x)
        assertEquals(-1, GoliathPitPlan.SIDES[0].wz(0, 1));
        assertEquals(1, GoliathPitPlan.SIDES[1].wx(0, 1));
        assertEquals(1, GoliathPitPlan.SIDES[2].wz(0, 1));
        assertEquals(-1, GoliathPitPlan.SIDES[3].wx(0, 1));
    }

    @Test
    void theGridTestsCoverEverySideTheTreasuryCanBeOn() {
        Set<Integer> sides = new HashSet<>();
        for (int[] c : GRID_CELLS) sides.add(GoliathPitPlan.treasurySide(c[0], c[1]));
        assertEquals(Set.of(0, 1, 2, 3), sides);
    }

    @Test
    void theTreasuryAndTheGrandEntranceFaceEachOther() {
        Set<Integer> sides = new HashSet<>();
        for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z += 7) sides.add(GoliathPitPlan.treasurySide(x, z));
        assertEquals(Set.of(0, 1, 2, 3), sides, "any side can hold the Treasury");
        for (int[] c : CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            int ts = GoliathPitPlan.treasurySide(c[0], c[1]);
            GoliathPitPlan.Side t = GoliathPitPlan.SIDES[ts];
            ZonePlan.Spot gate = p.gates.get(0);
            assertEquals(p.centreX() + t.wx(0, GoliathPitPlan.TREASURY_NEAR), gate.x());
            assertEquals(p.centreZ() + t.wz(0, GoliathPitPlan.TREASURY_NEAR), gate.z());
            ZonePlan.Spot arrival = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            assertEquals(p.centreX() - t.wx(0, GoliathPitPlan.GATE_FIRST - 4), arrival.x(), "the arrival is at the grand entrance, across the pit from the Treasury");
            assertEquals(p.centreZ() - t.wz(0, GoliathPitPlan.GATE_FIRST - 4), arrival.z());
        }
    }

    // ---------------------------------------------------------------- the dressing
    @Test
    void thePillarsStandApartFromTheTreasuryThePorchesAndTheRamps() {
        for (int[] c : CELLS) {
            List<GoliathPitPlan.Pillar> ps = GoliathPitPlan.pillars(c[0], c[1]);
            assertTrue(ps.size() >= 12 && ps.size() <= 16, "twelve to sixteen pillars: " + ps.size());
            GoliathPitPlan.Side t = GoliathPitPlan.SIDES[GoliathPitPlan.treasurySide(c[0], c[1])];
            Set<Integer> heights = new HashSet<>();
            for (GoliathPitPlan.Pillar p : ps) {
                assertFalse(GoliathPitPlan.inTreasurySector(t, p.x(), p.z()), "no pillar in front of the Treasury: " + p);
                assertFalse(GoliathPitPlan.inPorchSector(p.x(), p.z()), "no pillar at a porch: " + p);
                assertFalse(GoliathPitPlan.nearRamp(p.x(), p.z(), 3), "no pillar on a ramp: " + p);
                assertTrue(p.top() >= 4 && p.top() <= 18, "uneven heights, all broken short of the roof: " + p);
                assertTrue(Math.sqrt(p.x() * p.x() + p.z() * p.z()) <= 23, "within the colonnade");
                assertTrue(!p.whole() || p.top() >= 11, "only a tall pillar keeps its capital");
                heights.add(p.top());
            }
            assertTrue(heights.size() >= 5, "the pillars are of uneven height: " + heights);
            Set<String> at = new HashSet<>();
            for (GoliathPitPlan.Pillar p : ps) assertTrue(at.add(p.x() + "," + p.z()), "no two pillars on one spot");
            for (int i = 0; i < ps.size(); i++) for (int j = i + 1; j < ps.size(); j++) {
                GoliathPitPlan.Pillar a = ps.get(i), b = ps.get(j);
                assertTrue(Math.abs(a.x() - b.x()) > 4 || Math.abs(a.z() - b.z()) > 4, "the plinths (five wide) do not overlap: " + a + " " + b);
            }
        }
    }

    @Test
    void everyLanternHasSomethingToStandOnOrHangFrom() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            int standing = 0, hanging = 0;
            for (int y = p.floorY; y <= p.topY; y++) {
                for (int x = p.footprint.minX(); x <= p.footprint.maxX(); x++) {
                    for (int z = p.footprint.minZ(); z <= p.footprint.maxZ(); z++) {
                        String n = w.at(x, y, z);
                        if (!n.endsWith("lantern")) continue;
                        // the plan writes them standing (hanging=false) or hanging; which one is told by the op's property string
                        boolean hangs = false;
                        for (NexusOp o : p.ops) if (o.x1() == x && o.y1() == y && o.z1() == z && o.x2() == x && o.y2() == y && o.z2() == z && o.block().endsWith("lantern")) hangs = o.props().contains("hanging=true");
                        if (hangs) {
                            hanging++;
                            String above = w.at(x, y + 1, z);
                            assertTrue(above.equals("minecraft:chain") || !w.open(x, y + 1, z), "a hanging lantern hangs from a chain or a ceiling at " + x + "," + y + "," + z + ": " + above);
                        } else {
                            standing++;
                            assertTrue(w.solid(x, y - 1, z) || w.at(x, y - 1, z).equals("minecraft:stone_brick_wall"), "a standing lantern stands on something at " + x + "," + y + "," + z + ": " + w.at(x, y - 1, z));
                        }
                    }
                }
            }
            assertTrue(standing > 20 && hanging > 15, "light in the pit: " + standing + " standing, " + hanging + " hanging");
        }
    }

    @Test
    void everyChainHangsFromTheDomeAndEndsInALantern() {
        for (int[] c : GRID_CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            Raster w = new Raster(p);
            for (int x = p.footprint.minX(); x <= p.footprint.maxX(); x++) {
                for (int z = p.footprint.minZ(); z <= p.footprint.maxZ(); z++) {
                    for (int y = p.floorY + 1; y < p.topY; y++) {
                        if (!w.at(x, y, z).equals("minecraft:chain") || w.at(x, y - 1, z).equals("minecraft:chain")) continue;       // the lowest link of a chain
                        assertTrue(w.at(x, y - 1, z).endsWith("lantern"), "a chain ends in a lantern at " + x + "," + y + "," + z + ": " + w.at(x, y - 1, z));
                        int top = y;
                        while (w.at(x, top + 1, z).equals("minecraft:chain")) top++;
                        assertFalse(w.open(x, top + 1, z), "and hangs from the dome at " + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void thePitIsPaleStoneBrickWithGoldAndQuartzAccents() {
        Set<String> kinds = new HashSet<>();
        for (int[] c : CELLS) {
            ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            long bricks = 0, other = 0;
            for (NexusOp o : p.ops) {
                kinds.add(o.block());
                long v = Math.min(o.volume(), 100000);
                if (o.kind() == NexusOp.Kind.CARVE) continue;
                if (o.block().contains("stone_brick") || o.block().contains("cobblestone")) bricks += v;
                else other += v;
            }
            assertTrue(bricks > 4 * other, "mostly stone brick: " + bricks + " against " + other);
        }
        for (String k : new String[]{"minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:chiseled_stone_bricks", "minecraft:polished_diorite",
                "minecraft:polished_andesite", "minecraft:calcite", "minecraft:smooth_stone", "minecraft:bedrock", "minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:gold_block",
                "minecraft:quartz_bricks", "minecraft:lantern", "minecraft:soul_lantern", "minecraft:chain", "minecraft:moss_block", "minecraft:stone_brick_wall"}) {
            assertTrue(kinds.contains(k), "missing " + k);
        }
    }

    @Test
    void theRelicIndexIsAStableHash() {
        Set<Integer> seen = new HashSet<>();
        for (int x = -30; x <= 30; x++) {
            int a = GoliathPitPlan.centralRelicIndex(x, 4, 5);
            assertEquals(a, GoliathPitPlan.centralRelicIndex(x, 4, 5));
            assertTrue(a >= 0 && a < 5);
            seen.add(a);
        }
        assertEquals(5, seen.size(), "every relic can be the central one somewhere");
        assertEquals(0, GoliathPitPlan.centralRelicIndex(1, 1, 0), "no relics, no index");
    }

    @Test
    void theRngIsDeterministicAndEven() {
        PitRng a = new PitRng(42), b = new PitRng(42);
        for (int i = 0; i < 100; i++) assertEquals(a.next(), b.next());
        int[] n = new int[10];
        PitRng r = new PitRng(7);
        for (int i = 0; i < 20000; i++) n[r.below(10)]++;
        for (int k : n) assertTrue(k > 1700 && k < 2300, "even: " + k);
        for (int i = 0; i < 1000; i++) {
            int v = r.range(3, 6);
            assertTrue(v >= 3 && v <= 6);
            double u = r.unit();
            assertTrue(u >= 0 && u < 1);
        }
        assertEquals(PitRng.hash(1, 2, 3, 4), PitRng.hash(1, 2, 3, 4));
        assertNotEquals(PitRng.hash(1, 2, 3, 4), PitRng.hash(1, 2, 3, 5));
        assertTrue(PitRng.hash(-9, -9, -9, -9) >= 0);
    }

    @Test
    void theGroundIsOneTheTreasuryCollapseCanWorkOn() {
        for (int[] c : new int[][]{{-5, 0}, {3, -7}, {40, 12}}) {
            com.newuniverse.nusmp.dungeon.nexus.ZonePlan p = GoliathPitPlan.of(c[0], c[1]);
            assertTrue(p.footprint.halfX() <= 100 && p.footprint.halfZ() <= 100, "the collapse's largest half-width");
            assertTrue(p.topY > p.floorY && p.topY - p.floorY <= 330, "the collapse's largest height");
            assertTrue(p.insideCell());
        }
    }
}
