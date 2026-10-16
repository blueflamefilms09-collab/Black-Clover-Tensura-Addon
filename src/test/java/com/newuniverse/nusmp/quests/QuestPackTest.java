package com.newuniverse.nusmp.quests;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the quest pack (src/main/resources/quest_pack) is generated and is never loaded by the game until a server operator installs it, so a
 * structural mistake would only show up as a broken book on someone's server. This test parses the pack with a port of FTB Library's SNBT
 * grammar and asserts the invariants FTB Quests relies on: unique 16-digit hex ids that fit a signed long, quest dependencies that exist and
 * do not loop, reward tables and chapter groups that exist, chapter file names that match, lang keys that point at real objects, advancement
 * tasks that point at real advancement files, and every future-boss placeholder advancement being used by a quest. tools/quests/validate_snbt.py
 * is the stricter twin (it also checks every key against the FTB Quests source); this one runs in CI where that source is not present.
 */
class QuestPackTest {
    private static final Path PACK = Path.of("src/main/resources/quest_pack");
    private static final Path ADV = Path.of("src/main/resources/data/nusmp/advancement");
    private static final Pattern HEX = Pattern.compile("[0-9A-F]{16}");
    private static final Pattern AMP = Pattern.compile("&(?![0-9a-fk-or])");
    private static final Set<String> TASK_TYPES = Set.of("item", "custom", "xp", "dimension", "stat", "kill", "location", "checkmark",
            "advancement", "observation", "biome", "structure", "gamestage", "interaction", "fluid");
    private static final Set<String> REWARD_TYPES = Set.of("item", "choice", "all_table", "random", "loot", "command", "custom", "xp",
            "xp_levels", "advancement", "toast", "gamestage", "currency");

