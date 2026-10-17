package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Diamond Shield must be a pool that runs out and never a second health bar, so its arithmetic is held here. */
class CrystalShieldTest {
    @Test
    void itTakesBlowsUntilItIsUsedUpAndTheRestPasses() {
        CrystalShield s = new CrystalShield();
        s.open(100, 160, 100f);
        assertTrue(s.isOpen(100));
        CrystalShield.Verdict a = s.absorb(110, 30f);
        assertEquals(0f, a.passed(), 1e-6f);
        assertEquals(30f, a.absorbed(), 1e-6f);
        assertFalse(a.broke());
        assertEquals(70f, s.pool(111), 1e-6f);
        CrystalShield.Verdict b = s.absorb(120, 90f);
        assertEquals(20f, b.passed(), 1e-6f, "70 absorbed, 20 pass");
        assertEquals(70f, b.absorbed(), 1e-6f);
        assertTrue(b.broke());
        assertFalse(s.isOpen(121));
        assertEquals(0f, s.pool(121), 1e-6f);
    }

    @Test
    void whenItIsGoneNothingIsAbsorbed() {
        CrystalShield s = new CrystalShield();
        CrystalShield.Verdict v = s.absorb(5, 25f);
        assertEquals(25f, v.passed(), 1e-6f);
        assertEquals(0f, v.absorbed(), 1e-6f);
        assertFalse(v.broke());
    }

    @Test
    void itEndsByItselfWithTheTimeAndNothingRefillsIt() {
        CrystalShield s = new CrystalShield();
        s.open(0, 160, 100f);
        assertTrue(s.isOpen(159));
        assertFalse(s.isOpen(160));
        assertEquals(40f, s.absorb(160, 40f).passed(), 1e-6f);
        s.absorb(10, 10f);
        assertTrue(s.pool(11) <= 90f, "the pool only goes down");
    }

    @Test
    void anOddBlowChangesNothing() {
        CrystalShield s = new CrystalShield();
        s.open(0, 100, 50f);
        assertEquals(0f, s.absorb(1, 0f).absorbed(), 1e-6f);
        assertEquals(0f, s.absorb(1, -5f).absorbed(), 1e-6f);
        assertEquals(50f, s.pool(2), 1e-6f);
        s.close();
        assertFalse(s.isOpen(2));
    }
}
