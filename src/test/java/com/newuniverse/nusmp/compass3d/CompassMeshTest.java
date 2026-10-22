package com.newuniverse.nusmp.compass3d;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the 3D compass is geometry in code ({@link CompassMesh}) whose texture layout is also read by tools/compass/gen_compass3d_textures.py. This pins that the two agree (tools/compass/compass3d_layout.json),
 * that every quad is sane (finite, unit normal, uv in 0..1, not degenerate), that the parts stack the way the model needs (rim, dial, rose, needle, glass from back to front), and that the quad counts
 * stay inside a budget.
 */
class CompassMeshTest {
    private static final String[] PARTS = {"rim", "face", "star", "needle", "glass", "arrow", "bar", "chevron"};

    private static JsonObject layout() throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("tools/compass/compass3d_layout.json"))).getAsJsonObject();
    }

    private static void assertArrayEquals(float[] want, float[] got, float eps, String msg) {
        assertEquals(want.length, got.length, msg + " length");
        for (int i = 0; i < want.length; i++) assertEquals(want[i], got[i], eps, msg + " [" + i + "]");
    }

    private static void assertArrayEquals(float[] want, float[] got, float eps) { assertArrayEquals(want, got, eps, "array"); }

    private static void assertArrayEquals(float[] want, float[] got) { assertArrayEquals(want, got, 0f, "array"); }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    @Test
    void theMeshAndTheTextureGeneratorShareOneLayout() throws IOException {
        JsonObject l = layout();
        assertEquals(l.get("segs").getAsInt(), CompassMesh.SEGS);
        assertEquals(l.get("faceR").getAsFloat(), CompassMesh.FACE_R, 1e-6f);
        assertEquals(l.get("faceZ").getAsFloat(), CompassMesh.FACE_Z, 1e-6f);
        JsonArray rows = l.getAsJsonArray("rimRows");
        assertEquals(rows.size(), CompassMesh.RIM_ROWS.length);
        for (int i = 0; i < rows.size(); i++) assertArrayEquals(floats(rows.get(i).getAsJsonArray()), CompassMesh.RIM_ROWS[i], 1e-6f, "rim row " + i);
        JsonObject st = l.getAsJsonObject("star");
        assertEquals(st.get("valleyR").getAsFloat(), CompassMesh.STAR_VALLEY_R, 1e-6f);
        assertArrayEquals(floats(st.getAsJsonArray("tipR")), CompassMesh.STAR_TIP_R, 1e-6f);
        assertEquals(st.get("hubZ").getAsFloat(), CompassMesh.STAR_HUB_Z, 1e-6f);
        assertEquals(st.get("valleyZ").getAsFloat(), CompassMesh.STAR_VALLEY_Z, 1e-6f);
        assertEquals(st.get("tipZ").getAsFloat(), CompassMesh.STAR_TIP_Z, 1e-6f);
        assertEquals(st.get("shadowZ").getAsFloat(), CompassMesh.STAR_SHADOW_Z, 1e-6f);
        assertEquals(st.get("shadowGrow").getAsFloat(), CompassMesh.STAR_SHADOW_GROW, 1e-6f);
        JsonObject u = l.getAsJsonObject("starTexU");
        assertArrayEquals(floats(u.getAsJsonArray("light")), CompassMesh.TEX_U_LIGHT, 1e-6f);
        assertArrayEquals(floats(u.getAsJsonArray("dark")), CompassMesh.TEX_U_DARK, 1e-6f);
        assertArrayEquals(floats(u.getAsJsonArray("outline")), CompassMesh.TEX_U_OUTLINE, 1e-6f);
        JsonObject n = l.getAsJsonObject("needle");
        assertEquals(n.get("tip").getAsFloat(), CompassMesh.NEEDLE_TIP, 1e-6f);
        assertEquals(n.get("tail").getAsFloat(), CompassMesh.NEEDLE_TAIL, 1e-6f);
        assertEquals(n.get("shoulderHalf").getAsFloat(), CompassMesh.NEEDLE_SHOULDER_HALF, 1e-6f);
        assertEquals(n.get("shoulderZ").getAsFloat(), CompassMesh.NEEDLE_SHOULDER_Z, 1e-6f);
        assertEquals(n.get("ridgeZ").getAsFloat(), CompassMesh.NEEDLE_RIDGE_Z, 1e-6f);
        assertEquals(n.get("tipZ").getAsFloat(), CompassMesh.NEEDLE_TIP_Z, 1e-6f);
        assertEquals(n.get("ringY").getAsFloat(), CompassMesh.NEEDLE_RING_Y, 1e-6f);
        assertEquals(n.get("ringOuter").getAsFloat(), CompassMesh.NEEDLE_RING_OUTER, 1e-6f);
        assertEquals(n.get("ringInner").getAsFloat(), CompassMesh.NEEDLE_RING_INNER, 1e-6f);
        assertEquals(n.get("ringZ0").getAsFloat(), CompassMesh.NEEDLE_RING_Z0, 1e-6f);
        assertEquals(n.get("ringZ1").getAsFloat(), CompassMesh.NEEDLE_RING_Z1, 1e-6f);
        assertEquals(n.get("hubR").getAsFloat(), CompassMesh.NEEDLE_HUB_R, 1e-6f);
        JsonObject g = l.getAsJsonObject("glass");
        assertEquals(g.get("edgeZ").getAsFloat(), CompassMesh.GLASS_EDGE_Z, 1e-6f);
        assertEquals(g.get("domeH").getAsFloat(), CompassMesh.GLASS_DOME_H, 1e-6f);
    }

    @Test
    void theRimRowsFormOneUnbrokenProfileWithTilingTextureRows() {
        float[][] r = CompassMesh.RIM_ROWS;
        for (int i = 1; i < r.length; i++) {
            assertEquals(r[i - 1][2], r[i][0], 1e-6f, "row " + i + " starts where the row before ended (radius)");
            assertEquals(r[i - 1][3], r[i][1], 1e-6f, "row " + i + " starts where the row before ended (z)");
            assertEquals(r[i - 1][5], r[i][4], 1e-6f, "the texture rows tile: row " + i);
        }
        assertEquals(0f, r[0][4], 1e-6f);
        assertEquals(1f, r[r.length - 1][5], 1e-6f);
        assertEquals(CompassMesh.FACE_R, r[r.length - 1][2], 1e-6f, "the rim's inner lip meets the dial");
        assertEquals(CompassMesh.FACE_Z, r[r.length - 1][3], 1e-6f);
    }

    @Test
    void everyQuadIsSane() {
        for (String part : PARTS) {
            float[] q = CompassMesh.raw(part);
            assertTrue(q.length >= CompassMesh.STRIDE, part + " is empty");
            assertEquals(0, q.length % CompassMesh.STRIDE);
            for (int o = 0; o < q.length; o += CompassMesh.STRIDE) {
                for (int k = 0; k < CompassMesh.STRIDE; k++) assertTrue(Float.isFinite(q[o + k]), part + " has a non finite value");
                float l = (float) Math.sqrt(q[o + 20] * q[o + 20] + q[o + 21] * q[o + 21] + q[o + 22] * q[o + 22]);
                assertEquals(1f, l, 1e-3f, part + " normal");
                for (int k = 0; k < 4; k++) {
                    assertTrue(q[o + k * 5 + 3] >= -1e-4f && q[o + k * 5 + 3] <= 1.0001f, part + " u " + q[o + k * 5 + 3]);
                    assertTrue(q[o + k * 5 + 4] >= -1e-4f && q[o + k * 5 + 4] <= 1.0001f, part + " v " + q[o + k * 5 + 4]);
                }
                assertTrue(area(q, o) > 1e-9f, part + " has a zero area quad at " + o);
            }
        }
    }

    private static float area(float[] q, int o) {
        // two triangles of the quad (a,b,c) and (a,c,d)
        float s = 0;
        for (int t = 0; t < 2; t++) {
            int a = o, b = o + (t == 0 ? 5 : 10), c = o + (t == 0 ? 10 : 15);
            float ux = q[b] - q[a], uy = q[b + 1] - q[a + 1], uz = q[b + 2] - q[a + 2], vx = q[c] - q[a], vy = q[c + 1] - q[a + 1], vz = q[c + 2] - q[a + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            s += 0.5f * (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        }
        return s;
    }

    private static float[] bounds(float[] q) {
        float[] b = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        for (int o = 0; o < q.length; o += CompassMesh.STRIDE) {
            for (int k = 0; k < 4; k++) {
                for (int a = 0; a < 3; a++) {
                    b[a] = Math.min(b[a], q[o + k * 5 + a]);
                    b[3 + a] = Math.max(b[3 + a], q[o + k * 5 + a]);
                }
            }
        }
        return b;
    }

    @Test
    void thePartsStackFromBackToFrontAndFitTheRim() {
        float[] body = bounds(concat(CompassMesh.raw("rim"), CompassMesh.raw("face"), CompassMesh.raw("star")));
        assertTrue(body[0] >= -1.12f && body[3] <= 1.12f && body[1] >= -1.12f && body[4] <= 1.12f, "the compass has radius 1 (plus the lugs)");
        assertTrue(body[2] >= -0.27f && body[5] <= 0.14f, "thick: from the back dome to the front lip");
        float[] needle = bounds(CompassMesh.raw("needle")), glass = bounds(CompassMesh.raw("glass")), star = bounds(CompassMesh.raw("star"));
        assertTrue(needle[2] > CompassMesh.STAR_HUB_Z, "the needle floats above the rose");
        assertTrue(glass[2] >= needle[5] - 0.001f || glass[2] > 0.05f, "the glass is in front of the dial");
        float centreGlass = Float.MIN_VALUE;
        float[] g = CompassMesh.raw("glass");
        float zc = -1;
        for (int o = 0; o < g.length; o += CompassMesh.STRIDE) for (int k = 0; k < 4; k++) if (Math.abs(g[o + k * 5]) < 1e-6f && Math.abs(g[o + k * 5 + 1]) < 1e-6f) zc = Math.max(zc, g[o + k * 5 + 2]);
        assertTrue(zc > needle[5], "the glass dome covers the needle's hub: dome top " + zc + " needle top " + needle[5]);
        assertTrue(needle[4] <= CompassMesh.FACE_R && needle[1] >= -CompassMesh.FACE_R, "the needle stays inside the dial (tip " + needle[4] + ", ring end " + needle[1] + ")");
        assertTrue(star[4] <= 0.9f && star[1] >= -0.9f, "the rose stays inside the dial (its outline underlay may run to 0.88, where the rim's inner bevel hides it)");
        float[] arrow = bounds(CompassMesh.raw("arrow"));
        assertEquals(1.0f, arrow[5], 1e-5f, "the arrow's tip is at z = 1");
        assertTrue(arrow[2] < -0.7f, "the arrow has a tail ring behind it");
        float[] bar = bounds(CompassMesh.raw("bar")), chev = bounds(CompassMesh.raw("chevron"));
        assertEquals(0f, bar[2], 1e-6f);
        assertEquals(1f, bar[5], 1e-6f);
        assertEquals(1f, chev[5], 1e-6f);
    }

    private static float[] concat(float[]... parts) {
        int n = 0;
        for (float[] p : parts) n += p.length;
        float[] out = new float[n];
        int o = 0;
        for (float[] p : parts) { System.arraycopy(p, 0, out, o, p.length); o += p.length; }
        return out;
    }

    @Test
    void theNormalsLookOutward() {
        // the outer wall of the rim (radius 1, z between -0.14 and 0.08) faces away from the axis
        float[] rim = CompassMesh.raw("rim");
        int wall = 0;
        for (int o = 0; o < rim.length; o += CompassMesh.STRIDE) {
            boolean outer = true;
            for (int k = 0; k < 4; k++) {
                float x = rim[o + k * 5], y = rim[o + k * 5 + 1], z = rim[o + k * 5 + 2];
                if (Math.abs(Math.hypot(x, y) - 1f) > 1e-3f || z < -0.141f || z > 0.081f) outer = false;
            }
            if (!outer) continue;
            wall++;
            float cx = 0, cy = 0;
            for (int k = 0; k < 4; k++) { cx += rim[o + k * 5]; cy += rim[o + k * 5 + 1]; }
            float l = (float) Math.hypot(cx, cy);
            assertTrue((rim[o + 20] * cx + rim[o + 21] * cy) / l > 0.95f, "the outer wall faces outward");
        }
        assertEquals(CompassMesh.SEGS, wall, "one wall quad per segment");
        for (String part : new String[]{"face", "glass"}) {
            float[] q = CompassMesh.raw(part);
            for (int o = 0; o < q.length; o += CompassMesh.STRIDE) assertTrue(q[o + 22] > 0.5f, part + " faces the front");
        }
    }

    @Test
    void theQuadCountsStayInsideTheBudgetAndTheMeshIsDeterministic() {
        int whole = CompassMesh.bodyQuads() + CompassMesh.needleQuads() + CompassMesh.glassQuads();
        assertTrue(whole >= 400, "a real model, not a sprite: " + whole);
        assertTrue(whole <= 1000, "one compass per caster stays cheap: " + whole);
        assertTrue(CompassMesh.arrowQuads() <= 100);
        assertArrayEquals(CompassMesh.raw("rim"), CompassMesh.raw("rim"));
        assertArrayEquals(CompassMesh.raw("star"), CompassMesh.raw("star"));
    }

    @Test
    void theSinkSeesEveryQuadOfAPartOnItsLayer() {
        int[] seen = new int[CompassMesh.LAYERS];
        CompassMesh.Sink s = (layer, d, o) -> seen[layer]++;
        CompassMesh.body(s);
        CompassMesh.needle(s);
        CompassMesh.glass(s);
        assertEquals(CompassMesh.quads(CompassMesh.raw("rim")), seen[CompassMesh.RIM]);
        assertEquals(CompassMesh.quads(CompassMesh.raw("face")), seen[CompassMesh.FACE]);
        assertEquals(CompassMesh.quads(CompassMesh.raw("star")), seen[CompassMesh.STAR]);
        assertEquals(CompassMesh.quads(CompassMesh.raw("needle")), seen[CompassMesh.NEEDLE]);
        assertEquals(CompassMesh.quads(CompassMesh.raw("glass")), seen[CompassMesh.GLASS]);
    }
}
