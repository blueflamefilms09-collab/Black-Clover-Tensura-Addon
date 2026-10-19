package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: plate-and-pit jumping, solved by reading. A field of red, blue and gold tiles lies in rows, and a column of coloured tiles beside it (the legend, each legend tile level with the row it names)
 * gives the colour that is safe in each row. Step on a tile of the row's colour and the row is passed; step on any other and the tile gives way (a one-block pit, the delver falls a single block
 * and climbs out; a small cost) and comes back after six seconds. The field can be crossed from either end: a delver who has stood on a safe tile of the first AND of the last row has crossed it, and
 * that finishes it for the party. Each delver has their own progress, and every row holds at least one tile of its safe colour. The pits are shallow by design: the drop is one block and the floor
 * under is solid.
 */
public final class PitJump extends PuzzleBase {
    private final int cols, rows;
    private int[] colour, seq;
    private final int[] visited = new int[8];
    private long[] pitUntil;

    public PitJump(Params p) {
        super("pit_jump", p);
        cols = 3 + (p.lvl() >= 3 ? 1 : 0);
        rows = 4 + p.lvl();
    }

    @Override protected Layout makeLayout() { return Layout.sideBySide(Layout.Group.grid("field", Layout.Role.PLATE, cols, rows, "pitjump"), Layout.Group.grid("legend", Layout.Role.DISPLAY, 1, rows, "pitjump")); }

    @Override protected void init() {
        seq = new int[rows];
        colour = new int[cols * rows];
        pitUntil = new long[cols * rows];
        java.util.Arrays.fill(visited, 0);
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
        if (!a.stepsOn() || a.cell() >= cols * rows || a.actor() < 0 || a.actor() >= visited.length) return;
        int cell = a.cell(), r = cell / cols;
        if (now < pitUntil[cell]) return;
        if (colour[cell] == seq[r]) {
            visited[a.actor()] |= 1 << r;
            tone(cell, 3 + r * 2, "chime");
            if ((visited[a.actor()] & 1) != 0 && (visited[a.actor()] >> (rows - 1) & 1) != 0) win();
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
        int r = 0;
        while (r < rows && (visited[0] >> r & 1) != 0) r++;
        if (r >= rows) return null;
        for (int x = 0; x < cols; x++) if (colour[r * cols + x] == seq[r] && tick >= pitUntil[r * cols + x]) return Act.stepOn(0, r * cols + x, tick, 0);
        return Act.stepOn(0, r * cols, Math.max(tick, pitUntil[r * cols]), 0);
    }
}
