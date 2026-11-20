package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: Goku is fought in a wasteland round a martial-arts tournament ring (the boss realm, docs/boss_realm.md): a square white ring of quartz one block above a wide tiled apron, a pillar and a
 * lantern at each corner of the ring, all of it standing in a cracked red-earth wasteland with craters, scattered rubble, six rock spires and a cliff of terracotta five blocks high that closes it.
 * It is flat on purpose (a fight with a boss that is fast and flies at you needs no hiding place) and open (the blasts of the fight have room). Pure arithmetic: see {@link ArenaShape}.
 */
final class TournamentGroundsShape extends AbstractArenaShape {
    private static final int RING = 9, APRON = 13;
    private static final double WALL_FROM = 36.0, WALL_TO = 38.5;
    private static final int[][] SPIRES = ring(27.0, 15.0, 60.0, 6), CRATERS = ring(22.0, 40.0, 120.0, 3);

    TournamentGroundsShape() {
        super("tournament_grounds", "The Tournament Wastes", 40, ArenaKit.palette(
                FOUNDATION, "stone", FLOOR_A, "terracotta", FLOOR_B, "coarse_dirt", FLOOR_C, "polished_diorite", FLOOR_CRACKED, "cracked_stone_bricks", INLAY, "chiseled_quartz_block",
                LAMP_FLOOR, "glowstone", DAIS_BASE, "smooth_quartz", DAIS_TOP, "quartz_block", DAIS_EDGE, "chiseled_quartz_block", DAIS_RUNE, "gold_block",
                WALL, "terracotta", WALL_B, "orange_terracotta", WALL_CAP, "red_sandstone_slab", PILLAR, "quartz_pillar[axis=y]", PILLAR_CAP, "chiseled_quartz_block",
                LANTERN, "lantern[hanging=false]", STONE, "andesite", STONE_B, "stone", RUBBLE, "cobblestone", SIGIL_PAD, "polished_andesite", SIGIL, "lodestone"),
                new int[] {0, 0}, new int[][] {{31, 0}, {0, 31}, {-31, 0}, {0, -31}});
    }

    private static int[][] ring(double radius, double first, double step, int count) {
        int[][] out = new int[count][];
        for (int i = 0; i < count; i++) out[i] = ArenaKit.at(radius, first + i * step);
        return out;
    }

    private static int spireAt(int dx, int dz) {
        for (int i = 0; i < SPIRES.length; i++) if (dx >= SPIRES[i][0] && dx <= SPIRES[i][0] + 1 && dz >= SPIRES[i][1] && dz <= SPIRES[i][1] + 1) return i;
        return -1;
    }

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 17.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > WALL_TO) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= WALL_FROM) {                                                                         // the cliff: terracotta, a band of orange, a slab on top
            for (int dy = 0; dy <= 4; dy++) out.add(new ArenaCell(dy, dy == 2 ? WALL_B : WALL));
            out.add(new ArenaCell(5, WALL_CAP));
            return;
        }
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (ax <= RING && az <= RING) {                                                               // the ring: white quartz, one block above the apron, a pillar and a lantern at each corner
            out.add(new ArenaCell(0, DAIS_BASE));
            out.add(new ArenaCell(1, ax == RING || az == RING ? DAIS_EDGE : ax <= 1 && az <= 1 ? DAIS_RUNE : DAIS_TOP));
            if (ax == RING && az == RING) {
                for (int dy = 2; dy <= 4; dy++) out.add(new ArenaCell(dy, PILLAR));
                out.add(new ArenaCell(5, PILLAR_CAP));
                out.add(new ArenaCell(6, LANTERN));
            }
            return;
        }
        if (ax <= APRON && az <= APRON) {                                                             // the apron: polished tiles, a ring of chiselled quartz, a lamp at each corner
            out.add(new ArenaCell(0, ax == APRON && az == APRON ? LAMP_FLOOR : ax == APRON || az == APRON ? INLAY : FLOOR_C));
            return;
        }
        if (onSigilPad(dx, dz)) {
            out.add(new ArenaCell(0, SIGIL_PAD));
            if (onSigil(dx, dz)) out.add(new ArenaCell(1, SIGIL));
            return;
        }
        ArenaPart ground = ground(dx, dz);
        out.add(new ArenaCell(0, ground));
        int s = spireAt(dx, dz);
        if (s >= 0 && clear(SPIRES[s][0], SPIRES[s][1])) {                                           // a rock spire: 2 x 2, four to seven high, every other one lit
            int h = 4 + (int) (ArenaKit.n(SPIRES[s][0], SPIRES[s][1], 21) * 4);
            for (int dy = 1; dy <= h; dy++) out.add(new ArenaCell(dy, (dy + s) % 3 == 0 ? STONE_B : STONE));
            if (s % 2 == 0 && dx == SPIRES[s][0] && dz == SPIRES[s][1]) out.add(new ArenaCell(h + 1, LANTERN));
            return;
        }
        if (ground != FLOOR_CRACKED && clear(dx, dz) && ArenaKit.n(dx, dz, 9) > 0.975) out.add(new ArenaCell(1, RUBBLE));
    }

    private ArenaPart ground(int dx, int dz) {
        for (int[] c : CRATERS) if (ArenaKit.r(dx - c[0], dz - c[1]) <= 5.0) return FLOOR_CRACKED;   // a crater: cracked bricks
        return ArenaKit.n(dx, dz, 8) > 0.55 ? FLOOR_B : FLOOR_A;                                      // the wasteland: red terracotta and coarse dirt
    }
}
