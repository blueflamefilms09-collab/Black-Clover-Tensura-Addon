package com.newuniverse.nusmp.visual.sizes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner saw a tiny dagger and katanas that looked like a thin line. Data tests over the REAL mesh files: every dagger and katana mesh (the old ones, the upgraded
 * {@code up} ones and the Genesis {@code vu} ones) has a thickened copy whose in-hand size is inside the ranges of assets/nusmp/weapon_sizes.json for its class: length in
 * third and first person, blade width in first person at least 1/8 block (a vanilla sword's blade is 2.5 pixels), a visible spine, a grip that fits a hand, and no corner of
 * the mesh through the first-person camera. The old files must stay and must show the problem (a negative control), and no weapon is made shorter.
 */
class WeaponSizesTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");

    record Variant(String id, String tag, String name, Path old, Path sz) {}

    static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    static WeaponSizeRule.Rules rules() throws IOException { return WeaponSizeRule.parseRules(Files.readString(RES.resolve("weapon_sizes.json"))); }

    /** Every dagger and katana id of weapon_classes.json (the reviewed map) that is not excluded, with its class. */
    static Map<String, String> ids() throws IOException {
        WeaponSizeRule.Rules r = rules();
        Map<String, String> out = new TreeMap<>();
        for (var e : json(RES.resolve("weapon_classes.json")).getAsJsonObject("weapons").entrySet()) {
            String c = e.getValue().getAsString();
            if ((c.equals("dagger") || c.equals("katana")) && !r.excluded.contains(e.getKey())) out.put(e.getKey(), c);
        }
        return out;
    }

    static List<Variant> variants(String id) {
        List<Variant> v = new ArrayList<>();
        String[][] dirs = {{"old", "weapon_meshes", "weapon_meshes_sz"}, {"up", "weapon_meshes/up", "weapon_meshes_sz/up"}, {"vu", "weapon_meshes_vu", "weapon_meshes_vu_sz"}};
        for (String[] d : dirs) {
            Path old = RES.resolve(d[1] + "/" + id + ".txt");
            if (Files.exists(old)) v.add(new Variant(id, d[0], id, old, RES.resolve(d[2] + "/" + id + ".txt")));
        }
        return v;
    }

    static double scale(String id, String key) throws IOException {
        JsonObject d = json(RES.resolve("models/item/" + id + ".json")).getAsJsonObject("display");
        return d.getAsJsonObject(key).getAsJsonArray("scale").get(0).getAsDouble();
    }

    static double[] triple(String id, String key, String field) throws IOException {
        var a = json(RES.resolve("models/item/" + id + ".json")).getAsJsonObject("display").getAsJsonObject(key).getAsJsonArray(field);
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    @Test
    void theRealDaggersAndKatanasAreAllCovered() throws IOException {
        Map<String, String> ids = ids();
        long daggers = ids.values().stream().filter(c -> c.equals("dagger")).count(), katanas = ids.values().stream().filter(c -> c.equals("katana")).count();
        assertTrue(daggers >= 12, "daggers " + daggers);
        assertTrue(katanas >= 12, "katanas " + katanas);
        assertTrue(ids.containsKey("void_origin_katana") && ids.containsKey("kusanagi_the_grass_cutter") && ids.containsKey("unicorn_dagger"));
        int variants = 0;
        for (String id : ids.keySet()) variants += variants(id).size();
        assertTrue(variants >= 28, "variants " + variants);
    }

    @Test
    void everyThickenedMeshIsInsideTheSizeRangesOfItsClass() throws IOException {
        WeaponSizeRule.Rules rules = rules();
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (var e : ids().entrySet()) {
            String id = e.getKey();
            WeaponSizeRule.Limits lim = rules.limits.get(e.getValue());
            double s3 = scale(id, "thirdperson_righthand"), s1 = scale(id, "firstperson_righthand");
            double[] rot = triple(id, "firstperson_righthand", "rotation"), tr = triple(id, "firstperson_righthand", "translation");
            for (Variant v : variants(id)) {
                assertTrue(Files.exists(v.sz()), id + " [" + v.tag() + "]: no thickened copy " + v.sz());
                WeaponSizeRule.Measure m = WeaponSizeRule.measure(Files.readAllLines(v.sz()));
                problems.addAll(WeaponSizeRule.problems(id + " [" + v.tag() + "]", m, lim, s3, s1, rot, tr));
                checked++;
            }
        }
        assertTrue(checked >= 28, "checked " + checked);
        assertEquals(List.of(), problems);
    }

    @Test
    void theOldMeshesStayAndShowTheProblem() throws IOException {
        WeaponSizeRule.Rules rules = rules();
        int slivers = 0, total = 0;
        for (var e : ids().entrySet()) {
            String id = e.getKey();
            WeaponSizeRule.Limits lim = rules.limits.get(e.getValue());
            double s3 = scale(id, "thirdperson_righthand");
            for (Variant v : variants(id)) {
                assertTrue(Files.exists(v.old()), id + ": the old mesh was removed (it must stay)");
                WeaponSizeRule.Measure old = WeaponSizeRule.measure(Files.readAllLines(v.old()));
                WeaponSizeRule.Measure sz = WeaponSizeRule.measure(Files.readAllLines(v.sz()));
                assertTrue(sz.length * s3 >= old.length * s3 - 1e-3, id + " [" + v.tag() + "]: made shorter");
                assertTrue(sz.bladeWidth >= old.bladeWidth - 1e-6 && sz.bladeDepth >= old.bladeDepth - 1e-6, id + " [" + v.tag() + "]: made thinner");
                total++;
                if (old.bladeWidth * 0.54 < lim.bw1pMin()) slivers++;                        // at the slowest first-person scale of the katanas
            }
        }
        assertTrue(slivers * 10 >= total * 7, "the old blades are slivers (" + slivers + " of " + total + "): the negative control");
        // the negative control for the check itself: the old Void Origin Katana fails it
        WeaponSizeRule.Measure oldVoid = WeaponSizeRule.measure(Files.readAllLines(RES.resolve("weapon_meshes/void_origin_katana.txt")));
        double[] rot = triple("void_origin_katana", "firstperson_righthand", "rotation"), tr = triple("void_origin_katana", "firstperson_righthand", "translation");
        List<String> p = WeaponSizeRule.problems("old", oldVoid, rules.limits.get("katana"), 0.85, scale("void_origin_katana", "firstperson_righthand"), rot, tr);
        assertTrue(p.stream().anyMatch(s -> s.contains("blade width")) && p.stream().anyMatch(s -> s.contains("thickness")), p.toString());
    }

    @Test
    void theKatanaIsAboutAsWideAsAVanillaSwordInTheHandNotALine() throws IOException {
        // a vanilla sword's blade is about 2.5 pixels (0.16 block) wide at the handheld scale 0.68 in first person: 0.106; every katana and dagger is now at least 1/8 block
        for (var e : ids().entrySet()) {
            String id = e.getKey();
            for (Variant v : variants(id)) {
                WeaponSizeRule.Measure m = WeaponSizeRule.measure(Files.readAllLines(v.sz()));
                double w1 = m.bladeWidth * scale(id, "firstperson_righthand");
                assertTrue(w1 >= 0.115, id + " [" + v.tag() + "] first-person blade width " + w1 + " blocks (" + w1 * 16 + " px)");
            }
        }
    }

    @Test
    void daggersAreBetweenHalfAndThreeQuartersOfASwordAndKatanasLongerThanOne() throws IOException {
        for (var e : ids().entrySet()) {
            String id = e.getKey();
            double s3 = scale(id, "thirdperson_righthand");
            for (Variant v : variants(id)) {
                double l3 = WeaponSizeRule.measure(Files.readAllLines(v.sz())).length * s3;
                if (e.getValue().equals("dagger")) assertTrue(l3 >= 0.55 && l3 <= 0.76, id + " dagger third-person length " + l3);
                else assertTrue(l3 >= 1.40 && l3 <= 1.80, id + " katana third-person length " + l3);
            }
        }
    }

    @Test
    void theLevelOfDetailMeshesFollowTheirWeapon() throws IOException {
        int lods = 0;
        for (String id : ids().keySet()) {
            for (Variant v : variants(id)) {
                Path oldLod = RES.resolve(RES.relativize(v.old()).toString().replace(".txt", "_lod.txt"));
                if (!Files.exists(oldLod)) continue;
                Path szLod = RES.resolve(RES.relativize(v.sz()).toString().replace(".txt", "_lod.txt"));
                assertTrue(Files.exists(szLod), "no thickened level-of-detail mesh for " + id + " [" + v.tag() + "]");
                WeaponSizeRule.Measure a = WeaponSizeRule.measure(Files.readAllLines(v.sz())), b = WeaponSizeRule.measure(Files.readAllLines(szLod));
                assertEquals(a.place[3], b.place[3], 1e-9, id + ": the LOD mesh has the same place scale");
                assertEquals(a.place[0], b.place[0], 1e-9, id + ": the LOD mesh has the same origin");
                lods++;
            }
        }
        assertTrue(lods >= 4, "lods " + lods);
    }

    @Test
    void theRedirectMapsEachMeshFolderToItsCopyAndNothingElse() {
        assertEquals("weapon_meshes_sz/void_origin_katana.txt", WeaponSizeRule.szPath("weapon_meshes/void_origin_katana.txt"));
        assertEquals("weapon_meshes_sz/up/kusanagi_the_grass_cutter_lod.txt", WeaponSizeRule.szPath("weapon_meshes/up/kusanagi_the_grass_cutter_lod.txt"));
        assertEquals("weapon_meshes_vu_sz/void_origin_katana.txt", WeaponSizeRule.szPath("weapon_meshes_vu/void_origin_katana.txt"));
        assertNull(WeaponSizeRule.szPath("models/item/x.json"));
        assertNull(WeaponSizeRule.szPath(null));
        assertFalse(WeaponSizeRule.szPath("weapon_meshes/a.txt").startsWith("weapon_meshes/"));
    }

    @Test
    void theCornerDepthMathsMatchesTheVanillaPose() {
        // a mesh box that is one block high straight up the sprite diagonal at the vanilla handheld pose stays in front of the camera
        WeaponSizeRule.Measure m = new WeaponSizeRule.Measure();
        m.place = new double[]{0.25, 0.25, 45, 0.02, 0};
        m.boxMin = new double[]{-1, 0, -1};
        m.boxMax = new double[]{1, 50, 1};
        double near = WeaponSizeRule.nearestCornerDepth(m, new double[]{0, -90, 25}, new double[]{1.13, 3.2, 1.13}, 0.68);
        assertTrue(near > WeaponSizeRule.Z_MARGIN, "" + near);
        double tooBig = WeaponSizeRule.nearestCornerDepth(m, new double[]{0, -90, 25}, new double[]{1.13, 3.2, 1.13}, 3.0);
        assertTrue(tooBig < near, "a bigger scale comes nearer: " + tooBig);
    }
}
