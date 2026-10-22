package com.newuniverse.nusmp.compass3d;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every texture the 3D compass draws exists with the size the layout says, the additive ones carry no colour under a fully transparent texel (the white square rule, SafeAdditive), the solid
 * ones are fully opaque inside their cutout, the renderer names only textures that exist, and the lang fragment has no model or assistant names.
 */
class CompassAssetsTest {
    private static final Path TEX = Path.of("src/main/resources/assets/nusmp/textures/entity/compass3d");
    private static final Map<String, String> NAME = Map.of("rim", "rim", "face", "face", "faceGlow", "face_glow", "star", "star", "needle", "needle", "glass", "glass", "aura", "aura", "glint", "glint",
            "sealRing", "seal_ring");

    private static BufferedImage image(String file) throws IOException { return ImageIO.read(TEX.resolve(file + ".png").toFile()); }

    @Test
    void everyTextureExistsWithTheSizeOfTheLayout() throws IOException {
        JsonObject sizes = JsonParser.parseString(Files.readString(Path.of("tools/compass/compass3d_layout.json"))).getAsJsonObject().getAsJsonObject("textures");
        assertEquals(NAME.size(), sizes.size(), "the layout lists exactly the textures this test knows");
        for (var e : NAME.entrySet()) {
            assertTrue(Files.exists(TEX.resolve(e.getValue() + ".png")), "missing " + e.getValue());
            BufferedImage img = image(e.getValue());
            assertEquals(sizes.getAsJsonArray(e.getKey()).get(0).getAsInt(), img.getWidth(), e.getValue() + " width");
            assertEquals(sizes.getAsJsonArray(e.getKey()).get(1).getAsInt(), img.getHeight(), e.getValue() + " height");
        }
    }

    @Test
    void noTextureCarriesColourUnderAFullyTransparentTexel() throws IOException {
        for (String f : NAME.values()) {
            BufferedImage img = image(f);
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    assertFalse((v >>> 24) == 0 && (v & 0xFFFFFF) != 0, f + " has colour under alpha 0 at " + x + "," + y);
                }
        }
    }

    @Test
    void theSolidTexturesHaveNoHolesInTheirSampledColumns() throws IOException {
        // rim: fully opaque everywhere; face: opaque inside the disc (centre, and 0.95 of the radius)
        BufferedImage rim = image("rim");
        for (int y = 0; y < rim.getHeight(); y++) for (int x = 0; x < rim.getWidth(); x++) assertEquals(255, rim.getRGB(x, y) >>> 24, "rim is opaque");
        BufferedImage face = image("face");
        int w = face.getWidth(), c = w / 2;
        assertEquals(255, face.getRGB(c, c) >>> 24);
        assertEquals(255, face.getRGB(c + (int) (c * 0.95), c) >>> 24);
        assertEquals(0, face.getRGB(1, 1) >>> 24, "the corners outside the disc are transparent (cutout)");
        // star and needle: every texel opaque (no black gap at a seam: the painted columns are wider than the sampled ones)
        for (String f : List.of("star", "needle")) {
            BufferedImage img = image(f);
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    assertEquals(255, v >>> 24, f + " alpha");
                    assertTrue((v & 0xFFFFFF) != 0, f + " has an unpainted (black) texel at " + x + "," + y);
                }
        }
    }

    @Test
    void theFaceIsGreenAndTheRoseIsCreamGoldAsTheAnimeCompass() throws IOException {
        BufferedImage face = image("face");
        int c = face.getWidth() / 2, mid = face.getRGB(c + (int) (c * 0.70), c);           // the middle green band
        int r = (mid >> 16) & 255, g = (mid >> 8) & 255, b = mid & 255;
        assertTrue(g > r && g > b, "the dial is green");
        BufferedImage star = image("star");
        int lo = star.getRGB((int) (0.2 * star.getWidth()), (int) (0.1 * star.getHeight()));
        assertTrue(((lo >> 16) & 255) > 200 && ((lo >> 8) & 255) > 200, "the lit side of the rose is pale cream");
        BufferedImage nd = image("needle");
        int n = nd.getRGB((int) (0.2 * nd.getWidth()), (int) (0.3 * nd.getHeight()));
        assertTrue(((n >> 16) & 255) > 200 && ((n >> 16) & 255) > (n & 255) * 2, "the needle is gold");
    }

    @Test
    void theRendererAndTheMeshNameOnlyTexturesThatExist() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/compass/CompassRender.java"));
        var m = java.util.regex.Pattern.compile("tex\\(\"entity/compass3d/([a-z_]+)\"\\)").matcher(src);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(Files.exists(TEX.resolve(m.group(1) + ".png")), "CompassRender names a missing texture " + m.group(1));
        }
        assertEquals(9, n, "the renderer uses the nine compass textures");
    }

    @Test
    void theGeneratorIsDeterministicAndKnowsEveryTexture() throws IOException {
        String gen = Files.readString(Path.of("tools/compass/gen_compass3d_textures.py"));
        for (String f : NAME.values()) assertTrue(gen.contains("save(\"" + f + "\""), "the generator writes " + f);
        assertFalse(gen.contains("time.time") || gen.contains("random.random()"), "no clock, no unseeded random");
    }

    @Test
    void theLangFragmentHasNoModelOrAssistantNames() throws IOException {
        Path frag = Path.of("tools/lang/compass_3d.json");
        assertTrue(Files.exists(frag));
        JsonObject o = JsonParser.parseString(Files.readString(frag)).getAsJsonObject();
        assertTrue(o.size() >= 1);
        String all = Files.readString(frag).toLowerCase();
        for (String bad : List.of("claude", "anthropic", "gpt", "openai", "assistant")) assertFalse(all.contains(bad), bad);
    }
}
