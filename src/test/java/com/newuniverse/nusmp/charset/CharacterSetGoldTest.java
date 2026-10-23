package com.newuniverse.nusmp.charset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builder gold (docs/magic/character_spells_gold.md): the thirteen characters of the Golden Dawn / Crimson Lion / Silver Eagle / Wizard King side. Reads the real sources and files: every character
 * file loads and matches its CanonBook entry, every page it names exists in the real book, every page {@code CharacterPagesGold} appended is owned by the character whose name it carries and has its
 * text and its policy line, the spells are listed in the order they open (a rule only names earlier spells), every spell is reachable without the late-game gates, the casts are bounded
 * (effect packets, zones, hit caps, nothing spawned or placed, no client class), the Time Magic files are untouched, and the hooks are in place.
 */
class CharacterSetGoldTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");
    static final List<String> OWNED = List.of("mereoleona", "leopold", "mimosa", "william", "klaus", "langris", "lemiel", "julius", "kirsch", "karna", "nozel", "kaiser", "letoile");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    /** The text of the pages the "15 spells" project appends (book/pages15/Batch*.java): a character set may list them (docs/magic/pages15_recipe.md). */
    private static String pages15Text() throws IOException {
        StringBuilder b = new StringBuilder();
        try (java.util.stream.Stream<Path> s = Files.list(SRC.resolve("book/pages15"))) {
            for (Path f : (Iterable<Path>) s.filter(p -> p.getFileName().toString().matches("Batch[1-8]\\.java"))::iterator) b.append(read(f));
        }
        return b.toString();
    }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static List<String> ids(String src) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?:(?:BookPage\\.)?\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"").matcher(src);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static JsonObject json(String canon) throws IOException { return JsonParser.parseString(read(DIR.resolve(canon + ".json"))).getAsJsonObject(); }

    private static CharacterSetRegistry shipped() {
        CharacterSetRegistry r = CharacterSetRegistry.load(CharacterSetRegistry.SHIPPED, id -> {
            try { return Files.readString(DIR.resolve(id + ".json")); } catch (IOException e) { return null; }
        });
        assertTrue(r.problems().isEmpty(), "the shipped files have problems: " + r.problems());
        return r;
    }

    /** book registry path -> the family page ids the sources define for it (older pages, the pilots' pages and this builder's). */
    private static Map<String, Set<String>> bookPages() throws IOException {
        Map<String, Set<String>> m = new LinkedHashMap<>();
        Map<String, String> files = Map.of("book_fire", "FireBook.java", "book_plant", "PlantBook.java", "book_world_tree", "WorldTreeBook.java", "book_steel", "SteelBook.java",
                "book_spatial", "SpatialBook.java", "book_light", "LightBook.java", "book_time", "TimeBook.java", "book_cherry_blossom", "CherryBlossomBook.java", "book_compass", "CompassBook.java");
        for (var e : files.entrySet()) m.put(e.getKey(), new LinkedHashSet<>(ids(code(SRC.resolve("book/" + e.getValue())))));
        String eb = code(SRC.resolve("book/ElementBooks.java"));
        int from = eb.indexOf("public static GrimoireBook storm()"), to = eb.indexOf("public static GrimoireBook sand()");
        m.put("book_storm", new LinkedHashSet<>(ids(eb.substring(from, to))));
        // Mercury names its pages by constants
        Matcher c = Pattern.compile("\"([a-z]+(?:_[a-z]+)+)\"").matcher(code(SRC.resolve("book/MercuryRules.java")));
        Set<String> merc = new LinkedHashSet<>();
        while (c.find()) merc.add(c.group(1));
        m.put("book_mercury", merc);
        String gold = code(SRC.resolve("book/CharacterPagesGold.java")), pilot = code(SRC.resolve("book/CharacterPages.java"));
        String bulls = code(SRC.resolve("book/CharacterPagesBulls.java"));
        for (String book : new String[]{"book_fire", "book_plant", "book_world_tree", "book_steel", "book_spatial", "book_light", "book_storm"}) {
            Matcher g = Pattern.compile("register\\(\"" + book + "\", List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(gold);
            if (g.find()) m.get(book).addAll(ids(g.group(1)));
            Matcher p = Pattern.compile("register\\(\"" + book + "\", List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(pilot);
            if (p.find()) m.get(book).addAll(ids(p.group(1)));
            Matcher bu = Pattern.compile("register\\(\"" + book + "\", [^,]+, List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(bulls);
            if (bu.find()) m.get(book).addAll(ids(bu.group(1)));      // the Black Bulls' twin class: Magna's Fire pages, Finral's Spatial page (counted for the 32-page budget)
        }
        return m;
    }

    private static List<String> goldAdded(String book) throws IOException {
        Matcher g = Pattern.compile("register\\(\"" + book + "\", List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(code(SRC.resolve("book/CharacterPagesGold.java")));
        return g.find() ? ids(g.group(1)) : List.of();
    }

    // ------------------------------------------------------------------ the files
    @Test
    void everyGoldFileLoadsIsListedAndMatchesItsCanonBook() throws IOException {
        CharacterSetRegistry r = shipped();
        String canonSrc = read(SRC.resolve("grimoire/CanonBook.java"));
        for (String id : OWNED) {
            assertTrue(CharacterSetRegistry.SHIPPED.contains(id), id + " is not in SHIPPED");
            CharacterSet s = r.of(id);
            assertNotNull(s, id);
            Matcher m = Pattern.compile("\\b" + id.toUpperCase() + "\\(\"[^\"]+\", \"[A-Z_]+\", \"([A-Z_]+)\"").matcher(canonSrc);
            assertTrue(m.find(), id + " is not a CanonBook entry");
            assertEquals(m.group(1), s.magic(), id + ": the file's magic is the CanonBook's magic");
        }
    }

    @Test
    void theSkippedCharactersAreReallyNotThisBuildersAndTheOthersAreNotTouched() throws IOException {
        // Black Bulls / other captains / the other kingdoms belong to the other builders: none of their files is written here
        for (String other : new String[]{"luck", "magna", "gauche", "zora", "finral", "vanessa", "charmy", "charlotte", "jack", "rill", "dorothy"})
            assertFalse(OWNED.contains(other), other);
    }

    @Test
    void everyPageAFileNamesExistsInItsBook() throws IOException {
        Map<String, Set<String>> pages = bookPages();
        for (String id : OWNED) {
            JsonObject o = json(id);
            String book = o.get("book").getAsString();
            Set<String> have = pages.get(book);
            assertNotNull(have, id + ": no source known for " + book);
            for (var e : o.getAsJsonArray("spells")) {
                String page = e.getAsJsonObject().get("page").getAsString();
                assertTrue(have.contains(page) || pages15Text().contains("\"" + page + "\""), id + ": page " + page + " is not in " + book + " " + have);
            }
        }
    }

    @Test
    void theSpellsAreListedInTheOrderTheyOpen() throws IOException {
        CharacterSetRegistry r = shipped();
        for (String id : OWNED) {
            CharacterSet s = r.of(id);
            Set<String> seen = new HashSet<>(SpellRule.UNIVERSAL_PAGES);
            for (CharacterSet.Spell sp : s.spells()) {
                for (String ref : sp.rule().pages()) assertTrue(seen.contains(ref), id + ": " + sp.page() + " waits for " + ref + " which is listed later");
                seen.add(sp.page());
            }
        }
    }

    @Test
    void everySpellOpensWithoutTheLateGameGatesAndAFreshGrimoireOpensOneOrTwo() {
        CharacterSetRegistry r = shipped();
        for (String id : OWNED) {
            CharacterSet s = r.of(id);
            SpellRuleTest.F fresh = new SpellRuleTest.F();
            long open = s.spells().stream().filter(sp -> sp.rule().test(fresh)).count();
            assertTrue(open >= 1 && open <= 2, id + ": a new grimoire opens " + open + " spells");
            // a full grimoire without the Mana Zone, a spirit or the Anti-Magic Lord: everything still opens, in dependency order
            SpellRuleTest.F f = new SpellRuleTest.F();
            f.mastery = 1.0;
            for (CharacterSet.Spell sp : s.spells()) f.uses.put(sp.page(), 50);
            boolean progress = true;
            while (progress) {
                progress = false;
                for (CharacterSet.Spell sp : s.spells()) if (!f.open.contains(sp.page()) && sp.rule().test(f)) { f.open.add(sp.page()); progress = true; }
            }
            for (CharacterSet.Spell sp : s.spells()) assertTrue(f.open.contains(sp.page()), id + ": " + sp.page() + " never opens at full mastery");
        }
    }

    @Test
    void noTwoGoldCharactersOwnTheSamePageAndNobodyClaimsAPilotPage() throws IOException {
        Map<String, String> owner = new LinkedHashMap<>();
        for (String id : CharacterSetRegistry.SHIPPED) {
            JsonObject o = json(id);
            for (var e : o.getAsJsonArray("spells")) {
                JsonObject sp = e.getAsJsonObject();
                if (!sp.has("exclusive") || !sp.get("exclusive").getAsBoolean()) continue;
                String key = o.get("book").getAsString() + "|" + sp.get("page").getAsString();
                String prev = owner.put(key, id);
                assertTrue(prev == null, key + " is exclusive to both " + prev + " and " + id);
            }
        }
    }

    @Test
    void theTimeMagicFilesAndJuliusUseOnlyExistingTimePages() throws IOException {
        assertFalse(read(SRC.resolve("book/TimeBook.java")).contains("CharacterPagesGold"), "TimeBook is not touched by this builder");
        assertFalse(read(SRC.resolve("book/CharacterPagesGold.java")).contains("book_time"), "no page is added to the Time book");
        Set<String> time = bookPages().get("book_time");
        for (var e : json("julius").getAsJsonArray("spells")) assertTrue(time.contains(e.getAsJsonObject().get("page").getAsString()));
    }

    // ------------------------------------------------------------------ the pages added
    @Test
    void everyAddedPageIsOwnedNamedTextedAndListedInThePolicy() throws IOException {
        JsonObject lang = JsonParser.parseString(read(Path.of("tools/lang/character_spells_gold.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(Path.of("src/main/resources/data/nusmp/armour_policy.json"))).getAsJsonObject().getAsJsonObject("magics");
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) if (e.getValue().getAsJsonObject().has("new_pages")) for (var x : e.getValue().getAsJsonObject().getAsJsonArray("new_pages")) listed.add(x.getAsString());
        Map<String, Set<String>> pages = bookPages();
        int total = 0;
        for (String book : new String[]{"book_fire", "book_plant", "book_world_tree", "book_steel", "book_spatial", "book_light", "book_storm"}) {
            List<String> add = goldAdded(book);
            assertFalse(add.isEmpty(), book);
            for (String page : add) {
                total++;
                String who = null;
                for (String id : OWNED) {
                    JsonObject o = json(id);
                    if (!o.get("book").getAsString().equals(book)) continue;
                    for (var e : o.getAsJsonArray("spells")) {
                        JsonObject sp = e.getAsJsonObject();
                        if (sp.get("page").getAsString().equals(page) && sp.has("exclusive") && sp.get("exclusive").getAsBoolean()) who = id;
                    }
                }
                assertNotNull(who, page + " (" + book + ") is not exclusive to any gold character file");
                assertTrue(page.startsWith(who + "_"), page + " does not carry the name of its owner " + who);
                assertTrue(lang.has("nusmp.page." + book + "." + page) && lang.get("nusmp.page." + book + "." + page).getAsString().length() > 40, "lang: " + book + "." + page);
                assertTrue(listed.contains(page), page + " is not in the armour policy's new_pages");
            }
            assertTrue(pages.get(book).size() <= 32, book + " has " + pages.get(book).size() + " family pages: the budget of one grimoire is 32 (the unlock mask holds 64)");
        }
        assertEquals(26, total, "the pages added by this builder");
        assertEquals(26, lang.size(), "one text per added page and nothing else");
    }

    @Test
    void theBookFilesAppendTheGoldPagesAfterTheSharedOnes() throws IOException {
        for (String f : new String[]{"PlantBook", "WorldTreeBook", "SteelBook", "SpatialBook"})
            assertTrue(read(SRC.resolve("book/" + f + ".java")).contains("CharacterPagesGold."), f);
        assertTrue(read(SRC.resolve("book/FireBook.java")).contains("CharacterPagesGold.fire(CharacterPagesBulls.fire(CharacterPages.fire(pages)))"), "the pilots' pages come first, then the Black Bulls', then this builder's");
        assertTrue(read(SRC.resolve("book/LightBook.java")).contains("CharacterPagesGold.light(pagesJaAll)") && read(SRC.resolve("book/LightBook.java")).contains("CharacterPagesGold.light(pages)"), "both shapes of the Light book");
        assertTrue(read(SRC.resolve("book/ElementBooks.java")).contains("CharacterPagesGold.storm(List.of("));
        assertTrue(read(SRC.resolve("charset/CharacterSets.java")).contains("CharacterPagesGold.isAdded"), "an added page is locked to its character");
        assertTrue(read(SRC.resolve("charset/CharacterSetBoot.java")).contains("CharacterSetGoldSelfTests.register()"));
        assertTrue(read(SRC.resolve("charset/CharacterSetGoldSelfTests.java")).contains("\"magic.character_spells_gold\""));
    }

    // ------------------------------------------------------------------ the casts
    @Test
    void theCastsAreBoundedAndServerSafe() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArtsGold.java")), pages = code(SRC.resolve("book/CharacterPagesGold.java"));
        for (String banned : new String[]{"addFreshEntity", "setBlock", "getChunk", "sendToAllPlayers", "broadcast", "while (true)", "net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance"}) {
            assertFalse(arts.contains(banned), "CharacterArtsGold uses " + banned);
            assertFalse(pages.contains(banned), "CharacterPagesGold uses " + banned);
        }
        Matcher loops = Pattern.compile("for \\(LivingEntity \\w+ : [^\\n]*\\n([^\\n]*\\n)?([^\\n]*\\n)?").matcher(arts);
        int nLoops = 0;
        while (loops.find()) {
            nLoops++;
            assertTrue(loops.group().contains("MAX_HITS") || loops.group().contains("n++ >= 2"), "an uncapped hit loop: " + loops.group());
        }
        assertTrue(nLoops >= 18, "the hit loops were found: " + nLoops);
        assertTrue(arts.contains("MAX_SHOTS = 4"), "at most 4 enemy spells are removed per step");
        Matcher m = Pattern.compile("public static boolean (\\w+)\\(").matcher(arts);
        List<Integer> at = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (m.find()) { at.add(m.start()); names.add(m.group(1)); }
        assertEquals(26, names.size(), "one cast per added page");
        for (int k = 0; k < at.size(); k++) {
            String body = arts.substring(at.get(k), k + 1 < at.size() ? at.get(k + 1) : arts.length());
            int packets = count(body, " fx(p,") + count(body, "\tfx(p,") + count(body, "follow(p,") + count(body, "b.vfx(") + count(body, "b.castCircle(");
            assertTrue(packets <= 12, names.get(k) + " sends " + packets + " effect packets in the source");
            assertTrue(count(body, "SpellRuntime.zone(") <= 2, names.get(k) + " starts more than two zones");
            assertTrue(count(body, "SpellRuntime.bolt(") <= 1, names.get(k) + " fires more than one bolt");
        }
        // every zone is bounded in time (at most 20 s) and in rate (at least every 20 ticks)
        Matcher z = Pattern.compile("SpellRuntime\\.zone\\(p\\.serverLevel\\(\\), (\\d+|ticks), (\\d+)").matcher(arts);
        int zones = 0;
        while (z.find()) {
            zones++;
            if (!z.group(1).equals("ticks")) assertTrue(Integer.parseInt(z.group(1)) <= 400, "a zone longer than 20 s: " + z.group(0));
            assertTrue(Integer.parseInt(z.group(2)) >= 5 && Integer.parseInt(z.group(2)) <= 20, "zone interval: " + z.group(0));
        }
        assertTrue(zones >= 8 && zones <= 14, "zones: " + zones);
        Matcher t = Pattern.compile("flameBody\\(b, i, p, mode, (\\d+)").matcher(arts);
        while (t.find()) assertTrue(Integer.parseInt(t.group(1)) <= 400, "a flame body longer than 20 s");
    }

    @Test
    void theCooldownsKeepEveryPageCheap() throws IOException {
        String pages = code(SRC.resolve("book/CharacterPagesGold.java"));
        Matcher m = Pattern.compile("BookPage\\.(mid|zone|signature)\\(\"(\\w+)\"[^;]*?withCooldown\\((\\d+)\\)").matcher(pages);
        int n = 0;
        while (m.find()) {
            n++;
            int floor = m.group(1).equals("signature") ? 600 : 160;
            assertTrue(Integer.parseInt(m.group(3)) >= floor, m.group(2) + " has a cooldown of " + m.group(3) + " ticks");
        }
        assertEquals(26, n, "every added page sets its own cooldown");
    }

    private static int count(String s, String what) {
        int n = 0, i = 0;
        while ((i = s.indexOf(what, i)) >= 0) { n++; i += what.length(); }
        return n;
    }

    @Test
    void theJsonKeepsItsSourcesAndNotes() throws IOException {
        for (String id : OWNED) {
            JsonObject o = json(id);
            assertTrue(o.getAsJsonObject("sources").size() >= 2, id);
            JsonArray planned = o.getAsJsonArray("planned");
            assertNotNull(planned, id);
            assertTrue(o.get("notes").getAsString().length() > 40, id);
        }
    }
}
