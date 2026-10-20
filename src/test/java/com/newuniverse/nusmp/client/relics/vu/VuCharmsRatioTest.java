package com.newuniverse.nusmp.client.relics.vu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner asked for God grade items to look 5 times better and Genesis grade 20 times better. "Better looking" cannot be measured, so the upgrade records a proxy score V (documented
 * in docs/visual_upgrade/charms_genesis_worn.md: a weighted sum of the ratios of mesh quads, painted texels, render passes, ambient layers and animation groups, after over before) in
 * docs/visual_upgrade/charms_genesis_worn_after.json next to the baseline taken before any change. This test is the data test of those files: the baseline has all 27 items, the after file
 * has the same items, its numbers are the ones the model files give today (a stale document fails here), V recomputes from its components and meets the target of its grade. It says nothing
 * about how the items look in the game, and the texture term dominates V (see the doc).
 */
class VuCharmsRatioTest {
    static final Path DOCS = Path.of("docs/visual_upgrade");
    static final Path VU = Path.of("src/main/resources/assets/nusmp/visual_upgrade");

    static JsonObject load(String file) throws IOException { return JsonParser.parseString(Files.readString(DOCS.resolve(file))).getAsJsonObject(); }

    static Map<String, JsonObject> byId(JsonObject doc) {
        Map<String, JsonObject> out = new HashMap<>();
        for (JsonElement e : doc.getAsJsonArray("items")) out.put(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject());
        return out;
    }

    @Test
    void theBaselineAndTheAfterFileCoverTheSameTwentySevenItems() throws IOException {
        Map<String, JsonObject> base = byId(load("charms_genesis_worn_baseline.json")), after = byId(load("charms_genesis_worn_after.json"));
        assertEquals(27, base.size());
        assertEquals(base.keySet(), after.keySet());
        int god = 0, genesis = 0;
        for (JsonObject o : base.values()) {
            if (o.get("grade").getAsString().equals("GOD")) god++;
            else genesis++;
            for (String view : new String[]{"hand", "worn"}) {
                JsonObject v = o.getAsJsonObject(view);
                assertTrue(v.get("quads").getAsInt() > 0, o.get("id") + " " + view);
                assertTrue(v.get("painted_texels").getAsInt() > 0);
            }
        }
        assertEquals(18, god);
        assertEquals(9, genesis);
    }

    @Test
    void everyItemMeetsTheTargetOfItsGradeAndVRecomputesFromTheComponents() throws IOException {
        JsonObject doc = load("charms_genesis_worn_after.json");
        JsonObject w = doc.getAsJsonObject("weights");
        double sum = 0;
        for (String k : w.keySet()) sum += w.get(k).getAsDouble();
        assertEquals(1.0, sum, 1e-9, "the weights add up to 1");
        for (JsonElement e : doc.getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            JsonObject c = o.getAsJsonObject("components");
            double v = 0;
            for (String k : w.keySet()) v += w.get(k).getAsDouble() * c.get(k).getAsDouble();
            assertEquals(v, o.get("V_after").getAsDouble(), 0.06, id + ": V does not recompute from its components");
            double target = o.get("grade").getAsString().equals("GOD") ? 5.0 : 20.0;
            assertTrue(o.get("V_after").getAsDouble() >= target, id + " has V " + o.get("V_after") + ", target " + target);
            assertTrue(o.get("meets_target").getAsBoolean());
        }
    }

    @Test
    void theAfterFileMatchesTheModelFilesOnDisk() throws IOException {
        VuOverrides t = VuCharmsBudgetTest.table();
        Map<String, JsonObject> after = byId(load("charms_genesis_worn_after.json"));
        for (String id : t.handIds()) {
            for (String view : new String[]{"hand", "worn"}) {
                VuOverrides.Entry e = view.equals("hand") ? t.hand(id) : t.worn(id);
                int quads = 0, groups = 0;
                for (String p : e.pieces()) {
                    WornMesh m = VuCharmsBudgetTest.model(VuCharmsBudgetTest.name(p));
                    quads += m.quads;
                    groups += m.groups.length - 1;
                }
                JsonObject v = after.get(id).getAsJsonObject(view);
                assertEquals(quads, v.get("quads").getAsInt(), id + " " + view + ": quads in the after file are stale (run tools/visual_upgrade/charms_genesis_worn/report.py)");
                assertEquals(groups, v.get("groups").getAsInt(), id + " " + view + ": groups are stale");
            }
        }
    }

    @Test
    void everyGodItemHasAtLeastFourTimesTheTexelsAndEveryGenesisItemAtLeastEightTimes() throws IOException {
        Map<String, JsonObject> base = byId(load("charms_genesis_worn_baseline.json")), after = byId(load("charms_genesis_worn_after.json"));
        for (String id : base.keySet()) {
            double need = base.get(id).get("grade").getAsString().equals("GOD") ? 4.0 : 8.0;
            for (String view : new String[]{"hand", "worn"}) {
                double b = base.get(id).getAsJsonObject(view).get("painted_texels").getAsDouble(), a = after.get(id).getAsJsonObject(view).get("painted_texels").getAsDouble();
                assertTrue(a >= need * b, id + " " + view + ": " + a + " texels against " + b + " before (need " + need + "x)");
            }
        }
    }

    @Test
    void theMeshRatioIsReportedHonestly() throws IOException {
        // the vertex guard of the performance pass bounds the mesh: an item never has more quads than the cap allows, so the recorded mesh ratio is what the caps leave, not a target
        JsonObject doc = load("charms_genesis_worn_after.json");
        for (JsonElement e : doc.getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            int cap = o.get("grade").getAsString().equals("GOD") ? 2500 : 6000;
            for (String view : new String[]{"hand", "worn"}) assertTrue(o.getAsJsonObject(view).get("vertices").getAsInt() <= cap, o.get("id") + " " + view);
        }
        JsonArray items = doc.getAsJsonArray("items");
        assertEquals(27, items.size());
    }
}
