package com.newuniverse.nusmp.book.pages15;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Spec;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Why: batch 6 (Space, Time, Game and Mirror) adds 86 pages to eleven grimoires and extends the sets of seven characters; nothing can be played here, so this pins what can be read: the ids are unique and
 * readable, every tier and cooldown is inside its bounds, at most one signature per book, every page has its text, its policy line and its doc row, every book of the batch shows 15 pages in the basic grimoire
 * (counted by tools/page_counts.py, the same counter as PageCountsTest) and each of the seven character sets lists 15 pages that its book really has. Pure (java.nio, Gson, regular expressions).
 */
class Pages15Batch6Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path BATCH = ROOT.resolve("src/main/java/com/newuniverse/nusmp/book/pages15/Batch6.java");
    private static final List<String> BOOKS = List.of("book_compass", "book_copy", "book_dice", "book_slash", "book_gravity", "book_game", "book_spatial", "book_mirror", "book_time", "book_legion", "book_key", "book_kotodama");
    /** New pages each book of the batch must receive (the "New pages" column of docs/magic/page_counts.md section 8). */
    private static final Map<String, Integer> NEW = Map.ofEntries(Map.entry("book_compass", 12), Map.entry("book_copy", 11), Map.entry("book_dice", 11), Map.entry("book_slash", 11),
            Map.entry("book_gravity", 9), Map.entry("book_game", 9), Map.entry("book_spatial", 7), Map.entry("book_mirror", 7), Map.entry("book_time", 5), Map.entry("book_legion", 3), Map.entry("book_key", 1));
    /** Character set -> its book (the seven sets of this batch). */
    private static final Map<String, String> SETS = Map.of("letoile", "book_compass", "jack", "book_slash", "dante", "book_gravity", "finral", "book_spatial",
            "langris", "book_spatial", "gauche", "book_mirror", "julius", "book_time");

    private record Page(String book, String id, String name, Tier tier, int cooldown) {}

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException {
        return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static final Pattern DEF = Pattern.compile("\\b(starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"([^\"]+)\"");

    private static int close(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') {
                for (i++; i < s.length() && s.charAt(i) != '"'; i++) if (s.charAt(i) == '\\') i++;
            } else if (ch == '(') depth++;
            else if (ch == ')' && --depth == 0) return i;
        }
        return s.length();
    }

    private static List<Page> pages() throws IOException {
        String c = code(BATCH);
        List<Page> out = new ArrayList<>();
        Matcher reg = Pattern.compile("register\\(\\s*\"([a-z_]+)\"\\s*,").matcher(c);
        while (reg.find()) {
            String book = reg.group(1);
            String body = c.substring(reg.end(), close(c, c.indexOf('(', reg.start())));
            Matcher d = DEF.matcher(body);
            while (d.find()) {
                Matcher next = DEF.matcher(body);
                int to = next.find(d.end()) ? next.start() : body.length();
                Matcher cd = Pattern.compile("\\.withCooldown\\((\\d+)\\)").matcher(body.substring(d.end(), to));
                Tier t = Tier.valueOf(d.group(1).toUpperCase(Locale.ROOT));
                out.add(new Page(book, d.group(2), d.group(3), t, cd.find() ? Integer.parseInt(cd.group(1)) : t.cooldown));
            }
        }
        return out;
    }

    @Test
    void theBatchWritesExactlyTheNewPagesOfItsBooksAndTheyObeyTheRules() throws IOException {
        List<Page> pages = pages();
        Map<String, Integer> per = new LinkedHashMap<>();
        List<Spec> specs = new ArrayList<>();
        for (Page p : pages) {
            assertTrue(BOOKS.contains(p.book()), p.book() + " is not a book of batch 6");
            assertEquals(6, Pages15Plan.batchOf(p.book()), p.book());
            per.merge(p.book(), 1, Integer::sum);
            specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        }
        assertEquals(NEW, per, "pages per book");
        assertEquals(86, pages.size());
        Set<String> ids = new HashSet<>();
        for (Page p : pages) assertTrue(ids.add(p.id()), "id twice in the batch: " + p.id());
        // ids, names, tiers, cooldown bounds, at most one signature per book, no older id repeated (the older ids are the other page sources; the shared scan test checks them too)
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), Set.of()), "problems of the new pages");
        for (Page p : pages) assertEquals(null, Pages15Rules.idProblem(p.id()), p.id());
    }

    @Test
    void everyPageHasItsTextItsPolicyLineAndItsDocRowWithAnOrigin() throws IOException {
        List<Page> pages = pages();
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch6.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch6_new_pages.json"))).getAsJsonObject();
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) for (JsonElement x : e.getValue().getAsJsonArray()) listed.add(x.getAsString());
        Map<String, Pages15Rules.DocRow> rows = new HashMap<>();
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch6.md")).split("\n")) {
            Pages15Rules.DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) rows.put(r.book() + "/" + r.id(), r);
        }
        assertEquals(pages.size(), lang.size(), "one text per page");
        assertEquals(pages.size(), listed.size(), "one policy line per page");
        assertEquals(pages.size(), rows.size(), "one doc row per page");
        int canon = 0;
        for (Page p : pages) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, key);
            assertTrue(listed.contains(p.id()), p.id() + " in the policy file");
            Pages15Rules.DocRow r = rows.get(p.book() + "/" + p.id());
            assertTrue(r != null && r.origin() != null, p.id() + ": doc row with canon/original");
            assertEquals(p.name(), r.name());
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier());
            if (!r.origin().original()) canon++;
        }
        System.out.println("pages15 batch 6: " + pages.size() + " pages, " + canon + " canon, " + (pages.size() - canon) + " original");
        // the policy file groups by MagicType and each magic belongs to a book of this batch
        for (String magic : policy.keySet()) assertTrue(BOOKS.contains("book_" + magic.toLowerCase(Locale.ROOT)), magic);
    }

    // ------------------------------------------------------------------ the counter (python) and the character sets
    private static List<String> runScript(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("python3", "-I", "-B", ROOT.resolve("tools/page_counts.py").toString()));
        cmd.addAll(List.of(args));
        Process p;
        try {
            p = new ProcessBuilder(cmd).directory(ROOT.toFile()).redirectErrorStream(false).start();
        } catch (IOException noPython) {
            assumeTrue(false, "python3 is not on the path: " + noPython.getMessage());
            return List.of();
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(120, TimeUnit.SECONDS), "tools/page_counts.py did not finish");
        assertEquals(0, p.exitValue(), "tools/page_counts.py failed: " + new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        return List.of(out.split("\n"));
    }

    @Test
    void everyBookOfTheBatchShowsFifteenPagesInTheBasicGrimoireAndTheSevenSetsListFifteen() throws Exception {
        Map<String, Integer> basic = new HashMap<>();
        Map<String, Map<String, Integer>> chars = new HashMap<>();
        for (String line : runScript("--golden")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] halves = line.split("\\|", -1);
            String[] head = halves[0].trim().split("\\s+");
            basic.put(head[0], Integer.parseInt(head[2]));
            Map<String, Integer> c = new HashMap<>();
            for (String kv : halves[1].trim().split("\\s+")) if (!kv.isEmpty()) c.put(kv.split("=")[0], Integer.parseInt(kv.split("=")[1]));
            chars.put(head[0], c);
        }
        for (String b : BOOKS) assertTrue(basic.get(b) >= 15 && basic.get(b) <= 32, b + " shows " + basic.get(b) + " pages in the basic grimoire");
        for (var e : SETS.entrySet()) {
            int n = chars.get(e.getValue()).getOrDefault(e.getKey(), 0);
            assertTrue(n >= 15 && n <= 32, e.getKey() + " lists " + n + " pages");
        }
        List<String> check = runScript("--check");
        assertTrue(check.stream().noneMatch(l -> l.startsWith("SET PAGE NOT IN BOOK") || l.startsWith("WARN")), String.join("\n", check));
    }

    @Test
    void theSevenCharacterFilesParseWithoutRepeatedPagesAndListNewPages() throws IOException {
        Set<String> mine = new HashSet<>();
        for (Page p : pages()) mine.add(p.id());
        for (var e : SETS.entrySet()) {
            JsonObject d = JsonParser.parseString(read(ROOT.resolve("src/main/resources/data/nusmp/character_spells/" + e.getKey() + ".json"))).getAsJsonObject();
            assertEquals(e.getValue(), d.get("book").getAsString());
            JsonArray spells = d.getAsJsonArray("spells");
            Set<String> seen = new HashSet<>();
            boolean hasNew = false;
            for (JsonElement s : spells) {
                String page = s.getAsJsonObject().get("page").getAsString();
                assertTrue(seen.add(page), e.getKey() + ": page twice " + page);
                hasNew |= mine.contains(page);
            }
            assertTrue(hasNew, e.getKey() + " has new entries");
        }
    }
}
