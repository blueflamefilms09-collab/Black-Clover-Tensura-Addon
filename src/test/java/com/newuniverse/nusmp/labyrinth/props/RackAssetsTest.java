package com.newuniverse.nusmp.labyrinth.props;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rack is drawn and filled by resource files no compiler checks. A missing model is the purple cube, a loot table naming an item that does not exist is an empty rack with an error in the
 * log. This reads the files (no game needed): the block state has the four facings and a model, the model has a texture that exists and elements inside the 0..32 range the game allows, the six
 * loot tables exist for the six families, name only vanilla items from the known list, and have positive weights; the recipe gives the rack; the lang fragment names the block.
 */
class RackAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/", DATA = "src/main/resources/data/nusmp/";

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void theBlockStateModelTextureAndItemModelExist() throws Exception {
        JsonObject state = json(ASSETS + "blockstates/weapon_rack.json").getAsJsonObject("variants");
        for (String f : new String[]{"south", "west", "north", "east"}) {
            assertTrue(state.has("facing=" + f), "variant for " + f);
            assertEquals("nusmp:block/weapon_rack", state.getAsJsonObject("facing=" + f).get("model").getAsString());
        }
        assertEquals(90, state.getAsJsonObject("facing=west").get("y").getAsInt());
        assertEquals(270, state.getAsJsonObject("facing=east").get("y").getAsInt());
        JsonObject model = json(ASSETS + "models/block/weapon_rack.json");
        assertTrue(new File(ASSETS + "textures/block/weapon_rack.png").isFile());
        assertEquals("nusmp:block/weapon_rack", model.getAsJsonObject("textures").get("t").getAsString());
        JsonArray els = model.getAsJsonArray("elements");
        assertTrue(els.size() >= 8);
        double top = 0;
        for (JsonElement e : els) {
            JsonObject o = e.getAsJsonObject();
            for (String k : new String[]{"from", "to"}) for (JsonElement c : o.getAsJsonArray(k)) assertTrue(c.getAsDouble() >= -16 && c.getAsDouble() <= 32, "inside the range the game allows");
            top = Math.max(top, o.getAsJsonArray("to").get(1).getAsDouble());
            assertEquals(6, o.getAsJsonObject("faces").size());
        }
        assertEquals(RackLayout.FRAME_HEIGHT * 16.0, top, 0.001, "the model is as tall as the layout says");
        assertEquals("nusmp:block/weapon_rack", json(ASSETS + "models/item/weapon_rack.json").get("parent").getAsString());
    }

    @Test
    void everyFamilyHasALootTableOfKnownVanillaItems() throws Exception {
        Set<String> vanilla = new HashSet<>(Files.readAllLines(new File("src/test/resources/mega/gear/vanilla_items.txt").toPath()));
        for (String fam : RackFamilies.ALL) {
            JsonObject table = json(DATA + "loot_table/" + RackFamilies.tablePath(fam) + ".json");
            assertEquals("minecraft:chest", table.get("type").getAsString());
            JsonObject pool = table.getAsJsonArray("pools").get(0).getAsJsonObject();
            JsonObject rolls = pool.getAsJsonObject("rolls");
            assertTrue(rolls.get("min").getAsInt() >= 1 && rolls.get("max").getAsInt() <= RackLayout.SLOTS, fam + ": 1..5 rolls so a rack is never overfull");
            JsonArray entries = pool.getAsJsonArray("entries");
            assertTrue(entries.size() >= 8, fam + " has variety");
            Set<String> seen = new HashSet<>();
            for (JsonElement e : entries) {
                JsonObject o = e.getAsJsonObject();
                String name = o.get("name").getAsString();
                assertTrue(vanilla.contains(name), fam + ": unknown item " + name);
                assertTrue(o.get("weight").getAsInt() > 0);
                assertTrue(seen.add(name), fam + ": " + name + " listed twice");
            }
        }
    }

    @Test
    void theRecipeTheDropTheTagAndTheLangFragmentAreThere() throws Exception {
        JsonObject recipe = json(DATA + "recipe/weapon_rack.json");
        assertEquals("nusmp:weapon_rack", recipe.getAsJsonObject("result").get("id").getAsString());
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        for (JsonElement row : pattern) for (char c : row.getAsString().toCharArray()) if (c != ' ') assertTrue(recipe.getAsJsonObject("key").has(String.valueOf(c)), "key " + c);
        JsonObject drop = json(DATA + "loot_table/blocks/weapon_rack.json");
        assertEquals("nusmp:weapon_rack", drop.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        JsonObject tag = json("src/main/resources/data/minecraft/tags/block/mineable/axe.json");
        boolean listed = false;
        for (JsonElement e : tag.getAsJsonArray("values")) listed |= e.getAsString().equals("nusmp:weapon_rack");
        assertTrue(listed);
        JsonObject lang = json("tools/lang/weapon_racks.json");
        assertEquals("Weapon Rack", lang.get("block.nusmp.weapon_rack").getAsString());
        assertFalse(lang.get("nusmp.weapon_rack.decor").getAsString().isEmpty());
    }
}
