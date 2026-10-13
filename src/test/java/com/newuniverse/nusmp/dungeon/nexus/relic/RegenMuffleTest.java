package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Amulet's fallback for a player with no regeneration attribute: half of a natural rise is taken back (docs/living_dungeon.md section 7). */
class RegenMuffleTest {
    @Test
    void halfOfANaturalRiseIsTakenBack() {
        assertEquals(5.0, RegenMuffle.takeBack(1000.0, 1010.0, 10000.0), 1e-9);
        assertEquals(0.5, RegenMuffle.takeBack(0.0, 1.0, 10000.0), 1e-9);
    }

    @Test
    void nothingIsTakenWhenTheStoreFellOrStoodStill() {
        assertEquals(0.0, RegenMuffle.takeBack(500.0, 500.0, 10000.0));
        assertEquals(0.0, RegenMuffle.takeBack(500.0, 120.0, 10000.0), "spending mana is not regeneration");
    }

    @Test
    void aBigJumpIsNotNaturalAndIsLeftAlone() {
        double max = 10000.0;
        assertEquals(0.0, RegenMuffle.takeBack(1000.0, 1000.0 + max * RegenMuffle.NATURAL_RISE + 1.0, max), "a potion or the Sheath's drink");
        assertEquals(0.0, RegenMuffle.takeBack(0.0, max, max));
        assertTrue(RegenMuffle.takeBack(1000.0, 1000.0 + max * RegenMuffle.NATURAL_RISE, max) > 0.0, "up to the ceiling it is regeneration");
    }

    @Test
    void anUnknownOrEmptyPoolIsSafe() {
        assertEquals(0.0, RegenMuffle.takeBack(0.0, 5.0, 0.0));
        assertEquals(0.0, RegenMuffle.takeBack(0.0, 5.0, -3.0));
        assertEquals(0.0, RegenMuffle.takeBack(Double.NaN, 5.0, 100.0));
        assertEquals(0.0, RegenMuffle.takeBack(0.0, Double.NaN, 100.0));
        assertEquals(0.0, RegenMuffle.takeBack(0.0, 5.0, Double.NaN));
        assertEquals(0.0, RegenMuffle.takeBack(0.0, Double.POSITIVE_INFINITY, 100.0));
    }

    @Test
    void theShareIsHalf() {
        assertEquals(0.5, RegenMuffle.SHARE, 1e-12);
        double before = 200.0, after = 210.0, back = RegenMuffle.takeBack(before, after, 10000.0);
        assertEquals(5.0, (after - back) - before, 1e-9, "the wearer keeps half the rise");
    }
}
