package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.boss.budget.DamageBudget;
import com.newuniverse.nusmp.entity.zogratis.ReactiveGuard.Params;
import com.newuniverse.nusmp.entity.zogratis.ReactiveGuard.Verdict;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Leon study fix #4: the guard's odds are bounded, its cooldowns and the dodge cap hold, the counter fires once, and it never gets more through than a foe offered or than the budget allows. */
class ReactiveGuardTest {
    private static ReactiveGuard guard() { return new ReactiveGuard(Params.of(0.7, 0.3)); }

    @Test
    void leonsNumbersAreTheDefaults() {
        Params p = Params.of(ReactiveGuard.BLOCK_CHANCE, ReactiveGuard.DODGE_CHANCE);
        assertEquals(0.7, p.blockChance(), 1e-9);
        assertEquals(0.3, p.dodgeChance(), 1e-9);
        assertEquals(8, p.dodgeFrames());
        assertEquals(12, p.blockWindow());
        assertEquals(5.0, ReactiveGuard.STANCE_ENTER);
    }

    @Test
    void oddsAreClampedAndBadNumbersAreSafe() {
        Params p = Params.of(5.0, -3.0);
        assertEquals(ReactiveGuard.MAX_CHANCE, p.blockChance(), 1e-9, "never certain");
        assertEquals(0.0, p.dodgeChance(), 1e-9);
        assertEquals(0.0, Params.of(Double.NaN, Double.NaN).blockChance());
        Params weird = new Params(0.7, 0.3, 100, 100, 1, 1, 100, 0f);
        assertTrue(weird.blockCooldown() >= weird.blockWindow() * 2, "an action is at most half of its cycle");
        assertTrue(weird.dodgeCooldown() >= weird.dodgeFrames() * 2);
        assertTrue(weird.blockKeep() >= 0.05f, "a block always lets something through");
        assertTrue(weird.leastThrough() > 0.0);
    }

    @Test
    void theDefaultGuardLetsAtLeastTwoThirdsThroughInTheLongRun() {
        Params p = Params.of(0.7, 0.3);
        assertEquals(0.30, p.blockShare(), 1e-9);
        assertEquals(8 / 60.0, p.dodgeShare(), 1e-9);
        assertTrue(p.leastThrough() > 0.6, "got " + p.leastThrough());
        Params worst = Params.of(0.9, 0.9);
        assertTrue(worst.leastThrough() > 0.6, "the cooldowns, not the odds, bound it: " + worst.leastThrough());
    }

    @Test
    void outsideTheStanceNothingIsGuardedAndTheStanceHasHysteresis() {
        ReactiveGuard g = guard();
        assertEquals(Verdict.NONE, g.judge(100, 5.5, true, 0, 0, 1), "5.5 blocks: not yet in");
        assertEquals(Verdict.DODGE, g.judge(101, 4.9, true, 0.0, 0.0, 1), "inside 5: in");
        assertTrue(g.stance(5.9), "stays in until 6");
        assertFalse(g.stance(6.1));
        assertFalse(g.stance(5.5), "and does not come back until 5");
        assertTrue(g.stance(4.0));
    }

    @Test
    void aBossThatCannotGuardNeverDoes() {
        ReactiveGuard g = guard();
        for (int t = 0; t < 400; t++) assertEquals(Verdict.NONE, g.judge(t, 1.0, false, 0.0, 0.0, 1));
    }

    @Test
    void aDodgeIsInvulnerableForItsFramesThenOnCooldown() {
        ReactiveGuard g = guard();
        assertEquals(Verdict.DODGE, g.judge(100, 2, true, 0.0, 0.0, 1));
        for (int t = 101; t < 108; t++) assertEquals(Verdict.DODGE, g.judge(t, 2, true, 0.99, 0.99, 1), "i-frame tick " + t);
        assertEquals(Verdict.NONE, g.judge(108, 2, true, 0.0, 0.99, 1), "the 8 frames are over and the dodge is cooling down");
        assertEquals(Verdict.NONE, g.judge(159, 2, true, 0.0, 0.99, 1));
        assertEquals(Verdict.DODGE, g.judge(160, 2, true, 0.0, 0.99, 1), "ready again after 60 ticks");
        assertEquals(0f, g.apply(Verdict.DODGE, 50f));
    }

