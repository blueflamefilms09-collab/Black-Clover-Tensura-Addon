package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;

/**
 * Why: "dead end" and "loop" are properties of the room GRAPH (rooms are nodes, doorways are edges), and the owner's requirement is stated in those terms:
 * few degree-one rooms, many rooms that lie on a cycle. This is that graph with nothing else in it: no blocks, no templates, no Minecraft, so the metrics
 * and the algorithms built on it run in a unit test over thousands of seeds, and the same code judges a real sector in the game's self-test.
 *
 * <p>Definitions (the doc {@code docs/labyrinth/cyclic_wfc.md} derives them):
 * <ul>
 * <li><b>degree</b> of a room = the number of distinct rooms it has a doorway to;</li>
 * <li><b>dead end</b> = a room of degree 1 (or 0) that is not a terminal (entrance, stairs, boss chain, Safe Zone: see {@link Roles});</li>
 * <li><b>bridge</b> = an edge whose removal splits the graph; a room is <b>on a cycle</b> iff it touches at least one edge that is not a bridge;</li>
 * <li><b>cyclomatic number</b> = E - V + C, the number of independent loops;</li>
 * <li><b>dangling</b> rooms = the closure of "repeatedly remove a non-terminal room of degree 1": a dead end and the whole branch of rooms that leads only to it.</li>
 * </ul>
 * Nodes are numbered 0..n-1 in insertion order and never removed (a pruned room is simply left out of a {@link #without} copy), so ids stay stable.
 */
public final class RoomGraph {
    private final List<RoomCategory> categories = new ArrayList<>();
    private final List<Integer> roles = new ArrayList<>();
    private final List<TreeSet<Integer>> adj = new ArrayList<>();
    private int edges;

    public RoomGraph() {}

    /** Adds a room; returns its id. */
    public int addNode(RoomCategory category, int roleMask) {
        categories.add(category == null ? RoomCategory.TRANSITION : category);
        roles.add(roleMask);
        adj.add(new TreeSet<>());
        return categories.size() - 1;
    }

    /** Adds a doorway between two different rooms; false when it exists already, is a self-loop or names a room that does not exist. */
    public boolean addEdge(int a, int b) {
        if (a == b || a < 0 || b < 0 || a >= size() || b >= size()) return false;
        if (!adj.get(a).add(b)) return false;
        adj.get(b).add(a);
        edges++;
        return true;
    }

    public boolean hasEdge(int a, int b) { return a >= 0 && a < size() && adj.get(a).contains(b); }

    public int size() { return categories.size(); }

    public int edgeCount() { return edges; }

    public int degree(int n) { return adj.get(n).size(); }

    /** The rooms next to {@code n}, ascending. */
    public List<Integer> neighbours(int n) { return new ArrayList<>(adj.get(n)); }

    public RoomCategory category(int n) { return categories.get(n); }

    public int roles(int n) { return roles.get(n); }

    public boolean hasRole(int n, int mask) { return (roles.get(n) & mask) != 0; }

    public void addRole(int n, int mask) { roles.set(n, roles.get(n) | mask); }

    /** Every edge once as {a, b} with a &lt; b, in ascending order. */
    public List<int[]> edgeList() {
        List<int[]> out = new ArrayList<>(edges);
        for (int a = 0; a < size(); a++) for (int b : adj.get(a)) if (a < b) out.add(new int[] {a, b});
        return out;
    }

    public RoomGraph copy() {
        RoomGraph g = new RoomGraph();
        for (int i = 0; i < size(); i++) g.addNode(categories.get(i), roles.get(i));
        for (int[] e : edgeList()) g.addEdge(e[0], e[1]);
        return g;
    }

