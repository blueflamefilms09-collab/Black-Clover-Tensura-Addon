package com.newuniverse.nusmp.skilltree.ft.dragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the numbers of a route live in its JSON file so that balance is data (a datapack and {@code /reload} retune a technique with no rebuild), but a cast must never throw
 * or do nothing because a datapack author deleted a key, and a typo between "what the code reads" and "what the file says" cannot be seen by playing a game nobody can run.
 * So every route keeps ONE table of the shipped numbers in code, the safety net under the data: the skill reads the registry's value and falls back to this table, and a unit
 * test proves the table and the shipped file are the same numbers and that every key the skill classes read exists here. The class is PURE (no Minecraft or NeoForge type)
 * so that proof, and the budget check of {@link DragonBudget} over the defaults, run in a plain JUnit test. It is built once at class load by a fluent builder and is
 * immutable afterwards, so it is safe to read from any thread.
 */
public final class DragonTables {
    /** One node's shipped numbers: what it is, how many levels it has, and its tables (key to the number at each level, index 0 is level 1). Private: nothing outside edits them. */
    private record Node(DragonBudget.Kind kind, int maxLevel, Map<String, double[]> values) {}

    private final String route;
    private final Map<String, Node> nodes;

    private DragonTables(String route, Map<String, Node> nodes) {
        this.route = route;
        this.nodes = nodes;
    }

    /** Starts a builder for the route with this id ({@code nusmp:fire_dragon_slayer}). */
    public static Builder of(String route) { return new Builder(route); }

    public String route() { return route; }

    /** The node ids in the order they were declared. */
    public List<String> nodeIds() { return List.copyOf(nodes.keySet()); }

    public boolean hasNode(String node) { return nodes.containsKey(node); }

    public boolean has(String node, String key) {
        Node n = nodes.get(node);
        return n != null && n.values().containsKey(key);
    }

    public DragonBudget.Kind kind(String node) { return nodes.get(node).kind(); }

    public int maxLevel(String node) { return nodes.get(node).maxLevel(); }

    /**
     * The shipped number at a level (levels start at 1; a level past the table repeats its last entry, below 1 reads the first, exactly like {@code SkillNode.value}).
     * Returns 0 for a node or key that does not exist: the test that scans the skill classes makes that case impossible in the shipped code.
     */
    public double def(String node, String key, int level) {
        Node n = nodes.get(node);
        if (n == null) return 0.0;
        double[] t = n.values().get(key);
        if (t == null || t.length == 0) return 0.0;
        return t[Math.max(0, Math.min(t.length - 1, level - 1))];
    }

    /** A node's tables in the shape of the JSON {@code values} object, for the budget check and the comparison with the shipped file. */
    public Map<String, List<Double>> valuesOf(String node) {
        Map<String, List<Double>> out = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> e : nodes.get(node).values().entrySet()) {
            List<Double> l = new ArrayList<>(e.getValue().length);
            for (double d : e.getValue()) l.add(d);
            out.put(e.getKey(), Collections.unmodifiableList(l));
        }
        return out;
    }

    /** Every complaint {@link DragonBudget#check} has about the shipped defaults (empty when the tables obey the budget). */
    public List<String> violations() {
        List<String> bad = new ArrayList<>();
        for (Map.Entry<String, Node> e : nodes.entrySet()) {
            Node n = e.getValue();
            bad.addAll(DragonBudget.check(e.getKey(), n.kind(), n.maxLevel(), valuesOf(e.getKey())));
        }
        return bad;
    }

    /** Fluent builder: {@code of(route).node("iron_fist", ACTIVE, 5).v("damage", 6, 7, 8, 9, 11)...build()}. */
    public static final class Builder {
        private final String route;
        private final Map<String, Node> nodes = new LinkedHashMap<>();
        private String current;

        private Builder(String route) { this.route = route; }

        /** Starts the next node; its {@link #v} calls follow. */
        public Builder node(String id, DragonBudget.Kind kind, int maxLevel) {
            if (nodes.containsKey(id)) throw new IllegalArgumentException("node declared twice: " + id);
            nodes.put(id, new Node(kind, maxLevel, new LinkedHashMap<>()));
            current = id;
            return this;
        }

        /** Adds a table to the node just started: one number per level, starting at level 1. */
        public Builder v(String key, double... perLevel) {
            if (current == null) throw new IllegalStateException("v() before node()");
            Map<String, double[]> values = nodes.get(current).values();
            if (values.containsKey(key)) throw new IllegalArgumentException("table declared twice: " + current + "." + key);
            values.put(key, perLevel.clone());
            return this;
        }

        public DragonTables build() {
            Map<String, Node> frozen = new LinkedHashMap<>();
            for (Map.Entry<String, Node> e : nodes.entrySet()) {
                Node n = e.getValue();
                frozen.put(e.getKey(), new Node(n.kind(), n.maxLevel(), Collections.unmodifiableMap(new LinkedHashMap<>(n.values()))));
            }
            return new DragonTables(route, Collections.unmodifiableMap(frozen));
        }
    }
}
