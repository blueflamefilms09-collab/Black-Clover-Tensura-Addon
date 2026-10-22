package com.newuniverse.nusmp.gnome;

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
 * The files the Gnome needs: the models (three levels of detail and a glow-only model per form, two for the glow) with their sheets, the animation files and their clips against the server's tick
 * constants, the glow sheets (no colour under a transparent pixel: the safe additive rule), the lang text, the previews and the doc.
 */
class GnomeAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final String[] FORMS = {"companion", "full"};

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
    void everyModelIsWellFormedAndReadsASheetOfTheRightSize() throws IOException {
        for (String form : FORMS) {
            int fullW = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/gnome_" + form + ".png").toFile()).getWidth();
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_fx", "_fx_lod1"}) {
                String id = "gnome_" + form + suffix;
                JsonObject g = geo(id);
                JsonObject d = g.getAsJsonObject("description");
                int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
                BufferedImage tex = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
                assertEquals(tex.getWidth(), tw, id + " declares the size of its own sheet");
                assertEquals(tex.getHeight(), th, id);
                int want = switch (suffix) { case "" -> fullW; case "_lod1" -> fullW / 2; case "_lod2" -> fullW / 4; default -> fullW / 2; };
                assertEquals(want, tw, id + ": a lower level reads a smaller copy of the sheet; the glow-only models the half size glow sheet");
                assertFalse(Files.exists(ASSETS.resolve("textures/entity/" + id + "_glow.png")), id + " has no separate glow map (the glow-only model has its own sheet)");
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
    void bothFormsArePaintedOnOneSheetSoTheCompanionsFilesAreCopiesOfTheGiants() throws IOException {
        for (String suffix : new String[]{"", "_lod1", "_lod2", "_fx", "_fx_lod1"})
            assertTrue(java.util.Arrays.equals(Files.readAllBytes(ASSETS.resolve("textures/entity/gnome_full" + suffix + ".png")), Files.readAllBytes(ASSETS.resolve("textures/entity/gnome_companion" + suffix + ".png"))), "the sheet" + suffix + " is the same for both forms");
        String draw = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/prop/GnomeDraw.java"));
        assertTrue(draw.contains("lod(COMP, \"gnome_companion\", \"gnome_full\")"), "the game loads each sheet once: the companion reads the giant's textures");
    }

    @Test
    void theLodAndGlowModelsUseTheBonesOfTheFullModelUnchanged() throws IOException {
        for (String form : FORMS) {
            Map<String, JsonObject> full = bones(geo("gnome_" + form));
            for (String suffix : new String[]{"_lod1", "_lod2", "_fx", "_fx_lod1"}) {
                Map<String, JsonObject> other = bones(geo("gnome_" + form + suffix));
                assertEquals(full.keySet(), other.keySet(), form + suffix + " has the same bones (so the one animation file drives every model)");
                for (var e : other.entrySet()) {
                    JsonObject f = full.get(e.getKey());
                    assertEquals(f.getAsJsonArray("pivot"), e.getValue().getAsJsonArray("pivot"), form + suffix + " pivot of " + e.getKey());
                    assertEquals(f.has("parent") ? f.get("parent") : null, e.getValue().has("parent") ? e.getValue().get("parent") : null, form + suffix + " parent of " + e.getKey());
                    assertEquals(f.has("rotation") ? f.get("rotation") : null, e.getValue().has("rotation") ? e.getValue().get("rotation") : null, form + suffix + " rest rotation of " + e.getKey());
                }
            }
        }
    }

    @Test
    void theAnimationFilesHaveTheClipsDriveRealBonesAndMatchTheServersTicks() throws IOException {
        for (String form : FORMS) {
            JsonObject anim = json(ASSETS.resolve("animations/entity/gnome_" + form + ".animation.json")).getAsJsonObject("animations");
            Map<String, JsonObject> bones = bones(geo("gnome_" + form));
            for (String clip : GnomeDraw0.CLIPS) {
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
            for (String loops : new String[]{"idle", "walk", "guard", "doze"}) assertTrue(anim.getAsJsonObject(loops).get("loop").getAsBoolean(), loops + " loops");
            for (String once : new String[]{"rise", "cast", "sink", "punch", "stomp", "slam", "shrug", "pound", "kneel"}) assertFalse(anim.getAsJsonObject(once).get("loop").getAsBoolean(), once + " plays once");
            assertEquals(GnomeRules.SLAM_CLIP_TICKS, Math.round(anim.getAsJsonObject("slam").get("animation_length").getAsDouble() * 20), form + ": the slam clip is the server's 22 ticks");
            assertEquals(GnomeRules.SHRUG_CLIP_TICKS, Math.round(anim.getAsJsonObject("shrug").get("animation_length").getAsDouble() * 20), form + ": the shrug clip is 22 ticks");
            assertEquals(GnomeRules.POUND_CLIP_TICKS, Math.round(anim.getAsJsonObject("pound").get("animation_length").getAsDouble() * 20), form + ": the pound clip is 36 ticks");
            assertEquals(GnomeRules.KNEEL_CLIP_TICKS, Math.round(anim.getAsJsonObject("kneel").get("animation_length").getAsDouble() * 20), form + ": the kneel clip is 40 ticks");
            assertTrue(anim.getAsJsonObject("slam").getAsJsonObject("bones").getAsJsonObject("chest").getAsJsonObject("rotation").has(String.format("%.2f", GnomeRules.SLAM_CONTACT_TICK / 20.0)), "a chest keyframe at the slam contact");
            assertEquals(GnomeRules.FULL_RISE_TICKS, Math.round(anim.getAsJsonObject("rise").get("animation_length").getAsDouble() * 20), form + ": the rise clip is the server's 20 ticks");
            assertEquals(GnomeRules.CAST_CLIP_TICKS, Math.round(anim.getAsJsonObject("cast").get("animation_length").getAsDouble() * 20), form + ": the cast clip is the server's 24 ticks");
            assertEquals(GnomeRules.FULL_SINK_TICKS, Math.round(anim.getAsJsonObject("sink").get("animation_length").getAsDouble() * 20), form + ": the sink clip is the server's 20 ticks");
            assertEquals(GnomeRules.PUNCH_CLIP_TICKS, Math.round(anim.getAsJsonObject("punch").get("animation_length").getAsDouble() * 20), form + ": the punch clip is the server's 20 ticks");
            assertEquals(GnomeRules.STOMP_CLIP_TICKS, Math.round(anim.getAsJsonObject("stomp").get("animation_length").getAsDouble() * 20), form + ": the stomp clip is 18 ticks");
            assertTrue(anim.getAsJsonObject("punch").getAsJsonObject("bones").getAsJsonObject("chest").getAsJsonObject("rotation").has(String.format("%.2f", GnomeRules.PUNCH_CONTACT_TICK / 20.0)), "a chest keyframe at the punch contact");
            assertTrue(anim.getAsJsonObject("stomp").getAsJsonObject("bones").getAsJsonObject("chest").getAsJsonObject("rotation").has(String.format("%.2f", GnomeRules.STOMP_CONTACT_TICK / 20.0)), "a chest keyframe at the stomp contact");
            JsonObject castChest = anim.getAsJsonObject("cast").getAsJsonObject("bones").getAsJsonObject("chest").getAsJsonObject("rotation");
            assertTrue(castChest.has(String.format("%.2f", GnomeRules.CAST_CONTACT_TICK / 20.0)), form + ": a chest keyframe at the contact tick " + GnomeRules.CAST_CONTACT_TICK);
            JsonObject riseChest = anim.getAsJsonObject("rise").getAsJsonObject("bones").getAsJsonObject("chest").getAsJsonObject("rotation");
            assertTrue(riseChest.has(String.format("%.2f", GnomeRules.FULL_SLAM_TICK / 20.0)), form + ": a chest keyframe at the slam tick " + GnomeRules.FULL_SLAM_TICK);
        }
    }

    @Test
    void theClipIdsOfTheRulesAreThePositionsOfTheNamesTheDrawerPlays() throws IOException {
        String draw = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/prop/GnomeDraw.java"));
        String listed = "{\"" + String.join("\", \"", GnomeDraw0.CLIPS) + "\"}";
        assertTrue(draw.contains("CLIPS = " + listed), "GnomeDraw.CLIPS is " + listed);
        int[] ids = {GnomeRules.CLIP_IDLE, GnomeRules.CLIP_WALK, GnomeRules.CLIP_RISE, GnomeRules.CLIP_CAST, GnomeRules.CLIP_SINK, GnomeRules.CLIP_PUNCH, GnomeRules.CLIP_STOMP, GnomeRules.CLIP_SLAM,
                GnomeRules.CLIP_SHRUG, GnomeRules.CLIP_GUARD, GnomeRules.CLIP_POUND, GnomeRules.CLIP_DOZE, GnomeRules.CLIP_KNEEL};
        for (int i = 0; i < ids.length; i++) assertEquals(i, ids[i], GnomeDraw0.CLIPS[i] + " has the id of its position");
        String faces = "{\"" + String.join("\", \"", GnomeDraw0.FACES) + "\"}";
        assertTrue(draw.contains("FACES = " + faces), "GnomeDraw.FACES is " + faces);
        int[] fid = {GnomeRules.FACE_CALM, GnomeRules.FACE_AMUSED, GnomeRules.FACE_ANGRY, GnomeRules.FACE_SLEEPY, GnomeRules.FACE_SURPRISED, GnomeRules.FACE_STERN};
        for (int i = 0; i < fid.length; i++) assertEquals(i, fid[i], GnomeDraw0.FACES[i] + " has the id of its position");
        assertEquals(GnomeRules.FACES, fid.length);
    }

    @Test
    void everyFacePlateIsOneQuadOnTheHeadOfItsFormAndReadsTheFaceSheets() throws IOException {
        BufferedImage col = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/gnome_faces.png").toFile());
        BufferedImage fx = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/gnome_faces_fx.png").toFile());
        assertEquals(col.getWidth(), fx.getWidth());
        assertEquals(col.getHeight(), fx.getHeight());
        for (String form : FORMS) {
            Map<String, JsonObject> full = bones(geo("gnome_" + form));
            for (String face : GnomeDraw0.FACES) {
                JsonObject g = json(ASSETS.resolve("geo/gnome/gnome_" + form + "_face_" + face + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                JsonObject d = g.getAsJsonObject("description");
                assertEquals(col.getWidth(), d.get("texture_width").getAsInt(), form + " " + face);
                assertEquals(col.getHeight(), d.get("texture_height").getAsInt(), form + " " + face);
                Map<String, JsonObject> bones = bones(g);
                int quads = 0;
                for (JsonObject b : bones.values()) {
                    JsonObject f = full.get(b.get("name").getAsString());
                    assertNotNull(f, form + " " + face + " bone " + b.get("name") + " exists on the body model");
                    assertEquals(f.getAsJsonArray("pivot"), b.getAsJsonArray("pivot"), form + " " + face + " pivot of " + b.get("name") + " (so the one animation file moves the plate with the head)");
                    assertEquals(f.has("parent") ? f.get("parent") : null, b.has("parent") ? b.get("parent") : null);
                    for (JsonElement ce : b.getAsJsonArray("cubes"))
                        for (var uvFace : ce.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                            quads++;
                            JsonObject u = uvFace.getValue().getAsJsonObject();
                            JsonArray uv = u.getAsJsonArray("uv"), sz = u.getAsJsonArray("uv_size");
                            double u0 = uv.get(0).getAsDouble(), v0 = uv.get(1).getAsDouble();
                            assertTrue(u0 >= 0 && v0 >= 0 && u0 + sz.get(0).getAsDouble() <= col.getWidth() + 0.01 && v0 + sz.get(1).getAsDouble() <= col.getHeight() + 0.01, form + " " + face + " uv inside the face sheet");
                        }
                }
                assertEquals(GnomeLod.FACE_QUADS / 2, quads, form + " " + face + ": one quad (the cost table counts the colour quad and the glow quad)");
            }
        }
        int bad = 0, lit = 0;
        for (int y = 0; y < fx.getHeight(); y++)
            for (int x = 0; x < fx.getWidth(); x++) {
                int v = fx.getRGB(x, y);
                if ((v >>> 24) == 0 && (v & 0xFFFFFF) != 0) bad++;
                if ((v >>> 24) > 0) lit++;
            }
        assertEquals(0, bad, "gnome_faces_fx: colour under alpha 0");
        assertTrue(lit > 100, "the face glow sheet has glowing pixels: " + lit);
    }

    @Test
    void theLimbsMoveByBoneRotationAndTheBodyAndTheGroundByPositionOnly() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/gnome_full.animation.json")).getAsJsonObject("animations");
        for (String clip : GnomeDraw0.CLIPS)
            for (var be : anim.getAsJsonObject(clip).getAsJsonObject("bones").entrySet())
                if (be.getKey().startsWith("arm_") || be.getKey().startsWith("fore_") || be.getKey().startsWith("leg_") || be.getKey().startsWith("shin_"))
                    assertTrue(be.getValue().getAsJsonObject().has("rotation") && (!be.getValue().getAsJsonObject().has("position") || (clip.equals("shrug") && be.getKey().startsWith("arm_"))), clip + " " + be.getKey() + " (only the shrug heaves the shoulders by a position)");
        assertTrue(anim.getAsJsonObject("rise").getAsJsonObject("bones").getAsJsonObject("root").has("position"), "it rises out of the ground by the root's position");
        assertTrue(anim.getAsJsonObject("sink").getAsJsonObject("bones").getAsJsonObject("root").has("position"), "and sinks back the same way");
    }

    @Test
    void theGlowSheetsCarryNoColourUnderATransparentPixel() throws IOException {
        for (String f : new String[]{"gnome_companion_fx", "gnome_companion_fx_lod1", "gnome_full_fx", "gnome_full_fx_lod1"}) {
            BufferedImage img = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + f + ".png").toFile());
            int bad = 0, lit = 0;
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    if ((v >>> 24) == 0 && (v & 0xFFFFFF) != 0) bad++;
                    if ((v >>> 24) > 0) lit++;
                }
            assertEquals(0, bad, f + ": colour under alpha 0");
            assertTrue(lit > 200, f + " has glowing pixels: " + lit);
        }
    }

    @Test
    void theSheetIsPaintedInTheCelLookFlatTonesAndInk() throws IOException {
        BufferedImage img = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/gnome_full.png").toFile());
        java.util.Set<Integer> colours = new java.util.HashSet<>();
        int dark = 0, n = 0;
        for (int y = 0; y < img.getHeight(); y += 2)
            for (int x = 0; x < img.getWidth(); x += 2) {
                int v = img.getRGB(x, y);
                if ((v >>> 24) < 200) continue;
                n++;
                colours.add(v & 0xFFFFFF);
                int lum = (((v >> 16) & 255) * 3 + ((v >> 8) & 255) * 6 + (v & 255)) / 10;
                if (lum < 50) dark++;
            }
        assertTrue(n > 100000);
        assertTrue(dark > n / 40, "dark thin ink linework is there: " + dark + " of " + n);
        assertTrue(colours.size() < n / 4, "flat tones, not photographic gradients: " + colours.size() + " colours in " + n + " samples");
    }

    @Test
    void theLangFragmentAndThePicturesAndTheDocAreThere() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/gnome.json"));
        for (String k : new String[]{"key.nusmp.gnome", "nusmp.gnome.linked", "nusmp.gnome.unlinked", "nusmp.gnome.need_earth", "nusmp.gnome.full"}) assertTrue(lang.has(k), k);
        for (var e : lang.entrySet()) {
            String v = e.getValue().getAsString().toLowerCase();
            assertFalse(v.contains("hunger") || v.contains("starv") || v.contains("food"), e.getKey());
        }
        JsonObject scenes = json(Path.of("tools/render_preview/scenes.d/gnome.json"));
        for (String s : new String[]{"gnome_full", "gnome_full_close", "gnome_companion", "gnome_scale", "gnome_full_rise", "gnome_full_cast", "gnome_full_sink", "gnome_companion_walk", "gnome_companion_cast"}) assertTrue(scenes.has(s), s);
        for (String f : new String[]{"gnome_views.png", "gnome_scale.png"}) assertTrue(Files.exists(Path.of("docs/previews/gnome/" + f)), f);
        assertTrue(Files.exists(Path.of("docs/magic/gnome.md")));
    }

    /** The clip names of client.prop.GnomeDraw, repeated here (a common-side test never loads a client class). */
    private static final class GnomeDraw0 {
        static final String[] CLIPS = {"idle", "walk", "rise", "cast", "sink", "punch", "stomp", "slam", "shrug", "guard", "pound", "doze", "kneel"};
        static final String[] FACES = {"calm", "amused", "angry", "sleepy", "surprised", "stern"};
    }
}
