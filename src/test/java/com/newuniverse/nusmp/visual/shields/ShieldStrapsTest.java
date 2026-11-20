package com.newuniverse.nusmp.visual.shields;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4.72.0: the arm straps of the shields (ShieldStraps, docs/visual_upgrade/shield_fix.md "Straps"), pure geometry over the shipped data file and the real shield meshes: the rings
 * enclose the 4 x 4 px forearm with clearance, the straps stay inside the quad budget, the tabs reach the plate and the rings, they land on the arm in both hands and both states,
 * and the shield's fit box is untouched. Source scans: the option exists, the draw path allocates nothing.
 */
class ShieldStrapsTest {
    static final Path ASSETS = Path.of("src", "main", "resources", "assets", "anime_multiverse");
    static final Path JAVA = Path.of("src", "main", "java", "com", "newuniverse", "nusmp");

    static ShieldStraps.Data data() throws IOException { return ShieldStraps.parse(Files.readString(ASSETS.resolve("shield_straps.json"))); }

    /** The vertices of layers 0..2 of a mesh file, {x, y, z, u, v, nx, ny, nz}, four per quad; {@code body} gets layer 0 only. */
    static List<float[]> meshVerts(Path file, List<float[]> body) throws IOException {
        List<float[]> all = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String[] v = line.trim().split("\\s+");
            boolean q = v[0].equals("q"), qn = v[0].equals("qn");
            if (!q && !qn || Integer.parseInt(v[1]) > 2) continue;
            for (int k = 0; k < 4; k++) {
                float[] w = new float[8];
                if (q) {
                    for (int j = 0; j < 5; j++) w[j] = Float.parseFloat(v[2 + k * 5 + j]);
                    for (int j = 0; j < 3; j++) w[5 + j] = Float.parseFloat(v[22 + j]);
                } else {
                    for (int j = 0; j < 8; j++) w[j] = Float.parseFloat(v[2 + k * 8 + j]);
                }
                all.add(w);
                if (body != null && v[1].equals("0")) body.add(w);
            }
        }
        return all;
    }

    /** The fit the renderer computes for a mesh (third person), and the back of its plate on the item frame. */
    static float[] fitOf(List<float[]> verts) {
        float x0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE, z0 = Float.MAX_VALUE, z1 = -Float.MAX_VALUE;
        for (float[] v : verts) {
            x0 = Math.min(x0, v[0]); x1 = Math.max(x1, v[0]); y0 = Math.min(y0, v[1]); y1 = Math.max(y1, v[1]); z0 = Math.min(z0, v[2]); z1 = Math.max(z1, v[2]);
        }
        return ShieldFrame.fit(x0, x1, y0, y1, z0, z1, ShieldFrame.maxW(), ShieldFrame.maxH());
    }

    static float anchorOf(Path mesh) throws IOException {
        List<float[]> body = new ArrayList<>();
        List<float[]> all = meshVerts(mesh, body);
        return ShieldStraps.anchorZ(fitOf(all), ShieldStraps.plateBackZ(body.isEmpty() ? all : body));
    }

    static double[] cubeBox(ShieldStraps.Cube c) { return new double[]{c.from()[0], c.from()[1], c.from()[2], c.to()[0], c.to()[1], c.to()[2]}; }

    @Test
    void theRingsEncloseAFourByFourPixelForearmWithClearance() throws IOException {
        ShieldStraps.Data d = data();
        assertEquals(0.125, d.armHalf, 1e-9, "the arm is 4 x 4 px = 0.25 across");
        double armLo = -0.125, armHi = 0.125;
        int rings = 0;
        double innerMin = 9, outerMax = 0;
        for (ShieldStraps.Cube c : d.cubes) {
            double[] b = cubeBox(c);
            // no cube of the straps crosses the forearm's cross-section where the forearm is (the elbow to the wrist: y 0.25 .. 0.625)
            boolean overlapsY = b[1] < 0.625 && b[4] > 0.25;
            boolean overlapsArm = b[0] < armHi && b[3] > armLo && b[2] < armHi && b[5] > armLo;
            assertFalse(overlapsY && overlapsArm, "a strap cube cuts through the arm: " + java.util.Arrays.toString(b));
            if (c.part() == ShieldStraps.LEATHER && b[3] - b[0] > 0.3 && b[5] - b[2] < 0.05) rings++;               // a ring's front or back bar
        }
        assertEquals(4, rings, "two rings, a front and a back bar each");
        // the opening of each ring (left bar's inner face to the right bar's, back bar's to the front bar's) and its outside
        for (int r = 0; r < 2; r++) {
            ShieldStraps.Cube[] four = ringCubes(d, r);
            double[] front = cubeBox(four[0]), back = cubeBox(four[1]), left = cubeBox(four[2]), right = cubeBox(four[3]);
            double openX = right[0] - left[3], openZ = front[2] - back[5];
            assertEquals(0.28, openX, 0.011, "ring inner about 0.28");
            assertEquals(0.28, openZ, 0.011);
            assertTrue(openX >= 0.25 + 0.02 - 1e-9 && openZ >= 0.25 + 0.02 - 1e-9, "clearance of at least 1 px... 0.01 each side around the 0.25 arm");
            assertEquals(0.34, front[3] - front[0], 0.011, "ring outer about 0.34");
            assertEquals(0.34, front[5] - back[2], 0.011);
            assertEquals(0.07, front[4] - front[1], 0.006, "0.07 wide");
            // the four bars touch each other: the ring is closed
            assertEquals(front[2], left[5], 1e-9);
            assertEquals(back[5], left[2], 1e-9);
            assertEquals(left[3], right[0] - openX, 1e-9);
            innerMin = Math.min(innerMin, Math.min(openX, openZ));
            outerMax = Math.max(outerMax, front[3] - front[0]);
        }
        assertTrue(innerMin > 0.25 && outerMax < 0.36);
    }

    /** The four bars (front, back, left, right) of ring 0 (upper) or 1 (lower), in the order the generator writes them. */
    static ShieldStraps.Cube[] ringCubes(ShieldStraps.Data d, int ring) {
        List<ShieldStraps.Cube> bars = new ArrayList<>();
        for (ShieldStraps.Cube c : d.cubes) {
            double[] b = cubeBox(c);
            double cy = (b[1] + b[4]) / 2;
            boolean upper = cy < 0.45;
            if (c.part() == ShieldStraps.LEATHER && b[4] - b[1] < 0.075 && b[4] - b[1] > 0.065 && (ring == 0) == upper) bars.add(c);
        }
        assertEquals(4, bars.size());
        return bars.toArray(new ShieldStraps.Cube[0]);
    }

    @Test
    void theStrapsStayInsideTheQuadBudgetInEveryHandState() throws IOException {
        ShieldStraps.Data d = data();
        int worst = 0;
        for (Path mesh : ShieldFrameTest.shieldMeshes()) {
            float anchor = anchorOf(mesh);
            for (int ctx = 0; ctx < ShieldStraps.CONTEXTS; ctx++) {
                int q = ShieldStraps.quads(ShieldStraps.build(d, ctx, anchor));
                worst = Math.max(worst, q);
                assertTrue(q > 0 && q <= ShieldStraps.MAX_QUADS, mesh.getFileName() + " ctx " + ctx + ": " + q + " quads");
            }
        }
        assertTrue(worst <= ShieldStraps.MAX_QUADS);
        assertEquals(90, d.cubes.size() * 6, "the cubes alone: 15 cubes, 90 quads");
    }

    @Test
    void everyShieldGetsBothTabsInEveryHandStateAndTheyTouchThePlateAndTheRings() throws IOException {
        ShieldStraps.Data d = data();
        int full = (d.cubes.size() + 2 * 2) * 6;                                                                       // the cubes, and a tab + a rivet for each of the two attach points
        List<String> bad = new ArrayList<>();
        for (Path mesh : ShieldFrameTest.shieldMeshes()) {
            float anchor = anchorOf(mesh);
            for (int ctx = 0; ctx < 4; ctx++) {
                List<float[]>[] set = ShieldStraps.build(d, ctx, anchor);
                int q = ShieldStraps.quads(set);
                if (anchor >= 0f ? q != full : q < full - 12) bad.add(mesh.getFileName() + " ctx " + ctx + ": " + q + " quads, wanted " + full + " (a tab was dropped: the ring already reaches the plate's back; only the balloon shield, whose plate back is behind the frame's origin, may lose one), anchor " + anchor + ")");
                // the last tab (leather) and the last rivet (trim) end on the plate's back, the rivet a little inside it
                List<float[]> lea = set[ShieldStraps.LEATHER], tri = set[ShieldStraps.TRIM];
                double tabMax = -9, rivMax = -9;
                for (int i = lea.size() - 24; i < lea.size(); i++) tabMax = Math.max(tabMax, lea.get(i)[2]);
                for (int i = tri.size() - 24; i < tri.size(); i++) rivMax = Math.max(rivMax, tri.get(i)[2]);
                if (q > 90 && Math.abs(tabMax - anchor) > 1e-4) bad.add(mesh.getFileName() + " ctx " + ctx + ": the tab ends at " + tabMax + ", plate back " + anchor);
                if (q > 90 && Math.abs(rivMax - (anchor + d.overlap)) > 1e-4) bad.add(mesh.getFileName() + " ctx " + ctx + ": the rivet ends at " + rivMax + ", plate back " + anchor);
            }
        }
        assertEquals(List.of(), bad);
    }

    /** Known values from the independent Python chain of tools/item_preview/preview_shields.py (arm_chain, display, inverse): a point s px down the arm, on the shield frame. */
    @Test
    void theArmLandsWhereTheGamesOwnChainPutsIt() {
        // {left, raised, s px, x, y, z}
        double[][] want = {
                {0, 0, 5.5, -0.031, 0.0, -0.187}, {0, 0, 9, 0.188, 0.0, -0.188}, {0, 1, 5.5, 0.090, 0.213, -0.309}, {0, 1, 9, 0.199, 0.058, -0.199},
                {1, 0, 5.5, 0.031, 0.0, -0.187}, {1, 0, 9, -0.188, 0.0, -0.187}, {1, 1, 5.5, -0.001, 0.257, -0.282}, {1, 1, 9, -0.110, 0.102, -0.173}};
        for (double[] w : want) {
            double[] f = ShieldStraps.armFrame(w[0] != 0, w[1] != 0);
            double y = w[2] / 16;
            double x = f[1] * y + f[3], yy = f[5] * y + f[7], z = f[9] * y + f[11];
            assertEquals(w[3], x, 0.002, "x " + java.util.Arrays.toString(w));
            assertEquals(w[4], yy, 0.002, "y " + java.util.Arrays.toString(w));
            assertEquals(w[5], z, 0.002, "z " + java.util.Arrays.toString(w));
        }
    }

    @Test
    void theRingsSitOnTheArmAxisInBothHandsAndBothStates() throws IOException {
        ShieldStraps.Data d = data();
        for (int ctx = 0; ctx < 4; ctx++) {
            List<float[]>[] set = ShieldStraps.build(d, ctx, 0.2f);
            double[] f = ShieldStraps.armFrame(ctx >= 2, (ctx & 1) == 1);
            for (int ring = 0; ring < 2; ring++) {
                double[] c = new double[3];
                for (int i = 0; i < 96; i++) for (int k = 0; k < 3; k++) c[k] += set[ShieldStraps.LEATHER].get(ring * 96 + i)[k] / 96.0;      // the four bars of a ring come first, 4 x 6 quads x 4 vertices
                double y = (ring == 0 ? 0.3125 : 0.55);
                assertEquals(f[1] * y + f[3], c[0], 0.01, "ctx " + ctx + " ring " + ring + " x");
                assertEquals(f[5] * y + f[7], c[1], 0.01, "ctx " + ctx + " ring " + ring + " y");
                assertEquals(f[9] * y + f[11], c[2], 0.01, "ctx " + ctx + " ring " + ring + " z");
            }
        }
    }

    @Test
    void firstPersonOnlyBuildsAGripBarLowOnThePlatesBack() throws IOException {
        ShieldStraps.Data d = data();
        List<float[]>[] set = ShieldStraps.build(d, ShieldStraps.CTX_FIRST_PERSON, 0.2f);
        assertEquals(18, ShieldStraps.quads(set));
        for (List<float[]> l : set) for (float[] v : l) {
            assertTrue(v[1] < 0f, "below the middle of the plate");
            assertTrue(v[2] <= 0.2f + 0.03f, "behind or in the plate, never in front of the face");
        }
    }

    @Test
    void everyShieldMeshHasItsOwnTrimColourAndAFinitePlateBack() throws IOException {
        for (Path mesh : ShieldFrameTest.shieldMeshes()) {
            String id = mesh.getFileName().toString().replace(".txt", "");
            assertNotEquals(ShieldStraps.DEFAULT_TRIM_RGB, ShieldStraps.trimRgb(id), id + " has no trim colour of its own");
            List<float[]> body = new ArrayList<>();
            meshVerts(mesh, body);
            float back = ShieldStraps.plateBackZ(body);
            assertFalse(Float.isNaN(back), id + ": no back face found");
            float a = anchorOf(mesh);
            assertTrue(a > -0.3f && a < 0.5f, id + ": the plate's back lands at " + a);
        }
    }

    @Test
    void theStrapsAreNotPartOfTheFitOfAnyShield() throws IOException {
        // the straps live in their own file and are drawn by their own class; no mesh file carries them, and neither class touches the mesh layers or the fit
        try (var files = Files.walk(ASSETS.resolve("weapon_meshes"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".txt"))::iterator)
                if (ShieldSet.isShieldMesh(p.getFileName().toString().replace(".txt", ""))) assertFalse(Files.readString(p).contains("shield_straps"), p + " must not carry the straps");
        }
        String pure = Files.readString(JAVA.resolve("visual/shields/ShieldStraps.java")), client = Files.readString(JAVA.resolve("client/ShieldStrapRenderer.java"));
        assertFalse(pure.contains("ShieldFrame.fit("), "the straps never compute a fit");
        assertFalse(client.contains("layers[0].add") || client.contains(".layers[WeaponRenderer.BODY].add") || client.contains("shieldFit"), "the renderer never adds to the mesh");
        assertTrue(Files.readString(JAVA.resolve("client/WeaponRenderer.java")).contains("ShieldStrapRenderer.draw("), "the renderer draws the straps as their own pass");
    }

    @Test
    void theOptionExistsDefaultsOnAndTheDrawPathAllocatesNothing() throws IOException {
        assertTrue(ShieldStraps.enabled(), "default on");
        String cfg = Files.readString(JAVA.resolve("vfx/client/FlatPropsConfig.java"));
        assertTrue(cfg.contains(".define(\"shieldStraps\", true)") && cfg.contains("ShieldStraps.setEnabled("), "a CLIENT option visual.shieldStraps, default on, mirrored into the pure switch");
        assertTrue(cfg.indexOf("b.push(\"visual\")") < cfg.indexOf("\"shieldStraps\""), "under the visual section");
        String client = Files.readString(JAVA.resolve("client/ShieldStrapRenderer.java"));
        assertTrue(client.contains("if (!ShieldStraps.enabled()) return;"), "off = nothing is drawn");
        int from = client.indexOf("static void draw("), to = client.indexOf('}', client.lastIndexOf("emitRange("));
        String draw = client.substring(from, to);
        assertFalse(draw.contains("new ") || draw.contains(".stream(") || draw.contains("List.of") || draw.contains("String.format"), "the draw path allocates nothing per frame");
        assertTrue(draw.contains("CULL_DIST_SQ"), "distance culling");
        assertTrue(Files.readString(JAVA.resolve("item/ShieldBoot.java")).contains("SelfTests.register(\"visual.shields.straps\""), "the server smoke test covers the straps (prefix visual.shields.straps)");
        assertFalse(Files.readString(JAVA.resolve("visual/shields/ShieldStraps.java")).contains("net.minecraft"), "the pure class has no Minecraft types");
    }
}
