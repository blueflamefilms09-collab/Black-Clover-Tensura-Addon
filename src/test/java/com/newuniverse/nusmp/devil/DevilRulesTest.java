package com.newuniverse.nusmp.devil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every number of the Devil Contract system (3.48.0) is in {@link DevilRules} so it can be pinned without a world: the union lasts exactly five minutes and then locks for 24 in-game hours (24 000 ticks),
 * the level can not reach 100 % while that lock runs, a stronger tier climbs faster and drains more, the cost at 100 % is ten times the cost at 0 %, the stats follow a smooth curve and can not pass their hard caps
 * with any setting, and a hostile or damaged value (NaN, out of range) can not get through.
 */
class DevilRulesTest {
    @Test
    void theUnionIsExactlyFiveMinutesAndTheCooldownTwentyFourInGameHours() {
        assertEquals(6000L, DevilRules.unionTicks(DevilRules.DEFAULT_UNION_SECONDS));
        assertEquals(24000L, DevilRules.cooldownTicks(DevilRules.DEFAULT_COOLDOWN_HOURS));
        assertEquals(20 * 20L, DevilRules.unionTicks(1), "never shorter than twenty seconds");
        assertEquals(36000L * 20L, DevilRules.unionTicks(Integer.MAX_VALUE), "never longer than ten hours");
        assertEquals(0L, DevilRules.cooldownTicks(-5));
        assertEquals(240_000L, DevilRules.cooldownTicks(100000));
        assertEquals(1000L + 6000L, DevilRules.unionEnd(1000L, 6000L));
        assertEquals(500L + 24000L, DevilRules.readyAfter(500L, 24000L));
        assertTrue(DevilRules.unionDue(100L, 100L) && !DevilRules.unionDue(100L, 99L) && !DevilRules.unionDue(0L, 5000L), "due exactly at the end, never when there is no union");
        assertTrue(DevilRules.cooling(101L, 100L) && !DevilRules.cooling(100L, 100L));
    }

    @Test
    void secondsLeftRoundsUpAndNeverGoesNegative() {
        assertEquals(0, DevilRules.secondsLeft(100L, 100L));
        assertEquals(0, DevilRules.secondsLeft(50L, 100L));
        assertEquals(1, DevilRules.secondsLeft(101L, 100L));
        assertEquals(1, DevilRules.secondsLeft(120L, 100L));
        assertEquals(2, DevilRules.secondsLeft(121L, 100L));
        assertEquals(300, DevilRules.secondsLeft(6000L, 0L));
    }

    @Test
    void theLevelClimbsWithTheTierFallsWhenOffAndStopsAtTheCap() {
        assertEquals(100, secondsToFull(DevilTier.LOW), "a Low devil needs a hundred seconds");
        assertEquals(40, secondsToFull(DevilTier.SUPREME), "a Supreme one forty");
        assertTrue(DevilRules.climb(DevilTier.LOW) < DevilRules.climb(DevilTier.MID) && DevilRules.climb(DevilTier.MID) < DevilRules.climb(DevilTier.HIGH) && DevilRules.climb(DevilTier.HIGH) < DevilRules.climb(DevilTier.SUPREME));
        assertEquals(0f, DevilRules.climb(DevilTier.NONE));
        assertEquals(50f - DevilRules.DECAY, DevilRules.step(50f, false, DevilTier.HIGH, 100f), 1e-6);
        assertEquals(0f, DevilRules.step(1f, false, DevilTier.HIGH, 100f), "never below zero");
        assertEquals(99f, DevilRules.step(98.5f, true, DevilTier.SUPREME, DevilRules.cap(true)), "while the cooldown runs the level stops at 99");
        assertEquals(99f, DevilRules.step(100f, true, DevilTier.LOW, DevilRules.cap(true)), "a level above the cap is pulled down to it");
        assertEquals(100f, DevilRules.step(99.5f, true, DevilTier.LOW, DevilRules.cap(false)));
    }

    private static int secondsToFull(DevilTier t) {
        float level = 0f;
        int s = 0;
        while (level < 100f && s < 10000) {
            level = DevilRules.step(level, true, t, 100f);
            s++;
        }
        return s;
    }

    @Test
    void theCostAtFullIsTenTimesTheCostAtZeroAndTheUnionPaysAQuarterOfIt() {
        for (DevilTier t : new DevilTier[]{DevilTier.LOW, DevilTier.MID, DevilTier.HIGH, DevilTier.SUPREME}) {
            assertEquals(10.0, DevilRules.drain(t, 100f) / DevilRules.drain(t, 0f), 1e-9);
            assertEquals(DevilRules.drain(t, 100f) * 0.25, DevilRules.unionDrain(t), 1e-9);
            assertTrue(DevilRules.drain(t, 50f) > DevilRules.drain(t, 20f), "rising with the level");
        }
        assertTrue(DevilRules.baseDrain(DevilTier.LOW) < DevilRules.baseDrain(DevilTier.SUPREME));
        assertEquals(0.0, DevilRules.drain(DevilTier.NONE, 100f));
        assertEquals(DevilRules.drain(DevilTier.MID, 100f), DevilRules.drain(DevilTier.MID, 500f), 1e-9, "the level is clamped");
    }

    @Test
    void theBodyShowsHornsAtTwentyWingsAtFiftyAndTheUnionAtOneHundred() {
        assertEquals(0, DevilRules.stage(19.9f));
        assertEquals(1, DevilRules.stage(20f));
        assertEquals(1, DevilRules.stage(49.9f));
        assertEquals(2, DevilRules.stage(50f));
        assertEquals(2, DevilRules.stage(99.9f));
        assertEquals(3, DevilRules.stage(100f));
        assertFalse(DevilRules.flight(49.9f));
        assertTrue(DevilRules.flight(50f));
        assertEquals(0, DevilRules.bucket(4.9f));
        assertEquals(1, DevilRules.bucket(5f));
        assertEquals(20, DevilRules.bucket(100f));
        assertEquals(0, DevilRules.bucket(Float.NaN));
    }

