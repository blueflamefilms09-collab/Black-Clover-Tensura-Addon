package com.newuniverse.nusmp.boss.budget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The per-second damage budget: truncation, exhaustion, the 20-tick reset, independence, and the settle rule setHealth uses. */
class DamageBudgetTest {
    @Test
    void allowsWhatFitsAndTruncatesTheRest() {
        DamageBudget b = new DamageBudget(100f);
        assertEquals(40f, b.allow(40f));
        assertEquals(60f, b.allow(1000f), "the remainder of the budget, exactly");
        assertEquals(0f, b.left());
        assertEquals(0f, b.allow(5f), "an empty budget rejects");
    }

    @Test
    void badAmountsAllowNothingAndCostNothing() {
        DamageBudget b = new DamageBudget(10f);
        assertEquals(0f, b.allow(0f));
        assertEquals(0f, b.allow(-5f));
        assertEquals(0f, b.allow(Float.NaN));
        assertEquals(10f, b.left());
        assertEquals(10f, b.allow(Float.POSITIVE_INFINITY), "infinity (a kill) is cut to the budget");
    }

    @Test
    void resetsOnTheTwentiethTickOnly() {
        DamageBudget b = new DamageBudget(10f);
        b.allow(10f);
        for (int i = 1; i < 20; i++) {
            assertFalse(b.tick(), "tick " + i);
            assertEquals(0f, b.left(), "still spent at tick " + i);
        }
        assertTrue(b.tick(), "the 20th tick restores");
        assertEquals(10f, b.left());
        assertEquals(0, b.ticksInWindow());
        b.allow(3f);
        for (int i = 0; i < 19; i++) b.tick();
        assertEquals(7f, b.left(), "the second window runs its own 20 ticks");
        b.tick();
        assertEquals(10f, b.left());
    }

    @Test
    void twoBossesDoNotShareABudget() {
        DamageBudget a = new DamageBudget(10f), c = new DamageBudget(10f);
        a.allow(10f);
        assertEquals(0f, a.left());
        assertEquals(10f, c.left());
        for (int i = 0; i < 20; i++) a.tick();
        assertEquals(10f, a.left());
        assertEquals(10f, c.allow(10f));
    }

    @Test
    void aCapChangeNeverRefillsASpentWindow() {
        DamageBudget b = new DamageBudget(100f);
        b.allow(100f);
        b.setCap(150f);
        assertEquals(50f, b.left(), "only the new headroom is added");
        b.setCap(30f);
        assertEquals(0f, b.left());
        b.reset();
        assertEquals(30f, b.left());
    }

    @Test
    void capPerSecondIsAFractionWithAFloorAndAPhaseScale() {
        assertEquals(120f, BudgetPolicy.capPerSecond(2000f, 0.06, 20, 1.0), 0.001f);
        assertEquals(180f, BudgetPolicy.capPerSecond(2000f, 0.06, 20, 1.5), 0.001f);
        assertEquals(20f, BudgetPolicy.capPerSecond(100f, 0.06, 20, 1.0), "a small boss gets the floor");
        assertEquals(5f, BudgetPolicy.capPerSecond(5f, 0.06, 20, 1.0), "never above the whole bar");
        assertEquals(0f, BudgetPolicy.capPerSecond(0f, 0.06, 20, 1.0));
        assertEquals(120f, BudgetPolicy.capPerSecond(2000f, 0.06, 20, 0.2), "a phase scale under 1 is read as 1");
    }

    @Test
    void phaseScaleRisesIntoTheFinalPhase() {
        assertEquals(1.0, BudgetPolicy.phaseScale(0, 4, 1.5));
        assertEquals(1.0, BudgetPolicy.phaseScale(2, 4, 1.5));
        assertEquals(1.25, BudgetPolicy.phaseScale(3, 4, 1.5));
        assertEquals(1.5, BudgetPolicy.phaseScale(4, 4, 1.5));
        assertEquals(1.0, BudgetPolicy.phaseScale(4, 4, 0.5), "never tighter");
        assertEquals(1.0, BudgetPolicy.phaseScale(3, 0, 1.5), "no phases, no scale");
    }

    @Test
    void settleKeepsHealsAndCutsFalls() {
        assertEquals(80f, BudgetPolicy.settle(50f, 80f, 0f), "a heal passes with an empty budget");
        assertEquals(50f, BudgetPolicy.settle(50f, 50f, 0f));
        assertEquals(40f, BudgetPolicy.settle(50f, 40f, 10f), "a fall that fits lands exactly");
        assertEquals(40f, BudgetPolicy.settle(50f, 0f, 10f), "a kill is cut to the 10 allowed");
        assertEquals(50f, BudgetPolicy.settle(50f, 0f, 0f), "nothing allowed, nothing lost");
        assertEquals(50f, BudgetPolicy.settle(50f, Float.NaN, 10f));
    }

    /** Simulates the boss's health through setHealth for several seconds: a /kill every tick takes max/6% seconds, and always ends dead. */
    @Test
    void aKillEveryTickSpreadsOverSecondsAndStillKills() {
        float max = 1000f, health = max;
        DamageBudget b = new DamageBudget(BudgetPolicy.capPerSecond(max, 0.06, 20, 1.0));    // 60 per second
        int ticks = 0;
        while (health > 0f && ticks < 20 * 60) {
            float requested = 0f;                                                              // setHealth(0) like a kill
            float drop = health - requested;
            health = BudgetPolicy.settle(health, requested, b.allow(drop));
            b.tick();
            ticks++;
        }
        assertEquals(0f, health, "it dies through the normal path");
        assertTrue(ticks > 20 * 15 && ticks <= 20 * 17, "1000 hp at 60 hp/s is 17 s, not an instant kill: " + ticks);
    }

    @Test
    void aBossOnAHairOfHealthWithBudgetLeftDies() {
        DamageBudget b = new DamageBudget(20f);
        float health = 0.01f;
        health = BudgetPolicy.settle(health, 0f, b.allow(health));
        assertEquals(0f, health);
        DamageBudget spent = new DamageBudget(20f);
        spent.allow(20f);
        assertEquals(0.01f, BudgetPolicy.settle(0.01f, 0f, spent.allow(0.01f)), "stuck for the rest of the second only");
        for (int i = 0; i < 20; i++) spent.tick();
        assertEquals(0f, BudgetPolicy.settle(0.01f, 0f, spent.allow(0.01f)), "and dead when the window turns");
    }

    @Test
    void healingBetweenHitsIsNotChargedToTheBudget() {
        DamageBudget b = new DamageBudget(50f);
        float health = 500f;
        health = BudgetPolicy.settle(health, health - 50f, b.allow(50f));
        health = BudgetPolicy.settle(health, health + 200f, b.allow(0f));
        assertEquals(650f, health);
        assertEquals(0f, b.left(), "the heal neither refilled nor cost the budget");
    }
}
