package com.newuniverse.nusmp.elite.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.items.consumable.HealthPotionTier;
import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.weapon.AbilityParser;
import com.newuniverse.nusmp.elite.items.weapon.WeaponAbilities;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the owner's rules for new items are checkable: every weapon is a real 3D model in hand (a mesh with a placement line, a skin, a glow skin, a model json, the old-style 16x16 icon and its
 * _icon model), carries Tensura engravings (listed in WeaponEngravings.java), has a size class, a Better Combat file and a data-driven technique file that parses without a single complaint; every
 * armour piece has its icon, its models, its two painted layers and an engraving entry; every consumable, key and crate has what it needs; every item has a name in the lang fragment; every
 * sound event has a sounds.json entry and an .ogg. A missing piece is a missing texture or a crash in the game, so it is a failing test here.
 */
class ItemAssetsTest {
    static String text(String path) throws Exception {
        try (InputStream in = ItemAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static boolean exists(String path) { return ItemAssetsTest.class.getResource(path) != null; }

    static BufferedImage png(String path) throws Exception {
        try (InputStream in = ItemAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return javax.imageio.ImageIO.read(in);
        }
    }

    static String source(String relative) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/newuniverse/nusmp/" + relative)), StandardCharsets.UTF_8);
    }

    @Test
    void everyItemHasASixteenPixelIconOrItsOwnModel() throws Exception {
        List<String> flat = new java.util.ArrayList<>(EliteIds.WEAPONS);
        flat.addAll(EliteIds.ARMOR);
        flat.addAll(EliteIds.FLAT_ICON_ITEMS);
        flat.addAll(EliteIds.keys());
        for (String id : flat) {
            BufferedImage im = png("/assets/nusmp/textures/item/" + id + ".png");
            assertEquals(16, im.getWidth(), id + " icon width");
            assertEquals(16, im.getHeight(), id + " icon height");
            int opaque = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((im.getRGB(x, y) >>> 24) > 0) opaque++;
            assertTrue(opaque >= 40, id + " icon is nearly empty (" + opaque + " pixels)");
            assertTrue(exists("/assets/nusmp/models/item/" + id + ".json"), id + " has no item model");
        }
    }

    @Test
    void everyWeaponIsA3DModelWithEngravingsClassCombatFileAndTechniques() throws Exception {
        String engravings = source("item/WeaponEngravings.java");
        JsonObject classes = JsonParser.parseString(text("/assets/nusmp/weapon_classes.json")).getAsJsonObject().getAsJsonObject("weapons");
        for (String id : EliteIds.WEAPONS) {
            String mesh = text("/assets/nusmp/weapon_meshes/" + id + ".txt");
            assertTrue(mesh.contains("\nplace "), id + " mesh has no place line");
            assertTrue(mesh.lines().filter(l -> l.startsWith("q ")).count() > 60, id + " mesh has too few quads");
            assertTrue(mesh.contains("\nglow "), id + " has no glow line");
            assertEquals(128, 128);
            assertTrue(png("/assets/nusmp/textures/entity/weapon/" + id + ".png").getWidth() >= 128, id + " skin");
            assertTrue(exists("/assets/nusmp/textures/entity/weapon/" + id + "_glow.png"), id + " glow skin");
            JsonObject model = JsonParser.parseString(text("/assets/nusmp/models/item/" + id + ".json")).getAsJsonObject();
            assertEquals("minecraft:builtin/entity", model.get("parent").getAsString(), id + " model must be builtin/entity (drawn by WeaponRenderer)");
            assertEquals("nusmp:item/" + id, JsonParser.parseString(text("/assets/nusmp/models/item/" + id + "_icon.json")).getAsJsonObject().getAsJsonObject("textures").get("layer0").getAsString());
            assertTrue(engravings.contains("Map.entry(\"" + id + "\""), id + " has no engravings in WeaponEngravings.java (owner rule 6)");
            assertTrue(classes.has(id), id + " has no class in weapon_classes.json");
            assertTrue(exists("/data/nusmp/weapon_attributes/" + id + ".json"), id + " has no Better Combat file");
            AbilityParser.Result r = AbilityParser.parse(text("/data/nusmp/elite/items/abilities/" + id + ".json"));
            assertTrue(r.ok(), id + " technique file: " + r.errors());
            WeaponAbilities w = r.abilities();
            assertEquals("nusmp:" + id, w.item());
            assertTrue(w.primary().cooldown() >= 100 && w.alt().cooldown() >= 100, id + ": cooldowns of at least 5 s");
            assertTrue(w.alt() != w.primary(), id + " needs a second technique for sneak + right-click");
        }
        // the Void Edge is paired with the skill another builder makes, by data only
        WeaponAbilities v = AbilityParser.parse(text("/data/nusmp/elite/items/abilities/void_edge.json")).abilities();
        assertEquals("nusmp:void_edge", v.skill());
        assertTrue(v.skillMultiplier() > 1.0);
        assertTrue(v.primary().dark(), "the Void Edge uses Dark Rukh");
        assertFalse(AbilityParser.parse(text("/data/nusmp/elite/items/abilities/astral_edge.json")).abilities().primary().dark(), "the Astral Edge uses Light Rukh");
    }

    @Test
    void runicSwordsScaleByTier() throws Exception {
        WeaponAbilities a = AbilityParser.parse(text("/data/nusmp/elite/items/abilities/runic_sword_renewed.json")).abilities();
        WeaponAbilities b = AbilityParser.parse(text("/data/nusmp/elite/items/abilities/runic_sword_ancient.json")).abilities();
        WeaponAbilities c = AbilityParser.parse(text("/data/nusmp/elite/items/abilities/runic_sword_mythical.json")).abilities();
        assertTrue(a.primary().damage() < b.primary().damage() && b.primary().damage() < c.primary().damage());
        assertTrue(a.primary().radius() < b.primary().radius() && b.primary().radius() < c.primary().radius());
        assertTrue(a.alt().heal() < b.alt().heal() && b.alt().heal() < c.alt().heal());
        assertTrue(a.primary().runeScale() > 0 && c.primary().runeScale() > 0, "runes must add to the burst");
    }

    @Test
    void everyArmourPieceHasModelsLayersAndEngravings() throws Exception {
        String engravings = source("item/ArmorEngravings.java");
        for (String id : EliteIds.ARMOR) {
            JsonObject model = JsonParser.parseString(text("/assets/nusmp/models/item/" + id + ".json")).getAsJsonObject();
            assertEquals("minecraft:builtin/entity", model.get("parent").getAsString(), id + ": the worn-model library draws the object form");
            assertTrue(exists("/assets/nusmp/models/item/" + id + "_icon.json"), id + " has no _icon model");
            assertTrue(engravings.contains("Map.entry(\"" + id + "\""), id + " has no entry in ArmorEngravings.java");
        }
        BufferedImage l1 = png("/assets/nusmp/textures/models/armor/nightingale_layer_1.png"), l2 = png("/assets/nusmp/textures/models/armor/nightingale_layer_2.png");
        assertEquals(64, l1.getWidth());
        assertEquals(32, l1.getHeight());
        assertEquals(64, l2.getWidth());
        BufferedImage blank = png("/assets/nusmp/textures/models/armor/blank_layer.png");
        for (int y = 0; y < blank.getHeight(); y++) for (int x = 0; x < blank.getWidth(); x++) assertEquals(0, blank.getRGB(x, y) >>> 24, "the blank layer must be fully transparent");
    }

    @Test
    void chronicleBookIsA3DModelWithAFlatIcon() throws Exception {
        JsonObject m = JsonParser.parseString(text("/assets/nusmp/models/item/chronicle_book.json")).getAsJsonObject();
        assertEquals("neoforge:separate_transforms", m.get("loader").getAsString());
        JsonObject book = JsonParser.parseString(text("/assets/nusmp/models/item/chronicle_book_3d.json")).getAsJsonObject();
        assertTrue(book.getAsJsonArray("elements").size() >= 10, "a real 3D book has boards, spine, pages, clasp");
        assertTrue(exists("/assets/nusmp/textures/item/chronicle_book_3d.png"));
        assertTrue(exists("/assets/nusmp/models/item/chronicle_book_2d.json"));
    }

    @Test
    void everyCrateHasItsBlockFilesRecipeAndLootTable() throws Exception {
        for (CrateTier t : CrateTier.values()) {
            String id = t.crateId();
            JsonObject st = JsonParser.parseString(text("/assets/nusmp/blockstates/" + id + ".json")).getAsJsonObject().getAsJsonObject("variants");
            assertEquals(8, st.size(), id + ": 4 facings x open/closed");
            assertTrue(st.toString().contains("block/crate/" + id + "_open"), id + " blockstate must use the open model");
            assertEquals(128, png("/assets/nusmp/textures/block/crate/" + id + ".png").getWidth());
            JsonObject closed = JsonParser.parseString(text("/assets/nusmp/models/block/crate/" + id + ".json")).getAsJsonObject();
            JsonObject open = JsonParser.parseString(text("/assets/nusmp/models/block/crate/" + id + "_open.json")).getAsJsonObject();
            assertTrue(open.getAsJsonArray("elements").size() > closed.getAsJsonArray("elements").size(), id + ": the open model adds the light");
            assertTrue(text("/data/nusmp/loot_table/blocks/" + id + ".json").contains("\"nusmp:" + id + "\""));
            assertTrue(text("/data/nusmp/recipe/" + id + ".json").contains("\"nusmp:" + id + "\""));
            assertEquals("nusmp:block/crate/" + id, JsonParser.parseString(text("/assets/nusmp/models/item/" + id + ".json")).getAsJsonObject().get("parent").getAsString());
        }
        // the three keys are loot only: no recipe
        for (String k : EliteIds.keys()) assertFalse(exists("/data/nusmp/recipe/" + k + ".json"), k + " must have no recipe");
    }

    @Test
    void everyItemAndBlockHasAName() throws Exception {
        JsonObject lang = JsonParser.parseString(new String(Files.readAllBytes(Paths.get("tools/elite/items_lang.json")), StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> blockIds = new HashSet<>(EliteIds.crates());
        blockIds.addAll(EliteIds.plushies());
        for (String id : EliteIds.allItems()) {
            String key = (blockIds.contains(id) ? "block.nusmp." : "item.nusmp.") + id;
            assertTrue(lang.has(key), "lang fragment lacks " + key);
            assertFalse(lang.get(key).getAsString().isBlank());
        }
        assertTrue(lang.has("itemGroup.nusmp.elite_items"));
        for (String id : EliteIds.WEAPONS) for (String part : List.of("lore", "primary", "alt")) assertTrue(lang.has("tooltip.nusmp." + id + "." + part), id + " tooltip " + part);
    }

    @Test
    void soundEventsHaveEntriesAndFiles() throws Exception {
        JsonObject s = JsonParser.parseString(text("/assets/nusmp/sounds.json")).getAsJsonObject();
        assertTrue(s.has("zagred_word"), "the older sound entries must still be there");
        for (String ev : List.of("plush_squeak", "crate_unlock", "crate_reveal", "crate_locked")) {
            assertTrue(s.has(ev), "sounds.json lacks " + ev);
            for (var el : s.getAsJsonObject(ev).getAsJsonArray("sounds")) {
                String name = el.getAsJsonObject().get("name").getAsString();
                assertTrue(name.startsWith("nusmp:"));
                assertTrue(exists("/assets/nusmp/sounds/" + name.substring(6) + ".ogg"), "missing " + name + ".ogg");
            }
        }
    }

    @Test
    void healthPotionsAreCraftableAndChain() throws Exception {
        for (HealthPotionTier t : HealthPotionTier.values()) {
            String r = text("/data/nusmp/recipe/" + t.itemId() + ".json");
            assertTrue(r.contains("\"nusmp:" + t.itemId() + "\""), t.itemId() + " recipe result");
        }
        assertTrue(text("/data/nusmp/recipe/health_potion_medium.json").contains("nusmp:health_potion_small"));
        assertTrue(text("/data/nusmp/recipe/health_potion_large.json").contains("nusmp:health_potion_medium"));
    }

    @Test
    void healthPotionsGrowWithSize() {
        HealthPotionTier s = HealthPotionTier.SMALL, m = HealthPotionTier.MEDIUM, l = HealthPotionTier.LARGE;
        assertTrue(s.totalHeal() < m.totalHeal() && m.totalHeal() < l.totalHeal());
        assertTrue(s.useTicks() < m.useTicks() && m.useTicks() < l.useTicks(), "bigger potions take longer to drink");
        assertTrue(l.stack() < s.stack(), "the large potion stacks less");
        assertEquals(8f, s.totalHeal(), 1e-6);
        assertEquals(16f, m.totalHeal(), 1e-6);
        assertEquals(28f, l.totalHeal(), 1e-6);
        assertEquals("health_potion_large", l.itemId());
        assertTrue(l.absorb() > 0 && s.absorb() == 0);
    }
}
