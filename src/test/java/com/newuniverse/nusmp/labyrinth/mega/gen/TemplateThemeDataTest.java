package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rooms that belong to a theme are named by PREFIXES in {@code data/nusmp/mega/template_themes/<theme>.json}, and a prefix with a typo claims nothing and fails silently
 * in the game: the theme's own rooms would simply never be drawn (or, worse, a room would stay free for every theme because its file name does not start with the claimed
 * prefix). This test reads the files that ship, from the source tree, and checks them against the templates that ship: every file names a theme of the enum and says so in its own
 * name, every prefix names a kind folder and matches at least one real template, every template whose file name starts with a theme's id is claimed by that theme's file (no
 * orphan signature room), every weight in {@code relative} belongs to a claimed prefix, a theme has at least five rooms of its own, and every claimed room is in the generated
 * catalog the assembler tests run on (so the catalog is not stale). Pure: reads files from the project root, no game types.
 */
class TemplateThemeDataTest {
    private static final Path THEMES = Path.of("src/main/resources/data/nusmp/mega/template_themes");
    private static final Path STRUCT = Path.of("src/main/resources/data/nusmp/structure");
    private static final Path CATALOG = Path.of("src/test/resources/mega/catalog.json");
    private static final Set<String> FOLDERS = Set.of("corridor", "junction", "hall", "cavern", "set", "trial", "puzzle", "trap", "treasure", "safe");

    private static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** theme id -> its parsed file, in file-name order. */
    private static Map<String, JsonObject> files() throws IOException {
        Map<String, JsonObject> out = new TreeMap<>();
        if (!Files.isDirectory(THEMES)) return out;
        try (var list = Files.list(THEMES)) {
            for (Path p : list.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                out.put(p.getFileName().toString().replace(".json", ""), read(p));
            }
        }
        return out;
    }

    /** Every template id on disk ("nusmp:mega/hall/crypt_a") under structure/mega/<folder>/. */
    private static List<String> templatesOnDisk() throws IOException {
        List<String> out = new ArrayList<>();
        Path root = STRUCT.resolve("mega");
        try (var walk = Files.walk(root)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".nbt")).sorted().toList()) {
                String rel = STRUCT.relativize(p).toString().replace('\\', '/');
                out.add("nusmp:" + rel.substring(0, rel.length() - 4));
            }
        }
        return out;
    }

    private static List<String> prefixes(JsonObject o) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("templates")) out.add(e.getAsString());
        return out;
    }

    @Test
    void everyFileNamesARealThemeAndItsOwnName() throws IOException {
        Set<String> ids = new HashSet<>();
        for (RegionTheme t : RegionTheme.values()) ids.add(t.id());
        for (Map.Entry<String, JsonObject> e : files().entrySet()) {
            assertTrue(ids.contains(e.getKey()), e.getKey() + ".json is the file of no theme of the enum");
            assertEquals(e.getKey(), e.getValue().get("theme").getAsString(), e.getKey() + ".json names another theme inside");
            assertTrue(e.getValue().has("templates") && e.getValue().get("templates").isJsonArray() && e.getValue().getAsJsonArray("templates").size() > 0, e.getKey() + ": no template prefixes");
        }
    }

    @Test
    void everyPrefixIsAKindFolderOfTheThemeAndMatchesARealTemplate() throws IOException {
        List<String> disk = templatesOnDisk();
        for (Map.Entry<String, JsonObject> e : files().entrySet()) {
            Set<String> seen = new HashSet<>();
            for (String prefix : prefixes(e.getValue())) {
                assertTrue(seen.add(prefix), e.getKey() + ": prefix listed twice: " + prefix);
                String[] parts = prefix.split("/");
                assertTrue(parts.length == 3 && parts[0].equals("nusmp:mega"), e.getKey() + ": '" + prefix + "' must look like nusmp:mega/<folder>/<theme>_");
                assertTrue(FOLDERS.contains(parts[1]), e.getKey() + ": '" + parts[1] + "' is not a kind folder a theme may fill");
                assertTrue(parts[2].startsWith(e.getKey() + "_"), e.getKey() + ": the prefix '" + prefix + "' must start with the theme id and an underscore");
                int hits = 0;
                for (String t : disk) if (t.startsWith(prefix)) hits++;
                assertTrue(hits >= 1, e.getKey() + ": the prefix '" + prefix + "' matches no template on disk");
            }
        }
    }

    @Test
    void everyTemplateNamedAfterAThemeIsClaimedByThatTheme() throws IOException {
        Map<String, JsonObject> files = files();
        for (String t : templatesOnDisk()) {
            String name = t.substring(t.lastIndexOf('/') + 1);
            for (RegionTheme theme : RegionTheme.values()) {
                if (!name.startsWith(theme.id() + "_")) continue;
                JsonObject f = files.get(theme.id());
                assertTrue(f != null, t + " is named after the theme " + theme.id() + " but the theme has no template_themes file");
                boolean claimed = false;
                for (String prefix : prefixes(f)) if (t.startsWith(prefix)) claimed = true;
                assertTrue(claimed, t + " is named after the theme " + theme.id() + " but no prefix of its file claims it (it would be free for every theme)");
            }
        }
    }

    @Test
    void everyWeightBelongsToAClaimedPrefixAndIsInRange() throws IOException {
        for (Map.Entry<String, JsonObject> e : files().entrySet()) {
            JsonObject rel = e.getValue().has("relative") ? e.getValue().getAsJsonObject("relative") : new JsonObject();
            Set<String> claimed = new HashSet<>();
            for (String prefix : prefixes(e.getValue())) claimed.add(prefix.replace("nusmp:", ""));
            for (Map.Entry<String, JsonElement> w : rel.entrySet()) {
                assertTrue(claimed.contains(w.getKey()), e.getKey() + ": the weight of '" + w.getKey() + "' names no claimed prefix (the keys have no namespace: mega/hall/<theme>_)");
                int v = w.getValue().getAsInt();
                assertTrue(v >= 1 && v <= 1000, e.getKey() + ": weight " + v + " of " + w.getKey() + " is outside 1..1000");
            }
            for (String c : claimed) assertTrue(rel.has(c), e.getKey() + ": the prefix " + c + " has no weight (it would weigh 1 against the neighbours)");
        }
    }

    @Test
    void aThemeWithRoomsOfItsOwnHasAtLeastFiveAndTheyAreInTheCatalog() throws IOException {
        JsonObject catalog = read(CATALOG).getAsJsonObject("templates");
        List<String> disk = templatesOnDisk();
        for (Map.Entry<String, JsonObject> e : files().entrySet()) {
            Set<String> own = new TreeSet<>();
            for (String t : disk) for (String prefix : prefixes(e.getValue())) if (t.startsWith(prefix)) own.add(t);
            assertTrue(own.size() >= 5, e.getKey() + " has only " + own.size() + " rooms of its own: " + own);
            for (String t : own) assertTrue(catalog.has(t), t + " is not in src/test/resources/mega/catalog.json (run tools/labyrinth/mega_catalog.py)");
        }
    }

    @Test
    void noPrefixIsClaimedByTwoThemes() throws IOException {
        Map<String, String> owner = new TreeMap<>();
        for (Map.Entry<String, JsonObject> e : files().entrySet()) {
            for (String prefix : prefixes(e.getValue())) {
                String before = owner.put(prefix, e.getKey());
                assertTrue(before == null || before.equals(e.getKey()), "prefix " + prefix + " is claimed by both " + before + " and " + e.getKey());
            }
        }
    }
}
