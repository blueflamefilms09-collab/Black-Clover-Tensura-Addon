package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: memory tiles, the walk-on version of "find the pairs". A field of identical floor tiles hides pairs of symbols; stepping on a tile shows its symbol, a second tile shows its
 * partner or not, a mismatch hides both again after a second and a half. The party may make a fixed number of mismatches (a few more than there are pairs); one more and the floor
 * shuffles itself (a new deal, a small cost, a short pause). Several delvers can walk different tiles, so a big party really is faster. Matched tiles stay lit.
 */
public final class MemoryTiles extends PuzzleBase {
    private final int cols, rows, pairs;
    private int[] sym;
    private boolean[] up, done;
    private int first, miss, remaining;
    private int hideA = -1, hideB = -1;
    private long busyUntil;

    public MemoryTiles(Params p) {
        super("memory_tiles", p);
        cols = p.lvl() >= 4 ? 5 : 4;
        rows = p.lvl() >= 2 ? 4 : 3;
        pairs = cols * rows / 2;
        reshuffleOnRestart = true;
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("tiles", Layout.Role.PLATE, cols, rows, "symbol")); }

    @Override protected void init() {
        int n = cols * rows;
        sym = new int[n];
        int[] order = iota(n);
        shuffle(order, rng);
        int[] kinds = iota(12);
        shuffle(kinds, rng);
        for (int i = 0; i < n; i++) sym[order[i]] = 1 + kinds[(i / 2) % 12];
        up = new boolean[n];
        done = new boolean[n];
        first = -1;
        miss = 0;
        remaining = pairs;
        hideA = hideB = -1;
        busyUntil = 0;
    }

    @Override protected int failLimit() { return 1; }

    @Override protected void handle(Act a) {
        if (!a.stepsOn()) return;
        int i = a.cell();
        if (done[i] || up[i] || now < busyUntil) return;
        up[i] = true;
        tone(i, 4 + sym[i], "chime");
        if (first < 0) { first = i; return; }
        int f = first;
        first = -1;
        if (sym[f] == sym[i]) {
            done[f] = done[i] = true;
            spark(i, true);
            tone(i, 16, "bell");
            if (--remaining == 0) win();
        } else {
            hideA = f;
            hideB = i;
            busyUntil = now + 30;
            spark(i, false);
            if (++miss > pairs + 3) fail(a.actor(), 1, "hurt", "shuffle");
        }
    }

    @Override protected void onTick(long t) {
        if (hideA >= 0 && t >= busyUntil) {
            up[hideA] = up[hideB] = false;
            hideA = hideB = -1;
        }
    }

    @Override protected int face(int cell) { return done[cell] || up[cell] ? sym[cell] : 0; }

    @Override protected Act next(long tick) {
        long t = hideA >= 0 ? Math.max(tick, busyUntil) : tick;
        if (first >= 0) {
            for (int j = 0; j < sym.length; j++) if (j != first && !done[j] && !up[j] && sym[j] == sym[first]) return Act.stepOn(0, j, t, 0);
        }
        for (int j = 0; j < sym.length; j++) if (!done[j] && !up[j]) return Act.stepOn(0, j, t, 0);
        return hideA >= 0 ? Act.stepOn(0, hideA, t, 0) : null;
    }
}
