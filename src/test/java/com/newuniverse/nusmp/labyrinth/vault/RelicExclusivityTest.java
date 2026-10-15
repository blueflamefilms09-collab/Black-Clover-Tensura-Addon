package com.newuniverse.nusmp.labyrinth.vault;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Relics are exclusive to the Labyrinth: no recipe makes or uses one, no loot table outside the vault, the pedestal and the entity tables
 * names one, the global loot modifier that puts them in the vault is still enabled, and both code paths go through the exclusivity gate.
 * Pure: regex over the source tree (CWD = project root), no Gson, no Minecraft.
 */
class RelicExclusivityTest {
    private static final Path RELICS = Path.of("src/main/java/com/newuniverse/nusmp/relics");
    private static final Path RECIPES = Path.of("src/main/resources/data/nusmp/recipe");
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path LOOT = Path.of("src/main/resources/data/nusmp/loot_table");
    private static final Pattern DEF = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");

    private static List<Path> files(Path root, String suffix) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    /** Every relic id declared in the catalog, plus the left half of every pair (ids end in _left). */
    static TreeSet<String> relicIds() throws IOException {
        TreeSet<String> ids = new TreeSet<>();
        for (Path p : files(RELICS, ".java")) {
            Matcher m = DEF.matcher(read(p));
            while (m.find()) {
                ids.add(m.group(1));
                ids.add(m.group(1) + "_left");
            }
        }
        return ids;
    }

    private static List<String> mentions(String text, TreeSet<String> ids) {
        List<String> hit = new ArrayList<>();
        for (String id : ids) if (text.contains("\"nusmp:" + id + "\"")) hit.add(id);
        return hit;
    }

    private static String unix(Path p) { return p.toString().replace('\\', '/'); }

    @Test
    void theCatalogIsFound() throws IOException {
        assertTrue(relicIds().size() > 100, "the relic catalog should declare well over a hundred relics through RelicDef.of");
    }

    @Test
    void noRecipeMakesOrUsesARelic() throws IOException {
        TreeSet<String> ids = relicIds();
        for (Path p : files(RECIPES, ".json")) assertEquals(List.of(), mentions(read(p), ids), "a recipe names a relic: " + p);
    }

    @Test
    void noLootTableOutsideTheVaultNamesARelic() throws IOException {
        TreeSet<String> ids = relicIds();
        for (Path p : files(DATA, ".json")) {
            String u = unix(p);
            if (!u.contains("/loot_table/")) continue;
            if (u.endsWith("/loot_table/chests/dungeon_relic_vault.json") || u.endsWith("/loot_table/chests/labyrinth_pedestal.json")) continue;
            if (u.contains("/loot_table/entities/")) continue;                             // boss drops are an allowed source
            assertEquals(List.of(), mentions(read(p), ids), "a loot table outside the vault names a relic: " + p);
        }
    }

    @Test
    void theVaultTablesRollRelicsThroughTheEntryTypeAndTheTrialDoesNot() throws IOException {
        assertTrue(read(LOOT.resolve("chests/dungeon_relic_vault.json")).contains("\"type\": \"nusmp:relic\""));
        assertTrue(read(LOOT.resolve("chests/labyrinth_pedestal.json")).contains("\"type\": \"nusmp:relic\""));
        String trial = read(LOOT.resolve("chests/labyrinth_trial.json"));
        assertFalse(trial.contains("nusmp:relic"), "the trial chests are mundane");
        assertEquals(List.of(), mentions(trial, relicIds()));
    }

    @Test
    void theGlobalLootModifierIsStillEnabled() throws IOException {
        String glm = read(Path.of("src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json"));
        assertTrue(glm.contains("\"nusmp:relic_chests\""), "the vault's bonus relic comes through the modifier");
    }

    @Test
    void bothCodePathsAskTheGate() throws IOException {
        assertTrue(read(RELICS.resolve("RelicLootModifier.java")).contains("RelicExclusivity.allowsChest"));
        assertTrue(read(RELICS.resolve("RelicLoot.java")).contains("RelicExclusivity.allowsMobDrop"));
    }
}
