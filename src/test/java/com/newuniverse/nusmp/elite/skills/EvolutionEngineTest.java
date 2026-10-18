package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.evolution.EvolutionContext;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionEngine;
import com.newuniverse.nusmp.elite.skills.evolution.EvolutionTable;
import com.newuniverse.nusmp.elite.skills.evolution.Params;
import com.newuniverse.nusmp.elite.skills.evolution.Triggers;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the evolution engine is data driven, so what must be proven is not the twelve one-line triggers one by one (each is covered below) but the contract around them: every trigger is
 * registered, a path needs ALL of its requirements, a broken data file skips the broken path and never grants a free evolution, offerings are paid only when everything else passed, and the
 * evolutions.json that ships parses cleanly against the real trigger table. A fake context stands in for the player.
 */
class EvolutionEngineTest {
    /** A player made of plain fields. */
    static final class Fake implements EvolutionContext {
        final UUID id = UUID.randomUUID();
        final Set<String> owned = new HashSet<>(), mastered = new HashSet<>(), records = new HashSet<>(), titles = new HashSet<>(), awakened = new HashSet<>();
        final Map<String, Integer> items = new HashMap<>(), killsWith = new HashMap<>();
        double ep;
        int kills, forge, quality, held, resets, consumeCalls;
        String active = "";

        @Override public UUID id() { return id; }
        @Override public boolean owns(String s) { return owned.contains(s) || mastered.contains(s); }
        @Override public boolean ownsMastered(String s) { return mastered.contains(s); }
        @Override public double ep() { return ep; }
        @Override public int killsWith(String s, String f) { return killsWith.getOrDefault(s + "/" + f, 0); }
        @Override public int killsTotal(String f) { return kills; }
        @Override public int itemCount(String i) { return items.getOrDefault(i, 0); }
        @Override public boolean consumeItems(String i, int n) {
            consumeCalls++;
            if (itemCount(i) < n) return false;
            items.put(i, itemCount(i) - n);
            return true;
        }
        @Override public boolean awakened(String k) { return awakened.contains(k) || ("any".equals(k) && !awakened.isEmpty()); }
        @Override public int forgeCrafted() { return forge; }
        @Override public int forgeQuality() { return quality; }
        @Override public boolean record(String r) { return records.contains(r); }
        @Override public boolean hasTitle(String t) { return titles.contains(t); }
        @Override public String activeTitle() { return active; }
        @Override public int heldTitles() { return held; }
        @Override public int resets() { return resets; }
    }

    private static EvolutionTable.Path path(String json) {
        EvolutionTable t = new EvolutionTable().add(json, "test");
        assertTrue(t.errors().isEmpty(), "unexpected errors " + t.errors());
        assertEquals(1, t.size());
        return t.paths().get(0);
    }

    private static String one(String trigger, String params) {
        return "{\"a\":{\"b\":[{\"trigger\":\"" + trigger + "\"" + (params == null ? "" : ",\"params\":" + params) + "}]}}";
    }

    @Test
    void allTwelveTriggersAreRegisteredUnderTheirIds() {
        assertEquals(12, Triggers.count());
        for (String id : List.of("ability_mastered", "active_title_evolution", "awakening_evolution", "ep_gate", "forge_crafted_count", "forge_quality", "has_title",
                "held_title_count", "item_consume", "kill_evolution", "record_unlocked", "reset_count")) {
            assertNotNull(Triggers.get(id), id);
            assertEquals(id, Triggers.get(id).id());
        }
        assertNull(Triggers.get("nope"));
        assertNull(Triggers.get(null));
    }

