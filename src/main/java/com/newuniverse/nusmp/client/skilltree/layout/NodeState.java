package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeRules;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.NodeStatus;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.Verdict;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the canvas, the detail card, the Buy button and the tooltips all need the same answer to "what can the player do with this node right now", and that answer is the rule the server
 * enforces when the purchase packet arrives (route unlocked, not maxed, every parent at its level, enough SP for the next level). The core system already owns that rule, once, in the pure
 * {@link TreeRules}, so this class does NOT re-implement any of it: it asks {@code TreeRules.status}, {@code priceOfNext} and {@code check} and only gathers the answers into one value the
 * screen can hand around, plus the two small questions the screen alone has (which parents are still missing and at what level, and how many nodes of a route the player has learned). Two
 * copies of a rule drift apart, and a drifted copy on the client is a button that does nothing or, worse, a purchase that is offered and refused; asking the shared rule makes that
 * impossible. The screen decides nothing, it only predicts. It works on the core's tiny {@link TreeState} (the synced tree is wrapped by {@code PlayerTreeState}, the tests use
 * {@code SimpleTreeState}), so it needs no Minecraft type and runs in a unit test.
 *
 * @param status the six looks of a node, as the core names them (the first two cannot be clicked, {@code AVAILABLE} glows)
 * @param level  the level the player has bought (0 when none)
 * @param max    the highest level the node has
 * @param price  the SP cost of the next level, or -1 when the node is at its maximum (the core's {@code priceOfNext})
 * @param verdict the core's answer to "may the next level be bought?": {@code OK}, or the first rule that says no, in the server's own order and words (the Buy button and the refusal
 *               notice are worded from it, so the screen explains a refusal with the reason the server would give)
 */
public record NodeState(NodeStatus status, int level, int max, int price, Verdict verdict) {

    /** True when a purchase of the next level would be accepted right now (an owned node can be upgradable, an available one always is). */
    public boolean canBuy() { return verdict == Verdict.OK; }

    /** One prerequisite of a node: the parent node, the level it must reach, and the level the player has. */
    public record Requirement(String node, int need, int have) {
        public boolean met() { return have >= need; }
    }

    /** The state of {@code node} of {@code route} for the player whose tree is {@code s}, as the shared rules see it. */
    public static NodeState of(TreeState s, SkillRoute route, SkillNode node) {
        int level = Math.max(0, s.level(route.id(), node.id()));
        return new NodeState(TreeRules.status(route, node, s), level, node.maxLevel(), TreeRules.priceOfNext(node, level), TreeRules.check(route, node.id(), s));
    }

    /** The prerequisites of the node, in the order the data lists them, each with the level the player has (a parent the player has not touched is level 0). */
    public static List<Requirement> requirements(TreeState s, SkillRoute route, SkillNode node) {
        List<Requirement> out = new ArrayList<>();
        for (SkillNode.Parent p : node.parents()) out.add(new Requirement(p.node(), p.level(), s.level(route.id(), p.node())));
        return out;
    }

    /** How many nodes of the route the player has bought at least once. */
    public static int ownedNodes(TreeState s, SkillRoute route) {
        int n = 0;
        for (SkillNode node : route.nodes()) if (s.level(route.id(), node.id()) >= 1) n++;
        return n;
    }
}
