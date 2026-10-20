package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the plan is what the builder places, so its numbers are the server's cost and its features are the game: a tomb must stay inside its 192 block cell and the dimension's height, cost
 * tens of thousands of blocks and not hundreds of thousands (the builder spreads them over ticks, but it is still a bound), carve exactly two doorways per door, put exactly one seal of 60
 * blocks in the lair's door, one primary pedestal, one exit rift well away from the arrival, and the gravity rules in exactly the boxes the layout says: an inverted room and the tall corridor
 * up to (but not including) the landing tile, a weightless room, and no gravity anywhere else.
 */
class PocketPlanTest {
    private static LayoutGen.Params params() { return new LayoutGen.Params(7, 20, 0.35, 10, 2, 1, 1, List.of("fire", "plant", "frost")); }

    @Test
    void thePlanStaysInsideTheCellAndTheHeightAndCostsWhatItShould() {
        for (long s = 0; s < 150; s++) {
            PocketPlan plan = PocketPlan.of(LayoutGen.generate(s * 977L + 5, params()));
            assertTrue(plan.minX() >= -90 && plan.maxX() <= 90 && plan.minZ() >= -90 && plan.maxZ() <= 90, "inside the 192 block cell, seed " + s + ": " + plan.minX() + ".." + plan.maxX() + " / " + plan.minZ() + ".." + plan.maxZ());
            assertTrue(plan.maxY() <= PocketPlan.Y0 + 22, "height " + plan.maxY());
            long v = plan.volume();
            assertTrue(v > 6_000 && v < 90_000, "volume " + v + " for seed " + s);
            for (PocketPlan.Fill f : plan.ops()) assertTrue(f.x0() <= f.x1() && f.y0() <= f.y1() && f.z0() <= f.z1() && f.y0() >= PocketPlan.Y0 - 4, "fill " + f);
        }
    }

    @Test
    void everyDoorHasTwoDoorwaysAndTheSealIsSixtyBlocks() {
        for (long s = 0; s < 60; s++) {
            PocketLayout l = LayoutGen.generate(s + 11, params());
            PocketPlan plan = PocketPlan.of(l);
            int air = 0, seal = 0, sealBlocks = 0;
            for (PocketPlan.Fill f : plan.ops()) {
                if (f.mat() == PocketPlan.Mat.AIR) air++;
                if (f.mat() == PocketPlan.Mat.SEAL) {
                    seal++;
                    assertEquals(30L, f.volume(), "one seal layer is 5 wide and 6 tall");
                }
            }
            for (PocketPlan.Feature f : plan.all(PocketPlan.Type.SEAL_BLOCK)) sealBlocks++;
            assertEquals(l.doors().size() * 2, air, "two doorways to a door");
            assertEquals(2, seal, "two layers of seal");
            assertEquals(60, sealBlocks, "sixty seal blocks");
            // the last fills are the doorways: nothing is opened before everything stands
            int firstAir = -1;
            for (int i = 0; i < plan.ops().size(); i++) if (plan.ops().get(i).mat() == PocketPlan.Mat.AIR) { firstAir = i; break; }
            for (int i = firstAir; i < firstAir + air; i++) assertEquals(PocketPlan.Mat.AIR, plan.ops().get(i).mat());
        }
    }

