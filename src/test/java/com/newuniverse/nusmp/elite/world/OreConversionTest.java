package com.newuniverse.nusmp.elite.world;

import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.world.rules.MobTouch;
import com.newuniverse.nusmp.elite.world.rules.OreConversion;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** The ore conversion table and the per-chunk budget, and the mob replacement chance, tier and table. */
class OreConversionTest {
    private static String res(String path) throws Exception {
        try (var in = OreConversionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is shipped");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void theShippedMapParsesAndFindsOres() throws Exception {
        OreConversion.Table t = new OreConversion.Table();
        int n = t.add(JsonParser.parseString(res("/data/nusmp/elite/world/ore_conversion.json")).getAsJsonObject());
        assertTrue(n >= 10, "ore ids set: " + n);
        OreConversion.Entry iron = t.lookup("minecraft:iron_ore");
        assertNotNull(iron);
        assertEquals("tensura:magic_ore", iron.to().get(0).block());
        assertEquals("tensura:deepslate_magic_ore", t.lookup("minecraft:deepslate_iron_ore").to().get(0).block());
        assertNull(t.lookup("minecraft:stone"));
        assertNull(t.lookup(null));
        assertTrue(iron.fits(0.75, 10));
        assertFalse(iron.fits(0.5, 10), "under the density threshold");
    }

    @Test
    void pickIsWeightedAndSkipsMissingTargets() throws Exception {
        OreConversion.Table t = new OreConversion.Table();
        t.add(JsonParser.parseString(res("/data/nusmp/elite/world/ore_conversion.json")).getAsJsonObject());
        OreConversion.Entry gold = t.lookup("minecraft:gold_ore");
        assertEquals("tensura:silver_ore", OreConversion.pick(gold, 0.0, id -> true));
        assertEquals("tensura:silver_ore", OreConversion.pick(gold, 0.60, id -> true), "2 of 3 weight is silver");
        assertEquals("tensura:magic_ore", OreConversion.pick(gold, 0.70, id -> true));
        assertEquals("tensura:magic_ore", OreConversion.pick(gold, 0.0, id -> id.equals("tensura:magic_ore")), "silver missing: magic ore");
        assertNull(OreConversion.pick(gold, 0.5, id -> false), "no target exists");
        assertNull(OreConversion.pick(null, 0.5, id -> true));
        assertEquals("tensura:magic_ore", OreConversion.pick(gold, 5.0, id -> id.equals("tensura:magic_ore")), "a roll out of range is clamped");
    }

    @Test
    void aLaterFileReplacesAnOre() {
        OreConversion.Table t = new OreConversion.Table();
        t.add(JsonParser.parseString("{\"conversions\":[{\"from\":[\"a:ore\"],\"to\":[\"b:one\"]}]}").getAsJsonObject());
        t.add(JsonParser.parseString("{\"conversions\":[{\"from\":\"a:ore\",\"to\":[{\"block\":\"b:two\",\"weight\":4}],\"minDensity\":0.5}]}").getAsJsonObject());
        assertEquals("b:two", t.lookup("a:ore").to().get(0).block());
        assertEquals(0.5, t.lookup("a:ore").minDensity(), 1e-9);
    }

    @Test
    void brokenEntriesAreSkippedNotFatal() {
        OreConversion.Table t = new OreConversion.Table();
        assertEquals(0, t.add(JsonParser.parseString("{}").getAsJsonObject()));
        assertEquals(0, t.add(JsonParser.parseString("{\"conversions\":[{\"from\":[],\"to\":[\"x:y\"]},{\"from\":[\"a:b\"],\"to\":[]},5]}").getAsJsonObject()));
        assertTrue(t.isEmpty());
        assertEquals(0, t.add(null));
    }

    @Test
    void budgetLimitsPerWindowAndInTotal() {
        OreConversion.Budget b = new OreConversion.Budget();
        assertTrue(b.tryTake(1000, 100, 2, 5));
        assertTrue(b.tryTake(1010, 100, 2, 5));
        assertFalse(b.tryTake(1020, 100, 2, 5), "two per window");
        assertTrue(b.tryTake(1100, 100, 2, 5), "a new window");
        assertTrue(b.tryTake(1100, 100, 2, 5));
        assertFalse(b.tryTake(1100, 100, 2, 5));
        assertTrue(b.tryTake(1300, 100, 2, 5), "the fifth");
        assertFalse(b.tryTake(1500, 100, 2, 5), "the lifetime cap of 5 is reached");
        assertEquals(5, b.total);
        assertEquals(0, b.lifetimeLeft(5));
    }

    @Test
    void budgetOfZeroNeverConvertsAndAClockThatWentBackResetsTheWindow() {
        assertFalse(new OreConversion.Budget().tryTake(5, 100, 0, 5));
        assertFalse(new OreConversion.Budget().tryTake(5, 100, 1, 0));
        OreConversion.Budget b = new OreConversion.Budget(5000, 1, 1);
        assertTrue(b.tryTake(100, 100, 1, 10), "now < windowStart: the window restarts");
    }

    // ---------------------------------------------------------------- mob touch

    @Test
    void touchChanceIsZeroUnderTheThresholdAndCapped() {
        assertEquals(0.0, MobTouch.chance(0.5, 0.6, 0.08, 0.25), 1e-9);
        assertEquals(0.08, MobTouch.chance(0.6, 0.6, 0.08, 0.25), 1e-9);
        assertEquals(0.25, MobTouch.chance(1.0, 0.6, 0.08, 0.25), 1e-9, "base * 4 = 0.32 capped to 0.25");
        assertEquals(0.0, MobTouch.chance(1.0, 0.6, 0.0, 0.25), 1e-9);
        assertEquals(0.0, MobTouch.chance(1.0, 0.6, 0.08, 0.0), 1e-9);
        assertEquals(0.0, MobTouch.chance(1.0, 1.0, 0.08, 0.25), 1e-9, "a threshold of 1 switches it off");
    }

    @Test
    void tiersRiseWithDensity() {
        assertEquals(0, MobTouch.tier(0.5, 0.6));
        assertEquals(1, MobTouch.tier(0.6, 0.6));
        assertEquals(2, MobTouch.tier(0.85, 0.6));
        assertEquals(3, MobTouch.tier(0.95, 0.6));
    }

    @Test
    void theShippedMobTableAllowsVanillaMonstersOnly() throws Exception {
        MobTouch.Table t = new MobTouch.Table();
        t.add(JsonParser.parseString(res("/data/nusmp/elite/world/mob_touch.json")).getAsJsonObject());
        assertTrue(t.allows("minecraft:zombie"));
        assertFalse(t.allows("minecraft:cow"));
        assertFalse(t.allows("minecraft:ender_dragon"));
        assertFalse(t.allows("tensura:some_boss"));
        assertFalse(t.allows(null));
        assertEquals(3, t.tiers.size());
        assertTrue(t.stats(1).health() < t.stats(3).health());
        assertEquals(t.stats(3), t.stats(9), "a tier over the table uses the last row");
        assertEquals(t.stats(1), t.stats(0), "a tier under 1 uses the first row");
        t.add(JsonParser.parseString("{\"exclude\":[\"minecraft:zombie\"]}").getAsJsonObject());
        assertFalse(t.allows("minecraft:zombie"), "exclude beats eligible");
    }
}
