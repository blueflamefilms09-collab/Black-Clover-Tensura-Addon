package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.gen.TemplateThemes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the four themes of the good road (the Holy Church, the Celestial Court, the Sky Citadel and the Mountain Monastery) are four sets of DATA that nobody can
 * play, and the part that is easiest to get silently wrong is the glue between the signature rooms and the sectors: a room whose file name does not start with a
 * prefix of its theme's {@code template_themes} file is a room every theme draws (so the "signature" room turns up in a graveyard), a prefix that matches no file
 * is a typo that restricts nothing, and a room that the pool generator never saw is never drawn at all. This test reads the files that ship, from the source
 * tree, exactly as the pool generator and the game's data loader do: the file of each theme must name its theme, every prefix must be {@code nusmp:mega/<folder>/<theme>_},
 * every prefix must match real template files, every template of the theme must be claimed by a prefix, the weights must be whole numbers the pool maths accepts,
 * the theme must have the rooms the brief asks for (five at least, two halls, a set piece, a danger room, a calm room), the generated pool files must hold them, and
 * the rule of {@link TemplateThemes}, loaded from the same files, must allow them in their own theme only. Pure: reads files from the project root.
 */
class GoodRoadThemesDataTest {
    private static final List<String> THEMES = List.of("holy_church", "celestial_court", "sky_citadel", "mountain_monastery");
    private static final Path DATA = Path.of("src/main/resources/data/nusmp");
    private static final Path STRUCT = DATA.resolve("structure/mega");
    private static final Path TEMPLATE_THEMES = DATA.resolve("mega/template_themes");
    private static final Path POOLS = DATA.resolve("worldgen/template_pool/mega");
    private static final Set<String> DANGER = Set.of("trap", "trial", "puzzle");
    private static final Set<String> CALM = Set.of("safe", "treasure");

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + p.toAbsolutePath() + " (run the tests from the project directory): " + e.getMessage());
        }
    }

    private static JsonObject themeFile(String theme) {
        Path p = TEMPLATE_THEMES.resolve(theme + ".json");
        assertTrue(Files.exists(p), "missing data/nusmp/mega/template_themes/" + theme + ".json");
        return JsonParser.parseString(read(p)).getAsJsonObject();
    }

    /** The folders under structure/mega that hold at least one template named {@code <theme>_*.nbt}, mapped to those file stems. */
    private static Map<String, List<String>> roomsOf(String theme) {
        Map<String, List<String>> out = new java.util.TreeMap<>();
        try (Stream<Path> folders = Files.list(STRUCT)) {
            for (Path folder : (Iterable<Path>) folders.filter(Files::isDirectory)::iterator) {
                try (Stream<Path> files = Files.list(folder)) {
                    for (Path f : (Iterable<Path>) files.filter(x -> x.getFileName().toString().startsWith(theme + "_") && x.toString().endsWith(".nbt"))::iterator) {
                        String stem = f.getFileName().toString();
                        out.computeIfAbsent(folder.getFileName().toString(), k -> new ArrayList<>()).add(stem.substring(0, stem.length() - 4));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot list " + STRUCT.toAbsolutePath() + ": " + e.getMessage());
        }
        for (List<String> l : out.values()) java.util.Collections.sort(l);
        return out;
    }

    @Test
    void everyThemeHasItsEnumEntryItsPaletteAndItsEncounterTable() {
        for (String t : THEMES) {
            RegionTheme theme = RelicThemes.themeById(t);
            assertNotNull(theme, t + " is not an entry of RegionTheme (append the line, do not insert it)");
            assertEquals(t, theme.id());
            assertTrue(Files.exists(DATA.resolve("mega/themes/" + t + ".json")), t + ": no palette file");
            assertTrue(Files.exists(DATA.resolve("mega/encounters/" + t + ".json")), t + ": no encounter table");
            assertTrue(Files.exists(TEMPLATE_THEMES.resolve(t + ".json")), t + ": no template_themes file");
        }
    }

    @Test
    void theFileNamesItsThemeAndItsPrefixesAreTheThemesOwn() {
        for (String t : THEMES) {
            JsonObject o = themeFile(t);
            assertEquals(t, o.get("theme").getAsString(), t + ": the 'theme' of the file is not the file's name");
            JsonArray prefixes = o.getAsJsonArray("templates");
            assertTrue(prefixes.size() >= 3, t + ": a theme with rooms in fewer than three kinds of folder is thin");
            Set<String> seen = new TreeSet<>();
            for (JsonElement e : prefixes) {
                String p = e.getAsString();
                assertTrue(p.matches("nusmp:mega/[a-z]+/" + t + "_"), t + ": prefix '" + p + "' must be nusmp:mega/<folder>/" + t + "_");
                assertTrue(seen.add(p), t + ": prefix '" + p + "' is listed twice");
            }
        }
    }

    @Test
    void everyPrefixMatchesRealTemplatesAndEveryTemplateOfTheThemeIsClaimed() {
        for (String t : THEMES) {
            Set<String> folders = new TreeSet<>();
            for (JsonElement e : themeFile(t).getAsJsonArray("templates")) {
                String p = e.getAsString();
                String folder = p.substring("nusmp:mega/".length(), p.length() - (t.length() + 2));
                folders.add(folder);
            }
            Map<String, List<String>> rooms = roomsOf(t);
            assertEquals(folders, rooms.keySet(), t + ": the folders of the prefixes and the folders that hold its templates differ (a prefix with no room is a typo, a room with no prefix is free for every theme)");
        }
    }

    @Test
    void theRulesOfTheGameAllowTheRoomsInTheirOwnThemeOnly() {
        TemplateThemes rules = TemplateThemes.none();
        for (String t : THEMES) {
            List<String> prefixes = new ArrayList<>();
            for (JsonElement e : themeFile(t).getAsJsonArray("templates")) prefixes.add(e.getAsString());
            rules = rules.with(t, prefixes);
        }
        for (String t : THEMES) {
            for (Map.Entry<String, List<String>> f : roomsOf(t).entrySet()) {
                for (String stem : f.getValue()) {
                    String id = "nusmp:mega/" + f.getKey() + "/" + stem;
                    assertTrue(rules.restricted(id), id + " is not restricted to any theme");
                    assertEquals(List.of(t), rules.themesOf(id), id);
                    assertTrue(rules.allows(id, t), id + " must be drawn in " + t);
                    for (String other : List.of("gravity", "radiant", "abyss")) assertTrue(!rules.allows(id, other), id + " must not be drawn in " + other);
                    for (String other : THEMES) if (!other.equals(t)) assertTrue(!rules.allows(id, other), id + " must not be drawn in " + other);
                }
            }
        }
        assertTrue(rules.allows("nusmp:mega/hall/crypt_a", "holy_church"), "a template nobody claims stays free for everyone");
        assertTrue(!rules.restricted("nusmp:mega/hall/crypt_a"));
    }

    @Test
    void theWeightsAreWholeNumbersTheSamePrefixesTheFileNames() {
        for (String t : THEMES) {
            JsonObject o = themeFile(t);
            JsonObject rel = o.getAsJsonObject("relative");
            assertNotNull(rel, t + ": no 'relative' weights");
            Set<String> want = new TreeSet<>();
            for (JsonElement e : o.getAsJsonArray("templates")) want.add(e.getAsString().substring("nusmp:".length()));
            assertEquals(want, new TreeSet<>(rel.keySet()), t + ": 'relative' must weigh exactly the prefixes of 'templates' (without the namespace)");
            for (Map.Entry<String, JsonElement> e : rel.entrySet()) {
                double w = e.getValue().getAsDouble();
                assertTrue(w == Math.rint(w) && w >= 1 && w <= 1000, t + ": weight of " + e.getKey() + " is " + w);
            }
        }
    }

    @Test
    void everyThemeHasTheRoomsTheBriefAsksFor() {
        for (String t : THEMES) {
            Map<String, List<String>> rooms = roomsOf(t);
            int total = 0;
            for (List<String> l : rooms.values()) total += l.size();
            assertTrue(total >= 5, t + " has only " + total + " signature rooms");
            assertTrue(rooms.getOrDefault("hall", List.of()).size() >= 2, t + ": two halls of a different feel are the minimum");
            assertTrue(rooms.containsKey("set"), t + ": a set piece the party remembers");
            boolean danger = false, calm = false;
            for (String folder : rooms.keySet()) {
                if (DANGER.contains(folder)) danger = true;
                if (CALM.contains(folder)) calm = true;
            }
            assertTrue(danger, t + ": a danger room (trap, trial or puzzle)");
            assertTrue(calm, t + ": a calm room (safe or treasure)");
            for (List<String> l : rooms.values()) {
                Set<String> unique = new TreeSet<>(l);
                assertEquals(l.size(), unique.size());
            }
        }
    }

    @Test
    void theGeneratedPoolsHoldEveryRoomOfTheThemes() {
        String side = read(POOLS.resolve("side.json"));
        String safe = read(POOLS.resolve("safe.json"));
        for (String t : THEMES) {
            for (Map.Entry<String, List<String>> f : roomsOf(t).entrySet()) {
                for (String stem : f.getValue()) {
                    String id = "nusmp:mega/" + f.getKey() + "/" + stem;
                    assertTrue(side.contains("\"" + id + "\""), id + " is in no entry of the side pool (run tools/labyrinth/gen_mega_pools.py)");
                    if (f.getKey().equals("safe")) assertTrue(safe.contains("\"" + id + "\""), id + " is not in the safe pool");
                }
            }
        }
    }
}
