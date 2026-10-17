package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: the Seven Deadly Sins are a road-story, and their fights belong in the open: a round meadow in the daylight (the dimension keeps the world's own day and
 * night, so a boss whose strength follows the sun has a sun to follow), a cross of dirt paths over short grass, a circle of coarse earth in the middle ringed by
 * eight standing stones, wild flowers and tall grass, seven oak trees and a few mossy boulders, a wooden fence and, behind it, a hedge of leaves four blocks high
 * that closes the meadow (the fence stops a walker on its own, the hedge is what a jump would land in). Lanterns on fence posts and on four of the stones keep
 * the night from being black. The boss stands in the middle, the travellers appear in an arc on the south side and four pads at the rim lead home. Pure
 * arithmetic: see {@link ArenaShape}.
 */
final class SunlitPlainShape extends AbstractArenaShape {
    private static final double FENCE_FROM = 35.0, HEDGE_FROM = 36.0, HEDGE_TO = 38.5, CIRCLE = 7.0;
    private static final int[][] STONES = ring(15.0, 22.5, 45.0, 8), POSTS = ring(33.5, 22.5, 45.0, 8), TREES = ring(29.0, 11.0, 360.0 / 7.0, 7);

    SunlitPlainShape() {
        super("sunlit_plain", "The Sunlit Plain", 40, ArenaKit.palette(
                FOUNDATION, "dirt", GRASS, "grass_block", DIRT, "dirt", PATH, "dirt_path", COARSE, "coarse_dirt", STONE, "andesite", STONE_B, "mossy_cobblestone",
                RUBBLE, "cobblestone", POST, "oak_fence", LANTERN, "lantern[hanging=false]", HEDGE, "oak_leaves[persistent=true]", LOG, "oak_log[axis=y]",
                LEAVES, "oak_leaves[persistent=true]", FLOWER_A, "dandelion", FLOWER_B, "cornflower", TALL_GRASS, "short_grass",
                SIGIL_PAD, "stone_bricks", SIGIL, "lodestone"),
                new int[] {0, 0}, new int[][] {{30, 0}, {0, 30}, {-30, 0}, {0, -30}});
    }

    private static int[][] ring(double radius, double first, double step, int count) {
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) out[i] = ArenaKit.at(radius, first + i * step);
        return out;
    }

    /** The index of the standing stone whose 2 x 2 footprint holds this column, or -1. */
    private static int stoneAt(int dx, int dz) {
        for (int i = 0; i < STONES.length; i++) if (dx >= STONES[i][0] && dx <= STONES[i][0] + 1 && dz >= STONES[i][1] && dz <= STONES[i][1] + 1) return i;
        return -1;
    }

    private static int indexOf(int[][] list, int dx, int dz) {
        for (int i = 0; i < list.length; i++) if (list[i][0] == dx && list[i][1] == dz) return i;
        return -1;
    }

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 22.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > HEDGE_TO) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= HEDGE_FROM) {                                                                        // the hedge: four blocks of leaves on earth, too high to climb from the fence
            out.add(new ArenaCell(0, DIRT));
            for (int dy = 1; dy <= 4; dy++) out.add(new ArenaCell(dy, HEDGE));
            return;
        }
        ArenaPart ground = ground(dx, dz, r);
        out.add(new ArenaCell(0, ground));
        if (r >= FENCE_FROM) {                                                                        // a fence two rails high: a walker cannot jump it
            out.add(new ArenaCell(1, POST));
            out.add(new ArenaCell(2, POST));
            return;
        }
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
            return;
        }
        int s = stoneAt(dx, dz);
        if (s >= 0 && clear(STONES[s][0], STONES[s][1])) {                                           // a standing stone is 2 x 2 and 3 to 5 high; every other one carries a lantern
            int h = 3 + (int) (ArenaKit.n(STONES[s][0], STONES[s][1], 12) * 3);
            for (int dy = 1; dy <= h; dy++) out.add(new ArenaCell(dy, STONE));
            if (s % 2 == 0 && dx == STONES[s][0] && dz == STONES[s][1]) out.add(new ArenaCell(h + 1, LANTERN));
            return;
        }
        if (indexOf(POSTS, dx, dz) >= 0) {
            for (int dy = 1; dy <= 3; dy++) out.add(new ArenaCell(dy, POST));
            out.add(new ArenaCell(4, LANTERN));
            return;
        }
        if (tree(dx, dz, out)) return;
        if (ground == GRASS && clear(dx, dz) && !onSigilPad(dx, dz)) {
            double v = ArenaKit.n(dx, dz, 4);
            if (v > 0.975) out.add(new ArenaCell(1, FLOWER_A));
            else if (v > 0.955) out.add(new ArenaCell(1, FLOWER_B));
            else if (v > 0.62 && v < 0.78) out.add(new ArenaCell(1, TALL_GRASS));
            else if (r >= 10.0 && r <= 31.0 && ArenaKit.n(dx, dz, 3) > 0.985) out.add(new ArenaCell(1, STONE_B));
        }
    }

    private ArenaPart ground(int dx, int dz, double r) {
        if (onSigilPad(dx, dz)) return SIGIL_PAD;
        if (r <= CIRCLE) return COARSE;
        if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) return PATH;
        return GRASS;
    }

    /** A tree at one of the seven spots: a trunk five high and a rounded crown of leaves round its top. True when the column was written. */
    private boolean tree(int dx, int dz, List<ArenaCell> out) {
        for (int[] t : TREES) {
            double d = ArenaKit.r(dx - t[0], dz - t[1]);
            if (d > 2.4 || !clear(t[0], t[1])) continue;
            if (d == 0) {
                for (int dy = 1; dy <= 5; dy++) out.add(new ArenaCell(dy, LOG));
                out.add(new ArenaCell(6, LEAVES));
                out.add(new ArenaCell(7, LEAVES));
                return true;
            }
            out.add(new ArenaCell(4, LEAVES));
            out.add(new ArenaCell(5, LEAVES));
            if (d <= 1.6) out.add(new ArenaCell(6, LEAVES));
            if (d <= 1.2) out.add(new ArenaCell(7, LEAVES));
            return true;
        }
        return false;
    }
}
