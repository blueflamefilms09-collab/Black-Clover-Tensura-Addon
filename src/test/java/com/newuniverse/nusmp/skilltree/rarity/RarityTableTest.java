package com.newuniverse.nusmp.skilltree.rarity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the rarity of every Skill Book is data (the {@code rarity} and {@code theme} of each line of the shipped {@code skill_books.json}, written by {@code tools/lines/gen_book_index.py} from
 * {@code tools/lines/rarity_table.py}), and a table that drifts is silent: one line with a typo reads as common, a table whose mythic lines are all in blocked franchises leaves the rifts with nothing to
 * give, a theme the Guardian pick does not know never matches. This reads the shipped files and pins the owner's numbers: every line has a valid rarity and a known background theme; the spread is
 * about 30/28/24/12/6 percent; 10 to 22 mythic lines; every id the generator names by hand exists; the hand-written rarities and the Evolution skills of {@code NEW_SKILLS} are what the table says; every
 * route file has its line and every line its route file; and with the default blacklist applied there are still lines of every rarity, mythic ones among them, so no door is left with an empty pool.
 */
class RarityTableTest {
    private static final Path BOOKS = Path.of("src/main/resources/assets/nusmp/skill_books.json");
    private static final Path ROUTES = Path.of("src/main/resources/data/nusmp/unique_skills");
    private static final Path PY = Path.of("tools/lines/rarity_table.py");

    private static Map<String, JsonObject> lines() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(BOOKS, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, JsonObject> out = new LinkedHashMap<>();
        for (var e : root.getAsJsonObject("lines").entrySet()) out.put(e.getKey(), e.getValue().getAsJsonObject());
        return out;
    }

    /** the {@code "id": "rarity"} pairs of a dict in the generator, by its name */
    private static Map<String, String> dictOf(String name) throws IOException {
        String py = Files.readString(PY, StandardCharsets.UTF_8);
        int start = py.indexOf("\n" + name + " = {");
        assertTrue(start >= 0, "no " + name + " in rarity_table.py");
        int end = py.indexOf("\n}", start);
        int oneLine = py.indexOf("{}", start);
        Map<String, String> out = new LinkedHashMap<>();
        if (oneLine >= 0 && oneLine < start + name.length() + 6) return out;              // NEW_SKILLS = {}
        Matcher m = Pattern.compile("\"([a-z0-9_]+)\"\\s*:\\s*\"([a-z]+)\"").matcher(py.substring(start, end < 0 ? py.length() : end));
        while (m.find()) out.put(m.group(1), m.group(2));
        return out;
    }

    private static boolean blocked(JsonObject line) {
        return BlockedFranchises.blocked(BlockedFranchises.parse(BlockedFranchises.DEFAULT), line.get("franchise").getAsString());
    }

