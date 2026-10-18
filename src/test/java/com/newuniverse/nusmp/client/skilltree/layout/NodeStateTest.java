package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;
import com.newuniverse.nusmp.skilltree.rules.TreeRules;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.NodeStatus;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.Verdict;
import com.newuniverse.nusmp.skilltree.rules.TreeState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Why: {@link NodeState} is only a wrapper around the core's shared rules, so what these tests pin is the wrapping, not the rules (the core's own rule tests do that): the price is the
 * price of the NEXT level and -1 at the maximum, "can buy" is the core's verdict and not a second opinion (so the Buy button and the server cannot disagree), an owned node can still be
 * upgradable, the requirements list says what the player has and what is missing, and the learned-node count counts nodes, not levels. A final sweep runs every state of the sample tree
 * through both the wrapper and the core and demands the same status, price and verdict, so a change in either that makes them differ fails here first.
 */
class NodeStateTest {
    private static final SkillRoute FIRE = SampleRoutes.fireDragon();

    private static NodeState of(String node, TreeState s) { return NodeState.of(s, FIRE, FIRE.node(node)); }

    @Test
    void aSealedRouteIsSealedWhateverTheTreeHolds() {
        NodeState s = of("iron_fist", SampleRoutes.state(FIRE, false, 99, "iron_fist", 5));
        assertEquals(NodeStatus.ROUTE_LOCKED, s.status());
        assertFalse(s.canBuy());
        assertEquals(Verdict.NO_ROUTE, s.verdict());
        assertEquals(NodeStatus.ROUTE_LOCKED, of("roar", SampleRoutes.state(FIRE, false, 99)).status());
    }

    @Test
    void aRootNodeIsAvailableWithTheSpAndTooExpensiveWithout() {
        NodeState rich = of("iron_fist", SampleRoutes.state(FIRE, true, 1));
        assertEquals(NodeStatus.AVAILABLE, rich.status());
        assertTrue(rich.canBuy());
        assertEquals(1, rich.price());
        NodeState poor = of("iron_fist", SampleRoutes.state(FIRE, true, 0));
        assertEquals(NodeStatus.TOO_EXPENSIVE, poor.status());
        assertFalse(poor.canBuy());
        assertEquals(Verdict.NO_SP, poor.verdict());
    }

    @Test
    void aParentOneLevelShortBlocksAndAtItsLevelUnblocks() {
        NodeState blocked = of("roar", SampleRoutes.state(FIRE, true, 99, "iron_fist", 2));          // roar needs iron_fist 3
        assertEquals(NodeStatus.PARENTS_LOCKED, blocked.status());
        assertEquals(Verdict.PARENTS, blocked.verdict());
        NodeState open = of("roar", SampleRoutes.state(FIRE, true, 2, "iron_fist", 3));
        assertEquals(NodeStatus.AVAILABLE, open.status());
        assertTrue(open.canBuy());
        assertEquals(NodeStatus.TOO_EXPENSIVE, of("roar", SampleRoutes.state(FIRE, true, 1, "iron_fist", 3)).status());
    }

    @Test
    void theSecretArtNeedsBothBranchesAndTheRequirementsSayWhatIsMissing() {
        assertEquals(NodeStatus.PARENTS_LOCKED, of("crimson_lotus", SampleRoutes.state(FIRE, true, 99, "roar", 3)).status());
        TreeState both = SampleRoutes.state(FIRE, true, 8, "roar", 3, "claw", 3);
        assertEquals(NodeStatus.AVAILABLE, of("crimson_lotus", both).status());
        assertEquals(NodeStatus.TOO_EXPENSIVE, of("crimson_lotus", SampleRoutes.state(FIRE, true, 7, "roar", 3, "claw", 3)).status());
        assertEquals(List.of(new NodeState.Requirement("roar", 3, 3), new NodeState.Requirement("claw", 3, 3)), NodeState.requirements(both, FIRE, FIRE.node("crimson_lotus")));
        List<NodeState.Requirement> half = NodeState.requirements(SampleRoutes.state(FIRE, true, 0, "roar", 3, "claw", 1), FIRE, FIRE.node("crimson_lotus"));
        assertTrue(half.get(0).met());
        assertFalse(half.get(1).met());
        assertEquals(1, half.get(1).have());
        assertEquals(List.of(), NodeState.requirements(both, FIRE, FIRE.node("iron_fist")), "a root has no requirements");
    }

