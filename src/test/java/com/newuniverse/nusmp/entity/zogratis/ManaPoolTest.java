package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A finite pool that runs dry below 60% power, and an unbound one from there on. */
class ManaPoolTest {
    @Test
    void finitePoolRunsDryAndRefills() {
        ManaPool m = new ManaPool(PowerCurve.tuning(20));
        assertFalse(m.infinite());
        assertEquals(100, m.cap(), 1e-9);
        assertEquals(100, m.current(), 1e-9);
        assertTrue(m.spend(60));
        assertTrue(m.canAfford(40));
        assertFalse(m.canAfford(40.5));
        assertFalse(m.spend(60), "cannot pay twice");
        assertEquals(40, m.current(), 1e-9);
        for (int i = 0; i < 20 * 8; i++) m.tick();                       // 8 s at 2.5 per second
        assertEquals(60, m.current(), 1e-6);
        for (int i = 0; i < 20 * 100; i++) m.tick();
        assertEquals(100, m.current(), 1e-9, "never above the cap");
    }

    @Test
    void drawingMorePowerRefillsAndWidensThePool() {
        ManaPool m = new ManaPool(PowerCurve.tuning(20));
        m.spend(90);
        m.retune(PowerCurve.tuning(40));
        assertEquals(140, m.cap(), 1e-9);
        assertEquals(140, m.current(), 1e-9);
    }

    @Test
    void unboundFromSixtyPercent() {
        ManaPool m = new ManaPool(PowerCurve.tuning(60));
        assertTrue(m.infinite());
        assertTrue(m.canAfford(1e9));
        assertTrue(m.spend(1e9));
        assertEquals(Double.POSITIVE_INFINITY, m.available());
        assertEquals(1.0, m.fraction(), 1e-9);
        assertEquals(m.cap(), m.current(), 1e-9);
    }

    @Test
    void setIsClamped() {
        ManaPool m = new ManaPool(PowerCurve.tuning(40));
        m.set(-5);
        assertEquals(0, m.current(), 1e-9);
        m.set(1e6);
        assertEquals(140, m.current(), 1e-9);
    }
}
