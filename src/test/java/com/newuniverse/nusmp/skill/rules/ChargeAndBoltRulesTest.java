package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Argonaut's bell charge, Hero's Argonaut and Firebolt are the DanMachi core; their curves must hold. */
class ChargeAndBoltRulesTest {
    @Test
    void oneBellChimesPerSecondUpToTheMax() {
        assertEquals(0, ChargeRules.chargeAt(0, Tier.BASE));
        assertEquals(0, ChargeRules.chargeAt(19, Tier.BASE));
        assertEquals(1, ChargeRules.chargeAt(20, Tier.BASE));
        assertEquals(5, ChargeRules.chargeAt(1000, Tier.BASE));
        assertEquals(7, ChargeRules.chargeAt(150, Tier.ULTIMATE));
        assertEquals(10, ChargeRules.chargeAt(1000, Tier.ULTIMATE));
        assertEquals(0, ChargeRules.chargeAt(-4, Tier.BASE));
    }

    @Test
    void theHeroChargesTenBellsAndHitsHarder() {
        assertEquals(5, ChargeRules.maxCharge(Tier.MASTERED));
        assertEquals(10, ChargeRules.maxCharge(Tier.ULTIMATE));
        double base = ChargeRules.damage(8, 5, Tier.MASTERED, false);
        double hero = ChargeRules.damage(8, 10, Tier.ULTIMATE, false);
        assertTrue(hero > base * 2, "ten bells of the Hero beat five of the mastered Unique by more than double");
        assertTrue(ChargeRules.damage(8, 10, Tier.ULTIMATE, true) > hero, "a mastered Ultimate adds on top");
        assertEquals(0.0, ChargeRules.damage(8, 0, Tier.ULTIMATE, true));
    }

    @Test
    void chimesRingOncePerFullCharge() {
        assertFalse(ChargeRules.chimeNow(0, Tier.BASE));
        assertFalse(ChargeRules.chimeNow(10, Tier.BASE));
        assertTrue(ChargeRules.chimeNow(20, Tier.BASE));
        assertTrue(ChargeRules.chimeNow(100, Tier.BASE));
        assertFalse(ChargeRules.chimeNow(120, Tier.BASE), "no sixth bell for the Unique");
        assertTrue(ChargeRules.chimeNow(200, Tier.ULTIMATE));
    }

    @Test
    void theBeamGrowsWithChargeAndIsCapped() {
        assertTrue(ChargeRules.beamLength(10) > ChargeRules.beamLength(1));
        assertEquals(42.0, ChargeRules.beamLength(100));
        assertEquals(12.0, ChargeRules.beamLength(-3));
        assertTrue(ChargeRules.beamRadius(10) > ChargeRules.beamRadius(1));
        assertEquals(2, ChargeRules.modes(Tier.ULTIMATE));
        assertEquals(1, ChargeRules.modes(Tier.BASE));
        assertEquals(0, ChargeRules.Strike.STRIKE.ordinal());
        assertEquals(1, ChargeRules.Strike.BEAM.ordinal());
    }

    @Test
    void masteryOnlyForAStrikeThatHit() {
        assertEquals(0, ChargeRules.masteryGain(5, 0));
        assertEquals(5, ChargeRules.masteryGain(5, 2));
        assertEquals(1, ChargeRules.masteryGain(0, 1));
        assertTrue(ChargeRules.maxHeldTicks(Tier.ULTIMATE) > ChargeRules.maxHeldTicks(Tier.BASE));
    }

    @Test
    void fireboltDoublesWhenMastered() {
        assertEquals(8.0, FireboltRules.damage(8, Tier.BASE));
        assertEquals(16.0, FireboltRules.damage(8, Tier.MASTERED));
        assertEquals(0.0, FireboltRules.damage(-3, Tier.MASTERED));
        assertTrue(FireboltRules.splashRadius(Tier.MASTERED) > FireboltRules.splashRadius(Tier.BASE));
        assertTrue(FireboltRules.range(Tier.MASTERED) > FireboltRules.range(Tier.BASE));
        assertEquals(0, FireboltRules.masteryGain(false));
        assertEquals(1, FireboltRules.masteryGain(true));
    }
}
