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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: batch 5 of the "15 spells for every grimoire" project (Light, Dark and Curse: eleven books, 94 pages, eight character sets extended) cannot be played here, so the data that has to hold is checked by
 * reading the real files: ids unique and readable, the tier's cost and cooldown bounds ({@link Pages15Rules}), at most one signature per book, only this batch's books, every book's BASIC grimoire at 15 or more
 * shown pages (the old basic count from the plan of docs/magic/page_counts.md plus this batch's pages that no character set marks exclusive), the lang fragment and the armour-policy file covering every page, and
 * the eight character set files parsing, every entry's page existing in its book (a page of this batch or an older one) and every character grimoire showing 15 pages. Pure (java.nio, Gson, regular expressions);
 * the working directory is the project root.
 */
class Pages15Batch5Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path SETS = ROOT.resolve("src/main/resources/data/nusmp/character_spells");
    private static final int BATCH = 5;

    /** The basic grimoire's shown pages BEFORE this batch (the plan table of docs/magic/page_counts.md section 8, the "Now (total / basic)" column) and the pages the plan asks for. */
    private static final Map<String, int[]> BEFORE = new LinkedHashMap<>();
    static {
        BEFORE.put("book_bone", new int[]{4, 11});
        BEFORE.put("book_dark", new int[]{5, 10});
        BEFORE.put("book_shadow", new int[]{5, 10});
        BEFORE.put("book_ash", new int[]{5, 10});
        BEFORE.put("book_blood", new int[]{5, 10});
        BEFORE.put("book_light", new int[]{8, 7});
        BEFORE.put("book_curse", new int[]{8, 7});
        BEFORE.put("book_anti_magic", new int[]{8, 7});
        BEFORE.put("forbidden_magic", new int[]{8, 7});
        BEFORE.put("book_curse_warding", new int[]{11, 4});
        BEFORE.put("book_demon_light", new int[]{12, 3});
    }

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
        String c = code(SRC.resolve("book/pages15/Batch5.java"));
        List<Page> out = new ArrayList<>();
        Matcher reg = Pattern.compile("register\\(\\s*\"([a-z_]+)\"\\s*,").matcher(c);
        List<int[]> blocks = new ArrayList<>();
        List<String> books = new ArrayList<>();
        while (reg.find()) {
            blocks.add(new int[]{reg.end(), close(c, c.indexOf('(', reg.start()))});
            books.add(reg.group(1));
        }
        for (int k = 0; k < blocks.size(); k++) {
            String body = c.substring(blocks.get(k)[0], blocks.get(k)[1]);
            Matcher d = DEF.matcher(body);
            while (d.find()) {
                Matcher next = DEF.matcher(body);
                int to = next.find(d.end()) ? next.start() : body.length();
                Matcher cd = Pattern.compile("\\.withCooldown\\((\\d+)\\)").matcher(body.substring(d.end(), to));
                Tier t = Tier.valueOf(d.group(1).toUpperCase(Locale.ROOT));
                out.add(new Page(books.get(k), d.group(2), d.group(3), t, cd.find() ? Integer.parseInt(cd.group(1)) : t.cooldown));
            }
        }
        return out;
    }

    private static JsonObject set(String canon) throws IOException { return JsonParser.parseString(read(SETS.resolve(canon + ".json"))).getAsJsonObject(); }

    private static List<String> setFiles() throws IOException {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(SETS, "*.json")) {
            for (Path f : ds) out.add(f.getFileName().toString().replace(".json", ""));
        }
        out.sort(String::compareTo);
        return out;
    }

    // ------------------------------------------------------------------ the pages
    @Test
    void theBatchWritesPagesForItsElevenBooksOnlyAndEveryPageObeysTheRules() throws IOException {
        List<Page> pages = pages();
        assertTrue(pages.size() >= 92, "the plan asks for 92 pages, found " + pages.size());
        Map<String, Integer> perBook = new LinkedHashMap<>();
        List<Spec> specs = new ArrayList<>();
        for (Page p : pages) {
            assertEquals(BATCH, Pages15Plan.batchOf(p.book()), p.book() + " belongs to batch " + Pages15Plan.batchOf(p.book()));
            assertTrue(BEFORE.containsKey(p.book()), p.book() + " is not a book of batch 5");
            perBook.merge(p.book(), 1, Integer::sum);
            specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        }
        assertEquals(BEFORE.keySet(), perBook.keySet(), "every book of the batch got pages");
        for (var e : BEFORE.entrySet())
            assertTrue(perBook.get(e.getKey()) >= e.getValue()[1], e.getKey() + " needs at least " + e.getValue()[1] + " new pages, has " + perBook.get(e.getKey()));
        Set<String> older = olderIds();
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), older), "problems of the new pages");
        for (Page p : pages) assertFalse(older.contains(p.id()), p.id() + " repeats an older page");
    }

    /** Ids of every older page of any book (book sources except pages15). */
    private static Set<String> olderIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(SRC.resolve("book/pages15"))).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    @Test
    void everyBooksBasicGrimoireShowsAtLeast15PagesAndNoMoreThan32() throws IOException {
        Set<String> exclusive = new HashSet<>();
        for (String c : setFiles()) {
            JsonObject j = set(c);
            for (JsonElement e : j.getAsJsonArray("spells")) {
                JsonObject s = e.getAsJsonObject();
                if (s.get("exclusive").getAsBoolean()) exclusive.add(j.get("book").getAsString() + "|" + s.get("page").getAsString());
            }
        }
        Map<String, Integer> shown = new LinkedHashMap<>();
        for (Page p : pages()) if (!exclusive.contains(p.book() + "|" + p.id())) shown.merge(p.book(), 1, Integer::sum);
        for (var e : BEFORE.entrySet()) {
            int basic = e.getValue()[0] + shown.getOrDefault(e.getKey(), 0);
            assertEquals(null, Pages15Rules.visibleProblem(e.getKey(), basic, true), e.getKey() + " basic grimoire: " + basic);
        }
    }

    @Test
    void theLangFragmentAndTheArmourPolicyFileCoverEveryPage() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch5.json"))).getAsJsonObject();
        Set<String> listed = new HashSet<>();
        for (var e : JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch5_new_pages.json"))).getAsJsonObject().entrySet())
            e.getValue().getAsJsonArray().forEach(x -> listed.add(x.getAsString()));
        List<Page> pages = pages();
        for (Page p : pages) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text for " + key);
            assertTrue(listed.contains(p.id()), "armour policy file lists " + p.id());
        }
        assertEquals(pages.size(), lang.size(), "the fragment holds no key of a page that does not exist");
        assertEquals(pages.size(), listed.size(), "the armour policy file holds no id of a page that does not exist");
    }

    // ------------------------------------------------------------------ the character sets of the batch
    @Test
    void theCharacterSetsOfTheBatchParseListRealPagesAndShowFifteen() throws IOException {
        Set<String> mine = new HashSet<>();
        for (Page p : pages()) mine.add(p.book() + "|" + p.id());
        Set<String> older = olderIds();
        int checked = 0;
        for (String canon : List.of("zenon", "yami", "nacht", "zora", "vanica", "karna", "lemiel", "asta")) {
            JsonObject j = set(canon);
            String book = j.get("book").getAsString();
            assertTrue(BEFORE.containsKey(book), canon + ": book " + book);
            JsonArray spells = j.getAsJsonArray("spells");
            Set<String> seen = new HashSet<>();
            JsonObject sources = j.getAsJsonObject("sources");
            for (JsonElement e : spells) {
                JsonObject s = e.getAsJsonObject();
                String page = s.get("page").getAsString();
                assertTrue(seen.add(page), canon + ": page twice: " + page);
                assertTrue(sources.has(s.get("source").getAsString()), canon + ": source of " + page);
                assertTrue(mine.contains(book + "|" + page) || older.contains(page) || oldQuoted(page), canon + ": " + page + " is not a page of " + book);
            }
            // a rule may only name pages of the set (or the three spirit pages): the loader checks it too, repeated here for the entries added by this batch
            for (JsonElement e : spells) checkRule(canon, e.getAsJsonObject().get("acquire").getAsJsonObject(), seen);
            // mode 0 of the book is always shown, listed or not
            int visible = seen.size() + (seen.contains(firstPageOf(book, mine, older)) ? 0 : 1);
            assertEquals(null, Pages15Rules.visibleProblem(canon, Math.max(visible, 15), true), canon);
            assertTrue(visible >= 15 && visible <= Pages15Rules.MAX_VISIBLE, canon + " grimoire shows " + visible + " pages");
            checked++;
        }
        assertEquals(8, checked);
    }

    private static final Set<String> SPIRIT = Set.of("spirit_dive", "spirit_channeling", "devil_union");

    private static void checkRule(String canon, JsonObject r, Set<String> pages) {
        String type = r.get("type").getAsString();
        if (r.has("page")) assertTrue(pages.contains(r.get("page").getAsString()) || SPIRIT.contains(r.get("page").getAsString()), canon + ": a rule names a page outside the set: " + r.get("page"));
        if (r.has("of")) for (JsonElement x : r.getAsJsonArray("of")) checkRule(canon, x.getAsJsonObject(), pages);
        if (type.equals("mastery")) assertTrue(r.get("min").getAsDouble() >= 0 && r.get("min").getAsDouble() <= 1, canon + ": mastery out of range");
        if (type.equals("page_uses")) assertTrue(r.get("count").getAsInt() >= 1 && r.get("count").getAsInt() <= 1000, canon + ": count out of range");
    }

    /** True when the page id is written in quotes anywhere in the book sources (a page of an older appender whose factory the regex does not know). */
    private static boolean oldQuoted(String page) throws IOException {
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java")).toList()) if (read(f).contains("\"" + page + "\"")) return true;
        }
        return false;
    }

    /** The id of mode 0 of a book of the batch (the one page that is shown even when the set does not list it). */
    private static String firstPageOf(String book, Set<String> mine, Set<String> older) {
        return switch (book) {
            case "book_bone" -> "bone_spear";
            case "book_dark" -> "avidya_slash";
            case "book_shadow" -> "shadow_step";
            case "book_ash" -> "ash_bullets";
            case "book_blood" -> "blood_bullet";
            case "book_light" -> "judgment";
            case "book_anti_magic" -> "demon_slayer_slash";
            default -> "";
        };
    }
}
