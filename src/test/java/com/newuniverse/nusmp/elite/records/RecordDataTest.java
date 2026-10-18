package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.rule.FactsBuilder;
import com.newuniverse.nusmp.elite.records.rule.Live;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.Tracking;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the player's record data is saved with the player; a save that is old, new, hand-edited or damaged must still load as much as it can and must never throw (a player who cannot load cannot
 * log in), and the numbers that are only ever allowed to rise (bests) or grow (found sets) must really only rise and grow. Also covers the event arithmetic of {@link Tracking}.
 */
class RecordDataTest {
    private static RecordData sample() {
        RecordData d = new RecordData();
        d.unlock("kills_100", 1_700_000_000L);
        d.grantTitle("brawler", 1_700_000_100L);
        d.setActive("brawler");
        d.addCounter("kills_all", 120);
        d.bestMax("ep", 12345.5);
        d.mapInc("kills", "minecraft:zombie", 80);
        d.setAdd("biomes", "minecraft:plains");
        d.setAdd("biomes", "minecraft:desert");
        return d;
    }

    @Test
    void roundTripKeepsEverything() {
        RecordData back = RecordData.fromRaw(sample().toRaw());
        assertTrue(back.isUnlocked("kills_100"));
        assertEquals(1_700_000_000L, back.unlockedAt("kills_100"));
        assertTrue(back.holds("brawler"));
        assertEquals("brawler", back.active());
        assertEquals(120.0, back.counter("kills_all"), 0.0);
        assertEquals(12345.5, back.best("ep"), 0.0);
        assertEquals(80, back.mapGet("kills", "minecraft:zombie"));
        assertEquals(Set.of("minecraft:plains", "minecraft:desert"), back.set("biomes"));
        assertEquals(sample().toRaw(), back.toRaw());
    }

    @Test
    void copyIsIndependent() {
        RecordData a = sample();
        RecordData b = a.copy();
        b.unlock("other", 5);
        assertFalse(a.isUnlocked("other"));
        assertTrue(b.isUnlocked("kills_100"));
    }

    @Test
    void garbageNeverThrowsAndLoadsWhatItCan() {
        assertTrue(RecordData.fromRaw(null).isEmpty());
        assertTrue(RecordData.fromRaw("hello").isEmpty());
        assertTrue(RecordData.fromRaw(42).isEmpty());
        assertTrue(RecordData.fromRaw(new ArrayList<>()).isEmpty());
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("rec", "not a map");
        raw.put("tit", Map.of("good", 5L, "negative", -4L, "text", "x", "", 3L));
        raw.put("act", "missing_title");                                           // an active title that is not held is dropped
        raw.put("cnt", Map.of("a", 2.5, "nan", Double.NaN, "neg", -1.0, "str", "x"));
        raw.put("best", List.of(1, 2));
        raw.put("maps", Map.of("kills", Map.of("zombie", 3, "bad", "x", "neg", -2), "junk", 7));
        raw.put("sets", Map.of("biomes", List.of("a", 5, "", "b"), "other", "nope"));
        RecordData d = RecordData.fromRaw(raw);
        assertEquals(Set.of("good"), d.heldTitles());
        assertEquals("", d.active());
        assertEquals(2.5, d.counter("a"), 0.0);
        assertEquals(0.0, d.counter("nan"), 0.0);
        assertEquals(0.0, d.counter("neg"), 0.0);
        assertEquals(3, d.mapGet("kills", "zombie"));
        assertEquals(0, d.mapGet("kills", "neg"));
        assertEquals(Set.of("a", "b"), d.set("biomes"));
    }

    @Test
    void absurdlyLongListsAreCut() {
        Map<String, Object> times = new HashMap<>();
        for (int i = 0; i < RecordData.MAX_ENTRIES + 500; i++) times.put("r" + i, 7L);
        Map<String, Object> raw = new HashMap<>();
        raw.put("rec", times);
        assertEquals(RecordData.MAX_ENTRIES, RecordData.fromRaw(raw).unlockedCount());
        RecordData d = new RecordData();
        for (int i = 0; i < RecordData.MAX_ENTRIES + 10; i++) d.setAdd("biomes", "b" + i);
        assertEquals(RecordData.MAX_ENTRIES, d.set("biomes").size());
    }

