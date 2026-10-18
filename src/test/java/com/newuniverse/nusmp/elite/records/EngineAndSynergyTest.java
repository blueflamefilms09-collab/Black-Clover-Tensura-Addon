package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.rule.BuiltinSynergies;
import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;
import com.newuniverse.nusmp.elite.records.rule.Engine;
import com.newuniverse.nusmp.elite.records.rule.Facts;
import com.newuniverse.nusmp.elite.records.rule.FactsBuilder;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.StatNames;
import com.newuniverse.nusmp.elite.records.rule.SynergyMatcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the unlock loop (records unlock, titles follow, titles can need other titles and record counts) and the synergy matcher are the two decision engines of the system; both are pinned here with a
 * tiny hand-made catalog, so a change to either shows up as a failing case and not as a player who never gets a title.
 */
class EngineAndSynergyTest {
    private static final String RECORDS = """
            {"records":[
              {"id":"kills_10","name":"Ten","category":"combat","rarity":"common","condition":{"type":"kill_count","count":10}},
              {"id":"kills_50","name":"Fifty","category":"combat","rarity":"rare","condition":{"type":"kill_count","count":50}},
              {"id":"deaths_1","name":"Once","category":"survival","rarity":"epic","condition":{"type":"death_count","count":1}},
              {"id":"ftb_1","name":"Plot","category":"dominion","rarity":"common","condition":{"type":"ftb_chunks_claimed","count":1}}
            ]}""";
    private static final String TITLES = """
            {"titles":[
              {"id":"brawler","name":"Brawler","rarity":"common","tags":["combat"],"requirements":[{"record":"kills_10"}],"bonuses":{"attributes":[{"attribute":"minecraft:generic.attack_damage","amount":0.5}]}},
              {"id":"slayer","name":"Slayer","rarity":"rare","tags":["combat"],"requirements":[{"record":"kills_50"},{"title":"brawler"}]},
              {"id":"survivor","name":"Survivor","rarity":"epic","tags":["survivor"],"requirements":[{"record":"deaths_1"}]},
              {"id":"landowner","name":"Landowner","rarity":"common","tags":["dominion"],"requirements":[{"record":"ftb_1"}]},
              {"id":"keeper","name":"Keeper","rarity":"uncommon","tags":["meta"],"requirements":[{"records":{"count":2,"category":"combat"}}]},
              {"id":"hero","name":"Hero","rarity":"legendary","tags":["meta"],"requirements":[{"any":[{"ep":1000},{"record":"kills_50"}]}]},
              {"id":"pointy","name":"Pointy","rarity":"common","tags":[],"requirements":[{"points":10}]},
              {"id":"epic_hunter","name":"Epic Hunter","rarity":"common","tags":[],"requirements":[{"records":{"count":1,"rarity":"epic"}}]},
              {"id":"ep_title","name":"EP Title","rarity":"common","tags":[],"requirements":[{"condition":{"type":"ep_reached","ep":500}}]},
              {"id":"extra_combat","name":"Extra Combat","rarity":"common","tags":["combat"],"requirements":[{"record":"kills_10"}]}
            ]}""";
    private static final String SYNERGIES = """
            {"synergies":[
              {"id":"duo","name":"Duo","requires":["brawler","slayer"],"bonuses":{"stats":{"xp_bonus_percent":5}}},
              {"id":"combat3","name":"Combat Trio","tag":"combat","count":3,"bonuses":{"stats":{"kill_heal":1}}}
            ]}""";

    private static Catalog catalog() {
        return Catalog.load(List.of(new Catalog.Source("r", RECORDS)), List.of(new Catalog.Source("t", TITLES)), List.of(new Catalog.Source("s", SYNERGIES)), false).catalog();
    }

