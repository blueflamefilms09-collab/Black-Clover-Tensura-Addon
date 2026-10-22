package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a spirit's own mana pool is a pure object with no per-tick work, so its rules are pinned: it starts full, spending is all or nothing, it refills lazily from the game time (never above the maximum, a
 * clock that went backwards only moves the stamp), a new bond changes the limits without refilling it, and the maximum and the regeneration grow with trust (held to 0..100).
 */
class SpiritManaTest {
    private static final double EPS = 1e-9;

    @Test
    void startsFullAndSpendingIsAllOrNothing() {
        SpiritMana m = new SpiritMana(60, 2, 0);
        assertEquals(60, m.current(), EPS);
        assertTrue(m.spend(20));
        assertEquals(40, m.current(), EPS);
        assertFalse(m.spend(41), "more than it holds");
        assertEquals(40, m.current(), EPS);
        assertTrue(m.spend(40));
        assertEquals(0, m.current(), EPS);
        assertFalse(m.canSpend(0.5));
        assertTrue(m.canSpend(0), "a free spell is always affordable");
    }

    @Test
    void refillsLazilyFromTheClockAndNeverAboveTheMaximum() {
        SpiritMana m = new SpiritMana(60, 3, 100);
        m.spend(60);
        m.advance(100 + 40);                                  // 2 seconds at 3 per second
        assertEquals(6, m.current(), EPS);
        m.advance(100 + 40 + 20 * 1000);
        assertEquals(60, m.current(), EPS);
        m.advance(5);                                         // the clock went backwards: nothing changes but the stamp
        assertEquals(60, m.current(), EPS);
        m.spend(10);
        m.advance(5);
        assertEquals(50, m.current(), EPS);
    }

    @Test
    void aNewBondChangesTheLimitsWithoutARefill() {
        SpiritMana m = new SpiritMana(60, 2, 0);
        m.spend(30);
        m.configure(100, 5);
        assertEquals(30, m.current(), EPS);
        assertEquals(100, m.max(), EPS);
        assertEquals(5, m.regenPerSecond(), EPS);
        m.configure(20, 1);
        assertEquals(20, m.current(), EPS, "a smaller pool cuts what it holds");
        assertEquals(1.0, m.fill(), EPS);
    }

    @Test
    void theBondRaisesThePoolAndTheRegeneration() {
        assertEquals(60, SpiritMana.maxFor(60, 0.6, 0), EPS);
        assertEquals(120, SpiritMana.maxFor(60, 0.6, 100), EPS);
        assertEquals(120, SpiritMana.maxFor(60, 0.6, 5000), EPS, "trust is held to 100");
        assertEquals(60, SpiritMana.maxFor(60, 0.6, -40), EPS, "and to 0");
        assertTrue(SpiritMana.regenFor(2, 0.05, 80) > SpiritMana.regenFor(2, 0.05, 20));
        assertEquals(7, SpiritMana.regenFor(2, 0.05, 100), EPS);
        assertTrue(SpiritMana.maxFor(-50, 0, 0) >= 1, "a pool is never below one");
    }
}
