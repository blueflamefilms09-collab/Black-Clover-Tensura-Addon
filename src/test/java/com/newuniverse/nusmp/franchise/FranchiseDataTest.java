package com.newuniverse.nusmp.franchise;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The shipped data of the franchise engravings: enchantments, tag, lang, numbers, the item map, and the older engravings left alone. */
class FranchiseDataTest {
    private static final File RES = new File("src/main/resources");
    private static final Set<String> KINDS = Set.of("hit_bonus", "anti_ward", "spell_refund", "spell_power", "mana_trickle", "chance_roll", "first_strike", "kill_heal", "kill_ally",
            "kill_stacks", "charge_hit", "ally_regen", "shield_guard", "ward", "counter", "dodge", "second_wind", "debuff", "blood_price", "combo", "lifesteal", "kill_haste");
    private static final Set<String> COOLDOWN_KINDS = Set.of("spell_refund", "chance_roll", "kill_heal", "kill_ally", "dodge", "second_wind", "debuff", "lifesteal", "kill_haste");

    private static String read(String path) throws Exception { return Files.readString(new File(path).toPath()); }

    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(read(path)).getAsJsonObject(); }

    private static FranchiseData.Table table() throws Exception { return FranchiseData.parse(read("src/main/resources/data/nusmp/franchise/engravings.json")); }

    @Test
    void theTableIsWhole() throws Exception {
        FranchiseData.Table t = table();
        assertTrue(t.franchises().size() >= 30, "franchises: " + t.franchises().size());
        assertTrue(t.engravings().size() >= 40 && t.engravings().size() <= 80, "engravings: " + t.engravings().size());
        Set<String> fids = new HashSet<>();
        for (FranchiseData.Franchise f : t.franchises()) {
            assertTrue(fids.add(f.id()), "duplicate franchise " + f.id());
            assertTrue(t.of(f.id()).size() >= 2 && t.of(f.id()).size() <= 3, f.id() + " has " + t.of(f.id()).size() + " engravings (2 or 3)");
            assertFalse(f.name().isBlank());
        }
        for (FranchiseData.Engraving e : t.engravings().values()) {
            assertTrue(e.id().startsWith("fr_"), e.id());
            assertTrue(fids.contains(e.franchise()), e.id() + " names an unknown franchise");
            assertTrue(Set.of("main", "held", "any").contains(e.scope()), e.id() + " scope " + e.scope());
            assertFalse(e.effects().isEmpty(), e.id() + " does nothing");
        }
    }

    @Test
    void everyEngravingIsAWellFormedDatapackEnchantment() throws Exception {
        for (FranchiseData.Engraving e : table().engravings().values()) {
            File f = new File(RES, "data/nusmp/enchantment/" + e.id() + ".json");
            assertTrue(f.isFile(), "missing " + f);
            JsonObject o = JsonParser.parseString(Files.readString(f.toPath())).getAsJsonObject();
            // the fields a 1.21.1 enchantment requires
            assertEquals("enchantment.nusmp." + e.id(), o.getAsJsonObject("description").get("translate").getAsString());
            assertTrue(o.has("supported_items"), e.id());
            assertTrue(o.get("weight").getAsInt() >= 1, e.id());
            assertTrue(o.get("max_level").getAsInt() >= 3, e.id() + " must reach level 3 (a god relic is engraved at 3)");
            for (String cost : new String[]{"min_cost", "max_cost"}) {
                assertTrue(o.getAsJsonObject(cost).has("base") && o.getAsJsonObject(cost).has("per_level_above_first"), e.id() + " " + cost);
            }
            assertTrue(o.has("anvil_cost"), e.id());
            assertTrue(o.getAsJsonArray("slots").size() >= 1, e.id());
            assertTrue(o.has("effects") && o.get("effects").isJsonObject(), e.id());
        }
    }

    @Test
    void everyEngravingIsInTheTensuraEngravingTagAndTheOlderOnesStay() throws Exception {
        JsonObject tag = json("src/main/resources/data/tensura/tags/enchantment/engraving.json");
        assertFalse(tag.get("replace").getAsBoolean(), "the tag adds to Tensura's, it never replaces");
        Set<String> values = new HashSet<>();
        for (JsonElement v : tag.getAsJsonArray("values")) values.add(v.getAsString());
        assertTrue(values.contains("nusmp:anti_magic") && values.contains("nusmp:legacy_of_the_wizard_kings"), "the older entries stay");
        for (FranchiseData.Engraving e : table().engravings().values()) assertTrue(values.contains("nusmp:" + e.id()), e.id() + " is not in the tag");
        assertEquals(values.size(), tag.getAsJsonArray("values").size(), "no duplicate in the tag");
    }

    @Test
    void everyEngravingAndFranchiseHasLangText() throws Exception {
        JsonObject lang = json("tools/lang/franchise_engravings.json");
        FranchiseData.Table t = table();
        for (FranchiseData.Engraving e : t.engravings().values()) {
            String name = lang.has("enchantment.nusmp." + e.id()) ? lang.get("enchantment.nusmp." + e.id()).getAsString() : "";
            String desc = lang.has("enchantment.nusmp." + e.id() + ".desc") ? lang.get("enchantment.nusmp." + e.id() + ".desc").getAsString() : "";
            assertEquals(e.name(), name, e.id() + " name");
            assertTrue(desc.length() > 40, e.id() + " description");
            assertFalse(desc.toLowerCase().contains("hunger"), e.id() + " mentions hunger");
        }
        for (FranchiseData.Franchise f : t.franchises()) assertTrue(lang.has("franchise.nusmp." + f.id()), f.id() + " has no lang name");
        assertTrue(lang.has("tooltip.nusmp.franchise"));
    }

    @Test
    void everyEffectIsKnownAndCapped() throws Exception {
        for (FranchiseData.Engraving e : table().engravings().values()) {
            for (FranchiseData.Effect fx : e.effects()) {
                String where = e.id() + "/" + fx.kind();
                assertTrue(KINDS.contains(fx.kind()), "unknown kind " + where);
                boolean guard = fx.kind().equals("ward") || fx.kind().equals("shield_guard");
                for (String key : new String[]{"pct", "bonus"}) assertTrue(fx.d(key, 0) >= 0 && fx.d(key, 0) <= (guard ? FranchiseRules.GUARD_CAP : FranchiseRules.ATTACK_CAP), where + " " + key + " is more than its cap on its own");
                assertTrue(fx.d("chance", 0) <= (fx.kind().equals("debuff") ? 0.15 : 0.10), where + " chance");
                assertTrue(fx.d("heal", 0) <= 3.0, where + " heal");
                assertTrue(fx.d("cap", 0) <= FranchiseRules.HEAL_CAP, where + " cap");
                assertTrue(fx.d("mana", 0) <= 0.02, where + " mana");
                if (fx.kind().equals("mana_trickle")) assertTrue(fx.d("pct", 0) <= FranchiseRules.TRICKLE_CAP, where + " trickle");
                if (fx.kind().equals("spell_refund")) assertTrue(fx.d("pct", 0) <= 0.02, where + " refund");
                if (fx.kind().equals("ward") || fx.kind().equals("shield_guard")) assertTrue(fx.d("pct", 0) <= 0.20, where + " guard");
                if (fx.kind().equals("blood_price")) assertTrue(fx.d("cost", 0) > 0 && fx.d("cost", 0) <= 2.0 && fx.d("min_hp", 0) >= 6.0, where + " price (flat, never a share of the maximum health)");
                if (fx.kind().equals("second_wind")) assertTrue(fx.i("cd", 0) >= 1200 && fx.d("threshold", 1) <= 0.3, where + " second wind");
                if (fx.kind().equals("kill_ally")) assertTrue(fx.i("cd", 0) >= 600 && fx.i("life", 0) <= 600 && fx.d("chance", 0) <= 0.08, where + " ally");
                if (fx.kind().equals("dodge")) assertTrue(fx.i("cd", 0) >= 300 && fx.d("chance", 0) <= 0.06, where + " dodge");
                if (COOLDOWN_KINDS.contains(fx.kind())) assertTrue(fx.i("cd", 0) >= 20, where + " has no cooldown");
                if (fx.kind().equals("debuff")) {
                    String effect = fx.s("effect", "");
                    assertTrue(FranchiseRules.effectAllowed(effect) && !effect.isEmpty(), where + " effect " + effect);
                    assertTrue(fx.i("dur", 0) <= 100, where + " duration");
                }
                if (fx.kind().equals("kill_stacks")) assertTrue(fx.i("max", 0) * fx.d("pct", 0) <= FranchiseRules.ATTACK_CAP, where + " stacks together pass the cap");
                if (fx.kind().equals("combo")) assertTrue(fx.i("max", 0) * fx.d("pct", 0) <= FranchiseRules.ATTACK_CAP, where + " combo together passes the cap");
                if (fx.kind().equals("ally_regen")) assertTrue(fx.i("dur", 0) <= FranchiseRules.REGEN_DURATION_CAP && fx.d("radius", 0) <= 12, where + " aura");
            }
        }
    }

    @Test
    void noEngravingTouchesTheFoodBar() throws Exception {
        String data = read("src/main/resources/data/nusmp/franchise/engravings.json").toLowerCase();
        for (String bad : new String[]{"hunger", "saturation", "exhaustion", "food"}) assertFalse(data.contains(bad), "the table mentions " + bad);
    }

    @Test
    void everyFranchiseOfTheMapHasAtLeastTwoEngravings() throws Exception {
        FranchiseData.Table t = table();
        JsonObject map = json("src/main/resources/data/nusmp/relics/franchise_map.json");
        JsonObject counts = map.getAsJsonObject("franchises");
        assertTrue(counts.size() >= 15, "the map has " + counts.size() + " franchises");
        Set<String> known = new HashSet<>();
        for (FranchiseData.Franchise f : t.franchises()) known.add(f.id());
        for (Map.Entry<String, JsonElement> e : counts.entrySet()) {
            assertTrue(known.contains(e.getKey()), "the map names an unknown franchise " + e.getKey());
            assertTrue(t.of(e.getKey()).size() >= 2, e.getKey() + " has fewer than 2 engravings");
        }
        int total = 0;
        for (Map.Entry<String, JsonElement> e : map.getAsJsonObject("items").entrySet()) {
            assertTrue(known.contains(e.getValue().getAsString()), e.getKey() + " -> unknown franchise " + e.getValue());
            assertTrue(e.getKey().matches("[a-z0-9_]+"), e.getKey());
            total++;
        }
        assertTrue(total >= 700, "items in the map: " + total);
        for (Map.Entry<String, JsonElement> e : map.getAsJsonObject("prefixes").entrySet()) assertTrue(known.contains(e.getValue().getAsString()), e.getKey());
    }

    @Test
    void mapLookupFollowsExactPairAndPrefix() throws Exception {
        FranchiseMap.Map2 m = FranchiseMap.parse(read("src/main/resources/data/nusmp/relics/franchise_map.json"));
        assertEquals("black_clover", m.franchiseOf("demon_slayer_sword"));
        assertEquals("black_clover", m.franchiseOf("charter_grimoire_ban"), "by prefix");
        assertEquals("magi", m.franchiseOf("djinn_weapon_bael_dark"), "by prefix");
        assertEquals("shield_hero", m.franchiseOf("legendary_shield"));
        assertEquals("fairy_tail", m.franchiseOf("stormcaller_staff"));
        assertNull(m.franchiseOf("stone"), "an item without a franchise has none");
        assertNull(m.franchiseOf(""));
        assertNull(m.franchiseOf(null));
        FranchiseMap.Map2 tiny = FranchiseMap.parse("{\"items\":{\"twin_blades\":\"dnd\"},\"prefixes\":{\"abc\":\"fate\",\"abcdef\":\"naruto\"}}");
        assertEquals("dnd", tiny.franchiseOf("twin_blades_left"), "a pair's left half follows its right half");
        assertEquals("naruto", tiny.franchiseOf("abcdefg"), "the longest prefix wins");
        assertEquals("fate", tiny.franchiseOf("abcx"));
    }

    @Test
    void theProcThemesExist() throws Exception {
        JsonObject themes = json("src/main/resources/data/nusmp/particle_themes.json").getAsJsonObject("themes");
        for (FranchiseData.Franchise f : table().franchises()) assertTrue(themes.has(f.theme()), f.id() + " names the particle theme " + f.theme() + " that does not exist");
    }

    /** The older Tensura engravings of WeaponEngravings are append-only: every line of the baseline (taken before this release) is still there, unchanged. */
    @Test
    void theOlderWeaponEngravingsAreUnchanged() throws Exception {
        Map<String, String> now = new HashMap<>();
        Pattern entry = Pattern.compile("Map\\.entry\\(\"([a-z0-9_]+)\",\\s*List\\.of\\((.*)$");
        Pattern spec = Pattern.compile("\\be\\(\"([^\"]+)\",\\s*(\\d+)\\)");
        for (String line : read("src/main/java/com/newuniverse/nusmp/item/WeaponEngravings.java").split("\\R")) {
            Matcher m = entry.matcher(line);
            if (!m.find()) continue;
            List<String> specs = new ArrayList<>();
            Matcher s = spec.matcher(m.group(2));
            while (s.find()) specs.add(s.group(1) + "@" + s.group(2));
            now.put(m.group(1), String.join(";", specs));
        }
        int checked = 0;
        for (String line : read("src/test/resources/franchise/weapon_engravings_baseline.txt").split("\\R")) {
            if (line.isBlank()) continue;
            int eq = line.indexOf('=');
            String id = line.substring(0, eq);
            assertEquals(line.substring(eq + 1), now.get(id), "the engravings of " + id + " changed");
            checked++;
        }
        // 24 baseline rows left with the Requip weapons and the skill-book items the owner asked to remove (3.36.0); the other rows must stay
        assertTrue(checked >= 55, "baseline entries: " + checked);
        assertTrue(read("src/main/java/com/newuniverse/nusmp/item/WeaponEngravings.java").contains("static final String FLAG = \"NUEngraved\""), "the NUEngraved marker rule stays");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/franchise/FranchiseEngravings.java").contains("FLAG = \"NUFranchise\""), "the franchise engravings have their own marker");
    }

    @Test
    void theGeneratorIsInSyncWithTheShippedFiles() throws Exception {
        JsonArray arr = json("src/main/resources/data/nusmp/franchise/engravings.json").getAsJsonArray("engravings");
        File[] files = new File(RES, "data/nusmp/enchantment").listFiles((d, n) -> n.startsWith("fr_") && n.endsWith(".json"));
        assertNotNull(files);
        assertEquals(arr.size(), files.length, "one enchantment file per engraving, none left over");
    }
}
