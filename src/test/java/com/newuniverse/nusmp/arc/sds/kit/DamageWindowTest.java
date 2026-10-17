package com.newuniverse.nusmp.arc.sds.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: several of the Sins' rules ask how much damage came in the last few seconds, so the sliding window they share is tested. */
class DamageWindowTest {
    @Test
    void theWindowHoldsOnlyTheLastSpanTicks() {
        DamageWindow w = new DamageWindow(60);
        assertEquals(10f, w.add(100, 10f), 1e-6f);
        assertEquals(25f, w.add(130, 15f), 1e-6f);
        assertEquals(25f, w.total(159), 1e-6f, "the first blow is still 59 ticks old");
        assertEquals(15f, w.total(160), 1e-6f, "it leaves at exactly 60 ticks");
        assertEquals(0f, w.total(190), 1e-6f);
    }

    @Test
    void badAmountsCountForNothing() {
        DamageWindow w = new DamageWindow(20);
        assertEquals(0f, w.add(0, -5f), 1e-6f);
        assertEquals(0f, w.add(1, Float.NaN), 1e-6f);
        assertEquals(0f, w.add(2, Float.POSITIVE_INFINITY), 1e-6f);
        assertEquals(3f, w.add(3, 3f), 1e-6f);
        w.clear();
        assertEquals(0f, w.total(3), 1e-6f);
    }

    @Test
    void theGaugeTriggersOnceAtTheThresholdAndRestsForItsCooldown() {
        BurstGauge g = new BurstGauge(60, 400);
        assertFalse(g.hit(0, 100f, 175f, false));
        assertTrue(g.hit(20, 80f, 175f, false), "180 inside the window crosses 175");
        assertEquals(0f, g.held(20), 1e-6f, "the window is emptied by a trigger");
        assertFalse(g.hit(30, 400f, 175f, false), "no second trigger inside the cooldown");
        assertEquals(390, g.cooldownLeft(30));
        assertTrue(g.hit(500, 400f, 175f, false), "after the cooldown a burst triggers again");
    }

    @Test
    void antiMagicCountsDouble() {
        BurstGauge g = new BurstGauge(60, 0);
        assertFalse(g.hit(0, 80f, 175f, false));
        assertTrue(g.hit(5, 60f, 175f, true), "80 + 2 x 60 = 200");
    }

    @Test
    void aZeroThresholdNeverTriggersAndAResetForgetsEverything() {
        BurstGauge g = new BurstGauge(60, 100);
        assertFalse(g.hit(0, 1000f, 0f, false));
        assertTrue(g.hit(1, 1000f, 100f, false));
        g.reset();
        assertEquals(0, g.cooldownLeft(2));
        assertTrue(g.hit(3, 1000f, 100f, false));
    }
}
