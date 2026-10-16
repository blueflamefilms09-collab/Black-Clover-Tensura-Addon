package com.newuniverse.nusmp.arena;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the arena (1.46.0, {@code nusmp:zogratis_arena}) is built by code the first time somebody arrives, and a layout that is built
 * from a formula can be checked without a game: this pure class (no Minecraft types, unit tested) says, for every column at an offset
 * (dx, dz) from the centre, which {@link Part} stands at which height ({@code dy}, 0 is the walking surface of the floor, so a player
 * stands at dy = 1). {@link ArenaBuilder} only maps each part to a block and places it. The layout is a castle courtyard:
 * <ul>
 *   <li>a round floor of radius {@value #RADIUS} on two layers of foundation, in a checker of two stones with a gold ring at radius 12;</li>
 *   <li>a central dais of radius 5 raised one block, its edge in gold and a rune at its centre, where Lucius stands;</li>
 *   <li>four rune pillars on the diagonals (3 by 3, nine high, a light on top);</li>
 *   <li>a raised ring seat for spectators (radius 33 to 37, one block high at the inner half and two at the outer half);</li>
 *   <li>a wall of radius 41 to 42 (five high, with merlons on top and a lantern every twelfth) that closes the courtyard, so nobody
 *       falls into the void and no thrown item leaves;</li>
 *   <li>four Return Sigils at the compass points (radius 38).</li>
 * </ul>
 */
public final class ArenaLayout {
    private ArenaLayout() {}

    public static final int RADIUS = 42, DAIS_RADIUS = 5, FLOOR_Y = 64, WALL_INNER = 41, PILLAR_OFFSET = 20, SIGIL_RADIUS = 38, SEAT_INNER = 33, SEAT_OUTER = 37;
    public static final int WALL_HEIGHT = 5, PILLAR_HEIGHT = 9, CRENEL_STEPS = 96, LANTERN_EVERY = 12;
    /** How far above the floor (in blocks, relative to the walking surface) the highest block can be: the pillar's light. */
    public static final int MAX_DY = PILLAR_HEIGHT + 1, MIN_DY = -2;

    public enum Part {
        FOUNDATION, FLOOR_A, FLOOR_B, FLOOR_GOLD, DAIS_BASE, DAIS_TOP, DAIS_EDGE, DAIS_RUNE,
        SEAT_LOW, SEAT_HIGH, WALL, MERLON, LANTERN, PILLAR, PILLAR_BAND, PILLAR_LIGHT, SIGIL
    }

    /** One block of a column: its height and what it is. */
    public record Cell(int dy, Part part) {}

    /** The distance of a column from the centre. */
    public static double radius(int dx, int dz) { return Math.hypot(dx, dz); }

    /** Is the column inside the courtyard (the wall included)? */
    public static boolean inside(int dx, int dz) { return radius(dx, dz) <= RADIUS + 0.5; }

    /** Which pillar's footprint holds this column (0..3), or -1. */
    public static int pillarAt(int dx, int dz) {
        for (int k = 0; k < 4; k++) {
            int px = (k & 1) == 0 ? PILLAR_OFFSET : -PILLAR_OFFSET, pz = (k & 2) == 0 ? PILLAR_OFFSET : -PILLAR_OFFSET;
            if (Math.abs(dx - px) <= 1 && Math.abs(dz - pz) <= 1) return k;
        }
        return -1;
    }

    /** Is this column one of the four sigil spots? */
    public static boolean sigilAt(int dx, int dz) {
        return (Math.abs(dx) == SIGIL_RADIUS && dz == 0) || (dx == 0 && Math.abs(dz) == SIGIL_RADIUS);
    }

    /** The sigil columns, in a fixed order (east, west, south, north). */
    public static int[][] sigils() { return new int[][]{{SIGIL_RADIUS, 0}, {-SIGIL_RADIUS, 0}, {0, SIGIL_RADIUS}, {0, -SIGIL_RADIUS}}; }

    private static int crenelIndex(int dx, int dz) {
        double a = Math.atan2(dz, dx) / (Math.PI * 2) + 0.5;
        return (int) Math.floor(a * CRENEL_STEPS);
    }

    /** The blocks of one column, bottom to top; empty outside the courtyard. */
    public static List<Cell> column(int dx, int dz) {
        List<Cell> out = new ArrayList<>();
        double r = radius(dx, dz);
        if (r > RADIUS + 0.5) return out;
        out.add(new Cell(-2, Part.FOUNDATION));
        out.add(new Cell(-1, Part.FOUNDATION));
        if (r <= DAIS_RADIUS + 0.5) {
            out.add(new Cell(0, Part.DAIS_BASE));
            if (r <= 1.5) out.add(new Cell(1, Part.DAIS_RUNE));
            else if (r > DAIS_RADIUS - 1.0) out.add(new Cell(1, Part.DAIS_EDGE));
            else out.add(new Cell(1, Part.DAIS_TOP));
            return out;
        }
        if (Math.abs(r - 12.0) <= 0.6) out.add(new Cell(0, Part.FLOOR_GOLD));
        else out.add(new Cell(0, ((dx + dz) & 1) == 0 ? Part.FLOOR_A : Part.FLOOR_B));
        if (r > SEAT_INNER - 0.5 && r <= SEAT_OUTER + 0.5) {
            out.add(new Cell(1, r <= (SEAT_INNER + SEAT_OUTER) / 2.0 ? Part.SEAT_LOW : Part.SEAT_HIGH));
            if (r > (SEAT_INNER + SEAT_OUTER) / 2.0) out.add(new Cell(2, Part.SEAT_HIGH));
        }
        if (sigilAt(dx, dz)) out.add(new Cell(1, Part.SIGIL));
        int pk = pillarAt(dx, dz);
        if (pk >= 0) {
            int px = (pk & 1) == 0 ? PILLAR_OFFSET : -PILLAR_OFFSET, pz = (pk & 2) == 0 ? PILLAR_OFFSET : -PILLAR_OFFSET;
            boolean centre = dx == px && dz == pz;
            for (int dy = 1; dy <= PILLAR_HEIGHT; dy++) out.add(new Cell(dy, dy % 3 == 0 ? Part.PILLAR_BAND : Part.PILLAR));
            if (centre) out.add(new Cell(PILLAR_HEIGHT + 1, Part.PILLAR_LIGHT));
        }
        if (r > WALL_INNER - 0.5) {
            for (int dy = 1; dy <= WALL_HEIGHT; dy++) out.add(new Cell(dy, Part.WALL));
            int ci = crenelIndex(dx, dz);
            if ((ci & 1) == 0) {
                out.add(new Cell(WALL_HEIGHT + 1, Part.MERLON));
                if (ci % (LANTERN_EVERY * 2) == 0) out.add(new Cell(WALL_HEIGHT + 2, Part.LANTERN));
            }
        }
        return out;
    }

    /** The walking surface of the column (the dy of its highest solid, non-decor block), or Integer.MIN_VALUE outside. */
    public static int surface(int dx, int dz) {
        int best = Integer.MIN_VALUE;
        for (Cell c : column(dx, dz)) if (c.part() != Part.LANTERN) best = Math.max(best, c.dy());
        return best;
    }

    /**
     * Arrival spots for 'n' travellers: offsets (dx, dz) on a ring just outside the dais edge (radius 7), spread evenly, and the height above the
     * floor surface at which their feet appear (3 blocks: two blocks above where a player stands, so a fall of two blocks, never inside a block).
     */
    public static final int ARRIVAL_RADIUS = 7, ARRIVAL_DY = 3;

    public static int[][] arrivalSpots(int n) {
        int count = Math.max(1, Math.min(16, n));
        int[][] out = new int[count][2];
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count + Math.PI / 4;
            out[i][0] = (int) Math.round(Math.cos(a) * ARRIVAL_RADIUS);
            out[i][1] = (int) Math.round(Math.sin(a) * ARRIVAL_RADIUS);
        }
        return out;
    }
}
