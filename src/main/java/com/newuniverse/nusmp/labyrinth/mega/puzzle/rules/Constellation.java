package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: connect the stars. Five to seven star pillars stand scattered over the floor; the party must draw ONE closed figure through all of them without the thread ever crossing itself: click a
 * star to begin, click the next star to string a thread to it, and finally click the first star again to close the figure. A thread that would cross an earlier one snaps (a small cost,
 * the figure is cleared); clicking the last star again takes the last thread back. The stars are drawn so that no three lie on a line, which guarantees that a figure exists from any
 * starting star (every finite point set in general position has a simple closed path through all of it). The hint searches for a way to finish the figure from the threads already drawn
 * and, if there is none, takes the last thread back.
 */
public final class Constellation extends PuzzleBase {
    private static final int GRID = 6;
    private final int n;
    private int[] px, pz;
    private final List<Integer> chain = new ArrayList<>();

    public Constellation(Params p) {
        super("constellation", p);
        n = 5 + p.lvl() / 2;
    }

    @Override protected Layout makeLayout() {
        // the positions are drawn in init(), but the layout must be fixed now: the group is a POINTS group whose coordinates are those of the seed's own first deal
        int[][] pts = deal(new java.util.SplittableRandom(com.newuniverse.nusmp.labyrinth.mega.mechanics.Seeds.mix(com.newuniverse.nusmp.labyrinth.mega.mechanics.Seeds.mix(p.seed(), id.hashCode()), 13)));
        return Layout.of(Layout.Group.points("stars", Layout.Role.PILLAR, pts[0], pts[1], "star"));
    }

    private static long cross(int ax, int az, int bx, int bz, int cx, int cz) { return (long) (bx - ax) * (cz - az) - (long) (bz - az) * (cx - ax); }

    private static boolean general(int[] x, int[] z, int count) {
        for (int i = 0; i < count; i++) for (int j = i + 1; j < count; j++) for (int k = j + 1; k < count; k++) if (cross(x[i], z[i], x[j], z[j], x[k], z[k]) == 0) return false;
        return true;
    }

    /** The star positions of a deal: random points on a small grid, at least two apart (Manhattan), no three on a line; a fixed search when chance fails. */
    private int[][] deal(java.util.SplittableRandom r) {
        for (int attempt = 0; attempt < 400; attempt++) {
            int[] x = new int[n], z = new int[n];
            int c = 0;
            for (int guard = 0; guard < 80 && c < n; guard++) {
                int cx = r.nextInt(GRID), cz = r.nextInt(GRID);
                boolean ok = true;
                for (int i = 0; i < c; i++) if (Math.abs(x[i] - cx) + Math.abs(z[i] - cz) < 2) ok = false;
                if (!ok) continue;
                x[c] = cx;
                z[c] = cz;
                if (general(x, z, c + 1)) c++;
            }
            if (c == n) return new int[][] {x, z};
        }
        int[] x = new int[n], z = new int[n];
        if (!search(x, z, 0, 0)) throw new IllegalStateException("no star field");
        return new int[][] {x, z};
    }

    private boolean search(int[] x, int[] z, int c, int from) {
        if (c == n) return true;
        for (int cell = from; cell < GRID * GRID; cell++) {
            int cx = cell % GRID, cz = cell / GRID;
            boolean ok = true;
            for (int i = 0; i < c; i++) if (Math.abs(x[i] - cx) + Math.abs(z[i] - cz) < 2) ok = false;
            if (!ok) continue;
            x[c] = cx;
            z[c] = cz;
            if (general(x, z, c + 1) && search(x, z, c + 1, cell + 1)) return true;
        }
        return false;
    }

    @Override protected void init() {
        Layout.Group g = layout().groups().get(0);
        px = g.px();
        pz = g.pz();
        chain.clear();
    }

    private boolean crosses(int a, int b, int c, int dd) {
        long o1 = Long.signum(cross(px[a], pz[a], px[b], pz[b], px[c], pz[c])), o2 = Long.signum(cross(px[a], pz[a], px[b], pz[b], px[dd], pz[dd]));
        long o3 = Long.signum(cross(px[c], pz[c], px[dd], pz[dd], px[a], pz[a])), o4 = Long.signum(cross(px[c], pz[c], px[dd], pz[dd], px[b], pz[b]));
        return o1 * o2 < 0 && o3 * o4 < 0;
    }

    /** Would a thread from {@code a} to {@code b} cross any thread of {@code path} (a list of stars joined in order), ignoring threads that share a star with it? */
    private boolean crossesPath(List<Integer> path, int a, int b) {
        for (int i = 0; i + 1 < path.size(); i++) {
            int c = path.get(i), dd = path.get(i + 1);
            if (c == a || c == b || dd == a || dd == b) continue;
            if (crosses(a, b, c, dd)) return true;
        }
        return false;
    }

    @Override public List<int[]> links() {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i + 1 < chain.size(); i++) out.add(new int[] {chain.get(i), chain.get(i + 1)});
        if (solved && chain.size() == n) out.add(new int[] {chain.get(n - 1), chain.get(0)});
        return out;
    }

    private void snap(Act a) {
        chain.clear();
        fail(a.actor(), 1, "dark", "snap");
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= n) return;
        int s = a.cell();
        if (chain.isEmpty()) { chain.add(s); tone(s, 6, "chime"); return; }
        int last = chain.get(chain.size() - 1);
        if (s == last) {
            chain.remove(chain.size() - 1);
            tone(s, 2, "bass");
            return;
        }
        if (chain.contains(s)) {
            if (s == chain.get(0) && chain.size() == n) {
                if (crossesPath(chain, last, s)) { snap(a); return; }
                tone(s, 19, "bell");
                win();
            } else say(a.actor(), "linked", 0);
            return;
        }
        if (crossesPath(chain, last, s)) { spark(s, false); snap(a); return; }
        chain.add(s);
        tone(s, 6 + chain.size() * 2, "chime");
    }

    @Override protected int face(int cell) {
        if (!chain.contains(cell)) return 0;
        if (cell == chain.get(0)) return 3;
        return cell == chain.get(chain.size() - 1) ? 2 : 1;
    }

    /** A star to click next that keeps the figure finishable, -1 when none does. */
    private int completion() {
        boolean[] used = new boolean[n];
        for (int c : chain) used[c] = true;
        return extend(new ArrayList<>(chain), used, -1);
    }

    private int extend(List<Integer> path, boolean[] used, int firstStep) {
        if (path.size() == n) {
            int last = path.get(n - 1), first = path.get(0);
            return crossesPath(path, last, first) ? -1 : (firstStep < 0 ? first : firstStep);
        }
        int last = path.get(path.size() - 1);
        for (int s = 0; s < n; s++) {
            if (used[s] || crossesPath(path, last, s)) continue;
            used[s] = true;
            path.add(s);
            int r = extend(path, used, firstStep < 0 ? s : firstStep);
            path.remove(path.size() - 1);
            used[s] = false;
            if (r >= 0) return r;
        }
        return -1;
    }

    @Override protected Act next(long tick) {
        if (chain.isEmpty()) return Act.click(0, 0, tick);
        int step = completion();
        if (step >= 0) return Act.click(0, step, tick);
        return Act.click(0, chain.get(chain.size() - 1), tick);
    }
}
