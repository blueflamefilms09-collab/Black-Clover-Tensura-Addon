package com.newuniverse.nusmp.vfx.client.layer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The cached templates of the Mana Zone layer: complete, on the unit sphere, closed rings, a stable hash, and the ring of ended zones. */
class ManaZoneGeometryTest {
    @Test
    void theGeometryTablesAreCompleteAndOnTheUnitSphere() {
        for (int l = 0; l < 3; l++) {
            int lon = ManaZoneGeometry.LON[l], lat = ManaZoneGeometry.LAT[l];
            assertEquals((lat + 1) * (lon + 1) * 3, ManaZoneGeometry.SHELL_POS[l].length);
            assertEquals(lat * lon * 3, ManaZoneGeometry.SHELL_NRM[l].length);
            for (int i = 0; i < ManaZoneGeometry.SHELL_POS[l].length; i += 3) {
                float[] p = ManaZoneGeometry.SHELL_POS[l];
                assertEquals(1.0, Math.sqrt(p[i] * p[i] + p[i + 1] * p[i + 1] + p[i + 2] * p[i + 2]), 1e-5);
            }
            assertEquals(ManaZoneGeometry.RING[l] + 1, ManaZoneGeometry.RING_COS[l].length);
            assertEquals(ManaZoneGeometry.RING_COS[l][0], ManaZoneGeometry.RING_COS[l][ManaZoneGeometry.RING[l]], 1e-5f, "the ring closes");
        }
        assertEquals(ManaZoneGeometry.hash01(5, 3), ManaZoneGeometry.hash01(5, 3), 0f);
        for (int i = 0; i < 500; i++) { float h = ManaZoneGeometry.hash01(99, i); assertTrue(h >= 0f && h < 1f); }
        ManaZoneGeometry.clearEnded();
        assertFalse(ManaZoneGeometry.isEnded(42));
        ManaZoneGeometry.markEnded(42);
        assertTrue(ManaZoneGeometry.isEnded(42));
        for (long s = 100; s < 130; s++) ManaZoneGeometry.markEnded(s);
        assertFalse(ManaZoneGeometry.isEnded(42), "the ring of ended zones forgets the oldest");
        assertTrue(ManaZoneGeometry.isEnded(129));
        ManaZoneGeometry.clearEnded();
    }

}
