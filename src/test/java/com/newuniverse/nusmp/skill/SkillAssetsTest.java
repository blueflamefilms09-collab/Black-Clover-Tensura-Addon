package com.newuniverse.nusmp.skill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the skill rebuild promises three things that are easy to break by accident and invisible until someone opens the skill list: every
 * skill id still exists (old worlds must load), every skill has its own 32x32 icon (no two the same), and every skill's name and
 * description are in the lang fragment. The Unique -> Ultimate table must also stay as it was. This reads the sources and assets as files, so
 * it needs no Minecraft classes.
 */
class SkillAssetsTest {
    /** The ids the owner listed: 25 Uniques / Extras, 10 Ultimate evolutions, 3 standalone Ultimates. */
    private static final List<String> IDS = Arrays.asList(
            "summoner", "faith", "firebolt", "argonaut", "liaris_freese", "airiel", "hestia", "freya", "hephaestus", "loki",
            "wynn_fimbulvetr", "rea_laevateinn", "arcs_ray", "futsunomitama", "uchide_no_kozuchi", "hell_walker", "hawk_eye",
            "spirit_card", "unbreakable_will", "hati", "devils_footprints", "book_maker", "six_eyes", "gold_experience",
            "vector_manipulation",
            "lord_of_summons", "goddess_of_faith", "heros_argonaut", "hell_conqueror", "immortal_will", "adolla_burst",
            "all_fiction", "limitless", "gold_experience_requiem", "black_wings",
            "instant_death", "the_almighty", "the_visionary");

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Test
    void everyListedSkillIdIsStillRegistered() throws Exception {
        String registry = read("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java");
        for (String id : IDS) assertTrue(registry.contains("register(\"" + id + "\""), "skill id missing from NUSkills: " + id);
    }

    @Test
    void theEvolutionTableIsUnchanged() throws Exception {
        String src = read("src/main/java/com/newuniverse/nusmp/skill/SkillEvolution.java");
        String[][] pairs = {
                {"SUMMONER", "LORD_OF_SUMMONS"}, {"FAITH", "GODDESS_OF_FAITH"}, {"ARGONAUT", "HEROS_ARGONAUT"},
                {"HELL_WALKER", "HELL_CONQUEROR"}, {"UNBREAKABLE_WILL", "IMMORTAL_WILL"}, {"DEVILS_FOOTPRINTS", "ADOLLA_BURST"},
                {"BOOK_MAKER", "ALL_FICTION"}, {"SIX_EYES", "LIMITLESS"}, {"GOLD_EXPERIENCE", "GOLD_EXPERIENCE_REQUIEM"},
                {"VECTOR_MANIPULATION", "BLACK_WINGS"}};
        for (String[] p : pairs) assertTrue(src.contains("m.put(NUSkills." + p[0] + ", NUSkills." + p[1] + ");"), "evolution missing: " + p[0]);
        int puts = src.split("m\\.put\\(NUSkills\\.", -1).length - 1;
        assertEquals(pairs.length, puts, "no evolution path was added or dropped");
    }

    @Test
    void everySkillHasItsOwn32pxIcon() throws Exception {
        Map<String, String> seen = new HashMap<>();
        for (String id : IDS) {
            File f = new File("src/main/resources/assets/nusmp/textures/skill/" + id + ".png");
            assertTrue(f.isFile(), "icon missing: " + f);
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "icon unreadable: " + id);
            assertEquals(32, img.getWidth(), id + " width");
            assertEquals(32, img.getHeight(), id + " height");
            int[] px = img.getRGB(0, 0, 32, 32, null, 0, 32);
            String key = Arrays.toString(px);
            String clash = seen.put(key, id);
            assertNull(clash, id + " has the same icon as " + clash);
            long opaque = Arrays.stream(px).filter(p -> (p >>> 24) > 200).count();
            assertTrue(opaque > 32 * 32 * 0.8, id + " icon should fill its frame");
        }
    }

    @Test
    void everySkillNameAndDescriptionIsInTheLangFragment() throws Exception {
        JsonObject frag = JsonParser.parseString(read("tools/skills/lang_fragment.json")).getAsJsonObject();
        for (String id : IDS) {
            String name = "nusmp.skill." + id;
            assertTrue(frag.has(name) && !frag.get(name).getAsString().isBlank(), "name key missing: " + name);
            assertTrue(frag.has(name + ".description") && frag.get(name + ".description").getAsString().length() > 20,
                    "description key missing or too short: " + name);
        }
        // every chanted spell has all its lines
        int[][] lines = {{3}, {3}, {1}, {2}, {3}};
        String[] spells = {"wynn_fimbulvetr", "rea_laevateinn", "arcs_ray", "futsunomitama", "uchide_no_kozuchi"};
        for (int s = 0; s < spells.length; s++)
            for (int n = 1; n <= lines[s][0]; n++) assertTrue(frag.has("nusmp.skill.chant." + spells[s] + "." + n), spells[s] + " line " + n);
        // no value is empty
        for (var e : frag.entrySet()) assertFalse(e.getValue().getAsString().isBlank(), "empty text for " + e.getKey());
    }

    @Test
    void skillTextDerivesItsKeys() {
        SkillText t = SkillText.of("hati", "Hati", "A passive.");
        assertEquals("nusmp.skill.hati", t.nameKey());
        assertEquals("nusmp.skill.hati.description", t.descriptionKey());
        assertEquals("Hati", t.name());
    }

    @Test
    void noSkillSourceStillPointsAtAVanillaPlaceholderIcon() throws Exception {
        File dir = new File("src/main/java/com/newuniverse/nusmp/skill");
        for (File f : dir.listFiles((d, n) -> n.endsWith(".java"))) {
            String name = f.getName();
            if (name.startsWith("Grimoire") || name.startsWith("TimePages") || name.startsWith("NUSkills")) continue;   // not part of the rebuild
            String src = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            assertFalse(src.contains("withDefaultNamespace(\"textures/item/"), name + " still uses a vanilla item sprite as its skill icon");
            assertFalse(src.contains("withDefaultNamespace(\"textures/mob_effect/"), name + " still uses a vanilla effect sprite as its skill icon");
        }
    }

    @Test
    void noRebuiltSkillSpawnsAVanillaParticle() throws Exception {
        File dir = new File("src/main/java/com/newuniverse/nusmp/skill");
        for (File f : dir.listFiles((d, n) -> n.endsWith(".java"))) {
            String name = f.getName();
            if (name.startsWith("Grimoire") || name.startsWith("TimePages") || name.startsWith("NUSkills")) continue;
            String src = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            assertFalse(src.contains("ParticleTypes."), name + " references a vanilla particle");
            assertFalse(src.contains("sendParticles("), name + " sends a raw particle packet");
        }
    }
}
