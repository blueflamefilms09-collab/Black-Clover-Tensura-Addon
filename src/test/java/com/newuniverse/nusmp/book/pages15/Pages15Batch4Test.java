package com.newuniverse.nusmp.book.pages15;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.DocRow;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Spec;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Why: batch 4 (Earth, Stone and Metal) adds 96 pages to 16 books and extends three character grimoires; nothing can be played here, so this pure test pins the data the self-test
 * {@code magic.pages15} will check in the game: unique readable ids, tier cost and cooldown bounds, one signature per book at most, a lang text, an armour-policy line and a doc row (with a canon
 * source or "original") for every page, every one of the 16 books reaching 15 pages in its basic grimoire (the same counter as {@code PageCountsTest}, run through {@code tools/page_counts.py}), and the
 * character sets of Nozel, Klaus and Mars parsing with every entry naming a page of their book and listing 15 pages. Pure (java.nio, regular expressions, Gson); the working directory is the project root.
 */
class Pages15Batch4Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path BATCH = SRC.resolve("book/pages15/Batch4.java");

    /** book -> the minimum number of new pages ("New pages" column of docs/magic/page_counts.md section 8). */
    private static final Map<String, Integer> PLAN = new LinkedHashMap<>();
    static {
        PLAN.put("book_mercury", 11); PLAN.put("book_sand", 10); PLAN.put("book_steel", 10); PLAN.put("book_rock", 8);
        PLAN.put("book_red_ochre", 8); PLAN.put("book_stone", 8); PLAN.put("book_mineral", 8); PLAN.put("book_sandstone", 7);
        PLAN.put("book_earth", 6); PLAN.put("book_shakudo", 5); PLAN.put("book_crystal", 3); PLAN.put("book_glass", 3);
        PLAN.put("book_iron", 3); PLAN.put("book_copper", 3); PLAN.put("book_corundum", 2); PLAN.put("book_bronze", 1);
    }

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

    private static Set<String> olderIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        Path pages15 = SRC.resolve("book/pages15");
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(pages15)).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    @Test
    void everyBookOfTheBatchGetsAtLeastItsPlannedNumberOfPagesAndNoOtherBook() throws IOException {
        Map<String, Integer> per = new LinkedHashMap<>();
        for (Page p : pages()) per.merge(p.book(), 1, Integer::sum);
        assertEquals(PLAN.keySet(), per.keySet(), "Batch4 writes pages for exactly its 16 books");
        for (var e : PLAN.entrySet()) {
            assertTrue(per.get(e.getKey()) >= e.getValue(), e.getKey() + ": " + per.get(e.getKey()) + " new pages, the plan needs " + e.getValue());
            assertEquals(4, Pages15Plan.batchOf(e.getKey()), e.getKey() + " belongs to batch 4");
        }
        assertEquals(96, pages().size(), "96 new pages in all");
    }

    @Test
    void idsAreUniqueReadableAndNotAnOlderPageAndEveryTierIsInsideItsBounds() throws IOException {
        List<Spec> specs = new ArrayList<>();
        for (Page p : pages()) specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), olderIds()), "problems of the new pages (ids, names, cost, cooldown, one signature per book)");
        Map<String, Integer> sigs = new LinkedHashMap<>();
        for (Page p : pages()) if (p.tier() == Tier.SIGNATURE) sigs.merge(p.book(), 1, Integer::sum);
        for (var e : sigs.entrySet()) assertEquals(1, (int) e.getValue(), e.getKey() + ": at most one signature page");
    }

    @Test
    void theLangFragmentThePolicyFileAndTheDocCoverEveryPageAndNothingElse() throws IOException {
        List<Page> pages = pages();
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch4.json"))).getAsJsonObject();
        JsonObject pol = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch4_new_pages.json"))).getAsJsonObject();
        Map<String, String> magicOf = new LinkedHashMap<>();
        for (var e : pol.entrySet()) for (JsonElement id : e.getValue().getAsJsonArray()) assertTrue(magicOf.put(id.getAsString(), e.getKey()) == null, "policy id twice: " + id);
        Map<String, DocRow> rows = new LinkedHashMap<>();
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch4.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) assertTrue(rows.put(r.book() + "/" + r.id(), r) == null, "doc row twice: " + line);
        }
        int canon = 0;
        for (Page p : pages) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text for " + key);
            assertEquals(p.book().replace("book_", "").toUpperCase(Locale.ROOT), magicOf.get(p.id()), "armour policy magic of " + p.id());
            DocRow r = rows.get(p.book() + "/" + p.id());
            assertNotNull(r, "doc row for " + p.id());
            assertNotNull(r.origin(), p.id() + ": origin must be original or canon: <source>");
            assertEquals(p.name(), r.name());
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier());
            if (!r.origin().original()) { canon++; assertTrue(r.origin().source().length() > 10, p.id() + ": a real source"); }
        }
        assertEquals(pages.size(), lang.size(), "lang keys = pages");
        assertEquals(pages.size(), magicOf.size(), "policy ids = pages");
        assertEquals(pages.size(), rows.size(), "doc rows = pages");
        assertTrue(canon >= 10 && canon < pages.size(), "canon pages: " + canon);
    }

    // ------------------------------------------------------------------ the page counter (python) and the character sets
    private static List<String> script(String... args) throws IOException, InterruptedException {
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
    void everyBookOfTheBatchShowsAtLeast15PagesInItsBasicGrimoireAndTheGoldenFileSaysSo() throws Exception {
        Map<String, int[]> now = new LinkedHashMap<>();
        for (String line : script("--golden")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] head = line.split("\\|", -1)[0].trim().split("\\s+");
            now.put(head[0], new int[]{Integer.parseInt(head[1]), Integer.parseInt(head[2])});
        }
        Map<String, int[]> golden = new LinkedHashMap<>();
        for (String line : Files.readAllLines(ROOT.resolve("src/test/resources/pages/page_counts.golden.txt"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] head = line.split("\\|", -1)[0].trim().split("\\s+");
            golden.put(head[0], new int[]{Integer.parseInt(head[1]), Integer.parseInt(head[2])});
        }
        for (String book : PLAN.keySet()) {
            assertTrue(now.get(book)[1] >= Pages15Rules.GOAL, book + " shows " + now.get(book)[1] + " in the basic grimoire");
            assertTrue(now.get(book)[1] <= Pages15Rules.MAX_VISIBLE, book + " shows too many pages");
            assertTrue(now.get(book)[0] <= Pages15Rules.MAX_FAMILY, book + " family pages over the 64-bit mask");
            assertTrue(golden.get(book)[1] >= Pages15Rules.GOAL, book + ": the golden file was refreshed for this book");
            assertTrue(golden.get(book)[0] <= now.get(book)[0] && golden.get(book)[1] <= now.get(book)[1], book + ": the golden file never exceeds the sources");
        }
    }

    @Test
    void theThreeCharacterSetsParseListFifteenPagesAndNameOnlyPagesOfTheirBook() throws Exception {
        Map<String, String> books = Map.of("nozel", "book_mercury", "klaus", "book_steel", "mars", "book_mineral");
        for (var e : books.entrySet()) {
            JsonObject set = JsonParser.parseString(read(ROOT.resolve("src/main/resources/data/nusmp/character_spells/" + e.getKey() + ".json"))).getAsJsonObject();
            assertEquals(e.getValue(), set.get("book").getAsString());
            assertEquals(1, set.get("schema").getAsInt());
            Set<String> inBook = new LinkedHashSet<>();
            Matcher m = Pattern.compile("^\\s+\\d+ ([a-z0-9_]+)\\s", Pattern.MULTILINE).matcher(String.join("\n", script("--ids", e.getValue())));
            while (m.find()) inBook.add(m.group(1));
            assertTrue(inBook.size() >= 15, e.getValue() + " ids found by the script: " + inBook.size());
            JsonArray spells = set.getAsJsonArray("spells");
            Set<String> listed = new LinkedHashSet<>();
            Set<String> sources = set.getAsJsonObject("sources").keySet();
            for (JsonElement s : spells) {
                JsonObject o = s.getAsJsonObject();
                String page = o.get("page").getAsString();
                assertTrue(listed.add(page), e.getKey() + ": page twice " + page);
                assertTrue(inBook.contains(page), e.getKey() + ": " + page + " is not a page of " + e.getValue());
                assertTrue(sources.contains(o.get("source").getAsString()), e.getKey() + "/" + page + ": undeclared source");
                assertTrue(o.has("acquire") && o.getAsJsonObject("acquire").has("type"), e.getKey() + "/" + page + ": acquire rule");
            }
            assertEquals(15, spells.size(), e.getKey() + " lists 15 pages");
            Set<String> mine = new HashSet<>();
            for (Page p : pages()) if (p.book().equals(e.getValue())) mine.add(p.id());
            for (JsonElement s : spells) {
                JsonObject o = s.getAsJsonObject();
                if (mine.contains(o.get("page").getAsString())) assertFalse(o.get("exclusive").getAsBoolean(), "a new shared page must not be exclusive (it would leave the basic grimoire): " + o.get("page"));
            }
        }
        // Klaus lists every new Steel page; Nozel seven of the Mercury pages; Mars eight of the Mineral pages plus three older ones
        JsonObject klaus = JsonParser.parseString(read(ROOT.resolve("src/main/resources/data/nusmp/character_spells/klaus.json"))).getAsJsonObject();
        Set<String> kl = new HashSet<>();
        klaus.getAsJsonArray("spells").forEach(x -> kl.add(x.getAsJsonObject().get("page").getAsString()));
        for (Page p : pages()) if (p.book().equals("book_steel")) assertTrue(kl.contains(p.id()), "Klaus lists " + p.id());
    }
}
