package com.newuniverse.nusmp.visual.demonslayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.visual.genesis.VuBurst;
import com.newuniverse.nusmp.visual.genesis.VuMesh;
import com.newuniverse.nusmp.visual.genesis.VuRules;
import com.newuniverse.nusmp.visual.genesis.VuSpec;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped assets of the demon_slayer visual upgrade (tools/visual_upgrade/demon_slayer): the Genesis Demon-Slayer has an upgraded mesh, a level-of-detail mesh, skins and effect data and
 * keeps its old model; the mesh is inside the caps of the performance guard and carries the old renderer's placement; the effect data has what a Genesis item needs and what the renderer
 * reads (the shards, the clover and the bloom carry the stack-driven animations); the numbers of docs/visual_upgrade/demon_slayer_after.json meet the floors of the brief and the baseline
 * was taken from the old model before the upgrade.
 */
class DemonSlayerAssetsTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final Path DOCS = Path.of("docs", "visual_upgrade");
    static final String ID = DsCatalog.ID;

    static VuMesh mesh(String id) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(RES.resolve("weapon_meshes_vu").resolve(id + ".txt"), StandardCharsets.UTF_8)) {
            return VuMesh.parse(in);
        }
    }

    static VuSpec spec() throws IOException {
        return VuSpec.parse(Files.readString(RES.resolve("visual_upgrade").resolve(ID + ".json")));
    }

    @Test
    void theUpgradeIsThereAndTheOldModelStays() {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"weapon_meshes_vu/" + ID + ".txt", "weapon_meshes_vu/" + ID + "_lod.txt", "visual_upgrade/" + ID + ".json", "textures/entity/weapon_vu/" + ID + ".png",
                "textures/entity/weapon_vu/" + ID + "_glow.png", "textures/entity/weapon_vu/" + ID + "_veins.png", "textures/entity/weapon_vu/" + ID + "_lod.png", "textures/entity/weapon_vu/halo.png"})
            if (!Files.exists(RES.resolve(f))) bad.add(f + " missing");
        for (String f : new String[]{"textures/entity/demon_slayer_sword.png", "textures/entity/demon_slayer_bloom.png", "textures/entity/demon_slayer_bloom_glow.png", "models/item/demon_slayer_sword.json",
                "models/item/demon_slayer_sword_icon.json", "textures/item/demon_slayer_sword.png"})
            if (!Files.exists(RES.resolve(f))) bad.add(f + ": the old look was removed (it must stay: visual.upgrade = false draws it)");
        assertTrue(Files.exists(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "client", "DemonSlayerMesh.java")), "the old pixel model stays");
        assertEquals(List.of(), bad);
    }

    @Test
    void theMeshIsInsideTheCapsOfThePerformanceGuard() throws IOException {
        VuMesh full = mesh(ID), lod = mesh(ID + "_lod");
        int body = full.quads(VuMesh.BODY), welded = full.weldedVertices(VuMesh.BODY);
        assertTrue(body >= 2500, "body quads " + body);
        assertTrue(welded <= VuRules.MAX_VERTICES_GENESIS, "welded vertices " + welded);
        assertTrue(lod.quads(VuMesh.BODY) > 0 && lod.quads(VuMesh.BODY) <= VuRules.LOD_RATIO_MAX * body, "LOD quads " + lod.quads(VuMesh.BODY));
        assertTrue(lod.weldedVertices(VuMesh.BODY) <= VuRules.LOD_RATIO_MAX * welded, "LOD welded vertices " + lod.weldedVertices(VuMesh.BODY));
        assertEquals(0, lod.quads(VuMesh.HALO), "the LOD mesh carries no halo cards");
        assertTrue(full.quads(VuMesh.HALO) <= 120, "halo card quads " + full.quads(VuMesh.HALO));
        assertTrue(full.quads(VuMesh.GLOW) > 0 && full.quads(VuMesh.VEINS) > 0 && full.quads(VuMesh.VOID) > 0, "every pass has quads");
        assertTrue(!full.flares.isEmpty());
        assertEquals(0xFFFFFF, full.glowRgb, "the glow tint multiplies the skin's own colours");
    }

    @Test
    void theMeshHasTheOldRenderersPlacementAndSaneNumbers() throws IOException {
        VuMesh m = mesh(ID);
        float[] got = {m.ox, m.oy, m.angle, m.scale, m.ymin};
        for (int i = 0; i < 5; i++) assertEquals(DsCatalog.PLACE[i], got[i], 1e-4f * Math.max(1f, Math.abs(DsCatalog.PLACE[i])), "place field " + i);
        assertTrue(m.length() > 0.3f && m.length() < 6f, "length " + m.length());
        List<String> bad = new ArrayList<>();
        for (VuMesh.Group g : m.groups) {
            for (int l = 0; l < VuMesh.LAYERS; l++) {
                float[] a = g.verts[l];
                for (int i = 0; i < g.count[l]; i += VuMesh.FLOATS) {
                    for (int k = 0; k < 3; k++) if (!Float.isFinite(a[i + k])) bad.add("non-finite vertex in " + g.name);
                    if (l == VuMesh.BODY || l == VuMesh.GLOW || l == VuMesh.VEINS) {
                        if (a[i + 3] < -0.01f || a[i + 3] > 128.01f || a[i + 4] < -0.01f || a[i + 4] > 128.01f) bad.add("UV out of the 128 space in " + g.name);
                        float nl = (float) Math.sqrt(a[i + 5] * a[i + 5] + a[i + 6] * a[i + 6] + a[i + 7] * a[i + 7]);
                        if (nl < 0.5f || nl > 1.5f) bad.add("a normal of length " + nl + " in " + g.name);
                    }
                }
            }
            if (bad.size() > 20) break;
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theEffectDataHasWhatAGenesisItemNeedsAndWhatTheRendererReads() throws IOException {
        VuSpec s = spec();
        VuMesh m = mesh(ID);
        List<String> bad = new ArrayList<>();
        if (s.layers.size() < 6) bad.add(s.layers.size() + " ambient layers (floor 6)");
        if (s.idle.size() < 6) bad.add(s.idle.size() + " idle animations (a full set is at least 6)");
        if (s.draw == null) bad.add("no draw animation");
        if (s.swing == null) bad.add("no swing trail");
        if (s.soundLoop == null || !s.soundLoop.startsWith("nusmp:")) bad.add("no looped sound id");
        for (String act : DsCatalog.ACTS) if (s.act(act) == null) bad.add("act " + act + " has no signature burst");
        for (VuSpec.Act a : s.acts.values()) {
            if (!VuBurst.known(a.pattern())) bad.add("unknown burst pattern " + a.pattern());
            if (a.sound().isEmpty() || !a.sound().startsWith("nusmp:")) bad.add("a burst without a sound id");
        }
        List<String> used = new ArrayList<>();
        for (VuMesh.Group g : m.groups) if (!g.name.equals("-")) used.add(g.name);
        for (String g : used) if (!s.groups.containsKey(g)) bad.add("mesh group " + g + " has no animation data");
        for (String g : s.groups.keySet()) if (!used.contains(g)) bad.add("spec group " + g + " is not in the mesh");
        for (VuSpec.Idle i : s.idle) if (!i.group().isEmpty() && !s.groups.containsKey(i.group())) bad.add("idle " + i.id() + " names the unknown group " + i.group());
        for (VuSpec.Layer l : s.layers) if (!l.group().isEmpty() && !s.groups.containsKey(l.group())) bad.add("layer " + l.id() + " names the unknown group " + l.group());
        for (String g : DsCatalog.SHARDS) if (!kinds(s, g).contains(DsDynamics.FRACTURE)) bad.add("shard " + g + " has no fracture animation");
        for (String g : DsCatalog.CRACKS) if (!kinds(s, g).contains(DsDynamics.FRACTURE_GLOW) || !kinds(s, g).contains(DsDynamics.FRACTURE)) bad.add("crack light " + g + " lacks its fracture animations");
        for (String g : DsCatalog.BLOOM) if (!kinds(s, g).contains(DsDynamics.RESONANCE)) bad.add("group " + g + " has no resonance animation");
        if (m.quads(VuMesh.GLOW) == 0 || m.quads(VuMesh.VEINS) == 0 || m.quads(VuMesh.HALO) == 0) bad.add("a pass is empty");
        if (m.flares.isEmpty()) bad.add("no flares");
        assertEquals(List.of(), bad);
    }

    private static List<String> kinds(VuSpec s, String group) {
        List<String> out = new ArrayList<>();
        VuSpec.Group g = s.groups.get(group);
        if (g != null) for (VuSpec.Anim a : g.anim) out.add(a.kind);
        return out;
    }

    @Test
    void theShardsAreTheFourPartsOfTheOldFractureAndLieWhereTheOldOnesDid() throws IOException {
        // the old model broke at y = 1 | 11 | 22 | 33 | 49 (DemonSlayerMesh.CUTS): the groups s1..s3 must hold the blade between the cuts and nothing across one
        VuMesh m = mesh(ID);
        float[][] range = {{11f, 22f}, {22f, 33f}, {33f, 49.01f}};
        String[] names = {"s1", "s2", "s3"};
        for (int k = 0; k < 3; k++) {
            VuMesh.Group g = null;
            for (VuMesh.Group x : m.groups) if (x.name.equals(names[k])) g = x;
            assertTrue(g != null && g.quads(VuMesh.BODY) > 0, names[k] + " has body quads");
            float[] a = g.verts[VuMesh.BODY];
            for (int i = 0; i < g.count[VuMesh.BODY]; i += VuMesh.FLOATS) {
                float y = a[i + 1];
                assertTrue(y >= range[k][0] - 0.01f && y <= range[k][1], names[k] + ": a vertex at y " + y + " is outside " + range[k][0] + " .. " + range[k][1]);
            }
        }
    }

    @Test
    void theMeasuredRatiosMeetTheFloorsOfTheBrief() throws IOException {
        JsonObject after = JsonParser.parseString(Files.readString(DOCS.resolve("demon_slayer_after.json"))).getAsJsonObject();
        JsonArray items = after.getAsJsonObject("score").getAsJsonArray("items");
        assertEquals(1, items.size());
        JsonObject it = items.get(0).getAsJsonObject();
        if (!it.get("floors_ok").getAsBoolean()) {
            StringBuilder sb = new StringBuilder("floors not met:");
            for (var f : it.getAsJsonObject("floors").entrySet()) if (!f.getValue().getAsBoolean()) sb.append(' ').append(f.getKey());
            throw new AssertionError(sb.toString());
        }
        JsonObject r = it.getAsJsonObject("ratios");
        for (String k : new String[]{"mesh", "texture", "glow", "ambient", "anim"}) assertTrue(r.get(k).getAsDouble() >= 1.0, "component " + k + " did not improve");
        assertTrue(r.get("mesh").getAsDouble() >= 20.0, "the mesh detail is at least 20 times the baseline's quads: " + r.get("mesh").getAsDouble());
        assertTrue(it.get("V_after").getAsDouble() >= 20.0, "V_after " + it.get("V_after").getAsDouble());
        JsonObject lod = after.getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonObject("mesh");
        assertEquals(mesh(ID).quads(VuMesh.BODY), lod.getAsJsonObject("quads").get("body").getAsInt(), "the numbers in the after file are the shipped mesh's");
    }

    @Test
    void theBaselineWasTakenFromTheOldModelBeforeTheUpgrade() throws IOException {
        JsonObject base = JsonParser.parseString(Files.readString(DOCS.resolve("demon_slayer_baseline.json"))).getAsJsonObject();
        assertEquals("baseline", base.get("mode").getAsString());
        JsonElement e = base.getAsJsonArray("items").get(0);
        JsonObject it = e.getAsJsonObject();
        assertEquals("GENESIS", it.get("grade").getAsString());
        int quads = it.getAsJsonObject("mesh").getAsJsonObject("quads").get("body").getAsInt();
        assertTrue(quads > 0 && quads < 200, "the old pixel model is small: " + quads);
        assertEquals(64, it.getAsJsonObject("skin").get("size").getAsInt(), "the old skin is 64 px");
        assertTrue(it.getAsJsonArray("ambient_layers").size() <= 3);
    }
}
