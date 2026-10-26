package com.newuniverse.nusmp.skilltree.data;

import java.util.List;
import java.util.Map;

/**
 * Why: a node is the smallest thing a player can buy in a skill tree, and it has to be pure data so that a new magic (any anime magic) is a
 * JSON file and not a Java class. It carries exactly what the owner listed (an id, the prerequisite nodes, the SP cost, the maximum level, the ability it grants) plus
 * what the screen and the abilities need: the texts, a position on the tree canvas, an icon, and a table of per-level numbers ({@code values}) that the granted ability reads, so
 * balancing an ability is editing a datapack and not rebuilding the mod. Immutable and free of Minecraft types so the rules can be unit-tested without a server.
 *
 * @param id        unique within its route, lower-case snake ({@code provoke})
 * @param name      display name
 * @param desc      one or two sentences for the tooltip
 * @param parents   nodes that must already be at a level before this one can be bought (all of them); empty for a root node
 * @param cost      SP price of one level (the price of level n is {@link #costOf} )
 * @param costs     optional price per level (index 0 is level 1); when shorter than the maximum level the last entry repeats; empty means every level costs {@code cost}
 * @param maxLevel  how many times the node can be bought (at least 1)
 * @param skill     the id of the ManasCore skill this node grants as soon as it reaches level 1 ({@code nusmp:hk_provoke}), or an empty string for none
 * @param passives  ids of passive effects the node switches on while its level is at least 1 (implemented by the route's content code)
 * @param values    named tables of numbers by level (index 0 is level 1), for example {@code damage: [6, 8, 10]}; the ability reads them through {@link #value}
 * @param x         column on the tree canvas (grid units, 0 at the left)
 * @param y         row on the tree canvas (grid units, 0 at the top)
 * @param icon      an item id used as the node's icon ({@code minecraft:blaze_powder}), or an empty string for the route's icon
 * @param type      1.82.0 (Unique Skill Lines): {@code active_ability}, {@code passive_toggle} or {@code attribute_modifier}; an empty string for a node written before the field existed (see {@link #nodeType})
 * @param kind      the effect kind of the catalogue ({@code cone_strike}, {@code lifesteal}, {@code stance}, {@code attr}, ...) the line engine runs for this node, or an empty string for a classic node
 * @param params    the string (enum) parameters of the kind ({@code vfx}, {@code effect}, {@code condition}, ...); the numbers are in {@code values}
 * @param ultimate  3.26.0 (Evolution skills): this art is the Ultimate Evolution art, locked until the line's evolution requirement is met and the skill is mastered
 * @param evolved   3.26.0: the parameters that replace this passive's numbers and words once the line has evolved (empty for every other node)
 */
public record SkillNode(String id, String name, String desc, List<Parent> parents, int cost, List<Integer> costs, int maxLevel, String skill, List<String> passives,
                        Map<String, List<Double>> values, int x, int y, String icon, String type, String kind, Map<String, String> params,
                        boolean ultimate, EvolvedParams evolved) {

    public static final String TYPE_ACTIVE = "active_ability", TYPE_PASSIVE = "passive_toggle", TYPE_ATTR = "attribute_modifier";

    /** What a node is to the engine. */
    public enum NodeType { ACTIVE, PASSIVE, ATTR, PLAIN }

    /**
     * Why: the 16-argument form of the record before 3.26.0 (no ultimate flag, no evolved block). Every old call site keeps compiling and means a node with neither. Add the new, keep the old.
     */
    public SkillNode(String id, String name, String desc, List<Parent> parents, int cost, List<Integer> costs, int maxLevel, String skill, List<String> passives,
                     Map<String, List<Double>> values, int x, int y, String icon, String type, String kind, Map<String, String> params) {
        this(id, name, desc, parents, cost, costs, maxLevel, skill, passives, values, x, y, icon, type, kind, params, false, EvolvedParams.NONE);
    }

    /**
     * Why: the 13-argument form of the record before 1.82.0 (no type, no kind, no params). Every old call site keeps compiling and means exactly what it meant: a classic node whose ability is a
     * registered ManasCore skill or a hand-written passive. Add the new, keep the old.
     */
    public SkillNode(String id, String name, String desc, List<Parent> parents, int cost, List<Integer> costs, int maxLevel, String skill, List<String> passives,
                     Map<String, List<Double>> values, int x, int y, String icon) {
        this(id, name, desc, parents, cost, costs, maxLevel, skill, passives, values, x, y, icon, "", "", Map.of(), false, EvolvedParams.NONE);
    }

    /**
     * The node's type. The explicit {@code type} wins; an old node without it is inferred (has a {@code skill} -> active, has {@code passives} -> passive), as the contract says; anything
     * else is {@link NodeType#PLAIN} (a node that only unlocks other nodes).
     */
    public NodeType nodeType() {
        if (TYPE_ACTIVE.equals(type)) return NodeType.ACTIVE;
        if (TYPE_PASSIVE.equals(type)) return NodeType.PASSIVE;
        if (TYPE_ATTR.equals(type)) return NodeType.ATTR;
        if (grantsSkill()) return NodeType.ACTIVE;
        if (passives != null && !passives.isEmpty()) return NodeType.PASSIVE;
        return NodeType.PLAIN;
    }

    /** True when the line engine runs this node (it has a catalogue kind). */
    public boolean isLineNode() { return kind != null && !kind.isEmpty(); }

    /** True for a node that gets a switch in the Passives panel: a line node of type passive_toggle or attribute_modifier. */
    public boolean hasToggle() { return isLineNode() && (TYPE_PASSIVE.equals(type) || TYPE_ATTR.equals(type)); }

    /** True when this passive carries an {@code evolved} block (its numbers change once the line has evolved). */
    public boolean hasEvolved() { return evolved != null && !evolved.isEmpty(); }

    /** An enum parameter of the node ({@code vfx}, {@code effect}, ...), or {@code fallback} when it has none. */
    public String param(String key, String fallback) {
        String v = params == null ? null : params.get(key);
        return v == null || v.isEmpty() ? fallback : v;
    }

    /** A prerequisite: node {@code node} of the same route must be at least at {@code level}. */
    public record Parent(String node, int level) {}

    public boolean isRoot() { return parents.isEmpty(); }

    /** The SP price of buying {@code level} (1 to maxLevel). */
    public int costOf(int level) {
        if (costs.isEmpty()) return cost;
        int i = Math.max(0, Math.min(costs.size() - 1, level - 1));
        return costs.get(i);
    }

    /** The SP spent to reach {@code level} from nothing. */
    public int totalCost(int level) {
        int sum = 0;
        for (int l = 1; l <= Math.min(level, maxLevel); l++) sum += costOf(l);
        return sum;
    }

    /** The named number at a node level (levels start at 1; a level beyond the table repeats its last entry); {@code fallback} when the table does not exist. */
    public double value(String key, int level, double fallback) {
        List<Double> table = values.get(key);
        if (table == null || table.isEmpty()) return fallback;
        int i = Math.max(0, Math.min(table.size() - 1, level - 1));
        return table.get(i);
    }

    public boolean grantsSkill() { return skill != null && !skill.isEmpty(); }
}
