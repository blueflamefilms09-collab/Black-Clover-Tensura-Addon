package com.newuniverse.nusmp.fairytail;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Fairy Tail skills ship assets a typo breaks silently: a 32x32 icon per skill (all different), a lang entry for every skill,
 * description, spell and message, the seven armour pieces (geo, texture, glow) of each of the ten forms, their body shells, the book
 * model and the registration lines. This reads them as files, so it needs no Minecraft classes.
 */
class FairyTailAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    @Test
    void everySkillHasItsOwnFilledIcon() throws Exception {
        Map<String, String> seen = new HashMap<>();
        for (FtLine line : FtLine.values()) {
            File f = new File(ASSETS + "textures/skill/" + line.id + ".png");
            assertTrue(f.isFile(), "icon missing: " + f);
            BufferedImage img = ImageIO.read(f);
            assertEquals(32, img.getWidth(), line.id);
            assertEquals(32, img.getHeight(), line.id);
            int[] px = img.getRGB(0, 0, 32, 32, null, 0, 32);
            String clash = seen.put(Arrays.toString(px), line.id);
            assertNull(clash, line.id + " has the same icon as " + clash);
            long opaque = Arrays.stream(px).filter(p -> (p >>> 24) > 200).count();
            assertTrue(opaque > 32 * 32 * 0.8, line.id + " icon should fill its frame");
        }
    }

    @Test
    void everyNameDescriptionSpellAndMessageIsInTheLangFragment() throws Exception {
        JsonObject frag = JsonParser.parseString(read("tools/lang/fairy_tail_skills.json")).getAsJsonObject();
        for (FtLine line : FtLine.values()) {
            String name = "nusmp.skill." + line.id;
            assertEquals(line.name, frag.get(name).getAsString());
            assertTrue(frag.get(name + ".description").getAsString().length() > 40, name + " description");
            for (FtMove mv : line.moves()) {
                String key = "nusmp.skill.mode." + line.id + "." + mv.id();
                assertTrue(frag.has(key) && frag.get(key).getAsString().equals(mv.name()), "mode key missing or different: " + key);
            }
        }
        for (String k : new String[]{"nusmp.skill.msg.fairytail.no_energy", "nusmp.skill.msg.fairytail.no_target", "nusmp.skill.msg.fairytail.aim",
                "nusmp.skill.msg.fairytail.locked", "nusmp.skill.msg.fairytail.cooldown", "item.nusmp.fairy_tail_skill_book", "nusmp.fairytail.learned"}) {
            assertTrue(frag.has(k), "lang key missing: " + k);
        }
    }

    @Test
    void everyFormHasItsSevenGeoPiecesTexturesGlowMapsAndShell() throws Exception {
        for (FtForms f : FtForms.ALL) {
            for (String piece : FtForms.PIECES) {
                String id = f.geoKey() + "_arm_" + piece;
                assertTrue(new File(ASSETS + "geo/entity/" + id + ".geo.json").isFile(), "geo missing: " + id);
                assertTrue(new File(ASSETS + "textures/entity/" + id + ".png").isFile(), "texture missing: " + id);
                assertTrue(new File(ASSETS + "textures/entity/" + id + "_glow.png").isFile(), "glow map missing: " + id);
            }
            assertTrue(new File(ASSETS + "animations/entity/" + f.geoKey() + "_arm_accent.animation.json").isFile(), "accent animation missing: " + f.key());
            assertTrue(new File(ASSETS + "textures/aura/ft_" + f.key() + "_shell.png").isFile(), "shell missing: " + f.key());
            assertTrue(new File(ASSETS + "textures/aura/ft_" + f.key() + "_shell_glow.png").isFile(), "shell glow missing: " + f.key());
        }
    }

    @Test
    void theAuraPainterListsTheSamePiecesAsTheForms() throws Exception {
        String src = read("src/main/java/com/newuniverse/nusmp/client/aura/FairyTailAura.java");
        for (String piece : FtForms.PIECES) assertTrue(src.contains("\"" + piece + "\""), "painter lacks piece " + piece);
        assertTrue(read("src/main/java/com/newuniverse/nusmp/aura/Aura.java").contains("FAIRY_TAIL"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/client/aura/AuraRegistry.java").contains("FairyTailAura.register();"));
    }

    @Test
    void theBookHasItsModelAndTexture() throws Exception {
        assertTrue(new File(ASSETS + "textures/item/fairy_tail_skill_book.png").isFile());
        JsonObject model = JsonParser.parseString(read(ASSETS + "models/item/fairy_tail_skill_book.json")).getAsJsonObject();
        assertEquals("nusmp:item/fairy_tail_skill_book", model.getAsJsonObject("textures").get("layer0").getAsString());
        BufferedImage img = ImageIO.read(new File(ASSETS + "textures/item/fairy_tail_skill_book.png"));
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
    }

    @Test
    void theSkillsAreRegisteredOnceAndTheEntryPointsAreWired() throws Exception {
        String sys = read("src/main/java/com/newuniverse/nusmp/fairytail/FairyTailSystem.java");
        assertTrue(sys.contains("SKILLS.register(line.id"), "skills are registered from the table");
        assertTrue(sys.contains("\"fairy_tail_skill_book\""));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("FairyTailSystem.init(modEventBus);"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/multiverse/MultiverseCommands.java").contains("FairyTailCommand.node()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java").contains("FairyTailSystem.creativeItems()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/fairytail/FairyTailSelfTests.java").contains("\"skill.fairytail\""));
        // the shared evolution table is untouched: Fairy Tail runs its own evolution
        assertFalse(read("src/main/java/com/newuniverse/nusmp/skill/SkillEvolution.java").contains("fairytail"));
    }

    @Test
    void theSkillIdsDoNotClashWithTheOldSkillRegisters() throws Exception {
        String registry = read("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java");
        for (FtLine line : FtLine.values()) assertFalse(registry.contains("register(\"" + line.id + "\""), "id already used in NUSkills: " + line.id);
    }
}
