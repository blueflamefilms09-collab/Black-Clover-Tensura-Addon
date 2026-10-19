package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: rotating pillars that must all be turned to face the compass stone. Every pillar turns a quarter on a right click and, where two gears mesh, drags its neighbour round with it, so
 * the order of the clicks does not matter but their number does. The board is dealt by scrambling the solved board with a known number of clicks per pillar; four clicks on one pillar are
 * the identity, so the answer is simply "four minus what was dealt" for each pillar, and it stays valid however many extra clicks the party makes (the need is tracked per pillar). A party
 * that mashes jams the mechanism at a budget, which re-deals the same board and costs a little.
 */
public final class RotatingPillars extends PuzzleBase {
    private final int n;
    private boolean[] coupled;
    private int[] s, need;

    public RotatingPillars(Params p) {
        super("rotating_pillars", p);
        n = Math.min(6, 3 + (p.lvl() >= 2 ? 1 : 0) + (p.lvl() >= 4 ? 1 : 0) + (p.party() >= 4 ? 1 : 0));
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.ring("pillars", Layout.Role.PILLAR, n, "arrow"), Layout.Group.row("compass", Layout.Role.DISPLAY, 1, "arrow"));
    }

    @Override protected void init() {
        coupled = new boolean[n];
        int c = 0;
        for (int i = 0; i < n; i++) { coupled[i] = rng.nextInt(100) < 55; if (coupled[i]) c++; }
        if (c == 0) coupled[0] = true;
        int[] k;
        do {
            k = new int[n];
            for (int i = 0; i < n; i++) k[i] = rng.nextInt(4);
            s = new int[n];
            for (int i = 0; i < n; i++) for (int r = 0; r < k[i]; r++) turn(i);
        } while (allZero());
        need = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) { need[i] = (4 - k[i]) % 4; total += need[i]; }
        jamBudget = total * 2 + 8;
    }

    private void turn(int i) {
        s[i] = (s[i] + 1) % 4;
        if (coupled[i]) s[(i + 1) % n] = (s[(i + 1) % n] + 1) % 4;
    }

    private boolean allZero() {
        for (int v : s) if (v != 0) return false;
        return true;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= n) return;
        int i = a.cell();
        turn(i);
        need[i] = (need[i] + 3) % 4;
        tone(i, 3 + s[i] * 4, "bell");
        if (allZero()) win();
    }

    @Override protected int face(int cell) { return cell < n ? s[cell] : 0; }

    @Override protected Act next(long tick) {
        for (int i = 0; i < n; i++) if (need[i] > 0) return Act.click(0, i, tick);
        return null;
    }
}
