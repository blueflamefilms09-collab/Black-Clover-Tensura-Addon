package com.newuniverse.nusmp.spiritlight;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The files the Spirit of Light needs: the models (three levels of detail and a glow-only model per form) with their sheets, the clips of every pose (and the face each one wears), the particle
 * textures, the lang fragments, the previews and the doc. Pure file and JSON checks, no Minecraft class.
 */
class SpiritLightAssetsTest {
    private static final Path RES = Path.of("src", "main", "resources"), ASSETS = RES.resolve("assets/nusmp");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static JsonObject geo(String id) throws IOException {
        return json(ASSETS.resolve("geo/entity/" + id + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static Map<String, JsonObject> bones(JsonObject geo) {
        Map<String, JsonObject> out = new HashMap<>();
        for (JsonElement b : geo.getAsJsonArray("bones")) out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject());
        return out;
    }

    private static final String[] FORMS = {"cub", "seraph"}, SUFFIXES = {"", "_lod1", "_lod2", "_fx"};

    @Test
    void everyModelIsWellFormedWithItsOwnSheetOfTheSizeItDeclares() throws IOException {
        for (String form : FORMS) {
            int fullW = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/spiritlight_" + form + ".png").toFile()).getWidth();
            for (String suffix : SUFFIXES) {
                String id = "spiritlight_" + form + suffix;
                JsonObject g = geo(id);
                JsonObject d = g.getAsJsonObject("description");
                int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
                BufferedImage tex = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
                assertEquals(tex.getWidth(), tw, id + " declares the size of its own sheet");
                assertEquals(tex.getHeight(), th, id);
                assertFalse(Files.exists(ASSETS.resolve("textures/entity/" + id + "_glow.png")), id + ": the glow is a model of its own, not a glow map");
                if (suffix.equals("_lod1") || suffix.equals("_lod2")) assertTrue(tw < fullW, id + ": a lower level of detail reads a smaller sheet");
                Map<String, JsonObject> bones = bones(g);
                for (JsonObject b : bones.values()) {
                    if (b.has("parent")) assertTrue(bones.containsKey(b.get("parent").getAsString()), id + " bone " + b.get("name") + " has a parent");
                    for (JsonElement ce : b.getAsJsonArray("cubes")) {
                        for (var face : ce.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                            JsonObject f = face.getValue().getAsJsonObject();
                            JsonArray uv = f.getAsJsonArray("uv"), sz = f.getAsJsonArray("uv_size");
                            double u0 = uv.get(0).getAsDouble(), v0 = uv.get(1).getAsDouble(), u1 = u0 + sz.get(0).getAsDouble(), v1 = v0 + sz.get(1).getAsDouble();
                            assertTrue(Math.min(u0, u1) >= -0.01 && Math.max(u0, u1) <= tw + 0.01 && Math.min(v0, v1) >= -0.01 && Math.max(v0, v1) <= th + 0.01, id + " uv inside the sheet: " + uv + " " + sz);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theLodAndGlowModelsUseTheBonesOfTheFullModelAndTheGlowModelIsPosedExactlyLikeIt() throws IOException {
        for (String form : FORMS) {
            Map<String, JsonObject> full = bones(geo("spiritlight_" + form));
            for (String suffix : new String[]{"_lod1", "_lod2", "_fx"}) {
                for (var e : bones(geo("spiritlight_" + form + suffix)).entrySet()) {
                    JsonObject f = full.get(e.getKey());
                    assertNotNull(f, form + suffix + " bone " + e.getKey() + " exists in the full model");
                    if (suffix.equals("_fx")) {                                                  // so that the one animation file drives it
                        assertEquals(f.getAsJsonArray("pivot"), e.getValue().getAsJsonArray("pivot"), form + suffix + " pivot of " + e.getKey());
                        assertEquals(f.has("parent") ? f.get("parent") : null, e.getValue().has("parent") ? e.getValue().get("parent") : null, form + suffix + " parent of " + e.getKey());
                        assertEquals(f.has("rotation") ? f.get("rotation") : null, e.getValue().has("rotation") ? e.getValue().get("rotation") : null, form + suffix + " rest rotation of " + e.getKey());
                    }
                }
            }
        }
    }

    @Test
    void everyPoseOfTheTablesHasAClipOfTheRightLengthAndLoopAndTheFaceItWears() throws IOException {
        for (String form : FORMS) {
            boolean seraph = form.equals("seraph");
            JsonObject anim = json(ASSETS.resolve("animations/entity/spiritlight_" + form + ".animation.json")).getAsJsonObject("animations");
            Map<String, JsonObject> bones = bones(geo("spiritlight_" + form));
            String[] faces = seraph ? LightMoodRules.SERAPH_FACES : LightMoodRules.CUB_FACES;
            // the base face is the first face of the table (bone face_base); the overlays are the other three (face_<name>)
            for (int p = 0; p < LightMoodRules.poseCount(seraph); p++) {
                String pose = LightMoodRules.poseName(seraph, p);
                assertTrue(anim.has(pose), form + " clip " + pose);
                JsonObject c = anim.getAsJsonObject(pose);
                double len = c.get("animation_length").getAsDouble();
                assertTrue(len > 0);
                int ticks = LightMoodRules.poseTicks(seraph, p);
                if (ticks > 0) assertEquals(ticks, Math.round(len * 20), form + "/" + pose + " lasts as long as the table says");
                boolean loop = c.get("loop").getAsBoolean();
                assertEquals(ticks == 0, loop, form + "/" + pose + " loops exactly when the table holds it");
                for (var be : c.getAsJsonObject("bones").entrySet()) {
                    assertTrue(bones.containsKey(be.getKey()), form + "/" + pose + " bone " + be.getKey());
                    for (var track : be.getValue().getAsJsonObject().entrySet())
                        for (var key : track.getValue().getAsJsonObject().entrySet()) assertTrue(Double.parseDouble(key.getKey()) <= len + 1e-6, form + "/" + pose + " keyframe " + key.getKey() + " is inside the clip");
                }
                // the face: the scale tracks show exactly the one face of the pose and hide the others
                int want = LightMoodRules.face(seraph, p);
                for (int i = 0; i < 4; i++) {
                    String fb = i == 0 ? "face_base" : "face_" + faces[i];
                    assertTrue(bones.containsKey(fb), form + " has the face bone " + fb);
                    JsonObject scale = c.getAsJsonObject("bones").getAsJsonObject(fb).getAsJsonObject("scale");
                    double v = scale.entrySet().iterator().next().getValue().getAsJsonArray().get(0).getAsDouble();
                    if (i == want) assertTrue(v > 0.9, form + "/" + pose + " shows " + faces[i]);
                    else assertTrue(v < 0.01, form + "/" + pose + " hides " + faces[i]);
                }
            }
            assertTrue(anim.getAsJsonObject("idle").get("loop").getAsBoolean());
        }
    }

    @Test
    void theWingsRingsAndTailAreMovedByBoneRotationOnlyAndTheRingsTurn() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/spiritlight_seraph.animation.json")).getAsJsonObject("animations");
        JsonObject idle = anim.getAsJsonObject("idle").getAsJsonObject("bones");
        for (String ring : new String[]{"ring1", "ring2", "ring3"}) {
            assertTrue(idle.has(ring) && idle.getAsJsonObject(ring).has("rotation"), ring + " turns in the idle");
            JsonObject keys = idle.getAsJsonObject(ring).getAsJsonObject("rotation");
            double max = 0;
            for (var k : keys.entrySet()) max = Math.max(max, Math.abs(k.getValue().getAsJsonArray().get(1).getAsDouble()));
            assertTrue(max >= 360.0, ring + " makes at least a whole turn over the clip so the loop closes");
        }
        for (String pose : new String[]{"idle", "wings_open", "victory"})
            for (var be : anim.getAsJsonObject(pose).getAsJsonObject("bones").entrySet())
                if (be.getKey().startsWith("wing")) assertTrue(be.getValue().getAsJsonObject().has("rotation"), pose + " " + be.getKey());
        JsonObject cub = json(ASSETS.resolve("animations/entity/spiritlight_cub.animation.json")).getAsJsonObject("animations");
        assertTrue(cub.getAsJsonObject("idle").getAsJsonObject("bones").has("halo"), "the cub's halo turns");
        assertTrue(cub.getAsJsonObject("idle").getAsJsonObject("bones").has("t0"), "the tail swishes");
    }

    @Test
    void theSeraphHasSixWingsOfBladesAndThreeRingsAndTheCubHasAManeAndAHalo() throws IOException {
        Map<String, JsonObject> s = bones(geo("spiritlight_seraph"));
        for (String w : new String[]{"wingA_L", "wingA_R", "wingB_L", "wingB_R", "wingC_L", "wingC_R"}) {
            assertTrue(s.containsKey(w), w);
            assertTrue(s.get(w).getAsJsonArray("cubes").size() >= 7, w + " is a fan of blades");
        }
        for (String r : new String[]{"ring1", "ring2", "ring3", "prism"}) assertTrue(s.containsKey(r), r);
        Map<String, JsonObject> c = bones(geo("spiritlight_cub"));
        for (String r : new String[]{"mane_back", "halo", "jaw", "t0", "tuft", "ear_L", "ear_R", "arm_L", "arm_R", "face_sleepy", "face_fierce", "face_surprised"}) assertTrue(c.containsKey(r), r);
        assertTrue(c.get("mane_back").getAsJsonArray("cubes").size() >= 12, "the mane is a ring of tufts");
    }

    @Test
    void theParticleTexturesOfTheBeamTheRingTheGlowAndTheSparkAreThereAndSoft() throws IOException {
        for (String f : new String[]{"beam", "ring", "glow", "spark"}) {
            BufferedImage img = javax.imageio.ImageIO.read(ASSETS.resolve("textures/particle/spiritlight_" + f + ".png").toFile());
            assertTrue(img.getWidth() >= 64 && img.getHeight() >= 64, f);
            int solidWhite = 0, total = img.getWidth() * img.getHeight();
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    int a = v >>> 24;
                    if (a == 0 && (v & 0xFFFFFF) != 0) fail(f + ": colour under a fully transparent pixel");
                    if (a >= 235 && Math.min((v >> 16) & 255, Math.min((v >> 8) & 255, v & 255)) >= 245) solidWhite++;
                }
            assertTrue(solidWhite < total * 0.05, f + ": no flat white shape (" + solidWhite + " of " + total + " texels)");
        }
    }

    @Test
    void theLangFragmentsHaveEveryStringAndTheVoiceTextIsInThere() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "spirit_light.json"));
        for (String k : new String[]{"key.nusmp.spirit_light_link", "key.nusmp.spirit_light_command", "nusmp.spiritlight.need", "nusmp.spiritlight.full", "nusmp.spiritlight.linked", "nusmp.spiritlight.unlinked",
                "nusmp.spiritlight.recharge", "nusmp.spiritlight.powered_full", "nusmp.spiritlight.risen", "nusmp.spiritlight.fallen"})
            assertTrue(lang.has(k), k);
        JsonObject voice = json(Path.of("tools", "lang", "spirit_light_voice.json"));
        for (String[] l : SpiritLightVoiceLines.ALL) assertEquals(l[2], voice.get("nusmp.spiritlight.say." + l[0]).getAsString(), l[0]);
        JsonObject snd = json(Path.of("tools", "lang", "spirit_light_sounds.json"));
        for (String id : SpiritLightRules.SOUND_IDS) assertTrue(snd.has("subtitles.nusmp." + id), "subtitle of " + id);
        for (JsonObject o : new JsonObject[]{lang, voice, snd})
            for (var e : o.entrySet()) {
                String v = e.getValue().getAsString().toLowerCase();
                assertFalse(v.contains("hunger") || v.contains("starv") || v.contains("food"), e.getKey());
            }
    }

    @Test
    void thePreviewScenesPicturesAndDocAreThere() throws IOException {
        for (String f : new String[]{"spirit_light_companion", "spirit_light_powered", "spirit_light_poses", "spirit_light_faces", "spirit_light_lods", "spirit_light_props"})
            assertTrue(Files.exists(Path.of("tools", "render_preview", "scenes.d", f + ".json")), f);
        JsonObject props = json(Path.of("tools", "render_preview", "scenes.d", "spirit_light_props.json"));
        for (String s : new String[]{"spirit_light_in_game_companion", "spirit_light_in_game_powered", "spirit_light_beam", "spirit_light_pulse"}) assertTrue(props.has(s), s);
        assertTrue(json(Path.of("tools", "render_preview", "scenes.d", "spirit_light_companion.json")).has("spirit_light_companion"));
        assertTrue(json(Path.of("tools", "render_preview", "scenes.d", "spirit_light_powered.json")).has("spirit_light_powered"));
        for (String f : new String[]{"spirit_light_companion.png", "spirit_light_powered.png", "spirit_light_poses.png", "spirit_light_faces.png", "spirit_light_in_game.png", "spirit_light_lods.png"})
            assertTrue(Files.exists(Path.of("docs", "previews", "spirit_light", f)), f);
        String doc = Files.readString(Path.of("docs", "magic", "spirit_light.md"));
        for (String must : new String[]{"NEW", "REPLACED", "KEPT", "magic.spiritLight", "nusmp-spiritlight-server.toml", "magic.spirit_light", "Not verified"}) assertTrue(doc.contains(must), "the doc says " + must);
    }
}
