package com.newuniverse.nusmp.elite.records;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.records.rule.CondType;
import com.newuniverse.nusmp.elite.records.rule.Condition;
import com.newuniverse.nusmp.elite.records.rule.ConditionEvaluator;
import com.newuniverse.nusmp.elite.records.rule.Eval;
import com.newuniverse.nusmp.elite.records.rule.Facts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the twenty condition types are the whole unlock logic of the records system, so every one of them is pinned here on its own: it is met at the threshold and not one below, it reports the
 * progress the bar draws, the types with an outside source answer UNAVAILABLE (not zero) when the source is missing, ids match with the wildcard rule, and bad parameters are caught when a data file is
 * read. The test builds {@link Facts} by hand, so it needs no game.
 */
class ConditionEvaluatorTest {
    private static Condition c(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        List<String> problems = new ArrayList<>();
        Condition cond = Condition.parse(o, "test", problems);
        assertNotNull(cond, "should parse: " + json + " " + problems);
        return cond;
    }

    private static Eval ev(String json, Facts f) { return ConditionEvaluator.evaluate(c(json), f); }

    @Test
    void biomeDiscoverySingleAndCount() {
        Facts f = new Facts().set("biomes", Set.of("minecraft:plains", "minecraft:desert", "tensura:jura_forest"));
        assertTrue(ev("{\"type\":\"biome_discovery\",\"biome\":\"minecraft:desert\"}", f).met());
        assertFalse(ev("{\"type\":\"biome_discovery\",\"biome\":\"minecraft:jungle\"}", f).met());
        assertTrue(ev("{\"type\":\"biome_discovery\",\"biome\":\"plains\"}", f).met());           // a bare path is the minecraft namespace
        assertTrue(ev("{\"type\":\"biome_discovery\",\"count\":3}", f).met());
        assertFalse(ev("{\"type\":\"biome_discovery\",\"count\":4}", f).met());
        Eval ns = ev("{\"type\":\"biome_discovery\",\"count\":2,\"namespace\":\"minecraft\"}", f);
        assertTrue(ns.met());
        assertEquals(2.0, ns.current(), 0.0);
        assertEquals(0.5, ev("{\"type\":\"biome_discovery\",\"count\":6}", f).fraction(), 1e-9);
    }

    @Test
    void structureDiscoveryWildcard() {
        Facts f = new Facts().set("structures", Set.of("minecraft:village_plains", "minecraft:mansion"));
        assertTrue(ev("{\"type\":\"structure_discovery\",\"structure\":\"minecraft:village_*\"}", f).met());
        assertTrue(ev("{\"type\":\"structure_discovery\",\"structure\":\"minecraft:mansion\"}", f).met());
        assertFalse(ev("{\"type\":\"structure_discovery\",\"structure\":\"minecraft:village\"}", f).met());   // exact means exact
        assertTrue(ev("{\"type\":\"structure_discovery\",\"count\":2}", f).met());
        assertFalse(ev("{\"type\":\"structure_discovery\",\"count\":3}", f).met());
    }

    @Test
    void calamityDamageAndKill() {
        Facts f = new Facts().num("calamity_damage", 999).num("calamity_kills", 2).count("calamity_kills", "nusmp:storm", 2);
        assertFalse(ev("{\"type\":\"calamity_damage\",\"amount\":1000}", f).met());
        assertTrue(ev("{\"type\":\"calamity_damage\",\"amount\":999}", f).met());
        assertTrue(ev("{\"type\":\"calamity_kill\",\"count\":2}", f).met());
        assertFalse(ev("{\"type\":\"calamity_kill\",\"count\":3}", f).met());
        assertTrue(ev("{\"type\":\"calamity_kill\",\"calamity\":\"nusmp:storm\",\"count\":2}", f).met());
        assertFalse(ev("{\"type\":\"calamity_kill\",\"calamity\":\"nusmp:other\"}", f).met());
    }

    @Test
    void deathCount() {
        Facts f = new Facts().num("deaths", 9);
        assertFalse(ev("{\"type\":\"death_count\",\"count\":10}", f).met());
        assertTrue(ev("{\"type\":\"death_count\",\"count\":9}", f).met());
    }

