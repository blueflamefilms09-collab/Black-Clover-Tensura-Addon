package com.newuniverse.nusmp.skilltree.ft.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.ft.dragon.DragonBudget.Kind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the budget is the only thing standing between a typo in a route file and an over-strong or never-ending technique, and nobody can play the game here, so the budget
 * itself is tested: a model node passes, and each rule of design.md section 6 (raw damage 4..14 and at most 30 for a Secret Art, a cooldown of 60 ticks and 900 for a Secret
 * Art, a cost of 1..4 percent and at most 8 for a Secret Art, at most 12 targets) plus this builder's own (a higher level is never worse, one entry per level, the passive
 * caps) rejects exactly what it should and names the table that broke it.
 */
class DragonBudgetTest {
    private static Map<String, List<Double>> good() {
        Map<String, List<Double>> v = new LinkedHashMap<>();
        v.put("damage", List.of(6.0, 8.0, 10.0));
        v.put("targets", List.of(1.0, 2.0, 3.0));
        v.put("cost_pct", List.of(2.0, 2.0, 3.0));
        v.put("cooldown", List.of(120.0, 100.0, 80.0));
        return v;
    }

    private static String complaints(Kind kind, Map<String, List<Double>> v) { return String.join(" | ", DragonBudget.check("n", kind, 3, v)); }

    @Test
    void aModelNodePassesAndSoDoesAModelSecretArt() {
        assertEquals(List.of(), DragonBudget.check("n", Kind.ACTIVE, 3, good()));
        Map<String, List<Double>> s = good();
        s.put("final_damage", List.of(18.0, 24.0, 30.0));
        s.put("cost_pct", List.of(6.0, 7.0, 8.0));
        s.put("cooldown", List.of(1200.0, 1000.0, 900.0));
        assertEquals(List.of(), DragonBudget.check("n", Kind.SECRET_ART, 3, s));
    }

