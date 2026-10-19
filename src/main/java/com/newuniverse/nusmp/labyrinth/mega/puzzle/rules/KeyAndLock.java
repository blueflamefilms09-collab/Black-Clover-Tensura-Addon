package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: key and lock with a real carried item. Two to four coloured locks stand in a ring and as many key pedestals are scattered round the hall; a right click on a pedestal puts a
 * coloured key (a tagged item, named for its colour) in the hand, a right click with it on the lock of the same colour opens that lock, and opening a lock unseals the NEXT colour's key
 * pedestal: so the keys must be carried one at a time through a chain. A key pedestal gives out a key whenever asked (a dropped, lost or traded key never blocks the puzzle), a wrong key in a
 * lock bites for a little damage, and the keys the puzzle issued are taken back when it ends.
 */
public final class KeyAndLock extends PuzzleBase {
    private final int k;
    private boolean[] open;
    private String holding;

    public KeyAndLock(Params p) {
        super("key_and_lock", p);
        k = 2 + (p.lvl() >= 2 ? 1 : 0) + (p.lvl() >= 4 ? 1 : 0);
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.scatter("keys", Layout.Role.PILLAR, k, "keylock"), Layout.Group.ring("locks", Layout.Role.PILLAR, k, "keylock"));
    }

    @Override protected void init() {
        open = new boolean[k];
        holding = null;
    }

    private boolean available(int j) { return j == 0 || open[j - 1]; }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int c = a.cell();
        if (c < k) {
            if (available(c)) {
                holding = "key" + c;
                emit(Fx.give(a.actor(), c, holding));
                tone(c, 3 + c * 2, "chime");
            } else say(a.actor(), "sealed", 0);
            return;
        }
        int j = c - k;
        if (open[j]) return;
        if (a.item() == null) { say(a.actor(), "needKey", 0); return; }
        if (a.item().equals("key" + j)) {
            open[j] = true;
            holding = null;
            emit(Fx.consume(a.actor(), a.item()));
            spark(c, true);
            tone(c, 10 + 2 * j, "bell");
            boolean all = true;
            for (boolean b : open) all &= b;
            if (all) win();
        } else if (a.item().startsWith("key")) {
            spark(c, false);
            fail(a.actor(), 1, "hurt", "bite");
        }
    }

    @Override protected int face(int cell) {
        if (cell < k) return available(cell) ? cell : 16;
        int j = cell - k;
        return open[j] ? 8 + j : j;
    }

    @Override protected Act next(long tick) {
        for (int j = 0; j < k; j++) {
            if (open[j]) continue;
            if (("key" + j).equals(holding)) return Act.clickWith(0, k + j, holding, tick);
            return Act.click(0, j, tick);
        }
        return null;
    }
}
