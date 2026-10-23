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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Why: batch 8 of "15 spells for every grimoire" (Seal, Transform and Body) writes 94 pages for 14 books and extends the sets of four characters (Secre, Henry, Grey, Kiato). Nothing can be played here, so
 * the batch is pinned by reading its real files: every book of the batch gets at least the pages the plan asks for, ids and tiers obey {@link Pages15Rules}, the lang fragment, the armour-policy file and the
 * doc cover every page, the pages stay bounded (no entity, no block, no client class, and Recombination Magic never goes through a kit cast whose effect would spawn a prop), every book of the batch reaches
 * 15 visible pages in its basic grimoire and every character set of the batch reaches 15 (the page counter script is run when python3 is there), and the four set files parse and name only pages that exist.
 * Pure (java.nio, regular expressions, Gson); the working directory is the project root.
 */
class Pages15Batch8Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path BATCH = ROOT.resolve("src/main/java/com/newuniverse/nusmp/book/pages15/Batch8.java");

    /** Book -> pages the plan (docs/magic/page_counts.md section 8) asks batch 8 for at least. */
    private static final Map<String, Integer> MINIMUM = new LinkedHashMap<>();
    static {
        MINIMUM.put("book_sealing", 10); MINIMUM.put("book_skin", 10); MINIMUM.put("book_tongue", 10); MINIMUM.put("book_switching", 10);
        MINIMUM.put("book_recombination", 9); MINIMUM.put("book_transmutation", 8); MINIMUM.put("book_butoh", 6); MINIMUM.put("book_modification", 5);
        MINIMUM.put("book_hair", 5); MINIMUM.put("book_nail", 5); MINIMUM.put("book_scale", 5); MINIMUM.put("book_spike", 5);
        MINIMUM.put("book_chain", 3); MINIMUM.put("book_barrier", 3);
    }
    /** Character -> {book, entries the set file must have at least}. */
    private static final Map<String, String> SETS = Map.of("secre", "book_sealing", "henry", "book_recombination", "grey", "book_transmutation", "kiato", "book_butoh");

    private record Page(String book, String id, String name, Tier tier, int cooldown, String body) {}

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", ""); }

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

    private static final Pattern DEF = Pattern.compile("Pages15Kit\\.(starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"([^\"]+)\"");

    private static List<Page> pages() throws IOException {
        String c = code(BATCH);
        List<Page> out = new ArrayList<>();
        Matcher reg = Pattern.compile("register\\(\\s*\"([a-z_]+)\"\\s*,").matcher(c);
        while (reg.find()) {
            int open = c.indexOf('(', reg.start());
            String body = c.substring(reg.end(), close(c, open));
            Matcher d = DEF.matcher(body);
            List<int[]> spans = new ArrayList<>();
            List<String[]> found = new ArrayList<>();
            while (d.find()) {
                spans.add(new int[]{d.start(), d.end()});
                found.add(new String[]{d.group(1), d.group(2), d.group(3)});
            }
            for (int k = 0; k < found.size(); k++) {
                int to = k + 1 < spans.size() ? spans.get(k + 1)[0] : body.length();
                String seg = body.substring(spans.get(k)[1], to);
                Matcher cd = Pattern.compile("\\.withCooldown\\((\\d+)\\)").matcher(seg);
                Tier t = Tier.valueOf(found.get(k)[0].toUpperCase(Locale.ROOT));
                out.add(new Page(reg.group(1), found.get(k)[1], found.get(k)[2], t, cd.find() ? Integer.parseInt(cd.group(1)) : t.cooldown, seg));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ the pages themselves
    @Test
    void everyBookOfTheBatchGetsAtLeastThePagesThePlanAsksForAndNothingElse() throws IOException {
        Map<String, Integer> per = new LinkedHashMap<>();
        for (Page p : pages()) per.merge(p.book(), 1, Integer::sum);
        assertEquals(MINIMUM.keySet(), per.keySet(), "the batch writes pages for exactly its 14 books");
        for (var e : MINIMUM.entrySet()) assertTrue(per.get(e.getKey()) >= e.getValue(), e.getKey() + " has " + per.get(e.getKey()) + " new pages, the plan asks for " + e.getValue());
        for (String book : per.keySet()) assertEquals(8, Pages15Plan.batchOf(book), book + " belongs to batch 8");
    }

    @Test
    void idsTiersAndCooldownsObeyThePageRules() throws IOException {
        List<Spec> specs = new ArrayList<>();
        Map<String, Integer> signatures = new LinkedHashMap<>();
        for (Page p : pages()) {
            specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
            if (p.tier() == Tier.SIGNATURE) signatures.merge(p.book(), 1, Integer::sum);
        }
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), olderIds()), "problems of the new pages");
        for (var e : signatures.entrySet()) assertTrue(e.getValue() <= 1, e.getKey() + " has " + e.getValue() + " signature pages");
        Set<String> names = new HashSet<>();
        for (Page p : pages()) assertTrue(names.add(p.book() + "/" + p.name()), "name twice in a book: " + p.name());
    }

    private static Set<String> olderIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        Path book = ROOT.resolve("src/main/java/com/newuniverse/nusmp/book");
        try (var s = Files.walk(book)) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(book.resolve("pages15"))).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    @Test
    void theFragmentThePolicyFileAndTheDocCoverEveryPage() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch8.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch8_new_pages.json"))).getAsJsonObject();
        Map<String, DocRow> rows = new LinkedHashMap<>();
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch8.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) rows.put(r.book() + "/" + r.id(), r);
        }
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) for (JsonElement x : (JsonArray) e.getValue()) listed.add(e.getKey() + "/" + x.getAsString());
        List<Page> pages = pages();
        assertEquals(pages.size(), lang.size(), "one lang line per page and no other");
        assertEquals(pages.size(), rows.size(), "one doc row per page and no other");
        assertEquals(pages.size(), listed.size(), "one policy line per page and no other");
        int canon = 0;
        for (Page p : pages) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text for " + key);
            assertTrue(listed.contains(p.book().replace("book_", "").toUpperCase(Locale.ROOT) + "/" + p.id()), "policy line for " + p.id());
            DocRow r = rows.get(p.book() + "/" + p.id());
            assertNotNull(r, "doc row for " + p.id());
            assertNotNull(r.origin(), p.id() + ": origin");
            assertEquals(p.name(), r.name());
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier());
            if (!r.origin().original()) canon++;
        }
        System.out.println("pages15 batch 8: " + pages.size() + " pages, " + canon + " marked canon (ability or spell from a source), " + (pages.size() - canon) + " original");
    }

    // ------------------------------------------------------------------ bounded and server friendly (rules 9 and 11)
    @Test
    void theBatchSpawnsNothingPlacesNothingAndTouchesNoClientClass() throws IOException {
        String c = code(BATCH);
        for (String bad : List.of("addFreshEntity", "setBlock(", "net.minecraft.client", "com.mojang.blaze3d", "nusmp.client", "Minecraft.getInstance", "MagicProps.spawn", "getChunk(", "ServerLevel.getEntities(", "static final Map<UUID", "new Thread"))
            assertFalse(c.contains(bad), "Batch8 must not use " + bad);
        assertTrue(c.contains("MobEffectInstance") && c.contains("Pages15Kit.cast("), "the sanity of the scan itself");
    }

    @Test
    void recombinationPagesNeverUseAKitCastWhoseEffectWouldSpawnAProp() throws IOException {
        // ElementBook.vfx spawns a MagicPropEntity for every effect of Recombination Magic, so its pages send effects through Fx / VfxSpawn instead
        Pattern kitCast = Pattern.compile("Pages15Kit\\.(bolt|volley|cone|nova|field|line|dash|bind|empower|shot|healAllies)\\(");
        int n = 0;
        for (Page p : pages()) {
            if (!p.book().equals("book_recombination")) continue;
            n++;
            assertFalse(kitCast.matcher(p.body()).find(), p.id() + " uses a kit cast, whose effect would spawn a prop for this book");
        }
        assertTrue(n >= 9);
    }

    // ------------------------------------------------------------------ 15 visible pages (the page counter script) and the character sets
    private static List<String> script(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("python3", "-I", "-B", ROOT.resolve("tools/page_counts.py").toString()));
        cmd.addAll(List.of(args));
        Process p;
        try {
            p = new ProcessBuilder(cmd).directory(ROOT.toFile()).start();
        } catch (IOException noPython) {
            assumeTrue(false, "python3 is not on the path: " + noPython.getMessage());
            return List.of();
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(120, TimeUnit.SECONDS), "tools/page_counts.py did not finish");
        assertEquals(0, p.exitValue(), "tools/page_counts.py failed");
        return List.of(out.split("\n"));
    }

    @Test
    void everyBookOfTheBatchShowsFifteenPagesAndEverySetOfTheBatchHasFifteen() throws Exception {
        Map<String, int[]> basic = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> chars = new LinkedHashMap<>();
        for (String line : script("--golden")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] halves = line.split("\\|", -1);
            String[] head = halves[0].trim().split("\\s+");
            basic.put(head[0], new int[]{Integer.parseInt(head[1]), Integer.parseInt(head[2])});
            Map<String, Integer> m = new LinkedHashMap<>();
            for (String kv : halves[1].trim().split("\\s+")) if (!kv.isEmpty()) m.put(kv.split("=")[0], Integer.parseInt(kv.split("=")[1]));
            chars.put(head[0], m);
        }
        for (String book : MINIMUM.keySet()) {
            assertTrue(basic.containsKey(book), book + " is in the counter");
            assertTrue(basic.get(book)[1] >= Pages15Rules.GOAL, book + " shows " + basic.get(book)[1] + " pages in the basic grimoire");
            assertTrue(basic.get(book)[1] <= Pages15Rules.MAX_VISIBLE);
        }
        for (var s : SETS.entrySet()) {
            int shown = chars.get(s.getValue()).getOrDefault(s.getKey(), 0);
            assertTrue(shown >= Pages15Rules.GOAL, s.getKey() + " shows " + shown + " pages, the goal is " + Pages15Rules.GOAL);
            assertTrue(shown <= Pages15Rules.MAX_VISIBLE);
        }
        List<String> check = script("--check");
        assertTrue(check.stream().noneMatch(l -> l.startsWith("SET PAGE NOT IN BOOK") || l.startsWith("WARN")), String.join("\n", check));
    }

    @Test
    void theFourSetFilesParseNameRealPagesAndHaveNoDuplicate() throws Exception {
        for (var s : SETS.entrySet()) {
            JsonObject o = JsonParser.parseString(read(ROOT.resolve("src/main/resources/data/nusmp/character_spells/" + s.getKey() + ".json"))).getAsJsonObject();
            assertEquals(s.getKey(), o.get("canon").getAsString());
            assertEquals(s.getValue(), o.get("book").getAsString());
            Set<String> sources = o.getAsJsonObject("sources").keySet();
            List<String> ids = script("--ids", s.getValue());
            Set<String> real = new HashSet<>();
            for (String line : ids) {
                Matcher m = Pattern.compile("^\\s*\\d+\\s+([a-z0-9_]+)\\s").matcher(line);
                if (m.find()) real.add(m.group(1));
            }
            Set<String> seen = new HashSet<>();
            JsonArray spells = o.getAsJsonArray("spells");
            assertTrue(spells.size() >= 12, s.getKey() + " lists only " + spells.size());
            for (JsonElement e : spells) {
                JsonObject sp = e.getAsJsonObject();
                String page = sp.get("page").getAsString();
                assertTrue(real.contains(page), s.getKey() + " lists " + page + ", which " + s.getValue() + " does not have");
                assertTrue(seen.add(page), s.getKey() + " lists " + page + " twice");
                assertTrue(sources.contains(sp.get("source").getAsString()), s.getKey() + "/" + page + ": undeclared source");
            }
        }
    }
}
