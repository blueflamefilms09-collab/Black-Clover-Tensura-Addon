package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the survival route to a hero weapon is nineteen recipes, and three things can go wrong with them without any crash: an ingredient id that is not a
 * real item (the recipe is skipped at load and the route silently has a hole), two Seals with the same ingredients (one of them can never be made), and a
 * recipe file that no longer says what the table says. The table in {@link GemRecipes} is checked here against the game's own item list (a fixture generated
 * from the game's {@code Items} class), against the nine slots of the crafting grid and against itself, and then the shipped JSON is read back and compared
 * with it, so {@code tools/heroweapon/gen_gem_recipes.py} and the table cannot drift.
 */
class GemRecipesTest {
    private static final Path RECIPES = Path.of("src/main/resources/data/nusmp/recipe");
    private static final Path VANILLA = Path.of("src/test/resources/heroweapon/vanilla_item_ids.txt");

    private static Set<String> vanillaIds() throws Exception {
        Set<String> ids = new HashSet<>();
        for (String line : Files.readAllLines(VANILLA, StandardCharsets.UTF_8)) if (!line.isBlank()) ids.add("minecraft:" + line.trim());
        return ids;
    }

    private static JsonObject read(String name) throws Exception {
        try (Reader r = Files.newBufferedReader(RECIPES.resolve(name + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static List<String> ingredientIds(JsonArray ingredients) {
        List<String> ids = new ArrayList<>();
        for (JsonElement e : ingredients) {
            JsonObject o = e.getAsJsonObject();
            assertTrue(o.has("item") && o.size() == 1, "an ingredient is exactly one item id: " + o);
            ids.add(o.get("item").getAsString());
        }
        ids.sort(null);
        return ids;
    }

    @Test
    void everyWeaponWithASealHasARecipeAndNoOtherDoes() {
        List<GemRecipes.Seal> seals = GemRecipes.seals();
        assertEquals(17, seals.size());
        List<HeroKind> kinds = GemCatalog.sealKinds();
        for (int i = 0; i < kinds.size(); i++) {
            assertEquals(kinds.get(i), seals.get(i).kind(), "catalogue order at " + i);
            assertNotNull(GemRecipes.sealOf(kinds.get(i)));
            assertFalse(seals.get(i).why().isBlank(), "a recipe states its reason: " + kinds.get(i));
        }
        for (HeroKind k : HeroKind.values()) if (k.isHoly()) assertTrue(GemRecipes.sealOf(k) == null, k + " is Holy and has no Seal recipe");
    }

    @Test
    void noTwoSealsShareTheirIngredients() {
        Set<List<String>> seen = new HashSet<>();
        for (GemRecipes.Seal s : GemRecipes.seals()) assertTrue(seen.add(s.slotIds()), "the Seal of " + s.kind() + " has the same ingredients as another Seal");
        assertTrue(seen.add(GemRecipes.fragmentSlotIds()), "the fragment recipe equals a Seal recipe");
        assertTrue(seen.add(new ArrayList<>(GemRecipes.holySlotIds())), "the Holy gem equals another recipe");
    }

    @Test
    void everyRecipeFitsTheGridAndNamesRealIngredients() throws Exception {
        Set<String> vanilla = vanillaIds();
        assertTrue(vanilla.size() > 1000 && vanilla.contains("minecraft:iron_block"), "the fixture of vanilla item ids is the whole list");
        for (GemRecipes.Seal s : GemRecipes.seals()) {
            assertTrue(s.slots() >= 2 && s.slots() <= 9, s.kind() + " needs " + s.slots() + " slots");
            assertTrue(s.slotIds().contains(GemRecipes.FRAGMENT_ITEM), s.kind() + " needs the Gem Fragment");
            for (GemRecipes.Part p : s.parts()) {
                assertTrue(p.count() >= 1 && p.count() <= 8, s.kind() + ": " + p);
                assertTrue(vanilla.contains(p.item()), s.kind() + " asks for " + p.item() + ", which is not a vanilla item");
            }
        }
        assertEquals(9, GemRecipes.holySlotIds().size());
        for (String id : GemRecipes.usedVanillaIds()) assertTrue(vanilla.contains(id), id + " is not a vanilla item");
        assertTrue(GemRecipes.usedVanillaIds().size() >= 30, "the table really uses the vanilla items it lists");
    }

    @Test
    void theHolyGemShowsTheFourJewelColoursRoundANetherStar() {
        assertEquals(List.of("ENL", "NSN", "RNG"), GemRecipes.HOLY_PATTERN);
        assertEquals("minecraft:nether_star", GemRecipes.HOLY_KEYS.get('S'));
        List<String> cells = GemRecipes.holySlotIds();
        assertEquals("minecraft:emerald_block", cells.get(0), "top left: green, the Shield");
        assertEquals("minecraft:lapis_block", cells.get(2), "top right: blue, the Sword");
        assertEquals("minecraft:redstone_block", cells.get(6), "bottom left: red, the Spear");
        assertEquals("minecraft:gold_block", cells.get(8), "bottom right: yellow, the Bow");
        assertEquals(4, cells.stream().filter("minecraft:netherite_ingot"::equals).count());
        assertEquals(1, cells.stream().filter("minecraft:nether_star"::equals).count());
    }

    @Test
    void theShippedJsonIsTheTable() throws Exception {
        // the Holy Calling Gem
        JsonObject holy = read(GemCatalog.HOLY_ID);
        assertEquals("minecraft:crafting_shaped", holy.get("type").getAsString());
        JsonArray rows = holy.getAsJsonArray("pattern");
        assertEquals(GemRecipes.HOLY_PATTERN.size(), rows.size());
        for (int i = 0; i < rows.size(); i++) assertEquals(GemRecipes.HOLY_PATTERN.get(i), rows.get(i).getAsString());
        JsonObject key = holy.getAsJsonObject("key");
        Set<Character> used = new HashSet<>();
        for (String row : GemRecipes.HOLY_PATTERN) for (char c : row.toCharArray()) used.add(c);
        assertEquals(used.size(), key.size(), "the key defines exactly the symbols the pattern uses");
        for (char c : used) assertEquals(GemRecipes.HOLY_KEYS.get(c), key.getAsJsonObject(String.valueOf(c)).get("item").getAsString());
        assertEquals("nusmp:" + GemCatalog.HOLY_ID, holy.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, holy.getAsJsonObject("result").get("count").getAsInt());

        // the fragment
        JsonObject fragment = read(GemCatalog.FRAGMENT_ID);
        assertEquals("minecraft:crafting_shapeless", fragment.get("type").getAsString());
        assertEquals(GemRecipes.fragmentSlotIds(), ingredientIds(fragment.getAsJsonArray("ingredients")));
        assertEquals("nusmp:" + GemCatalog.FRAGMENT_ID, fragment.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, fragment.getAsJsonObject("result").get("count").getAsInt());

        // the seventeen Seals
        for (GemRecipes.Seal s : GemRecipes.seals()) {
            String id = GemCatalog.sealId(s.kind());
            JsonObject r = read(id);
            assertEquals("minecraft:crafting_shapeless", r.get("type").getAsString(), id);
            assertEquals(s.slotIds(), ingredientIds(r.getAsJsonArray("ingredients")), id + " ingredients");
            assertEquals("nusmp:" + id, r.getAsJsonObject("result").get("id").getAsString(), id + " result");
            assertEquals(1, r.getAsJsonObject("result").get("count").getAsInt(), id + " count");
            assertFalse(r.getAsJsonObject("result").has("components"), id + ": a Seal is an item of its own, not a component");
        }
    }

    @Test
    void thereAreExactlyNineteenRecipeFilesOfOurs() throws Exception {
        Set<String> expected = new HashSet<>();
        for (String id : GemCatalog.allItemIds()) expected.add(id + ".json");
        Set<String> found = new HashSet<>();
        try (var stream = Files.list(RECIPES)) {
            stream.map(p -> p.getFileName().toString())
                    .filter(n -> n.equals(GemCatalog.HOLY_ID + ".json") || n.equals(GemCatalog.FRAGMENT_ID + ".json") || n.startsWith(GemCatalog.SEAL_PREFIX))
                    .forEach(found::add);
        }
        assertEquals(expected, found);
    }
}
