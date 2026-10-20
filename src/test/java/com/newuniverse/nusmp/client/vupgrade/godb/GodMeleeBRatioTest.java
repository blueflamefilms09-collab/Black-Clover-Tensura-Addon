package com.newuniverse.nusmp.client.vupgrade.godb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The measured proxies of docs/visual_upgrade/god_melee_b_after.json against the targets of the brief for GOD grade, and against the files they were measured from (the numbers cannot
 * drift away from the assets): V_after >= 5 x V_before (the weights are in the file), >= 4x the painted texels, a separate emissive map, >= 3 ambient layers, an idle animation, a
 * swing trail, at least 600 quads. The mesh ratio itself is NOT a target here: the brief's 5x quads does not fit its own 2500-vertex cap (docs/visual_upgrade/god_melee_b.md).
 */
class GodMeleeBRatioTest {
    static final Path DOCS = Path.of("docs", "visual_upgrade");

    static JsonObject read(String name) throws IOException {
        return JsonParser.parseString(Files.readString(DOCS.resolve(name))).getAsJsonObject();
    }

    @Test
    void everyItemMeetsTheGodTargets() throws IOException {
        JsonObject after = read("god_melee_b_after.json");
        JsonArray items = after.getAsJsonArray("items");
        assertEquals(9, items.size());
        for (JsonElement e : items) {
            JsonObject it = e.getAsJsonObject();
            String id = it.get("item").getAsString();
            assertEquals("GOD", it.get("grade").getAsString());
            assertTrue(it.get("V_after").getAsDouble() >= 5.0, id + ": V_after " + it.get("V_after"));
            JsonObject r = it.getAsJsonObject("ratios");
            assertTrue(r.get("texture").getAsDouble() >= 4.0, id + ": painted texels x" + r.get("texture"));
            assertTrue(r.get("mesh").getAsDouble() >= 1.0, id + ": the mesh never gets smaller");
            assertTrue(it.getAsJsonObject("mesh").get("quads_full_detail").getAsInt() >= 600, id + ": 600 quads floor");
            assertTrue(it.getAsJsonObject("texture").getAsJsonArray("emissive_maps").size() >= 2, id + ": a separate emissive glow map");
            assertTrue(it.getAsJsonObject("ambient_layers").get("count").getAsInt() >= 3, id + ": ambient layers");
            assertTrue(it.getAsJsonObject("idle_animations").get("count").getAsInt() >= 3, id + ": idle animations");
            assertTrue(it.get("swing_trail").getAsBoolean() && it.get("unique_act_vfx").getAsBoolean(), id + ": swing trail and act effects");
        }
    }

    @Test
    void theRecordedNumbersAreTheOnesInTheMeshFiles() throws IOException {
        JsonObject after = read("god_melee_b_after.json");
        for (JsonElement e : after.getAsJsonArray("items")) {
            JsonObject it = e.getAsJsonObject();
            String id = it.get("item").getAsString();
            UpgradeSpec s = UpgradeSpec.parse(UpgradeSpecTest.read(id));
            GodMeleeBBudgetTest.MeshStat full = GodMeleeBBudgetTest.stat(GodMeleeBBudgetTest.RES.resolve("weapon_meshes/" + s.mesh() + ".txt"));
            GodMeleeBBudgetTest.MeshStat orbit = GodMeleeBBudgetTest.stat(GodMeleeBBudgetTest.RES.resolve("weapon_meshes/" + s.orbitMesh() + ".txt"));
            assertEquals(full.body() + s.orbitPieces() * orbit.body(), it.getAsJsonObject("mesh").get("quads_full_detail").getAsInt(), id + ": quads_full_detail");
            assertEquals(s.passes().size(), it.getAsJsonObject("passes").get("count").getAsInt(), id + ": passes");
            assertEquals(s.ambientLayers().size(), it.getAsJsonObject("ambient_layers").get("count").getAsInt(), id + ": ambient layers");
            assertEquals(s.animations().size(), it.getAsJsonObject("idle_animations").get("count").getAsInt(), id + ": idle animations");
        }
    }

    @Test
    void theBaselineIsTheOldStateOfTheSameNineItems() throws IOException {
        JsonObject base = read("god_melee_b_baseline.json");
        JsonArray items = base.getAsJsonArray("items");
        assertEquals(9, items.size());
        for (JsonElement e : items) {
            JsonObject it = e.getAsJsonObject();
            String id = it.get("item").getAsString();
            assertTrue(it.getAsJsonObject("mesh").get("quads_body").getAsInt() > 0, id);
            assertEquals(1, it.getAsJsonObject("ambient_layers").get("count").getAsInt(), id + ": the old look had the grade shimmer only");
            assertEquals(1, it.getAsJsonObject("idle_animations").get("count").getAsInt(), id + ": the old look had the glow pulse only");
            assertFalse(it.get("swing_trail").getAsBoolean(), id);
        }
    }
}
