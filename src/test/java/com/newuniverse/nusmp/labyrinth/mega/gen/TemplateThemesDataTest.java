package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a theme's signature rooms are tied to it by NAME (a file prefix in {@code data/nusmp/mega/template_themes/<theme>.json}), and a name is the kind of
 * link nobody notices breaking: a room renamed in its generator, a prefix with a typo, a theme file for a theme that has no palette, a room that no pool
 * lists (so it is never drawn at all) or one that two themes both claim would all be silent in the game and wrong. This test reads the files that ship, from
 * the source tree, and holds them to the rules of {@code docs/labyrinth/mega_v2.md} section 5.1 and the builders' brief: every theme file belongs to a theme
 * file of {@code themes/}, names prefixes that really match templates (and only rooms of its own theme), covers every template that carries its name, holds
 * at least five signature rooms, weighs them with positive numbers that address real templates, and every one of those rooms is listed by a pool. Pure: it
 * reads files from the project directory and uses no game class.
 */
class TemplateThemesDataTest {
    private static final Path THEME_FILES = Path.of("src/main/resources/data/nusmp/mega/template_themes");
    private static final Path THEMES = Path.of("src/main/resources/data/nusmp/mega/themes");
    private static final Path STRUCTURE = Path.of("src/main/resources/data/nusmp/structure/mega");
    private static final Path POOLS = Path.of("src/main/resources/data/nusmp/worldgen/template_pool/mega");
    private static final String NAMESPACE = "nusmp:";
    private static final String PREFIX = NAMESPACE + "mega/";

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + p.toAbsolutePath() + " (run the tests from the project directory): " + e.getMessage());
        }
    }

    /** Every template id of the structure folders, as {@code mega/<folder>/<name>} (no namespace, no extension). */
    private static Set<String> allTemplates() {
        Set<String> out = new TreeSet<>();
        try (Stream<Path> folders = Files.list(STRUCTURE)) {
            for (Path folder : (Iterable<Path>) folders.filter(Files::isDirectory)::iterator) {
                try (Stream<Path> files = Files.list(folder)) {
                    for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".nbt"))::iterator) {
                        String name = f.getFileName().toString();
                        out.add("mega/" + folder.getFileName() + "/" + name.substring(0, name.length() - 4));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot list " + STRUCTURE + ": " + e.getMessage());
        }
        return out;
    }

    /** The theme files, theme id (the file name) to the parsed object. */
    private static Map<String, JsonObject> themeFiles() {
        Map<String, JsonObject> out = new TreeMap<>();
        try (Stream<Path> files = Files.list(THEME_FILES)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".json"))::iterator) {
                String name = f.getFileName().toString();
                out.put(name.substring(0, name.length() - 5), JsonParser.parseString(read(f)).getAsJsonObject());
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot list " + THEME_FILES + ": " + e.getMessage());
        }
        return out;
    }

    private static List<String> prefixes(JsonObject o) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("templates")) out.add(e.getAsString());
        return out;
    }

    /** The template ids (no namespace) a theme file claims: every template whose id starts with one of its prefixes. */
    private static Set<String> claimed(JsonObject o, Set<String> all) {
        Set<String> out = new TreeSet<>();
        for (String p : prefixes(o)) {
            String bare = p.substring(NAMESPACE.length());
            for (String t : all) if (t.startsWith(bare)) out.add(t);
        }
        return out;
    }

    private static Set<String> pooled() {
        Set<String> out = new HashSet<>();
        try (Stream<Path> files = Files.list(POOLS)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".json"))::iterator) {
                JsonObject o = JsonParser.parseString(read(f)).getAsJsonObject();
                for (JsonElement e : o.getAsJsonArray("elements")) {
                    JsonObject el = e.getAsJsonObject().getAsJsonObject("element");
                    if (el.has("location")) out.add(el.get("location").getAsString().substring(NAMESPACE.length()));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot list " + POOLS + ": " + e.getMessage());
        }
        return out;
    }

    @Test
    void everyThemeFileIsWellFormedAndBelongsToATheme() {
        Map<String, JsonObject> files = themeFiles();
        assertFalse(files.isEmpty(), "no template theme file ships");
        for (Map.Entry<String, JsonObject> e : files.entrySet()) {
            JsonObject o = e.getValue();
            String id = e.getKey();
            assertEquals(id, o.get("theme").getAsString(), id + ": the 'theme' field must be the file name");
            assertTrue(Files.exists(THEMES.resolve(id + ".json")), id + ": there is no data/nusmp/mega/themes/" + id + ".json for this theme");
            assertTrue(o.has("templates") && o.get("templates").isJsonArray() && !o.getAsJsonArray("templates").isEmpty(), id + ": 'templates' must list prefixes");
            for (String p : prefixes(o)) {
                assertTrue(p.startsWith(PREFIX), id + ": prefix '" + p + "' must start with " + PREFIX);
                assertTrue(p.endsWith("/" + id + "_"), id + ": prefix '" + p + "' must be the room name of this theme: nusmp:mega/<folder>/" + id + "_");
            }
            assertTrue(o.has("relative") && o.get("relative").isJsonObject(), id + ": 'relative' (the weights) is missing");
        }
    }

    @Test
    void everyPrefixMatchesRealTemplatesAndOnlyRoomsOfItsOwnTheme() {
        Set<String> all = allTemplates();
        for (Map.Entry<String, JsonObject> e : themeFiles().entrySet()) {
            String id = e.getKey();
            for (String p : prefixes(e.getValue())) {
                String bare = p.substring(NAMESPACE.length());
                int hits = 0;
                for (String t : all) {
                    if (!t.startsWith(bare)) continue;
                    hits++;
                    String name = t.substring(t.lastIndexOf('/') + 1);
                    assertTrue(name.startsWith(id + "_"), id + ": prefix '" + p + "' also catches '" + t + "', which is not a room of this theme");
                }
                assertTrue(hits >= 1, id + ": prefix '" + p + "' matches no template (a typo, or a room that was renamed)");
            }
        }
    }

    @Test
    void everyRoomThatCarriesAThemesNameIsCoveredByItsFileAndNoRoomIsClaimedTwice() {
        Set<String> all = allTemplates();
        Map<String, JsonObject> files = themeFiles();
        Map<String, String> owner = new TreeMap<>();
        for (Map.Entry<String, JsonObject> e : files.entrySet()) {
            for (String t : claimed(e.getValue(), all)) {
                String before = owner.put(t, e.getKey());
                assertTrue(before == null, t + " is claimed by both " + before + " and " + e.getKey());
            }
        }
        for (String t : all) {
            String name = t.substring(t.lastIndexOf('/') + 1);
            for (String id : files.keySet()) {
                if (name.startsWith(id + "_")) assertEquals(id, owner.get(t), t + " is named after the theme " + id + " but its file does not claim it (add the prefix of its folder)");
            }
        }
    }

    @Test
    void everyThemeHasAtLeastFiveSignatureRoomsInAtLeastThreeKinds() {
        Set<String> all = allTemplates();
        for (Map.Entry<String, JsonObject> e : themeFiles().entrySet()) {
            Set<String> rooms = claimed(e.getValue(), all);
            assertTrue(rooms.size() >= 5, e.getKey() + " has only " + rooms.size() + " signature rooms: " + rooms);
            Set<String> kinds = new TreeSet<>();
            for (String t : rooms) kinds.add(t.split("/")[1]);
            assertTrue(kinds.size() >= 3, e.getKey() + " has rooms of only " + kinds + ": a hall, a set piece and a danger or calm room are the minimum");
        }
    }

    @Test
    void theWeightsAreWholePositiveNumbersThatAddressRealRooms() {
        Set<String> all = allTemplates();
        for (Map.Entry<String, JsonObject> e : themeFiles().entrySet()) {
            JsonObject rel = e.getValue().getAsJsonObject("relative");
            assertFalse(rel.entrySet().isEmpty(), e.getKey() + ": no weights");
            for (Map.Entry<String, JsonElement> w : rel.entrySet()) {
                assertTrue(w.getValue().isJsonPrimitive() && w.getValue().getAsJsonPrimitive().isNumber(), e.getKey() + ": weight of " + w.getKey() + " is not a number");
                double v = w.getValue().getAsDouble();
                assertTrue(v >= 1 && v == Math.rint(v) && v <= 1000, e.getKey() + ": weight of " + w.getKey() + " is " + v);
                assertTrue(w.getKey().startsWith("mega/"), e.getKey() + ": weight key '" + w.getKey() + "' must look like mega/<folder>/<name>");
                boolean any = false;
                for (String t : all) if (t.startsWith(w.getKey())) any = true;
                assertTrue(any, e.getKey() + ": weight key '" + w.getKey() + "' matches no template");
                assertTrue(w.getKey().contains("/" + e.getKey() + "_"), e.getKey() + ": weight key '" + w.getKey() + "' addresses a room of another theme");
            }
        }
    }

    @Test
    void everySignatureRoomIsListedByAPoolSoItCanBeDrawnAtAll() {
        Set<String> all = allTemplates();
        Set<String> pooled = pooled();
        List<String> orphans = new ArrayList<>();
        for (Map.Entry<String, JsonObject> e : themeFiles().entrySet()) for (String t : claimed(e.getValue(), all)) if (!pooled.contains(t)) orphans.add(t);
        assertEquals(List.of(), orphans, "rooms that no pool lists (run tools/labyrinth/gen_mega_pools.py): they would never be drawn");
    }

    @Test
    void theFilesArePlainAsciiAndNameSeveralKindsOfRoom() {
        for (Map.Entry<String, JsonObject> e : themeFiles().entrySet()) {
            String raw = read(THEME_FILES.resolve(e.getKey() + ".json"));
            for (char c : raw.toCharArray()) assertTrue(c < 128, e.getKey() + ": the shipped data stays plain ASCII");
            JsonArray templates = e.getValue().getAsJsonArray("templates");
            assertTrue(templates.size() >= 2, e.getKey() + " names only " + templates.size() + " kinds of room");
        }
    }
}