    @Test
    void theHandMadeCatalogLoadsCleanly() {
        Catalog.Loaded l = Catalog.load(List.of(new Catalog.Source("r", RECORDS)), List.of(new Catalog.Source("t", TITLES)), List.of(new Catalog.Source("s", SYNERGIES)), false);
        assertEquals(List.of(), l.problems());
        assertEquals(4, l.catalog().records().size());
        assertEquals(10, l.catalog().titles().size());
        assertEquals(2, l.catalog().synergies().size());
    }

    @Test
    void unlocksFollowTheFactsAndChainIntoTitles() {
        Catalog cat = catalog();
        RecordData d = new RecordData();
        Facts f = new Facts().num("kills_all", 60).num("ep", 0).markUnavailable("ftbchunks").markUnavailable("tensura");
        Engine.Result r = Engine.run(cat, f, d, 1000);
        assertEquals(Set.of("kills_10", "kills_50"), new HashSet<>(r.newRecords()));
        assertTrue(r.newTitles().containsAll(List.of("brawler", "slayer", "keeper", "hero")), "titles that need records, titles and a record count: " + r.newTitles());
        assertFalse(d.holds("landowner"), "FTB Chunks is unavailable: no title");
        assertFalse(d.holds("survivor"));
        assertFalse(d.isUnlocked("ftb_1"));
        assertFalse(Engine.run(cat, f, d, 2000).any(), "a second run with the same facts changes nothing");
        assertEquals(1000L, d.unlockedAt("kills_10"));
    }

    @Test
    void unavailableNeverUnlocksAndAvailableDoes() {
        Catalog cat = catalog();
        RecordData d = new RecordData();
        Facts f = new Facts().num("ftb_chunks", 5);
        assertFalse(Engine.run(cat, f.markUnavailable("ftbchunks"), d, 1).newRecords().contains("ftb_1"));
        Facts g = new Facts().num("ftb_chunks", 5);
        assertTrue(Engine.run(cat, g, d, 2).newRecords().contains("ftb_1"));
        assertTrue(d.holds("landowner"));
    }

    @Test
    void pointsRarityAndEpRequirements() {
        Catalog cat = catalog();
        RecordData d = new RecordData();
        Facts f = new Facts().num("kills_all", 60).num("deaths", 1).num("ep", 600);
        Engine.run(cat, f, d, 5);
        // kills_10 common 1 + kills_50 rare 4 + deaths_1 epic 8 = 13 >= 10
        assertTrue(d.holds("pointy"));
        assertTrue(d.holds("epic_hunter"));
        assertTrue(d.holds("ep_title"));
    }

    @Test
    void nothingIsEverTakenAway() {
        Catalog cat = catalog();
        RecordData d = new RecordData();
        Engine.run(cat, new Facts().num("kills_all", 60), d, 5);
        Engine.run(cat, new Facts(), d, 6);                    // the numbers dropped to zero
        assertTrue(d.isUnlocked("kills_50"));
        assertTrue(d.holds("slayer"));
    }

    @Test
    void engineWorksThroughTheFactsBuilderFromSavedData() {
        Catalog cat = catalog();
        RecordData d = new RecordData();
        for (int i = 0; i < 12; i++) d.addCounter("kills_all", 1);
        Engine.run(cat, FactsBuilder.build(d), d, 9);
        assertTrue(d.isUnlocked("kills_10"));
        assertFalse(d.isUnlocked("kills_50"));
        var e = Engine.recordEval(cat.record("kills_50"), FactsBuilder.build(d));
        assertEquals(12.0 / 50.0, e.fraction(), 1e-9);
    }

    // ------------------------------------------------------------------ synergy matcher
    private static Set<String> tags(Catalog cat, String id) { return cat.tagsOf(id); }

