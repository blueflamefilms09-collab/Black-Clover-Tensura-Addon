package com.newuniverse.nusmp.charset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Black Bulls' character spell sets (docs/magic/character_spells_bulls.md): the twelve shipped files load with no problem; every page they name is a real page of the real book source; the
 * 20 pages the squad added are appended, new, exclusive to a character, have text and are listed in the armour policy; the hooks are in place; the casts stay bounded (rules 9 and 11); the flair
 * palette names real effect shapes and stays inside its limits. Pure (java.nio, regular expressions, Gson): the working directory is the project root.
 */
class CharacterSetBullsTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");
    static final List<String> BULLS = List.of("vanessa", "charmy", "luck", "magna", "gauche", "gordon", "zora", "grey", "henry", "finral", "nacht", "secre");
    /** canon -> {book, magic}. */
    private static final Map<String, String[]> WHO = new LinkedHashMap<>();
    static {
        WHO.put("vanessa", new String[]{"book_thread", "THREAD"});
        WHO.put("charmy", new String[]{"book_cotton", "COTTON"});
        WHO.put("luck", new String[]{"book_lightning", "LIGHTNING"});
        WHO.put("magna", new String[]{"book_fire", "FLAME"});
        WHO.put("gauche", new String[]{"book_mirror", "MIRROR"});
        WHO.put("gordon", new String[]{"book_poison", "POISON"});
        WHO.put("zora", new String[]{"book_ash", "ASH"});
        WHO.put("grey", new String[]{"book_transmutation", "TRANSMUTATION"});
        WHO.put("henry", new String[]{"book_recombination", "RECOMBINATION"});
        WHO.put("finral", new String[]{"book_spatial", "SPATIAL"});
        WHO.put("nacht", new String[]{"book_shadow", "SHADOW"});
        WHO.put("secre", new String[]{"book_sealing", "SEALING"});
    }
    /** The new pages by book, as the squad's page class lists them. */
    private static final Map<String, List<String>> ADDED = new LinkedHashMap<>();
    static {
        ADDED.put("book_cotton", List.of("fluffy_sheep_cushion"));
        ADDED.put("book_lightning", List.of("thunderclap_crumbling_orb", "thunder_gods_gloves", "keraunos"));
        ADDED.put("book_fire", List.of("exploding_buckshot", "grand_slam", "soul_chain_deathmatch"));
        ADDED.put("book_mirror", List.of("double_reflect_refrain", "mirrors_brigade"));
        ADDED.put("book_poison", List.of("verbotenes_obst", "poison_clouds_abode"));
        ADDED.put("book_ash", List.of("ash_snare_trap", "counter_trap_magic"));
        ADDED.put("book_transmutation", List.of("transformation_guise"));
        ADDED.put("book_recombination", List.of("mana_rocket_punch", "mini_bull"));
        ADDED.put("book_spatial", List.of("fallen_angel_wingbeat"));
        ADDED.put("book_shadow", List.of("dark_hunting_ground", "atonement_monument"));
        ADDED.put("book_sealing", List.of("inverse_release"));
    }

    private static String read(Path p) throws IOException { return Files.readString(p); }

    /** The source of the eight "15 spells for every grimoire" batches (book/pages15/Batch1..8.java): their page ids are pages of the book too. */
    private static String pages15Source() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int n = 1; n <= 8; n++) sb.append(read(SRC.resolve("book/pages15/Batch" + n + ".java")));
        return sb.toString();
    }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static final Pattern PAGE = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");

    private static List<String> ids(String src) {
        List<String> out = new ArrayList<>();
        Matcher m = PAGE.matcher(src);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static String between(String s, String from, String to) {
        int a = s.indexOf(from);
        assertTrue(a >= 0, "not found: " + from);
        int b = s.indexOf(to, a + from.length());
        return s.substring(a, b < 0 ? s.length() : b);
    }

    /** The page ids of the shared (older) book, read from the real sources. */
    private static List<String> legacy(String book) throws IOException {
        Path b = SRC.resolve("book");
        switch (book) {
            case "book_thread": return ids(read(b.resolve("ThreadBook.java")));
            case "book_cotton": return ids(read(b.resolve("CottonBook.java")));
            case "book_lightning": return ids(read(b.resolve("LightningBook.java")));
            case "book_fire": return ids(between(read(b.resolve("FireBook.java")), "List.of(", "public FireBook()"));
            case "book_mirror": { List<String> l = ids(read(b.resolve("MirrorBook.java"))); l.addAll(ids(read(b.resolve("MirrorRebuild.java")))); return l; }
            case "book_ash": return ids(read(b.resolve("AshBook.java")));
            case "book_spatial": return ids(read(b.resolve("SpatialBook.java")));
            case "book_sealing": {
                List<String> l = ids(read(b.resolve("SealingBook.java")));
                l.addAll(ids(read(b.resolve("ext/SealExt.java"))));
                l.addAll(ids(read(SRC.resolve("seal/SecreSeals.java"))));
                return l;
            }
            case "book_poison": return ids(between(read(b.resolve("ElementBooks.java")), "public static GrimoireBook poison()", "public static GrimoireBook reinforcement()"));
            case "book_shadow": return ids(between(read(b.resolve("ElementBooks.java")), "public static GrimoireBook shadow()", "public static GrimoireBook poison()"));
            case "book_transmutation": return constants(read(b.resolve("TransmutationRules.java")));
            case "book_recombination": return constants(read(b.resolve("RecombinationRules.java")));
            default: throw new AssertionError(book);
        }
    }

    /** The page id constants of a Rules class (the rebuilt books name their pages by constants). */
    private static List<String> constants(String src) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^\\s*public static final String ([^;]+);").matcher(src);
        while (m.find()) {
            Matcher q = Pattern.compile("[A-Z_]+ = \"([a-z0-9_]+)\"").matcher(m.group(1));
            while (q.find()) out.add(q.group(1));
        }
        return out;
    }

    private static CharacterSetRegistry shipped() {
        CharacterSetRegistry r = CharacterSetRegistry.load(CharacterSetRegistry.SHIPPED, id -> {
            try { return Files.readString(DIR.resolve(id + ".json")); } catch (IOException e) { return null; }
        });
        assertTrue(r.problems().isEmpty(), "the shipped files have problems: " + r.problems());
        return r;
    }

    @Test
    void theTwelveFilesAreShippedAndLoadWithNoProblem() {
        CharacterSetRegistry r = shipped();
        for (String c : BULLS) {
            assertTrue(CharacterSetRegistry.SHIPPED.contains(c), c + " is not in CharacterSetRegistry.SHIPPED");
            CharacterSet s = r.of(c);
            assertNotNull(s, c);
            assertEquals(WHO.get(c)[0], s.book(), c);
            assertEquals(WHO.get(c)[1], s.magic(), c);
            assertTrue(s.spells().size() >= 3, c + " lists " + s.spells().size() + " spells");
            assertEquals(s.canon(), c);
        }
    }

    @Test
    void theCanonBookEntriesMatch() throws IOException {
        String cb = read(SRC.resolve("grimoire/CanonBook.java"));
        for (String c : BULLS) {
            Matcher m = Pattern.compile("(?m)^\\s*" + c.toUpperCase() + "\\(\"[^\"]+\", \"[A-Z_]+\", \"([A-Z_]+)\"").matcher(cb);
            assertTrue(m.find(), c + " is not a CanonBook entry");
            assertEquals(WHO.get(c)[1], m.group(1), c + ": the file's magic is not the CanonBook's");
        }
        assertFalse(BULLS.contains("letoile"), "Letoile is a Golden Dawn member and belongs to the Golden Dawn builder");
    }

    @Test
    void everyPageAFileNamesIsARealPageOfItsBook() throws IOException {
        for (String c : BULLS) {
            JsonObject o = JsonParser.parseString(read(DIR.resolve(c + ".json"))).getAsJsonObject();
            String book = o.get("book").getAsString();
            List<String> have = new ArrayList<>(legacy(book));
            have.addAll(ADDED.getOrDefault(book, List.of()));
            have.addAll(com.newuniverse.nusmp.book.pages15.Pages15Batch6Ids.of(book));      // the pages the 15-spells project appended (batch 6)
            for (var e : o.getAsJsonArray("spells")) {
                String page = e.getAsJsonObject().get("page").getAsString();
                assertTrue(have.contains(page) || pages15Source().contains("\"" + page + "\""), c + ": page " + page + " is not in " + book + " " + have);      // the "15 spells for every grimoire" batches add pages in book/pages15/BatchN.java
            }
        }
    }

    @Test
    void theAddedPagesAreNewAndMatchTheSourceOfTheirClass() throws IOException {
        String pages = read(SRC.resolve("book/CharacterPagesBulls.java"));
        for (var e : ADDED.entrySet()) {
            String block = between(pages, "register(\"" + e.getKey() + "\"", "));");
            assertEquals(e.getValue(), ids(block), e.getKey() + ": the page class and this test disagree");
            for (String id : e.getValue()) assertFalse(legacy(e.getKey()).contains(id), id + " is not a new id in " + e.getKey());
        }
        int total = 0;
        for (var e : ADDED.values()) total += e.size();
        assertEquals(20, total);
    }

    @Test
    void noBookGoesOverTheTwentyFourFamilyPagesTheUnlockMaskAllows() throws IOException {
        for (var e : ADDED.entrySet()) {
            int n = legacy(e.getKey()).size() + e.getValue().size();
            if (e.getKey().equals("book_fire")) n += 2;      // the pilot's Leo Parma and Salamander's Breath
            assertTrue(n <= 32, e.getKey() + " would have " + n + " family pages (budget 32)");
        }
    }

    @Test
    void everyAddedPageIsExclusiveToACharacterHasTextAndIsInTheArmourPolicy() throws IOException {
        JsonObject lang = JsonParser.parseString(read(Path.of("tools/lang/character_spells_bulls.json"))).getAsJsonObject();
        String policy = read(Path.of("src/main/resources/data/nusmp/armour_policy.json"));
        for (var e : ADDED.entrySet()) {
            for (String id : e.getValue()) {
                boolean owned = false;
                for (String c : BULLS) {
                    JsonObject o = JsonParser.parseString(read(DIR.resolve(c + ".json"))).getAsJsonObject();
                    if (!o.get("book").getAsString().equals(e.getKey())) continue;
                    for (var s : o.getAsJsonArray("spells")) {
                        JsonObject sp = s.getAsJsonObject();
                        if (sp.get("page").getAsString().equals(id) && sp.get("exclusive").getAsBoolean()) owned = true;
                    }
                }
                assertTrue(owned, id + " is not exclusive to any Black Bulls file");
                assertTrue(lang.has("nusmp.page." + e.getKey() + "." + id), "lang fragment: nusmp.page." + e.getKey() + "." + id);
                assertTrue(lang.get("nusmp.page." + e.getKey() + "." + id).getAsString().length() > 40, id + ": the text is too short");
                assertTrue(policy.contains("\"" + id + "\""), id + " is not listed in new_pages of armour_policy.json");
            }
        }
        assertEquals(20, lang.size());
    }

    @Test
    void thePilotFilesAndTheSquadNeverClaimTheSamePage() throws IOException {
        Map<String, String> owner = new LinkedHashMap<>();
        for (String c : CharacterSetRegistry.SHIPPED) {
            JsonObject o = JsonParser.parseString(read(DIR.resolve(c + ".json"))).getAsJsonObject();
            for (var s : o.getAsJsonArray("spells")) {
                JsonObject sp = s.getAsJsonObject();
                if (!sp.get("exclusive").getAsBoolean()) continue;
                String key = o.get("book").getAsString() + "|" + sp.get("page").getAsString();
                assertNull(owner.put(key, c), key + " is exclusive to two characters (" + owner.get(key) + " and " + c + ")");
            }
        }
    }

    @Test
    void theRulesWaitForWhatTheSeriesSaysTheyWaitFor() {
        CharacterSetRegistry r = shipped();
        CharacterSet.Spell drr = r.of("gauche").spell("double_reflect_refrain");
        assertFalse(drr.rule().test(facts(1.0, Map.of(), List.of())), "Double Reflect Refrain needs a Real Double first");
        assertTrue(drr.rule().test(facts(1.0, Map.of("real_double", 2), List.of())));
        CharacterSet.Spell ker = r.of("luck").spell("keraunos");
        assertFalse(ker.rule().test(facts(1.0, Map.of(), List.of())));
        assertTrue(ker.rule().test(facts(0.5, Map.of("thunder_boots", 3), List.of())));
        CharacterSet.Spell rouge = r.of("vanessa").spell("rouge");
        assertFalse(rouge.rule().test(facts(1.0, Map.of(), List.of())), "Rouge awakens after the plain thread has been used");
        CharacterSet.Spell glutton = r.of("charmy").spell("gluttons_banquet");
        assertFalse(glutton.rule().test(facts(1.0, Map.of(), List.of())), "Glutton's Banquet comes after the Sheep Cook");
        CharacterSet.Spell gallus = r.of("nacht").spell("unite_gallus");
        assertFalse(gallus.rule().test(facts(1.0, Map.of("devil_union", 1), List.of())));
        assertTrue(gallus.rule().test(facts(0.0, Map.of("devil_union", 2), List.of())));
        CharacterSet.Spell soul = r.of("magna").spell("soul_chain_deathmatch");
        assertFalse(soul.rule().test(facts(1.0, Map.of(), List.of())));
    }

    private static RuleFacts facts(double mastery, Map<String, Integer> uses, List<String> open) {
        return new RuleFacts() {
            @Override public double mastery() { return mastery; }
            @Override public int uses(String pageId) { return uses.getOrDefault(pageId, 0); }
            @Override public boolean pageOpen(String pageId) { return open.contains(pageId); }
            @Override public int trust() { return 0; }
            @Override public boolean flag(String name) { return false; }
        };
    }

    @Test
    void aBullsGrimoireSeesOnlyHisPagesAndTheBasicBookLosesTheSignatures() {
        CharacterSetRegistry r = shipped();
        assertTrue(r.hidden("luck", "book_lightning", "rising_salim", false, true), "no source gives Rising Salim to Luck");
        assertFalse(r.hidden("luck", "book_lightning", "keraunos", false, true));
        assertFalse(r.hidden("luck", "book_lightning", "chain", true, true), "mode 0 is never hidden");
        assertTrue(r.hidden("", "book_lightning", "keraunos", false, true), "the basic book does not show Keraunos");
        assertTrue(r.hidden("magna", "book_fire", "sol_linea", false, true));
        assertFalse(r.hidden("magna", "book_fire", "grand_slam", false, true));
        assertTrue(r.hidden("gauche", "book_mirror", "mirror_step", false, true));
        assertFalse(r.hidden("kaiser", "book_lightning", "rising_salim", false, true), "a character without a file keeps the whole book");
    }

    // ------------------------------------------------------------------ the sources
    @Test
    void theHooksAreInPlace() throws IOException {
        String el = code(SRC.resolve("book/ElementBook.java"));
        assertTrue(el.contains("CharacterPagesBulls.forMagic(magic.name(), pages)"), "ElementBook appends the squad's pages by magic");
        for (String b : new String[]{"LightningBook", "MirrorBook", "SpatialBook", "SealingBook"}) {
            String fn = b.replace("Book", "").toLowerCase();
            assertTrue(code(SRC.resolve("book/" + b + ".java")).contains("CharacterPagesBulls." + fn + "(pages)"), b);
        }
        assertTrue(code(SRC.resolve("book/FireBook.java")).contains("CharacterPagesBulls.fire(CharacterPages.fire(pages))"));
        assertTrue(code(SRC.resolve("book/CharacterPages.java")).contains("CharacterPagesBulls.isAdded(bookPath, pageId)"));
        assertTrue(code(SRC.resolve("book/GrimoireBook.java")).contains("CharacterFlairBulls.onCast(player, instance, p)"));
        assertTrue(code(SRC.resolve("charset/CharacterSetBoot.java")).contains("CharacterSetBullsSelfTests.register()"));
        assertTrue(code(SRC.resolve("charset/CharacterSetBullsSelfTests.java")).contains("\"magic.character_spells_bulls\""));
        // the pages are appended to the END of the older list, never inserted
        assertTrue(code(SRC.resolve("book/CharacterPagesBulls.java")).contains("CharacterPages.append(base,"));
    }

    @Test
    void theCommonCodeNeverTouchesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/CharacterArtsBulls.java", "book/CharacterPagesBulls.java", "book/CharacterFlairBulls.java", "charset/BullsFlairPalette.java", "charset/CharacterSetBullsSelfTests.java"}) {
            String c = code(SRC.resolve(f));
            for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance"}) if (c.contains(b)) bad.add(f + " uses " + b);
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void nothingScansTheWorldSpawnsEntitiesOrLoadsChunksPerTick() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArtsBulls.java"));
        for (String b : new String[]{"addFreshEntity", "setBlock", "getChunk(", "sendToAllPlayers", "broadcast", "players()", "getAllEntities", "Minecraft"}) assertFalse(arts.contains(b), "CharacterArtsBulls uses " + b);
        assertEquals(0, count(arts, "while (true)"), "no unbounded loops");
        assertTrue(count(arts, "SpellRuntime.zone(") <= 9, "a handful of zones, each bounded in time");
        assertTrue(count(arts, "hasChunkAt") >= 1, "the one teleport tests the chunk before it moves anyone");
        String flair = code(SRC.resolve("book/CharacterFlairBulls.java"));
        for (String b : new String[]{"getEntitiesOfClass", "addFreshEntity", "setBlock", "getChunk(", "players()", "static final Map", "HashMap"}) assertFalse(flair.contains(b), "CharacterFlairBulls uses " + b);
    }

    @Test
    void everyZoneLastsAtMostTwelveSecondsAndEveryHitLoopIsCapped() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArtsBulls.java"));
        Matcher z = Pattern.compile("SpellRuntime\\.zone\\(p\\.serverLevel\\(\\), (\\d+), (\\d+),").matcher(arts);
        int zones = 0;
        while (z.find()) {
            zones++;
            assertTrue(Integer.parseInt(z.group(1)) <= 400, "a zone of " + z.group(1) + " ticks");
            assertTrue(Integer.parseInt(z.group(2)) >= 5, "a zone step every " + z.group(2) + " ticks");
        }
        assertTrue(zones >= 6, "zones found: " + zones);
        for (String loop : new String[]{"GrimoireBook.around(", "GrimoireBook.along("}) {
            int at = 0;
            while ((at = arts.indexOf(loop, at)) >= 0) {
                String tail = arts.substring(at, Math.min(arts.length(), at + 260));
                assertTrue(tail.contains("MAX_HITS") || tail.contains("n >= 4") || tail.contains("best"), "an uncapped loop near: " + tail.substring(0, Math.min(90, tail.length())));
                at += loop.length();
            }
        }
    }

    @Test
    void everyCastKeepsItsEffectPacketsBounded() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArtsBulls.java"));
        Matcher m = Pattern.compile("public static boolean (\\w+)\\(").matcher(arts);
        List<Integer> at = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (m.find()) { at.add(m.start()); names.add(m.group(1)); }
        assertEquals(20, names.size(), "one cast per added page: " + names);
        for (int k = 0; k < at.size(); k++) {
            String body = arts.substring(at.get(k), k + 1 < at.size() ? at.get(k + 1) : arts.length());
            int packets = count(body, "fx(p,") + count(body, "b.vfx(") + count(body, "VfxSpawn.sendFollowing(") + count(body, "b.castCircle(") + count(body, "b.impact(");
            assertTrue(packets <= 8, names.get(k) + " sends " + packets + " effect packets in the source (a loop counts once per round)");
        }
    }

    // ------------------------------------------------------------------ the flair palette (pure)
    @Test
    void theFlairPaletteNamesRealShapesAndStaysBounded() throws IOException {
        String shapes = read(SRC.resolve("vfx/VfxShape.java"));
        assertEquals(BULLS, BullsFlairPalette.canons());
        for (String c : BullsFlairPalette.canons()) {
            BullsFlairPalette.Look look = BullsFlairPalette.of(c);
            for (String n : List.of(look.ring(), look.burst(), look.aura())) assertTrue(Pattern.compile("\\b" + n + "\\b").matcher(shapes).find(), c + ": no VfxShape " + n);
            assertTrue(look.color() != 0, c);
        }
        assertNull(BullsFlairPalette.of("yuno"));
        assertNull(BullsFlairPalette.of(null));
        assertEquals(0, BullsFlairPalette.packetsFor(8));
        assertEquals(1, BullsFlairPalette.packetsFor(12));
        assertEquals(2, BullsFlairPalette.packetsFor(20));
        assertEquals(3, BullsFlairPalette.packetsFor(35));
        assertTrue(BullsFlairPalette.packetsFor(1000) <= BullsFlairPalette.MAX_PACKETS);
        Set<Integer> colours = new LinkedHashSet<>();
        for (String c : BullsFlairPalette.canons()) colours.add(BullsFlairPalette.of(c).color());
        assertTrue(colours.size() >= 11, "a signature colour per character");
    }

    @Test
    void aPlayerGetsAFlairAtMostOncePerGap() {
        assertTrue(BullsFlairPalette.gapOk(100, 0));
        assertFalse(BullsFlairPalette.gapOk(103, 100));
        assertTrue(BullsFlairPalette.gapOk(106, 100));
        assertTrue(BullsFlairPalette.gapOk(5, 100), "the clock went back (a new world): allowed");
    }

    @Test
    void theFlairOnlyRunsForASetAndWhenTheSwitchIsOn() throws IOException {
        String flair = code(SRC.resolve("book/CharacterFlairBulls.java"));
        assertTrue(flair.contains("CharacterSets.enabled()") && flair.contains("CharacterSets.registry().of(canon) == null"));
        assertTrue(flair.contains("BullsFlairPalette.gapOk("));
        assertTrue(flair.contains("packets >= 3"), "the third layer is only for signature pages");
    }

    private static int count(String s, String what) {
        int n = 0, i = 0;
        while ((i = s.indexOf(what, i)) >= 0) { n++; i += what.length(); }
        return n;
    }
}
