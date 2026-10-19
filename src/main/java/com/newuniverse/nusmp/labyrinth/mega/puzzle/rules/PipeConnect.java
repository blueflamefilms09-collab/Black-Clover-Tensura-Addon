package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayDeque;

/**
 * Why: pipe connecting. A field of pipe tiles (straights, bends and tees, each drawn as a glazed tile that shows which way it points) lies between a golden inlet on the west edge and a
 * drain on the east edge; a right click turns a tile a quarter. Water flows from the inlet through every tile whose openings meet (wet tiles change colour, so the party watches the water),
 * and the puzzle is solved when it reaches the drain. The board is dealt backwards: a random west-to-east route is laid first, so it is solvable, filled with decoy pipes, then every tile is
 * turned at random. Which of two equal turns of a straight pipe is used does not matter: tiles are compared by their openings.
 */
public final class PipeConnect extends PuzzleBase {
    private static final int[] DX = {0, 1, 0, -1}, DY = {-1, 0, 1, 0};                  // N, E, S, W; bit = 1 << direction
    private final int w, h;
    private int sr, dr;
    private int[] type, orient, need;

    public PipeConnect(Params p) {
        super("pipe_connect", p);
        w = 4 + (p.lvl() >= 2 ? 1 : 0);
        h = 3 + (p.lvl() >= 1 ? 1 : 0) + (p.lvl() >= 3 ? 1 : 0);
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("pipes", Layout.Role.PLATE, w + 2, h, "pipe")); }

    private static int maskOf(int type, int o) {
        int m = type == 0 ? 5 : type == 1 ? 3 : 7;
        for (int i = 0; i < o; i++) m = ((m << 1) | (m >> 3)) & 15;
        return m;
    }

    @Override protected void init() {
        type = new int[w * h];
        orient = new int[w * h];
        need = new int[w * h];
        java.util.Arrays.fill(need, -1);
        sr = rng.nextInt(h);
        boolean[] seen = new boolean[w * h];
        int x = 0, y = sr, in = 3;                                                     // water enters the first tile from the west
        while (true) {
            seen[y * w + x] = true;
            int out = 1;
            if (x < w - 1) {
                java.util.List<Integer> options = new java.util.ArrayList<>();
                options.add(1);
                options.add(1);
                if (y > 0 && !seen[(y - 1) * w + x]) options.add(0);
                if (y < h - 1 && !seen[(y + 1) * w + x]) options.add(2);
                out = options.get(rng.nextInt(options.size()));
            }
            need[y * w + x] = (1 << in) | (1 << out);
            if (x == w - 1 && out == 1) { dr = y; break; }
            x += DX[out];
            y += DY[out];
            in = (out + 2) % 4;
        }
        for (int i = 0; i < w * h; i++) {
            if (need[i] >= 0) {
                int m = need[i];
                boolean straight = m == 5 || m == 10;
                type[i] = straight ? 0 : 1;
            } else {
                type[i] = rng.nextInt(3);
            }
        }
        do {
            for (int i = 0; i < w * h; i++) orient[i] = rng.nextInt(4);
        } while (flows());
        jamBudget = 3 * w * h + 16;
    }

    private boolean[] wet() {
        boolean[] wet = new boolean[w * h];
        if ((maskOf(type[sr * w], orient[sr * w]) & 8) == 0) return wet;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        wet[sr * w] = true;
        q.add(sr * w);
        while (!q.isEmpty()) {
            int cur = q.poll();
            int cm = maskOf(type[cur], orient[cur]);
            int cx = cur % w, cy = cur / w;
            for (int d = 0; d < 4; d++) {
                if ((cm >> d & 1) == 0) continue;
                int nx = cx + DX[d], ny = cy + DY[d];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                int ni = ny * w + nx;
                if (wet[ni]) continue;
                if ((maskOf(type[ni], orient[ni]) >> ((d + 2) % 4) & 1) == 0) continue;
                wet[ni] = true;
                q.add(ni);
            }
        }
        return wet;
    }

    private boolean flows() {
        boolean[] wet = wet();
        int end = dr * w + (w - 1);
        return wet[end] && (maskOf(type[end], orient[end]) & 2) != 0;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int x = a.cell() % (w + 2), y = a.cell() / (w + 2);
        if (x < 1 || x > w) return;
        int i = y * w + (x - 1);
        orient[i] = (orient[i] + 1) % 4;
        tone(a.cell(), 4 + orient[i] * 2, "chime");
        if (flows()) win();
    }

    @Override protected int face(int cell) {
        int x = cell % (w + 2), y = cell / (w + 2);
        if (x == 0) return y == sr ? 24 : 25;
        if (x == w + 1) return y == dr ? (flows() ? 27 : 26) : 25;
        int i = y * w + (x - 1);
        return type[i] * 8 + orient[i] * 2 + (wet()[i] ? 1 : 0);
    }

    @Override protected Act next(long tick) {
        for (int i = 0; i < w * h; i++) {
            if (need[i] < 0) continue;
            if (maskOf(type[i], orient[i]) != need[i]) return Act.click(0, (i / w) * (w + 2) + (i % w) + 1, tick);
        }
        return null;
    }
}
