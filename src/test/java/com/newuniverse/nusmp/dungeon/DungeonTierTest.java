package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Living Dungeon's tiers and its per-chunk ecosystem (docs/living_dungeon.md). */
class DungeonTierTest {
    @Test
    void depthDecidesTheTierAndThePlaceDecidesTheRest() {
        Set<DungeonTier> seen = EnumSet.noneOf(DungeonTier.class);
        for (int x = -2000; x <= 2000; x += 40)
            for (int z = -2000; z <= 2000; z += 40)
                for (int y : new int[]{100, 30, 0, -30, -50, -60}) seen.add(DungeonTier.at(x, y, z));
        assertEquals(EnumSet.allOf(DungeonTier.class), seen, "every tier exists somewhere");
        for (int x = -300; x <= 300; x += 17) {
            for (int z = -300; z <= 300; z += 17) {
                assertEquals(DungeonTier.at(x, 10, z), DungeonTier.at(x, 10, z), "stable");
                assertNotEquals(DungeonTier.GOLDEN_CITY, DungeonTier.at(x, 0, z), "the City is below y=-40");
                assertNotEquals(DungeonTier.GOLDEN_CITY, DungeonTier.at(x, 80, z));
                DungeonTier up = DungeonTier.at(x, 80, z);
                assertTrue(up == DungeonTier.UPPER_CATACOMBS || up == DungeonTier.VOLCANIC_VENTS, up.name());
            }
        }
    }

    @Test
    void volcanicAirIsThinnerThanTheRest() {
        assertTrue(DungeonTier.VOLCANIC_VENTS.oxygenCeiling < DungeonTier.UPPER_CATACOMBS.oxygenCeiling);
    }

    @Test
    void ecosystemValuesStayInRange() {
        EcosystemData e = new EcosystemData(5000f, -3f, 250f);
        assertEquals(EcosystemData.MAGICULE_MAX, e.magicule);
        assertEquals(0f, e.biomass);
        assertEquals(EcosystemData.OXYGEN_MAX, e.oxygen);
        assertFalse(e.thinAir());
        e.oxygen = 20f;
        assertTrue(e.thinAir());
        e.magicule = -9f;
        e.clamp();
        assertEquals(0f, e.magicule);
    }
}
