package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Why: the sliding-block puzzle on the floor: a grid of numbered tiles with one hole, a right click on a tile in the hole's row or column slides the whole line toward the hole, and the
 * tiles must end in order (the finished picture is shown on a second grid beside it). Dealt by sliding backwards from the finished board, so it is always solvable (half of all random
 * arrangements of a sliding puzzle are not). The rule keeps the HISTORY of the hole's moves, cancelling a move against its reverse, so the shortest known way back to the finished board is
 * always "undo the history", however many wrong slides were made.
 */
public final class SlidingBlock extends PuzzleBase {
    private static final int[] DR = {-1, 0, 1, 0}, DC = {0, 1, 0, -1};
    private final int cols, rows;
    private int[] board;
    private int blank;
    private Deque<Integer> history;
    private int dealt;

    public SlidingBlock(Params p) {
        super("sliding_block", p);
        cols = p.lvl() >= 3 ? 4 : 3;
        rows = 3;
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.grid("tiles", Layout.Role.PLATE, cols, rows, "symbol"), Layout.Group.grid("picture", Layout.Role.DISPLAY, cols, rows, "symbol"));
    }

    @Override protected void init() {
        int n = cols * rows;
        board = new int[n];
        for (int i = 0; i < n - 1; i++) board[i] = i + 1;
        board[n - 1] = 0;
        blank = n - 1;
        history = new ArrayDeque<>();
        dealt = 18 + 8 * p.lvl();
        int last = -1;
        int made = 0;
        while (made < dealt || isSolved()) {
            int d = rng.nextInt(4);
            if (last >= 0 && d == (last + 2) % 4) continue;
            if (!canMove(d)) continue;
            moveBlank(d);
            last = d;
            made++;
        }
        jamBudget = 3 * made + 30;
    }

    private boolean canMove(int d) {
        int r = blank / cols + DR[d], c = blank % cols + DC[d];
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    /** Moves the hole one step in direction d (the tile there slides into the hole) and records it in the history. */
    private void moveBlank(int d) {
        int r = blank / cols + DR[d], c = blank % cols + DC[d];
        int target = r * cols + c;
        board[blank] = board[target];
        board[target] = 0;
        blank = target;
        if (!history.isEmpty() && history.peek() == (d + 2) % 4) history.pop();
        else history.push(d);
    }

    private boolean isSolved() {
        for (int i = 0; i < board.length - 1; i++) if (board[i] != i + 1) return false;
        return board[board.length - 1] == 0;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= cols * rows) return;
        int tr = a.cell() / cols, tc = a.cell() % cols, br = blank / cols, bc = blank % cols;
        if (a.cell() == blank || (tr != br && tc != bc)) { say(a.actor(), "stuck", 0); return; }
        int d = tr < br ? 0 : tr > br ? 2 : tc > bc ? 1 : 3;
        int steps = Math.abs(tr - br) + Math.abs(tc - bc);
        for (int i = 0; i < steps; i++) moveBlank(d);
        tone(a.cell(), 6 + steps, "bass");
        if (isSolved()) win();
    }

    @Override protected int face(int cell) {
        if (cell < cols * rows) return board[cell];
        int j = cell - cols * rows;
        return j == cols * rows - 1 ? 0 : j + 1;
    }

    @Override protected Act next(long tick) {
        if (history.isEmpty()) return null;
        int d = (history.peek() + 2) % 4;
        int r = blank / cols + DR[d], c = blank % cols + DC[d];
        return Act.click(0, r * cols + c, tick);
    }
}
