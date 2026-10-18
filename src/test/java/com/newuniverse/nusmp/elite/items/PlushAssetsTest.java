package com.newuniverse.nusmp.elite.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.items.plush.PlushKinds;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Why: a plushie is a block id, a model, a texture, a blockstate, an item model, a loot table and a name, produced by a generator from the Java list; a missing piece shows as a purple-black cube
 * in the game, so every plushie is checked here: the registry list is clean and has the owner's 54 names; each plushie has a model (cube elements inside the texture's UV space, six faces each, a
 * texture reference that names ITS texture), a 64x64 PNG whose every face rectangle is mostly opaque (no face points at an empty patch), a blockstate with the four facings, an item model, a
 * loot table that drops itself and a lang key in the fragment. It also keeps the whole set small (each texture a few KB).
 */
class PlushAssetsTest {
    private static final List<String> OWNER_LIST = List.of("Allay", "Armadillo", "Axolotl", "Bat", "Bee", "Blaze", "Camel", "Cat", "Chicken", "Cow", "Creeper", "Dolphin", "Donkey", "Drowned",
            "Elder Guardian", "Ender Dragon", "Enderman", "Evoker", "Fox", "Frog", "Ghast", "Glow Squid", "Goat", "Guardian", "Hoglin", "Horse", "Iron Golem", "Llama", "Magma Cube", "Mooshroom",
            "Ocelot", "Panda", "Parrot", "Phantom", "Pig", "Piglin", "Polar Bear", "Rabbit", "Sheep", "Shulker", "Skeleton", "Slime", "Sniffer", "Spider", "Squid", "Strider", "Turtle", "Vex",
            "Villager", "Warden", "Witch", "Wither", "Wolf", "Zombie");

    static JsonObject json(String path) throws Exception {
        try (InputStream in = PlushAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static BufferedImage png(String path) throws Exception {
        try (InputStream in = PlushAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return ImageIO.read(in);
        }
    }

    @Test
    void theListIsCleanAndHasTheOwnersNames() {
        assertTrue(PlushKinds.wellFormed());
        assertTrue(PlushKinds.ALL.size() >= 40, "the owner asked for 40+ plushies");
        Set<String> have = new java.util.HashSet<>();
        for (PlushKinds.Kind k : PlushKinds.ALL) have.add(k.name());
        for (String n : OWNER_LIST) assertTrue(have.contains(n), "the owner's list names " + n);
        assertEquals(PlushKinds.ALL.size(), EliteIds.plushies().size());
    }

    @Test
    void everyPlushHasItsModelTextureBlockstateItemModelLootTableAndName() throws Exception {
        int detailed = 0;
        for (PlushKinds.Kind k : PlushKinds.ALL) {
            String id = k.id();
            JsonObject model = json("/assets/nusmp/models/block/plush/" + id + ".json");
            assertEquals("nusmp:block/plush/" + id, model.getAsJsonObject("textures").get("0").getAsString(), id + ": the model's texture");
            assertEquals("nusmp:block/plush/" + id, model.getAsJsonObject("textures").get("particle").getAsString(), id + ": the particle texture");
            JsonArray els = model.getAsJsonArray("elements");
            assertTrue(els.size() >= 2, id + " has only " + els.size() + " cube");
            if (els.size() >= 6) detailed++;
            BufferedImage tex = png("/assets/nusmp/textures/block/plush/" + id + ".png");
            assertEquals(64, tex.getWidth(), id + " texture width");
            assertEquals(64, tex.getHeight(), id + " texture height");
            for (JsonElement el : els) {
                JsonObject e = el.getAsJsonObject();
                JsonObject faces = e.getAsJsonObject("faces");
                assertEquals(6, faces.size(), id + "/" + e.get("name") + ": six faces");
                for (String f : faces.keySet()) {
                    JsonArray uv = faces.getAsJsonObject(f).getAsJsonArray("uv");
                    double u0 = uv.get(0).getAsDouble() * 4, v0 = uv.get(1).getAsDouble() * 4, u1 = uv.get(2).getAsDouble() * 4, v1 = uv.get(3).getAsDouble() * 4;
                    assertTrue(u0 >= 0 && v0 >= 0 && u1 <= 64.001 && v1 <= 64.001 && u1 > u0 && v1 > v0, id + "/" + e.get("name") + "/" + f + ": uv outside the texture");
                    int opaque = 0, total = 0;
                    for (int y = (int) Math.floor(v0 + 0.01); y < (int) Math.ceil(v1 - 0.01); y++)
                        for (int x = (int) Math.floor(u0 + 0.01); x < (int) Math.ceil(u1 - 0.01); x++) {
                            total++;
                            if ((tex.getRGB(x, y) >>> 24) > 100) opaque++;
                        }
                    assertTrue(total > 0 && opaque >= total * 0.9, id + "/" + e.get("name") + "/" + f + ": the face points at an empty patch of the texture (" + opaque + "/" + total + ")");
                }
                JsonArray from = e.getAsJsonArray("from"), to = e.getAsJsonArray("to");
                for (int a = 0; a < 3; a++) assertTrue(to.get(a).getAsDouble() > from.get(a).getAsDouble(), id + ": a cube with no size");
                if (e.has("rotation")) {
                    double ang = e.getAsJsonObject("rotation").get("angle").getAsDouble();
                    assertTrue(ang == 22.5 || ang == -22.5 || ang == 45 || ang == -45 || ang == 0, id + ": " + ang + " is not an angle the model format allows");
                }
            }
            JsonObject st = json("/assets/nusmp/blockstates/plush_" + id + ".json").getAsJsonObject("variants");
            assertEquals(4, st.size(), id + ": four facings");
            for (String f : List.of("north", "east", "south", "west")) assertTrue(st.has("facing=" + f), id + ": facing " + f);
            assertEquals("nusmp:block/plush/" + id, json("/assets/nusmp/models/item/plush_" + id + ".json").get("parent").getAsString());
            JsonObject loot = json("/data/nusmp/loot_table/blocks/plush_" + id + ".json");
            assertTrue(loot.toString().contains("\"nusmp:plush_" + id + "\""), id + ": the loot table must drop the plushie");
        }
        assertTrue(detailed >= PlushKinds.ALL.size() - 4, "only " + detailed + " plushies have the six or more cubes (head, body, limbs, features) of a distinct plush; the cube-bodied mobs (slime, magma cube, shulker) are the exceptions");
    }

    @Test
    void everyPlushHasALangKey() throws Exception {
        JsonObject lang = com.google.gson.JsonParser.parseString(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("tools/elite/items_lang.json")), StandardCharsets.UTF_8)).getAsJsonObject();
        for (PlushKinds.Kind k : PlushKinds.ALL) {
            String key = "block.nusmp.plush_" + k.id();
            assertTrue(lang.has(key), "lang fragment lacks " + key);
            assertEquals(k.name() + " Plushie", lang.get(key).getAsString());
        }
    }

    @Test
    void theTexturesStayTiny() throws Exception {
        long total = 0;
        for (PlushKinds.Kind k : PlushKinds.ALL) {
            try (InputStream in = PlushAssetsTest.class.getResourceAsStream("/assets/nusmp/textures/block/plush/" + k.id() + ".png")) {
                int n = in.readAllBytes().length;
                assertTrue(n < 8000, k.id() + " texture is " + n + " bytes");
                total += n;
            }
        }
        assertTrue(total < 250_000, "all plushie textures together are " + total + " bytes");
    }
}
