package com.newuniverse.nusmp.book.pages15;

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

/**
 * Why: batch 2 of the "15 spells for every grimoire" project (Water, Ice, Fluids and Art) adds 93 pages to 17 books and extends two character sets. Nothing can be cast here, so the data is pinned by reading the
 * real files: the ids are unique, readable and new; the tiers, cooldowns and signature count obey {@link Pages15Rules}; every book of the batch gets at least the pages the plan asks for and its basic grimoire
 * reaches 15 shown pages (the same logic as {@code PageCountsTest}: the page counter script is run); the lang fragment, the armour-policy file and the doc cover every page; and the two character set files
 * (noelle, rill) parse, keep their old entries and only name pages their book has. Pure (java.nio, regular expressions, Gson); the working directory is the project root.
 */
class Pages15Batch2Test {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path BATCH = SRC.resolve("book/pages15/Batch2.java");

    /** The minimum new pages the plan (docs/magic/page_counts.md section 8) asks of each book of this batch. */
    private static final Map<String, Integer> MIN_NEW = new LinkedHashMap<>();
    static {
        MIN_NEW.put("book_creation", 12); MIN_NEW.put("book_mist", 11); MIN_NEW.put("book_painting", 10); MIN_NEW.put("book_snow", 7); MIN_NEW.put("book_ice", 6);
        MIN_NEW.put("book_water", 5); MIN_NEW.put("book_demon_water", 5); MIN_NEW.put("book_mucus", 5); MIN_NEW.put("book_mud", 5); MIN_NEW.put("book_food", 4);
        MIN_NEW.put("book_eye", 4); MIN_NEW.put("book_demon_ice", 3); MIN_NEW.put("book_bubble", 3); MIN_NEW.put("book_ice_wedge", 3); MIN_NEW.put("book_gel", 3);
        MIN_NEW.put("book_black_oil", 3); MIN_NEW.put("book_eyeball", 3);
    }

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", ""); }

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
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(SRC.resolve("book/pages15"))).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    @Test
    void everyBookOfTheBatchGetsAtLeastThePagesThePlanAsksForAndNoOtherBookIsTouched() throws IOException {
        Map<String, Integer> per = new LinkedHashMap<>();
        for (Page p : pages()) per.merge(p.book(), 1, Integer::sum);
        assertEquals(MIN_NEW.keySet(), per.keySet(), "Batch2 writes pages for exactly the 17 books of the batch");
        for (var e : MIN_NEW.entrySet()) {
            assertTrue(per.get(e.getKey()) >= e.getValue(), e.getKey() + ": " + per.get(e.getKey()) + " new pages, the plan asks for " + e.getValue());
            assertEquals(2, Pages15Plan.batchOf(e.getKey()), e.getKey() + " belongs to batch 2");
        }
    }

    @Test
    void idsAreUniqueReadableNewAndInsideTheTierRules() throws IOException {
        List<Spec> specs = new ArrayList<>();
        for (Page p : pages()) specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of(), olderIds()));
        Map<String, Integer> sig = new LinkedHashMap<>();
        for (Page p : pages()) if (p.tier() == Tier.SIGNATURE) sig.merge(p.book(), 1, Integer::sum);
        for (var e : sig.entrySet()) assertTrue(e.getValue() <= 1, e.getKey() + " has " + e.getValue() + " signature pages");
        // a sensible mix: the starter / mid / zone pages are the large majority
        long signatures = pages().stream().filter(p -> p.tier() == Tier.SIGNATURE).count();
        assertTrue(signatures <= MIN_NEW.size() && signatures * 8 <= pages().size(), "signature pages: " + signatures);
    }

    @Test
    void theLangFragmentThePolicyFileAndTheDocCoverEveryPage() throws IOException {
        List<Page> pages = pages();
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/pages15_batch2.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/test/resources/armour_policy/pages15_batch2_new_pages.json"))).getAsJsonObject();
        Set<String> listed = new HashSet<>();
        policy.entrySet().forEach(e -> e.getValue().getAsJsonArray().forEach(x -> listed.add(x.getAsString())));
        Map<String, DocRow> rows = new LinkedHashMap<>();
        for (String line : read(ROOT.resolve("docs/magic/pages15_batch2.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) assertTrue(rows.put(r.book() + "/" + r.id(), r) == null, "doc row twice: " + r.id());
        }
        for (Page p : pages) {
            String key = Pages15Rules.langKey(p.book(), p.id());
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, key);
            assertTrue(listed.contains(p.id()), p.id() + " is in the armour-policy file");
            DocRow r = rows.get(p.book() + "/" + p.id());
            assertNotNull(r, "doc row for " + p.id());
            assertNotNull(r.origin(), p.id() + ": origin is original or canon: <source>");
            assertEquals(p.name(), r.name());
            assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier());
            if (!r.origin().original()) assertTrue(r.origin().source().length() >= 8, p.id() + ": a canon page names its source");
        }
        assertEquals(pages.size(), lang.size(), "the fragment has no key that is not a page");
        assertEquals(pages.size(), listed.size());
        assertEquals(pages.size(), rows.size());
        long canon = rows.values().stream().filter(r -> !r.origin().original()).count();
        assertTrue(canon > 0 && canon < rows.size(), "both canon and original pages are marked: canon " + canon);
    }

    @Test
    void theCastsStayInsideTheKitAndTouchNothingOfTheClient() throws IOException {
        String c = code(BATCH);
        assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("nusmp.client") || c.contains("Minecraft.getInstance"), "no client class");
        for (String banned : List.of("level.addFreshEntity", "setBlock(", "EntityType", "getChunk(", "new Thread", "static Map<UUID", "static final Map<UUID", "Player.getUUID", "CHANNEL", "PacketDistributor"))
            assertFalse(c.contains(banned), "Batch2 must not use " + banned);
        // the only waits and zones are the kit's (bounded); the trap wait is clamped to 20 ticks
        assertTrue(c.contains("Math.min(delay, 20)"), "the delayed strike is clamped");
        assertTrue(c.contains("Math.min(fraction, 0.02)"), "the magicule drain is clamped");
        assertTrue(c.contains("Math.min(ticks, 400)") && c.contains("Math.min(ticks, 80)"), "buff and silence durations are clamped");
        assertFalse(c.contains("SpellRuntime."), "all timing goes through Pages15Kit.Ctx");
    }

    @Test
    void theBasicGrimoireOfEveryBookReaches15AndTheTwoCharacterSetsToo() throws Exception {
        List<String> cmd = List.of("python3", "-I", "-B", ROOT.resolve("tools/page_counts.py").toString(), "--check");
        Process p;
        try {
            p = new ProcessBuilder(cmd).directory(ROOT.toFile()).start();
        } catch (IOException noPython) {
            return;      // python3 is not on the path: nothing to check here (PageCountsTest skips the same way)
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(120, TimeUnit.SECONDS));
        assertEquals(0, p.exitValue(), "page_counts.py --check: " + out);
        Map<String, int[]> now = new LinkedHashMap<>();
        Pattern row = Pattern.compile("^(book_[a-z_]+)[ \\t]+(\\d+)[ \\t]+(\\d+)[ \\t]+(\\d+)[ \\t]*(.*)$", Pattern.MULTILINE);
        Matcher m = row.matcher(out);
        Map<String, String> chars = new LinkedHashMap<>();
        while (m.find()) { now.put(m.group(1), new int[]{Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))}); chars.put(m.group(1), m.group(5)); }
        for (String book : MIN_NEW.keySet()) {
            assertTrue(now.containsKey(book), book + " is in the table");
            assertTrue(now.get(book)[1] >= Pages15Rules.GOAL, book + ": the basic grimoire shows " + now.get(book)[1] + " pages, the goal is " + Pages15Rules.GOAL);
        }
        assertTrue(chars.get("book_water").contains("noelle=15"), "Noelle's grimoire: " + chars.get("book_water"));
        assertTrue(chars.get("book_painting").contains("rill=15"), "Rill's grimoire: " + chars.get("book_painting"));
    }

    @Test
    void noelleAndRillKeepTheirOldEntriesGainTheNewOnesAndOnlyNameRealPages() throws IOException {
        Path dir = ROOT.resolve("src/main/resources/data/nusmp/character_spells");
        Set<String> mine = new HashSet<>();
        for (Page p : pages()) mine.add(p.id());
        Set<String> real = olderIds();
        real.addAll(mine);
        Map<String, List<String>> oldFirst = Map.of(
                "noelle", List.of("waterball", "roar", "sea_dragons_nest", "cradle", "point_blank_roar", "valkyrie_dress"),
                "rill", List.of("brushstroke", "spring_of_restriction", "souterrain_groundswell", "sirenes_game", "deux_tempetes", "four_headed_lindworm", "vouivres_shout", "souterrain_giant", "gods_game",
                        "master_of_valhalla", "painted_menagerie", "twilight_of_valhalla", "elemental_quintet"));
        Map<String, List<String>> added = Map.of(
                "noelle", List.of("aqua_javelin", "sea_dragon_god_vortex", "scale_mail", "torrent", "mermaid_form", "tide_lance", "sea_dragons_wrath"),
                "rill", List.of("heat_infused_ice"));
        for (String who : List.of("noelle", "rill")) {
            List<String> bad = new ArrayList<>();
            CharacterSet set = CharacterSetLoader.parse(read(dir.resolve(who + ".json")), bad);
            assertEquals(List.of(), bad, who + ".json parses clean");
            assertNotNull(set, who + ".json parses");
            assertEquals(List.of(), CharacterSetLoader.problems(set), who + ".json has no problems");
            JsonObject json = JsonParser.parseString(read(dir.resolve(who + ".json"))).getAsJsonObject();
            List<String> pagesInFile = new ArrayList<>();
            json.getAsJsonArray("spells").forEach(x -> pagesInFile.add(x.getAsJsonObject().get("page").getAsString()));
            assertEquals(oldFirst.get(who), pagesInFile.subList(0, oldFirst.get(who).size()), who + ": the old entries keep their order (new entries are appended)");
            assertTrue(pagesInFile.containsAll(added.get(who)), who + " lists the new entries");
            for (String page : pagesInFile) assertTrue(real.contains(page), who + " names " + page + ", which is no page of any book");
            json.getAsJsonArray("spells").forEach(x -> {
                JsonObject s = x.getAsJsonObject();
                if (added.get(who).contains(s.get("page").getAsString())) assertFalse(s.get("exclusive").getAsBoolean(), s.get("page") + ": shared pages stay in the basic book");
            });
        }
    }
}
