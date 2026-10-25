package com.newuniverse.nusmp.mana.method;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 3.8.0: a rune writes the user's own element. The table for every MagicType, hybrids, malformed fusions and the page rule. */
class RuneOwnElementTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    /** Every MagicType name, read from the enum's source (the enum itself needs Minecraft classes the offline run does not have). */
    private static List<String> allMagicNames() throws IOException {
        String src = Files.readString(SRC.resolve("blackclover/MagicType.java"), StandardCharsets.UTF_8);
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^\\s{4}([A-Z][A-Z_0-9]*)\\(\"").matcher(src);
        while (m.find()) names.add(m.group(1));
        return names;
    }

    private static final Map<String, RuneElement> BASIC = Map.of("FLAME", RuneElement.FIRE, "WATER", RuneElement.WATER, "WIND", RuneElement.WIND, "EARTH", RuneElement.EARTH);

    @Test
    void everyMagicTypeHasAnAnswerAndOnlyTheFourBasicOnesOwnAnElement() throws IOException {
        List<String> names = allMagicNames();
        assertTrue(names.size() > 80, "the enum was read: " + names.size());
        for (String must : List.of("FLAME", "WATER", "WIND", "EARTH", "TIME", "STAR", "LIGHTNING", "PLANT", "MAGMA", "ICE", "STORM", "SEALING", "ANTI_MAGIC"))
            assertTrue(names.contains(must), must + " is a MagicType");
        for (String n : names) {
            List<RuneElement> owned = RuneOwnElement.ownedBy(n, "");
            if (BASIC.containsKey(n)) assertEquals(List.of(BASIC.get(n)), owned, n + " writes exactly its own rune");
            else assertTrue(owned.isEmpty(), n + " is not a basic element: it writes no Flame / Water / Wind / Earth rune, but owns " + owned);
        }
    }

    @Test
    void timeStarAndTheOtherNonElementalMagicsGetNoElementalPage() {
        for (String n : List.of("TIME", "STAR", "SEALING", "SPATIAL", "GRAVITY", "REINFORCEMENT", "LIGHT", "DARK", "ANTI_MAGIC", "CREATION"))
            for (String page : List.of("mana_method_fire", "mana_method_water", "mana_method_wind", "mana_method_earth"))
                assertFalse(RuneOwnElement.allowed(true, page, n, ""), n + " must not get " + page);
    }

    @Test
    void lightningAndPlantHaveTheTrueMagicPageOnlyNoBasicRunePage() {
        assertTrue(RuneOwnElement.ownedBy("LIGHTNING", "").isEmpty());
        assertTrue(RuneOwnElement.ownedBy("PLANT", "").isEmpty());
        assertNull(RuneOwnElement.basicOf("LIGHTNING"));
        assertNull(RuneOwnElement.basicOf("PLANT"));
        // consistency with True Magic: its natural set is these four plus lightning and plant
        assertEquals(RuneElement.FIRE, RuneOwnElement.basicOf("FLAME"));
        assertEquals(RuneElement.WIND, RuneOwnElement.basicOf("wind"), "the name is read without regard to case");
    }

    @Test
    void aHybridGrimoireCountsForBothOfItsElements() {
        // Glass = Fire + Earth in this mod's blends: the book's own magic is not elemental, the parents are
        assertEquals(List.of(RuneElement.FIRE, RuneElement.EARTH), RuneOwnElement.ownedBy("GLASS", "FLAME:EARTH"));
        assertEquals(List.of(RuneElement.FIRE, RuneElement.WATER), RuneOwnElement.ownedBy("FLAME", "FLAME:WATER"), "a parent equal to the book's own magic is counted once");
        assertEquals(List.of(RuneElement.WATER, RuneElement.WIND), RuneOwnElement.ownedBy("STORM", "WATER:WIND"));
        // one parent elemental, one not: only the elemental one
        assertEquals(List.of(RuneElement.WIND), RuneOwnElement.ownedBy("STAR", "STAR:WIND"));
        assertEquals(List.of(RuneElement.EARTH), RuneOwnElement.ownedBy("WORLD_TREE", "PLANT:EARTH"));
        // a hybrid of two non-elementals
        assertTrue(RuneOwnElement.ownedBy("STAR", "STAR:TIME").isEmpty());
        // and the book's own magic is added to the parents
        assertEquals(List.of(RuneElement.FIRE, RuneElement.WIND), RuneOwnElement.ownedBy("FLAME", "STAR:WIND"));
    }

    @Test
    void aBrokenFusionStringNeverCountsForAnything() {
        for (String f : new String[]{null, "", ":", "FLAME", "FLAME:", ":WATER", "FLAMEWATER", "FLAM:WAT", "flame water", "A:"})
            assertTrue(RuneOwnElement.ownedBy("TIME", f).isEmpty(), "'" + f + "' on a Time grimoire owns nothing");
        assertEquals(List.of(RuneElement.WATER), RuneOwnElement.ownedBy("TIME", "STAR:WATER"), "a well formed pair is read");
        assertEquals(List.of(RuneElement.FIRE), RuneOwnElement.ownedBy("TIME", "FLAME:WATERS"), "a longer word is not WATER; only the Flame side counts");
        assertEquals(List.of(RuneElement.FIRE, RuneElement.WATER), RuneOwnElement.ownedBy("TIME", "flame:water"), "lower case is read too");
        assertFalse(RuneOwnElement.owns(null, null, RuneElement.FIRE));
        assertFalse(RuneOwnElement.owns("FLAME", "", null));
    }

    @Test
    void theSettingOffGivesEveryGrimoireAllFourAsIn370() throws IOException {
        for (String n : allMagicNames())
            for (String page : List.of("mana_method_fire", "mana_method_water", "mana_method_wind", "mana_method_earth"))
                assertTrue(RuneOwnElement.allowed(false, page, n, ""), "ownElementOnly=false: " + n + " has " + page);
    }

    @Test
    void theSettingOnLetsOnlyTheOwnerOfTheElementCast() {
        assertTrue(RuneOwnElement.allowed(true, "mana_method_fire", "FLAME", ""));
        assertFalse(RuneOwnElement.allowed(true, "mana_method_water", "FLAME", ""));
        assertFalse(RuneOwnElement.allowed(true, "mana_method_wind", "FLAME", ""));
        assertFalse(RuneOwnElement.allowed(true, "mana_method_earth", "FLAME", ""));
        assertTrue(RuneOwnElement.allowed(true, "mana_method_water", "WATER", ""));
        assertTrue(RuneOwnElement.allowed(true, "mana_method_wind", "WIND", ""));
        assertTrue(RuneOwnElement.allowed(true, "mana_method_earth", "EARTH", ""));
        assertTrue(RuneOwnElement.allowed(true, "mana_method_fire", "GLASS", "FLAME:EARTH") && RuneOwnElement.allowed(true, "mana_method_earth", "GLASS", "FLAME:EARTH"));
        assertFalse(RuneOwnElement.allowed(true, "mana_method_water", "GLASS", "FLAME:EARTH"));
    }

    @Test
    void theOtherPagesAreNeverFilteredByElement() {
        for (String page : List.of("mana_method_true", "mana_method_air_step", "mana_method_dash", "mana_method_body", "mana_method_brush", "mana_method_state", "mana_strike", "anything", ""))
            for (String n : List.of("TIME", "STAR", "FLAME", "ANTI_MAGIC")) {
                assertNull(RuneOwnElement.pageElement(page), page + " is not an elemental page");
                assertTrue(RuneOwnElement.allowed(true, page, n, ""), n + " keeps " + page);
            }
        assertNull(RuneOwnElement.pageElement(null));
    }

    @Test
    void thePageIdsAreTheOnesTheBookRegisters() throws IOException {
        String pages = Files.readString(SRC.resolve("mana/method/ManaMethodPages.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("FIRE_ID = \"(\\w+)\", WATER_ID = \"(\\w+)\", WIND_ID = \"(\\w+)\", EARTH_ID = \"(\\w+)\"").matcher(pages);
        assertTrue(m.find(), "the four ids are declared together");
        assertEquals(RuneElement.FIRE, RuneOwnElement.pageElement(m.group(1)));
        assertEquals(RuneElement.WATER, RuneOwnElement.pageElement(m.group(2)));
        assertEquals(RuneElement.WIND, RuneOwnElement.pageElement(m.group(3)));
        assertEquals(RuneElement.EARTH, RuneOwnElement.pageElement(m.group(4)));
        assertTrue(pages.contains("STATE_ID = \"mana_method_state\""));
    }

    @Test
    void theTrueMagicNaturalListAgreesWithTheBasicElements() throws IOException {
        String rules = Files.readString(SRC.resolve("mana/zone/TrueMagicRules.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("NATURAL = Set\\.of\\(([^)]*)\\)").matcher(rules);
        assertTrue(m.find());
        String list = m.group(1);
        for (String n : BASIC.keySet()) assertTrue(list.contains("\"" + n + "\""), "True Magic's natural list names " + n);
        assertTrue(list.contains("\"LIGHTNING\"") && list.contains("\"PLANT\""), "the two that have the True Magic page but no basic rune page");
    }
}
