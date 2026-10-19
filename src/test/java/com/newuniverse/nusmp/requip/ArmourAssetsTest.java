package com.newuniverse.nusmp.requip;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the table {@link ArmourSets} says which geo models each armour has; tools/gen_erza_requip_geo.py writes them. Nothing but this test notices when the two drift apart (a piece in the table
 * with no model draws nothing in game and the game only logs it; a model with no piece is dead weight). It checks, from the files on disk, that every piece has its geo model, texture, glow map and (for
 * the animated ones) animation, that no {@code erza_*} model exists that the table does not name, and that every weapon of the skill has its mesh, skin, glow, icon and item models.
 */
class ArmourAssetsTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");

    private static boolean has(String rel) {
        File f = new File(ASSETS, rel);
        return f.isFile() && f.length() > 16;
    }

    @Test
    void everyPieceOfTheTableHasItsModelTextureAndGlow() {
        List<String> missing = new ArrayList<>();
        for (Armour a : Armour.values()) for (ArmourSets.Piece p : ArmourSets.of(a)) {
            String id = ArmourSets.modelId(a, p);
            for (String rel : List.of("geo/entity/" + id + ".geo.json", "textures/entity/" + id + ".png", "textures/entity/" + id + "_glow.png"))
                if (!has(rel)) missing.add(rel);
            if (p.clip() != null && !has("animations/entity/" + id + ".animation.json")) missing.add("animations/entity/" + id + ".animation.json");
        }
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    @Test
    void noModelExistsThatTheTableDoesNotName() {
        Set<String> wanted = new HashSet<>(ArmourSets.allModelIds());
        File dir = new File(ASSETS, "geo/entity");
        String[] names = dir.list();
        assertTrue(names != null, "geo folder");
        List<String> extra = new ArrayList<>();
        int found = 0;
        for (String n : names) {
            if (!n.startsWith("erza_") || !n.endsWith(".geo.json")) continue;
            found++;
            String id = n.substring(0, n.length() - ".geo.json".length());
            if (!wanted.contains(id)) extra.add(id);
        }
        assertTrue(extra.isEmpty(), "models the table does not name: " + extra);
        assertEquals(wanted.size(), found);
    }

    @Test
    void everyWeaponHasItsMeshSkinGlowIconAndModels() {
        assertEquals(6, RequipItemIds.WEAPONS.size());
        List<String> missing = new ArrayList<>();
        for (String w : RequipItemIds.WEAPONS) {
            for (String rel : List.of("weapon_meshes/" + w + ".txt", "textures/entity/weapon/" + w + ".png", "textures/entity/weapon/" + w + "_glow.png",
                    "textures/item/" + w + ".png", "models/item/" + w + ".json", "models/item/" + w + "_icon.json"))
                if (!has(rel)) missing.add(rel);
        }
        for (String rel : List.of("textures/item/titanias_requip.png", "models/item/titanias_requip.json", "textures/skill/erza_requip.png"))
            if (!has(rel)) missing.add(rel);
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    /** The weapon ids of the table, without loading the item register (it needs the game). */
    static final class RequipItemIds {
        static final List<String> WEAPONS;

        static {
            Set<String> s = new java.util.LinkedHashSet<>();
            for (Armour a : Armour.values()) if (a.weapon() != null) s.add(a.weapon());
            WEAPONS = List.copyOf(s);
        }
    }
}
