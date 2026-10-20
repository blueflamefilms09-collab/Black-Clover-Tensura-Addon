package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the armour policy is data (data/nusmp/armour_policy.json), and a wrong line in data fails silently in play: a magic missing from the file would read as "no armour", a typo in a
 * page id would make an armour that nothing can put on, and a page that casting a bolt can reach would bring the old "every cast equips" back. This test reads the SHIPPED file with
 * the real parser (any complaint fails the build) and checks it against the REAL list of magics (MagicType.java) and the REAL page ids of the books (the sources under book/):
 * every magic has an entry; the armoured share is small; every armour or form page named exists; no page that is not an armour or form page answers anything but "no armour
 * instruction"; no page id of the 1.98.0 release was removed, renamed or reordered (the snapshot made by tools/page_id_snapshot.py); and the pure rules of ArmourRules (what an armour
 * page does with the armour that is worn, when an armour ends, the lease). Pure (java.nio, regular expressions, Gson); the working directory is the project root.
 */
class ArmourPolicyTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path MAGIC_TYPE = ROOT.resolve("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java");
    private static final Path BOOKS = ROOT.resolve("src/main/java/com/newuniverse/nusmp/book");
    private static final Path SHAPES = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java");
    private static final Path FILE = ROOT.resolve("src/main/resources/data/nusmp/armour_policy.json");
    private static final Path SNAPSHOT = ROOT.resolve("src/test/resources/armour_policy/page_ids_1_98.txt");
    private static final Pattern PAGE = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
    /** The pages every grimoire has besides its family pages (GrimoireBook.pages()). */
    private static final Set<String> UNIVERSAL = Set.of("spirit_dive", "spirit_channeling", "devil_union", "summon_grimoire", "store_weapon", "draw_weapon", "mana_strike", "mana_zone");

    private static ArmourPolicy policy() throws IOException { return ArmourPolicy.parse(Files.readString(FILE, StandardCharsets.UTF_8)); }

    private static List<String> magics() throws IOException {
        String src = Files.readString(MAGIC_TYPE, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("(?m)^\\s{4}([A-Z_]+)\\(\"[^\"]+\",\\s*\"[^\"]+\",\\s*\"[A-Z]+\"").matcher(src);
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /** file (relative to book/) -> its page ids in source order. */
    private static Map<String, List<String>> pagesNow() throws IOException {
        Map<String, List<String>> out = new LinkedHashMap<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(BOOKS)) { files = s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
        for (Path f : files) {
            Matcher m = PAGE.matcher(Files.readString(f, StandardCharsets.UTF_8));
            List<String> ids = new ArrayList<>();
            while (m.find()) ids.add(m.group(1));
            if (!ids.isEmpty()) out.put(BOOKS.relativize(f).toString().replace('\\', '/'), ids);
        }
        return out;
    }

    private static Set<String> allPageIds() throws IOException {
        Set<String> all = new LinkedHashSet<>(UNIVERSAL);
        for (List<String> l : pagesNow().values()) all.addAll(l);
        // Mercury names its four old pages by constants (MercuryRules.BLADE ...), which the page regular expression cannot see
        Matcher m = Pattern.compile("\"([a-z]+(?:_[a-z]+)+)\"").matcher(Files.readString(BOOKS.resolve("MercuryRules.java"), StandardCharsets.UTF_8));
        while (m.find()) all.add(m.group(1));
        return all;
    }

    // ------------------------------------------------------------------ the shipped table
    @Test
    void theShippedFileParsesWithoutAComplaintAndKnowsEveryMagic() throws IOException {
        ArmourPolicy p = policy();
        assertTrue(p.problems().isEmpty(), "problems: " + p.problems());
        List<String> magics = magics();
        assertEquals(105, magics.size(), "the enum read from the source: " + magics.size());
        for (String m : magics) {
            ArmourPolicy.Entry e = p.entry(m);
            assertNotNull(e, m + " has no entry in the armour policy");
            assertFalse(e.canon().isBlank(), m + " has no canon justification");
        }
        for (String k : p.entries().keySet()) assertTrue(magics.contains(k), k + " is in the policy but not in MagicType");
        assertEquals(magics.size(), p.entries().size());
    }

    @Test
    void theCountsAreNoneToggleAndFormAndTheArmouredShareIsSmall() throws IOException {
        ArmourPolicy p = policy();
        int none = p.count(ArmourPolicy.Kind.NONE), toggle = p.count(ArmourPolicy.Kind.TOGGLE), form = p.count(ArmourPolicy.Kind.FORM);
        assertEquals(p.entries().size(), none + toggle + form);
        int armoured = toggle + form;
        assertTrue(armoured * 10 <= p.entries().size() * 4, armoured + " armoured magics of " + p.entries().size() + " is more than 40 percent");
        assertTrue(armoured * 3 <= p.entries().size(), "the target is about a third at most: " + armoured);
        assertTrue(form >= 4, "Spirit Dive is the form of the four spirit magics, Devil Union and Devil Unite are forms");
        for (String m : List.of("FLAME", "WATER", "WIND", "EARTH", "ANTI_MAGIC", "SHADOW")) assertEquals(ArmourPolicy.Kind.FORM, p.kind(m), m);
        for (String m : List.of("LIGHTNING", "DARK", "MERCURY", "CORUNDUM", "BEAST")) assertEquals(ArmourPolicy.Kind.TOGGLE, p.kind(m), m);
        for (String m : List.of("AIR", "SMOKE", "WING", "TREE", "SPATIAL", "SONG", "SHAKUDO", "BRONZE", "KEY", "BODY", "BONE", "SEALING", "MIRROR", "SLASH", "GRAVITY"))
            assertEquals(ArmourPolicy.Kind.NONE, p.kind(m), m + " used to equip on every cast and has no armour in the anime");
    }

    @Test
    void everyArmourAndFormPageIsARealPageAndNoOtherPageAnswersAnInstruction() throws IOException {
        ArmourPolicy p = policy();
        Set<String> pages = allPageIds();
        for (ArmourPolicy.Entry e : p.entries().values()) {
            for (String id : e.armourPages().keySet()) assertTrue(pages.contains(id) || e.newPages().contains(id), e.magic() + ": armour page " + id + " is not a page of any book");
            for (String id : e.formPages()) assertTrue(pages.contains(id), e.magic() + ": form page " + id + " is not a page of any book");
            for (String id : e.externalPages()) assertTrue(pages.contains(id), e.magic() + ": form page " + id + " is not a page of any book");
            for (String id : pages) {
                ArmourPolicy.Instruction want = e.armourPages().containsKey(id) ? ArmourPolicy.Instruction.ARMOUR : e.formPages().contains(id) ? ArmourPolicy.Instruction.FORM : ArmourPolicy.Instruction.NONE;
                assertEquals(want, p.forCast(e.magic(), id), e.magic() + " / " + id);
            }
        }
    }

    @Test
    void aCastOfANonArmourPageReturnsNoAuraInstruction() throws IOException {
        ArmourPolicy p = policy();
        // the bolts and zones that used to equip the armour: they answer NONE for the magic they belong to
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("FLAME", "exploding_fireball"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("FLAME", "crimson_lion_king"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("WATER", "waterball"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("LIGHTNING", "chain"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("DARK", "avidya_slash"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("AIR", "attribute_guard"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("SHAKUDO", "attribute_guard"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("TREE", "attribute_domain"));
        // unknown input is NONE, never an exception
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast(null, "x"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("NOT_A_MAGIC", "flame_armour"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("FLAME", null));
        // the explicit pages are the only ones that answer
        assertEquals(ArmourPolicy.Instruction.ARMOUR, p.forCast("FLAME", "flame_armour"));
        assertEquals(ArmourPolicy.Instruction.ARMOUR, p.forCast("WATER", "valkyrie_dress"));
        assertEquals(ArmourPolicy.Instruction.ARMOUR, p.forCast("DARK", "dark_cloak"));
        assertEquals(ArmourPolicy.Instruction.FORM, p.forCast("FLAME", "spirit_dive"));
        assertEquals(ArmourPolicy.Instruction.NONE, p.forCast("AIR", "spirit_dive"), "a magic without a spirit has no dive");
        // every page of every NONE magic, whatever it is called
        Set<String> pages = allPageIds();
        for (ArmourPolicy.Entry e : p.entries().values())
            if (e.kind() == ArmourPolicy.Kind.NONE)
                for (String id : pages) assertEquals(ArmourPolicy.Instruction.NONE, p.forCast(e.magic(), id), e.magic() + " / " + id);
    }

    @Test
    void theArmourPagesOfASingleMagicNeverOverlapAndStylesFitTheAuraPayload() throws IOException {
        for (ArmourPolicy.Entry e : policy().entries().values()) {
            for (String id : e.armourPages().keySet()) assertFalse(e.formPages().contains(id), e.magic() + " " + id);
            for (int s : e.armourPages().values()) assertTrue(s >= 0 && s < 128, e.magic() + " style " + s + " does not fit the byte of PlayerAuraPayload");
            if (e.kind() != ArmourPolicy.Kind.NONE && !e.aura().isEmpty()) assertTrue(isAura(e.aura()), e.magic() + ": " + e.aura() + " is not an Aura constant");
            for (String a : e.legacyAuras()) assertTrue(isAura(a), e.magic() + ": legacy " + a + " is not an Aura constant");
        }
    }

    private static boolean isAura(String name) throws IOException {
        String src = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/aura/Aura.java"), StandardCharsets.UTF_8);
        return Pattern.compile("(?m)^\\s+" + Pattern.quote(name) + "[,\\s]").matcher(src).find();
    }

    @Test
    void everyShimmerIsARealEffectShapeOfANoneMagic() throws IOException {
        String shapes = Files.readString(SHAPES, StandardCharsets.UTF_8);
        int n = 0;
        for (ArmourPolicy.Entry e : policy().entries().values()) {
            if (e.shimmer().isEmpty()) continue;
            n++;
            assertEquals(ArmourPolicy.Kind.NONE, e.kind(), e.magic());
            assertTrue(Pattern.compile("\\b" + Pattern.quote(e.shimmer()) + "\\b").matcher(shapes).find(), e.magic() + ": no VfxShape " + e.shimmer());
            assertFalse(e.legacyAuras().isEmpty(), e.magic() + ": a shimmer carries the look of an armour the magic used to wear");
        }
        assertTrue(n >= 30, "the magics that lost their auto-armour keep a light shimmer: " + n);
    }

    @Test
    void auraNamesMapBackToTheirMagic() throws IOException {
        ArmourPolicy p = policy();
        assertEquals("FLAME", p.magicOfAura("FLAME"));
        assertEquals("SMOKE", p.magicOfAura("SMOKE"));
        assertEquals("EARTH", p.magicOfAura("WIKI"), "the first magic that names the wiki layer is the armoured one");
        assertNull(p.magicOfAura("ERZA"), "Erza Requip is its own skill, not a magic of the policy");
        assertNull(p.magicOfAura(null));
    }

    // ------------------------------------------------------------------ no page id changed
    @Test
    void noPageIdOfTheLastReleaseWasRemovedRenamedOrReordered() throws IOException {
        Map<String, List<String>> now = pagesNow();
        ArmourPolicy p = policy();
        Set<String> appended = new LinkedHashSet<>();
        for (ArmourPolicy.Entry e : p.entries().values()) appended.addAll(e.newPages());
        int files = 0, ids = 0;
        for (String line : Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            int c = line.indexOf(": ");
            String file = line.substring(0, c);
            List<String> old = List.of(line.substring(c + 2).split(" "));
            List<String> cur = now.getOrDefault(file, List.of());
            files++;
            ids += old.size();
            // the old ids are an in-order subsequence of the current ones, with nothing missing
            int at = 0;
            for (String id : old) {
                int k = cur.subList(at, cur.size()).indexOf(id);
                assertTrue(k >= 0, file + ": page " + id + " was removed, renamed or moved before an earlier page");
                at += k + 1;
            }
            // everything else is a new page and has to be a page this release says it appended
            List<String> extra = new ArrayList<>(cur);
            for (String id : old) extra.remove(id);
            for (String id : extra) assertTrue(appended.contains(id), file + ": new page " + id + " is not listed as appended in the armour policy");
        }
        assertTrue(files > 60 && ids > 800, "the snapshot is read: " + files + " files, " + ids + " ids");
        for (String file : now.keySet()) if (Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8).stream().noneMatch(l -> l.startsWith(file + ": ")))
            assertTrue(appended.containsAll(now.get(file)), file + " is new and defines pages the policy does not list");
    }

    // ------------------------------------------------------------------ the pure rules
    private static ArmourRules.Held held(String magic, int style, boolean form) { return new ArmourRules.Held(magic, style, form, "X"); }

    @Test
    void anArmourPagePutsOnTakesOffRestylesSwapsOrKeeps() {
        assertEquals(ArmourRules.Outcome.PUT_ON, ArmourRules.armourPage(null, "FLAME", 0));
        assertEquals(ArmourRules.Outcome.TAKE_OFF, ArmourRules.armourPage(held("FLAME", 0, false), "FLAME", 0));
        assertEquals(ArmourRules.Outcome.RESTYLE, ArmourRules.armourPage(held("WATER", 0, false), "WATER", 1));
        assertEquals(ArmourRules.Outcome.SWAP, ArmourRules.armourPage(held("FLAME", 0, false), "WATER", 1));
        assertEquals(ArmourRules.Outcome.KEEP, ArmourRules.armourPage(held("FLAME", 0, true), "FLAME", 0), "a worn dive keeps its look");
        assertTrue(ArmourRules.onlyTakesOff(held("FLAME", 0, false), "FLAME", 0));
        assertFalse(ArmourRules.onlyTakesOff(null, "FLAME", 0));
        assertFalse(ArmourRules.onlyTakesOff(held("FLAME", 0, false), "WATER", 0));
        assertEquals(ArmourRules.Outcome.PUT_ON, ArmourRules.formStart(null, "FLAME"));
        assertEquals(ArmourRules.Outcome.RESTYLE, ArmourRules.formStart(held("FLAME", 0, false), "FLAME"));
        assertEquals(ArmourRules.Outcome.SWAP, ArmourRules.formStart(held("WATER", 1, false), "FLAME"));
    }

    @Test
    void theStyleRulesRaiseTheStyleOnlyWhenTheirConditionHolds() {
        assertEquals(0, ArmourRules.style("none", 0, true, true));
        assertEquals(1, ArmourRules.style("sneak", 0, true, false));
        assertEquals(0, ArmourRules.style("sneak", 0, false, true));
        assertEquals(1, ArmourRules.style("mastery", 0, false, true));
        assertEquals(0, ArmourRules.style("mastery", 0, true, false));
        assertEquals(100, ArmourRules.style("none", 100, false, false));
        assertEquals(100, ArmourRules.style("sneak", 100, true, false), "a table style above one is never lowered");
    }

    @Test
    void anArmourEndsWhenTheWearerDiesIsKnockedOutOrCannotPayTheUpkeep() {
        assertEquals(ArmourRules.Drop.NONE, ArmourRules.drop(true, false, true, false, true, 100.0, 10.0));
        assertEquals(ArmourRules.Drop.DEATH, ArmourRules.drop(false, false, true, false, true, 100.0, 10.0));
        assertEquals(ArmourRules.Drop.KNOCKED_OUT, ArmourRules.drop(true, true, true, false, false, 100.0, 10.0));
        assertEquals(ArmourRules.Drop.NO_MANA, ArmourRules.drop(true, false, true, false, true, 5.0, 10.0));
        assertEquals(ArmourRules.Drop.NONE, ArmourRules.drop(true, false, true, false, false, 5.0, 10.0), "the upkeep is only paid once a second");
        assertEquals(ArmourRules.Drop.NONE, ArmourRules.drop(true, false, true, true, true, 0.0, 10.0), "creative pays nothing");
        assertEquals(ArmourRules.Drop.NONE, ArmourRules.drop(true, false, true, false, true, 0.0, 0.0), "a free armour never runs dry");
        assertEquals(ArmourRules.Drop.UNKNOWN_MAGIC, ArmourRules.drop(true, false, false, false, false, 100.0, 10.0));
        assertEquals(ArmourRules.Drop.DEATH, ArmourRules.drop(false, true, false, false, true, 0.0, 10.0), "death wins over every other reason");
    }

    @Test
    void theManaOfASecondIsUpkeepOutAmbientInClampedToThePool() {
        assertEquals(90.0, ArmourRules.manaAfterSecond(100.0, 1000.0, 10.0, 0.0), 1e-9);
        assertEquals(98.0, ArmourRules.manaAfterSecond(100.0, 1000.0, 20.0, 18.0), 1e-9);
        assertEquals(1000.0, ArmourRules.manaAfterSecond(995.0, 1000.0, 0.0, 12.0), 1e-9);
        assertEquals(0.0, ArmourRules.manaAfterSecond(5.0, 1000.0, 10.0, 0.0), 1e-9);
    }

    @Test
    void theLeaseIsRenewedLongBeforeItEndsAndTheShatterIsInsideTheShatterWindow() {
        assertTrue(ArmourRules.REFRESH_EVERY + 18 < ArmourRules.LEASE_TICKS, "the lease never reaches the 18 tick shatter window between two refreshes");
        assertTrue(ArmourRules.SHATTER_TICKS >= 18 && ArmourRules.SHATTER_TICKS <= 40, "taking it off tells the clients a time inside the shatter window of EquipAnimation");
        assertTrue(ArmourRules.refreshDue(200) && !ArmourRules.refreshDue(201));
        assertTrue(ArmourRules.secondDue(40) && !ArmourRules.secondDue(41));
        assertTrue(ArmourRules.hpUpkeepAllowed(10f, false, 0.5));
        assertFalse(ArmourRules.hpUpkeepAllowed(6.2f, false, 0.5), "the poison never takes the wearer below six");
        assertFalse(ArmourRules.hpUpkeepAllowed(20f, true, 0.5), "never in creative");
        assertFalse(ArmourRules.hpUpkeepAllowed(20f, false, 0.0));
    }

    @Test
    void theFirstDivesAreHalfDivesUntilMasteryOrThreeDives() {
        assertTrue(ArmourRules.halfDive(0, false));
        assertTrue(ArmourRules.halfDive(ArmourRules.HALF_DIVES - 1, false));
        assertFalse(ArmourRules.halfDive(ArmourRules.HALF_DIVES, false));
        assertFalse(ArmourRules.halfDive(0, true), "a mastered book dives full at once");
        assertEquals(ArmourRules.HALF_SCALE, ArmourRules.diveScale(0, false), 1e-6);
        assertEquals(1f, ArmourRules.diveScale(5, false), 1e-6);
        assertEquals(0.12f, ArmourRules.scaled(0.2f, 0.6f, 1.0), 1e-6);
        assertEquals(0f, ArmourRules.scaled(0.2f, 1f, 0.0), 1e-6);
        assertEquals(0f, ArmourRules.scaled(-0.2f, 1f, 1.0), 1e-6, "a bonus is never negative");
    }

    @Test
    void theBonusNumbersAreSmallEnoughToBalance() throws IOException {
        for (ArmourPolicy.Entry e : policy().entries().values()) {
            if (e.kind() == ArmourPolicy.Kind.NONE) continue;
            for (ArmourPolicy.Bonus b : List.of(e.bonus(), e.formBonus())) {
                assertTrue(b.cut() >= 0f && b.cut() <= 0.3f, e.magic() + " cut " + b.cut());
                assertTrue(b.attack() >= 0f && b.attack() <= 0.3f, e.magic() + " attack " + b.attack());
                assertTrue(b.speed() >= 0f && b.speed() <= 0.4f, e.magic() + " speed " + b.speed());
                assertTrue(b.knockback() >= 0f && b.knockback() <= 0.7f, e.magic() + " knockback " + b.knockback());
                assertTrue(b.upkeep() >= 0.0 && b.upkeep() <= 0.03, e.magic() + " upkeep " + b.upkeep());
                assertTrue(b.absorb() >= 0.0 && b.absorb() < 0.02, e.magic() + " ambient intake " + b.absorb() + " must stay below the dive upkeep of 2 percent");
            }
        }
    }
}
