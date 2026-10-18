package com.newuniverse.nusmp.elite.races;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.races.rule.EvolutionRules;
import com.newuniverse.nusmp.elite.races.rule.RaceCtx;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleParser;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the evolution rules decide when a player becomes the next race, so each kind, the weighted bar, the all-or-nothing commit and the strict parser are checked without a server. */
class RaceEvolutionRuleTest {
    private static final double EPS = 1e-9;

    /** A settable stand-in for a player. */
    static final class Fake implements RaceCtx {
        double ep;
        double scale = 1.0;
        final Map<String, Integer> kills = new HashMap<>();
        final Map<String, Double> mastery = new HashMap<>();
        final Map<String, Integer> items = new HashMap<>();
        final Set<String> records = new HashSet<>();

        @Override public double ep() { return ep; }
        @Override public int kills(String key) { return kills.getOrDefault(key, 0); }
        @Override public double mastery(String skillId) { return mastery.getOrDefault(skillId, 0.0); }
        @Override public int itemCount(String itemId) { return items.getOrDefault(itemId, 0); }
        @Override public boolean record(String recordId) { return records.contains(recordId); }
        @Override public boolean consume(String itemId, int count) {
            int have = items.getOrDefault(itemId, 0);
            if (have < count) return false;
            items.put(itemId, have - count);
            return true;
        }
        @Override public double epScale() { return scale; }
    }

    @Test
    void epGateIsARatioAndScalesWithTheServer() {
        Fake f = new Fake();
        EvolutionRules.EpGate g = new EvolutionRules.EpGate(1000, 50);
        f.ep = 250;
        assertEquals(0.25, g.progress(f), EPS);
        assertFalse(g.met(f));
        f.ep = 5000;
        assertEquals(1.0, g.progress(f), EPS);
        assertTrue(g.met(f));
        f.scale = 10.0;   // the server made every gate ten times harder
        assertEquals(0.5, g.progress(f), EPS);
        f.scale = 0.0;    // a scale of zero removes the gate, it does not divide by zero
        assertTrue(g.met(f));
    }

    @Test
    void killsMasteryAndRecordRulesReadTheirOwnNumbers() {
        Fake f = new Fake();
        EvolutionRules.Kills k = new EvolutionRules.Kills("boss", 4, 10);
        f.kills.put("boss", 1);
        f.kills.put("any", 999);
        assertEquals(0.25, k.progress(f), EPS);          // the boss counter, not the total
        f.kills.put("boss", 9);
        assertTrue(k.met(f));
        EvolutionRules.Mastery m = new EvolutionRules.Mastery("tensura:strength", 0.5, 10);
        f.mastery.put("tensura:strength", 0.2);
        assertEquals(0.4, m.progress(f), EPS);
        f.mastery.put("tensura:strength", 1.0);
        assertTrue(m.met(f));
        EvolutionRules.RecordUnlocked r = new EvolutionRules.RecordUnlocked("saiyan.legend", 10);
        assertFalse(r.met(f));
        f.records.add("saiyan.legend");
        assertTrue(r.met(f));
    }

    @Test
    void anItemRuleConsumesOnlyWhenAskedAndOnlyOnCommit() {
        Fake f = new Fake();
        f.items.put("minecraft:diamond", 3);
        EvolutionRules.Item carry = new EvolutionRules.Item("minecraft:diamond", 2, false, 10);
        EvolutionRules.Item offer = new EvolutionRules.Item("minecraft:diamond", 2, true, 10);
        assertTrue(carry.met(f));
        assertEquals(3, f.itemCount("minecraft:diamond"));   // progress never takes anything
        carry.commit(f);
        assertEquals(3, f.itemCount("minecraft:diamond"));   // a carry rule never takes
        offer.commit(f);
        assertEquals(1, f.itemCount("minecraft:diamond"));
    }

    @Test
    void anyOfTakesTheBestChildAndCommitsOnlyTheOneThatWasMet() {
        Fake f = new Fake();
        f.items.put("a:x", 1);
        f.items.put("b:y", 1);
        EvolutionRules.AnyOf any = new EvolutionRules.AnyOf(List.of(
                new EvolutionRules.Item("a:x", 1, true, 10), new EvolutionRules.Item("b:y", 1, true, 10)), 20);
        assertTrue(any.met(f));
        any.commit(f);
        assertEquals(0, f.itemCount("a:x"));
        assertEquals(1, f.itemCount("b:y"));                  // only the first met child paid
        Fake none = new Fake();
        none.kills.put("any", 3);
        EvolutionRules.AnyOf mixed = new EvolutionRules.AnyOf(List.of(new EvolutionRules.Kills("any", 10, 5), new EvolutionRules.RecordUnlocked("r", 5)), 10);
        assertEquals(0.3, mixed.progress(none), EPS);
        assertFalse(mixed.met(none));
    }

