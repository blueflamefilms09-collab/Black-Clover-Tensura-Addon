package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code high} is "the look the mod always had", and that is a promise, so it is pinned here against the numbers the code had before the pass (64 effects at once, full detail up to 96
 * blocks); {@code medium} and {@code low} must be strictly cheaper in every number; the names the config accepts parse in any case and anything else falls back; and the settings holder clamps
 * what a hand-edited file might say (an aura far limit below the near one, a refresh period of 3 ticks) and has the pre-1.99 defaults.
 */
class VfxQualityTest {
    @Test
    void highKeepsTheOldNumbers() {
        VfxQuality h = VfxQuality.HIGH;
        assertEquals(64, h.maxActive);                    // VfxManager.MAX_ACTIVE was 64
        assertEquals(1.0f, h.detail);
        assertEquals(1.0f, h.detailAt(0.0));
        assertEquals(1.0f, h.detailAt(96.0));
        assertTrue(h.frameVertices >= 64 * 400, "the frame budget must never bind on high: 64 effects of at most 400 vertices");
    }

    @Test
    void mediumAndLowAreStrictlyCheaperInEveryNumber() {
        VfxQuality h = VfxQuality.HIGH, m = VfxQuality.MEDIUM, l = VfxQuality.LOW;
        assertTrue(m.detail < h.detail && l.detail < m.detail);
        assertTrue(m.maxActive < h.maxActive && l.maxActive < m.maxActive);
        assertTrue(m.frameVertices < h.frameVertices && l.frameVertices < m.frameVertices);
        assertTrue(m.lodNear < h.lodNear && l.lodNear < m.lodNear);
        assertTrue(m.lodFar < h.lodFar && l.lodFar < m.lodFar);
    }

    @Test
    void detailNeverExceedsTheQualitiesOwnAndDropsWithDistance() {
        for (VfxQuality q : VfxQuality.values()) {
            float last = 2f;
            for (double d = 0; d < 400; d += 3) {
                float v = q.detailAt(d);
                assertTrue(v <= q.detail, q + " at " + d);
                assertTrue(v <= last, q + " must not get more detailed with distance at " + d);
                assertEquals(q.detailAt(d), q.detailAtSq(d * d), q + " squared at " + d);
                assertEquals(q.culled(d), q.culledSq(d * d));
                last = v;
            }
            assertEquals(0f, q.detailAt(q.lodFar + 1));
            assertTrue(q.culled(q.lodFar + 1));
            assertFalse(q.culled(q.lodFar));
        }
    }

    @Test
    void namesParseInAnyCaseAndFallBack() {
        assertSame(VfxQuality.LOW, VfxQuality.byName("low", VfxQuality.HIGH));
        assertSame(VfxQuality.MEDIUM, VfxQuality.byName(" Medium ", VfxQuality.HIGH));
        assertSame(VfxQuality.HIGH, VfxQuality.byName("HIGH", VfxQuality.LOW));
        assertSame(VfxQuality.LOW, VfxQuality.byName("ultra", VfxQuality.LOW));
        assertSame(VfxQuality.MEDIUM, VfxQuality.byName(null, VfxQuality.MEDIUM));
        assertEquals(null, VfxQuality.byName("auto", null));
    }

    @Test
    void theSettingsHaveThePre199DefaultsAndClamp() {
        PerfSettings.resetDefaults();
        try {
            assertSame(VfxQuality.HIGH, PerfSettings.vfxQuality());
            assertEquals(10, PerfSettings.relicScanInterval());          // Relics.TTL
            assertEquals(5, PerfSettings.entityScanInterval());          // ElsdociaEvents.SCAN_EVERY
            assertEquals(48, PerfSettings.auraLodNear());
            assertEquals(96, PerfSettings.auraLodFar());
            PerfSettings.setAuraLod(100, 50);
            assertTrue(PerfSettings.auraLodFar() >= PerfSettings.auraLodNear(), "far is never below near");
            PerfSettings.setRelicScanInterval(0);
            assertEquals(1, PerfSettings.relicScanInterval());
            PerfSettings.setEntityScanInterval(100000);
            assertEquals(PerfMath.MAX_INTERVAL, PerfSettings.entityScanInterval());
            PerfSettings.setPacketRefreshTicks(3);
            assertEquals(20, PerfSettings.packetRefreshTicks());
            PerfSettings.setPacketRefreshTicks(0);
            assertEquals(0, PerfSettings.packetRefreshTicks());
            PerfSettings.setPacketRefreshTicks(-4);
            assertEquals(0, PerfSettings.packetRefreshTicks());
            PerfSettings.setMaxParticlesPerTick(-1);
            assertEquals(0, PerfSettings.maxParticlesPerTick());
        } finally {
            PerfSettings.resetDefaults();
        }
    }

    @Test
    void theClientQualityOverridesTheCommonOneAndAutoFollowsIt() {
        PerfSettings.resetDefaults();
        try {
            PerfSettings.setCommonQuality(VfxQuality.MEDIUM);
            assertSame(VfxQuality.MEDIUM, PerfSettings.vfxQuality());
            PerfSettings.setClientQuality(VfxQuality.LOW);
            assertSame(VfxQuality.LOW, PerfSettings.vfxQuality());
            assertSame(VfxQuality.MEDIUM, PerfSettings.commonQuality());
            PerfSettings.setClientQuality(null);
            assertSame(VfxQuality.MEDIUM, PerfSettings.vfxQuality());
        } finally {
            PerfSettings.resetDefaults();
        }
    }
}
