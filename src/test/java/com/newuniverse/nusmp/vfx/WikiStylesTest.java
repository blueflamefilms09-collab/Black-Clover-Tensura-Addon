package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.WikiStyles.Group;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The 30 wiki magics and their visual groups (docs/wiki_magic_vfx.md). */
class WikiStylesTest {
    private static String[] names(String... n) { return n; }

    private static void assertGroup(Group g, String... magics) {
        for (String m : magics) {
            int style = WikiStyles.styleOf(m);
            assertTrue(WikiStyles.valid(style), m);
            assertEquals(g, WikiStyles.group(style), m);
        }
    }

    @Test
    void theOwnersGroupsAreExact() {
        assertGroup(Group.MOTES, "AIR", "SNOW", "SMOKE");
        assertGroup(Group.TRAILS, "HAIR", "POISON_PLANT", "VINE", "WING");
        assertGroup(Group.CIRCLES, "MEMORY", "MODIFICATION", "PERMEATION", "SOUL", "SOUND", "SWITCHING", "TREE");
        assertGroup(Group.SHARDS, "MINERAL", "NAIL", "RED_OCHRE", "ROCK", "SCALE", "SHAKUDO", "SPIKE", "STONE");
        assertGroup(Group.FLUID, "MUCUS", "MUD", "TONGUE");
        assertGroup(Group.SPIRAL, "SKIN", "SONG", "VORTEX");
        assertGroup(Group.INFERNAL, "SOUL_CORPSE");
        assertGroup(Group.SHARDS, "SANDSTONE");                              // the one the list did not name
    }

    @Test
    void thirtyDistinctMagicsEveryGroupUsed() {
        Set<String> seen = new HashSet<>();
        int[] perGroup = new int[Group.values().length];
        for (int i = 0; i < WikiStyles.COUNT; i++) {
            assertTrue(seen.add(WikiStyles.name(i)), "duplicate " + WikiStyles.name(i));
            assertEquals(i, WikiStyles.styleOf(WikiStyles.name(i)));
            perGroup[WikiStyles.group(i).ordinal()]++;
        }
        assertEquals(30, seen.size());
        for (Group g : Group.values()) {
            assertTrue(perGroup[g.ordinal()] > 0, g.name());
            assertEquals(perGroup[g.ordinal()], WikiStyles.groupSize(g), g.name());
        }
        assertEquals(1, WikiStyles.groupSize(Group.INFERNAL));
        assertEquals(30, java.util.Arrays.stream(Group.values()).mapToInt(WikiStyles::groupSize).sum());
    }

    @Test
    void theNumericStylesOfTheFirstLayerAreKept() {
        // the order of the old switch in WikiMagicLayer.style(): the per-magic variation of every existing look
        String[] old = names("AIR", "HAIR", "MEMORY", "MINERAL", "MODIFICATION", "MUCUS", "MUD", "NAIL", "PERMEATION", "POISON_PLANT",
                "RED_OCHRE", "ROCK", "SANDSTONE", "SCALE", "SHAKUDO", "SKIN", "SMOKE", "SNOW", "SONG", "SOUL_CORPSE", "SOUL", "SOUND",
                "SPIKE", "SWITCHING", "TONGUE", "TREE", "STONE", "VINE", "VORTEX", "WING");
        for (int i = 0; i < old.length; i++) assertEquals(i, WikiStyles.styleOf(old[i]), old[i]);
    }

    @Test
    void variantsAreDistinctInsideAGroup() {
        for (Group g : Group.values()) {
            Set<Integer> v = new HashSet<>();
            for (int i = 0; i < WikiStyles.COUNT; i++) if (WikiStyles.group(i) == g) {
                assertTrue(WikiStyles.variant(i) >= 0 && WikiStyles.variant(i) < WikiStyles.groupSize(g));
                assertTrue(v.add(WikiStyles.variant(i)), "variant clash in " + g);
            }
            assertEquals(WikiStyles.groupSize(g), v.size());
        }
    }

    /** WikiMagicLayer / WikiAura index small per-variant tables by the magic's place in its group: these places are pinned here. */
    @Test
    void variantOrderMatchesThePerVariantTables() {
        String[][] order = {
                {"AIR", "SMOKE", "SNOW"},                                   // RISE: hang, billow, fall
                {"HAIR", "POISON_PLANT", "VINE", "WING"},                   // CRESCENTS / SWEEP; Vine wiggles, Wing beats
                {"SKIN", "SONG", "VORTEX"},                                 // STRANDS / HELIX; Song has notes, Vortex a cone
                {"MUCUS", "MUD", "TONGUE"},                                 // Mud is heavy and brown, Tongue is a ribbon
        };
        for (String[] group : order) for (int v = 0; v < group.length; v++)
            assertEquals(v, WikiStyles.variant(WikiStyles.styleOf(group[v])), group[v]);
        assertEquals(0, WikiStyles.variant(WikiStyles.styleOf("SOUL_CORPSE")));
    }

