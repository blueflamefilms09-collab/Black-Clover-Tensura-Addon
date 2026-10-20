package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Element;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Placed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: the promises a hall makes are checkable from its plan alone, and the same checks must run in three places (the JUnit tests, the in-game self-test that builds every hall in the real world, and
 * the preview script's sanity pass), so they live here once, as one function that returns the problems in plain sentences (an empty list is a good hall). The checks are: nothing is written outside
 * the footprint; the boundary wall is closed all the way round (two blocks thick, up to the roof) except at the two doorways the floor's template keeps; the ceiling is closed; the floor is under every
 * interior column; every interactive element lies inside the hall, has cells and is not inside the boss's clearing; every hazard zone and add spot is inside the hall; a walk from the entry reaches the
 * foot of the dais, the front of the Gatekeeper (once the locked gate is open) and most of the floor; and the boss's clearing (radius 6 about the altar) holds nothing but the dais.
 */
public final class ArenaChecks {
    private ArenaChecks() {}

    /** All problems of a plan, in plain sentences; empty when the hall is sound. */
    public static List<String> problems(BossArenaStructure a) {
        List<String> out = new ArrayList<>();
        int size = a.size, c = a.c, last = size - 1;
        if (a.outOfBounds() > 0) out.add(a.theme + ": " + a.outOfBounds() + " writes fell outside the footprint");

        // the boundary wall: closed everywhere except the doorways
        int holes = 0;
        for (int layer = 0; layer < 2 && holes < 5; layer++) {
            for (int i = layer; i <= last - layer; i++) {
                for (int y = 1; y <= a.height - 1; y++) {
                    holes += wallHole(a, i, y, layer) ? 1 : 0;
                    holes += wallHole(a, i, y, last - layer) ? 1 : 0;
                    holes += wallHole(a, layer, y, i) ? 1 : 0;
                    holes += wallHole(a, last - layer, y, i) ? 1 : 0;
                }
            }
        }
        if (holes > 0) out.add(a.theme + ": the boundary wall has holes (" + holes + " found, first checked)");

        // the ceiling and the floor
        int open = 0, nofloor = 0;
        for (int z = 0; z < size; z++)
            for (int x = 0; x < size; x++) {
                if (!a.solidAt(x, a.height - 1, z)) open++;
                if (z >= 2 && z <= last - 2 && x >= 2 && x <= last - 2 && !hasFloor(a, x, z)) nofloor++;
            }
        if (open > 0) out.add(a.theme + ": the ceiling is open in " + open + " columns");
        if (nofloor > 0) out.add(a.theme + ": " + nofloor + " interior columns have no floor");

        // the doorways: reserved, on the middle column, on both walls
        for (int y = 1; y <= 4; y++)
            for (int dx = -1; dx <= 1; dx++) {
                if (!a.isReserved(c + dx, y, 0) || !a.isReserved(c + dx, y, 1) || !a.isReserved(c + dx, y, last) || !a.isReserved(c + dx, y, last - 1)) {
                    out.add(a.theme + ": the doorway cells at x=" + (c + dx) + " y=" + y + " are not reserved");
                    y = 5;
                    break;
                }
            }

        // elements
        if (a.elements().isEmpty()) out.add(a.theme + ": the hall has no interactive elements");
        for (Element e : a.elements()) {
            if (e.cells.isEmpty()) out.add(a.theme + ": element " + e.id + " has no cells");
            for (Placed p : e.cells) {
                if (p.x() < 2 || p.x() > last - 2 || p.z() < 2 || p.z() > last - 2 || p.y() < 0 || p.y() >= a.height - 1) {
                    out.add(a.theme + ": element " + e.id + " (" + e.kind + ") has a cell outside the room at " + p.x() + "," + p.y() + "," + p.z());
                    break;
                }
                if (!a.get(p.x(), p.y(), p.z()).equals(p.spec())) {
                    out.add(a.theme + ": element " + e.id + " (" + e.kind + ") was overwritten at " + p.x() + "," + p.y() + "," + p.z() + " (" + a.get(p.x(), p.y(), p.z()) + " over " + p.spec() + ")");
                    break;
                }
            }
            if (e.effect == BossArenaStructure.Effect.ADDS && e.adds <= 0) out.add(a.theme + ": element " + e.id + " should spawn adds but says 0");
            if (e.effect == BossArenaStructure.Effect.TOPPLE && e.strip.length != 4) out.add(a.theme + ": pillar " + e.id + " has no fall strip");
            double d = Math.hypot(e.x() - c, e.z() - c);
            if (d < 6.5 && !e.locked) out.add(a.theme + ": element " + e.id + " (" + e.kind + ") stands inside the boss's clearing");
        }
        for (BossArenaStructure.Zone z : a.zones()) if (z.x() < 2 || z.x() > last - 2 || z.z() < 2 || z.z() > last - 2) out.add(a.theme + ": zone " + z.kind() + " lies outside the room");
        for (BossArenaStructure.Spot s : a.spots()) if (s.x() < 2 || s.x() > last - 2 || s.z() < 2 || s.z() > last - 2) out.add(a.theme + ": add spot " + s.x() + "," + s.z() + " lies outside the room");
        if (a.spots().size() < 4) out.add(a.theme + ": fewer than four add spots (" + a.spots().size() + ")");
        if (a.zones().size() < 2) out.add(a.theme + ": fewer than two hazard zones");

        // the clearing round the altar: nothing but the dais (rows 1 and 2) within radius 6
        int clutter = 0;
        for (int z = c - 6; z <= c + 6; z++)
            for (int x = c - 6; x <= c + 6; x++) {
                if (Math.hypot(x - c, z - c) > 6) continue;
                for (int y = 3; y <= 8; y++) if ((x != c || z != c) && a.solidAt(x, y, z)) clutter++;
            }
        if (clutter > 0) out.add(a.theme + ": " + clutter + " solid cells stand in the clearing round the altar");

        // walks
        WalkResult w = walk(a, false);
        if (!w.reachedDais) out.add(a.theme + ": the dais cannot be reached from the entry");
        WalkResult open2 = walk(a, true);
        if (!open2.reachedGate) out.add(a.theme + ": the Gatekeeper cannot be reached from the entry once the gate is open");
        if (w.walkable > 0 && w.reached < 0.55 * w.walkable) out.add(a.theme + ": only " + w.reached + " of " + w.walkable + " walkable columns can be reached from the entry");
        return out;
    }

    private static boolean wallHole(BossArenaStructure a, int x, int y, int z) {
        if (a.isReserved(x, y, z)) return false;
        return !a.solidAt(x, y, z);
    }

    private static boolean hasFloor(BossArenaStructure a, int x, int z) {
        for (int y = 0; y >= -BossArenaStructure.FOUNDATION; y--) if (a.solidAt(x, y, z) || isPoolBottom(a, x, y, z)) return true;
        return false;
    }

    private static boolean isPoolBottom(BossArenaStructure a, int x, int y, int z) {
        String s = a.get(x, y, z);
        return s != null && Solidity.kind(s) == Solidity.Kind.LIQUID && a.solidAt(x, y - 1, z);
    }

    // ================================================================ walking
    /** The outcome of a walk from the entry: columns reached, columns that could be walked at all, and whether the dais and the Gatekeeper's front were reached. */
    public record WalkResult(int reached, int walkable, boolean reachedDais, boolean reachedGate) {}

    /** The highest y in [lo, hi] where a body can stand in the column (a solid block under it and two open cells over), or Integer.MIN_VALUE. {@code openGate} treats locked elements as removed. */
    static int standY(BossArenaStructure a, int x, int z, int lo, int hi, boolean openGate) {
        for (int y = hi; y >= lo; y--) {
            String below = cell(a, x, y, z, openGate);
            if (!Solidity.ground(below)) continue;
            if (Solidity.open(cell(a, x, y + 1, z, openGate)) && Solidity.open(cell(a, x, y + 2, z, openGate))) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static String cell(BossArenaStructure a, int x, int y, int z, boolean openGate) {
        String s = a.get(x, y, z);
        if (s == null) return y < -BossArenaStructure.FOUNDATION ? "minecraft:stone" : null;
        if (openGate) {
            Element e = a.elementAt(x, y, z);
            if (e != null && e.locked) return null;
        }
        return s;
    }

    public static WalkResult walk(BossArenaStructure a, boolean openGate) {
        int size = a.size, c = a.c;
        int[] start = a.entry();
        int sy = standY(a, start[0], start[1], -3, 3, openGate);
        if (sy == Integer.MIN_VALUE) return new WalkResult(0, 0, false, false);
        boolean[][] seen = new boolean[size][size];
        int[][] yAt = new int[size][size];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[] {start[0], start[1], sy});
        seen[start[0]][start[1]] = true;
        yAt[start[0]][start[1]] = sy;
        int reached = 0;
        boolean dais = false, gate = false;
        int gateZ = 2;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            reached++;
            if (Math.hypot(cur[0] - c, cur[1] - c) <= 9.5 && cur[2] >= 1) dais = true;
            if (cur[1] <= gateZ + 3 && Math.abs(cur[0] - c) <= 1) gate = true;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = cur[0] + d[0], nz = cur[1] + d[1];
                if (nx < 2 || nz < 2 || nx > size - 3 || nz > size - 3 || seen[nx][nz]) continue;
                int ny = standY(a, nx, nz, cur[2] - 3, cur[2] + 1, openGate);
                if (ny == Integer.MIN_VALUE) continue;
                seen[nx][nz] = true;
                yAt[nx][nz] = ny;
                q.add(new int[] {nx, nz, ny});
            }
        }
        int walkable = 0;
        for (int z = 2; z <= size - 3; z++) for (int x = 2; x <= size - 3; x++) if (standY(a, x, z, -3, 4, openGate) != Integer.MIN_VALUE) walkable++;
        return new WalkResult(reached, walkable, dais, gate);
    }
}
