package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.awt.image.BufferedImage;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Why: an item whose model or icon is missing shows as a purple-black cube in the player's hand and inventory, and nothing else notices until someone plays. The
 * nineteen gem items each need a flat model that points at a 16 x 16 icon; the icons are the one thing the player sees of a Seal in a chest, so seventeen of
 * them must be told apart, each family must carry its own tint (amber for the Seven Star Weapons, aqua for the others), and the Holy Calling Gem must really
 * show the four jewel colours. All of it is read from the source tree, so a new weapon appended to the catalogue fails here until its Seal has a model and an icon.
 */
class GemAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");

    private static BufferedImage icon(String id) throws Exception {
        Path file = ASSETS.resolve("textures/item/" + id + ".png");
        assertTrue(Files.exists(file), "missing icon " + file);
        BufferedImage im = ImageIO.read(file.toFile());
        assertEquals(16, im.getWidth(), id + " width");
        assertEquals(16, im.getHeight(), id + " height");
        return im;
    }

    private static int[] pixels(BufferedImage im) {
        int[] out = new int[16 * 16];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) out[y * 16 + x] = im.getRGB(x, y);
        return out;
    }

    private static int opaque(BufferedImage im) {
        int n = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (((im.getRGB(x, y) >>> 24) & 255) > 0) n++;
        return n;
    }

    @Test
    void everyItemHasAFlatModelThatPointsAtItsIcon() throws Exception {
        for (String id : GemCatalog.allItemIds()) {
            Path file = ASSETS.resolve("models/item/" + id + ".json");
            assertTrue(Files.exists(file), "missing model " + file);
            JsonObject m;
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                m = JsonParser.parseReader(r).getAsJsonObject();
            }
            assertEquals("minecraft:item/generated", m.get("parent").getAsString(), id);
            assertEquals("nusmp:item/" + id, m.getAsJsonObject("textures").get("layer0").getAsString(), id);
            assertFalse(m.has("overrides"), id + ": one item, one look (the Seals are items of their own)");
        }
    }

    @Test
    void everyIconIsA16x16SpriteWithHardEdges() throws Exception {
        for (String id : GemCatalog.allItemIds()) {
            BufferedImage im = icon(id);
            int solid = opaque(im);
            assertTrue(solid >= 40 && solid <= 240, id + " draws " + solid + " pixels");
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int a = (im.getRGB(x, y) >>> 24) & 255;
                    assertTrue(a == 0 || a == 255, id + " has a half-transparent pixel at " + x + "," + y);
                }
        }
    }

    @Test
    void theSeventeenSealsAndTheTwoOtherItemsAreAllDifferentPictures() throws Exception {
        Map<String, String> seen = new HashMap<>();
        for (String id : GemCatalog.allItemIds()) {
            int[] px = pixels(icon(id));
            String key = java.util.Arrays.toString(px);
            String twin = seen.put(key, id);
            assertTrue(twin == null, id + " has the same icon as " + twin);
        }
    }

    @Test
    void theSealsOfEachFamilyCarryTheFamilysTint() throws Exception {
        for (HeroKind k : GemCatalog.sealKinds()) {
            BufferedImage im = icon(GemCatalog.sealId(k));
            int warm = 0, cool = 0;
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int argb = im.getRGB(x, y);
                    if (((argb >>> 24) & 255) == 0) continue;
                    int r = (argb >> 16) & 255, b = argb & 255;
                    if (r > b + 40) warm++;
                    if (b > r + 30) cool++;
                }
            if (k.heroClass() == com.newuniverse.nusmp.heroweapon.HeroClass.SEVEN_STAR) assertTrue(warm > cool, k + " is a Seven Star Weapon: its seal should be amber (warm " + warm + ", cool " + cool + ")");
            else assertTrue(cool > warm, k + " is a Vassal Weapon: its seal should be aqua (warm " + warm + ", cool " + cool + ")");
        }
    }

    @Test
    void theHolyCallingGemShowsAllFourJewelColours() throws Exception {
        BufferedImage im = icon(GemCatalog.HOLY_ID);
        int green = 0, blue = 0, red = 0, yellow = 0;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int argb = im.getRGB(x, y);
                if (((argb >>> 24) & 255) == 0) continue;
                int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
                if (g > r + 30 && g > b + 30) green++;
                if (b > r + 40 && b > g + 10) blue++;
                if (r > g + 60 && r > b + 60) red++;
                if (r > 150 && g > 120 && b < 110) yellow++;
            }
        assertTrue(green >= 10, "green (the Shield): " + green);
        assertTrue(blue >= 10, "blue (the Sword): " + blue);
        assertTrue(red >= 10, "red (the Spear): " + red);
        assertTrue(yellow >= 10, "yellow (the Bow): " + yellow);
    }

    @Test
    void noStrayFilesOfOurPrefixWithoutAnItem() throws Exception {
        List<String> stray = new ArrayList<>();
        try (var stream = Files.list(ASSETS.resolve("textures/item"))) {
            stream.map(p -> p.getFileName().toString()).filter(n -> n.startsWith(GemCatalog.SEAL_PREFIX) && n.endsWith(".png")).forEach(n -> {
                String id = n.substring(0, n.length() - 4);
                if (!GemCatalog.allItemIds().contains(id)) stray.add(n);
            });
        }
        assertTrue(stray.isEmpty(), "icons without an item: " + stray);
    }
}
