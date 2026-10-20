package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Why: the seeded generator of the exclusive pockets' floor plans. It works in a fixed canonical frame (the entry on the west edge, the treasury on the east edge, the mega-boss's lair
 * right in front of the treasury) and then turns the whole plan by a quarter turn four ways, so every seed is a different tomb but all obey the same rules. The steps: (1) a winding path
 * from the entry to the lair (random cell weights, then the cheapest path: so the way to the boss is never a straight line); (2) a randomised Prim growth that adds rooms up to the
 * wanted count, each joined to a room that already stands (so the plan is connected by construction); (3) extra "loop" doors between neighbouring rooms with the definition's chance, and
 * every dead end that has a neighbour is joined to it with a better chance, so the dungeon is a net of loops with a few real dead ends (the alcoves); (4) the treasury behind the lair, joined by
 * the sealed door, with nothing else beside it; (5) the kinds: the key room (the Sigil's) at the end of a dead end far from the entry, then the zones the definition asks for (ceiling-walker
 * rooms, weightless rooms, ceiling rivers, trap rooms) in rooms that keep a plain neighbour, and combat for the rest. A plan is accepted only when {@link LayoutCheck} finds nothing wrong
 * with it and it has all the zones that were asked for; otherwise the next attempt (a different salt of the same seed) is tried, so the result is still a function of the seed alone.
 */
public final class LayoutGen {
    private LayoutGen() {}

    /** The attempts made before the best plan found is returned anyway (its problems are then reported by {@link LayoutCheck}). */
    public static final int ATTEMPTS = 60;

    /** The generator's inputs: the grid side, the number of rooms (the treasury included), the share of loop doors, the boss lair's half width and the zones. */
    public record Params(int grid, int rooms, double cycles, int bossHalf, int inverted, int negated, int rivers, List<String> trapElements) {
        public Params {
            trapElements = List.copyOf(trapElements);
        }

        public static Params of(PocketDef d) {
            List<String> t = new ArrayList<>();
            for (PocketElement e : d.traps()) t.add(e.id());
            return new Params(d.layout().grid(), d.layout().rooms(), d.layout().cycles(), d.layout().bossHalf(), d.gravity().inverted(), d.gravity().negated(), d.gravity().rivers(), t);
        }
    }

