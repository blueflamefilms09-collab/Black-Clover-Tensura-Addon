package com.newuniverse.nusmp.charter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Canon Grimoire (the old Charter Grimoire) is a hand-held 3D book made of files no compiler reads: a wrapper model, a body model of about forty cubes with a display block and a
 * 192 x 240 palette atlas, per book. The owner's bug report was a giant flat slab in the first person and a tray floating beside the hand in the third, which is a wrong display block and a
 * wrong box, and a missing texture is the purple-and-black checker. This test reads the shipped files of EVERY book (found from the model folder, and checked against the Charter table) and
 * fails on: a book without the new structure (wrapper using {@code neoforge:separate_transforms}, the body, the icon model for the GUI); fewer than 12 elements; an element or uv out of range;
 * a face texture that does not resolve; an atlas that is not a small palette PNG; a missing or absurd display transform for any of the eight contexts (a scale that would bring the slab back,
 * left and right hand not equal, a translation the game would clamp); a book box that is not book sized; the old art not kept as _v1; and any Java string that still builds the words
 * "Charter Grimoire" for the player. Docs: docs/canon/canon_grimoire.md. The in-game look is previewed only by tools/canon/preview_canon.py.
 */
class CanonGrimoireAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final List<String> CONTEXTS = List.of("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "ground", "gui", "head", "fixed");
    private static final Pattern MODEL = Pattern.compile("charter_grimoire_([a-z0-9_]+)\\.json");

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static String local(String ref) {
        assertTrue(ref.startsWith("nusmp:"), "this mod's namespace: " + ref);
        return ref.substring(6);
    }

    private static List<String> ids() throws Exception {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(Paths.get(ASSETS + "models/item"))) {
            for (Path p : (Iterable<Path>) s.sorted()::iterator) {
                Matcher m = MODEL.matcher(p.getFileName().toString());
                if (!m.matches()) continue;
                String id = m.group(1);
                if (id.endsWith("_icon") || id.endsWith("_3d") || id.endsWith("_v1")) continue;
                out.add(id);
            }
        }
        return out;
    }

    @Test
    void everyRegisteredGrimoireHasABook() throws Exception {
        List<String> ids = ids();
        assertTrue(ids.size() >= 30, "the seven bosses and the arc characters: " + ids.size());
        for (String core : CharterTable.ids()) assertTrue(ids.contains(core), "no model for the registered grimoire " + core);
        for (String id : List.of("dante", "vanica", "zenon", "lucius", "devil_avatar", "goliath", "charybdis_spawn", "meliodas", "ban", "diane", "escanor")) assertTrue(ids.contains(id), id);
    }

    @Test
    void theWrapperUsesTheBodyInHandAndTheOldIconInTheGui() throws Exception {
        for (String id : ids()) {
            JsonObject top = json(ASSETS + "models/item/charter_grimoire_" + id + ".json");
            assertEquals("neoforge:separate_transforms", top.get("loader").getAsString(), id);
            assertEquals("front", top.get("gui_light").getAsString(), id + ": the flat icon must be lit from the front");
            assertEquals("nusmp:item/charter_grimoire_" + id + "_icon", top.getAsJsonObject("textures").get("particle").getAsString(), id);
            String body = local(top.getAsJsonObject("base").get("parent").getAsString());
            assertEquals("item/charter_grimoire_" + id + "_3d", body, id);
            assertTrue(new File(ASSETS + "models/" + body + ".json").isFile(), id + ": the body model is missing");
            JsonObject gui = top.getAsJsonObject("perspectives").getAsJsonObject("gui");
            assertEquals(1, top.getAsJsonObject("perspectives").size(), id + ": only the GUI differs from the body");
            String icon = local(gui.get("parent").getAsString());
            assertEquals("item/charter_grimoire_" + id + "_icon", icon, id);
            JsonObject iconModel = json(ASSETS + "models/" + icon + ".json");
            assertEquals("minecraft:item/generated", iconModel.get("parent").getAsString(), id);
            String sprite = local(iconModel.getAsJsonObject("textures").get("layer0").getAsString());
            assertTrue(new File(ASSETS + "textures/" + sprite + ".png").isFile(), id + ": the old icon sprite is missing");
        }
    }

    @Test
    void theBodyIsARealBookWithItsTexture() throws Exception {
        for (String id : ids()) {
            JsonObject body = json(ASSETS + "models/item/charter_grimoire_" + id + "_3d.json");
            JsonObject tex = body.getAsJsonObject("textures");
            String atlas = local(tex.get("0").getAsString());
            assertEquals("item/charter_grimoire_" + id, atlas, id);
            File png = new File(ASSETS + "textures/" + atlas + ".png");
            assertTrue(png.isFile(), id + ": texture missing");
            BufferedImage img = ImageIO.read(png);
            assertTrue(img.getColorModel() instanceof IndexColorModel, id + ": the atlas must be a palette PNG");
            assertTrue(((IndexColorModel) img.getColorModel()).getMapSize() <= 256, id);
            assertTrue(img.getWidth() <= 256 && img.getHeight() <= 256 && img.getWidth() >= 64, id + ": a small atlas, " + img.getWidth() + "x" + img.getHeight());
            JsonArray size = body.getAsJsonArray("texture_size");
            assertEquals(img.getWidth(), size.get(0).getAsInt(), id + ": texture_size is the atlas size");
            assertEquals(img.getHeight(), size.get(1).getAsInt(), id);
            assertTrue(new File(ASSETS + "textures/" + local(tex.get("particle").getAsString()) + ".png").isFile(), id + ": particle texture");

            JsonArray elements = body.getAsJsonArray("elements");
            assertTrue(elements.size() >= 12, id + " has only " + elements.size() + " elements");
            double[] lo = {99, 99, 99}, hi = {-99, -99, -99};
            for (JsonElement el : elements) {
                JsonObject e = el.getAsJsonObject();
                JsonArray from = e.getAsJsonArray("from"), to = e.getAsJsonArray("to");
                for (int a = 0; a < 3; a++) {
                    double f = from.get(a).getAsDouble(), t = to.get(a).getAsDouble();
                    assertTrue(f <= t && f >= -16 && t <= 32, id + "/" + e.get("name") + ": element out of range");
                    lo[a] = Math.min(lo[a], f);
                    hi[a] = Math.max(hi[a], t);
                }
                for (var fe : e.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = fe.getValue().getAsJsonObject();
                    assertEquals("#0", face.get("texture").getAsString(), id + "/" + e.get("name") + ": faces use the atlas");
                    JsonArray uv = face.getAsJsonArray("uv");
                    for (int k = 0; k < 4; k++) {
                        double u = uv.get(k).getAsDouble();
                        assertTrue(u >= 0 && u <= 16, id + "/" + e.get("name") + ": uv out of 0..16");
                    }
                    assertFalse(face.has("tintindex"), id + ": nothing is tinted");
                }
            }
            // the box: a book (about 10.6 x 13 x 4 units without the ribbon), never the 11 x 15 slab: width <= 12, height (boards, ribbon excluded: first 0..16) <= 16, depth <= 5
            assertTrue(hi[0] - lo[0] <= 12.0, id + ": too wide " + (hi[0] - lo[0]));
            assertTrue(hi[2] - lo[2] <= 5.0, id + ": too thick " + (hi[2] - lo[2]));
            assertTrue(hi[1] - lo[1] <= 16.5, id + ": too tall " + (hi[1] - lo[1]));
            assertTrue(hi[2] - lo[2] >= 3.0, id + ": a book has thickness (not a slab) " + (hi[2] - lo[2]));
        }
    }

    @Test
    void theDisplayBlockHoldsAndSizesTheBook() throws Exception {
        for (String id : ids()) {
            JsonObject display = json(ASSETS + "models/item/charter_grimoire_" + id + "_3d.json").getAsJsonObject("display");
            for (String ctx : CONTEXTS) assertTrue(display.has(ctx), id + " has no display for " + ctx);
            for (String ctx : CONTEXTS) {
                JsonObject d = display.getAsJsonObject(ctx);
                for (int k = 0; k < 3; k++) {
                    double s = d.getAsJsonArray("scale").get(k).getAsDouble();
                    assertTrue(s >= 0.25 && s <= 1.3, id + "/" + ctx + ": scale " + s + " would make the book vanish or a giant");
                    assertTrue(Math.abs(d.getAsJsonArray("translation").get(k).getAsDouble()) <= 80, id + "/" + ctx + ": the game clamps a translation to 80");
                    assertTrue(Math.abs(d.getAsJsonArray("rotation").get(k).getAsDouble()) <= 360, id + "/" + ctx);
                }
                assertEquals(d.getAsJsonArray("scale").get(0).getAsDouble(), d.getAsJsonArray("scale").get(2).getAsDouble(), 1e-9, id + "/" + ctx + ": uniform scale");
            }
            // the bug: a slab that fills the screen. In the hands the book may be at most 0.6 (first person) and 0.7 (third person) of its model size.
            for (String ctx : List.of("firstperson_righthand", "firstperson_lefthand"))
                assertTrue(display.getAsJsonObject(ctx).getAsJsonArray("scale").get(0).getAsDouble() <= 0.6, id + "/" + ctx + ": too big for the first person (the old slab was 0.68)");
            for (String ctx : List.of("thirdperson_righthand", "thirdperson_lefthand"))
                assertTrue(display.getAsJsonObject(ctx).getAsJsonArray("scale").get(0).getAsDouble() <= 0.7, id + "/" + ctx);
            // left and right hands carry the same numbers on purpose: the game mirrors them (docs/canon/canon_grimoire.md section 3)
            assertEquals(display.get("firstperson_righthand"), display.get("firstperson_lefthand"), id);
            assertEquals(display.get("thirdperson_righthand"), display.get("thirdperson_lefthand"), id);
            // the old display of the slab had its front turned sideways (ry -90) in the first person: the cover must face the camera now (|ry| well under 60)
            double ry = display.getAsJsonObject("firstperson_righthand").getAsJsonArray("rotation").get(1).getAsDouble();
            assertTrue(Math.abs(ry) < 45, id + ": the first person cover must face the player, ry = " + ry);
        }
    }

    @Test
    void theOldArtIsKept() throws Exception {
        for (String id : ids()) {
            assertTrue(new File(ASSETS + "models/item/charter_grimoire_" + id + "_v1.json").isFile(), id + ": the old model is kept as _v1");
            assertTrue(new File(ASSETS + "textures/item/charter_grimoire_" + id + "_v1.png").isFile(), id + ": the old texture is kept as _v1");
            JsonObject old = json(ASSETS + "models/item/charter_grimoire_" + id + "_v1.json");
            assertEquals("nusmp:item/charter_grimoire_" + id + "_v1", old.getAsJsonObject("textures").get("0").getAsString(), id + ": v1 points at the v1 texture");
            assertTrue(old.getAsJsonArray("elements").size() >= 5, id);
        }
    }

    @Test
    void noJavaStringBuildsTheOldNameForThePlayer() throws Exception {
        Pattern literal = Pattern.compile("\"(?:[^\"\\\\\\n]|\\\\.)*\"");
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java"))) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                int n = 0;
                for (String line : Files.readAllLines(p)) {
                    n++;
                    String t = line.trim();
                    if (t.startsWith("*") || t.startsWith("/*") || t.startsWith("//")) continue;
                    Matcher m = literal.matcher(line);
                    while (m.find()) {
                        String s = m.group();
                        if (s.contains("Charter Grimoire") || s.contains("charter grimoire") || s.contains("Charter Grimoires")) bad.add(p + ":" + n + " " + s);
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "the book is the Canon Grimoire now: " + bad);
    }

    @Test
    void theItemFallbackNameIsCanon() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/newuniverse/nusmp/charter/CharterGrimoireItem.java"));
        assertTrue(src.contains("\"Canon Grimoire of \""), "the fallback name of the arc books");
    }
}
