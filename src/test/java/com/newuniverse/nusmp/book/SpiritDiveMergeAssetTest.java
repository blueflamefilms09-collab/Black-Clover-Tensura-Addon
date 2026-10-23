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
 * Why: the merge is made of generated meshes (tools/gen_sdm_models.py) and two generated textures; a missing file, a mesh with a vertex out of the 32 px frame, a mesh that is not all on the additive layer or
 * a texture that carries colour under alpha 0 (the additive blend would show it as a box: SafeAdditive) would only show up in a running game. This test reads the real files.
 */
class SpiritDiveMergeAssetTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    @Test
    void everyMeshFileExistsParsesAndIsAllAdditiveWithinItsFrame() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String n : SpiritDiveMergeRules.meshes()) {
            Path f = ASSETS.resolve("sdm_meshes/" + n + ".txt");
            if (!Files.exists(f)) { bad.add("missing " + n); continue; }
            long lines = Files.readAllLines(f).stream().filter(l -> l.startsWith("q ")).count();
            SpiritDiveWind3Mesh m = SpiritDiveMergeMesh.get(n);
            if (m.quads(SpiritDiveWind3Mesh.GLOW) != lines) bad.add(n + ": " + lines + " quad lines but " + m.quads(SpiritDiveWind3Mesh.GLOW) + " parsed");
            if (m.quads(SpiritDiveWind3Mesh.SOLID) + m.quads(SpiritDiveWind3Mesh.ORNAMENT) != 0) bad.add(n + " has non additive quads");
            if (m.radius > 40f) bad.add(n + " reaches " + m.radius + " px from its centre (the meshes are 32 px on the longest side)");
            int cubes = m.quads(SpiritDiveWind3Mesh.GLOW) / 6;
            if (cubes < (n.endsWith("_core") ? 6 : 12)) bad.add(n + " has only " + cubes + " cubes");
            for (float[] nn : new float[][]{m.nrm[SpiritDiveWind3Mesh.GLOW]})
                for (int i = 0; i + 2 < nn.length; i += 3) {
                    float l = nn[i] * nn[i] + nn[i + 1] * nn[i + 1] + nn[i + 2] * nn[i + 2];
                    if (Math.abs(l - 1f) > 0.01f) { bad.add(n + " has a normal that is not a unit vector"); break; }
                }
            // outward winding: the normal points away from the mesh's own centre for the quads of the outer faces (a mirrored mesh would show its inside)
            int outward = 0, total = 0;
            float[] pos = m.pos[SpiritDiveWind3Mesh.GLOW], nrm = m.nrm[SpiritDiveWind3Mesh.GLOW];
            for (int q = 0; q < m.quads(SpiritDiveWind3Mesh.GLOW); q++) {
                float cx = 0, cy = 0, cz = 0;
                for (int k = 0; k < 4; k++) { cx += pos[q * 12 + k * 3] / 4; cy += pos[q * 12 + k * 3 + 1] / 4; cz += pos[q * 12 + k * 3 + 2] / 4; }
                total++;
                if (cx * nrm[q * 3] + cy * nrm[q * 3 + 1] + cz * nrm[q * 3 + 2] > -1e-3f) outward++;
            }
            if (outward < total * 0.55f) bad.add(n + ": only " + outward + " of " + total + " quads face away from the centre (inside out?)");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theTexturesExistAndAreCleanUnderAlphaZero() throws IOException {
        for (String t : SpiritDiveMergeRules.textures()) {
            Path f = ASSETS.resolve("textures/" + t + ".png");
            assertTrue(Files.exists(f), "missing " + t);
            BufferedImage im = ImageIO.read(f.toFile());
            assertTrue(im.getColorModel().hasAlpha(), t + " has an alpha channel");
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    assertTrue((p >>> 24) != 0 || (p & 0xFFFFFF) == 0, t + " has colour under alpha 0 at " + x + "," + y);
                }
        }
        BufferedImage skin = ImageIO.read(ASSETS.resolve("textures/aura/sdm_skin.png").toFile());
        assertEquals(255, skin.getRGB(8, 8) >>> 24, "the middle of the skin (where the meshes are mapped) is opaque plain light");
        assertEquals(0xFFFFFF, skin.getRGB(8, 8) & 0xFFFFFF);
        BufferedImage tr = ImageIO.read(ASSETS.resolve("textures/aura/sdm_trail.png").toFile());
        assertTrue((tr.getRGB(tr.getWidth() - 4, tr.getHeight() / 2) >>> 24) > 150, "the head of the strip is bright");
        assertTrue((tr.getRGB(2, tr.getHeight() / 2) >>> 24) < 40, "the tail of the strip fades out");
    }

    @Test
    void theSharedAndOldAssetsAreStillThere() {
        for (String f : new String[]{"textures/aura/sda_soft.png", "textures/particle/glow.png", "sdw3_meshes/crown.txt", "textures/aura/sdw3_skin.png"})
            assertTrue(Files.exists(ASSETS.resolve(f)), f + " must stay");
    }
}
