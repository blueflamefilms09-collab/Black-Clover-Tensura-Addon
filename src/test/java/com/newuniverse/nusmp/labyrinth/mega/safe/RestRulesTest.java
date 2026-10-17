package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the rest rules are a balance decision ("secure a room, then rest") and a balance decision is only a decision when it is pinned as
 * numbers: every refusal, in the order it wins, the cool-down edge, the interruptions, the durations, what each rest gives and the ambush
 * chance. The defaults are asserted by value so a careless edit of {@link RestRules} (which is also the config default) fails here.
 */
class RestRulesTest {
    private static final double R = RestRules.DEFAULT_HOSTILE_RADIUS;
    private static final int COOLDOWN = RestRules.DEFAULT_LONG_COOLDOWN_TICKS;

    /** A start request that is fine in every respect: enabled, survival, idle, in a safe zone, next to the anchor, no hostile, 3 short rests, never long-rested. */
    private static RestRules.Start ok() {
        return new RestRules.Start(true, true, false, true, 1.0, Double.POSITIVE_INFINITY, 3, 100_000L, -1L);
    }

    private static RestRules.Start with(boolean enabled, boolean survival, boolean resting, boolean zone, double dist, double hostile, int left, long now, long lastLong) {
        return new RestRules.Start(enabled, survival, resting, zone, dist, hostile, left, now, lastLong);
    }

    @Test void theDefaultsAreTheBriefsNumbers() {
        assertEquals(10, RestRules.DEFAULT_SHORT_SECONDS);
        assertEquals(30, RestRules.DEFAULT_LONG_SECONDS);
        assertEquals(3, RestRules.DEFAULT_SHORT_USES);
        assertEquals(0.4, RestRules.DEFAULT_SHORT_HEALTH, 1e-9);
        assertEquals(0.4, RestRules.DEFAULT_SHORT_MANA, 1e-9);
        assertEquals(6000, RestRules.DEFAULT_LONG_COOLDOWN_TICKS);
        assertEquals(16.0, RestRules.DEFAULT_HOSTILE_RADIUS, 1e-9);
        assertEquals(1.5, RestRules.MOVE_LIMIT, 1e-9);
        assertEquals(0.05, RestRules.DEFAULT_AMBUSH_PER_THREAT, 1e-9);
        assertEquals(0.5, RestRules.DEFAULT_AMBUSH_CAP, 1e-9);
    }

    @Test void seatsAndHearthsGiveShortRestsBedsGiveLongOnes() {
        assertEquals(RestKind.SHORT, RestRules.kindOf(MarkerKind.SEAT));
        assertEquals(RestKind.SHORT, RestRules.kindOf(MarkerKind.HEARTH));
        assertEquals(RestKind.LONG, RestRules.kindOf(MarkerKind.BED));
        assertNull(RestRules.kindOf(MarkerKind.FURNITURE), "a table is a compat hint, not an anchor");
        assertNull(RestRules.kindOf(MarkerKind.SPAWN));
        assertNull(RestRules.kindOf(MarkerKind.PATROL));
        assertNull(RestRules.kindOf(MarkerKind.ARRIVAL));
        assertNull(RestRules.kindOf(null));
        assertTrue(RestRules.isAnchor(MarkerKind.BED));
        assertFalse(RestRules.isAnchor(MarkerKind.PEDESTAL));
    }

