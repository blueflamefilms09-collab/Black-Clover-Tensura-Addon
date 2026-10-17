package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a floor boss grows with the depth but never past a cap and never below the numbers its builder balanced; these tests pin the first floor (no change),
 * the growth per floor, the caps, the sign of the floor (the Abyssal road counts the same way) and the harmless handling of a bad rate.
 */
class FloorScaleTest {
    private static final ArcCharacter C = ArcCharacter.boss("fs_boss", "Boss").floors(null, 20, 80).build();

    @Test
    void itIsExactlyTheDesignOnItsFirstFloorAndAbove() {
        assertEquals(1.0, FloorScale.healthFactor(C, 20, 0.02), 0.0);
        assertEquals(1.0, FloorScale.healthFactor(C, 10, 0.02), 0.0);
        assertEquals(1.0, FloorScale.damageFactor(C, 20, 0.01), 0.0);
        assertEquals(0, FloorScale.floorsBelowDesign(C, 5));
    }

    @Test
    void itGrowsByTheRatePerFloorAndTheAbyssalRoadCountsTheSameWay() {
        assertEquals(1.0 + 0.02 * 20, FloorScale.healthFactor(C, 40, 0.02), 1e-9);
        assertEquals(1.0 + 0.02 * 20, FloorScale.healthFactor(C, -40, 0.02), 1e-9);
        assertEquals(1.0 + 0.01 * 20, FloorScale.damageFactor(C, 40, 0.01), 1e-9);
        assertTrue(FloorScale.healthFactor(C, 50, 0.02) > FloorScale.healthFactor(C, 40, 0.02));
    }

    @Test
    void theCapsHold() {
        assertEquals(FloorScale.MAX_HEALTH_FACTOR, FloorScale.healthFactor(C, 100, 1.0), 0.0);
        assertEquals(FloorScale.MAX_DAMAGE_FACTOR, FloorScale.damageFactor(C, 100, 1.0), 0.0);
        assertEquals(1.0, FloorScale.healthFactor(C, 100, -5.0), 0.0, "a negative rate cannot shrink a boss");
        assertEquals(1.0, FloorScale.damageFactor(C, 100, 0.0), 0.0);
    }
}
