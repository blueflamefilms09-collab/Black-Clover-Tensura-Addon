package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.PocketSignText;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketSwitchRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the harness for the report "the hidden dungeons are one room, or there is no switch" (1.101.0, docs/labyrinth/pocket_fix.md), for all thirteen exclusive tombs and many seeds, with no game. For each
 * tomb it runs the layout generator and the plan, places the plan's fills on a voxel grid in the order the builder places them, and walks it: how many rooms there are and of which kinds, whether there is a
 * loop, which rooms the arrival reaches on foot with the seal shut and open, where the lever is, whether it hangs on a wall in the key room that the arrival reaches WITHOUT crossing the seal (so it can never
 * be behind the door it opens), whether the Sigil's chest and the signs stand where they can be read, and that the treasury has its own way out. And it REPRODUCES the defect: when the Dungeon Guard's
 * rule (a change that takes a held block away is refused) is applied to the same fills, every doorway stays a wall and the arrival reaches ONE room. It also scans the sources: every file of the rift code
 * that sets a block to air must run inside the guard's internal span.
 */
class PocketFixTest {
    private static final int SEEDS = 24;

    private static LayoutGen.Params params(PocketDef d) { return LayoutGen.Params.of(d); }

    /** The solid picture of the plan as placed in order; with {@code guardRefuses} an air fill over a solid cell is refused (the 1.99.0 defect). */
    private static boolean[] solid(PocketPlan plan, boolean sealOpen, boolean guardRefuses, int[] dims) {
        int x0 = plan.minX() - 3, z0 = plan.minZ() - 3, y0 = PocketPlan.Y0 - 6, w = plan.maxX() - plan.minX() + 7, d = plan.maxZ() - plan.minZ() + 7, h = plan.maxY() - y0 + 4;
        dims[0] = x0;
        dims[1] = y0;
        dims[2] = z0;
        dims[3] = w;
        dims[4] = h;
        dims[5] = d;
        boolean[] s = new boolean[w * h * d];
        for (PocketPlan.Fill f : plan.ops()) {
            if (f.mat() == PocketPlan.Mat.SEAL && sealOpen) continue;
            boolean put = f.mat() != PocketPlan.Mat.AIR && f.mat() != PocketPlan.Mat.LIQUID;
            for (int x = f.x0(); x <= f.x1(); x++)
                for (int y = f.y0(); y <= f.y1(); y++)
                    for (int z = f.z0(); z <= f.z1(); z++) {
                        int i = ((x - x0) * h + (y - y0)) * d + (z - z0);
                        if (guardRefuses && !put && s[i]) continue;
                        s[i] = put;
                    }
        }
        return s;
    }

