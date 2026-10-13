package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Charybdis Basin as planned: its size and depth, its sealing, the flood, the dry Treasury and airlocks, the room the leviathan swims in. */
class CharybdisBasinPlanTest {
    private static final int[][] CELLS = {{0, 0}, {1, 0}, {-3, 5}, {7, -4}, {-12, -9}, {20, 31}, {-1, -1}, {3, 2}, {-40, 17}, {99, -99}};
    private static final int F = CharybdisBasinPlan.FLOOR_Y;

    // ---------------------------------------------------------------- a stand-in world to build the plan into
    /** The plan's ops applied to a block grid whose every cell starts as air (the worst case: a cave all round), to check the basin is sealed and shaped. */
    private static final class Raster {
        final int x0, y0, z0, sx, sy, sz;
        final int[] cells;
        final Map<String, Integer> ids = new HashMap<>();
        final List<String> names = new ArrayList<>();

        Raster(ZonePlan p) {
            x0 = p.footprint.minX() - 4;
            z0 = p.footprint.minZ() - 4;
            y0 = p.floorY - CharybdisBasinPlan.FOUNDATION - 3;
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

        boolean air(int x, int y, int z) { return at(x, y, z).equals("minecraft:air"); }

        boolean water(int x, int y, int z) { return at(x, y, z).equals("minecraft:water"); }

        /** A cell with water in it that would spread: water itself and everything that holds some (plants and waterlogged blocks). */
        boolean wet(int x, int y, int z) {
            String n = at(x, y, z);
            return n.equals("minecraft:water") || n.equals("minecraft:kelp") || n.equals("minecraft:kelp_plant") || n.equals("minecraft:seagrass") || n.equals("minecraft:sea_pickle")
                    || n.endsWith("_coral_fan") || n.endsWith("_coral");
        }

        /** Things that stand in a cell without being solid (a swimmer passes through, a fish too). */
        boolean soft(int x, int y, int z) { return air(x, y, z) || wet(x, y, z); }
    }

    private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private static int treasurySideOf(int cx, int cz) { return CharybdisBasinPlan.treasurySide(cx, cz); }

    /** One cell for each side the Treasury can be on. */
    private static int[][] cellsPerSide() {
        int[][] out = new int[4][];
        for (int cx = -40; cx < 40 && (out[0] == null || out[1] == null || out[2] == null || out[3] == null); cx++) {
            for (int cz = -2; cz < 3; cz++) {
                int s = treasurySideOf(cx, cz);
                if (out[s] == null) out[s] = new int[]{cx, cz};
            }
        }
        for (int s = 0; s < 4; s++) assertNotNull(out[s], "some cell has the Treasury on side " + s);
        return out;
    }

    // ---------------------------------------------------------------- the size and the place
    @Test
    void theBasinFitsItsCellAndTheDeepWaterways() {
        for (int[] c : CELLS) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            assertEquals(NexusZone.CHARYBDIS_BASIN, p.zone);
            assertTrue(p.insideCell(), "inside its 128-block cell: " + c[0] + "," + c[1]);
            assertFalse(p.opsStrayFromFootprint(), "no op leaves the chunks of the footprint");
            assertEquals(F, p.floorY);
            assertEquals(F + CharybdisBasinPlan.HEIGHT + CharybdisBasinPlan.ROOF, p.topY);
            assertTrue(p.topY < DungeonTier.UPPER_Y, "the roof is under the Upper Catacombs: " + p.topY);
            assertTrue(p.floorY - CharybdisBasinPlan.FOUNDATION > DungeonTier.CITY_Y, "the foundation is above the Variable Core: " + (p.floorY - CharybdisBasinPlan.FOUNDATION));
            assertEquals(Footprint.Shape.OVAL, p.footprint.shape());
            assertEquals(CharybdisBasinPlan.OUTER, p.footprint.halfX());
            assertEquals(CharybdisBasinPlan.OUTER, p.footprint.halfZ());
            assertTrue(CharybdisBasinPlan.OUTER >= 28 && CharybdisBasinPlan.OUTER <= 32, "a footprint radius of 28 to 32");
            assertEquals(CharybdisBasinPlan.centreX(c[0], c[1]), p.centreX());
            assertEquals(CharybdisBasinPlan.centreZ(c[0], c[1]), p.centreZ());
            assertTrue(Math.abs(p.centreX() - NexusZone.centre(c[0])) <= CharybdisBasinPlan.JITTER && Math.abs(p.centreZ() - NexusZone.centre(c[1])) <= CharybdisBasinPlan.JITTER);
        }
    }

