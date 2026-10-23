package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.pages15.Pages15Rules.DocRow;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Origin;
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
 * Why: the rules every "15 spells for every grimoire" page obeys (readable ids, tiers, cooldown bounds, one signature per book, the page limits, the canon / original mark) are pure code, so they are pinned here
 * without a game; and the plan table (which batch owns which book) is checked against the plan in docs/magic/page_counts.md so the two can never drift apart. Pure; the working directory is the project root.
 */
class Pages15RulesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static Spec spec(String book, String id, Tier t, int cooldown) { return new Spec(book, id, "Some Spell", t.costPercent, cooldown); }

    // ------------------------------------------------------------------ ids and names
    @Test
    void readableSnakeCaseIdsPassAndNumberedOrMalformedOnesFail() {
        for (String ok : List.of("ember_lance", "shining_flame_cross", "d6_roll", "crescent_moon", "slicing_wind_emperor_gale")) assertNull(Pages15Rules.idProblem(ok), ok);
        for (String bad : List.of("", "ab", "Ember_Lance", "ember lance", "ember__lance", "_ember", "ember_", "fire_x01", "book_fire_x12", "page_3", "spell12", "ember_2", "mana_zone", "summon_grimoire",
                "this_id_is_far_too_long_to_be_a_readable_spell_name_at_all"))
            assertNotNull(Pages15Rules.idProblem(bad), "'" + bad + "' must be refused");
        assertNotNull(Pages15Rules.idProblem(null));
    }

    @Test
    void namesAreTrimmedPrintableAndNotTiny() {
        assertNull(Pages15Rules.nameProblem("Ember Lance"));
        assertNotNull(Pages15Rules.nameProblem(" Ember Lance"));
        assertNotNull(Pages15Rules.nameProblem("Ember Lance "));
        assertNotNull(Pages15Rules.nameProblem("  "));
        assertNotNull(Pages15Rules.nameProblem("Em"));
        assertNotNull(Pages15Rules.nameProblem("Bad\nName"));
        assertNotNull(Pages15Rules.nameProblem(null));
        assertEquals("nusmp.page.book_fire.ember_lance", Pages15Rules.langKey("book_fire", "ember_lance"));
    }

    // ------------------------------------------------------------------ tiers
    @Test
    void theFourTiersAreTheBookPageTiersWithGrowingDamageAndBoundedCooldowns() {
        assertEquals(Tier.STARTER, Tier.ofCost(8));
        assertEquals(Tier.MID, Tier.ofCost(12));
        assertEquals(Tier.ZONE, Tier.ofCost(20));
        assertEquals(Tier.SIGNATURE, Tier.ofCost(35));
        assertNull(Tier.ofCost(10));
        double lastHit = 0;
        int lastCooldown = 0;
        for (Tier t : Tier.values()) {
            assertTrue(t.hit > lastHit && t.cooldown > lastCooldown, t + " is stronger and slower than the tier below");
            assertTrue(t.minCooldown <= t.cooldown && t.cooldown <= t.maxCooldown, t + " default cooldown is inside its bounds");
            assertTrue(t.pulse < t.hit, t + ": a zone pulse is smaller than a single hit");
            assertTrue(t.maxHit() <= 40f && t.maxTargets <= 10, t + " damage and targets are bounded");
            assertTrue(t.masteryByCost() > 0 && t.masteryByCost() <= 1.0);
            lastHit = t.hit;
            lastCooldown = t.cooldown;
        }
        assertEquals(1.0, Tier.SIGNATURE.masteryByCost(), 1e-9, "a signature page needs the full bar (docs/magic/page_counts.md 4.3)");
        assertEquals(8 / 35.0, Tier.STARTER.masteryByCost(), 1e-9);
    }

    @Test
    void theTiersMatchBookPageSourceNumbers() throws IOException {
        // BookPage.starter / mid / zone / signature: (cost %, floor, cooldown). The kit takes its numbers from the Tier enum, so the two must be the same.
        String src = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/book/BookPage.java"), StandardCharsets.UTF_8);
        Map<String, Tier> by = Map.of("starter", Tier.STARTER, "mid", Tier.MID, "zone", Tier.ZONE, "signature", Tier.SIGNATURE);
        for (var e : by.entrySet()) {
            Matcher m = Pattern.compile("static BookPage " + e.getKey() + "\\(String id, String name, Cast c\\)\\s*\\{ return new BookPage\\(id, name, name, (\\d+), (\\d+), (\\d+), c\\); \\}").matcher(src);
            assertTrue(m.find(), "BookPage." + e.getKey());
            assertEquals((int) e.getValue().costPercent, Integer.parseInt(m.group(1)), e.getKey() + " cost %");
            assertEquals((int) e.getValue().costFloor, Integer.parseInt(m.group(2)), e.getKey() + " floor");
            assertEquals(e.getValue().cooldown, Integer.parseInt(m.group(3)), e.getKey() + " cooldown");
        }
    }

    // ------------------------------------------------------------------ the checks over a set of pages
    @Test
    void aCleanSetOfPagesHasNoProblem() {
        List<Spec> specs = List.of(spec("book_fire", "ember_lance", Tier.STARTER, 100), spec("book_fire", "cinder_storm", Tier.MID, 200), spec("book_fire", "phoenix_rise", Tier.SIGNATURE, 900),
                spec("book_water", "tide_lash", Tier.ZONE, 400));
        assertEquals(List.of(), Pages15Rules.problems(specs, Map.of("book_fire", 24, "book_water", 17), Set.of("old_page")));
    }

    @Test
    void theProblemsAreFoundIdsTwiceTiersCooldownsSignaturesAndLimits() {
        Set<String> older = Set.of("fireball");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "ember_lance", Tier.MID, 200), spec("book_water", "ember_lance", Tier.MID, 200)), Map.of(), older).isEmpty(), "an id twice across books");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "fireball", Tier.MID, 200)), Map.of(), older).isEmpty(), "an id of an older page");
        assertFalse(Pages15Rules.problems(List.of(new Spec("book_fire", "ember_lance", "Ember Lance", 10, 200)), Map.of(), older).isEmpty(), "a cost that is no tier");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "ember_lance", Tier.MID, 20)), Map.of(), older).isEmpty(), "a cooldown under the tier");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "ember_lance", Tier.STARTER, 5000)), Map.of(), older).isEmpty(), "a cooldown over the tier");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "ember_lance", Tier.SIGNATURE, 900), spec("book_fire", "sun_crown", Tier.SIGNATURE, 900)), Map.of(), older).isEmpty(), "two signatures");
        assertFalse(Pages15Rules.problems(List.of(spec("book_nowhere", "ember_lance", Tier.MID, 200)), Map.of(), older).isEmpty(), "a book that is not in the plan");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "fire_x01", Tier.MID, 200)), Map.of(), older).isEmpty(), "a numbered id");
        List<Spec> many = new ArrayList<>();
        for (int i = 0; i < Pages15Rules.MAX_NEW_PER_BOOK + 1; i++) many.add(spec("book_fire", "spell_name_" + (char) ('a' + i), Tier.STARTER, 100));
        assertFalse(Pages15Rules.problems(many, Map.of(), older).isEmpty(), "too many new pages in one book");
        assertFalse(Pages15Rules.problems(List.of(spec("book_fire", "ember_lance", Tier.MID, 200)), Map.of("book_fire", Pages15Rules.MAX_FAMILY), older).isEmpty(), "over the 64 modes of the mask");
        assertTrue(Pages15Rules.problems(List.of(spec("book_fire", "ember_lance", Tier.MID, 200)), Map.of("book_fire", Pages15Rules.MAX_FAMILY - 1), older).isEmpty(), "the 64th page is fine");
    }

    @Test
    void theFireBookCanReach33FamilyPagesAndStillShowFifteen() {
        // Fire: 24 family pages now (18 hidden in the basic grimoire) + 9 new = 33, which only the 64-bit mask can hold; the basic grimoire shows 6 + 9 = 15.
        List<Spec> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) nine.add(spec("book_fire", "fire_spell_" + (char) ('a' + i), i == 8 ? Tier.SIGNATURE : Tier.MID, i == 8 ? 900 : 200));
        assertEquals(List.of(), Pages15Rules.problems(nine, Map.of("book_fire", 24), Set.of()));
        assertNull(Pages15Rules.visibleProblem("book_fire", 15, true));
        assertNotNull(Pages15Rules.visibleProblem("book_fire", 14, true));
        assertNull(Pages15Rules.visibleProblem("book_fire", 14, false), "an unclaimed book is not held to the goal");
        assertNull(Pages15Rules.visibleProblem("book_fire", Pages15Rules.MAX_VISIBLE, true));
        assertNotNull(Pages15Rules.visibleProblem("book_fire", Pages15Rules.MAX_VISIBLE + 1, false), "no grimoire shows more than 32");
        assertEquals(32, Pages15Rules.MAX_VISIBLE);
        assertEquals(64, Pages15Rules.MAX_FAMILY);
        assertEquals(15, Pages15Rules.GOAL);
    }

    // ------------------------------------------------------------------ canon or original
    @Test
    void everyPageIsMarkedCanonWithASourceOrOriginal() {
        assertEquals(new Origin(true, ""), Pages15Rules.parseOrigin("original"));
        assertEquals(new Origin(true, ""), Pages15Rules.parseOrigin("  Original "));
        assertEquals(new Origin(false, "en-wiki: Fire Magic"), Pages15Rules.parseOrigin("canon: en-wiki: Fire Magic"));
        assertEquals(new Origin(false, "ja-wiki: 炎魔法"), Pages15Rules.parseOrigin("Canon:ja-wiki: 炎魔法"));
        assertNull(Pages15Rules.parseOrigin("canon"));
        assertNull(Pages15Rules.parseOrigin("canon:"));
        assertNull(Pages15Rules.parseOrigin("canon: x"));
        assertNull(Pages15Rules.parseOrigin("made up"));
        assertNull(Pages15Rules.parseOrigin(null));
    }

    @Test
    void aBatchDocRowIsParsed() {
        DocRow r = Pages15Rules.parseDocRow("| `book_fire` | `ember_lance` | Ember Lance | mid | canon: en-wiki: Fire Magic | a lance of embers |");
        assertNotNull(r);
        assertEquals("book_fire", r.book());
        assertEquals("ember_lance", r.id());
        assertEquals("Ember Lance", r.name());
        assertEquals("mid", r.tier());
        assertEquals(new Origin(false, "en-wiki: Fire Magic"), r.origin());
        DocRow o = Pages15Rules.parseDocRow("| `forbidden_magic` | `devil_pact_mark` | Devil Pact Mark | zone | original |");
        assertNotNull(o);
        assertTrue(o.origin().original());
        DocRow bad = Pages15Rules.parseDocRow("| `book_fire` | `ember_lance` | Ember Lance | mid | maybe |");
        assertNotNull(bad);
        assertNull(bad.origin(), "an origin that is neither canon nor original is flagged by a null origin");
        assertNull(Pages15Rules.parseDocRow("| Book | Id | Name | Tier | Origin |"));
        assertNull(Pages15Rules.parseDocRow("| --- | --- | --- | --- | --- |"));
        assertNull(Pages15Rules.parseDocRow("plain text"));
        assertNull(Pages15Rules.parseDocRow(null));
    }

    // ------------------------------------------------------------------ the plan
    @Test
    void thePlanCoversEveryBookOnceInEightBatchesAndMatchesTheDoc() throws IOException {
        List<String> all = Pages15Plan.allBooks();
        assertEquals(106, all.size());
        assertEquals(new HashSet<>(all).size(), all.size());
        int sum = 0;
        for (int b = 1; b <= Pages15Plan.BATCHES; b++) {
            assertFalse(Pages15Plan.booksOf(b).isEmpty(), "batch " + b);
            assertFalse(Pages15Plan.nameOf(b).isEmpty());
            sum += Pages15Plan.booksOf(b).size();
        }
        assertEquals(106, sum);
        assertEquals(0, Pages15Plan.batchOf("book_nowhere"));
        assertEquals(0, Pages15Plan.batchOf(null));
        assertEquals("", Pages15Plan.nameOf(0));
        assertEquals("", Pages15Plan.nameOf(9));
        assertEquals(1, Pages15Plan.batchOf("book_fire"));
        assertEquals(5, Pages15Plan.batchOf("forbidden_magic"));
        // the same table as docs/magic/page_counts.md section 8 (the generated plan block)
        String doc = Files.readString(ROOT.resolve("docs/magic/page_counts.md"), StandardCharsets.UTF_8);
        doc = doc.substring(doc.indexOf("<!-- GENERATED:plan"));
        Map<Integer, List<String>> fromDoc = new LinkedHashMap<>();
        int cur = 0;
        for (String line : doc.split("\n")) {
            Matcher h = Pattern.compile("^#### Batch (\\d): (.*)$").matcher(line);
            if (h.matches()) {
                cur = Integer.parseInt(h.group(1));
                assertEquals(Pages15Plan.nameOf(cur), h.group(2).strip(), "name of batch " + cur);
                fromDoc.put(cur, new ArrayList<>());
                continue;
            }
            Matcher r = Pattern.compile("^\\| `([a-z_]+)` \\|").matcher(line);
            if (cur != 0 && r.find()) fromDoc.get(cur).add(r.group(1));
        }
        assertEquals(8, fromDoc.size());
        for (int b = 1; b <= 8; b++) assertEquals(fromDoc.get(b), Pages15Plan.booksOf(b), "batch " + b + " against the plan in docs/magic/page_counts.md");
    }
}
