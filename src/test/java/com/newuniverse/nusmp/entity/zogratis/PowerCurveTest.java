package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Percentage Power System: steps, monotonic power, Triad rage, tuning and the one-step damage clamp. */
class PowerCurveTest {
    @Test
    void healthAsksForTheRightStep() {
        assertEquals(20, PowerCurve.target(1.0f));
        assertEquals(20, PowerCurve.target(0.85f), "under 85%, not at 85%");
        assertEquals(40, PowerCurve.target(0.8499f));
        assertEquals(40, PowerCurve.target(0.65f));
        assertEquals(60, PowerCurve.target(0.6499f));
        assertEquals(60, PowerCurve.target(0.45f));
        assertEquals(80, PowerCurve.target(0.4499f));
        assertEquals(80, PowerCurve.target(0.25f), "the cap holds until under 25%");
        assertEquals(100, PowerCurve.target(0.2499f));
        assertEquals(100, PowerCurve.target(0f));
        assertEquals(20, PowerCurve.target(Float.NaN));
    }

    @Test
    void powerNeverGoesDown() {
        int p = PowerCurve.OPENING;
        for (float h = 1f; h >= 0f; h -= 0.01f) {
            int n = PowerCurve.next(h, p);
            assertTrue(n >= p, "power fell at " + h);
            p = n;
        }
        assertEquals(100, p);
        assertEquals(100, PowerCurve.next(1f, 100), "healing back up keeps the power");
        assertEquals(50, PowerCurve.next(0.95f, 50), "rage power survives a healthy bar");
        assertEquals(60, PowerCurve.next(0.6f, 50));
        assertEquals(100, PowerCurve.next(0.5f, 250), "clamped to 100");
    }

    @Test
    void nextIsPure() {
        for (float h : new float[]{0.9f, 0.6f, 0.3f, 0.1f})
            for (int prev : new int[]{20, 40, 50, 80, 100}) assertEquals(PowerCurve.next(h, prev), PowerCurve.next(h, prev));
    }

    @Test
    void rageIsTenPointsNeverPastTheCap() {
        assertEquals(30, PowerCurve.rage(20));
        assertEquals(50, PowerCurve.rage(40));
        assertEquals(70, PowerCurve.rage(60));
        assertEquals(80, PowerCurve.rage(70), "70 + 10 reaches the cap");
        assertEquals(80, PowerCurve.rage(75), "never above 80");
        assertEquals(80, PowerCurve.rage(80));
        assertEquals(100, PowerCurve.rage(100), "100 is kept");
        for (int p = 0; p <= 100; p++) {
            int r = PowerCurve.rage(p);
            assertTrue(r >= p);
            assertTrue(p >= 100 || r <= 100);
            if (p < 80) assertTrue(r <= 80);
        }
    }

    @Test
    void gatesAndStages() {
        assertFalse(PowerCurve.gatesOpen(80));
        assertTrue(PowerCurve.gatesOpen(100));
        assertEquals(0, PowerCurve.stage(20));
        assertEquals(0, PowerCurve.stage(30));
        assertEquals(1, PowerCurve.stage(40));
        assertEquals(1, PowerCurve.stage(50));
        assertEquals(2, PowerCurve.stage(60));
        assertEquals(3, PowerCurve.stage(80));
        assertEquals(4, PowerCurve.stage(100));
        assertEquals(0.85f, PowerCurve.nextGateHealth(20));
        assertEquals(0.65f, PowerCurve.nextGateHealth(40));
        assertEquals(0.65f, PowerCurve.nextGateHealth(50), "a rage value waits for the next real step");
        assertEquals(0.25f, PowerCurve.nextGateHealth(80));
        assertEquals(0f, PowerCurve.nextGateHealth(100));
    }

