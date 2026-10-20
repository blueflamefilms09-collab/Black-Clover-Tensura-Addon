package com.newuniverse.nusmp.client.upgrade;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the visual upgrade of the twelve God melee (a) weapons (docs/visual_upgrade/god_melee_a.md) as data: every item has its upgraded mesh, low-detail mesh, 1024 skin, glow map, veins
 * map and effect spec; the full mesh stays inside the performance guard (2500 welded vertices at full detail), the low-detail mesh inside 15% of it; the orbit lines match the
 * quads that use them; every spec carries what the God target asks for (passes, ambient layers, idle animations, a swing trail, a recipe for every act) and every recipe ends before the
 * effect that carries it. Reads the files (no game needed).
 */
class UpgradeGodMeleeATest {
    static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    static final int GOD_UNIQUE_VERTEX_CAP = 2500;
    static final double LOD_SHARE = 0.15;

    /** What a mesh file says: quads per layer, welded vertices, orbit lines. */
    record Mesh(int[] layers, int quads, int unique, List<String[]> orbits, Set<Integer> orbitLayersUsed, String place) {}

    static Mesh read(Path file) throws IOException {
        int[] layers = new int[3];
        int quads = 0;
        Set<String> verts = new HashSet<>();
        List<String[]> orbits = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        String place = null;
        for (String line : Files.readAllLines(file)) {
            String[] v = line.trim().split("\\s+");
            if (v.length == 0 || v[0].isEmpty() || v[0].startsWith("#")) continue;
            if (v[0].equals("place")) place = line;
            if (v[0].equals("orbit")) orbits.add(v);
            if (v[0].equals("q") || v[0].equals("qn")) {
                int stride = v[0].equals("q") ? 5 : 8;
                int layer = Integer.parseInt(v[1]);
                if (layer < 3) layers[layer]++; else used.add(layer);
                quads++;
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * stride;
                    verts.add(two(v[o]) + " " + two(v[o + 1]) + " " + two(v[o + 2]));
                    for (int j = 0; j < 3; j++) {
                        double d = Double.parseDouble(v[o + j]);
                        assertTrue(!Double.isNaN(d) && !Double.isInfinite(d), file + ": a bad number");
                    }
                }
            }
        }
        return new Mesh(layers, quads, verts.size(), orbits, used, place);
    }

    /** A coordinate of the file at two decimals, rounded half up on its own text (tools/visual_upgrade/god_melee_a/common.py does the same). */
    static String two(String token) { return new java.math.BigDecimal(token).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }

    static int[] pngSize(Path p) throws IOException {
        byte[] png = Files.readAllBytes(p);
        int w = ((png[16] & 255) << 24) | ((png[17] & 255) << 16) | ((png[18] & 255) << 8) | (png[19] & 255);
        int h = ((png[20] & 255) << 24) | ((png[21] & 255) << 16) | ((png[22] & 255) << 8) | (png[23] & 255);
        return new int[]{w, h};
    }

    static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void everyUpgradedWeaponHasItsFilesAndTheOldOnesStay() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : UpgradeItems.GOD_MELEE_A) {
            for (String f : new String[]{"weapon_meshes/up/" + id + ".txt", "weapon_meshes/up/" + id + "_lod.txt", "textures/entity/weapon/up/" + id + ".png", "textures/entity/weapon/up/" + id + "_glow.png",
                    "textures/entity/weapon/up/" + id + "_veins.png", "textures/entity/weapon/up/" + id + "_lod.png", "textures/entity/weapon/up/" + id + "_lod_glow.png", "visual_upgrade/" + id + ".json",
                    // the shipped look stays (add the new, keep the old)
                    "weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png", "textures/item/" + id + ".png", "models/item/" + id + "_icon.json"})
                if (!Files.exists(ASSETS.resolve(f))) bad.add(f);
        }
        for (String s : new String[]{"_pull1", "_pull2", "_pull3", "_charged"}) {
            if (!Files.exists(ASSETS.resolve("weapon_meshes/up/dawnpiercer_crossbow" + s + ".txt"))) bad.add("dawnpiercer_crossbow" + s);
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theSkinsAre1024AndTheLowDetailSkinsAre256() throws IOException {
        for (String id : UpgradeItems.GOD_MELEE_A) {
            for (String f : new String[]{"", "_glow", "_veins"}) {
                int[] s = pngSize(ASSETS.resolve("textures/entity/weapon/up/" + id + f + ".png"));
                assertEquals(1024, s[0], id + f + " width");
                assertEquals(1024, s[1], id + f + " height");
            }
            for (String f : new String[]{"_lod", "_lod_glow"}) {
                int[] s = pngSize(ASSETS.resolve("textures/entity/weapon/up/" + id + f + ".png"));
                assertEquals(256, s[0], id + f + " width");
            }
        }
    }

    @Test
    void theFullMeshIsInsideTheGuardAndTheLowDetailMeshInsideAShareOfIt() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : UpgradeItems.GOD_MELEE_A) {
            Mesh full = read(ASSETS.resolve("weapon_meshes/up/" + id + ".txt"));
            Mesh lod = read(ASSETS.resolve("weapon_meshes/up/" + id + "_lod.txt"));
            if (full.unique() > GOD_UNIQUE_VERTEX_CAP) bad.add(id + " full mesh has " + full.unique() + " welded vertices (cap " + GOD_UNIQUE_VERTEX_CAP + ")");
            if (full.quads() < 600) bad.add(id + " has only " + full.quads() + " quads (floor 600)");
            if (lod.quads() > LOD_SHARE * full.quads()) bad.add(id + " low-detail mesh has " + lod.quads() + " of " + full.quads() + " quads");
            if (lod.unique() > LOD_SHARE * full.unique()) bad.add(id + " low-detail mesh has " + lod.unique() + " of " + full.unique() + " welded vertices");
            if (lod.quads() * 4 > 2500) bad.add(id + " low-detail mesh emits " + lod.quads() * 4 + " vertices");
            if (!lod.orbits().isEmpty() || !lod.orbitLayersUsed().isEmpty()) bad.add(id + " low-detail mesh has orbiting groups");
            if (full.layers()[0] < 300 || full.layers()[2] < 20) bad.add(id + " body / glow quads " + full.layers()[0] + " / " + full.layers()[2]);
            if (full.place() == null) bad.add(id + " has no place line");
            // the placement of the old mesh is kept (same weapon size and orientation as before)
            Mesh old = read(ASSETS.resolve("weapon_meshes/" + id + ".txt"));
            if (!old.place().equals(full.place())) bad.add(id + " place line differs from the shipped mesh: " + full.place() + " vs " + old.place());
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyOrbitLineHasItsQuadsAndEveryOrbitingQuadItsLine() throws IOException {
        for (String id : UpgradeItems.GOD_MELEE_A) {
            for (String file : new String[]{id, "dawnpiercer_crossbow_pull2", "dawnpiercer_crossbow_charged"}) {
                if (!file.equals(id) && !id.equals("dawnpiercer_crossbow")) continue;
                Mesh m = read(ASSETS.resolve("weapon_meshes/up/" + file + ".txt"));
                Set<Integer> declared = new HashSet<>();
                for (String[] o : m.orbits()) {
                    assertEquals(11, o.length, file + ": an orbit line has 11 fields");
                    declared.add(Integer.parseInt(o[1]));
                    double speed = Double.parseDouble(o[8]);
                    assertTrue(Math.abs(speed) <= 8.0, file + ": an orbit of " + speed + " degrees a tick is too fast to read");
                    double ax = Double.parseDouble(o[5]), ay = Double.parseDouble(o[6]), az = Double.parseDouble(o[7]);
                    assertTrue(ax * ax + ay * ay + az * az > 0.01, file + ": an orbit needs an axis");
                }
                assertEquals(declared, m.orbitLayersUsed(), file + ": declared and used orbit layers");
            }
        }
    }

    @Test
    void everySpecCarriesWhatTheGodTargetAsks() throws IOException {
        List<String> bad = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (String id : UpgradeItems.GOD_MELEE_A) {
            JsonObject o = json(ASSETS.resolve("visual_upgrade/" + id + ".json"));
            UpgradeSpec s = UpgradeSpec.parse(o);
            if (!s.id.equals(id)) bad.add(id + ": the spec names " + s.id);
            if (s.passes.size() < 4) bad.add(id + ": " + s.passes.size() + " passes");
            if (s.ambient.size() < 3) bad.add(id + ": " + s.ambient.size() + " ambient layers (God needs 3)");
            if (s.ambient.size() < 6) bad.add(id + ": " + s.ambient.size() + " ambient layers (this release builds 6)");
            if (s.idle.size() < 3) bad.add(id + ": " + s.idle.size() + " idle animations");
            if (s.swing == null) bad.add(id + ": no swing trail");
            for (String a : new String[]{"glow_breath", "flow_veins", "halo_breath"}) if (!s.idles(a)) bad.add(id + ": no idle " + a);
            if (s.acts.size() < 2 || s.acts.size() > 3) bad.add(id + ": " + s.acts.size() + " act signatures");
            Set<String> types = new HashSet<>();
            for (UpgradeSpec.Layer l : s.ambient) types.add(l.type());
            if (types.size() < 4) bad.add(id + ": only " + types.size() + " kinds of ambient layer");
            for (UpgradeSpec.Act a : s.acts) {
                if (a.id() != UpgradeItems.signatureId(id, a.act())) bad.add(id + "/" + a.act() + ": the signature id is not the crc of its name");
                if (!ids.add(a.id())) bad.add(id + "/" + a.act() + ": the signature id is used twice");
                if (a.recipe().isEmpty()) bad.add(id + "/" + a.act() + ": empty recipe");
                if (a.recipe().size() > 5) bad.add(id + "/" + a.act() + ": a recipe of " + a.recipe().size() + " components is more than the vertex budget affords");
                if (a.maxDur() > UpgradeItems.SIGNATURE_DURATION) bad.add(id + "/" + a.act() + ": ends at " + a.maxDur() + " ticks, after the carrying effect (" + UpgradeItems.SIGNATURE_DURATION + ")");
                for (UpgradeSpec.Comp c : a.recipe()) {
                    if (!Files.exists(ASSETS.resolve("textures/particle/" + c.tex() + ".png"))) bad.add(id + "/" + a.act() + ": texture " + c.tex() + " does not exist");
                    if (c.t().isEmpty()) bad.add(id + "/" + a.act() + ": a component without a type");
                }
            }
            for (UpgradeSpec.Layer l : s.ambient) if (l.rate() > 24f) bad.add(id + ": layer " + l.type() + " spawns " + l.rate() + " particles a second");
            double perSecond = 0;
            for (UpgradeSpec.Layer l : s.ambient) perSecond += l.rate();
            if (perSecond > 90) bad.add(id + ": all layers together spawn " + perSecond + " particles a second");
            for (String f : new String[]{s.skin, s.glow, s.veins, s.lodSkin, s.lodGlow})
                if (!Files.exists(ASSETS.resolve("textures/entity/weapon/" + f))) bad.add(id + ": map " + f + " is missing");
            if (!Files.exists(ASSETS.resolve("weapon_meshes/" + s.lodMesh + ".txt"))) bad.add(id + ": low-detail mesh " + s.lodMesh + " is missing");
            if (s.lodNear < 8 || s.lodGlowOff < s.lodNear) bad.add(id + ": level of detail distances " + s.lodNear + " / " + s.lodGlowOff);
            if (s.ambientSound.isEmpty()) bad.add(id + ": no ambient sound id");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theActsOfTheSpecAreTheActsOfTheWeapon() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/wave/Wave71.java"));
        for (String id : UpgradeItems.GOD_MELEE_A) {
            int a = src.indexOf("RelicDef.of(\"" + id + "\"");
            int b = src.indexOf("out.accept", a + 10);
            String block = src.substring(a, b > 0 ? b : src.length());
            UpgradeSpec s = UpgradeSpec.parse(json(ASSETS.resolve("visual_upgrade/" + id + ".json")));
            List<String> keys = new ArrayList<>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\.act\\(Act\\.of\\(\"([a-z_]+)\"").matcher(block);
            while (m.find()) keys.add(m.group(1));
            List<String> have = new ArrayList<>();
            for (UpgradeSpec.Act act : s.acts) have.add(act.act());
            assertEquals(keys, have, id + ": a signature for every act, in order");
        }
    }

    @Test
    void theRatiosOfTheAfterFileAreTheNumbersOfTheFilesAndMeetWhatWeClaim() throws IOException {
        JsonObject after = json(Path.of("docs/visual_upgrade/god_melee_a_after.json"));
        JsonObject before = json(Path.of("docs/visual_upgrade/god_melee_a_baseline.json"));
        assertEquals(12, after.getAsJsonArray("items").size());
        List<String> bad = new ArrayList<>();
        for (var e : after.getAsJsonArray("items")) {
            JsonObject it = e.getAsJsonObject();
            String id = it.get("id").getAsString();
            JsonObject r = it.getAsJsonObject("ratios_vs_baseline");
            Mesh full = read(ASSETS.resolve("weapon_meshes/up/" + id + ".txt"));
            // the file states what the meshes say
            if (it.getAsJsonObject("mesh").get("quads_total").getAsInt() != full.quads()) bad.add(id + ": after.json quads differ from the mesh file");
            if (it.getAsJsonObject("mesh").get("unique_vertices").getAsInt() != full.unique()) bad.add(id + ": after.json unique vertices differ from the mesh file");
            double base = 0;
            for (var b : before.getAsJsonArray("items")) if (b.getAsJsonObject().get("id").getAsString().equals(id)) base = b.getAsJsonObject().getAsJsonObject("skin").get("painted_texels").getAsDouble();
            double now = it.getAsJsonObject("skin").get("painted_texels").getAsDouble();
            if (Math.abs(r.get("skin_painted_texels").getAsDouble() - now / base) > 0.01) bad.add(id + ": skin ratio is not now / before");
            // the claims of this release: a skin of at least 4x the painted texels, 4 passes, 6 ambient layers, an idle set and a swing trail, a recipe for every act
            if (r.get("skin_painted_texels").getAsDouble() < 4.0) bad.add(id + ": skin " + r.get("skin_painted_texels").getAsDouble() + "x (target 4x)");
            if (it.getAsJsonObject("passes").get("count").getAsInt() < 4) bad.add(id + ": passes");
            if (it.getAsJsonObject("ambient").get("count").getAsInt() < 6) bad.add(id + ": ambient layers");
            if (!it.get("lod_mesh").getAsBoolean()) bad.add(id + ": no low-detail mesh");
            if (it.getAsJsonObject("glow_map").getAsJsonArray("size").get(0).getAsInt() != 1024) bad.add(id + ": glow map size");
            if (it.get("V").getAsDouble() <= it.get("baseline_V").getAsDouble()) bad.add(id + ": V did not rise");
            // the honest part: the 5x composite is reported, not asserted; the file says which items reach it and the flag has to agree with the number
            boolean claims5 = it.get("v_target_5x_met").getAsBoolean();
            if (claims5 != (r.get("V").getAsDouble() >= 5.0)) bad.add(id + ": the 5x flag disagrees with the V ratio " + r.get("V").getAsDouble());
        }
        assertEquals(List.of(), bad);
    }
}