    @Test
    void thePlanIsDeterministicAndCellsDiffer() {
        ZonePlan a = CharybdisBasinPlan.of(5, 6), b = CharybdisBasinPlan.of(5, 6), c = CharybdisBasinPlan.of(6, 6);
        assertEquals(a.ops, b.ops);
        assertEquals(a.centralRelic, b.centralRelic);
        assertNotEquals(a.centreX(), c.centreX(), "different cells, different places");
        Set<Integer> sides = new HashSet<>();
        Set<Integer> centres = new HashSet<>();
        for (int cx = -30; cx < 30; cx++) {
            sides.add(treasurySideOf(cx, 3));
            centres.add(CharybdisBasinPlan.centreX(cx, 3) - NexusZone.centre(cx));
        }
        assertEquals(4, sides.size(), "the Treasury lands on every side");
        assertTrue(centres.size() >= 8, "the centre moves about in its cell");
    }

    @Test
    void everyOpStaysInsideTheFootprintAndItsHeights() {
        for (int[] c : new int[][]{{0, 0}, {-3, 5}}) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            assertTrue(p.ops.size() > 200 && p.ops.size() < 40000, "a plan of a sane size: " + p.ops.size());
            for (NexusOp o : p.ops) {
                assertTrue(o.y1() >= p.floorY - CharybdisBasinPlan.FOUNDATION && o.y2() <= p.topY, "inside the heights: " + o);
                for (int[] corner : new int[][]{{o.x1(), o.z1()}, {o.x2(), o.z1()}, {o.x1(), o.z2()}, {o.x2(), o.z2()}})
                    assertTrue(p.footprint.contains(corner[0], corner[1]), "inside the footprint: " + o);
            }
        }
    }

    // ---------------------------------------------------------------- the water stays in, and nothing wet touches air
    @Test
    void noAirCellEverTouchesWater() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            int wetCells = 0;
            for (int y = r.y0 + 1; y < r.y0 + r.sy - 1; y++) {
                for (int z = r.z0 + 1; z < r.z0 + r.sz - 1; z++) {
                    for (int x = r.x0 + 1; x < r.x0 + r.sx - 1; x++) {
                        if (!r.wet(x, y, z)) continue;
                        wetCells++;
                        for (int[] d : DIRS)
                            assertFalse(r.air(x + d[0], y + d[1], z + d[2]), "water (or a plant holding water) next to air at " + x + "," + y + "," + z + " (" + r.at(x, y, z) + ") towards " + d[0] + "," + d[1] + "," + d[2]
                                    + " cell " + c[0] + "," + c[1]);
                    }
                }
            }
            assertTrue(wetCells > 40000, "a flooded basin: " + wetCells);
        }
    }

    @Test
    void everyAirCellOfTheInteriorBelongsToADryRoom() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            int[] t = CharybdisBasinPlan.treasuryBounds(c[0], c[1]);
            Set<Long> lockCells = new HashSet<>();
            for (CharybdisBasinPlan.Hall h : CharybdisBasinPlan.halls(c[0], c[1])) {
                // the airlock's chamber: five wide, from the inner door to the cell before the sealed face
                for (int along = 23; along <= 30; along++) {
                    for (int lat = -2; lat <= 2; lat++) {
                        int px = h.doorX() - h.dx() * (CharybdisBasinPlan.GATE_DOOR - along), pz = h.doorZ() - h.dz() * (CharybdisBasinPlan.GATE_DOOR - along);
                        int ax = px + (h.dx() == 0 ? lat : 0), az = pz + (h.dz() == 0 ? lat : 0);
                        for (int y = F + 1; y <= F + 3; y++) lockCells.add(key(ax, y, az));
                    }
                }
            }
            int bad = 0;
            for (int y = F + 1; y <= p.topY; y++) {
                for (int z = p.footprint.minZ(); z <= p.footprint.maxZ(); z++) {
                    for (int x = p.footprint.minX(); x <= p.footprint.maxX(); x++) {
                        if (!p.footprint.contains(x, z) || !r.air(x, y, z)) continue;
                        boolean treasury = x >= t[0] && x <= t[3] && y >= t[1] && y <= t[4] && z >= t[2] && z <= t[5];
                        if (!treasury && !lockCells.contains(key(x, y, z))) {
                            if (bad++ < 5) fail("an air cell outside the dry rooms at " + x + "," + y + "," + z + " in cell " + c[0] + "," + c[1]);
                        }
                    }
                }
            }
            assertEquals(0, bad);
        }
    }

    private static long key(int x, int y, int z) { return ((long) x & 0xFFFFFL) << 40 | ((long) y & 0xFFFFFL) << 20 | ((long) z & 0xFFFFFL); }

    @Test
    void theWallRingIsSolidAllRoundAndTheRoofAndFloorAreWhole() {
        ZonePlan p = CharybdisBasinPlan.of(0, 0);
        Raster r = new Raster(p);
        int cx = p.centreX(), cz = p.centreZ();
        for (int y = F - CharybdisBasinPlan.FOUNDATION; y <= p.topY; y++) {
            for (int z = -CharybdisBasinPlan.OUTER; z <= CharybdisBasinPlan.OUTER; z++) {
                for (int x = -CharybdisBasinPlan.OUTER; x <= CharybdisBasinPlan.OUTER; x++) {
                    boolean inner = (long) x * x + (long) z * z <= (long) CharybdisBasinPlan.R * CharybdisBasinPlan.R;
                    boolean outer = p.footprint.contains(cx + x, cz + z);
                    if (!outer) continue;
                    String n = r.at(cx + x, y, cz + z);
                    boolean dais = y < F && (long) x * x + (long) z * z <= (long) CharybdisBasinPlan.STEP_1 * CharybdisBasinPlan.STEP_1;
                    if (dais) continue;
                    if (y < F || y > F + CharybdisBasinPlan.HEIGHT) assertFalse(n.equals("minecraft:air") || n.equals("minecraft:water"), "foundation and roof are solid at " + x + "," + y + "," + z + ": " + n);
                    else if (!inner && y >= F + 8) assertFalse(n.equals("minecraft:air") || n.equals("minecraft:water"), "the wall is solid at " + x + "," + y + "," + z + ": " + n);
                }
            }
        }
        assertEquals("minecraft:prismarine", r.at(cx + 32, F + 5, cz), "the wall's outer skin is rough prismarine");
    }

    // ---------------------------------------------------------------- the dais and the swimming band
    @Test
    void theDaisIsAWellInThreeStepsAndTheLeviathanRisesFromItsBottom() {
        ZonePlan p = CharybdisBasinPlan.of(0, 0);
        Raster r = new Raster(p);
        int cx = p.centreX(), cz = p.centreZ();
        // the floor drops 1, 2 and 3 blocks towards the middle
        assertFalse(r.soft(cx + 9, F - 1, cz) , "the first step is solid at f-1");
        assertTrue(r.water(cx + 9, F, cz), "and the cell above it is water");
        assertFalse(r.soft(cx + 6, F - 2, cz));
        assertTrue(r.water(cx + 6, F - 1, cz) && r.water(cx + 6, F, cz));
        assertFalse(r.soft(cx + 3, F - 3, cz));
        assertTrue(r.water(cx + 3, F - 2, cz) && r.water(cx + 3, F, cz));
        assertFalse(r.soft(cx + 15, F, cz) && !r.at(cx + 15, F, cz).contains("prismarine"), "the floor outside the dais is at f");
        // a column of water the width of the leviathan from the bottom of the well up through the whole swimming band
        for (int y = F - 2; y <= F + 10; y++)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    if (dx * dx + dz * dz <= 5) assertTrue(r.water(cx + dx, y, cz + dz), "water for it to rise through at " + dx + "," + y + "," + dz + ": " + r.at(cx + dx, y, cz + dz));
        assertEquals(F + CharybdisBasinPlan.HOVER, p.spawns(ZonePlan.ROLE_BOSS).get(0).y());
        assertEquals(cx, p.spawns(ZonePlan.ROLE_BOSS).get(0).x());
        assertEquals(cz, p.spawns(ZonePlan.ROLE_BOSS).get(0).z());
    }

    @Test
    void nothingStandsInTheBandTheBossSwimsIn() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            int cx = p.centreX(), cz = p.centreZ();
            for (int y = F + CharybdisBasinPlan.BAND_LOW; y <= F + 10; y++) {
                for (int dz = -16; dz <= 16; dz++) {
                    for (int dx = -16; dx <= 16; dx++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d < CharybdisBasinPlan.BAND_MIN - 1.5 || d > CharybdisBasinPlan.BAND_MAX + 1.5) continue;
                        assertTrue(r.wet(cx + dx, y, cz + dz), "the band is open water at " + dx + "," + (y - F) + "," + dz + ": " + r.at(cx + dx, y, cz + dz));
                    }
                }
            }
        }
    }

    @Test
    void theRuinIsThere() {
        ZonePlan p = CharybdisBasinPlan.of(0, 0);
        Map<String, Integer> count = new HashMap<>();
        for (NexusOp o : p.ops) count.merge(o.block(), 1, Integer::sum);
        for (String b : new String[]{"minecraft:prismarine_bricks", "minecraft:dark_prismarine", "minecraft:prismarine", "minecraft:sea_lantern", "minecraft:kelp", "minecraft:kelp_plant",
                "minecraft:sea_pickle", "minecraft:water", "minecraft:iron_door"})
            assertTrue(count.getOrDefault(b, 0) > 0, "the plan uses " + b);
        long corals = count.entrySet().stream().filter(e -> e.getKey().endsWith("_coral_block")).mapToInt(Map.Entry::getValue).sum();
        assertTrue(corals >= 10, "coral accents: " + corals);
        assertTrue(count.get("minecraft:sea_lantern") > 100, "the water is dark, so there are lamps: " + count.get("minecraft:sea_lantern"));
        assertEquals(CharybdisBasinPlan.HEIGHT, 24);
    }

    // ---------------------------------------------------------------- the Treasury
    @Test
    void theTreasuryIsADryRoomBehindAFloodedCorridorAndASealedDoorway() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            int[] t = CharybdisBasinPlan.treasuryBounds(c[0], c[1]);
            // the room is air all through, nine by seven by five, sealed by solid stone on every side but the doorway
            int wide = t[3] - t[0] + 1, deep = t[5] - t[2] + 1;
            assertEquals(63, wide * deep, "nine by seven");
            assertEquals(5, t[4] - t[1] + 1, "five high");
            for (int y = t[1]; y <= t[4]; y++)
                for (int z = t[2]; z <= t[5]; z++)
                    for (int x = t[0]; x <= t[3]; x++)
                        if (!r.at(x, y, z).equals("minecraft:dark_prismarine") && !r.at(x, y, z).equals("minecraft:sea_lantern")) assertTrue(r.air(x, y, z), "the room is air at " + x + "," + y + "," + z + ": " + r.at(x, y, z));
            ZonePlan.Spot gate = p.gates.get(0);
            assertEquals(1, p.gates.size());
            assertEquals(F + 1, gate.y());
            int gx = gate.x(), gz = gate.z();
            // the doorway cells (two high) are solid until populate sets the gate in
            for (int dy = 0; dy < 2; dy++) assertFalse(r.soft(gx, gate.y() + dy, gz), "the doorway is shut stone until the mana gate: " + r.at(gx, gate.y() + dy, gz));
            // the water side: the corridor leads up to it and is flooded all the way to the mouth
            int dirX = Integer.signum(gx - p.centreX()), dirZ = Integer.signum(gz - p.centreZ());
            if (Math.abs(gx - p.centreX()) > Math.abs(gz - p.centreZ())) dirZ = 0; else dirX = 0;
            for (int back = 1; back <= CharybdisBasinPlan.TREASURY_NEAR - CharybdisBasinPlan.CORRIDOR_FROM + 1; back++)
                for (int dy = 0; dy < 3; dy++) assertTrue(r.water(gx - dirX * back, gate.y() + dy, gz - dirZ * back), "the corridor is flooded " + back + " cells from the gate (height " + dy + ")");
            // the dry side
            for (int dy = 0; dy < 2; dy++) assertTrue(r.air(gx + dirX, gate.y() + dy, gz + dirZ), "dry air behind the doorway");
        }
    }

    @Test
    void theRelicsStandOnPlinthsInsideTheDryRoom() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            int[] t = CharybdisBasinPlan.treasuryBounds(c[0], c[1]);
            assertNotNull(p.centralRelic);
            assertEquals(4, p.minorRelics.size(), "four minor relics and one central one");
            List<ZonePlan.Spot> all = new ArrayList<>(p.minorRelics);
            all.add(p.centralRelic);
            Set<ZonePlan.Spot> seen = new HashSet<>();
            for (ZonePlan.Spot s : all) {
                assertTrue(seen.add(s), "no two pedestals on one spot");
                assertTrue(s.x() >= t[0] && s.x() <= t[3] && s.z() >= t[2] && s.z() <= t[5], "inside the room");
                assertEquals(F + 2, s.y());
                assertTrue(r.air(s.x(), s.y(), s.z()), "a free cell for the pedestal");
                assertTrue(r.air(s.x(), s.y() + 1, s.z()) && r.air(s.x(), s.y() + 2, s.z()), "room above it");
                assertEquals("minecraft:dark_prismarine", r.at(s.x(), s.y() - 1, s.z()), "a plinth under it");
            }
        }
    }

    // ---------------------------------------------------------------- the airlocks
    @Test
    void threeAirlocksLetPeopleInDryAndStandable() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            List<CharybdisBasinPlan.Hall> halls = CharybdisBasinPlan.halls(c[0], c[1]);
            assertEquals(3, halls.size(), "one on each side but the Treasury's");
            assertEquals(3, p.entrances.size());
            assertEquals(3, p.spawns(ZonePlan.ROLE_ARRIVAL).size());
            Set<Integer> sides = new HashSet<>();
            for (CharybdisBasinPlan.Hall h : halls) {
                assertNotEquals(treasurySideOf(c[0], c[1]), h.side());
                assertTrue(sides.add(h.side()));
                assertEquals(1, Math.abs(h.dx()) + Math.abs(h.dz()), "one step outward");
                // the door cell is on the footprint's edge, the sealed face one cell in
                assertTrue(p.footprint.contains(h.doorX(), h.doorZ()), "the outer door cell is inside the footprint");
                assertEquals(h.doorX() - h.dx(), h.firstX());
                assertEquals(h.doorZ() - h.dz(), h.firstZ());
                for (int dy = 1; dy <= 2; dy++) {
                    assertFalse(r.soft(h.firstX(), F + dy, h.firstZ()), "the sealed face is solid until the way is cut: " + r.at(h.firstX(), F + dy, h.firstZ()));
                    assertFalse(r.soft(h.doorX(), F + dy, h.doorZ()), "and so is the door cell");
                    // the chamber behind the face: dry air
                    assertTrue(r.air(h.firstX() - h.dx(), F + dy, h.firstZ() - h.dz()), "air behind the face");
                    // the inner door: both halves, with the basin's water in front of it and dry air behind
                    int ix = h.doorX() - h.dx() * 10, iz = h.doorZ() - h.dz() * 10;
                    assertEquals("minecraft:iron_door", r.at(ix, F + dy, iz), "the open iron door at height " + dy);
                    assertTrue(r.water(ix - h.dx(), F + dy, iz - h.dz()), "water in front of the door");
                    assertTrue(r.air(ix + h.dx(), F + dy, iz + h.dz()), "air behind the door");
                }
            }
            for (ZonePlan.Spot e : p.entrances) {
                assertEquals(F + 1, e.y());
                assertTrue(r.air(e.x(), e.y(), e.z()) && r.air(e.x(), e.y() + 1, e.z()), "feet and head free");
                assertFalse(r.soft(e.x(), e.y() - 1, e.z()), "and a floor under them");
                assertTrue(p.footprint.contains(e.x(), e.z()));
            }
            ZonePlan.Spot rescue = p.rescuePoint();
            assertTrue(p.spawns(ZonePlan.ROLE_ARRIVAL).contains(new ZonePlan.Spot(rescue.x(), rescue.y(), rescue.z())), "the rescue point is an arrival spot, inside a dry airlock");
        }
    }

    @Test
    void theWaysOutOfTheBasinAreWhatTheCollapseUsesAsExits() {
        ZonePlan p = CharybdisBasinPlan.of(2, 2);
        assertFalse(p.entrances.isEmpty());
        for (ZonePlan.Spot e : p.entrances) assertTrue(p.contains(e.x(), e.y(), e.z(), 0));
        // far enough apart to be separate exits (the collapse keeps those 6 blocks apart)
        for (int i = 0; i < p.entrances.size(); i++)
            for (int j = i + 1; j < p.entrances.size(); j++)
                assertTrue(Math.abs(p.entrances.get(i).x() - p.entrances.get(j).x()) + Math.abs(p.entrances.get(i).z() - p.entrances.get(j).z()) > 12);
    }

    // ---------------------------------------------------------------- where the megalodons come out
    @Test
    void theMegalodonSpotsAreInOpenWaterNearTheWall() {
        for (int[] c : cellsPerSide()) {
            ZonePlan p = CharybdisBasinPlan.of(c[0], c[1]);
            Raster r = new Raster(p);
            List<ZonePlan.Spot> spots = p.spawns(ZonePlan.ROLE_MINION);
            assertEquals(24, spots.size());
            int open = 0;
            for (ZonePlan.Spot s : spots) {
                double d = Math.hypot(s.x() - p.centreX(), s.z() - p.centreZ());
                assertTrue(d >= CharybdisBasinPlan.R - 4.5 && d <= CharybdisBasinPlan.R - 1.5, "a few blocks off the wall: " + d);
                assertTrue(s.y() > F + 2 && s.y() < p.topY - 3);
                if (r.water(s.x(), s.y(), s.z()) && r.water(s.x(), s.y() + 1, s.z()) && r.water(s.x(), s.y() - 1, s.z())) open++;
            }
            assertTrue(open >= 16, "most of the spots are open water: " + open);
        }
    }

    @Test
    void theRelicIndexIsStableAndInRange() {
        for (int[] c : CELLS) {
            int a = CharybdisBasinPlan.centralRelicIndex(c[0], c[1], 5), b = CharybdisBasinPlan.centralRelicIndex(c[0], c[1], 5);
            assertEquals(a, b);
            assertTrue(a >= 0 && a < 5);
        }
        assertEquals(0, CharybdisBasinPlan.centralRelicIndex(1, 1, 0));
        Set<Integer> seen = new HashSet<>();
        for (int cx = 0; cx < 50; cx++) seen.add(CharybdisBasinPlan.centralRelicIndex(cx, 1, 5));
        assertTrue(seen.size() >= 4, "the central relic varies from basin to basin");
    }
}