    private static boolean[] flood(boolean[] solid, int[] dims, int sx, int sy, int sz) {
        int w = dims[3], h = dims[4], d = dims[5];
        boolean[] seen = new boolean[solid.length];
        int[] stack = new int[w * h * d];
        int sp = 0;
        int start = ((sx - dims[0]) * h + (sy - dims[1])) * d + (sz - dims[2]);
        stack[sp++] = start;
        seen[start] = true;
        int[] dx = {1, -1, 0, 0, 0, 0}, dy = {0, 0, 1, -1, 0, 0}, dz = {0, 0, 0, 0, 1, -1};
        while (sp > 0) {
            int c = stack[--sp];
            int z = c % d, y = (c / d) % h, x = c / (d * h);
            for (int k = 0; k < 6; k++) {
                int nx = x + dx[k], ny = y + dy[k], nz = z + dz[k];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h || nz < 0 || nz >= d) continue;
                int n = (nx * h + ny) * d + nz;
                if (solid[n] || seen[n]) continue;
                seen[n] = true;
                stack[sp++] = n;
            }
        }
        return seen;
    }

    private static boolean at(boolean[] grid, int[] dims, int x, int y, int z) { return grid[((x - dims[0]) * dims[4] + (y - dims[1])) * dims[5] + (z - dims[2])]; }

    /** The ids of the rooms the arrival reaches on foot (a room counts when its middle, or the cell beside a pillar in its middle, is reached). */
    private static Set<Integer> reached(PocketLayout l, PocketPlan plan, boolean sealOpen, boolean guardRefuses) {
        int[] dims = new int[6];
        boolean[] solid = solid(plan, sealOpen, guardRefuses, dims);
        PocketPlan.Feature a = plan.first(PocketPlan.Type.ARRIVAL);
        boolean[] seen = flood(solid, dims, a.x(), a.y(), a.z());
        Set<Integer> out = new TreeSet<>();
        int g = l.grid(), y = PocketPlan.Y0 + 1;
        for (PocketLayout.Room r : l.rooms()) {
            int x = r.cx(g), z = r.cz(g);
            boolean ok = at(solid, dims, x, y, z) ? at(seen, dims, x + 1, y + 1, z + 1) : at(seen, dims, x, y, z);
            if (ok) out.add(r.id());
        }
        return out;
    }

    private static Map<String, PocketDef> defs() throws IOException { return PocketDataTest.defs(); }

    @Test
    void everyTombIsManyRoomsWithALoopAndTheRoomsTheOwnerAskedFor() throws IOException {
        assertTrue(defs().size() >= 13, "thirteen tombs");
        StringBuilder table = new StringBuilder();
        for (PocketDef d : defs().values()) {
            int minRooms = Integer.MAX_VALUE, maxRooms = 0, minKinds = Integer.MAX_VALUE;
            for (long s = 0; s < SEEDS; s++) {
                PocketLayout l = LayoutGen.generate(s * 7919L + 11, params(d));
                int n = l.rooms().size();
                minRooms = Math.min(minRooms, n);
                maxRooms = Math.max(maxRooms, n);
                assertTrue(n >= 8, d.id() + " seed " + s + ": only " + n + " rooms");
                EnumSet<PocketLayout.Kind> kinds = EnumSet.noneOf(PocketLayout.Kind.class);
                for (PocketLayout.Room r : l.rooms()) kinds.add(r.kind());
                minKinds = Math.min(minKinds, kinds.size());
                assertTrue(kinds.containsAll(EnumSet.of(PocketLayout.Kind.ENTRY, PocketLayout.Kind.BOSS, PocketLayout.Kind.TREASURY, PocketLayout.Kind.KEY)), d.id() + ": entry hall, lair, key room and treasury");
                assertTrue(kinds.size() >= 5, d.id() + " seed " + s + ": only the kinds " + kinds);
                assertTrue(l.count(PocketLayout.Kind.TRAP) >= 1, d.id() + ": a trap room");
                assertTrue(l.count(PocketLayout.Kind.COMBAT) + l.count(PocketLayout.Kind.RUIN) >= 1, d.id() + ": a guard or fighting room");
                if (d.gravity().inverted() > 0) assertTrue(l.count(PocketLayout.Kind.INVERTED) >= d.gravity().inverted(), d.id() + ": the inverted-gravity room");
                int open = 0;
                for (PocketLayout.Door door : l.doors()) if (!door.sealed()) open++;
                assertTrue(open - (n - 2) >= 1, d.id() + " seed " + s + ": no loop (" + open + " open doors for " + n + " rooms)");
                assertEquals(1, l.doors().stream().filter(PocketLayout.Door::sealed).count());
            }
            table.append(String.format("  %-20s rooms %d..%d (>= %d kinds), grid %d%n", d.id(), minRooms, maxRooms, minKinds, d.layout().grid()));
        }
        System.out.print("tombs and rooms:\n" + table);
    }

    /** The harness's readable output (run the class and read the console): for each tomb and seed 1, every room with its centre, every door and what it leads to, and where the switch, the Sigil's chest and the signs stand. */
    @Test
    void printTheRoomsDoorsAndSwitchOfEveryTomb() throws IOException {
        StringBuilder out = new StringBuilder();
        for (PocketDef d : defs().values()) {
            PocketLayout l = LayoutGen.generate(1L, params(d));
            PocketPlan plan = PocketPlan.of(l);
            out.append(d.id()).append(": ").append(l.rooms().size()).append(" rooms, ").append(l.doors().size()).append(" doors, ").append(plan.ops().size()).append(" fills, ").append(plan.volume()).append(" block placements\n");
            for (PocketLayout.Room r : l.rooms()) {
                StringBuilder doors = new StringBuilder();
                for (PocketLayout.Door door : l.doors()) if (door.a() == r.id() || door.b() == r.id()) doors.append(' ').append(door.a() == r.id() ? door.b() : door.a()).append(door.sealed() ? "(SEALED)" : "");
                out.append(String.format("    room %2d %-8s at (%4d,%4d) half %2d  doors to%s%n", r.id(), r.kind(), r.cx(l.grid()), r.cz(l.grid()), r.half(), doors));
            }
            PocketPlan.Feature lever = plan.first(PocketPlan.Type.SWITCH), chest = plan.first(PocketPlan.Type.SIGIL_CHEST), exit2 = plan.first(PocketPlan.Type.EXIT_SHORTCUT);
            out.append(String.format("    SWITCH (lever) at (%d,%d,%d) facing %s in room %d (key room %d); Sigil chest (%d,%d,%d); treasury exit (%d,%d,%d); %d signs%n", lever.x(), lever.y(), lever.z(), lever.tag(),
                    l.roomAt(lever.x() + 0.5, lever.z() + 0.5), l.keyId(), chest.x(), chest.y(), chest.z(), exit2.x(), exit2.y(), exit2.z(), plan.all(PocketPlan.Type.SIGN).size()));
        }
        System.out.print(out);
        assertTrue(out.length() > 1000);
    }

    @Test
    void theArrivalReachesEveryRoomButTheTreasuryUntilTheSealOpens() throws IOException {
        for (PocketDef d : defs().values())
            for (long s = 0; s < SEEDS; s++) {
                PocketLayout l = LayoutGen.generate(s * 31L + 5, params(d));
                PocketPlan plan = PocketPlan.of(l);
                Set<Integer> shut = reached(l, plan, false, false), open = reached(l, plan, true, false);
                Set<Integer> all = new TreeSet<>();
                for (PocketLayout.Room r : l.rooms()) all.add(r.id());
                Set<Integer> notTreasury = new TreeSet<>(all);
                notTreasury.remove(l.treasuryId());
                assertEquals(notTreasury, shut, d.id() + " seed " + s + ": with the seal shut");
                assertEquals(all, open, d.id() + " seed " + s + ": with the seal open");
            }
    }

    @Test
    void whenTheGuardRefusesTheCarvingEveryTombIsOneRoom() throws IOException {
        // the defect of 1.99.0 and 1.100.0 on paper: the doorways are air over the walls, the guard refuses them, and the delver stands in a closed hall
        int checked = 0;
        for (PocketDef d : defs().values())
            for (long s = 0; s < 6; s++) {
                PocketLayout l = LayoutGen.generate(s * 101L + 9, params(d));
                PocketPlan plan = PocketPlan.of(l);
                assertEquals(Set.of(l.entryId()), reached(l, plan, false, true), d.id() + ": ONLY ONE ROOM");
                assertEquals(Set.of(l.entryId()), reached(l, plan, true, true), d.id() + ": the seal being open would not change it");
                checked++;
            }
        assertTrue(checked >= 78);
    }

    @Test
    void theLeverHangsOnAWallOfTheKeyRoomAndCanBeReachedWithTheSealShut() throws IOException {
        for (PocketDef d : defs().values())
            for (long s = 0; s < SEEDS; s++) {
                PocketLayout l = LayoutGen.generate(s * 13L + 3, params(d));
                PocketPlan plan = PocketPlan.of(l);
                List<PocketPlan.Feature> levers = plan.all(PocketPlan.Type.SWITCH);
                assertEquals(1, levers.size(), d.id() + ": exactly one seal lever");
                PocketPlan.Feature lever = levers.get(0);
                String where = d.id() + " seed " + s + " lever " + lever;
                assertEquals(l.keyId(), l.roomAt(lever.x() + 0.5, lever.z() + 0.5), where + ": in the key room");
                assertNotEquals(l.treasuryId(), l.roomAt(lever.x() + 0.5, lever.z() + 0.5));
                int[] dims = new int[6];
                boolean[] solid = solid(plan, false, false, dims);
                int bx = lever.x(), bz = lever.z(), fx = 0, fz = 0;
                switch (lever.tag().charAt(0)) {
                    case 'N' -> fz = -1;
                    case 'S' -> fz = 1;
                    case 'W' -> fx = -1;
                    default -> fx = 1;
                }
                assertFalse(at(solid, dims, lever.x(), lever.y(), lever.z()), where + ": the lever's own cell is air");
                assertTrue(at(solid, dims, lever.x() - fx, lever.y(), lever.z() - fz), where + ": a wall behind the lever");
                // the cell in front of it (where the delver stands) is reached from the arrival with the seal SHUT: it is never behind the door it opens
                boolean[] seen = flood(solid, dims, plan.first(PocketPlan.Type.ARRIVAL).x(), plan.first(PocketPlan.Type.ARRIVAL).y(), plan.first(PocketPlan.Type.ARRIVAL).z());
                assertTrue(at(seen, dims, lever.x() + fx, PocketPlan.Y0, lever.z() + fz), where + ": the cell in front of the lever is reached with the seal shut");
                // not in a doorway, not in the wall of the seal
                for (PocketLayout.Door door : l.doors())
                    for (int room : new int[]{door.a(), door.b()}) {
                        int[] w = plan.doorway(door, room);
                        assertFalse(lever.x() >= w[0] && lever.x() <= w[3] && lever.y() >= w[1] && lever.y() <= w[4] && lever.z() >= w[2] && lever.z() <= w[5], where + ": in a doorway");
                    }
                // lit: lantern each side, a sign over it; the sign has a wall behind it
                long lanterns = plan.all(PocketPlan.Type.LANTERN).stream().filter(f -> Math.abs(f.x() - lever.x()) + Math.abs(f.z() - lever.z()) == 2).count();
                assertEquals(2, lanterns, where + ": two lanterns beside the lever");
                assertTrue(plan.all(PocketPlan.Type.SIGN).stream().anyMatch(f -> f.x() == lever.x() && f.z() == lever.z() && f.y() == lever.y() + 1 && f.tag().endsWith(":tomb_lever")), where + ": a sign over it");
                for (PocketPlan.Feature sign : plan.all(PocketPlan.Type.SIGN)) {
                    int sx = 0, sz = 0;
                    switch (sign.tag().charAt(0)) {
                        case 'N' -> sz = -1;
                        case 'S' -> sz = 1;
                        case 'W' -> sx = -1;
                        default -> sx = 1;
                    }
                    assertTrue(at(solid, dims, sign.x() - sx, sign.y(), sign.z() - sz), d.id() + " seed " + s + ": a wall behind the sign " + sign);
                    assertFalse(at(solid, dims, sign.x(), sign.y(), sign.z()), d.id() + ": the sign's own cell is air");
                }
            }
    }

    private static void assertNotEquals(int a, int b) { org.junit.jupiter.api.Assertions.assertNotEquals(a, b); }

    @Test
    void theSigilIsInAChestOfTheKeyRoomAndTheSealHasASignInTheLair() throws IOException {
        for (PocketDef d : defs().values())
            for (long s = 0; s < SEEDS; s++) {
                PocketLayout l = LayoutGen.generate(s * 17L + 1, params(d));
                PocketPlan plan = PocketPlan.of(l);
                PocketPlan.Feature chest = plan.first(PocketPlan.Type.SIGIL_CHEST);
                assertNotNull(chest, d.id());
                assertEquals(l.keyId(), l.roomAt(chest.x() + 0.5, chest.z() + 0.5), d.id() + ": the Sigil's chest is in the key room");
                int[] dims = new int[6];
                boolean[] solid = solid(plan, false, false, dims);
                boolean[] seen = flood(solid, dims, plan.first(PocketPlan.Type.ARRIVAL).x(), plan.first(PocketPlan.Type.ARRIVAL).y(), plan.first(PocketPlan.Type.ARRIVAL).z());
                assertTrue(at(seen, dims, chest.x(), chest.y(), chest.z()), d.id() + ": the chest's cell is reached with the seal shut");
                long seal = plan.all(PocketPlan.Type.SIGN).stream().filter(f -> f.tag().endsWith(":seal")).count();
                assertEquals(2, seal, d.id() + ": two signs on the lair's wall by the seal");
                for (PocketPlan.Feature f : plan.all(PocketPlan.Type.SIGN))
                    if (f.tag().endsWith(":seal")) assertEquals(l.bossId(), l.roomAt(f.x() + 0.5, f.z() + 0.5), d.id() + ": the seal's sign is inside the lair");
            }
    }

    @Test
    void theTreasuryHasItsOwnWayOutAndTheSealFillsBothLayers() throws IOException {
        for (PocketDef d : defs().values())
            for (long s = 0; s < SEEDS; s++) {
                PocketLayout l = LayoutGen.generate(s * 23L + 2, params(d));
                PocketPlan plan = PocketPlan.of(l);
                PocketPlan.Feature exit = plan.first(PocketPlan.Type.EXIT_SHORTCUT);
                assertNotNull(exit, d.id());
                assertEquals(l.treasuryId(), l.roomAt(exit.x() + 0.5, exit.z() + 0.5), d.id() + ": the second exit is in the treasury");
                int[] dims = new int[6];
                boolean[] solid = solid(plan, true, false, dims);
                boolean[] seen = flood(solid, dims, plan.first(PocketPlan.Type.ARRIVAL).x(), plan.first(PocketPlan.Type.ARRIVAL).y(), plan.first(PocketPlan.Type.ARRIVAL).z());
                assertTrue(at(seen, dims, exit.x(), exit.y(), exit.z()), d.id() + ": with the seal open the treasury exit is reached");
                // both layers of the seal stand in the shut tomb (the doorway of the lair's wall is not laid over the first)
                boolean[] shut = solid(plan, false, false, dims);
                int standing = 0;
                for (PocketPlan.Feature f : plan.all(PocketPlan.Type.SEAL_BLOCK)) if (at(shut, dims, f.x(), f.y(), f.z())) standing++;
                assertEquals(60, standing, d.id() + " seed " + s + ": sixty seal blocks stand");
            }
    }

    @Test
    void everyFeatureStandsInsideItsRoomAndNotInAWall() throws IOException {
        for (PocketDef d : defs().values())
            for (long s = 0; s < 8; s++) {
                PocketLayout l = LayoutGen.generate(s * 29L + 6, params(d));
                PocketPlan plan = PocketPlan.of(l);
                int[] dims = new int[6];
                boolean[] solid = solid(plan, false, false, dims);
                for (PocketPlan.Type t : new PocketPlan.Type[]{PocketPlan.Type.SWITCH, PocketPlan.Type.LANTERN, PocketPlan.Type.SIGIL_CHEST, PocketPlan.Type.EXIT_SHORTCUT, PocketPlan.Type.SIGN})
                    for (PocketPlan.Feature f : plan.all(t)) {
                        assertFalse(at(solid, dims, f.x(), f.y(), f.z()), d.id() + " seed " + s + ": " + f + " is inside a block");
                        assertTrue(l.roomAt(f.x() + 0.5, f.z() + 0.5) >= 0, d.id() + ": " + f + " is in a room");
                        if (t == PocketPlan.Type.LANTERN || t == PocketPlan.Type.SIGIL_CHEST || t == PocketPlan.Type.EXIT_SHORTCUT) assertTrue(at(solid, dims, f.x(), f.y() - 1, f.z()), d.id() + ": " + f + " stands on a floor");
                    }
            }
    }

    @Test
    void theSignsFitTheBoardAndSayWhatOpensTheDoor() {
        for (String key : PocketSignText.KEYS) {
            String[] lines = PocketSignText.lines(key);
            assertEquals(4, lines.length, key);
            for (String line : lines) assertTrue(line.length() <= PocketSignText.MAX_LINE, key + ": '" + line + "' is too long for a sign");
        }
        assertTrue(String.join(" ", PocketSignText.lines("seal")).toLowerCase().contains("lever"), "the seal's sign names the lever");
        assertTrue(String.join(" ", PocketSignText.lines("vault")).toLowerCase().contains("lever"), "the vault's sign names the lever");
    }

    @Test
    void theWatchdogProbesOnlyABuiltTombWithPeopleInIt() {
        assertFalse(PocketSwitchRules.watchdogDue(1000, 0, 0, true), "not while forming");
        assertFalse(PocketSwitchRules.watchdogDue(1000, 0, 2, true), "not while collapsing");
        assertFalse(PocketSwitchRules.watchdogDue(1000, 0, 1, false), "not when empty");
        assertFalse(PocketSwitchRules.watchdogDue(1050, 1000, 1, true), "not twice in an interval");
        assertTrue(PocketSwitchRules.watchdogDue(1100, 1000, 1, true));
    }

    @Test
    void everyFileThatSetsAirInTheRiftCodeRunsInsideTheGuardsInternalSpan() throws IOException {
        // the regression guard for the root cause: a source that places air must name the Dungeon Guard
        Path root = Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/rift");
        List<String> bad = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path f : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = f.getFileName().toString();
                if (name.endsWith("SelfTests.java")) continue;                      // the tests' own scenery is wrapped where it matters; they are the guard's customers, not builders
                String text = Files.readString(f);
                boolean air = text.contains("Blocks.AIR") || text.contains("Mat.AIR") || text.contains("AIR =");
                boolean sets = text.contains(".setBlock(");
                if (air && sets && !text.contains("DungeonGuard")) bad.add(name);
            }
        }
        assertTrue(bad.isEmpty(), "these files place air outside the Dungeon Guard's internal span: " + bad);
        for (String must : new String[]{"PocketBuilder.java", "pocket/PocketBuild.java", "pocket/PocketClear.java", "pocket/PocketCollapse.java", "pocket/ExclusivePockets.java", "RiftService.java"})
            assertTrue(Files.readString(root.resolve(must)).contains("DungeonGuard"), must + " must name the guard");
    }
}
