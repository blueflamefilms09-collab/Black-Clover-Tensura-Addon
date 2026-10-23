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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: batch 1 of the "15 spells for every grimoire" project (Flame, Lightning, Star and Thread: ten books, 92 pages, eight character grimoires) cannot be played here, so what can be shown offline is pinned by
 * reading the real files: the ten books are exactly the plan's batch 1, every page obeys the rules of {@link Pages15Rules} (readable unique id, a tier with its cooldown bounds, at most one signature per book),
 * every page has its lang text, its armour-policy line and its doc row with a valid origin, the mix of tiers is the one the doc promises, every book of the batch SHOWS at least 15 pages in its basic grimoire and
 * every character grimoire of the batch at least 15 (counted by the same script as {@code PageCountsTest}), and the eight character set files parse, only name pages their book has, and every entry's rule is sane.
 * Pure (java.nio, regular expressions, Gson); the working directory is the project root.
 */
class Pages15Batch1Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path BATCH = SRC.resolve("book/pages15/Batch1.java");
    private static final List<String> BOOKS = List.of("book_explosion", "book_storm", "book_thread", "book_magma", "book_sword", "book_reinforcement", "book_fire", "book_star", "book_lightning", "book_demon_fire");
    /** character -> his book: the eight character grimoires of batch 1 and the number of entries this batch added to each set. */
    private static final Map<String, String> CHARACTERS = Map.of("kaiser", "book_storm", "vanessa", "book_thread", "fuegoleon", "book_fire", "leopold", "book_fire",
            "magna", "book_fire", "mereoleona", "book_fire", "gadjah", "book_lightning", "luck", "book_lightning");
    private static final Map<String, Integer> ADDED = Map.of("kaiser", 12, "vanessa", 6, "fuegoleon", 8, "leopold", 10, "magna", 11, "mereoleona", 9, "gadjah", 11, "luck", 7);
    private static final Map<String, Integer> NEW_PAGES = Map.of("book_explosion", 11, "book_storm", 11, "book_thread", 11, "book_magma", 10, "book_sword", 10, "book_reinforcement", 10,
            "book_fire", 9, "book_star", 9, "book_lightning", 8, "book_demon_fire", 3);

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException {
        return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private record Page(String book, String id, String name, Tier tier, int cooldown) {}

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
            int open = c.indexOf('(', reg.start());
            String body = c.substring(reg.end(), close(c, open));
            Matcher d = DEF.matcher(body);
            while (d.find()) {
                Matcher next = DEF.matcher(body);
                int to = next.find(d.end()) ? next.start() : body.length();
                Matcher cd = Pattern.compile("\\.withCooldown\\((\\d+)\\)").matcher(body.substring(d.end(), to));
                Tier t = Tier.valueOf(d.group(1).toUpperCase(Locale.ROOT));
                out.add(new Page(reg.group(1), d.group(2), d.group(3), t, cd.find() ? Integer.parseInt(cd.group(1)) : t.cooldown));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the pages
    @Test
    void theTenBooksAreExactlyBatchOneAndEachGotTheAnnouncedNumberOfPages() throws IOException {
        Map<String, Integer> per = new LinkedHashMap<>();
        for (Page p : pages()) per.merge(p.book(), 1, Integer::sum);
        assertEquals(new LinkedHashSet<>(BOOKS), per.keySet().stream().collect(LinkedHashSet::new, Set::add, Set::addAll), "the books written in Batch1");
        for (String b : BOOKS) {
            assertEquals(1, Pages15Plan.batchOf(b), b + " belongs to batch 1");
            assertTrue(per.get(b) >= NEW_PAGES.get(b), b + ": " + per.get(b) + " new pages, the plan asks for at least " + NEW_PAGES.get(b));
        }
        assertEquals(92, pages().size(), "92 pages in all");
    }

    @Test
    void idsAreReadableUniqueAndInsideTheTierBoundsWithAtMostOneSignaturePerBook() throws IOException {
        List<Spec> specs = new ArrayList<>();
        for (Page p : pages()) specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), olderIds()), "problems of the pages of batch 1");
    }

    @Test
    void theTierMixIsMostlyStarterMidAndZoneWithAtMostOneSignatureAndEveryBookHasAllRoles() throws IOException {
        Map<String, Map<Tier, Integer>> mix = new LinkedHashMap<>();
        for (Page p : pages()) mix.computeIfAbsent(p.book(), k -> new LinkedHashMap<>()).merge(p.tier(), 1, Integer::sum);
        for (var e : mix.entrySet()) {
            int sig = e.getValue().getOrDefault(Tier.SIGNATURE, 0), n = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            assertTrue(sig <= 1, e.getKey() + ": " + sig + " signature pages");
            assertTrue(e.getValue().getOrDefault(Tier.STARTER, 0) >= 1 || n <= 3, e.getKey() + " has no starter page");
            assertTrue(e.getValue().getOrDefault(Tier.MID, 0) + e.getValue().getOrDefault(Tier.ZONE, 0) >= Math.min(n, 2), e.getKey() + " has no mid / zone page");
        }
    }

    @Test
    void noTwoPagesOfABookShareTheSameCastAndEveryNameIsUniqueInItsBook() throws IOException {
        String c = code(BATCH);
        Map<String, Set<String>> casts = new LinkedHashMap<>();
        Matcher reg = Pattern.compile("register\\(\\s*\"([a-z_]+)\"\\s*,").matcher(c);
        while (reg.find()) {
            String body = c.substring(reg.end(), close(c, c.indexOf('(', reg.start())));
            Matcher d = DEF.matcher(body);
            List<int[]> at = new ArrayList<>();
            while (d.find()) at.add(new int[]{d.start(), d.end()});
            Set<String> seen = new HashSet<>();
            for (int k = 0; k < at.size(); k++) {
                String text = body.substring(at.get(k)[1], k + 1 < at.size() ? at.get(k + 1)[0] : body.length()).replaceAll("\\s+", " ").replaceAll("\\.with(Cooldown|Anim)\\([^)]*\\)", "").trim();
                assertTrue(seen.add(text), reg.group(1) + ": two pages with the same cast: " + text);
            }
            casts.put(reg.group(1), seen);
        }
        Set<String> names = new HashSet<>();
        for (Page p : pages()) assertTrue(names.add(p.book() + "/" + p.name()), "name twice in a book: " + p.name());
    }

    private static Set<String> olderIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        try (var s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(SRC.resolve("book/pages15"))).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    // ------------------------------------------------------------------ lang, policy, doc
    @Test
    void everyPageHasItsLangTextItsPolicyLineAndItsDocRowAndNothingElseIsInThoseFiles() throws IOException {
        List<Page> pages = pages();
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch1.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch1_new_pages.json"))).getAsJsonObject();
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) for (JsonElement x : (JsonArray) e.getValue()) listed.add(x.getAsString());
        Map<String, DocRow> rows = new LinkedHashMap<>();
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch1.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) assertTrue(rows.put(r.book() + "/" + r.id(), r) == null, "doc row twice: " + r.book() + "/" + r.id());
        }
        Set<String> mine = new HashSet<>();
        for (Page p : pages) {
            mine.add(p.book() + "/" + p.id());
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text of " + key);
            assertTrue(listed.contains(p.id()), "armour policy file lists " + p.id());
            DocRow r = rows.get(p.book() + "/" + p.id());
            assertTrue(r != null && r.origin() != null, "doc row with a valid origin for " + p.id());
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier(), p.id() + ": tier in the doc");
            assertEquals(p.name(), r.name(), p.id() + ": name in the doc");
            if (!r.origin().original()) assertTrue(r.origin().source().length() >= 8, p.id() + ": a canon origin names its source");
        }
        assertEquals(mine.size(), lang.size(), "lang fragment has exactly the pages of the batch");
        assertEquals(mine, rows.keySet(), "doc rows are exactly the pages of the batch");
        assertEquals(mine.size(), listed.size(), "policy file has exactly the pages of the batch");
        for (var e : policy.entrySet()) assertFalse(e.getValue().getAsJsonArray().isEmpty(), e.getKey());
    }

    // ------------------------------------------------------------------ the character set files
    @Test
    void theEightCharacterSetFilesParseAndEveryNewEntryNamesARealPageOfTheirBook() throws IOException {
        Set<String> batchIds = new HashSet<>();
        for (Page p : pages()) batchIds.add(p.book() + "/" + p.id());
        Set<String> older = olderIds();
        for (var ch : CHARACTERS.entrySet()) {
            JsonObject set = JsonParser.parseString(read(ROOT.resolve("src/main/resources/data/nusmp/character_spells/" + ch.getKey() + ".json"))).getAsJsonObject();
            assertEquals(ch.getKey(), set.get("canon").getAsString());
            assertEquals(ch.getValue(), set.get("book").getAsString());
            JsonArray spells = set.getAsJsonArray("spells");
            Set<String> seen = new HashSet<>();
            int mine = 0;
            for (JsonElement el : spells) {
                JsonObject s = el.getAsJsonObject();
                String page = s.get("page").getAsString();
                assertTrue(seen.add(page), ch.getKey() + ": page twice " + page);
                boolean fromBatch = batchIds.contains(ch.getValue() + "/" + page);
                if (fromBatch) {
                    mine++;
                    assertFalse(s.get("exclusive").getAsBoolean(), ch.getKey() + "/" + page + ": a page of the basic book must not be exclusive");
                    assertEquals("mastery", s.getAsJsonObject("acquire").get("type").getAsString(), ch.getKey() + "/" + page);
                } else if (s.get("note").getAsString().startsWith("Existing generic page")) {
                    mine++;
                    assertTrue(older.contains(page), ch.getKey() + "/" + page + ": an existing page");
                    assertFalse(s.get("exclusive").getAsBoolean());
                }
                if (s.getAsJsonObject("acquire").get("type").getAsString().equals("mastery")) {
                    double min = s.getAsJsonObject("acquire").get("min").getAsDouble();
                    assertTrue(min >= 0 && min <= 1, ch.getKey() + "/" + page + ": mastery " + min);
                }
            }
            assertEquals(ADDED.get(ch.getKey()).intValue(), mine, ch.getKey() + ": entries added by batch 1");
        }
    }

    // ------------------------------------------------------------------ the counts (the same script as PageCountsTest)
    private static List<String> script(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("python3", "-I", "-B", ROOT.resolve("tools/page_counts.py").toString()));
        cmd.addAll(List.of(args));
        Process p;
        try {
            p = new ProcessBuilder(cmd).directory(ROOT.toFile()).start();
        } catch (IOException noPython) {
            return null;
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(120, TimeUnit.SECONDS), "tools/page_counts.py did not finish");
        assertEquals(0, p.exitValue(), "tools/page_counts.py failed: " + new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        return List.of(out.split("\n"));
    }

    @Test
    void everyBookOfTheBatchShowsFifteenPagesInItsBasicGrimoireAndEveryCharacterGrimoireOfTheBatchToo() throws Exception {
        List<String> lines = script("--golden");
        if (lines == null) return;      // no python3 on the path: the numbers are checked by the CI run of PageCountsTest
        Map<String, int[]> basic = new LinkedHashMap<>();
        Map<String, Integer> chars = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] halves = line.split("\\|", -1);
            String[] head = halves[0].trim().split("\\s+");
            basic.put(head[0], new int[]{Integer.parseInt(head[1]), Integer.parseInt(head[2])});
            for (String kv : halves[1].trim().split("\\s+")) if (!kv.isEmpty()) chars.put(head[0] + "/" + kv.split("=")[0], Integer.parseInt(kv.split("=")[1]));
        }
        for (String b : BOOKS) {
            assertTrue(basic.get(b)[1] >= Pages15Rules.GOAL, b + ": the basic grimoire shows " + basic.get(b)[1] + ", the goal is " + Pages15Rules.GOAL);
            assertTrue(basic.get(b)[1] <= Pages15Rules.MAX_VISIBLE, b + " shows too many pages");
            assertTrue(basic.get(b)[0] <= Pages15Rules.MAX_FAMILY, b + " has too many family pages");
        }
        for (var ch : CHARACTERS.entrySet()) {
            int shown = chars.getOrDefault(ch.getValue() + "/" + ch.getKey(), 0);
            assertTrue(shown >= Pages15Rules.GOAL && shown <= Pages15Rules.MAX_VISIBLE, ch.getKey() + ": his grimoire shows " + shown + " pages");
        }
        List<String> check = script("--check");
        assertTrue(check.stream().noneMatch(l -> l.startsWith("SET PAGE NOT IN BOOK") || l.startsWith("WARN")), String.join("\n", check));
    }

    @Test
    void theCustomCastsStayInsideTheKitsBoundsAndTouchNoClientClass() throws IOException {
        String c = code(BATCH);
        assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("nusmp.client") || c.contains("Minecraft.getInstance"), "client class in Batch1");
        assertFalse(c.contains("addFreshEntity") || c.contains("setBlock(") || c.contains("tempBlock") || c.contains("getChunk") || c.contains("getEntitiesOfClass"), "Batch1 spawns, places blocks, loads chunks or scans: use the Ctx");
        assertFalse(Pattern.compile("\\bstatic\\s+(?!final\\b)(?!boolean\\b)(?!ElementBook\\b)(?!List\\b)[A-Za-z<>?,\\s]*\\s\\w+\\s*(=|;)").matcher(c.replaceAll("\\(.*", "")).find(), "mutable static state in Batch1");
        assertTrue(c.contains("Math.min(n, 6)") && c.contains("Math.min(count, 6)"), "the series casts are capped at 6 steps");
    }
}
