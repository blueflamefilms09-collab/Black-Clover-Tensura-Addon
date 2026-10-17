package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Why: "farthest by walking distance" is the whole point of where a floor's forward stairs go, and a walker's distance is not the distance on the map: it is how
 * many doorways have to be passed to get from one room to another, through the loops as well as the tree of joins. That is a breadth-first search over the room
 * graph, and it is needed in three places (the assembler choosing the stairwell's host, the audit checking the stairs are not just a doorstep from the arrival,
 * the saved-sector audit and the tests proving the stairwell stands at the far end), so it is written once, on plain numbers, with no game type.
 */
public final class WalkGraph {
    private WalkGraph() {}

    /**
     * The number of edges on the shortest walk from {@code source} to every node of an undirected graph of {@code n} nodes (0..n-1), -1 for a node that cannot be
     * reached. Edges with an end outside 0..n-1, an edge from a node to itself and repeated edges are ignored; a source outside the graph reaches nothing.
     */
    public static int[] distances(int n, List<int[]> edges, int source) {
        int[] dist = new int[Math.max(0, n)];
        Arrays.fill(dist, -1);
        if (source < 0 || source >= dist.length) return dist;
        List<List<Integer>> adj = new ArrayList<>(dist.length);
        for (int i = 0; i < dist.length; i++) adj.add(new ArrayList<>());
        if (edges != null) {
            for (int[] e : edges) {
                if (e == null || e.length < 2) continue;
                int a = e[0], b = e[1];
                if (a == b || a < 0 || b < 0 || a >= dist.length || b >= dist.length) continue;
                adj.get(a).add(b);
                adj.get(b).add(a);
            }
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        dist[source] = 0;
        queue.add(source);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : adj.get(u)) {
                if (dist[v] < 0) {
                    dist[v] = dist[u] + 1;
                    queue.add(v);
                }
            }
        }
        return dist;
    }

    /** The largest finite distance in {@code dist} (0 for an empty array or one with nothing reached). */
    public static int farthest(int[] dist) {
        int best = 0;
        for (int d : dist) best = Math.max(best, d);
        return best;
    }
}