    @Test
    void eachTriggerPassesAndFailsOnItsOwnCondition() {
        Fake f = new Fake();
        // ability_mastered: default subject is the evolving skill ("a")
        EvolutionTable.Path p = path(one("ability_mastered", null));
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.owned.add("a");
        assertFalse(EvolutionEngine.check(p, f).ok(), "owned but not mastered");
        f.mastered.add("a");
        assertTrue(EvolutionEngine.check(p, f).ok());
        assertTrue(EvolutionEngine.check(path(one("ability_mastered", "{\"skill\":\"z\"}")), f).unmet().size() == 1, "another subject");

        p = path(one("ep_gate", "{\"min\":1000}"));
        f.ep = 999;
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.ep = 1000;
        assertTrue(EvolutionEngine.check(p, f).ok());

        p = path(one("kill_evolution", "{\"count\":5,\"filter\":\"boss\"}"));
        f.killsWith.put("a/boss", 4);
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.killsWith.put("a/boss", 5);
        assertTrue(EvolutionEngine.check(p, f).ok());
        p = path(one("kill_evolution", "{\"count\":5,\"withSkill\":false}"));
        f.kills = 4;
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.kills = 5;
        assertTrue(EvolutionEngine.check(p, f).ok(), "all kills when withSkill is false");

        p = path(one("forge_crafted_count", "{\"count\":50}"));
        f.forge = 49;
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.forge = 50;
        assertTrue(EvolutionEngine.check(p, f).ok());

        p = path(one("forge_quality", "{\"min\":\"epic\"}"));
        f.quality = 3;
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.quality = 4;
        assertTrue(EvolutionEngine.check(p, f).ok());
        p = path(one("forge_quality", "{\"min\":6}"));
        assertFalse(EvolutionEngine.check(p, f).ok());

        p = path(one("record_unlocked", "{\"record\":\"slay_calamity\"}"));
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.records.add("slay_calamity");
        assertTrue(EvolutionEngine.check(p, f).ok());
        assertFalse(EvolutionEngine.check(path(one("record_unlocked", "{}")), f).ok(), "no record named: never passes");

        p = path(one("has_title", "{\"title\":\"hero\"}"));
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.titles.add("hero");
        assertTrue(EvolutionEngine.check(p, f).ok());
        assertFalse(EvolutionEngine.check(path(one("has_title", null)), f).ok(), "no title named: never passes");

        p = path(one("active_title_evolution", "{\"titles\":[\"x\",\"y\"]}"));
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.active = "y";
        assertTrue(EvolutionEngine.check(p, f).ok());
        p = path(one("active_title_evolution", "{\"title\":\"x\"}"));
        assertFalse(EvolutionEngine.check(p, f).ok());
        assertTrue(EvolutionEngine.check(path(one("active_title_evolution", null)), f).ok(), "any active title");
        f.active = "";
        assertFalse(EvolutionEngine.check(path(one("active_title_evolution", null)), f).ok());

        p = path(one("held_title_count", "{\"count\":3}"));
        f.held = 2;
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.held = 3;
        assertTrue(EvolutionEngine.check(p, f).ok());

        p = path(one("reset_count", "{\"count\":2}"));
        f.resets = 1;
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.resets = 2;
        assertTrue(EvolutionEngine.check(p, f).ok());

        p = path(one("awakening_evolution", "{\"kind\":\"hero\"}"));
        assertFalse(EvolutionEngine.check(p, f).ok());
        f.awakened.add("hero");
        assertTrue(EvolutionEngine.check(p, f).ok());
        assertFalse(EvolutionEngine.check(path(one("awakening_evolution", "{\"kind\":\"demon_lord\"}")), f).ok());
        assertTrue(EvolutionEngine.check(path(one("awakening_evolution", null)), f).ok(), "any awakening");
    }

    @Test
    void aPathNeedsEveryRequirementAndListsWhatIsMissing() {
        String json = "{\"a\":{\"b\":[{\"trigger\":\"ep_gate\",\"params\":{\"min\":100}},{\"trigger\":\"reset_count\",\"params\":{\"count\":3}},"
                + "{\"trigger\":\"has_title\",\"params\":{\"title\":\"t\"}}]}}";
        EvolutionTable.Path p = path(json);
        Fake f = new Fake();
        f.ep = 500;
        EvolutionEngine.Outcome o = EvolutionEngine.check(p, f);
        assertFalse(o.ok());
        assertEquals(2, o.unmet().size(), "EP is met, the reset count and the title are not");
        f.resets = 3;
        f.titles.add("t");
        assertTrue(EvolutionEngine.check(p, f).ok());
        assertTrue(EvolutionEngine.check(p, f).unmet().isEmpty());
    }

    @Test
    void anOfferingIsPaidOnlyWhenEverythingElsePassed() {
        String json = "{\"a\":{\"b\":[{\"trigger\":\"item_consume\",\"params\":{\"item\":\"minecraft:netherite_ingot\",\"count\":4}},"
                + "{\"trigger\":\"ep_gate\",\"params\":{\"min\":10}}]}}";
        EvolutionTable.Path p = path(json);
        Fake f = new Fake();
        f.items.put("minecraft:netherite_ingot", 9);
        f.ep = 5;
        assertFalse(EvolutionEngine.commit(p, f));
        assertEquals(9, f.itemCount("minecraft:netherite_ingot"), "a failed requirement costs nothing");
        assertEquals(0, f.consumeCalls);
        f.ep = 10;
        assertTrue(EvolutionEngine.commit(p, f));
        assertEquals(5, f.itemCount("minecraft:netherite_ingot"));
        f.items.put("minecraft:netherite_ingot", 3);
        assertFalse(EvolutionEngine.check(p, f).ok(), "too few items");
        assertFalse(EvolutionEngine.check(path(one("item_consume", "{\"count\":2}")), f).ok(), "no item named: never passes");
    }

