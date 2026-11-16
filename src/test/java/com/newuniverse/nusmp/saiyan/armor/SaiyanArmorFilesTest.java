package com.newuniverse.nusmp.saiyan.armor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the craftable Saiyan alloy set (docs/saiyan_armor.md) is pinned by reading the repo: the material numbers sit between iron and netherite, the recipes name registered items and cost what the table
 * says, every file the set needs exists (item models, sprites, armour layers, worn meshes and skins, catalog entries, lang), the cosmetic armour is switched by the new client setting and the set bonus
 * is capped and server-side. Nothing here needs the game.
 */
class SaiyanArmorFilesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";
    private static final String RES = "src/main/resources/";
    private static final String ASSETS = RES + "assets/anime_multiverse/";
    private static final List<String> MESHES = List.of("saiyan_alloy_chest", "saiyan_alloy_pauldron", "saiyan_alloy_vambrace", "saiyan_alloy_skirt", "saiyan_alloy_greave", "saiyan_alloy_scouter");

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    private static boolean exists(String rel) { return Files.exists(ROOT.resolve(rel)); }

    @Test
    void theMaterialSitsBetweenIronAndNetherite() {
        for (int i = 0; i < 4; i++) {
            assertTrue(SaiyanArmorRules.DEFENSE[i] >= SaiyanArmorRules.IRON_DEFENSE[i] && SaiyanArmorRules.DEFENSE[i] <= SaiyanArmorRules.NETHERITE_DEFENSE[i], "defense of slot " + i);
        }
        assertTrue(SaiyanArmorRules.total(SaiyanArmorRules.DEFENSE) > SaiyanArmorRules.total(SaiyanArmorRules.IRON_DEFENSE), "better than iron overall");
        assertTrue(SaiyanArmorRules.total(SaiyanArmorRules.DEFENSE) < SaiyanArmorRules.total(SaiyanArmorRules.NETHERITE_DEFENSE), "worse than netherite overall");
        assertTrue(SaiyanArmorRules.DURABILITY_FACTOR > SaiyanArmorRules.IRON_FACTOR && SaiyanArmorRules.DURABILITY_FACTOR < SaiyanArmorRules.NETHERITE_FACTOR);
        assertTrue(SaiyanArmorRules.TOUGHNESS > SaiyanArmorRules.IRON_TOUGHNESS && SaiyanArmorRules.TOUGHNESS < SaiyanArmorRules.NETHERITE_TOUGHNESS);
        assertTrue(SaiyanArmorRules.KNOCKBACK > SaiyanArmorRules.IRON_KNOCKBACK && SaiyanArmorRules.KNOCKBACK < SaiyanArmorRules.NETHERITE_KNOCKBACK);
        assertEquals(4, SaiyanArmorRules.PIECES.size());
    }

    @Test
    void theSetBonusIsWholeSetOnlyAndCapped() {
        assertEquals(0.0, SaiyanArmorRules.kiRegenBonus(3, true, 1.0));
        assertEquals(0.0, SaiyanArmorRules.kiRegenBonus(4, false, 1.0));
        assertEquals(1.0, SaiyanArmorRules.kiRegenBonus(4, true, 1.0));
        assertEquals(SaiyanArmorRules.MAX_KI_REGEN, SaiyanArmorRules.kiRegenBonus(4, true, 500.0));
        assertEquals(0.0, SaiyanArmorRules.kiRegenBonus(4, true, -3.0));
        assertEquals(0.0, SaiyanArmorRules.kiRegenBonus(4, true, Double.NaN));
        assertTrue(SaiyanArmorRules.DEFAULT_KI_REGEN <= SaiyanArmorRules.MAX_KI_REGEN);
    }

    @Test
    void recipesNameRegisteredItemsAndCostTheTable() throws IOException {
        String items = read(MAIN + "saiyan/armor/SaiyanArmorItems.java");
        for (String id : SaiyanArmorRules.PIECES) {
            assertTrue(exists(RES + "data/anime_multiverse/recipe/" + id + ".json"), id + " has a recipe");
            assertTrue(items.contains("SaiyanArmorRules." + constOf(id)), id + " is registered from the rules table");
        }
        String[] ingots = new String[4];
        for (int i = 0; i < 4; i++) {
            String r = read(RES + "data/anime_multiverse/recipe/" + SaiyanArmorRules.PIECES.get(i) + ".json");
            assertTrue(r.contains("\"anime_multiverse:" + SaiyanArmorRules.PIECES.get(i) + "\""), "the result");
            Matcher m = Pattern.compile("\"pattern\": \\[(.*?)\\]", Pattern.DOTALL).matcher(r);
            assertTrue(m.find());
            int a = 0;
            for (char c : m.group(1).toCharArray()) if (c == 'A') a++;
            assertEquals(SaiyanArmorRules.COST[i], a, SaiyanArmorRules.PIECES.get(i) + " costs the table's ingots");
            ingots[i] = r;
        }
        assertTrue(ingots[0].contains("minecraft:glass_pane"), "the scouter has a lens");
        for (String name : List.of("saiyan_alloy_ingot_from_iron", "saiyan_alloy_ingot_from_scrap")) {
            String r = read(RES + "data/anime_multiverse/recipe/" + name + ".json");
            assertTrue(r.contains("\"anime_multiverse:saiyan_alloy_ingot\"") && r.contains("\"count\": 2"), name);
        }
        assertFalse(exists(RES + "data/anime_multiverse/recipe/saiyan_alloy_battle_armor_smithing.json"), "no smithing path: netherite is better than the alloy");
    }

    private static String constOf(String id) {
        switch (id) {
            case "saiyan_alloy_scouter": return "SCOUTER";
            case "saiyan_alloy_battle_armor": return "BATTLE_ARMOR";
            case "saiyan_alloy_waist_guard": return "WAIST_GUARD";
            default: return "GREAVES";
        }
    }

    @Test
    void everyFileTheSetNeedsExists() throws IOException {
        for (String id : SaiyanArmorRules.PIECES) {
            assertTrue(exists(ASSETS + "textures/item/" + id + ".png"), id + " sprite");
            assertTrue(exists(ASSETS + "models/item/" + id + ".json"), id + " object model");
            assertTrue(exists(ASSETS + "models/item/" + id + "_icon.json"), id + " icon model");
            assertTrue(read(ASSETS + "models/item/" + id + ".json").contains("builtin/entity"), id + " is drawn by the worn model renderer");
        }
        assertTrue(exists(ASSETS + "textures/item/" + SaiyanArmorRules.INGOT + ".png") && exists(ASSETS + "models/item/" + SaiyanArmorRules.INGOT + ".json"));
        assertTrue(exists(ASSETS + "textures/models/armor/saiyan_alloy_layer_1.png") && exists(ASSETS + "textures/models/armor/saiyan_alloy_layer_2.png"), "the plain layers");
        for (String m : MESHES) {
            assertTrue(exists(ASSETS + "relic_worn/" + m + ".txt"), m + " mesh");
            String t = read(ASSETS + "relic_worn/" + m + ".txt");
            Matcher skin = Pattern.compile("(?m)^skin (\\S+)").matcher(t);
            assertTrue(skin.find(), m + " has a baked skin");
            assertTrue(exists(ASSETS + "textures/" + skin.group(1)), m + " skin file " + skin.group(1));
            Matcher glow = Pattern.compile("(?m)^glowtex (\\S+)").matcher(t);
            if (glow.find()) assertTrue(exists(ASSETS + "textures/" + glow.group(1)), m + " glow file");
        }
        String cat = read(ASSETS + "relic_worn/catalog.txt");
        for (String id : SaiyanArmorRules.PIECES) assertTrue(cat.contains("\nitem " + id + " "), id + " is in the worn catalog");
        for (String m : MESHES) assertTrue(cat.contains(m), m + " is used by the catalog");
    }

    @Test
    void langKeysAreInTheMergeFile() throws IOException {
        String lang = read("tools/lang/saiyan_armor.json");
        for (String id : SaiyanArmorRules.PIECES) assertTrue(lang.contains("\"item.anime_multiverse." + id + "\""), id + " name");
        assertTrue(lang.contains("\"item.anime_multiverse.saiyan_alloy_ingot\""));
        assertTrue(lang.contains("\"tooltip.anime_multiverse.saiyan_alloy\"") && lang.contains("\"tooltip.anime_multiverse.saiyan_alloy.set\""));
    }

    @Test
    void theCosmeticArmourIsKeptAndSwitchedByTheClientSetting() throws IOException {
        String cfg = read(MAIN + "client/aura/SaiyanLookClientConfig.java");
        assertTrue(cfg.contains("\"autoArmour\"") && cfg.contains("define(\"autoArmour\", false)"), "default off");
        String aura = read(MAIN + "client/aura/SaiyanAura.java");
        assertTrue(aura.contains("SaiyanLookClientConfig.autoArmour()") && aura.contains("SaiyanArmorSet.worn("), "the cosmetic armour needs the setting and yields to a worn set");
        assertTrue(exists(MAIN + "saiyan/SaiyanLook.java") && exists("tools/gen_saiyan_armor_geo.py"), "the old cosmetic armour code stays");
        assertTrue(read(MAIN + "client/aura/SaiyanLookClientConfig.java").contains("autoArmour = false"), "the reset path");
    }

    @Test
    void theServerSideIsServerSafe() throws IOException {
        for (String f : List.of("SaiyanArmorRules", "SaiyanArmorConfig", "SaiyanArmorSet", "SaiyanArmorItem", "SaiyanArmorItems", "SaiyanArmorBoot", "SaiyanArmorSelfTests")) {
            String t = read(MAIN + "saiyan/armor/" + f + ".java");
            assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client") || t.contains("com.mojang.blaze3d") || t.contains("Minecraft.getInstance"), f + " is common code");
        }
        String boot = read(MAIN + "saiyan/armor/SaiyanArmorBoot.java");
        assertTrue(boot.contains("ModConfig.Type.SERVER") && boot.contains("nusmp-saiyan-armor-server.toml"), "gameplay setting is a server config");
        String set = read(MAIN + "saiyan/armor/SaiyanArmorSet.java");
        assertFalse(set.contains("getEntitiesOfClass") || set.contains("level().getEntities") || set.contains("new ArrayList") || set.contains("new HashMap"), "four slot reads, no scan, no allocation");
        assertTrue(read(MAIN + "saiyan/SaiyanManager.java").contains("SaiyanArmorSet.kiRegenBonus(p)"), "the bonus rides the existing Ki regen");
    }
}
