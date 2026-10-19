package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Why: water-level control, the old measuring puzzle: two or three glass tanks of different sizes (each as tall as it is big), a spring and a drain. Click a tank to pick it, then the spring
 * (fill it), the drain (empty it) or another tank (pour into it until that one is full or this one is empty). A column beside them shows the level that must stand in ONE of the tanks. A goal
 * that equals a tank's size is never dealt (it would be a single fill), and every dealt goal is checked by a search over all click sequences, so it is reachable; from ANY state a drain
 * of every tank leads back to the start, so a party can never make it unsolvable. The hint search used by the tests is the same search.
 */
public final class WaterLevel extends PuzzleBase {
    private final int n;
    private int[] cap, fill;
    private int goal, sel;

    public WaterLevel(Params p) {
        super("water_level", p);
        n = p.lvl() >= 2 ? 3 : 2;
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("tanks", Layout.Role.PILLAR, n, "water"), Layout.Group.row("spring", Layout.Role.PILLAR, 1, "plain"), Layout.Group.row("drain", Layout.Role.PILLAR, 1, "plain"),
                Layout.Group.row("goal", Layout.Role.DISPLAY, 1, "water"));
    }

    @Override public int height(int cell) {
        if (cell < n) return cap[cell];
        return cell == n + 2 ? goal : 1;
    }

    @Override protected void init() {
        cap = null;
        int target = 6 + 2 * p.lvl(), found = 0, bestGap = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 200; attempt++) {
            int[] c = new int[n];
            boolean dup = false;
            for (int i = 0; i < n; i++) { c[i] = 3 + rng.nextInt(5); for (int j = 0; j < i; j++) dup |= c[j] == c[i]; }
            if (dup) continue;
            int g = 1 + rng.nextInt(6);
            boolean isCap = false;
            for (int v : c) isCap |= v == g;
            if (isCap) continue;
            int d = depth(c, g);
            if (d < 5 || d > 30) continue;
            int gap = Math.abs(d - target);
            if (gap < bestGap) { bestGap = gap; cap = c; goal = g; found = d; }
        }
        if (cap == null) {
            cap = n == 2 ? new int[] {3, 5} : new int[] {3, 5, 7};
            goal = 4;
            found = depth(cap, goal);
        }
        fill = new int[n];
        sel = -1;
        jamBudget = Math.max(40, 2 * found + 12);
    }

    private static long key(int[] fill, int sel) {
        long k = sel + 1;
        for (int f : fill) k = k * 16 + f;
        return k;
    }

    /** Applies a click to a state (arrays are modified); the clicks are 0..n-1 tanks, n the spring, n+1 the drain. Returns the new selection. */
    private int apply(int[] cap, int[] fill, int sel, int click) {
        int n = cap.length;
        if (click < n) {
            if (sel < 0) return click;
            if (sel == click) return -1;
            int amount = Math.min(fill[sel], cap[click] - fill[click]);
            fill[sel] -= amount;
            fill[click] += amount;
            return -1;
        }
        if (sel < 0) return -1;
        fill[sel] = click == n ? cap[sel] : 0;
        return -1;
    }

    private static boolean reached(int[] fill, int goal) {
        for (int f : fill) if (f == goal) return true;
        return false;
    }

    /** The fewest clicks from empty to a tank holding the goal, or -1. */
    private int depth(int[] c, int g) {
        int[] start = new int[c.length];
        return search(c, start, -1, g, null);
    }

    /** Breadth-first search over (fills, selection); returns the depth and stores the first click of a shortest path in {@code firstClick[0]}. */
    private int search(int[] c, int[] fills, int sel0, int g, int[] firstClick) {
        int n = c.length;
        if (reached(fills, g)) return 0;
        Map<Long, int[]> seen = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int[] s0 = new int[n + 3];
        System.arraycopy(fills, 0, s0, 0, n);
        s0[n] = sel0;
        s0[n + 1] = 0;
        s0[n + 2] = -1;
        queue.add(s0);
        seen.put(key(fills, sel0), s0);
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int click = 0; click < n + 2; click++) {
                int[] f = Arrays.copyOf(cur, n);
                int sel = apply(c, f, cur[n], click);
                long k = key(f, sel);
                if (seen.containsKey(k)) continue;
                int[] nx = new int[n + 3];
                System.arraycopy(f, 0, nx, 0, n);
                nx[n] = sel;
                nx[n + 1] = cur[n + 1] + 1;
                nx[n + 2] = cur[n + 2] < 0 ? click : cur[n + 2];
                if (reached(f, g)) {
                    if (firstClick != null) firstClick[0] = nx[n + 2];
                    return nx[n + 1];
                }
                seen.put(k, nx);
                queue.add(nx);
            }
        }
        return -1;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= n + 2) return;
        int before = sel;
        sel = apply(cap, fill, sel, a.cell());
        if (a.cell() >= n && before < 0) { say(a.actor(), "pickFirst", 0); return; }
        tone(a.cell(), a.cell() < n ? 4 + fill[a.cell()] : 9, "bass");
        if (reached(fill, goal)) win();
    }

    @Override protected int face(int cell) { return 0; }

    @Override public int face(int cell, int layer) {
        if (cell < n) {
            boolean w = layer < fill[cell];
            return sel == cell ? (w ? 3 : 2) : (w ? 1 : 0);
        }
        if (cell == n) return 1;
        if (cell == n + 1) return 0;
        return 3;
    }

    @Override protected Act next(long tick) {
        int[] first = {0};
        int d = search(cap, fill, sel, goal, first);
        if (d < 0) return Act.click(0, 0, tick);
        return Act.click(0, first[0], tick);
    }
}
