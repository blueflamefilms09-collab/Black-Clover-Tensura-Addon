package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.prop.RecombAnimeRules;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the anime look of Recombination Magic is four generated models (tools/recomb/gen_recomb_anime.py): the house-golem, the house, the ring of building pieces and the drill. Their files must be what
 * the painter expects: bones linked, every drawn face with its own uv inside a texture of the declared size, a main texture that is solid inside every face (the render type is alpha tested), a glow map of
 * the same size whose eyes are yellow-green and whose windows are warm, every clip the painter asks for, a cube count under the prop budget, and the older models still shipped next to them (the old look
 * stays reachable behind visual.recombAnime).
 */
class RecombAnimeAssetsTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    private static GeoModelData model(String name) throws Exception {
        return GeoModelData.parse(name, json(new File(ASSETS, "geo/entity/recombanime_" + name + ".geo.json")));
    }

    @Test
    void everyModelIsWellFormedAndUnderItsCubeCap() throws Exception {
        for (int m = 0; m < RecombAnimeRules.MODELS.length; m++) {
            String name = RecombAnimeRules.MODELS[m];
            GeoModelData d = model(name);
            assertFalse(d.roots.isEmpty(), name + ": no bones");
            assertTrue(d.cubeCount() > 5 && d.cubeCount() <= RecombAnimeRules.MAX_CUBES[m], name + ": " + d.cubeCount() + " cubes");
            for (GeoModelData.Bone b : d.bones.values()) {
                if (b.parent != null) assertTrue(d.bones.containsKey(b.parent), name + ": bone " + b.name + " has the unknown parent " + b.parent);
                for (GeoModelData.Cube c : b.cubes) {
                    int faces = 0;
                    for (float[] uv : c.uv) {
                        if (uv == null) continue;
                        faces++;
                        for (int i = 0; i < 4; i += 2) assertTrue(uv[i] >= -0.01f && uv[i] <= d.texW + 0.01f, name + ": u outside the texture");
                        for (int i = 1; i < 4; i += 2) assertTrue(uv[i] >= -0.01f && uv[i] <= d.texH + 0.01f, name + ": v outside the texture");
                    }
                    assertTrue(faces >= 1 && faces <= 6, name + ": a cube with " + faces + " faces in " + b.name);
                }
            }
        }
    }

    @Test
    void hiddenFacesAreLeftOutSoTheGolemStaysUnderTheVertexBudget() throws Exception {
        GeoModelData d = model("golem");
        int faces = 0;
        for (GeoModelData.Bone b : d.bones.values()) for (GeoModelData.Cube c : b.cubes) for (float[] uv : c.uv) if (uv != null) faces++;
        // 4 vertices a face, drawn twice (colour and glow): the preview's note for a prop is 6000 vertices in a frame
        assertTrue(faces * 4 * 2 <= 4200, "the golem draws " + faces + " faces (" + faces * 8 + " vertices with the glow pass)");
        assertTrue(faces < 6 * d.cubeCount(), "some faces are not drawn");
    }

    private static int faces(GeoModelData d) {
        int faces = 0;
        for (GeoModelData.Bone b : d.bones.values()) for (GeoModelData.Cube c : b.cubes) for (float[] uv : c.uv) if (uv != null) faces++;
        return faces;
    }

    @Test
    void theLowPolyGolemKeepsTheBonesAndClipsAtAFractionOfTheQuads() throws Exception {
        GeoModelData full = model("golem"), low = model("golem_lod");
        assertEquals(full.bones.keySet(), low.bones.keySet(), "the low-poly golem has every bone of the full one, so the same pose fits both");
        assertTrue(faces(full) <= RecombAnimeRules.FULL_QUADS_MAX, "the full golem is " + faces(full) + " quads a pass");
        assertTrue(faces(low) <= RecombAnimeRules.LOW_QUADS_MAX, "the low-poly golem is " + faces(low) + " quads a pass");
        assertTrue(faces(low) * 2.5 < faces(full), "the low-poly golem is at most 40 percent of the full one's quads (" + faces(low) + " of " + faces(full) + ")");
        GeoAnim a = GeoAnim.parse(json(new File(ASSETS, "animations/entity/recombanime_golem.animation.json"))), b = GeoAnim.parse(json(new File(ASSETS, "animations/entity/recombanime_golem_lod.animation.json")));
        assertEquals(a.names(), b.names());
        // the two share the model's outline: the same unturned extremes within a few pixels
        assertEquals(0f, minY(low), 0.01f);
        assertEquals(maxY(full), maxY(low), 30f, "the same height class");
    }

    private static float minY(GeoModelData d) {
        float m = Float.MAX_VALUE;
        for (GeoModelData.Bone b : d.bones.values()) for (GeoModelData.Cube c : b.cubes) if (!c.hasRot) m = Math.min(m, c.oy);
        return m;
    }

    private static float maxY(GeoModelData d) {
        float m = -Float.MAX_VALUE;
        for (GeoModelData.Bone b : d.bones.values()) for (GeoModelData.Cube c : b.cubes) if (!c.hasRot) m = Math.max(m, c.oy + c.sy);
        return m;
    }

    @Test
    void theSmallModelsStayCheap() throws Exception {
        assertTrue(faces(model("house")) <= 100, "a house is " + faces(model("house")) + " quads a pass");
        assertTrue(faces(model("ring")) <= 60);
        assertTrue(faces(model("drill")) <= 120);
    }

    @Test
    void texturesAndGlowMapsMatchTheModels() throws Exception {
        for (String name : RecombAnimeRules.MODELS) {
            GeoModelData d = model(name);
            File tex = new File(ASSETS, "textures/entity/recombanime_" + name + ".png"), glow = new File(ASSETS, "textures/entity/recombanime_" + name + "_glow.png");
            assertTrue(tex.isFile() && glow.isFile(), name + ": texture or glow map missing");
            BufferedImage t = ImageIO.read(tex), g = ImageIO.read(glow);
            assertEquals(d.texW, t.getWidth(), name + " width");
            assertEquals(d.texH, t.getHeight(), name + " height");
            assertEquals(d.texW, g.getWidth(), name + " glow width");
            assertEquals(d.texH, g.getHeight(), name + " glow height");
            assertTrue(t.getWidth() <= 2048 && t.getHeight() <= 2048, name + ": the texture is larger than 2048");
            // every face rectangle is solid in the main texture (the model is drawn alpha tested)
            for (GeoModelData.Bone b : d.bones.values()) for (GeoModelData.Cube c : b.cubes) for (float[] uv : c.uv) {
                if (uv == null) continue;
                int x0 = Math.round(Math.min(uv[0], uv[2])), x1 = Math.round(Math.max(uv[0], uv[2])), y0 = Math.round(Math.min(uv[1], uv[3])), y1 = Math.round(Math.max(uv[1], uv[3]));
                assertTrue(x1 > x0 && y1 > y0, name + ": an empty face rectangle");
                int[] samples = {x0 + (x1 - x0) / 2, y0 + (y1 - y0) / 2, x0, y0, x1 - 1, y1 - 1};
                for (int i = 0; i < samples.length; i += 2) assertEquals(255, t.getRGB(samples[i], samples[i + 1]) >>> 24, name + ": a face texel is not solid");
            }
        }
    }

    private static int[] glowStats(BufferedImage g, float[] uv) {
        int x0 = Math.round(Math.min(uv[0], uv[2])), x1 = Math.round(Math.max(uv[0], uv[2])), y0 = Math.round(Math.min(uv[1], uv[3])), y1 = Math.round(Math.max(uv[1], uv[3]));
        int green = 0, warm = 0;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = g.getRGB(x, y), r = (p >> 16) & 255, gg = (p >> 8) & 255, b = p & 255;
            if (gg > 170 && r > 90 && b < 120 && gg >= r * 0.85) green++;                      // yellow-green
            if (r > 200 && gg > 110 && b < 150 && r >= gg) warm++;                             // warm window light
        }
        return new int[]{green, warm};
    }

    @Test
    void theGolemsEyesGlowYellowGreenAndItsWindowsWarm() throws Exception {
        GeoModelData d = model("golem");
        BufferedImage g = ImageIO.read(new File(ASSETS, "textures/entity/recombanime_golem_glow.png"));
        GeoModelData.Cube headBlock = null;
        for (GeoModelData.Cube c : d.bones.get("head").cubes) if (c.sx == 20f && c.sy == 16f && c.sz == 20f) headBlock = c;
        assertNotNull(headBlock, "the head block (20 x 16 x 20) is there");
        int[] eyes = glowStats(g, headBlock.uv[GeoModelData.NORTH]);
        assertTrue(eyes[0] > 80, "the visor's glow map holds the yellow-green eyes: " + eyes[0] + " texels");
        int warm = 0;
        for (GeoModelData.Cube c : d.bones.get("torso").cubes) if (c.sx == 64f && c.sy == 20f) warm += glowStats(g, c.uv[GeoModelData.NORTH])[1];
        assertTrue(warm > 500, "the chest's lit windows glow warm: " + warm + " texels");
        // and the glow map is dark where nothing glows: the corner of the sheet's free area adds nothing
        long lit = 0, total = 0;
        for (int y = 0; y < g.getHeight(); y += 3) for (int x = 0; x < g.getWidth(); x += 3) {
            int p = g.getRGB(x, y);
            if ((((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) > 90) lit++;
            total++;
        }
        assertTrue(lit * 10 < total, "only a small part of the glow map is lit (windows, eyes, lanterns): " + lit + " of " + total);
    }

    @Test
    void everyClipThePainterAsksForExistsAndMovesRealBones() throws Exception {
        for (int m = 0; m < RecombAnimeRules.MODELS.length; m++) {
            String name = RecombAnimeRules.MODELS[m];
            GeoModelData d = model(name);
            GeoAnim a = GeoAnim.parse(json(new File(ASSETS, "animations/entity/recombanime_" + name + ".animation.json")));
            for (String clip : RecombAnimeRules.CLIPS[m]) {
                GeoAnim.Clip c = a.clip(clip);
                assertNotNull(c, name + ": no clip " + clip);
                assertFalse(c.bones.isEmpty(), name + ": the clip " + clip + " moves nothing");
                for (String bone : c.bones.keySet()) assertTrue(d.bones.containsKey(bone), name + ": clip " + clip + " moves the unknown bone " + bone);
            }
            assertTrue(a.clip("idle").loop, name + ": idle loops");
        }
        GeoAnim golem = GeoAnim.parse(json(new File(ASSETS, "animations/entity/recombanime_golem.animation.json")));
        assertTrue(golem.clip("walk").loop);
        assertFalse(golem.clip("punch").loop);
        assertFalse(golem.clip("assemble").loop);
        assertFalse(golem.clip("collapse").loop);
    }

    @Test
    void theOlderModelsAreStillShippedBehindTheOption() {
        for (String n : new String[]{"bull", "room", "halo", "drill", "helm", "chest", "back"}) {
            assertTrue(new File(ASSETS, "geo/entity/recombination_" + n + ".geo.json").isFile(), "recombination_" + n + " must stay (the old look is one switch away)");
            assertTrue(new File(ASSETS, "textures/entity/recombination_" + n + ".png").isFile());
        }
    }

    @Test
    void theGolemStandsOnTheGroundAndIsTall() throws Exception {
        GeoModelData d = model("golem");
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (GeoModelData.Bone b : d.bones.values()) for (GeoModelData.Cube c : b.cubes) {
            if (c.hasRot) continue;                                                     // turned cubes (towers, roofs) are measured by their pivot below
            minY = Math.min(minY, c.oy);
            maxY = Math.max(maxY, c.oy + c.sy);
        }
        assertEquals(0f, minY, 0.01f, "the feet are at y = 0");
        assertTrue(maxY > 110f, "the head and the roofs rise above 110 px: " + maxY);
        assertTrue(RecombAnimeRules.HEIGHT_PX >= maxY, "the declared height holds every unturned cube");
    }
}
