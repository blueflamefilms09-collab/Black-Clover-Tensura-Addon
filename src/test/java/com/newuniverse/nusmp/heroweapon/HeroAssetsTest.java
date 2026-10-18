package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: owner rule 7 says every weapon is a real 3D model in hand with its sprite kept as the inventory icon. The renderer draws a weapon only if its files
 * exist, and a missing one shows as a purple-black cube in the owner's hand, so the existence of all six files of every weapon (and the three of the curse look
 * of a Holy Weapon) is checked from the source tree. This test FAILS until the four art builders have delivered their weapons, and then names each missing
 * file; that is its job. Files: {@code models/item/<id>.json} (the in-hand model), {@code models/item/<id>_icon.json} (the inventory icon model),
 * {@code textures/item/<id>.png} (the 16x16 sprite), {@code weapon_meshes/<id>.txt}, {@code textures/entity/weapon/<id>.png} and {@code <id>_glow.png}.
 */
class HeroAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");

    @Test
    void everyWeaponHasItsModelFiles() {
        List<String> missing = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) {
            String id = k.itemId();
            need(missing, "models/item/" + id + ".json");
            need(missing, "models/item/" + id + "_icon.json");
            need(missing, "textures/item/" + id + ".png");
            need(missing, "weapon_meshes/" + id + ".txt");
            need(missing, "textures/entity/weapon/" + id + ".png");
            need(missing, "textures/entity/weapon/" + id + "_glow.png");
            if (k.isHoly()) {
                need(missing, "weapon_meshes/" + id + "_curse.txt");
                need(missing, "textures/entity/weapon/" + id + "_curse.png");
                need(missing, "textures/entity/weapon/" + id + "_curse_glow.png");
            }
        }
        assertTrue(missing.isEmpty(), missing.size() + " missing asset files, first: " + missing.subList(0, Math.min(12, missing.size())));
    }

    private static void need(List<String> missing, String relative) {
        if (!Files.exists(ASSETS.resolve(relative))) missing.add(relative);
    }
}
