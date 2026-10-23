package com.newuniverse.nusmp.book;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the ANIME look is made of generated textures and borrows the geo models of the four spirits; a missing file, an additive texture with colour under a zero alpha (the additive blend would show it as
 * a box: SafeAdditive), a plume texture without its outline or a backdrop whose quad count in the budget does not match the model would only show up in a running game. This test reads the real files.
 */
class SpiritDiveAnimeAssetTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    private static BufferedImage png(String tex) throws IOException {
        return ImageIO.read(ASSETS.resolve("textures/" + tex + ".png").toFile());
    }

    @Test
    void everyTextureExistsWithItsSize() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String t : SpiritDiveAnimeRules.allTextures()) {
            if (!Files.exists(ASSETS.resolve("textures/" + t + ".png"))) { bad.add("missing " + t); continue; }
            BufferedImage im = png(t);
            int w = im.getWidth(), h = im.getHeight();
            if (t.contains("swirl") && (w != 256 || h != 256)) bad.add(t + " is " + w + "x" + h);
            if ((t.contains("plume_") || t.contains("plume_glow")) && (w != 128 || h != 256)) bad.add(t + " is " + w + "x" + h);
            if (t.contains("lock_") && (w != 64 || h != 128)) bad.add(t + " is " + w + "x" + h);
            if ((t.contains("halo") || t.contains("hair")) && (w != 256 || h != 256)) bad.add(t + " is " + w + "x" + h);
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyAdditiveTextureIsCleanUnderAlphaZero() throws IOException {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (String t : SpiritDiveAnimeRules.allTextures()) {
            if (!SpiritDiveAnimeRules.additive(t)) continue;
            BufferedImage im = png(t);
            if (!im.getColorModel().hasAlpha()) continue;                      // an opaque additive texture is black where it must not light: checked below
            checked++;
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    if ((p >>> 24) == 0 && (p & 0xFFFFFF) != 0) { bad.add(t + " has colour under alpha 0 at " + x + "," + y); y = im.getHeight(); break; }
                }
        }
        assertTrue(checked >= 18, "the scan found the additive textures with alpha (" + checked + ")");
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theSwirlAndRimTexturesAreOpaqueAndTheSwirlHasDarkGapsForTheSkinToShowThrough() throws IOException {
        for (Element e : Element.values()) {
            BufferedImage im = png(SpiritDiveAnimeRules.swirlTexture(e));
            int bright = 0, dim = 0, n = im.getWidth() * im.getHeight();
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    assertEquals(255, p >>> 24, "the swirl is opaque (black = no light)");
                    int lum = (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3;
                    if (lum > 150) bright++;
                    if (lum < 110) dim++;
                }
            assertTrue(bright > n / 50, e + ": some bright streaks");
            assertTrue(dim > n / 3, e + ": a dim base so the skin and the clothes show through (" + dim + " of " + n + ")");
        }
        for (String t : new String[]{SpiritDiveAnimeRules.RIM_TEXTURE, SpiritDiveAnimeRules.SOFT_TEXTURE}) {
            BufferedImage im = png(t);
            assertEquals(64, im.getWidth());
            assertTrue(((im.getRGB(3, 3) >> 8) & 255) > 120, t + " is a lit texture");
        }
    }

    @Test
    void thePlumeBodiesHaveAnOutlineAndAFadingRootAndTheGlowIsSparse() throws IOException {
        for (Element e : Element.values()) {
            BufferedImage body = png(SpiritDiveAnimeRules.plumeTexture(e));
            int w = body.getWidth(), h = body.getHeight();
            int midA = body.getRGB(w / 2, h / 2) >>> 24, rootA = body.getRGB(w / 2, 1) >>> 24, edgeA = body.getRGB(4, h / 4) >>> 24;
            assertTrue(midA > 120 && midA < 255, e + " body alpha " + midA + ": translucent");
            assertTrue(rootA < 80, e + " the root fades in (alpha " + rootA + ")");
            assertTrue(edgeA > 150, e + " the outline is nearly solid (alpha " + edgeA + ")");
            // the outline is the darkest band of the plume: darker than the middle
            int o = body.getRGB(4, h / 4), m = body.getRGB(w / 2, h / 2);
            assertTrue(lum(o) < lum(m), e + ": the outline is darker than the body");
            BufferedImage glow = png(SpiritDiveAnimeRules.plumeGlowTexture(e));
            int lit = 0;
            for (int y = 0; y < glow.getHeight(); y += 4) for (int x = 0; x < glow.getWidth(); x += 4) if ((glow.getRGB(x, y) >>> 24) > 100) lit++;
            assertTrue(lit > 20 && lit < (glow.getWidth() / 4) * (glow.getHeight() / 4) * 0.8, e + " the glow is the leading edge and the haze, not a full card (" + lit + ")");
        }
    }

    private static int lum(int p) { return ((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255); }

    @Test
    void theHairCapCoversTheTopAndTheBackOfTheHatLayerAndLeavesTheFaceFree() throws IOException {
        for (Element e : Element.values()) {
            BufferedImage im = png(SpiritDiveAnimeRules.hairTexture(e));
            assertTrue((im.getRGB(44 * 4, 4 * 4) >>> 24) > 200, e + ": the top of the head is covered");
            assertTrue((im.getRGB(60 * 4, 10 * 4) >>> 24) > 200, e + ": the back of the head is covered");
            assertTrue((im.getRGB(44 * 4, 15 * 4) >>> 24) == 0, e + ": the chin side of the front face is free");
            assertTrue((im.getRGB(44 * 4, 12 * 4) >>> 24) == 0, e + ": the eyes are free");
            assertTrue((im.getRGB(44 * 4, 9 * 4) >>> 24) > 200, e + ": a fringe over the forehead");
            assertTrue((im.getRGB(52 * 4, 0) >>> 24) == 0, e + ": nothing on the underside of the hat");
        }
    }

    // ------------------------------------------------------------------ the spirit behind the wearer
    @Test
    void theBackdropQuadCountsAreTheModelsRealOnes() throws IOException {
        for (Element e : Element.values()) {
            int quads = 0;
            for (String id : SpiritDiveAnimeRules.BACKDROP_MODEL[e.ordinal()]) {
                String stem = id.replace(':', '_');
                Path geo = ASSETS.resolve("geo/entity/" + stem + ".geo.json");
                assertTrue(Files.exists(geo), "missing " + geo);
                assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + stem + "_glow.png")), "the backdrop draws the glow map of " + stem);
                JsonObject root = JsonParser.parseString(Files.readString(geo, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement g : root.getAsJsonArray("minecraft:geometry"))
                    for (JsonElement b : g.getAsJsonObject().getAsJsonArray("bones"))
                        if (b.getAsJsonObject().has("cubes")) quads += 6 * b.getAsJsonObject().getAsJsonArray("cubes").size();
            }
            assertEquals(quads, SpiritDiveAnimeRules.BACKDROP_QUADS[e.ordinal()], e + ": the budget counts the model's own quads");
        }
    }

    @Test
    void theCircleBehindTheBackAndTheEntryPillarAreTheExistingSprites() {
        for (String s : new String[]{"sdv_sigil_wind", "sdv_sigil_water", "sdv_sigil_fire", "sdv_sigil_earth", "sdv_pillar"})
            assertTrue(Files.exists(ASSETS.resolve("textures/aura/" + s + ".png")), s);
    }
}
