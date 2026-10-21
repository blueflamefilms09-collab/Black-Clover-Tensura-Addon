package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The files of the backpack and of the Luck loot: every model, texture, blockstate variant, loot table and recipe exists and says the right thing; the global modifier is switched on
 * beside the older ones; the lang fragment holds every text the code looks up; nothing in the new code touches the food bar or names a tool.
 */
class BackpackAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp");

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void everyTierHasItsTextureItsModelsAndItsBlockstateVariants() throws IOException {
        JsonObject states = json(ASSETS.resolve("blockstates/backpack.json")).getAsJsonObject("variants");
        assertEquals(20, states.size(), "four facings times five tiers");
        for (BackpackTier t : BackpackTier.values()) {
            assertTrue(Files.exists(ASSETS.resolve("textures/block/backpack_" + t.id() + ".png")), t.id() + " texture");
            JsonObject block = json(ASSETS.resolve("models/block/backpack_" + t.id() + ".json"));
            assertTrue(block.getAsJsonArray("elements").size() >= 6, t.id() + " is a real 3D model made of cuboids");
            assertEquals("nusmp:block/backpack_" + t.id(), block.getAsJsonObject("textures").get("all").getAsString());
            assertEquals("nusmp:block/backpack_" + t.id(), json(ASSETS.resolve("models/item/backpack_" + t.id() + ".json")).get("parent").getAsString());
            for (String f : new String[]{"north", "east", "south", "west"}) {
                String key = "facing=" + f + ",tier=" + t.ordinal();
                assertTrue(states.has(key), "variant " + key);
                assertEquals("nusmp:block/backpack_" + t.id(), states.getAsJsonObject(key).get("model").getAsString());
            }
            if (t.upgradeItemId() != null) {
                assertTrue(Files.exists(ASSETS.resolve("textures/item/" + t.upgradeItemId() + ".png")), t.upgradeItemId() + " sprite");
                assertEquals("nusmp:item/" + t.upgradeItemId(), json(ASSETS.resolve("models/item/" + t.upgradeItemId() + ".json")).getAsJsonObject("textures").get("layer0").getAsString());
            }
        }
    }

    @Test
    void everyFaceOfTheModelHasUvInsideTheAtlas() throws IOException {
        for (BackpackTier t : BackpackTier.values()) {
            for (JsonElement e : json(ASSETS.resolve("models/block/backpack_" + t.id() + ".json")).getAsJsonArray("elements")) {
                JsonObject faces = e.getAsJsonObject().getAsJsonObject("faces");
                for (String f : faces.keySet()) {
                    JsonArray uv = faces.getAsJsonObject(f).getAsJsonArray("uv");
                    for (int i = 0; i < 4; i++) assertTrue(uv.get(i).getAsDouble() >= 0.0 && uv.get(i).getAsDouble() <= 16.0, t.id() + " uv out of range");
                    assertTrue(uv.get(2).getAsDouble() > uv.get(0).getAsDouble() && uv.get(3).getAsDouble() > uv.get(1).getAsDouble(), t.id() + " empty uv");
                }
            }
        }
    }

    @Test
    void theItemModelPicksTheTierByTheClientProperty() throws IOException {
        JsonObject main = json(ASSETS.resolve("models/item/backpack.json"));
        JsonArray overrides = main.getAsJsonArray("overrides");
        assertEquals(4, overrides.size(), "iron, gold, diamond and netherite override the leather default");
        for (int i = 0; i < overrides.size(); i++) {
            JsonObject o = overrides.get(i).getAsJsonObject();
            assertEquals(i + 1, o.getAsJsonObject("predicate").get("nusmp:backpack_tier").getAsInt());
            assertEquals("nusmp:item/backpack_" + BackpackTier.of(i + 1).id(), o.get("model").getAsString());
        }
        String client = Files.readString(JAVA.resolve("client/loot/LootClient.java"));
        assertTrue(client.contains("\"backpack_tier\""), "the property the overrides ask for is registered by the client");
        assertTrue(client.contains("RegisterMenuScreensEvent") && client.contains("BackpackScreen::new"), "the window is registered");
    }

    @Test
    void theLootTableCarriesTheContentsAndTheTier() throws IOException {
        String t = Files.readString(DATA.resolve("nusmp/loot_table/blocks/backpack.json"));
        assertTrue(t.contains("copy_components") && t.contains("block_entity"), "components are copied from the block entity");
        for (String c : new String[]{"minecraft:container", "nusmp:backpack_tier"}) assertTrue(t.contains(c), c);
        assertTrue(t.contains("nusmp:backpack"));
    }

    @Test
    void everyRecipeNamesRealItemsAndTheUpgradeRecipeIsSpecial() throws IOException {
        assertEquals("nusmp:backpack_upgrade", json(DATA.resolve("nusmp/recipe/backpack_upgrade.json")).get("type").getAsString());
        assertTrue(Files.readString(JAVA.resolve("labyrinth/loot/backpack/BackpackRegistry.java")).contains("register(\"backpack_upgrade\""), "the serializer is registered under the id the JSON names");
        List<String> made = new ArrayList<>(List.of("nusmp:backpack"));
        for (BackpackTier t : BackpackTier.values()) if (t.upgradeItemId() != null) made.add("nusmp:" + t.upgradeItemId());
        for (String id : made) {
            Path p = DATA.resolve("nusmp/recipe/" + id.substring(6) + ".json");
            assertTrue(Files.exists(p), "recipe for " + id);
            JsonObject r = json(p);
            assertEquals(id, r.getAsJsonObject("result").get("id").getAsString());
            JsonObject key = r.getAsJsonObject("key");
            List<String> rows = new ArrayList<>();
            r.getAsJsonArray("pattern").forEach(x -> rows.add(x.getAsString()));
            assertEquals(3, rows.size());
            for (String row : rows) {
                assertEquals(3, row.length());
                for (char c : row.toCharArray()) if (c != ' ') assertTrue(key.has(String.valueOf(c)), id + " uses " + c + " with no key");
            }
            for (String k : key.keySet()) assertTrue(key.getAsJsonObject(k).get("item").getAsString().startsWith("minecraft:"), "ingredients are vanilla items: " + k);
        }
    }

    @Test
    void theLuckLootModifierIsSwitchedOnBesideTheOlderOnes() throws IOException {
        JsonObject list = json(DATA.resolve("neoforge/loot_modifiers/global_loot_modifiers.json"));
        assertFalse(list.get("replace").getAsBoolean());
        String entries = list.getAsJsonArray("entries").toString();
        for (String e : new String[]{"nusmp:relic_chests", "nusmp:gear_chests", "nusmp:skill_books", "nusmp:rift_books", "nusmp:hub_coins", "nusmp:celestial_keys", "nusmp:anime_loot", "nusmp:luck_loot"})
            assertTrue(entries.contains("\"" + e + "\""), e + " is listed");
        JsonObject mod = json(DATA.resolve("nusmp/loot_modifiers/luck_loot.json"));
        assertEquals("nusmp:luck_loot", mod.get("type").getAsString());
        assertEquals(0, mod.getAsJsonArray("conditions").size());
        assertTrue(Files.readString(JAVA.resolve("labyrinth/loot/LuckLootModifier.java")).contains("MODIFIERS.register(\"luck_loot\""));
    }

    @Test
    void theLangFragmentHoldsEveryTextTheCodeLooksUp() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/luck_loot_backpack.json"));
        List<String> need = new ArrayList<>(List.of("block.nusmp.backpack", "item.nusmp.backpack", "container.nusmp.backpack", "item.nusmp.backpack.filled", "item.nusmp.backpack.lore",
                "item.nusmp.backpack_upgrade.tier", "item.nusmp.backpack_upgrade.lore", "itemGroup.nusmp.dungeon_loot"));
        for (BackpackTier t : BackpackTier.values()) {
            need.add("item.nusmp.backpack.tier." + t.id());
            if (t.upgradeItemId() != null) need.add("item.nusmp." + t.upgradeItemId());
        }
        for (String k : need) if (!k.equals("itemGroup.nusmp.dungeon_loot")) assertTrue(lang.has(k), "missing " + k);
        Pattern banned = Pattern.compile("\\b(claude|anthropic|gemini|sonnet|opus|chatgpt)\\b", Pattern.CASE_INSENSITIVE);
        for (String k : lang.keySet()) assertFalse(banned.matcher(lang.get(k).getAsString()).find(), k + " names a tool");
    }

    @Test
    void nothingNewTouchesTheFoodBarOrNamesATool() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(JAVA.resolve("labyrinth/loot"))) { files.addAll(s.filter(f -> f.toString().endsWith(".java")).toList()); }
        files.add(JAVA.resolve("client/loot/BackpackScreen.java"));
        files.add(Path.of("tools/gen_backpack_assets.py"));
        Pattern banned = Pattern.compile("\\b(claude|anthropic|gemini|sonnet|opus|chatgpt)\\b", Pattern.CASE_INSENSITIVE);
        for (Path p : files) {
            String text = Files.readString(p);
            assertFalse(banned.matcher(text).find(), p + " names a tool");
            if (!p.getFileName().toString().equals("LootSelfTests.java")) for (String token : new String[]{"addExhaustion", "causeFoodExhaustion", "setFoodLevel", "setSaturation", "FoodData"})
                assertFalse(text.contains(token), p + " uses " + token);
        }
    }
}
