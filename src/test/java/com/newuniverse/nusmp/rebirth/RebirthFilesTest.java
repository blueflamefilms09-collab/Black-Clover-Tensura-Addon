package com.newuniverse.nusmp.rebirth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The shipped files of the two sigils: icons, models, lang, recipes, franchise map, creative tab, and the smoke prefix before the pinned tail. */
class RebirthFilesTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final List<String> IDS = List.of("sigil_of_rebirth", "sigil_of_the_second_book");

    private static JsonObject json(Path p) throws Exception { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void everySigilHasA16pxIconWithATransparentCornerAndAModel() throws Exception {
        for (String id : IDS) {
            File png = RES.resolve("assets/anime_multiverse/textures/item/" + id + ".png").toFile();
            assertTrue(png.isFile(), "icon " + id);
            BufferedImage img = ImageIO.read(png);
            assertEquals(16, img.getWidth());
            assertEquals(16, img.getHeight());
            assertEquals(0, img.getRGB(0, 0) >>> 24, "transparent corner");
            assertTrue((img.getRGB(8, 8) >>> 24) == 255, "an opaque centre");
            JsonObject m = json(RES.resolve("assets/anime_multiverse/models/item/" + id + ".json"));
            assertEquals("anime_multiverse:item/" + id, m.getAsJsonObject("textures").get("layer0").getAsString());
        }
    }

    @Test
    void langHasNameAndTooltips() throws Exception {
        JsonObject lang = json(RES.resolve("assets/anime_multiverse/lang/en_us.json"));
        for (String id : IDS) for (String k : new String[]{"", ".tip1", ".tip2"}) assertTrue(lang.has("item.anime_multiverse." + id + k), id + k);
    }

    @Test
    void recipesUseVanillaIngredientsOnlyAndNameTheSigil() throws Exception {
        for (String id : IDS) {
            JsonObject r = json(RES.resolve("data/anime_multiverse/recipe/" + id + ".json"));
            assertEquals("anime_multiverse:" + id, r.getAsJsonObject("result").get("id").getAsString());
            JsonArray ing = r.getAsJsonArray("ingredients");
            assertTrue(ing.size() >= 7);
            for (JsonElement e : ing) assertTrue(e.getAsJsonObject().get("item").getAsString().startsWith("minecraft:"), "vanilla ingredients only");
            assertTrue(ing.toString().contains("minecraft:nether_star"), id + " needs a nether star");
        }
        String second = Files.readString(RES.resolve("data/anime_multiverse/recipe/sigil_of_the_second_book.json"));
        assertEquals(2, second.split("minecraft:emerald_block", -1).length - 1, "two emerald blocks");
        String reb = Files.readString(RES.resolve("data/anime_multiverse/recipe/sigil_of_rebirth.json"));
        assertEquals(4, reb.split("minecraft:amethyst_shard", -1).length - 1, "four amethyst shards");
        assertTrue(reb.contains("minecraft:totem_of_undying") && reb.contains("minecraft:enchanted_golden_apple"));
    }

    @Test
    void franchiseMapCreativeTabAndSmokeListKnowTheSigils() throws Exception {
        JsonObject map = json(RES.resolve("data/anime_multiverse/relics/franchise_map.json")).getAsJsonObject("items");
        for (String id : IDS) assertEquals("black_clover", map.get(id).getAsString());
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java")).contains("RebirthItems.creativeItems()"));
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("RebirthBoot.init(modEventBus, modContainer)"));
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"));
        assertTrue(smoke.contains(",rebirth,"), "the rebirth prefix is in the smoke list");
        assertTrue(smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the pinned tail is intact");
    }
}
