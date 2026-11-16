package com.newuniverse.nusmp.sinweapons;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the five Berserk weapons' numbers are pure ({@link SinRules}) and their data is checked from the repo's files: every weapon has a mesh, skins, icon, model, Better Combat file and engravings, and the
 * techniques are bounded (targets, cooldown, packets). Pure: java.nio only, CWD = repo root.
 */
class SinRulesTest {
    private static final Path RES = Path.of("src", "main", "resources");

    @Test
    void fiveWeaponsWithUniqueIdsAndBoundedTechniques() {
        assertEquals(5, SinRules.Kind.values().length);
        assertEquals(5, SinRules.IDS.size());
        for (SinRules.Kind k : SinRules.Kind.values()) {
            assertTrue(SinRules.IDS.contains(k.id), k.id);
            assertTrue(k.cooldown >= 40 && k.cooldown <= 400, k.id + " cooldown");
            assertTrue(SinRules.targetCap(k) >= 1 && SinRules.targetCap(k) <= 8, k.id + " cap");
            assertTrue(SinRules.worstPacketsPerUse(k) <= 30, k.id + " packets per use");
            assertTrue(k.reach <= 28.0, k.id + " reach");
            assertTrue(k.techDamage <= 20f, k.id + " damage");
        }
        assertEquals(1, SinRules.targetCap(SinRules.Kind.ENVY), "one shot, one target");
    }

    @Test
    void theLineAndTheCross() {
        assertTrue(SinRules.onLine(5, 0.5, 8, 1.0, 0.3));
        assertFalse(SinRules.onLine(-1, 0, 8, 1.0, 0.3), "behind");
        assertFalse(SinRules.onLine(9, 0, 8, 1.0, 0.3), "past the tip");
        assertFalse(SinRules.onLine(5, 2.0, 8, 1.0, 0.3), "beside it");
        assertTrue(SinRules.onCross(10, 0, 0, 0.3), "the stem");
        assertTrue(SinRules.onCross(SinRules.BAR_AT, 4.0, 1.0, 0.3), "the crossbar");
        assertFalse(SinRules.onCross(2, 4.0, 0, 0.3), "beside the stem, short of the bar");
        assertFalse(SinRules.onCross(SinRules.BAR_AT, 6.0, 0, 0.3), "past the bar's end");
    }

    @Test
    void damageAndCovetAreCapped() {
        assertEquals(0f, SinRules.techniqueDamage(Float.NaN, 100, false));
        assertEquals(0f, SinRules.techniqueDamage(10f, -5, false));
        assertEquals(16f * 1.5f, SinRules.techniqueDamage(16f, 100, true), 1e-4);
        assertTrue(SinRules.techniqueDamage(1e9f, 1000, true) <= 400f);
        assertFalse(SinRules.covetReady(100, 120), "inside the gap");
        assertTrue(SinRules.covetReady(120, 120));
        assertTrue(SinRules.covetReady(100, 1_000_000), "a clock set back does not lock the wielder out");
        assertEquals(SinRules.COVET_MAX_TICKS, SinRules.covetTicks(1_000_000));
        assertEquals(1, SinRules.covetTicks(-4));
    }

    @Test
    void everyWeaponHasItsDataFiles() throws IOException {
        for (String id : SinRules.IDS) {
            for (String f : new String[]{"assets/anime_multiverse/weapon_meshes/" + id + ".txt", "assets/anime_multiverse/textures/entity/weapon/" + id + ".png",
                    "assets/anime_multiverse/textures/entity/weapon/" + id + "_glow.png", "assets/anime_multiverse/textures/item/" + id + ".png",
                    "assets/anime_multiverse/models/item/" + id + ".json", "assets/anime_multiverse/models/item/" + id + "_icon.json",
                    "data/anime_multiverse/weapon_attributes/" + id + ".json"})
                assertTrue(Files.exists(RES.resolve(f)), id + ": missing " + f);
            assertTrue(Files.readString(RES.resolve("assets/anime_multiverse/weapon_classes.json")).contains("\"" + id + "\""), id + " has no weapon class");
            assertTrue(Files.readString(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "item", "WeaponEngravings.java")).contains("Map.entry(\"" + id + "\""), id + " has no engravings");
            assertTrue(Files.readString(Path.of("tools", "lang", "berserk_weapons.json")).contains("\"item.anime_multiverse." + id + "\""), id + " has no lang entry");
        }
        assertTrue(Files.readString(RES.resolve("data/anime_multiverse/weapon_attributes/black_chakrams.json")).contains("bettercombat:twin_blade"), "the chakrams swing as a pair");
    }
}
