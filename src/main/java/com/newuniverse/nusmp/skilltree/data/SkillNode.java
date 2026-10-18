package com.newuniverse.nusmp.skilltree.data;

import java.util.List;
import java.util.Map;

/**
 * Why: a node is the smallest thing a player can buy in a skill tree, and it has to be pure data so that a new magic (a Fairy Tail one today, any anime tomorrow) is a
 * JSON file and not a Java class. It carries exactly what the owner listed (an id, the prerequisite nodes, the SP cost, the maximum level, the ability it grants) plus
 * what the screen and the abilities need: the texts, a position on the tree canvas, an icon, and a table of per-level numbers ({@code values}) that the granted ability reads, so
 * balancing an ability is editing a datapack and not rebuilding the mod. Immutable and free of Minecraft types so the rules can be unit-tested without a server.
 *
 * @param id        unique within its route, lower-case snake ({@code fire_dragons_roar})
 * @param name      display name
 * @param desc      one or two sentences for the tooltip
 * @param parents   nodes that must already be at a level before this one can be bought (all of them); empty for a root node
 * @param cost      SP price of one level (the price of level n is {@link #costOf} )
 * @param costs     optional price per level (index 0 is level 1); when shorter than the maximum level the last entry repeats; empty means every level costs {@code cost}
 * @param maxLevel  how many times the node can be bought (at least 1)
 * @param skill     the id of the ManasCore skill this node grants as soon as it reaches level 1 ({@code nusmp:ft_fire_dragons_roar}), or an empty string for none
 * @param passives  ids of passive effects the node switches on while its level is at least 1 (implemented by the route's content code)
 * @param values    named tables of numbers by level (index 0 is level 1), for example {@code damage: [6, 8, 10]}; the ability reads them through {@link #value}
 * @param x         column on the tree canvas (grid units, 0 at the left)
 * @param y         row on the tree canvas (grid units, 0 at the top)
 * @param icon      an item id used as the node's icon ({@code minecraft:blaze_powder}), or an empty string for the route's icon
 */
public record SkillNode(String id, String name, String desc, List<Parent> parents, int cost, List<Integer> costs, int maxLevel, String skill, List<String> passives,
                        Map<String, List<Double>> values, int x, int y, String icon) {

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
