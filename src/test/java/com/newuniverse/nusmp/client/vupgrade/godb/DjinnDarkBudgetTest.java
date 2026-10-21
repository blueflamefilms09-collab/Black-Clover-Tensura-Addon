package com.newuniverse.nusmp.client.vupgrade.godb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The performance budget and the data of the 36 upgraded dark Djinn weapons (release 1.108.0, slice djinn_dark_weapons): every one has a data table the God melee (b) code reads, a level-of-detail
 * mesh with at most 15% of the quads, a full-detail mesh (with its orbiting pieces) inside the God cap of 2500 welded vertices and over the floor of 600 quads, an orbiting piece, a 1024 skin
 * with a core glow map and a separate vein glow map, and the old files the fallback draws are still shipped; the placement in the hand is the old one; and every particle layer is the mod's own
 * particle ({@code fx}), never a vanilla one.
 */
class DjinnDarkBudgetTest {
    static final Path RES = GodMeleeBBudgetTest.RES;
    static final Path TEX = RES.resolve("textures/entity/weapon");

    /** The ids of the 36 dark Djinn weapons, in roster order, from docs/djinn_roster_36.json. */
    static List<String> ids() throws IOException {
        JsonArray roster = JsonParser.parseString(Files.readString(Path.of("docs", "djinn_roster_36.json"))).getAsJsonArray();
        List<String> out = new ArrayList<>();
        for (JsonElement e : roster) out.add("djinn_weapon_" + e.getAsJsonObject().get("id").getAsString() + "_dark");
        return out;
    }

    static UpgradeSpec spec(String id) throws IOException {
        return UpgradeSpec.parse(UpgradeSpecTest.read(id));
    }

    @Test
    void thereAreThirtySixTablesAndTheyAllParse() throws IOException {
        List<String> ids = ids();
        assertEquals(36, ids.size());
        for (String id : ids) {
            UpgradeSpec s = spec(id);
            assertEquals(id, s.item());
            assertEquals("up/" + id, s.mesh());
            assertEquals(UpgradeSpec.LAYER_KINDS.size(), s.layers().size(), id + ": seven particle layers");
            assertTrue(s.orbitPieces() >= 4 && s.orbitPieces() <= 12, id + ": orbiting pieces " + s.orbitPieces());
            assertTrue(s.trail().ghosts() >= 3, id + ": swing ghosts");
            assertTrue(s.passes().size() >= 5, id + ": passes " + s.passes());
            assertTrue(s.ambientLayers().size() >= 8, id + ": ambient layers " + s.ambientLayers());
            assertTrue(s.animations().size() >= 6, id + ": idle animations " + s.animations());
            for (String k : UpgradeSpec.LAYER_KINDS) assertTrue(s.ambientLayers().contains(k), id + " lists " + k);
            for (UpgradeSpec.Layer l : s.layers()) assertEquals("fx", l.particle(), id + ": layer " + l.kind() + " must use the mod's own particle");
        }
    }

    @Test
    void everyWeaponHasALevelOfDetailMeshWithinTheBudget() throws IOException {
        for (String id : ids()) {
            UpgradeSpec s = spec(id);
            GodMeleeBBudgetTest.MeshStat full = GodMeleeBBudgetTest.stat(RES.resolve("weapon_meshes/" + s.mesh() + ".txt"));
            GodMeleeBBudgetTest.MeshStat lod = GodMeleeBBudgetTest.stat(RES.resolve("weapon_meshes/" + s.lodMesh() + ".txt"));
            GodMeleeBBudgetTest.MeshStat orbit = GodMeleeBBudgetTest.stat(RES.resolve("weapon_meshes/" + s.orbitMesh() + ".txt"));
            assertTrue(full.body() >= GodMeleeBBudgetTest.GOD_QUAD_FLOOR, id + ": " + full.body() + " quads, the floor of a God hand weapon is 600");
            assertTrue(lod.body() > 0, id + ": the level-of-detail mesh is empty");
            assertTrue(lod.body() <= 0.15 * full.body(), id + ": the level-of-detail mesh has " + lod.body() + " of " + full.body() + " quads (limit 15%)");
            assertTrue(lod.glow() > 0, id + ": the level-of-detail mesh keeps a glow layer");
            assertTrue(orbit.body() > 0 && orbit.body() <= 80, id + ": the orbiting piece has " + orbit.body() + " quads");
            int fullDetail = full.welded() + s.orbitPieces() * orbit.welded();
            assertTrue(fullDetail <= GodMeleeBBudgetTest.GOD_WELDED_CAP, id + ": " + fullDetail + " welded vertices at full detail, the God cap is " + GodMeleeBBudgetTest.GOD_WELDED_CAP);
            assertTrue(lod.welded() <= 0.15 * GodMeleeBBudgetTest.GOD_WELDED_CAP, id + ": the level-of-detail mesh has " + lod.welded() + " welded vertices");
        }
    }