    @Test
    void anOwnedNodeIsOwnedAndStillUpgradableWhenItCanAffordTheNextLevel() {
        NodeState rich = of("iron_fist", SampleRoutes.state(FIRE, true, 5, "iron_fist", 2));
        assertEquals(NodeStatus.OWNED, rich.status());
        assertEquals(2, rich.level());
        assertEquals(5, rich.max());
        assertTrue(rich.canBuy());
        NodeState poor = of("iron_fist", SampleRoutes.state(FIRE, true, 0, "iron_fist", 2));
        assertEquals(NodeStatus.OWNED, poor.status());
        assertFalse(poor.canBuy());
        assertEquals(Verdict.NO_SP, poor.verdict());
    }

    @Test
    void aMaxedNodeHasNoPriceAndCannotBeBought() {
        NodeState s = of("iron_fist", SampleRoutes.state(FIRE, true, 999, "iron_fist", 5));
        assertEquals(NodeStatus.MAXED, s.status());
        assertFalse(s.canBuy());
        assertEquals(Verdict.MAXED, s.verdict());
        assertEquals(-1, s.price(), "the core's convention: no next level, no price");
        assertEquals(NodeStatus.MAXED, of("iron_fist", SampleRoutes.state(FIRE, true, 0, "iron_fist", 9)).status(), "a level above the maximum (stale data) is still maxed");
    }

    @Test
    void thePriceIsThePriceOfTheNextLevel() {
        SkillNode tiered = new SkillNode("t", "t", "", List.of(), 1, List.of(2, 3, 5), 3, "", List.of(), Map.of(), 0, 0, "");
        SkillRoute r = SampleRoutes.route("nusmp:tiered", tiered);
        assertEquals(2, NodeState.of(SampleRoutes.state(r, true, 9), r, tiered).price());
        assertEquals(3, NodeState.of(SampleRoutes.state(r, true, 9, "t", 1), r, tiered).price());
        assertEquals(5, NodeState.of(SampleRoutes.state(r, true, 9, "t", 2), r, tiered).price());
        assertFalse(NodeState.of(SampleRoutes.state(r, true, 4, "t", 2), r, tiered).canBuy(), "4 SP does not cover the 5 SP third level");
        assertTrue(NodeState.of(SampleRoutes.state(r, true, 5, "t", 2), r, tiered).canBuy());
    }

    @Test
    void levelsOfOtherRoutesDoNotLeakIn() {
        SimpleTreeState other = new SimpleTreeState().unlock(FIRE.id()).withSp(1);
        other.setLevel("nusmp:lightning_dragon_slayer", "iron_fist", 5);
        assertEquals(NodeStatus.AVAILABLE, of("iron_fist", other).status());
        assertEquals(0, of("iron_fist", other).level());
    }

    @Test
    void ownedNodesCountsNodesNotLevels() {
        assertEquals(0, NodeState.ownedNodes(SampleRoutes.state(FIRE, true, 0), FIRE));
        assertEquals(2, NodeState.ownedNodes(SampleRoutes.state(FIRE, true, 0, "iron_fist", 5, "roar", 1, "claw", 0), FIRE));
    }

    @Test
    void theWrapperAndTheCoreNeverDisagreeOnAnyStateOfTheSampleTree() {
        String[][] ladders = {{}, {"iron_fist", "1"}, {"iron_fist", "3"}, {"iron_fist", "5", "scales", "3", "claw", "2"}, {"iron_fist", "5", "roar", "3", "claw", "3"}};
        for (boolean unlocked : new boolean[]{false, true}) {
            for (int sp : new int[]{0, 1, 2, 3, 8, 50}) {
                for (String[] ladder : ladders) {
                    Object[] pairs = new Object[ladder.length];
                    for (int i = 0; i < ladder.length; i++) pairs[i] = i % 2 == 0 ? ladder[i] : Integer.valueOf(ladder[i]);
                    TreeState s = SampleRoutes.state(FIRE, unlocked, sp, pairs);
                    for (SkillNode n : FIRE.nodes()) {
                        NodeState ns = NodeState.of(s, FIRE, n);
                        String at = n.id() + " sp=" + sp + " unlocked=" + unlocked + " ladder=" + String.join(",", ladder);
                        assertEquals(TreeRules.status(FIRE, n, s), ns.status(), at);
                        assertEquals(TreeRules.check(FIRE, n.id(), s), ns.verdict(), at);
                        assertEquals(TreeRules.canBuy(FIRE, n.id(), s), ns.canBuy(), at);
                        assertEquals(TreeRules.priceOfNext(n, s.level(FIRE.id(), n.id())), ns.price(), at);
                    }
                }
            }
        }
    }
}
