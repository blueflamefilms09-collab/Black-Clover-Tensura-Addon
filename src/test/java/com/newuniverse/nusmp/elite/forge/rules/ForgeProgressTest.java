package com.newuniverse.nusmp.elite.forge.rules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the progress a player has at the forge is stored in an attachment (saved with the player), so its reading must be TOLERANT of a save from an older or newer
 * version, and an untouched player must add nothing to their save. The pure class and its map form are tested here; the NBT adapter has an in-game self-test.
 */
class ForgeProgressTest {
    @Test
    void anEmptyProgressWritesNothing() {
        ForgeProgress p = new ForgeProgress();
        assertTrue(p.isEmpty());
        assertTrue(p.toMap().isEmpty());
        assertEquals(0, p.crafted());
        assertEquals(0, p.best());
    }

    @Test
    void recordingAForgeCountsEverywhere() {
        ForgeProgress p = new ForgeProgress();
        p.recordForge(StationTier.BASIC, "nusmp:iron_sword", Quality.RARE);
        p.recordForge(StationTier.MYTHRIL, "nusmp:thing", Quality.LEGENDARY);
        p.recordForge(StationTier.MYTHRIL, "nusmp:thing", Quality.COMMON);
        assertEquals(3, p.crafted());
        assertEquals(1, p.craftedAt(StationTier.BASIC));
        assertEquals(2, p.craftedAt(StationTier.MYTHRIL));
        assertEquals(1, p.craftedOf(Quality.RARE));
        assertEquals(1, p.craftedOf(Quality.COMMON));
        assertEquals(Quality.LEGENDARY.level(), p.best());
        assertEquals(2, p.topStation());
        assertEquals(2, p.known().size());
        assertTrue(p.knows("nusmp:thing"));
        assertFalse(p.isEmpty());
    }

    @Test
    void theMapFormRoundTrips() {
        ForgeProgress p = new ForgeProgress();
        p.recordForge(StationTier.DRAGONFORGE, "nusmp:a", Quality.MYTHICAL);
        p.setSpecialization(Specialization.RUNESMITH);
        p.setSpecialization(Specialization.WEAPONSMITH);
        ForgeProgress q = ForgeProgress.fromMap(p.toMap());
        assertEquals(p.toMap(), q.toMap());
        assertEquals(1, q.crafted());
        assertEquals(6, q.best());
        assertEquals("weaponsmith", q.specializationId());
        assertEquals(1, q.specChanges());
        assertEquals(3, q.topStation());
        assertEquals(1, p.copy().crafted());
    }

    @Test
    void readingToleratesMissingWrongAndHostileKeys() {
        assertTrue(ForgeProgress.fromMap(null).isEmpty());
        assertTrue(ForgeProgress.fromMap(new HashMap<>()).isEmpty());
        Map<String, Object> m = new HashMap<>();
        m.put("crafted", "twelve");              // wrong type
        m.put("by_tier", "no");                  // wrong type
        m.put("by_quality", List.of(1, "x", -5, 3));   // junk inside
        m.put("best", 99);                       // out of range
        m.put("top_station", -4);
        m.put("spec", "bladedancer");            // unknown
        m.put("known", List.of("a", 5, "", "b"));
        ForgeProgress p = ForgeProgress.fromMap(m);
        assertEquals(0, p.crafted());
        assertEquals(6, p.best());
        assertEquals(0, p.topStation());
        assertEquals("", p.specializationId());
        assertEquals(1, p.craftedOf(Quality.COMMON));
        assertEquals(0, p.craftedOf(Quality.UNCOMMON));
        assertEquals(0, p.craftedOf(Quality.RARE), "a negative count falls back to 0");
        assertEquals(3, p.craftedOf(Quality.EPIC));
        assertEquals(2, p.known().size());
        // a number of the wrong width is accepted
        Map<String, Object> n = new HashMap<>();
        n.put("crafted", 7L);
        n.put("best", 3.0);
        ForgeProgress q = ForgeProgress.fromMap(n);
        assertEquals(7, q.crafted());
        assertEquals(3, q.best());
    }

    @Test
    void theKnownListIsBounded() {
        ForgeProgress p = new ForgeProgress();
        for (int i = 0; i < ForgeProgress.MAX_KNOWN + 50; i++) p.recordForge(StationTier.BASIC, "nusmp:r" + i, Quality.COMMON);
        assertEquals(ForgeProgress.MAX_KNOWN, p.known().size());
        assertEquals(ForgeProgress.MAX_KNOWN + 50, p.crafted(), "the counts keep counting");
        List<Object> huge = new ArrayList<>();
        for (int i = 0; i < 2000; i++) huge.add("x" + i);
        Map<String, Object> m = new HashMap<>();
        m.put("known", huge);
        assertEquals(ForgeProgress.MAX_KNOWN, ForgeProgress.fromMap(m).known().size());
    }

    @Test
    void theFirstSpecializationIsFreeAndChangesAreCounted() {
        ForgeProgress p = new ForgeProgress();
        assertFalse(p.changeCosts());
        assertTrue(p.setSpecialization(Specialization.TOOLSMITH));
        assertEquals(0, p.specChanges());
        assertTrue(p.changeCosts());
        assertFalse(p.setSpecialization(Specialization.TOOLSMITH), "the same one is not a change");
        assertFalse(p.setSpecialization(null));
        assertTrue(p.setSpecialization(Specialization.ARMORSMITH));
        assertEquals(1, p.specChanges());
        assertNotNull(p.specialization());
        assertEquals(Specialization.ARMORSMITH, p.specialization());
    }
}