    @Test
    void everyLineHasAValidRarityAndAKnownTheme() throws IOException {
        Set<String> themes = new HashSet<>(BossThemes.BACKGROUNDS);
        List<String> bad = new ArrayList<>();
        for (var e : lines().entrySet()) {
            JsonObject l = e.getValue();
            if (!l.has("rarity") || BookRarity.byId(l.get("rarity").getAsString()) == null) bad.add(e.getKey() + ": rarity " + l.get("rarity"));
            if (!l.has("theme") || !themes.contains(l.get("theme").getAsString())) bad.add(e.getKey() + ": theme " + l.get("theme"));
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void theSpreadIsAboutThirtyTwentyEightTwentyFourTwelveSix() throws IOException {
        Map<String, JsonObject> lines = lines();
        int[] n = new int[BookRarity.count()];
        for (JsonObject l : lines.values()) n[BookRarity.fromId(l.get("rarity").getAsString()).rank()]++;
        double total = lines.size();
        assertTrue(total >= 200, "the table has the lines: " + total);
        double[][] band = {{26, 34}, {24, 32}, {20, 28}, {8, 16}, {3.5, 9}};
        for (BookRarity r : BookRarity.values()) {
            double pct = 100.0 * n[r.rank()] / total;
            assertTrue(pct >= band[r.rank()][0] && pct <= band[r.rank()][1], r.id() + " is " + pct + " percent (" + n[r.rank()] + " of " + (int) total + "), wanted " + band[r.rank()][0] + " to " + band[r.rank()][1]);
        }
    }

    @Test
    void ten_to_twentyTwoMythicLines() throws IOException {
        long mythic = lines().values().stream().filter(l -> "mythic".equals(l.get("rarity").getAsString())).count();
        assertTrue(mythic >= 10 && mythic <= 22, "mythic lines: " + mythic);
    }

    @Test
    void theGeneratorsHandWrittenRaritiesAreInTheTableAndMatch() throws IOException {
        Map<String, JsonObject> lines = lines();
        Map<String, String> explicit = dictOf("EXPLICIT");
        assertTrue(explicit.size() >= 30, "the explicit dict was read: " + explicit.size());
        for (var e : explicit.entrySet()) {
            assertNotNull(BookRarity.byId(e.getValue()), e.getKey() + " names the unknown rarity " + e.getValue());
            JsonObject l = lines.get(e.getKey());
            assertNotNull(l, "the hand-written id " + e.getKey() + " is not a line of the table");
            assertEquals(e.getValue(), l.get("rarity").getAsString(), e.getKey());
        }
        for (var e : dictOf("NEW_SKILLS").entrySet()) {
            assertNotNull(BookRarity.byId(e.getValue()), e.getKey() + " names the unknown rarity " + e.getValue());
            JsonObject l = lines.get(e.getKey());
            if (l != null) assertEquals(e.getValue(), l.get("rarity").getAsString(), "the Evolution skill " + e.getKey());
        }
    }

    @Test
    void everyRouteFileHasItsLineAndEveryLineItsRouteFile() throws IOException {
        Set<String> inTable = lines().keySet();
        Set<String> files = new HashSet<>();
        File[] list = ROUTES.toFile().listFiles((d, f) -> f.endsWith(".json"));
        assertNotNull(list, "no route folder");
        for (File f : list) files.add(f.getName().substring(0, f.getName().length() - 5));
        Set<String> noLine = new HashSet<>(files);
        noLine.removeAll(inTable);
        Set<String> noFile = new HashSet<>(inTable);
        noFile.removeAll(files);
        assertTrue(noLine.isEmpty(), "route files with no line in the table (run gen_book_index.py): " + noLine);
        assertTrue(noFile.isEmpty(), "lines with no route file: " + noFile);
    }

    @Test
    void withTheDefaultBlacklistEveryDoorStillHasABook() throws IOException {
        int[] open = new int[BookRarity.count()];
        for (JsonObject l : lines().values()) if (!blocked(l)) open[BookRarity.fromId(l.get("rarity").getAsString()).rank()]++;
        assertTrue(open[BookRarity.MYTHIC.rank()] >= 5, "the rift needs mythic lines outside the blocked franchises: " + open[BookRarity.MYTHIC.rank()]);
        for (BookRarity r : BookRarity.values()) assertTrue(open[r.rank()] >= 5, r.id() + " lines outside the blocked franchises: " + open[r.rank()]);
    }

    @Test
    void theHandBuiltClassicsKeepTheirPlace() throws IOException {
        Map<String, JsonObject> l = lines();
        assertEquals("common", l.get("basic_swordsmanship").get("rarity").getAsString());
        assertEquals("uncommon", l.get("lady_luck").get("rarity").getAsString());
        assertEquals("rare", l.get("oath_of_the_heavy_armor").get("rarity").getAsString());
        assertEquals("mythic", l.get("smoldering_fang_of_insanity").get("rarity").getAsString(), "the Black Market's flagship");
    }

    @Test
    void aTableWithoutTheFieldReadsAsCommon() {
        assertSame(BookRarity.COMMON, BookRarity.fromId(null));
        com.newuniverse.nusmp.skilltree.unique.LineIndex.Line l = com.newuniverse.nusmp.skilltree.unique.LineIndex.parse(JsonParser.parseString(
                "{\"franchises\":[\"tensura\"],\"lines\":{\"old_line\":{\"book\":\"nusmp:old_line_book\",\"name\":\"Old\",\"color\":\"#102030\",\"franchise\":\"tensura\"}}}").getAsJsonObject()).get(0);
        assertSame(BookRarity.COMMON, l.rarity(), "a table written before 3.26.0 has no rarity field");
        assertEquals("", l.theme());
    }
}
