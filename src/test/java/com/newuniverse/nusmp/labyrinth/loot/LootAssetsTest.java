package com.newuniverse.nusmp.labyrinth.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the loot is eighty items and each needs a file in half a dozen places; a missing icon is an invisible item, a missing lang key is a raw id on screen, a recipe that names an
 * item that does not exist is silently dropped by the game. This reads the files (no game needed) and fails on the first gap: every item has its model, its sprite and its lang
 * lines; every weapon and trinket has its mesh, skins, class row and (weapons) its Better Combat file and engravings; every recipe names real items; every material has a use;
 * the generated text class and the lang fragment say the same words; the global modifier is switched on; and nothing in the loot or squad code can touch hunger.
 */
class LootAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final LootRoster ROSTER = LootRoster.parse(new InputStreamReader(LootAssetsTest.class.getResourceAsStream(LootRoster.RESOURCE), StandardCharsets.UTF_8));

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void everyItemHasAModelAndASprite() {
        List<String> bad = new ArrayList<>();
        for (String id : ROSTER.ids()) {
            if (!Files.exists(ASSETS.resolve("models/item/" + id + ".json"))) bad.add(id + " model");
            if (!Files.exists(ASSETS.resolve("textures/item/" + id + ".png"))) bad.add(id + " sprite");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyItemHasItsNameAndLoreInTheLangFragment() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/squad_loot.json"));
        List<String> bad = new ArrayList<>();
        for (String id : ROSTER.ids()) {
            if (!lang.has("item.nusmp." + id)) bad.add(id + " name");
            if (!lang.has("item.nusmp." + id + ".lore")) bad.add(id + " lore");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theGeneratedTextAndTheLangFragmentSayTheSame() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/squad_loot.json"));
        String src = Files.readString(JAVA.resolve("labyrinth/loot/LootText.java"));
        Matcher m = Pattern.compile("Map\\.entry\\(\"([^\"]+)\", \"((?:[^\"\\\\]|\\\\.)*)\"\\)").matcher(src);
        int n = 0;
        List<String> bad = new ArrayList<>();
        while (m.find()) {
            n++;
            String key = m.group(1), text = m.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
            if (!lang.has(key)) bad.add(key + " missing from the fragment");
            else if (!lang.get(key).getAsString().equals(text)) bad.add(key + " differs");
        }
        assertTrue(n >= 30, "entries read from LootText: " + n);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theLangFragmentHoldsOnlyMyKeysAndNoToolNames() throws IOException {
        JsonObject lang = json(Path.of("tools/lang/squad_loot.json"));
        Set<String> mine = new HashSet<>(ROSTER.ids());
        Pattern banned = Pattern.compile("\\b(claude|anthropic|gemini|sonnet|opus|chatgpt)\\b", Pattern.CASE_INSENSITIVE);
        for (String key : lang.keySet()) {
            if (key.startsWith("item.nusmp.")) {
                String rest = key.substring("item.nusmp.".length());
                String id = rest.endsWith(".lore") ? rest.substring(0, rest.length() - 5) : rest;
                assertTrue(mine.contains(id) || id.equals("mana_surge") || key.startsWith("item.nusmp.loot"), "an item key that is not in the roster: " + key);
            }
            assertFalse(banned.matcher(lang.get(key).getAsString()).find(), key + " names a tool");
        }
    }

    @Test
    void everyWeaponAndTrinketIsDrawnInThreeDimensions() throws IOException {
        String classesA = Files.readString(ASSETS.resolve("weapon_classes.json")), classesB = Files.readString(Path.of("tools/relics/weapon_classes.json"));
        List<String> ids = new ArrayList<>();
        ROSTER.weapons().forEach(w -> ids.add(w.id()));
        ROSTER.trinkets().forEach(t -> ids.add(t.id()));
        List<String> bad = new ArrayList<>();
        for (String id : ids) {
            if (!Files.exists(ASSETS.resolve("weapon_meshes/" + id + ".txt"))) bad.add(id + " mesh");
            if (!Files.exists(ASSETS.resolve("textures/entity/weapon/" + id + ".png"))) bad.add(id + " skin");
            if (!Files.exists(ASSETS.resolve("textures/entity/weapon/" + id + "_glow.png"))) bad.add(id + " glow skin");
            if (!Files.exists(ASSETS.resolve("models/item/" + id + "_icon.json"))) bad.add(id + " icon model");
            if (!Files.readString(ASSETS.resolve("models/item/" + id + ".json")).contains("builtin/entity")) bad.add(id + " hand model is not the 3D one");
            if (!classesA.contains("\"" + id + "\"")) bad.add(id + " class row (assets)");
            if (!classesB.contains("\"" + id + "\"")) bad.add(id + " class row (tools)");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyWeaponHasBetterCombatFilesAndEngravingsInTheEngravingTable() throws IOException {
        String engravings = Files.readString(JAVA.resolve("item/WeaponEngravings.java"));
        List<String> bad = new ArrayList<>();
        for (LootRoster.WeaponDef w : ROSTER.weapons()) {
            if (!Files.exists(DATA.resolve("nusmp/weapon_attributes/" + w.id() + ".json"))) bad.add(w.id() + " Better Combat attributes");
            if (!engravings.contains("Map.entry(\"" + w.id() + "\"")) bad.add(w.id() + " engraving row");
            for (LootRoster.Engraving e : w.engravings()) {
                String row = rowOf(engravings, w.id());
                if (!row.contains("e(\"" + e.id() + "\", " + e.level() + ")")) bad.add(w.id() + " engraving " + e.id() + " " + e.level());
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    private static String rowOf(String src, String id) {
        int i = src.indexOf("Map.entry(\"" + id + "\"");
        if (i < 0) return "";
        int j = src.indexOf('\n', i);
        return src.substring(i, j < 0 ? src.length() : j);
    }

    @Test
    void everyRecipeNamesRealItemsAndMakesARosterItem() throws IOException {
        Set<String> mine = new HashSet<>(ROSTER.ids());
        List<String> bad = new ArrayList<>();
        int n = 0;
        try (Stream<Path> files = Files.list(DATA.resolve("nusmp/recipe"))) {
            for (Path p : files.filter(f -> f.getFileName().toString().startsWith("loot_")).toList()) {
                n++;
                JsonObject r = json(p);
                String result = r.getAsJsonObject("result").get("id").getAsString();
                if (!result.startsWith("nusmp:") || !mine.contains(result.substring(6))) bad.add(p.getFileName() + " makes " + result);
                List<String> items = new ArrayList<>(), tags = new ArrayList<>();
                ingredients(r, items, tags);
                for (String item : items) if (!exists(item, mine)) bad.add(p.getFileName() + " ingredient " + item);
                for (String tag : tags) {
                    if (tag.startsWith("nusmp:") && !Files.exists(DATA.resolve("nusmp/tags/item/" + tag.substring(6) + ".json"))) bad.add(p.getFileName() + " tag " + tag);
                }
            }
        }
        assertTrue(n >= 30, "loot recipes: " + n);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    private static boolean exists(String item, Set<String> mine) {
        if (item.startsWith("minecraft:")) return true;
        if (!item.startsWith("nusmp:")) return false;
        String id = item.substring(6);
        return mine.contains(id) || Files.exists(ASSETS.resolve("models/item/" + id + ".json"));
    }

    private static void ingredients(JsonElement e, List<String> items, List<String> tags) {
        if (e.isJsonArray()) {
            for (JsonElement x : (JsonArray) e) ingredients(x, items, tags);
        } else if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (String key : o.keySet()) {
                if (key.equals("result") || key.equals("pattern") || key.equals("type") || key.equals("category") || key.equals("group")) continue;
                if (key.equals("item") && o.get(key).isJsonPrimitive()) items.add(o.get(key).getAsString());
                else if (key.equals("tag") && o.get(key).isJsonPrimitive()) tags.add(o.get(key).getAsString());
                else ingredients(o.get(key), items, tags);
            }
        }
    }

    @Test
    void everyMaterialHasAUse() throws IOException {
        Set<String> used = new HashSet<>(ROSTER.codeUses().keySet());
        try (Stream<Path> files = Files.list(DATA.resolve("nusmp/recipe"))) {
            for (Path p : files.filter(f -> f.getFileName().toString().startsWith("loot_")).toList()) {
                List<String> items = new ArrayList<>(), tags = new ArrayList<>();
                ingredients(json(p), items, tags);
                for (String item : items) if (item.startsWith("nusmp:")) used.add(item.substring(6));
                for (String tag : tags) {
                    Path tp = DATA.resolve("nusmp/tags/item/" + tag.substring(tag.indexOf(':') + 1) + ".json");
                    if (!tag.startsWith("nusmp:") || !Files.exists(tp)) continue;
                    for (JsonElement v : json(tp).getAsJsonArray("values")) {
                        String id = v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString();
                        if (id.startsWith("nusmp:")) used.add(id.substring(6));
                    }
                }
            }
        }
        List<String> unused = new ArrayList<>();
        for (LootRoster.MaterialDef m : ROSTER.materials()) if (!used.contains(m.id())) unused.add(m.id());
        assertTrue(unused.isEmpty(), "monster parts that nothing uses: " + unused);
        assertTrue(ROSTER.codeUses().size() >= 3, "code uses: " + ROSTER.codeUses().keySet());
    }

    @Test
    void theGlobalModifierIsSwitchedOnAndItsFileIsThere() throws IOException {
        assertTrue(Files.readString(DATA.resolve("neoforge/loot_modifiers/global_loot_modifiers.json")).contains("\"nusmp:anime_loot\""));
        assertEquals("nusmp:anime_loot", json(DATA.resolve("nusmp/loot_modifiers/anime_loot.json")).get("type").getAsString());
    }

    @Test
    void theTrinketsFitTheirCuriosSlots() throws IOException {
        for (LootRoster.TrinketDef t : ROSTER.trinkets()) {
            String tag = Files.readString(DATA.resolve("curios/tags/item/" + t.slot() + ".json"));
            assertTrue(tag.contains("\"nusmp:" + t.id() + "\""), t.id() + " is not in the " + t.slot() + " slot tag");
        }
    }

    @Test
    void nothingInTheLootOrSquadCodeCanDrainHunger() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String dir : new String[]{"labyrinth/loot", "labyrinth/squad", "client/loot"}) {
            try (Stream<Path> files = Files.walk(JAVA.resolve(dir))) {
                for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    String t = Files.readString(p);
                    String name = p.getFileName().toString();
                    for (String token : new String[]{"addExhaustion", "causeFoodExhaustion", "setFoodLevel", "setSaturation", "MobEffects.HUNGER"}) {
                        if (t.contains(token) && !(name.equals("LootSelfTests.java") && token.equals("MobEffects.HUNGER"))) bad.add(name + " uses " + token);
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noToolOrModelNamesInTheNewCodeAndDocs() throws IOException {
        Pattern banned = Pattern.compile("\\b(claude|anthropic|gemini|sonnet|opus|chatgpt)\\b", Pattern.CASE_INSENSITIVE);
        List<Path> files = new ArrayList<>();
        for (String dir : new String[]{"src/main/java/com/newuniverse/nusmp/labyrinth/loot", "src/main/java/com/newuniverse/nusmp/labyrinth/squad", "src/main/java/com/newuniverse/nusmp/client/loot", "tools/loot"}) {
            try (Stream<Path> s = Files.walk(Path.of(dir))) {
                files.addAll(s.filter(f -> f.toString().endsWith(".java") || f.toString().endsWith(".py")).toList());
            }
        }
        Path doc = Path.of("docs/labyrinth/squads_and_loot.md");
        if (Files.exists(doc)) files.add(doc);
        List<String> bad = new ArrayList<>();
        for (Path p : files) if (banned.matcher(Files.readString(p)).find()) bad.add(p.toString());
        assertTrue(bad.isEmpty(), bad.toString());
    }
}
