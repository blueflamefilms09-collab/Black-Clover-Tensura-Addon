package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: the dungeon of Orario (DanMachi) is a place without a sky, and its floor bosses (the Goliath of the pit, the Spawn of Charybdis whose old basin went with the
 * Living Dungeon) are fought in a cavern: a round chamber about thirty blocks across whose rough stone wall bulges and recedes (the radius of the wall follows two
 * slow waves of the angle, so it is never a clean circle), a roof of stone bricks twenty-two blocks up, six stone pillars that hold it, a low mound of rock in the
 * middle, patches of moss and cracked stone on the floor, and lamps sunk in the floor and in the wall, because nothing here sees the sun. The boss stands on the
 * mound, the travellers appear in an arc on the south side and two pads at the east and west lead home. This arena is the heaviest to build (a wall and a roof) and
 * the one that never sees the weather. Pure arithmetic: see {@link ArenaShape}.
 */
final class OrarioPitShape extends AbstractArenaShape {
    private static final double WALL_TO = 34.5;
    private static final int ROOF = 22;
    private static final int[][] PILLARS = ring(19.5, 30.0, 60.0, 6), LAMPS_INNER = ring(12.0, 22.5, 45.0, 8), LAMPS_OUTER = ring(22.0, 15.0, 30.0, 12);

    OrarioPitShape() {
        super("orario_pit", "The Pit of the Dungeon", 35, ArenaKit.palette(
                FOUNDATION, "cobbled_deepslate", FLOOR_A, "stone", FLOOR_B, "cobblestone", FLOOR_C, "andesite", FLOOR_CRACKED, "cracked_stone_bricks",
                FLOOR_MOSSY, "mossy_cobblestone", LAMP_FLOOR, "glowstone", DAIS_BASE, "stone", DAIS_TOP, "polished_andesite",
                WALL, "stone", WALL_B, "andesite", RUBBLE_B, "mossy_cobblestone", CEILING, "stone_bricks", CEILING_LAMP, "glowstone",
                PILLAR, "stone_bricks", PILLAR_BAND, "chiseled_stone_bricks", SIGIL_PAD, "chiseled_stone_bricks", SIGIL, "lodestone"),
                new int[] {0, 0}, new int[][] {{26, 0}, {-26, 0}});
    }

    private static int[][] ring(double radius, double first, double step, int count) {
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) out[i] = ArenaKit.at(radius, first + i * step);
        return out;
    }

    private static boolean in(int[][] list, int dx, int dz) {
        for (int[] p : list) if (p[0] == dx && p[1] == dz) return true;
        return false;
    }

    /** The radius of the wall's inner face in the direction of a column: thirty blocks, bulging and receding by about two and a half. */
    static double wallFrom(int dx, int dz) {
        double a = Math.toRadians(ArenaKit.deg(dx, dz));
        return 30.0 + 1.4 * Math.sin(3.0 * a + 0.7) + 1.0 * Math.sin(7.0 * a + 2.1);
    }

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 24.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > WALL_TO) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= wallFrom(dx, dz)) {
            wall(dx, dz, r, out);
            return;
        }
        if (r <= 4.5) {
            out.add(new ArenaCell(0, DAIS_BASE));
            if (r <= 3.2) out.add(new ArenaCell(1, DAIS_TOP));
            out.add(new ArenaCell(ROOF, CEILING));
            return;
        }
        out.add(new ArenaCell(0, floor(dx, dz, r)));
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
        } else if (in(PILLARS, dx, dz) && clear(dx, dz)) {
            for (int dy = 1; dy < ROOF; dy++) out.add(new ArenaCell(dy, dy == 6 || dy == 14 ? PILLAR_BAND : PILLAR));
        } else if (r >= 11.0 && r <= wallFrom(dx, dz) - 1.5 && !onSigilPad(dx, dz) && ArenaKit.n(dx, dz, 51) > 0.955 && clear(dx, dz)) {
            int h = 1 + (int) (ArenaKit.n(dx, dz, 52) * 3);                                           // a rock outcrop one to three high: cover in the open floor
            for (int dy = 1; dy <= h; dy++) out.add(new ArenaCell(dy, dy == 1 && ArenaKit.n(dx, dz, 53) > 0.6 ? RUBBLE_B : WALL_B));
        }
        out.add(new ArenaCell(ROOF, CEILING));
    }

    private ArenaPart floor(int dx, int dz, double r) {
        if (onSigilPad(dx, dz)) return SIGIL_PAD;
        if ((in(LAMPS_INNER, dx, dz) || in(LAMPS_OUTER, dx, dz)) && clear(dx, dz)) return LAMP_FLOOR;
        double v = ArenaKit.n(dx, dz, 21) + (r > 24.0 ? 0.12 : 0.0);
        if (v < 0.40) return FLOOR_A;
        if (v < 0.62) return FLOOR_B;
        if (v < 0.80) return FLOOR_C;
        return v < 0.92 ? FLOOR_CRACKED : FLOOR_MOSSY;
    }

    /** The wall: rough stone from the floor to the roof, with lamps sunk in the lowest part of its face, and the roof's own course on top of it. */
    private void wall(int dx, int dz, double r, List<ArenaCell> out) {
        out.add(new ArenaCell(0, WALL));
        boolean face = r < wallFrom(dx, dz) + 1.1;
        int lamp = face && ArenaKit.n(dx, dz, 31) > 0.86 ? 3 + (int) (ArenaKit.n(dx, dz, 32) * 8) : -1;
        for (int dy = 1; dy < ROOF; dy++) {
            double v = ArenaKit.n(dx, dz, 40 + dy);
            out.add(new ArenaCell(dy, dy == lamp ? CEILING_LAMP : v > 0.93 ? RUBBLE_B : v > 0.72 ? WALL_B : WALL));
        }
        out.add(new ArenaCell(ROOF, CEILING));
    }
}
