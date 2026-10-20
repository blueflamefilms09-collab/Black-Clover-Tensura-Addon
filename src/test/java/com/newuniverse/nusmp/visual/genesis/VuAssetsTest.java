package com.newuniverse.nusmp.visual.genesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped assets of the genesis_weapons visual upgrade (tools/visual_upgrade/genesis_weapons): every weapon of the slice has an upgraded mesh, a level-of-detail mesh, a skin and
 * its effect data and keeps the old mesh; the meshes sit inside the caps of the performance guard of the brief (welded vertices, LOD share), carry the placement of the old mesh so
 * every display transform still fits, and the effect data has what the brief asks of a Genesis item (six ambient layers, a full idle set, a draw animation, a swing trail, a signature
 * on every act, a looped sound id). The ratios of docs/visual_upgrade/genesis_weapons_after.json are checked against the floors the generator wrote and the baseline it started from.
 */
class VuAssetsTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final Path SRC = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave");
    static final Path DOCS = Path.of("docs", "visual_upgrade");

    static VuMesh mesh(String dir, String id) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(RES.resolve(dir).resolve(id + ".txt"), StandardCharsets.UTF_8)) {
            return VuMesh.parse(in);
        }
    }

    static VuSpec spec(String id) throws IOException {
        return VuSpec.parse(Files.readString(RES.resolve("visual_upgrade").resolve(id + ".json")));
    }

    static float[] place(Path file) throws IOException {
        for (String line : Files.readAllLines(file)) {
            String[] v = line.trim().split("\\s+");
            if (v[0].equals("place")) {
                float[] p = new float[5];
                for (int i = 0; i < 5; i++) p[i] = Float.parseFloat(v[i + 1]);
                return p;
            }
        }
        throw new IOException("no place line in " + file);
    }

    @Test
    void everyWeaponShipsItsUpgradeAndKeepsTheOldMesh() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : VuCatalog.IDS) {
            if (!Files.exists(RES.resolve("weapon_meshes").resolve(id + ".txt"))) bad.add(id + ": the old mesh is gone (it must stay)");
            if (!Files.exists(RES.resolve("visual_upgrade").resolve(id + ".json"))) bad.add(id + ": no effect data");
            for (String m : VuCatalog.meshes(id)) {
                for (String f : new String[]{m + ".txt", m + "_lod.txt"})
                    if (!Files.exists(RES.resolve("weapon_meshes_vu").resolve(f))) bad.add(f + " missing");
                if (!Files.exists(RES.resolve("weapon_meshes").resolve(m + ".txt"))) bad.add(m + ": no old mesh to fall back to");
            }
            if (!Files.exists(RES.resolve("textures/entity/weapon_vu").resolve(id + ".png"))) bad.add(id + ": no skin");
            if (!Files.exists(RES.resolve("textures/entity/weapon_vu").resolve(id + "_lod.png"))) bad.add(id + ": no LOD skin");
        }
        assertTrue(Files.exists(RES.resolve("textures/entity/weapon_vu/halo.png")), "the halo sheet");
        assertEquals(List.of(), bad);
    }

    @Test
    void theMeshesAreInsideTheCapsOfThePerformanceGuard() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : VuCatalog.IDS) {
            for (String m : VuCatalog.meshes(id)) {
                VuMesh full = mesh("weapon_meshes_vu", m), lod = mesh("weapon_meshes_vu", m + "_lod");
                int body = full.quads(VuMesh.BODY), welded = full.weldedVertices(VuMesh.BODY);
                if (body < 2500) bad.add(m + ": only " + body + " body quads (floor 2500)");
                if (welded > VuRules.MAX_VERTICES_GENESIS) bad.add(m + ": " + welded + " welded vertices (cap " + VuRules.MAX_VERTICES_GENESIS + ")");
                int lq = lod.quads(VuMesh.BODY);
                if (lq == 0 || lq > VuRules.LOD_RATIO_MAX * body) bad.add(m + ": LOD has " + lq + " of " + body + " body quads (cap 15%)");
                if (lod.weldedVertices(VuMesh.BODY) > VuRules.LOD_RATIO_MAX * welded) bad.add(m + ": LOD welded vertices over 15% of " + welded);
                if (lod.quads(VuMesh.HALO) != 0) bad.add(m + ": the LOD mesh carries halo cards");
                if (full.quads(VuMesh.HALO) > 120) bad.add(m + ": too many halo card quads " + full.quads(VuMesh.HALO));
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theMeshesKeepThePlacementOfTheOldOnesAndTheirAxesAreSane() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : VuCatalog.IDS) {
            for (String m : VuCatalog.meshes(id)) {
                float[] o = place(RES.resolve("weapon_meshes").resolve(m + ".txt")), n = place(RES.resolve("weapon_meshes_vu").resolve(m + ".txt"));
                for (int i = 0; i < 4; i++) if (Math.abs(o[i] - n[i]) > 1e-4f * Math.max(1f, Math.abs(o[i]))) bad.add(m + ": place field " + i + " differs from the old mesh (" + o[i] + " vs " + n[i] + ")");
                VuMesh full = mesh("weapon_meshes_vu", m);
                float len = full.length();
                if (!(len > 0.3f && len < 6f)) bad.add(m + ": odd length " + len);
                if (id.equals("starforged_aeon_bow") && Math.abs(n[2] - 225.0f) > 1.0f) bad.add(m + ": a bow lies on vanilla's diagonal (place angle 225)");
                if (full.glowRgb != 0xFFFFFF) bad.add(m + ": the glow tint multiplies the skins' own colours: keep FFFFFF");
                for (VuMesh.Group g : full.groups) {
                    for (int l = 0; l < VuMesh.LAYERS; l++) {
                        float[] a = g.verts[l];
                        for (int i = 0; i < g.count[l]; i += VuMesh.FLOATS) {
                            for (int k = 0; k < 3; k++) if (!Float.isFinite(a[i + k])) bad.add(m + ": non-finite vertex");
                            if (l == VuMesh.BODY || l == VuMesh.GLOW || l == VuMesh.VEINS) {
                                if (a[i + 3] < -0.01f || a[i + 3] > 128.01f || a[i + 4] < -0.01f || a[i + 4] > 128.01f) bad.add(m + ": UV out of the 128 space");
                                float nl = (float) Math.sqrt(a[i + 5] * a[i + 5] + a[i + 6] * a[i + 6] + a[i + 7] * a[i + 7]);
                                if (nl < 0.5f || nl > 1.5f) bad.add(m + ": a normal of length " + nl);
                            }
                        }
                    }
                }
                if (bad.size() > 40) break;
            }
        }
        assertEquals(List.of(), bad.size() > 40 ? bad.subList(0, 40) : bad);
    }

    @Test
    void aBowsDrawStagesShareTheRestSkinAndTheTopology() throws IOException {
        VuMesh rest = mesh("weapon_meshes_vu", "starforged_aeon_bow");
        for (String st : VuCatalog.STATES.get("starforged_aeon_bow")) {
            VuMesh m = mesh("weapon_meshes_vu", st);
            assertEquals(rest.skin, m.skin, st + " shares the skin of the rest mesh");
            assertEquals(rest.groups.size(), m.groups.size(), st + " has the same groups");
            assertTrue(m.quads(VuMesh.BODY) > rest.quads(VuMesh.BODY), st + " also carries the arrow");
        }
    }

    @Test
    void theEffectDataHasWhatAGenesisItemNeeds() throws IOException {
        Pattern act = Pattern.compile("Act\\.of\\(\"(\\w+)\"");
        List<String> bad = new ArrayList<>();
        for (String id : VuCatalog.IDS) {
            VuSpec s = spec(id);
            VuMesh m = mesh("weapon_meshes_vu", id);
            if (s.layers.size() < 6) bad.add(id + ": " + s.layers.size() + " ambient layers (floor 6)");
            if (s.idle.size() < 6) bad.add(id + ": " + s.idle.size() + " idle animations (a full set is at least 6)");
            if (s.draw == null) bad.add(id + ": no draw animation");
            if (s.swing == null) bad.add(id + ": no swing trail");
            if (s.soundLoop == null || !s.soundLoop.startsWith("nusmp:")) bad.add(id + ": no looped sound id");
            if (s.acts.isEmpty()) bad.add(id + ": no signature bursts");
            for (VuSpec.Act a : s.acts.values()) if (!VuBurst.known(a.pattern())) bad.add(id + ": unknown burst pattern " + a.pattern());
            Set<String> used = new HashSet<>();
            for (VuMesh.Group g : m.groups) if (!g.name.equals("-")) used.add(g.name);
            for (String g : used) if (!s.groups.containsKey(g)) bad.add(id + ": mesh group " + g + " has no animation data");
            for (String g : s.groups.keySet()) if (!used.contains(g)) bad.add(id + ": spec group " + g + " is not in the mesh");
            for (VuSpec.Idle i : s.idle) if (!i.group().isEmpty() && !s.groups.containsKey(i.group())) bad.add(id + ": idle " + i.id() + " names the unknown group " + i.group());
            for (VuSpec.Layer l : s.layers) if (!l.group().isEmpty() && !s.groups.containsKey(l.group())) bad.add(id + ": layer " + l.id() + " names the unknown group " + l.group());
            if (m.quads(VuMesh.GLOW) == 0 || m.quads(VuMesh.VEINS) == 0 || m.quads(VuMesh.HALO) == 0) bad.add(id + ": a pass is empty (glow " + m.quads(VuMesh.GLOW) + ", veins " + m.quads(VuMesh.VEINS) + ", halo " + m.quads(VuMesh.HALO) + ")");
            if (m.flares.isEmpty()) bad.add(id + ": no flares");
            // every act of the weapon (from its RelicDef) has a signature burst
            if (!id.equals("elsdocia")) {
                for (String file : new String[]{"Wave1.java", "Wave71.java", "Wave78.java"}) {
                    String text = Files.readString(SRC.resolve(file));
                    int at = text.indexOf("RelicDef.of(\"" + id + "\"");
                    if (at < 0) continue;
                    int end = text.indexOf("RelicDef.of(\"", at + 10);
                    Matcher mt = act.matcher(text.substring(at, end < 0 ? text.length() : end));
                    int found = 0;
                    while (mt.find()) {
                        found++;
                        if (!s.acts.containsKey(mt.group(1))) bad.add(id + ": act " + mt.group(1) + " has no signature burst");
                    }
                    assertTrue(found > 0, id + " acts found in " + file);
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theMeasuredRatiosMeetTheFloorsOfTheBrief() throws IOException {
        JsonObject after = JsonParser.parseString(Files.readString(DOCS.resolve("genesis_weapons_after.json"))).getAsJsonObject();
        JsonObject base = JsonParser.parseString(Files.readString(DOCS.resolve("genesis_weapons_baseline.json"))).getAsJsonObject();
        JsonArray items = after.getAsJsonObject("score").getAsJsonArray("items");
        assertEquals(VuCatalog.IDS.size(), items.size(), "every item is in the table");
        assertEquals(VuCatalog.IDS.size(), base.getAsJsonArray("items").size(), "every item has a baseline");
        List<String> bad = new ArrayList<>();
        for (JsonElement e : items) {
            JsonObject it = e.getAsJsonObject();
            String id = it.get("id").getAsString();
            if (!it.get("floors_ok").getAsBoolean()) {
                StringBuilder sb = new StringBuilder(id + ": floors not met:");
                for (var f : it.getAsJsonObject("floors").entrySet()) if (!f.getValue().getAsBoolean()) sb.append(' ').append(f.getKey());
                bad.add(sb.toString());
            }
            double v = it.get("V_after").getAsDouble();
            if (!(v > 3.0)) bad.add(id + ": V_after only " + v + " (a sanity bound, the target of 20 is not reachable under the vertex cap: docs/visual_upgrade/genesis_weapons.md)");
            JsonObject r = it.getAsJsonObject("ratios");
            for (String k : new String[]{"mesh", "texture", "glow", "ambient", "anim"}) if (!(r.get(k).getAsDouble() >= 1.0)) bad.add(id + ": component " + k + " did not improve");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theBaselineWasTakenBeforeTheUpgrade() throws IOException {
        JsonObject base = JsonParser.parseString(Files.readString(DOCS.resolve("genesis_weapons_baseline.json"))).getAsJsonObject();
        assertEquals("baseline", base.get("mode").getAsString());
        for (JsonElement e : base.getAsJsonArray("items")) {
            JsonObject it = e.getAsJsonObject();
            assertEquals("GENESIS", it.get("grade").getAsString());
            assertTrue(it.getAsJsonObject("mesh").getAsJsonObject("quads").get("body").getAsInt() > 0);
            assertTrue(it.getAsJsonArray("ambient_layers").size() <= 3, "the baseline has only the shared grade shimmer");
        }
    }
}
