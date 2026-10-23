package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: merge 2 is made of generated meshes (tools/gen_sdm2_models.py for the slim spirits, tools/gen_sdm2_crowns.py for the crowns); a missing file, a mesh with a vertex out of its frame, a non unit normal, a
 * spirit that is secretly a stack of boxes or a crown that is not the small distinct shape would only show up in a running game. This test reads the real files. The old meshes (sdm_meshes, sdw4_meshes) must still be there.
 */
class SpiritDiveMerge2AssetTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    private static void unitNormals(SpiritDiveWind3Mesh m, List<String> bad, String n) {
        for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++) {
            float[] nn = m.nrm[l];
            for (int i = 0; i + 2 < nn.length; i += 3) {
                float len = nn[i] * nn[i] + nn[i + 1] * nn[i + 1] + nn[i + 2] * nn[i + 2];
                if (Math.abs(len - 1f) > 0.01f) { bad.add(n + " has a normal that is not a unit vector"); return; }
            }
        }
    }

    @Test
    void everySlimSpiritMeshExistsParsesAndHasASolidBodyAndAGlowShell() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String n : SpiritDiveMerge2Rules.meshes()) {
            Path f = ASSETS.resolve("sdm2_meshes/" + n + ".txt");
            if (!Files.exists(f)) { bad.add("missing " + n); continue; }
            long lines = Files.readAllLines(f).stream().filter(l -> l.startsWith("q ")).count();
            SpiritDiveWind3Mesh m = SpiritDiveMerge2Rules.mesh(n);
            if (m.quads(0) + m.quads(1) + m.quads(2) != lines) bad.add(n + ": " + lines + " quad lines but " + (m.quads(0) + m.quads(1) + m.quads(2)) + " parsed");
            if (m.quads(SpiritDiveWind3Mesh.ORNAMENT) != 0) bad.add(n + " has ornament quads");
            if (m.quads(SpiritDiveWind3Mesh.SOLID) < 12) bad.add(n + " has no solid body");
            if (!n.endsWith("_core") && m.quads(SpiritDiveWind3Mesh.GLOW) < 4) bad.add(n + " has no glow shell");
            if (m.radius > 40f) bad.add(n + " reaches " + m.radius + " px (32 px on the longest side)");
            unitNormals(m, bad, n);
        }
        assertTrue(bad.isEmpty(), String.join("; ", bad));
    }

    @Test
    void theSlimSpiritsAreSmoothNotStackedCubes() {
        // a cube model has at most six normal directions per cube; a smooth, curved mesh has many different ones
        for (String n : SpiritDiveMerge2Rules.meshes()) {
            if (n.endsWith("_core")) continue;
            SpiritDiveWind3Mesh m = SpiritDiveMerge2Rules.mesh(n);
            Set<Long> dirs = new HashSet<>();
            float[] nn = m.nrm[SpiritDiveWind3Mesh.SOLID];
            for (int i = 0; i + 2 < nn.length; i += 3) dirs.add(Math.round(nn[i] * 6f) * 10000L + Math.round(nn[i + 1] * 6f) * 100L + Math.round(nn[i + 2] * 6f));
            assertTrue(dirs.size() >= 40, n + " has only " + dirs.size() + " different face directions");
        }
    }

    @Test
    void thePosesAreTheCanonOnes() {
        // the character Sylph has the wings spread (wider than tall), the standard one has them folded (taller than wide); the Undine is a tall S, the Salamander is longer than tall
        float[] b = bounds(SpiritDiveMerge2Rules.mesh("wind_char"));
        assertTrue(b[0] > b[1] * 1.2f, "wind_char spread wings: " + b[0] + " x " + b[1]);
        b = bounds(SpiritDiveMerge2Rules.mesh("wind_std"));
        assertTrue(b[1] > b[0] * 1.5f, "wind_std folded wings: " + b[0] + " x " + b[1]);
        b = bounds(SpiritDiveMerge2Rules.mesh("water_char"));
        assertTrue(b[1] > b[0] * 1.4f, "the mermaid is a tall S");
        b = bounds(SpiritDiveMerge2Rules.mesh("fire_char"));
        assertTrue(b[0] > 20f && b[2] > 20f, "the coiled Salamander fills its frame in x and z: " + b[0] + " " + b[2]);
    }

    private static float[] bounds(SpiritDiveWind3Mesh m) {
        float[] lo = {1e9f, 1e9f, 1e9f}, hi = {-1e9f, -1e9f, -1e9f};
        for (int l = 0; l < 2; l++) {
            float[] p = m.pos[l];
            for (int i = 0; i + 2 < p.length; i += 3)
                for (int k = 0; k < 3; k++) { lo[k] = Math.min(lo[k], p[i + k]); hi[k] = Math.max(hi[k], p[i + k]); }
        }
        return new float[]{hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]};
    }

    @Test
    void everyCrownMeshExistsIsSmallAndHasABrightSolidBody() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String n : SpiritDiveMerge2Rules.crownMeshes()) {
            Path f = ASSETS.resolve("sdm2_meshes/" + n + ".txt");
            if (!Files.exists(f)) { bad.add("missing " + n); continue; }
            SpiritDiveWind3Mesh m = SpiritDiveMerge2Rules.mesh(n);
            if (m.quads(SpiritDiveWind3Mesh.SOLID) < 8) bad.add(n + " has no solid body");
            if (m.quads(SpiritDiveWind3Mesh.GLOW) < 1) bad.add(n + " has no glow");
            if (m.quads(SpiritDiveWind3Mesh.ORNAMENT) != 0) bad.add(n + " has a second ring (ornament layer)");
            unitNormals(m, bad, n);
            float maxXZ = 0f, maxY = -1e9f, minY = 1e9f;
            float[] p = m.pos[SpiritDiveWind3Mesh.SOLID];
            for (int i = 0; i + 2 < p.length; i += 3) { maxXZ = Math.max(maxXZ, (float) Math.hypot(p[i], p[i + 2])); maxY = Math.max(maxY, p[i + 1]); minY = Math.min(minY, p[i + 1]); }
            if (maxXZ > SpiritDiveMerge2Rules.NEW_RING + 3.5f) bad.add(n + " is " + maxXZ + " px wide (old ring: " + SpiritDiveMerge2Rules.OLD_RING + ")");
            if (maxXZ < SpiritDiveMerge2Rules.NEW_RING - 0.5f) bad.add(n + " has no ring of radius " + SpiritDiveMerge2Rules.NEW_RING);
            if (maxY > SpiritDiveMerge2Rules.NEW_TALL + 3f) bad.add(n + " is " + maxY + " px tall (old: " + SpiritDiveMerge2Rules.OLD_TALL + ")");
            if (maxY < 6f) bad.add(n + " is not tall enough to read: " + maxY);
            if (minY < -1.2f) bad.add(n + " hangs below its ring: " + minY);
        }
        assertTrue(bad.isEmpty(), String.join("; ", bad));
    }

    @Test
    void theWindCrownHasTheTallBarAtTheFrontAndAlternatingHeights() {
        // the front of the head is mesh +z: the tallest solid vertex must stand at x = 0, z = ring radius (the bar of the front), and the bars next to it are shorter
        for (String n : new String[]{"crown_wind_std", "crown_wind_char"}) {
            SpiritDiveWind3Mesh m = SpiritDiveMerge2Rules.mesh(n);
            float[] p = m.pos[SpiritDiveWind3Mesh.SOLID];
            float top = -1e9f, topX = 0f, topZ = 0f;
            for (int i = 0; i + 2 < p.length; i += 3) if (p[i + 1] > top) { top = p[i + 1]; topX = p[i]; topZ = p[i + 2]; }
            assertEquals(0f, topX, 0.2f, n + ": the tall bar is central");
            assertTrue(topZ > SpiritDiveMerge2Rules.NEW_RING * 0.8f, n + ": the tall bar is at the front (+z)");
            Set<Integer> heights = new HashSet<>();
            for (int i = 0; i + 2 < p.length; i += 3) if (p[i + 1] > 4.5f) heights.add(Math.round(p[i + 1] * 2f));
            assertTrue(heights.size() >= 5, n + ": the bars differ in height");
        }
    }

    @Test
    void theHalfCrownKeepsOnlyTheLeftSide() {
        for (String n : new String[]{"crown_wind_std_half", "crown_wind_char_half"}) {
            float[] p = SpiritDiveMerge2Rules.mesh(n).pos[SpiritDiveWind3Mesh.SOLID];
            for (int i = 0; i + 2 < p.length; i += 3) assertTrue(p[i] >= -0.9f, n + " has a vertex at x " + p[i]);   // the arrow heads of the two bars on the middle line are 0.8 px half wide
        }
    }

    @Test
    void theOldMeshesAndTheirTexturesStayAndTheCrownMeshesDifferPerElement() {
        for (String n : SpiritDiveMergeRules.meshes()) assertTrue(Files.exists(ASSETS.resolve("sdm_meshes/" + n + ".txt")), "the boxy mesh " + n + " stays");
        for (String n : SpiritDiveWind4Rules.NAMES) assertTrue(Files.exists(ASSETS.resolve("sdw4_meshes/" + n + ".txt")), "the old crown " + n + " stays");
        for (String t : SpiritDiveMergeRules.textures()) assertTrue(Files.exists(ASSETS.resolve("textures/" + t + ".png")), t);
        Set<Integer> verts = new HashSet<>();
        for (Element e : Element.values()) verts.add(SpiritDiveMerge2Rules.crownVerts(e, false, false));
        assertTrue(verts.size() >= 3, "the element crowns are different shapes");
    }
}
