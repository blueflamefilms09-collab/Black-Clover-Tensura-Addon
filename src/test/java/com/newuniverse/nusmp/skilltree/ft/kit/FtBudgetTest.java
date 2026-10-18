package com.newuniverse.nusmp.skilltree.ft.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillNode.Parent;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the balance law of the Fairy Tail techniques (design.md section 6) is only a law if a number that breaks it is REFUSED, so every rule of {@link FtBudget} is pinned here from both sides: a legal
 * node passes, and each limit (damage band, cooldown floor, cost band, target cap, heal cap, construct life, the tell for a heavy hit, the Secret Art sequence cap, a construct that outlasts its cooldown, a turret that
 * cannot fire all its shots, a table of the wrong length) is broken one at a time and must be named. The route-level rules (node count, one root, one Secret Art that needs two branches, prices, canvas,
 * parents in order) are pinned the same way on a small legal route.
 */
class FtBudgetTest {
    private static Map<String, List<Double>> legal() {
        Map<String, List<Double>> v = new LinkedHashMap<>();
        v.put("damage", List.of(5.0, 8.0));
        v.put("cost", List.of(0.02, 0.04));
        v.put("cooldown", List.of(60.0, 100.0));
        v.put("targets", List.of(4.0, 12.0));
        return v;
    }

    private static boolean flags(Map<String, List<Double>> v, FtBudget.Kind kind, String word) {
        for (String s : FtBudget.check("n", kind, 2, v)) if (s.contains(word)) return true;
        return false;
    }

    private static Map<String, List<Double>> with(String key, Double... t) {
        Map<String, List<Double>> v = legal();
        v.put(key, List.of(t));
        return v;
    }

    @Test
    void aLegalOrdinaryNodePasses() {
        assertEquals(List.of(), FtBudget.check("n", FtBudget.Kind.ORDINARY, 2, legal()));
    }

    @Test
    void damageBands() {
        assertTrue(flags(with("damage", 5.0, 14.5), FtBudget.Kind.ORDINARY, "damage"), "14.5 raw is above the ordinary band");
        assertTrue(flags(with("damage", 3.5, 8.0), FtBudget.Kind.ORDINARY, "damage"), "3.5 raw is below the ordinary band");
        assertEquals(List.of(), FtBudget.check("n", FtBudget.Kind.ORDINARY, 2, with("damage", 0.0, 8.0)), "zero means no damage at this level and is allowed");
        Map<String, List<Double>> art = with("damage", 20.0, 31.0);
        art.put("cost", List.of(0.06, 0.08));
        art.put("cooldown", List.of(900.0, 1000.0));
        art.put("windup", List.of(10.0, 10.0));
        assertTrue(flags(art, FtBudget.Kind.SECRET_ART, "damage"), "31 raw is above the Secret Art cap");
    }

    @Test
    void cooldownFloors() {
        assertTrue(flags(with("cooldown", 59.0, 100.0), FtBudget.Kind.ORDINARY, "cooldown"), "under 3 s");
        Map<String, List<Double>> art = with("cooldown", 899.0, 900.0);
        art.put("cost", List.of(0.06, 0.08));
        art.put("damage", List.of(10.0, 12.0));
        art.put("windup", List.of(10.0, 10.0));
        assertTrue(flags(art, FtBudget.Kind.SECRET_ART, "cooldown"), "a Secret Art under 45 s");
    }

    @Test
    void costBands() {
        assertTrue(flags(with("cost", 0.02, 0.05), FtBudget.Kind.ORDINARY, "cost"), "5 percent is above the ordinary band");
        assertTrue(flags(with("cost", 0.005, 0.02), FtBudget.Kind.ORDINARY, "cost"), "half a percent is below the band");
        Map<String, List<Double>> art = with("cost", 0.06, 0.09);
        art.put("cooldown", List.of(900.0, 1000.0));
        art.put("damage", List.of(10.0, 12.0));
        art.put("windup", List.of(10.0, 10.0));
        assertTrue(flags(art, FtBudget.Kind.SECRET_ART, "cost"), "9 percent is above the Secret Art band");
    }

    @Test
    void targetHealAndLifeCaps() {
        assertTrue(flags(with("targets", 4.0, 13.0), FtBudget.Kind.ORDINARY, "targets"));
        assertTrue(flags(with("heal", 0.1, 0.26), FtBudget.Kind.ORDINARY, "heal"));
        assertTrue(flags(with("life", 100.0, 201.0), FtBudget.Kind.ORDINARY, "life"));
        assertEquals(List.of(), FtBudget.check("n", FtBudget.Kind.ORDINARY, 2, with("heal", 0.1, 0.25)), "exactly 25 percent is allowed");
    }

