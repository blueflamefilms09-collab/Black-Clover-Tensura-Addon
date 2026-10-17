package com.newuniverse.nusmp.labyrinth.mega.gear;

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

/**
 * Why: the chest side is switched on by DATA, not by code: a type id in {@code data/nusmp/loot_modifiers/gear_chests.json} that must match the id the Java registers,
 * a dimension condition that must name the Mega-Dungeon's real dimension id, and an entry in the list file {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}
 * that is SHARED with the relic modifier (a careless edit that replaces the list would silently switch the relic chests off, or forget the gear chests). A typo in any
 * of the three does nothing visible: the game logs a parse error at load and no chest ever holds a piece. So the shipped files are read as the build sees them
 * and held against the Java sources: the list keeps {@code nusmp:relic_chests} and adds {@code nusmp:gear_chests} exactly once and does not replace the other
 * mods' lists; the modifier's {@code type} is the id {@link ChestLootModifier} registers; the condition names the dimension {@code Labyrinth.MEGA_DIMENSION} is built
 * from and NOT the Sanctuary's; and the shipped {@code chests} section of {@code tables.json} equals the rules the code falls back on. Working directory = project
 * root (the same assumption as the other data tests).
 */
class GearChestDataTest {
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(Path p) throws IOException {
        return (Map<String, Object>) MiniJson.parse(Files.readString(p, StandardCharsets.UTF_8));
    }

    @Test
    void theListFileKeepsTheRelicModifierAndAddsTheGearOne() throws IOException {
        Map<String, Object> list = json(DATA.resolve("neoforge/loot_modifiers/global_loot_modifiers.json"));
        assertEquals(Boolean.FALSE, list.get("replace"), "the list must not replace the lists of other mods and packs");
        List<?> entries = (List<?>) list.get("entries");
        assertEquals(1, entries.stream().filter("nusmp:relic_chests"::equals).count(), "the relic modifier must stay listed, once");
        assertEquals(1, entries.stream().filter("nusmp:gear_chests"::equals).count(), "the gear modifier must be listed, once");
        assertEquals(entries.size(), entries.stream().distinct().count(), "no duplicate entries");
        for (Object e : entries) assertTrue(Files.exists(DATA.resolve("nusmp/loot_modifiers/" + ((String) e).substring("nusmp:".length()) + ".json")), e + " names no file");
    }

    @Test
    void theModifierFileNamesTheTypeTheJavaRegistersAndTheMegaDimension() throws IOException {
        Map<String, Object> m = json(DATA.resolve("nusmp/loot_modifiers/gear_chests.json"));
        assertEquals("nusmp:gear_chests", m.get("type"));
        String java = Files.readString(JAVA.resolve("labyrinth/mega/gear/ChestLootModifier.java"), StandardCharsets.UTF_8);
        assertTrue(java.contains("MODIFIERS.register(\"gear_chests\""), "the Java registers the type under the id the JSON names");
        assertTrue(java.contains("extends LootModifier"), "the base class that reads the JSON conditions");
        List<?> conditions = (List<?>) m.get("conditions");
        assertNotNull(conditions, "LootModifier's codec requires a conditions list");
        assertEquals(1, conditions.size());
        Map<?, ?> c = (Map<?, ?>) conditions.get(0);
        assertEquals("minecraft:location_check", c.get("condition"));
        Map<?, ?> predicate = (Map<?, ?>) c.get("predicate");
        assertEquals(1, predicate.size(), "only the dimension is checked: no position, no biome");
        String labyrinth = Files.readString(JAVA.resolve("labyrinth/Labyrinth.java"), StandardCharsets.UTF_8);
        Matcher mega = Pattern.compile("MEGA_DIMENSION = ResourceKey\\.create\\(Registries\\.DIMENSION, ResourceLocation\\.fromNamespaceAndPath\\(NUSMP\\.MODID, \"([a-z_]+)\"\\)\\)").matcher(labyrinth);
        assertTrue(mega.find(), "Labyrinth.MEGA_DIMENSION is built differently now: update this test");
        assertEquals("nusmp:" + mega.group(1), predicate.get("dimension"), "the condition must name the Mega-Dungeon's dimension id");
        assertFalse(String.valueOf(predicate.get("dimension")).contains("sanctuary"), "the Hub is not part of the Mega-Dungeon");
        assertFalse(m.containsKey("neoforge:conditions"), "no optional-mod condition: nothing here depends on another mod");
    }

    @Test
    void theShippedChestsSectionIsTheCodesFallback() throws IOException {
        ChestRules shipped = GearFx.tables().chests();
        assertEquals(ChestRules.DEFAULT.treasureWords(), shipped.treasureWords());
        assertEquals(ChestRules.DEFAULT.excluded(), shipped.excluded(), "the data file adds no exclusions of its own; the two relic tables are fixed in code");
        assertEquals(ChestRules.Kind.STANDARD, shipped.classify("nusmp", "chests/labyrinth_trial"));
        assertEquals(ChestRules.Kind.NONE, shipped.classify("nusmp", "chests/dungeon_relic_vault"));
        assertEquals(ChestRules.Kind.NONE, shipped.classify("nusmp", "chests/labyrinth_pedestal"));
        Map<String, Object> file = json(GearFx.TABLES);
        assertTrue(file.containsKey("chests"), "the shipped file documents the section");
        assertTrue(((Map<?, ?>) file.get("chests")).containsKey("why"), "and says why");
    }

    @Test
    void everyChestTableOfThisModIsClassifiedOnPurpose() throws IOException {
        List<String> found = new ArrayList<>();
        try (var files = Files.list(DATA.resolve("nusmp/loot_table/chests"))) {
            files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p -> found.add(p.getFileName().toString().replace(".json", "")));
        }
        assertFalse(found.isEmpty());
        ChestRules r = GearFx.tables().chests();
        for (String f : found) {
            ChestRules.Kind k = r.classify("nusmp", "chests/" + f);
            boolean relic = f.equals("dungeon_relic_vault") || f.equals("labyrinth_pedestal");
            assertEquals(relic ? ChestRules.Kind.NONE : ChestRules.Kind.STANDARD, k, "nusmp:chests/" + f + " (a new table of this mod must be decided here: relic table or plain chest)");
        }
    }
}
