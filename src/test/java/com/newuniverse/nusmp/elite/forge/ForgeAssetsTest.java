package com.newuniverse.nusmp.elite.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the four stations and three kits are made of many small files (a blockstate with a lit and an unlit model, two block models, an item model, a loot table, six
 * textures per tier, a crafting recipe) and one wrong path is a purple-and-black block in game. This test follows every reference: each blockstate model exists, each
 * model's textures exist as PNG files of 16x16, each item model has its parent or texture, each loot table drops its own item, each recipe names a result that is the
 * registered id, and all four station blocks are in the pickaxe tag.
 */
class ForgeAssetsTest {
    static final Path RES = Path.of("src/main/resources");
    static final Path ASSETS = RES.resolve("assets/nusmp");
    static final Path DATA = RES.resolve("data/nusmp");

    static JsonObject json(Path p) throws IOException {
        assertTrue(Files.exists(p), "missing file " + p);
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    static Path texture(String ref) {
        assertTrue(ref.startsWith("nusmp:"), "unexpected texture namespace " + ref);
        return ASSETS.resolve("textures/" + ref.substring("nusmp:".length()) + ".png");
    }

    static Path model(String ref) {
        assertTrue(ref.startsWith("nusmp:"), "unexpected model namespace " + ref);
        return ASSETS.resolve("models/" + ref.substring("nusmp:".length()) + ".json");
    }

    @Test
    void everyStationFileExistsAndPointsAtRealFiles() throws IOException {
        for (StationTier t : StationTier.values()) {
            String name = "forge_station_" + t.id();
            JsonObject state = json(ASSETS.resolve("blockstates/" + name + ".json")).getAsJsonObject("variants");
            assertEquals(2, state.size(), name + " has lit=false and lit=true");
            assertTrue(state.has("lit=false") && state.has("lit=true"), name + " variants");
            for (String key : new String[]{"lit=false", "lit=true"}) {
                String ref = state.getAsJsonObject(key).get("model").getAsString();
                JsonObject m = json(model(ref));
                JsonObject tex = m.getAsJsonObject("textures");
                for (String k : tex.keySet()) {
                    Path png = texture(tex.get(k).getAsString());
                    assertTrue(Files.exists(png), name + " " + key + " texture " + k + " -> " + png);
                }
                // every face's texture variable is declared
                for (JsonElement el : m.getAsJsonArray("elements")) {
                    for (var face : el.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                        String v = face.getValue().getAsJsonObject().get("texture").getAsString();
                        assertTrue(v.startsWith("#") && tex.has(v.substring(1)), name + ": face uses undeclared " + v);
                    }
                }
            }
            assertEquals("nusmp:block/" + name, json(ASSETS.resolve("models/item/" + name + ".json")).get("parent").getAsString());
            JsonObject loot = json(DATA.resolve("loot_table/blocks/" + name + ".json"));
            String dropped = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
            assertEquals("nusmp:" + name, dropped);
            if (t.prev() == null) {
                JsonObject r = json(DATA.resolve("recipe/" + name + ".json"));
                assertEquals("nusmp:" + name, r.getAsJsonObject("result").get("id").getAsString());
            } else {
                String kit = "forge_upgrade_kit_" + t.id();
                assertTrue(Files.exists(texture("nusmp:item/" + kit)), kit + " icon");
                assertEquals("nusmp:item/" + kit, json(ASSETS.resolve("models/item/" + kit + ".json")).getAsJsonObject("textures").get("layer0").getAsString());
                assertEquals("nusmp:" + kit, json(DATA.resolve("recipe/" + kit + ".json")).getAsJsonObject("result").get("id").getAsString());
            }
        }
    }

    @Test
    void texturesAre16x16Pngs() throws IOException {
        List<String> bad = new ArrayList<>();
        for (StationTier t : StationTier.values()) {
            for (String face : new String[]{"side", "side_lit", "plate", "top", "top_lit", "bottom"}) check(ASSETS.resolve("textures/block/forge_station_" + t.id() + "_" + face + ".png"), bad);
            if (t.prev() != null) check(ASSETS.resolve("textures/item/forge_upgrade_kit_" + t.id() + ".png"), bad);
        }
        assertTrue(bad.isEmpty(), "bad textures: " + bad);
    }

    private static void check(Path png, List<String> bad) throws IOException {
        byte[] b = Files.readAllBytes(png);
        boolean sig = b.length > 24 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        int w = ((b[16] & 0xFF) << 24) | ((b[17] & 0xFF) << 16) | ((b[18] & 0xFF) << 8) | (b[19] & 0xFF);
        int h = ((b[20] & 0xFF) << 24) | ((b[21] & 0xFF) << 16) | ((b[22] & 0xFF) << 8) | (b[23] & 0xFF);
        if (!sig || w != 16 || h != 16) bad.add(png + " is " + w + "x" + h);
    }

    @Test
    void theKitRecipesAreTheCostOfTheUpgradePath() throws IOException {
        // each kit's recipe uses the materials of its own tier and none of the next: the upgrade path gets dearer step by step
        String[][] must = {{"magisteel", "tensura:low_magisteel_ingot"}, {"mythril", "tensura:mithril_ingot"}, {"dragonforge", "tensura:adamantite_ingot"}};
        for (String[] m : must) {
            String text = Files.readString(DATA.resolve("recipe/forge_upgrade_kit_" + m[0] + ".json"));
            assertTrue(text.contains(m[1]), m[0] + " kit must use " + m[1]);
        }
        assertTrue(Files.readString(DATA.resolve("recipe/forge_upgrade_kit_dragonforge.json")).contains("tensura:hihiirokane_ingot"));
        assertTrue(Files.readString(DATA.resolve("recipe/forge_upgrade_kit_dragonforge.json")).contains("minecraft:nether_star"));
    }

    @Test
    void theStationsAreMineableWithAPickaxe() throws IOException {
        JsonObject tag = json(RES.resolve("data/minecraft/tags/block/mineable/pickaxe.json"));
        List<String> values = new ArrayList<>();
        for (JsonElement e : tag.getAsJsonArray("values")) values.add(e.getAsString());
        for (StationTier t : StationTier.values()) assertTrue(values.contains("nusmp:forge_station_" + t.id()), t.id() + " is not in the pickaxe tag");
        assertTrue(values.contains("nusmp:relic_forge"), "the existing entries must stay");
    }
}
