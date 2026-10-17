package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Why: a wandering boss that walks to a random point of the sector would cut through walls in the planner's mind and stall against them in the
 * world; one that walks from ROOM to ROOM along the doorways follows the dungeon's own shape and meets the party where the party actually walks.
 * The room graph is already saved with every sector (a room per placed piece, an edge per doorway, loops included), so the patrol is a random
 * walk on it with three habits: it does not turn straight back (the node it just came from is unlikely, unless it is a dead end), it leans away
 * from places it has been lately (so it covers the sector instead of orbiting two halls), and it keeps its distance from safe rooms (it never
 * enters one, and it does not loiter at their doors, which would turn the rest room's doorway into a trap). Nodes that cannot be entered (safe
 * rooms, the Guardian's hall, the vault) are in the graph but marked, so a corridor that passes beside them still connects. Pure: nodes are
 * numbers and coordinates, built by the game from the sector's rooms and markers.
 */
public final class PatrolGraph {
    /** A place on the patrol: a room (its id), where to stand in it, whether the wanderer may go there, and whether the room only carries traffic. */
    public record Node(int id, double x, double y, double z, boolean allowed, boolean circulation) {}

    private final Map<Integer, Node> nodes = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> neighbours = new HashMap<>();
    private final Set<Integer> nearForbidden = new HashSet<>();
    private int allowedCount;

    private PatrolGraph() {}

    /** Builds the graph. Edges naming an unknown node and self-loops are ignored, duplicates are merged. */
    public static PatrolGraph build(List<Node> list, List<int[]> edges) {
        PatrolGraph g = new PatrolGraph();
        for (Node n : list) {
            if (g.nodes.putIfAbsent(n.id(), n) == null) {
                g.neighbours.put(n.id(), new ArrayList<>());
                if (n.allowed()) g.allowedCount++;
            }
        }
        for (int[] e : edges) {
            if (e.length < 2 || e[0] == e[1] || !g.nodes.containsKey(e[0]) || !g.nodes.containsKey(e[1])) continue;
            List<Integer> a = g.neighbours.get(e[0]), b = g.neighbours.get(e[1]);
            if (!a.contains(e[1])) a.add(e[1]);
            if (!b.contains(e[0])) b.add(e[0]);
        }
        for (Node n : g.nodes.values()) {
            if (n.allowed()) continue;
            for (int o : g.neighbours.get(n.id())) g.nearForbidden.add(o);
        }
        return g;
    }

    public Node node(int id) { return nodes.get(id); }

    public boolean has(int id) { return nodes.containsKey(id); }

    /** How many nodes the wanderer may stand in. */
    public int size() { return allowedCount; }

    /** Is the node next to a place the wanderer may not enter? */
    public boolean nearForbidden(int id) { return nearForbidden.contains(id); }

    /** The nodes joined to this one by a doorway that the wanderer may enter. */
    public List<Integer> neighbours(int id) {
        List<Integer> all = neighbours.get(id);
        if (all == null) return List.of();
        List<Integer> out = new ArrayList<>(all.size());
        for (int o : all) if (nodes.get(o).allowed()) out.add(o);
        return out;
    }

    /**
     * The next node of a patrol standing at {@code current}. {@code recent} is the nodes visited lately, oldest first; {@code avoid} is nodes
     * that failed lately (the way was blocked). Weights: a recent node counts a quarter, the one just left half of that again, a node beside a
     * forbidden room half, a room (not a corridor) a third more; a node in {@code avoid} counts nothing unless every neighbour is avoided.
     * Returns -1 when the node has no neighbour it may enter.
     */
    public int next(int current, List<Integer> recent, Set<Integer> avoid, Random r) {
        List<Integer> cands = neighbours(current);
        if (cands.isEmpty()) return -1;
        boolean allAvoided = true;
        for (int c : cands) if (avoid == null || !avoid.contains(c)) allAvoided = false;
        int last = recent == null || recent.isEmpty() ? Integer.MIN_VALUE : recent.get(recent.size() - 1);
        int previous = last == current && recent.size() >= 2 ? recent.get(recent.size() - 2) : last;
        double[] w = new double[cands.size()];
        double total = 0.0;
        for (int i = 0; i < w.length; i++) {
            int c = cands.get(i);
            double x = 1.0;
            if (recent != null && recent.contains(c)) x *= 0.25;
            if (c == previous) x *= 0.5;
            if (nearForbidden.contains(c)) x *= 0.5;
            if (!nodes.get(c).circulation()) x *= 1.3;
            if (!allAvoided && avoid != null && avoid.contains(c)) x = 0.0;
            w[i] = x;
            total += x;
        }
        if (total <= 0.0) return cands.get(r.nextInt(cands.size()));
        double roll = r.nextDouble() * total;
        for (int i = 0; i < w.length; i++) {
            roll -= w[i];
            if (roll < 0.0) return cands.get(i);
        }
        return cands.get(cands.size() - 1);
    }

    /** The allowed node nearest to a point in the horizontal plane, or -1 when there is none. */
    public int nearest(double x, double z) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (Node n : nodes.values()) {
            if (!n.allowed()) continue;
            double dx = n.x() - x, dz = n.z() - z, d = dx * dx + dz * dz;
            if (d < bd) {
                bd = d;
                best = n.id();
            }
        }
        return best;
    }

    /**
     * Where a wanderer may appear: an allowed node at least {@code minDist} blocks from every point in {@code avoid} (the party), a room rather
     * than a corridor by three to one; when no node is that far, the one that is farthest from the party. -1 when the graph has no allowed node.
     */
    public int spawnNode(List<double[]> avoid, double minDist, Random r) {
        List<Node> far = new ArrayList<>();
        Node farthest = null;
        double fd = -1.0;
        for (Node n : nodes.values()) {
            if (!n.allowed()) continue;
            double nearest = Double.MAX_VALUE;
            for (double[] p : avoid) {
                double dx = n.x() - p[0], dy = n.y() - p[1], dz = n.z() - p[2];
                nearest = Math.min(nearest, Math.sqrt(dx * dx + dy * dy + dz * dz));
            }
            if (nearest >= minDist) far.add(n);
            if (nearest > fd) {
                fd = nearest;
                farthest = n;
            }
        }
        if (far.isEmpty()) return farthest == null ? -1 : farthest.id();
        double total = 0.0;
        for (Node n : far) total += n.circulation() ? 1.0 : 3.0;
        double roll = r.nextDouble() * total;
        for (Node n : far) {
            roll -= n.circulation() ? 1.0 : 3.0;
            if (roll < 0.0) return n.id();
        }
        return far.get(far.size() - 1).id();
    }
}
