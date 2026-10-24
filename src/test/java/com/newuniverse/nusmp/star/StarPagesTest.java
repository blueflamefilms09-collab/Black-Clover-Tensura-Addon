package com.newuniverse.nusmp.star;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.pages15.Pages15Rules;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.DocRow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the seven new pages of the Star book are appended LAST (rule 2) and each needs its tier, its readable id, a lang text, a line in the armour policy and a row in the doc with its canon source. The page list is
 * read from the source (it needs the game to build), the same way the Pages15 tests read their batches. Also pinned: the ids are unique across ALL books, no older page of the Star book was touched, Halation is not
 * a page, and the Quintile texts say "unverified canon".
 */
class StarPagesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) {
        try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + p, e); }
    }

    private record Page(String factory, String id, String name, int cooldown) {}

    private static List<Page> pages() {
        String src = read(JAVA.resolve("star/StarPages.java")).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
        Matcher m = Pattern.compile("Pages15Kit\\.(starter|mid|zone|signature)\\(\"([a-z_]+)\", \"([^\"]+)\", [^\\n]*?\\)\\.withCooldown\\((\\d+)\\)").matcher(src);
        List<Page> out = new ArrayList<>();
        while (m.find()) out.add(new Page(m.group(1), m.group(2), m.group(3), Integer.parseInt(m.group(4))));
        return out;
    }

    private static String idOf(String id) { return id; }

    @Test
    void theSevenPagesAreInTheWrittenOrderWithTheirTiersAndTheCooldownsOfTheirTier() {
        List<Page> pages = pages();
        assertEquals(7, pages.size());
        List<String> ids = new ArrayList<>();
        for (Page p : pages) ids.add(idOf(p.id()));
        assertEquals(StarRules.NEW_PAGES, ids, "the page list is the rules' list, in order");
        assertEquals(List.of("starter", "mid", "mid", "zone", "zone", "zone", "zone"), pages.stream().map(Page::factory).toList());
        int signatures = 0;
        for (Page p : pages) {
            Pages15Rules.Tier tier = Pages15Rules.Tier.valueOf(p.factory().toUpperCase());
            assertNull(Pages15Rules.idProblem(idOf(p.id())), idOf(p.id()));
            assertNull(Pages15Rules.nameProblem(p.name()), p.name());
            if (p.factory().equals("signature")) signatures++;
            if (!idOf(p.id()).equals(StarRules.SEED))
                assertTrue(p.cooldown() >= tier.minCooldown && p.cooldown() <= tier.maxCooldown, idOf(p.id()) + " cooldown " + p.cooldown() + " inside " + tier);
        }
        assertEquals(0, signatures, "Star already has its one signature page (meteor_swarm)");
        assertEquals(20, pages.stream().filter(p -> p.id().equals("star_seed")).findFirst().get().cooldown(), "Star Seed asks 20 ticks, the one deliberate exception (a star is cheap; the config clamps 10 to 80)");
        assertTrue(Pages15Rules.MAX_NEW_PER_BOOK >= 9 + 7, "the Star book stays inside the 20 added pages of a book (9 of Pages15 + 7)");
        assertTrue(15 + 7 <= Pages15Rules.MAX_FAMILY);
    }

    @Test
    void theIdsAreNewAcrossEveryBookAndHalationIsNotAPage() throws IOException {
        Set<String> older = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        try (var s = Files.walk(JAVA.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java")).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) older.add(m.group(1));
            }
        }
        for (String id : StarRules.NEW_PAGES) assertFalse(older.contains(id), id + " is already a page of another book");
        assertFalse(older.contains("halation"));
        for (String id : List.of("star_shot", "starfall", "constellation", "comet", "star_shield", "supernova", "quartile_flagellum", "twinkle_dart", "star_conjunction", "never_never_land", "star_beam",
                "star_chain", "orbit_barrage", "stardust_field", "meteor_swarm"))
            assertTrue(older.contains(id), "the older page " + id + " is still defined");
    }

    @Test
    void everyPageHasItsLangTextItsPolicyLineAndItsDocRow() throws IOException {
        JsonObject lang = JsonParser.parseString(read(ROOT.resolve("tools/lang/star_magic.json"))).getAsJsonObject();
        assertEquals(7, lang.size(), "the fragment has exactly the new pages");
        for (String id : StarRules.NEW_PAGES) {
            String key = Pages15Rules.langKey("book_star", id);
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() >= 30, "lang text of " + key);
            assertFalse(lang.get(key).getAsString().contains("Halation"));
        }
        for (String id : List.of(StarRules.QUINTILE_SCUTUM, StarRules.QUINTILE_HASTA, StarRules.QUINTILE_FLAGELLUM))
            assertTrue(lang.get(Pages15Rules.langKey("book_star", id)).getAsString().contains("unverified canon"), id + " says unverified canon");
        assertTrue(lang.get(Pages15Rules.langKey("book_star", StarRules.BLACK_NEVERLAND)).getAsString().contains("ch. 381 to 383"));
        for (String id : List.of(StarRules.SCUTUM, StarRules.HASTA, StarRules.BLACK_NEVERLAND, StarRules.QUINTILE_SCUTUM, StarRules.QUINTILE_HASTA, StarRules.QUINTILE_FLAGELLUM))
            assertTrue(lang.get(Pages15Rules.langKey("book_star", id)).getAsString().contains("Grinberryall heir"), id + " says it needs the heir mark");
        JsonObject policy = JsonParser.parseString(read(ROOT.resolve("src/main/resources/data/nusmp/armour_policy.json"))).getAsJsonObject();
        JsonArray listed = policy.getAsJsonObject("magics").getAsJsonObject("STAR").getAsJsonArray("new_pages");
        List<String> listedIds = new ArrayList<>();
        for (JsonElement e : listed) listedIds.add(e.getAsString());
        assertEquals(StarRules.NEW_PAGES, listedIds, "armour_policy.json lists the new pages of STAR in order");
        Map<String, DocRow> rows = new LinkedHashMap<>();
        for (String line : read(ROOT.resolve("docs/vfx_remake/star_ingame.md")).split("\n")) {
            DocRow r = Pages15Rules.parseDocRow(line);
            if (r != null) rows.put(r.id(), r);
        }
        List<Page> pages = pages();
        for (Page pg : pages) {
            String id = idOf(pg.id());
            DocRow r = rows.get(id);
            assertTrue(r != null && r.origin() != null, "doc row with a valid origin for " + id);
            assertEquals("book_star", r.book());
            assertEquals(pg.factory(), r.tier(), id + ": tier in the doc");
            assertEquals(pg.name(), r.name(), id + ": name in the doc");
            if (!r.origin().original()) assertTrue(r.origin().source().length() >= 8, id + ": a canon origin names its source");
        }
        assertEquals(new HashSet<>(StarRules.NEW_PAGES), rows.keySet(), "the doc rows are exactly the new pages");
    }
}
