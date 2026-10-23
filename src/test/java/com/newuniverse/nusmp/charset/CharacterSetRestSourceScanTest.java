package com.newuniverse.nusmp.charset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the real sources and fails when a rule of docs/magic/character_spells_rest.md stops being true: every page a file of this builder names exists in the real book (or is one the builder added),
 * the added pages follow the older ones and belong to a character, the hook in the grimoire is in place, the common code never touches a client class, every cast is bounded.
 */
class CharacterSetRestSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");
    private static final List<String> REST = List.of("charlotte", "jack", "rill", "dorothy", "zenon", "vanica", "dante", "lucius", "rades", "gadjah", "mars", "kahono", "kiato");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    /** The source files (and an optional section of the first) whose quoted strings are the family page ids of a book. */
    /** "15 spells" project: the page ids the eight Batch classes append to a book (book/pages15/Batch1..8.java). */
    private static List<String> pages15(String book) throws IOException {
        List<String> out = new ArrayList<>();
        for (int n = 1; n <= 8; n++) {
            String t = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/book/pages15/Batch" + n + ".java"));
            Matcher m = Pattern.compile("register\\(\\s*\"" + book + "\"\\s*,").matcher(t);
            if (!m.find()) continue;
            int end = t.indexOf("register(", m.end());
            Matcher d = Pattern.compile("\\b(?:starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"").matcher(t.substring(m.end(), end < 0 ? t.length() : end));
            while (d.find()) out.add(d.group(1));
        }
        return out;
    }

    private static String bookSource(String book) throws IOException {
        String b = "book/";
        return switch (book) {
            case "book_briar" -> read(SRC.resolve(b + "BriarBook.java"));
            case "book_painting" -> read(SRC.resolve(b + "PaintingBook.java"));
            case "book_blood" -> section(read(SRC.resolve(b + "ElementBooks.java")), "public static GrimoireBook blood()", "public static GrimoireBook creation()");
            case "book_dream" -> section(read(SRC.resolve(b + "ElementBooks.java")), "public static GrimoireBook dream()", "public static GrimoireBook antiMagic()");
            case "book_bone" -> section(read(SRC.resolve(b + "ElementBooks.java")), "static List<BookPage> legacyBonePages()", "public static GrimoireBook blood()") + read(SRC.resolve(b + "BoneBook.java"));
            case "book_lightning" -> read(SRC.resolve(b + "LightningBook.java"));
            case "book_gravity" -> read(SRC.resolve(b + "GravityBook.java")) + read(SRC.resolve(b + "GravityWorksPages.java"));
            case "book_slash" -> read(SRC.resolve(b + "SlashBook.java")) + read(SRC.resolve(b + "SlashWorks.java"));
            case "book_soul" -> read(SRC.resolve(b + "SoulBook.java")) + read(SRC.resolve(b + "ext/SoulExt.java")) + read(SRC.resolve("soul/PaladinCasts.java"));
            case "book_soul_corpse" -> read(SRC.resolve(b + "SoulCorpseBook.java")) + read(SRC.resolve(b + "ext/SoulCorpseExt.java")) + read(SRC.resolve("corpse/SoulCorpseAccess.java"));
            case "book_mineral" -> section(read(SRC.resolve(b + "EarthFamilyBooks.java")), "// MINERAL", "return new EarthFamilyBook");
            case "book_song" -> read(SRC.resolve(b + "SongBook.java")) + read(SRC.resolve("song/SongPages.java")) + read(SRC.resolve("song/SongMode.java"));
            case "book_butoh" -> read(SRC.resolve(b + "ButohBook.java"));
            default -> throw new IllegalArgumentException(book);
        };
    }

    private static String section(String s, String from, String to) {
        int a = s.indexOf(from), z = s.indexOf(to, a + from.length());
        assertTrue(a >= 0 && z > a, "section " + from);
        return s.substring(a, z);
    }

    private static List<String> added(String book) throws IOException {
        String file = read(SRC.resolve("book/CharacterPagesRest.java"));
        Matcher m = Pattern.compile("register\\(\"" + book + "\", List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(file);
        List<String> out = new ArrayList<>();
        if (m.find()) { Matcher q = Pattern.compile("BookPage\\.\\w+\\(\"(\\w+)\"").matcher(m.group(1)); while (q.find()) out.add(q.group(1)); }
        return out;
    }

    private static Map<String, JsonObject> files() throws IOException {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        for (String id : REST) out.put(id, JsonParser.parseString(read(DIR.resolve(id + ".json"))).getAsJsonObject());
        return out;
    }

    @Test
    void everyPageAFileNamesExistsInItsBookOrIsOneThatWasAdded() throws IOException {
        for (var e : files().entrySet()) {
            String book = e.getValue().get("book").getAsString();
            String src = bookSource(book);
            List<String> add = added(book);
            for (var sp : e.getValue().getAsJsonArray("spells")) {
                String page = sp.getAsJsonObject().get("page").getAsString();
                assertTrue(add.contains(page) || src.contains("\"" + page + "\"") || pages15(book).contains(page), e.getKey() + ": page " + page + " is not in " + book);
            }
        }
    }

    @Test
    void theAddedPagesAreNewAndEachBelongsToACharacterFileThatMarksItExclusive() throws IOException {
        Map<String, JsonObject> files = files();
        int total = 0;
        for (String book : List.of("book_briar", "book_painting", "book_blood", "book_lightning", "book_mineral", "book_song")) {
            List<String> add = added(book);
            assertFalse(add.isEmpty(), book);
            String src = bookSource(book);
            for (String id : add) {
                total++;
                assertFalse(src.contains("\"" + id + "\""), id + " is not a new id in " + book);
                boolean owned = false;
                for (JsonObject o : files.values()) {
                    if (!o.get("book").getAsString().equals(book)) continue;
                    for (var sp : o.getAsJsonArray("spells")) {
                        JsonObject s = sp.getAsJsonObject();
                        if (s.get("page").getAsString().equals(id) && s.get("exclusive").getAsBoolean()) owned = true;
                    }
                }
                assertTrue(owned, id + " (" + book + ") is not exclusive to any character file");
            }
        }
        assertEquals(15, total);
    }

    @Test
    void everyAddedPageHasItsTextAndItsPolicyLine() throws IOException {
        JsonObject lang = JsonParser.parseString(read(Path.of("tools/lang/character_spells_rest.json"))).getAsJsonObject();
        String policy = read(Path.of("src/main/resources/data/nusmp/armour_policy.json"));
        for (String book : List.of("book_briar", "book_painting", "book_blood", "book_lightning", "book_mineral", "book_song"))
            for (String id : added(book)) {
                assertTrue(lang.has("nusmp.page." + book + "." + id), "lang fragment: nusmp.page." + book + "." + id);
                assertTrue(policy.contains("\"" + id + "\""), "armour_policy.json new_pages: " + id);
            }
    }

    @Test
    void theHooksAreInPlaceAndTheOldLockLinesStay() throws IOException {
        String gb = code(SRC.resolve("book/GrimoireBook.java"));
        assertTrue(gb.contains("new ArrayList<>(familyAll())") && gb.contains("CharacterPagesRest.append(getRegistryName().getPath(), f)") && gb.contains("familyAll().size()"), "the family list is the book's list followed by the added pages");
        assertTrue(gb.contains("CharacterFlourishRest.after(player, instance, p)"), "the flourish follows a counted cast");
        assertTrue(code(SRC.resolve("book/CharacterPages.java")).contains("CharacterPagesRest.isAdded(bookPath, pageId)"), "the added pages belong to their character");
        assertTrue(code(SRC.resolve("charset/CharacterSetBoot.java")).contains("CharacterRestSelfTests.register()"));
        assertTrue(code(SRC.resolve("charset/CharacterRestSelfTests.java")).contains("\"magic.character_spells_rest\""));
        String cs = code(SRC.resolve("book/CharacterSpells.java"));
        for (String line : List.of("lock(\"book_plant\", \"briar_prison\", CanonBook.CHARLOTTE)", "lock(\"book_soul\", \"paladin_conversion\", CanonBook.LUCIUS)", "lock(\"book_song\", \"song_crescendo\", CanonBook.KAHONO)", "lock(\"book_soul_corpse\", \"corpse_jimmy\", CanonBook.RADES)"))
            assertTrue(cs.contains(line), "the old lock line stays: " + line);
        String cb = code(SRC.resolve("grimoire/CanonBook.java"));
        assertTrue(cb.contains("CHARLOTTE(\"Charlotte Roselei\", \"THREE_LEAF\", \"BRIAR\"") && cb.contains("LUCIUS(\"Lucius Zogratis\", \"SPADE\", \"SOUL\"") && cb.contains("MARS(\"Mars\", \"DIAMOND\", \"MINERAL\""));
    }

    @Test
    void theCommonCodeNeverTouchesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"book/CharacterArtsRest.java", "book/CharacterPagesRest.java", "charset/CharacterFlourishRest.java", "charset/FlourishTable.java", "charset/CharacterRestSelfTests.java"}) {
            String c = code(SRC.resolve(f));
            for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance"}) if (c.contains(b)) bad.add(f + " uses " + b);
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void nothingScansTheWorldOrSpawnsEntitiesPerTickAndNoFoodIsTouched() throws IOException {
        for (String f : new String[]{"charset/CharacterFlourishRest.java", "charset/FlourishTable.java"}) {
            String c = code(SRC.resolve(f));
            for (String b : new String[]{"getEntitiesOfClass", "addFreshEntity", "setBlock", "getChunk", "players()", "getFoodData", "causeFoodExhaustion"}) assertFalse(c.contains(b), f + " uses " + b);
        }
        String arts = code(SRC.resolve("book/CharacterArtsRest.java"));
        for (String b : new String[]{"addFreshEntity", "setBlock", "getChunk", "sendToAllPlayers", "broadcast", "getFoodData", "causeFoodExhaustion", "FoodData"}) assertFalse(arts.contains(b), "CharacterArtsRest uses " + b);
        assertEquals(0, count(arts, "while (true)"), "no unbounded loops");
        assertTrue(count(arts, "SpellRuntime.zone(") <= 5, "a handful of zones, each bounded in time");
        assertTrue(count(arts, "MAX_HITS") >= 12, "every hit loop is capped");
        assertTrue(count(arts, "getEntitiesOfClass") <= 3, "world queries only inside the casts");
    }

    @Test
    void everyCastKeepsItsEffectPacketsBounded() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArtsRest.java"));
        Matcher m = Pattern.compile("public static boolean (\\w+)\\(").matcher(arts);
        List<Integer> at = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (m.find()) { at.add(m.start()); names.add(m.group(1)); }
        assertEquals(15, names.size(), "one method per added page");
        for (int k = 0; k < at.size(); k++) {
            String body = arts.substring(at.get(k), k + 1 < at.size() ? at.get(k + 1) : arts.length());
            int packets = count(body, "fx(p,") + count(body, "b.vfx(") + count(body, "VfxSpawn.sendFollowing(") + count(body, "b.castCircle(");
            assertTrue(packets <= 8, names.get(k) + " sends " + packets + " effect packets in the source (a loop counts once per round)");
        }
        String fl = code(SRC.resolve("charset/CharacterFlourishRest.java"));
        assertTrue(fl.contains("GAP = 12") && fl.contains("page.costPercent() <= 0") && fl.contains("CharacterSetConfig.enabled()"), "the flourish is throttled, skips free pages and obeys the switch");
    }

    @Test
    void theTwoSpellsThatCostLifeRefuseWhenTheyCannotBePaid() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArtsRest.java"));
        assertTrue(count(arts, "if (!pay(p,") == 3, "Red Beast, Herd of Red Beasts and Apocalypra Astrauza");
        assertTrue(arts.contains("p.getHealth() <= cost + 2") && arts.contains("selfDamageMult"), "the price never kills");
    }

    private static int count(String s, String what) {
        int n = 0, i = 0;
        while ((i = s.indexOf(what, i)) >= 0) { n++; i += what.length(); }
        return n;
    }
}
