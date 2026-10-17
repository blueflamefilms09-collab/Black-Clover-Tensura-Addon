package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: the Spade Kingdom's rulers (the Dark Triad, their devils) are fought in the one place that says "a kingdom that belongs to them": a long throne room
 * of black stone, under the open sky so a flying devil has room. A hall 23 blocks wide and 51 long (north is the throne): a chequered floor of polished
 * blackstone with a gold line along each wall, a purple carpet from the door to the dais, ten black pillars with soul lanterns, walls nine blocks high with
 * bands, and at the north end a dais of three tiers with a throne of stairs and a high back. The boss stands on the floor in front of the dais (the dais is
 * scenery and a stage for the fight to climb onto, not a place to hide), the travellers appear in two rows at the door end, and two pads at the south corners lead
 * home. Pure arithmetic of the column: see {@link ArenaShape}.
 */
final class SpadeThroneShape extends AbstractArenaShape {
    private static final int HALF_X = 13, HALF_Z = 27, WALL_X = 12, WALL_Z = 26;
    private static final int[] PILLAR_Z = {-14, -6, 2, 10, 18};

    SpadeThroneShape() {
        super("spade_throne", "The Throne Room of the Spade Kingdom", 27, ArenaKit.withStairs(ArenaKit.palette(
                FOUNDATION, "blackstone", FLOOR_A, "polished_blackstone_bricks", FLOOR_B, "polished_blackstone", INLAY, "gold_block",
                LAMP_FLOOR, "shroomlight", DAIS_BASE, "polished_blackstone_bricks", DAIS_TOP, "chiseled_polished_blackstone", DAIS_EDGE, "gilded_blackstone",
                THRONE, "blackstone_stairs[facing=north,half=bottom]", WALL, "polished_blackstone_bricks", WALL_B, "chiseled_polished_blackstone",
                WALL_CAP, "polished_blackstone_brick_slab", PILLAR, "polished_blackstone", PILLAR_BAND, "crying_obsidian", PILLAR_CAP, "chiseled_polished_blackstone",
                LANTERN, "soul_lantern[hanging=false]", CARPET, "purple_carpet", SIGIL_PAD, "gilded_blackstone", SIGIL, "lodestone"), "polished_blackstone_stairs"),
                new int[] {0, -10}, new int[][] {{-9, 23}, {9, 23}});
    }

    @Override
    protected int[][] arrival(int n) {
        int first = Math.min(n, 9);
        int[][] a = ArenaKit.row(first, 21, 2);
        if (n <= 9) return a;
        int[][] b = ArenaKit.row(n - 9, 19, 2);
        int[][] out = new int[n][];
        System.arraycopy(a, 0, out, 0, 9);
        System.arraycopy(b, 0, out, 9, n - 9);
        return out;
    }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax > HALF_X || az > HALF_Z) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (ax >= WALL_X || az >= WALL_Z) {
            wall(out);
            return;
        }
        out.add(new ArenaCell(0, floor(dx, dz)));
        dais(dx, dz, out);
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
            return;
        }
        if (ax == 8) for (int pz : PILLAR_Z) if (dz == pz && clear(dx, dz)) {
            for (int dy = 1; dy <= 8; dy++) out.add(new ArenaCell(dy, dy == 3 || dy == 6 ? PILLAR_BAND : PILLAR));
            out.add(new ArenaCell(9, PILLAR_CAP));
            out.add(new ArenaCell(10, LANTERN));
            return;
        }
        carpet(dx, dz, out);
    }

    /** The purple runner: five wide over the floor from the door to the foot of the dais, three wide up the stairs and over the tiers (never under the throne). */
    private void carpet(int dx, int dz, List<ArenaCell> out) {
        int ax = Math.abs(dx);
        boolean onFloor = ax <= 2 && dz >= -17 && dz <= 24;
        boolean onDais = ax <= 1 && dz >= -23 && dz <= -18;
        if (!onFloor && !onDais) return;
        int top = Integer.MIN_VALUE;
        for (ArenaCell c : out) if (c.part().solid()) top = Math.max(top, c.dy());
        if (top != Integer.MIN_VALUE) out.add(new ArenaCell(top + 1, CARPET));
    }

    private ArenaPart floor(int dx, int dz) {
        if (onSigilPad(dx, dz)) return SIGIL_PAD;
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax == 10 && az <= 24) return INLAY;
        if ((ax == 5 || ax == 6) && (az == 3 || az == 4) && dz > 0 && ax != 0) return LAMP_FLOOR;
        return ((dx + dz) & 1) == 0 ? FLOOR_A : FLOOR_B;
    }

    private void wall(List<ArenaCell> out) {
        out.add(new ArenaCell(0, WALL));
        for (int dy = 1; dy <= 9; dy++) out.add(new ArenaCell(dy, dy == 4 || dy == 8 ? WALL_B : WALL));
        out.add(new ArenaCell(10, WALL_CAP));
    }

    /** The three tiers at the north end, the stairs in front of each and the throne with its back. The floor cell (dy 0) is already written. */
    private void dais(int dx, int dz, List<ArenaCell> out) {
        int ax = Math.abs(dx);
        if (dz >= -25 && dz <= -19 && ax <= 8) {                                                            // tier one, top at dy 1
            if (dz <= -22 && ax <= 5) {                                                                     // tier two, top at dy 2
                out.add(new ArenaCell(1, DAIS_BASE));
                if (dz <= -24 && ax <= 3) {                                                                 // tier three, top at dy 3
                    out.add(new ArenaCell(2, DAIS_BASE));
                    out.add(new ArenaCell(3, DAIS_TOP));
                    throne(dx, dz, out);
                } else if (dz == -23 && ax <= 3) {
                    out.add(new ArenaCell(2, DAIS_TOP));                                                    // tier two's own surface
                    out.add(new ArenaCell(3, STEP_N));                                                      // the stairs up to tier three stand on it
                } else {
                    out.add(new ArenaCell(2, DAIS_TOP));
                }
            } else if (dz == -21 && ax <= 5) {
                out.add(new ArenaCell(1, DAIS_BASE));
                out.add(new ArenaCell(2, STEP_N));                                                          // the stairs up to tier two
            } else {
                out.add(new ArenaCell(1, ax == 8 ? DAIS_EDGE : DAIS_TOP));
            }
        } else if (dz == -18 && ax <= 8) {
            out.add(new ArenaCell(1, STEP_N));                                                              // the stairs up to tier one
        }
    }

    /** The throne on tier three: a seat of stairs at (0, -24), arms either side and a back against the north wall. */
    private void throne(int dx, int dz, List<ArenaCell> out) {
        if (dz == -24 && dx == 0) out.add(new ArenaCell(4, THRONE));
        else if (dz == -24 && Math.abs(dx) == 1) out.add(new ArenaCell(4, DAIS_EDGE));
        else if (dz == -25 && Math.abs(dx) <= 1) {
            int h = dx == 0 ? 6 : 5;
            for (int dy = 4; dy <= h; dy++) out.add(new ArenaCell(dy, WALL_B));
        }
    }
}