    @Test
    void distanceTraveledModes() {
        Facts f = new Facts().num("distance_all", 12000).num("distance_fly", 500).num("distance_elytra", 6000);
        assertTrue(ev("{\"type\":\"distance_traveled\",\"meters\":10000}", f).met());          // mode defaults to all
        assertFalse(ev("{\"type\":\"distance_traveled\",\"meters\":10000,\"mode\":\"fly\"}", f).met());
        assertTrue(ev("{\"type\":\"distance_traveled\",\"meters\":5000,\"mode\":\"elytra\"}", f).met());
        assertFalse(ev("{\"type\":\"distance_traveled\",\"meters\":1,\"mode\":\"boat\"}", f).met());
    }

    @Test
    void epReachedAndTensuraUnavailable() {
        Facts f = new Facts().num("ep", 5000);
        assertTrue(ev("{\"type\":\"ep_reached\",\"ep\":5000}", f).met());
        assertFalse(ev("{\"type\":\"ep_reached\",\"ep\":5001}", f).met());
        Facts none = new Facts().markUnavailable("tensura");
        assertTrue(ev("{\"type\":\"ep_reached\",\"ep\":1}", none).unavailable());
        assertFalse(ev("{\"type\":\"ep_reached\",\"ep\":1}", none).met());
        assertEquals(0.0, ev("{\"type\":\"ep_reached\",\"ep\":1}", none).fraction(), 0.0);
    }

    @Test
    void forgeQualityByNumberAndWord() {
        Facts f = new Facts().num("forge_best_quality", 4);
        assertTrue(ev("{\"type\":\"forge_quality\",\"quality\":4}", f).met());
        assertTrue(ev("{\"type\":\"forge_quality\",\"quality\":\"epic\"}", f).met());
        assertFalse(ev("{\"type\":\"forge_quality\",\"quality\":\"legendary\"}", f).met());
        assertFalse(ev("{\"type\":\"forge_quality\",\"quality\":\"mythic\"}", f).met());
        assertTrue(ev("{\"type\":\"forge_quality\",\"quality\":\"mythic\"}", new Facts().num("forge_best_quality", 6)).met());
    }

    @Test
    void forgeCountAndMinimumQuality() {
        Facts f = new Facts().num("forge_count", 30).count("forge_quality", "1", 20).count("forge_quality", "4", 6).count("forge_quality", "5", 4);
        assertTrue(ev("{\"type\":\"forge_count\",\"count\":30}", f).met());
        assertFalse(ev("{\"type\":\"forge_count\",\"count\":31}", f).met());
        assertTrue(ev("{\"type\":\"forge_count\",\"count\":10,\"min_quality\":4}", f).met());
        assertFalse(ev("{\"type\":\"forge_count\",\"count\":11,\"min_quality\":4}", f).met());
        assertTrue(ev("{\"type\":\"forge_count\",\"count\":4,\"min_quality\":\"legendary\"}", f).met());
    }

    @Test
    void ftbChunksClaimedAndAbsent() {
        assertTrue(ev("{\"type\":\"ftb_chunks_claimed\",\"count\":9}", new Facts().num("ftb_chunks", 9)).met());
        assertFalse(ev("{\"type\":\"ftb_chunks_claimed\",\"count\":9}", new Facts().num("ftb_chunks", 8)).met());
        Eval gone = ev("{\"type\":\"ftb_chunks_claimed\",\"count\":1}", new Facts().num("ftb_chunks", 50).markUnavailable("ftbchunks"));
        assertTrue(gone.unavailable(), "FTB Chunks absent must be unavailable even if a stale number is present");
    }

    @Test
    void statusConditions() {
        assertFalse(ev("{\"type\":\"demon_lord_status\",\"status\":\"true\"}", new Facts().num("demon_status", 1)).met());
        assertTrue(ev("{\"type\":\"demon_lord_status\",\"status\":\"seed\"}", new Facts().num("demon_status", 1)).met());
        assertTrue(ev("{\"type\":\"demon_lord_status\"}", new Facts().num("demon_status", 2)).met());            // default is the true Demon Lord
        assertTrue(ev("{\"type\":\"true_hero_status\",\"status\":\"egg\"}", new Facts().num("hero_status", 1)).met());
        assertFalse(ev("{\"type\":\"true_hero_status\"}", new Facts().num("hero_status", 1)).met());
        assertTrue(ev("{\"type\":\"true_hero_status\"}", new Facts().num("hero_status", 2)).met());
        assertTrue(ev("{\"type\":\"higher_being_status\"}", new Facts().num("higher_being", 1)).met());
        assertFalse(ev("{\"type\":\"higher_being_status\",\"status\":\"divine\"}", new Facts().num("higher_being", 1)).met());
        assertTrue(ev("{\"type\":\"higher_being_status\",\"status\":\"divine\"}", new Facts().num("higher_being", 2)).met());
        for (String t : new String[]{"demon_lord_status", "true_hero_status", "higher_being_status"}) {
            assertTrue(ev("{\"type\":\"" + t + "\"}", new Facts().markUnavailable("tensura")).unavailable(), t);
        }
    }