    @Test
    void unlockAndGrantAreOnceOnly() {
        RecordData d = new RecordData();
        assertTrue(d.unlock("x", 10));
        assertFalse(d.unlock("x", 20));
        assertEquals(10L, d.unlockedAt("x"));
        assertFalse(d.unlock("", 1));
        assertTrue(d.grantTitle("t", 5));
        assertFalse(d.grantTitle("t", 6));
        assertTrue(d.unlock("zero_time", 0));
        assertTrue(d.unlockedAt("zero_time") > 0, "a zero timestamp is stored as 1 so it still reads as unlocked");
    }

    @Test
    void activeTitleMustBeHeldAndRevokeClearsIt() {
        RecordData d = new RecordData();
        assertFalse(d.setActive("ghost"));
        d.grantTitle("a", 1);
        d.grantTitle("b", 1);
        assertTrue(d.setActive("a"));
        assertEquals("a", d.active());
        assertTrue(d.setActive("b"));
        assertTrue(d.revokeTitle("b"));
        assertEquals("", d.active());
        assertTrue(d.setActive("a"));
        assertTrue(d.setActive(""));
        assertEquals("", d.active());
    }

    @Test
    void bestsOnlyRiseAndCountersNeverGoNegative() {
        RecordData d = new RecordData();
        assertTrue(d.bestMax("ep", 100));
        assertFalse(d.bestMax("ep", 50));
        assertFalse(d.bestMax("ep", Double.NaN));
        assertEquals(100.0, d.best("ep"), 0.0);
        d.addCounter("x", 5);
        d.addCounter("x", -50);
        assertEquals(0.0, d.counter("x"), 0.0);
        d.addCounter("x", Double.POSITIVE_INFINITY);
        assertEquals(0.0, d.counter("x"), 0.0);
        d.mapInc("m", "k", Integer.MAX_VALUE);
        d.mapInc("m", "k", 100);
        assertEquals(Integer.MAX_VALUE, d.mapGet("m", "k"));
    }

    @Test
    void foldingLiveReadingsOnlyRaisesAndGrows() {
        RecordData d = new RecordData();
        Live live = new Live();
        live.tensuraOk = true;
        live.ep = 5000;
        live.demon = 1;
        live.ftbChunks = 12.0;
        live.deaths = 3;
        live.distance.put("all", 3000.0);
        live.skills = Set.of("tensura:a", "tensura:b");
        live.skillsByType.put("unique", Set.of("tensura:a"));
        assertTrue(FactsBuilder.absorb(d, live));
        assertFalse(FactsBuilder.absorb(d, live), "the same reading again changes nothing");
        // the player then loses EP, releases claims, forgets a skill
        Live later = new Live();
        later.tensuraOk = true;
        later.ep = 10;
        later.ftbChunks = 2.0;
        later.skills = Set.of("tensura:b");
        FactsBuilder.absorb(d, later);
        var f = FactsBuilder.build(d);
        assertEquals(5000.0, f.num("ep"), 0.0);
        assertEquals(12.0, f.num("ftb_chunks"), 0.0);
        assertEquals(1.0, f.num("demon_status"), 0.0);
        assertEquals(3.0, f.num("deaths"), 0.0);
        assertEquals(3000.0, f.num("distance_all"), 0.0);
        assertEquals(Set.of("tensura:a", "tensura:b"), f.set("skills"));
        assertEquals(Set.of("tensura:a"), f.set("skills:unique"));
        assertFalse(f.unavailable("tensura"));
        assertFalse(f.unavailable("ftbchunks"));
        assertFalse(f.unavailable("skills"));
    }

    @Test
    void sourcesThatNeverAnsweredAreUnavailable() {
        RecordData d = new RecordData();
        Live live = new Live();                       // Tensura not ok, no FTB Chunks, skills unreadable
        var f = FactsBuilder.build(d, live);
        assertTrue(f.unavailable("tensura"));
        assertTrue(f.unavailable("ftbchunks"));
        assertTrue(f.unavailable("skills"));
    }

