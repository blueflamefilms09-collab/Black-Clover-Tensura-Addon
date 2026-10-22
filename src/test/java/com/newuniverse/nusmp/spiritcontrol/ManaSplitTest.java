package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every combat cast costs BOTH pools, so the split is pinned as pure arithmetic: at the default half and half the two parts add up to the whole price, at 0 the spirit pays everything and the owner nothing
 * (and no floor is charged), at 1 the reverse, a bad share is held to 0..1, a missing number reads as one half, and the owner's magicules follow their maximum, the floor and the mana law's multiplier.
 */
class ManaSplitTest {
    private static final double EPS = 1e-9;

    @Test
    void halfAndHalfAddsUpToTheWholePrice() {
        for (double units : new double[]{1, 8, 14.5, 16, 100}) {
            assertEquals(units, ManaSplit.spiritUnits(units, 0.5) + ManaSplit.userUnits(units, 0.5), EPS);
            assertEquals(units / 2, ManaSplit.spiritUnits(units, 0.5), EPS);
        }
    }

    @Test
    void theEdgesOfTheShare() {
        assertEquals(10, ManaSplit.spiritUnits(10, 0), EPS);
        assertEquals(0, ManaSplit.userUnits(10, 0), EPS);
        assertEquals(0, ManaSplit.spiritUnits(10, 1), EPS);
        assertEquals(10, ManaSplit.userUnits(10, 1), EPS);
        assertEquals(0.0, ManaSplit.clampShare(-3), EPS);
        assertEquals(1.0, ManaSplit.clampShare(7), EPS);
        assertEquals(0.5, ManaSplit.clampShare(Double.NaN), EPS);
        assertEquals(0, ManaSplit.spiritUnits(-5, 0.5), EPS);
    }

    @Test
    void theOwnersMagiculesFollowTheirMaximumTheFloorAndTheLawsMultiplier() {
        // 8 units at half: 4 user units, 0.25 percent of a 10000 maximum per unit = 100 magicules
        assertEquals(100.0, ManaSplit.userMagicules(8, 0.5, 10000, 0.25, 15, 1.0), EPS);
        // a small pool hits the floor
        assertEquals(15.0, ManaSplit.userMagicules(8, 0.5, 100, 0.25, 15, 1.0), EPS);
        // the mana law's multiplier applies to the whole price
        assertEquals(200.0, ManaSplit.userMagicules(8, 0.5, 10000, 0.25, 15, 2.0), EPS);
        // when the owner pays nothing there is no floor either
        assertEquals(0.0, ManaSplit.userMagicules(8, 0.0, 10000, 0.25, 15, 1.0), EPS);
        // a stronger mage pays more for the same spell
        assertTrue(ManaSplit.userMagicules(8, 0.5, 20000, 0.25, 15, 1.0) > ManaSplit.userMagicules(8, 0.5, 10000, 0.25, 15, 1.0));
        // nonsense inputs never go negative
        assertEquals(0.0, ManaSplit.userMagicules(8, 0.5, -5, -1, -1, -1), EPS);
    }
}