    /**
     * A copy without the given rooms (and their doorways). The returned array maps old ids to new ids (-1 for a removed room), so the caller can follow its own
     * records across the removal.
     */
    public Pruned without(BitSet removed) {
        int[] map = new int[size()];
        RoomGraph g = new RoomGraph();
        for (int i = 0; i < size(); i++) map[i] = removed.get(i) ? -1 : g.addNode(categories.get(i), roles.get(i));
        for (int[] e : edgeList()) if (map[e[0]] >= 0 && map[e[1]] >= 0) g.addEdge(map[e[0]], map[e[1]]);
        return new Pruned(g, map);
    }

    public record Pruned(RoomGraph graph, int[] oldToNew) {}

    // ------------------------------------------------------------------------------------------------------------------------------ connectivity

    /** The number of connected components (0 for an empty graph). */
    public int components() {
        boolean[] seen = new boolean[size()];
        int c = 0;
        for (int s = 0; s < size(); s++) {
            if (seen[s]) continue;
            c++;
            ArrayDeque<Integer> q = new ArrayDeque<>();
            q.add(s);
            seen[s] = true;
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : adj.get(u)) if (!seen[v]) { seen[v] = true; q.add(v); }
            }
        }
        return c;
    }

    public boolean connected() { return size() <= 1 || components() == 1; }

    /** Walking distances (doorways) from {@code source}; -1 for a room that cannot be reached. */
    public int[] distances(int source) {
        int[] d = new int[size()];
        Arrays.fill(d, -1);
        if (source < 0 || source >= size()) return d;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        d[source] = 0;
        q.add(source);
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adj.get(u)) if (d[v] < 0) { d[v] = d[u] + 1; q.add(v); }
        }
        return d;
    }

    /** The distance between two rooms, -1 when they are not connected. */
    public int distance(int a, int b) { return distances(a)[b]; }

    /** The rooms of one shortest walk from {@code a} to {@code b} (inclusive, ties broken towards lower ids), or an empty list when none exists. */
    public List<Integer> shortestPath(int a, int b) {
        int[] d = distances(b);
        List<Integer> path = new ArrayList<>();
        if (d[a] < 0) return path;
        int u = a;
        path.add(u);
        while (u != b) {
            int next = -1;
            for (int v : adj.get(u)) if (d[v] == d[u] - 1) { next = v; break; }
            u = next;
            path.add(u);
        }
        return path;
    }

    // ------------------------------------------------------------------------------------------------------------------------------ loops

    /** The bridges as keys {@code a * size + b} (a &lt; b). Iterative lowlink search, so a long corridor cannot overflow the stack. */
    public BitSet bridges() {
        int n = size();
        BitSet out = new BitSet();
        int[] disc = new int[n], low = new int[n];
        Arrays.fill(disc, -1);
        int time = 0;
        for (int root = 0; root < n; root++) {
            if (disc[root] >= 0) continue;
            ArrayDeque<int[]> stack = new ArrayDeque<>();          // {node, parent, next-neighbour-index}
            List<List<Integer>> lists = new ArrayList<>();
            for (int i = 0; i < n; i++) lists.add(null);
            disc[root] = low[root] = time++;
            lists.set(root, new ArrayList<>(adj.get(root)));
            stack.push(new int[] {root, -1, 0});
            while (!stack.isEmpty()) {
                int[] top = stack.peek();
                int u = top[0];
                List<Integer> nb = lists.get(u);
                if (top[2] < nb.size()) {
                    int v = nb.get(top[2]++);
                    if (v == top[1]) continue;
                    if (disc[v] < 0) {
                        disc[v] = low[v] = time++;
                        lists.set(v, new ArrayList<>(adj.get(v)));
                        stack.push(new int[] {v, u, 0});
                    } else low[u] = Math.min(low[u], disc[v]);
                } else {
                    stack.pop();
                    if (!stack.isEmpty()) {
                        int p = stack.peek()[0];
                        low[p] = Math.min(low[p], low[u]);
                        if (low[u] > disc[p]) out.set(key(Math.min(p, u), Math.max(p, u)));
                    }
                }
            }
        }
        return out;
    }

    private int key(int a, int b) { return a * size() + b; }

    /** Which rooms lie on a cycle: those that touch an edge that is not a bridge. */
    public BitSet onCycle() {
        BitSet bridges = bridges();
        BitSet out = new BitSet(size());
        for (int[] e : edgeList()) {
            if (!bridges.get(key(e[0], e[1]))) {
                out.set(e[0]);
                out.set(e[1]);
            }
        }
        return out;
    }

    /** The cyclomatic number E - V + C: how many independent loops the graph has (0 for a tree or forest). */
    public int cyclomatic() { return edges - size() + components(); }

    /** The share (0..1) of rooms that lie on a cycle. */
    public double cycleShare() { return size() == 0 ? 0.0 : onCycle().cardinality() / (double) size(); }

    // ------------------------------------------------------------------------------------------------------------------------------ dead ends

    /** The dead ends: rooms of degree 0 or 1 whose roles do not intersect {@code terminalMask}. (A graph of one room has no dead end: nothing to be stuck in.) */
    public List<Integer> deadEnds(int terminalMask) {
        List<Integer> out = new ArrayList<>();
        if (size() <= 1) return out;
        for (int i = 0; i < size(); i++) if (degree(i) <= 1 && (roles.get(i) & terminalMask) == 0) out.add(i);
        return out;
    }

    /** Dead ends as a share (0..1) of all rooms. */
    public double deadEndRatio(int terminalMask) { return size() == 0 ? 0.0 : deadEnds(terminalMask).size() / (double) size(); }

    /**
     * The rooms of the dangling branches: repeatedly take away a room of degree 1 that is not a terminal (taking it away lowers its neighbour's degree, which
     * may expose the next). In a tree-like dungeon this is every room whose only purpose is to lead to a dead end. Returned in the order they fall away
     * (leaves first), which is also the order {@link DeadEndPruner} removes them in.
     */
    public List<Integer> danglingOrder(int terminalMask) {
        int n = size();
        int[] deg = new int[n];
        for (int i = 0; i < n; i++) deg[i] = degree(i);
        boolean[] gone = new boolean[n];
        List<Integer> order = new ArrayList<>();
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (deg[i] <= 1 && (roles.get(i) & terminalMask) == 0) q.add(i);
        while (!q.isEmpty()) {
            int u = q.poll();
            if (gone[u]) continue;
            if (deg[u] > 1) continue;
            gone[u] = true;
            order.add(u);
            for (int v : adj.get(u)) {
                if (gone[v]) continue;
                deg[v]--;
                if (deg[v] <= 1 && (roles.get(v) & terminalMask) == 0) q.add(v);
            }
        }
        return order;
    }

    /** The rooms of each category. */
    public int[] categoryCounts() {
        int[] c = new int[RoomCategory.values().length];
        for (RoomCategory k : categories) c[k.ordinal()]++;
        return c;
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the report

    /** All the numbers the doc, the tests and the self-test talk about, for one graph and one terminal mask. */
    public record Report(int rooms, int edges, int components, int deadEnds, double deadEndRatio, int dangling, int cyclomatic, int onCycle, double cycleShare,
                         int bridges, double meanDegree) {
        @Override
        public String toString() {
            return String.format(java.util.Locale.ROOT, "rooms=%d edges=%d deadEnds=%d (%.1f%%) dangling=%d loops=%d onCycle=%d (%.1f%%) bridges=%d meanDegree=%.2f components=%d",
                    rooms, edges, deadEnds, deadEndRatio * 100, dangling, cyclomatic, onCycle, cycleShare * 100, bridges, meanDegree, components);
        }
    }

    public Report report(int terminalMask) {
        int n = size();
        BitSet cyc = onCycle();
        return new Report(n, edges, components(), deadEnds(terminalMask).size(), deadEndRatio(terminalMask), danglingOrder(terminalMask).size(), cyclomatic(),
                cyc.cardinality(), n == 0 ? 0.0 : cyc.cardinality() / (double) n, bridges().cardinality(), n == 0 ? 0.0 : 2.0 * edges / n);
    }
}