    @Test
    void statsFollowASmoothCurveAndNoSettingCanPassTheHardCaps() {
        assertEquals(0.0, DevilRules.smooth(0), 1e-12);
        assertEquals(1.0, DevilRules.smooth(1), 1e-12);
        assertEquals(0.5, DevilRules.smooth(0.5), 1e-12);
        assertTrue(DevilRules.smooth(0.2) < 0.2 && DevilRules.smooth(0.8) > 0.8, "slow at the start, heavy at the end");
        double prev = -1;
        for (int l = 0; l <= 100; l += 5) {
            double v = DevilRules.strength(DevilTier.HIGH, l, false, 1.0);
            assertTrue(v >= prev, "never goes down as the level rises");
            prev = v;
        }
        assertEquals(DevilRules.tierStrength(DevilTier.HIGH), DevilRules.strength(DevilTier.HIGH, 100f, false, 1.0), 1e-9);
        assertEquals(DevilRules.tierStrength(DevilTier.LOW) * 1.5, DevilRules.strength(DevilTier.LOW, 100f, true, 1.0), 1e-9, "the union is a bonus of half again");
        for (DevilTier t : new DevilTier[]{DevilTier.LOW, DevilTier.MID, DevilTier.HIGH, DevilTier.SUPREME}) {
            assertTrue(DevilRules.strength(t, 100f, true, 2.0) <= DevilRules.MAX_STRENGTH, "strength cap, even in the union at double scale");
            assertTrue(DevilRules.speed(t, 100f, true, 2.0) <= DevilRules.MAX_SPEED, "speed cap, even in the union at double scale");
        }
        assertEquals(0.0, DevilRules.strength(DevilTier.SUPREME, 100f, true, 0.0), "scale zero adds nothing");
        assertEquals(0.0, DevilRules.speed(DevilTier.SUPREME, Float.NaN, true, 1.0), "a NaN level adds nothing");
    }

    @Test
    void thePriceDecidesTheTierAndTheNewPricesAreTheOnlyOnesThatAddASacrifice() {
        assertEquals(DevilTier.LOW, DevilRules.tierForPrice("tax"));
        assertEquals(DevilTier.MID, DevilRules.tierForPrice("seal"));
        assertEquals(DevilTier.MID, DevilRules.tierForPrice("maxhp"));
        assertEquals(DevilTier.HIGH, DevilRules.tierForPrice("eye_left"));
        assertEquals(DevilTier.HIGH, DevilRules.tierForPrice("eye_right"));
        assertEquals(DevilTier.SUPREME, DevilRules.tierForPrice("half"));
        assertEquals(DevilTier.LOW, DevilRules.tierForPrice(null));
        assertNull(DevilRules.sacrificeForPrice("tax"));
        assertNull(DevilRules.sacrificeForPrice("seal"));
        assertNull(DevilRules.sacrificeForPrice("maxhp"));
        assertEquals(Sacrifice.HALF_HEALTH, DevilRules.sacrificeForPrice("half"));
        assertEquals(Sacrifice.LOST_EYE_LEFT, DevilRules.sacrificeForPrice("eye_left"));
        assertEquals(Sacrifice.LOST_EYE_RIGHT, DevilRules.sacrificeForPrice("eye_right"));
        assertEquals(-0.5, DevilRules.HALF_HEALTH_MULT);
    }

    @Test
    void aLostEyeNarrowsTheViewAndTheReachButOnlyAsFarAsThePlayerAllows() {
        assertEquals(1.0, DevilRules.fovFactor(0, 1.0), 1e-12);
        assertTrue(DevilRules.fovFactor(1, 1.0) < 1.0 && DevilRules.fovFactor(2, 1.0) < DevilRules.fovFactor(1, 1.0));
        assertEquals(1.0, DevilRules.fovFactor(2, 0.0), 1e-12, "strength 0 = no narrowing");
        assertTrue(DevilRules.fovFactor(2, 5.0) >= DevilRules.fovFactor(2, 1.0), "the strength is clamped to one");
        assertTrue(DevilRules.fovFactor(99, 1.0) >= 0.7, "never a tunnel");
        assertEquals(0.0, DevilRules.eyeReachPenalty(0));
        assertTrue(DevilRules.eyeReachPenalty(1) < 0 && DevilRules.eyeReachPenalty(2) < DevilRules.eyeReachPenalty(1));
        assertTrue(DevilRules.eyeReachPenalty(2) >= -1.0, "a lost eye never costs more than a block");
    }

    @Test
    void asksAreRateLimitedAndAClockThatWentBackwardsIsNotStuck() {
        assertFalse(DevilRules.askAllowed(100L, 100L + DevilRules.MIN_ASK_GAP - 1));
        assertTrue(DevilRules.askAllowed(100L, 100L + DevilRules.MIN_ASK_GAP));
        assertTrue(DevilRules.askAllowed(1000L, 5L), "a restarted world clock must not lock the key");
        assertTrue(DevilRules.askAllowed(Long.MIN_VALUE / 2, 0L));
    }

    @Test
    void theHelpersClampHostileValues() {
        assertEquals(0f, DevilRules.clampF(Float.NaN, 0f, 100f));
        assertEquals(100f, DevilRules.clampF(1e9f, 0f, 100f));
        assertEquals(3, DevilRules.clampInt(3, 0, 5));
        assertEquals(5, DevilRules.clampInt(99, 0, 5));
        assertEquals(0.0, DevilRules.clampD(Double.NaN, 0, 1));
    }
}