    private static Map<String, Object> read(Path p) throws IOException {
        return QstMiniSnbt.parse(Files.readString(p));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) { return (Map<String, Object>) o; }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) { return o == null ? List.of() : (List<Object>) o; }

    private static List<Path> chapterFiles() throws IOException {
        try (Stream<Path> s = Files.list(PACK.resolve("chapters"))) {
            return s.filter(p -> p.toString().endsWith(".snbt")).sorted().toList();
        }
    }

    /** Every object id of the pack with the kind of object, failing on duplicates and on a bad shape. */
    private static Map<String, String> allIds() throws IOException {
        Map<String, String> ids = new HashMap<>();
        for (Path p : chapterFiles()) {
            Map<String, Object> c = read(p);
            put(ids, (String) c.get("id"), "chapter", p.toString());
            for (Object qo : list(c.get("quests"))) {
                Map<String, Object> q = map(qo);
                put(ids, (String) q.get("id"), "quest", p.toString());
                for (Object t : list(q.get("tasks"))) put(ids, (String) map(t).get("id"), "task", p.toString());
                for (Object r : list(q.get("rewards"))) put(ids, (String) map(r).get("id"), "reward", p.toString());
            }
        }
        try (Stream<Path> s = Files.list(PACK.resolve("reward_tables"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".snbt")).toList()) {
                Map<String, Object> t = read(p);
                put(ids, (String) t.get("id"), "table", p.toString());
                for (Object r : list(t.get("rewards"))) put(ids, (String) map(r).get("id"), "tablereward", p.toString());
            }
        }
        for (Object g : list(read(PACK.resolve("chapter_groups.snbt")).get("chapter_groups")))
            put(ids, (String) map(g).get("id"), "group", "chapter_groups.snbt");
        return ids;
    }

    private static void put(Map<String, String> ids, String id, String kind, String where) {
        assertTrue(id != null && HEX.matcher(id).matches(), where + ": id " + id + " must be 16 upper-case hex digits");
        long v = Long.parseLong(id, 16);                  // throws for 8..F first digit: FTB Quests parses ids exactly this way
        assertTrue(v != 0L && v != 1L, where + ": reserved id");
        assertTrue(ids.put(id, kind) == null, where + ": duplicate id " + id);
    }

    @Test
    void sizeOfTheBook() throws IOException {
        Map<String, String> ids = allIds();
        long chapters = ids.values().stream().filter("chapter"::equals).count();
        long quests = ids.values().stream().filter("quest"::equals).count();
        assertTrue(chapters >= 14, "at least 14 chapters, found " + chapters);
        assertTrue(quests >= 200, "at least 200 quests, found " + quests);
        assertEquals(3, ids.values().stream().filter("group"::equals).count(), "three chapter groups");
    }

    @Test
    void idsAreStableAcrossRegeneration() throws IOException {
        // sha256 of "chapter/prologue" and "quest/prologue/welcome", first 63 bits: changing the hash would reset every player's progress
        Map<String, String> ids = allIds();
        assertEquals("chapter", ids.get("2E585A49FFEBE2E6"), "the prologue chapter id moved");
        assertEquals("quest", ids.get("1F4428D635FC1DB1"), "the prologue welcome quest id moved");
        assertEquals("group", ids.get("5D0FDBAA39C60882"), "the journey group id moved");
        assertEquals("table", ids.get("64B0D15FE1971D30"), "the journey cache table id moved");
    }

    @Test
    void dataFileAndPackFiles() throws IOException {
        Map<String, Object> data = read(PACK.resolve("data.snbt"));
        assertEquals(13, ((Number) data.get("version")).intValue());
        assertEquals("en_us", data.get("fallback_locale"));
        Set<String> onDisk = new HashSet<>();
        try (Stream<Path> st = Files.walk(PACK)) {
            for (Path p : st.filter(Files::isRegularFile).toList()) onDisk.add(PACK.relativize(p).toString().replace('\\', '/'));
        }
        assertTrue(onDisk.containsAll(Set.of("data.snbt", "lang/en_us.snbt", "chapter_groups.snbt")), "the pack files: " + onDisk);
        for (String f : onDisk) {
            boolean ok = f.equals("data.snbt") || f.equals("chapter_groups.snbt") || f.equals("lang/en_us.snbt") || f.equals("README.txt")
                    || (f.startsWith("chapters/") || f.startsWith("reward_tables/")) && f.endsWith(".snbt");
            assertTrue(ok, "unexpected file in the pack (the installer copies everything but README*): " + f);
        }
    }

    @Test
    void chaptersGroupsAndFileNames() throws IOException {
        Set<String> groups = new HashSet<>();
        for (Object g : list(read(PACK.resolve("chapter_groups.snbt")).get("chapter_groups"))) groups.add((String) map(g).get("id"));
        Set<String> orders = new HashSet<>();
        for (Path p : chapterFiles()) {
            String name = p.getFileName().toString();
            assertEquals(name.toLowerCase(), name, "FTB Quests lower-cases chapter file names");
            Map<String, Object> c = read(p);
            assertEquals(name.substring(0, name.length() - 5), c.get("filename"), name + ": filename key");
            String g = (String) c.get("group");
            assertTrue(g != null && groups.contains(g), name + ": chapter group " + g + " does not exist");
            assertTrue(orders.add(g + "#" + c.get("order_index")), name + ": order_index repeated inside its group");
            assertTrue(!c.containsKey("title") && !c.containsKey("subtitle"), name + ": titles live in the lang file only");
        }
    }

    @Test
    void questsHaveTasksShapesAndNumbers() throws IOException {
        for (Path p : chapterFiles()) {
            for (Object qo : list(read(p).get("quests"))) {
                Map<String, Object> q = map(qo);
                String w = p.getFileName() + " quest " + q.get("id");
                assertTrue(q.get("x") instanceof Double && q.get("y") instanceof Double, w + ": x and y are doubles");
                if (q.containsKey("size")) assertTrue(q.get("size") instanceof Double, w + ": size is a double");
                assertTrue(q.get("icon") instanceof Map, w + ": icon is an item compound");
                assertTrue(!list(q.get("tasks")).isEmpty(), w + ": a quest needs a task");
                assertTrue(!q.containsKey("title") && !q.containsKey("description") && !q.containsKey("subtitle"), w + ": text is in the lang file");
                for (Object t : list(q.get("tasks"))) assertTrue(TASK_TYPES.contains(map(t).get("type")), w + ": task type " + map(t).get("type"));
                for (Object r : list(q.get("rewards"))) assertTrue(REWARD_TYPES.contains(map(r).get("type")), w + ": reward type " + map(r).get("type"));
            }
        }
    }

    @Test
    void dependenciesExistAndDoNotLoop() throws IOException {
        Map<String, List<String>> deps = new HashMap<>();
        for (Path p : chapterFiles()) {
            for (Object qo : list(read(p).get("quests"))) {
                Map<String, Object> q = map(qo);
                List<String> d = new ArrayList<>();
                for (Object x : list(q.get("dependencies"))) d.add((String) x);
                deps.put((String) q.get("id"), d);
            }
        }
        for (Map.Entry<String, List<String>> e : deps.entrySet())
            for (String d : e.getValue()) {
                assertTrue(deps.containsKey(d), e.getKey() + " depends on " + d + " which is not a quest");
                assertTrue(!d.equals(e.getKey()), e.getKey() + " depends on itself");
            }
        Map<String, Integer> state = new HashMap<>();
        for (String start : deps.keySet()) visit(start, deps, state);
    }

    private static void visit(String n, Map<String, List<String>> deps, Map<String, Integer> state) {
        Integer st = state.get(n);
        if (st != null) {
            assertTrue(st == 2, "dependency cycle through " + n);
            return;
        }
        state.put(n, 1);
        for (String d : deps.get(n)) visit(d, deps, state);
        state.put(n, 2);
    }

    @Test
    void rewardTablesExist() throws IOException {
        Set<String> tables = new HashSet<>();
        try (Stream<Path> s = Files.list(PACK.resolve("reward_tables"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".snbt")).toList()) {
                Map<String, Object> t = read(p);
                tables.add((String) t.get("id"));
                assertTrue(!list(t.get("rewards")).isEmpty(), p + ": empty reward table");
            }
        }
        int refs = 0;
        for (Path p : chapterFiles()) {
            for (Object qo : list(read(p).get("quests"))) {
                for (Object ro : list(map(qo).get("rewards"))) {
                    Map<String, Object> r = map(ro);
                    if (r.containsKey("table_id")) {
                        refs++;
                        assertTrue(r.get("table_id") instanceof Long, "table_id is read with getLong and must be a long tag");
                        String hex = String.format("%016X", (Long) r.get("table_id"));
                        assertTrue(tables.contains(hex), "reward table " + hex + " does not exist");
                    }
                }
            }
        }
        assertTrue(refs > 100, "the quests use their reward tables, found " + refs + " references");
    }

    @Test
    void advancementTasksPointAtRealAdvancementsAndEveryPlaceholderHasAQuest() throws IOException {
        Set<String> used = new HashSet<>();
        int adv = 0;
        for (Path p : chapterFiles()) {
            for (Object qo : list(read(p).get("quests"))) {
                for (Object to : list(map(qo).get("tasks"))) {
                    Map<String, Object> t = map(to);
                    if (!"advancement".equals(t.get("type"))) continue;
                    adv++;
                    String a = (String) t.get("advancement");
                    assertTrue(a != null && a.startsWith("nusmp:"), p.getFileName() + ": advancement " + a);
                    String path = a.substring(6);
                    assertTrue(Files.exists(ADV.resolve(path + ".json")), p.getFileName() + ": advancement file " + a + " does not exist");
                    used.add(path);
                }
            }
        }
        assertTrue(adv >= 300, "most tasks are advancement tasks, found " + adv);
        try (Stream<Path> s = Files.walk(ADV.resolve("future"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                String path = "future/" + f.getFileName().toString().replace(".json", "");
                assertTrue(used.contains(path), "the placeholder advancement " + path + " has no quest");
            }
        }
    }

    @Test
    void langKeysPointAtObjectsAndTextIsSafe() throws IOException {
        Map<String, String> ids = allIds();
        Map<String, Object> lang = read(PACK.resolve("lang/en_us.snbt"));
        Pattern key = Pattern.compile("(file|chapter_group|chapter|quest|task|reward|reward_table)\\.([0-9A-F]{16})\\.(title|quest_subtitle|quest_desc|chapter_subtitle)");
        Map<String, String> kind = Map.of("chapter_group", "group", "chapter", "chapter", "quest", "quest", "task", "task", "reward", "reward",
                "reward_table", "table");
        Set<String> titled = new HashSet<>();
        for (Map.Entry<String, Object> e : lang.entrySet()) {
            var m = key.matcher(e.getKey());
            assertTrue(m.matches(), "malformed lang key " + e.getKey());
            if (m.group(1).equals("file")) {
                assertEquals("0000000000000001", m.group(2));
            } else {
                assertEquals(kind.get(m.group(1)), ids.get(m.group(2)), e.getKey() + " points at the wrong object");
            }
            boolean isList = m.group(3).equals("quest_desc") || m.group(3).equals("chapter_subtitle");
            assertTrue(isList ? e.getValue() instanceof List : e.getValue() instanceof String, e.getKey() + ": wrong value shape");
            List<String> texts = new ArrayList<>();
            if (e.getValue() instanceof String s) texts.add(s);
            else for (Object o : list(e.getValue())) texts.add((String) o);
            for (String t : texts) {
                for (char c : t.toCharArray()) assertTrue(c >= 32 && c < 127, e.getKey() + ": non-ASCII or control character in the text");
                assertTrue(!AMP.matcher(t).find(), e.getKey() + ": '&' is a colour code prefix in FTB text: " + t);
                assertTrue(!t.replace("{@pagebreak}", "").contains("{"), e.getKey() + ": '{' starts a text tag: " + t);
            }
            if (m.group(3).equals("title")) titled.add(m.group(2));
        }
        for (Map.Entry<String, String> e : ids.entrySet())
            if (e.getValue().equals("quest") || e.getValue().equals("chapter"))
                assertTrue(titled.contains(e.getKey()), e.getValue() + " " + e.getKey() + " has no title");
    }
}