    @Test
    void deathsTakeTheLargerOfVanillaAndLocal() {
        RecordData d = new RecordData();
        Tracking.death(d);
        Tracking.death(d);
        Live live = new Live();
        live.deaths = 1;
        assertEquals(2.0, FactsBuilder.build(d, live).num("deaths"), 0.0);
        live.deaths = 9;
        assertEquals(9.0, FactsBuilder.build(d, live).num("deaths"), 0.0);
    }

    @Test
    void forgeHookNumbersAndLocalCountersMerge() {
        RecordData d = new RecordData();
        Tracking.forged(d, 3);
        Tracking.forged(d, 5);
        Live live = new Live();
        live.hookForgeCount = 1;
        live.hookForgeBest = 2;
        var f = FactsBuilder.build(d, live);
        assertEquals(2.0, f.num("forge_count"), 0.0);
        assertEquals(5.0, f.num("forge_best_quality"), 0.0);
        live.hookForgeCount = 40;
        live.hookForgeBest = 6;
        f = FactsBuilder.build(d, live);
        assertEquals(40.0, f.num("forge_count"), 0.0);
        assertEquals(6.0, f.num("forge_best_quality"), 0.0);
        assertEquals(1, f.count("forge_quality", "3"));
        assertEquals(1, f.count("forge_quality", "5"));
    }

    @Test
    void trackingEvents() {
        RecordData d = new RecordData();
        Tracking.kill(d, new Tracking.Victim("minecraft:zombie", true, false, false, false));
        Tracking.kill(d, new Tracking.Victim("minecraft:cow", false, true, false, false));
        Tracking.kill(d, new Tracking.Victim("minecraft:player", false, false, true, false));
        Tracking.kill(d, new Tracking.Victim("minecraft:wither", true, false, false, true));
        Tracking.kill(d, new Tracking.Victim("", true, false, false, false));          // ignored
        Tracking.kill(d, null);
        var f = FactsBuilder.build(d);
        assertEquals(4.0, f.num("kills_all"), 0.0);
        assertEquals(2.0, f.num("kills_hostile"), 0.0);
        assertEquals(1.0, f.num("kills_passive"), 0.0);
        assertEquals(1.0, f.num("kills_player"), 0.0);
        assertEquals(1.0, f.num("kills_boss"), 0.0);
        assertEquals(1, f.count("kills", "minecraft:wither"));
        Tracking.hit(d, 12);
        Tracking.hit(d, 7);
        Tracking.hit(d, -3);
        Tracking.hit(d, Double.NaN);
        assertEquals(12.0, d.best("single_hit"), 0.0);
        Tracking.forged(d, 0);
        Tracking.forged(d, 99);
        assertEquals(1.0, d.counter("forge_count"), 0.0);
        assertEquals(6.0, d.best("forge_quality"), 0.0);
        Tracking.calamityDamage(d, 250);
        Tracking.calamityDamage(d, -5);
        Tracking.calamityKill(d, "nusmp:storm");
        Tracking.calamityKill(d, null);
        assertEquals(250.0, d.counter("calamity_damage"), 0.0);
        assertEquals(2.0, d.counter("calamity_kills"), 0.0);
        assertEquals(1, d.mapGet("calamity_kills", "nusmp:storm"));
        Tracking.walpurgis(d);
        Tracking.warKill(d);
        Tracking.warKill(d);
        Tracking.warWin(d);
        assertEquals(1.0, d.counter("walpurgis"), 0.0);
        assertEquals(2.0, d.counter("war_kills"), 0.0);
        assertEquals(1.0, d.counter("war_wins"), 0.0);
        assertTrue(Tracking.biome(d, "minecraft:plains"));
        assertFalse(Tracking.biome(d, "minecraft:plains"));
        assertTrue(Tracking.structure(d, "minecraft:mansion"));
        assertNotNull(d.toRaw());
    }
}
