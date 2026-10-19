package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: two of the puzzles (the watchful sentries and the swinging pendulums) are the same game: cross a field of floor tiles from the start row to the goal row, where rows of "lanes"
 * alternate with safe rows, and a lane is dangerous at some moments. The shared part is the board, the step bookkeeping, the exposure timer (standing on a dangerous lane tile for longer than
 * a short grace counts as being caught: a small cost, and the delver is put back at the start row - a walk back, never a trap), the goal and the hint (wait for a clear moment, step on).
 * A subclass says which lane tile is dangerous when. Lane rows are the odd rows; row 0 is the start, the last row the goal. Time is the game tick the world passes in.
 */
public abstract class LaneRule extends PuzzleBase {
    static final int ACTORS = 8;
    protected final int w, lanes, rows;
    protected final int[] rowOf = new int[ACTORS];
    private final Map<Integer, Set<Integer>> on = new HashMap<>();
    private final int[] exposure = new int[ACTORS];
    private long lastT;

    protected LaneRule(String id, Params p, int w, int lanes) {
        super(id, p);
        this.w = w;
        this.lanes = lanes;
        this.rows = 2 * lanes + 1;
    }

    /** 0 safe, 1 dangerous, 2 about to be dangerous, for the tile in column {@code col} of lane {@code lane} at tick {@code t}. */
    protected abstract int laneState(int lane, int col, long t);

    /** Ticks a delver may stand on a dangerous tile before being caught. */
    protected abstract int grace();

    /** An optional second group of display cells (statues); null for none. */
    protected Layout.Group extraGroup() { return null; }

    protected int extraFace(int j) { return 0; }

    /** Called by a subclass's init. */
    protected final void laneReset() {
        java.util.Arrays.fill(rowOf, 0);
        java.util.Arrays.fill(exposure, 0);
        on.clear();
        lastT = now;
    }

    @Override protected Layout makeLayout() {
        Layout.Group g = Layout.Group.grid("field", Layout.Role.PLATE, w, rows, "lane");
        Layout.Group x = extraGroup();
        return x == null ? Layout.of(g) : Layout.of(g, x);
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.STEP || a.cell() >= w * rows || a.actor() < 0 || a.actor() >= ACTORS) return;
        if (a.stepsOn()) {
            on.computeIfAbsent(a.actor(), k -> new HashSet<>()).add(a.cell());
            int r = a.cell() / w;
            rowOf[a.actor()] = Math.max(rowOf[a.actor()], r);
            if (r == rows - 1) { tone(a.cell(), 19, "bell"); win(); }
        } else {
            Set<Integer> s = on.get(a.actor());
            if (s != null) s.remove(a.cell());
        }
    }

    @Override protected void onTick(long t) {
        int dt = (int) Math.max(0, Math.min(20, t - lastT));
        lastT = t;
        if (dt == 0) return;
        for (int actor = 0; actor < ACTORS; actor++) {
            Set<Integer> s = on.get(actor);
            boolean exposed = false;
            if (s != null) for (int c : s) {
                int r = c / w;
                if (r % 2 == 1 && laneState((r - 1) / 2, c % w, t) == 1) exposed = true;
            }
            if (!exposed) { exposure[actor] = 0; continue; }
            exposure[actor] += dt;
            if (exposure[actor] >= grace()) {
                exposure[actor] = 0;
                rowOf[actor] = 0;
                if (s != null) s.clear();
                emit(Fx.back(actor));
                fail(actor, 1, "hurt", "spotted");
                if (solved) return;
            }
        }
    }

    @Override protected int face(int cell) {
        if (cell >= w * rows) return extraFace(cell - w * rows);
        int r = cell / w;
        if (r == 0) return 3;
        if (r == rows - 1) return 4;
        if (r % 2 == 0) return 0;
        return laneState((r - 1) / 2, cell % w, now);
    }

    @Override protected Act next(long tick) {
        int nextRow = rowOf[0] + 1;
        if (nextRow >= rows) return null;
        if (nextRow % 2 == 0) return Act.stepOn(0, nextRow * w, tick, 4);
        int lane = (nextRow - 1) / 2;
        for (long t = tick; t < tick + 4000; t += 2) {
            for (int col = 0; col < w; col++) {
                boolean clear = true;
                for (long d = 0; d <= 26 && clear; d += 2) clear = laneState(lane, col, t + d) == 0;
                if (clear) return Act.stepOn(0, nextRow * w + col, t, 8);
            }
        }
        return Act.stepOn(0, nextRow * w, tick, 8);
    }
}
