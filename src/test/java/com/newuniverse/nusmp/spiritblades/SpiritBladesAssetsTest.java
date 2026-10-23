package com.newuniverse.nusmp.spiritblades;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The files the two Wind Spirit blades need: the 3D models with skin, glow and icon (and the bow's draw stages), the class, grip and Better Combat rows, the engravings, the lang text, and that nothing hands them out. */
class SpiritBladesAssetsTest {
    private static final String SWORD = "spirit_of_zephyr", BOW = "spirit_of_boreas";
    private static final Path RES = Path.of("src", "main", "resources"), ASSETS = RES.resolve("assets/nusmp");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(read(p)).getAsJsonObject(); }

    @Test
    void bothBladesHaveTheirModelsMeshSkinsAndIcon() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String id : new String[]{SWORD, BOW})
            for (String f : new String[]{"models/item/" + id + ".json", "models/item/" + id + "_icon.json", "weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png",
                    "textures/entity/weapon/" + id + "_glow.png", "textures/item/" + id + ".png"})
                if (!Files.exists(ASSETS.resolve(f))) missing.add(f);
        assertTrue(missing.isEmpty(), "missing " + missing);
        for (String id : new String[]{SWORD, BOW}) {
            assertEquals("minecraft:builtin/entity", json(ASSETS.resolve("models/item/" + id + ".json")).get("parent").getAsString(), "the 3D model draws through the weapon renderer");
            assertEquals("nusmp:item/" + id, json(ASSETS.resolve("models/item/" + id + "_icon.json")).getAsJsonObject("textures").get("layer0").getAsString(), "the old-style sprite is the inventory icon");
            BufferedImage icon = javax.imageio.ImageIO.read(ASSETS.resolve("textures/item/" + id + ".png").toFile());
            assertEquals(16, icon.getWidth());
            assertEquals(16, icon.getHeight());
        }
    }

    @Test
    void theMeshesAreRealModelsWithAGlowSkin() throws IOException {
        for (String id : new String[]{SWORD, BOW}) {
            String mesh = read(ASSETS.resolve("weapon_meshes/" + id + ".txt"));
            assertTrue(mesh.lines().anyMatch(l -> l.startsWith("glow ")), id + ": an additive glow skin");
            assertTrue(mesh.lines().filter(l -> l.startsWith("qn ")).count() > 600, id + ": a real model, hundreds of quads");
            assertTrue(mesh.lines().anyMatch(l -> l.startsWith("place ")), id + ": placed on the sprite diagonal");
        }
        assertTrue(read(ASSETS.resolve("weapon_meshes/" + SWORD + ".txt")).contains("\ngripxyz "), "the hand holds the wrapped grip: a gripxyz line");
        // the bow has its three draw stages, each with the arrow of light on the nock (so each is bigger than the rest mesh)
        long rest = Files.size(ASSETS.resolve("weapon_meshes/" + BOW + ".txt"));
        for (String s : new String[]{"_pull1", "_pull2", "_pull3"}) {
            Path p = ASSETS.resolve("weapon_meshes/" + BOW + s + ".txt");
            assertTrue(Files.exists(p), p.toString());
            assertTrue(Files.size(p) > rest, s + " carries the arrow");
        }
    }

    @Test
    void theClassGripAndBetterCombatRowsExist() throws IOException {
        String classes = read(ASSETS.resolve("weapon_classes.json"));
        assertTrue(classes.contains("\"" + SWORD + "\": \"longsword\""), "class row of the sword (game copy)");
        assertTrue(classes.contains("\"" + BOW + "\": \"bow\""), "class row of the bow (game copy)");
        String tools = read(Path.of("tools", "relics", "weapon_classes.json"));
        assertTrue(tools.contains("\"" + SWORD + "\": {") && tools.contains("\"" + BOW + "\": {"), "class rows (tools copy)");
        String golden = read(Path.of("tools", "weapon_grip", "grip_golden.json"));
        for (String id : new String[]{SWORD, BOW}) assertTrue(golden.contains("\"" + id + "\""), "the grip golden knows " + id);
        assertEquals("bettercombat:sword", json(RES.resolve("data/nusmp/weapon_attributes/" + SWORD + ".json")).get("parent").getAsString());
        String bc = read(Path.of("tools", "bettercombat", "manifest.json"));
        assertTrue(bc.contains("\"" + SWORD + "\": {"), "the sword is in the Better Combat inventory");
        assertTrue(bc.contains("\"" + BOW + "\": \"ranged (bow)"), "the bow is excluded as ranged");
    }

    @Test
    void theEngravingsAreTheOnesTheOwnerNamed() throws IOException {
        String eng = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "item", "WeaponEngravings.java"));
        int at = eng.indexOf("Map.entry(\"" + SWORD + "\"");
        assertTrue(at > 0);
        String line = eng.substring(at, eng.indexOf('\n', at));
        for (String e : new String[]{"severance", "barrier_piercing", "holy_weapon"}) assertTrue(line.contains("e(\"" + e + "\""), SWORD + " " + e);
        at = eng.indexOf("Map.entry(\"" + BOW + "\"");
        assertTrue(at > 0);
        line = eng.substring(at, eng.indexOf('\n', at));
        assertTrue(line.contains("e(\"magic_interference\""), BOW + " magic_interference");
    }

    @Test
    void theLeafSpriteIsBlackUnderItsTransparentTexels() throws IOException {
        BufferedImage im = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/weapon/spirit_blades_leaf.png").toFile());
        assertEquals(32, im.getWidth());
        assertTrue(im.getColorModel().hasAlpha());
        int opaque = 0;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int argb = im.getRGB(x, y);
                if ((argb >>> 24) < 4) assertTrue((argb & 0xFFFFFF) == 0, "colour under a transparent texel at " + x + "," + y + " (it is drawn additively: docs/vfx_square_bug.md)");
                else opaque++;
            }
        }
        assertTrue(opaque > 60 && opaque < 600, "a leaf, not a box: " + opaque);
    }

    @Test
    void theLangFragmentHasEveryString() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "spirit_blades.json"));
        for (String id : new String[]{SWORD, BOW}) {
            assertTrue(lang.has("item.nusmp." + id), id);
            assertTrue(lang.has("item.nusmp." + id + ".lore"), id + " lore");
        }
        assertEquals("Spirit of Zephyr", lang.get("item.nusmp." + SWORD).getAsString());
        assertEquals("Spirit of Boreas", lang.get("item.nusmp." + BOW).getAsString());
    }

    @Test
    void theBladesAreCreativeOnlyNothingHandsThemOut() throws IOException {
        String tab = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "NUCreativeTab.java"));
        assertTrue(tab.contains("SpiritBladesBoot.creativeItems()"), "in the creative tab");
        List<String> hits = new ArrayList<>();
        try (Stream<Path> files = Files.walk(RES.resolve("data"))) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json"))::iterator) {
                String name = f.toString().replace('\\', '/');
                if (name.contains("/weapon_attributes/") || name.contains("/relics/") || name.contains("/curios/")) continue;     // data tables of weapons, not ways to get one
                String t = read(f);
                if (t.contains("\"nusmp:" + SWORD + "\"") || t.contains("\"nusmp:" + BOW + "\"")) hits.add(name);
            }
        }
        assertTrue(hits.isEmpty(), "a loot table, recipe, tag or advancement names a blade: " + hits);
        assertFalse(Files.exists(RES.resolve("data/nusmp/recipe/" + SWORD + ".json")));
        assertFalse(Files.exists(RES.resolve("data/nusmp/recipe/" + BOW + ".json")));
    }

    @Test
    void theWeaponRendererDrawsThem() throws IOException {
        String r = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "client", "WeaponRenderer.java"));
        assertTrue(r.contains("SpiritBladesBoot.renderables()"), "listed as 3D weapons");
        assertTrue(r.contains("SpiritBladesFx.extras("), "the leaves are drawn");
        String v = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "client", "relics", "item", "ItemStateVariants.java"));
        assertTrue(v.contains("spiritblades.BoreasBowItem"), "the bow's draw stages are chosen (its pull meshes carry the arrow)");
    }

    @Test
    void thePreviewsAndTheDocAreThere() {
        for (String f : new String[]{"turntable_zephyr.png", "turntable_boreas.png", "bow_draw_stages.png", "inventory_icons.png", "in_hand_grip_sheet.png"})
            assertTrue(Files.exists(Path.of("docs", "previews", "spirit_blades", f)), f);
        assertTrue(Files.exists(Path.of("docs", "magic", "spirit_blades.md")));
    }
}
