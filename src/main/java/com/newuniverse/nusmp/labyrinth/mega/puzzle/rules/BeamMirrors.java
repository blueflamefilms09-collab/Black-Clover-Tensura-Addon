package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: mirror and beam routing with real optics and no redstone. A beam leaves a source on the west edge heading east across a floor grid, bounces off every mirror it meets ('/' or '\'),
 * and must light the crystal. The beam is traced from the mirrors' current turn at every change and drawn as lit floor, so the party sees at once what a turn does; clicking a mirror
 * turns it. The board is dealt backwards: a random self-avoiding route with a mirror at each bend is laid out first (so a solution exists), a few decoy mirrors off the route are added,
 * and some of the route's mirrors are turned the wrong way. Pure; the trace is a function of the mirrors only.
 */
public final class BeamMirrors extends PuzzleBase {
    private static final int[] DX = {1, 0, -1, 0}, DY = {0, 1, 0, -1};            // E, S, W, N (rows grow southward)
    private final int w, h;
    private int sr, tx, ty;
    private final Map<Integer, Integer> mirror = new HashMap<>();                    // cell -> 0 '/' or 1 '\'
    private final Map<Integer, Integer> wanted = new HashMap<>();                    // route mirrors -> orientation the route needs
    private boolean[] lit;
    private boolean hit;

    public BeamMirrors(Params p) {
        super("beam_mirrors", p);
        w = 5 + (p.lvl() >= 3 ? 1 : 0);
        h = 4 + (p.lvl() >= 2 ? 1 : 0);
    }

    private int idx(int x, int y) { return y * (w + 1) + x; }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("floor", Layout.Role.PLATE, w + 1, h, "beam")); }

    /** '/' sends E to N, N to E, W to S, S to W; '\' sends E to S, S to E, W to N, N to W. Directions are 0 E, 1 S, 2 W, 3 N. */
    private static int reflect(int dir, int o) {
        if (o == 0) return switch (dir) { case 0 -> 3; case 3 -> 0; case 2 -> 1; default -> 2; };
        return switch (dir) { case 0 -> 1; case 1 -> 0; case 2 -> 3; default -> 2; };
    }

    private static int orientationFor(int in, int out) { return reflect(in, 0) == out ? 0 : 1; }

    @Override protected void init() {
        for (int attempt = 0; attempt < 200; attempt++) {
            if (layRoute()) break;
        }
        // scramble: turn some of the route's mirrors the wrong way
        for (int tries = 0; tries < 20; tries++) {
            boolean any = false;
            for (Map.Entry<Integer, Integer> e : wanted.entrySet()) {
                if (rng.nextInt(100) < 60) { mirror.put(e.getKey(), 1 - e.getValue()); any = true; } else mirror.put(e.getKey(), e.getValue());
            }
            if (any) { trace(); if (!hit) break; }
        }
        trace();
        if (hit) {                                                                    // (a rare draw) turn exactly one route mirror, the first that makes the beam miss
            for (int key : wanted.keySet()) {
                for (Map.Entry<Integer, Integer> e : wanted.entrySet()) mirror.put(e.getKey(), e.getValue());
                mirror.put(key, 1 - wanted.get(key));
                trace();
                if (!hit) break;
            }
        }
        jamBudget = 3 * wanted.size() + 10;
    }

    private boolean layRoute() {
        mirror.clear();
        wanted.clear();
        sr = rng.nextInt(h);
        boolean[][] used = new boolean[w + 2][h];
        int x = 0, y = sr, dir = 0;
        int bends = Math.min(4, 2 + p.lvl() / 2);
        for (int seg = 0; seg <= bends; seg++) {
            int max = 0;
            int cx = x, cy = y;
            while (true) {
                int nx = cx + DX[dir], ny = cy + DY[dir];
                if (nx < 1 || nx > w || ny < 0 || ny >= h || used[nx][ny]) break;
                max++;
                cx = nx;
                cy = ny;
            }
            if (max == 0) return false;
            int len = seg == bends ? Math.max(1, Math.min(max, 1 + rng.nextInt(max))) : 1 + rng.nextInt(max);
            for (int i = 0; i < len; i++) { x += DX[dir]; y += DY[dir]; used[x][y] = true; }
            if (seg == bends) { tx = x; ty = y; break; }
            int turn = rng.nextBoolean() ? 1 : 3;
            int out = (dir + turn) % 4;
            wanted.put(idx(x, y), orientationFor(dir, out));
            dir = out;
        }
        if (wanted.size() < 2 || (tx == 0)) return false;
        // decoys
        int decoys = 2 + p.lvl() / 2;
        for (int tries = 0; tries < 60 && decoys > 0; tries++) {
            int dx = 1 + rng.nextInt(w), dy = rng.nextInt(h);
            if (used[dx][dy] || (dx == tx && dy == ty)) continue;
            if (mirror.containsKey(idx(dx, dy))) continue;
            mirror.put(idx(dx, dy), rng.nextInt(2));
            decoys--;
        }
        for (Map.Entry<Integer, Integer> e : wanted.entrySet()) mirror.put(e.getKey(), e.getValue());
        return true;
    }

    private void trace() {
        lit = new boolean[(w + 1) * h];
        hit = false;
        int x = 0, y = sr, dir = 0;
        for (int steps = 0; steps < 6 * (w + 1) * h; steps++) {
            x += DX[dir];
            y += DY[dir];
            if (x < 1 || x > w || y < 0 || y >= h) return;
            lit[idx(x, y)] = true;
            if (x == tx && y == ty) { hit = true; return; }
            Integer m = mirror.get(idx(x, y));
            if (m != null) dir = reflect(dir, m);
        }
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        Integer m = mirror.get(a.cell());
        if (m == null) return;
        mirror.put(a.cell(), 1 - m);
        trace();
        tone(a.cell(), hit ? 14 : 5 + (m == 0 ? 2 : 0), "chime");
        if (hit) win();
    }

    @Override protected int face(int cell) {
        int x = cell % (w + 1), y = cell / (w + 1);
        if (x == 0) return y == sr ? 6 : 9;
        if (x == tx && y == ty) return hit ? 8 : 7;
        Integer m = mirror.get(cell);
        if (m != null) return (lit[cell] ? 4 : 2) + m;
        return lit[cell] ? 1 : 0;
    }

    @Override protected Act next(long tick) {
        for (Map.Entry<Integer, Integer> e : wanted.entrySet()) if (!mirror.get(e.getKey()).equals(e.getValue())) return Act.click(0, e.getKey(), tick);
        return null;
    }
}
