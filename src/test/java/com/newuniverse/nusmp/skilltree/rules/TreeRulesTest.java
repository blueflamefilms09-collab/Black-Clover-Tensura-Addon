package com.newuniverse.nusmp.skilltree.rules;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillNode.Parent;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.ClassVerdict;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.NodeStatus;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.Purchase;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.Refund;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.Verdict;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: these are the rules a modified client would try to break (buy what it cannot afford, skip a parent, buy past the maximum, change class during the cooldown, cast a technique it does not
 * own), so each rule is proved here at its edges and each refusal is proved to change NOTHING. The route used is shaped like the owner's example (a root, two branches, a Secret Art that needs
 * both), with a flat price, a per-level price list and a repeated last price, so every pricing path is exercised. A state is compared before and after every refused call; that comparison is
 * the negative control that stops a rule from "refusing" while still having spent the points.
 */
class TreeRulesTest {
    private static final String R = "nusmp:fire_dragon_slayer";
    private static final String R2 = "nusmp:ice_make";

    private static SkillNode node(String id, List<Parent> parents, int cost, List<Integer> costs, int max) {
        return new SkillNode(id, id, "", parents, cost, costs, max, "", List.of(), Map.of(), 0, 0, "");
    }

    /** fist (flat 1, max 3) -> roar (needs fist 2; prices 2,3 then 3; max 3) and appetite (needs fist 1; flat 1; max 1) -> lotus (needs roar 1 and appetite 1; 4; max 1). */
    private static SkillRoute route() {
        SkillNode fist = node("iron_fist", List.of(), 1, List.of(), 3);
        SkillNode roar = node("roar", List.of(new Parent("iron_fist", 2)), 2, List.of(2, 3), 3);
        SkillNode appetite = node("appetite", List.of(new Parent("iron_fist", 1)), 1, List.of(), 1);
        SkillNode lotus = node("crimson_lotus", List.of(new Parent("roar", 1), new Parent("appetite", 1)), 4, List.of(), 1);
        return new SkillRoute(R, "fairy_tail", "Fire Dragon Slayer", "", "nusmp:fire_book", "Fang", "", 0xE8541C, "minecraft:blaze_powder", "fire_dragon_slayer", 3, 1, List.of(fist, roar, appetite, lotus));
    }

    private static SkillRoute ice() {
        return new SkillRoute(R2, "fairy_tail", "Ice Make", "", "nusmp:ice_book", "Frost", "", 0x66CCFF, "minecraft:ice", "ice_make", 3, 1, List.of(node("static", List.of(), 1, List.of(), 1)));
    }

    private static String snapshot(SimpleTreeState s) { return s.sp() + "/" + s.spSpent() + "/" + s.levels() + "/" + s.routes(); }

    // ------------------------------------------------------------------ check / buy
    @Test
    void theVerdictsComeInTheOwnersOrder() {
        SkillRoute r = route();
        assertEquals(Verdict.NO_ROUTE, TreeRules.check(r, "iron_fist", new SimpleTreeState().withSp(99)), "the route is not unlocked, whatever the SP");
        assertEquals(Verdict.NO_ROUTE, TreeRules.check(null, "iron_fist", new SimpleTreeState().unlock(R)));
        assertEquals(Verdict.UNKNOWN_NODE, TreeRules.check(r, "ghost", new SimpleTreeState().unlock(R).withSp(99)));
        assertEquals(Verdict.UNKNOWN_NODE, TreeRules.check(r, null, new SimpleTreeState().unlock(R).withSp(99)));
        assertEquals(Verdict.NO_SP, TreeRules.check(r, "iron_fist", new SimpleTreeState().unlock(R)), "0 SP, price 1");
        assertEquals(Verdict.OK, TreeRules.check(r, "iron_fist", new SimpleTreeState().unlock(R).withSp(1)), "exactly the price is enough");
        assertEquals(Verdict.PARENTS, TreeRules.check(r, "roar", new SimpleTreeState().unlock(R).withSp(99)), "parents are checked before the SP: roar needs fist 2");

        SimpleTreeState s = new SimpleTreeState().unlock(R).withSp(99);
        s.setLevel(R, "iron_fist", 1);
        assertEquals(Verdict.PARENTS, TreeRules.check(r, "roar", s), "one level short of the parent");
        s.setLevel(R, "iron_fist", 2);
        assertEquals(Verdict.OK, TreeRules.check(r, "roar", s), "exactly the parent's level");
        s.setLevel(R, "iron_fist", 3);
        assertEquals(Verdict.MAXED, TreeRules.check(r, "iron_fist", s), "maxed is reported before anything else about that node");
        assertEquals(Verdict.PARENTS, TreeRules.check(r, "crimson_lotus", s), "the Secret Art needs both branches");
        s.setLevel(R, "roar", 1);
        assertEquals(Verdict.PARENTS, TreeRules.check(r, "crimson_lotus", s), "one branch is not enough");
        s.setLevel(R, "appetite", 1);
        assertEquals(Verdict.OK, TreeRules.check(r, "crimson_lotus", s), "both branches");
    }

