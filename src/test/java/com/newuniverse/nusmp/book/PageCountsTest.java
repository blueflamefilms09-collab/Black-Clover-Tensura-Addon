package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Why: the owner wants every grimoire to have 15 spells ("pages 15" project, docs/magic/page_counts.md). This is the floor under that work: the golden file
 * {@code src/test/resources/pages/page_counts.golden.txt} (made by {@code python3 -B tools/page_counts.py --golden}) holds, for every grimoire book, the family pages it has today, the pages its BASIC
 * grimoire shows and the pages of every character grimoire. The phase 2 builders may only INCREASE these numbers (rule 2: nothing is removed), and when they do they refresh the golden file with the
 * same command.
 *
 * <p>What is checked, all without a game: (1) the golden file is well formed and names exactly the registered books (NUSkills + the generic wiki attributes of WikiBooks); (2) a few simple books whose
 * pages are all written in their own source file are counted here by hand, in Java, independently of the script (the golden total is never below that); (3) when {@code python3} is on the path, the script
 * is run and no number is lower than the golden one (a removed, renamed or hidden page shows up here); (4) the "15 spells" goal: the shortfalls are LISTED (not failed) unless the system property
 * {@code nusmp.pages15=strict} is set, which phase 2 sets when it is finished. Pure (java.nio, regular expressions); the working directory is the project root.
 */
class PageCountsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path GOLDEN = ROOT.resolve("src/test/resources/pages/page_counts.golden.txt");
    private static final int GOAL = 15;

    /** book -> {total, basic, character -> pages visible}. */
    record Row(String book, int total, int basic, Map<String, Integer> chars) {}

    private static Map<String, Row> parse(List<String> lines) {
        Map<String, Row> out = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] halves = line.split("\\|", -1);
            String[] head = halves[0].trim().split("\\s+");
            assertEquals(3, head.length, "book total basic: " + line);
            Map<String, Integer> chars = new LinkedHashMap<>();
            for (String kv : halves[1].trim().split("\\s+")) {
                if (kv.isEmpty()) continue;
                String[] p = kv.split("=");
                chars.put(p[0], Integer.parseInt(p[1]));
            }
            assertTrue(out.put(head[0], new Row(head[0], Integer.parseInt(head[1]), Integer.parseInt(head[2]), chars)) == null, "twice: " + head[0]);
        }
        return out;
    }

    private static Map<String, Row> golden() throws IOException { return parse(Files.readAllLines(GOLDEN, StandardCharsets.UTF_8)); }

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    /** The books the game registers: every {@code book("book_x", ...)} of NUSkills plus one per generic wiki attribute (MagicType name of WikiBooks.NEW_ATTRIBUTES, lower case). */
    private static Set<String> registered() throws IOException {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("book\\(\"(book_[a-z_]+|forbidden_magic)\",").matcher(code(SRC.resolve("skill/NUSkills.java")));
        while (m.find()) out.add(m.group(1));
        String wb = code(SRC.resolve("book/WikiBooks.java"));
        int from = wb.indexOf("NEW_ATTRIBUTES = EnumSet.of("), to = wb.indexOf(");", from);
        Matcher a = Pattern.compile("MagicType\\.([A-Z_]+)").matcher(wb.substring(from, to));
        while (a.find()) out.add("book_" + a.group(1).toLowerCase());
        return out;
    }

    // ------------------------------------------------------------------ the golden file
    @Test
    void theGoldenFileIsWellFormedAndNamesExactlyTheRegisteredBooks() throws IOException {
        Map<String, Row> g = golden();
        assertTrue(g.size() >= 100, "books in the golden file: " + g.size());
        assertEquals(registered(), g.keySet(), "the golden file and the registered books differ: refresh it with python3 -B tools/page_counts.py --golden");
        int pages = 0;
        for (Row r : g.values()) {
            assertTrue(r.total() >= 1 && r.basic() >= 1 && r.basic() <= r.total(), r.book() + ": total " + r.total() + " basic " + r.basic());
            // the engine's own limit: the unlock mask is 64 bits ("Unlocked" + "Unlocked2", book.UnlockMask), so a book can never have more than 64 family pages; and no grimoire may SHOW more than 32
            assertTrue(r.total() <= 64, r.book() + " has " + r.total() + " family pages: the 64-bit unlock mask holds 64");
            assertTrue(r.basic() <= 32, r.book() + "'s basic grimoire shows " + r.basic() + " pages: one grimoire may show 32");
            for (var c : r.chars().entrySet()) assertTrue(c.getValue() >= 1 && c.getValue() <= r.total(), r.book() + "/" + c.getKey());
            pages += r.total();
        }
        assertTrue(pages >= 1000, "family pages in all books: " + pages);
        assertTrue(g.values().stream().mapToInt(r -> r.chars().size()).sum() >= 43, "at least the 43 character grimoires of the 43 shipped sets");
    }

    // ------------------------------------------------------------------ counted by hand, in Java, without the script
    /** Books whose every page is written in the book's own file (no ext class, no character or basic pages): the number of page definitions there is the book's total. */
    private static final Map<String, String> SELF_CONTAINED = Map.of(
            "book_time", "book/TimeBook.java",
            "book_barrier", "book/BarrierBook.java",
            "book_bronze", "book/BronzeBook.java",
            "book_key", "book/KeyBook.java",
            "book_legion", "book/LegionBook.java",
            "book_corundum", "book/CorundumBook.java");

    private static int definitions(String src) {
        Matcher m = Pattern.compile("(?:(?<!Runtime\\.)(?:BookPage\\.)?\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*(?:\"[a-z0-9_]+\"|(?:[A-Z]\\w*\\.)?[A-Z][A-Z0-9_]+)\\s*,").matcher(src);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    @Test
    void sixSimpleBooksCountedByHandAgreeWithTheGoldenFile() throws IOException {
        Map<String, Row> g = golden();
        for (var e : SELF_CONTAINED.entrySet()) {
            int mine = definitions(code(SRC.resolve(e.getValue())));
            assertTrue(mine >= 10, e.getKey() + ": the file defines " + mine + " pages");
            assertTrue(g.get(e.getKey()).total() >= mine, e.getKey() + ": the golden file says " + g.get(e.getKey()).total() + " but the book's own file defines " + mine);
        }
        // Kotodama writes its pages as word("id", ...): 16 of them
        Matcher w = Pattern.compile("\\bword\\(\\s*\"[a-z0-9_]+\"\\s*,").matcher(code(SRC.resolve("book/KotodamaBook.java")));
        int words = 0;
        while (w.find()) words++;
        assertTrue(g.get("book_kotodama").total() >= words && words >= 16, "kotodama words " + words);
        // Fire: 11 own pages + 2 (Yuno-era CharacterPages) + 3 (Magna) + 6 (Mereoleona, Leopold) + 2 basic = 24 (read from the sources of the five files)
        int fire = definitions(code(SRC.resolve("book/FireBook.java")));
        assertEquals(11, fire, "FireBook's own pages");
        assertTrue(g.get("book_fire").total() >= fire + 2 + 3 + 6 + 2, "fire total " + g.get("book_fire").total());
    }

    // ------------------------------------------------------------------ the script, when python is there
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
    void noNumberIsLowerThanTheGoldenFileAndTheSetsFindTheirPages() throws Exception {
        Map<String, Row> g = golden();
        Map<String, Row> now = parse(runScript("--golden"));
        for (Row old : g.values()) {
            Row cur = now.get(old.book());
            assertTrue(cur != null, old.book() + " is no longer a registered book");
            assertTrue(cur.total() >= old.total(), old.book() + ": " + cur.total() + " pages, it had " + old.total() + " (rule 2: nothing is removed)");
            assertTrue(cur.basic() >= old.basic(), old.book() + ": the basic grimoire shows " + cur.basic() + ", it showed " + old.basic());
            for (var c : old.chars().entrySet())
                assertTrue(cur.chars().getOrDefault(c.getKey(), 0) >= c.getValue(), old.book() + "/" + c.getKey() + ": " + cur.chars().get(c.getKey()) + " pages, he had " + c.getValue());
        }
        // --check fails when a character set lists a page its book does not have (the extractor missed a page) or a page id cannot be resolved
        List<String> check = runScript("--check");
        assertTrue(check.stream().noneMatch(l -> l.startsWith("SET PAGE NOT IN BOOK") || l.startsWith("WARN")), String.join("\n", check));
    }

    @Test
    void theFifteenSpellGoalListsTheShortfallsAndFailsOnlyInStrictMode() throws Exception {
        Map<String, Row> now = parse(runScript("--golden"));
        List<String> shortBasic = new ArrayList<>(), shortChar = new ArrayList<>();
        int basicMissing = 0, charMissing = 0;
        for (Row r : now.values()) {
            if (r.basic() < GOAL) { shortBasic.add(r.book() + " " + r.basic()); basicMissing += GOAL - r.basic(); }
            for (var c : r.chars().entrySet()) if (c.getValue() < GOAL) { shortChar.add(r.book() + "/" + c.getKey() + " " + c.getValue()); charMissing += GOAL - c.getValue(); }
        }
        System.out.println("pages15: " + shortBasic.size() + " basic grimoires under " + GOAL + " (" + basicMissing + " spells missing), " + shortChar.size() + " character grimoires under " + GOAL + " (" + charMissing + " missing)");
        if ("strict".equals(System.getProperty("nusmp.pages15"))) {
            assertTrue(shortBasic.isEmpty(), "basic grimoires under " + GOAL + ": " + shortBasic);
            assertTrue(shortChar.isEmpty(), "character grimoires under " + GOAL + ": " + shortChar);
        } else {
            assertTrue(basicMissing >= 0 && charMissing >= 0);      // lenient until phase 2 is done; the numbers are in the build log and in docs/magic/page_counts.md
        }
    }
}
