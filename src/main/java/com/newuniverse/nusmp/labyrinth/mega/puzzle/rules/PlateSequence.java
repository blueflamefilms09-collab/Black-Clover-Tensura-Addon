package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: the plain pressure-plate sequence, made fair. A ring of coloured plates is set in the floor and a row of coloured tiles on the wall side spells the order to walk them in; each right
 * plate lights, a wrong one puts the lights out and costs a little health, and the lights also fade if nobody steps for fifteen seconds (so a party that wandered off finds it fresh).
 * Plates are stepped on, never clicked, so two delvers can share the walk. The order is a permutation of the plates drawn from the seed, longer with depth.
 */
public final class PlateSequence extends PuzzleBase {
    private final int n, k;
    private int[] colour, order;
    private boolean[] lit;
    private int step;
    private long lastStepAt;

    public PlateSequence(Params p) {
        super("plate_sequence", p);
        n = Math.min(7, 4 + (p.lvl() >= 2 ? 1 : 0) + (p.lvl() >= 4 ? 1 : 0) + (p.party() >= 4 ? 1 : 0));
        k = Math.min(n, 3 + p.lvl());
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.ring("plates", Layout.Role.PLATE, n, "colour"), Layout.Group.row("inscription", Layout.Role.DISPLAY, k, "colour"));
    }

    @Override protected void init() {
        int[] c = iota(8);
        shuffle(c, rng);
        colour = new int[n];
        System.arraycopy(c, 0, colour, 0, n);
        int[] o = iota(n);
        shuffle(o, rng);
        order = new int[k];
        System.arraycopy(o, 0, order, 0, k);
        lit = new boolean[n];
        step = 0;
        lastStepAt = now;
    }

    private void clear() {
        step = 0;
        lit = new boolean[n];
    }

    @Override protected void handle(Act a) {
        if (!a.stepsOn() || a.cell() >= n) return;
        int i = a.cell();
        if (lit[i]) return;
        if (order[step] == i) {
            lit[i] = true;
            step++;
            lastStepAt = now;
            tone(i, step * 2, "harp");
            spark(i, true);
            if (step == k) win();
        } else {
            clear();
            spark(i, false);
            fail(a.actor(), 1, "hurt", "wrong");
        }
    }

    @Override protected void onTick(long t) {
        if (step > 0 && t - lastStepAt > 300) {
            clear();
            say(-1, "fade", 0);
        }
    }

    @Override protected int face(int cell) {
        if (cell < n) return lit[cell] ? 8 + colour[cell] : colour[cell];
        int j = cell - n;
        return j < step ? 8 + colour[order[j]] : colour[order[j]];
    }

    @Override protected Act next(long tick) { return step < k ? Act.stepOn(0, order[step], tick, 0) : null; }

    /** For the tests: the plates in the order they must be walked. */
    public int[] order() { return order.clone(); }
}
