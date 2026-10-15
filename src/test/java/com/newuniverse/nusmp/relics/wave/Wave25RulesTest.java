package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The maths of the wave 25 relics: rocks, saves as chances, the Corpse Mask's faces, the Witch's beasts, the Death Knight's days (relics4/doc_wave25.md). */
class Wave25RulesTest {
    @Test
    void savesAreD20PlusBonusAgainstDc() {
        assertEquals(0.40, Wave25Pure.saveChance(15, 2), 1e-9);
        assertEquals(0.55, Wave25Pure.saveChance(12, 2), 1e-9);
        assertEquals(0.15, Wave25Pure.saveChance(20, 2), 1e-9);
        assertEquals(0.0, Wave25Pure.saveChance(40, 2), 1e-9);
        assertEquals(1.0, Wave25Pure.saveChance(2, 2), 1e-9);
        assertTrue(Wave25Pure.saves(12, 15, 2), "face 13 + 2 = 15");
        assertFalse(Wave25Pure.saves(11, 15, 2));
        assertEquals(0.2025, Wave25Pure.withDisadvantage(0.45), 1e-9);
    }

    @Test
    void aCurseNeverTakesTheLastHealthOrMoreThanItsCap() {
        assertEquals(6f, Wave25Pure.selfHurt(30f, 20f, 20f, 0.30), 1e-6);
        assertEquals(2f, Wave25Pure.selfHurt(2f, 20f, 20f, 0.30), 1e-6);
        assertEquals(2f, Wave25Pure.selfHurt(30f, 3f, 20f, 0.30), 1e-6, "three health left: two can be taken");
        assertEquals(0f, Wave25Pure.selfHurt(30f, 1f, 20f, 0.30), 1e-6);
    }

    @Test
    void rocksFeedAndOnlyRocks() {
        assertEquals(1.0, Wave25Pure.rockNutrition("minecraft:cobblestone"), 1e-9);
        assertEquals(1.0, Wave25Pure.rockNutrition("cobbled_deepslate"), 1e-9);
        assertEquals(0.5, Wave25Pure.rockNutrition("minecraft:gravel"), 1e-9);
        assertEquals(0.0, Wave25Pure.rockNutrition("minecraft:diamond"), 1e-9);
        assertEquals(0.0, Wave25Pure.rockNutrition("minecraft:raw_iron"), 1e-9, "ore is kept");
        assertEquals(0.0, Wave25Pure.rockNutrition("othermod:cobblestone"), 1e-9);
        assertEquals(0.0, Wave25Pure.rockNutrition(null), 1e-9);
        assertEquals(3, Wave25Pure.itemsToEat(1.0, 64, 8, 3.0));
        assertEquals(8, Wave25Pure.itemsToEat(1.0, 64, 8, 20.0), "at most eight a meal");
        assertEquals(2, Wave25Pure.itemsToEat(1.0, 2, 8, 20.0), "at most the stack");
        assertEquals(0, Wave25Pure.itemsToEat(1.0, 64, 8, 0.0));
        assertEquals(0, Wave25Pure.itemsToEat(0.0, 64, 8, 5.0));
    }

    @Test
    void mimicsAreFoundByIdAndTheBiteIsANaturalOne() {
        assertTrue(Wave25Pure.isMimicId("mimic:mimic"));
        assertTrue(Wave25Pure.isMimicId("tensura:cave_mimic"));
        assertFalse(Wave25Pure.isMimicId("minecraft:zombie"));
        assertFalse(Wave25Pure.isMimicId(null));
        assertTrue(Wave25Pure.fumble(0));
        assertFalse(Wave25Pure.fumble(1));
        assertFalse(Wave25Pure.fumble(19));
    }

    @Test
    void theLyreHitsOnlyHumanoidsAndCelestials() {
        assertTrue(Wave25Pure.hearsLyre("minecraft:pillager"));
        assertTrue(Wave25Pure.hearsLyre("tensura:goblin"));
        assertTrue(Wave25Pure.hearsLyre("tensura:holy_angel"));
        assertFalse(Wave25Pure.hearsLyre("minecraft:creeper"));
        assertFalse(Wave25Pure.hearsLyre("minecraft:spider"));
        assertTrue(Wave25Pure.isCelestialId("x:seraph_guard"));
        assertEquals(14f, Wave25Pure.lyreDamage(14, false), 1e-6);
        assertEquals(7f, Wave25Pure.lyreDamage(14, true), 1e-6);
        assertEquals(1f, Wave25Pure.lyreDamage(1, true), 1e-6);
        assertEquals(1f, Wave25Pure.lyreDamage(0, false), 1e-6);
    }

    @Test
    void theWitchHasEightBeastsAndCyclesThroughThem() {
        assertEquals(8, Wave25Pure.Form.values().length);
        assertEquals("Cat", Wave25Pure.Form.of(0).label());
        assertEquals("Goat", Wave25Pure.Form.of(7).label());
        assertEquals("Cat", Wave25Pure.Form.of(8).label(), "wraps");
        assertEquals("Goat", Wave25Pure.Form.of(-1).label());
        assertEquals(1, Wave25Pure.formAfter(0));
        assertEquals(0, Wave25Pure.formAfter(7));
        assertTrue(Wave25Pure.Form.SPIDER.poisonBite());
        assertFalse(Wave25Pure.Form.CAT.poisonBite());
        for (Wave25Pure.Form f : Wave25Pure.Form.values()) {
            assertTrue(f.effects().length >= 2, f.label());
            for (String e : f.effects()) assertTrue(e.matches("[a-z_]+:[a-z_]+:\\d"), e);
        }
        assertEquals(3, Wave25Pure.rollForm(n -> 3));
        assertEquals(0, Wave25Pure.allowanceAfter(10, 20));
        assertEquals(80, Wave25Pure.allowanceAfter(100, 20));
        assertEquals(100, Wave25Pure.allowanceAfter(100, -5));
    }

