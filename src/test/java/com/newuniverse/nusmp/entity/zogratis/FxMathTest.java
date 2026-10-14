package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The boss effect numbers: payload flags, Lucius's gold-to-purple drift and the per-tick packet cap. */
class FxMathTest {
    @Test
    void flagsLiveInTheTwoLowBitsOnly() {
        long r = 0x7FFF_FFFF_FFFF_FFFFL;
        assertEquals(r & ~3L, FxMath.seed(r, false, false));
        assertEquals((r & ~3L) | 1, FxMath.seed(r, true, false));
        assertEquals((r & ~3L) | 2, FxMath.seed(r, false, true));
        assertEquals((r & ~3L) | 3, FxMath.seed(r, true, true));
        assertEquals(r & ~3L, FxMath.seed(r, true, true) & ~3L, "the random part is untouched");
    }

    @Test
    void luciusDriftsGoldToPurpleAndNeverBack() {
        float last = -1f;
        for (int p = 0; p <= 100; p += 10) {
            float k = FxMath.purple(p);
            assertTrue(k >= last, "purple fell at " + p);
            last = k;
        }
        assertEquals(0f, FxMath.purple(20));
        assertEquals(0f, FxMath.purple(40));
        assertEquals(1f, FxMath.purple(100));
        assertEquals(FxMath.LUCIUS_GOLD, FxMath.divine(20));
        assertEquals(FxMath.LUCIUS_PURPLE, FxMath.divine(100));
        assertNotEquals(FxMath.divine(60), FxMath.divine(80));
    }

    @Test
    void mixClampsAndBlends() {
        assertEquals(0x000000, FxMath.mix(0x000000, 0xFFFFFF, -1f));
        assertEquals(0xFFFFFF, FxMath.mix(0x000000, 0xFFFFFF, 5f));
        assertEquals(0x808080, FxMath.mix(0x000000, 0xFFFFFF, 0.5f));
    }

    @Test
    void throttleCapsPerTickAndResets() {
        FxMath.Throttle t = new FxMath.Throttle();
        int sent = 0;
        for (int i = 0; i < 100; i++) if (t.allow(10L)) sent++;
        assertEquals(FxMath.MAX_PER_TICK, sent);
        assertFalse(t.allow(10L));
        assertTrue(t.allow(11L), "a new tick starts fresh");
        assertEquals(1, t.sentThisTick());
    }
}
