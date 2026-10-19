package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a number lock whose code is scattered through the room as clues. Three or four dial pillars each count 0 to 9 on a click (sneaking turns back); a lock stone tries the code. Clue
 * plaques stand about the room and each states a true fact about the code ("the digits add up to 14", "the second digit is even", "the first is greater than the third", "all the digits
 * differ", "the third is twice the second", "the first and last differ by 5", "the fourth digit is 7"). The facts are drawn until exactly ONE code satisfies all of them, checked by trying
 * every code, so the code can always be worked out; a wrong try says how many digits are right and costs a little. The dials show their digit in the action bar and by shade.
 */
public final class NumberLock extends PuzzleBase {
    private record Clue(int type, int a, int b, int v) {}

    private final int d, plaques;
    private int[] code, dial;
    private final List<Clue> clues = new ArrayList<>();

    public NumberLock(Params p) {
        super("number_lock", p);
        d = p.lvl() >= 3 ? 4 : 3;
        plaques = d + 2;
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("dials", Layout.Role.PILLAR, d, "digit"), Layout.Group.scatter("plaques", Layout.Role.PILLAR, plaques, "altar"), Layout.Group.row("lock", Layout.Role.PILLAR, 1, "altar"));
    }

    private static boolean holds(Clue c, int[] x) {
        return switch (c.type()) {
            case 0 -> java.util.Arrays.stream(x).sum() == c.v();
            case 1 -> x[c.a()] == c.v();
            case 2 -> x[c.a()] % 2 == c.v();
            case 3 -> x[c.a()] > x[c.b()];
            case 4 -> java.util.Arrays.stream(x).distinct().count() == x.length;
            case 5 -> x[c.a()] == 2 * x[c.b()];
            default -> Math.abs(x[c.a()] - x[c.b()]) == c.v();
        };
    }

    private int count() {
        int total = 1;
        for (int i = 0; i < d; i++) total *= 10;
        int n = 0;
        int[] x = new int[d];
        for (int v = 0; v < total; v++) {
            int t = v;
            for (int i = d - 1; i >= 0; i--) { x[i] = t % 10; t /= 10; }
            boolean ok = true;
            for (Clue c : clues) if (!holds(c, x)) { ok = false; break; }
            if (ok) n++;
        }
        return n;
    }

    private Clue randomTrue() {
        for (int tries = 0; tries < 60; tries++) {
            int type = switch (rng.nextInt(6)) { case 0 -> 0; case 1 -> 2; case 2 -> 3; case 3 -> 4; case 4 -> 5; default -> 6; };
            int a = rng.nextInt(d), b = rng.nextInt(d);
            if (a == b && type != 0 && type != 2 && type != 4) continue;
            Clue c = switch (type) {
                case 0 -> new Clue(0, 0, 0, java.util.Arrays.stream(code).sum());
                case 2 -> new Clue(2, a, 0, code[a] % 2);
                case 3 -> new Clue(3, a, b, 0);
                case 4 -> new Clue(4, 0, 0, 0);
                case 5 -> new Clue(5, a, b, 0);
                default -> new Clue(6, a, b, Math.abs(code[a] - code[b]));
            };
            if (holds(c, code)) return c;
        }
        return new Clue(0, 0, 0, java.util.Arrays.stream(code).sum());
    }

    @Override protected void init() {
        code = new int[d];
        for (int i = 0; i < d; i++) code[i] = rng.nextInt(10);
        clues.clear();
        for (int k = 0; k < plaques - d && count() > 1; k++) clues.add(randomTrue());
        int guard = 0;
        while (count() > 1 && guard++ < 40) {
            int pos = rng.nextInt(d);
            Clue eq = new Clue(1, pos, 0, code[pos]);
            boolean known = false;
            for (Clue c : clues) known |= c.type() == 1 && c.a() == pos;
            if (!known) clues.add(eq);
        }
        while (clues.size() < plaques) clues.add(randomTrue());
        for (int i = clues.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Clue t = clues.get(i);
            clues.set(i, clues.get(j));
            clues.set(j, t);
        }
        dial = new int[d];
        if (java.util.Arrays.equals(dial, code)) dial[0] = 1;
    }

    private String[] words(Clue c) {
        return switch (c.type()) {
            case 0 -> new String[] {Integer.toString(c.v())};
            case 1 -> new String[] {Integer.toString(c.a() + 1), Integer.toString(c.v())};
            case 2 -> new String[] {Integer.toString(c.a() + 1)};
            case 4 -> new String[0];
            case 6 -> new String[] {Integer.toString(c.a() + 1), Integer.toString(c.b() + 1), Integer.toString(c.v())};
            default -> new String[] {Integer.toString(c.a() + 1), Integer.toString(c.b() + 1)};
        };
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int c = a.cell();
        if (c < d) {
            dial[c] = (dial[c] + (a.sneaking() ? 9 : 1)) % 10;
            tone(c, dial[c] + 2, "harp");
            say(a.actor(), "dial", dial[c], Integer.toString(c + 1), Integer.toString(dial[c]));
        } else if (c < d + plaques) {
            Clue cl = clues.get(c - d);
            String key = "clue." + cl.type() + (cl.type() == 2 ? (cl.v() == 0 ? "e" : "o") : "");
            say(a.actor(), key, 0, words(cl));
        } else {
            int right = 0;
            for (int i = 0; i < d; i++) if (dial[i] == code[i]) right++;
            if (right == d) { tone(c, 19, "bell"); win(); return; }
            say(a.actor(), "digitsRight", right, Integer.toString(right), Integer.toString(d));
            fail(a.actor(), 1, "drain", "wrongCode");
        }
    }

    @Override protected int face(int cell) { return cell < d ? dial[cell] : 0; }

    @Override protected Act next(long tick) {
        for (int i = 0; i < d; i++) if (dial[i] != code[i]) return Act.click(0, i, tick);
        return Act.click(0, d + plaques, tick);
    }

    /** For the tests: how many codes satisfy every clue (must be exactly 1). */
    public int solutions() { return count(); }

    /** For the tests: the number of clues. */
    public int clueCount() { return clues.size(); }
}
