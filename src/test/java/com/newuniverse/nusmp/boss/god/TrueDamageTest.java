package com.newuniverse.nusmp.boss.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The percent-of-maximum true damage: tiers, critical, the 40% single-hit cap, and the pool drain. */
class TrueDamageTest {
    @Test
    void tiersGrowFromThreeToTwelvePercent() {
        assertEquals(3.0, TrueDamage.Tier.CHIP.percent);
        assertEquals(12.0, TrueDamage.Tier.FINISHER.percent);
        double last = 0;
        for (TrueDamage.Tier t : TrueDamage.Tier.values()) {
            assertTrue(t.percent > last, t.name());
            last = t.percent;
        }
    }

    @Test
    void theOldBaseNumbersLandInTheRightTiers() {
        assertEquals(TrueDamage.Tier.CHIP, TrueDamage.tierOfBase(5f), "chrono bolt");
        assertEquals(TrueDamage.Tier.CHIP, TrueDamage.tierOfBase(6.5f), "soul mote");
        assertEquals(TrueDamage.Tier.LIGHT, TrueDamage.tierOfBase(9f), "gravity pin");
        assertEquals(TrueDamage.Tier.MEDIUM, TrueDamage.tierOfBase(12f), "spatial strike");
        assertEquals(TrueDamage.Tier.MEDIUM, TrueDamage.tierOfBase(16f));
        assertEquals(TrueDamage.Tier.HEAVY, TrueDamage.tierOfBase(20f), "light step");
        assertEquals(TrueDamage.Tier.HEAVY, TrueDamage.tierOfBase(22f));
        assertEquals(TrueDamage.Tier.FINISHER, TrueDamage.tierOfBase(26f), "soul touch");
        assertEquals(TrueDamage.Tier.CHIP, TrueDamage.tierOfBase(0f));
        assertEquals(TrueDamage.Tier.CHIP, TrueDamage.tierOfBase(Float.NaN));
    }

    @Test
    void theAmountIsAShareOfTheVictimsMaximumHealth() {
        assertEquals(6f, TrueDamage.amount(TrueDamage.Tier.CHIP, 200f, 1.0, false), 1e-4f);
        assertEquals(24f, TrueDamage.amount(TrueDamage.Tier.FINISHER, 200f, 1.0, false), 1e-4f);
        assertEquals(240f, TrueDamage.amount(TrueDamage.Tier.FINISHER, 2000f, 1.0, false), 1e-3f, "armour and EP do not enter: only the maximum");
    }

    @Test
    void aGuaranteedCriticalIsOneAndAHalfTimes() {
        float plain = TrueDamage.amount(TrueDamage.Tier.MEDIUM, 100f, 1.0, false);
        float crit = TrueDamage.amount(TrueDamage.Tier.MEDIUM, 100f, 1.0, true);
        assertEquals(plain * 1.5f, crit, 1e-4f);
    }

    @Test
    void noHitEverTakesMoreThanFortyPercentOfAFullHealthPlayer() {
        for (TrueDamage.Tier t : TrueDamage.Tier.values())
            for (double scale : new double[]{1.0, 2.0, 10.0, 100.0})
                assertTrue(TrueDamage.amount(t, 100f, scale, true) <= 40f + 1e-4f, t + " x" + scale);
        assertEquals(40f, TrueDamage.amount(TrueDamage.Tier.FINISHER, 100f, 100.0, true), 1e-4f);
    }

    @Test
    void brokenInputsGiveNothing() {
        assertEquals(0f, TrueDamage.amount(TrueDamage.Tier.HEAVY, 0f, 1.0, false));
        assertEquals(0f, TrueDamage.amount(TrueDamage.Tier.HEAVY, 100f, 0.0, false));
        assertEquals(0f, TrueDamage.amount(null, 100f, 1.0, false));
        assertEquals(0f, TrueDamage.amount(TrueDamage.Tier.HEAVY, -5f, 1.0, false));
    }

    @Test
    void thePoolDrainFollowsThePercentAndTheSameCap() {
        assertEquals(0.09, TrueDamage.poolFraction(TrueDamage.Tier.HEAVY, 1.0), 1e-9);
        assertEquals(0.045, TrueDamage.poolFraction(TrueDamage.Tier.HEAVY, 0.5), 1e-9);
        assertEquals(0.40, TrueDamage.poolFraction(TrueDamage.Tier.FINISHER, 50.0), 1e-9);
        assertEquals(0.0, TrueDamage.poolFraction(TrueDamage.Tier.HEAVY, 0.0));
        assertEquals(0.0, TrueDamage.poolFraction(null, 1.0));
    }
}
