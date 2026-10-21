package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the ambush director is a rule about FAIRNESS as much as about difficulty, and each half of it is a sentence of the owner's spec: it answers a bypass (a room left
 * with its wave alive, travel faster than a threshold, a teleport past), it is budgeted (a cooldown, a count per window, a crowd cap), it NEVER fires inside the Hub or a
 * safe zone, and it never touches hunger. These tests pin every refusal and its order, how heat rises and cools, that a fair player never reaches the threshold and a
 * runner does, how the band climbs with repeats and depth, and that a bad configuration cannot switch the budget off.
 */
class AmbushRulesTest {
    private static final AmbushRules.Config C = AmbushRules.Config.DEFAULT;

    private static AmbushRules.Facts facts(boolean enabled, boolean peaceful, boolean survival, boolean inMega, boolean safe, boolean locked, long cool, int fired, int live, double heat) {
        return new AmbushRules.Facts(enabled, peaceful, survival, inMega, safe, locked, cool, fired, live, heat);
    }

    private static AmbushRules.Facts free() { return facts(true, false, true, true, false, false, 0L, 0, 0, C.threshold()); }

    @Test
    void aHotFreePlayerInAnOrdinaryRoomIsAmbushed() {
        assertEquals(AmbushRules.Why.FIRE, AmbushRules.decide(free(), C));
    }

    @Test
    void everyExclusionRefusesWithItsOwnName() {
        assertEquals(AmbushRules.Why.OFF, AmbushRules.decide(facts(false, false, true, true, false, false, 0L, 0, 0, 9), C));
        assertEquals(AmbushRules.Why.PEACEFUL, AmbushRules.decide(facts(true, true, true, true, false, false, 0L, 0, 0, 9), C));
        assertEquals(AmbushRules.Why.NOT_SURVIVAL, AmbushRules.decide(facts(true, false, false, true, false, false, 0L, 0, 0, 9), C));
        assertEquals(AmbushRules.Why.WRONG_PLACE, AmbushRules.decide(facts(true, false, true, false, false, false, 0L, 0, 0, 9), C), "outside the Mega-Dungeon (the Hub is another dimension)");
        assertEquals(AmbushRules.Why.SAFE, AmbushRules.decide(facts(true, false, true, true, true, false, 0L, 0, 0, 9), C));
        assertEquals(AmbushRules.Why.LOCKED_IN, AmbushRules.decide(facts(true, false, true, true, false, true, 0L, 0, 0, 9), C));
        assertEquals(AmbushRules.Why.HEAT, AmbushRules.decide(facts(true, false, true, true, false, false, 0L, 0, 0, C.threshold() - 0.01), C));
        assertEquals(AmbushRules.Why.COOLDOWN, AmbushRules.decide(facts(true, false, true, true, false, false, 5L, 0, 0, 9), C));
        assertEquals(AmbushRules.Why.BUDGET, AmbushRules.decide(facts(true, false, true, true, false, false, 0L, C.budgetMax(), 0, 9), C));
        assertEquals(AmbushRules.Why.CROWDED, AmbushRules.decide(facts(true, false, true, true, false, false, 0L, 0, C.crowdCap(), 9), C));
    }

    @Test
    void aSafeRoomIsRefusedBeforeAnythingElseAboutThePlayerIsAsked() {
        // hot, free of cooldown, empty budget: the safe room still wins over every later gate
        assertEquals(AmbushRules.Why.SAFE, AmbushRules.decide(facts(true, false, true, true, true, true, 0L, 0, 0, 99), C));
    }

    @Test
    void aSignalIsWorthWhatTheDocSays() {
        assertEquals(3.0, AmbushRules.points(AmbushRules.Signal.LEFT_LIVE_ROOM, 0, C));
        assertEquals(5.0, AmbushRules.points(AmbushRules.Signal.LEFT_LIVE_ROOM, 4, C));
        assertEquals(1.5, AmbushRules.points(AmbushRules.Signal.FAST_TRAVEL, 9, C));
        assertEquals(4.0, AmbushRules.points(AmbushRules.Signal.TELEPORT, 0, C));
        assertEquals(5.0, AmbushRules.points(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, C));
        assertTrue(AmbushRules.points(AmbushRules.Signal.LEFT_LIVE_ROOM, -3, C) >= 0.0, "a negative count is no count");
    }

