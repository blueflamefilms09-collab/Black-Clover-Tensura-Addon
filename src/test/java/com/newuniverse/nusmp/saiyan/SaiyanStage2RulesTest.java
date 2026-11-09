package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Stage 2 numbers (training, Ki Flight, Saiyan Senses, the combat passives) are plain arithmetic, so they are pinned here with no game. */
class SaiyanStage2RulesTest {
    // ------------------------------------------------------------------ flight
    @Test
    void flightHasFiveTiersEarnedByFlightSeconds() {
        assertEquals(5, SaiyanFlightRules.Tier.values().length);
        assertEquals(SaiyanFlightRules.Tier.NOVICE, SaiyanFlightRules.tierOf(0));
        assertEquals(SaiyanFlightRules.Tier.NOVICE, SaiyanFlightRules.tierOf(119));
        assertEquals(SaiyanFlightRules.Tier.APPRENTICE, SaiyanFlightRules.tierOf(120));
        assertEquals(SaiyanFlightRules.Tier.ADEPT, SaiyanFlightRules.tierOf(600));
        assertEquals(SaiyanFlightRules.Tier.EXPERT, SaiyanFlightRules.tierOf(1800));
        assertEquals(SaiyanFlightRules.Tier.MASTER, SaiyanFlightRules.tierOf(5400));
        assertEquals(SaiyanFlightRules.Tier.MASTER, SaiyanFlightRules.tierOf(Long.MAX_VALUE));
    }

    @Test
    void flightSpeedRisesWithTheTierAndIsCut() {
        double last = -1;
        for (SaiyanFlightRules.Tier t : SaiyanFlightRules.Tier.values()) {
            double s = SaiyanFlightRules.speed(t, 1.0);
            assertTrue(s > last, "speed must rise with the tier");
            last = s;
        }
        assertEquals(0.05, SaiyanFlightRules.speed(SaiyanFlightRules.Tier.ADEPT, 1.0), 1e-9);
        assertEquals(SaiyanFlightRules.MAX_SPEED, SaiyanFlightRules.speed(SaiyanFlightRules.Tier.MASTER, 100.0), 1e-9);
        assertEquals(0.0, SaiyanFlightRules.speed(SaiyanFlightRules.Tier.MASTER, -3.0), 1e-9);
    }

    @Test
    void onlyAMasterBurstsAndOnlyANoviceHasAHoverLimit() {
        for (SaiyanFlightRules.Tier t : SaiyanFlightRules.Tier.values()) {
            assertEquals(t == SaiyanFlightRules.Tier.MASTER, SaiyanFlightRules.canBurst(t));
            assertEquals(t == SaiyanFlightRules.Tier.NOVICE, SaiyanFlightRules.hoverLimited(t));
        }
        assertTrue(SaiyanFlightRules.hoverLeft(9, 10));
        assertFalse(SaiyanFlightRules.hoverLeft(10, 10));
    }

    @Test
    void theServerChecksHowFarAFlyingPlayerReallyWent() {
        double adept = SaiyanFlightRules.speed(SaiyanFlightRules.Tier.ADEPT, 1.0);
        assertTrue(SaiyanFlightRules.speedOk(11.0, adept, false), "ordinary flight passes");
        assertTrue(SaiyanFlightRules.speedOk(21.0, adept, false), "sprint-flying passes");
        assertFalse(SaiyanFlightRules.speedOk(80.0, adept, false), "far too fast for an Adept");
        assertTrue(SaiyanFlightRules.speedOk(80.0, adept, true), "a burst dash may");
        assertFalse(SaiyanFlightRules.speedOk(500.0, adept, true), "not even a burst covers that");
        assertFalse(SaiyanFlightRules.countsAsFlight(1.0), "hovering teaches nothing");
        assertTrue(SaiyanFlightRules.countsAsFlight(5.0));
    }

    // ------------------------------------------------------------------ senses
    @Test
    void sensesHaveTheSpecRadii() {
        assertEquals(16.0, SaiyanSenseRules.Tier.BEGINNER.radius, 1e-9);
        assertEquals(32.0, SaiyanSenseRules.Tier.INTERMEDIATE.radius, 1e-9);
        assertEquals(64.0, SaiyanSenseRules.Tier.ADVANCED.radius, 1e-9);
        assertEquals(128.0, SaiyanSenseRules.Tier.MASTER.radius, 1e-9);
        assertEquals(SaiyanSenseRules.Tier.BEGINNER, SaiyanSenseRules.tierOf(29));
        assertEquals(SaiyanSenseRules.Tier.INTERMEDIATE, SaiyanSenseRules.tierOf(30));
        assertEquals(SaiyanSenseRules.Tier.MASTER, SaiyanSenseRules.tierOf(500));
        assertEquals(40.0, SaiyanSenseRules.radius(SaiyanSenseRules.Tier.MASTER, 40.0), 1e-9);
    }

