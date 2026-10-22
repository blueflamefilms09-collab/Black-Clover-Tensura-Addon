package com.newuniverse.nusmp.spiritdark;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.spiritdark.SpiritDarkSoundRules.Cue;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The files the Spirit of Darkness needs: models with their sheets (three levels of detail and a glow-only model per form, the face models with seven mood sheets), the clips, the sounds, the lang text, the previews. */
class SpiritDarkAssetsTest {
    private static final Path RES = Path.of("src", "main", "resources"), ASSETS = RES.resolve("assets/nusmp");
    private static final String[] FORMS = {"companion", "powered"};
    private static final String[] MOODS = {"smug", "grin", "angry", "bored", "surprised", "sinister", "amused"};

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static JsonObject geo(String id) throws IOException {
        return json(ASSETS.resolve("geo/entity/" + id + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static Map<String, JsonObject> bones(JsonObject geo) {
        Map<String, JsonObject> out = new HashMap<>();
        for (JsonElement b : geo.getAsJsonArray("bones")) out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject());
        return out;
    }

    @Test
    void everyModelOfBothFormsIsWellFormedAndHasItsOwnSheetOfTheRightSize() throws IOException {
        for (String form : FORMS) {
            int fullW = ImageIO.read(ASSETS.resolve("textures/entity/spirit_dark_" + form + ".png").toFile()).getWidth();
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_fx"}) {
                String id = "spirit_dark_" + form + suffix;
                JsonObject d = geo(id).getAsJsonObject("description");
                int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
                BufferedImage tex = ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
                assertEquals(tex.getWidth(), tw, id + " declares the size of its own sheet");
                assertEquals(tex.getHeight(), th, id);
                int want = suffix.equals("") ? fullW : suffix.equals("_lod1") ? fullW / 2 : suffix.equals("_lod2") ? fullW / 4 : fullW / 2;
                assertEquals(want, tw, id + ": a lower level of detail reads a smaller copy of the sheet; the glow-only model the half size glow sheet");
                assertFalse(Files.exists(ASSETS.resolve("textures/entity/" + id + "_glow.png")), id + " has no separate glow map (the glow-only model has its own sheet)");
                Map<String, JsonObject> b = bones(geo(id));
                for (JsonObject bone : b.values()) {
                    if (bone.has("parent")) assertTrue(b.containsKey(bone.get("parent").getAsString()), id + " bone " + bone.get("name") + " has a parent");
                    for (JsonElement ce : bone.getAsJsonArray("cubes")) {
                        for (var face : ce.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                            JsonObject f = face.getValue().getAsJsonObject();
                            double u0 = f.getAsJsonArray("uv").get(0).getAsDouble(), v0 = f.getAsJsonArray("uv").get(1).getAsDouble();
                            double u1 = u0 + f.getAsJsonArray("uv_size").get(0).getAsDouble(), v1 = v0 + f.getAsJsonArray("uv_size").get(1).getAsDouble();
                            assertTrue(Math.min(u0, u1) >= -0.01 && Math.max(u0, u1) <= tw + 0.01 && Math.min(v0, v1) >= -0.01 && Math.max(v0, v1) <= th + 0.01, id + " uv inside the sheet");
                        }
                    }
                }
            }
        }
    }

    @Test
    void theFaceModelsShareTheBodysBonesAndEveryMoodHasAColourAndAGlowSheetOfTheSameSize() throws IOException {
        for (String form : FORMS) {
            Map<String, JsonObject> body = bones(geo("spirit_dark_" + form));
            for (String model : new String[]{"face", "faceglow"}) {
                String id = "spirit_dark_" + form + "_" + model;
                JsonObject g = geo(id);
                int tw = g.getAsJsonObject("description").get("texture_width").getAsInt(), th = g.getAsJsonObject("description").get("texture_height").getAsInt();
                for (var e : bones(g).entrySet()) {
                    JsonObject b = body.get(e.getKey());
                    assertNotNull(b, id + " bone " + e.getKey() + " exists in the body model, so the body's clips move the face");
                    assertEquals(b.getAsJsonArray("pivot"), e.getValue().getAsJsonArray("pivot"), id + " pivot of " + e.getKey());
                    assertEquals(b.has("parent") ? b.get("parent") : null, e.getValue().has("parent") ? e.getValue().get("parent") : null, id + " parent of " + e.getKey());
                }
                String prefix = model.equals("face") ? "spirit_dark_" + form + "_face_" : "spirit_dark_" + form + "_faceglow_";
                for (String mood : MOODS) {
                    BufferedImage img = ImageIO.read(ASSETS.resolve("textures/entity/" + prefix + mood + ".png").toFile());
                    assertEquals(tw, img.getWidth(), prefix + mood);
                    assertEquals(th, img.getHeight(), prefix + mood);
                }
            }
        }
    }

    @Test
    void theAnimationFilesHaveEveryPoseAndDriveRealBones() throws IOException {
        String[] powered = {"idle", "flap", "bow", "smug", "cast", "feast", "recoil", "cackle", "battle", "vanish", "appear", "farewell"};
        String[] companion = {"idle", "bow", "smug", "cast", "feast", "recoil", "cackle", "battle", "vanish", "appear", "farewell"};
        for (String form : FORMS) {
            JsonObject anim = json(ASSETS.resolve("animations/entity/spirit_dark_" + form + ".animation.json")).getAsJsonObject("animations");
            Map<String, JsonObject> bones = bones(geo("spirit_dark_" + form));
            for (String clip : form.equals("powered") ? powered : companion) {
                assertTrue(anim.has(clip), form + " clip " + clip);
                JsonObject c = anim.getAsJsonObject(clip);
                double len = c.get("animation_length").getAsDouble();
                assertTrue(len > 0);
                for (var be : c.getAsJsonObject("bones").entrySet()) {
                    assertTrue(bones.containsKey(be.getKey()), form + "/" + clip + " bone " + be.getKey());
                    for (var track : be.getValue().getAsJsonObject().entrySet())
                        for (var key : track.getValue().getAsJsonObject().entrySet()) assertTrue(Double.parseDouble(key.getKey()) <= len + 1e-6, form + "/" + clip + " keyframe " + key.getKey() + " is inside the clip");
                }
            }
            for (DarkMoodRules.Pose p : DarkMoodRules.Pose.values()) assertTrue(anim.has(p.clip), "the pose " + p + " has its clip in " + form);
            for (String loop : new String[]{"idle", "smug", "cackle", "battle"}) assertTrue(anim.getAsJsonObject(loop).get("loop").getAsBoolean(), loop + " loops");
            for (String once : new String[]{"bow", "cast", "feast", "recoil", "vanish", "appear", "farewell"}) assertFalse(anim.getAsJsonObject(once).get("loop").getAsBoolean(), once + " plays once");
            assertTrue(anim.getAsJsonObject("appear").get("animation_length").getAsDouble() * 20 <= DarkMoodRules.Pose.APPEAR.ticks + 1, "the appear clip fits the ticks the server gives it");
        }
    }

    @Test
    void wingsTailAndCoatAreMovedByBoneRotationOnly() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/spirit_dark_powered.animation.json")).getAsJsonObject("animations");
        for (var clip : anim.entrySet())
            for (var be : clip.getValue().getAsJsonObject().getAsJsonObject("bones").entrySet())
                if (be.getKey().startsWith("wing") || be.getKey().startsWith("tail") || be.getKey().startsWith("coat")) assertTrue(be.getValue().getAsJsonObject().has("rotation"), clip.getKey() + " " + be.getKey());
    }

