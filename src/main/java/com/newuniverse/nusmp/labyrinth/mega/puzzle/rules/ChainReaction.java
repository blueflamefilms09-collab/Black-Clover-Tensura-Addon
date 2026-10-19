package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Why: a chain reaction. Charge crystals stand in a grid; each holds some charge (shown by colour) and bursts when its charge reaches the number of its neighbours, sending one charge to
 * each neighbour, which may burst in turn. A right click adds one charge to a crystal. The golden core crystal must burst. The grid is dealt with a mix of crystals one charge short of
 * bursting and others, and is accepted only if a search over all click sequences finds a way to burst the core in 2 to 4 clicks; the party has that many clicks plus one or two spare. Out
 * of clicks, the mechanism fizzles (a small cost) and the grid goes back to its first charges, the same deal. The hint is the same search.
 */
public final class ChainReaction extends PuzzleBase {
    private final int w, h;
    private int[] cap, q, q0;
    private int core;

    public ChainReaction(Params p) {
        super("chain_reaction", p);
        w = p.lvl() >= 2 ? 4 : 3;
        h = p.lvl() >= 4 ? 4 : 3;
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("crystals", Layout.Role.PILLAR, w, h, "charge")); }

    private int degree(int i) {
        int r = i / w, c = i % w, d = 0;
        if (r > 0) d++;
        if (r < h - 1) d++;
        if (c > 0) d++;
        if (c < w - 1) d++;
        return d;
    }

    /** Adds one charge at {@code i} and lets everything burst; true when the core burst. */
    private boolean click(int[] state, int i) {
        boolean coreBurst = false;
        state[i]++;
        ArrayDeque<Integer> work = new ArrayDeque<>();
        work.add(i);
        int guard = 0;
        while (!work.isEmpty() && guard++ < 4000) {
            int cur = work.poll();
            if (state[cur] < cap[cur]) continue;
            state[cur] -= cap[cur];
            if (cur == core) coreBurst = true;
            int r = cur / w, c = cur % w;
            if (r > 0) { state[cur - w]++; work.add(cur - w); }
            if (r < h - 1) { state[cur + w]++; work.add(cur + w); }
            if (c > 0) { state[cur - 1]++; work.add(cur - 1); }
            if (c < w - 1) { state[cur + 1]++; work.add(cur + 1); }
            if (state[cur] >= cap[cur]) work.add(cur);
        }
        return coreBurst;
    }

    private static long key(int[] s) {
        long k = 0;
        for (int v : s) k = k * 4 + v;
        return k;
    }

    /** Fewest clicks (up to {@code limit}) that burst the core from {@code from}; -1 if none. {@code first[0]} gets the first click of a shortest way. */
    private int search(int[] from, int limit, int[] first) {
        Map<Long, Boolean> seen = new HashMap<>();
        java.util.List<int[]> level = new java.util.ArrayList<>();
        java.util.List<Integer> firsts = new java.util.ArrayList<>();
        level.add(from.clone());
        firsts.add(-1);
        seen.put(key(from), true);
        for (int depth = 1; depth <= limit; depth++) {
            java.util.List<int[]> nextLevel = new java.util.ArrayList<>();
            java.util.List<Integer> nextFirsts = new java.util.ArrayList<>();
            for (int s = 0; s < level.size(); s++) {
                for (int i = 0; i < w * h; i++) {
                    int[] st = level.get(s).clone();
                    boolean burst = click(st, i);
                    int f = firsts.get(s) < 0 ? i : firsts.get(s);
                    if (burst) { if (first != null) first[0] = f; return depth; }
                    if (seen.putIfAbsent(key(st), true) == null) { nextLevel.add(st); nextFirsts.add(f); }
                }
            }
            level = nextLevel;
            firsts = nextFirsts;
        }
        return -1;
    }

    @Override protected void init() {
        int n = w * h;
        cap = new int[n];
        for (int i = 0; i < n; i++) cap[i] = degree(i);
        int hi = 2 + Math.min(2, p.lvl());
        int best = -1;
        int[] bestQ = null;
        int bestCore = 0;
        for (int attempt = 0; attempt < 300; attempt++) {
            int[] s = new int[n];
            for (int i = 0; i < n; i++) s[i] = rng.nextInt(100) < 50 ? cap[i] - 1 : rng.nextInt(cap[i]);
            core = rng.nextInt(n);
            int d = search(s, hi, null);
            if (d >= 2 && d <= hi) { best = d; bestQ = s; bestCore = core; break; }
            if (d >= 1 && best < 0) { best = d; bestQ = s; bestCore = core; }
        }
        if (bestQ == null) {
            bestQ = new int[n];
            for (int i = 0; i < n; i++) bestQ[i] = cap[i] - 1;
            bestCore = n / 2;
            best = 1;
        }
        core = bestCore;
        q0 = bestQ;
        q = q0.clone();
        jamBudget = best + 1 + (p.party() >= 3 ? 1 : 0);
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        boolean burst = click(q, a.cell());
        tone(a.cell(), 4 + q[a.cell()] * 3, "bass");
        if (burst) { spark(core, true); tone(core, 19, "bell"); win(); }
    }

    @Override protected int face(int cell) { return Math.min(3, q[cell]) + (cell == core ? 4 : 0); }

    @Override protected Act next(long tick) {
        int[] first = {0};
        int d = search(q, 4, first);
        return Act.click(0, d < 0 ? 0 : first[0], tick);
    }
}