    @Test
    void stealthShortensWhereACreatureCanBeFelt() {
        assertTrue(SaiyanSenseRules.detectable(60.0, 64.0, false, false, false));
        assertFalse(SaiyanSenseRules.detectable(60.0, 64.0, true, false, false), "a suppressed Ki is felt at a quarter of the radius");
        assertTrue(SaiyanSenseRules.detectable(16.0, 64.0, true, false, false));
        assertFalse(SaiyanSenseRules.detectable(40.0, 64.0, false, false, true), "a sneaking one at half");
        assertFalse(SaiyanSenseRules.detectable(9.0, 64.0, false, true, false), "an invisible one only up close");
        assertTrue(SaiyanSenseRules.detectable(7.0, 64.0, false, true, false));
    }

    @Test
    void signatureClassesReadPowerAgainstTheSensingSaiyan() {
        assertEquals(0, SaiyanSenseRules.signature(10, 1000));
        assertEquals(1, SaiyanSenseRules.signature(500, 1000));
        assertEquals(2, SaiyanSenseRules.signature(2000, 1000));
        assertEquals(3, SaiyanSenseRules.signature(8000, 1000));
        assertEquals(4, SaiyanSenseRules.signature(50_000, 1000));
        assertEquals(0, SaiyanSenseRules.signature(Double.NaN, 1000));
        assertEquals(0, SaiyanSenseRules.signature(100, 0));
    }

    @Test
    void packedOffsetsRoundTripAndClamp() {
        assertEquals(12.5, SaiyanSenseRules.unpack(SaiyanSenseRules.pack(12.5)), 1e-9);
        assertEquals(-100.0, SaiyanSenseRules.unpack(SaiyanSenseRules.pack(-100.0)), 1e-9);
        assertEquals(Short.MAX_VALUE, SaiyanSenseRules.pack(1.0e9));
        assertEquals(Short.MIN_VALUE, SaiyanSenseRules.pack(-1.0e9));
    }

    // ------------------------------------------------------------------ combat passives
    @Test
    void reflexStartsAtThreePercentGrowsAndNeverMakesAnInvulnerableSaiyan() {
        assertEquals(0.03, SaiyanCombatRules.reflexChance(0.03, 0.15, 100), 1e-9);
        assertEquals(0.15, SaiyanCombatRules.reflexChance(0.03, 0.15, 10_000_000), 1e-9);
        assertTrue(SaiyanCombatRules.reflexChance(0.03, 0.15, 10_000) > 0.03);
        assertEquals(SaiyanCombatRules.HARD_DODGE_CAP, SaiyanCombatRules.reflexChance(5.0, 5.0, 1.0e12), 1e-9);
        assertEquals(SaiyanCombatRules.HARD_DODGE_CAP, SaiyanCombatRules.capped(0.7, 0.5), 1e-9);
        assertFalse(SaiyanCombatRules.dodged(0.99, 5.0), "even a huge setting never dodges everything");
        assertTrue(SaiyanCombatRules.dodged(0.01, 0.03));
        assertFalse(SaiyanCombatRules.dodged(0.05, 0.03));
    }

    @Test
    void dodgesHaveABudgetPerWindow() {
        assertTrue(SaiyanCombatRules.budgetOk(2, 100, 150, 400, 3));
        assertFalse(SaiyanCombatRules.budgetOk(3, 100, 150, 400, 3));
        assertTrue(SaiyanCombatRules.budgetOk(3, 100, 600, 400, 3), "a new window");
    }

    @Test
    void flowBuildsInsideTheWindowAndFades() {
        assertEquals(1, SaiyanCombatRules.flowAfter(0, 1000, -5000, 120, 5));
        assertEquals(2, SaiyanCombatRules.flowAfter(1, 1050, 1000, 120, 5));
        assertEquals(5, SaiyanCombatRules.flowAfter(5, 1050, 1000, 120, 5), "capped");
        assertEquals(1, SaiyanCombatRules.flowAfter(4, 2000, 1000, 120, 5), "a late one starts over");
        assertEquals(0, SaiyanCombatRules.flowNow(3, 2000, 1000, 120));
        assertEquals(3, SaiyanCombatRules.flowNow(3, 1050, 1000, 120));
    }

