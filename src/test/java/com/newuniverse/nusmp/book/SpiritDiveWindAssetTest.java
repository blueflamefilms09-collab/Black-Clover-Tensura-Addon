package com.newuniverse.nusmp.book;

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
 * Why: the PACK Wind is made of generated textures (tools/gen_spirit_dive_wind_textures.py); a missing file, a wrong size (the painters assume the aspect of each: a bar is 24x96, a sword 64x256), a
 * texture that carries colour under alpha 0 (the additive blend would show it as a box: SafeAdditive), a leaf without its midrib or a texture that is blue would only show up in a running game. This test
 * reads the real files.
 */
class SpiritDiveWindAssetTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    private static BufferedImage png(String tex) throws IOException {
        return ImageIO.read(ASSETS.resolve("textures/" + tex + ".png").toFile());
    }

    @Test
    void everyTextureExistsWithItsSize() throws IOException {
        List<String> bad = new ArrayList<>();
        int[][] want = {{128, 256}, {128, 256}, {64, 64}, {24, 96}, {64, 16}, {128, 128}, {256, 256}, {64, 256}, {128, 128}, {128, 256}, {32, 256}, {32, 32}};
        List<String> all = SpiritDiveWindRules.allTextures();
        assertEquals(want.length, all.size());
        for (int i = 0; i < all.size(); i++) {
            String t = all.get(i);
            if (!Files.exists(ASSETS.resolve("textures/" + t + ".png"))) { bad.add("missing " + t); continue; }
            BufferedImage im = png(t);
            if (im.getWidth() != want[i][0] || im.getHeight() != want[i][1]) bad.add(t + " is " + im.getWidth() + "x" + im.getHeight() + ", expected " + want[i][0] + "x" + want[i][1]);
        }
        assertTrue(bad.isEmpty(), bad.toString());
        for (String t : SpiritDiveWindRules.sharedTextures()) assertTrue(Files.exists(ASSETS.resolve("textures/" + t + ".png")), "the REEL look's " + t + " must stay");
    }

    @Test
    void everyTextureIsCleanUnderAlphaZeroAndNeverBlue() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String t : SpiritDiveWindRules.allTextures()) {
            BufferedImage im = png(t);
            assertTrue(im.getColorModel().hasAlpha(), t + " has alpha");
            long r = 0, g = 0, b = 0;
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    int a = p >>> 24;
                    if (a == 0 && (p & 0xFFFFFF) != 0) bad.add(t + " has colour under alpha 0 at " + x + "," + y);
                    if (a > 40) { r += p >> 16 & 255; g += p >> 8 & 255; b += p & 255; }
                }
            assertTrue(g >= r && g >= b, t + " is green-white (r " + r + ", g " + g + ", b " + b + ")");
        }
        assertTrue(bad.isEmpty(), bad.subList(0, Math.min(5, bad.size())).toString());
    }

    @Test
    void theLeafHasAMidribAndAnEdgeAndTheGlowIsNotAFullCard() throws IOException {
        BufferedImage leaf = png(SpiritDiveWindRules.LEAF), glow = png(SpiritDiveWindRules.LEAF_GLOW);
        int w = leaf.getWidth(), h = leaf.getHeight();
        assertTrue((leaf.getRGB(w / 2, h / 2) >>> 24) > 200, "a solid midrib");
        assertTrue((leaf.getRGB(w / 2, 0) >>> 24) == 0, "the very first row is clear");
        assertTrue(lum(leaf.getRGB(w / 2, h / 2)) > lum(leaf.getRGB(w / 4, h / 2)), "the midrib is brighter than the sheet");
        assertTrue(lum(leaf.getRGB(1, h / 2)) > lum(leaf.getRGB(w / 4, h / 2)), "a bright edge");
        int lit = 0;
        for (int y = 0; y < h; y += 2) for (int x = 0; x < w; x++) if ((glow.getRGB(x, y) >>> 24) > 100) lit++;
        assertTrue(lit > 0 && lit < w * h / 2 * 0.6, "the glow is the edge, the rib and the veins, not a card (" + lit + ")");
    }

    @Test
    void theBarHasAnArrowHeadAtTheTopAndAShaftBelow() throws IOException {
        BufferedImage bar = png(SpiritDiveWindRules.BAR);
        int w = bar.getWidth(), h = bar.getHeight();
        int top = 0, shaft = 0;
        for (int x = 0; x < w; x++) { if ((bar.getRGB(x, 2) >>> 24) > 60) top++; }
        int head = extent(bar, 21), shaftW = extent(bar, h - 8);
        shaft = shaftW;
        assertTrue(head > shaft + 4, "wider at the arrow head than on the shaft (" + head + " vs " + shaft + ")");
        assertTrue(top <= 3, "a pointed tip");
        assertTrue(shaft >= 1);
    }

    @Test
    void theSwordIsALongBladeWithAGuardAt70PercentAndTheCrossesAreRings() throws IOException {
        BufferedImage sword = png(SpiritDiveWindRules.SWORD);
        int w = sword.getWidth(), h = sword.getHeight(), hilt = (int) (0.70 * h);
        int blade = 0, guard = 0;
        for (int x = 0; x < w; x++) { if ((sword.getRGB(x, hilt - 40) >>> 24) > 60) blade++; if ((sword.getRGB(x, hilt) >>> 24) > 60) guard++; }
        assertTrue(guard > blade, "the guard is wider than the blade (" + guard + " vs " + blade + ")");
        BufferedImage circle = png(SpiritDiveWindRules.CIRCLE), wrist = png(SpiritDiveWindRules.WRIST);
        assertTrue((circle.getRGB(circle.getWidth() / 2, circle.getHeight() / 2) >>> 24) < 60, "the circle is a ring: clear in the middle");
        assertTrue((wrist.getRGB(wrist.getWidth() / 2, wrist.getHeight() / 2) >>> 24) < 60, "the wrist circle is a ring: clear in the middle");
    }

    @Test
    void theGeneratorExistsAndWritesEveryTexture() throws IOException {
        String py = Files.readString(Path.of("").toAbsolutePath().resolve("tools/gen_spirit_dive_wind_textures.py"), java.nio.charset.StandardCharsets.UTF_8);
        for (String t : SpiritDiveWindRules.allTextures()) {
            String name = t.substring(t.lastIndexOf('/') + 1);
            assertTrue(py.contains(name), "the generator does not write " + name);
        }
    }

    /** The width between the outermost visible pixels of a row. */
    private static int extent(BufferedImage im, int y) {
        int lo = -1, hi = -1;
        for (int x = 0; x < im.getWidth(); x++) if ((im.getRGB(x, y) >>> 24) > 60) { if (lo < 0) lo = x; hi = x; }
        return lo < 0 ? 0 : hi - lo + 1;
    }

    private static int lum(int p) { return ((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255); }
}
