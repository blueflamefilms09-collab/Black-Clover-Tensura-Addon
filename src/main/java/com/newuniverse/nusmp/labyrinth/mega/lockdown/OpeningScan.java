package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.gen.Clearance7;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: a room can only be locked where it has a WALL. The rooms of the Mega-Dungeon are boxes with two wall layers, the doorways are the gaps in them, and the
 * lockdown fills a gap with a barrier; but not every room is a box (a cavern has no wall at all, a ruined hall may have lost half of one), and a "doorway" scan of
 * a cavern would find a hundred cells and seal a hole the size of a hillside, or worse, a hole with a way round. So the passable cells found in the room's inner
 * wall layer are grouped into openings (cells that touch edge to edge, in the wall's plane) and judged: there must be at least one opening, none may be bigger than a
 * doorway of the clearance rule could be, there may not be more of them than a room has sides to put them in, and the total is bounded. A room that fails is an
 * "open plan" room: it does not lock (the ambush director still watches it). The geometry is plain integers, so the judgement is a unit test. Pure.
 * <p>
 * 1.107.0 (integration with the 7 x 7 clearance, docs/labyrinth/clearance7.md): a doorway of a sector that was cut ({@code GenData.Stat.clear7}) is 7 wide and 7 high through BOTH
 * wall layers, its outermost columns lie one block from the corner of the room, and twelve of them are more cells than the old limits allowed. So for a cut room {@link #scan}
 * reads the wall lines from one block closer to the corner (the old inset of 2 missed the two outer columns of a doorway at the end of a wall and left a one-block gap on each side
 * of the barrier), looks at least 7 rows high, fills the whole depth of the doorway (the inner and the outer wall layer) instead of one layer, and {@link #limitsFor} lets twelve
 * 7 x 7 doorways pass. For a room that was not cut everything is exactly what it was: inset 2, one layer, the old limits.
 */
public final class OpeningScan {
    private OpeningScan() {}

    /** A passable block in the wall layer. */
    public record Cell(int x, int y, int z) {}

    /** The limits of a judgement. The defaults hold four doors of 7 by 7 (the clearance rule) with room to spare. */
    public record Limits(int maxTotal, int maxPerOpening, int maxOpenings) {
        public static final Limits DEFAULT = new Limits(320, 96, 12);
    }

    /** The judgement: whether to lock, why not otherwise, and the numbers. */
    public record Result(boolean ok, String why, int cells, int openings, int biggest) {}

    /**
     * Groups the cells into openings: two cells are one opening when they share a face (6-neighbourhood, so a doorway stays one piece and an arch with a gap
     * above its keystone is still one).
     */
    public static List<List<Cell>> openings(Iterable<Cell> cells) {
        Set<Cell> left = new HashSet<>();
        for (Cell c : cells) left.add(c);
        List<List<Cell>> out = new ArrayList<>();
        int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!left.isEmpty()) {
            Cell seed = left.iterator().next();
            List<Cell> group = new ArrayList<>();
            ArrayDeque<Cell> queue = new ArrayDeque<>();
            queue.add(seed);
            left.remove(seed);
            while (!queue.isEmpty()) {
                Cell c = queue.poll();
                group.add(c);
                for (int[] n : d) {
                    Cell next = new Cell(c.x() + n[0], c.y() + n[1], c.z() + n[2]);
                    if (left.remove(next)) queue.add(next);
                }
            }
            out.add(group);
        }
        out.sort((a, b) -> {
            Cell x = a.stream().min(OpeningScan::order).orElse(new Cell(0, 0, 0)), y = b.stream().min(OpeningScan::order).orElse(new Cell(0, 0, 0));
            return order(x, y);
        });
        return out;
    }

    private static int order(Cell a, Cell b) {
        if (a.x() != b.x()) return Integer.compare(a.x(), b.x());
        if (a.z() != b.z()) return Integer.compare(a.z(), b.z());
        return Integer.compare(a.y(), b.y());
    }

    /** Judges a set of passable wall cells. */
    public static Result judge(Iterable<Cell> cells, Limits limits) {
        List<List<Cell>> groups = openings(cells);
        int total = 0, biggest = 0;
        for (List<Cell> g : groups) {
            total += g.size();
            biggest = Math.max(biggest, g.size());
        }
        if (groups.isEmpty()) return new Result(false, "no opening in the wall: there is nothing to close", 0, 0, 0);
        if (groups.size() > limits.maxOpenings()) return new Result(false, groups.size() + " openings: an open-plan room", total, groups.size(), biggest);
        if (biggest > limits.maxPerOpening()) return new Result(false, "an opening of " + biggest + " cells: not a doorway", total, groups.size(), biggest);
        if (total > limits.maxTotal()) return new Result(false, total + " cells to close: an open-plan room", total, groups.size(), biggest);
        return new Result(true, "ok", total, groups.size(), biggest);
    }

    // ---------------------------------------------------------------------------------------------------------------------------- the scan (pure: the world is a callback)

    /** Is the cell a place a player can walk through? The game side answers: loaded, no collision, not already one of the lockdown's barriers. */
    public interface Free {
        boolean at(int x, int y, int z);
    }

    /** What a scan found: {@code plane} are the passable cells of the wall line (one per doorway cell, what {@link #judge} counts), {@code fill} the cells the barrier takes (the plane, and for a cut room the outer layer too). */
    public record Scan(List<Cell> plane, List<Cell> fill) {}

    /** The cells of one 7 x 7 doorway of the clearance rule, in blocks. */
    public static final int CUT_CELLS = Clearance7.REQUIRED * Clearance7.REQUIRED;

    /** The first row scanned: one above the floor row of the room's template, kept inside the box. */
    public static int firstRow(int minY, int maxY, int floorRow) { return Math.max(minY + 1, Math.min(minY + floorRow + 1, maxY - 2)); }

    /** How many rows are scanned above {@link #firstRow}: the configured height, and for a cut room at least the 7 rows of the doorway. */
    public static int rows(int openHeight, boolean cut) { return cut ? Math.max(openHeight, Clearance7.REQUIRED) : openHeight; }

    /** Blocks from a room's corner at which the wall line starts: a cut doorway can reach one block from it, an old one never did. */
    public static int inset(boolean cut) { return cut ? 1 : 2; }

    /** The limits for a room: the configured ones, and for a cut room enough for {@code maxOpenings} doorways of 7 by 7 (and an arch row above them). */
    public static Limits limitsFor(Limits base, boolean cut) {
        if (!cut) return base;
        int per = Math.max(base.maxPerOpening(), Clearance7.REQUIRED * (Clearance7.REQUIRED + 2));
        int total = Math.max(base.maxTotal(), base.maxOpenings() * CUT_CELLS);
        return new Limits(total, per, base.maxOpenings());
    }

    /**
     * Reads the doorways of a room box. The wall is read in the line {@code wallDepth} blocks in from each of the four faces (the corners left out), from {@link #firstRow} up
     * {@link #rows} rows; a cell counts when it and every cell between it and the outside of the box along the same line is free (a niche behind a door cap has a back wall and is no
     * doorway). The cells are returned in a stable order, each once (at a corner a cut doorway of one wall and one of the next can share a cell).
     *
     * @param cut the room's sector was cut to 7 x 7: start one block from the corner, look at least 7 rows high, and fill the doorway through the whole wall depth
     */
    public static Scan scan(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int wallDepth, int openHeight, int floorRow, boolean cut, Free free) {
        int y0 = firstRow(minY, maxY, floorRow), height = rows(openHeight, cut), in = inset(cut);
        Set<Cell> plane = new LinkedHashSet<>(), fill = new LinkedHashSet<>();
        for (int y = y0; y < y0 + height && y <= maxY; y++) {
            for (int u = minX + in; u <= maxX - in; u++) {                          // the north and south faces run along x
                line(plane, fill, free, u, y, minZ, 0, 1, wallDepth, cut);
                line(plane, fill, free, u, y, maxZ, 0, -1, wallDepth, cut);
            }
            for (int u = minZ + in; u <= maxZ - in; u++) {                          // the west and east faces run along z
                line(plane, fill, free, minX, y, u, 1, 0, wallDepth, cut);
                line(plane, fill, free, maxX, y, u, -1, 0, wallDepth, cut);
            }
        }
        return new Scan(new ArrayList<>(plane), new ArrayList<>(fill));
    }

    private static void line(Set<Cell> plane, Set<Cell> fill, Free free, int x, int y, int z, int dx, int dz, int depth, boolean cut) {
        for (int k = 0; k <= depth; k++) if (!free.at(x + dx * k, y, z + dz * k)) return;
        Cell at = new Cell(x + dx * depth, y, z + dz * depth);
        plane.add(at);
        if (cut) {
            for (int k = 0; k <= depth; k++) fill.add(new Cell(x + dx * k, y, z + dz * k));
        } else {
            fill.add(at);
        }
    }

    /** The width (the longer side across the wall) and the height of an opening's bounding box, for reports. */
    public static int[] extent(List<Cell> opening) {
        if (opening.isEmpty()) return new int[] {0, 0};
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
        for (Cell c : opening) {
            x0 = Math.min(x0, c.x());
            x1 = Math.max(x1, c.x());
            y0 = Math.min(y0, c.y());
            y1 = Math.max(y1, c.y());
            z0 = Math.min(z0, c.z());
            z1 = Math.max(z1, c.z());
        }
        return new int[] {Math.max(x1 - x0, z1 - z0) + 1, y1 - y0 + 1};
    }

    // ---------------------------------------------------------------------------------------------------------------------------- the self-check

    /**
     * A made-up 27 x 31 x 27 room with walls two layers thick and four doorways cut exactly the way {@link Clearance7#cuts} cuts a connected door of a 9-wide module (7 wide, rows 9..15,
     * both wall layers, the 3-block apron behind; the north and south doorways are in the first and last module, so their outer columns lie one block from the corner). Returns the
     * problems (empty = the barrier covers every cut doorway completely, takes nothing solid, and the old scan really would have left a gap). Used by the unit test and by the
     * self-tests {@code dungeon.lockdown} and {@code dungeon.clearance7}.
     */
    public static List<String> cutDoorwayCheck() {
        List<String> bad = new ArrayList<>();
        final int n = 27, floorRow = 8, minY = 0, maxY = 30, half = Clearance7.HALF, req = Clearance7.REQUIRED, wall = 2, apron = Clearance7.APRON;
        // x0, z0, x1, z1 of each cut and the face it opens (0 north, 1 south, 2 west, 3 east)
        int[][] cuts = {
                {1, 0, 1 + 2 * half, wall - 1 + apron, 0},                                    // north wall, the first module (centre 4): x 1..7
                {n - 2 - 2 * half, n - wall - apron, n - 2, n - 1, 1},                        // south wall, the last module (centre 22): x 19..25
                {0, 13 - half, wall - 1 + apron, 13 + half, 2},                               // west wall, the middle module (centre 13): z 10..16
                {n - wall - apron, 13 - half, n - 1, 13 + half, 3}};                          // east wall, the middle module
        OpeningScan.Free room = (x, y, z) -> {
            if (x < 0 || z < 0 || x >= n || z >= n || y < minY || y > maxY) return false;
            boolean solid = x < wall || z < wall || x >= n - wall || z >= n - wall || y <= floorRow || y > floorRow + 12;
            if (!solid) return true;
            if (y < floorRow + 1 || y > floorRow + req) return false;
            for (int[] c : cuts) if (x >= c[0] && x <= c[2] && z >= c[1] && z <= c[3]) return true;
            return false;
        };
        Scan cutScan = scan(0, minY, 0, n - 1, maxY, n - 1, 1, 9, floorRow, true, room);
        List<List<Cell>> doors = openings(cutScan.plane());
        if (doors.size() != 4) bad.add("the scan of a cut room found " + doors.size() + " openings, not 4");
        if (cutScan.plane().size() != 4 * CUT_CELLS) bad.add("the scan of a cut room found " + cutScan.plane().size() + " wall cells, not " + 4 * CUT_CELLS + " (4 doorways of 7 x 7)");
        if (cutScan.fill().size() != 8 * CUT_CELLS) bad.add("the barrier of a cut room takes " + cutScan.fill().size() + " cells, not " + 8 * CUT_CELLS + " (4 doorways, 7 x 7, both wall layers)");
        for (List<Cell> g : doors) {
            int[] e = extent(g);
            if (e[0] != req || e[1] != req) bad.add("a doorway of the cut room reads " + e[0] + " x " + e[1] + ", not " + req + " x " + req);
        }
        Set<Cell> filled = new HashSet<>(cutScan.fill());
        int missing = 0;
        for (int[] c : cuts) {                                                              // every cell of the cut that lies in the two wall layers of its face, rows 9..15
            int x0 = c[0], x1 = c[2], z0 = c[1], z1 = c[3];
            switch (c[4]) {
                case 0 -> z1 = c[1] + (wall - 1);
                case 1 -> z0 = c[3] - (wall - 1);
                case 2 -> x1 = c[0] + (wall - 1);
                default -> x0 = c[2] - (wall - 1);
            }
            for (int y = floorRow + 1; y <= floorRow + req; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (!filled.contains(new Cell(x, y, z))) missing++;
        }
        if (missing > 0) bad.add(missing + " cells of the cut doorways are not covered by the barrier");
        for (Cell c : cutScan.fill()) if (!room.at(c.x(), c.y(), c.z())) bad.add("the barrier would fill the solid cell " + c);
        Result judged = judge(cutScan.plane(), limitsFor(Limits.DEFAULT, true));
        if (!judged.ok()) bad.add("the limits for a cut room refuse four 7 x 7 doorways: " + judged.why());
        List<Cell> twelve = new ArrayList<>();
        for (int d = 0; d < 12; d++) for (int x = 0; x < req; x++) for (int y = 0; y < req; y++) twelve.add(new Cell(d * 10 + x, y, 1));
        if (!judge(twelve, limitsFor(Limits.DEFAULT, true)).ok()) bad.add("twelve 7 x 7 doorways of a cut room are refused: " + judge(twelve, limitsFor(Limits.DEFAULT, true)).why());
        if (judge(twelve, Limits.DEFAULT).ok()) bad.add("twelve 7 x 7 doorways pass the old limits (the check is not meaningful)");
        // the same room read the old way: one layer, and the outer column of the north and south doorways is missed (the gap this class closes)
        Scan oldScan = scan(0, minY, 0, n - 1, maxY, n - 1, 1, 9, floorRow, false, room);
        if (oldScan.plane().size() >= cutScan.plane().size()) bad.add("the old scan of a 7 x 7 room should miss the outer columns but found " + oldScan.plane().size() + " cells");
        if (oldScan.fill().size() != oldScan.plane().size()) bad.add("an old room's barrier is not one layer");
        return bad;
    }
}
