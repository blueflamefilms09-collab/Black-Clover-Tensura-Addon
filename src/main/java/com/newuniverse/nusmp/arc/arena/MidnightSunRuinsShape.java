package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: the first saga of the story (the Elf Reincarnation arc: the Eye of the Midnight Sun, a cult whose temple lies in ruins) needs a place that reads as
 * "an old sanctuary of a sun that no longer rises": a round floor of weathered stone with cracks and moss, a golden sun inlaid in it (a ring and twelve rays
 * round the dais), a ring of broken pillars of unequal height, a ruined wall whose gaps are low but never open (it is four blocks high everywhere, so nothing
 * leads out, not even from a heap of rubble), rubble on the floor, soul lanterns on stone posts and a few lamps sunk in the paving so the fight is lit at night. The boss stands on the dais
 * in the middle, the travellers appear in an arc on the south side, and four pads at the rim lead home. Pure arithmetic of the column: see {@link ArenaShape}.
 */
final class MidnightSunRuinsShape extends AbstractArenaShape {
    private static final double INNER = 35.0, OUTER = 38.5, DAIS = 5.5;
    private static final int[][] PILLARS = ring(20.0, 15.0, 30.0, 12), LAMP_POSTS = ring(33.0, 22.5, 45.0, 8), LAMPS = ring(17.0, 0.0, 60.0, 6);

    MidnightSunRuinsShape() {
        super("midnight_sun_ruins", "The Ruins of the Midnight Sun", 40, ArenaKit.palette(
                FOUNDATION, "cobbled_deepslate", FLOOR_A, "stone_bricks", FLOOR_B, "polished_deepslate", FLOOR_CRACKED, "cracked_stone_bricks",
                FLOOR_MOSSY, "mossy_stone_bricks", INLAY, "gold_block", INLAY_B, "yellow_terracotta", LAMP_FLOOR, "sea_lantern",
                DAIS_BASE, "chiseled_stone_bricks", DAIS_TOP, "smooth_stone", DAIS_EDGE, "gilded_blackstone", DAIS_RUNE, "crying_obsidian",
                WALL, "stone_bricks", WALL_B, "mossy_stone_bricks", WALL_CAP, "stone_brick_slab", RUBBLE, "cobblestone", RUBBLE_B, "mossy_cobblestone",
                PILLAR, "polished_andesite", PILLAR_BAND, "chiseled_stone_bricks", PILLAR_CAP, "chiseled_stone_bricks",
                POST, "stone_brick_wall", LANTERN, "soul_lantern[hanging=false]", SIGIL_PAD, "gilded_blackstone", SIGIL, "lodestone"),
                new int[] {0, 0}, new int[][] {{31, 0}, {0, 31}, {-31, 0}, {0, -31}});
    }

    /** {@code count} columns on a circle: radius, first angle, step in degrees. */
    private static int[][] ring(double radius, double first, double step, int count) {
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) out[i] = ArenaKit.at(radius, first + i * step);
        return out;
    }

    /** The south-west column of the 2 x 2 pillar this column belongs to, or null. */
    private static int[] pillarBase(int dx, int dz) {
        for (int[] p : PILLARS) if (dx >= p[0] && dx <= p[0] + 1 && dz >= p[1] && dz <= p[1] + 1) return p;
        return null;
    }

    private static int indexOf(int[][] list, int dx, int dz) {
        for (int i = 0; i < list.length; i++) if (list[i][0] == dx && list[i][1] == dz) return i;
        return -1;
    }

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 24.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > OUTER) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= INNER) {
            wall(dx, dz, out);
            return;
        }
        out.add(new ArenaCell(0, r <= DAIS ? DAIS_BASE : floor(dx, dz, r)));
        if (r <= DAIS) {
            out.add(new ArenaCell(1, r <= 1.2 ? DAIS_RUNE : r > 4.3 ? DAIS_EDGE : DAIS_TOP));
            return;
        }
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
            return;
        }
        int[] base = pillarBase(dx, dz);
        if (base != null && clear(base[0], base[1])) {                                                // a pillar is 2 x 2: a thin pole would read as a fence post
            int h = 3 + (int) (ArenaKit.n(base[0], base[1], 5) * 6);
            for (int dy = 1; dy <= h; dy++) out.add(new ArenaCell(dy, dy % 3 == 0 ? PILLAR_BAND : PILLAR));
            if (h >= 6 && dx == base[0] && dz == base[1]) out.add(new ArenaCell(h + 1, PILLAR_CAP));
            return;
        }
        if (indexOf(LAMP_POSTS, dx, dz) >= 0) {
            for (int dy = 1; dy <= 3; dy++) out.add(new ArenaCell(dy, POST));
            out.add(new ArenaCell(4, LANTERN));
            return;
        }
        if (r >= 8.0 && r <= 32.0 && !onSigilPad(dx, dz) && ArenaKit.n(dx, dz, 3) > 0.972 && clear(dx, dz))
            out.add(new ArenaCell(1, ArenaKit.n(dx, dz, 4) > 0.5 ? RUBBLE_B : RUBBLE));
    }

    private ArenaPart floor(int dx, int dz, double r) {
        if (onSigilPad(dx, dz)) return SIGIL_PAD;
        if (indexOf(LAMPS, dx, dz) >= 0) return LAMP_FLOOR;
        if (Math.abs(r - 8.5) <= 0.55) return INLAY;
        if (r >= 9.6 && r <= 15.6) for (int k = 0; k < 12; k++) if (ArenaKit.onRay(dx, dz, 30.0 * k, 0.62)) return INLAY_B;
        double v = ArenaKit.n(dx, dz, 1) + (r > 27.0 ? 0.22 : 0.0);
        return v < 0.50 ? FLOOR_A : v < 0.70 ? FLOOR_CRACKED : v < 0.92 ? FLOOR_MOSSY : FLOOR_B;
    }

    /** The ruined wall: four blocks high everywhere (nothing walks out, not even from a heap of rubble), then broken higher in places up to eight. */
    private void wall(int dx, int dz, List<ArenaCell> out) {
        out.add(new ArenaCell(0, WALL));
        out.add(new ArenaCell(1, WALL));
        out.add(new ArenaCell(2, ArenaKit.n(dx, dz, 6) > 0.5 ? WALL_B : WALL));
        out.add(new ArenaCell(3, WALL));
        double a = Math.toRadians(ArenaKit.deg(dx, dz));
        double wave = 4.4 + 2.6 * Math.sin(a * 4.0 + 0.6) + 1.2 * Math.sin(a * 9.0 + 1.7) + (ArenaKit.n(dx, dz, 7) - 0.5) * 1.6 - (ArenaKit.r(dx, dz) - INNER) * 0.35;
        int top = Math.max(4, Math.min(8, (int) Math.round(wave)));
        for (int dy = 4; dy <= top; dy++) out.add(new ArenaCell(dy, ArenaKit.n(dx, dz, 10 + dy) > 0.65 ? WALL_B : WALL));
        if (top > 4 && ArenaKit.n(dx, dz, 11) > 0.5) out.add(new ArenaCell(top + 1, WALL_CAP));
    }
}
