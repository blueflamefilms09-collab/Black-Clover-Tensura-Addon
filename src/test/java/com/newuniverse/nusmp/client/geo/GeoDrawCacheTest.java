package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the geo draw is the draw routine of every animated model of the mod, and the performance pass made it allocation free: the corners and texture rectangles of a cube are worked out once
 * ({@code Cube#prepareDraw}) and animation tracks are sampled into a scratch array ({@code Track#sampleInto}). "Same pictures" is the promise, so this compares them, bit for bit, with the
 * formulas the drawing used to run on every frame (copied here), over EVERY model and animation file of the mod.
 */
class GeoDrawCacheTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    /** The old per-frame arithmetic of GeoDraw.cube: eight corners as float[3], index = x + 2 y + 4 z. */
    private static float[][] oldCorners(GeoModelData.Bone b, GeoModelData.Cube c) {
        float g = c.inflate;
        float minX = c.ox - b.px - g, maxX = c.ox - b.px + c.sx + g;
        float minY = -(c.oy + c.sy) + b.py - g, maxY = -c.oy + b.py + g;
        float minZ = c.oz - b.pz - g, maxZ = c.oz - b.pz + c.sz + g;
        if (c.mirror) { float tmp = maxX; maxX = minX; minX = tmp; }
        float[][] v = new float[8][];
        for (int i = 0; i < 8; i++) v[i] = new float[]{(i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ};
        return v;
    }

    private static void same(float a, float b, String what) {
        assertEquals(Float.floatToRawIntBits(a), Float.floatToRawIntBits(b), what + ": " + a + " vs " + b);
    }

    @Test
    void everyCubeOfEveryModelIsPreparedLikeTheFormulaItReplaces() throws Exception {
        File[] files = new File(ASSETS, "geo/entity").listFiles((d, n) -> n.endsWith(".geo.json"));
        assertNotNull(files, "no geo files found; the test runs from the repository root");
        int cubes = 0;
        for (File f : files) {
            GeoModelData m = GeoModelData.parse(f.getName(), json(f));
            for (GeoModelData.Bone b : m.bones.values()) {
                for (GeoModelData.Cube c : b.cubes) {
                    c.prepareDraw(m, b);
                    float[][] old = oldCorners(b, c);
                    for (int i = 0; i < 8; i++) {
                        for (int k = 0; k < 3; k++) same(old[i][k], c.drawCorners[i * 3 + k], f.getName() + " " + b.name + " corner " + i + "." + k);
                    }
                    int mask = 0;
                    for (int face = 0; face < 6; face++) {
                        float[] uv = c.uv[face];
                        if (uv == null) continue;
                        mask |= 1 << face;
                        same(uv[0] / m.texW, c.drawUv[face * 4], f.getName() + " u1");
                        same(uv[1] / m.texH, c.drawUv[face * 4 + 1], f.getName() + " v1");
                        same(uv[2] / m.texW, c.drawUv[face * 4 + 2], f.getName() + " u2");
                        same(uv[3] / m.texH, c.drawUv[face * 4 + 3], f.getName() + " v2");
                    }
                    assertEquals(mask, c.drawMask, f.getName() + " " + b.name + ": the faces that exist");
                    cubes++;
                }
            }
        }
        assertTrue(cubes > 1000, "the test should have seen the mod's cubes, saw " + cubes);
    }

    @Test
    void aMirroredInflatedCubeIsPreparedLikeTheFormula() {
        GeoModelData m = GeoModelData.empty("t");
        GeoModelData.Bone b = new GeoModelData.Bone();
        b.px = 3.5f; b.py = 11f; b.pz = -2f;
        GeoModelData.Cube c = new GeoModelData.Cube();
        c.ox = -1.5f; c.oy = 4f; c.oz = 0.25f; c.sx = 3f; c.sy = 5f; c.sz = 2f; c.inflate = 0.4f; c.mirror = true;
        c.uv[GeoModelData.TOP] = new float[]{2f, 3f, 5f, 7f};
        c.prepareDraw(m, b);
        float[][] old = oldCorners(b, c);
        for (int i = 0; i < 8; i++) for (int k = 0; k < 3; k++) same(old[i][k], c.drawCorners[i * 3 + k], "corner " + i);
        assertEquals(1 << GeoModelData.TOP, c.drawMask);
        same(2f / 16, c.drawUv[GeoModelData.TOP * 4], "u1");
        same(7f / 16, c.drawUv[GeoModelData.TOP * 4 + 3], "v2");
    }

    @Test
    void sampleIntoEqualsSampleOnEveryTrackOfEveryAnimation() throws Exception {
        File[] files = new File(ASSETS, "animations/entity").listFiles((d, n) -> n.endsWith(".animation.json"));
        assertNotNull(files);
        Random rnd = new Random(1999);
        float[] out = new float[3];
        int tracks = 0;
        for (File f : files) {
            GeoAnim anim = GeoAnim.parse(json(f));
            for (String name : anim.names()) {
                GeoAnim.Clip clip = anim.clip(name);
                for (Map.Entry<String, GeoAnim.BoneAnim> e : clip.bones.entrySet()) {
                    for (GeoAnim.Track t : new GeoAnim.Track[]{e.getValue().rotation, e.getValue().position, e.getValue().scale}) {
                        if (t == null) continue;
                        tracks++;
                        float len = Math.max(clip.length, 1f);
                        float[] times = {-1f, 0f, 0.0001f, len * 0.25f, len * 0.5f, len * 0.999f, len, len + 3f, rnd.nextFloat() * len, rnd.nextFloat() * len};
                        for (float time : times) {
                            float[] a = t.sample(time);
                            boolean any = t.sampleInto(time, out);
                            if (a == null) {
                                assertTrue(!any);
                                continue;
                            }
                            assertTrue(any);
                            for (int k = 0; k < 3; k++) same(a[k], out[k], f.getName() + " " + name + " " + e.getKey() + " at " + time);
                        }
                    }
                }
            }
        }
        assertTrue(tracks > 1000, "saw " + tracks + " tracks");
    }

    @Test
    void anEmptyTrackWritesNothing() {
        GeoAnim a = GeoAnim.parse(JsonParser.parseString("{\"animations\":{\"x\":{\"animation_length\":1.0,\"bones\":{\"b\":{}}}}}").getAsJsonObject());
        GeoAnim.BoneAnim ba = a.clip("x").bones.get("b");
        assertNotNull(ba);
        assertTrue(ba.rotation == null && ba.position == null && ba.scale == null);
    }
}
