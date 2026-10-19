package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: mimic chests, as a logic puzzle instead of a coin toss. Three or four chests stand in a row, one holds the treasure and the others are mimics. Every chest carries an inscription,
 * either "the treasure is in chest X" or "the treasure is not in chest X", and the vault's rule is stated plainly: exactly ONE inscription tells the truth. The inscriptions are drawn
 * until exactly one chest fits that rule (checked by trying every chest), so the treasure can always be worked out. A plain right click reads a chest's inscription (free, any number of
 * times); a sneaking right click opens it. A mimic bites (a cost, and a weak monster climbs out) and is spent for good, so a party that guesses wrong can still deduce the real chest from
 * what is left; the treasure chest can never be spent.
 */
public final class MimicChests extends PuzzleBase {
    private static final String LETTERS = "ABCD";
    private final int c;
    private int[] type, at;
    private int real;
    private boolean[] spent;

    public MimicChests(Params p) {
        super("mimic_chests", p);
        c = 3 + (p.lvl() >= 3 ? 1 : 0);
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.row("chests", Layout.Role.CHEST, c, "chest")); }

    /** How many inscriptions are true if the treasure lies in chest {@code loc}. */
    private int truths(int[] ty, int[] x, int loc) {
        int t = 0;
        for (int i = 0; i < c; i++) if (ty[i] == 0 ? loc == x[i] : loc != x[i]) t++;
        return t;
    }

    /** The one location where exactly one inscription is true, or -1 when there is none or more than one. */
    private int unique(int[] ty, int[] x) {
        int found = -1;
        for (int loc = 0; loc < c; loc++) {
            if (truths(ty, x, loc) != 1) continue;
            if (found >= 0) return -1;
            found = loc;
        }
        return found;
    }

    @Override protected void init() {
        type = null;
        for (int attempt = 0; attempt < 500 && type == null; attempt++) {
            int[] ty = new int[c], x = new int[c];
            for (int i = 0; i < c; i++) { ty[i] = rng.nextInt(2); x[i] = rng.nextInt(c); }
            int u = unique(ty, x);
            if (u >= 0) { type = ty; at = x; real = u; }
        }
        if (type == null) {                                                           // the first assignment, in order, that works
            int total = 1;
            for (int i = 0; i < c; i++) total *= 2 * c;
            for (int code = 0; code < total && type == null; code++) {
                int[] ty = new int[c], x = new int[c];
                int t = code;
                for (int i = 0; i < c; i++) { int v = t % (2 * c); t /= 2 * c; ty[i] = v / c; x[i] = v % c; }
                int u = unique(ty, x);
                if (u >= 0) { type = ty; at = x; real = u; }
            }
        }
        spent = new boolean[c];
    }

    @Override protected boolean countsAsMove(Act a) { return false; }

    @Override protected int failLimit() { return 3; }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= c) return;
        int i = a.cell();
        if (!a.sneaking()) {
            say(a.actor(), type[i] == 0 ? "inscr.in" : "inscr.not", i, String.valueOf(LETTERS.charAt(i)), String.valueOf(LETTERS.charAt(at[i])));
            return;
        }
        if (spent[i]) return;
        if (i == real) {
            tone(i, 19, "bell");
            spark(i, true);
            win();
        } else {
            spent[i] = true;
            emit(Fx.mimic(a.actor(), i));
            tone(i, 0, "bass");
            fail(a.actor(), 2, "ambush", "mimic");
        }
    }

    @Override protected int face(int cell) {
        if (solved && cell == real) return 2;
        return spent[cell] ? 1 : 0;
    }

    @Override protected Act next(long tick) { return Act.sneakClick(0, real, tick); }

    /** For the tests. */
    public int realChest() { return real; }
}
