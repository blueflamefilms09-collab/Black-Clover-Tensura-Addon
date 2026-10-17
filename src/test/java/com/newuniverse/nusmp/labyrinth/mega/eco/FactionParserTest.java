package com.newuniverse.nusmp.labyrinth.mega.eco;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a faction file is hand-written and a datapack may retune it, so the reader must give a usable faction (or a clear list of complaints) for
 * ANY input, never an exception: the brief's own example parses cleanly, a missing field takes its default, an out-of-range number is clamped and
 * reported, a typo'd key or theme is reported instead of silently ignored, and a file that is not an object or has no usable id gives no faction.
 */
class FactionParserTest {
    private static Map<String, Object> briefExample() {
        return MiniJson.object("""
                {"id": "goblin", "name": "Goblin Warrens", "color": "#5C8A3A", "tag": "nusmp:mega/goblin", "rivals": ["ogre", "beast"], "allies": [],
                 "aggro": {"range": 14, "nativeBonus": 8}, "themeWeights": {"gravity": 3, "blood": 1, "verdant": 4},
                 "drops": {"meat": "raw_monster_meat", "meatChance": 0.5}}""");
    }

    @Test void theBriefsExampleParsesCleanly() {
        FactionParser.Result r = FactionParser.parse("goblin", briefExample());
        assertTrue(r.problems().isEmpty(), "problems: " + r.problems());
        FactionDef d = r.def();
        assertEquals("goblin", d.id());
        assertEquals("Goblin Warrens", d.name());
        assertEquals(0x5C8A3A, d.color());
        assertEquals("nusmp:mega/goblin", d.tag());
        assertEquals(List.of("ogre", "beast"), d.rivals());
        assertTrue(d.allies().isEmpty());
        assertEquals(14, d.range());
        assertEquals(8, d.nativeBonus());
        assertEquals(3.0, d.themeWeights().get("gravity").doubleValue());
        assertEquals(4.0, d.themeWeights().get("verdant").doubleValue());
        assertEquals("raw_monster_meat", d.meat());
        assertEquals(0.5, d.meatChance());
        assertFalse(d.territorial());
        assertFalse(d.oneWay());
    }

    @Test void anEmptyObjectTakesEveryDefault() {
        FactionParser.Result r = FactionParser.parse("ogre", Map.of());
        assertTrue(r.problems().isEmpty(), "problems: " + r.problems());
        FactionDef d = r.def();
        assertEquals("Ogre", d.name());
        assertEquals("nusmp:mega/ogre", d.tag());
        assertEquals(FactionParser.DEFAULT_COLOR, d.color());
        assertEquals(FactionDef.UNSET, d.range());
        assertEquals(FactionDef.UNSET, d.nativeBonus());
        assertTrue(d.themeWeights().isEmpty());
        assertEquals(FactionParser.DEFAULT_CUT, d.meat());
        assertEquals(FactionParser.DEFAULT_MEAT_CHANCE, d.meatChance());
    }

