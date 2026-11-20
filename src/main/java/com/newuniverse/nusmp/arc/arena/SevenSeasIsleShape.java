package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: Sinbad is the King of the Seven Seas, so the place he is fought in (the boss realm, docs/boss_realm.md) is a sea and a sky: a round island of sand and grass with a sandstone dais in the
 * middle (where he waits), five palm trees, and all round it a shallow sea of light-blue and cyan blocks laid in rings of prismarine like waves, with seven dark-prismarine pillars standing in it
 * (one for each of his seven djinn, each lit by a lantern), closed by a wall of prismarine bricks five blocks high. The dimension keeps its noon sky, so the sky is the open sky above the wall. No
 * water is placed (a liquid in a void would flow and tick); the sea is a floor, which also keeps every column a plain stack the closure tests can walk. Pure arithmetic: see {@link ArenaShape}.
 */
final class SevenSeasIsleShape extends AbstractArenaShape {
    private static final double ISLE = 13.0, BEACH = 10.0, DAIS = 4.0, WALL_FROM = 36.0, WALL_TO = 38.5, PILLAR_RING = 25.0;
    private static final int[][] PILLARS = ring(PILLAR_RING, 12.0, 360.0 / 7.0, 7), PALMS = ring(7.0, 20.0, 72.0, 5);

    SevenSeasIsleShape() {
        super("seven_seas_isle", "The Isle of the Seven Seas", 40, ArenaKit.palette(
                FOUNDATION, "stone", FLOOR_A, "sand", FLOOR_B, "light_blue_concrete", FLOOR_C, "cyan_concrete", INLAY, "prismarine", INLAY_B, "prismarine_bricks",
                LAMP_FLOOR, "sea_lantern", GRASS, "grass_block", DAIS_BASE, "sandstone", DAIS_TOP, "smooth_sandstone", DAIS_EDGE, "chiseled_sandstone", DAIS_RUNE, "gold_block",
                WALL, "prismarine_bricks", WALL_B, "dark_prismarine", WALL_CAP, "prismarine_brick_slab", PILLAR, "dark_prismarine", PILLAR_BAND, "prismarine", PILLAR_CAP, "prismarine_bricks",
                LANTERN, "lantern[hanging=false]", LOG, "jungle_log[axis=y]", LEAVES, "jungle_leaves[persistent=true]", FLOWER_A, "dandelion", FLOWER_B, "cornflower", TALL_GRASS, "short_grass",
                SIGIL_PAD, "prismarine_bricks", SIGIL, "lodestone"),
                new int[] {0, 0}, new int[][] {{31, 0}, {0, 31}, {-31, 0}, {0, -31}});
    }

    private static int[][] ring(double radius, double first, double step, int count) {
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) out[i] = ArenaKit.at(radius, first + i * step);
        return out;
    }

    private static int indexOf(int[][] list, int dx, int dz) {
        for (int i = 0; i < list.length; i++) if (list[i][0] == dx && list[i][1] == dz) return i;
        return -1;
    }

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 11.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > WALL_TO) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= WALL_FROM) {                                                                         // the sea wall: prismarine, five high, banded with dark prismarine
            for (int dy = 0; dy <= 4; dy++) out.add(new ArenaCell(dy, dy == 2 ? WALL_B : WALL));
            out.add(new ArenaCell(5, WALL_CAP));
            return;
        }
        if (r <= DAIS) {                                                                              // the sandstone dais, a gold rune in its heart
            out.add(new ArenaCell(0, DAIS_BASE));
            out.add(new ArenaCell(1, r <= 1.2 ? DAIS_RUNE : r > 3.0 ? DAIS_EDGE : DAIS_TOP));
            return;
        }
        if (r <= ISLE) {
            ArenaPart ground = r >= BEACH ? FLOOR_A : GRASS;
            out.add(new ArenaCell(0, ground));
            if (ground == GRASS && !palm(dx, dz, out) && clear(dx, dz)) {
                double v = ArenaKit.n(dx, dz, 7);
                if (v > 0.97) out.add(new ArenaCell(1, FLOWER_A));
                else if (v > 0.945) out.add(new ArenaCell(1, FLOWER_B));
                else if (v > 0.6 && v < 0.75) out.add(new ArenaCell(1, TALL_GRASS));
            }
            return;
        }
        out.add(new ArenaCell(0, onSigilPad(dx, dz) ? SIGIL_PAD : sea(dx, dz, r)));                   // the sea: a floor of blue blocks with rings of prismarine for waves
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
            return;
        }
        if (indexOf(PILLARS, dx, dz) >= 0) {                                                          // one pillar for each djinn, a lantern on its cap
            for (int dy = 1; dy <= 6; dy++) out.add(new ArenaCell(dy, dy == 3 ? PILLAR_BAND : PILLAR));
            out.add(new ArenaCell(7, PILLAR_CAP));
            out.add(new ArenaCell(8, LANTERN));
        }
    }

    private ArenaPart sea(int dx, int dz, double r) {
        double wave = Math.abs(((r - ISLE) % 6.0) - 3.0);                                             // a ring of prismarine every six blocks
        if (wave < 0.5) return INLAY;
        if (r > 30.0 && wave < 1.0) return INLAY_B;
        if (ArenaKit.n(dx, dz, 5) > 0.992 && clear(dx, dz)) return LAMP_FLOOR;                        // a few lamps under the water-coloured floor keep the night from being black
        return ArenaKit.n(dx, dz, 6) > 0.7 ? FLOOR_C : FLOOR_B;
    }

    /** A palm at one of five spots: a trunk four high and a crown of jungle leaves. True when the column was written. */
    private boolean palm(int dx, int dz, List<ArenaCell> out) {
        for (int[] t : PALMS) {
            double d = ArenaKit.r(dx - t[0], dz - t[1]);
            if (d > 2.4 || !clear(t[0], t[1])) continue;
            if (d == 0) {
                for (int dy = 1; dy <= 4; dy++) out.add(new ArenaCell(dy, LOG));
                out.add(new ArenaCell(5, LEAVES));
                out.add(new ArenaCell(6, LEAVES));
                return true;
            }
            out.add(new ArenaCell(4, LEAVES));
            if (d <= 1.6) out.add(new ArenaCell(5, LEAVES));
            return true;
        }
        return false;
    }
}
