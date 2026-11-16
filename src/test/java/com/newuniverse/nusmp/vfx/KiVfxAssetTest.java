package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.client.layer.KiVfx;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Dragon Ball Ki look (KiLayer, docs/vfx_remake/ki_dragonball.md) reads its 15 textures by name and the server sends its 13 shapes by ordinal, so both are pinned: every texture exists with a
 * power-of-two size, some visible and some clear texels, clear corners on the round sprites, no black colour in a clear texel next to a visible one (no dark fringe under linear filtering), no stray ki_ file,
 * every name drawn by the layer and made by the generator; the shapes are appended after NEN_DOMAIN in order; the layer is registered, keeps the engine's 400-vertex cap, draws nothing with the classic
 * setting on, and every shape is sent by the Saiyan code.
 */
class KiVfxAssetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path PARTICLE = ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/particle");
    private static final Path LAYER = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer/KiLayer.java");
    private static final VfxShape[] SHAPES = {VfxShape.KI_BLAST, VfxShape.KI_BEAM, VfxShape.KI_CHARGE, VfxShape.KI_SPHERE, VfxShape.KI_SHOCKWAVE, VfxShape.KI_AFTERIMAGE, VfxShape.KI_RUSH,
            VfxShape.KI_STRIKE, VfxShape.KI_BARRIER, VfxShape.KI_DEFLECT, VfxShape.KI_BIND, VfxShape.KI_DRAGON, VfxShape.KI_TRANSFER};

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    private static BufferedImage image(String name) throws IOException {
        BufferedImage img = ImageIO.read(PARTICLE.resolve(KiVfx.fileName(name) + ".png").toFile());
        assertNotNull(img, name);
        return img;
    }

    @Test
    void everyTextureIsThereWithAPowerOfTwoSizeAndSomethingToDraw() throws IOException {
        List<String> bad = new ArrayList<>();
        Set<String> round = Set.of("core", "halo", "flash", "spark", "swirl", "hex");
        for (String name : KiVfx.TEXTURE_NAMES) {
            assertTrue(Files.isRegularFile(PARTICLE.resolve(KiVfx.fileName(name) + ".png")), "missing " + name);
            BufferedImage img = image(name);
            int w = img.getWidth(), h = img.getHeight();
            if (Integer.bitCount(w) != 1 || Integer.bitCount(h) != 1 || w > 256 || h > 256) bad.add(name + " is " + w + "x" + h);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            int visible = 0, clear = 0;
            for (int p : px) if ((p >>> 24) > 20) visible++; else clear++;
            if (visible == 0 || clear == 0) bad.add(name + " has no visible or no clear texel");
            if (round.contains(name)) for (int c : new int[]{0, w - 1, (h - 1) * w, h * w - 1}) if ((px[c] >>> 24) != 0) bad.add(name + " corner is not clear");
            int dark = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) != 0 || (px[y * w + x] & 0xFFFFFF) != 0) continue;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int xx = x + dx, yy = y + dy;
                    if (xx >= 0 && yy >= 0 && xx < w && yy < h && (px[yy * w + xx] >>> 24) > 20) dark++;
                }
            }
            if (dark > 0) bad.add(name + " has black clear texels next to visible ones");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noStrayKiFileAndTheGeneratorMakesEveryName() throws IOException {
        Set<String> expected = new HashSet<>();
        for (String p : KiVfx.allTextures()) expected.add(p.substring(p.lastIndexOf('/') + 1));
        Set<String> found = new HashSet<>();
        File[] files = PARTICLE.toFile().listFiles((d, n) -> n.startsWith(KiVfx.PREFIX));
        assertNotNull(files);
        for (File f : files) found.add(f.getName());
        assertEquals(expected, found, "the ki_ files and the list in KiVfx must be the same set");
        String gen = Files.readString(ROOT.resolve("tools/gen_ki_vfx_textures.py"), StandardCharsets.UTF_8);
        assertTrue(gen.contains("PREFIX = \"ki_\""));
        for (String n : KiVfx.TEXTURE_NAMES) assertTrue(gen.contains("\"" + n + "\""), "the generator does not make " + n);
    }

    @Test
    void everyTextureIsDrawnByTheLayer() throws IOException {
        String layer = code(LAYER);
        for (String n : KiVfx.TEXTURE_NAMES) {
            String constant = n.toUpperCase(java.util.Locale.ROOT);
            assertTrue(layer.contains(constant + " = t(\"" + n + "\")"), n + " has no constant");
            assertTrue(layer.split("\\b" + constant + "\\b", -1).length - 1 >= 2, n + " is generated but never drawn (a dead asset)");
        }
    }

    @Test
    void theShapesAreAppendedAfterNenInOrderAndTheLayerIsRegistered() throws IOException {
        for (int i = 0; i < SHAPES.length; i++) assertEquals(VfxShape.NEN_DOMAIN.ordinal() + 1 + i, SHAPES[i].ordinal(), "appended in order: " + SHAPES[i]);
        String layer = code(LAYER);
        for (VfxShape s : SHAPES) assertTrue(layer.contains("VfxShape." + s.name()) || layer.contains("case " + s.name()), s + " is not drawn by KiLayer");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java")).contains("register(new com.newuniverse.nusmp.vfx.client.layer.KiLayer())"));
        assertFalse(layer.contains("hardVertexCap") || layer.contains("vertexBudget"), "the Ki layer keeps the engine's 400-vertex cap per effect");
        assertTrue(layer.contains("if (KiVfx.classic()) return;"), "the classic setting must leave the layer drawing nothing");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/client/aura/SaiyanLookClientConfig.java")).contains("KiVfx.setClassic(CLASSIC_VFX.get())"), "the client setting must reach the switch");
    }

    @Test
    void everyShapeIsSentByTheSaiyanTechniquesThroughTheOneHelper() throws IOException {
        Path saiyan = ROOT.resolve("src/main/java/com/newuniverse/nusmp/saiyan");
        StringBuilder all = new StringBuilder();
        for (String f : new String[]{"SaiyanAbilities.java", "GokuAbilities.java", "SaiyanTechniques.java", "SaiyanTechniques3.java"}) {
            String c = code(saiyan.resolve(f));
            assertFalse(c.contains("VfxSpawn."), f + " must send its Ki effects through SaiyanKiFx");
            all.append(c);
        }
        for (VfxShape s : SHAPES) assertTrue(all.toString().contains("VfxShape." + s.name()), s + " is never sent");
        assertTrue(code(saiyan.resolve("SaiyanKiFx.java")).contains("VfxSpawn.send("), "SaiyanKiFx must go through VfxSpawn (the per-tick effect budget)");
    }
}
