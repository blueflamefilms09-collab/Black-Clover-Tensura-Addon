package com.newuniverse.nusmp.quests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why (4.101.0): the game places an advancement purely from the shape of its tree, so the tab is only easy to move around in while no tab is a long single column. This reads the shipped
 * files, counts the rows (leaves) and columns (depth) of every tab the way the game's tidy-tree layout does, and fails if a tab grows taller than {@link #MAX_ROWS} rows or wider than
 * {@link #MAX_COLUMNS} columns, if there are more tabs than the screen has room for, or if a tab head lacks the shared background.
 */
class AdvancementLayoutTest {
    private static final Path BASE = Path.of("src/main/resources/data/anime_multiverse/advancement");
    static final int MAX_ROWS = 32, MAX_COLUMNS = 16, MAX_TABS = 20;

    private static Map<String, JsonObject> load() throws IOException {
        Map<String, JsonObject> all = new HashMap<>();
        try (Stream<Path> s = Files.walk(BASE)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json"))::iterator) {
                String rel = BASE.relativize(p).toString().replace('\\', '/');
                rel = rel.substring(0, rel.length() - 5);
                all.put("anime_multiverse:" + rel, JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }
        return all;
    }

    private static Map<String, List<String>> children(Map<String, JsonObject> all) {
        Map<String, List<String>> kids = new HashMap<>();
        for (Map.Entry<String, JsonObject> e : all.entrySet()) {
            JsonObject o = e.getValue();
            if (o.has("parent") && o.has("display")) kids.computeIfAbsent(o.get("parent").getAsString(), k -> new ArrayList<>()).add(e.getKey());
        }
        return kids;
    }

    private static int rows(String id, Map<String, List<String>> kids) {
        List<String> k = kids.getOrDefault(id, List.of());
        if (k.isEmpty()) return 1;
        int n = 0;
        for (String c : k) n += rows(c, kids);
        return n;
    }

    private static int columns(String id, Map<String, List<String>> kids) {
        int m = 0;
        for (String c : kids.getOrDefault(id, List.of())) m = Math.max(m, columns(c, kids));
        return 1 + m;
    }

    private static List<String> tabHeads(Map<String, JsonObject> all) {
        List<String> heads = new ArrayList<>();
        for (Map.Entry<String, JsonObject> e : all.entrySet()) {
            JsonObject o = e.getValue();
            if (o.has("display") && !o.has("parent") && e.getKey().startsWith("anime_multiverse:") && (e.getKey().equals("anime_multiverse:root") || e.getKey().startsWith("anime_multiverse:tab/")))
                heads.add(e.getKey());
        }
        return heads;
    }

    @Test
    void noTabIsALongColumnOrAWideBand() throws IOException {
        Map<String, JsonObject> all = load();
        Map<String, List<String>> kids = children(all);
        List<String> heads = tabHeads(all);
        assertTrue(heads.contains("anime_multiverse:root"));
        for (String h : heads) {
            assertTrue(rows(h, kids) <= MAX_ROWS, h + " is " + rows(h, kids) + " rows tall (the limit is " + MAX_ROWS + ")");
            assertTrue(columns(h, kids) <= MAX_COLUMNS, h + " is " + columns(h, kids) + " columns wide (the limit is " + MAX_COLUMNS + ")");
        }
    }

    @Test
    void thereAreFewerTabsThanTheScreenHoldsAndEveryTabHasTheSharedBackground() throws IOException {
        Map<String, JsonObject> all = load();
        List<String> heads = tabHeads(all);
        assertTrue(heads.size() <= MAX_TABS, heads.size() + " tabs");
        String bg = all.get("anime_multiverse:root").getAsJsonObject("display").get("background").getAsString();
        for (String h : heads) assertEquals(bg, all.get(h).getAsJsonObject("display").get("background").getAsString(), h);
    }

    @Test
    void everyShownAdvancementIsInsideSomeTab() throws IOException {
        Map<String, JsonObject> all = load();
        List<String> heads = tabHeads(all);
        for (Map.Entry<String, JsonObject> e : all.entrySet()) {
            JsonObject o = e.getValue();
            if (!o.has("display")) continue;
            String cur = e.getKey();
            int guard = 0;
            while (all.get(cur) != null && all.get(cur).has("parent")) {
                cur = all.get(cur).get("parent").getAsString();
                assertTrue(++guard < 1000, "cycle at " + e.getKey());
            }
            assertTrue(heads.contains(cur) || cur.equals("anime_multiverse:devil/contract_crafted"), e.getKey() + " ends at " + cur + ", which is not a tab head");
        }
    }
}
