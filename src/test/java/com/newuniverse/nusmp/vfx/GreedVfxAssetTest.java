package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.devil.DevilWorld;
import com.newuniverse.nusmp.vfx.client.layer.GreedVfx;
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
 * Why: the Black Sword Greed / devil-hit look (GreedLayer + GreedDevilHits, docs/vfx_remake/greed_devils.md) reads its 19 textures by name and the server sends its 10 shapes by ordinal, so both are pinned:
 * every texture exists with a power-of-two size, some visible and some clear texels, clear corners on the round sprites, no black colour in a clear texel next to a visible one, no stray greed_ file, every name
 * drawn by the layer and made by the generator; the shapes are appended after KI_TRANSFER in order and DEVIL_HIT is the last shape; the layer is registered, keeps the engine's 400-vertex cap, draws nothing
 * with the classic settings on; every Greed shape is sent by the Greed code through GreedFx and the devil hit by DevilCombat through DevilHitFx; the world list matches the devil enum.
 */
class GreedVfxAssetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path PARTICLE = ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/particle");
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final VfxShape[] SHAPES = {VfxShape.GREED_SLASH, VfxShape.GREED_LANCE, VfxShape.GREED_FLAME, VfxShape.GREED_THREADS, VfxShape.GREED_REAP, VfxShape.GREED_DOME, VfxShape.GREED_MAW,
            VfxShape.GREED_LIGHTNING, VfxShape.GREED_AWAKEN, VfxShape.DEVIL_HIT};

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    private static String layers() throws IOException { return code(LAYER_DIR.resolve("GreedLayer.java")) + "\n" + code(LAYER_DIR.resolve("GreedDevilHits.java")); }

    @Test
    void everyTextureIsThereWithAPowerOfTwoSizeAndSomethingToDraw() throws IOException {
        List<String> bad = new ArrayList<>();
        Set<String> round = Set.of("core", "glow", "void", "flash", "pentagram", "crack", "scales", "blood");
        for (String name : GreedVfx.TEXTURE_NAMES) {
            Path f = PARTICLE.resolve(GreedVfx.fileName(name) + ".png");
            assertTrue(Files.isRegularFile(f), "missing " + name);
            BufferedImage img = ImageIO.read(f.toFile());
            assertNotNull(img, name);
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
    void noStrayGreedFileAndTheGeneratorMakesEveryName() throws IOException {
        Set<String> expected = new HashSet<>();
        for (String p : GreedVfx.allTextures()) expected.add(p.substring(p.lastIndexOf('/') + 1));
        Set<String> found = new HashSet<>();
        File[] files = PARTICLE.toFile().listFiles((d, n) -> n.startsWith(GreedVfx.PREFIX));
        assertNotNull(files);
        for (File f : files) found.add(f.getName());
        assertEquals(expected, found, "the greed_ files and the list in GreedVfx must be the same set");
        String gen = Files.readString(ROOT.resolve("tools/gen_greed_vfx_textures.py"), StandardCharsets.UTF_8);
        assertTrue(gen.contains("PREFIX = \"greed_\""));
        for (String n : GreedVfx.TEXTURE_NAMES) assertTrue(gen.contains("\"" + n + "\""), "the generator does not make " + n);
    }

    @Test
    void everyTextureIsDrawnByTheLayer() throws IOException {
        String layer = code(LAYER_DIR.resolve("GreedLayer.java")), all = layers();
        for (String n : GreedVfx.TEXTURE_NAMES) {
            String constant = n.toUpperCase(java.util.Locale.ROOT);
            assertTrue(layer.contains(constant + " = t(\"" + n + "\")"), n + " has no constant");
            assertTrue(all.split("\\b" + constant + "\\b", -1).length - 1 >= 2, n + " is generated but never drawn (a dead asset)");
        }
    }

    @Test
    void theShapesAreAppendedAfterKiInOrderAndTheLayerIsRegistered() throws IOException {
        for (int i = 0; i < SHAPES.length; i++) assertEquals(VfxShape.KI_TRANSFER.ordinal() + 1 + i, SHAPES[i].ordinal(), "appended in order: " + SHAPES[i]);
        VfxShape[] all = VfxShape.values();
        assertEquals(VfxShape.DEVIL_HIT, all[all.length - 1], "wave 2: DEVIL_HIT is the last shape (a later family is appended after it: move this pin)");
        String layer = code(LAYER_DIR.resolve("GreedLayer.java"));
        for (VfxShape s : SHAPES) assertTrue(layer.contains("VfxShape." + s.name()) || layer.contains("case " + s.name()), s + " is not drawn by GreedLayer");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java")).contains("register(new com.newuniverse.nusmp.vfx.client.layer.GreedLayer())"));
        assertFalse(layer.contains("hardVertexCap") || layer.contains("vertexBudget"), "the Greed layer keeps the engine's 400-vertex cap per effect");
        assertTrue(layer.contains("if (inst.shape == VfxShape.DEVIL_HIT ? GreedVfx.classicDevil() : GreedVfx.classicGreed()) return;"), "the classic settings must leave the layer drawing nothing");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/GreedVfxConfig.java")).contains("GreedVfx.setClassic(GREED.get(), DEVIL.get())"), "the client settings must reach the switches");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("GreedVfxConfig.init(modEventBus, modContainer)"), "the client config is registered");
    }

    @Test
    void everyShapeIsSentThroughTheTwoHelpers() throws IOException {
        Path greed = ROOT.resolve("src/main/java/com/newuniverse/nusmp/greed"), devil = ROOT.resolve("src/main/java/com/newuniverse/nusmp/devil");
        StringBuilder all = new StringBuilder();
        for (String f : new String[]{"GreedForms.java", "GreedService.java"}) {
            String c = code(greed.resolve(f));
            assertFalse(c.contains("VfxSpawn."), f + " must send its effects through GreedFx");
            all.append(c);
        }
        for (VfxShape s : SHAPES) if (s != VfxShape.DEVIL_HIT) assertTrue(all.toString().contains("VfxShape." + s.name()), s + " is never sent");
        assertTrue(code(greed.resolve("GreedFx.java")).contains("VfxSpawn.send("), "GreedFx must go through VfxSpawn (the per-tick effect budget)");
        String combat = code(devil.resolve("DevilCombat.java"));
        assertTrue(combat.contains("DevilHitFx.hit(") && !combat.contains("VfxSpawn."), "DevilCombat sends its hit through DevilHitFx");
        assertTrue(combat.indexOf("DevilHitFx.hit(") > combat.indexOf("c.lastEffect = now;"), "the hit is sent only after the once-a-second gate");
        assertTrue(code(devil.resolve("DevilHitFx.java")).contains("VfxSpawn.send(level, VfxShape.DEVIL_HIT"));
        assertTrue(Files.readString(ROOT.resolve(".github/workflows/smoke.yml"), StandardCharsets.UTF_8).contains(",greed,greed.vfx,devil.vfx,"), "the greed.vfx / devil.vfx self-tests are on the smoke list");
    }

    @Test
    void theDevilWorldListMatchesTheEnum() {
        DevilWorld[] w = DevilWorld.values();
        assertEquals(w.length, GreedVfx.DEVIL_WORLDS.length, "a devil world was added: give it a hit look");
        for (int i = 0; i < w.length; i++) {
            assertEquals(w[i].name(), GreedVfx.DEVIL_WORLDS[i]);
            assertEquals(i, GreedVfx.devilWorld(i + 1f), "power " + (i + 1) + " is " + w[i]);
        }
        assertEquals(0, GreedVfx.devilWorld(0f));
        assertEquals(0, GreedVfx.devilWorld(42f));
    }
}
