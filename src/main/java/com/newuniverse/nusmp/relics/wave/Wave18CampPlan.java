package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wave 18: the layout of The Pocket Campsite, one large tent, two small tents and a campfire with seats, as a list of cells in the camp's own
 * coordinates: {@code u} to the right of the one who points the pouch, {@code v} forward, {@code y} up from the ground (0 is the first block above
 * it). The campfire is the origin. Pure; the glue turns a cell into a block and the camp's facing into a rotation.
 */
public final class Wave18CampPlan {
    private Wave18CampPlan() {}

    /** What a cell is made of. */
    public enum Kind { FIRE, SEAT, BIG_WALL, BIG_ROOF, LEFT_WALL, LEFT_ROOF, RIGHT_WALL, RIGHT_ROOF, BED_FOOT, BED_HEAD }

    public record Cell(int u, int y, int v, Kind kind) {}

    /** The large tent: 5 by 5, its centre this far in front of the fire. */
    static final int BIG_V = 6;
    /** The small tents: 3 by 3, this far to each side and ahead. */
    static final int SMALL_U = 5, SMALL_V = 2;

    /** Every cell of the camp, each position once. */
    public static List<Cell> plan() {
        List<Cell> out = new ArrayList<>();
        out.add(new Cell(0, 0, 0, Kind.FIRE));
        for (int[] s : new int[][]{{2, 0}, {-2, 0}, {0, -2}, {0, 2}}) out.add(new Cell(s[0], 0, s[1], Kind.SEAT));
        // the large tent: a ring of wool two high with the door on the side facing the fire, a roof, a peaked cap, a bed inside
        for (int du = -2; du <= 2; du++) {
            for (int dv = -2; dv <= 2; dv++) {
                boolean ring = Math.abs(du) == 2 || Math.abs(dv) == 2;
                boolean door = du == 0 && dv == -2;
                if (ring && !door) {
                    out.add(new Cell(du, 0, BIG_V + dv, Kind.BIG_WALL));
                    out.add(new Cell(du, 1, BIG_V + dv, Kind.BIG_WALL));
                }
                out.add(new Cell(du, 2, BIG_V + dv, Kind.BIG_ROOF));
            }
        }
        for (int du = -1; du <= 1; du++) for (int dv = -1; dv <= 1; dv++) out.add(new Cell(du, 3, BIG_V + dv, Kind.BIG_ROOF));
        out.add(new Cell(-1, 0, BIG_V, Kind.BED_FOOT));
        out.add(new Cell(-1, 0, BIG_V + 1, Kind.BED_HEAD));
        small(out, -SMALL_U, +1, Kind.LEFT_WALL, Kind.LEFT_ROOF);
        small(out, +SMALL_U, -1, Kind.RIGHT_WALL, Kind.RIGHT_ROOF);
        return out;
    }

    /** A 3 by 3 tent centred at (cu, SMALL_V) whose door is on the side {@code doorSide} (+1 right, -1 left) of its centre row. */
    private static void small(List<Cell> out, int cu, int doorSide, Kind wall, Kind roof) {
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                boolean ring = du != 0 || dv != 0;
                boolean door = du == doorSide && dv == 0;
                if (ring && !door) {
                    out.add(new Cell(cu + du, 0, SMALL_V + dv, wall));
                    out.add(new Cell(cu + du, 1, SMALL_V + dv, wall));
                }
                out.add(new Cell(cu + du, 2, SMALL_V + dv, roof));
            }
        }
    }

    /** The cells on the ground (y = 0): the ones that need something solid under them. */
    public static List<Cell> floor(List<Cell> cells) {
        List<Cell> f = new ArrayList<>();
        for (Cell c : cells) if (c.y() == 0) f.add(c);
        return f;
    }

    /** The floor of a tent interior is part of the footprint too: cells under a roof with no wall cell at ground level. */
    public static Set<Long> footprint(List<Cell> cells) {
        Set<Long> s = new HashSet<>();
        for (Cell c : cells) s.add(key(c.u(), c.v()));
        return s;
    }

    public static long key(int u, int v) { return ((long) u << 32) ^ (v & 0xFFFFFFFFL); }

    /** The world offset of a local cell for a camp that faces {@code (fx, fz)} (a unit horizontal direction): right is {@code (-fz, fx)}. */
    public static int[] world(int u, int v, int fx, int fz) {
        return new int[]{u * (-fz) + v * fx, u * fx + v * fz};
    }
}
