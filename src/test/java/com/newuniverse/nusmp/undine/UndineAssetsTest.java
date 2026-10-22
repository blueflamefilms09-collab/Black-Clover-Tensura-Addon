package com.newuniverse.nusmp.undine;

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

/**
 * The files the Undine needs: the models (three levels of body, two of the water pass, the foam pass and the wave base) with their sheets and the one animation file; the quad counts of
 * {@link UndineLod} are the generated files' own counts; every animated bone exists; every uv is inside its sheet; the sheets are painted (not flat) and the additive ones have nothing under alpha 0;
 * the preview images and the doc are there.
 */
class UndineAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    private static JsonObject geo(String id) throws IOException {
        return json(ASSETS.resolve("geo/entity/" + id + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static int quads(JsonObject g) {
        int n = 0;
        for (JsonElement b : g.getAsJsonArray("bones")) for (JsonElement c : b.getAsJsonObject().getAsJsonArray("cubes")) n += c.getAsJsonObject().getAsJsonObject("uv").size();
        return n;
    }

    private static Set<String> boneNames(JsonObject g) {
        Set<String> out = new HashSet<>();
        for (JsonElement b : g.getAsJsonArray("bones")) out.add(b.getAsJsonObject().get("name").getAsString());
        return out;
    }

    private static final String[] MODELS = {"undine_body", "undine_body_lod1", "undine_body_lod2", "undine_water", "undine_water_lod1", "undine_fx", "undine_base", "undine_base_lod1"};

    @Test
    void theQuadTablesOfUndineLodAreTheGeneratedFilesOwnCounts() throws IOException {
        String[] body = {"undine_body", "undine_body_lod1", "undine_body_lod2"}, water = {"undine_water", "undine_water_lod1"}, base = {"undine_base", "undine_base_lod1"};
        for (int l = 0; l < 3; l++) assertEquals(UndineLod.BODY_QUADS[l], quads(geo(body[l])), body[l]);
        for (int l = 0; l < 2; l++) assertEquals(UndineLod.WATER_QUADS[l], quads(geo(water[l])), water[l]);
        assertEquals(0, UndineLod.WATER_QUADS[2], "the lowest level has no water pass");
        assertEquals(UndineLod.FX_QUADS[0], quads(geo("undine_fx")));
        assertEquals(UndineLod.FX_QUADS[1], quads(geo("undine_fx")), "the foam pass is one model used on the two nearer levels");
        assertEquals(0, UndineLod.FX_QUADS[2]);
        for (int l = 0; l < 2; l++) assertEquals(UndineLod.BASE_QUADS[l], quads(geo(base[l])), base[l]);
        assertEquals(0, UndineLod.BASE_QUADS[2]);
    }

    @Test
    void everyModelIsWellFormedHasItsOwnSheetAndKeepsItsUvsInside() throws IOException {
        for (String id : MODELS) {
            JsonObject g = geo(id);
            JsonObject d = g.getAsJsonObject("description");
            int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
            BufferedImage tex = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
            assertEquals(tex.getWidth(), tw, id + " declares the width of its own sheet");
            assertEquals(tex.getHeight(), th, id + " declares the height of its own sheet");
            assertTrue(tw <= 2048 && th <= 2048, id + ": a sheet is at most 2048 x 2048");
            Set<String> names = boneNames(g);
            for (JsonElement be : g.getAsJsonArray("bones")) {
                JsonObject b = be.getAsJsonObject();
                if (b.has("parent")) assertTrue(names.contains(b.get("parent").getAsString()), id + " bone " + b.get("name") + " has its parent");
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    JsonObject c = ce.getAsJsonObject();
                    JsonArray size = c.getAsJsonArray("size");
                    assertTrue(size.get(0).getAsDouble() >= 0 && size.get(1).getAsDouble() >= 0 && size.get(2).getAsDouble() >= 0, id + ": no negative size");
                    for (var face : c.getAsJsonObject("uv").entrySet()) {
                        JsonArray uv = face.getValue().getAsJsonObject().getAsJsonArray("uv"), sz = face.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                        double u0 = uv.get(0).getAsDouble(), v0 = uv.get(1).getAsDouble(), u1 = u0 + sz.get(0).getAsDouble(), v1 = v0 + sz.get(1).getAsDouble();
                        for (double u : new double[]{u0, u1}) assertTrue(u >= -0.01 && u <= tw + 0.01, id + ": u " + u + " outside " + tw);
                        for (double v : new double[]{v0, v1}) assertTrue(v >= -0.01 && v <= th + 0.01, id + ": v " + v + " outside " + th);
                    }
                }
            }
        }
    }

    @Test
    void theLevelsOfDetailKeepTheBodyAndStayFarUnderTheBudget() throws IOException {
        assertTrue(quads(geo("undine_body")) + quads(geo("undine_water")) + quads(geo("undine_fx")) <= UndineLod.COMPANION_CAP[0]);
        assertTrue(quads(geo("undine_body_lod1")) + quads(geo("undine_water_lod1")) + quads(geo("undine_fx")) <= UndineLod.COMPANION_CAP[1]);
        assertTrue(quads(geo("undine_body_lod2")) <= UndineLod.COMPANION_CAP[2]);
        Set<String> core = Set.of("float", "hip", "torso", "neck", "head", "arm_l", "arm_r", "fore_l", "fore_r", "hand_l", "hand_r");
        for (String id : new String[]{"undine_body", "undine_body_lod1", "undine_body_lod2", "undine_water", "undine_water_lod1", "undine_fx"})
            assertTrue(boneNames(geo(id)).containsAll(core), id + " keeps the skeleton the animation file drives");
        // a lower level of detail reads a smaller copy of the art
        int w0 = geo("undine_body").getAsJsonObject("description").get("texture_width").getAsInt();
        assertEquals(w0 / 2, geo("undine_body_lod1").getAsJsonObject("description").get("texture_width").getAsInt());
        assertEquals(w0 / 4, geo("undine_body_lod2").getAsJsonObject("description").get("texture_width").getAsInt());
    }

    @Test
    void everyPoseHasItsClipsAndTheLoopingOnesLoop() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/undine_body.animation.json")).getAsJsonObject("animations");
        for (UndinePose pose : UndinePose.values()) for (String clip : pose.clips()) assertTrue(anim.has(clip), "the pose " + pose + " needs the clip " + clip);
        for (UndinePose pose : new UndinePose[]{UndinePose.WELCOME, UndinePose.CLASP, UndinePose.WAVE, UndinePose.SORROW})
            assertTrue(anim.getAsJsonObject(pose.clip(false)).get("loop").getAsBoolean(), pose + " is a held pose that loops");
        for (UndinePose pose : new UndinePose[]{UndinePose.RISE, UndinePose.CAST, UndinePose.POWER, UndinePose.FLINCH, UndinePose.FAREWELL})
            for (String clip : pose.clips()) {
                var loop = anim.getAsJsonObject(clip).get("loop");
                assertFalse(loop.isJsonPrimitive() && loop.getAsJsonPrimitive().isBoolean() && loop.getAsBoolean(), clip + " plays once");
                double length = anim.getAsJsonObject(clip).get("animation_length").getAsDouble();
                assertTrue(length * 20.0 <= pose.ticks() + 2 && length * 20.0 >= pose.ticks() * 0.4, clip + " is about as long as the server holds its pose (" + length + " s, " + pose.ticks() + " ticks)");
            }
        assertEquals(10, UndinePose.values().length, "none, the entrance and the eight things she does");
    }

