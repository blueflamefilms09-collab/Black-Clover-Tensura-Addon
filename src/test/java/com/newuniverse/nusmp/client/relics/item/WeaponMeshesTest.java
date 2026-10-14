package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shipped meshes (src/main/resources/assets/nusmp/weapon_meshes) against the class table of assets/nusmp/weapon_classes.json: after
 * tools/relics/weapon_fixes.json the runtime normaliser has nothing to do on any of them, and the sorrows_edge crescent faces forward.
 * (Meshes other agents were rebuilding in the same release are skipped by prefix.)
 */
class WeaponMeshesTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final String[] SKIP = {"elsdocia", "twin_hook_swords", "twin_swords_of_deflection", "whirling_blades"};

    static boolean skipped(String id) {
        for (String s : SKIP) if (id.equals(s) || id.startsWith(s + "_")) return true;
        return false;
    }

    /** The "weapons" map of weapon_classes.json (a flat id -> class object) and the "classes" rows. */
    static WeaponClasses loadClasses() throws IOException {
        String json = Files.readString(RES.resolve("weapon_classes.json"));
        Map<String, WeaponClasses.Cls> table = new HashMap<>();
        Matcher m = Pattern.compile("\"(\\w+)\"\\s*:\\s*\\{\\s*\"min\"\\s*:\\s*([\\d.]+),\\s*\"max\"\\s*:\\s*([\\d.]+),\\s*\"head_min\"\\s*:\\s*([\\d.]+),\\s*\"grip\"\\s*:\\s*([\\d.]+)").matcher(json);
        while (m.find()) table.put(m.group(1), new WeaponClasses.Cls(m.group(1), Float.parseFloat(m.group(2)), Float.parseFloat(m.group(3)),
                Float.parseFloat(m.group(4)), Float.parseFloat(m.group(5))));
        Map<String, String> weapons = new HashMap<>();
        String tail = json.substring(json.indexOf("\"weapons\""));
        Matcher w = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"(\\w+)\"").matcher(tail);
        while (w.find()) weapons.put(w.group(1), w.group(2));
        return new WeaponClasses(table, weapons);
    }

    static float thirdPersonScale(String id) throws IOException {
        for (String cand : new String[]{id, WeaponClasses.baseId(id)}) {
            Path p = RES.resolve("models/item/" + cand + ".json");
            if (!Files.exists(p)) continue;
            Matcher m = Pattern.compile("\"thirdperson_righthand\"\\s*:\\s*\\{[^}]*?\"scale\"\\s*:\\s*\\[\\s*([\\d.]+)").matcher(Files.readString(p));
            if (m.find()) return Float.parseFloat(m.group(1));
            break;
        }
        return 0.85f;
    }

    @SuppressWarnings("unchecked")
    static List<float[]>[] readLayers(Path mesh, float[] placeOut) throws IOException {
        List<float[]>[] layers = new List[]{new ArrayList<float[]>(), new ArrayList<float[]>(), new ArrayList<float[]>()};
        for (String line : Files.readAllLines(mesh)) {
            String[] v = line.trim().split("\\s+");
            if (v[0].equals("place")) for (int i = 0; i < 5; i++) placeOut[i] = Float.parseFloat(v[i + 1]);
            if (v[0].equals("q") || v[0].equals("qn")) {
                int st = v[0].equals("q") ? 5 : 8;
                int layer = Integer.parseInt(v[1]);
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * st;
                    float nx = v[0].equals("q") ? Float.parseFloat(v[22]) : Float.parseFloat(v[o + 5]);
                    layers[layer].add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]), 0, 0, nx, 0, 0});
                }
            }
        }
        return layers;
    }

    @Test
    void theRuntimeNormaliserHasNothingToDoOnAShippedMesh() throws IOException {
        WeaponClasses classes = loadClasses();
        assertTrue(classes.className("sorrows_edge").equals("crescent"), "the class map is read");
        int checked = 0;
        List<String> changed = new ArrayList<>();
        try (Stream<Path> files = Files.list(RES.resolve("weapon_meshes"))) {
            for (Path p : (Iterable<Path>) files.sorted()::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".txt")) continue;
                String id = name.substring(0, name.length() - 4);
                if (skipped(id)) continue;
                float[] place = new float[5];
                List<float[]>[] layers = readLayers(p, place);
                if (layers[0].isEmpty()) continue;
                WeaponClasses.Cls cls = classes.classOf(id);
                WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers, place, cls.name(), cls, thirdPersonScale(id));
                checked++;
                if (r.changed()) changed.add(id + ": " + r.log());
            }
        }
        assertTrue(checked > 100, "checked " + checked);
        assertEquals(List.of(), changed);
    }

    @Test
    void sorrowsEdgeFacesForwardAndIsBold() throws IOException {
        float[] place = new float[5];
        List<float[]>[] layers = readLayers(RES.resolve("weapon_meshes/sorrows_edge.txt"), place);
        WeaponOrientation.Measures m = WeaponOrientation.measure(layers[0], place[3]);
        assertTrue(m.imbalance < -0.05, "the crescent's mass is on the forward (-x) side: " + m.imbalance);
        assertTrue(m.maxWidth * thirdPersonScale("sorrows_edge") >= 0.5, "head at least the crescent minimum");
        assertTrue(WeaponOrientation.detect(m, "crescent", place[2]).isEmpty());
    }
}
