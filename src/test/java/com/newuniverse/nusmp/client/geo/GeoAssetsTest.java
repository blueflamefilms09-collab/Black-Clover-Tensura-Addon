package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The geo.json models and animation.json files of the attribute expansion, headless: every model parses, its bones are linked, its uvs sit
 * inside a texture that exists at the size the model declares (and so does its glow map), and every animation names real bones.
 */
class GeoAssetsTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final File GEO = new File(ASSETS, "geo/entity"), ANIM = new File(ASSETS, "animations/entity"), TEX = new File(ASSETS, "textures/entity");

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    private static String stem(File f) { return f.getName().replace(".geo.json", "").replace(".animation.json", ""); }

    @Test
    void boxUvIsVanillasLayout() {
        JsonObject o = JsonParser.parseString("{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry.t\",\"texture_width\":64,\"texture_height\":32},"
                + "\"bones\":[{\"name\":\"head\",\"pivot\":[0,24,0],\"cubes\":[{\"origin\":[-4,24,-4],\"size\":[8,8,8],\"uv\":[0,0]}]}]}]}").getAsJsonObject();
        GeoModelData m = GeoModelData.parse("t", o);
        assertEquals(64, m.texW);
        assertEquals(32, m.texH);
        GeoModelData.Cube c = m.bones.get("head").cubes.get(0);
        assertArrayEquals(new float[]{0, 8, 8, 16}, c.uv[GeoModelData.WEST]);
        assertArrayEquals(new float[]{8, 8, 16, 16}, c.uv[GeoModelData.NORTH]);
        assertArrayEquals(new float[]{16, 8, 24, 16}, c.uv[GeoModelData.EAST]);
        assertArrayEquals(new float[]{24, 8, 32, 16}, c.uv[GeoModelData.SOUTH]);
        assertArrayEquals(new float[]{8, 0, 16, 8}, c.uv[GeoModelData.TOP]);
        assertArrayEquals(new float[]{16, 8, 24, 0}, c.uv[GeoModelData.BOTTOM]);
    }

    @Test
    void animationsInterpolateAndLoop() {
        JsonObject o = JsonParser.parseString("{\"format_version\":\"1.8.0\",\"animations\":{\"idle\":{\"loop\":true,\"animation_length\":2.0,\"bones\":{\"a\":"
                + "{\"rotation\":{\"0.0\":[0,0,0],\"1.0\":[10,20,30],\"2.0\":[0,0,0]},\"scale\":[2,2,2]}}},\"once\":{\"loop\":\"hold_on_last_frame\",\"animation_length\":1.0}}}").getAsJsonObject();
        GeoAnim a = GeoAnim.parse(o);
        GeoAnim.Clip idle = a.clip("idle");
        assertNotNull(idle);
        assertTrue(idle.loop);
        assertFalse(a.clip("once").loop);
        float[] r = idle.bones.get("a").rotation.sample(0.5f);
        assertArrayEquals(new float[]{5, 10, 15}, r, 1e-4f);
        assertArrayEquals(new float[]{2, 2, 2}, idle.bones.get("a").scale.sample(1.7f), 1e-4f);
        assertEquals(0.5f, idle.time(2.5f), 1e-4f);
        assertEquals(1.0f, a.clip("once").time(5f), 1e-4f);
    }

    @Test
    void everyModelIsWellFormed() throws Exception {
        File[] files = GEO.listFiles((d, n) -> n.endsWith(".geo.json"));
        if (files == null) return;
        for (File f : files) {
            GeoModelData m = GeoModelData.parse(f.getName(), json(f));
            assertFalse(m.roots.isEmpty(), f.getName() + ": no bones");
            for (GeoModelData.Bone b : m.bones.values()) {
                if (b.parent != null) assertTrue(m.bones.containsKey(b.parent), f.getName() + ": bone " + b.name + " has the unknown parent " + b.parent);
                for (GeoModelData.Cube c : b.cubes) {
                    assertTrue(c.sx >= 0 && c.sy >= 0 && c.sz >= 0, f.getName() + ": negative size in " + b.name);
                    for (float[] uv : c.uv) {
                        if (uv == null) continue;
                        for (int i = 0; i < 4; i += 2) assertTrue(uv[i] >= -0.01f && uv[i] <= m.texW + 0.01f, f.getName() + ": u outside the texture in " + b.name + " (" + uv[i] + " of " + m.texW + ")");
                        for (int i = 1; i < 4; i += 2) assertTrue(uv[i] >= -0.01f && uv[i] <= m.texH + 0.01f, f.getName() + ": v outside the texture in " + b.name + " (" + uv[i] + " of " + m.texH + ")");
                    }
                }
            }
            File tex = new File(TEX, stem(f) + ".png");
            assertTrue(tex.isFile(), f.getName() + ": missing texture " + tex.getName());
            BufferedImage img = ImageIO.read(tex);
            assertEquals(m.texW, img.getWidth(), f.getName() + ": texture width differs from texture_width");
            assertEquals(m.texH, img.getHeight(), f.getName() + ": texture height differs from texture_height");
            File glow = new File(TEX, stem(f) + "_glow.png");
            if (glow.isFile()) {
                BufferedImage g = ImageIO.read(glow);
                assertEquals(img.getWidth(), g.getWidth(), f.getName() + ": glow map size differs");
                assertEquals(img.getHeight(), g.getHeight(), f.getName() + ": glow map size differs");
            }
        }
    }

    @Test
    void everyAnimationNamesRealBones() throws Exception {
        File[] files = ANIM.listFiles((d, n) -> n.endsWith(".animation.json"));
        if (files == null) return;
        // The Mob Foundry shares its clips between many models ("foundry_anim_*"): they have no model of the same name,
        // so each bone must exist in at least one model (the renderer walks the mob's own bones and ignores clip bones it lacks, e.g. a skirt panel).
        Set<String> foundryBones = new HashSet<>();
        File[] geos = GEO.listFiles((d, n) -> n.endsWith(".geo.json"));
        if (geos != null) for (File g : geos) foundryBones.addAll(GeoModelData.parse(g.getName(), json(g)).bones.keySet());
        for (File f : files) {
            if (f.getName().startsWith("foundry_anim_")) {
                GeoAnim shared = GeoAnim.parse(json(f));
                for (String name : shared.names())
                    for (String bone : shared.clip(name).bones.keySet())
                        assertTrue(foundryBones.contains(bone), f.getName() + ": animation " + name + " moves the bone " + bone + " that no model has");
                continue;
            }
            File geo = new File(GEO, stem(f) + ".geo.json");
            if (stem(f).equals("tinydjinn_anim")) {
                // 1.105.0: the Tiny Djinn's one shared clip file: every bone a clip moves must exist in EVERY tiny model (they share one skeleton)
                File[] tiny = GEO.listFiles((d, n) -> n.startsWith("tinydjinn_") && n.endsWith(".geo.json"));
                assertTrue(tiny != null && tiny.length > 0, f.getName() + ": no tiny djinn model");
                GeoAnim shared = GeoAnim.parse(json(f));
                for (File t : tiny) {
                    Set<String> own = GeoModelData.parse(t.getName(), json(t)).bones.keySet();
                    for (String name : shared.names())
                        for (String bone : shared.clip(name).bones.keySet())
                            assertTrue(own.contains(bone), f.getName() + ": animation " + name + " moves the bone " + bone + " that " + t.getName() + " lacks");
                }
                continue;
            }
            if (!geo.isFile() && stem(f).startsWith("abyssal_")) {
                // The abyssal boss looks share one clip file between their three phase models ("abyssal_<look>_p1..p3"): each animated bone must exist in at least one of them.
                Set<String> phaseBones = new HashSet<>();
                for (int p = 1; p <= 3; p++) {
                    File ph = new File(GEO, stem(f) + "_p" + p + ".geo.json");
                    if (ph.isFile()) phaseBones.addAll(GeoModelData.parse(ph.getName(), json(ph)).bones.keySet());
                }
                assertFalse(phaseBones.isEmpty(), f.getName() + ": no phase model " + stem(f) + "_p1.geo.json");
                GeoAnim shared = GeoAnim.parse(json(f));
                for (String name : shared.names())
                    for (String bone : shared.clip(name).bones.keySet())
                        assertTrue(phaseBones.contains(bone), f.getName() + ": animation " + name + " moves the unknown bone " + bone);
                continue;
            }
            assertTrue(geo.isFile(), f.getName() + ": no model " + geo.getName());
            Set<String> bones = new HashSet<>(GeoModelData.parse(geo.getName(), json(geo)).bones.keySet());
            GeoAnim a = GeoAnim.parse(json(f));
            for (String name : a.names()) {
                for (String bone : a.clip(name).bones.keySet()) assertTrue(bones.contains(bone), f.getName() + ": animation " + name + " moves the unknown bone " + bone);
            }
        }
    }
}
