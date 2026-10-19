package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: shadow casting: a lamp throws the shadows of three to five turnable pillars on a row of six wall tiles, and a second row shows the shadow the vault demands. Each pillar has four
 * faces; turned to a face it shades its own two or three tiles (every face a different set), the shadow row is the union of all pillars' shades, and the target is the union of one face of
 * each. Any combination that casts exactly the target is accepted (there is often more than one), the shadow row updates at every click so the effect of a turn can be read straight off,
 * and the hint chooses the cheapest combination in clicks. Mashing jams the lamp (small cost, same deal).
 */
public final class ShadowCast extends PuzzleBase {
    private static final int CELLS = 6;
    private final int k;
    private int[][] mask;
    private int[] o;
    private int target;

    public ShadowCast(Params p) {
        super("shadow_cast", p);
        k = 3 + (p.lvl() >= 2 ? 1 : 0) + (p.lvl() >= 4 ? 1 : 0);
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("pillars", Layout.Role.PILLAR, k, "arrow"), Layout.Group.row("shadow", Layout.Role.DISPLAY, CELLS, "tile"), Layout.Group.row("wanted", Layout.Role.DISPLAY, CELLS, "tile"));
    }

    private int randomMask() {
        int m = 0;
        int bits = 2 + rng.nextInt(2);
        while (Integer.bitCount(m) < bits) m |= 1 << rng.nextInt(CELLS);
        return m;
    }

    @Override protected void init() {
        mask = new int[k][4];
        for (int i = 0; i < k; i++) {
            for (int f = 0; f < 4; f++) {
                int m;
                boolean dup;
                do {
                    m = randomMask();
                    dup = false;
                    for (int g = 0; g < f; g++) dup |= mask[i][g] == m;
                } while (dup);
                mask[i][f] = m;
            }
        }
        target = 0;
        for (int i = 0; i < k; i++) target |= mask[i][rng.nextInt(4)];
        o = new int[k];
        do {
            for (int i = 0; i < k; i++) o[i] = rng.nextInt(4);
        } while (shade() == target);
        jamBudget = 6 * k + 12;
    }

    private int shade() {
        int m = 0;
        for (int i = 0; i < k; i++) m |= mask[i][o[i]];
        return m;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= k) return;
        int i = a.cell();
        o[i] = (o[i] + 1) % 4;
        tone(i, 2 + o[i] * 3, "bass");
        if (shade() == target) win();
    }

    @Override protected int face(int cell) {
        if (cell < k) return o[cell];
        int j = cell - k;
        if (j < CELLS) return (shade() >> j & 1) == 1 ? 0 : 1;
        return (target >> (j - CELLS) & 1) == 1 ? 0 : 1;
    }

    @Override protected Act next(long tick) {
        int best = Integer.MAX_VALUE;
        int[] pick = null;
        int[] cand = new int[k];
        int total = 1;
        for (int i = 0; i < k; i++) total *= 4;
        for (int code = 0; code < total; code++) {
            int c = code, m = 0, cost = 0;
            for (int i = 0; i < k; i++) { cand[i] = c & 3; c >>= 2; m |= mask[i][cand[i]]; cost += (cand[i] - o[i] + 4) % 4; }
            if (m == target && cost < best) { best = cost; pick = cand.clone(); }
        }
        if (pick == null) return null;
        for (int i = 0; i < k; i++) if (pick[i] != o[i]) return Act.click(0, i, tick);
        return null;
    }
}
