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
 * Why: the 3.26.0 "painted grimoires" are 55 generated 3D models (11 franchises x 5 rarities), 55 icon models and wrappers, 55 painted icons, 11 covers, 5 trims and an animated edge, joined by
 * 75 overrides that pick one of them from three item properties. Every mistake is silent in the game: a missing texture is the purple-and-black checker, an override one step off shows the
 * wrong franchise or rarity, a model above the element limit makes the game refuse it, a legacy override out of place hides the new look forever. This test reads the shipped files and fails on:
 * a franchise list that is not the eleven in the contract order; any of the 55 body models missing, not parsing, above 140 elements, with a face whose texture variable or file does not
 * resolve, a uv outside 0..16, a tint index other than 1, a rotation the model format cannot take, an element outside -16..32; an icon or a texture of the wrong size; a missing animation
 * file for the mythic edge; the override list not being old first, then the 55 new ones in ascending (franchise, rarity) order, then the legacy ones; and a pure re-implementation of the game's
 * override matching (the LAST override whose every predicate is at most the property wins) picking the right model for every one of the 55 franchise x rarity pairs and for the legacy setting.
 */
class BookLookV2AssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final List<String> CONTRACT_ORDER = List.of("exiled_heavy_knight", "tensura", "black_clover", "magi", "solo_leveling", "danmachi", "tbate", "tower_of_god",
            "god_of_high_school", "omniscient_readers_viewpoint", "fairy_tail");
    private static final String[] CONTEXTS = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "ground", "gui", "head", "fixed"};

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
        assertTrue(ref.startsWith("nusmp:"), "this mod's namespace: " + ref);
        return ref.substring(6);
    }

    private static BufferedImage png(String texturePathWithoutExt) throws Exception {
        File f = new File(ASSETS + "textures/" + texturePathWithoutExt + ".png");
        assertTrue(f.isFile(), "missing texture " + texturePathWithoutExt);
        return ImageIO.read(f);
    }

    // ------------------------------------------------------------------------------------------------------------------ the table
    @Test
    void theElevenFranchisesAreInTheContractOrderAndFitTheTwentyStepProperty() throws Exception {
        assertEquals(CONTRACT_ORDER, franchises(), "new franchises are only appended: the order is the (index + 1) / 20 property value");
        JsonObject table = json(ASSETS + "skill_books.json");
        assertEquals(20, table.get("prop_steps").getAsInt());
        assertTrue(franchises().size() + 1 <= 20, "the largest value (n) / 20 must stay at or below 1");
        assertEquals(11, BookLookFiles.LEGACY_FRANCHISES + 1, "ten franchises have a 1.82.0 cover, the 11th is new");
    }

    // ------------------------------------------------------------------------------------------------------------------ models
    @Test
    void theParentHasEveryDisplayContextWithSaneNumbers() throws Exception {
        JsonObject parent = model("skill_book_v2");
        assertTrue(parent.has("textures") && parent.getAsJsonObject("textures").has("particle"));
        JsonObject display = parent.getAsJsonObject("display");
        for (String c : CONTEXTS) {
            assertTrue(display.has(c), "display is missing " + c);
            JsonObject d = display.getAsJsonObject(c);
            for (JsonElement s : d.getAsJsonArray("scale")) assertTrue(s.getAsDouble() > 0.05 && s.getAsDouble() <= 4.0, c + " scale " + s);
            for (JsonElement t : d.getAsJsonArray("translation")) assertTrue(Math.abs(t.getAsDouble()) <= 80, c + " translation " + t);
            assertEquals(3, d.getAsJsonArray("rotation").size());
        }
        assertEquals(display.get("thirdperson_righthand"), display.get("thirdperson_lefthand"), "the game mirrors a left hand itself: equal numbers = mirrored pose");
        assertEquals(display.get("firstperson_righthand"), display.get("firstperson_lefthand"));
        assertEquals(180.0, display.getAsJsonObject("fixed").getAsJsonArray("rotation").get(1).getAsDouble(), 1e-9);
        assertEquals(180.0, display.getAsJsonObject("head").getAsJsonArray("rotation").get(1).getAsDouble(), 1e-9);
        double groundScale = display.getAsJsonObject("ground").getAsJsonArray("scale").get(0).getAsDouble();
        assertTrue(groundScale >= 0.5 && groundScale <= 0.9, "a book on the ground is about three quarters of a block wide, not " + groundScale);
    }

    @Test
    void all55BodyModelsExistParseAndStayWithinTheLimits() throws Exception {
        Set<String> parentTextures = new HashSet<>(model("skill_book_v2").getAsJsonObject("textures").keySet());
        int checked = 0;
        for (String f : franchises()) {
            for (String r : BookLook.RARITY_IDS) {
                String name = BookLookFiles.modelName(f, r);
                JsonObject m = model(name);
                String where = name + ": ";
                assertEquals("nusmp:item/skill_book_v2", m.get("parent").getAsString(), where + "parent");
                JsonObject tex = m.getAsJsonObject("textures");
                assertEquals("nusmp:item/skill_book_v2/" + f, tex.get("cover").getAsString(), where + "cover");
                assertEquals("nusmp:item/skill_book_v2/" + f + "_fx", tex.get("fx").getAsString(), where + "relief and glow layer");
                assertEquals("nusmp:item/skill_book_v2/trim_" + r, tex.get("trim").getAsString(), where + "trim");
                if (r.equals("mythic")) assertEquals("nusmp:item/skill_book_v2/glow_mythic", tex.get("glow").getAsString(), where + "the mythic page edge is the animated texture");
                else assertFalse(tex.has("glow"), where + "only the mythic book has the animated edge");
                Set<String> defined = new HashSet<>(parentTextures);
                for (Map.Entry<String, JsonElement> t : tex.entrySet()) {
                    defined.add(t.getKey());
                    png(local(t.getValue().getAsString()));
                }
                JsonArray elements = m.getAsJsonArray("elements");
                assertTrue(elements.size() >= 40 && elements.size() <= 140, where + elements.size() + " elements (limit 140)");
                Set<String> names = new HashSet<>();
                boolean glowUsed = false;
                for (JsonElement el : elements) {
                    JsonObject e = el.getAsJsonObject();
                    String en = where + e.get("name").getAsString();
                    assertTrue(names.add(e.get("name").getAsString()), en + " is a duplicate name");
                    JsonArray from = e.getAsJsonArray("from"), to = e.getAsJsonArray("to");
                    for (int i = 0; i < 3; i++) {
                        assertTrue(from.get(i).getAsDouble() >= -16 && to.get(i).getAsDouble() <= 32, en + " leaves the -16..32 range");
                        assertTrue(from.get(i).getAsDouble() <= to.get(i).getAsDouble(), en + " has a negative axis");
                    }
                    if (e.has("rotation")) {
                        JsonObject rot = e.getAsJsonObject("rotation");
                        double a = rot.get("angle").getAsDouble();
                        assertTrue(Math.abs(a) <= 45 && Math.abs(a / 22.5 - Math.rint(a / 22.5)) < 1e-9, en + " rotation " + a + " is not one of -45, -22.5, 0, 22.5, 45");
                        assertTrue(Set.of("x", "y", "z").contains(rot.get("axis").getAsString()));
                        assertEquals(3, rot.getAsJsonArray("origin").size());
                    }
                    for (Map.Entry<String, JsonElement> fe : e.getAsJsonObject("faces").entrySet()) {
                        JsonObject face = fe.getValue().getAsJsonObject();
                        String ref = face.get("texture").getAsString();
                        assertTrue(ref.startsWith("#") && defined.contains(ref.substring(1)), en + "." + fe.getKey() + " names an undefined texture " + ref);
                        if (ref.equals("#glow")) glowUsed = true;
                        JsonArray uv = face.getAsJsonArray("uv");
                        assertEquals(4, uv.size());
                        for (JsonElement u : uv) assertTrue(u.getAsDouble() >= 0 && u.getAsDouble() <= 16, en + "." + fe.getKey() + " uv outside 0..16: " + uv);
                        if (face.has("tintindex")) assertEquals(1, face.get("tintindex").getAsInt(), en + ": the client tints index 1 only");
                    }
                }
                assertEquals(r.equals("mythic"), glowUsed, where + "the animated edge is used by the mythic book and by no other");
                checked++;
            }
        }
        assertEquals(55, checked);
    }

    @Test
    void theFiftyFiveIconModelsAndWrappersAreWellFormed() throws Exception {
        for (String f : franchises()) {
            for (String r : BookLook.RARITY_IDS) {
                JsonObject icon = model("skill_book_icon_" + f + "_" + r);
                assertEquals(2, icon.getAsJsonArray("elements").size(), "one quad for the painted icon and one for its tint layer");
                for (String key : new String[]{"icon", "tint"}) {
                    BufferedImage img = png(local(icon.getAsJsonObject("textures").get(key).getAsString()));
                    assertEquals(64, img.getWidth(), f + "_" + r + " " + key);
                    assertEquals(64, img.getHeight(), f + "_" + r + " " + key);
                }
                JsonObject st = model("skill_book_v2st_" + f + "_" + r);
                assertEquals("neoforge:separate_transforms", st.get("loader").getAsString());
                assertEquals("front", st.get("gui_light").getAsString(), "the flat icon must be lit from the front");
                assertEquals("nusmp:item/" + BookLookFiles.modelName(f, r), st.getAsJsonObject("base").get("parent").getAsString());
                assertEquals("nusmp:item/skill_book_icon_" + f + "_" + r, st.getAsJsonObject("perspectives").getAsJsonObject("gui").get("parent").getAsString());
            }
        }
    }

    @Test
    void theTexturesHaveTheirSizesAndTheMythicEdgeIsAnimated() throws Exception {
        for (String f : franchises()) {
            BufferedImage cover = png("item/skill_book_v2/" + f);
            assertEquals(512, cover.getWidth());
            assertEquals(256, cover.getHeight());
            BufferedImage fx = png("item/skill_book_v2/" + f + "_fx");
            assertEquals(288, fx.getWidth());
            assertEquals(208, fx.getHeight());
            // the front, back and spine regions must be fully opaque: a cutout hole would show the inside of the book
            for (int[] r : new int[][]{{0, 0, 144, 208}, {144, 0, 288, 208}, {288, 0, 352, 208}}) {
                for (int y = r[1]; y < r[3]; y += 3) for (int x = r[0]; x < r[2]; x += 3) assertEquals(255, cover.getRGB(x, y) >>> 24, f + " cover has a hole at " + x + "," + y);
            }
        }
        for (String r : BookLook.RARITY_IDS) {
            BufferedImage trim = png("item/skill_book_v2/trim_" + r);
            assertEquals(256, trim.getWidth());
            assertEquals(256, trim.getHeight());
        }
        BufferedImage glow = png("item/skill_book_v2/glow_mythic");
        assertEquals(32, glow.getWidth());
        assertEquals(0, glow.getHeight() % glow.getWidth(), "an animated texture is a vertical strip of square frames");
        assertTrue(glow.getHeight() / glow.getWidth() >= 2, "at least two frames");
        JsonObject mcmeta = json(ASSETS + "textures/item/skill_book_v2/glow_mythic.png.mcmeta");
        assertTrue(mcmeta.has("animation"), "the .mcmeta animation block");
        assertTrue(mcmeta.getAsJsonObject("animation").get("frametime").getAsInt() >= 1);
    }

    @Test
    void everyFileTheLookNeedsIsInTheSourceTree() throws Exception {
        List<String> missing = BookLookFiles.missing(franchises(), p -> new File("src/main/resources/" + p).isFile());
        assertTrue(missing.isEmpty(), "missing: " + missing);
        assertEquals(3 + 5 + 2 + 11 * (2 + 5 * 5) + 10 * 2, BookLookFiles.requiredPaths(franchises()).size(), "the number of files the look needs");
    }

    @Test
    void theOldModelsAndTexturesAreStillThere() throws Exception {
        for (String f : franchises().subList(0, BookLookFiles.LEGACY_FRANCHISES)) {
            model("skill_book_3d_" + f);
            model("skill_book_st_" + f);
            assertTrue(new File(ASSETS + "textures/item/skill_book_3d/" + f + ".png").isFile(), "the 1.82.0 cover of " + f);
        }
        assertEquals("nusmp:item/skill_book_3d", model("skill_book_st_generic").getAsJsonObject("base").get("parent").getAsString());
        assertTrue(new File(ASSETS + "textures/item/skill_book.png").isFile());
        assertTrue(new File(ASSETS + "textures/item/skill_book_cover.png").isFile());
    }

    // ------------------------------------------------------------------------------------------------------------------ overrides
    private record Override(Map<String, Float> predicate, String model) {}

    private static List<Override> overrides() throws Exception {
        List<Override> out = new ArrayList<>();
        for (JsonElement e : model("skill_book").getAsJsonArray("overrides")) {
            JsonObject o = e.getAsJsonObject();
            Map<String, Float> p = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> pe : o.getAsJsonObject("predicate").entrySet()) p.put(pe.getKey(), pe.getValue().getAsFloat());
            out.add(new Override(p, o.get("model").getAsString()));
        }
        return out;
    }

    /** The game's rule (ItemOverrides): the last override whose every predicate is at most the item's property value wins; a missing property counts as 0. Returns the model, or null for the base model. */
    static String pick(List<Override> list, Map<String, Float> props) {
        for (int i = list.size() - 1; i >= 0; i--) {
            boolean all = true;
            for (Map.Entry<String, Float> p : list.get(i).predicate().entrySet()) {
                float v = props.getOrDefault(p.getKey(), 0f);
                if (!(v >= p.getValue())) {
                    all = false;
                    break;
                }
            }
            if (all) return list.get(i).model();
        }
        return null;
    }

    @Test
    void theOverrideListIsOldFirstThenTheFiftyFiveThenTheLegacyOnes() throws Exception {
        List<String> fr = franchises();
        List<Override> list = overrides();
        int legacyCount = BookLookFiles.LEGACY_FRANCHISES;
        assertEquals(legacyCount + fr.size() * 5 + fr.size(), list.size());
        for (int i = 0; i < legacyCount; i++) {
            Override o = list.get(i);
            assertEquals(Set.of("nusmp:franchise"), o.predicate().keySet(), "the first ten are the 1.82.0 overrides, kept");
            assertEquals("nusmp:item/skill_book_st_" + fr.get(i), o.model());
        }
        float lastF = -1, lastR = -1;
        for (int k = 0; k < fr.size() * 5; k++) {
            Override o = list.get(legacyCount + k);
            assertEquals(Set.of("nusmp:franchise", "nusmp:rarity"), o.predicate().keySet());
            float tf = o.predicate().get("nusmp:franchise"), tr = o.predicate().get("nusmp:rarity");
            assertTrue(tf > lastF || (tf == lastF && tr > lastR), "ascending (franchise, rarity), the last match wins: entry " + k);
            lastF = tf;
            lastR = tr;
            assertEquals("nusmp:item/skill_book_v2st_" + fr.get(k / 5) + "_" + BookLook.RARITY_IDS.get(k % 5), o.model());
        }
        for (int i = 0; i < fr.size(); i++) {
            Override o = list.get(legacyCount + fr.size() * 5 + i);
            assertEquals(Set.of("nusmp:look", "nusmp:franchise"), o.predicate().keySet(), "the legacy overrides come last and need nusmp:look");
            assertEquals(1.0f, o.predicate().get("nusmp:look"));
            assertEquals(i < legacyCount ? "nusmp:item/skill_book_st_" + fr.get(i) : "nusmp:item/skill_book_st_generic", o.model());
        }
        for (Override o : list) assertTrue(new File(ASSETS + "models/" + local(o.model()) + ".json").isFile(), "the model behind an override is missing: " + o.model());
    }

    @Test
    void theGameMatchingPicksTheRightModelForAll55PairsAndTheLegacySetting() throws Exception {
        List<String> fr = franchises();
        List<Override> list = overrides();
        int steps = 20;
        for (int i = 0; i < fr.size(); i++) {
            float fv = (i + 1) / (float) steps;                                  // BookLook.Index.franchiseValue
            for (int r = 0; r < 5; r++) {
                float rv = BookLook.rarityStep(r);                              // (rank + 1) / 6
                String want = "nusmp:item/skill_book_v2st_" + fr.get(i) + "_" + BookLook.RARITY_IDS.get(r);
                assertEquals(want, pick(list, Map.of("nusmp:franchise", fv, "nusmp:rarity", rv, "nusmp:look", 0f)), fr.get(i) + " " + r + " (painted look)");
                String legacy = i < BookLookFiles.LEGACY_FRANCHISES ? "nusmp:item/skill_book_st_" + fr.get(i) : "nusmp:item/skill_book_st_generic";
                assertEquals(legacy, pick(list, Map.of("nusmp:franchise", fv, "nusmp:rarity", rv, "nusmp:look", 1f)), fr.get(i) + " " + r + " (legacy look)");
            }
        }
        // a book with no franchise (value 0) keeps the base model, new or legacy
        assertNull(pick(list, Map.of("nusmp:franchise", 0f, "nusmp:rarity", BookLook.rarityStep(3), "nusmp:look", 0f)));
        assertNull(pick(list, Map.of("nusmp:franchise", 0f, "nusmp:rarity", BookLook.rarityStep(0), "nusmp:look", 1f)));
    }

    @Test
    void theRarityThresholdsSitHalfAStepBelowEachValue() throws Exception {
        List<Override> list = overrides();
        Set<Float> seen = new HashSet<>();
        for (Override o : list) if (o.predicate().containsKey("nusmp:rarity")) seen.add(o.predicate().get("nusmp:rarity"));
        assertEquals(5, seen.size());
        for (int rank = 0; rank < 5; rank++) {
            float threshold = (2 * rank + 1) / 12f;
            boolean found = false;
            for (float s : seen) found |= Math.abs(s - threshold) < 1e-5;
            assertTrue(found, "no override threshold near " + threshold + " for rank " + rank);
            assertTrue(BookLook.rarityStep(rank) > threshold + 0.04f, "the value of rank " + rank + " clears its own threshold with room for float rounding");
            if (rank > 0) assertTrue(BookLook.rarityStep(rank - 1) < threshold - 0.04f, "the value of the rank below stays under it");
        }
    }
}
