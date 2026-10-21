package com.newuniverse.nusmp.visual.shields;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.110.0: the shield frame fit (ShieldFrame) over the real shield meshes: every mesh lands centred on vanilla's plate, its face on the plate's front plane, inside the
 * footprint box, and no mesh of a shield is turned or mirrored by the file (place angle 90 = upright, handle toward -z).
 */
class ShieldFrameTest {
    static final Path MESHES = Path.of("src", "main", "resources", "assets", "nusmp", "weapon_meshes");

    /** {xmin, xmax, ymin, ymax, zmin, zmax} over the body, void and glow layers; the place line in {@code place} (5 floats). */
    static float[] box(Path file, float[] place) throws IOException {
        float[] b = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
        for (String line : Files.readAllLines(file)) {
            String[] v = line.trim().split("\\s+");
            if (v[0].equals("place")) for (int i = 0; i < 5; i++) place[i] = Float.parseFloat(v[i + 1]);
            boolean q = v[0].equals("q"), qn = v[0].equals("qn");
            if (!q && !qn || Integer.parseInt(v[1]) > 2) continue;
            int stride = q ? 5 : 8;
            for (int k = 0; k < 4; k++) {
                float x = Float.parseFloat(v[2 + k * stride]), y = Float.parseFloat(v[3 + k * stride]), z = Float.parseFloat(v[4 + k * stride]);
                b[0] = Math.min(b[0], x);
                b[1] = Math.max(b[1], x);
                b[2] = Math.min(b[2], y);
                b[3] = Math.max(b[3], y);
                b[4] = Math.min(b[4], z);
                b[5] = Math.max(b[5], z);
            }
        }
        return b;
    }

    static List<Path> shieldMeshes() throws IOException {
        List<Path> out = new ArrayList<>();
        try (var files = Files.walk(MESHES)) {
            for (Path p : (Iterable<Path>) files.sorted()::iterator) {
                String n = p.getFileName().toString();
                if (!n.endsWith(".txt")) continue;
                if (ShieldSet.isShieldMesh(n.substring(0, n.length() - 4))) out.add(p);
            }
        }
        return out;
    }

    @Test
    void everyShieldMeshFitsTheVanillaPlateFrame() throws IOException {
        List<Path> meshes = shieldMeshes();
        assertTrue(meshes.size() >= ShieldSet.IDS.size() + 1, "the shields' own meshes, the curse form and the upgraded ones: " + meshes.size());
        List<String> bad = new ArrayList<>();
        for (Path p : meshes) {
            float[] place = new float[5];
            float[] b = box(p, place);
            String id = p.toString().replace('\\', '/').replaceAll(".*weapon_meshes/", "");
            if (!(b[1] > b[0]) || !(b[3] > b[2])) {
                bad.add(id + ": no body");
                continue;
            }
            if (Math.abs(place[2] - 90f) > 0.01f) bad.add(id + ": place angle " + place[2] + " (a shield mesh is upright, 90)");
            if (!(b[4] < 0f && b[5] > 0f)) bad.add(id + ": the mesh does not straddle z = 0 (face +z, handle -z)");
            float[] fit = ShieldFrame.fit(b[0], b[1], b[2], b[3], b[5]);
            float[] lo = ShieldFrame.apply(fit, b[0], b[2], b[4]), hi = ShieldFrame.apply(fit, b[1], b[3], b[5]);
            if (hi[0] - lo[0] > ShieldFrame.MAX_W + 1e-4f || hi[1] - lo[1] > ShieldFrame.MAX_H + 1e-4f) bad.add(id + ": bigger than the footprint box");
            if (Math.abs(hi[0] + lo[0]) > 1e-4f || Math.abs(hi[1] + lo[1]) > 1e-4f) bad.add(id + ": not centred on the plate");
            if (Math.abs(hi[2] - ShieldFrame.FACE_Z) > 1e-5f) bad.add(id + ": the face is not on the plate's front plane (z = " + hi[2] + ")");
            if (!(lo[2] < hi[2]) || lo[2] < -0.25f) bad.add(id + ": the back reaches " + lo[2] + " behind the plate");
            float h = hi[1] - lo[1], w = hi[0] - lo[0];
            if (Math.max(h, w * 0.9f) < 0.7f) bad.add(id + ": smaller than 0.7 blocks (h " + h + ", w " + w + ")");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theFitIsAUniformScaleAndAShift() {
        float[] fit = ShieldFrame.fit(-10f, 10f, -20f, 20f, 5f);
        assertEquals(Math.min(ShieldFrame.MAX_W / 20f, ShieldFrame.MAX_H / 40f), fit[0], 1e-6f);
        float[] top = ShieldFrame.apply(fit, 0f, 20f, 5f);
        assertEquals(ShieldFrame.MAX_H / 2f, top[1], 1e-5f);
        assertEquals(ShieldFrame.FACE_Z, top[2], 1e-6f);
        // a broken box leaves the mesh alone
        float[] none = ShieldFrame.fit(0f, 0f, 0f, 1f, 0f);
        assertEquals(1f, none[0]);
        assertEquals(0f, none[3]);
    }

    @Test
    void theVanillaPlateIsTheReference() {
        // 12 x 22 x 1 pixel plate, emblem on the +z side at 2/16, the handle behind it: ShieldFrame's constants say the same
        assertEquals(2f / 16f, ShieldFrame.FACE_Z, 1e-9f);
        assertTrue(ShieldFrame.MAX_H < 22f / 16f, "shorter than vanilla's plate");
        assertTrue(ShieldFrame.MAX_W > 12f / 16f, "room for the winged shield");
    }
}
