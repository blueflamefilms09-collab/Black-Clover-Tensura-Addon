package com.newuniverse.nusmp.avatar.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two recipes of the Soul Forge update: the Genesis Core (new: until now it could only be given by command) and the Soul Forge (which needs a Genesis
 * Core and a pile of rare things). Checks the shape is a legal 3x3 pattern with every key used, every vanilla ingredient is a real 1.21 item, every ingredient
 * of this mod is registered in the source, and the economics the owner asked for hold (the core uses the eight named rarities; the forge uses a core).
 */
class ForgeRecipesTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/recipe");
    private static final Set<String> VANILLA = Set.of("minecraft:netherite_ingot", "minecraft:diamond_block", "minecraft:heart_of_the_sea", "minecraft:nether_star",
            "minecraft:totem_of_undying", "minecraft:echo_shard", "minecraft:enchanted_golden_apple", "minecraft:crying_obsidian");

    private static JsonObject recipe(String name) throws Exception { return JsonParser.parseString(Files.readString(DIR.resolve(name + ".json"))).getAsJsonObject(); }

    private static Set<String> ingredients(JsonObject r) {
        Set<String> out = new HashSet<>();
        for (var e : r.getAsJsonObject("key").entrySet()) out.add(e.getValue().getAsJsonObject().get("item").getAsString());
        return out;
    }

    private static boolean registered(String source, String name) throws Exception {
        return Files.readString(Path.of(source)).contains("register(\"" + name + "\"");
    }

    @Test
    void bothRecipesAreLegalShapedRecipes() throws Exception {
        for (String name : new String[]{"genesis_core", "soul_forge"}) {
            JsonObject r = recipe(name);
            assertEquals("minecraft:crafting_shaped", r.get("type").getAsString(), name);
            JsonArray pattern = r.getAsJsonArray("pattern");
            assertTrue(pattern.size() >= 1 && pattern.size() <= 3, name + " rows");
            Set<Character> used = new HashSet<>();
            for (int i = 0; i < pattern.size(); i++) {
                String row = pattern.get(i).getAsString();
                assertTrue(row.length() >= 1 && row.length() <= 3, name + " row width");
                for (char c : row.toCharArray()) if (c != ' ') used.add(c);
            }
            Set<Character> keys = new HashSet<>();
            for (String k : r.getAsJsonObject("key").keySet()) { assertEquals(1, k.length(), "key is one character"); keys.add(k.charAt(0)); }
            assertEquals(keys, used, name + ": every key is used and every symbol has a key");
            assertEquals("nusmp:" + name, r.getAsJsonObject("result").get("id").getAsString());
            assertEquals(1, r.getAsJsonObject("result").get("count").getAsInt());
        }
    }

    @Test
    void everyIngredientExists() throws Exception {
        for (String name : new String[]{"genesis_core", "soul_forge"}) {
            for (String id : ingredients(recipe(name))) {
                if (id.startsWith("minecraft:")) assertTrue(VANILLA.contains(id), name + " uses an item this test does not know: " + id);
                else if (id.equals("nusmp:genesis_core")) assertTrue(registered("src/main/java/com/newuniverse/nusmp/avatar/item/AvatarItems.java", "genesis_core"));
                else if (id.equals("nusmp:dark_rukh_shard")) assertTrue(registered("src/main/java/com/newuniverse/nusmp/relics/djinn/DjinnItems.java", "dark_rukh_shard"));
                else assertTrue(false, name + " uses an unknown mod item " + id);
            }
        }
    }

    @Test
    void theResultsAreRegistered() throws Exception {
        assertTrue(registered("src/main/java/com/newuniverse/nusmp/avatar/item/AvatarItems.java", "genesis_core"));
        String forge = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/avatar/item/SoulForgeBlocks.java"));
        assertTrue(forge.contains("register(\"soul_forge\""), "the block and its item are registered as soul_forge");
    }

    @Test
    void theGenesisCoreNeedsTheEightRarities() throws Exception {
        Set<String> need = Set.of("minecraft:nether_star", "minecraft:heart_of_the_sea", "minecraft:totem_of_undying", "minecraft:netherite_ingot", "minecraft:diamond_block",
                "minecraft:enchanted_golden_apple", "minecraft:echo_shard", "nusmp:dark_rukh_shard");
        assertEquals(need, ingredients(recipe("genesis_core")));
    }

    @Test
    void theSoulForgeNeedsAGenesisCoreAndNothingElseCheap() throws Exception {
        Set<String> got = ingredients(recipe("soul_forge"));
        assertTrue(got.contains("nusmp:genesis_core"));
        assertTrue(got.contains("minecraft:nether_star"));
        assertTrue(got.contains("minecraft:netherite_ingot"));
        assertTrue(got.contains("minecraft:enchanted_golden_apple"));
        for (String id : got) assertFalse(id.equals("minecraft:iron_ingot") || id.equals("minecraft:gold_ingot") || id.equals("minecraft:cobblestone"), "no cheap filler: " + id);
    }

    @Test
    void thereIsNoCircularRecipe() throws Exception {
        // the Genesis Core must not need the Soul Forge or itself, or neither could ever be made
        assertFalse(ingredients(recipe("genesis_core")).contains("nusmp:soul_forge"));
        assertFalse(ingredients(recipe("genesis_core")).contains("nusmp:genesis_core"));
    }
}
