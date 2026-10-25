package com.newuniverse.nusmp.star;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.book.pages15.Pages15Rules;
import com.newuniverse.nusmp.charset.CharacterSet;
import com.newuniverse.nusmp.charset.CharacterSetRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "the new Star effects are for every Star user, not just Spade" (docs/star_vfx_for_all.md). The access rule is pure ({@link StarAccess}), so it is checked as a TABLE over every kingdom, every shipped
 * character, "nobody" (the basic grimoire) and both values of {@code star.vfxForAll}: the shared pages are open to everybody, Black Neverland still needs the heir (Spade cover + cleared Raid + signature mastery),
 * and with the setting off the table is exactly the 3.7.0 one ({@link StarRules#needsHeir}). Also pinned: no shipped character set can hide a Star page, the replacement lang texts no longer say the heir mark is needed.
 */
class StarAccessTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static String read(String rel) {
        try { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static List<String> allPages() {
        List<String> pages = new ArrayList<>(StarRules.NEW_PAGES);
        pages.add(StarRules.FLAGELLUM);
        return pages;
    }

    private static List<String> kingdoms() {
        List<String> k = new ArrayList<>();
        for (Kingdom x : Kingdom.values()) k.add(x.name());
        k.add(null);                                                       // no cover at all
        return k;
    }

    @Test
    void theSharedPagesAreOpenToEveryStarGrimoireWhateverItsKingdomWithTheSettingOn() {
        assertTrue(kingdoms().size() >= 5, "all four kingdoms and no cover");
        for (String kingdom : kingdoms()) {
            for (double mastery : new double[]{0.0, 0.5, 0.99, 1.0}) {
                for (boolean raid : new boolean[]{false, true}) {
                    for (String page : StarAccess.SHARED) assertTrue(StarAccess.mayCast(page, true, kingdom, mastery, 1.0, raid, true), page + " for " + kingdom + " mastery " + mastery + " raid " + raid);
                    assertTrue(StarAccess.mayCast(StarRules.SEED, true, kingdom, mastery, 1.0, raid, true));
                    assertTrue(StarAccess.mayCast(StarRules.CONJUNCTION, true, kingdom, mastery, 1.0, raid, true));
                }
            }
        }
    }

    @Test
    void blackNeverlandIsTheHeirsWhateverTheSetting() {
        for (boolean all : new boolean[]{true, false}) {
            for (String kingdom : kingdoms()) {
                boolean spade = "SPADE".equals(kingdom);
                assertEquals(spade, StarAccess.mayCast(StarRules.BLACK_NEVERLAND, true, kingdom, 1.0, 1.0, true, all), "full heir facts, " + kingdom + " all=" + all);
                assertFalse(StarAccess.mayCast(StarRules.BLACK_NEVERLAND, true, kingdom, 1.0, 1.0, false, all), "Raid not cleared, " + kingdom);
                assertFalse(StarAccess.mayCast(StarRules.BLACK_NEVERLAND, true, kingdom, 0.9, 1.0, true, all), "mastery short, " + kingdom);
                assertFalse(StarAccess.mayCast(StarRules.BLACK_NEVERLAND, false, kingdom, 1.0, 1.0, true, all), "no Star grimoire, " + kingdom);
            }
        }
        assertEquals(Set.of(StarRules.BLACK_NEVERLAND), StarAccess.ALWAYS_HEIR);
    }

    @Test
    void withTheSettingOffTheTableIsExactlyTheOneOf370() {
        for (String page : allPages()) {
            assertEquals(StarRules.needsHeir(page), StarAccess.needsHeir(page, false), page + " needs the heir exactly as before");
            for (String kingdom : kingdoms()) {
                boolean heir = StarRules.isHeir("SPADE".equals(kingdom), 1.0, 1.0, true);
                assertEquals(heir || !StarRules.needsHeir(page), StarAccess.mayCast(page, true, kingdom, 1.0, 1.0, true, false), page + " " + kingdom);
                assertEquals(!StarRules.needsHeir(page), StarAccess.mayCast(page, true, kingdom, 0.3, 1.0, false, false), page + " " + kingdom + " not an heir");
            }
        }
        assertTrue(StarAccess.whip(true, false) && !StarAccess.whip(false, false) && StarAccess.whip(false, true) && StarAccess.whip(true, true));
    }

    @Test
    void theSettingIsOnByDefaultAndTheSharedSetIsTheOldHeirSetMinusNeverland() {
        assertTrue(StarAccess.DEFAULT_VFX_FOR_ALL);
        assertEquals("vfxForAll", StarAccess.CONFIG_KEY);
        java.util.Set<String> expected = new java.util.HashSet<>(StarRules.HEIR_PAGES);
        expected.add(StarRules.FLAGELLUM);
        expected.remove(StarRules.BLACK_NEVERLAND);
        assertEquals(expected, StarAccess.SHARED, "every page that was heir-only in 3.7.0, but Black Neverland");
        for (String page : StarAccess.SHARED) assertFalse(StarAccess.needsHeir(page, true), page);
        for (String page : StarRules.NEW_PAGES) if (!StarAccess.SHARED.contains(page) && !StarAccess.ALWAYS_HEIR.contains(page)) assertFalse(StarRules.needsHeir(page), page + " was never gated");
        String cfg = read("src/main/java/com/newuniverse/nusmp/star/StarConfig.java");
        assertTrue(cfg.contains("VFX_FOR_ALL = b.comment(") && cfg.contains(".define(StarAccess.CONFIG_KEY, StarAccess.DEFAULT_VFX_FOR_ALL)") && cfg.contains("public static boolean vfxForAll()"), "a SERVER setting read through StarConfig");
    }

    @Test
    void noShippedCharacterSetCanHideAStarPageAndTheBasicGrimoireListsThemAll() {
        CharacterSetRegistry reg = CharacterSetRegistry.loadShipped();
        assertTrue(reg.problems().isEmpty(), "the shipped sets load: " + reg.problems());
        for (CharacterSet set : reg.all())
            assertFalse("book_star".equals(set.book()), set.canon() + " has a Star set: a set lists SOME pages and hides the rest, so the new Star pages would have to be listed in it (add them, then adjust this test)");
        List<String> canons = new ArrayList<>(CharacterSetRegistry.SHIPPED);
        canons.add("");                                                    // the basic grimoire
        canons.add(null);
        assertTrue(canons.size() > 40);
        for (String canon : canons) {
            for (String page : allPages()) {
                assertFalse(reg.hidden(canon, "book_star", page, false, true), "hidden in the Star grimoire of '" + canon + "': " + page);
                assertFalse(reg.hidden(canon, "book_star", page, false, false), page);
                Boolean opens = reg.opens(canon, "book_star", page, null);
                assertTrue(opens == null, "a set must not decide when " + page + " opens for '" + canon + "'");
            }
        }
    }

    @Test
    void theReplacementTextsNoLongerAskForTheHeirMarkAndBlackNeverlandStillDoes() {
        JsonObject all = JsonParser.parseString(read("tools/lang/star_all.json")).getAsJsonObject();
        Set<String> shared = new java.util.HashSet<>(StarAccess.SHARED);
        shared.remove(StarRules.FLAGELLUM);                                // the Flagellum's text is the old cone's and has no heir line
        assertEquals(shared.size(), all.size(), "one replacement per shared page");
        for (String id : shared) {
            String key = Pages15Rules.langKey("book_star", id);
            assertTrue(all.has(key), key);
            String text = all.get(key).getAsString();
            assertFalse(text.contains("Needs the Grinberryall heir mark"), key + " must not ask for the heir mark any more");
            assertTrue(text.contains("Open to every Star mage") && text.length() >= 60, key);
            if (StarRules.unverifiedCanon(id)) assertTrue(text.contains("unverified canon"), key + " keeps its canon mark");
        }
        JsonObject old = JsonParser.parseString(read("tools/lang/star_magic.json")).getAsJsonObject();
        assertTrue(old.get(Pages15Rules.langKey("book_star", StarRules.BLACK_NEVERLAND)).getAsString().contains("Needs the Grinberryall heir mark"), "Black Neverland keeps its gate text");
        assertTrue(read("tools/lang/merge_pages15.py").contains("star_all.json"), "the lang merge knows the replace fragment");
        assertEquals(0, new com.google.gson.Gson().toJsonTree(all).getAsJsonObject().keySet().stream().filter(k -> !k.startsWith("nusmp.page.book_star.")).count());
    }
}
