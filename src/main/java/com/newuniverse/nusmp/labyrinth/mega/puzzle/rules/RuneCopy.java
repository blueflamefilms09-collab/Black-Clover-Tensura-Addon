package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: rune copying is a memory-and-craft puzzle: a master row of runes is shown, a row of blank copy stones stands in front of it, and a right click on a copy stone cycles its rune (a
 * sneaking click cycles backwards). The seal stone checks the copy. On the first floors the master stays in view; deeper, it fades after a while and the oracle stone shows it again for
 * ten seconds (with a rest between, so it cannot be used as a flashlight). A wrong seal tells how many runes were right, clears the copy and costs a little. The copy stones are never
 * locked, so a half-finished copy left by a party that wandered off is simply corrected.
 */
public final class RuneCopy extends PuzzleBase {
    private final int n;
    private int[] target, cur;
    private long shownUntil, oracleReady;

    public RuneCopy(Params p) {
        super("rune_copy", p);
        n = Math.min(7, 3 + Math.min(3, p.lvl()) + (p.party() >= 4 ? 1 : 0));
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("master", Layout.Role.DISPLAY, n, "rune"), Layout.Group.row("copy", Layout.Role.PILLAR, n, "rune"),
                Layout.Group.row("seal", Layout.Role.PILLAR, 1, "altar"), Layout.Group.row("oracle", Layout.Role.PILLAR, 1, "altar"));
    }

    @Override protected void init() {
        target = new int[n];
        do {
            for (int i = 0; i < n; i++) target[i] = rng.nextInt(6);
        } while (allZero());
        cur = new int[n];
        shownUntil = p.lvl() == 0 ? Long.MAX_VALUE : now + 420 - 40L * p.lvl();
        oracleReady = 0;
    }

    private boolean allZero() {
        for (int v : target) if (v != 0) return false;
        return true;
    }

    @Override protected void onBegin(long t) { shownUntil = p.lvl() == 0 ? Long.MAX_VALUE : t + 420 - 40L * p.lvl(); }

    @Override protected boolean countsAsMove(Act a) { return false; }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int c = a.cell();
        if (c >= n && c < 2 * n) {
            int i = c - n;
            cur[i] = (cur[i] + (a.sneaking() ? 5 : 1)) % 6;
            tone(c, 2 + cur[i] * 2, "harp");
        } else if (c == 2 * n) {
            int right = 0;
            for (int i = 0; i < n; i++) if (cur[i] == target[i]) right++;
            if (right == n) { tone(c, 19, "bell"); win(); return; }
            cur = new int[n];
            say(a.actor(), "close", right, Integer.toString(right), Integer.toString(n));
            fail(a.actor(), 1, "drain", "mismatch");
        } else if (c == 2 * n + 1) {
            if (now >= oracleReady) {
                shownUntil = now + 200;
                oracleReady = now + 300;
                tone(c, 12, "bell");
            } else say(a.actor(), "rest", 0);
        }
    }

    @Override protected int face(int cell) {
        if (cell < n) return now < shownUntil ? target[cell] : 6;
        if (cell < 2 * n) return cur[cell - n];
        return 0;
    }

    @Override protected Act next(long tick) {
        for (int i = 0; i < n; i++) if (cur[i] != target[i]) return Act.click(0, n + i, tick);
        return Act.click(0, 2 * n, tick);
    }
}
