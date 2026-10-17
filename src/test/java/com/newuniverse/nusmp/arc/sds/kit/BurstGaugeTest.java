package com.newuniverse.nusmp.arc.sds.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: "a burst breaks it" is the shared counter of the Sins' concentration moves, so the trigger, the double weight of anti-magic, the cool-down and the window are pinned. */
class BurstGaugeTest {
    @Test
    void aBurstInsideTheWindowTriggersExactlyOnceAndEmptiesTheWindow() {
        BurstGauge g = new BurstGauge(60, 400);
        assertFalse(g.hit(100, 80f, 200f, false));
        assertFalse(g.hit(120, 80f, 200f, false));
        assertEquals(160f, g.held(121), 1e-6f);
        assertTrue(g.hit(140, 80f, 200f, false), "240 inside the span is over 200");
        assertEquals(0f, g.held(141), 1e-6f, "the window is emptied by the trigger");
    }

    @Test
    void blowsSpreadOverTimeNeverTrigger() {
        BurstGauge g = new BurstGauge(60, 0);
        for (int i = 0; i < 40; i++) assertFalse(g.hit(i * 40L, 80f, 200f, false), "one blow of 80 every two seconds is never 200 inside three");
    }

    @Test
    void antiMagicCountsDouble() {
        BurstGauge g = new BurstGauge(60, 0);
        assertFalse(g.hit(0, 60f, 200f, true));
        assertTrue(g.hit(10, 50f, 200f, true), "120 + 100 = 220");
        BurstGauge h = new BurstGauge(60, 0);
        assertFalse(h.hit(0, 60f, 200f, false));
        assertFalse(h.hit(10, 50f, 200f, false), "the same blows without anti-magic are 110");
    }

    @Test
    void afterATriggerItStaysQuietForItsCooldown() {
        BurstGauge g = new BurstGauge(60, 400);
        assertTrue(g.hit(0, 500f, 200f, false));
        assertEquals(400, g.cooldownLeft(0));
        assertFalse(g.hit(100, 500f, 200f, false), "a huge blow during the cooldown does not trigger");
        assertEquals(300, g.cooldownLeft(100));
        assertFalse(g.hit(399, 10f, 200f, false));
        assertTrue(g.hit(400, 500f, 200f, false), "ready again at the tick the cooldown ends");
        assertEquals(0, g.cooldownLeft(900));
    }

    @Test
    void aBlowDuringTheCooldownStillCountsTowardsTheNextWindow() {
        BurstGauge g = new BurstGauge(60, 100);
        assertTrue(g.hit(0, 300f, 200f, false));
        assertFalse(g.hit(50, 150f, 200f, false));
        assertTrue(g.hit(100, 100f, 200f, false), "the 150 of tick 50 is still inside the span when the cooldown is over at tick 100");
    }

    @Test
    void aThresholdOfNothingNeverTriggers() {
        BurstGauge g = new BurstGauge(60, 0);
        assertFalse(g.hit(0, 1000f, 0f, false));
        assertFalse(g.hit(1, 1000f, -5f, false));
        assertFalse(g.hit(2, 1000f, Float.NaN, false));
    }

    @Test
    void resetForgetsTheWindowAndTheCooldown() {
        BurstGauge g = new BurstGauge(60, 400);
        assertTrue(g.hit(0, 300f, 200f, false));
        assertFalse(g.hit(10, 100f, 200f, false));
        g.reset();
        assertEquals(0, g.cooldownLeft(11));
        assertEquals(0f, g.held(11), 1e-6f);
        assertTrue(g.hit(12, 300f, 200f, false));
    }
}
