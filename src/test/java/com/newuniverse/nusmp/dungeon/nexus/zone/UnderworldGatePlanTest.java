package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldGatePlan.Gate;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.newuniverse.nusmp.dungeon.nexus.zone.UnderworldGatePlan.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Underworld Gate's plan: bounds, determinism, a sealed shell, ways in, a sealed Treasury, one central pedestal, the skywalk and the islands. */
class UnderworldGatePlanTest {
    private static final int[][] CELLS = {{0, 0}, {3, 2}, {-4, 7}, {9, -9}, {12, 5}, {-1, -1}, {5, -3}, {-8, -8}};

    // ---------------------------------------------------------------- a small voxel world to apply a plan to
    /** The cells of a plan applied to a world of rock: what is there after every op has run in order (blocks by name, air as null). */
    static final class Sim {
        final ZonePlan plan;
        final int x0, y0, z0, w, h, d;
        final short[] cells;
        final List<String> palette = new ArrayList<>();
        final Map<String, Short> ids = new HashMap<>();

        Sim(ZonePlan plan) {
            this.plan = plan;
            x0 = plan.centreX() - OUTER - 3;
            z0 = plan.centreZ() - OUTER - 3;
            y0 = plan.floorY - FOUNDATION - 2;
            w = 2 * (OUTER + 3) + 1;
            d = w;
            h = plan.topY - y0 + 3;
            cells = new short[w * h * d];
            palette.add(null);
            short rock = id("minecraft:stone");
            java.util.Arrays.fill(cells, rock);
            for (NexusOp o : plan.ops) {
                short b = o.kind() == NexusOp.Kind.CARVE || o.block().equals(NexusOp.AIR) ? 0 : id(o.block());
                for (int x = o.x1(); x <= o.x2(); x++) {
                    for (int y = o.y1(); y <= o.y2(); y++) {
                        for (int z = o.z1(); z <= o.z2(); z++) {
                            int i = index(x, y, z);
                            if (o.kind() == NexusOp.Kind.FILL_AIR && cells[i] != 0) continue;
                            cells[i] = b;
                        }
                    }
                }
            }
        }

        short id(String block) { return ids.computeIfAbsent(block, k -> { palette.add(k); return (short) (palette.size() - 1); }); }

        int index(int x, int y, int z) { return ((x - x0) * h + (y - y0)) * d + (z - z0); }

        boolean in(int x, int y, int z) { return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h && z >= z0 && z < z0 + d; }

        String at(int x, int y, int z) { return in(x, y, z) ? palette.get(cells[index(x, y, z)]) : "minecraft:stone"; }

        boolean air(int x, int y, int z) { return at(x, y, z) == null; }

        /** Can a body occupy this cell? Air, and the small things that give way: carpets, chains, end rods, crystals. */
        boolean passable(int x, int y, int z) {
            String b = at(x, y, z);
            return b == null || b.contains("carpet") || b.contains("chain") || b.contains("end_rod") || b.contains("amethyst_cluster");
        }

        boolean standable(int x, int y, int z) { return passable(x, y, z) && passable(x, y + 1, z) && !passable(x, y - 1, z); }
    }

    private static final Map<Long, Sim> SIMS = new HashMap<>();

    private static Sim sim(int cx, int cz) { return SIMS.computeIfAbsent(((long) cx << 32) | (cz & 0xFFFFFFFFL), k -> new Sim(UnderworldGatePlan.of(cx, cz))); }

    /** The world position of a local offset (u, v) at height layer h of the Nexus of this cell. */
    private static int[] world(int cx, int cz, int u, int h, int v) {
        ZonePlan p = UnderworldGatePlan.of(cx, cz);
        Side s = SIDES[gateSide(cx, cz)];
        return new int[]{p.centreX() + s.wx(u, v), p.floorY + h, p.centreZ() + s.wz(u, v)};
    }

