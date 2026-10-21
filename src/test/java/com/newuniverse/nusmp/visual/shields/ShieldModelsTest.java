package com.newuniverse.nusmp.visual.shields;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.110.0: the shipped item model jsons of every shield-class item against the shared vanilla shield preset (docs/visual_upgrade/shield_fix.md): the same display values as
 * minecraft:item/shield and shield_blocking (first and third person, left and right, ground, fixed; gui and head are identities because the inventory keeps the flat icon), a
 * {@code blocking} override to the {@code _blocking} model, the {@code _icon} model of the old sprite untouched, a 3D mesh, and nothing else in the class table that is a shield
 * is left out of the set.
 */
class ShieldModelsTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");

    static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void everyShieldModelHasTheVanillaPresetAndTheBlockingOverride() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : ShieldSet.IDS) {
            Path low = RES.resolve("models/item/" + id + ".json"), up = RES.resolve("models/item/" + id + "_blocking.json");
            assertTrue(Files.exists(low), id + " has a model");
            bad.addAll(ShieldModelCheck.problems(id, json(low), Files.exists(up) ? json(up) : null));
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void thePresetIsVanillasShieldAsWrittenOut() {
        // minecraft:item/shield and shield_blocking of 1.21.1, spelled once more here so an accidental edit of the preset is caught
        assertTrue(ShieldPreset.within(new double[]{0, 90, 0, 10, 6, -4, 1, 1, 1}, ShieldPreset.of("thirdperson_righthand", false)));
        assertTrue(ShieldPreset.within(new double[]{0, 90, 0, 10, 6, 12, 1, 1, 1}, ShieldPreset.of("thirdperson_lefthand", false)));
        assertTrue(ShieldPreset.within(new double[]{0, 180, 5, -10, 2, 0, 1.25, 1.25, 1.25}, ShieldPreset.of("firstperson_righthand", false)));
        assertTrue(ShieldPreset.within(new double[]{0, 180, 5, 10, 0, 0, 1.25, 1.25, 1.25}, ShieldPreset.of("firstperson_lefthand", false)));
        assertTrue(ShieldPreset.within(new double[]{0, 180, 0, -2, 4, -5, 0.5, 0.5, 0.5}, ShieldPreset.of("fixed", false)));
        assertTrue(ShieldPreset.within(new double[]{0, 0, 0, 4, 4, 2, 0.25, 0.25, 0.25}, ShieldPreset.of("ground", false)));
        assertTrue(ShieldPreset.within(new double[]{45, 135, 0, 3.51, 11, -2, 1, 1, 1}, ShieldPreset.of("thirdperson_righthand", true)));
        assertTrue(ShieldPreset.within(new double[]{45, 135, 0, 13.51, 3, 5, 1, 1, 1}, ShieldPreset.of("thirdperson_lefthand", true)));
        assertTrue(ShieldPreset.within(new double[]{0, 180, -5, -15, 5, -11, 1.25, 1.25, 1.25}, ShieldPreset.of("firstperson_righthand", true)));
        assertTrue(ShieldPreset.within(new double[]{0, 180, -5, 5, 5, -11, 1.25, 1.25, 1.25}, ShieldPreset.of("firstperson_lefthand", true)));
        for (String ctx : ShieldPreset.CONTEXTS) {
            assertNotNull(ShieldPreset.of(ctx, false), ctx);
            assertNotNull(ShieldPreset.of(ctx, true), ctx);
        }
        // the raised model differs from the lowered one in the four hand contexts and nowhere else
        for (String ctx : ShieldPreset.CONTEXTS) {
            boolean same = ShieldPreset.within(ShieldPreset.of(ctx, false), ShieldPreset.of(ctx, true));
            assertEquals(!ShieldPreset.HAND_CONTEXTS.contains(ctx), same, ctx);
        }
        // a value off by more than the tolerance does not match
        double[] off = ShieldPreset.of("thirdperson_righthand", false).clone();
        off[1] += 5;
        assertFalse(ShieldPreset.within(ShieldPreset.of("thirdperson_righthand", false), off));
    }

    @Test
    void theInventoryKeepsTheOldSpriteIcon() throws IOException {
        for (String id : ShieldSet.IDS) {
            Path icon = RES.resolve("models/item/" + id + "_icon.json");
            assertTrue(Files.exists(icon), id + " keeps its _icon model");
            JsonObject o = json(icon);
            String parent = o.get("parent").getAsString();
            assertTrue(parent.equals("minecraft:item/handheld") || parent.equals("minecraft:item/generated"), id + "_icon is still the flat sprite (" + parent + ")");
            assertTrue(o.getAsJsonObject("textures").has("layer0"), id + "_icon draws its sprite");
            assertFalse(o.has("display"), id + "_icon carries no display of its own");
        }
    }

    @Test
    void everyShieldHasAMeshAndASkin() {
        for (String id : ShieldSet.IDS) {
            assertTrue(Files.exists(RES.resolve("weapon_meshes/" + id + ".txt")), id + " mesh");
            assertTrue(Files.exists(RES.resolve("textures/entity/weapon/" + id + ".png")), id + " skin");
        }
    }

    @Test
    void theClassTableAndTheSetAgree() throws IOException {
        JsonObject weapons = json(RES.resolve("weapon_classes.json")).getAsJsonObject("weapons");
        Set<String> inTable = new TreeSet<>();
        for (var e : weapons.entrySet()) if ("shield".equals(e.getValue().getAsString()) && !ShieldSet.CLASS_ONLY.contains(e.getKey())) inTable.add(e.getKey());
        assertEquals(new TreeSet<>(ShieldSet.IDS), inTable, "every id the class table files under shield (but the mirror) is in ShieldSet, and nothing more");
    }

    @Test
    void noOtherModelHasABlockingOverrideWithoutBeingAShield() throws IOException {
        List<String> stray = new ArrayList<>();
        try (var files = Files.list(RES.resolve("models/item"))) {
            for (Path p : (Iterable<Path>) files.sorted()::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".json")) continue;
                String id = name.substring(0, name.length() - 5);
                if (ShieldSet.IDS.contains(id) || (id.endsWith("_blocking") && ShieldSet.IDS.contains(id.substring(0, id.length() - 9)))) continue;
                if (Files.readString(p).contains("\"blocking\"")) stray.add(id);
            }
        }
        assertEquals(List.of(), stray, "a model that swaps on blocking must be a shield of ShieldSet (so it gets the preset and the predicate)");
    }

    @Test
    void theSetHelpersFindTheBaseItem() {
        assertEquals("legendary_shield", ShieldSet.baseId("legendary_shield_curse"));
        assertEquals("calamity_wave_aegis", ShieldSet.baseId("calamity_wave_aegis_lod"));
        assertTrue(ShieldSet.isShieldMesh("legendary_shield_curse"));
        assertTrue(ShieldSet.isShieldMesh("calamity_wave_aegis_lod"));
        assertFalse(ShieldSet.isShieldMesh("demon_slasher_katana"));
        assertFalse(ShieldSet.isShieldMesh("vassal_mirror"));
        assertFalse(ShieldSet.isShieldItem(null));
    }
}
