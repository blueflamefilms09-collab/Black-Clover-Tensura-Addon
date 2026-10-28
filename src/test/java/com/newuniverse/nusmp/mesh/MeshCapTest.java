package com.newuniverse.nusmp.mesh;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** 3.32.0: the detail limit keeps the extent of every layer, never drops the protected layers, is deterministic and leaves small meshes alone. */
class MeshCapTest {
    /** A quad of the given side length at (x, y), z = 0 (flat). */
    private static String qn(int layer, double x, double y, double side) {
        return String.format(Locale.ROOT, "qn %d %.3f %.3f 0.000 0.000 0.000 0.000 0.000 1.000 %.3f %.3f 0.000 1.000 0.000 0.000 0.000 1.000 "
                + "%.3f %.3f 0.000 1.000 1.000 0.000 0.000 1.000 %.3f %.3f 0.000 0.000 1.000 0.000 0.000 1.000",
                layer, x, y, x + side, y, x + side, y + side, x, y + side);
    }

    private static String mesh(int bodyQuads, int voidQuads, int haloQuads, boolean orbit) {
        StringBuilder sb = new StringBuilder("# part blade 0 60\nplace 0 0 0 0 0\n");
        if (orbit) sb.append("orbit 3 0.5 1.0\n");
        for (int i = 0; i < bodyQuads; i++) sb.append(qn(0, i, 10, 0.1 + (i % 7) * 0.2)).append('\n');
        for (int i = 0; i < voidQuads; i++) sb.append(qn(1, i, 20, 0.05)).append('\n');
        for (int i = 0; i < haloQuads; i++) sb.append(qn(4, i, 30, 0.05)).append('\n');
        for (int i = 0; i < 40; i++) sb.append(qn(3, i, 40, 0.05)).append('\n');
        return sb.toString();
    }

    @Test
    void aSmallMeshIsReturnedUntouched() {
        String text = mesh(50, 5, 5, false);
        MeshCap.Result r = MeshCap.cap(text, 3000);
        assertFalse(r.changed());
        assertSame(text, r.text());
    }

    @Test
    void zeroLimitMeansNoCut() {
        String text = mesh(500, 0, 0, false);
        assertFalse(MeshCap.cap(text, 0).changed());
    }

    @Test
    void theLimitIsRespectedAndTheSmallestQuadsGoFirst() {
        String text = mesh(2000, 0, 0, false);
        MeshCap.Result r = MeshCap.cap(text, 1000);
        assertEquals(2040, r.quadsBefore());
        assertEquals(1000, r.quadsAfter());
        assertEquals(1000, MeshCap.count(r.text()));
        assertTrue(r.areaLost() > 0 && r.areaLost() < 0.6, "lost " + r.areaLost());
        // the surviving body quads are the big ones: the smallest side in the input is 0.1, all of those are gone before any 1.3 one
        assertTrue(r.areaAfter() > r.areaBefore() * 0.4);
    }

    @Test
    void theExtentOfEveryLayerStaysTheSame() {
        String text = mesh(2000, 30, 30, false);
        MeshCap.Result r = MeshCap.cap(text, 600);
        assertTrue(r.changed());
        assertArrayEquals(MeshCap.bounds(text), MeshCap.bounds(r.text()), 0.0);
    }

    @Test
    void voidAndHaloQuadsAreNeverDropped() {
        String text = mesh(2000, 100, 100, false);
        String cut = MeshCap.cap(text, 500).text();
        assertEquals(100, countLayer(cut, 1));
        assertEquals(100, countLayer(cut, 4));
    }

    @Test
    void orbitLayersAreKeptWhenTheMeshHasOrbitLines() {
        String text = mesh(2000, 0, 0, true);
        String cut = MeshCap.cap(text, 500).text();
        assertEquals(40, countLayer(cut, 3), "orbit group quads stay");
        assertTrue(cut.contains("orbit 3 0.5 1.0"));
        String noOrbit = MeshCap.cap(mesh(2000, 0, 0, false), 500).text();
        assertTrue(countLayer(noOrbit, 3) < 40 || countLayer(noOrbit, 0) == 500 - 40, "veins are cappable without orbit lines");
    }

    @Test
    void headerLinesAndOrderSurvive() {
        String text = mesh(1500, 0, 0, false);
        String cut = MeshCap.cap(text, 400).text();
        assertTrue(cut.startsWith("# part blade 0 60\nplace 0 0 0 0 0\n"));
        String[] a = text.split("\n"), b = cut.split("\n");
        int j = 0;
        for (String line : b) {                                                 // the cut is a subsequence of the original
            while (j < a.length && !a[j].equals(line)) j++;
            assertTrue(j < a.length, "line out of order: " + line);
            j++;
        }
    }

    @Test
    void theCutIsDeterministic() {
        String text = mesh(2000, 10, 10, false);
        assertEquals(MeshCap.cap(text, 700).text(), MeshCap.cap(text, 700).text());
    }

    @Test
    void aCutMeshStillSurvivesPacking() throws IOException {
        String cut = MeshCap.cap(mesh(2500, 10, 10, false), 900).text();
        assertNull(MeshPack.firstDifference(cut, MeshPack.unpack(MeshPack.pack(cut))));
    }

    /** The shipped heavy meshes: the cut keeps their extents, respects the default limit and never touches a file under it. */
    @Test
    void shippedHeavyMeshesKeepTheirExtentUnderTheDefaultLimit() throws IOException {
        Path dir = MeshPackTest.RES.resolve("weapon_meshes_vu");
        if (!Files.isDirectory(dir)) return;
        int heavy = 0;
        try (var s = Files.list(dir)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".txt")).sorted().toList()) {
                String text = Files.readString(p);
                MeshCap.Result r = MeshCap.cap(text, 3000);
                if (!r.changed()) { assertSame(text, r.text(), p.toString()); continue; }
                heavy++;
                assertTrue(r.quadsAfter() <= 3000 + 24, p + " has " + r.quadsAfter());              // 24 = the protected extreme quads of up to 4 layers
                assertTrue(Arrays.equals(MeshCap.bounds(text), MeshCap.bounds(r.text())), p + " extent changed");
                assertTrue(r.areaLost() < 0.5, p + " lost " + r.areaLost());
            }
        }
        assertTrue(heavy > 0, "some shipped mesh is over the limit");
    }

    private static int countLayer(String text, int layer) {
        int n = 0;
        for (String line : text.split("\n")) {
            String[] t = line.trim().split("\\s+");
            if ((t[0].equals("qn") || t[0].equals("q")) && Integer.parseInt(t[1]) == layer) n++;
        }
        return n;
    }
}