    @Test
    void anEmptyOrBrokenPathNeverEvolvesAnybody() {
        Fake f = new Fake();
        EvolutionTable t = new EvolutionTable().add("{\"a\":{\"b\":[]}}", "empty");
        assertEquals(1, t.size());
        assertFalse(EvolutionEngine.check(t.paths().get(0), f).ok(), "no requirements is not a free evolution");
        assertFalse(EvolutionEngine.commit(t.paths().get(0), f));
        assertFalse(EvolutionEngine.check(null, f).ok());

        EvolutionTable bad = new EvolutionTable();
        bad.add("{\"a\":{\"b\":[{\"trigger\":\"ep_gate\"},{\"trigger\":\"epgate\"}]}}", "typo");
        assertEquals(0, bad.size(), "a path with an unknown trigger is skipped as a whole");
        assertEquals(1, bad.errors().size());
        assertTrue(bad.errors().get(0).contains("epgate"));
        bad.add("not json at all {", "garbage");
        bad.add("[1,2]", "array");
        bad.add("{\"a\":5}", "scalar");
        bad.add("{\"a\":{\"b\":{\"trigger\":\"ep_gate\"}}}", "notlist");
        bad.add("{\"a\":{\"b\":[5]}}", "notobj");
        bad.add("{\"a\":{\"b\":[{\"params\":{}}]}}", "notrigger");
        assertEquals(0, bad.size());
        assertEquals(7, bad.errors().size());
    }

    @Test
    void laterFilesOverrideEarlierOnesPerPathAndCommentsAreIgnored() {
        EvolutionTable t = new EvolutionTable();
        t.add("{\"_comment\":\"x\",\"a\":{\"_note\":1,\"b\":[{\"trigger\":\"ep_gate\",\"params\":{\"min\":100}}],\"c\":[{\"trigger\":\"reset_count\"}]}}", "base");
        t.add("{\"a\":{\"b\":[{\"trigger\":\"ep_gate\",\"params\":{\"min\":5}}]}}", "pack");
        assertEquals(2, t.size());
        assertEquals(5.0, t.get("a", "b").reqs().get(0).params().dbl("min", 0), 1e-9);
        assertEquals(2, t.from("a").size());
        assertEquals(0, t.from("zzz").size());
        assertNull(t.get("a", "zzz"));
        assertTrue(t.errors().isEmpty());
    }

    @Test
    void paramsReadLooselyTypedValuesWithoutThrowing() {
        Params p = Params.of("n", 7, "s", "12", "bad", "x", "d", 2.5, "b", true, "l", List.of("a", "b"), "one", "solo");
        assertEquals(7, p.integer("n", 0));
        assertEquals(12, p.integer("s", 0), "numbers in strings are read");
        assertEquals(3, p.integer("bad", 3), "unreadable falls back");
        assertEquals(9, p.integer("missing", 9));
        assertEquals(2.5, p.dbl("d", 0), 1e-9);
        assertTrue(p.bool("b", false));
        assertEquals(List.of("a", "b"), p.strings("l"));
        assertEquals(List.of("solo"), p.strings("one"));
        assertTrue(p.strings("nothing").isEmpty());
        assertEquals("dflt", Params.EMPTY.str("k", "dflt"));
        assertTrue(p.has("n") && !p.has("zz"));
    }

    @Test
    void theShippedEvolutionsParseCleanlyAndEveryPathHasRequirements() throws Exception {
        File file = new File("src/main/resources/data/nusmp/elite/skills/evolutions.json");
        assertTrue(file.isFile(), "evolutions.json must ship");
        EvolutionTable t = new EvolutionTable().add(Files.readString(file.toPath(), StandardCharsets.UTF_8), "shipped");
        assertTrue(t.errors().isEmpty(), "shipped evolutions have errors: " + t.errors());
        assertTrue(t.size() >= 7, "every new Ultimate has a path, got " + t.size());
        Set<String> targets = new HashSet<>();
        for (EvolutionTable.Path p : t.paths()) {
            assertFalse(p.reqs().isEmpty(), p.from() + " -> " + p.to() + " has no requirements");
            assertTrue(p.from().startsWith("nusmp:") && p.to().startsWith("nusmp:"), "ids are namespaced");
            assertNotEquals(p.from(), p.to());
            assertTrue(targets.add(p.to()), "two paths lead to " + p.to());
        }
        for (String ult : List.of("basilisk_skin", "beelzebuth", "gaia", "hephaestus_ultimate", "ouroboros", "rapheal", "void_sovereign"))
            assertTrue(targets.contains("nusmp:" + ult), "no evolution leads to " + ult);
        // every one of the twelve triggers is used by at least one shipped path, so the whole engine is exercised by real data
        Set<String> used = new HashSet<>();
        for (EvolutionTable.Path p : t.paths()) for (EvolutionTable.Req r : p.reqs()) used.add(r.trigger().id());
        assertEquals(new HashSet<>(Triggers.ids()), used, "all twelve triggers appear in the shipped data");
    }
}
