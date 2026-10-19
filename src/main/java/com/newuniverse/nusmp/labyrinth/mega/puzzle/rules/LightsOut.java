package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: the tile-flip grid ("lights out"): a square of floor tiles, a right click on one flips it and its four neighbours, and every tile must end lit. The board is dealt from the solved
 * board by a known set of presses, so a solution always exists; presses commute and undo themselves, so what is still needed is the dealt set XOR the presses made so far, and the rule
 * keeps exactly that. Walking over the tiles never presses them (only a click does). Mashing jams the grid at a budget (small cost, same deal).
 */
public final class LightsOut extends PuzzleBase {
    private final int n;
    private boolean[] on, need;

    public LightsOut(Params p) {
        super("lights_out", p);
        n = 3 + (p.lvl() >= 1 ? 1 : 0) + (p.lvl() >= 3 ? 1 : 0);
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("tiles", Layout.Role.PLATE, n, n, "tile")); }

    private void press(boolean[] state, int i) {
        int r = i / n, c = i % n;
        state[i] = !state[i];
        if (r > 0) state[i - n] = !state[i - n];
        if (r < n - 1) state[i + n] = !state[i + n];
        if (c > 0) state[i - 1] = !state[i - 1];
        if (c < n - 1) state[i + 1] = !state[i + 1];
    }

    @Override protected void init() {
        int cells = n * n;
        do {
            on = new boolean[cells];
            java.util.Arrays.fill(on, true);
            need = new boolean[cells];
            int presses = Math.max(3, cells / 3 + rng.nextInt(3));
            for (int k = 0; k < presses; k++) {
                int i = rng.nextInt(cells);
                need[i] = !need[i];
                press(on, i);
            }
        } while (allOn());
        jamBudget = 2 * cells + 12;
    }

    private boolean allOn() {
        for (boolean b : on) if (!b) return false;
        return true;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        press(on, a.cell());
        need[a.cell()] = !need[a.cell()];
        tone(a.cell(), a.cell() % 12, "chime");
        if (allOn()) win();
    }

    @Override protected int face(int cell) { return on[cell] ? 1 : 0; }

    @Override protected Act next(long tick) {
        for (int i = 0; i < need.length; i++) if (need[i]) return Act.click(0, i, tick);
        return null;
    }
}
