package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the numbers of Ki Barrier, Spirit Bomb, Instant Transmission and Meteor Combination are plain arithmetic, so their bounds are pinned here with no game. */
class SaiyanTechRulesTest {
    @Test
    void theBarrierSoaksAPoolAndLetsTheRestThrough() {
        assertEquals(60.0, SaiyanTechRules.barrierPool(60.0, 0.0), 1e-9);
        assertEquals(180.0, SaiyanTechRules.barrierPool(60.0, 1.0), 1e-9, "three times the pool at the top");
        assertEquals(180.0, SaiyanTechRules.barrierPool(60.0, 5.0), 1e-9, "growth is clamped");
        assertEquals(0.0, SaiyanTechRules.throughBarrier(50.0, 20.0), 1e-9);
        assertEquals(30.0, SaiyanTechRules.throughBarrier(50.0, 80.0), 1e-9);
        assertEquals(30.0, SaiyanTechRules.poolAfter(50.0, 20.0), 1e-9);
        assertEquals(0.0, SaiyanTechRules.poolAfter(50.0, 80.0), 1e-9);
        assertEquals(0.0, SaiyanTechRules.throughBarrier(-5.0, 10.0) - 10.0, 1e-9, "a negative pool soaks nothing");
    }

    @Test
    void theBarrierEndsWithItsTimeOrItsPool() {
        assertTrue(SaiyanTechRules.barrierUp(1.0, 99, 100));
        assertFalse(SaiyanTechRules.barrierUp(1.0, 100, 100));
        assertFalse(SaiyanTechRules.barrierUp(0.0, 0, 100));
    }

    @Test
    void theSpiritBombIsFedByAtMostTwentyFourFriendsAndGathersAtMostTwiceAndAHalfAsFast() {
        assertEquals(24, SaiyanTechRules.sources(1000));
        assertEquals(0, SaiyanTechRules.sources(-3));
        assertEquals(1.0, SaiyanTechRules.gatherRate(0), 1e-9);
        assertTrue(SaiyanTechRules.gatherRate(5) > SaiyanTechRules.gatherRate(1));
        assertEquals(2.5, SaiyanTechRules.gatherRate(1000), 1e-9);
        assertEquals(0.5, SaiyanTechRules.charge(80, 8.0, 1.0), 1e-9);
        assertEquals(1.0, SaiyanTechRules.charge(80, 8.0, 2.5), 1e-9, "a crowd fills it sooner, never past 1");
        assertEquals(0.0, SaiyanTechRules.charge(-5, 8.0, 1.0), 1e-9);
    }

    @Test
    void theSpiritBombGrowsWithChargeAndFriendsButIsBounded() {
        double lone = SaiyanTechRules.bombDamage(100.0, 1.0, 0), fed = SaiyanTechRules.bombDamage(100.0, 1.0, 24);
        assertEquals(100.0, lone, 1e-9);
        assertEquals(148.0, fed, 1e-9, "+2% per friend, 24 friends at most");
        assertEquals(148.0, SaiyanTechRules.bombDamage(100.0, 1.0, 999), 1e-9);
        assertEquals(30.0, SaiyanTechRules.bombDamage(100.0, 0.0, 0), 1e-9);
        assertEquals(8.0, SaiyanTechRules.bombRadius(8.0, 1.0), 1e-9);
        assertEquals(3.2, SaiyanTechRules.bombRadius(8.0, 0.0), 1e-9);
        assertEquals(2.0, SaiyanTechRules.bombRadius(2.0, 0.0), 1e-9, "never under two blocks");
    }

    @Test
    void aThrownBombTravelsBetweenTenTicksAndItsLimit() {
        assertEquals(10, SaiyanTechRules.travelTicks(0.0, 60));
        assertEquals(40, SaiyanTechRules.travelTicks(48.0, 60));
        assertEquals(60, SaiyanTechRules.travelTicks(1.0e6, 60));
        assertEquals(10, SaiyanTechRules.travelTicks(1.0e6, 3), "a silly limit still leaves ten ticks");
    }

    @Test
    void theBlinkAndTheLandingStayInsideTheirRanges() {
        assertEquals(24.0, SaiyanTechRules.blinkRange(64.0, 24.0), 1e-9);
        assertEquals(10.0, SaiyanTechRules.blinkRange(10.0, 24.0), 1e-9);
        assertEquals(2.0, SaiyanTechRules.blinkRange(0.5, 24.0), 1e-9);
        assertEquals(8.0, SaiyanTechRules.landingDistance(10.0, 2.0), 1e-9);
        assertEquals(0.0, SaiyanTechRules.landingDistance(1.0, 2.0), 1e-9);
    }

    @Test
    void theMeteorCombinationHasAtMostSixteenHitsAndAHeavyLastOne() {
        assertEquals(16, SaiyanTechRules.meteorHits(1000));
        assertEquals(1, SaiyanTechRules.meteorHits(0));
        assertEquals(8, SaiyanTechRules.meteorHits(8));
        for (int i = 0; i < 7; i++) assertEquals(1.0, SaiyanTechRules.meteorHitFactor(i, 8), 1e-9);
        assertEquals(3.0, SaiyanTechRules.meteorHitFactor(7, 8), 1e-9);
        assertEquals(3.0, SaiyanTechRules.meteorHitFactor(0, 1), 1e-9, "a one-hit rush is the heavy hit");
        assertTrue(SaiyanTechRules.inReach(3.9, 4.0));
        assertFalse(SaiyanTechRules.inReach(4.1, 4.0));
    }
}
