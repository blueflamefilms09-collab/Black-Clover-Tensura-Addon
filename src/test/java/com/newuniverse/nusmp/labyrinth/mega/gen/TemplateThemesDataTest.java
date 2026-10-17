package com.newuniverse.nusmp.labyrinth.mega.gen;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a theme's own rooms are claimed by a data file ({@code data/nusmp/mega/template_themes/<theme>.json}) that names them only by id PREFIX, so a typo in a prefix (a
 * folder that does not exist, a prefix nothing starts with, a room whose name the prefix misses) is a room that is silently free for every theme, or a claim on nothing, and
 * neither fails anywhere else. This test reads the shipped files and the shipped templates and checks that they agree: every file parses and names a real theme, every prefix is
 * a real kind folder and claims at least one template, every template named {@code <theme>_*} is claimed by that theme's file (and only by it), the weights are positive and
 * written for exactly the prefixes the file claims, each theme has the five rooms the design asks for (two halls, a set piece, a danger room, a calm room), the pure rule
 * ({@link TemplateThemes}) built from the files keeps each themed room to its own sectors and leaves every other template free, and every themed room is in a pool (a room
 * in no pool would never be drawn: the checker calls it an orphan) and in the catalog the assembler tests read. Pure: reads files from the project root (the working
 * directory of Gradle's test task), no Minecraft.
 */
class TemplateThemesDataTest {
    private static final Path THEMES = Path.of("src/main/resources/data/nusmp/mega/template_themes");
    private static final Path STRUCT = Path.of("src/main/resources/data/nusmp/structure/mega");
    private static final Path POOLS = Path.of("src/main/resources/data/nusmp/worldgen/template_pool/mega");
    private static final Path CATALOG = Path.of("src/test/resources/mega/catalog.json");

    /** One parsed theme file. */
    private record Claim(String theme, List<String> prefixes, Map<String, Integer> relative) {}

    private static Map<String, Claim> claims() throws IOException {
        Map<String, Claim> out = new TreeMap<>();
        try (var files = Files.list(THEMES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                String file = p.getFileName().toString();
                String stem = file.substring(0, file.length() - ".json".length());
                Map<String, Object> doc = MiniJson.object(MiniJson.parse(Files.readString(p, StandardCharsets.UTF_8)));
                assertEquals(stem, doc.get("theme"), file + ": the theme inside must be the file name");
                List<String> prefixes = new ArrayList<>();
                for (Object o : MiniJson.array(doc.get("templates"))) prefixes.add((String) o);
                Map<String, Integer> relative = new TreeMap<>();
                Object rel = doc.get("relative");
                if (rel != null) for (Map.Entry<String, Object> e : MiniJson.object(rel).entrySet()) relative.put(e.getKey(), ((Long) e.getValue()).intValue());
                out.put(stem, new Claim(stem, prefixes, relative));
            }
        }
        return out;
    }

    /** Every template of the data folder as "nusmp:mega/&lt;folder&gt;/&lt;name&gt;". */
    private static List<String> templates() throws IOException {
        List<String> out = new ArrayList<>();
        try (var folders = Files.list(STRUCT)) {
            for (Path folder : folders.filter(Files::isDirectory).sorted().toList()) {
                try (var files = Files.list(folder)) {
                    for (Path f : files.filter(x -> x.toString().endsWith(".nbt")).sorted().toList()) {
                        String name = f.getFileName().toString();
                        out.add("nusmp:mega/" + folder.getFileName() + "/" + name.substring(0, name.length() - ".nbt".length()));
                    }
                }
            }
        }
        return out;
    }

    private static String folderOf(String templateId) {
        return templateId.split("/")[1];
    }

    @Test
    void everyFileParsesAndNamesARealTheme() throws IOException {
        Map<String, Claim> all = claims();
        assertFalse(all.isEmpty(), "no template_themes file ships");
        for (Claim c : all.values()) {
            assertTrue(c.theme().equals("gravity") || RegionTheme.fromId(c.theme()) != RegionTheme.GRAVITY, c.theme() + " is not a RegionTheme");
            assertFalse(c.prefixes().isEmpty(), c.theme() + " claims nothing");
            Set<String> seen = new HashSet<>();
            for (String p : c.prefixes()) {
                assertTrue(p.startsWith("nusmp:mega/"), c.theme() + ": prefix " + p + " must start with nusmp:mega/");
                assertTrue(p.endsWith("_"), c.theme() + ": prefix " + p + " must end with an underscore (a prefix without it would claim a neighbour's rooms)");
                assertTrue(p.split("/").length == 3, c.theme() + ": prefix " + p + " must be nusmp:mega/<folder>/<name>_");
                assertTrue(Files.isDirectory(STRUCT.resolve(folderOf(p))), c.theme() + ": " + folderOf(p) + " is not a kind folder of the templates");
                assertTrue(seen.add(p), c.theme() + ": prefix " + p + " is listed twice");
            }
        }
    }

    @Test
    void everyPrefixClaimsATemplateAndEveryThemedRoomIsClaimedByItsTheme() throws IOException {
        Map<String, Claim> all = claims();
        List<String> templates = templates();
        for (Claim c : all.values()) {
            for (String p : c.prefixes()) {
                long hits = templates.stream().filter(t -> t.startsWith(p)).count();
                assertTrue(hits >= 1, c.theme() + ": the prefix " + p + " claims no template");
            }
            for (String t : templates) {
                String name = t.substring(t.lastIndexOf('/') + 1);
                if (name.startsWith(c.theme() + "_")) {
                    boolean claimed = c.prefixes().stream().anyMatch(t::startsWith);
                    assertTrue(claimed, t + " is named for " + c.theme() + " but its file does not claim it (it would be free for every theme)");
                }
            }
        }
    }

    @Test
    void weightsArePositiveAndWrittenForExactlyTheClaimedPrefixes() throws IOException {
        for (Claim c : claims().values()) {
            Set<String> expected = new TreeSet<>();
            for (String p : c.prefixes()) expected.add(p.substring("nusmp:".length()));
            assertEquals(expected, new TreeSet<>(c.relative().keySet()), c.theme() + ": `relative` must be written (as mega/<folder>/<name>_) for exactly the claimed prefixes");
            for (Map.Entry<String, Integer> e : c.relative().entrySet()) {
                assertTrue(e.getValue() >= 1 && e.getValue() <= 150, c.theme() + ": weight of " + e.getKey() + " is " + e.getValue() + " (1..150)");
            }
        }
    }

    @Test
    void everyThemeHasTheFiveKindsOfRoomTheDesignAsksFor() throws IOException {
        List<String> templates = templates();
        for (Claim c : claims().values()) {
            List<String> mine = templates.stream().filter(t -> c.prefixes().stream().anyMatch(t::startsWith)).toList();
            assertTrue(mine.size() >= 5, c.theme() + " has " + mine.size() + " rooms (five or more are asked for)");
            long halls = mine.stream().filter(t -> folderOf(t).equals("hall")).count();
            assertTrue(halls >= 2, c.theme() + " needs two halls of a different feel, has " + halls);
            assertTrue(mine.stream().anyMatch(t -> folderOf(t).equals("set")), c.theme() + " has no set piece");
            assertTrue(mine.stream().anyMatch(t -> Set.of("trap", "trial", "puzzle").contains(folderOf(t))), c.theme() + " has no danger room (trap, trial or puzzle)");
            assertTrue(mine.stream().anyMatch(t -> Set.of("treasure", "safe").contains(folderOf(t))), c.theme() + " has no calm room (treasure or safe)");
        }
    }

    @Test
    void theRuleBuiltFromTheFilesKeepsThemedRoomsToTheirSectorsAndFreesTheRest() throws IOException {
        Map<String, Claim> all = claims();
        TemplateThemes rule = TemplateThemes.none();
        for (Claim c : all.values()) rule = rule.with(c.theme(), c.prefixes());
        List<String> templates = templates();
        int themed = 0;
        for (String t : templates) {
            List<String> owners = new ArrayList<>();
            for (Claim c : all.values()) if (c.prefixes().stream().anyMatch(t::startsWith)) owners.add(c.theme());
            if (owners.isEmpty()) {
                assertFalse(rule.restricted(t), t + " is claimed by nobody and must be free");
                for (RegionTheme th : RegionTheme.values()) assertTrue(rule.allows(t, th.id()), t + " is free for " + th.id());
                continue;
            }
            themed++;
            assertTrue(rule.restricted(t), t + " is claimed and must be restricted");
            for (RegionTheme th : RegionTheme.values()) {
                assertEquals(owners.contains(th.id()), rule.allows(t, th.id()), t + " in a sector of " + th.id() + " (owners " + owners + ")");
            }
        }
        assertTrue(themed >= 20, "at least twenty themed rooms ship (four themes of five or more each), found " + themed);
    }

    @Test
    void everyThemedRoomIsInAPoolAndInTheCatalog() throws IOException {
        Map<String, Claim> all = claims();
        String pools = "";
        try (var files = Files.list(POOLS)) {
            StringBuilder sb = new StringBuilder();
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) sb.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n');
            pools = sb.toString();
        }
        String catalog = Files.readString(CATALOG, StandardCharsets.UTF_8);
        for (String t : templates()) {
            if (all.values().stream().noneMatch(c -> c.prefixes().stream().anyMatch(t::startsWith))) continue;
            assertTrue(pools.contains("\"" + t + "\""), t + " is in no template pool (run tools/labyrinth/gen_mega_pools.py): it would never be drawn");
            assertTrue(catalog.contains("\"" + t + "\""), t + " is not in src/test/resources/mega/catalog.json (run tools/labyrinth/mega_catalog.py)");
        }
    }
}