    @Test
    void theMaskKeepsFiveFacesAndReplacesTheOldestAfterTheGap() {
        List<String> f = new ArrayList<>();
        long now = 0;
        for (int i = 0; i < 5; i++) f = Wave25Pure.storeFace(f, "minecraft:zombie|Z" + i, now, Long.MIN_VALUE / 2, 5, 1200);
        assertEquals(5, f.size());
        assertEquals(f, Wave25Pure.storeFace(f, "minecraft:zombie|New", 10, 0, 5, 1200), "full, and too soon: nothing changes");
        List<String> g = Wave25Pure.storeFace(f, "minecraft:zombie|New", 1300, 0, 5, 1200);
        assertEquals(5, g.size());
        assertEquals("minecraft:zombie|New", g.get(4));
        assertFalse(g.contains("minecraft:zombie|Z0"), "the oldest went");
        List<String> h = Wave25Pure.storeFace(f, "minecraft:zombie|Z1", 1, 0, 5, 1200);
        assertEquals("minecraft:zombie|Z1", h.get(4), "the same face seen again moves to the front");
        assertEquals(5, h.size());
        assertEquals(5, f.size(), "the input list is untouched");
        assertEquals("minecraft:villager", Wave25Pure.faceType("minecraft:villager|Bob"));
        assertEquals("Bob", Wave25Pure.faceName("minecraft:villager|Bob"));
        assertEquals("minecraft:villager", Wave25Pure.faceName("minecraft:villager"));
        assertEquals(1, Wave25Pure.nextFace(0, 3));
        assertEquals(0, Wave25Pure.nextFace(2, 3));
        assertEquals(0, Wave25Pure.nextFace(5, 0));
        assertTrue(Wave25Pure.isHumanoidId("minecraft:player"));
        assertFalse(Wave25Pure.isHumanoidId("minecraft:cow"));
    }

    @Test
    void theDeathKnightsDcGrowsByTwoAndItsBonusStopsAtFour() {
        assertEquals(12, Wave25Pure.helmetDc(1));
        assertEquals(14, Wave25Pure.helmetDc(2));
        assertEquals(20, Wave25Pure.helmetDc(5));
        int con = 0;
        for (int d = 0; d < 9; d++) con = Wave25Pure.conAfterDay(con);
        assertEquals(4, con);
        assertEquals(1, Wave25Pure.conAfterDay(0));
        assertEquals(1, Wave25Pure.conAfterDay(-3));
        assertEquals(2, Wave25Pure.dice(2, 8, n -> 0), "2d8 at its lowest");
        assertEquals(16, Wave25Pure.curseDamage(1, n -> n - 1), "2d8 at its highest");
        assertEquals(32, Wave25Pure.curseDamage(2, n -> n - 1), "the second failure is 4d8");
        assertEquals(2, Wave25Pure.curseDamage(0, n -> 0), "stage 0 counts as the first (2d8)");
        assertFalse(Wave25Pure.helmetLapsed(10, 11));
        assertFalse(Wave25Pure.helmetLapsed(10, 10));
        assertTrue(Wave25Pure.helmetLapsed(10, 12));
    }

    @Test
    void theRosesDawnGainIsTwoD6PlusTwo() {
        assertEquals(4, Wave25Pure.dawnGain(n -> 0));
        assertEquals(14, Wave25Pure.dawnGain(n -> n - 1));
    }

    @Test
    void theSpoonsDisadvantageEffectsAndTheBucketsThoughts() {
        assertTrue(Wave25Pure.isDisadvantage("minecraft:weakness"));
        assertTrue(Wave25Pure.isDisadvantage("tensura:fear"));
        assertFalse(Wave25Pure.isDisadvantage("minecraft:speed"));
        assertFalse(Wave25Pure.isDisadvantage(null));
        assertTrue(Wave25Pure.thought("minecraft:zombie", 0).contains("Brains"));
        assertFalse(Wave25Pure.thought("x:unknown_thing", -7).isEmpty(), "negative picks still land inside the table");
        assertEquals("moments", Wave25Pure.ageText(100));
        assertEquals("2 minutes", Wave25Pure.ageText(2400));
        assertEquals("1 day", Wave25Pure.ageText(24000));
    }

    @Test
    void everyRelicIdAndFlagIsUnique() {
        String[] ids = {Wave25Tune.ID_ROCK, Wave25Tune.ID_GAME, Wave25Tune.ID_MIMIC, Wave25Tune.ID_ROSE, Wave25Tune.ID_LYRE, Wave25Tune.ID_SPOON,
                Wave25Tune.ID_LANTERN, Wave25Tune.ID_FORGOTTEN, Wave25Tune.ID_WITCH, Wave25Tune.ID_BUCKET, Wave25Tune.ID_MASK, Wave25Tune.ID_DKH};
        assertEquals(12, java.util.Set.of(ids).size());
        for (String id : ids) assertTrue(id.matches("[a-z0-9_]+"), id);
        String[] flags = {Wave25Tune.F_ROCK, Wave25Tune.F_GAME, Wave25Tune.F_MIMIC, Wave25Tune.F_ROSE, Wave25Tune.F_SPOON, Wave25Tune.F_LANTERN,
                Wave25Tune.F_FORGOTTEN, Wave25Tune.F_WITCH, Wave25Tune.F_BUCKET, Wave25Tune.F_MASK, Wave25Tune.F_DKH};
        assertEquals(11, java.util.Set.of(flags).size());
    }
}