    @Test
    void killEntityExactWildcardAndList() {
        Facts f = new Facts().count("kills", "minecraft:zombie", 40).count("kills", "minecraft:husk", 10).count("kills", "tensura:orc", 7).count("kills", "tensura:goblin", 5);
        assertTrue(ev("{\"type\":\"kill_entity\",\"entity\":\"minecraft:zombie\",\"count\":40}", f).met());
        assertFalse(ev("{\"type\":\"kill_entity\",\"entity\":\"minecraft:zombie\",\"count\":41}", f).met());
        assertTrue(ev("{\"type\":\"kill_entity\",\"entities\":[\"minecraft:zombie\",\"minecraft:husk\"],\"count\":50}", f).met());
        assertTrue(ev("{\"type\":\"kill_entity\",\"entity\":\"tensura:*\",\"count\":12}", f).met());
        assertFalse(ev("{\"type\":\"kill_entity\",\"entity\":\"tensura:*\",\"count\":13}", f).met());
        assertTrue(ev("{\"type\":\"kill_entity\",\"entity\":\"zombie\"}", f).met());                    // count defaults to 1, bare path is minecraft
        assertFalse(ev("{\"type\":\"kill_entity\",\"entity\":\"minecraft:warden\"}", f).met());
    }

    @Test
    void killCountGroups() {
        Facts f = new Facts().num("kills_all", 100).num("kills_hostile", 60).num("kills_player", 1).num("kills_boss", 2).num("kills_passive", 5);
        assertTrue(ev("{\"type\":\"kill_count\",\"count\":100}", f).met());
        assertFalse(ev("{\"type\":\"kill_count\",\"count\":101}", f).met());
        assertTrue(ev("{\"type\":\"kill_count\",\"count\":60,\"group\":\"hostile\"}", f).met());
        assertFalse(ev("{\"type\":\"kill_count\",\"count\":2,\"group\":\"player\"}", f).met());
        assertTrue(ev("{\"type\":\"kill_count\",\"count\":2,\"group\":\"boss\"}", f).met());
        assertTrue(ev("{\"type\":\"kill_count\",\"count\":5,\"group\":\"passive\"}", f).met());
    }

    @Test
    void singleHitDamage() {
        Facts f = new Facts().num("single_hit", 249.5);
        assertFalse(ev("{\"type\":\"single_hit_damage\",\"damage\":250}", f).met());
        assertTrue(ev("{\"type\":\"single_hit_damage\",\"damage\":249.5}", f).met());
    }

    @Test
    void skillLearnedByIdCountAndType() {
        Facts f = new Facts().set("skills", Set.of("tensura:great_sage", "tensura:sage", "tensura:predator")).set("skills:unique", Set.of("tensura:predator"))
                .set("skills:extra", Set.of("tensura:sage", "tensura:x"));
        assertTrue(ev("{\"type\":\"skill_learned\",\"skill\":\"tensura:great_sage\"}", f).met());
        assertFalse(ev("{\"type\":\"skill_learned\",\"skill\":\"tensura:nope\"}", f).met());
        assertTrue(ev("{\"type\":\"skill_learned\",\"count\":3}", f).met());
        assertFalse(ev("{\"type\":\"skill_learned\",\"count\":4}", f).met());
        assertTrue(ev("{\"type\":\"skill_learned\",\"count\":1,\"skill_type\":\"unique\"}", f).met());
        assertFalse(ev("{\"type\":\"skill_learned\",\"count\":2,\"skill_type\":\"unique\"}", f).met());
        assertTrue(ev("{\"type\":\"skill_learned\",\"count\":2,\"skill_type\":\"extra\"}", f).met());
        assertTrue(ev("{\"type\":\"skill_learned\",\"count\":1}", new Facts().markUnavailable("skills")).unavailable());
    }

    @Test
    void walpurgisAndWar() {
        Facts f = new Facts().num("walpurgis", 3).num("war_kills", 49).num("war_wins", 5);
        assertTrue(ev("{\"type\":\"walpurgis_attended\",\"count\":3}", f).met());
        assertFalse(ev("{\"type\":\"walpurgis_attended\",\"count\":4}", f).met());
        assertFalse(ev("{\"type\":\"war_kills\",\"count\":50}", f).met());
        assertTrue(ev("{\"type\":\"war_kills\",\"count\":49}", f).met());
        assertTrue(ev("{\"type\":\"war_wins\",\"count\":5}", f).met());
        assertFalse(ev("{\"type\":\"war_wins\",\"count\":6}", f).met());
    }

