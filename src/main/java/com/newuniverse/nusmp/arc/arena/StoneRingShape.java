package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: a character with no arena of its own (an ally the roster names a fight for, a saga this file does not know yet, a typo in an arena id) must still have a
 * place to be fought in, and a plain, honest one is better than none: a round floor of stone bricks, a ring inlaid in it, a dais in the middle, four stout
 * pillars with lanterns on the diagonals, a wall four blocks high round it, and a way home on each of the four sides. It is the fallback of
 * {@link ArenaShapes#forCharacter} and the arena of the saga of the allies. Pure arithmetic: see {@link ArenaShape}.
 */
final class StoneRingShape extends AbstractArenaShape {
    private static final double WALL_FROM = 27.5, WALL_TO = 30.0, DAIS = 4.0;
    private static final int[][] PILLARS = {ArenaKit.at(20.0, 45.0), ArenaKit.at(20.0, 135.0), ArenaKit.at(20.0, 225.0), ArenaKit.at(20.0, 315.0)};

    StoneRingShape() {
        super("stone_ring", "The Ring of Stone", 31, ArenaKit.palette(
                FOUNDATION, "stone", FLOOR_A, "stone_bricks", FLOOR_B, "polished_andesite", INLAY, "chiseled_stone_bricks", DAIS_BASE, "stone_bricks",
                DAIS_TOP, "smooth_stone", DAIS_EDGE, "chiseled_stone_bricks", DAIS_RUNE, "gold_block", WALL, "stone_bricks", WALL_CAP, "stone_brick_slab",
                PILLAR, "stone_bricks", PILLAR_BAND, "chiseled_stone_bricks", PILLAR_CAP, "chiseled_stone_bricks", LANTERN, "lantern[hanging=false]",
                SIGIL_PAD, "polished_andesite", SIGIL, "lodestone"),
                new int[] {0, 0}, new int[][] {{23, 0}, {0, 23}, {-23, 0}, {0, -23}});
    }

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 10.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > WALL_TO) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= WALL_FROM) {
            out.add(new ArenaCell(0, WALL));
            for (int dy = 1; dy <= 4; dy++) out.add(new ArenaCell(dy, WALL));
            out.add(new ArenaCell(5, WALL_CAP));
            return;
        }
        if (r <= DAIS) {
            out.add(new ArenaCell(0, DAIS_BASE));
            out.add(new ArenaCell(1, r <= 1.2 ? DAIS_RUNE : r > 3.0 ? DAIS_EDGE : DAIS_TOP));
            return;
        }
        out.add(new ArenaCell(0, onSigilPad(dx, dz) ? SIGIL_PAD : Math.abs(r - 14.0) <= 0.55 ? INLAY : ((dx + dz) & 1) == 0 ? FLOOR_A : FLOOR_B));
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
            return;
        }
        for (int[] p : PILLARS) {
            if (p[0] != dx || p[1] != dz) continue;
            for (int dy = 1; dy <= 6; dy++) out.add(new ArenaCell(dy, dy == 3 ? PILLAR_BAND : PILLAR));
            out.add(new ArenaCell(7, PILLAR_CAP));
            out.add(new ArenaCell(8, LANTERN));
            return;
        }
    }
}