    @Test
    void canBuyIsTheYesNoFormOfTheVerdict() {
        SkillRoute r = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).withSp(1);
        assertTrue(TreeRules.canBuy(r, "iron_fist", s));
        assertFalse(TreeRules.canBuy(r, "roar", s), "parents");
        assertFalse(TreeRules.canBuy(r, "ghost", s), "unknown node");
        assertFalse(TreeRules.canBuy(r, "iron_fist", new SimpleTreeState().unlock(R)), "no SP");
        assertFalse(TreeRules.canBuy(r, "iron_fist", new SimpleTreeState().withSp(9)), "route locked");
        assertEquals(1, s.sp(), "asking changes nothing");
    }

    @Test
    void aRefusedPurchaseChangesNothing() {
        SkillRoute r = route();
        // fist maxed with 3 SP in hand: fist (MAXED), the Secret Art (PARENTS) and a ghost (UNKNOWN_NODE) are refused
        SimpleTreeState maxed = new SimpleTreeState().unlock(R).withSp(3);
        maxed.setLevel(R, "iron_fist", 3);
        String before = snapshot(maxed);
        for (String id : new String[]{"iron_fist", "crimson_lotus", "ghost"}) {
            Purchase p = TreeRules.buy(r, id, maxed);
            assertFalse(p.ok(), id);
            assertEquals(0, p.price(), id);
            assertEquals(before, snapshot(maxed), "a refusal of " + id + " must leave the state alone");
        }
        assertEquals(3, TreeRules.buy(r, "iron_fist", maxed).level(), "a refusal reports the level the node already has");
        // the parents are met but the SP is not enough
        SimpleTreeState poor = new SimpleTreeState().unlock(R);
        poor.setLevel(R, "iron_fist", 2);
        before = snapshot(poor);
        assertEquals(Verdict.NO_SP, TreeRules.buy(r, "roar", poor).verdict());
        assertEquals(before, snapshot(poor));
        // one point short of the price is still short
        SimpleTreeState oneShort = new SimpleTreeState().unlock(R).withSp(1);
        oneShort.setLevel(R, "iron_fist", 2);
        before = snapshot(oneShort);
        assertEquals(Verdict.NO_SP, TreeRules.buy(r, "roar", oneShort).verdict(), "roar costs 2");
        assertEquals(before, snapshot(oneShort));
        // the route is not unlocked
        SimpleTreeState none = new SimpleTreeState().withSp(9);
        before = snapshot(none);
        assertEquals(Verdict.NO_ROUTE, TreeRules.buy(r, "iron_fist", none).verdict());
        assertEquals(before, snapshot(none));
    }

    @Test
    void aPurchaseSpendsExactlyThePriceOfTheNextLevelAndRaisesTheLevel() {
        SkillRoute r = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).withSp(20);
        // flat price
        Purchase a = TreeRules.buy(r, "iron_fist", s);
        assertTrue(a.ok());
        assertEquals(1, a.level());
        assertEquals(1, a.price());
        assertEquals(19, s.sp());
        assertEquals(1, s.spSpent());
        TreeRules.buy(r, "iron_fist", s);
        assertEquals(2, s.level(R, "iron_fist"));
        // per-level prices: 2, 3, then the last one repeats
        assertEquals(2, TreeRules.buy(r, "roar", s).price());
        assertEquals(3, TreeRules.buy(r, "roar", s).price());
        assertEquals(3, TreeRules.buy(r, "roar", s).price(), "the third level repeats the last listed price");
        assertEquals(3, s.level(R, "roar"));
        assertEquals(Verdict.MAXED, TreeRules.buy(r, "roar", s).verdict());
        assertEquals(20 - 1 - 1 - 2 - 3 - 3, s.sp());
        assertEquals(s.spSpent() + s.sp(), 20, "SP is conserved: what is spent plus what is left is what there was");
    }

    @Test
    void buyingEveryNodeToItsMaximumCostsExactlyTheRoutesTotal() {
        SkillRoute r = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).withSp(r.totalCost());
        for (String id : new String[]{"iron_fist", "roar", "appetite", "crimson_lotus"}) {
            while (TreeRules.buy(r, id, s).ok()) { /* buy until the node refuses */ }
        }
        assertEquals(0, s.sp());
        assertEquals(r.totalCost(), s.spSpent());
        for (SkillNode n : r.nodes()) assertEquals(n.maxLevel(), s.level(R, n.id()), n.id());
    }

    @Test
    void priceOfNextAndTheNodeStatusTellTheScreenWhatItNeeds() {
        SkillRoute r = route();
        SkillNode roar = r.node("roar");
        assertEquals(2, TreeRules.priceOfNext(roar, 0));
        assertEquals(3, TreeRules.priceOfNext(roar, 1));
        assertEquals(3, TreeRules.priceOfNext(roar, 2));
        assertEquals(-1, TreeRules.priceOfNext(roar, 3));

        SimpleTreeState locked = new SimpleTreeState().withSp(99);
        assertEquals(NodeStatus.ROUTE_LOCKED, TreeRules.status(r, r.node("iron_fist"), locked));
        SimpleTreeState s = new SimpleTreeState().unlock(R);
        assertEquals(NodeStatus.TOO_EXPENSIVE, TreeRules.status(r, r.node("iron_fist"), s), "root, but no SP");
        s.setSp(1);
        assertEquals(NodeStatus.AVAILABLE, TreeRules.status(r, r.node("iron_fist"), s));
        assertEquals(NodeStatus.PARENTS_LOCKED, TreeRules.status(r, r.node("roar"), s));
        s.setLevel(R, "iron_fist", 1);
        assertEquals(NodeStatus.OWNED, TreeRules.status(r, r.node("iron_fist"), s));
        s.setLevel(R, "iron_fist", 3);
        assertEquals(NodeStatus.MAXED, TreeRules.status(r, r.node("iron_fist"), s));
        assertEquals(NodeStatus.AVAILABLE, TreeRules.status(r, r.node("appetite"), s));
        assertEquals(NodeStatus.PARENTS_LOCKED, TreeRules.status(r, r.node("crimson_lotus"), new SimpleTreeState().unlock(R).withSp(99)), "SP does not unlock a node whose parents are missing");
    }

    // ------------------------------------------------------------------ SP
    @Test
    void addSpIsClampedAtZeroAndAtTheCapButNeverLowersAnOverCapTotal() {
        assertEquals(15, TreeRules.addSp(10, 5, 9999));
        assertEquals(9999, TreeRules.addSp(9990, 50, 9999), "a gain stops at the cap");
        assertEquals(9999, TreeRules.addSp(9999, 1, 9999));
        assertEquals(12000, TreeRules.addSp(12000, 5, 9999), "a total already over a lowered cap is left alone by a gain");
        assertEquals(11990, TreeRules.addSp(12000, -10, 9999), "and a loss still takes points off it");
        assertEquals(0, TreeRules.addSp(3, -10, 9999), "never below zero");
        assertEquals(7, TreeRules.addSp(10, -3, 9999));
        assertEquals(10, TreeRules.addSp(10, 0, 9999));
        assertEquals(Integer.MAX_VALUE, TreeRules.addSp(Integer.MAX_VALUE - 1, 100, Integer.MAX_VALUE), "no overflow");
        assertEquals(0, TreeRules.addSp(0, Integer.MIN_VALUE, 9999), "no underflow");
    }

    // ------------------------------------------------------------------ respec
    @Test
    void aRespecClearsTheRouteAndGivesBackWhatWasSpentOnIt() {
        SkillRoute r = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).unlock(R2).withSp(30);
        for (String id : new String[]{"iron_fist", "iron_fist", "roar", "appetite"}) TreeRules.buy(r, id, s);
        TreeRules.buy(ice(), "static", s);
        int spentBefore = s.spSpent();
        assertEquals(1 + 1 + 2 + 1 + 1, spentBefore);
        int leftBefore = s.sp();
        Refund f = TreeRules.respec(r, R, s);
        assertEquals(1 + 1 + 2 + 1, f.sp(), "fist x2, roar x1, appetite x1");
        assertEquals(3, f.cleared());
        assertEquals(List.of("iron_fist", "roar", "appetite"), f.nodeIds());
        assertEquals(leftBefore + f.sp(), s.sp());
        assertEquals(spentBefore - f.sp(), s.spSpent());
        assertEquals(0, s.level(R, "iron_fist"));
        assertEquals(1, s.level(R2, "static"), "another route is untouched");
        assertTrue(s.hasRoute(R), "a respec keeps the route unlocked");
        assertEquals(30, s.sp() + s.spSpent(), "SP is conserved across a respec");
    }

    @Test
    void aRespecNeverGivesBackMoreThanWasReallySpent() {
        SkillRoute r = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).withSp(0);
        s.setLevel(R, "iron_fist", 3);                                    // set by an admin: nothing was paid
        s.setLevel(R, "roar", 3);
        s.setSpSpent(2);                                                  // and only 2 points were ever really spent
        Refund f = TreeRules.respec(r, R, s);
        assertEquals(2, f.sp(), "the refund is capped at what was spent, so a respec cannot mint SP");
        assertEquals(2, s.sp());
        assertEquals(0, s.spSpent());
        SimpleTreeState free = new SimpleTreeState().unlock(R);
        free.setLevel(R, "iron_fist", 3);
        assertEquals(0, TreeRules.respec(r, R, free).sp(), "nothing spent, nothing back");
        assertEquals(0, free.level(R, "iron_fist"));
    }

    @Test
    void aRespecOfARouteTheRegistryNoLongerKnowsStillClearsItsNodes() {
        SimpleTreeState s = new SimpleTreeState().unlock("nusmp:gone").withSp(4);
        s.setLevel("nusmp:gone", "a", 2);
        s.setLevel("nusmp:gone", "b", 1);
        s.setSpSpent(3);
        Refund f = TreeRules.respec(null, "nusmp:gone", s);
        assertEquals(2, f.cleared());
        assertEquals(0, f.sp(), "their price is unknown, so nothing is guessed");
        assertEquals(4, s.sp());
        assertTrue(s.levels().isEmpty());
    }

    @Test
    void aRespecOfARouteWithNothingBoughtIsANoOp() {
        SimpleTreeState s = new SimpleTreeState().unlock(R).withSp(5);
        String before = snapshot(s);
        Refund f = TreeRules.respec(route(), R, s);
        assertEquals(0, f.cleared());
        assertEquals(0, f.sp());
        assertEquals(before, snapshot(s));
    }

    // ------------------------------------------------------------------ class
    @Test
    void theFirstClassIsFreeAndTheCooldownAppliesToChanges() {
        SkillRoute fire = route(), ice = ice();
        SimpleTreeState s = new SimpleTreeState().unlock(R).unlock(R2);
        assertEquals(ClassVerdict.OK, TreeRules.classSwitch(s, fire, 1000, 600, false), "no class yet: free");
        s.withClass("fire_dragon_slayer", 1000);
        assertEquals(ClassVerdict.SAME, TreeRules.classSwitch(s, fire, 1001, 600, false));
        assertEquals(ClassVerdict.COOLDOWN, TreeRules.classSwitch(s, ice, 1001, 600, false), "just changed");
        assertEquals(ClassVerdict.COOLDOWN, TreeRules.classSwitch(s, ice, 1599, 600, false), "one tick before it ends");
        assertEquals(ClassVerdict.OK, TreeRules.classSwitch(s, ice, 1600, 600, false), "exactly when it ends");
        assertEquals(ClassVerdict.OK, TreeRules.classSwitch(s, ice, 1001, 600, true), "a bypass (creative, a command) ignores it");
        assertEquals(ClassVerdict.OK, TreeRules.classSwitch(s, ice, 1001, 0, false), "a cooldown of 0 is no cooldown");
    }

    @Test
    void aClassOfARouteThatIsNotOwnedIsRefusedWhateverElseHolds() {
        SimpleTreeState s = new SimpleTreeState().unlock(R).withClass("fire_dragon_slayer", 0);
        assertEquals(ClassVerdict.NOT_UNLOCKED, TreeRules.classSwitch(s, ice(), 99999, 0, true), "even a bypass cannot pick a class the player does not have");
        assertEquals(ClassVerdict.NOT_UNLOCKED, TreeRules.classSwitch(s, null, 99999, 0, true));
    }

    @Test
    void theCooldownRemainingHandlesTheOddCases() {
        assertEquals(0, TreeRules.cooldownRemaining(0, 5000, 600), "never changed");
        assertEquals(600, TreeRules.cooldownRemaining(1000, 1000, 600));
        assertEquals(1, TreeRules.cooldownRemaining(1000, 1599, 600));
        assertEquals(0, TreeRules.cooldownRemaining(1000, 1600, 600));
        assertEquals(0, TreeRules.cooldownRemaining(1000, 9999, 600));
        assertEquals(0, TreeRules.cooldownRemaining(1000, 500, 600), "the clock was set back: the stamp is not trusted");
        assertEquals(0, TreeRules.cooldownRemaining(1000, 1001, 0));
    }

    @Test
    void healClassOnlyActsWhenThereIsNoClassAndARouteTheRegistryKnows() {
        Map<String, SkillRoute> reg = new LinkedHashMap<>();
        reg.put(R, route());
        reg.put(R2, ice());
        SimpleTreeState s = new SimpleTreeState().unlock("nusmp:unknown").unlock(R2).unlock(R);
        assertEquals("ice_make", TreeRules.healClass(s, reg::get), "the first owned route the registry knows, in unlock order");
        s.withClass("anything", 0);
        assertNull(TreeRules.healClass(s, reg::get), "a class that is set is left alone");
        assertNull(TreeRules.healClass(new SimpleTreeState().withSp(9), reg::get), "no routes, nothing to do");
        assertNull(TreeRules.healClass(new SimpleTreeState().unlock("nusmp:unknown"), reg::get), "no known route, nothing to do");
    }

    // ------------------------------------------------------------------ the cast gate
    @Test
    void theCastGateNeedsTheRouteTheNodeAndTheClass() {
        SkillRoute r = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).withClass("fire_dragon_slayer", 0);
        s.setLevel(R, "iron_fist", 1);
        assertTrue(TreeRules.canCast(r, s, R, "iron_fist", true, false), "owned route, bought node, active class");
        assertFalse(TreeRules.canCast(r, s, R, "roar", true, false), "node not bought");
        assertFalse(TreeRules.canCast(r, new SimpleTreeState().withClass("fire_dragon_slayer", 0), R, "iron_fist", true, false), "route not owned, even with a stale node level");
        SimpleTreeState stale = new SimpleTreeState().withClass("fire_dragon_slayer", 0);
        stale.setLevel(R, "iron_fist", 3);
        assertFalse(TreeRules.canCast(r, stale, R, "iron_fist", false, true), "a node level without the route never casts, creative or not");
        assertFalse(TreeRules.canCast(r, s, R, null, true, false));
        assertFalse(TreeRules.canCast(r, s, null, "iron_fist", true, false));
        assertFalse(TreeRules.canCast(r, null, R, "iron_fist", true, false));
    }

    @Test
    void theActiveClassGateCanBeSwitchedOffAndCreativePlayersPassIt() {
        SkillRoute fire = route();
        SimpleTreeState s = new SimpleTreeState().unlock(R).unlock(R2).withClass("ice_make", 0);          // owns fire, but the active class is ice
        s.setLevel(R, "iron_fist", 1);
        assertFalse(TreeRules.canCast(fire, s, R, "iron_fist", true, false), "the owned technique of an inactive class cannot be cast");
        assertTrue(TreeRules.canCast(fire, s, R, "iron_fist", false, false), "with requireActiveClass off the class does not matter");
        assertTrue(TreeRules.canCast(fire, s, R, "iron_fist", true, true), "creative players pass the class check");
        assertFalse(TreeRules.canCast(null, s, R, "iron_fist", true, false), "a route the registry does not know cannot have its class checked, so it is refused");
        assertTrue(TreeRules.canCast(null, s, R, "iron_fist", false, false), "but with the class check off the attachment alone decides");
        assertFalse(TreeRules.canCast(fire, new SimpleTreeState().unlock(R).withClass("", 0), R, "iron_fist", true, false), "no class chosen: the class check cannot pass");
    }
}
