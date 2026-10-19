package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: a weight-balance puzzle that can be read at a glance: stone pillars of different HEIGHTS (the height is the weight) stand in a row, a click sends a pillar to the left pan, the right
 * pan or off the scale, and three lamps show which pan is heavier. Every stone must end up on a pan and the pans must balance. The set of weights is drawn so that a perfect split exists (a
 * subset summing to half of an even total), the weights are all different (so there is no trivial swap), and the stones start off the scale. A shove-everything-around party jams the beam
 * at a budget (re-deal, small cost).
 */
public final class WeightScales extends PuzzleBase {
    private final int n;
    private int[] w, sol, side;

    public WeightScales(Params p) {
        super("weight_scales", p);
        n = 4 + (p.lvl() >= 1 ? 1 : 0) + (p.lvl() >= 3 ? 1 : 0);
    }

    @Override protected Layout makeLayout() {
        Layout.Group[] g = {Layout.Group.row("stones", Layout.Role.PILLAR, n, "side").tall(1), Layout.Group.row("beam", Layout.Role.DISPLAY, 3, "tile")};
        return Layout.of(g);
    }

    @Override public int height(int cell) { return cell < n ? w[cell] : 1; }

    @Override protected void init() {
        w = null;
        for (int attempt = 0; attempt < 400 && w == null; attempt++) {
            int[] pool = iota(7);
            shuffle(pool, rng);
            int[] cand = new int[n];
            int total = 0;
            for (int i = 0; i < n; i++) { cand[i] = pool[i] + 1; total += cand[i]; }
            if (total % 2 != 0) continue;
            int mask = split(cand, total / 2);
            if (mask < 0) continue;
            w = cand;
            sol = new int[n];
            for (int i = 0; i < n; i++) sol[i] = (mask >> i & 1) == 1 ? 1 : 2;
        }
        if (w == null) {                                                              // {1,2,3,4}: 1+4 = 2+3 (only reached if 400 draws all failed)
            n4();
        }
        side = new int[n];
        jamBudget = 4 * n + 10;
    }

    private void n4() {
        w = new int[n];
        for (int i = 0; i < n; i++) w[i] = i + 1;
        int total = 0;
        for (int v : w) total += v;
        if (total % 2 != 0) w[n - 1]++;
        total = 0;
        for (int v : w) total += v;
        int mask = split(w, total / 2);
        sol = new int[n];
        for (int i = 0; i < n; i++) sol[i] = (mask >> i & 1) == 1 ? 1 : 2;
    }

    /** A subset of the weights that sums to {@code half}, as a bit mask, or -1. */
    static int split(int[] ws, int half) {
        for (int m = 1; m < (1 << ws.length) - 1; m++) {
            int s = 0;
            for (int i = 0; i < ws.length; i++) if ((m >> i & 1) == 1) s += ws[i];
            if (s == half) return m;
        }
        return -1;
    }

    private int sum(int pan) {
        int s = 0;
        for (int i = 0; i < n; i++) if (side[i] == pan) s += w[i];
        return s;
    }

    private boolean balanced() {
        for (int i = 0; i < n; i++) if (side[i] == 0) return false;
        return sum(1) == sum(2);
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= n) return;
        int i = a.cell();
        side[i] = (side[i] + 1) % 3;
        tone(i, 2 + w[i], "bass");
        if (balanced()) win();
    }

    @Override protected int face(int cell) {
        if (cell < n) return side[cell];
        int l = sum(1), r = sum(2);
        return switch (cell - n) {
            case 0 -> l > r ? 1 : 0;
            case 1 -> l == r && l > 0 ? 1 : 0;
            default -> r > l ? 1 : 0;
        };
    }

    @Override protected Act next(long tick) {
        for (int i = 0; i < n; i++) if (side[i] != sol[i]) return Act.click(0, i, tick);
        return null;
    }
}