    @Test
    void everyTypeIsCoveredByThisTestAndHasANonThrowingEvaluator() {
        Set<CondType> seen = EnumSet.noneOf(CondType.class);
        String[] samples = {
                "{\"type\":\"biome_discovery\",\"count\":1}", "{\"type\":\"calamity_damage\",\"amount\":1}", "{\"type\":\"calamity_kill\"}", "{\"type\":\"death_count\"}",
                "{\"type\":\"distance_traveled\",\"meters\":1}", "{\"type\":\"ep_reached\",\"ep\":1}", "{\"type\":\"forge_quality\",\"quality\":1}", "{\"type\":\"forge_count\"}",
                "{\"type\":\"ftb_chunks_claimed\"}", "{\"type\":\"demon_lord_status\"}", "{\"type\":\"higher_being_status\"}", "{\"type\":\"true_hero_status\"}",
                "{\"type\":\"kill_entity\",\"entity\":\"minecraft:pig\"}", "{\"type\":\"kill_count\"}", "{\"type\":\"single_hit_damage\",\"damage\":1}", "{\"type\":\"skill_learned\",\"count\":1}",
                "{\"type\":\"structure_discovery\",\"count\":1}", "{\"type\":\"walpurgis_attended\"}", "{\"type\":\"war_kills\"}", "{\"type\":\"war_wins\"}"};
        for (String s : samples) {
            Condition cond = c(s);
            seen.add(cond.type());
            assertNotNull(ConditionEvaluator.evaluate(cond, new Facts()));
        }
        assertEquals(20, CondType.values().length);
        assertEquals(CondType.values().length, seen.size(), "a condition type has no sample here");
    }

    @Test
    void badParametersAreRejectedWhenReadingAFile() {
        String[] bad = {
                "{\"type\":\"nope\"}", "{\"type\":\"biome_discovery\"}", "{\"type\":\"calamity_damage\"}", "{\"type\":\"distance_traveled\",\"meters\":5,\"mode\":\"teleport\"}",
                "{\"type\":\"distance_traveled\",\"mode\":\"walk\"}", "{\"type\":\"ep_reached\",\"ep\":-4}", "{\"type\":\"forge_quality\",\"quality\":9}", "{\"type\":\"forge_quality\"}",
                "{\"type\":\"demon_lord_status\",\"status\":\"maybe\"}", "{\"type\":\"kill_entity\"}", "{\"type\":\"kill_count\",\"group\":\"aliens\"}", "{\"type\":\"kill_count\",\"count\":0}",
                "{\"type\":\"skill_learned\",\"count\":2,\"skill_type\":\"divine\"}", "{\"type\":\"single_hit_damage\"}", "{\"type\":\"structure_discovery\"}"};
        for (String s : bad) {
            List<String> problems = new ArrayList<>();
            Condition cond = Condition.parse(JsonParser.parseString(s), "t", problems);
            assertNull(cond, s);
            assertFalse(problems.isEmpty(), s);
        }
        List<String> p = new ArrayList<>();
        assertNull(Condition.parse(JsonParser.parseString("[1]"), "t", p));
    }

    @Test
    void progressFractionsAreClampedAndTargetsAreNeverZero() {
        Eval e = ev("{\"type\":\"kill_count\",\"count\":100}", new Facts().num("kills_all", 25));
        assertEquals(0.25, e.fraction(), 1e-9);
        assertEquals(100.0, e.target(), 0.0);
        assertEquals(1.0, ev("{\"type\":\"kill_count\",\"count\":100}", new Facts().num("kills_all", 9999)).fraction(), 0.0);
        assertEquals(1.0, Eval.atLeast(0, 0).target(), 0.0);
    }

    @Test
    void idMatchingRules() {
        assertTrue(ConditionEvaluator.idMatches("minecraft:village_*", "minecraft:village_taiga"));
        assertFalse(ConditionEvaluator.idMatches("minecraft:village_*", "minecraft:villager"));
        assertTrue(ConditionEvaluator.idMatches("zombie", "minecraft:zombie"));
        assertFalse(ConditionEvaluator.idMatches("tensura:zombie", "minecraft:zombie"));
        assertFalse(ConditionEvaluator.idMatches(null, "x"));
    }
}
