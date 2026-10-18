package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the 3D Skill Book is built from files no compiler reads (a model with ~30 cube elements and a display block, twelve cover textures, an atlas, ten generated models and a
 * table), and every mistake in them is silent or ugly in game: a missing texture is the purple-and-black checker, an override one franchise off shows the wrong cover, a tint index
 * on the wrong face turns the gold clasp the line's colour, a cover that is not grey changes the line's colour, a display scale of 0 makes the book vanish in a hand. This test reads
 * the shipped files and fails on: the registered model not being a {@code neoforge:separate_transforms} that uses the 3D body in hand/on the ground/in frames and the OLD sprite model in
 * the GUI; the old sprite model not being kept byte-for-byte meaningful; the franchise overrides not matching the table's franchise list (one per franchise, ascending half-step thresholds,
 * the model behind each one existing and using that franchise's cover); any face whose texture does not resolve or whose file is missing; a tint index other than 1; elements or uv
 * out of the model's range; a missing or absurd display transform for any of the eight contexts (left and right hands equal on purpose, see the doc); a cover that is not 128x128 greys
 * no darker than 104 with a full-white highlight (so the tint is clean); the atlas not 64x64; and the new lang keys.
 */
class BookArtAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static JsonObject model(String name) throws Exception { return json(ASSETS + "models/item/" + name + ".json"); }

    private static List<String> franchises() throws Exception {
        List<String> out = new ArrayList<>();
        for (JsonElement e : json(ASSETS + "skill_books.json").getAsJsonArray("franchises")) out.add(e.getAsString());
        return out;
    }

    private static String local(String ref) {
        assertTrue(ref.startsWith("nusmp:"), "textures and models are in this mod's namespace: " + ref);
        return ref.substring(6);
    }

    @Test
    void theRegisteredModelUsesTheBodyInHandAndTheOldSpriteInTheGui() throws Exception {
        JsonObject top = model("skill_book");
        assertEquals("neoforge:separate_transforms", top.get("loader").getAsString());
        assertEquals("front", top.get("gui_light").getAsString(), "the flat inventory sprite must be lit from the front or it goes dark");
        assertEquals("nusmp:item/skill_book_3d", top.getAsJsonObject("base").get("parent").getAsString());
        assertEquals("nusmp:item/skill_book_3d/base", top.getAsJsonObject("textures").get("particle").getAsString(), "the wrapper needs its own particle texture");
        JsonObject perspectives = top.getAsJsonObject("perspectives");
        assertEquals(1, perspectives.size(), "only the GUI differs from the body");
        assertEquals("nusmp:item/skill_book_2d", perspectives.getAsJsonObject("gui").get("parent").getAsString());
        JsonObject old = model("skill_book_2d");
        assertEquals("minecraft:item/generated", old.get("parent").getAsString());
        assertEquals("nusmp:item/skill_book", old.getAsJsonObject("textures").get("layer0").getAsString());
        assertEquals("nusmp:item/skill_book_cover", old.getAsJsonObject("textures").get("layer1").getAsString());
    }

    @Test
    void oneOverridePerFranchiseWithHalfStepThresholdsAndTheRightCover() throws Exception {
        List<String> fr = franchises();
        JsonObject table = json(ASSETS + "skill_books.json");
        int steps = table.get("prop_steps").getAsInt();
        assertTrue(fr.size() + 1 <= steps, "the property value (index + 1) / steps must stay at or below 1: " + fr.size() + " franchises, " + steps + " steps");
        JsonArray overrides = model("skill_book").getAsJsonArray("overrides");
        assertEquals(fr.size(), overrides.size(), "one override per franchise");
        double previous = 0;
        for (int i = 0; i < fr.size(); i++) {
            JsonObject o = overrides.get(i).getAsJsonObject();
            double threshold = o.getAsJsonObject("predicate").get("nusmp:franchise").getAsDouble();
            assertEquals((i + 1.0) / steps - 0.5 / steps, threshold, 1e-5, "threshold of " + fr.get(i));
            assertTrue(threshold > previous, "thresholds ascend (the last matching override wins)");
            previous = threshold;
            String target = local(o.get("model").getAsString());
            assertEquals("item/skill_book_st_" + fr.get(i), target);
            JsonObject st = model("skill_book_st_" + fr.get(i));
            assertEquals("neoforge:separate_transforms", st.get("loader").getAsString());
            assertEquals("nusmp:item/skill_book_2d", st.getAsJsonObject("perspectives").getAsJsonObject("gui").get("parent").getAsString(), "the GUI icon stays the old sprite for every franchise");
            assertEquals("nusmp:item/skill_book_3d_" + fr.get(i), st.getAsJsonObject("base").get("parent").getAsString());
            JsonObject body = model("skill_book_3d_" + fr.get(i));
            assertEquals("nusmp:item/skill_book_3d", body.get("parent").getAsString());
            assertEquals("nusmp:item/skill_book_3d/" + fr.get(i), body.getAsJsonObject("textures").get("cover").getAsString());
            assertTrue(new File(ASSETS + "textures/item/skill_book_3d/" + fr.get(i) + ".png").isFile(), "missing cover of " + fr.get(i));
        }
    }

    @Test
    void theFranchiseOrderMatchesTheCatalogueOfTheGenerator() throws Exception {
        JsonObject cat = json("tools/lines/catalog.json").getAsJsonObject("franchises");
        List<String> expected = new ArrayList<>(cat.keySet());
        assertEquals(expected, franchises(), "tools/lines/lines_lib.FRANCHISES (via catalog.json) and assets/nusmp/skill_books.json list the franchises in different order: the covers would swap");
    }

    @Test
    void theBodyModelIsWellFormed() throws Exception {
        JsonObject body = model("skill_book_3d");
        JsonObject tex = body.getAsJsonObject("textures");
        Set<String> files = new HashSet<>();
        for (Map.Entry<String, JsonElement> t : tex.entrySet()) {
            String path = local(t.getValue().getAsString());
            assertTrue(new File(ASSETS + "textures/" + path + ".png").isFile(), "missing texture " + path);
            files.add(t.getKey());
        }
        assertTrue(files.contains("base") && files.contains("cover") && files.contains("particle"));
        JsonArray elements = body.getAsJsonArray("elements");
        assertTrue(elements.size() >= 20, "a thick board pair, a three-step spine with bands, three page boxes, a ribbon, four caps and a clasp: " + elements.size());
        Set<String> names = new HashSet<>();
        int tinted = 0;
        for (JsonElement el : elements) {
            JsonObject e = el.getAsJsonObject();
            assertTrue(names.add(e.get("name").getAsString()), "duplicate element name " + e.get("name"));
            for (String key : new String[]{"from", "to"}) {
                JsonArray a = e.getAsJsonArray(key);
                assertEquals(3, a.size());
                for (JsonElement c : a) assertTrue(c.getAsDouble() >= -16 && c.getAsDouble() <= 32, e.get("name") + " " + key + " is outside the model's -16..32 range");
            }
            for (int i = 0; i < 3; i++) assertTrue(e.getAsJsonArray("from").get(i).getAsDouble() < e.getAsJsonArray("to").get(i).getAsDouble(), e.get("name") + " has an empty axis");
            for (Map.Entry<String, JsonElement> f : e.getAsJsonObject("faces").entrySet()) {
                JsonObject face = f.getValue().getAsJsonObject();
                String ref = face.get("texture").getAsString();
                assertTrue(ref.startsWith("#") && files.contains(ref.substring(1)), e.get("name") + "." + f.getKey() + " names an undefined texture " + ref);
                JsonArray uv = face.getAsJsonArray("uv");
                assertEquals(4, uv.size());
                for (JsonElement u : uv) assertTrue(u.getAsDouble() >= 0 && u.getAsDouble() <= 16, e.get("name") + "." + f.getKey() + " uv outside 0..16");
                if (face.has("tintindex")) {
                    assertEquals(1, face.get("tintindex").getAsInt(), "the client tints index 1 only; anything else would be white (or a different colour)");
                    tinted++;
                    boolean cover = ref.equals("#cover"), ribbon = e.get("name").getAsString().startsWith("ribbon");
                    assertTrue(cover || ribbon, "only the cover leather and the ribbon take the line's colour, not " + e.get("name"));
                } else {
                    assertTrue(!ref.equals("#cover"), "a cover face without a tint would keep the grey: " + e.get("name") + "." + f.getKey());
                }
            }
        }
        assertTrue(tinted >= 12, "the cover faces carry the tint: " + tinted);
    }

    @Test
    void theDisplayBlockHasEveryContextAndSaneNumbers() throws Exception {
        JsonObject display = model("skill_book_3d").getAsJsonObject("display");
        String[] contexts = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "ground", "gui", "head", "fixed"};
        for (String c : contexts) {
            assertTrue(display.has(c), "display is missing " + c);
            JsonObject d = display.getAsJsonObject(c);
            for (JsonElement s : d.getAsJsonArray("scale")) assertTrue(s.getAsDouble() > 0.05 && s.getAsDouble() <= 4.0, c + " scale " + s + " (the game clamps above 4)");
            for (JsonElement t : d.getAsJsonArray("translation")) assertTrue(Math.abs(t.getAsDouble()) <= 80, c + " translation " + t + " (the game clamps beyond 80)");
            assertEquals(3, d.getAsJsonArray("rotation").size());
        }
        // the game mirrors a left-hand context itself (negates tx, ry, rz): equal numbers give a mirrored pose, which is what a book wants (cover turned outward on both sides)
        assertEquals(display.get("thirdperson_righthand"), display.get("thirdperson_lefthand"));
        assertEquals(display.get("firstperson_righthand"), display.get("firstperson_lefthand"));
        // the ground pose lays the book on its back: rotation about X by -90 puts the front cover (+Z) up
        JsonArray ground = display.getAsJsonObject("ground").getAsJsonArray("rotation");
        assertEquals(-90.0, ground.get(0).getAsDouble(), 1e-9);
        assertEquals(0.0, ground.get(1).getAsDouble(), 1e-9);
        assertEquals(0.0, ground.get(2).getAsDouble(), 1e-9);
        // frames and the head turn the front cover to the viewer (rotation about Y by 180 like every item sprite)
        assertEquals(180.0, display.getAsJsonObject("fixed").getAsJsonArray("rotation").get(1).getAsDouble(), 1e-9);
        assertEquals(180.0, display.getAsJsonObject("head").getAsJsonArray("rotation").get(1).getAsDouble(), 1e-9);
    }

    @Test
    void theCoversAreGreyAtLeast104WithAWhiteHighlight() throws Exception {
        List<String> names = new ArrayList<>(franchises());
        names.add("generic");
        for (String n : names) {
            BufferedImage img = ImageIO.read(new File(ASSETS + "textures/item/skill_book_3d/" + n + ".png"));
            assertEquals(128, img.getWidth(), n);
            assertEquals(128, img.getHeight(), n);
            int lightest = 0, opaque = 0;
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int argb = img.getRGB(x, y);
                    if ((argb >>> 24) == 0) continue;
                    opaque++;
                    int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
                    assertTrue(r == g && g == b, n + " pixel " + x + "," + y + " is not grey, so the line's colour would shift: " + Integer.toHexString(argb));
                    assertTrue(r >= 104, n + " pixel " + x + "," + y + " is too dark to tint: " + r);
                    lightest = Math.max(lightest, r);
                }
            }
            assertEquals(255, lightest, n + ": the embossed highlight is full white, so the brightest tinted pixel is exactly the line's colour");
            assertTrue(opaque > 72 * 104 + 32 * 104, n + ": the front, spine and edge regions are fully painted: " + opaque);
        }
    }

    @Test
    void theAtlasIsSixtyFourSquareAndPaintedWhereTheModelLooks() throws Exception {
        BufferedImage img = ImageIO.read(new File(ASSETS + "textures/item/skill_book_3d/base.png"));
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
        // the regions of tools/lines/book_model.py, in pixels: none may be transparent (cutout alpha would punch holes in the book)
        int[][] regions = {{0, 0, 16, 16}, {16, 0, 32, 16}, {0, 16, 16, 32}, {16, 16, 24, 32}, {24, 16, 32, 24}, {32, 0, 64, 32}, {32, 32, 64, 64}};
        for (int[] r : regions) {
            for (int y = r[1]; y < r[3]; y++) {
                for (int x = r[0]; x < r[2]; x++) assertTrue((img.getRGB(x, y) >>> 24) == 255, "transparent pixel in atlas region " + r[0] + "," + r[1] + " at " + x + "," + y);
            }
        }
        // the ribbon is tinted by the client, so it must be grey
        for (int y = 16; y < 32; y++) {
            for (int x = 16; x < 24; x++) {
                int argb = img.getRGB(x, y);
                assertTrue((argb >> 16 & 255) == (argb >> 8 & 255) && (argb >> 8 & 255) == (argb & 255), "the ribbon cloth must be grey, got " + Integer.toHexString(argb));
            }
        }
    }

    @Test
    void theNewLangKeysAreFlatAndCoverEveryFranchise() throws Exception {
        JsonObject lang = json("tools/lines/lang_book.json");
        for (String f : franchises()) {
            assertTrue(lang.has("franchise.nusmp." + f), "no display name for franchise " + f);
            assertFalse(lang.get("franchise.nusmp." + f).getAsString().isEmpty());
        }
        assertTrue(lang.has("tooltip.nusmp.skill_book.franchise"));
        for (Map.Entry<String, JsonElement> e : lang.entrySet()) assertTrue(e.getValue().isJsonPrimitive(), "lang_book.json is flat key -> text: " + e.getKey());
    }
}
