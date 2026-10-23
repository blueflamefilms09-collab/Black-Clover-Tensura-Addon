package com.newuniverse.nusmp.book.pages15;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.DocRow;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Spec;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.charset.CharacterSet;
import com.newuniverse.nusmp.charset.CharacterSetLoader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: batch 7 ("Nature and Beast") adds 86 pages to 13 books and the owner's goal is that every one of those grimoires shows 15 spells. Nothing here can be played, so the batch is pinned from its
 * own files: ids unique and readable, tiers and cooldowns inside the bounds, at most one signature per book, every book of the batch reaching 15 visible pages (the basic count before the project plus
 * the pages added here, the same arithmetic as {@code PageCountsTest} / {@code page_counts.py --check}), a lang text, a doc row with a valid origin and an armour-policy id for every page, and the six
 * character spell sets this batch extended (they parse without a problem, reach 15 pages, and every page they name is a real page of their book). Pure (java.nio, Gson, regular expressions); the working
 * directory is the project root.
 */
class Pages15Batch7Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path BATCH = SRC.resolve("book/pages15/Batch7.java");

    /** book -> {pages the BASIC grimoire showed before this project, pages this batch must add at least} (docs/magic/page_counts.md section 8, batch 7). */
    private static final Map<String, int[]> PLAN = new LinkedHashMap<>();

    static {
        PLAN.put("book_world_tree", new int[]{1, 14});
        PLAN.put("book_poison", new int[]{4, 11});
        PLAN.put("book_cotton", new int[]{4, 11});
        PLAN.put("book_plant", new int[]{8, 7});
        PLAN.put("book_cherry_blossom", new int[]{9, 6});
        PLAN.put("book_briar", new int[]{9, 6});
        PLAN.put("book_tree", new int[]{10, 5});
        PLAN.put("book_vine", new int[]{10, 5});
        PLAN.put("book_poison_plant", new int[]{10, 5});
        PLAN.put("book_beast", new int[]{10, 5});
        PLAN.put("book_body", new int[]{10, 5});
        PLAN.put("book_fungus", new int[]{12, 3});
        PLAN.put("book_demon_beast", new int[]{12, 3});
    }

    /** character -> book of the six sets this batch extended. */
    private static final Map<String, String> SETS = new LinkedHashMap<>();

    static {
        SETS.put("william", "book_world_tree");
        SETS.put("gordon", "book_poison");
        SETS.put("charmy", "book_cotton");
        SETS.put("mimosa", "book_plant");
        SETS.put("kirsch", "book_cherry_blossom");
        SETS.put("charlotte", "book_briar");
    }

    private record Page(String book, String id, String name, Tier tier, int cooldown) {}

    private static final Pattern DEF = Pattern.compile("\\b(starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"([^\"]+)\"");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException {
        return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

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

    /** The pages of Batch7, with the book of the register block each sits in. */
    private static List<Page> pages() throws IOException {
        String c = code(BATCH);
        List<Page> out = new ArrayList<>();
        Matcher reg = Pattern.compile("register\\(\\s*\"([a-z_]+)\"\\s*,").matcher(c);
        while (reg.find()) {
            String body = c.substring(reg.end(), close(c, c.indexOf('(', reg.start())));
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

    private static Set<String> olderIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        Path own = SRC.resolve("book/pages15");
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(own)).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    private static Map<String, List<Page>> byBook() throws IOException {
        Map<String, List<Page>> m = new LinkedHashMap<>();
        for (Page p : pages()) m.computeIfAbsent(p.book(), k -> new ArrayList<>()).add(p);
        return m;
    }

    @Test
    void theBatchWritesPagesForExactlyItsThirteenBooksAndEveryPageObeysTheRules() throws IOException {
        List<Page> pages = pages();
        assertEquals(86, pages.size(), "the 86 pages of batch 7");
        assertEquals(new LinkedHashSet<>(PLAN.keySet()), new LinkedHashSet<>(byBook().keySet()), "the books of the batch");
        List<Spec> specs = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Page p : pages) {
            assertEquals(7, Pages15Plan.batchOf(p.book()), p.book() + " belongs to batch 7");
            assertTrue(ids.add(p.id()), "id twice in the batch: " + p.id());
            specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        }
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), olderIds()), "problems of the new pages (unique readable ids, tier bounds, one signature per book)");
        for (Page p : pages) assertTrue(Pages15Rules.idProblem(p.id()) == null && Pages15Rules.nameProblem(p.name()) == null, p.id());
    }

    @Test
    void everyBookOfTheBatchReachesFifteenVisiblePagesInItsBasicGrimoire() throws IOException {
        Map<String, List<Page>> by = byBook();
        for (var e : PLAN.entrySet()) {
            int added = by.getOrDefault(e.getKey(), List.of()).size();
            assertTrue(added >= e.getValue()[1], e.getKey() + ": " + added + " new pages, the plan says at least " + e.getValue()[1]);
            assertEquals(null, Pages15Rules.visibleProblem(e.getKey(), e.getValue()[0] + added, true), e.getKey());
            long sig = by.get(e.getKey()).stream().filter(p -> p.tier() == Tier.SIGNATURE).count();
            assertTrue(sig <= 1, e.getKey() + " has " + sig + " signature pages");
        }
    }

    @Test
    void everyPageHasItsLangTextItsDocRowAndItsPolicyLine() throws IOException {
        List<Page> pages = pages();
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch7.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch7_new_pages.json"))).getAsJsonObject();
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) {
            assertTrue(Pages15Plan.batchOf("book_" + e.getKey().toLowerCase(Locale.ROOT)) == 7, e.getKey() + " is a magic of batch 7");
            for (JsonElement x : e.getValue().getAsJsonArray()) assertTrue(listed.add(x.getAsString()), "listed twice: " + x);
        }
        Map<String, DocRow> rows = new LinkedHashMap<>();
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch7.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) assertTrue(rows.put(r.book() + "/" + r.id(), r) == null, "doc row twice: " + r.id());
        }
        int canon = 0;
        for (Page p : pages) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text for " + key);
            assertTrue(listed.contains(p.id()), "armour policy line for " + p.id());
            DocRow r = rows.get(p.book() + "/" + p.id());
            assertNotNull(r, "doc row for " + p.id());
            assertNotNull(r.origin(), p.id() + ": origin is original or canon: <source>");
            if (!r.origin().original()) { canon++; assertTrue(r.origin().source().contains("wiki") || r.origin().source().contains("manga"), p.id() + ": a canon page names its wiki or chapter"); }
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier(), p.id() + ": tier in the doc");
            assertEquals(p.name(), r.name(), p.id() + ": name in the doc");
        }
        assertEquals(pages.size(), lang.size(), "nothing but the pages in the lang fragment");
        assertEquals(pages.size(), listed.size(), "nothing but the pages in the policy file");
        assertEquals(pages.size(), rows.size(), "nothing but the pages in the doc");
        assertEquals(7, canon, "canon pages of the batch: Dracaena Formation, Spirit Light Tree, Magic Flower Bullet, Princess Healing Flower Garden, Holy Hunting, Bogeyman, Holy Howling");
    }

    @Test
    void theSixCharacterSetsParseReachFifteenPagesAndNameOnlyRealPages() throws IOException {
        Map<String, List<Page>> by = byBook();
        JsonObject en = JsonParser.parseString(read(ROOT.resolve("src/main/resources/assets/nusmp/lang/en_us.json"))).getAsJsonObject();
        for (var e : SETS.entrySet()) {
            String json = read(ROOT.resolve("src/main/resources/data/nusmp/character_spells/" + e.getKey() + ".json"));
            List<String> bad = new ArrayList<>();
            CharacterSet set = CharacterSetLoader.parse(json, bad);
            assertEquals(List.of(), bad, e.getKey() + " parses without a problem");
            assertEquals(e.getValue(), set.book(), e.getKey() + "'s book");
            assertEquals(List.of(), CharacterSetLoader.problems(set), e.getKey() + " has no problem");
            assertTrue(set.spells().size() >= 15, e.getKey() + " lists " + set.spells().size() + " pages, the goal is 15");
            Set<String> real = new HashSet<>();
            for (String k : en.keySet()) if (k.startsWith("nusmp.page." + e.getValue() + ".")) real.add(k.substring(("nusmp.page." + e.getValue() + ".").length()));
            Set<String> mine = new HashSet<>();
            for (Page p : by.get(e.getValue())) mine.add(p.id());
            Set<String> listedPages = new HashSet<>();
            for (CharacterSet.Spell s : set.spells()) {
                assertTrue(listedPages.add(s.page()), e.getKey() + ": " + s.page() + " twice");
                assertTrue(mine.contains(s.page()) || real.contains(s.page()), e.getKey() + ": page " + s.page() + " is not a page of " + e.getValue());
            }
            assertTrue(listedPages.containsAll(mine), e.getKey() + " lists every new page of " + e.getValue() + ": missing " + mine.stream().filter(x -> !listedPages.contains(x)).toList());
            JsonArray arr = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("spells");
            for (JsonElement x : arr) {
                JsonObject o = x.getAsJsonObject();
                if (mine.contains(o.get("page").getAsString())) assertTrue(!o.get("exclusive").getAsBoolean(), o.get("page").getAsString() + " is shared with the basic grimoire (not exclusive)");
            }
        }
    }
}
