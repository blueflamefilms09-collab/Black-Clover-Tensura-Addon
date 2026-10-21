package com.newuniverse.nusmp.labyrinth.trial;

import com.newuniverse.nusmp.labyrinth.mega.gen.Clearance7;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Sealed Archway closes a trial hall with a column of Magic Barrier over every Ward Rune, and the templates put three runes in the floor of each doorway (the inner wall
 * layer) and a column of at most four cells over each: the doorway of the first generator, 3 wide and 4 high. A hall of a sector made with the 7 x 7 clearance ({@code gen.clearance7},
 * docs/labyrinth/clearance7.md) has its doorways cut 7 wide and 7 high through BOTH wall layers, so three columns of four would leave two open columns on each side and three open rows
 * above, and the "sealed" hall would be open. This turns the runes into the columns that fill the cut doorway completely: each line of runes is widened to 7 (two columns more on
 * each side of three), the same widening is made in the outer wall layer (the side away from the archway), and the columns may rise 7 cells. The game side only places a barrier in a cell
 * that is air, so a column that meets wall places nothing: a widening that goes too far is harmless. A hall that was not cut gets exactly its runes and the cap of 4. Plain integers, so
 * it is a unit test.
 */
public final class ArchwayDoorways {
    private ArchwayDoorways() {}

    /** The floor cell a barrier column stands on (the rune's place for a rune); the column rises from the cell above it. */
    public record Col(int x, int y, int z) {}

    /** How many cells a column may rise: 7 in a doorway cut to 7 x 7, the 4 of the old doorway otherwise. */
    public static int columnCap(boolean cut) { return cut ? Clearance7.REQUIRED : TrialMath.MAX_COLUMN; }

    /**
     * The columns of a hall.
     *
     * @param runes the runes found in the archway's box, as {x, y, z}
     * @param cx    the archway's x and z: the outer wall layer lies on the side of a line away from it
     * @param cut   the hall's sector was cut to 7 x 7
     */
    public static List<Col> columns(List<int[]> runes, int cx, int cz, boolean cut) {
        Set<Col> out = new LinkedHashSet<>();
        for (int[] r : runes) out.add(new Col(r[0], r[1], r[2]));
        if (!cut) return new ArrayList<>(out);
        Set<Col> runeSet = new HashSet<>(out);
        Set<Col> seen = new HashSet<>();
        for (Col start : new ArrayList<>(runeSet)) {
            if (!seen.add(start)) continue;
            List<Col> line = new ArrayList<>();
            List<Col> queue = new ArrayList<>(List.of(start));
            while (!queue.isEmpty()) {                                                     // the runes that touch edge to edge on the same floor row are one doorway
                Col c = queue.remove(queue.size() - 1);
                line.add(c);
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    Col n = new Col(c.x() + d[0], c.y(), c.z() + d[1]);
                    if (runeSet.contains(n) && seen.add(n)) queue.add(n);
                }
            }
            if (line.size() < 2) continue;                                                 // one rune alone has no direction: leave it as the template drew it
            int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
            for (Col c : line) {
                x0 = Math.min(x0, c.x());
                x1 = Math.max(x1, c.x());
                z0 = Math.min(z0, c.z());
                z1 = Math.max(z1, c.z());
            }
            boolean alongX = x1 - x0 >= z1 - z0;
            if (alongX ? z0 != z1 : x0 != x1) continue;                                    // not a straight line of runes: leave it
            int lo = alongX ? x0 : z0, hi = alongX ? x1 : z1, fixed = alongX ? z0 : x0, y = line.get(0).y();
            int size = hi - lo + 1;
            if (size >= Clearance7.REQUIRED) continue;
            int padLo = (Clearance7.REQUIRED - size) / 2, padHi = Clearance7.REQUIRED - size - padLo;
            int away = Integer.signum(fixed - (alongX ? cz : cx));                          // the outer wall layer is one step away from the archway
            for (int t = lo - padLo; t <= hi + padHi; t++) {
                out.add(alongX ? new Col(t, y, fixed) : new Col(fixed, y, t));
                if (away != 0) out.add(alongX ? new Col(t, y, fixed + away) : new Col(fixed + away, y, t));
            }
        }
        return new ArrayList<>(out);
    }

    /** A check for the self-test {@code dungeon.clearance7}: returns the problems (empty = a cut hall's doorway is filled completely and an old hall's is untouched). */
    public static List<String> selfCheck() {
        List<String> bad = new ArrayList<>();
        List<int[]> runes = new ArrayList<>();
        for (int x = 12; x <= 14; x++) runes.add(new int[] {x, 8, 1});                     // three runes in the inner layer of the north wall of a 27 x 27 hall
        List<Col> old = columns(runes, 13, 13, false);
        if (old.size() != 3) bad.add("an old hall's doorway gives " + old.size() + " columns, not its 3 runes");
        List<Col> cut = columns(runes, 13, 13, true);
        if (cut.size() != 2 * Clearance7.REQUIRED) bad.add("a cut hall's doorway gives " + cut.size() + " columns, not " + 2 * Clearance7.REQUIRED + " (7 wide, two wall layers)");
        for (int x = 10; x <= 16; x++) {
            if (!cut.contains(new Col(x, 8, 1))) bad.add("the inner layer of a cut doorway lacks the column at x " + x);
            if (!cut.contains(new Col(x, 8, 0))) bad.add("the outer layer of a cut doorway lacks the column at x " + x);
        }
        if (columnCap(true) != Clearance7.REQUIRED || columnCap(false) != TrialMath.MAX_COLUMN) bad.add("the column caps are wrong");
        return bad;
    }
}
