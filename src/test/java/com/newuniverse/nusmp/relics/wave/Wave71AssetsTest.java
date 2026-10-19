package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every weapon of wave 71 is a real 3D model in hand (a baked mesh, a skin, a glow skin, an item model that hands over to the renderer, the sprite as the inventory icon),
 * and the data the other systems keep per weapon must have its row: the size class (assets and tools copy), the Better Combat file of a melee weapon, the dungeon road of the
 * Crucible. This test reads the files (no game needed); the size and orientation rules are WeaponMeshesTest's, the bow rules BowDisplayGuardTest's.
 */
class Wave71AssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = Path.of("src/main/resources/data/nusmp");

    private static List<String> ids() throws IOException {
        Matcher m = Pattern.compile("\"([a-z0-9_]+)\"").matcher(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/wave/Wave71.java")).split("IDS = \\{")[1].split("\\};")[0]);
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static boolean ranged(String id) { return id.endsWith("_bow") || id.endsWith("_crossbow"); }

    @Test
    void everyWeaponHasItsMeshSkinsSpriteAndItemModels() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : ids()) {
            for (String f : new String[]{"weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png", "textures/item/" + id + ".png",
                    "models/item/" + id + ".json", "models/item/" + id + "_icon.json"})
                if (!Files.exists(ASSETS.resolve(f))) bad.add(f);
            Path model = ASSETS.resolve("models/item/" + id + ".json");
            if (Files.exists(model) && !Files.readString(model).contains("\"minecraft:builtin/entity\"")) bad.add(id + ": the item model is not builtin/entity");
            Path icon = ASSETS.resolve("models/item/" + id + "_icon.json");
            if (Files.exists(icon) && !Files.readString(icon).contains("\"nusmp:item/" + id + "\"")) bad.add(id + ": the icon model does not show the sprite");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theSpriteIs16x16AndTheMeshIsAGoodMesh() throws IOException {
        for (String id : ids()) {
            byte[] png = Files.readAllBytes(ASSETS.resolve("textures/item/" + id + ".png"));
            int w = ((png[16] & 255) << 24) | ((png[17] & 255) << 16) | ((png[18] & 255) << 8) | (png[19] & 255);
            int h = ((png[20] & 255) << 24) | ((png[21] & 255) << 16) | ((png[22] & 255) << 8) | (png[23] & 255);
            assertEquals(16, w, id + " sprite width");
            assertEquals(16, h, id + " sprite height");
            List<String> lines = Files.readAllLines(ASSETS.resolve("weapon_meshes/" + id + ".txt"));
            int quads = 0, glow = 0;
            boolean place = false;
            for (String l : lines) {
                if (l.startsWith("place ")) place = true;
                if (l.startsWith("qn 0 ") || l.startsWith("q 0 ")) quads++;
                if (l.startsWith("qn 2 ") || l.startsWith("q 2 ")) glow++;
                if (l.startsWith("qn ") || l.startsWith("q ")) {
                    for (String t : l.split("\\s+")) {
                        if (t.equals("NaN") || t.equals("Infinity") || t.equals("-Infinity")) throw new AssertionError(id + ": a bad number in the mesh");
                    }
                }
            }
            assertTrue(place, id + " has a place line");
            assertTrue(quads >= 300 && quads <= 3500, id + " body quads " + quads);
            assertTrue(glow > 20, id + " has a glow layer: " + glow);
        }
    }

    @Test
    void everyWeaponHasASizeClassInBothTables() throws IOException {
        String a = Files.readString(ASSETS.resolve("weapon_classes.json")), b = Files.readString(Path.of("tools/relics/weapon_classes.json"));
        List<String> bad = new ArrayList<>();
        for (String id : ids()) {
            if (!a.contains("\"" + id + "\": \"")) bad.add(id + " (assets)");
            if (!b.contains("\"" + id + "\": {")) bad.add(id + " (tools)");
        }
        assertEquals(List.of(), bad);
        for (String bow : new String[]{"silver_eagles_galewing_bow", "starforged_aeon_bow"}) assertTrue(a.contains("\"" + bow + "\": \"bow\""));
        assertTrue(a.contains("\"dawnpiercer_crossbow\": \"crossbow\""));
    }

    @Test
    void theBowsAndTheCrossbowHaveTheirDrawStates() throws IOException {
        for (String id : ids()) {
            if (!ranged(id)) continue;
            for (String s : new String[]{"_pull1", "_pull2", "_pull3"}) assertTrue(Files.exists(ASSETS.resolve("weapon_meshes/" + id + s + ".txt")), id + s);
            if (id.endsWith("_crossbow")) assertTrue(Files.exists(ASSETS.resolve("weapon_meshes/" + id + "_charged.txt")), id + "_charged");
        }
    }

    @Test
    void everyMeleeWeaponHasABetterCombatFile() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : ids()) {
            String cls = Pattern.compile("\"" + id + "\": \"([a-z]+)\"").matcher(Files.readString(ASSETS.resolve("weapon_classes.json"))).results().map(r -> r.group(1)).findFirst().orElse("");
            boolean melee = !(cls.equals("bow") || cls.equals("crossbow") || cls.equals("shield") || cls.equals("item") || cls.equals("handitem"));
            if (melee && !Files.exists(DATA.resolve("weapon_attributes/" + id + ".json"))) bad.add(id);
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyWeaponLeadsToADungeonRoad() throws IOException {
        String themes = Files.readString(DATA.resolve("mega/relic_themes.json"));
        for (String id : ids()) assertTrue(themes.contains("\"" + id + "\": \""), "no relic_themes.json row for " + id);
    }
}
