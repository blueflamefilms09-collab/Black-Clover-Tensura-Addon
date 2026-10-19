package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: colour matching with a rule everyone knows from paint: pairs of pillars feed a lamp, a pillar cycles empty, red, blue, yellow on a click, and a lamp shows what its two pillars make
 * (red and blue make purple, red and yellow orange, blue and yellow green, two alike or one alone give that colour, nothing gives dark). A second row shows the colour each lamp must show.
 * Any pair that makes the target colour is accepted, so the puzzle has several answers and no trick; the party can see every effect of a click at once. Mashing jams the palette (small
 * cost, same deal again).
 */
public final class ColourMix extends PuzzleBase {
    private final int m;
    private int[] pil, target;

    public ColourMix(Params p) {
        super("colour_mix", p);
        m = Math.min(4, 2 + (p.lvl() >= 2 ? 1 : 0) + (p.party() >= 4 ? 1 : 0));
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("pillars", Layout.Role.PILLAR, 2 * m, "primary"), Layout.Group.row("lamps", Layout.Role.DISPLAY, m, "primary"), Layout.Group.row("targets", Layout.Role.DISPLAY, m, "primary"));
    }

    /** The mixing table: 0 dark, 1 red, 2 blue, 3 yellow, 4 purple, 5 orange, 6 green. */
    static int mix(int a, int b) {
        if (a == 0) return b;
        if (b == 0 || a == b) return a;
        int lo = Math.min(a, b), hi = Math.max(a, b);
        if (lo == 1 && hi == 2) return 4;
        if (lo == 1 && hi == 3) return 5;
        return 6;
    }

    @Override protected void init() {
        target = new int[m];
        for (int j = 0; j < m; j++) {
            int t;
            do { t = mix(rng.nextInt(4), rng.nextInt(4)); } while (t == 0);
            target[j] = t;
        }
        pil = new int[2 * m];
        do {
            for (int i = 0; i < 2 * m; i++) pil[i] = rng.nextInt(4);
        } while (allMatch());
        jamBudget = 14 + 6 * m;
    }

    private boolean allMatch() {
        for (int j = 0; j < m; j++) if (mix(pil[2 * j], pil[2 * j + 1]) != target[j]) return false;
        return true;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= 2 * m) return;
        int i = a.cell();
        pil[i] = (pil[i] + 1) % 4;
        tone(i, 4 + pil[i] * 3, "harp");
        if (allMatch()) win();
    }

    @Override protected int face(int cell) {
        if (cell < 2 * m) return pil[cell];
        int j = cell - 2 * m;
        if (j < m) return mix(pil[2 * j], pil[2 * j + 1]);
        return target[j - m];
    }

    @Override protected Act next(long tick) {
        for (int j = 0; j < m; j++) {
            if (mix(pil[2 * j], pil[2 * j + 1]) == target[j]) continue;
            int best = Integer.MAX_VALUE, ba = 0, bb = 0;
            for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) {
                if (mix(a, b) != target[j]) continue;
                int c = (a - pil[2 * j] + 4) % 4 + (b - pil[2 * j + 1] + 4) % 4;
                if (c < best) { best = c; ba = a; bb = b; }
            }
            if (ba != pil[2 * j]) return Act.click(0, 2 * j, tick);
            if (bb != pil[2 * j + 1]) return Act.click(0, 2 * j + 1, tick);
        }
        return null;
    }
}