    @Test
    void namedSynergyNeedsEveryRequiredTitleHeld() {
        Catalog cat = catalog();
        SynergyDef duo = cat.synergy("duo");
        assertFalse(SynergyMatcher.matches(duo, Set.of("brawler"), id -> tags(cat, id)));
        assertTrue(SynergyMatcher.matches(duo, Set.of("brawler", "slayer"), id -> tags(cat, id)));
        assertTrue(SynergyMatcher.matches(duo, Set.of("brawler", "slayer", "survivor"), id -> tags(cat, id)));
        assertArrayEq(new int[]{1, 2}, SynergyMatcher.progress(duo, Set.of("slayer"), id -> tags(cat, id)));
    }

    private static void assertArrayEq(int[] a, int[] b) { assertEquals(a[0] + "/" + a[1], b[0] + "/" + b[1]); }

    @Test
    void tagSynergyCountsHeldTitlesWithTheTag() {
        Catalog cat = catalog();
        SynergyDef trio = cat.synergy("combat3");
        assertFalse(SynergyMatcher.matches(trio, Set.of("brawler", "slayer"), id -> tags(cat, id)));
        // keeper and hero are tagged meta, not combat: only two combat titles exist in the catalog
        Set<String> held = new HashSet<>(Set.of("brawler", "slayer"));
        assertArrayEq(new int[]{2, 3}, SynergyMatcher.progress(trio, held, id -> tags(cat, id)));
    }

    @Test
    void starTagAndRarityTagAreImplicit() {
        Catalog cat = catalog();
        SynergyDef any3 = new SynergyDef("any3", "Any", "", List.of(), "*", 3, Bonuses.NONE);
        SynergyDef rare1 = new SynergyDef("rare1", "Rare", "", List.of(), "rarity:rare", 1, Bonuses.NONE);
        assertFalse(SynergyMatcher.matches(any3, Set.of("brawler", "slayer"), id -> tags(cat, id)));
        assertTrue(SynergyMatcher.matches(any3, Set.of("brawler", "slayer", "survivor"), id -> tags(cat, id)));
        assertTrue(SynergyMatcher.matches(rare1, Set.of("slayer"), id -> tags(cat, id)));
        assertFalse(SynergyMatcher.matches(rare1, Set.of("brawler"), id -> tags(cat, id)));
    }

    @Test
    void mixedRequiresAndTagMustBothHold() {
        Catalog cat = catalog();
        SynergyDef mix = new SynergyDef("mix", "Mix", "", List.of("survivor"), "combat", 2, Bonuses.NONE);
        assertFalse(SynergyMatcher.matches(mix, Set.of("survivor", "brawler"), id -> tags(cat, id)));
        assertFalse(SynergyMatcher.matches(mix, Set.of("brawler", "slayer"), id -> tags(cat, id)));
        assertTrue(SynergyMatcher.matches(mix, Set.of("survivor", "brawler", "slayer"), id -> tags(cat, id)));
    }

    @Test
    void emptyHeldSetAndEmptyDefinitionNeverMatch() {
        Catalog cat = catalog();
        assertEquals(0, SynergyMatcher.match(cat.synergies(), Set.of(), id -> tags(cat, id)).size());
        SynergyDef empty = new SynergyDef("empty", "Empty", "", List.of(), "", 0, Bonuses.NONE);
        assertFalse(SynergyMatcher.matches(empty, Set.of("brawler"), id -> tags(cat, id)));
    }

    @Test
    void matchedBonusesAreCombined() {
        Catalog cat = catalog();
        Set<String> held = Set.of("brawler", "slayer", "survivor");
        List<SynergyDef> m = SynergyMatcher.match(cat.synergies(), held, id -> tags(cat, id));
        assertTrue(m.stream().anyMatch(s -> s.id().equals("duo")));
        Bonuses b = SynergyMatcher.combined(m);
        assertEquals(5.0, b.stat(StatNames.XP_BONUS_PERCENT), 1e-9);
    }