    @Test
    void adaptationClimbsToFifteenPercentAndIsOverAfterTheFight() {
        assertEquals(0.0, SaiyanCombatRules.adaptation(0, 0.15, 10), 1e-9);
        assertEquals(0.075, SaiyanCombatRules.adaptation(5, 0.15, 10), 1e-9);
        assertEquals(0.15, SaiyanCombatRules.adaptation(10, 0.15, 10), 1e-9);
        assertEquals(0.15, SaiyanCombatRules.adaptation(1000, 0.15, 10), 1e-9);
        assertEquals(SaiyanCombatRules.HARD_ADAPT_CAP, SaiyanCombatRules.adaptation(10, 9.0, 10), 1e-9);
        assertFalse(SaiyanCombatRules.adaptationOver(1500, 1000, 600));
        assertTrue(SaiyanCombatRules.adaptationOver(1700, 1000, 600));
    }

    @Test
    void zenkaiNeedsANearDeathARecoveryAndADangerousFoe() {
        assertTrue(SaiyanCombatRules.nearDeath(3.0, 20.0, 0.2));
        assertFalse(SaiyanCombatRules.nearDeath(0.0, 20.0, 0.2), "dead is not a near-death");
        assertFalse(SaiyanCombatRules.nearDeath(10.0, 20.0, 0.2));
        assertTrue(SaiyanCombatRules.recovered(14.0, 20.0, 0.6));
        assertFalse(SaiyanCombatRules.recovered(5.0, 20.0, 0.6));
        assertTrue(SaiyanCombatRules.worthy(600, 1000, 0.5));
        assertFalse(SaiyanCombatRules.worthy(100, 1000, 0.5), "a weak attacker is no dangerous fight");
    }

    @Test
    void zenkaiHasACooldownADailyCapAndDiminishingReturns() {
        assertTrue(SaiyanCombatRules.zenkaiAllowed(100_000, -1, 24000, 0, 1));
        assertFalse(SaiyanCombatRules.zenkaiAllowed(100_000, 90_000, 24000, 0, 1), "too soon after the last");
        assertFalse(SaiyanCombatRules.zenkaiAllowed(100_000, 10_000, 24000, 1, 1), "the day's limit");
        assertTrue(SaiyanCombatRules.zenkaiAllowed(100_000, 10_000, 24000, 0, 1));
        double first = SaiyanCombatRules.zenkaiGain(10_000, 0.05, 0, 0.25);
        double fifth = SaiyanCombatRules.zenkaiGain(10_000, 0.05, 4, 0.25);
        assertEquals(500.0, first, 1e-9);
        assertTrue(fifth < first, "diminishing returns");
        assertEquals(0.0, SaiyanCombatRules.zenkaiGain(-5, 0.05, 0, 0.25), 1e-9);
    }

    // ------------------------------------------------------------------ training and mastery
    @Test
    void trainingIsCappedPerDayAndMasteryIsCountedInUses() {
        assertEquals(60.0, SaiyanTraining.dailyCap(SaiyanTraining.Cat.PHYSICAL), 1e-9);
        assertEquals(3.0, SaiyanTraining.dailyCap(SaiyanTraining.Cat.DIVINE), 1e-9);
        assertEquals(10.0, SaiyanTraining.room(SaiyanTraining.Cat.PHYSICAL, 50.0, 1.0), 1e-9);
        assertEquals(0.0, SaiyanTraining.room(SaiyanTraining.Cat.PHYSICAL, 70.0, 1.0), 1e-9);
        assertEquals(SaiyanTraining.Cat.KI, SaiyanTraining.Cat.byId("ki"));
        assertEquals(null, SaiyanTraining.Cat.byId("nope"));
        assertEquals(5, SaiyanTraining.UNIVERSAL.size());
        assertFalse(SaiyanTraining.mastered("blast", 199));
        assertTrue(SaiyanTraining.mastered("blast", 200));
        assertEquals(0.5, SaiyanTraining.mastery01("barrage", 50), 1e-9);
        assertEquals(1.0, SaiyanTraining.mastery01("barrage", 5000), 1e-9);
    }
}