    @Test
    void aHeavyHitNeedsATellOfEightTicks() {
        Map<String, List<Double>> heavy = with("damage", 8.0, 9.0);
        assertTrue(flags(heavy, FtBudget.Kind.ORDINARY, "windup"), "9 raw with no windup");
        heavy.put("windup", List.of(7.0, 12.0));
        assertTrue(flags(heavy, FtBudget.Kind.ORDINARY, "windup"), "a windup of 7 ticks is too short to see");
        heavy.put("windup", List.of(8.0, 12.0));
        assertEquals(List.of(), FtBudget.check("n", FtBudget.Kind.ORDINARY, 2, heavy));
    }

    @Test
    void aSecretArtSequenceIsHeldToTheCapPerTarget() {
        Map<String, List<Double>> art = new LinkedHashMap<>();
        art.put("pulses", List.of(3.0, 3.0));
        art.put("pulse_damage", List.of(4.0, 5.0));
        art.put("burst_damage", List.of(12.0, 16.0));
        art.put("windup", List.of(10.0, 10.0));
        art.put("cost", List.of(0.06, 0.08));
        art.put("cooldown", List.of(900.0, 1000.0));
        assertTrue(flags(art, FtBudget.Kind.SECRET_ART, "sequence"), "3 x 5 + 16 = 31 raw over the sequence");
        art.put("burst_damage", List.of(12.0, 15.0));
        assertEquals(List.of(), FtBudget.check("n", FtBudget.Kind.SECRET_ART, 2, art), "3 x 5 + 15 = 30 is exactly the cap");
    }

    @Test
    void aConstructOrABuffCannotOutlastItsCooldown() {
        Map<String, List<Double>> v = with("life", 100.0, 120.0);
        assertTrue(flags(v, FtBudget.Kind.ORDINARY, "outlasts"), "life 120 over a cooldown of 100");
        Map<String, List<Double>> b = with("seconds", 3.0, 6.0);
        assertTrue(flags(b, FtBudget.Kind.ORDINARY, "outlasts"), "6 s is 120 ticks, over a cooldown of 100");
    }

    @Test
    void aTurretMustHaveTimeToFireAllItsShots() {
        Map<String, List<Double>> v = with("life", 50.0, 90.0);
        v.put("cooldown", List.of(200.0, 200.0));
        v.put("shots", List.of(3.0, 5.0));
        v.put("interval", List.of(18.0, 18.0));
        assertTrue(flags(v, FtBudget.Kind.ORDINARY, "do not fit"), "3 shots 18 ticks apart need 54 ticks, the life is 50");
    }

    @Test
    void aTableOfTheWrongLengthOrWithABadNumberIsNamed() {
        assertTrue(flags(with("range", 5.0), FtBudget.Kind.ORDINARY, "entries"), "one entry for two levels");
        assertTrue(flags(with("range", 5.0, Double.NaN), FtBudget.Kind.ORDINARY, "holds"));
        assertTrue(flags(with("range", 5.0, -1.0), FtBudget.Kind.ORDINARY, "holds"));
    }

    @Test
    void anActiveNodeNeedsACostAndACooldown() {
        Map<String, List<Double>> v = new LinkedHashMap<>();
        v.put("damage", List.of(5.0, 6.0));
        assertTrue(flags(v, FtBudget.Kind.ORDINARY, "no cooldown"));
        assertTrue(flags(v, FtBudget.Kind.ORDINARY, "no cost"));
        assertFalse(flags(v, FtBudget.Kind.PASSIVE, "no cooldown"), "a passive has neither");
    }

    // ---------------------------------------------------------------- whole routes
    private static SkillNode node(String id, List<Parent> parents, int cost, List<Integer> costs, int max, String skill, List<String> passives, int x, int y) {
        Map<String, List<Double>> v = new LinkedHashMap<>();
        if (!skill.isEmpty()) {
            List<Double> five = new ArrayList<>(), c = new ArrayList<>(), cd = new ArrayList<>();
            for (int i = 0; i < max; i++) { five.add(5.0); c.add(0.02); cd.add(cost >= 4 ? 1000.0 : 100.0); }
            v.put("damage", five);
            v.put("cost", c);
            v.put("cooldown", cd);
        }
        return new SkillNode(id, id, "d", parents, cost, costs, max, skill, passives, v, x, y, "");
    }