    @Test void aCleanRequestStartsBothKinds() {
        assertEquals(RestRules.Refusal.NONE, RestRules.checkStart(RestKind.SHORT, ok(), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NONE, RestRules.checkStart(RestKind.LONG, ok(), R, COOLDOWN));
    }

    @Test void everyRefusalIsReported() {
        assertEquals(RestRules.Refusal.DISABLED, RestRules.checkStart(RestKind.SHORT, with(false, true, false, true, 1, 99, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NOT_SURVIVAL, RestRules.checkStart(RestKind.SHORT, with(true, false, false, true, 1, 99, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.BUSY, RestRules.checkStart(RestKind.SHORT, with(true, true, true, true, 1, 99, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NOT_SAFE_ZONE, RestRules.checkStart(RestKind.LONG, with(true, true, false, false, 1, 99, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.TOO_FAR, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 3.6, 99, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.HOSTILE_NEAR, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 1, 12, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NO_SHORT_RESTS, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 1, 99, 0, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.LONG_COOLDOWN, RestRules.checkStart(RestKind.LONG, with(true, true, false, true, 1, 99, 3, 1000, 500), R, COOLDOWN));
    }

    @Test void theFirstFailureWins() {
        // disabled beats everything, a safe zone beats distance, distance beats hostiles, hostiles beat the resource checks
        assertEquals(RestRules.Refusal.DISABLED, RestRules.checkStart(RestKind.SHORT, with(false, false, true, false, 99, 1, 0, 0, 0), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NOT_SAFE_ZONE, RestRules.checkStart(RestKind.SHORT, with(true, true, false, false, 99, 1, 0, 0, 0), R, COOLDOWN));
        assertEquals(RestRules.Refusal.TOO_FAR, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 99, 1, 0, 0, 0), R, COOLDOWN));
        assertEquals(RestRules.Refusal.HOSTILE_NEAR, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 1, 1, 0, 0, 0), R, COOLDOWN));
    }

    @Test void aHostileExactlyAtTheRadiusStillBlocks() {
        assertEquals(RestRules.Refusal.HOSTILE_NEAR, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 1, 16.0, 3, 0, -1), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NONE, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 1, 16.01, 3, 0, -1), R, COOLDOWN));
    }

    @Test void shortRestsDoNotCareAboutTheLongCooldownAndViceVersa() {
        assertEquals(RestRules.Refusal.NONE, RestRules.checkStart(RestKind.SHORT, with(true, true, false, true, 1, 99, 1, 1000, 999), R, COOLDOWN));
        assertEquals(RestRules.Refusal.NONE, RestRules.checkStart(RestKind.LONG, with(true, true, false, true, 1, 99, 0, 100_000, 1), R, COOLDOWN));
    }

    @Test void theLongCooldownEndsExactlyOnTime() {
        assertEquals(6000L, RestRules.longCooldownRemaining(1000, 1000, COOLDOWN));
        assertEquals(1L, RestRules.longCooldownRemaining(6999, 1000, COOLDOWN));
        assertEquals(0L, RestRules.longCooldownRemaining(7000, 1000, COOLDOWN));
        assertEquals(0L, RestRules.longCooldownRemaining(9000, 1000, COOLDOWN));
        assertEquals(0L, RestRules.longCooldownRemaining(10, -1, COOLDOWN), "never rested: no wait");
        assertEquals(0L, RestRules.longCooldownRemaining(10, 5, 0), "a zero cooldown is no cooldown");
    }

    @Test void aRestInProgressBreaksInTheDocumentedOrder() {
        assertEquals(RestRules.Interrupt.NONE, RestRules.checkChannel(new RestRules.Sample(true, true, 0.4, false, 99), R));
        assertEquals(RestRules.Interrupt.STATE, RestRules.checkChannel(new RestRules.Sample(false, false, 9, true, 1), R));
        assertEquals(RestRules.Interrupt.DAMAGED, RestRules.checkChannel(new RestRules.Sample(true, false, 9, true, 1), R));
        assertEquals(RestRules.Interrupt.MOVED, RestRules.checkChannel(new RestRules.Sample(true, false, 1.6, false, 1), R));
        assertEquals(RestRules.Interrupt.LEFT_ZONE, RestRules.checkChannel(new RestRules.Sample(true, false, 0.2, false, 1), R));
        assertEquals(RestRules.Interrupt.HOSTILE, RestRules.checkChannel(new RestRules.Sample(true, true, 0.2, false, 10), R));
    }

    @Test void walkingExactlyOneAndAHalfBlocksIsStillResting() {
        assertEquals(RestRules.Interrupt.NONE, RestRules.checkChannel(new RestRules.Sample(true, true, 1.5, false, 99), R));
        assertEquals(RestRules.Interrupt.MOVED, RestRules.checkChannel(new RestRules.Sample(true, true, 1.51, false, 99), R));
    }

    @Test void durationsAreTheConfiguredSecondsInTicksAndNeverZero() {
        assertEquals(200, RestRules.ticksFor(RestKind.SHORT, 10, 30));
        assertEquals(600, RestRules.ticksFor(RestKind.LONG, 10, 30));
        assertEquals(20, RestRules.ticksFor(RestKind.SHORT, 0, 30), "a bad config cannot make a rest instant");
        assertEquals(20, RestRules.ticksFor(RestKind.LONG, 10, -5));
    }

    @Test void progressIsWholePercentClampedToTheRange() {
        assertEquals(0, RestRules.progressPercent(0, 200));
        assertEquals(40, RestRules.progressPercent(80, 200));
        assertEquals(50, RestRules.progressPercent(300, 600));
        assertEquals(99, RestRules.progressPercent(199, 200));
        assertEquals(100, RestRules.progressPercent(200, 200));
        assertEquals(100, RestRules.progressPercent(9999, 200));
        assertEquals(0, RestRules.progressPercent(-5, 200));
        assertEquals(100, RestRules.progressPercent(1, 0));
        assertFalse(RestRules.finished(199, 200));
        assertTrue(RestRules.finished(200, 200));
        assertTrue(RestRules.finished(250, 200));
    }

    @Test void aLongRestGivesEverythingAndAShortRestTheConfiguredShare() {
        assertEquals(1.0, RestRules.healthShare(RestKind.LONG, 0.4), 1e-9);
        assertEquals(0.4, RestRules.healthShare(RestKind.SHORT, 0.4), 1e-9);
        assertEquals(1.0, RestRules.manaShare(RestKind.LONG, 0.0), 1e-9);
        assertEquals(0.0, RestRules.manaShare(RestKind.SHORT, -1.0), 1e-9);
        assertEquals(1.0, RestRules.manaShare(RestKind.SHORT, 7.0), 1e-9, "shares are clamped to 0..1");
        assertEquals(0.0, RestRules.healthShare(RestKind.SHORT, Double.NaN), 1e-9);
    }

    @Test void healingIsCappedByTheMissingHealth() {
        assertEquals(8f, RestRules.healAmount(1f, 20f, 0.4), 1e-4f, "40% of 20 hearts-halves");
        assertEquals(3f, RestRules.healAmount(17f, 20f, 0.4), 1e-4f, "only the missing 3 are given");
        assertEquals(0f, RestRules.healAmount(20f, 20f, 1.0), 1e-4f);
        assertEquals(19f, RestRules.healAmount(1f, 20f, 1.0), 1e-4f, "a long rest tops up");
        assertEquals(0f, RestRules.healAmount(25f, 20f, 1.0), 1e-4f, "never negative");
    }

    @Test void shortRestsAreSpentOneByOneAndNeverGoNegative() {
        assertEquals(3, RestRules.shortRestsLeft(3, 0));
        assertEquals(1, RestRules.shortRestsLeft(3, 2));
        assertEquals(0, RestRules.shortRestsLeft(3, 3));
        assertEquals(0, RestRules.shortRestsLeft(3, 99));
        assertEquals(3, RestRules.shortRestsLeft(3, -4));
    }

    @Test void theAmbushChanceGrowsWithThreatAndIsCapped() {
        assertEquals(0.05, RestRules.ambushChance(1, 0.05, 0.5), 1e-9);
        assertEquals(0.25, RestRules.ambushChance(5, 0.05, 0.5), 1e-9);
        assertEquals(0.5, RestRules.ambushChance(10, 0.05, 0.5), 1e-9);
        assertEquals(0.5, RestRules.ambushChance(400, 0.05, 0.5), 1e-9);
        assertEquals(0.0, RestRules.ambushChance(0, 0.05, 0.5), 1e-9);
        assertEquals(0.0, RestRules.ambushChance(3, 0.0, 0.5), 1e-9);
        assertEquals(0.0, RestRules.ambushChance(-3, 0.05, 0.5), 1e-9);
    }

    @Test void theAmbushRollIsStrictlyBelowTheChance() {
        assertTrue(RestRules.rollAmbush(0.25, 0.0));
        assertTrue(RestRules.rollAmbush(0.25, 0.2499));
        assertFalse(RestRules.rollAmbush(0.25, 0.25));
        assertFalse(RestRules.rollAmbush(0.0, 0.0), "a zero chance never rolls");
    }

    @Test void theAmbushArrivesBetweenAQuarterAndThreeQuartersOfTheRest() {
        assertEquals(150L, RestRules.ambushDelay(600, 0.0));
        assertEquals(300L, RestRules.ambushDelay(600, 0.5));
        assertEquals(450L, RestRules.ambushDelay(600, 1.0));
        assertTrue(RestRules.ambushDelay(600, -3.0) >= 150L);
        assertTrue(RestRules.ambushDelay(600, 9.0) <= 450L);
    }

    @Test void restKindIdsRoundTrip() {
        assertEquals("short", RestKind.SHORT.id());
        assertEquals("long", RestKind.LONG.id());
        assertEquals(RestKind.LONG, RestKind.byId(" LONG "));
        assertNull(RestKind.byId("nap"));
        assertNull(RestKind.byId(null));
    }
}
