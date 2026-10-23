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
 * Why: the Wind remake's pieces are generated files (tools/gen_sdw3_models.py): a mesh whose uv falls on a transparent texel of the atlas would lose a face in the game (the emissive pass drops it), a
 * texture with colour under alpha 0 would show as a box when drawn additive, a blue tint would leave the wind palette, a missing file would only show up in a running game. This test reads the real files.
 */
class SpiritDiveWind3AssetTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    private static BufferedImage png(String tex) throws IOException { return ImageIO.read(ASSETS.resolve("textures/" + tex + ".png").toFile()); }

    @Test
    void theTexturesExistWithTheirSize() throws IOException {
        for (String t : SpiritDiveWind3Rules.allTextures()) {
            assertTrue(Files.exists(ASSETS.resolve("textures/" + t + ".png")), "missing " + t);
            BufferedImage im = png(t);
            assertEquals(64, im.getWidth(), t);
            assertEquals(64, im.getHeight(), t);
            assertTrue(im.getColorModel().hasAlpha(), t + " has alpha");
        }
        for (String n : SpiritDiveWind3Mesh.NAMES) assertTrue(Files.exists(ASSETS.resolve("sdw3_meshes/" + n + ".txt")), "missing mesh " + n);
    }

    @Test
    void everyFaceOfEveryMeshLandsOnAnOpaqueTexelOfTheAtlas() throws IOException {
        BufferedImage skin = png(SpiritDiveWind3Rules.SKIN);
        List<String> bad = new ArrayList<>();
        for (String n : SpiritDiveWind3Mesh.NAMES) {
            SpiritDiveWind3Mesh m = SpiritDiveWind3Mesh.get(n);
            for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++)
                for (int q = 0; q < m.quads(l); q++) {
                    float u = 0, v = 0;
                    for (int k = 0; k < 4; k++) { u += m.uv[l][q * 8 + k * 2] / 4f; v += m.uv[l][q * 8 + k * 2 + 1] / 4f; }
                    int a = skin.getRGB(Math.min(63, (int) (u * 64f)), Math.min(63, (int) (v * 64f))) >>> 24;
                    if (a < 250) { bad.add(n + " layer " + l + " quad " + q + " uv " + u + "," + v); break; }
                }
        }
        assertTrue(bad.isEmpty(), "a face on a transparent texel: " + bad);
    }

    @Test
    void theTexturesAreCleanUnderAlphaZeroAndNeverBlue() throws IOException {
        for (String t : SpiritDiveWind3Rules.allTextures()) {
            BufferedImage im = png(t);
            long r = 0, g = 0, b = 0;
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y), a = p >>> 24;
                    if (a == 0) assertTrue((p & 0xFFFFFF) == 0, t + " has colour under alpha 0 at " + x + "," + y);
                    else { r += p >> 16 & 255; g += p >> 8 & 255; b += p & 255; }
                }
            assertTrue(g >= b && g >= r * 0.9, t + " is green / white, not blue");
        }
    }

    @Test
    void theMeshTintsAreGreenOrWhite() {
        for (String n : SpiritDiveWind3Mesh.NAMES) {
            SpiritDiveWind3Mesh m = SpiritDiveWind3Mesh.get(n);
            for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++)
                for (int c : m.tint[l]) assertTrue((c >> 8 & 255) >= (c & 255), n + " tint " + Integer.toHexString(c) + " has blue over green");
        }
    }
}