    @Test
    void theNewSkinsAreBiggerAndTheOldFilesAreKept() throws IOException {
        for (String id : ids()) {
            UpgradeSpec s = spec(id);
            Path skin = TEX.resolve(s.mesh() + ".png");
            assertTrue(Files.exists(skin) && Files.exists(TEX.resolve(s.mesh() + "_glow.png")) && Files.exists(TEX.resolve(s.mesh() + "_glow2.png")), id + ": skin, core glow and vein glow");
            assertTrue(Files.exists(RES.resolve("weapon_meshes/" + id + ".txt")) && Files.exists(TEX.resolve(id + ".png")) && Files.exists(TEX.resolve(id + "_glow.png")), id + ": the old mesh and skins are kept (add the new, keep the old)");
            BufferedImage im = ImageIO.read(skin.toFile());
            assertTrue(im.getWidth() >= 1024 && im.getWidth() == im.getHeight(), id + ": the skin is " + im.getWidth() + " px");
            long before = GodMeleeBBudgetTest.painted(TEX.resolve(id + ".png")) + GodMeleeBBudgetTest.painted(TEX.resolve(id + "_glow.png"));
            long after = GodMeleeBBudgetTest.painted(skin) + GodMeleeBBudgetTest.painted(TEX.resolve(s.mesh() + "_glow.png")) + GodMeleeBBudgetTest.painted(TEX.resolve(s.mesh() + "_glow2.png"));
            assertTrue(after >= 4 * before, id + ": painted texels " + before + " -> " + after + " (target 4x)");
        }
    }

    @Test
    void theMeshesCarryTheDirectivesTheRendererNeeds() throws IOException {
        for (String id : ids()) {
            for (String suffix : new String[]{"", "_lod"}) {
                List<String> lines = Files.readAllLines(RES.resolve("weapon_meshes/up/" + id + suffix + ".txt"));
                assertTrue(lines.stream().anyMatch(l -> l.startsWith("place ")), id + suffix + ": place line");
                assertTrue(lines.stream().anyMatch(l -> l.trim().equals("orient manual")), id + suffix + ": orient manual (the runtime normaliser never touches it)");
                assertTrue(lines.stream().anyMatch(l -> l.startsWith("glow ")), id + suffix + ": glow line");
            }
            String oldPlace = Files.readAllLines(RES.resolve("weapon_meshes/" + id + ".txt")).stream().filter(l -> l.startsWith("place ")).findFirst().orElseThrow();
            String newPlace = Files.readAllLines(RES.resolve("weapon_meshes/up/" + id + ".txt")).stream().filter(l -> l.startsWith("place ")).findFirst().orElseThrow();
            assertEquals(oldPlace, newPlace, id + ": the placement in the hand is the old one");
        }
    }

    @Test
    void everyOldQuadIsStillInTheUpgradedMesh() throws IOException {
        // add the new, keep the old: every body quad of the old mesh (position and normal) is a quad of the upgraded one (only its UV moved to the enlarged skin)
        for (String id : ids()) {
            java.util.Set<String> now = new java.util.HashSet<>();
            for (String line : Files.readAllLines(RES.resolve("weapon_meshes/up/" + id + ".txt"))) {
                String[] v = line.trim().split("\\s+");
                if (v[0].equals("qn") && v[1].equals("0")) now.add(key(v));
            }
            int missing = 0, total = 0;
            for (String line : Files.readAllLines(RES.resolve("weapon_meshes/" + id + ".txt"))) {
                String[] v = line.trim().split("\\s+");
                if (v[0].equals("qn") && v[1].equals("0")) {
                    total++;
                    if (!now.contains(key(v))) missing++;
                }
            }
            assertTrue(total > 100, id);
            assertEquals(0, missing, id + ": old body quads missing from the upgraded mesh");
        }
    }

    private static String key(String[] v) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 4; k++) {
            int o = 2 + 8 * k;
            sb.append(String.format("%.2f,%.2f,%.2f;", Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2])));
        }
        return sb.toString();
    }

    @Test
    void theTablesBelongToTheDarkWeaponsOfTheRoster() throws IOException {
        JsonObject first = UpgradeSpecTest.read(ids().get(0));
        assertEquals("djinn_weapon_baal_dark", first.get("item").getAsString());
        try (var s = Files.list(UpgradeSpecTest.DIR)) {
            long n = s.filter(p -> p.getFileName().toString().startsWith("djinn_weapon_") && p.toString().endsWith(".json")).count();
            assertEquals(36, n, "one table per dark Djinn weapon and none for the light ones (they are Legend grade)");
        }
    }
}