    @Test
    void darkPalettesAreLiftedBrightOnesAreNot() {
        assertEquals(0x9FE8FF, WikiStyles.lift(0x9FE8FF), "Air is already bright");
        assertEquals(0xE7F7FF, WikiStyles.lift(0xE7F7FF));
        int shakudo = WikiStyles.lift(0x6E4946);
        assertTrue(shakudo != 0x6E4946, "the dark bronze is lifted");
        assertTrue(((shakudo >> 16) & 255) > 0x6E && (shakudo & 255) > 0x46 && (shakudo & 0xFF000000) == 0);
        int black = WikiStyles.lift(0x000000);
        assertEquals(153, (black >> 16) & 255, "black is lifted by the 0.6 cap: " + Integer.toHexString(black));
        assertEquals(black, WikiStyles.lift(0x010101) | 0, "and a near-black one is capped the same way");
        for (int i = 0; i < WikiStyles.COUNT; i++) {                              // every palette ends up bright enough and is never darkened
            int rgb = 0x808080;
            assertTrue(WikiStyles.lift(rgb) == rgb);
        }
        assertEquals(0x00FFFFFF, WikiStyles.lift(0xFFFFFFFF) & 0x00FFFFFF);
    }

    @Test
    void unknownInputNeverThrows() {
        assertEquals(-1, WikiStyles.styleOf(null));
        assertEquals(-1, WikiStyles.styleOf("FLAME"));
        assertEquals(-1, WikiStyles.styleOf("air"));
        assertEquals(Group.MOTES, WikiStyles.group(-1));
        assertEquals(Group.MOTES, WikiStyles.group(30));
        assertEquals(0, WikiStyles.variant(99));
        assertNull(WikiStyles.name(30));
        assertEquals("wiki_air", WikiStyles.texture(-5));
        assertEquals("wiki_poison_plant", WikiStyles.texture(9));
        assertEquals("wiki_grp_infernal", WikiStyles.groupTexture(Group.INFERNAL));
        assertEquals("wiki_shell_spiral", WikiStyles.shellTexture(Group.SPIRAL));
    }

    @Test
    void theBlendFollowsTheShapeLanguage() {
        assertFalse(Group.MOTES.additive);
        assertFalse(Group.FLUID.additive);
        assertFalse(Group.INFERNAL.additive, "the dark core is alpha blended (embers are added on top)");
        assertTrue(Group.CIRCLES.additive);
        assertTrue(Group.SHARDS.additive);
    }

    /** The table must list exactly the 30 magics that WikiBooks treats as new attributes (read from the source: no Minecraft needed). */
    @Test
    void tableMatchesTheWikiBooksAttributeSet() throws Exception {
        String src = Files.readString(new File("src/main/java/com/newuniverse/nusmp/book/WikiBooks.java").toPath());
        int from = src.indexOf("NEW_ATTRIBUTES = EnumSet.of(");
        int to = src.indexOf(");", from);
        assertTrue(from > 0 && to > from);
        Matcher m = Pattern.compile("MagicType\\.([A-Z_]+)").matcher(src.substring(from, to));
        List<String> inBooks = new ArrayList<>();
        while (m.find()) inBooks.add(m.group(1));
        assertEquals(30, inBooks.size());
        assertEquals(new HashSet<>(inBooks), tableNames());
        // and the colour switch has an entry for each
        for (String n : inBooks) assertTrue(src.contains("case " + n + " -> 0xFF"), "palette of " + n);
    }

    @Test
    void everyTextureTheTableNamesIsGenerated() {
        File dir = new File("src/main/resources/assets/nusmp/textures/particle");
        for (int i = 0; i < WikiStyles.COUNT; i++) assertTrue(new File(dir, WikiStyles.texture(i) + ".png").isFile(), WikiStyles.texture(i));
        for (Group g : Group.values()) {
            assertTrue(new File(dir, WikiStyles.groupTexture(g) + ".png").isFile(), WikiStyles.groupTexture(g));
            assertTrue(new File(dir, WikiStyles.shellTexture(g) + ".png").isFile(), WikiStyles.shellTexture(g));
        }
    }

    private static Set<String> tableNames() {
        Set<String> s = new HashSet<>();
        for (int i = 0; i < WikiStyles.COUNT; i++) s.add(WikiStyles.name(i));
        return s;
    }
}