    @Test
    void everyFaceHasAnOverlayCardWithItsOwnTextureAndTheCalmFaceIsTheBodys() throws IOException {
        assertEquals(7, UndineFace.values().length);
        assertEquals(0, UndineFace.CALM.id());
        assertFalse(UndineFace.CALM.hasOverlay());
        for (UndineFace f : UndineFace.values()) {
            if (!f.hasOverlay()) continue;
            BufferedImage sheet = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/undine_face_" + f.key() + ".png").toFile());
            assertEquals(320, sheet.getWidth());
            assertEquals(392, sheet.getHeight());
            JsonObject g = geo("undine_face_" + f.key());
            assertEquals(UndineLod.FACE_QUADS[0], quads(g), f + " is one card");
            JsonObject d = g.getAsJsonObject("description");
            assertEquals(sheet.getWidth(), d.get("texture_width").getAsInt());
            assertEquals(sheet.getHeight(), d.get("texture_height").getAsInt());
            JsonObject face = null;
            for (JsonElement be : g.getAsJsonArray("bones")) for (JsonElement ce : be.getAsJsonObject().getAsJsonArray("cubes")) face = ce.getAsJsonObject().getAsJsonObject("uv").entrySet().iterator().next().getValue().getAsJsonObject();
            assertNotNull(face, f + " has its card");
            int u = (int) Math.round(face.getAsJsonArray("uv").get(0).getAsDouble()), v = (int) Math.round(face.getAsJsonArray("uv").get(1).getAsDouble()), w = (int) Math.round(face.getAsJsonArray("uv_size").get(0).getAsDouble()), h = (int) Math.round(face.getAsJsonArray("uv_size").get(1).getAsDouble());
            int x0 = Math.min(u, u + w), y0 = v, opaque = 0;
            for (int y = y0; y < y0 + h; y += 2) for (int x = x0; x < x0 + Math.abs(w); x += 2) if ((sheet.getRGB(x, y) >>> 24) > 200) opaque++;
            assertTrue(opaque > 500, f + " has its expression painted in its texture (" + opaque + " opaque samples)");
            assertEquals(0, sheet.getRGB(0, 0) >>> 24, f + ": the edge of an expression is transparent, so it blends into the calm face");
            for (int y = 0; y < sheet.getHeight(); y++) for (int x = 0; x < sheet.getWidth(); x++) {
                int a = sheet.getRGB(x, y);
                if ((a >>> 24) == 0) assertEquals(0, a & 0xFFFFFF, f + ": no colour under alpha 0 at " + x + "," + y);
            }
        }
    }

