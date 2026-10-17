package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Heath's Crystal Ice Shield: a wall against range, not against a blade. */
class FrostShieldTest {
    @Test
    void rangeIsSwallowedUntilThePoolIsGone() {
        FrostShield s = new FrostShield();
        s.raise(0, 140, 100);
        assertTrue(s.up(0));
        FrostShield.Result r = s.absorb(10, 30, FrostShield.Blow.RANGED);
        assertEquals(0.0, r.through(), 1e-9);
        assertFalse(r.broke());
        assertEquals(70.0, s.pool(), 1e-9);
        r = s.absorb(11, 70, FrostShield.Blow.RANGED);
        assertEquals(0.0, r.through(), 1e-9, "exactly the pool: swallowed, and the shield is spent");
        r = s.absorb(12, 5, FrostShield.Blow.RANGED);
        assertEquals(5.0, r.through(), 1e-9, "no shield left");
        assertFalse(s.up(12));
    }

    @Test
    void theLastBlowOfARangedPoolPassesTheRemainderAndBreaksIt() {
        FrostShield s = new FrostShield();
        s.raise(0, 140, 50);
        FrostShield.Result r = s.absorb(1, 80, FrostShield.Blow.RANGED);
        assertEquals(30.0, r.through(), 1e-9);
        assertTrue(r.broke());
        assertFalse(s.up(1));
    }

    @Test
    void aBladeGoesThroughAndOnlyCracksIt() {
        FrostShield s = new FrostShield();
        s.raise(0, 140, 100);
        FrostShield.Result r = s.absorb(1, 40, FrostShield.Blow.MELEE);
        assertEquals(40.0, r.through(), 1e-9, "a melee blow is not softened");
        assertFalse(r.broke());
        assertEquals(80.0, s.pool(), 1e-9, "but half of it comes off the pool");
        r = s.absorb(2, 170, FrostShield.Blow.MELEE);
        assertEquals(170.0, r.through(), 1e-9);
        assertTrue(r.broke(), "the pool is gone: cracked through");
        assertFalse(s.up(2));
    }

    @Test
    void antiMagicShattersItAtOnce() {
        FrostShield s = new FrostShield();
        s.raise(0, 140, 1000);
        FrostShield.Result r = s.absorb(1, 12, FrostShield.Blow.ANTI_MAGIC);
        assertEquals(12.0, r.through(), 1e-9);
        assertTrue(r.broke());
        assertFalse(s.up(2));
    }

    @Test
    void itEndsWithItsTimeAndNeverSaysItBrokeThen() {
        FrostShield s = new FrostShield();
        s.raise(0, 140, 100);
        assertTrue(s.up(139));
        assertFalse(s.up(140));
        FrostShield.Result r = s.absorb(140, 20, FrostShield.Blow.RANGED);
        assertEquals(20.0, r.through(), 1e-9);
        assertFalse(r.broke(), "running out of time is not a shatter");
        assertEquals(0.0, s.absorb(1, 0, FrostShield.Blow.MELEE).through(), 1e-9, "a blow of no size is nothing");
    }

    @Test
    void aBlowIsClassifiedByWhoDealtItAndFromHowFar() {
        assertEquals(FrostShield.Blow.ANTI_MAGIC, FrostShield.classify(true, true, 2));
        assertEquals(FrostShield.Blow.ANTI_MAGIC, FrostShield.classify(false, true, 30));
        assertEquals(FrostShield.Blow.MELEE, FrostShield.classify(true, false, 4.5));
        assertEquals(FrostShield.Blow.RANGED, FrostShield.classify(true, false, 4.6), "a long reach is still range");
        assertEquals(FrostShield.Blow.RANGED, FrostShield.classify(false, false, 1), "a spell or an arrow, however close");
    }
}