    @Test
    void theKeyFeaturesStandWhereTheyShould() {
        for (long s = 0; s < 120; s++) {
            PocketLayout l = LayoutGen.generate(s * 13 + 1, params());
            PocketPlan plan = PocketPlan.of(l);
            assertEquals(1, plan.all(PocketPlan.Type.EXIT_RIFT).size());
            assertEquals(1, plan.all(PocketPlan.Type.ARRIVAL).size());
            assertEquals(1, plan.all(PocketPlan.Type.BOSS_SPAWN).size());
            assertEquals(1, plan.all(PocketPlan.Type.KEY_SPAWN).size());
            assertEquals(1, plan.all(PocketPlan.Type.PEDESTAL_PRIMARY).size());
            assertEquals(1, plan.all(PocketPlan.Type.PEDESTAL_SECONDARY).size());
            assertEquals(3, plan.all(PocketPlan.Type.CHEST_TREASURE).size());
            PocketPlan.Feature exit = plan.first(PocketPlan.Type.EXIT_RIFT), arrive = plan.first(PocketPlan.Type.ARRIVAL);
            assertTrue(Math.hypot(exit.x() - arrive.x(), exit.z() - arrive.z()) >= 3.5, "the arrival is outside the exit tear");
            assertTrue(plan.box(l.entryId()).contains(exit.x() + 0.5, exit.y(), exit.z() + 0.5), "the exit rift is in the entry room");
            assertTrue(plan.box(l.entryId()).contains(arrive.x() + 0.5, arrive.y(), arrive.z() + 0.5));
            assertTrue(plan.box(l.bossId()).contains(plan.first(PocketPlan.Type.BOSS_SPAWN).x() + 0.5, PocketPlan.Y0, plan.first(PocketPlan.Type.BOSS_SPAWN).z() + 0.5));
            assertTrue(plan.box(l.keyId()).contains(plan.first(PocketPlan.Type.KEY_SPAWN).x() + 0.5, PocketPlan.Y0, plan.first(PocketPlan.Type.KEY_SPAWN).z() + 0.5));
            PocketPlan.RoomBox tb = plan.box(l.treasuryId());
            for (PocketPlan.Type t : new PocketPlan.Type[]{PocketPlan.Type.PEDESTAL_PRIMARY, PocketPlan.Type.PEDESTAL_SECONDARY, PocketPlan.Type.CHEST_TREASURE})
                for (PocketPlan.Feature f : plan.all(t)) assertTrue(tb.contains(f.x() + 0.5, f.y(), f.z() + 0.5), t + " " + f + " is in the treasury, seed " + s);
            int traps = 0;
            for (PocketLayout.Room r : l.rooms()) if (r.kind() == PocketLayout.Kind.TRAP) traps += 4;
            assertEquals(traps, plan.all(PocketPlan.Type.TRAP).size(), "four tiles to a trap room");
            for (PocketPlan.Feature f : plan.all(PocketPlan.Type.TRAP)) {
                assertFalse(f.tag().isEmpty(), "a trap tile names its element");
                assertTrue(PocketElement.byId(f.tag()) != null);
            }
            for (PocketPlan.RoomBox b : plan.boxes()) for (int[] spot : b.spots()) assertTrue(b.contains(spot[0] + 0.5, PocketPlan.Y0, spot[1] + 0.5), "a spot is inside its room");
        }
    }

    @Test
    void gravityHoldsOnlyWhereTheZonesAre() {
        for (long s = 0; s < 120; s++) {
            PocketLayout l = LayoutGen.generate(s * 7 + 2, params());
            PocketPlan plan = PocketPlan.of(l);
            int g = l.grid();
            for (PocketLayout.Room r : l.rooms()) {
                double cx = r.cx(g) + 0.5, cz = r.cz(g) + 0.5;
                PocketLayout.Kind at = plan.gravityAt(cx, PocketPlan.Y0 + 1, cz);
                if (r.kind() == PocketLayout.Kind.INVERTED) assertEquals(PocketLayout.Kind.INVERTED, at, "inside an inverted room");
                else if (r.kind() == PocketLayout.Kind.NEGATED) assertEquals(PocketLayout.Kind.NEGATED, at, "inside a weightless room");
                else assertNull(at, "no gravity rule in a " + r.kind() + " room, seed " + s);
                assertNull(plan.gravityAt(cx, PocketPlan.Y0 + 40, cz), "above the tomb");
            }
            for (PocketPlan.Feature f : plan.all(PocketPlan.Type.AMBIENT)) {
                if (!f.tag().equals("landing")) continue;
                assertNull(plan.gravityAt(f.x() + 0.5, PocketPlan.Y0 + 1, f.z() + 0.5), "the landing tile is outside the zone, seed " + s);
            }
            for (PocketPlan.Zone z : plan.zones()) assertTrue(z.x0() <= z.x1() && z.z0() <= z.z1() && z.y0() < z.y1());
        }
    }

    @Test
    void anInvertedRoomIsEnteredThroughACeilingDoorAndATallCorridor() {
        int checked = 0;
        for (long s = 0; s < 200 && checked < 40; s++) {
            PocketLayout l = LayoutGen.generate(s * 3 + 4, params());
            PocketPlan plan = PocketPlan.of(l);
            for (PocketLayout.Room r : l.rooms()) {
                if (r.kind() != PocketLayout.Kind.INVERTED) continue;
                checked++;
                assertEquals(14, r.height());
                int zones = 0;
                for (PocketPlan.Zone z : plan.zones()) if (z.room() == r.id()) zones++;
                assertEquals(1 + l.neighbours(r.id(), true).size(), zones, "the room and one corridor per door");
            }
        }
        assertTrue(checked > 0);
    }

