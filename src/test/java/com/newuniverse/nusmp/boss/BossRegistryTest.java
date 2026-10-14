package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Strict entity isolation: only this mod's own bosses are ever recognised. */
class BossRegistryTest {
    @Test
    void theSevenBossesAreRecognisedByNamespaceAndPath() {
        assertEquals("dante", BossRegistry.idOf("nusmp", "dante_zogratis").orElseThrow());
        assertEquals("vanica", BossRegistry.idOf("nusmp", "vanica_zogratis").orElseThrow());
        assertEquals("zenon", BossRegistry.idOf("nusmp", "zenon_zogratis").orElseThrow());
        assertEquals("lucius", BossRegistry.idOf("nusmp", "lucius_zogratis").orElseThrow());
        assertEquals("devil_avatar", BossRegistry.idOf("nusmp", "devil_avatar").orElseThrow());
        assertEquals("goliath", BossRegistry.idOf("nusmp", "goliath").orElseThrow());
        assertEquals("charybdis_spawn", BossRegistry.idOf("nusmp", "charybdis_spawn").orElseThrow());
        assertEquals(7, BossRegistry.ids().size());
        assertEquals(7, new HashSet<>(BossRegistry.ids()).size(), "ids are unique");
    }

    @Test
    void vanillaAndTensuraEntitiesAreNeverBosses() {
        // the same paths in another namespace, and ordinary mobs: all refused
        for (String ns : new String[]{"minecraft", "tensura", "manascore", "curios", "", "NUSMP", "nusmp "})
            for (String path : BossRegistry.paths())
                assertTrue(BossRegistry.idOf(ns, path).isEmpty(), ns + ":" + path);
        for (String path : new String[]{"zombie", "ender_dragon", "wither", "warden", "slime", "goblin", "orc_lord", "charybdis", "hound_dog", "lucius", "goliath_boss"})
            assertFalse(BossRegistry.isBoss("minecraft", path), path);
        assertFalse(BossRegistry.isBoss("tensura", "charybdis"));
        assertFalse(BossRegistry.isBoss("tensura", "goliath"), "a base-Tensura entity sharing a path stays untouched");
    }

    @Test
    void theOrderTheFollowersAndOtherModEntitiesAreExcluded() {
        for (String path : new String[]{"god_of_order", "dark_disciple", "blood_beast", "zogratis_clone", "spirit_lord", "zagred", "gatekeeper",
                "magicule_scale", "dungeon_slime", "tower_wizard"})
            assertTrue(BossRegistry.idOf("nusmp", path).isEmpty(), path);
        assertTrue(BossRegistry.idOf("nusmp", null).isEmpty());
        assertTrue(BossRegistry.idOf(null, "goliath").isEmpty());
    }

    @Test
    void everyBossHasABuffRowAndEveryRowIsABoss() {
        Set<String> rows = new HashSet<>();
        for (BossBuffTable.Profile p : BossBuffTable.all()) rows.add(p.bossId());
        assertEquals(new HashSet<>(BossRegistry.ids()), rows);
        for (String id : BossRegistry.ids()) assertTrue(BossRegistry.known(id));
        assertFalse(BossRegistry.known("god_of_order"));
        assertNull(BossBuffTable.profile("god_of_order"));
        assertNull(BossBuffTable.profile(null));
    }
}
