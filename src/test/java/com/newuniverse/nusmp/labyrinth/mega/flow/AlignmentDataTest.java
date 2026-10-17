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
            if (families.contains(key)) continue;
            if (pages.contains("\"" + key + "\"")) continue;
            bad.add(key);
        }
        assertTrue(bad.isEmpty(), "exact entries that are neither a magic family nor a spell page id found in the book sources (a typo?): " + bad);
        assertNotNull(t.exactEntries().get("curse_warding"));
    }

    private static String allBookSources() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> walk = Files.walk(BOOKS)) {
            for (Path f : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator) sb.append(read(f)).append('\n');
        }
        return sb.toString();
    }
}
