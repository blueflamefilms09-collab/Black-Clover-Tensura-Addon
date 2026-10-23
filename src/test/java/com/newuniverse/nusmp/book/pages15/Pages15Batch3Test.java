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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: batch 3 of the "15 spells for every grimoire" project (Wind, Air, Sound, Mind and Soul: 13 books, 87 pages) cannot be played here, so what it promises is pinned by reading its real files: the
 * ids are unique and readable and every tier / cooldown is inside the rules, each book of the batch gets the number of pages the plan asks for and so reaches 15 in the basic grimoire (the old basic count
 * is the golden number from before the batch), no new page is hidden from the basic grimoire by a character set, every page has its lang text, its armour-policy line and its doc row with a valid
 * origin, the four character sets that were extended parse, reach 15 pages and only name pages their book has, and the batch file touches no client class and spawns nothing. Pure (java.nio, Gson,
 * regular expressions); the working directory is the project root.
 */
class Pages15Batch3Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path BATCH = SRC.resolve("book/pages15/Batch3.java");
    private static final Path SETS = ROOT.resolve("src/main/resources/data/nusmp/character_spells");

    /** book -> new pages the plan asks for (docs/magic/page_counts.md section 8, batch 3). */
    private static final Map<String, Integer> NEW = new LinkedHashMap<>();
    /** book -> pages its BASIC grimoire showed before this batch (the golden file as it was). */
    private static final Map<String, Integer> BASIC_BEFORE = new LinkedHashMap<>();

    static {
        String[] b = {"book_dream", "book_illusion", "book_air", "book_smoke", "book_vortex", "book_wing", "book_wind", "book_sound", "book_memory", "book_permeation", "book_soul", "book_soul_corpse", "book_song"};
        int[] n = {11, 10, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 2};
        int[] basic = {4, 5, 8, 8, 8, 8, 9, 9, 9, 9, 9, 9, 13};
        for (int i = 0; i < b.length; i++) {
            NEW.put(b[i], n[i]);
            BASIC_BEFORE.put(b[i], basic[i]);
        }
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

    private static Map<String, List<Page>> byBook() throws IOException {
        Map<String, List<Page>> m = new LinkedHashMap<>();
        for (Page p : pages()) m.computeIfAbsent(p.book(), k -> new ArrayList<>()).add(p);
        return m;
    }

    @Test
    void theBatchWritesExactlyThePlannedPagesOfExactlyItsBooks() throws IOException {
        Map<String, List<Page>> m = byBook();
        assertEquals(NEW.keySet(), m.keySet(), "the books of batch 3");
        int all = 0;
        for (var e : NEW.entrySet()) {
            assertEquals(3, Pages15Plan.batchOf(e.getKey()), e.getKey() + " belongs to batch 3");
            assertTrue(m.get(e.getKey()).size() >= e.getValue(), e.getKey() + " has " + m.get(e.getKey()).size() + " new pages, the plan asks for " + e.getValue());
            all += m.get(e.getKey()).size();
        }
        assertEquals(87, all, "87 pages in all");
    }

    @Test
    void everyBookOfTheBatchReaches15PagesInItsBasicGrimoire() throws IOException {
        Map<String, List<Page>> m = byBook();
        for (var e : BASIC_BEFORE.entrySet())
            assertTrue(e.getValue() + m.get(e.getKey()).size() >= Pages15Rules.GOAL, e.getKey() + ": " + e.getValue() + " + " + m.get(e.getKey()).size() + " is under " + Pages15Rules.GOAL);
        for (var e : m.entrySet()) {
            assertNull(Pages15Rules.visibleProblem(e.getKey(), BASIC_BEFORE.get(e.getKey()) + e.getValue().size(), true));
            assertTrue(e.getValue().stream().filter(p -> p.tier() == Tier.SIGNATURE).count() <= 1, e.getKey() + ": at most one signature page");
        }
    }

    private static void assertNull(Object o) { org.junit.jupiter.api.Assertions.assertNull(o); }

    @Test
    void idsNamesTiersAndCooldownsObeyTheRules() throws IOException {
        List<Spec> specs = new ArrayList<>();
        for (Page p : pages()) specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        // ids that already exist as an older page anywhere under book/ (outside this package)
        Set<String> older = new HashSet<>();
        Pattern def = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(SRC.resolve("book/pages15"))).toList()) {
                Matcher mm = def.matcher(read(f));
                while (mm.find()) older.add(mm.group(1));
            }
        }
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), older));
        // the ids are also free of the saved ids of the whole project (a page id is a save key shared by every book)
        Set<String> seen = new HashSet<>();
        for (Page p : pages()) assertTrue(seen.add(p.id()), "twice: " + p.id());
    }

    @Test
    void everyPageHasItsLangTextItsPolicyLineAndItsDocRow() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch3.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch3_new_pages.json"))).getAsJsonObject();
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) e.getValue().getAsJsonArray().forEach(x -> listed.add(x.getAsString()));
        Map<String, DocRow> rows = new LinkedHashMap<>();
        int canon = 0;
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch3.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) rows.put(r.book() + "/" + r.id(), r);
        }
        for (Page p : pages()) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text for " + key);
            assertTrue(listed.contains(p.id()), "armour policy line for " + p.id());
            DocRow r = rows.get(p.book() + "/" + p.id());
            assertNotNull(r, "doc row for " + p.id());
            assertNotNull(r.origin(), p.id() + ": origin");
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier());
            assertEquals(p.name(), r.name());
            if (!r.origin().original()) {
                canon++;
                assertTrue(r.origin().source().length() >= 12, p.id() + ": a canon page names its source");
            }
        }
        assertEquals(87, rows.size());
        assertTrue(canon >= 15, "canon pages first: " + canon);
        assertEquals(87, lang.size());
        assertEquals(87, listed.size());
    }

    @Test
    void theExtendedCharacterSetsParseReach15AndNameOnlyPagesTheirBookHas() throws IOException {
        Map<String, String> sets = Map.of("dorothy", "book_dream", "yuno", "book_wind", "lucius", "book_soul", "kahono", "book_song", "rades", "book_soul_corpse");
        Set<String> mine = new HashSet<>();
        for (Page p : pages()) mine.add(p.id());
        // all page ids that appear in any source under book/ (older pages)
        StringBuilder all = new StringBuilder();
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java")).toList()) all.append(read(f));
        }
        for (var e : sets.entrySet()) {
            JsonObject o = JsonParser.parseString(read(SETS.resolve(e.getKey() + ".json"))).getAsJsonObject();
            assertEquals(e.getValue(), o.get("book").getAsString());
            JsonArray spells = o.getAsJsonArray("spells");
            assertTrue(spells.size() >= 15, e.getKey() + " lists " + spells.size() + " pages");
            Set<String> seen = new HashSet<>();
            for (JsonElement x : spells) {
                JsonObject s = x.getAsJsonObject();
                String page = s.get("page").getAsString();
                assertTrue(seen.add(page), e.getKey() + ": twice " + page);
                assertTrue(mine.contains(page) || all.indexOf("\"" + page + "\"") >= 0, e.getKey() + ": " + page + " is not a page of the book");
                if (mine.contains(page)) assertFalse(s.get("exclusive").getAsBoolean(), e.getKey() + ": " + page + " must stay in the basic grimoire");
                assertTrue(o.getAsJsonObject("sources").has(s.get("source").getAsString()), e.getKey() + ": undeclared source");
            }
        }
        // no set of ANY character hides a new page from the basic grimoire
        try (Stream<Path> s = Files.list(SETS)) {
            for (Path f : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                for (JsonElement x : JsonParser.parseString(read(f)).getAsJsonObject().getAsJsonArray("spells")) {
                    JsonObject sp = x.getAsJsonObject();
                    if (mine.contains(sp.get("page").getAsString())) assertFalse(sp.get("exclusive").getAsBoolean(), f.getFileName() + ": " + sp.get("page").getAsString());
                }
            }
        }
    }

    @Test
    void theBatchFileIsServerSafeAndBounded() throws IOException {
        String c = code(BATCH);
        for (String bad : List.of("net.minecraft.client", "com.mojang.blaze3d", "nusmp.client", "Minecraft.getInstance", "setBlock(", "addFreshEntity", "new Zombie", "getChunk(", "forceChunk", "static Map<UUID", "ThreadLocal"))
            assertFalse(c.contains(bad), "Batch3 must not use " + bad);
        // every standing effect goes through the kit's zone (capped at 2 per player, 100 ticks, 4+ tick steps), never SpellRuntime directly
        assertFalse(c.contains("SpellRuntime"), "zones only through the kit");
        // the helpers' caps
        assertTrue(c.contains("out.size() >= 6") && c.contains("n >= 8") && c.contains("n >= max"), "the helper caps (6 players, 8 mobs, projectiles)");
        assertTrue(c.contains("BalanceLaw.beginControl"), "hard control takes the shared lock");
    }
}
