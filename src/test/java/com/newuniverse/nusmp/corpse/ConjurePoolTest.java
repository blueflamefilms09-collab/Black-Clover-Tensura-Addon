package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The conjure pool: right body for the place, a fair weighted draw, a count that grows with mastery and respects the cap. */
class ConjurePoolTest {
    @Test
    void settingFollowsDimensionThenWaterThenDarkThenClimate() {
        assertEquals(ConjurePool.Setting.NETHER, ConjurePool.setting("minecraft:the_nether", true, true, true, true));
        assertEquals(ConjurePool.Setting.END, ConjurePool.setting("minecraft:the_end", false, false, false, false));
        assertEquals(ConjurePool.Setting.WET, ConjurePool.setting("minecraft:overworld", true, true, true, true));
        assertEquals(ConjurePool.Setting.UNDERGROUND, ConjurePool.setting("minecraft:overworld", false, true, true, true));
        assertEquals(ConjurePool.Setting.COLD, ConjurePool.setting("minecraft:overworld", false, false, true, true));
        assertEquals(ConjurePool.Setting.DRY, ConjurePool.setting("minecraft:overworld", false, false, false, true));
        assertEquals(ConjurePool.Setting.SURFACE, ConjurePool.setting("some:other", false, false, false, false));
    }

    @Test
    void everyPoolIsNonEmptyAndOnlyVanillaUndead() {
        for (ConjurePool.Setting s : ConjurePool.Setting.values()) {
            List<ConjurePool.Entry> p = ConjurePool.pool(s);
            assertFalse(p.isEmpty(), s.toString());
            for (ConjurePool.Entry e : p) {
                assertTrue(e.id().startsWith("minecraft:"), e.id());
                assertTrue(e.weight() > 0);
            }
        }
    }

    @Test
    void placesGetTheirOwnBodies() {
        assertTrue(ids(ConjurePool.Setting.DRY).contains("minecraft:husk"));
        assertTrue(ids(ConjurePool.Setting.COLD).contains("minecraft:stray"));
        assertTrue(ids(ConjurePool.Setting.WET).contains("minecraft:drowned"));
        assertFalse(ids(ConjurePool.Setting.SURFACE).contains("minecraft:drowned"));
    }

    @Test
    void theDrawIsWeightedAndClamped() {
        List<ConjurePool.Entry> p = List.of(new ConjurePool.Entry("a", 3), new ConjurePool.Entry("b", 1));
        assertEquals("a", ConjurePool.pick(p, 0.0));
        assertEquals("a", ConjurePool.pick(p, 0.74));
        assertEquals("b", ConjurePool.pick(p, 0.76));
        assertEquals("b", ConjurePool.pick(p, 5.0));
        assertEquals("a", ConjurePool.pick(p, -1.0));
        assertNull(ConjurePool.pick(List.of(), 0.5));
    }

    @Test
    void everyEntryCanBeDrawn() {
        for (ConjurePool.Setting s : ConjurePool.Setting.values()) {
            List<ConjurePool.Entry> p = ConjurePool.pool(s);
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (int i = 0; i < 1000; i++) seen.add(ConjurePool.pick(p, i / 1000.0));
            assertEquals(p.size(), seen.size(), s.toString());
        }
    }

    @Test
    void moddedUndeadAreAddedOnlyWhenTheyExistAndNeverInWater() {
        java.util.function.Predicate<String> has = id -> id.equals("tensura:ghoul");
        List<ConjurePool.Entry> p = ConjurePool.poolWithModded(ConjurePool.Setting.UNDERGROUND, has, true);
        assertTrue(p.stream().anyMatch(e -> e.id().equals("tensura:ghoul")));
        assertFalse(p.stream().anyMatch(e -> e.id().equals("tensura:wight")));
        assertEquals(ConjurePool.pool(ConjurePool.Setting.UNDERGROUND).size(), ConjurePool.poolWithModded(ConjurePool.Setting.UNDERGROUND, has, false).size());
        assertEquals(ConjurePool.pool(ConjurePool.Setting.WET).size(), ConjurePool.poolWithModded(ConjurePool.Setting.WET, has, true).size());
    }

    @Test
    void countGrowsWithMasteryAndRespectsTheCap() {
        assertEquals(1, ConjurePool.count(0.0, 8));
        assertEquals(1, ConjurePool.count(0.30, 8));
        assertEquals(2, ConjurePool.count(0.34, 8));
        assertEquals(3, ConjurePool.count(0.67, 8));
        assertEquals(3, ConjurePool.count(1.0, 8));
        assertEquals(2, ConjurePool.count(1.0, 2));
        assertEquals(1, ConjurePool.count(1.0, 0));
    }

    private static List<String> ids(ConjurePool.Setting s) {
        return ConjurePool.pool(s).stream().map(ConjurePool.Entry::id).toList();
    }
}
