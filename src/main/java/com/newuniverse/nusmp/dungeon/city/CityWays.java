package com.newuniverse.nusmp.dungeon.city;

import java.util.ArrayList;
import java.util.List;

/**
 * The ways into a Golden City. A city is a sealed vault in a cave world that is flooded below sea level, and the Dungeon's stone may not be
 * dug (DungeonConfig.UNBREAKABLE), so a city that cannot be entered would be a city nobody can use. After a city is built, the builder
 * looks round its outer wall for the places where a cave comes close, and there it opens a passage one block wide and two high through the
 * wall and the shell (and through the rock between them and the cave, when the cave is a few blocks off) that ends in an open iron door:
 * a door cell cannot hold water, so the flooded caves stay out while a swimmer can come in. This class only chooses the passages. It is
 * pure: the world is reached through a {@link Probe}.
 */
final class CityWays {
    private CityWays() {}

    /** What a probe says of a cell: rock (anything that is not air or water), a cave (air or water), or not loaded. */
    static final int SOLID = 0, OPEN = 1, UNKNOWN = 2;
    /** The most rock that may be dug between the shell and a cave, the most ways a city gets, and how far apart (blocks, along both axes) two doors must be. */
    static final int REACH = 12, MAX = 3, APART = 16;
    /** A cave must have at least this many open cells in the 3 x 3 x 4 block behind the last cell of a passage, so a one-cell pocket does not count. */
    static final int MIN_CAVE = 10;

    /** The world, read at one cell. */
    interface Probe { int at(int x, int y, int z); }

    /**
     * One passage: a straight line of cells from the first cell of the wall (fromX, fromZ) outward in the direction (dx, dz) to the last cell
     * to be opened (lastX, lastZ), two cells high from the walking surface up. The door stands in the outermost cell of the district
     * (doorX, doorZ); the cells beyond it, {@code dug} of them, are rock cut to meet the cave. The score ranks ways: short digs first.
     */
    record Way(int dx, int dz, int fromX, int fromZ, int doorX, int doorZ, int lastX, int lastZ, int dug, int score) {
        /** The number of cells on the line, from the first to the last. */
        int length() { return Math.abs(lastX - fromX) + Math.abs(lastZ - fromZ) + 1; }
    }

    /** The ways to open in a built city, best first, at most {@link #MAX}, each at least {@link #APART} from the others. Deterministic. */
    static List<Way> choose(CityPlan p, Probe probe) {
        List<Way> found = new ArrayList<>();
        for (int axis = 0; axis < 2; axis++) {
            int a = axis == 0 ? p.semiX : p.semiZ, b = axis == 0 ? p.semiZ : p.semiX;
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int w = -(b - 14); w <= b - 14; w++) {
                    if (Math.abs(w) <= 6) continue;                              // the avenues, the gates and the arches keep their ends
                    Way way = candidate(p, probe, axis, sign, w, a, b);
                    if (way != null) found.add(way);
                }
            }
        }
        found.sort((x, y) -> Integer.compare(y.score(), x.score()));          // stable: equal scores keep their scan order
        List<Way> chosen = new ArrayList<>();
        for (Way way : found) {
            if (chosen.size() >= MAX) break;
            boolean apart = true;
            for (Way other : chosen) {
                if (Math.abs(way.doorX() - other.doorX()) + Math.abs(way.doorZ() - other.doorZ()) < APART) apart = false;
            }
            if (apart) chosen.add(way);
        }
        return chosen;
    }

    /**
     * The line of the row w (an offset across the district) outward from the middle, along x (axis 0) or z (axis 1), on the side `sign`. Null
     * unless it can be made a way: the wall and shell on it must be walled in at its sides, the city must be open just inside, and a cave
     * must lie within reach outside.
     */
    private static Way candidate(CityPlan p, Probe probe, int axis, int sign, int w, int a, int b) {
        int ext = CityPlan.extent(w, a, b);                  // the outermost cell of the district on this row (the shell's outer face)
        int shellIn = CityPlan.extent(w, a - 2, b - 2);      // the last cell of the vault inside the shell
        int in = CityPlan.extent(w, a - 5, b - 5);           // the last cell inside the wall, where the city proper begins
        if (ext < 0 || shellIn < 0 || in < 0 || ext - in > 8) return null;
        for (int u = in + 1; u < ext; u++) {                 // beside the passage there must be stone of the city, not the cave outside
            if (!CityPlan.inside(u, w - 1, a, b) || !CityPlan.inside(u, w + 1, a, b)) return null;
        }
        int y1 = p.floorY + 1, y2 = p.floorY + 2;
        for (int u = in - 1; u <= in; u++) {                 // the city is open just inside the wall
            if (probe.at(x(p, axis, sign, u, w), y1, z(p, axis, sign, u, w)) != OPEN || probe.at(x(p, axis, sign, u, w), y2, z(p, axis, sign, u, w)) != OPEN) return null;
        }
        int dug = -1;
        for (int t = 0; t <= REACH; t++) {
            int u = ext + 1 + t, px = x(p, axis, sign, u, w), pz = z(p, axis, sign, u, w);
            int low = probe.at(px, y1, pz), high = probe.at(px, y2, pz);
            if (low == UNKNOWN || high == UNKNOWN) return null;
            if (low == OPEN && high == OPEN) {
                dug = t;
                break;
            }
        }
        if (dug < 0) return null;
        int open = 0;
        for (int u = ext + 1 + dug; u <= ext + 3 + dug; u++) {
            for (int v = w - 1; v <= w + 1; v++) {
                for (int y = y1 - 1; y <= y2 + 1; y++) if (probe.at(x(p, axis, sign, u, v), y, z(p, axis, sign, u, v)) == OPEN) open++;
            }
        }
        if (open < MIN_CAVE) return null;
        int last = ext + dug;
        int dx = axis == 0 ? sign : 0, dz = axis == 0 ? 0 : sign;
        return new Way(dx, dz, x(p, axis, sign, in + 1, w), z(p, axis, sign, in + 1, w), x(p, axis, sign, ext, w), z(p, axis, sign, ext, w),
                x(p, axis, sign, last, w), z(p, axis, sign, last, w), dug, (REACH - dug) * 64 + open);
    }

    private static int x(CityPlan p, int axis, int sign, int u, int w) { return p.centreX + (axis == 0 ? sign * u : w); }

    private static int z(CityPlan p, int axis, int sign, int u, int w) { return p.centreZ + (axis == 0 ? w : sign * u); }
}