    private static List<SkillNode> legalNodes() {
        List<SkillNode> n = new ArrayList<>();
        n.add(node("a", List.of(), 1, List.of(), 2, "nusmp:ft_a", List.of(), 0, 4));
        n.add(node("b", List.of(new Parent("a", 1)), 2, List.of(), 2, "nusmp:ft_b", List.of(), 2, 2));
        n.add(node("c", List.of(new Parent("a", 1)), 2, List.of(), 2, "nusmp:ft_c", List.of(), 2, 6));
        n.add(node("d", List.of(new Parent("b", 1)), 1, List.of(), 1, "", List.of("p_d"), 4, 1));
        n.add(node("e", List.of(new Parent("b", 1)), 2, List.of(), 2, "nusmp:ft_e", List.of(), 4, 3));
        n.add(node("f", List.of(new Parent("c", 1)), 2, List.of(), 2, "nusmp:ft_f", List.of(), 4, 5));
        n.add(node("g", List.of(new Parent("c", 1)), 2, List.of(), 2, "nusmp:ft_g", List.of(), 4, 7));
        n.add(node("h", List.of(new Parent("b", 2), new Parent("c", 2)), 4, List.of(4, 5), 2, "nusmp:ft_h", List.of(), 7, 4));
        return n;
    }

    private static SkillRoute route(List<SkillNode> nodes) {
        return new SkillRoute("nusmp:t", "fairy_tail", "T", "", "nusmp:t_book", "B", "", 0xFFFFFF, "minecraft:stick", "t", 3, 1, nodes);
    }

    private static boolean routeFlags(List<SkillNode> nodes, String word) {
        for (String s : FtBudget.problems(route(nodes))) if (s.contains(word)) return true;
        return false;
    }

    @Test
    void aLegalRoutePasses() {
        assertEquals(List.of(), FtBudget.problems(route(legalNodes())));
    }

    @Test
    void routeShapeRules() {
        List<SkillNode> few = new ArrayList<>(legalNodes().subList(0, 6));
        assertTrue(routeFlags(few, "nodes, not"), "six nodes is below the 7 to 10 band");

        List<SkillNode> secondRoot = new ArrayList<>(legalNodes());
        secondRoot.set(3, node("d", List.of(), 1, List.of(), 1, "", List.of("p_d"), 4, 1));
        assertTrue(routeFlags(secondRoot, "root nodes"), "two roots");

        List<SkillNode> oneParent = new ArrayList<>(legalNodes());
        oneParent.set(7, node("h", List.of(new Parent("b", 2)), 4, List.of(4, 5), 2, "nusmp:ft_h", List.of(), 7, 4));
        assertTrue(routeFlags(oneParent, "two branches"), "a Secret Art with one parent");

        List<SkillNode> twoArts = new ArrayList<>(legalNodes());
        twoArts.set(4, node("e", List.of(new Parent("b", 1), new Parent("c", 1)), 4, List.of(4, 4), 2, "nusmp:ft_e", List.of(), 4, 3));
        assertTrue(routeFlags(twoArts, "Secret Arts"), "two Secret Arts");

        List<SkillNode> late = new ArrayList<>(legalNodes());
        SkillNode moved = late.remove(1);
        late.add(moved);
        assertTrue(routeFlags(late, "before its parent") || routeFlags(late, "unknown parent"), "a node before its parent");
    }

    @Test
    void routePriceCanvasAndNamingRules() {
        List<SkillNode> dear = new ArrayList<>(legalNodes());
        dear.set(1, node("b", List.of(new Parent("a", 1)), 4, List.of(), 2, "", List.of("p_b"), 2, 2));
        assertTrue(routeFlags(dear, "costs 4"), "a passive at 4 SP is a Secret Art price");

        List<SkillNode> cheap = new ArrayList<>(legalNodes());
        cheap.set(7, node("h", List.of(new Parent("b", 2), new Parent("c", 2)), 2, List.of(2, 2), 2, "nusmp:ft_h", List.of(), 7, 4));
        assertTrue(routeFlags(cheap, "Secret Arts"), "a Secret Art at 2 SP is not one, so the route has none");

        List<SkillNode> offCanvas = new ArrayList<>(legalNodes());
        offCanvas.set(2, node("c", List.of(new Parent("a", 1)), 2, List.of(), 2, "nusmp:ft_c", List.of(), 13, 6));
        assertTrue(routeFlags(offCanvas, "off the canvas"));

        List<SkillNode> stacked = new ArrayList<>(legalNodes());
        stacked.set(2, node("c", List.of(new Parent("a", 1)), 2, List.of(), 2, "nusmp:ft_c", List.of(), 2, 2));
        assertTrue(routeFlags(stacked, "shares the cell"));

        List<SkillNode> badSkill = new ArrayList<>(legalNodes());
        badSkill.set(1, node("b", List.of(new Parent("a", 1)), 2, List.of(), 2, "nusmp:other_b", List.of(), 2, 2));
        assertTrue(routeFlags(badSkill, "nusmp:ft_"));

        List<SkillNode> empty = new ArrayList<>(legalNodes());
        empty.set(3, node("d", List.of(new Parent("b", 1)), 1, List.of(), 1, "", List.of(), 4, 1));
        assertTrue(routeFlags(empty, "no skill and no passive"));
    }
}
