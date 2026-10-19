package com.newuniverse.nusmp.labyrinth.mega.flow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the classification table is data, and a wrong letter in data fails silently in play: a family misspelled in {@code exact} is a Dark magic that is
 * never rejected, a new Dark family that nobody put in the file is a mage who walks through the holy air untouched. This test reads the SHIPPED
 * {@code alignments.json} the way the game would, parses it with the real parser (any complaint fails the build), and checks it against the REAL list
 * of magic families read from {@code MagicType.java}: every family has an alignment that this test names on purpose (3 Light, 16 Dark, and the neutral
 * rest listed one by one), so adding a family to the enum fails here until somebody decides what it is; and a line was removed from the enum or
 * the file, it fails too. It also pins the cross-family pages and the Tensura skill ids that the keywords are meant to catch. Pure (java.nio and
 * regular expressions); the working directory is the project root, as for the other data tests.
 */
class AlignmentDataTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path FILE = ROOT.resolve("src/main/resources/data/nusmp/mega/flow/alignments.json");
    private static final Path MAGIC_TYPE = ROOT.resolve("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java");
    private static final Path BOOKS = ROOT.resolve("src/main/java/com/newuniverse/nusmp/book");
    private static final Path SOURCES = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    /** The Light soul of the mod's own grouping: Light, Lightning, Sword. */
    private static final Set<String> LIGHT = Set.of("LIGHT", "LIGHTNING", "SWORD");
    /** The Darkness soul (Dark, Shadow, Poison, Eyeball, Curse, the four Demon magics, Demon Beast, Black Oil, Smoke, Soul, Soul Corpse) plus Blood and Anti-Magic. */
    private static final Set<String> DARK = Set.of("DARK", "SHADOW", "POISON", "DEMON_BEAST", "EYEBALL", "CURSE", "DEMON_FIRE", "DEMON_ICE", "DEMON_LIGHT",
            "DEMON_WATER", "BLACK_OIL", "SMOKE", "SOUL", "SOUL_CORPSE", "BLOOD", "ANTI_MAGIC");
    /**
     * Every other family, on purpose. Several of them draw Dark Rukh particles (Mercury, Sand, Bone, Illusion, Ash, Cotton, Spatial, Gravity, Barrier, Mud,
     * Permeation, Sandstone, Switching, Kotodama): that is how they LOOK, not what they ARE, so they are not rejected. Curse-Warding is a ward against curses.
     */
    private static final Set<String> NEUTRAL = Set.of(
            "FLAME", "EXPLOSION", "MAGMA", "WATER", "ICE", "MERCURY", "MIST", "WIND", "STAR", "STORM", "EARTH", "PLANT", "SAND", "SPATIAL", "MIRROR",
            "GRAVITY", "TIME", "SEALING", "REINFORCEMENT", "BEAST", "BONE", "CREATION", "COPY", "ILLUSION", "DREAM", "STEEL", "THREAD",
            "TRANSMUTATION", "ASH", "COTTON", "RECOMBINATION", "PAINTING", "WORLD_TREE", "DICE", "SLASH", "COMPASS", "KOTODAMA", "GAME", "BODY",
            "EYE", "CURSE_WARDING", "BARRIER", "KEY", "CHAIN", "BUTOH", "BRIAR", "CHERRY_BLOSSOM", "FUNGUS", "FOOD", "CRYSTAL", "CORUNDUM", "BRONZE",
            "COPPER", "IRON", "GEL", "GLASS", "BUBBLE", "ICE_WEDGE", "LEGION", "AIR", "HAIR", "MEMORY", "MINERAL", "MODIFICATION", "MUCUS", "MUD",
            "NAIL", "PERMEATION", "POISON_PLANT", "RED_OCHRE", "ROCK", "SANDSTONE", "SCALE", "SHAKUDO", "SKIN", "SNOW", "SONG", "SOUND", "SPIKE",
            "SWITCHING", "TONGUE", "TREE", "STONE", "VINE", "VORTEX", "WING");

    /**
     * The entries of the shipped file that name a Tensura skill by its namespaced id. The mod's sources cannot prove them (Tensura is a dependency, not source here),
     * so they are listed by hand from the owner's Tensura 2.0.1.3 id reference; a typo here would be a rule that never matches, which is why the test names them.
     */
    private static final Set<String> TENSURA_ENTRIES = Set.of("tensura:poison", "tensura:create_greater_undead", "tensura:create_lesser_undead");

    /**
     * Every page id whose own name carries a light or dark word, or has a page entry (79 of the 632 distinct page ids of the mod), asked ALONE. Pinned so that a new page named
     * after light or darkness cannot start being boosted or rejected unnoticed: the test fails with the new name, and the fix is either to accept it here (it
     * lives in a family that decides anyway, or it really is light or dark) or to give it an exact NEUTRAL entry in the file (the Raging Black Bull is the example).
     */
    private static final Set<String> TOUCHED_BY_NAME = new LinkedHashSet<>(List.of((
            "abyssal_ward abyssal_whirlpool arresting_dark_weave black_blade black_divider black_flame_bullets black_flame_pillar black_flame_twister black_flame_veil "
            + "black_form black_hole black_hurricane black_lightning_battle_fiend black_meteorite black_moon black_oil_eruption black_oil_sea black_oil_strike blood_bullet "
            + "blood_pact blood_whip catoblepas_s_evil_eye curse_candle_ritual_disk curse_cleanse curse_epidemic curse_filled_nails curse_misfortune curse_strike "
            + "curse_transfer curse_warding_strike curse_workers_neighbor cursed_step dark_tide_wave death_scythe death_thrust demon_beast_mantle demon_beast_strike "
            + "demon_dawn demon_fire_apocalypse demon_fire_strike demon_ice_blizzard demon_ice_body demon_ice_lance demon_ice_shards demon_ice_shatter demon_ice_slide "
            + "demon_ice_soul demon_ice_strike demon_ice_wall demon_ice_wave demon_ice_winter demon_ice_zero demon_king demon_light_strike demon_porcupine_stingers "
            + "demon_slayer_slash demon_water_strike devil_dive devil_union evil_eye fallen_angel_gate heavens_shadow_second_sight hellfire_wave myriad_black shadow_bind "
            + "shadow_realm shadow_step shadow_travel summon_demon_destroyer summon_demon_dweller undying_curse veil_of_black_light divine_punishment light_prison "
            + "light_shaft_of_divine_punishment light_speed light_step light_sword_of_judgment purging_light"
            + " black_hole_collapse cursed_ash dark_cloak evil_gods_pressure_craft shadow_embrace").split(" ")));

    /** A page declaration in the sources: {@code starter("id", ...)}, {@code BookPage.mid("id", ...)}, {@code new BookPage("id", ...)} and the other tiers. */
    private static final Pattern PAGE = Pattern.compile("(?<![A-Za-z0-9_])(?:BookPage\\.)?(?:starter|mid|zone|signature|daily)\\(\\s*\"([a-z0-9_]+)\"|new BookPage\\(\\s*\"([a-z0-9_]+)\"");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static MagicAlignments shipped() throws IOException {
        MagicAlignments.Parsed p = MagicAlignments.parse(FlowJson.object(read(FILE)));
        assertTrue(p.problems().isEmpty(), "alignments.json has complaints: " + p.problems());
        return p.table();
    }

    /** The constant names of the MagicType enum, in file order. */
    private static List<String> families() throws IOException {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("^\\s+([A-Z][A-Z0-9_]*)\\(\"", Pattern.MULTILINE).matcher(read(MAGIC_TYPE));
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test
    void theShippedFileParsesWithoutAComplaintAndSaysWhy() throws IOException {
        Map<String, Object> json = FlowJson.object(read(FILE));
        assertTrue(json.get("why") instanceof String s && s.length() > 200, "every data file explains itself");
        MagicAlignments t = shipped();
        assertTrue(t.lightKeywords().size() >= 8 && t.darkKeywords().size() >= 15, "the keyword lists are not empty");
        for (String w : t.lightKeywords()) assertTrue(!t.darkKeywords().contains(w), "'" + w + "' is in both keyword lists");
    }

    @Test
    void everyMagicFamilyHasAnAlignmentThatThisTestNamesOnPurpose() throws IOException {
        List<String> families = families();
        Set<String> declared = new LinkedHashSet<>();
        declared.addAll(LIGHT);
        declared.addAll(DARK);
        declared.addAll(NEUTRAL);
        assertEquals(LIGHT.size() + DARK.size() + NEUTRAL.size(), declared.size(), "a family is listed under two alignments in this test");
        Set<String> missing = new TreeSet<>(families);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(), "new magic families that this test does not place (decide LIGHT, DARK or NEUTRAL, and add them to alignments.json if they are not neutral): " + missing);
        Set<String> gone = new TreeSet<>(declared);
        gone.removeAll(families);
        assertTrue(gone.isEmpty(), "families this test knows that MagicType no longer has: " + gone);
        assertEquals(105, families.size(), "the brief counts 105 families; if the enum grew, update the number here with the table");
    }

    @Test
    void theShippedTableClassifiesEveryFamilyAsDeclared() throws IOException {
        MagicAlignments t = shipped();
        List<String> wrong = new ArrayList<>();
        for (String f : families()) {
            Alignment want = LIGHT.contains(f) ? Alignment.LIGHT : DARK.contains(f) ? Alignment.DARK : Alignment.NEUTRAL;
            Alignment got = t.classify(f.toLowerCase(Locale.ROOT));
            if (got != want) wrong.add(f + " is " + got + " but should be " + want + " (" + t.explain(f.toLowerCase(Locale.ROOT)).rule() + ")");
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
    }

    @Test
    void theFamilyDecidesAlongsideAnyPageOfItsOwn() throws IOException {
        MagicAlignments t = shipped();
        // a page named after the opposite colour does not turn a Light family dark, or a Dark family light, unless the page has its own entry
        assertEquals(Alignment.LIGHT, t.classify("strike", "light"));
        assertEquals(Alignment.DARK, t.classify("light_prison", "demon_light"), "Light Prison belongs to Demon Light Magic, which is dark");
        assertEquals(Alignment.DARK, t.classify("purging_light", "demon_light"));
        assertEquals(Alignment.DARK, t.classify("bright_judgment_whip", "demon_light"));
        assertEquals(Alignment.NEUTRAL, t.classify("curse_cleanse", "curse_warding"), "a ward against curses is not a curse");
        assertEquals(Alignment.NEUTRAL, t.classify("undying_curse", "curse_warding"));
        assertEquals(Alignment.NEUTRAL, t.classify("curse_warding_strike", "curse_warding"));
        assertEquals(Alignment.NEUTRAL, t.classify("hex_aegis", "curse_warding"));
    }

    @Test
    void thePagesTheKeywordsAreMeantToCatchInNeutralFamilies() throws IOException {
        MagicAlignments t = shipped();
        assertEquals(Alignment.DARK, t.classify("devil_union", "light"), "Devil Union is on every grimoire, and it is a devil's pact");
        assertEquals(Alignment.DARK, t.classify("devil_union", "flame"));
        assertEquals(Alignment.DARK, t.classify("devil_dive", "anti_magic"));
        assertEquals(Alignment.DARK, t.classify("black_lightning_battle_fiend", "lightning"), "the page's own entry beats the Lightning family's");
        assertEquals(Alignment.DARK, t.classify("fallen_angel_gate", "spatial"), "angel is light, fallen is dark: dark wins");
        assertEquals(Alignment.DARK, t.classify("myriad_black", "spatial"));
        assertEquals(Alignment.DARK, t.classify("arresting_dark_weave", "thread"));
        assertEquals(Alignment.DARK, t.classify("death_scythe", "slash"));
        assertEquals(Alignment.DARK, t.classify("demon_king", "gravity"));
        assertEquals(Alignment.DARK, t.classify("evil_eye", "eye"));
        assertEquals(Alignment.NEUTRAL, t.classify("flame_bullet", "flame"));
        assertEquals(Alignment.NEUTRAL, t.classify("blinding_flash", "mist"));
        assertEquals(Alignment.NEUTRAL, t.classify("mana_strike", "wind"));
        assertEquals(Alignment.DARK, t.classify("summon_grimoire", "dark_beast"), "when the page says nothing, the keywords of the family id still count");
    }

    @Test
    void theTensuraSkillIdsTheKeywordsAreMeantToCatch() throws IOException {
        MagicAlignments t = shipped();
        for (String id : new String[] {"tensura:light", "tensura:solar_beam", "tensura:solar_flare", "tensura:solar_rain", "tensura:solar_wave", "tensura:sacred_haki",
                "tensura:light_transform", "tensura:magic_light_transform", "tensura:divine_ki_release"})
            assertEquals(Alignment.LIGHT, t.classify(id), id);
        for (String id : new String[] {"tensura:darkness", "tensura:dark_cube", "tensura:darkness_cannon", "tensura:true_darkness", "tensura:shadow_bind", "tensura:curse",
                "tensura:curse_bind", "tensura:hellfire", "tensura:demon_dominate", "tensura:demon_marionette", "tensura:demon_lord_haki", "tensura:black_flame",
                "tensura:black_lightning", "tensura:blood_mist", "tensura:death_march_dance", "tensura:dark_eight_palms", "tensura:darkness_transform",
                "tensura:magic_darkness_transform", "tensura:shadow_motion"})
            assertEquals(Alignment.DARK, t.classify(id), id);
        for (String id : new String[] {"tensura:lighten", "tensura:healing", "tensura:fire_ball", "tensura:lightning_lance", "tensura:thunder", "tensura:flight",
                "tensura:explosion", "tensura:megiddo", "tensura:burden", "tensura:analyze", "tensura:mental_crush", "tensura:dominate", "tensura:hypnos",
                "tensura:water_cutter", "tensura:full_recovery", "nusmp:book_flame", "minecraft:fireball"})
            assertEquals(Alignment.NEUTRAL, t.classify(id), id);
    }

    @Test
    void everyExactEntryNamesARealFamilyOrARealPage() throws IOException {
        MagicAlignments t = shipped();
        Set<String> families = new LinkedHashSet<>();
        for (String f : families()) families.add(f.toLowerCase(Locale.ROOT));
        String pages = allBookSources();
        List<String> bad = new ArrayList<>();
        for (String key : t.exactEntries().keySet()) {
            if (families.contains(key) || TENSURA_ENTRIES.contains(key)) continue;
            if (pages.contains("\"" + key + "\"")) continue;
            bad.add(key);
        }
        assertTrue(bad.isEmpty(), "exact entries that are neither a magic family nor a spell page id found in the book sources (a typo?): " + bad);
        assertNotNull(t.exactEntries().get("curse_warding"));
    }

    @Test
    void theCorrectionsFoundByReadingEveryRealPageStand() throws IOException {
        MagicAlignments t = shipped();
        assertEquals(Alignment.NEUTRAL, t.classify("raging_black_bull", "recombination"), "the Black Bulls are a squad, not darkness");
        assertEquals(Alignment.NEUTRAL, t.classify("raging_black_bull"), "and by its own name too");
        assertEquals(Alignment.NEUTRAL, t.classify("tensura:poison"), "Tensura's physical Poison toggle is not the Darkness-soul Poison Magic");
        assertEquals(Alignment.DARK, t.classify("poison"), "the family itself is still dark");
        assertEquals(Alignment.DARK, t.classify("tensura:create_greater_undead"));
        assertEquals(Alignment.DARK, t.classify("tensura:create_lesser_undead"));
        assertEquals(Alignment.NEUTRAL, t.classify("turn_undead_hypothetical"), "'undead' is deliberately not a keyword");
    }

    @Test
    void thePageNamesTheKeywordsTouchAreExactlyTheKnownOnes() throws IOException {
        MagicAlignments t = shipped();
        Set<String> pages = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            for (Path f : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher m = PAGE.matcher(read(f));
                while (m.find()) pages.add(m.group(1) != null ? m.group(1) : m.group(2));
            }
        }
        assertTrue(pages.size() > 500, "the page scan found only " + pages.size() + " page ids: the pattern no longer matches how pages are declared");
        Set<String> touched = new TreeSet<>();
        for (String id : pages) if (t.classify(id) != Alignment.NEUTRAL) touched.add(id);
        Set<String> fresh = new TreeSet<>(touched);
        fresh.removeAll(TOUCHED_BY_NAME);
        Set<String> gone = new TreeSet<>(TOUCHED_BY_NAME);
        gone.removeAll(touched);
        assertTrue(fresh.isEmpty(), "pages whose name now makes them light or dark (accept them in TOUCHED_BY_NAME, or give them an exact neutral entry): " + fresh);
        assertTrue(gone.isEmpty(), "pages that were light or dark by name and no longer are (renamed, removed, or the rules changed): " + gone);
    }

    @Test
    void everyPageOfALightOrDarkFamilyIsDecidedByItsFamilyNotByItsName() throws IOException {
        MagicAlignments t = shipped();
        // the only place a page name can overrule its family is an exact page entry, and those are listed on purpose
        assertEquals(Alignment.LIGHT, t.classify("dark_prison", "light"), "a Light family stays light whatever its page is called");
        assertEquals(Alignment.DARK, t.classify("holy_nonsense", "dark"), "a Dark family stays dark whatever its page is called");
        assertEquals(Alignment.DARK, t.classify("devil_union", "light"), "except where a page has an entry of its own");
    }

    private static String allBookSources() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> walk = Files.walk(BOOKS)) {
            for (Path f : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) sb.append(read(f)).append('\n');
        }
        return sb.toString();
    }
}
