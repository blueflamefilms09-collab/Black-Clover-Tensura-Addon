package com.newuniverse.nusmp.elite.hub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a data-driven system is only as good as its files, and the Hub is wired through data in four places that no compiler checks: the global loot modifier list (a modifier that is not listed never
 * runs), the item models and textures (a missing one is a purple cube in the player's hand), the lang fragment (a message key used in code with no English text is a raw key on screen) and the
 * three existing drop listeners that must ask the currency seam (so the stone drops can be replaced by coins). Each is read from the source tree here.
 */
class HubDataFilesTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel)); }

    @Test
    void theCoinLootModifierIsListedAndHasItsJson() throws IOException {
        JsonObject list = JsonParser.parseString(read("resources/data/neoforge/loot_modifiers/global_loot_modifiers.json")).getAsJsonObject();
        Set<String> entries = new HashSet<>();
        for (JsonElement e : list.getAsJsonArray("entries")) entries.add(e.getAsString());
        assertTrue(entries.contains("nusmp:hub_coins"), "the global loot modifier list must name nusmp:hub_coins");
        for (String old : new String[] {"nusmp:relic_chests", "nusmp:gear_chests", "nusmp:skill_books", "nusmp:rift_books"}) assertTrue(entries.contains(old), "the old modifier " + old + " is still listed (add the new, keep the old)");
        JsonObject mod = JsonParser.parseString(read("resources/data/nusmp/loot_modifiers/hub_coins.json")).getAsJsonObject();
        assertTrue(mod.get("type").getAsString().equals("nusmp:hub_coins"));
        assertTrue(read("java/com/newuniverse/nusmp/elite/hub/CoinLootModifier.java").contains("MODIFIERS.register(\"hub_coins\""), "the codec is registered under the id the JSON names");
    }

    @Test
    void theItemModelsAndTexturesExist() throws IOException {
        for (String m : new String[] {"dungeon_map", "dungeon_map_2d", "dungeon_map_3d", "quest_giver_npc_spawn_egg", "merchant_npc_spawn_egg"}) {
            JsonObject o = JsonParser.parseString(read("resources/assets/nusmp/models/item/" + m + ".json")).getAsJsonObject();
            assertTrue(o.size() > 0, m);
        }
        for (String t : new String[] {"item/dungeon_map", "entity/hub/quest_giver_npc", "entity/hub/merchant_npc"}) assertTrue(Files.exists(MAIN.resolve("resources/assets/nusmp/textures/" + t + ".png")), t);
        JsonObject threeD = JsonParser.parseString(read("resources/assets/nusmp/models/item/dungeon_map_3d.json")).getAsJsonObject();
        JsonArray elements = threeD.getAsJsonArray("elements");
        assertTrue(elements.size() >= 5, "the held map is a real 3D model made of several cuboids");
        assertTrue(read("resources/assets/nusmp/models/item/dungeon_map.json").contains("separate_transforms"), "3D in the hand, the sprite as the icon");
    }

    @Test
    void everyMessageKeyUsedInCodeHasEnglishText() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/elite/hub_lang.json"))).getAsJsonObject();
        Pattern key = Pattern.compile("\"((?:message|item|entity|key)\\.(?:categories\\.)?nusmp\\.[a-z0-9_.]+)\"");
        Set<String> used = new HashSet<>();
        try (Stream<Path> files = Stream.concat(Files.walk(MAIN.resolve("java/com/newuniverse/nusmp/elite/hub")), Files.walk(MAIN.resolve("java/com/newuniverse/nusmp/client/elite/hub")))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = key.matcher(Files.readString(p));
                while (m.find()) used.add(m.group(1));
            }
        }
        assertTrue(used.size() >= 25, "found the keys: " + used.size());
        for (String k : used) assertTrue(lang.has(k), "tools/elite/hub_lang.json lacks " + k);
        assertTrue(lang.has("entity.nusmp.quest_giver_npc") && lang.has("entity.nusmp.merchant_npc") && lang.has("item.nusmp.dungeon_map"));
    }

    @Test
    void theThreeOldDropListenersAskTheCurrencySeam() throws IOException {
        for (String f : new String[] {"labyrinth/dnd/economy/EconomyDrops.java", "labyrinth/mega/eco/FactionDrops.java", "labyrinth/mega/foundry/FoundryDrops.java"}) {
            assertTrue(read("java/com/newuniverse/nusmp/" + f).contains("elite.hub.Currency.stonesSuppressed("), f + " must ask the currency seam before it drops stones");
        }
    }

    @Test
    void theHubIsWiredIntoTheEliteSystemOnce() throws IOException {
        String s = read("java/com/newuniverse/nusmp/elite/EliteSystem.java");
        int first = s.indexOf("elite.hub.HubRegistry.init(modBus, container);");
        assertTrue(first > 0 && first == s.lastIndexOf("elite.hub.HubRegistry.init(modBus, container);"), "exactly one wiring line");
        assertTrue(first > s.indexOf("---- hub:"), "inside the hub section");   // 1.88.0: the hub is the only section left
    }
}
