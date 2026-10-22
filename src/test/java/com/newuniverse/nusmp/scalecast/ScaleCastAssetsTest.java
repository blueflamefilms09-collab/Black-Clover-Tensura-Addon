package com.newuniverse.nusmp.scalecast;

import com.google.gson.JsonElement;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every texture the Scale draws exists with the size the generator writes, the additive ones carry no colour under a fully transparent texel and the two card textures fade to nothing
 * at their border (the white square rule, SafeAdditive), the solid ones are opaque, the renderer names only textures that exist, the lang fragment holds every key the painter asks for and
 * no model or assistant names, and the preview PNGs (idle, tilted heavy and light, the suppression aura, the levels, beside a caster) are there.
 */
class ScaleCastAssetsTest {
    private static final Path TEX = Path.of("src/main/resources/assets/nusmp/textures/entity/scalecast");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Map<String, int[]> SIZE = Map.of("gold", new int[]{64, 64}, "enamel", new int[]{64, 64}, "chain", new int[]{32, 32}, "gem", new int[]{32, 32},
            "glow_runes", new int[]{64, 128}, "glow_halo", new int[]{256, 256}, "glow_ring", new int[]{256, 256});
    private static final List<String> SOLID = List.of("gold", "enamel", "chain", "gem"), ADDITIVE = List.of("glow_runes", "glow_halo", "glow_ring");

    private static BufferedImage image(String f) throws IOException { return ImageIO.read(TEX.resolve(f + ".png").toFile()); }

    @Test
    void everyTextureExistsWithItsSize() throws IOException {
        for (var e : SIZE.entrySet()) {
            assertTrue(Files.exists(TEX.resolve(e.getKey() + ".png")), "missing " + e.getKey());
            BufferedImage img = image(e.getKey());
            assertEquals(e.getValue()[0], img.getWidth(), e.getKey() + " width");
            assertEquals(e.getValue()[1], img.getHeight(), e.getKey() + " height");
        }
    }

    @Test
    void noAdditiveTextureCarriesColourUnderAFullyTransparentTexel() throws IOException {
        for (String f : ADDITIVE) {
            BufferedImage img = image(f);
            int lit = 0;
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    assertFalse((v >>> 24) == 0 && (v & 0xFFFFFF) != 0, f + " has colour under alpha 0 at " + x + "," + y);
                    if ((v >>> 24) > 40) lit++;
                }
            assertTrue(lit > img.getWidth() * img.getHeight() / 50, f + " is not empty");
        }
    }

    @Test
    void theCardTexturesFadeToNothingAtTheirBorder() throws IOException {
        for (String f : List.of("glow_halo", "glow_ring")) {
            BufferedImage img = image(f);
            int w = img.getWidth(), h = img.getHeight();
            for (int i = 0; i < w; i++) {
                assertTrue((img.getRGB(i, 0) >>> 24) <= 6 && (img.getRGB(i, h - 1) >>> 24) <= 6, f + " has a hard edge on a horizontal border");
                assertTrue((img.getRGB(0, i) >>> 24) <= 6 && (img.getRGB(w - 1, i) >>> 24) <= 6, f + " has a hard edge on a vertical border");
            }
            int c = (img.getRGB(w / 2, h / 2) >>> 24);
            assertTrue(f.equals("glow_halo") ? c > 60 : c >= 0, f + " centre");
        }
    }

    @Test
    void theSolidTexturesAreFullyOpaqueAndNotBlack() throws IOException {
        for (String f : SOLID) {
            BufferedImage img = image(f);
            long sum = 0;
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    assertEquals(255, v >>> 24, f + " alpha at " + x + "," + y);
                    sum += ((v >> 16) & 255) + ((v >> 8) & 255) + (v & 255);
                }
            assertTrue(sum / (img.getWidth() * img.getHeight() * 3L) > 60, f + " is too dark to be a lit material");
        }
    }

    @Test
    void theGoldIsGoldAndTheEnamelIsTealLikeTheScaleColour() throws IOException {
        BufferedImage g = image("gold"), e = image("enamel");
        double r = 0, gr = 0, b = 0, er = 0, eg = 0, eb = 0;
        int n = g.getWidth() * g.getHeight();
        for (int y = 0; y < g.getHeight(); y++)
            for (int x = 0; x < g.getWidth(); x++) {
                int v = g.getRGB(x, y), u = e.getRGB(x, y);
                r += (v >> 16) & 255; gr += (v >> 8) & 255; b += v & 255;
                er += (u >> 16) & 255; eg += (u >> 8) & 255; eb += u & 255;
            }
        assertTrue(r / n > gr / n && gr / n > b / n * 1.4, "gold: red over green over blue");
        assertTrue(eg / n > er / n * 1.2 && eb / n > er / n * 1.1, "enamel: teal, green and blue over red");
    }

    @Test
    void theRendererNamesOnlyTexturesThatExist() throws IOException {
        String src = Files.readString(SRC.resolve("client/scalecast/ScaleRender.java"));
        Matcher m = Pattern.compile("AuraRender\\.tex\\(\"entity/scalecast/(\\w+)\"\\)").matcher(src);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(Files.exists(TEX.resolve(m.group(1) + ".png")), "the renderer names a missing texture " + m.group(1));
        }
        assertEquals(SIZE.size(), n, "the renderer uses all " + SIZE.size() + " textures of the folder (found " + n + ")");
        assertTrue(src.contains("AuraRender.tex(\"particle/glow\")") && Files.exists(Path.of("src/main/resources/assets/nusmp/textures/particle/glow.png")));
    }

    @Test
    void theLangFragmentHoldsEveryKeyThePainterAsksForAndNoModelNames() throws IOException {
        String text = Files.readString(Path.of("tools/lang/scale_casting.json"));
        assertTrue(text.startsWith("{") && text.trim().endsWith("}"));
        JsonObject frag = JsonParser.parseString(text).getAsJsonObject();
        for (JsonElement v : frag.asMap().values()) assertTrue(v.isJsonPrimitive() && !v.getAsString().isBlank());
        String painter = Files.readString(SRC.resolve("client/prop/ScaleBalancePainter.java"));
        Matcher m = Pattern.compile("\"(nusmp\\.scale\\.[a-z_.]+)\"").matcher(painter);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(frag.has(m.group(1)), "missing lang key " + m.group(1));
        }
        assertEquals(frag.size(), n, "every key of the fragment is used and every used key is in it");
        for (String banned : new String[]{"Claude", "Anthropic", "Gemini", "GPT", "assistant"})
            assertFalse(text.contains(banned), "the fragment names " + banned);
    }

    @Test
    void thePreviewsOfTheScenesAreCommitted() {
        for (String f : List.of("scale_views", "scale_weighing", "scale_levels", "scale_caster", "scale_aura"))
            assertTrue(Files.exists(Path.of("docs/previews/scale_casting/" + f + ".png")), "missing preview " + f);
    }
}
