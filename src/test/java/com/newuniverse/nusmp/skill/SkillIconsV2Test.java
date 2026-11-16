package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: tools/skills/gen_icons_v2.py redrew the grimoire icons that were placeholders (29 stub magics x grimoire / buff / ultimate / sym, and the 30
 * wiki magics that shared six motifs). tools/skills/icons_v2.txt lists every redrawn icon as {@code set|path}. This pins, per set, that no two
 * redrawn icons are the same picture (not even their glyphs: the centre of the frame is compared), that each one fills its 32 px frame, that the
 * old version is kept under tools/skills/icons_classic/ (never in the resources), and that every redrawn icon really is a replacement. Reads files only.
 */
class SkillIconsV2Test {
    private static final String TEX = "src/main/resources/assets/anime_multiverse/textures/skill/";
    private static final String CLASSIC = "tools/skills/icons_classic/";
    /** Mean absolute RGB difference of the central 18x18 px (the glyph, not the frame) below which two icons count as the same symbol. */
    private static final double MIN_GLYPH_DIFF = 25.0;

    private static Map<String, List<String>> manifest() throws Exception {
        Map<String, List<String>> sets = new LinkedHashMap<>();
        for (String line : Files.readAllLines(Path.of("tools/skills/icons_v2.txt"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\\|");
            assertEquals(2, p.length, "bad manifest line: " + line);
            sets.computeIfAbsent(p[0], k -> new ArrayList<>()).add(p[1]);
        }
        return sets;
    }

    private static BufferedImage load(String path) throws Exception {
        File f = new File(path);
        assertTrue(f.isFile(), "missing: " + path);
        BufferedImage img = ImageIO.read(f);
        assertNotNull(img, "unreadable: " + path);
        assertEquals(32, img.getWidth(), path + " width");
        assertEquals(32, img.getHeight(), path + " height");
        return img;
    }

    private static double centreDiff(BufferedImage a, BufferedImage b) {
        long sum = 0;
        for (int y = 7; y < 25; y++)
            for (int x = 7; x < 25; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                for (int s = 0; s <= 16; s += 8) sum += Math.abs(((p >> s) & 255) - ((q >> s) & 255));
            }
        return sum / (18.0 * 18.0 * 3.0);
    }

    @Test
    void theManifestListsEverySetWithItsExpectedSize() throws Exception {
        Map<String, List<String>> sets = manifest();
        assertEquals(Arrays.asList("A-grimoire", "A-buff", "A-ultimate", "A-sym", "B-grimoire"), new ArrayList<>(sets.keySet()), "sets");
        for (String k : new String[] {"A-grimoire", "A-buff", "A-ultimate", "A-sym"}) assertEquals(29, sets.get(k).size(), k);
        assertEquals(30, sets.get("B-grimoire").size(), "B-grimoire");
    }

    @Test
    void everyRedrawnIconIsDistinctFromTheOthersOfItsSet() throws Exception {
        for (var e : manifest().entrySet()) {
            List<String> paths = e.getValue();
            List<BufferedImage> imgs = new ArrayList<>();
            for (String p : paths) imgs.add(load(TEX + p));
            for (int i = 0; i < imgs.size(); i++)
                for (int j = i + 1; j < imgs.size(); j++) {
                    assertFalse(Arrays.equals(imgs.get(i).getRGB(0, 0, 32, 32, null, 0, 32), imgs.get(j).getRGB(0, 0, 32, 32, null, 0, 32)),
                            paths.get(i) + " is the same picture as " + paths.get(j));
                    double d = centreDiff(imgs.get(i), imgs.get(j));
                    assertTrue(d >= MIN_GLYPH_DIFF, paths.get(i) + " and " + paths.get(j) + " draw (nearly) the same symbol, centre diff " + d);
                }
        }
    }

    @Test
    void everyRedrawnIconFillsItsFrame() throws Exception {
        for (var e : manifest().entrySet()) {
            for (String p : e.getValue()) {
                int[] px = load(TEX + p).getRGB(0, 0, 32, 32, null, 0, 32);
                long opaque = Arrays.stream(px).filter(c -> (c >>> 24) > 200).count();
                if (e.getKey().equals("A-sym")) {        // a symbol layer is only the glyph on a clear background: it must have a real glyph, not a stub speck
                    assertTrue(opaque > 32 * 32 * 0.10, p + " symbol layer is nearly empty");
                } else {
                    assertTrue(opaque > 32 * 32 * 0.9, p + " icon should fill its frame");
                }
            }
        }
    }

    @Test
    void theOldVersionOfEveryRedrawnIconIsKeptOutsideTheResourcesAndWasReplaced() throws Exception {
        for (var e : manifest().entrySet()) {
            for (String p : e.getValue()) {
                BufferedImage old = load(CLASSIC + p), now = load(TEX + p);
                assertFalse(Arrays.equals(old.getRGB(0, 0, 32, 32, null, 0, 32), now.getRGB(0, 0, 32, 32, null, 0, 32)), p + " was not actually redrawn");
            }
        }
        assertFalse(new File("src/main/resources/assets/anime_multiverse/textures/skill/icons_classic").exists(), "the v2 classic copies must not ship in the jar");
    }
}