    /** The plan of a seed. The first accepted attempt wins; the same seed and parameters always give the same plan. */
    public static PocketLayout generate(long seed, Params p) {
        PocketLayout best = null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            PocketLayout l = attempt(seed, attempt, p);
            if (l == null) continue;
            if (accepted(l, p)) return l;
            if (best == null) best = l;
        }
        if (best != null) return best;
        throw new IllegalStateException("no plan could be made for the seed " + seed + " with " + p);
    }

    /** Does the plan have no problem and every zone that was asked for? */
    public static boolean accepted(PocketLayout l, Params p) {
        return LayoutCheck.validate(l).isEmpty() && l.count(PocketLayout.Kind.INVERTED) >= p.inverted() && l.count(PocketLayout.Kind.NEGATED) >= p.negated()
                && l.count(PocketLayout.Kind.RIVER) >= p.rivers() && l.rooms().size() >= p.rooms();
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static PocketLayout attempt(long seed, int attempt, Params p) {
        int g = p.grid();
        Random rnd = new Random(RiftRules.mix(seed, 0xA11CE00L + attempt));
        int ez = g / 2 + rnd.nextInt(3) - 1, tz = g / 2 + rnd.nextInt(3) - 1;
        int[] entry = {0, ez}, boss = {g - 2, tz}, treasury = {g - 1, tz};
        boolean[][] blocked = new boolean[g][g], sel = new boolean[g][g];
        blocked[treasury[0]][treasury[1]] = true;                                      // nothing stands beside the treasury but the lair
        for (int[] d : DIRS) {
            int x = treasury[0] + d[0], z = treasury[1] + d[1];
            if (x >= 0 && x < g && z >= 0 && z < g && !(x == boss[0] && z == boss[1])) blocked[x][z] = true;
        }
        List<int[]> edges = new ArrayList<>();                                         // pairs of cells {x0, z0, x1, z1}
        sel[entry[0]][entry[1]] = true;
        // (1) the winding path: cheapest path over random cell weights
        int[][] weight = new int[g][g];
        for (int x = 0; x < g; x++) for (int z = 0; z < g; z++) weight[x][z] = 1 + rnd.nextInt(5);
        int[][] prev = new int[g * g][];
        int[] dist = new int[g * g];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[1]).thenComparingInt(a -> a[2]));
        dist[entry[0] * g + entry[1]] = 0;
        pq.add(new int[]{0, entry[0], entry[1]});
        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            if (cur[0] > dist[cur[1] * g + cur[2]]) continue;
            for (int[] d : DIRS) {
                int x = cur[1] + d[0], z = cur[2] + d[1];
                if (x < 0 || x >= g || z < 0 || z >= g || blocked[x][z]) continue;
                int nd = cur[0] + weight[x][z];
                if (nd < dist[x * g + z]) {
                    dist[x * g + z] = nd;
                    prev[x * g + z] = new int[]{cur[1], cur[2]};
                    pq.add(new int[]{nd, x, z});
                }
            }
        }
        if (dist[boss[0] * g + boss[1]] == Integer.MAX_VALUE) return null;
        int count = 1;
        for (int[] c = boss; prev[c[0] * g + c[1]] != null; c = prev[c[0] * g + c[1]]) {
            int[] q = prev[c[0] * g + c[1]];
            if (!sel[c[0]][c[1]]) {
                sel[c[0]][c[1]] = true;
                count++;
            }
            edges.add(new int[]{q[0], q[1], c[0], c[1]});
        }
        // (2) randomised Prim growth up to the wanted count (the treasury is the last room)
        int target = Math.min(Math.max(p.rooms() - 1, count), g * g - 2);
        while (count < target) {
            List<int[]> cand = new ArrayList<>();
            for (int x = 0; x < g; x++)
                for (int z = 0; z < g; z++) {
                    if (!sel[x][z]) continue;
                    for (int[] d : DIRS) {
                        int nx = x + d[0], nz = z + d[1];
                        if (nx >= 0 && nx < g && nz >= 0 && nz < g && !sel[nx][nz] && !blocked[nx][nz]) cand.add(new int[]{x, z, nx, nz});
                    }
                }
            if (cand.isEmpty()) break;
            int[] e = cand.get(rnd.nextInt(cand.size()));
            sel[e[2]][e[3]] = true;
            edges.add(e);
            count++;
        }
        sel[treasury[0]][treasury[1]] = true;
        // (3) loops: every adjacent pair of rooms that stands, with the chance; then dead ends are joined to a neighbour with a better chance
        for (int x = 0; x < g; x++)
            for (int z = 0; z < g; z++) {
                if (!sel[x][z] || blocked[x][z]) continue;
                for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                    int nx = x + d[0], nz = z + d[1];
                    if (nx >= g || nz >= g || !sel[nx][nz] || blocked[nx][nz] || joined(edges, x, z, nx, nz)) continue;
                    if (rnd.nextDouble() < p.cycles()) edges.add(new int[]{x, z, nx, nz});
                }
            }
        for (int x = 0; x < g; x++)
            for (int z = 0; z < g; z++) {
                if (!sel[x][z] || blocked[x][z] || (x == entry[0] && z == entry[1]) || degree(edges, x, z) != 1) continue;
                List<int[]> opts = new ArrayList<>();
                for (int[] d : DIRS) {
                    int nx = x + d[0], nz = z + d[1];
                    if (nx >= 0 && nx < g && nz >= 0 && nz < g && sel[nx][nz] && !blocked[nx][nz] && !joined(edges, x, z, nx, nz)) opts.add(new int[]{x, z, nx, nz});
                }
                if (!opts.isEmpty() && rnd.nextDouble() < 0.6) edges.add(opts.get(rnd.nextInt(opts.size())));
            }
        if (edges.size() - (count - 1) < 1) {                                          // at least one loop, if the plan has any place for one
            outer:
            for (int x = 0; x < g; x++)
                for (int z = 0; z < g; z++) {
                    if (!sel[x][z] || blocked[x][z]) continue;
                    for (int[] d : new int[][]{{1, 0}, {0, 1}}) {
                        int nx = x + d[0], nz = z + d[1];
                        if (nx < g && nz < g && sel[nx][nz] && !blocked[nx][nz] && !joined(edges, x, z, nx, nz)) {
                            edges.add(new int[]{x, z, nx, nz});
                            break outer;
                        }
                    }
                }
        }
        // (4) the rooms, numbered row by row, turned by a quarter turn k times
        int turns = rnd.nextInt(4);
        List<int[]> cells = new ArrayList<>();
        for (int z = 0; z < g; z++) for (int x = 0; x < g; x++) if (sel[x][z]) cells.add(turn(new int[]{x, z}, g, turns));
        cells.sort(Comparator.<int[]>comparingInt(c -> c[1]).thenComparingInt(c -> c[0]));
        int n = cells.size();
        int[][] index = new int[g][g];
        for (int[] row : index) java.util.Arrays.fill(row, -1);
        for (int i = 0; i < n; i++) index[cells.get(i)[0]][cells.get(i)[1]] = i;
        int eId = idOf(index, turn(entry, g, turns)), bId = idOf(index, turn(boss, g, turns)), tId = idOf(index, turn(treasury, g, turns));
        List<PocketLayout.Door> doors = new ArrayList<>();
        for (int[] e : edges) {
            int a = idOf(index, turn(new int[]{e[0], e[1]}, g, turns)), b = idOf(index, turn(new int[]{e[2], e[3]}, g, turns));
            doors.add(new PocketLayout.Door(Math.min(a, b), Math.max(a, b), false));
        }
        doors.add(new PocketLayout.Door(Math.min(bId, tId), Math.max(bId, tId), true));
        // (5) the kinds
        PocketLayout.Kind[] kind = new PocketLayout.Kind[n];
        kind[eId] = PocketLayout.Kind.ENTRY;
        kind[bId] = PocketLayout.Kind.BOSS;
        kind[tId] = PocketLayout.Kind.TREASURY;
        int[] depth = depths(n, doors, eId);
        int kId = -1;
        for (int i = 0; i < n; i++) {                                                  // the key room: a dead end if there is one, the farthest from the entry
            if (kind[i] != null || depth[i] < 2 || depth[i] > depth[bId] + 2) continue;
            if (kId < 0 || score(doors, i, depth) > score(doors, kId, depth)) kId = i;
        }
        if (kId < 0) for (int i = 0; i < n; i++) if (kind[i] == null && (kId < 0 || depth[i] > depth[kId])) kId = i;
        if (kId < 0) return null;
        kind[kId] = PocketLayout.Kind.KEY;
        List<Integer> rest = new ArrayList<>();
        for (int i = 0; i < n; i++) if (kind[i] == null) rest.add(i);
        java.util.Collections.shuffle(rest, rnd);
        int trapRooms = Math.max(2, Math.min(4, rest.size() / 5)), ruins = Math.max(1, Math.min(3, rest.size() / 6));
        place(kind, rest, doors, PocketLayout.Kind.INVERTED, p.inverted(), null);          // the constrained zones first: they need plain neighbours
        place(kind, rest, doors, PocketLayout.Kind.NEGATED, p.negated(), null);
        place(kind, rest, doors, PocketLayout.Kind.RIVER, p.rivers(), null);
        place(kind, rest, doors, PocketLayout.Kind.TRAP, Math.min(trapRooms, Math.max(0, rest.size() - 3)), PocketLayout.Kind.ENTRY);
        place(kind, rest, doors, PocketLayout.Kind.RUIN, ruins, null);
        for (int i : rest) {
            if (kind[i] != null) continue;
            kind[i] = degreeOf(doors, i) == 1 && rnd.nextDouble() < 0.7 ? PocketLayout.Kind.ALCOVE : PocketLayout.Kind.COMBAT;
        }
        int trapCycle = 0;
        List<PocketLayout.Room> rooms = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int half = kind[i] == PocketLayout.Kind.BOSS ? p.bossHalf() : kind[i].half();
            String el = "";
            if (kind[i] == PocketLayout.Kind.TRAP) el = p.trapElements().isEmpty() ? "fire" : p.trapElements().get(trapCycle++ % p.trapElements().size());
            rooms.add(new PocketLayout.Room(i, cells.get(i)[0], cells.get(i)[1], kind[i], half, kind[i].height(), el));
        }
        return new PocketLayout(seed, g, rooms, doors, eId, bId, tId, kId);
    }

    /** Gives {@code count} of the rooms still free the kind: a gravity room keeps plain neighbours (no entry, lair, key, treasury, no other gravity room); a trap room is not beside {@code avoid}. */
    private static void place(PocketLayout.Kind[] kind, List<Integer> rest, List<PocketLayout.Door> doors, PocketLayout.Kind what, int count, PocketLayout.Kind avoid) {
        int placed = 0;
        for (int i : rest) {
            if (placed >= count) return;
            if (kind[i] != null) continue;
            boolean ok = true;
            for (int nb : neighbours(doors, i)) {
                PocketLayout.Kind k = kind[nb];
                if (what.gravity() && k != null && (k.gravity() || k.plain())) ok = false;
                if (avoid != null && k == avoid) ok = false;
            }
            if (!ok) continue;
            kind[i] = what;
            placed++;
        }
    }

    private static List<Integer> neighbours(List<PocketLayout.Door> doors, int id) {
        List<Integer> out = new ArrayList<>();
        for (PocketLayout.Door d : doors) {
            if (d.a() == id) out.add(d.b());
            else if (d.b() == id) out.add(d.a());
        }
        return out;
    }

    private static int degreeOf(List<PocketLayout.Door> doors, int id) {
        int n = 0;
        for (PocketLayout.Door d : doors) if (!d.sealed() && (d.a() == id || d.b() == id)) n++;
        return n;
    }

    private static int score(List<PocketLayout.Door> doors, int id, int[] depth) { return (degreeOf(doors, id) == 1 ? 1000 : 0) + depth[id]; }

    /** Breadth-first depths from {@code from} over the doors that are not sealed; unreachable rooms keep -1. */
    static int[] depths(int n, List<PocketLayout.Door> doors, int from) {
        int[] depth = new int[n];
        java.util.Arrays.fill(depth, -1);
        depth[from] = 0;
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        q.add(from);
        while (!q.isEmpty()) {
            int c = q.poll();
            for (PocketLayout.Door d : doors) {
                if (d.sealed()) continue;
                int o = d.a() == c ? d.b() : d.b() == c ? d.a() : -1;
                if (o >= 0 && depth[o] < 0) {
                    depth[o] = depth[c] + 1;
                    q.add(o);
                }
            }
        }
        return depth;
    }

    private static boolean joined(List<int[]> edges, int x0, int z0, int x1, int z1) {
        for (int[] e : edges) {
            if ((e[0] == x0 && e[1] == z0 && e[2] == x1 && e[3] == z1) || (e[0] == x1 && e[1] == z1 && e[2] == x0 && e[3] == z0)) return true;
        }
        return false;
    }

    private static int degree(List<int[]> edges, int x, int z) {
        int n = 0;
        for (int[] e : edges) if ((e[0] == x && e[1] == z) || (e[2] == x && e[3] == z)) n++;
        return n;
    }

    private static int idOf(int[][] index, int[] cell) { return index[cell[0]][cell[1]]; }

    /** A cell turned by {@code turns} quarter turns about the centre of a grid of side {@code g}. */
    static int[] turn(int[] cell, int g, int turns) {
        int x = cell[0], z = cell[1];
        for (int i = 0; i < turns; i++) {
            int nx = g - 1 - z, nz = x;
            x = nx;
            z = nz;
        }
        return new int[]{x, z};
    }
}
