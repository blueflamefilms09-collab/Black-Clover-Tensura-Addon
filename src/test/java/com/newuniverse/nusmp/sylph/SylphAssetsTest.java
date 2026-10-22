package com.newuniverse.nusmp.sylph;

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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The files the Sylph needs: the models (three levels, the wings, the glow-only model) of both forms with their sheets and animations, the lang text, the previews and the doc. */
class SylphAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp");

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
    void everyModelOfBothFormsIsWellFormedAndHasItsOwnSheet() throws IOException {
        for (String form : new String[]{"small", "powered"}) {
            int fullW = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/sylph_" + form + ".png").toFile()).getWidth();
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_wings", "_fx"}) {
                String id = "sylph_" + form + suffix;
                JsonObject d = geo(id).getAsJsonObject("description");
                int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
                BufferedImage tex = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
                assertEquals(tex.getWidth(), tw, id + " declares the size of its own sheet");
                assertEquals(tex.getHeight(), th, id);
                if (suffix.equals("_lod1")) assertEquals(fullW / 2, tw, "a lower level of detail reads a smaller copy of the sheet");
                if (suffix.equals("_lod2")) assertEquals(fullW / 4, tw);
                Map<String, JsonObject> bones = bones(geo(id));
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
            for (String g : new String[]{"sylph_" + form + "_wings_glow"}) assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + g + ".png")), g);
            BufferedImage wings = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/sylph_" + form + "_wings.png").toFile());
            BufferedImage glow = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/sylph_" + form + "_wings_glow.png").toFile());
            assertEquals(wings.getWidth(), glow.getWidth(), "the glow map of the wings has the layout of the wings' sheet");
            assertEquals(wings.getHeight(), glow.getHeight());
        }
        assertTrue(Files.exists(ASSETS.resolve("textures/entity/sylph_halo.png")));
    }

    @Test
    void theLodAndOtherModelsUseBonesOfTheFullModelWithTheSamePivots() throws IOException {
        for (String form : new String[]{"small", "powered"}) {
            Map<String, JsonObject> full = bones(geo("sylph_" + form));
            for (String suffix : new String[]{"_lod1", "_lod2", "_wings", "_fx"}) {
                for (var e : bones(geo("sylph_" + form + suffix)).entrySet()) {
                    if (e.getKey().equals("root")) continue;
                    JsonObject f = full.get(e.getKey());
                    if (f == null) {                                                           // only the wing bones and the petals are the wings model's own; every other bone exists in the full body
                        assertTrue(e.getKey().startsWith("wing") || e.getKey().startsWith("petal"), form + suffix + " bone " + e.getKey() + " exists in the full model");
                        continue;
                    }
                    assertEquals(f.getAsJsonArray("pivot"), e.getValue().getAsJsonArray("pivot"), form + suffix + " pivot of " + e.getKey() + " (so the one animation file drives every model)");
                    if (!suffix.equals("_lod2") && !suffix.equals("_lod1")) assertEquals(f.has("parent") ? f.get("parent") : null, e.getValue().has("parent") ? e.getValue().get("parent") : null, form + suffix + " parent of " + e.getKey());
                }
            }
        }
    }

    @Test
    void theAnimationFilesHaveTheirClipsAndDriveRealBones() throws IOException {
        for (String form : new String[]{"small", "powered"}) {
            JsonObject anim = json(ASSETS.resolve("animations/entity/sylph_" + form + ".animation.json")).getAsJsonObject("animations");
            Set<String> names = new HashSet<>();
            for (String suffix : new String[]{"", "_wings", "_fx"}) names.addAll(bones(geo("sylph_" + form + suffix)).keySet());
            String[] clips = form.equals("small") ? SylphRules.SMALL_CLIPS : SylphRules.POWERED_CLIPS;
            for (String clip : clips) {
                assertTrue(anim.has(clip), form + " clip " + clip);
                JsonObject c = anim.getAsJsonObject(clip);
                double len = c.get("animation_length").getAsDouble();
                assertTrue(len > 0);
                assertTrue(c.get("loop").getAsBoolean(), clip + " loops (the painter picks a clip by the clock)");
                for (var be : c.getAsJsonObject("bones").entrySet()) {
                    assertTrue(names.contains(be.getKey()), form + "/" + clip + " bone " + be.getKey() + " is a bone of one of her models");
                    for (var track : be.getValue().getAsJsonObject().entrySet())
                        for (var key : track.getValue().getAsJsonObject().entrySet()) assertTrue(Double.parseDouble(key.getKey()) <= len + 1e-6, form + "/" + clip + " keyframe " + key.getKey() + " is inside the clip");
                }
            }
        }
    }

    @Test
    void everyClipShowsExactlyOneFace() throws IOException {
        for (String form : new String[]{"small", "powered"}) {
            JsonObject anim = json(ASSETS.resolve("animations/entity/sylph_" + form + ".animation.json")).getAsJsonObject("animations");
            Set<String> faces = new HashSet<>();
            for (String b : bones(geo("sylph_" + form)).keySet()) if (b.startsWith("face_")) faces.add(b);
            assertEquals(3, faces.size(), form + " has three face plates");
            for (String clip : form.equals("small") ? SylphRules.SMALL_CLIPS : SylphRules.POWERED_CLIPS) {
                JsonObject bones = anim.getAsJsonObject(clip).getAsJsonObject("bones");
                int shown = 0;
                for (String f : faces) {
                    assertTrue(bones.has(f) && bones.getAsJsonObject(f).has("scale"), clip + " sets the scale of " + f);
                    double v = bones.getAsJsonObject(f).getAsJsonObject("scale").entrySet().iterator().next().getValue().getAsJsonArray().get(0).getAsDouble();
                    if (v > 0.5) shown++;
                    else assertTrue(v > 0.0, "a hidden face is scaled to a tiny positive number, never 0 (a zero scale makes bad normals)");
                }
                assertEquals(1, shown, form + "/" + clip + " shows one face");
            }
        }
    }

    @Test
    void theHairAndWingsMoveByBoneRotationOnly() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/sylph_powered.animation.json")).getAsJsonObject("animations");
        for (String clip : SylphRules.POWERED_CLIPS)
            for (var be : anim.getAsJsonObject(clip).getAsJsonObject("bones").entrySet())
                if (be.getKey().startsWith("wing") || be.getKey().startsWith("hair") || be.getKey().startsWith("petal") || be.getKey().startsWith("sk")) assertTrue(be.getValue().getAsJsonObject().has("rotation"), clip + " " + be.getKey());
    }

    @Test
    void theSmallFormIsSmallAndThePoweredFormBig() throws IOException {
        // the drawn size in blocks: the model's height (1/16 units) times the draw scale (SylphDraw.SMALL_SCALE / POWERED_SCALE, kept in step by the source scan)
        double small = height("sylph_small") * 0.24 / 16.0, powered = height("sylph_powered") * 0.36 / 16.0;
        assertTrue(small > 0.5 && small < 0.7, "the small form stands about " + small + " blocks with her ahoge");
        assertTrue(powered > 1.4 && powered < 2.0, "the powered form with her streaming hair is " + powered + " blocks tall");
        double span = halfSpan("sylph_powered_wings") * 2 * 0.36 / 16.0;
        assertTrue(span > 2.2 && span < 3.2, "her wings spread " + span + " blocks");
    }

    private static double height(String id) throws IOException {
        double top = 0;
        for (JsonObject b : bones(geo(id)).values()) {
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                if (c.has("rotation")) continue;
                top = Math.max(top, c.getAsJsonArray("origin").get(1).getAsDouble() + c.getAsJsonArray("size").get(1).getAsDouble());
            }
        }
        return top;
    }

    private static double halfSpan(String id) throws IOException {
        double x = 0;
        for (JsonObject b : bones(geo(id)).values()) {
            double rz = b.has("rotation") ? Math.abs(b.getAsJsonArray("rotation").get(2).getAsDouble()) : 0;
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                double ox = c.getAsJsonArray("origin").get(0).getAsDouble(), sx = c.getAsJsonArray("size").get(0).getAsDouble();
                double reach = Math.max(Math.abs(ox), Math.abs(ox + sx));
                x = Math.max(x, Math.abs(b.getAsJsonArray("pivot").get(0).getAsDouble()) + (reach - Math.abs(b.getAsJsonArray("pivot").get(0).getAsDouble())) * Math.cos(Math.toRadians(rz)));
            }
        }
        return x;
    }

    @Test
    void theLangFragmentHasEveryString() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "sylph.json"));
        for (String k : new String[]{"key.nusmp.sylph", "nusmp.sylph.full", "nusmp.sylph.linked", "nusmp.sylph.unlinked"}) assertTrue(lang.has(k), k);
        for (SylphSoundSet.Cue c : SylphSoundSet.cues()) assertTrue(lang.has(c.subtitleKey()), c.subtitleKey());
        for (var e : lang.entrySet()) {
            String v = e.getValue().getAsString().toLowerCase();
            assertFalse(v.contains("hunger") || v.contains("starv") || v.contains("food"), e.getKey());
        }
    }

    @Test
    void theHeadlessPreviewScenesAndPicturesAreThere() throws IOException {
        JsonObject scenes = json(Path.of("tools", "render_preview", "scenes.d", "sylph.json"));
        for (String s : new String[]{"sylph_small", "sylph_small_face", "sylph_small_cast", "sylph_small_lod1", "sylph_small_lod2", "sylph_scale", "sylph_powered", "sylph_powered_face", "sylph_powered_cast",
                "sylph_powered_lod1", "sylph_powered_lod2", "sylph_powered_scale"})
            assertTrue(scenes.has(s), s);
        for (String f : new String[]{"sylph_vs_reference.png", "sylph_scale.png"}) assertTrue(Files.exists(Path.of("docs", "previews", "sylph", f)), f);
        assertTrue(Files.exists(Path.of("docs", "magic", "sylph.md")));
    }
}