    @Test
    void theSoundsAreRegisteredInSoundsJsonWithSubtitlesAndTheFilesAreShortMonoOgg() throws IOException {
        JsonObject sounds = json(ASSETS.resolve("sounds.json"));
        JsonObject lang = json(Path.of("tools/lang/spirit_dark.json"));
        long total = 0;
        for (Cue c : Cue.values()) {
            assertTrue(sounds.has(c.eventPath()), c.eventPath());
            JsonObject e = sounds.getAsJsonObject(c.eventPath());
            assertEquals("subtitles.nusmp." + c.eventPath(), e.get("subtitle").getAsString());
            assertTrue(lang.has("subtitles.nusmp." + c.eventPath()), "subtitle line for " + c.id);
            var arr = e.getAsJsonArray("sounds");
            assertTrue(arr.size() >= 2, c.id + " has variants");
            for (JsonElement s : arr) {
                JsonObject so = s.getAsJsonObject();
                assertEquals(c.range, so.get("attenuation_distance").getAsInt(), c.id + " carries as far as the rules say");
                Path f = ASSETS.resolve("sounds/" + so.get("name").getAsString().substring("nusmp:".length()) + ".ogg");
                assertTrue(Files.exists(f), "file " + f);
                byte[] d = Files.readAllBytes(f);
                total += d.length;
                assertEquals('O', d[0]);
                int at = indexOf(d, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'});
                assertTrue(at > 0 && (d[at + 11] & 0xff) == 1, f + " is mono");
            }
        }
        assertTrue(total < 1_500_000, "the synthesised cues stay small: " + total);
        Set<String> named = new HashSet<>();
        for (String k : sounds.keySet()) if (k.startsWith("spirit.dark.") && !k.startsWith("spirit.dark.voice.")) named.add(k);
        Set<String> want = new HashSet<>();
        for (Cue c : Cue.values()) want.add(c.eventPath());
        assertEquals(want, named, "no orphan entries");
        for (String old : new String[]{"zagred_word", "zagred_phase", "mob.goblin.ambient", "voice.system_floor_enter"}) assertTrue(sounds.has(old), "the older entry " + old + " is kept");
    }

    private static int indexOf(byte[] d, byte[] pat) {
        outer:
        for (int i = 0; i <= d.length - pat.length; i++) {
            for (int k = 0; k < pat.length; k++) if (d[i + k] != pat[k]) continue outer;
            return i;
        }
        return -1;
    }

    @Test
    void theLangFragmentHasEveryStringAndNothingAboutHunger() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "spirit_dark.json"));
        for (String k : new String[]{"key.nusmp.spirit_dark_link", "key.nusmp.spirit_dark_step", "key.nusmp.spirit_dark_power", "nusmp.spirit_dark.linked", "nusmp.spirit_dark.unlinked", "nusmp.spirit_dark.full",
                "nusmp.spirit_dark.needlink", "nusmp.spirit_dark.recharging", "nusmp.spirit_dark.steprecharging", "nusmp.spirit_dark.nostep", "nusmp.spirit_dark.poweredfull"})
            assertTrue(lang.has(k), k);
        for (var e : lang.entrySet()) {
            String v = e.getValue().getAsString().toLowerCase();
            assertFalse(v.contains("hunger") || v.contains("starv") || v.contains("food"), e.getKey());
        }
    }

    @Test
    void thePreviewScenesPicturesAndDocAreThere() throws IOException {
        JsonObject scenes = json(Path.of("tools", "render_preview", "scenes.d", "spirit_dark.json"));
        for (String s : new String[]{"sd_powered_game", "sd_companion_game", "sd_powered_battle", "sd_powered_lod", "sd_powered_lod2", "sd_companion_lod", "sd_companion_lod2", "sdp_powered_bow", "sdf_powered_grin", "sdf_companion_sinister"})
            assertTrue(scenes.has(s), s);
        for (String f : new String[]{"spirit_dark_poses.png", "spirit_dark_faces.png", "powered_demon.png", "companion_wisp.png", "lods.png"})
            assertTrue(Files.exists(Path.of("docs", "previews", "spirit_dark", f)), f);
        assertTrue(Files.exists(Path.of("docs", "magic", "spirit_dark.md")));
    }
}
