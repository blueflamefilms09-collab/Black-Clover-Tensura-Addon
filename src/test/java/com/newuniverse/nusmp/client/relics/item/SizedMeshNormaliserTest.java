package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: WeaponRenderer runs the normaliser ({@link WeaponNormaliser}) on every mesh it loads, and since 1.110.0 it loads the thickened copy of a dagger or katana mesh
 * (assets/nusmp/weapon_meshes_sz, tools/relics/weapon_sizes.py) instead of the old file. The copy must be held and sized so that the normaliser has nothing to do on it
 * (no turn, no mirror, no scale-up), exactly like the old shipped meshes (WeaponMeshesTest).
 */
class SizedMeshNormaliserTest {
    static final Path RES = WeaponMeshesTest.RES;

    @Test
    void theNormaliserHasNothingToDoOnAThickenedMesh() throws IOException {
        WeaponClasses classes = WeaponMeshesTest.loadClasses();
        List<String> changed = new ArrayList<>();
        int checked = 0;
        for (String dir : new String[]{"weapon_meshes_sz", "weapon_meshes_sz/up"}) {
            try (var files = Files.list(RES.resolve(dir))) {
                for (Path p : (Iterable<Path>) files.sorted()::iterator) {
                    String name = p.getFileName().toString();
                    if (!name.endsWith(".txt")) continue;
                    String file = name.substring(0, name.length() - 4);
                    String id = file.endsWith("_lod") ? file.substring(0, file.length() - 4) : file;
                    float[] place = new float[6];
                    place[5] = Float.NaN;
                    List<float[]>[] layers = UpgradedMeshNormaliserTest.layers(p, place);
                    if (layers[0].isEmpty()) continue;
                    WeaponClasses.Cls cls = classes.classOf(id);
                    assertTrue(cls.name().equals("dagger") || cls.name().equals("katana"), id + " is a " + cls.name());
                    WeaponNormaliser.Result r = WeaponNormaliser.normalise(layers, place, cls.name(), cls, WeaponMeshesTest.thirdPersonScale(id), place[5]);
                    checked++;
                    if (r.changed()) changed.add(dir + "/" + file + ": " + r.log());
                }
            }
        }
        assertTrue(checked >= 28, "checked " + checked);
        assertEquals(List.of(), changed);
    }
}
