package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the layout classes are tested on the SHAPES the shipped trees have (a root, two branches, passives on the side, a Secret Art that needs one node from each branch), not on
 * the shipped files, because those files belong to the content builders and are not all there while this screen is built. The shapes follow the Fire Dragon Slayer example of the
 * design (Iron Fist, Roar, then a Secret Art) with the owner's "two branches and a secret art needing two" rule, so a change to the layout code is checked against the tree the owner
 * actually asked for.
 */
final class SampleRoutes {
    private SampleRoutes() {}

    /** A node with parents given as alternating id and level: {@code node("roar", 2, 1, 2, 3, "iron_fist", 3)}. */
    static SkillNode node(String id, int x, int y, int cost, int max, Object... parents) {
        List<SkillNode.Parent> ps = new ArrayList<>();
        for (int i = 0; i + 1 < parents.length; i += 2) ps.add(new SkillNode.Parent((String) parents[i], (Integer) parents[i + 1]));
        return new SkillNode(id, id, "", ps, cost, List.of(), max, "", List.of(), Map.of(), x, y, "");
    }

    static SkillRoute route(String id, SkillNode... nodes) {
        return new SkillRoute(id, "fairy_tail", id, "", id + "_book", id + " book", "", 0xE8541C, "minecraft:blaze_powder", id.substring(id.indexOf(':') + 1), 3, 1, List.of(nodes));
    }

    /** The shape of the Fire Dragon Slayer: 9 nodes, two branches (ranged above, melee below), passives in the middle, a Secret Art that needs the end of both branches. */
    static SkillRoute fireDragon() {
        return route("nusmp:fire_dragon_slayer",
                node("iron_fist", 0, 3, 1, 5),
                node("roar", 2, 1, 2, 3, "iron_fist", 3),
                node("brilliant_flame", 4, 0, 3, 3, "roar", 2),
                node("claw", 2, 5, 2, 3, "iron_fist", 2),
                node("wing_attack", 4, 6, 3, 3, "claw", 2),
                node("scales", 2, 3, 1, 5, "iron_fist", 1),
                node("flame_eater", 4, 3, 2, 3, "scales", 3),
                node("crimson_lotus", 6, 3, 8, 1, "roar", 3, "claw", 3),
                node("dragon_force", 6, 6, 4, 2, "wing_attack", 3));
    }

    /** A single root: the smallest legal tree. */
    static SkillRoute single() { return route("nusmp:single", node("only", 0, 0, 1, 1)); }

    /** A vertical chain of {@code n} nodes, each needing the one above at level 1. */
    static SkillRoute chain(int n) {
        SkillNode[] nodes = new SkillNode[n];
        for (int i = 0; i < n; i++) nodes[i] = i == 0 ? node("n0", 3, 0, 1, 2) : node("n" + i, 3, i, 1, 2, "n" + (i - 1), 1);
        return route("nusmp:chain", nodes);
    }

    /** The widest tree the schema allows: 13 columns by 10 rows with a node on every corner. */
    static SkillRoute wide() {
        return route("nusmp:wide",
                node("a", 0, 0, 1, 1), node("b", 12, 0, 1, 1, "a", 1), node("c", 0, 9, 1, 1, "a", 1), node("d", 12, 9, 1, 1, "b", 1, "c", 1));
    }

    /** A player of the core's in-memory state: the route unlocked (or not), {@code sp} Skill Points, and node levels given as alternating id and level. */
    static SimpleTreeState state(SkillRoute r, boolean unlocked, int sp, Object... idsAndLevels) {
        SimpleTreeState s = new SimpleTreeState().withSp(sp);
        if (unlocked) s.unlock(r.id());
        for (int i = 0; i + 1 < idsAndLevels.length; i += 2) s.setLevel(r.id(), (String) idsAndLevels[i], (Integer) idsAndLevels[i + 1]);
        return s;
    }
}
