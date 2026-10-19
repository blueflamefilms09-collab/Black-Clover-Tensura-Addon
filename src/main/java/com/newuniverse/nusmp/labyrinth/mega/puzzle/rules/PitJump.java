package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: plate-and-pit jumping, solved by reading. A field of red, blue and gold tiles lies in rows between the start and the far wall, and a row of coloured tiles on the wall (the legend)
 * names the colour that is safe in each row, from the near row to the far one. Step on a tile of the legend's colour and the row is passed; step on any other and the tile gives way (a
 * one-block pit, the delver falls a single block and climbs out; a small cost) and comes back after six seconds. Each delver has their own progress, the first to reach the far row
 * finishes it for the party, and every row holds at least one tile of its safe colour. The pits are shallow by design: the drop is one block and the floor under is solid.
 */
public final class PitJump extends PuzzleBase {
    private final int cols, rows;
    private int[] colour, seq;
    private final int[] rowOf = new int[8];
    private long[] pitUntil;

    public PitJump(Params p) {
        super("pit_jump", p);
        cols = 3 + (p.lvl() >= 3 ? 1 : 0);
        rows = 4 + p.lvl();
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("field", Layout.Role.PLATE, cols, rows, "pitjump"), Layout.Group.row("legend", Layout.Role.DISPLAY, rows, "pitjump")); }

    @Override protected void init() {
        seq = new int[rows];
        colour = new int[cols * rows];
        pitUntil = new long[cols * rows];
        java.util.Arrays.fill(rowOf, -1);
        int c = rng.nextInt(cols);
        for (int r = 0; r < rows; r++) {
            seq[r] = rng.nextInt(3);
            c = Math.max(0, Math.min(cols - 1, c + rng.nextInt(3) - 1));
            for (int x = 0; x < cols; x++) colour[r * cols + x] = rng.nextInt(3);
            colour[r * cols + c] = seq[r];
        }
    }

    @Override protected int failLimit() { return 6; }

    @Override protected void handle(Act a) {
        if (!a.stepsOn() || a.cell() >= cols * rows || a.actor() < 0 || a.actor() >= rowOf.length) return;
        int cell = a.cell(), r = cell / cols;
        if (now < pitUntil[cell] || r <= rowOf[a.actor()]) return;
        if (colour[cell] == seq[r]) {
            rowOf[a.actor()] = r;
            tone(cell, 3 + r * 2, "chime");
            if (r == rows - 1) win();
        } else {
            pitUntil[cell] = now + 120;
            emit(Fx.pit(cell));
            fail(a.actor(), 1, "hurt", "pit");
        }
    }

    @Override protected int face(int cell) {
        if (cell < cols * rows) return now < pitUntil[cell] ? 3 : colour[cell];
        return seq[cell - cols * rows];
    }

    @Override protected Act next(long tick) {
        int r = rowOf[0] + 1;
        if (r >= rows) return null;
        for (int x = 0; x < cols; x++) if (colour[r * cols + x] == seq[r] && tick >= pitUntil[r * cols + x]) return Act.stepOn(0, r * cols + x, tick, 0);
        return Act.stepOn(0, r * cols, Math.max(tick, pitUntil[r * cols]), 0);
    }
}