    /** A voxel picture of the plan: true = a solid block. The seal can be left out (opened). */
    private static boolean[] voxels(PocketPlan plan, boolean sealOpen, int[] dims) {
        int x0 = plan.minX() - 3, z0 = plan.minZ() - 3, y0 = PocketPlan.Y0 - 6, w = plan.maxX() - plan.minX() + 7, d = plan.maxZ() - plan.minZ() + 7, h = plan.maxY() - y0 + 4;
        dims[0] = x0;
        dims[1] = y0;
        dims[2] = z0;
        dims[3] = w;
        dims[4] = h;
        dims[5] = d;
        boolean[] solid = new boolean[w * h * d];
        for (PocketPlan.Fill f : plan.ops()) {
            if (f.mat() == PocketPlan.Mat.SEAL && sealOpen) continue;
            boolean put = f.mat() != PocketPlan.Mat.AIR && f.mat() != PocketPlan.Mat.LIQUID;
            for (int x = f.x0(); x <= f.x1(); x++)
                for (int y = f.y0(); y <= f.y1(); y++)
                    for (int z = f.z0(); z <= f.z1(); z++) solid[((x - x0) * h + (y - y0)) * d + (z - z0)] = put;
        }
        return solid;
    }

    /** Flood fill of the air from a cell; returns {count reached, touchedTheOutside (0/1)}. */
    private static int[] flood(boolean[] solid, int[] dims, int sx, int sy, int sz, boolean[] seen) {
        int w = dims[3], h = dims[4], d = dims[5];
        int[] stack = new int[w * h * d];
        int sp = 0, count = 0, outside = 0;
        int start = ((sx - dims[0]) * h + (sy - dims[1])) * d + (sz - dims[2]);
        stack[sp++] = start;
        seen[start] = true;
        int[] dx = {1, -1, 0, 0, 0, 0}, dy = {0, 0, 1, -1, 0, 0}, dz = {0, 0, 0, 0, 1, -1};
        while (sp > 0) {
            int c = stack[--sp];
            count++;
            int z = c % d, y = (c / d) % h, x = c / (d * h);
            if (x == 0 || x == w - 1 || y == 0 || y == h - 1 || z == 0 || z == d - 1) outside = 1;
            for (int k = 0; k < 6; k++) {
                int nx = x + dx[k], ny = y + dy[k], nz = z + dz[k];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h || nz < 0 || nz >= d) continue;
                int n = (nx * h + ny) * d + nz;
                if (solid[n] || seen[n]) continue;
                seen[n] = true;
                stack[sp++] = n;
            }
        }
        return new int[]{count, outside};
    }

    @Test
    void theShellIsClosedAndEveryRoomIsConnectedExceptTheTreasuryUntilTheSealIsOpen() {
        for (long s = 0; s < 40; s++) {
            PocketLayout l = LayoutGen.generate(s * 41 + 7, params());
            PocketPlan plan = PocketPlan.of(l);
            PocketPlan.Feature arrive = plan.first(PocketPlan.Type.ARRIVAL);
            int[] dims = new int[6];
            for (int pass = 0; pass < 2; pass++) {
                boolean open = pass == 1;
                boolean[] solid = voxels(plan, open, dims);
                boolean[] seen = new boolean[solid.length];
                int[] res = flood(solid, dims, arrive.x(), arrive.y(), arrive.z(), seen);
                assertEquals(0, res[1], "the tomb leaks into the void (seal " + (open ? "open" : "shut") + "), seed " + s);
                int g = l.grid();
                for (PocketLayout.Room r : l.rooms()) {
                    int x = r.cx(g), z = r.cz(g), y = PocketPlan.Y0 + (r.kind() == PocketLayout.Kind.NEGATED ? 1 : 1);
                    int idx = (((x - dims[0]) * dims[4]) + (y - dims[1])) * dims[5] + (z - dims[2]);
                    boolean reached = seen[idx] || solid[idx];                         // a pillar or a tile in the very middle of a room does not count as a miss
                    if (solid[idx]) {
                        int probe = (((x + 1 - dims[0]) * dims[4]) + (y + 1 - dims[1])) * dims[5] + (z + 1 - dims[2]);
                        reached = seen[probe];
                    }
                    boolean want = r.kind() != PocketLayout.Kind.TREASURY || open;
                    assertEquals(want, reached, "room " + r.id() + " " + r.kind() + " reached=" + reached + " (seal " + (open ? "open" : "shut") + "), seed " + s);
                }
            }
        }
    }

    @Test
    void thePlanIsAFunctionOfTheLayout() {
        PocketLayout l = LayoutGen.generate(424242L, params());
        PocketPlan a = PocketPlan.of(l), b = PocketPlan.of(l);
        assertEquals(a.ops(), b.ops());
        assertEquals(a.features(), b.features());
        assertEquals(a.zones(), b.zones());
        assertNotNull(a.first(PocketPlan.Type.PEDESTAL_PRIMARY));
    }
}
