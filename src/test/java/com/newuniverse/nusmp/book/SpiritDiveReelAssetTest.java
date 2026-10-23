package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the REEL look is made of generated textures (tools/gen_spirit_dive_reel_textures.py); a missing file, a wrong size, a texture that carries colour under alpha 0 (the additive blend would show
 * it as a box: SafeAdditive), a mosaic whose levels do not grow or a wing without its quill would only show up in a running game. This test reads the real files.
 */
class SpiritDiveReelAssetTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    private static BufferedImage png(String tex) throws IOException {
        return ImageIO.read(ASSETS.resolve("textures/" + tex + ".png").toFile());
    }

    @Test
    void everyTextureExistsWithItsSize() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String t : SpiritDiveReelRules.allTextures()) {
            if (!Files.exists(ASSETS.resolve("textures/" + t + ".png"))) { bad.add("missing " + t); continue; }
            BufferedImage im = png(t);
            int w = im.getWidth(), h = im.getHeight();
            if (t.contains("sdr_wing_") && !t.contains("wind") && (w != 32 || h != 128)) bad.add(t + " is " + w + "x" + h + " (a blade is 32x128)");
            if (t.equals("aura/sdr_wing_wind") && (w != 128 || h != 256)) bad.add(t + " is " + w + "x" + h);
            if (t.equals("aura/sdr_wing_glow_wind") && (w != 128 || h != 256)) bad.add(t + " is " + w + "x" + h);
            if (t.contains("sdr_cube_") && (w != h)) bad.add(t + " is not square");
            if (t.contains("sdr_mosaic_") && (w != 64 || h != 64)) bad.add(t + " is " + w + "x" + h + " (the player skin sheet is 64x64)");
            if (t.contains("sdr_circle_") && w != h) bad.add(t + " is not square");
            if (t.contains("sdr_ring") && w != h) bad.add(t + " is not square");
        }
        assertTrue(bad.isEmpty(), bad.toString());
        for (String t : SpiritDiveReelRules.sharedTextures()) assertTrue(Files.exists(ASSETS.resolve("textures/" + t + ".png")), "the ANIME look's " + t + " must stay");
    }

    @Test
    void everyTextureIsCleanUnderAlphaZero() throws IOException {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (String t : SpiritDiveReelRules.allTextures()) {
            BufferedImage im = png(t);
            if (!im.getColorModel().hasAlpha()) continue;
            checked++;
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    if ((p >>> 24) == 0 && (p & 0xFFFFFF) != 0) { bad.add(t + " has colour under alpha 0 at " + x + "," + y); y = im.getHeight(); break; }
                }
        }
        assertTrue(checked >= 40, "the scan found the textures with alpha (" + checked + ")");
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theMosaicGrowsLevelByLevelAndCarriesTwoOrThreeShades() throws IOException {
        for (Element e : Element.values()) {
            int prev = -1;
            for (int k = 1; k <= SpiritDiveReelRules.MOSAIC_LEVELS; k++) {
                BufferedImage im = png(SpiritDiveReelRules.mosaicTexture(e, k));
                int lit = 0;
                java.util.Set<Integer> shades = new java.util.HashSet<>();
                for (int y = 0; y < im.getHeight(); y++)
                    for (int x = 0; x < im.getWidth(); x++) {
                        int p = im.getRGB(x, y);
                        if ((p >>> 24) > 40) { lit++; shades.add((p & 0xFFFFFF) >> 4); }
                    }
                assertTrue(lit >= prev, e + " level " + k + ": the patches only ever grow (" + lit + " after " + prev + ")");
                prev = lit;
                if (k == SpiritDiveReelRules.MOSAIC_LEVELS) assertTrue(lit > 300 && shades.size() >= 3, e + ": the last level is the whole patchwork in several shades (" + lit + ", " + shades.size() + ")");
            }
        }
    }

    @Test
    void theCubesAreBrightBlocksWithALightCentre() throws IOException {
        for (Element e : Element.values()) {
            BufferedImage im = png(SpiritDiveReelRules.cubeTexture(e));
            int w = im.getWidth(), h = im.getHeight();
            assertTrue(lum(im.getRGB(w / 2, h / 2)) > lum(im.getRGB(1, 1)) || lum(im.getRGB(w / 2, h / 2)) > 450, e + ": a light core inside the block");
            assertEquals(255, im.getRGB(w / 2, h / 2) >>> 24, e + ": the block is solid");
        }
    }

    @Test
    void everyWingBladeHasAQuillAndAFadingRoot() throws IOException {
        for (Element e : new Element[]{Element.WATER, Element.FIRE, Element.EARTH}) {
            BufferedImage im = png(SpiritDiveReelRules.wingTexture(e));
            int w = im.getWidth(), h = im.getHeight();
            assertTrue((im.getRGB(w / 2, h / 2) >>> 24) > 200, e + ": a solid quill");
            assertTrue((im.getRGB(w / 2, 0) >>> 24) == 0, e + ": the very first row is clear");
            BufferedImage g = png(SpiritDiveReelRules.wingGlowTexture(e));
            int lit = 0;
            for (int y = 0; y < g.getHeight(); y += 2) for (int x = 0; x < g.getWidth(); x++) if ((g.getRGB(x, y) >>> 24) > 100) lit++;
            assertTrue(lit > 0 && lit < g.getWidth() * g.getHeight() / 2 * 0.7, e + ": the glow is the edge and the quill, not a full card (" + lit + ")");
        }
        BufferedImage wind = png(SpiritDiveReelRules.wingTexture(Element.WIND));
        int solid = 0;
        for (int y = 0; y < wind.getHeight(); y += 4) for (int x = 0; x < wind.getWidth(); x += 4) if ((wind.getRGB(x, y) >>> 24) > 60) solid++;
        assertTrue(solid > 300, "the leaf is a membrane with hexagons: " + solid);
    }

    @Test
    void theGeneratorExistsAndWritesEveryTexture() throws IOException {
        String py = Files.readString(Path.of("").toAbsolutePath().resolve("tools/gen_spirit_dive_reel_textures.py"), java.nio.charset.StandardCharsets.UTF_8);
        for (String p : new String[]{"sdr_cube_", "sdr_bar_", "sdr_circle_", "sdr_mosaic_", "sdr_wing_", "sdr_wing_glow_", "sdr_ribbon", "sdr_ring", "sdr_ring_gold", "sdr_halo_ring", "sdr_ground", "sdr_flash", "sdr_spark", "sdr_gold",
                "sdr_sword_", "sdr_spike", "sdr_spire"})
            assertTrue(py.contains(p), "the generator does not write " + p);
    }

    private static int lum(int p) { return ((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255); }
}