    @Test
    void heatCoolsByItselfAndNeverBelowZero() {
        assertEquals(6.0 - 0.25 * 4, AmbushRules.cooled(6.0, 80L, C), 1e-9);
        assertEquals(0.0, AmbushRules.cooled(1.0, 20L * 60L, C));
        assertEquals(5.0, AmbushRules.cooled(5.0, -100L, C), "time cannot run backwards");
    }

    @Test
    void theBandStartsAtStandardAndClimbsWithRepeatsAndDepth() {
        assertEquals(Band.STANDARD, AmbushRules.band(0, 1));
        assertEquals(Band.STANDARD, AmbushRules.band(1, 1));
        assertEquals(Band.HARD, AmbushRules.band(2, 1));
        assertEquals(Band.HARD, AmbushRules.band(3, 1));
        assertEquals(Band.AMBUSH, AmbushRules.band(4, 1));
        assertEquals(Band.AMBUSH, AmbushRules.band(99, 1), "the ladder tops out");
        assertEquals(Band.STANDARD, AmbushRules.band(0, 7));
        assertEquals(Band.HARD, AmbushRules.band(0, 16 + 8), "depth alone climbs too (three bands of eight floors)");
        for (int s = 0; s < 12; s++) for (int d = 0; d < 80; d += 7) assertNotEquals(Band.NOTHING, AmbushRules.band(s, d));
        assertEquals(Band.STANDARD, AmbushRules.band(-5, -5), "nonsense inputs are the first rung");
    }

    @Test
    void aBadConfigurationCannotSwitchTheBudgetOff() {
        AmbushRules.Config bad = new AmbushRules.Config(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0).clamped();
        assertTrue(bad.threshold() >= 1.0, "a threshold of nothing would ambush everybody");
        assertTrue(bad.budgetMax() >= 1 && bad.budgetWindowTicks() >= 600);
        assertTrue(bad.cooldownTicks() >= 100);
        assertTrue(bad.crowdCap() >= 1);
        assertTrue(bad.speedLimit() >= 6.0, "a sprint is 5.6 blocks a second: a lower limit would ambush every runner");
        assertTrue(bad.teleportBlocks() >= 8.0);
        assertTrue(bad.minLiveMobs() >= 1);
        AmbushRules.Config huge = new AmbushRules.Config(1e9, 1e9, 1e9, 1e9, 1e9, 1e9, 1e9, 1e9, 1_000_000, 1e9, 1000, Long.MAX_VALUE / 2, 1000, Long.MAX_VALUE / 2, 1000, Long.MAX_VALUE / 2).clamped();
        assertTrue(huge.threshold() <= 100.0 && huge.budgetMax() <= 20 && huge.crowdCap() <= 64 && huge.speedWindowTicks() <= 400);
    }

    @Test
    void aFairPlayerNeverReachesTheThresholdAndARunnerDoes() {
        // a fair fight: leaving one room with two monsters behind, once, then a quiet minute
        AmbushLedger fair = new AmbushLedger();
        fair.add(AmbushRules.Signal.LEFT_LIVE_ROOM, 2, 0, C);
        assertTrue(fair.heat(0, C) < C.threshold());
        assertTrue(fair.heat(20L * 30L, C) < 1.0, "half a minute later it is gone");
        // a runner: left a room with four alive, then ran a corridor for four seconds
        AmbushLedger runner = new AmbushLedger();
        runner.add(AmbushRules.Signal.LEFT_LIVE_ROOM, 4, 0, C);
        for (int s = 1; s <= 4; s++) runner.add(AmbushRules.Signal.FAST_TRAVEL, 0, 20L * s, C);
        assertTrue(runner.heat(80, C) >= C.threshold(), "heat " + runner.heat(80, C));
    }
}
