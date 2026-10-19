package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the braziers that must be lit in order, solved by READING instead of guessing. A ring of braziers of different colours stands round a lectern; the lectern, when clicked, recites the
 * rules of the flames ("the red flame must wake before the blue one"): the rules of a hidden order, one for each neighbouring pair plus a few extra, given in a shuffled order. Lighting a
 * brazier before the ones that must come first makes every flame gutter (a little damage, all dark again); lighting them in an order that keeps every rule is the answer, and the rules fix
 * that order completely. The lectern can be read any number of times.
 */
public final class TorchOrder extends PuzzleBase {
    static final String[] NAMES = {"red", "blue", "green", "yellow", "purple", "orange", "cyan", "white"};
    private final int n;
    private int[] col, order;
    private boolean[] lit;
    private final List<int[]> rules = new ArrayList<>();

    public TorchOrder(Params p) {
        super("torch_order", p);
        n = Math.min(7, 4 + (p.lvl() >= 2 ? 1 : 0) + (p.lvl() >= 4 ? 1 : 0) + (p.party() >= 4 ? 1 : 0));
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.ring("braziers", Layout.Role.PILLAR, n, "colour"), Layout.Group.row("lectern", Layout.Role.PILLAR, 1, "altar"));
    }

    @Override protected void init() {
        int[] c = iota(8);
        shuffle(c, rng);
        col = new int[n];
        System.arraycopy(c, 0, col, 0, n);
        order = iota(n);
        shuffle(order, rng);
        rules.clear();
        for (int i = 0; i + 1 < n; i++) rules.add(new int[] {order[i], order[i + 1]});
        for (int e = 0; e < 1 + p.lvl() / 2; e++) {
            int i = rng.nextInt(n - 2), j = i + 2 + rng.nextInt(n - i - 2);
            rules.add(new int[] {order[i], order[j]});
        }
        for (int i = rules.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int[] t = rules.get(i);
            rules.set(i, rules.get(j));
            rules.set(j, t);
        }
        lit = new boolean[n];
    }

    @Override protected int failLimit() { return 4; }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int i = a.cell();
        if (i == n) {
            say(a.actor(), "recite", rules.size(), Integer.toString(rules.size()));
            for (int[] r : rules) say(a.actor(), "rule", 0, "$colour." + NAMES[col[r[0]]], "$colour." + NAMES[col[r[1]]]);
            return;
        }
        if (lit[i]) return;
        boolean ok = true;
        for (int[] r : rules) if (r[1] == i && !lit[r[0]]) ok = false;
        if (ok) {
            lit[i] = true;
            spark(i, true);
            tone(i, 3 + 2 * count(), "harp");
            if (count() == n) win();
        } else {
            lit = new boolean[n];
            spark(i, false);
            fail(a.actor(), 1, "hurt", "gutter");
        }
    }

    private int count() {
        int k = 0;
        for (boolean b : lit) if (b) k++;
        return k;
    }

    @Override protected int face(int cell) {
        if (cell == n) return 0;
        return lit[cell] ? 8 + col[cell] : col[cell];
    }

    @Override protected Act next(long tick) {
        for (int i = 0; i < n; i++) if (!lit[order[i]]) return Act.click(0, order[i], tick);
        return null;
    }
}
