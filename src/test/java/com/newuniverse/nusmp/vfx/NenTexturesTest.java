package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the remade Nen effects (3.57.0, tools/gen_nen_textures.py) are drawn additively, so each texture must exist, be a power of two on each side and carry no colour under a transparent pixel. */
class NenTexturesTest {
    private static final String[] NAMES = {"nen_flame_0", "nen_flame_1", "nen_flame_2", "nen_flame_3", "nen_ribbon", "nen_core", "nen_burst", "nen_ring", "nen_hex", "nen_sphere", "nen_eye",
            "nen_bolt", "nen_link", "nen_needle", "nen_gum", "nen_shard", "nen_crack", "nen_spark", "nen_mark"};

    @Test
    void everyTextureExistsAndIsCleanUnderItsAlpha() throws Exception {
        for (String n : NAMES) {
            Path p = Path.of("src/main/resources/assets/nusmp/textures/particle/" + n + ".png");
            assertTrue(Files.isRegularFile(p), n + " is missing");
            BufferedImage img = ImageIO.read(p.toFile());
            int w = img.getWidth(), h = img.getHeight();
            assertTrue(Integer.bitCount(w) == 1 && Integer.bitCount(h) == 1, n + " is not a power of two: " + w + "x" + h);
            assertTrue(w * h <= 256 * 256, n + " is bigger than 256x256");
            int solid = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) == 0) assertEquals(0, argb & 0xFFFFFF, n + " has colour under a transparent pixel at " + x + "," + y);
                else solid++;
            }
            assertTrue(solid > 0, n + " is empty");
        }
    }
}
