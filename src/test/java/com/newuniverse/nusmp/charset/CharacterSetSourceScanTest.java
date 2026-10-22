package com.newuniverse.nusmp.charset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the real sources and fails when a rule of docs/magic/character_spells.md stops being true: every page a character file names exists in the real book, the pages this feature added follow
 * the older pages in their legacy order, the hooks in the grimoire code are in place, the common code never touches a client class, nothing scans the world per tick, every added page has its text.
 */
class CharacterSetSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");
    private static final Map<String, String> BOOK_FILES = Map.of("book_wind", "WindBook.java", "book_water", "WaterBook.java", "book_fire", "FireBook.java", "book_dark", "DarkBook.java", "book_anti_magic", "AntiMagicBook.java");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static List<String> ids(String src) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?:BookPage\\.\\w+|new BookPage)\\(\"(\\w+)\"").matcher(src);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /** The legacy family page ids of a book, in order (the part of the source before the constructor). */
    private static List<String> legacy(String bookPath) throws IOException {
        String file = read(SRC.resolve("book/" + BOOK_FILES.get(bookPath)));
        int from = file.indexOf("List.of(");
        int to = file.indexOf("public " + BOOK_FILES.get(bookPath).replace(".java", "") + "()");
        if (bookPath.equals("book_anti_magic")) { from = file.indexOf("public AntiMagicBook()"); to = file.indexOf("public static BookPage.Cast paid"); }
        return ids(file.substring(from, to));
    }

    private static List<String> added(String bookPath) throws IOException {
        String file = read(SRC.resolve("book/CharacterPages.java"));
        Matcher m = Pattern.compile("register\\(\"" + bookPath + "\", List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(file);
        return m.find() ? ids(m.group(1)) : List.of();
    }

    @Test
    void theLegacyPagesAreUntouchedAndInOrder() throws IOException {
        assertEquals(List.of("kamaitachi", "gust_lane", "spirit_storm", "towering_tornado", "slicing_wind_emperor", "spirit_of_zephyr"), legacy("book_wind"));
        assertEquals(List.of("waterball", "roar", "cradle", "aqua_javelin", "sea_dragons_nest", "valkyrie_dress", "sea_dragons_wrath", "tide_lance", "scale_mail", "torrent", "mermaid_form", "sea_dragon_god_form", "black_dragon_form"), legacy("book_water"));
        assertEquals(List.of("exploding_fireball", "sol_linea", "calderos", "leo_rugiens", "spiral_flame", "wild_bursting_flame", "ignis_columna", "calidos_brachium", "crimson_lion_king", "flame_armour", "phoenix_flare"), legacy("book_fire"));
        assertEquals(List.of("avidya_slash", "black_hole", "dimension_slash", "death_thrust", "black_blade", "avidya_wild_slash", "black_moon", "iai_slash", "dimension_slash_equinox", "dark_cloak", "shadow_embrace", "black_hole_collapse"), legacy("book_dark"));
        assertEquals(List.of("demon_slayer_slash", "black_divider", "black_meteorite", "black_hurricane", "black_form", "bull_thrust", "infinite_slash", "infinite_slash_equinox"), legacy("book_anti_magic"));
    }

    @Test
    void theAddedPagesAreNewAndAppendedByTheFourBooks() throws IOException {
        for (String book : new String[]{"book_wind", "book_water", "book_fire", "book_dark"}) {
            List<String> add = added(book);
            assertFalse(add.isEmpty(), book);
            for (String id : add) assertFalse(legacy(book).contains(id), id + " is not a new id");
            String src = read(SRC.resolve("book/" + BOOK_FILES.get(book)));
            assertTrue(src.contains("CharacterPages." + book.replace("book_", "") + "(pages)"), book + " appends the pages after the shared ones");
        }
        assertTrue(added("book_wind").size() + legacy("book_wind").size() <= 24, "at most 24 family pages (the unlock mask is 32 bits)");
    }

    @Test
    void everyPageAFileNamesExistsInItsBook() throws IOException {
        try (Stream<Path> s = Files.list(DIR)) {
            for (Path f : (Iterable<Path>) s.filter(p -> p.toString().endsWith(".json"))::iterator) {
                JsonObject o = JsonParser.parseString(read(f)).getAsJsonObject();
                String book = o.get("book").getAsString();
                if (!BOOK_FILES.containsKey(book)) continue;      // the books of the later content builders are checked by their own scan test (CharacterSetRestSourceScanTest)
                List<String> have = new ArrayList<>(legacy(book));
                have.addAll(added(book));
                for (var e : o.getAsJsonArray("spells")) {
                    String page = e.getAsJsonObject().get("page").getAsString();
                    assertTrue(have.contains(page), f.getFileName() + ": page " + page + " is not in " + book + " " + have);
                }
            }
        }
    }

    @Test
    void everyAddedPageBelongsToACharacterFile() throws IOException {
        for (String book : new String[]{"book_wind", "book_water", "book_fire", "book_dark"}) {
            for (String id : added(book)) {
                boolean owned = false;
                try (Stream<Path> s = Files.list(DIR)) {
                    for (Path f : (Iterable<Path>) s.filter(p -> p.toString().endsWith(".json"))::iterator) {
                        JsonObject o = JsonParser.parseString(read(f)).getAsJsonObject();
                        if (!o.get("book").getAsString().equals(book)) continue;
                        for (var e : o.getAsJsonArray("spells")) {
                            JsonObject sp = e.getAsJsonObject();
                            if (sp.get("page").getAsString().equals(id) && sp.has("exclusive") && sp.get("exclusive").getAsBoolean()) owned = true;
                        }
                    }
                }
                assertTrue(owned, id + " (" + book + ") is not exclusive to any character file");
            }
        }
    }

    @Test
    void everyAddedPageHasItsText() throws IOException {
        JsonObject lang = JsonParser.parseString(read(Path.of("tools/lang/character_spells.json"))).getAsJsonObject();
        for (String book : new String[]{"book_wind", "book_water", "book_fire", "book_dark"})
            for (String id : added(book)) assertTrue(lang.has("nusmp.page." + book + "." + id), "lang fragment: nusmp.page." + book + "." + id);
    }

    @Test
    void theGrimoireHooksAreInPlace() throws IOException {
        String gb = code(SRC.resolve("book/GrimoireBook.java"));
        assertTrue(gb.contains("CharacterSets.hidden(i, mode)"), "isUnlocked hides the pages the character never used");
        assertTrue(gb.contains("CharacterSets.onCast(instance, p.id())"), "a successful cast is counted");
        String mp = code(SRC.resolve("multiverse/MasteryPages.java"));
        assertTrue(mp.contains("CharacterSets.opens(p, inst, book, m)") && mp.contains("masteryCost(page, m, n)"), "the character's rule replaces the mastery order only when he has a set");
        String cs = code(SRC.resolve("book/CharacterSpells.java"));
        assertTrue(cs.contains("addedPageOwners") && cs.contains("CharacterSets.listed"), "the old locks stay, the added pages are locked to their character");
        assertTrue(cs.contains("lock(\"book_wind\", \"spirit_of_zephyr\", CanonBook.YUNO)"), "the old lock lines stay");
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("CharacterSetBoot.init(modContainer)"));
        assertTrue(code(SRC.resolve("charset/CharacterSetBoot.java")).contains("\"nusmp-character-spells-server.toml\""));
        assertTrue(code(SRC.resolve("charset/CharacterSetSelfTests.java")).contains("\"magic.character_spells\""));
    }

    @Test
    void theSwitchRestoresTodaysBehaviour() throws IOException {
        String cs = code(SRC.resolve("charset/CharacterSets.java"));
        assertTrue(cs.contains("public static Boolean opens") && cs.contains("if (!enabled()) return null;"), "off: the mastery order decides");
        assertTrue(cs.contains("if (!enabled() || !(i.getSkill() instanceof GrimoireBook b)"), "off: nothing is hidden");
        assertTrue(code(SRC.resolve("charset/CharacterSetConfig.java")).contains(".define(\"characterSpellSets\", true)"));
    }

    @Test
    void theCommonCodeNeverTouchesAClientClass() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"charset", "book/CharacterArts.java", "book/CharacterPages.java"}) {
            Path p = SRC.resolve(f);
            List<Path> files;
            if (Files.isDirectory(p)) { try (Stream<Path> s = Files.list(p)) { files = s.filter(x -> x.toString().endsWith(".java")).toList(); } } else files = List.of(p);
            for (Path x : files) {
                String c = code(x);
                for (String b : new String[]{"net.minecraft.client", "com.mojang.blaze3d", "com.newuniverse.nusmp.client", "Minecraft.getInstance"}) if (c.contains(b)) bad.add(x.getFileName() + " uses " + b);
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void nothingScansTheWorldOrSpawnsEntitiesPerTick() throws IOException {
        for (Path p : new Path[]{SRC.resolve("charset/CharacterSets.java"), SRC.resolve("charset/CharacterSetRegistry.java")}) {
            String c = code(p);
            for (String b : new String[]{"getEntitiesOfClass", "addFreshEntity", "setBlock", "getChunk", "players()"}) assertFalse(c.contains(b), p.getFileName() + " uses " + b);
        }
        String arts = code(SRC.resolve("book/CharacterArts.java"));
        for (String b : new String[]{"addFreshEntity", "setBlock", "getChunk", "sendToAllPlayers", "broadcast"}) assertFalse(arts.contains(b), "CharacterArts uses " + b);
        assertEquals(0, count(arts, "while (true)"), "no unbounded loops");
        assertTrue(count(arts, "SpellRuntime.zone(") <= 5, "a handful of zones, each bounded in time");
        assertTrue(count(arts, "MAX_HITS") >= 8, "every hit loop is capped");
    }

    @Test
    void everyCastKeepsItsEffectPacketsBounded() throws IOException {
        String arts = code(SRC.resolve("book/CharacterArts.java"));
        // one method body = from its signature to the next "public static boolean"
        Matcher m = Pattern.compile("public static boolean (\\w+)\\(").matcher(arts);
        List<int[]> at = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (m.find()) { at.add(new int[]{m.start()}); names.add(m.group(1)); }
        for (int k = 0; k < at.size(); k++) {
            String body = arts.substring(at.get(k)[0], k + 1 < at.size() ? at.get(k + 1)[0] : arts.length());
            int packets = count(body, "fx(p,") + count(body, "b.vfx(") + count(body, "VfxSpawn.sendFollowing(") + count(body, "b.castCircle(");
            assertTrue(packets <= 7, names.get(k) + " sends " + packets + " effect packets in the source (a loop counts once per round)");
        }
    }

    private static int count(String s, String what) {
        int n = 0, i = 0;
        while ((i = s.indexOf(what, i)) >= 0) { n++; i += what.length(); }
        return n;
    }
}
