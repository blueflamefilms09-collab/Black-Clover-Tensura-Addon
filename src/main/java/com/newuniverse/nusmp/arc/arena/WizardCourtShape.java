package com.newuniverse.nusmp.arc.arena;

import java.util.List;

import static com.newuniverse.nusmp.arc.arena.ArenaPart.*;

/**
 * Why: the last saga of the Black Clover story is the Wizard King's, and its arena is the opposite of the Spade Kingdom's black hall: a bright round court of
 * white quartz in the open air, a gold star (a ring, eight rays and a second ring) inlaid in the floor, a low dais of two steps in the middle, eight tall white
 * pillars each crowned by a sea lantern, and a pale wall five blocks high that closes the court. The boss stands on the dais, the travellers appear in an arc on
 * the south side and four pads at the rim lead home. (Lucius himself is fought in the arena of the Final Hour, which has its own courtyard; this court is
 * the arena of every other character of the saga and the fallback when the Final Hour's dimension is not available.) Pure arithmetic: see {@link ArenaShape}.
 */
final class WizardCourtShape extends AbstractArenaShape {
    private static final double WALL_FROM = 33.5, WALL_TO = 36.5, DAIS = 5.5;
    private static final int[][] PILLARS = ring(27.0, 22.5, 45.0, 8), LAMPS = ring(18.0, 22.5, 45.0, 8);

    WizardCourtShape() {
        super("wizard_court", "The Court of the Wizard King", 37, ArenaKit.withStairs(ArenaKit.palette(
                FOUNDATION, "stone_bricks", FLOOR_A, "quartz_block", FLOOR_B, "smooth_quartz", FLOOR_C, "quartz_bricks", INLAY, "gold_block",
                INLAY_B, "lapis_block", LAMP_FLOOR, "sea_lantern", DAIS_BASE, "quartz_block", DAIS_TOP, "smooth_quartz",
                DAIS_EDGE, "chiseled_quartz_block", DAIS_RUNE, "gold_block", WALL, "smooth_quartz", WALL_B, "quartz_bricks", WALL_CAP, "quartz_slab",
                PILLAR, "quartz_pillar[axis=y]", PILLAR_BAND, "chiseled_quartz_block", PILLAR_CAP, "chiseled_quartz_block", CEILING_LAMP, "sea_lantern",
                SIGIL_PAD, "gold_block", SIGIL, "lodestone"), "quartz_stairs"),
                new int[] {0, 0}, new int[][] {{30, 0}, {0, 30}, {-30, 0}, {0, -30}});
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

    @Override
    protected int[][] arrival(int n) { return ArenaKit.arc(n, 22.0, 90.0, 3.0); }

    @Override
    protected void fill(int dx, int dz, List<ArenaCell> out) {
        double r = ArenaKit.r(dx, dz);
        if (r > WALL_TO) return;
        out.add(new ArenaCell(-2, FOUNDATION));
        out.add(new ArenaCell(-1, FOUNDATION));
        if (r >= WALL_FROM) {
            out.add(new ArenaCell(0, WALL));
            for (int dy = 1; dy <= 5; dy++) out.add(new ArenaCell(dy, dy == 3 ? WALL_B : WALL));
            out.add(new ArenaCell(6, WALL_CAP));
            return;
        }
        if (r <= DAIS + 1.0) {
            out.add(new ArenaCell(0, DAIS_BASE));
            if (r <= DAIS) out.add(new ArenaCell(1, r <= 1.2 ? DAIS_RUNE : r > 4.5 ? DAIS_EDGE : DAIS_TOP));
            else out.add(new ArenaCell(1, ArenaKit.stepToCentre(dx, dz)));
            return;
        }
        out.add(new ArenaCell(0, floor(dx, dz, r)));
        if (onSigil(dx, dz)) {
            out.add(new ArenaCell(1, SIGIL));
            return;
        }
        if (in(PILLARS, dx, dz) && clear(dx, dz)) {
            for (int dy = 1; dy <= 8; dy++) out.add(new ArenaCell(dy, dy == 4 ? PILLAR_BAND : PILLAR));
            out.add(new ArenaCell(9, PILLAR_CAP));
            out.add(new ArenaCell(10, CEILING_LAMP));
        }
    }

    private ArenaPart floor(int dx, int dz, double r) {
        if (onSigilPad(dx, dz)) return SIGIL_PAD;
        if (in(LAMPS, dx, dz) && clear(dx, dz)) return LAMP_FLOOR;
        if (Math.abs(r - 12.0) <= 0.55 || Math.abs(r - 24.0) <= 0.55) return INLAY;
        if (r >= 13.0 && r <= 23.0) for (int k = 0; k < 8; k++) if (ArenaKit.onRay(dx, dz, 45.0 * k, 0.6)) return INLAY;
        if (r >= 31.0 && r <= 32.0) return INLAY_B;
        return ((dx + dz) & 1) == 0 ? FLOOR_A : FLOOR_B;
    }
}