    @Test
    void theBarIsAWeightedShareAndAnEmptySetIsComplete() {
        Fake f = new Fake();
        RaceRuleSet set = new RaceRuleSet(List.of(new EvolutionRules.EpGate(100, 75), new EvolutionRules.Kills("any", 10, 25)));
        f.ep = 100;                       // the first rule alone: 75 %
        assertEquals(75.0, set.percent(f), EPS);
        f.kills.put("any", 5);            // plus half of the other: 75 + 12.5
        assertEquals(87.5, set.percent(f), EPS);
        assertFalse(set.allMet(f));
        f.kills.put("any", 10);
        assertEquals(100.0, set.percent(f), EPS);
        assertTrue(set.allMet(f));
        assertEquals(100.0, RaceRuleSet.EMPTY.percent(f), EPS);
        assertTrue(RaceRuleSet.EMPTY.allMet(f));
        // weights are normalised: 3 and 1 become 75 and 25 whatever they add up to
        RaceRuleSet odd = new RaceRuleSet(List.of(new EvolutionRules.EpGate(1, 3), new EvolutionRules.Kills("any", 1, 1)));
        assertEquals(75.0, odd.sharePercent(odd.rules().get(0)), EPS);
    }

    @Test
    void nothingIsTakenUnlessEveryRuleIsMet() {
        Fake f = new Fake();
        f.items.put("nusmp:genesis_core", 1);
        RaceRuleSet set = new RaceRuleSet(List.of(new EvolutionRules.EpGate(1000, 50), new EvolutionRules.Item("nusmp:genesis_core", 1, true, 50)));
        f.ep = 10;
        RaceRuleSet.Outcome no = set.tryCommit(f);
        assertFalse(no.ok());
        assertEquals(1, no.missing().size());
        assertEquals(1, f.itemCount("nusmp:genesis_core"));   // the core is still there
        f.ep = 5000;
        RaceRuleSet.Outcome yes = set.tryCommit(f);
        assertTrue(yes.ok());
        assertEquals(0, f.itemCount("nusmp:genesis_core"));
    }

    // ---------------------------------------------------------------- parser
    private static JsonArray array(String json) { return JsonParser.parseString(json).getAsJsonArray(); }

    @Test
    void theParserBuildsEveryKindFromJson() {
        RaceRuleSet set = RaceRuleParser.parseSet(array("""
                [{"type":"ep","min":4000,"weight":50},
                 {"type":"kills","key":"boss","count":3,"weight":10},
                 {"type":"mastery","skill":"tensura:strength","fraction":0.25,"weight":10},
                 {"type":"item","item":"minecraft:diamond","count":2,"consume":true,"weight":10},
                 {"type":"record","id":"saiyan.legend","weight":10},
                 {"type":"any_of","weight":10,"rules":[{"type":"kills","count":5},{"type":"record","id":"x"}]}]
                """), "test");
        assertEquals(6, set.rules().size());
        assertTrue(set.rules().get(3) instanceof EvolutionRules.Item it && it.consume());
        assertTrue(set.rules().get(5) instanceof EvolutionRules.AnyOf a && a.rules().size() == 2);
    }

    private static void rejects(String json, String fragment) {
        try {
            RaceRuleParser.parseSet(array(json), "t");
            assertTrue(false, "accepted: " + json);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(fragment), "message '" + e.getMessage() + "' should mention '" + fragment + "'");
        }
    }

    @Test
    void theParserRefusesWhatCouldGiveAFreeOrBrokenEvolution() {
        rejects("[{\"type\":\"nope\"}]", "unknown rule type");
        rejects("[{\"type\":\"ep\"}]", "missing number");
        rejects("[{\"type\":\"ep\",\"min\":-5}]", "outside");
        rejects("[{\"type\":\"kills\",\"count\":0}]", "outside");
        rejects("[{\"type\":\"kills\",\"key\":\"dragons\",\"count\":1}]", "not one of");
        rejects("[{\"type\":\"mastery\",\"skill\":\"strength\",\"fraction\":0.5}]", "namespaced");
        rejects("[{\"type\":\"mastery\",\"skill\":\"tensura:x\",\"fraction\":1.5}]", "outside");
        rejects("[{\"type\":\"item\",\"item\":\"Minecraft:Stone\",\"count\":1}]", "namespaced");
        rejects("[{\"type\":\"ep\",\"min\":1,\"weight\":500}]", "outside");
        rejects("[{\"type\":\"any_of\",\"rules\":[{\"type\":\"record\",\"id\":\"x\"}]}]", "at least two");
        rejects("[{\"type\":\"any_of\",\"rules\":[{\"type\":\"any_of\",\"rules\":[{\"type\":\"record\",\"id\":\"a\"},{\"type\":\"record\",\"id\":\"b\"}]},{\"type\":\"record\",\"id\":\"c\"}]}]", "nested");
        rejects("[5]", "must be an object");
        JsonObject big = new JsonObject();
        JsonArray many = new JsonArray();
        for (int i = 0; i < RaceRuleParser.MAX_RULES + 1; i++) {
            JsonObject r = new JsonObject();
            r.addProperty("type", "record");
            r.addProperty("id", "r" + i);
            many.add(r);
        }
        big.add("x", many);
        try {
            RaceRuleParser.parseSet(many, "t");
            assertTrue(false, "too many rules accepted");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("more than"));
        }
    }
}
