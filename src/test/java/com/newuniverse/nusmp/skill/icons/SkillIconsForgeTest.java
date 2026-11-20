package com.newuniverse.nusmp.skill.icons;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: tools/icon_forge_v3/forge.py repainted every skill icon in pixel art (docs/skill_icons_v3.md). This pins what that redesign promises: every icon it replaced still exists at its old path and size, its
 * old picture is kept under textures/skill_old (rule 2, shown by the client setting skillIcons.classic), the new picture is real pixel art (a small palette: the smooth painter it replaces used 600 to 800
 * colours a picture, Tensura's own icons about 80), Time Magic was not touched, and the list the client switch reads matches the forge's list. Reads files only.
 */
class SkillIconsForgeTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path TEX = ASSETS.resolve("textures/skill");
    private static final Path OLD = ASSETS.resolve("textures/skill_old");
    /** Pixel art keeps few colours; the smooth painter it replaced had 600 to 800 a 32 px icon. */
    private static final int MAX_COLOURS = 170;

    private static List<String> replaced() throws Exception {
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("tools/icon_forge_v3/icons_v4.txt"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            out.add(line.trim());
        }
        return out;
    }

    private static BufferedImage png(Path p) throws Exception {
        assertTrue(Files.exists(p), "missing " + p);
        BufferedImage im = ImageIO.read(p.toFile());
        assertNotNull(im, "unreadable " + p);
        return im;
    }

    @Test
    void everyReplacedIconKeepsItsPathAndSizeAndItsOldPicture() throws Exception {
        List<String> all = replaced();
        assertTrue(all.size() >= 450, "the forge replaced " + all.size() + " pictures");
        for (String rel : all) {
            BufferedImage now = png(TEX.resolve(rel));
            assertEquals(32, now.getWidth(), rel + " width");
            assertEquals(0, now.getHeight() % 32, rel + " height is whole 32 px frames");
            BufferedImage old = png(OLD.resolve(rel));
            assertEquals(32, old.getWidth(), rel + " old width");
        }
    }

    @Test
    void theNewIconsAreRealPixelArt() throws Exception {
        for (String rel : replaced()) {
            BufferedImage im = png(TEX.resolve(rel));
            if (im.getHeight() != 32) continue;                       // strips: judged frame by frame in SkillIconsV3Test
            Set<Integer> colours = new HashSet<>();
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) colours.add(im.getRGB(x, y));
            assertTrue(colours.size() <= MAX_COLOURS, rel + " has " + colours.size() + " colours (pixel art keeps at most " + MAX_COLOURS + ")");
            int a = im.getRGB(16, 16) >>> 24;
            assertEquals(255, a, rel + ": the middle is opaque");
        }
    }

    @Test
    void timeMagicIsNotTouched() throws Exception {
        for (String rel : replaced()) {
            assertFalse(rel.contains("time"), "Time Magic icons are the quality standard and stay as they are: " + rel);
        }
    }

    @Test
    void theClientSwitchListMatchesTheForgeList() throws Exception {
        List<String> stills = new ArrayList<>();
        for (String rel : replaced()) if (!rel.startsWith("anim/") && !rel.startsWith("veldanava/")) stills.add(rel);
        List<String> listed = SkillIconIndex.parseOld(Files.readString(ASSETS.resolve("skill_icons_old.json")));
        assertEquals(new HashSet<>(stills), new HashSet<>(listed));
        assertTrue(listed.size() <= SkillIconIndex.MAX_OLD, "bounded");
        assertEquals(listed, SkillIconIndex.loadOld(), "the jar ships the same list");
    }

    @Test
    void badOldListsAreEmptyNeverAnException() {
        assertTrue(SkillIconIndex.parseOld(null).isEmpty());
        assertTrue(SkillIconIndex.parseOld("").isEmpty());
        assertTrue(SkillIconIndex.parseOld("not json").isEmpty());
        assertTrue(SkillIconIndex.parseOld("{\"paths\":[\"../x.png\"]}").isEmpty(), "no path may leave the skill folder");
        assertTrue(SkillIconIndex.parseOld("{\"paths\":[\"A.png\"]}").isEmpty(), "lower case only");
        assertTrue(SkillIconIndex.parseOld("{\"paths\":[\"a.png\",\"a.png\"]}").isEmpty(), "duplicate");
        assertEquals(List.of("a.png", "grimoire/flame.png"), SkillIconIndex.parseOld("{\"paths\":[\"a.png\",\"grimoire/flame.png\"]}"));
        assertEquals("textures/skill_old/grimoire/flame.png", SkillIconIndex.oldPath("grimoire/flame.png"));
        assertEquals("textures/skill/grimoire/flame.png", SkillIconIndex.livePath("grimoire/flame.png"));
    }

    @Test
    void theClientOptionAndSwapKnowTheClassicSwitch() throws Exception {
        String opt = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/skillicons/SkillIconOptions.java"));
        assertTrue(opt.contains("\"classic\""), "skillIcons.classic is a client config");
        assertTrue(opt.contains("ModConfigSpec.BooleanValue CLASSIC"), "the option exists");
        String swap = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/skillicons/SkillIconSwap.java"));
        assertTrue(swap.contains("SkillIconOptions.classic()"), "the swap reads it");
        assertTrue(swap.contains("skill_icons_old.json"), "and the list of replaced icons");
    }
}
