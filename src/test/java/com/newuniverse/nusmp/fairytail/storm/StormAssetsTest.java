package com.newuniverse.nusmp.fairytail.storm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The files the Stormcaller Staff and its storms need: the 3D model with skin, glow and icon, the class and grip rows, the Better Combat file, the franchise row, the textures of the effect and the lang text. */
class StormAssetsTest {
    private static final String ID = "stormcaller_staff";
    private static final Path RES = Path.of("src", "main", "resources"), ASSETS = RES.resolve("assets/nusmp");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(read(p)).getAsJsonObject(); }

    @Test
    void theStaffHasItsModelsMeshSkinsAndIcon() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"models/item/" + ID + ".json", "models/item/" + ID + "_icon.json", "weapon_meshes/" + ID + ".txt", "textures/entity/weapon/" + ID + ".png",
                "textures/entity/weapon/" + ID + "_glow.png", "textures/item/" + ID + ".png"})
            if (!Files.exists(ASSETS.resolve(f))) missing.add(f);
        assertTrue(missing.isEmpty(), "missing " + missing);
        assertEquals("minecraft:builtin/entity", json(ASSETS.resolve("models/item/" + ID + ".json")).get("parent").getAsString(), "the 3D model draws through the weapon renderer");
        assertEquals("nusmp:item/" + ID, json(ASSETS.resolve("models/item/" + ID + "_icon.json")).getAsJsonObject("textures").get("layer0").getAsString(), "the old sprite is the inventory icon");
        BufferedImage icon = javax.imageio.ImageIO.read(ASSETS.resolve("textures/item/" + ID + ".png").toFile());
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
    }

    @Test
    void theMeshIsAStaffHeldOnItsGrip() throws IOException {
        String mesh = read(ASSETS.resolve("weapon_meshes/" + ID + ".txt"));
        assertTrue(mesh.contains("\ngripxyz "), "the hand holds the wrapped grip: a gripxyz line");
        assertTrue(mesh.lines().filter(l -> l.startsWith("qn ")).count() > 3000, "a real model: thousands of quads");
        assertTrue(mesh.lines().anyMatch(l -> l.startsWith("glow ")), "an additive glow skin");
        assertTrue(read(ASSETS.resolve("weapon_classes.json")).contains("\"" + ID + "\": \"staff\""), "class row (game copy)");
        assertTrue(read(Path.of("tools", "relics", "weapon_classes.json")).contains("\"" + ID + "\": {"), "class row (tools copy)");
        String golden = read(Path.of("tools", "weapon_grip", "grip_golden.json"));
        int at = golden.indexOf("\"" + ID + "\"");
        assertTrue(at > 0, "the grip golden knows the staff");
        assertTrue(golden.substring(at, at + 200).contains("\"source\": \"gripxyz\""), "the grip comes from the mesh's own point");
    }

    @Test
    void betterCombatKnowsTheStaffAsAStaff() throws IOException {
        JsonObject bc = json(RES.resolve("data/nusmp/weapon_attributes/" + ID + ".json"));
        assertEquals("bettercombat:staff", bc.get("parent").getAsString());
        assertTrue(read(Path.of("tools", "bettercombat", "manifest.json")).contains("\"" + ID + "\": {"));
    }

    @Test
    void theStaffIsFairyTailAndEngraved() throws IOException {
        assertTrue(read(RES.resolve("data/nusmp/relics/franchise_map.json")).contains("\"" + ID + "\": \"fairy_tail\""));
        assertTrue(read(Path.of("tools", "relics", "franchise_overrides.json")).contains("\"" + ID + "\": \"fairy_tail\""));
        String eng = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "item", "WeaponEngravings.java"));
        int at = eng.indexOf("Map.entry(\"" + ID + "\"");
        assertTrue(at > 0);
        String line = eng.substring(at, eng.indexOf('\n', at));
        for (String e : new String[]{"elemental_boost", "magic_weapon", "barrier_piercing", "swift"}) assertTrue(line.contains("e(\"" + e + "\""), e);
    }

    @Test
    void theEffectTexturesExistAndAreLuminanceAlpha() throws IOException {
        for (String t : new String[]{"cloud", "rain", "bolt", "hail", "wave", "funnel"}) {
            Path p = ASSETS.resolve("textures/particle/storm_" + t + ".png");
            assertTrue(Files.exists(p), p.toString());
            BufferedImage im = javax.imageio.ImageIO.read(p.toFile());
            assertTrue(im.getWidth() >= 32 && im.getHeight() >= 32, t);
            assertTrue(im.getColorModel().hasAlpha(), t + " has an alpha channel");
        }
        // the additive bolt is black under its transparent texels (docs/vfx_square_bug.md): it can never light up as a square
        BufferedImage bolt = javax.imageio.ImageIO.read(ASSETS.resolve("textures/particle/storm_bolt.png").toFile());
        int checked = 0;
        for (int y = 0; y < bolt.getHeight(); y++) {
            for (int x = 0; x < bolt.getWidth(); x++) {
                int argb = bolt.getRGB(x, y);
                if ((argb >>> 24) < 4) {
                    assertTrue(((argb >> 16) & 255) < 8, "colour under a transparent texel of the bolt at " + x + "," + y);
                    checked++;
                }
            }
        }
        assertTrue(checked > 0);
    }

    @Test
    void theLayerNamesOnlyTexturesThatExist() throws IOException {
        String layer = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "vfx", "client", "layer", "StormLayer.java"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("byName\\(\"(storm_[a-z]+)\"\\)").matcher(layer);
        int n = 0;
        while (m.find()) {
            assertTrue(Files.exists(ASSETS.resolve("textures/particle/" + m.group(1) + ".png")), m.group(1));
            n++;
        }
        assertEquals(6, n);
    }

    @Test
    void theLangFragmentHasEveryString() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "storm_staff.json"));
        for (String k : new String[]{"item.nusmp." + ID, "item.nusmp." + ID + ".lore", "nusmp.storm.tooltip.magic", "nusmp.storm.selected", "nusmp.storm.no_energy", "nusmp.storm.cooldown",
                "nusmp.storm.sky_full", "nusmp.skill.book_storm", "nusmp.skill.book_storm.description", "nusmp.skill.grimoire_storm"})
            assertTrue(lang.has(k), k);
        for (StormSpell s : StormSpell.values()) {
            assertTrue(lang.has("nusmp.storm.spell." + s.id), s.id);
            assertEquals(s.description, lang.get("nusmp.storm.spell." + s.id + ".desc").getAsString(), "the text of " + s.id + " is the table's");
            assertEquals(s.displayName, lang.get("nusmp.storm.spell." + s.id).getAsString());
        }
        assertTrue(lang.get("nusmp.skill.book_storm").getAsString().contains("Fairy Tail"), "the magic UI labels Storm Magic as Fairy Tail");
        for (var e : lang.entrySet()) {
            String v = e.getValue().getAsString().toLowerCase();
            assertFalse(v.contains("hunger") || v.contains("starv") || v.contains("food"), e.getKey());
        }
    }

    @Test
    void theHeadlessPreviewSceneAndPicturesAreThere() throws IOException {
        JsonObject scenes = json(Path.of("tools", "vfx_preview", "scenes.d", "storm_staff.json"));
        for (String s : new String[]{"storm_gathering", "storm_downpour", "storm_hailstorm", "storm_thunderhead", "storm_twister", "storm_eye", "storm_gale_front", "storm_surge"})
            assertTrue(scenes.has(s), s);
        for (String f : new String[]{"stormcaller_staff.png", "storm_gathering.png", "storm_hailstorm.png", "storm_thunderhead.png", "storm_twister.png", "storm_eye.png", "storm_gale_front.png", "storm_surge.png"})
            assertTrue(Files.exists(Path.of("docs", "previews", "storm_staff", f)), f);
        assertTrue(Files.exists(Path.of("docs", "magic", "storm_staff.md")));
    }
}
