package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The generated art (tools/relics/gen_djinn_art.py) is all there and well formed: meshes, skins, icons and models of all twelve Djinn. */
class DjinnAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp");
    private static final Set<String> ALWAYS = Set.of("helm", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l", "gauntlet_r",
            "greave_l", "greave_r", "crest", "sheath", "wing_in_l", "wing_in_r", "wing_out_l", "wing_out_r");

    private static BufferedImage png(Path p) throws IOException {
        assertTrue(Files.exists(p), "missing " + p);
        return ImageIO.read(p.toFile());
    }

    @Test
    void everyDjinnHasItsArmourPiecesWithSaneQuads() throws IOException {
        for (Djinn d : Djinn.values()) {
            Path dir = ASSETS.resolve("djinn").resolve(d.id());
            for (String piece : ALWAYS) {
                Djinn.Look l = d.look();
                if (piece.startsWith("wing") && l.wings() == Djinn.Look.Wings.NONE) continue;
                Path f = dir.resolve(piece + ".txt");
                assertTrue(Files.exists(f), "missing " + f);
                int quads = 0;
                for (String line : Files.readAllLines(f)) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    String[] v = line.trim().split("\\s+");
                    assertEquals("qn", v[0], f.toString());
                    assertEquals(2 + 32, v.length, "a qn line has a layer and four vertices of eight numbers: " + f);
                    int layer = Integer.parseInt(v[1]);
                    assertTrue(layer == 0 || layer == 2, "layer " + layer + " in " + f);
                    for (int k = 0; k < 4; k++) {
                        int o = 2 + k * 8;
                        for (int j = 0; j < 3; j++) assertTrue(Math.abs(Float.parseFloat(v[o + j])) < 60f, "a vertex far from the model in " + f);
                        float u = Float.parseFloat(v[o + 3]), w = Float.parseFloat(v[o + 4]);
                        assertTrue(u >= -0.5f && u <= 128.5f && w >= -0.5f && w <= 128.5f, "uv out of the 128 space in " + f + ": " + u + "," + w);
                        double n = Math.sqrt(Math.pow(Float.parseFloat(v[o + 5]), 2) + Math.pow(Float.parseFloat(v[o + 6]), 2) + Math.pow(Float.parseFloat(v[o + 7]), 2));
                        assertEquals(1.0, n, 0.02, "unit normals in " + f);
                    }
                    quads++;
                }
                assertTrue(quads > 0, "empty piece " + f);
                assertTrue(quads < 700, "a single piece stays light: " + f + " has " + quads);
            }
            if (d.look().tail() != Djinn.Look.Tail.NONE) { assertTrue(Files.exists(dir.resolve("tail_seg.txt"))); assertTrue(Files.exists(dir.resolve("tail_tip.txt"))); }
            if (d.look().halo() != Djinn.Look.Halo.NONE) assertTrue(Files.exists(dir.resolve("halo.txt")));
            if (d.look().horns() != Djinn.Look.Horns.NONE) assertTrue(Files.exists(dir.resolve("horn_l.txt")));
            if (d.look().ears()) assertTrue(Files.exists(dir.resolve("ears.txt")));
        }
    }

    @Test
    void theWholeSetStaysUnderTheFrameBudget() throws IOException {
        for (Djinn d : Djinn.values()) {
            long total = 0;
            try (var files = Files.list(ASSETS.resolve("djinn").resolve(d.id()))) {
                for (Path f : (Iterable<Path>) files::iterator) total += Files.readAllLines(f).stream().filter(s -> s.startsWith("qn")).count();
            }
            assertTrue(total < 6000, d.name() + " draws " + total + " quads in a Full Equip; keep it under 6000");
        }
    }

    @Test
    void theTexturesHaveTheirSizes() throws IOException {
        Path ent = ASSETS.resolve("textures").resolve("entity").resolve("djinn");
        BufferedImage sil = png(ent.resolve("djinn_silhouette.png"));
        assertEquals(64, sil.getWidth());
        assertEquals(64, sil.getHeight());
        for (Djinn d : Djinn.values()) {
            BufferedImage a = png(ent.resolve(d.id() + "_armor.png")), g = png(ent.resolve(d.id() + "_armor_glow.png"));
            assertEquals(a.getWidth(), g.getWidth());
            assertEquals(a.getHeight(), g.getHeight());
            assertTrue(a.getWidth() >= 512 && a.getWidth() <= 2048);
            BufferedImage r = png(ent.resolve(d.id() + "_rune.png"));
            assertEquals(64, r.getWidth());
            assertEquals(64, r.getHeight());
            BufferedImage b = png(ent.resolve(d.id() + "_bust.png"));
            assertEquals(32, b.getWidth());
            BufferedImage icon = png(ASSETS.resolve("textures").resolve("item").resolve("djinn_seal_" + d.id() + ".png"));
            assertEquals(16, icon.getWidth());
            assertEquals(16, icon.getHeight());
            Path model = ASSETS.resolve("models").resolve("item").resolve("djinn_seal_" + d.id() + ".json");
            String json = Files.readString(model);
            assertTrue(json.contains("minecraft:item/generated") && json.contains("nusmp:item/djinn_seal_" + d.id()), model.toString());
        }
    }

    @Test
    void theRuneOverlayHasEyesAndIsNotEmpty() throws IOException {
        for (Djinn d : Djinn.values()) {
            BufferedImage r = png(ASSETS.resolve("textures").resolve("entity").resolve("djinn").resolve(d.id() + "_rune.png"));
            int lit = 0;
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) if ((r.getRGB(x, y) >>> 24) > 0) lit++;
            assertTrue(lit > 150 && lit < 1500, d.name() + " rune pixels: " + lit);
            assertTrue((r.getRGB(10, 12) >>> 24) > 0 && (r.getRGB(13, 12) >>> 24) > 0, "the eyes glow");
        }
    }
}