    @Test
    void oneBurstCannotSkipAStep() {
        // 1000 max health at power 20: the line is 850
        assertEquals(10f, PowerCurve.clampDamage(1000f, 1000f, 20, 10f), "far above the line: unchanged");
        assertEquals(150.5f, PowerCurve.clampDamage(1000f, 1000f, 20, 900f), 0.001f, "stops half a point under the line");
        assertEquals(50f, PowerCurve.clampDamage(900f, 1000f, 20, 50f), "landing exactly on the line is allowed");
        assertEquals(50.5f, PowerCurve.clampDamage(900f, 1000f, 20, 400f), 0.001f);
        assertEquals(400f, PowerCurve.clampDamage(840f, 1000f, 20, 400f), "already below the line: the power update catches up next tick");
        assertEquals(5000f, PowerCurve.clampDamage(1000f, 1000f, 100, 5000f), "no line at 100%");
        assertEquals(0f, PowerCurve.clampDamage(1000f, 1000f, 20, 0f));
        // the hit that crosses the line stops at it, and the next hit (at the new power) is bounded by the following line
        float hp = 1000f;
        int power = 20;
        float hit = PowerCurve.clampDamage(hp, 1000f, power, 2000f);
        hp -= hit;
        assertTrue(hp < 850f && hp > 849f);
        power = PowerCurve.next(hp / 1000f, power);
        assertEquals(40, power);
        hit = PowerCurve.clampDamage(hp, 1000f, power, 2000f);
        hp -= hit;
        assertTrue(hp < 650f && hp > 649f, "the second step is bounded by its own line: " + hp);
    }

    @Test
    void tuningGrowsWithPower() {
        PowerCurve.Tuning prev = PowerCurve.tuning(0);
        for (int p = 1; p <= 100; p++) {
            PowerCurve.Tuning t = PowerCurve.tuning(p);
            assertTrue(t.damageMultiplier() >= prev.damageMultiplier(), "damage at " + p);
            assertTrue(t.cooldownMultiplier() <= prev.cooldownMultiplier(), "cooldown at " + p);
            assertTrue(t.speedMultiplier() >= prev.speedMultiplier(), "speed at " + p);
            assertTrue(t.windUpMultiplier() <= prev.windUpMultiplier(), "wind-up at " + p);
            assertTrue(t.regenFractionPerSecond() >= prev.regenFractionPerSecond(), "regen at " + p);
            assertTrue(t.discipleCap() >= prev.discipleCap(), "disciples at " + p);
            assertTrue(t.castSlots() >= prev.castSlots(), "slots at " + p);
            assertTrue(t.ultimateGapTicks() <= prev.ultimateGapTicks(), "ultimate gap at " + p);
            assertTrue(t.manaCap() >= prev.manaCap(), "mana cap at " + p);
            assertEquals(p, t.power());
            prev = t;
        }
    }

    @Test
    void anchorsAreExact() {
        PowerCurve.Tuning t20 = PowerCurve.tuning(20), t100 = PowerCurve.tuning(100);
        assertEquals(0.60, t20.damageMultiplier(), 1e-9);
        assertEquals(1.60, t100.damageMultiplier(), 1e-9);
        assertEquals(1, t20.discipleCap());
        assertEquals(6, t100.discipleCap());
        assertEquals(1, t20.castSlots());
        assertEquals(4, t100.castSlots());
        assertEquals(PowerCurve.tuning(20).damageMultiplier(), PowerCurve.tuning(5).damageMultiplier(), "under 20 uses the opening numbers");
        assertEquals(0.80, PowerCurve.tuning(40).damageMultiplier(), 1e-9);
        assertEquals(0.70, PowerCurve.tuning(30).damageMultiplier(), 1e-9, "halfway between the 20 and 40 anchors");
    }

    @Test
    void manaIsUnboundFromSixty() {
        for (int p = 0; p < 60; p++) assertFalse(PowerCurve.tuning(p).infiniteMana(), "finite at " + p);
        for (int p = 60; p <= 100; p++) assertTrue(PowerCurve.tuning(p).infiniteMana(), "unbound at " + p);
        assertTrue(PowerCurve.tuning(60).cooldownMultiplier() < PowerCurve.tuning(59).cooldownMultiplier() - 0.005 || PowerCurve.tuning(60).cooldownMultiplier() < PowerCurve.tuning(40).cooldownMultiplier(),
                "cooldowns shrink further once the mana is unbound");
    }

    @Test
    void discipleShareIsAtMostFortyPercent() {
        assertEquals(0.08, PowerCurve.discipleShare(20), 1e-9);
        assertEquals(0.4, PowerCurve.discipleShare(100), 1e-9);
        assertEquals(0.4, PowerCurve.discipleShare(300), 1e-9);
        assertEquals(0.0, PowerCurve.discipleShare(-5), 1e-9);
    }
}