    @Test
    void aBlockCutsToAQuarterForItsWindowThenCoolsDown() {
        ReactiveGuard g = guard();
        assertEquals(Verdict.BLOCK, g.judge(100, 2, true, 0.99, 0.0, 7));
        assertEquals(12.5f, g.apply(Verdict.BLOCK, 50f), 1e-4);
        assertEquals(Verdict.BLOCK, g.judge(111, 2, true, 0.99, 0.99, 7), "inside the 12-tick window every hit is blocked");
        assertEquals(Verdict.NONE, g.judge(112, 2, true, 0.99, 0.0, 7), "the window is over, the block cools down");
        assertEquals(Verdict.NONE, g.judge(139, 2, true, 0.99, 0.0, 7));
        assertEquals(Verdict.BLOCK, g.judge(140, 2, true, 0.99, 0.0, 7));
        assertEquals(50f, g.apply(Verdict.NONE, 50f));
    }

    @Test
    void theCounterFiresOnceAfterTheBlockWindowAndOnlyForABlock() {
        ReactiveGuard g = guard();
        assertEquals(-1, g.takeCounter(0), "nothing pending");
        g.judge(100, 2, true, 0.99, 0.0, 42);
        assertEquals(-1, g.takeCounter(105), "not before the block window ends");
        assertEquals(42, g.takeCounter(112));
        assertEquals(-1, g.takeCounter(113), "once");
        g.judge(200, 2, true, 0.0, 0.0, 43);
        assertEquals(-1, g.takeCounter(300), "a dodge is not answered");
        g.judge(400, 2, true, 0.99, 0.0, 44);
        g.reset();
        assertEquals(-1, g.takeCounter(500), "an interrupted boss drops its counter");
    }

    @Test
    void dodgesAreCappedPerSecondWhateverTheCooldown() {
        Params p = new Params(0.5, 0.9, 2, 12, 40, 1, 1, 0.25f);
        assertTrue(p.dodgeCooldown() >= 20, "one dodge a second at the most");
        Params two = new Params(0.5, 0.9, 2, 12, 40, 1, 2, 0.25f);
        assertTrue(two.dodgeCooldown() >= 10);
        ReactiveGuard h = new ReactiveGuard(p);
        long last = -100;
        int fresh = 0;
        for (int t = 0; t < 20 * 60; t++) {
            if (h.judge(t, 2, true, 0.0, 0.99, 1) == Verdict.DODGE && t - last >= p.dodgeFrames()) {      // a dodge that is not one of the i-frames of the last
                assertTrue(t - last >= 20, "a new dodge only after a second (t=" + t + ")");
                last = t;
                fresh++;
            }
        }
        assertTrue(fresh > 10, "and it does dodge: " + fresh);
    }

    @Test
    void neverMoreThroughThanOfferedNorThanTheBudgetAndAlwaysSomething() {
        Random rnd = new Random(7);
        ReactiveGuard g = guard();
        DamageBudget budget = new DamageBudget(100f);
        double offered = 0, afterGuard = 0, afterBudget = 0;
        float maxBudgetSecond = 0f, second = 0f;
        for (int t = 0; t < 20 * 600; t++) {                               // ten minutes of a hit on every tick, the worst case for the boss
            float hit = 10f;
            Verdict v = g.judge(t, 1.5, true, rnd.nextDouble(), rnd.nextDouble(), 1);
            float kept = g.apply(v, hit);
            assertTrue(kept >= 0f && kept <= hit, "the guard only cuts");
            float landed = budget.allow(kept);                              // the budget still sits behind it
            offered += hit;
            afterGuard += kept;
            afterBudget += landed;
            second += landed;
            if (budget.tick()) { maxBudgetSecond = Math.max(maxBudgetSecond, second); second = 0f; }
        }
        assertTrue(afterBudget <= afterGuard + 1e-3 && afterGuard <= offered);
        assertTrue(maxBudgetSecond <= 100f + 1e-3, "the budget's cap per second still holds behind the guard: " + maxBudgetSecond);
        assertTrue(afterGuard / offered > 0.55, "a foe that never stops still gets about two thirds of its damage past the guard: " + afterGuard / offered);
        assertTrue(afterGuard > 0, "the boss can always be hurt");
    }

    @Test
    void theFractionOfBlockedAndDodgedHitsStaysBoundedForAnyOdds() {
        for (double odds : new double[]{0.0, 0.3, 0.7, 0.9, 1.0}) {
            ReactiveGuard g = new ReactiveGuard(Params.of(odds, odds));
            Random rnd = new Random(1);
            int guarded = 0, n = 20 * 600;
            for (int t = 0; t < n; t++) if (g.judge(t, 2, true, rnd.nextDouble(), rnd.nextDouble(), 1) != Verdict.NONE) guarded++;
            assertTrue(guarded / (double) n <= 0.44, "odds " + odds + " guarded " + guarded / (double) n + " of the ticks' hits");
        }
    }
}