    @Test void numbersAreClampedAndReported() {
        Map<String, Object> j = MiniJson.object("""
                {"aggro": {"range": 999, "nativeBonus": -4}, "themeWeights": {"gravity": 5000, "blood": -2}, "drops": {"meat": "raw_slime_jelly", "meatChance": 7}}""");
        FactionParser.Result r = FactionParser.parse("slime", j);
        FactionDef d = r.def();
        assertEquals(FactionDef.MAX_RANGE, d.range());
        assertEquals(0, d.nativeBonus());
        assertEquals(FactionParser.MAX_WEIGHT, d.themeWeights().get("gravity").doubleValue());
        assertEquals(0.0, d.themeWeights().get("blood").doubleValue());
        assertEquals(1.0, d.meatChance());
        assertEquals("raw_slime_jelly", d.meat());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("aggro.range")), "range clamp should be reported: " + r.problems());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("aggro.nativeBonus")), "bonus clamp should be reported: " + r.problems());
    }

    @Test void typosAreReportedNotSwallowed() {
        Map<String, Object> j = MiniJson.object("""
                {"rival": ["x"], "color": "green", "tag": "NoNamespace", "themeWeights": {"lava": 2}, "drops": {"meat": "steak"}, "rivals": ["undead", "undead", "ogre", "ogre"]}""");
        FactionParser.Result r = FactionParser.parse("ogre", j);
        String all = String.join("\n", r.problems());
        assertTrue(all.contains("unknown key 'rival'"), all);
        assertTrue(all.contains("color 'green'"), all);
        assertTrue(all.contains("tag 'NoNamespace'"), all);
        assertTrue(all.contains("unknown theme 'lava'"), all);
        assertTrue(all.contains("drops.meat 'steak'"), all);
        assertTrue(all.contains("lists the faction itself"), all);
        assertEquals(List.of("undead"), r.def().rivals(), "duplicates and the faction itself are dropped");
        assertEquals("nusmp:mega/ogre", r.def().tag());
        assertEquals(FactionParser.DEFAULT_COLOR, r.def().color());
        assertEquals(FactionParser.DEFAULT_CUT, r.def().meat());
        assertTrue(r.def().themeWeights().isEmpty());
    }

    @Test void wrongTypesAreReportedAndDefaulted() {
        Map<String, Object> j = MiniJson.object("""
                {"name": 5, "rivals": "ogre", "aggro": 3, "themeWeights": [1], "drops": "all", "territorial": "yes", "oneWay": 1}""");
        FactionParser.Result r = FactionParser.parse("cultist", j);
        assertEquals(7, r.problems().size(), "one complaint per bad field: " + r.problems());
        FactionDef d = r.def();
        assertEquals("Cultist", d.name());
        assertTrue(d.rivals().isEmpty());
        assertFalse(d.territorial());
        assertFalse(d.oneWay());
    }

    @Test void theFlagsAreRead() {
        FactionDef d = FactionParser.parse("construct", MiniJson.object("{\"territorial\": true, \"oneWay\": true}")).def();
        assertTrue(d.territorial());
        assertTrue(d.oneWay());
    }

    @Test void aFileThatIsNotAnObjectGivesNoFaction() {
        assertNull(FactionParser.parse("goblin", null).def());
        assertFalse(FactionParser.parse("goblin", null).problems().isEmpty());
    }

    @Test void aBadFileNameGivesNoFaction() {
        for (String bad : new String[] {"", "Goblin Warrens", "9lives", "gob-lin", null}) {
            FactionParser.Result r = FactionParser.parse(bad, Map.of());
            assertNull(r.def(), "'" + bad + "' is not a usable id");
            assertFalse(r.problems().isEmpty());
        }
        assertTrue(FactionParser.validId("goblin"));
        assertTrue(FactionParser.validId("deep_one2"));
        assertFalse(FactionParser.validId("Goblin"));
    }

    @Test void theFileNameWinsOverADeclaredId() {
        FactionParser.Result r = FactionParser.parse("beast", MiniJson.object("{\"id\": \"wolf\"}"));
        assertEquals("beast", r.def().id());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("file name wins")));
    }

    @Test void colourForms() {
        assertEquals(0x00FF7F, FactionParser.parseColor("#00FF7F"));
        assertEquals(0x00FF7F, FactionParser.parseColor("00ff7f"));
        assertEquals(-1, FactionParser.parseColor("#FFF"));
        assertEquals(-1, FactionParser.parseColor("#-12345"));
        assertEquals(-1, FactionParser.parseColor("#GGGGGG"));
    }

    @Test void cutNamesMapToTheEconomyKinds() {
        assertEquals(0, FactionDef.cutKindOf("raw_slime_jelly"));
        assertEquals(1, FactionDef.cutKindOf("raw_armor_flank"));
        assertEquals(2, FactionDef.cutKindOf("raw_basilisk_fillet"));
        assertEquals(3, FactionDef.cutKindOf("nusmp:raw_direwolf_haunch"));
        assertEquals(4, FactionDef.cutKindOf("raw_monster_meat"));
        assertEquals(4, FactionDef.cutKindOf("steak"));
        assertEquals(4, FactionDef.cutKindOf(null));
        assertEquals(FactionDef.CUTS.size(), 5);
    }
}
