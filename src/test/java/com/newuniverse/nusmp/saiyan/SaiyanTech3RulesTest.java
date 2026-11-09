package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the numbers of Dragon Fist, God Bind, Ki Deflection and Energy Transfer (3.71.0) are pure arithmetic and every count of them is bounded; this pins both. */
class SaiyanTech3RulesTest {
    @Test
    void dragonFistNeedsTheMartialTrainingAndHasAtMostEightSteps() {
        assertFalse(SaiyanTech3Rules.dragonUnlocked(14.9, 15.0));
        assertTrue(SaiyanTech3Rules.dragonUnlocked(15.0, 15.0));
        assertTrue(SaiyanTech3Rules.dragonUnlocked(0.0, -5.0), "a negative need is no need");
        assertEquals(8, SaiyanTech3Rules.dragonSteps(1000));
        assertEquals(2, SaiyanTech3Rules.dragonSteps(-3));
        assertEquals(6, SaiyanTech3Rules.dragonSteps(6));
    }

    @Test
    void onlyTheLastDragonStepDamagesAndTheBodyRisesToItsTop() {
        for (int i = 0; i < 5; i++) assertEquals(0.0, SaiyanTech3Rules.dragonStepFactor(i, 6), 1e-9);
        assertEquals(3.0, SaiyanTech3Rules.dragonStepFactor(5, 6), 1e-9);
        assertEquals(3.0, SaiyanTech3Rules.dragonHeight(5, 6, 3.0), 1e-9);
        assertTrue(SaiyanTech3Rules.dragonHeight(1, 6, 3.0) < SaiyanTech3Rules.dragonHeight(2, 6, 3.0));
        assertEquals(3.0, SaiyanTech3Rules.dragonHeight(50, 6, 3.0), 1e-9, "never past the top");
        assertEquals(0.0, SaiyanTech3Rules.dragonHeight(0, 6, -2.0), 1e-9);
    }

    @Test
    void godBindIsShorterOnABossAndOnAPlayerAndNeverPastThirtySeconds() {
        assertEquals(120, SaiyanTech3Rules.bindTicks(6.0, false, false, 0.25, 0.5));
        assertEquals(30, SaiyanTech3Rules.bindTicks(6.0, true, false, 0.25, 0.5));
        assertEquals(60, SaiyanTech3Rules.bindTicks(6.0, false, true, 0.25, 0.5));
        assertEquals(30, SaiyanTech3Rules.bindTicks(6.0, true, true, 0.25, 0.5), "a boss player gets the smaller share, not the product");
        assertEquals(0, SaiyanTech3Rules.bindTicks(6.0, true, false, 0.0, 0.5), "a share of zero resists fully");
        assertEquals(SaiyanTech3Rules.MAX_BIND_TICKS, SaiyanTech3Rules.bindTicks(1.0e9, false, false, 1.0, 1.0));
        assertEquals(0, SaiyanTech3Rules.bindTicks(-4.0, false, false, 1.0, 1.0));
        assertTrue(SaiyanTech3Rules.bindHolds(99, 100));
        assertFalse(SaiyanTech3Rules.bindHolds(100, 100));
    }

    @Test
    void aTurnedProjectileIsNeverFasterThanThreeBlocksATick() {
        assertEquals(SaiyanTech3Rules.MAX_DEFLECT_SPEED, SaiyanTech3Rules.deflectSpeed(100.0, 3.0), 1e-9);
        assertEquals(0.72, SaiyanTech3Rules.deflectSpeed(0.0, 1.2), 1e-9, "a slow one is sent at a fast arrow's speed");
        assertEquals(1.2, SaiyanTech3Rules.deflectSpeed(1.0, 1.2), 1e-9);
        assertTrue(SaiyanTech3Rules.deflectSpeed(-5.0, -5.0) >= 0.3);
        assertEquals(8, SaiyanTech3Rules.MAX_DEFLECTS);
    }

    @Test
    void thePulseRunsEverySecondTickOnly() {
        int n = 0;
        for (long t = 0; t < 100; t++) if (SaiyanTech3Rules.pulseTick(t)) n++;
        assertEquals(50, n);
    }

    @Test
    void energyTransferLosesSomeKiIsCappedAndNeedsAReceiverWithKi() {
        assertEquals(16.0, SaiyanTech3Rules.receivedPercent(1000.0, 20.0, 0.8, 1000.0, 30.0), 1e-9);
        assertEquals(30.0, SaiyanTech3Rules.receivedPercent(1000.0, 100.0, 1.0, 100.0, 30.0), 1e-9, "capped");
        assertEquals(0.0, SaiyanTech3Rules.receivedPercent(1000.0, 20.0, 0.8, 0.0, 30.0), 1e-9);
        assertEquals(0.0, SaiyanTech3Rules.receivedPercent(0.0, 20.0, 0.8, 1000.0, 30.0), 1e-9);
        assertTrue(SaiyanTech3Rules.receivedPercent(1000.0, 20.0, 5.0, 1000.0, 100.0) <= 20.0 + 1e-9, "efficiency over 1 never makes Ki");
        assertEquals(0.0, SaiyanTech3Rules.receivedPercent(1000.0, -20.0, 0.8, 1000.0, 30.0), 1e-9);
    }
}
