package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the books are drawn and declared by resource files that no Java compiler checks, and a wrong file shows in game as the purple-and-black missing texture, a cover that
 * does not take the route's colour, or an SP Book that always shows the wrong sprite; a loot modifier whose JSON is missing or not listed simply never drops anything and
 * says nothing. This test reads the files (no game needed) and fails when: a sprite is missing or not 16 by 16 pixels; the two layers of the Skill Book overlap or leave a
 * hole (the cover is the part the client tints, so it must hold only greys, or the tint would shift the colours, and it must not sit under the gem or the fittings); an SP
 * sprite is empty; the Skill Book model does not name both layers in the order the tint index relies on (layer 1 is the cover); the SP Book model does not carry exactly one
 * override per kind, in the order of the kind table, with the threshold half a step below the kind's model value and the kind's own model behind it; or the loot modifier's
 * definition, its type id and its place in the global list are wrong (the two older entries must still be there: add the new, keep the old).
 */
class ItemsAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final String DATA = "src/main/resources/data/";

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static BufferedImage sprite(String name) throws Exception {
        File f = new File(ASSETS + "textures/item/" + name + ".png");
        assertTrue(f.isFile(), "missing sprite " + name);
        BufferedImage img = ImageIO.read(f);
        assertEquals(16, img.getWidth(), name + " width");
        assertEquals(16, img.getHeight(), name + " height");
        return img;
    }

    private static int opaque(BufferedImage img) {
        int n = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((img.getRGB(x, y) >>> 24) > 0) n++;
        return n;
    }

    @Test
    void theSkillBookLayersAreComplementary() throws Exception {
        BufferedImage book = sprite("skill_book"), cover = sprite("skill_book_cover");
        int both = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean a = (book.getRGB(x, y) >>> 24) > 0, c = (cover.getRGB(x, y) >>> 24) > 0;
                if (a && c) both++;
            }
        }
        assertEquals(0, both, "a pixel in both layers would be drawn twice and tinted wrongly");
        assertTrue(opaque(book) > 90 && opaque(book) < 200, "the book layer: " + opaque(book) + " opaque pixels");
        assertTrue(opaque(cover) > 60 && opaque(cover) < 150, "the cover layer: " + opaque(cover) + " opaque pixels");
    }

    @Test
    void theCoverHoldsOnlyGreysSoTheTintIsClean() throws Exception {
        BufferedImage cover = sprite("skill_book_cover");
        int lightest = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int argb = cover.getRGB(x, y);
                if ((argb >>> 24) == 0) continue;
                int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
                assertTrue(r == g && g == b, "cover pixel " + x + "," + y + " is not grey: " + Integer.toHexString(argb));
                assertTrue(r >= 90, "cover pixel " + x + "," + y + " is too dark to tint: " + r);
                lightest = Math.max(lightest, r);
            }
        }
        assertEquals(255, lightest, "the highlight is full white so the brightest tinted pixel is exactly the route's colour");
    }

    @Test
    void everySpSpriteExistsAndIsNotEmpty() throws Exception {
        assertTrue(opaque(sprite("sp_book")) > 60);
        for (SpKind k : SpKind.values()) {
            int n = opaque(sprite("sp_book_" + k.id()));
            assertTrue(n > 60 && n < 230, k.id() + ": " + n + " opaque pixels");
        }
    }

    @Test
    void theSkillBookModelNamesBothLayersInOrder() throws Exception {
        JsonObject model = json(ASSETS + "models/item/skill_book.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        JsonObject tex = model.getAsJsonObject("textures");
        assertEquals("nusmp:item/skill_book", tex.get("layer0").getAsString());
        assertEquals("nusmp:item/skill_book_cover", tex.get("layer1").getAsString(), "layer 1 is tint index 1, the one the client colours");
    }

    @Test
    void theSpBookModelHasOneOverridePerKindInTableOrder() throws Exception {
        JsonObject model = json(ASSETS + "models/item/sp_book.json");
        assertEquals("nusmp:item/sp_book", model.getAsJsonObject("textures").get("layer0").getAsString());
        JsonArray overrides = model.getAsJsonArray("overrides");
        SpKind[] kinds = SpKind.values();
        assertEquals(kinds.length, overrides.size(), "one override per kind");
        double previous = 0;
        for (int i = 0; i < kinds.length; i++) {
            JsonObject o = overrides.get(i).getAsJsonObject();
            double threshold = o.getAsJsonObject("predicate").get("nusmp:sp_kind").getAsDouble();
            assertEquals(kinds[i].modelValue() - 0.05, threshold, 1e-6, "threshold of " + kinds[i].id());
            assertTrue(threshold > previous, "thresholds ascend (the last matching override wins)");
            previous = threshold;
            assertEquals("nusmp:item/sp_book_" + kinds[i].id(), o.get("model").getAsString());
            JsonObject kindModel = json(ASSETS + "models/item/sp_book_" + kinds[i].id() + ".json");
            assertEquals("nusmp:item/sp_book_" + kinds[i].id(), kindModel.getAsJsonObject("textures").get("layer0").getAsString());
        }
    }

    @Test
    void theLootModifierIsDefinedAndListedAndTheOldEntriesStay() throws Exception {
        JsonObject def = json(DATA + "nusmp/loot_modifiers/skill_books.json");
        assertEquals("nusmp:skill_books", def.get("type").getAsString());
        assertTrue(def.has("conditions") && def.getAsJsonArray("conditions").size() == 0, "the decision is in the code: the conditions list is present and empty");
        JsonObject list = json(DATA + "neoforge/loot_modifiers/global_loot_modifiers.json");
        assertFalse(list.get("replace").getAsBoolean(), "the list must not replace other mods' modifiers");
        String entries = list.getAsJsonArray("entries").toString();
        assertTrue(entries.contains("\"nusmp:skill_books\""), "the modifier is not switched on");
        assertTrue(entries.contains("\"nusmp:relic_chests\"") && entries.contains("\"nusmp:gear_chests\""), "the two older entries must stay");
    }
}