    /** Where can a walker get to from here (steps of one block up, a few down, no swimming): the cells of the standing places reached. */
    private static Set<Long> walk(Sim s, int[] start, Set<Long> blocked) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(start);
        seen.add(key(start[0], start[1], start[2]));
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] dir : dirs) {
                int nx = c[0] + dir[0], nz = c[2] + dir[1];
                for (int ny = c[1] + 1; ny >= c[1] - 4; ny--) {
                    if (!s.standable(nx, ny, nz) || blocked.contains(key(nx, ny, nz)) || blocked.contains(key(nx, ny + 1, nz))) continue;
                    boolean clear = true;
                    if (ny <= c[1]) {                                    // level or down: the body walks out at its own height, then drops
                        for (int y = ny; y <= c[1] + 1; y++) if (!s.passable(nx, y, nz) || blocked.contains(key(nx, y, nz))) clear = false;
                    } else if (!s.passable(c[0], c[1] + 2, c[2])) {      // up one block: a jump needs room over the head where it starts
                        clear = false;
                    }
                    if (!clear) continue;
                    if (seen.add(key(nx, ny, nz))) q.add(new int[]{nx, ny, nz});
                    break;
                }
            }
        }
        return seen;
    }

    private static long key(int x, int y, int z) { return ((long) (x + 4096) << 40) | ((long) (y + 512) << 24) | (z + 4096); }

    // ---------------------------------------------------------------- the plan as a whole
    @Test
    void aPlanIsDeterministicAndNamedForItsZone() {
        for (int[] c : CELLS) {
            ZonePlan a = UnderworldGatePlan.of(c[0], c[1]), b = UnderworldGatePlan.of(c[0], c[1]);
            assertEquals(NexusZone.UNDERWORLD_GATE, a.zone);
            assertEquals(a.ops, b.ops, "the same cell gives the same plan");
            assertEquals(a.phaseCount(), b.phaseCount());
            assertEquals(a.allSpawns(), b.allSpawns());
            assertEquals(c[0], a.cellX);
            assertEquals(c[1], a.cellZ);
        }
        assertNotEquals(UnderworldGatePlan.of(0, 0).ops, UnderworldGatePlan.of(1, 0).ops);
    }

    @Test
    void theHallStandsInsideItsCellAndInTheDeepestTier() {
        for (int cx = -6; cx <= 6; cx++) {
            for (int cz = -6; cz <= 6; cz++) {
                ZonePlan p = UnderworldGatePlan.of(cx, cz);
                assertTrue(p.insideCell(), "cell " + cx + "," + cz + " keeps its hall inside it");
                assertEquals(Footprint.Shape.OVAL, p.footprint.shape());
                assertEquals(OUTER, p.footprint.halfX());
                assertTrue(p.floorY >= -56 && p.floorY <= -54, "floor " + p.floorY);
                assertEquals(p.floorY + HEIGHT + ROOF, p.topY);
                assertTrue(Math.abs(p.centreX() - NexusZone.centre(cx)) <= JITTER && Math.abs(p.centreZ() - NexusZone.centre(cz)) <= JITTER);
                assertFalse(p.opsStrayFromFootprint(), "no op reaches a chunk the footprint does not touch");
            }
        }
    }

    @Test
    void everyOpStaysInsideTheOvalAndTheHeights() {
        for (int[] c : CELLS) {
            ZonePlan p = UnderworldGatePlan.of(c[0], c[1]);
            Footprint fp = p.footprint;
            for (NexusOp o : p.ops) {
                assertTrue(fp.contains(o.x1(), o.z1()) && fp.contains(o.x1(), o.z2()) && fp.contains(o.x2(), o.z1()) && fp.contains(o.x2(), o.z2()), "op " + o + " leaves the oval");
                assertTrue(o.y1() >= p.floorY - FOUNDATION && o.y2() <= p.topY, "op " + o + " leaves the heights");
                assertTrue(o.y1() > -64 + 0 && o.y2() < 120);
            }
        }
    }

    @Test
    void theOpsAreInFourPhasesAndAreNotTooMany() {
        for (int[] c : CELLS) {
            ZonePlan p = UnderworldGatePlan.of(c[0], c[1]);
            assertEquals(4, p.phaseCount());
            for (int i = 0; i < p.phaseStart(1); i++) assertEquals(NexusOp.Kind.CARVE, p.ops.get(i).kind(), "phase 0 only clears the air");
            assertTrue(p.ops.size() < 12000, "ops " + p.ops.size());
            long volume = 0;
            for (NexusOp o : p.ops) volume += o.volume();
            assertTrue(volume < 1_600_000L, "volume " + volume);
            for (int i = 0; i < p.phaseStart(1); i++) assertEquals(NexusOp.Kind.CARVE, p.ops.get(i).kind());
            for (int i = p.phaseStart(1); i < p.ops.size(); i++) assertNotEquals(NexusOp.Kind.CARVE, p.ops.get(i).kind(), "later phases only place blocks");
        }
    }

    @Test
    void theShellKeepsTheCavesOut() {
        for (int[] c : new int[][]{{0, 0}, {3, 2}, {-4, 7}}) {
            Sim s = sim(c[0], c[1]);
            ZonePlan p = s.plan;
            ZonePlan.Spot a = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            ArrayDeque<int[]> q = new ArrayDeque<>();
            Set<Long> seen = new HashSet<>();
            q.add(new int[]{a.x(), a.y(), a.z()});
            seen.add(key(a.x(), a.y(), a.z()));
            int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!q.isEmpty()) {
                int[] cur = q.poll();
                long r2 = (long) (cur[0] - p.centreX()) * (cur[0] - p.centreX()) + (long) (cur[2] - p.centreZ()) * (cur[2] - p.centreZ());
                assertTrue(r2 <= (long) (R + 1) * (R + 1) + 1, "air at " + cur[0] + "," + cur[1] + "," + cur[2] + " is outside the hall (r2 " + r2 + ")");
                assertTrue(cur[1] > p.floorY && cur[1] <= p.floorY + HEIGHT, "air at height " + cur[1]);
                for (int[] dir : dirs) {
                    int nx = cur[0] + dir[0], ny = cur[1] + dir[1], nz = cur[2] + dir[2];
                    if (!s.in(nx, ny, nz) || !s.air(nx, ny, nz) || !seen.add(key(nx, ny, nz))) continue;
                    q.add(new int[]{nx, ny, nz});
                }
            }
            assertTrue(seen.size() > 150_000, "the hall holds a great volume of air: " + seen.size());
        }
    }

    // ---------------------------------------------------------------- the Gate, the Treasury and the pedestals
    @Test
    void thereIsOneCentralPedestalAndFourMinorOnesOnSolidFloor() {
        for (int[] c : CELLS) {
            Sim s = sim(c[0], c[1]);
            ZonePlan p = s.plan;
            assertNotNull(p.centralRelic);
            assertEquals(4, p.minorRelics.size());
            List<ZonePlan.Spot> all = new ArrayList<>(p.minorRelics);
            all.add(p.centralRelic);
            assertEquals(5, new HashSet<>(all).size(), "five different pedestals");
            for (ZonePlan.Spot sp : all) {
                assertTrue(s.air(sp.x(), sp.y(), sp.z()), "the pedestal's own cell is free");
                assertFalse(s.passable(sp.x(), sp.y() - 1, sp.z()), "and stands on something");
                assertTrue(s.air(sp.x(), sp.y() + 1, sp.z()));
            }
            int[] expect = world(c[0], c[1], 0, DAIS_TOP + 2, -8);
            assertArrayEquals(expect, new int[]{p.centralRelic.x(), p.centralRelic.y(), p.centralRelic.z()});
            assertEquals(5, p.centralRelic.y() - p.floorY, "on top of the plinth");
            for (ZonePlan.Spot m : p.minorRelics) assertEquals(DAIS_TOP + 1, m.y() - p.floorY);
        }
    }

    @Test
    void theTreasuryIsReachedOnlyThroughTheGate() {
        for (int[] c : new int[][]{{0, 0}, {3, 2}, {-4, 7}, {9, -9}}) {
            Sim s = sim(c[0], c[1]);
            ZonePlan p = s.plan;
            assertEquals(1, p.gates.size());
            ZonePlan.Spot g = p.gates.get(0);
            int[] expect = world(c[0], c[1], 0, DAIS_TOP + 1, PORTAL_FRONT);
            assertArrayEquals(expect, new int[]{g.x(), g.y(), g.z()});
            assertTrue(s.air(g.x(), g.y(), g.z()) && s.air(g.x(), g.y() + 1, g.z()), "the doorway is two high");
            assertFalse(s.passable(g.x(), g.y() + 2, g.z()), "with a lintel over it");
            // one wide: both sides are stone, for the whole of the doorway, on both layers of the wall
            for (int dy = 0; dy <= 1; dy++) {
                for (int side = -1; side <= 1; side += 2) {
                    for (int v = PORTAL_BACK; v <= PORTAL_FRONT; v++) {
                        int[] w = world(c[0], c[1], side, DAIS_TOP + 1 + dy, v);
                        assertFalse(s.passable(w[0], w[1], w[2]), "doorway side at v=" + v);
                    }
                }
            }
            int[] inside = world(c[0], c[1], 0, DAIS_TOP + 1, -3), front = world(c[0], c[1], 0, DAIS_TOP + 1, 6);
            assertTrue(s.standable(inside[0], inside[1], inside[2]), "the Treasury floor");
            assertTrue(s.standable(front[0], front[1], front[2]), "the hall in front of the portal");
            Set<Long> shut = new HashSet<>();
            shut.add(key(g.x(), g.y(), g.z()));
            shut.add(key(g.x(), g.y() + 1, g.z()));
            Set<Long> closed = walk(s, front, shut), open = walk(s, front, new HashSet<>());
            assertFalse(closed.contains(key(inside[0], inside[1], inside[2])), "with the mana gate shut nobody gets in");
            assertTrue(open.contains(key(inside[0], inside[1], inside[2])), "with it open the Treasury can be reached");
            for (ZonePlan.Spot m : p.minorRelics) assertTrue(open.contains(key(m.x(), m.y(), m.z())) || s.standable(m.x(), m.y(), m.z()));
            assertTrue(s.standable(p.centralRelic.x(), p.centralRelic.y() - 1, p.centralRelic.z()) || !s.passable(p.centralRelic.x(), p.centralRelic.y() - 1, p.centralRelic.z()));
        }
    }

    @Test
    void theFourGatesOfTheShellAreReachedFromTheHallAndKeepTheirWalls() {
        for (int[] c : CELLS) {
            Sim s = sim(c[0], c[1]);
            ZonePlan p = s.plan;
            List<Gate> gates = UnderworldGatePlan.gates(c[0], c[1]);
            assertEquals(4, gates.size());
            assertEquals(4, p.entrances.size());
            Set<Integer> sides = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                Gate g = gates.get(i);
                ZonePlan.Spot e = p.entrances.get(i);
                sides.add(g.side());
                assertEquals(1, Math.abs(g.dx()) + Math.abs(g.dz()), "an axis direction");
                assertTrue(s.standable(e.x(), e.y(), e.z()), "the entrance " + i + " is a place to stand");
                assertEquals(p.floorY + 1, e.y());
                // the niche in front of the seal is open, the seal and the door cell are stone
                int nx = e.x() + g.dx(), nz = e.z() + g.dz();
                assertTrue(s.air(nx, e.y(), nz) && s.air(nx, e.y() + 3, nz), "the niche");
                assertFalse(s.passable(g.firstX(), e.y(), g.firstZ()), "the seal");
                assertFalse(s.passable(g.doorX(), e.y(), g.doorZ()), "the door cell is shell");
                assertEquals(g.firstX() + g.dx(), g.doorX());
                assertEquals(g.firstZ() + g.dz(), g.doorZ());
                long r2 = (long) (g.doorX() - p.centreX()) * (g.doorX() - p.centreX()) + (long) (g.doorZ() - p.centreZ()) * (g.doorZ() - p.centreZ());
                assertEquals((long) OUTER * OUTER, r2, "the door is the outermost cell of the shell on its axis");
                assertTrue(p.footprint.contains(g.doorX(), g.doorZ()));
            }
            assertEquals(Set.of(0, 1, 2, 3), sides);
        }
    }

    @Test
    void everyEntranceLeadsToTheGateAndTheArrivalToo() {
        for (int[] c : new int[][]{{0, 0}, {3, 2}, {-4, 7}, {9, -9}, {12, 5}}) {
            Sim s = sim(c[0], c[1]);
            ZonePlan p = s.plan;
            int[] goal = world(c[0], c[1], 0, DAIS_TOP + 1, 6);
            long goalKey = key(goal[0], goal[1], goal[2]);
            for (ZonePlan.Spot e : p.entrances) {
                Set<Long> reach = walk(s, new int[]{e.x(), e.y(), e.z()}, new HashSet<>());
                assertTrue(reach.contains(goalKey), "entrance at " + e.x() + "," + e.z() + " cannot reach the vestibule (reached " + reach.size() + " places)");
                for (ZonePlan.Spot o : p.entrances) assertTrue(reach.contains(key(o.x(), o.y(), o.z())), "the entrances are joined by the floor");
            }
            ZonePlan.Spot a = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            assertTrue(walk(s, new int[]{a.x(), a.y(), a.z()}, new HashSet<>()).contains(goalKey));
        }
    }

    @Test
    void theWayToTheGateNeedsNoJumpAndHasOnlyStairsToClimb() {
        // in a heavy zone nobody can jump a block: the front way is level to the foot of the dais, then three courses of stairs, then the level floor of the vestibule
        for (int[] c : CELLS) {
            Sim s = sim(c[0], c[1]);
            for (int u = -3; u <= 3; u++) {
                for (int v = R - 1; v >= DAIS_FRONT + 4; v--) {
                    int[] floor = world(c[0], c[1], u, 0, v), body = world(c[0], c[1], u, 1, v), head = world(c[0], c[1], u, 2, v);
                    assertFalse(s.passable(floor[0], floor[1], floor[2]), "the way has a floor at " + u + "," + v);
                    assertTrue(s.passable(body[0], body[1], body[2]) && s.passable(head[0], head[1], head[2]), "and is clear at " + u + "," + v + ": " + s.at(body[0], body[1], body[2]));
                }
                for (int k = 0; k < 3; k++) {
                    int[] st = world(c[0], c[1], u, DAIS_TOP - k, DAIS_FRONT + 1 + k);
                    assertTrue(s.at(st[0], st[1], st[2]).contains("stairs"), "stair course " + k + " at u=" + u);
                    int[] above = world(c[0], c[1], u, DAIS_TOP - k + 1, DAIS_FRONT + 1 + k);
                    assertTrue(s.passable(above[0], above[1], above[2]) && s.passable(above[0], above[1] + 1, above[2]));
                }
                for (int v = DAIS_FRONT; v >= VEST_BACK && Math.abs(u) <= 1; v--) {          // lanterns stand beside the doorway, not in front of it
                    int[] floor = world(c[0], c[1], u, DAIS_TOP, v), body = world(c[0], c[1], u, DAIS_TOP + 1, v);
                    assertFalse(s.passable(floor[0], floor[1], floor[2]));
                    assertTrue(s.passable(body[0], body[1], body[2]), "the vestibule is clear at " + u + "," + v + ": " + s.at(body[0], body[1], body[2]));
                }
            }
        }
    }

    @Test
    void theArrivalSpotIsOnTheFrontWayInFrontOfTheGate() {
        for (int[] c : CELLS) {
            ZonePlan p = UnderworldGatePlan.of(c[0], c[1]);
            ZonePlan.Spot a = p.spawns(ZonePlan.ROLE_ARRIVAL).get(0);
            assertArrayEquals(world(c[0], c[1], 0, 1, 41), new int[]{a.x(), a.y(), a.z()});
            assertEquals(a, p.rescuePoint());
            Gate front = UnderworldGatePlan.gates(c[0], c[1]).get(gateSide(c[0], c[1]));
            assertEquals(gateSide(c[0], c[1]), front.side(), "the front gate is on the side the Gate faces");
        }
    }

    // ---------------------------------------------------------------- the creatures' places
    @Test
    void everySpawnSpotIsAFreeStandingPlace() {
        for (int[] c : CELLS) {
            Sim s = sim(c[0], c[1]);
            ZonePlan p = s.plan;
            int total = 0;
            for (var e : p.allSpawns().entrySet()) {
                for (ZonePlan.Spot sp : e.getValue()) {
                    total++;
                    assertTrue(s.standable(sp.x(), sp.y(), sp.z()), e.getKey() + " spot " + sp + " is not a place to stand");
                    assertTrue(p.footprint.contains(sp.x(), sp.z(), -3), "inside the hall");
                    assertTrue(s.passable(sp.x(), sp.y() + 2, sp.z()), "room over its head");
                }
            }
            assertEquals(1, p.spawns(ROLE_GATEKEEPER).size());
            assertEquals(POSTS, p.spawns(ZonePlan.ROLE_ELITE).size(), "every Devil has a post");
            assertTrue(p.spawns(ZonePlan.ROLE_MINION).size() >= 12, "enough places for Disciples to appear: " + p.spawns(ZonePlan.ROLE_MINION).size());
            assertEquals(1, p.spawns(ZonePlan.ROLE_ARRIVAL).size());
            assertTrue(total > 20);
            ZonePlan.Spot k = p.spawns(ROLE_GATEKEEPER).get(0);
            assertArrayEquals(world(c[0], c[1], 0, DAIS_TOP + 1, 8), new int[]{k.x(), k.y(), k.z()}, "the Gatekeeper stands in the vestibule");
            int[] v = vestibuleBounds(c[0], c[1]);
            assertTrue(k.x() >= v[0] && k.x() <= v[3] && k.y() >= v[1] && k.y() <= v[4] && k.z() >= v[2] && k.z() <= v[5]);
        }
    }

    @Test
    void theDisciplesSpotsAreSpreadAroundTheHallAndAwayFromTheGate() {
        for (int[] c : CELLS) {
            ZonePlan p = UnderworldGatePlan.of(c[0], c[1]);
            Set<Integer> quarters = new HashSet<>();
            for (ZonePlan.Spot sp : p.spawns(ZonePlan.ROLE_MINION)) {
                double dx = sp.x() - p.centreX(), dz = sp.z() - p.centreZ();
                assertTrue(Math.hypot(dx, dz) >= 20, "not in the Gate's own yard");
                quarters.add((dx >= 0 ? 1 : 0) + (dz >= 0 ? 2 : 0));
                double depth = UnderworldGatePlan.depth(c[0], c[1], sp.x(), sp.z());
                assertTrue(depth >= 0 && depth <= 1);
            }
            assertEquals(4, quarters.size(), "in every quarter of the hall");
        }
    }

    // ---------------------------------------------------------------- the gravity zones
    @Test
    void theFiveZonesLieInsideTheHallAndAreMarkedInItsFloor() {
        for (int[] c : CELLS) {
            ZonePlan p = UnderworldGatePlan.of(c[0], c[1]);
            Sim s = sim(c[0], c[1]);
            List<GravityCycle.Zone> zones = UnderworldGatePlan.zones(c[0], c[1]);
            assertEquals(5, zones.size());
            Set<GravityCycle.Kind> kinds = new HashSet<>();
            Set<Integer> phases = new HashSet<>();
            for (GravityCycle.Zone z : zones) {
                double d = Math.hypot(z.x() - p.centreX(), z.z() - p.centreZ());
                assertTrue(d + z.radius() <= R - 1, "zone " + z.id() + " reaches the wall");
                assertEquals(p.floorY + 1, z.yMin());
                assertTrue(z.yMax() <= p.floorY + HEIGHT - 4);
                assertFalse(s.air(z.x(), p.floorY, z.z()), "the floor is under the middle of the zone");
                assertEquals("minecraft:crying_obsidian", s.at(z.x(), p.floorY, z.z()), "marked with a glowing seal at its middle");
                kinds.add(z.kind());
                phases.add(z.phase());
            }
            assertEquals(3, kinds.size(), "a well, a float and a tide at least");
            assertTrue(phases.size() >= 4, "zones are out of step");
            double[] closest = {1e9};
            int[] v = vestibuleBounds(c[0], c[1]);
            for (GravityCycle.Zone z : zones) {
                for (int[] corner : new int[][]{{v[0], v[2]}, {v[0], v[5]}, {v[3], v[2]}, {v[3], v[5]}})
                    closest[0] = Math.min(closest[0], Math.hypot(z.x() - corner[0], z.z() - corner[1]) - z.radius());
            }
            assertTrue(closest[0] > 4, "no zone reaches the vestibule, where the Gatekeeper fights: " + closest[0]);
        }
    }

    @Test
    void theRoadToTheGatePassesTwoZonesAndNeverAWall() {
        List<GravityCycle.Zone> zones = UnderworldGatePlan.zones(0, 0);
        int crossed = 0;
        ZonePlan p = UnderworldGatePlan.of(0, 0);
        Side s = SIDES[gateSide(0, 0)];
        for (GravityCycle.Zone z : zones) {
            int u = 0, v = 0;
            // the zone's offset in the local frame: invert the side mapping by trying the candidates
            for (int tu = -50; tu <= 50; tu++) for (int tv = -50; tv <= 50; tv++) if (p.centreX() + s.wx(tu, tv) == z.x() && p.centreZ() + s.wz(tu, tv) == z.z()) { u = tu; v = tv; }
            if (Math.abs(u) <= z.radius() && v >= 17) crossed++;
        }
        assertEquals(2, crossed, "the way from the front gate crosses the float and the well");
    }

    // ---------------------------------------------------------------- the skywalk, the islands and the rest
    @Test
    void theSkywalkClimbsInHalfBlocksAndHasOnlyJumpableGaps() {
        List<SpCell> cells = UnderworldGatePlan.spiral();
        assertTrue(cells.size() > 700, "cells " + cells.size());
        int min = 99, max = 0;
        Map<String, SpCell> byCell = new HashMap<>();
        for (SpCell c : cells) {
            min = Math.min(min, c.level());
            max = Math.max(max, c.level());
            byCell.put(c.u() + "," + c.v() + "," + (int) Math.floor(c.unwrapped() / 360.0), c);
            for (double[] g : SP_GAPS) assertFalse(c.unwrapped() >= g[0] && c.unwrapped() <= g[1], "no cell in a gap");
        }
        assertEquals(1, min);
        assertEquals(SP_LEVELS, max);
        int checked = 0;
        for (SpCell a : cells) {
            for (int[] dir : new int[][]{{1, 0}, {0, 1}}) {
                for (SpCell b : cells) {
                    if (b.u() != a.u() + dir[0] || b.v() != a.v() + dir[1] || Math.abs(b.unwrapped() - a.unwrapped()) > 30) continue;
                    assertTrue(Math.abs(a.level() - b.level()) <= 1, "neighbouring cells differ by at most a half block: " + a + " " + b);
                    checked++;
                }
            }
        }
        assertTrue(checked > 1000);
        for (double[] g : SP_GAPS) {
            double widthBlocks = Math.toRadians(g[1] - g[0]) * SP_R;
            assertTrue(widthBlocks >= 1.5 && widthBlocks <= 3.5, "a gap of " + widthBlocks + " blocks can be jumped");
        }
        assertEquals(1, UnderworldGatePlan.spiralLevel(SP_A0));
        assertEquals(SP_LEVELS, UnderworldGatePlan.spiralLevel(SP_A1));
    }

    @Test
    void theSkywalkIsMadeOfSlabsThatAreReallyPlaced() {
        for (int[] c : new int[][]{{0, 0}, {3, 2}}) {
            Sim s = sim(c[0], c[1]);
            int slabs = 0, tops = 0, bottoms = 0;
            for (SpCell sc : UnderworldGatePlan.spiral()) {
                int h = sc.level() % 2 == 0 ? sc.level() / 2 : sc.level() / 2 + 1;
                int[] w = world(c[0], c[1], sc.u(), h, sc.v());
                String b = s.at(w[0], w[1], w[2]);
                assertNotNull(b, "the skywalk is there at " + sc);
                if (b.contains("slab")) {
                    slabs++;
                    if (sc.level() % 2 == 0) tops++;
                    else bottoms++;
                }
            }
            assertTrue(slabs > 600 && tops > 250 && bottoms > 250, "slabs " + slabs + " tops " + tops + " bottoms " + bottoms);
        }
    }

    @Test
    void islandsAreApartInsideTheHallAndHighEnoughToHangFree() {
        List<Isle> isles = UnderworldGatePlan.isles();
        assertEquals(6, isles.size());
        for (int i = 0; i < isles.size(); i++) {
            Isle a = isles.get(i);
            assertTrue(Math.hypot(a.u(), a.v()) + a.radius() <= R - 2, "island " + i + " is clear of the wall");
            assertTrue(a.layer() - a.depth() - 3 >= 1, "its core hangs in the air: " + (a.layer() - a.depth() - 3));
            assertTrue(a.layer() + 7 < HEIGHT - 6, "and stays clear of the roof");
            assertEquals(0, a.level() % 2, "its top is a whole block, level with the spur");
            for (int j = i + 1; j < isles.size(); j++) {
                Isle b = isles.get(j);
                assertTrue(Math.hypot(a.u() - b.u(), a.v() - b.v()) >= a.radius() + b.radius() + 3, "islands " + i + " and " + j);
            }
            // the spur joins the skywalk at about the island's level
            double un = ISLES[i][1];
            assertTrue(Math.abs(UnderworldGatePlan.spiralLevel(un) - a.level()) <= 1);
        }
    }

    @Test
    void islandsHangersAndPillarsKeepTheirDistance() {
        List<Isle> isles = UnderworldGatePlan.isles();
        for (int[] h : HANGERS) {
            double hu = h[1] * Math.cos(Math.toRadians(h[0])), hv = h[1] * Math.sin(Math.toRadians(h[0]));
            assertTrue(Math.hypot(hu, hv) + h[2] <= R - 1, "a hanger clears the wall");
            assertTrue(HEIGHT - h[3] + 1 - 3 >= 16, "hangs no lower than the middle of the hall");
            for (Isle i : isles) assertTrue(Math.hypot(hu - i.u(), hv - i.v()) >= h[2] + i.radius() + 3, "a hanger and an island overlap");
        }
        for (int[] pl : PILLARS) {
            double pu = pl[1] * Math.cos(Math.toRadians(pl[0])), pv = pl[1] * Math.sin(Math.toRadians(pl[0]));
            int[][] dir = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
            int segs = (pl[2] + 3) / 4;
            double lastU = pu + dir[pl[3]][0] * (segs - 1) + 1, lastV = pv + dir[pl[3]][1] * (segs - 1) + 1;
            assertTrue(Math.hypot(Math.max(Math.abs(pu), Math.abs(lastU)), Math.max(Math.abs(pv), Math.abs(lastV))) <= R - 1, "a pillar leans toward the Gate, not into the wall");
            assertTrue(Math.hypot(lastU, lastV) < Math.hypot(pu, pv) + 2);
        }
    }

    @Test
    void theHallHasItsLightsAndItsGlowingPortal() {
        for (int[] c : CELLS) {
            Sim s = sim(c[0], c[1]);
            int[] portal = world(c[0], c[1], 0, 9, PORTAL_BACK);
            assertEquals("minecraft:pearlescent_froglight", s.at(portal[0], portal[1], portal[2]), "glowing froglight behind the glass");
            int[] glass = world(c[0], c[1], 5, 14, PORTAL_FRONT);
            assertEquals("minecraft:purple_stained_glass", s.at(glass[0], glass[1], glass[2]));
            int light = 0;
            for (int x = s.x0; x < s.x0 + s.w; x++) for (int z = s.z0; z < s.z0 + s.d; z++) for (int y = s.plan.floorY; y <= s.plan.topY; y++) {
                String b = s.at(x, y, z);
                if (b != null && (b.contains("crying_obsidian") || b.contains("soul_lantern") || b.contains("end_rod") || b.contains("froglight"))) light++;
            }
            assertTrue(light > 400, "light sources " + light);
        }
    }

    // ---------------------------------------------------------------- the shapes
    @Test
    void theArchIsOneWideRoundAndSymmetricInHeight() {
        assertEquals(-1, archHalf(VEST_BACK - 1));
        assertEquals(-1, archHalf(ARCH_TOP + 1));
        for (int h = VEST_BACK; h <= 13; h++) assertEquals(VEST_W, archHalf(h));
        int last = VEST_W;
        for (int h = 13; h <= ARCH_TOP; h++) {
            assertTrue(archHalf(h) <= last && archHalf(h) >= 1, "h " + h);
            last = archHalf(h);
        }
        assertTrue(archHalf(ARCH_TOP) <= 4, "a round head");
    }

    @Test
    void theSpadeIsASpade() {
        assertTrue(spade(0, 0));
        assertTrue(spade(0.6, -0.3) && spade(-0.6, -0.3), "symmetric across");
        assertFalse(spade(0, 3) || spade(0, -3) || spade(2, 0) || spade(-2, 0));
        double minX = 9, maxX = -9, minY = 9, maxY = -9;
        int in = 0;
        for (double y = -2; y <= 2; y += 0.02) for (double x = -2; x <= 2; x += 0.02) {
            if (!spade(x, y)) continue;
            in++;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            assertEquals(spade(x, y), spade(-x, y), "mirror symmetry at " + x + "," + y);
        }
        assertTrue(maxX - minX > 2.0 && maxX - minX < 2.6, "width " + (maxX - minX));
        assertTrue(maxY - minY > 2.6 && maxY - minY < 3.2, "height " + (maxY - minY));
        assertTrue(Math.abs(minY + maxY) < 0.25, "centred");
        // a point at the top and a stem at the bottom: the figure is narrow at both ends and wide in the middle-low
        int widthTop = 0, widthMid = 0, widthStem = 0;
        for (double x = -2; x <= 2; x += 0.02) {
            if (spade(x, maxY - 0.3)) widthTop++;
            if (spade(x, -0.3)) widthMid++;
            if (spade(x, minY + 0.15)) widthStem++;
        }
        assertTrue(widthTop < widthMid && widthStem < widthMid / 2, "top " + widthTop + " mid " + widthMid + " stem " + widthStem);
        assertTrue(in > 500);
    }

    @Test
    void sidesTurnTheHallAndAllFourAreUsed() {
        Set<Integer> sides = new HashSet<>();
        Set<Integer> floors = new HashSet<>();
        for (int cx = -8; cx <= 8; cx++) for (int cz = -8; cz <= 8; cz++) {
            sides.add(gateSide(cx, cz));
            floors.add(floorY(cx, cz));
        }
        assertEquals(Set.of(0, 1, 2, 3), sides);
        assertEquals(Set.of(-56, -55, -54), floors);
        for (int i = 0; i < 4; i++) {
            Side s = SIDES[i];
            assertEquals(0, s.wx(0, 0));
            int ox = s.wx(0, 1), oz = s.wz(0, 1);
            assertEquals(1, Math.abs(ox) + Math.abs(oz), "outward is one block on an axis");
            assertEquals(new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}[i][0], ox, "side " + i + " faces " + new String[]{"north", "east", "south", "west"}[i]);
            assertEquals(new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}[i][1], oz);
        }
    }

    @Test
    void theCentralRelicIsOneOfFiveAndVariesFromCellToCell() {
        Set<Integer> seen = new HashSet<>();
        for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) {
            int i = UnderworldGatePlan.centralRelicIndex(cx, cz, 5);
            assertTrue(i >= 0 && i < 5);
            seen.add(i);
        }
        assertEquals(5, seen.size());
        assertEquals(0, UnderworldGatePlan.centralRelicIndex(1, 1, 0));
    }

    @Test
    void theTreasuryAndVestibuleBoundsMatchTheirPlan() {
        for (int[] c : CELLS) {
            Sim s = sim(c[0], c[1]);
            int[] t = treasuryBounds(c[0], c[1]), v = vestibuleBounds(c[0], c[1]);
            for (int x = t[0]; x <= t[3]; x++) for (int z = t[2]; z <= t[5]; z++) assertFalse(s.passable(x, t[1] - 1, z), "the Treasury has a floor at " + x + "," + z);
            int[] mid = world(c[0], c[1], 0, DAIS_TOP + 1, -3);
            assertTrue(mid[0] >= t[0] && mid[0] <= t[3] && mid[2] >= t[2] && mid[2] <= t[5] && mid[1] >= t[1] && mid[1] <= t[4]);
            int[] vm = world(c[0], c[1], 0, DAIS_TOP + 2, 7);
            assertTrue(vm[0] >= v[0] && vm[0] <= v[3] && vm[2] >= v[2] && vm[2] <= v[5] && vm[1] >= v[1] && vm[1] <= v[4]);
            assertEquals(2 * TREAS_U + 1, Math.max(t[3] - t[0], t[5] - t[2]) + 1, "its long side is the width of its air");
            // the two rooms do not overlap
            boolean overlap = t[0] <= v[3] && t[3] >= v[0] && t[2] <= v[5] && t[5] >= v[2];
            assertFalse(overlap, "treasury and vestibule are different rooms");
        }
    }

    @Test
    void depthRisesTowardTheGate() {
        int cx = 3, cz = 2;
        int x = centreX(cx, cz), z = centreZ(cx, cz);
        assertEquals(1.0, UnderworldGatePlan.depth(cx, cz, x, z), 1e-9);
        assertEquals(0.0, UnderworldGatePlan.depth(cx, cz, x + R, z), 1e-9);
        assertEquals(0.0, UnderworldGatePlan.depth(cx, cz, x + 80, z), 1e-9);
        double last = -1;
        for (int d = R; d >= 0; d -= 4) {
            double v = UnderworldGatePlan.depth(cx, cz, x + d, z);
            assertTrue(v >= last);
            last = v;
        }
    }
}