    @Test
    void rawDamageIsHeldBetweenTheFloorAndTheCapOfItsKind() {
        Map<String, List<Double>> v = good();
        v.put("damage", List.of(6.0, 8.0, 14.5));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.damage[3]"), complaints(Kind.ACTIVE, v));
        v.put("damage", List.of(3.0, 8.0, 10.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.damage[1]"), "under the floor of 4");
        v.put("damage", List.of(14.0, 14.0, 14.0));
        assertEquals("", complaints(Kind.ACTIVE, v), "14 is the last allowed number");
        v.put("damage", List.of(20.0, 25.0, 30.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.damage[1]"), "20 is too much for an ordinary technique");
        v.put("cost_pct", List.of(6.0, 7.0, 8.0));
        v.put("cooldown", List.of(1200.0, 1000.0, 900.0));
        assertEquals("", complaints(Kind.SECRET_ART, v), "...and fine for a Secret Art");
        v.put("damage", List.of(20.0, 25.0, 31.0));
        assertTrue(complaints(Kind.SECRET_ART, v).contains("n.damage[3]"), "31 is over even the Secret Art cap");
    }

    @Test
    void cooldownCostAndTargetsHaveTheirFloorsAndCaps() {
        Map<String, List<Double>> v = good();
        v.put("cooldown", List.of(120.0, 100.0, 59.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.cooldown[3]"), "59 ticks is under 3 seconds");
        v = good();
        v.put("cost_pct", List.of(2.0, 2.0, 4.5));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.cost_pct[3]"), "4.5 percent is over 4");
        v.put("cost_pct", List.of(0.5, 2.0, 3.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.cost_pct[1]"), "under 1 percent");
        v = good();
        v.put("targets", List.of(1.0, 2.0, 13.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.targets[3]"), "13 targets");
        v = good();
        v.put("cooldown", List.of(899.0, 899.0, 899.0));
        v.put("cost_pct", List.of(8.0, 8.0, 8.0));
        v.put("final_damage", List.of(20.0, 20.0, 20.0));
        assertTrue(complaints(Kind.SECRET_ART, v).contains("n.cooldown[1]"), "899 ticks is under the 45 seconds of a Secret Art");
        v.put("cooldown", List.of(900.0, 900.0, 900.0));
        v.put("cost_pct", List.of(8.5, 8.5, 8.5));
        assertTrue(complaints(Kind.SECRET_ART, v).contains("n.cost_pct[1]"), "8.5 percent is over the Secret Art cap");
    }

    @Test
    void aHigherLevelIsNeverWorseAndEveryLevelHasANumber() {
        Map<String, List<Double>> v = good();
        v.put("damage", List.of(8.0, 7.0, 10.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.damage: level 2"), "damage that falls with the level");
        v = good();
        v.put("cooldown", List.of(100.0, 120.0, 80.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.cooldown: level 2"), "a cooldown that grows with the level");
        v = good();
        v.put("cost_pct", List.of(3.0, 2.0, 4.0));
        assertEquals("", complaints(Kind.ACTIVE, v), "the price may move either way");
        v = good();
        v.put("damage", List.of(6.0, 8.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("2 entries for max level 3"), "a missing level");
        v = good();
        v.put("range", List.of(5.0, Double.NaN, 7.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("not a finite"), "NaN");
        v.put("range", List.of(5.0, -1.0, 7.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("not a finite"), "negative");
    }

    @Test
    void anActiveNodeNeedsItsCooldownCostAndDamage() {
        Map<String, List<Double>> v = good();
        v.remove("cooldown");
        assertTrue(complaints(Kind.ACTIVE, v).contains("needs a cooldown"));
        v = good();
        v.remove("cost_pct");
        assertTrue(complaints(Kind.ACTIVE, v).contains("needs a cost_pct"));
        v = good();
        v.remove("damage");
        assertTrue(complaints(Kind.ACTIVE, v).contains("needs a damage"));
        assertTrue(complaints(Kind.ACTIVE, Map.of()).contains("no values"));
    }

    @Test
    void passivesHaveTheirOwnSmallCaps() {
        Map<String, List<Double>> p = new LinkedHashMap<>();
        p.put("heal_pct", List.of(2.0, 3.0, 4.0));
        p.put("recharge_pct", List.of(1.0, 1.5, 2.0));
        p.put("recoil", List.of(2.0, 3.0, 4.0));
        p.put("evade_pct", List.of(8.0, 12.0, 16.0));
        p.put("icd", List.of(40.0, 30.0, 20.0));
        assertEquals("", complaints(Kind.PASSIVE, p), "a passive needs no cooldown or cost");
        p.put("heal_pct", List.of(2.0, 3.0, 5.5));
        assertTrue(complaints(Kind.PASSIVE, p).contains("n.heal_pct[3]"), "5.5 percent a bite is over the passive cap");
        p.put("heal_pct", List.of(2.0, 3.0, 4.0));
        p.put("recoil", List.of(2.0, 3.0, 7.0));
        assertTrue(complaints(Kind.PASSIVE, p).contains("n.recoil[3]"), "recoil over 6");
        p.put("recoil", List.of(2.0, 3.0, 4.0));
        p.put("evade_pct", List.of(8.0, 12.0, 26.0));
        assertTrue(complaints(Kind.PASSIVE, p).contains("n.evade_pct[3]"), "evade over 25 percent");
        p.put("evade_pct", List.of(8.0, 12.0, 16.0));
        p.put("icd", List.of(40.0, 30.0, 10.0));
        assertTrue(complaints(Kind.PASSIVE, p).contains("n.icd[3]"), "triggering more than once a second");
        assertTrue(DragonBudget.check("n", Kind.PASSIVE, 3, Map.of("hits", List.of(1.0, 9.0, 9.0))).toString().contains("n.hits[2]"), "more than 8 follow-up hits");
    }

    @Test
    void repeatedHitsOnTheSameFoesAreSpacedByTheHurtFrameMargin() {
        Map<String, List<Double>> v = good();
        v.put("gap", List.of(13.0, 12.0, 11.0));
        assertEquals("", complaints(Kind.ACTIVE, v), "11 ticks is the shortest allowed spacing");
        v.put("gap", List.of(13.0, 12.0, 10.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.gap[3]"), "10 ticks would be swallowed by the vanilla hurt-frames");
        v.put("gap", List.of(11.0, 12.0, 13.0));
        assertTrue(complaints(Kind.ACTIVE, v).contains("n.gap: level 2"), "a gap that grows with the level is a weaker technique");
    }

    @Test
    void theTellRuleStartsJustAboveEight() {
        assertFalse(DragonBudget.needsTell(8.0));
        assertTrue(DragonBudget.needsTell(8.1));
        assertTrue(DragonBudget.needsTell(30.0));
        assertTrue(DragonBudget.isRawKey("hit_damage"));
        assertFalse(DragonBudget.isRawKey("range"));
    }
}
