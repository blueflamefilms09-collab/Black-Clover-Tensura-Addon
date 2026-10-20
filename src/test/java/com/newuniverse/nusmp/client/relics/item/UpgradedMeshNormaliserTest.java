package com.newuniverse.nusmp.client.relics.item;

import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: when a mesh loads, {@code WeaponRenderer} checks it against its size class and, if it is too small, upside down or turned the wrong way, scales / flips / mirrors it
 * ({@link WeaponNormaliser}). The upgraded meshes (weapon_meshes/up) carry far more geometry than the shipped ones, so this test runs the same normaliser over each of them (and over the
 * low-detail meshes and the crossbow's draw states) and demands that it has nothing to do: the upgraded weapon is held and sized exactly like the shipped one. Orbiting groups (layer 3 and
 * up) are not part of what the normaliser measures, as in the renderer.
 */
class UpgradedMeshNormaliserTest {
    static final Path RES = WeaponMeshesTest.RES;

    /** Like WeaponMeshesTest.readLayers, but the orbiting groups (layer 3 and up) are skipped, as the renderer keeps them apart. */
    @SuppressWarnings("unchecked")
    static List<float[]>[] layers(Path mesh, float[] place) throws IOException {
        List<float[]>[] layers = new List[]{new ArrayList<float[]>(), new ArrayList<float[]>(), new ArrayList<float[]>()};
        for (String line : Files.readAllLines(mesh)) {
            String[] v = line.trim().split("\\s+");
            if (v[0].equals("place")) for (int i = 0; i < 5; i++) place[i] = Float.parseFloat(v[i + 1]);
            if (v[0].equals("grip") && v.length > 1) place[5] = Float.parseFloat(v[1]);
            if (v[0].equals("q") || v[0].equals("qn")) {
                int st = v[0].equals("q") ? 5 : 8;
                int layer = Integer.parseInt(v[1]);
                if (layer > 2) continue;
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
    void theNormaliserHasNothingToDoOnAnUpgradedMesh() throws IOException {
        WeaponClasses classes = WeaponMeshesTest.loadClasses();
        List<String> changed = new ArrayList<>();
        int checked = 0;
        for (String id : UpgradeItems.GOD_MELEE_A) {
            List<String> files = new ArrayList<>(List.of(id, id + "_lod"));
            if (id.equals("dawnpiercer_crossbow")) files.addAll(List.of(id + "_pull1", id + "_pull2", id + "_pull3", id + "_charged"));
            for (String f : files) {
                Path p = RES.resolve("weapon_meshes/up/" + f + ".txt");
                assertTrue(Files.exists(p), f);
                float[] place = new float[6];
                place[5] = Float.NaN;
                List<float[]>[] l = layers(p, place);
                // the low-detail mesh is read as its weapon (renderer: WeaponRenderer.read(id, ...)), a state as its base id's class
                WeaponClasses.Cls cls = classes.classOf(id);
                WeaponNormaliser.Result r = WeaponNormaliser.normalise(l, place, cls.name(), cls, WeaponMeshesTest.thirdPersonScale(id), place[5]);
                checked++;
                if (r.changed()) changed.add(f + ": " + r.log());
            }
        }
        assertEquals(28, checked);
        assertEquals(List.of(), changed);
    }
}
