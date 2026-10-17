package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lucifero's Gravity Shroud: it shreds what flies at him, and enough steel or anti-magic tears it. */
class ShroudStateTest {
    @Test
    void whileItStandsRangedAndMagicAreHalvedAndNothingElseIs() {
        ShroudState s = new ShroudState();
        s.raise(1000, 200);
        assertTrue(s.up(0) && s.up(999) && !s.up(1000));
        assertEquals(0.5f, s.factor(DamageKind.RANGED, 10), 1e-6f);
        assertEquals(0.5f, s.factor(DamageKind.MAGIC, 10), 1e-6f);
        assertEquals(1f, s.factor(DamageKind.MELEE, 10), 1e-6f);
        assertEquals(1f, s.factor(DamageKind.ANTI_MAGIC, 10), 1e-6f);
        assertEquals(1f, s.factor(DamageKind.OTHER, 10), 1e-6f);
        assertEquals(1f, s.factor(DamageKind.MAGIC, 1000), 1e-6f, "expired");
        assertEquals(1f, s.factor(null, 10), 1e-6f);
    }

    @Test
    void onlyMeleeAndAntiMagicWearItAndTheTearComesOnce() {
        ShroudState s = new ShroudState();
        s.raise(1000, 100);
        assertFalse(s.wear(DamageKind.RANGED, 500, 5), "arrows do not wear it");
        assertFalse(s.wear(DamageKind.MAGIC, 500, 5), "nor spells");
        assertFalse(s.wear(DamageKind.OTHER, 500, 5));
        assertEquals(0.0, s.worn(), 1e-9);
        assertFalse(s.wear(DamageKind.MELEE, 60, 5));
        assertEquals(0.6, s.worn(), 1e-9);
        assertTrue(s.wear(DamageKind.ANTI_MAGIC, 40, 6), "this one tears it");
        assertFalse(s.up(7), "torn means down");
        assertFalse(s.wear(DamageKind.MELEE, 500, 8), "a torn shroud cannot tear twice");
        assertEquals(1f, s.factor(DamageKind.RANGED, 8), 1e-6f);
    }

    @Test
    void raisingItAgainStartsFresh() {
        ShroudState s = new ShroudState();
        s.raise(1000, 100);
        s.wear(DamageKind.MELEE, 99, 1);
        s.raise(2000, 100);
        assertEquals(0.0, s.worn(), 1e-9);
        assertTrue(s.up(1500));
        s.drop();
        assertFalse(s.up(1));
        s.raise(10, 0);
        assertFalse(s.wear(DamageKind.MELEE, -3, 1) || s.wear(DamageKind.MELEE, 0, 1), "damage that is not positive does nothing");
    }

    @Test
    void theStaggerIsShortAndSmall() {
        assertTrue(ShroudState.STAGGER_TICKS <= 60 && ShroudState.STAGGER_TICKS >= 20);
        assertTrue(ShroudState.STAGGER_BONUS > 1f, "a bonus");
        assertTrue(ShroudState.STAGGER_BONUS <= 1.25f, "no more than the exposed bonus of the framework (1.25)");
    }
}
