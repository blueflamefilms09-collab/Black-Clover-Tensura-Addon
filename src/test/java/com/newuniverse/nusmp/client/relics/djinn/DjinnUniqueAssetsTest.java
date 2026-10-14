package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The generated art of the unique and dark looks (tools/relics/gen_djinn_unique.py) is all there and well formed, each Djinn's pieces differ from every
 * other Djinn's, the dark sets add the eye crest and the halo, the weapon is in item space, the old shared rig is still in the repo, and each set stays
 * under the frame budget.
 */
class DjinnUniqueAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp");
    private static final List<String> CORE = List.of("helm", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l", "gauntlet_r", "greave_l",
            "greave_r", "crest", "wing_in_l", "wing_in_r", "wing_out_l", "wing_out_r", "weapon", "backdrop");
    /** The most lines (body + glow quads) a set may draw in a Full Equip, the tail counted five times as the layer draws it. */
    private static final int BUDGET = 5200;

    private static Path set(Djinn d, String prefix) { return ASSETS.resolve("djinn").resolve(prefix + "_" + d.id()); }

    /** Parses a piece file: asserts the format of every line and returns {lines, min y, max y}. */
    private static float[] check(Path f) throws IOException {
        assertTrue(Files.exists(f), "missing " + f);
        int quads = 0;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (String line : Files.readAllLines(f)) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] v = line.trim().split("\\s+");
            assertEquals("qn", v[0], f.toString());
            assertEquals(2 + 32, v.length, "a qn line has a layer and four vertices of eight numbers: " + f);
            int layer = Integer.parseInt(v[1]);
            assertTrue(layer == 0 || layer == 2, "layer " + layer + " in " + f);
            for (int k = 0; k < 4; k++) {
                int o = 2 + k * 8;
                for (int j = 0; j < 3; j++) assertTrue(Math.abs(Float.parseFloat(v[o + j])) < 80f, "a vertex far from the model in " + f);
                minY = Math.min(minY, Float.parseFloat(v[o + 1]));
                maxY = Math.max(maxY, Float.parseFloat(v[o + 1]));
                float u = Float.parseFloat(v[o + 3]), w = Float.parseFloat(v[o + 4]);
                assertTrue(u >= -0.5f && u <= 128.5f && w >= -0.5f && w <= 128.5f, "uv out of the 128 space in " + f + ": " + u + "," + w);
                double n = Math.sqrt(Math.pow(Float.parseFloat(v[o + 5]), 2) + Math.pow(Float.parseFloat(v[o + 6]), 2) + Math.pow(Float.parseFloat(v[o + 7]), 2));
                assertEquals(1.0, n, 0.02, "unit normals in " + f);
            }
            quads++;
        }
        assertTrue(quads > 0, "empty piece " + f);
        assertTrue(quads < 800, "a single piece stays light: " + f + " has " + quads);
        return new float[]{quads, minY, maxY};
    }

    private static int lines(Path dir) throws IOException {
        int total = 0;
        try (var files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                int n = (int) Files.readAllLines(f).stream().filter(s -> s.startsWith("qn")).count();
                total += f.getFileName().toString().equals("tail_seg.txt") ? n * 5 : n;
            }
        }
        return total;
    }

    @Test
    void everyDjinnHasAUniqueAndADarkSetWithTheCorePieces() throws IOException {
        for (Djinn d : Djinn.values()) {
            for (String prefix : new String[]{"u", "d"}) {
                Path dir = set(d, prefix);
                assertTrue(Files.isDirectory(dir), "missing set " + dir);
                for (String piece : CORE) check(dir.resolve(piece + ".txt"));
                assertTrue(Files.exists(dir.resolve(DjinnLook.MARKER_PIECE + ".txt")), "the marker piece decides the set");
            }
            assertTrue(Files.exists(set(d, "d").resolve("eye.txt")), d.name() + ": the Dark Djinn's eye crest");
            assertTrue(Files.exists(set(d, "d").resolve("halo.txt")), d.name() + ": every dark Djinn wears the ring of thorns");
            assertFalse(Files.exists(set(d, "u").resolve("eye.txt")), d.name() + ": the eye crest is dark only");
        }
    }

    @Test
    void theSetsStayUnderTheFrameBudget() throws IOException {
        for (Djinn d : Djinn.values()) {
            for (String prefix : new String[]{"u", "d"}) {
                int n = lines(set(d, prefix));
                assertTrue(n <= BUDGET, d.name() + " " + prefix + " draws " + n + " quads in a Full Equip; the budget is " + BUDGET);
                assertTrue(n >= 2000, d.name() + " " + prefix + " has only " + n + " quads: is a builder missing?");
            }
        }
    }

    @Test
    void theWeaponIsInItemSpaceWithTheBladeAlongPlusY() throws IOException {
        for (Djinn d : Djinn.values()) {
            for (String prefix : new String[]{"u", "d"}) {
                float[] w = check(set(d, prefix).resolve("weapon.txt"));
                assertTrue(w[2] > 20f, d.name() + " weapon reaches " + w[2] + " px along +y");
                assertTrue(w[1] > -34f && w[1] < 6f, d.name() + " weapon's grip end is " + w[1] + ": the grip sits near the origin (a bow is held in the middle: its lower limb reaches back)");
            }
        }
    }

    @Test
    void noTwoDjinnShareAPieceThatMakesTheSilhouette() throws IOException {
        for (String piece : new String[]{"helm", "wing_out_l", "weapon", "backdrop", "pauldron_l", "chest"}) {
            for (String prefix : new String[]{"u", "d"}) {
                Map<Integer, Djinn> seen = new HashMap<>();
                for (Djinn d : Djinn.values()) {
                    int h = Files.readAllLines(set(d, prefix).resolve(piece + ".txt")).stream().filter(s -> s.startsWith("qn")).mapToInt(s -> s.substring(0, Math.min(s.length(), 90)).hashCode()).sum();
                    Djinn other = seen.put(h, d);
                    assertNull(other, piece + " of " + d.name() + " (" + prefix + ") is the same as " + (other == null ? "" : other.name()));
                }
            }
        }
    }

    @Test
    void theOldSharedRigStaysAsTheFallback() throws IOException {
        Path ent = ASSETS.resolve("textures").resolve("entity").resolve("djinn");
        for (Djinn d : Djinn.values()) {
            assertTrue(Files.exists(ASSETS.resolve("djinn").resolve(d.id()).resolve("chest.txt")), d.name() + ": the old rig's chest");
            assertTrue(Files.exists(ASSETS.resolve("djinn").resolve(d.id()).resolve("sheath.txt")), d.name() + ": the old rig's sheath");
            assertTrue(Files.exists(ent.resolve(d.id() + "_armor.png")), d.name());
            assertTrue(Files.exists(ent.resolve(d.id() + "_armor_glow.png")), d.name());
            assertTrue(Files.exists(ent.resolve(d.id() + "_rune.png")), d.name());
            assertTrue(Files.exists(ent.resolve(d.id() + "_bust.png")), d.name());
        }
    }

    private static BufferedImage png(Path p) throws IOException {
        assertTrue(Files.exists(p), "missing " + p);
        return ImageIO.read(p.toFile());
    }

    @Test
    void theTexturesHaveTheirSizes() throws IOException {
        Path ent = ASSETS.resolve("textures").resolve("entity").resolve("djinn");
        for (Djinn d : Djinn.values()) {
            for (String v : new String[]{"u", "d"}) {
                BufferedImage a = png(ent.resolve(d.id() + "_" + v + "_armor.png")), g = png(ent.resolve(d.id() + "_" + v + "_armor_glow.png"));
                assertEquals(a.getWidth(), a.getHeight());
                assertTrue(a.getWidth() >= 512, "armour skin of at least 512 px");
                assertEquals(a.getWidth(), g.getWidth(), "the glow skin matches the body skin");
                BufferedImage r = png(ent.resolve(d.id() + "_" + v + "_rune.png"));
                assertEquals(64, r.getWidth());
                assertEquals(64, r.getHeight());
                int lit = 0;
                for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) if ((r.getRGB(x, y) >>> 24) > 0) lit++;
                assertTrue(lit > 40, d.name() + " " + v + " rune has only " + lit + " lit pixels");
            }
            BufferedImage art = png(ent.resolve(d.id() + "_d_art.png"));
            assertEquals(64, art.getWidth());
            assertEquals(64, art.getHeight());
            BufferedImage bust = png(ent.resolve(d.id() + "_d_bust.png"));
            assertEquals(32, bust.getWidth());
            assertEquals(32, bust.getHeight());
        }
    }

    @Test
    void theDarkSkinsAreDarkAndGold() throws IOException {
        Path ent = ASSETS.resolve("textures").resolve("entity").resolve("djinn");
        for (Djinn d : Djinn.values()) {
            BufferedImage dark = png(ent.resolve(d.id() + "_d_armor.png")), own = png(ent.resolve(d.id() + "_u_armor.png"));
            assertTrue(meanLuma(dark) < meanLuma(own) * 0.8, d.name() + ": the dark skin is darker than the Djinn's own (" + meanLuma(dark) + " vs " + meanLuma(own) + ")");
        }
    }

    private static double meanLuma(BufferedImage im) {
        double sum = 0;
        long n = 0;
        for (int y = 0; y < im.getHeight(); y += 3) {
            for (int x = 0; x < im.getWidth(); x += 3) {
                int p = im.getRGB(x, y);
                if ((p >>> 24) < 128) continue;
                sum += 0.3 * ((p >> 16) & 255) + 0.59 * ((p >> 8) & 255) + 0.11 * (p & 255);
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }
}
