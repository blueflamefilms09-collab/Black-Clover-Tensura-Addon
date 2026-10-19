package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayDeque;

/**
 * Why: the gravity flip, as a tilting maze. A lit marble lies on a floor maze; four pillars (each marked with an arrow) tip the whole room one way, and the marble slides in that direction
 * until a wall stops it - or until it falls into the gold hole, which stops it too. The party must tip the room until the marble drops into the hole. The maze is drawn at random, then
 * checked by a breadth-first search over the marble's resting places: a maze is only used if the hole can be reached in between 3 and 11 tips (a fixed, always solvable fallback exists), so
 * the same search is the hint. Tipping costs nothing, but the room only tips so many times before it lurches (a small cost, the marble goes back to its start).
 */
public final class GravityMaze extends PuzzleBase {
    private static final int[] DR = {-1, 0, 1, 0}, DC = {0, 1, 0, -1};
    private final int w, h;
    private boolean[] wall;
    private int start, goal, ball;

    public GravityMaze(Params p) {
        super("gravity_maze", p);
        w = 5 + (p.lvl() + 1) / 2;
        h = 5 + p.lvl() / 2;
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.grid("maze", Layout.Role.DISPLAY, w, h, "maze"), Layout.Group.row("tilt", Layout.Role.PILLAR, 4, "arrow")); }

    private int slide(int from, int d) {
        int r = from / w, c = from % w;
        while (true) {
            int nr = r + DR[d], nc = c + DC[d];
            if (nr < 0 || nr >= h || nc < 0 || nc >= w || wall[nr * w + nc]) return r * w + c;
            r = nr;
            c = nc;
            if (r * w + c == goal) return goal;
        }
    }

    /** Fewest tips from {@code from} to the hole, or -1; {@code first[0]} gets the first tip of a shortest way. */
    private int bfs(int from, int[] first) {
        if (from == goal) return 0;
        int n = w * h;
        int[] dist = new int[n], firstDir = new int[n];
        java.util.Arrays.fill(dist, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        dist[from] = 0;
        q.add(from);
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (int d = 0; d < 4; d++) {
                int nx = slide(cur, d);
                if (dist[nx] >= 0) continue;
                dist[nx] = dist[cur] + 1;
                firstDir[nx] = cur == from ? d : firstDir[cur];
                if (nx == goal) { if (first != null) first[0] = firstDir[nx]; return dist[nx]; }
                q.add(nx);
            }
        }
        return -1;
    }

    @Override protected void init() {
        int n = w * h;
        int want = 3 + p.lvl() / 2;
        int bestDist = -1;
        boolean[] bestWall = null;
        int bestStart = 0, bestGoal = 0;
        for (int attempt = 0; attempt < 400; attempt++) {
            boolean[] wl = new boolean[n];
            double density = 0.22 + 0.02 * p.lvl();
            for (int i = 0; i < n; i++) wl[i] = rng.nextDouble() < density;
            int s = rng.nextInt(n), g = rng.nextInt(n);
            if (s == g || wl[s] || wl[g]) continue;
            wall = wl;
            start = s;
            goal = g;
            int d = bfs(s, null);
            if (d >= want && d <= 11) { bestDist = d; bestWall = wl; bestStart = s; bestGoal = g; break; }
            if (d >= 2 && d <= 11 && d > bestDist) { bestDist = d; bestWall = wl; bestStart = s; bestGoal = g; }
        }
        if (bestWall == null) {
            bestWall = new boolean[n];
            bestStart = 0;
            bestGoal = w - 1;
            bestDist = 1;
        }
        wall = bestWall;
        start = bestStart;
        goal = bestGoal;
        ball = start;
        jamBudget = 2 * bestDist + 6;
    }

    @Override protected boolean countsAsMove(Act a) { return a.kind() == Act.Kind.CLICK && a.cell() >= w * h; }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() < w * h) return;
        int d = a.cell() - w * h;
        ball = slide(ball, d);
        tone(a.cell(), 3 + d * 4, "bass");
        if (ball == goal) { spark(goal, true); win(); }
    }

    @Override protected int face(int cell) {
        if (cell >= w * h) return cell - w * h;
        if (cell == ball) return cell == goal ? 4 : 2;
        if (cell == goal) return 3;
        return wall[cell] ? 1 : 0;
    }

    @Override protected Act next(long tick) {
        int[] first = {0};
        int d = bfs(ball, first);
        return Act.click(0, w * h + (d < 0 ? 0 : first[0]), tick);
    }
}
