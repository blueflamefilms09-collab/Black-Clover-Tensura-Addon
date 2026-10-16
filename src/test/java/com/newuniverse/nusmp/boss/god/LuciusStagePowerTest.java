package com.newuniverse.nusmp.boss.god;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The power curve read over the virtual three-stage bar: monotone, continuous across stages, the Gates only late, and no skipped line. */
class LuciusStagePowerTest {
    private static final float MAX = 4800f;

    private static float frac(int stage, float health) { return LuciusStage.virtualHealth(stage, health, MAX) / LuciusStage.virtualMax(MAX); }

    @Test
    void powerNeverFallsWhenHealthFallsThroughAllThreeStages() {
        int power = PowerCurve.OPENING, last = power;
        for (int stage = 1; stage <= 3; stage++) {
            for (float hp = MAX; hp >= 0f; hp -= 48f) {
                power = PowerCurve.next(frac(stage, hp), power);
                assertTrue(power >= last, "stage " + stage + " hp " + hp);
                last = power;
            }
        }
        assertEquals(PowerCurve.FULL, power, "the Gates are open by the end");
    }

    @Test
    void theGatesOpenInTheLastStageAndNotBefore() {
        int power = PowerCurve.OPENING;
        for (int stage = 1; stage <= 2; stage++)
            for (float hp = MAX; hp >= 0f; hp -= 24f) {
                power = PowerCurve.next(frac(stage, hp), power);
                assertTrue(power < PowerCurve.FULL, "stage " + stage + " hp " + hp);
            }
        assertEquals(PowerCurve.CAP, power, "stage 3 starts at the cap");
        int p3 = power;
        for (float hp = MAX; hp >= 0f; hp -= 24f) p3 = PowerCurve.next(frac(3, hp), p3);
        assertEquals(PowerCurve.FULL, p3);
    }

    @Test
    void theEndOfOneStageAndTheStartOfTheNextAreTheSamePoint() {
        assertEquals(frac(1, 0f), frac(2, MAX), 1e-6f);
        assertEquals(frac(2, 0f), frac(3, MAX), 1e-6f);
    }

    @Test
    void aStageStartedAtFullHealthDoesNotRestartThePowerAt20() {
        int power = PowerCurve.OPENING;
        for (float hp = MAX; hp >= 0f; hp -= 48f) power = PowerCurve.next(frac(1, hp), power);
        int atStageTwo = PowerCurve.next(frac(2, MAX), power);
        assertTrue(atStageTwo >= power, "a refill is not a reset");
        assertTrue(atStageTwo >= 40);
    }

    @Test
    void aHugeHitCannotSkipALineOnTheVirtualBar() {
        float vmax = LuciusStage.virtualMax(MAX);
        float vh = LuciusStage.virtualHealth(1, MAX, MAX);
        int due = PowerCurve.next(vh / vmax, PowerCurve.OPENING);
        float clamped = PowerCurve.clampDamage(vh, vmax, due, 1_000_000f);
        assertTrue(clamped < vmax * 0.2f, "bounded by the next line: " + clamped);
        assertTrue(clamped > 0f);
    }

    @Test
    void duringARefillTheNewStageCountsAsFull() {
        float hp = 1f;
        float whileRefilling = LuciusStage.virtualHealth(2, MAX, MAX) / LuciusStage.virtualMax(MAX);
        float naive = LuciusStage.virtualHealth(2, hp, MAX) / LuciusStage.virtualMax(MAX);
        assertTrue(naive < whileRefilling - 0.3f, "the dip the glue avoids");
        assertEquals(PowerCurve.next(frac(2, MAX), 40), PowerCurve.next(whileRefilling, 40));
    }
}