    @Test
    void builtinSynergiesAreAtLeastFifteenWithUniqueIds() {
        List<SynergyDef> all = BuiltinSynergies.all();
        assertTrue(all.size() >= 15, "built-in synergies: " + all.size());
        Set<String> ids = new HashSet<>();
        for (SynergyDef s : all) {
            assertTrue(ids.add(s.id()), "duplicate synergy id " + s.id());
            assertFalse(s.bonuses().isEmpty(), s.id() + " gives nothing");
            assertTrue(!s.requires().isEmpty() || (!s.tag().isEmpty() && s.tagCount() > 0), s.id());
        }
    }

    @Test
    void aSynergyWithTheSameIdFromDataReplacesTheBuiltinOne() {
        String json = "{\"synergies\":[{\"id\":\"two_crowns\",\"name\":\"Retuned\",\"requires\":[\"x\"],\"bonuses\":{\"stats\":{\"kill_heal\":9}}}]}";
        Catalog c = new Catalog(List.of(), List.of(), com.newuniverse.nusmp.elite.records.rule.DataParser.synergies(json, "t").items(), true);
        assertEquals("Retuned", c.synergy("two_crowns").name());
        assertEquals(BuiltinSynergies.all().size(), c.synergies().size());
    }

    @Test
    void validateFindsBrokenReferencesAndLoops() {
        String titles = """
                {"titles":[
                 {"id":"aa","name":"A","requirements":[{"title":"bb"}]},
                 {"id":"bb","name":"B","requirements":[{"title":"aa"}]},
                 {"id":"cc","name":"C","requirements":[{"record":"nope"}]},
                 {"id":"dd","name":"D","requirements":[{"title":"ghost"}],"bonuses":{"stats":{"made_up_stat":1}}}
                ]}""";
        String syn = "{\"synergies\":[{\"id\":\"s1\",\"name\":\"S\",\"requires\":[\"zzz\"]},{\"id\":\"s2\",\"name\":\"S2\",\"tag\":\"nothing\",\"count\":2}]}";
        Catalog.Loaded l = Catalog.load(List.of(), List.of(new Catalog.Source("t", titles)), List.of(new Catalog.Source("s", syn)), false);
        String all = String.join("\n", l.problems());
        assertTrue(all.contains("loop"), all);
        assertTrue(all.contains("unknown record 'nope'"), all);
        assertTrue(all.contains("unknown title 'ghost'"), all);
        assertTrue(all.contains("unknown stat 'made_up_stat'"), all);
        assertTrue(all.contains("synergy s1 needs the unknown title 'zzz'"), all);
        assertTrue(all.contains("synergy s2 needs 2 titles tagged 'nothing'"), all);
    }

    @Test
    void brokenEntriesAreSkippedNotFatal() {
        String recs = "{\"records\":[{\"id\":\"ok\",\"name\":\"Ok\",\"category\":\"combat\",\"condition\":{\"type\":\"death_count\"}},"
                + "{\"id\":\"BAD ID\",\"name\":\"x\",\"category\":\"combat\",\"condition\":{\"type\":\"death_count\"}},"
                + "{\"id\":\"nocat\",\"name\":\"x\",\"category\":\"mystery\",\"condition\":{\"type\":\"death_count\"}},"
                + "{\"id\":\"nocond\",\"name\":\"x\",\"category\":\"combat\"},"
                + "{\"id\":\"badrar\",\"name\":\"x\",\"category\":\"combat\",\"rarity\":\"shiny\",\"condition\":{\"type\":\"death_count\"}}]}";
        var p = com.newuniverse.nusmp.elite.records.rule.DataParser.records(recs, "f");
        assertEquals(1, p.items().size());
        assertEquals(4, p.problems().size(), String.valueOf(p.problems()));
        assertTrue(com.newuniverse.nusmp.elite.records.rule.DataParser.records("not json {", "f").items().isEmpty());
        assertEquals(1, com.newuniverse.nusmp.elite.records.rule.DataParser.records("not json {", "f").problems().size());
        assertTrue(new ArrayList<>(com.newuniverse.nusmp.elite.records.rule.DataParser.titles("[]", "f").items()).isEmpty());
    }
}