    @Test
    void theAnimationFileHasTheFourClipsAndEveryAnimatedBoneExistsInSomeModel() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/undine_body.animation.json")).getAsJsonObject("animations");
        for (String clip : new String[]{"idle", "full_idle", "cast", "open"}) assertTrue(anim.has(clip), "clip " + clip);
        assertTrue(anim.getAsJsonObject("idle").get("loop").getAsBoolean() && anim.getAsJsonObject("full_idle").get("loop").getAsBoolean(), "the idles loop");
        assertFalse(anim.getAsJsonObject("cast").get("loop").isJsonPrimitive() && anim.getAsJsonObject("cast").get("loop").getAsJsonPrimitive().isBoolean() && anim.getAsJsonObject("cast").get("loop").getAsBoolean(), "the cast pose plays once and holds");
        Set<String> all = new HashSet<>();
        for (String id : MODELS) all.addAll(boneNames(geo(id)));
        for (var c : anim.entrySet()) {
            JsonObject bones = c.getValue().getAsJsonObject().getAsJsonObject("bones");
            for (String b : bones.keySet()) {
                if (b.startsWith("hr_")) continue;                                                    // the hair bones: one per ribbon, the ones past the count of a level are simply not there
                assertTrue(all.contains(b), "clip " + c.getKey() + " animates the unknown bone " + b);
            }
        }
        // hair bones named by any model are animated by the idles (the ribbons sway)
        JsonObject idle = anim.getAsJsonObject("idle").getAsJsonObject("bones");
        for (String id : MODELS) for (String b : boneNames(geo(id))) if (b.startsWith("hr_")) assertTrue(idle.has(b), id + ": the ribbon bone " + b + " has no sway in the idle clip");
        // the idles, the cast and the open clip keep the skirt and the hair moving
        assertTrue(idle.has("sk0") && idle.has("hair_back") && idle.has("float"));
    }

    @Test
    void theSheetsArePaintedNotFlatAndTheAdditiveOnesHaveNothingUnderAlphaZero() throws IOException {
        for (String id : MODELS) {
            BufferedImage img = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
            Set<Integer> colours = new HashSet<>();
            int opaque = 0, total = 0;
            for (int y = 0; y < img.getHeight(); y += 3) for (int x = 0; x < img.getWidth(); x += 3) {
                int argb = img.getRGB(x, y);
                total++;
                if ((argb >>> 24) > 200) opaque++;
                colours.add(argb & 0xF8F8F8F8);
            }
            assertTrue(colours.size() > 12 || id.equals("undine_fx"), id + " looks flat: " + colours.size() + " colours");
            assertTrue(opaque > 0 || id.equals("undine_fx"), id + " has no opaque texel");
        }
        for (String id : new String[]{"undine_fx", "undine_ring"}) {
            BufferedImage img = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
            int bad = 0;
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
                int a = img.getRGB(x, y);
                if ((a >>> 24) == 0 && (a & 0xFFFFFF) != 0) bad++;
            }
            assertEquals(0, bad, id + ": colour under alpha 0 (the white-square bug)");
        }
        BufferedImage ring = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/undine_ring.png").toFile());
        assertEquals(0, ring.getRGB(0, 0) >>> 24, "the ring card fades to nothing at its corner");
        assertEquals(0, ring.getRGB(ring.getWidth() / 2, ring.getHeight() / 2) >>> 24, "...and at its centre");
        assertEquals(0, ring.getRGB(ring.getWidth() - 1, ring.getHeight() / 2) >>> 24, "...and at its edge");
    }

    @Test
    void theBodySheetHasMoreThanOneTonePerPartSoItIsNotAFlatCutOut() throws IOException {
        // the likeness is painted: the face has skin, a darker shade, blue irises and dark line work; the gown has blue, darker blue and white foam
        BufferedImage img = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/undine_body.png").toFile());
        Map<String, Integer> hist = new HashMap<>();
        for (int y = 0; y < img.getHeight(); y += 2) for (int x = 0; x < img.getWidth(); x += 2) {
            int argb = img.getRGB(x, y);
            if ((argb >>> 24) < 200) continue;
            hist.merge(Integer.toHexString(argb & 0xFCFCFCFC), 1, Integer::sum);
        }
        assertTrue(hist.size() > 60, "the body sheet is a painted sheet (" + hist.size() + " tones)");
        int white = 0, blue = 0, dark = 0, skin = 0;
        for (var e : hist.entrySet()) {
            int rgb = (int) Long.parseLong(e.getKey(), 16);
            int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
            if (r > 230 && g > 238 && b > 240) white += e.getValue();
            else if (b > r + 30 && b > 150) blue += e.getValue();
            else if (r < 90 && g < 90 && b < 110) dark += e.getValue();
            else if (r > 225 && g > 215 && b > 210 && r >= b) skin += e.getValue();
        }
        assertTrue(white > 0 && blue > white / 4 && dark > 0 && skin > 0, "white foam, blue water, dark line work and pale skin are all painted: " + white + "/" + blue + "/" + dark + "/" + skin);
    }

    @Test
    void theDocAndThePreviewImagesAreThere() {
        assertTrue(Files.isRegularFile(Path.of("docs/magic/undine.md")), "docs/magic/undine.md");
        for (String f : new String[]{"undine_vs_reference.png", "undine_scale.png", "undine_companion.png", "undine_full.png"})
            assertTrue(Files.isRegularFile(Path.of("docs/previews/undine/" + f)), f);
    }
}
