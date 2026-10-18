package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.hammer.PoiseMath.Result;
import com.newuniverse.nusmp.heroweapon.weapon.hammer.PoiseMath.State;
import org.junit.jupiter.api.Test;

/**
 * Why: the poise gauge decides when the Hammer's blows turn into a break, so its arithmetic is pinned: the size of a gauge, the recovery by the clock, the break, the lockout
 * that stops a foe being broken again at once, and the safety for a clock that went backwards.
 */
class PoiseMathTest {
    @Test
    void theGaugeIsAsBigAsTheFoeIsTough() {
        assertEquals(12f, PoiseMath.capacity(5f, false), 1e-6);                // a small mob: the floor
        assertEquals(12f, PoiseMath.capacity(20f, false), 1e-6);               // a zombie: 60 percent of 20
        assertEquals(60f, PoiseMath.capacity(100f, false), 1e-3);              // an iron golem
        assertEquals(80f, PoiseMath.capacity(400f, false), 1e-6);              // the ceiling
        assertEquals(100f, PoiseMath.capacity(200f, true), 1e-6);              // a boss: the floor
        assertEquals(250f, PoiseMath.capacity(1000f, true), 1e-6);
        assertEquals(300f, PoiseMath.capacity(9000f, true), 1e-6);             // a boss: the ceiling
    }

    @Test
    void blowsAddUpAndTheLastOneBreaksTheFoe() {
        float cap = 30f;
        Result a = PoiseMath.add(State.EMPTY, 10f, 100, cap, false);
        assertFalse(a.broke());
        assertEquals(10f, a.state().value(), 1e-6);
        Result b = PoiseMath.add(a.state(), 10f, 110, cap, false);
        assertFalse(b.broke());
        assertEquals(20f, b.state().value(), 1e-6);
        Result c = PoiseMath.add(b.state(), 10f, 120, cap, false);
        assertTrue(c.broke(), "30 of 30 breaks");
        assertEquals(0f, c.state().value(), 1e-6);
        assertTrue(PoiseMath.broken(c.state(), 120 + PoiseMath.BREAK_TICKS - 1));
        assertFalse(PoiseMath.broken(c.state(), 120 + PoiseMath.BREAK_TICKS));
    }

    @Test
    void aBrokenFoeCannotBeBrokenAgainUntilTheLockoutEnds() {
        float cap = 20f;
        State s = PoiseMath.add(State.EMPTY, 25f, 1000, cap, false).state();
        long freeAt = 1000 + PoiseMath.BREAK_TICKS + PoiseMath.LOCKOUT;
        assertTrue(PoiseMath.locked(s, freeAt - 1));
        Result during = PoiseMath.add(s, 100f, freeAt - 1, cap, false);
        assertFalse(during.broke(), "locked: no poise damage");
        assertEquals(s, during.state(), "locked: nothing changes");
        assertFalse(PoiseMath.locked(s, freeAt));
        assertTrue(PoiseMath.add(s, 25f, freeAt, cap, false).broke(), "free again");
    }

    @Test
    void theGaugeRecoversByTheClock() {
        float cap = 50f;
        State s = PoiseMath.add(State.EMPTY, 40f, 500, cap, false).state();
        assertEquals(40f, PoiseMath.current(s, 500 + PoiseMath.DECAY_DELAY, cap), 1e-4, "held for the delay");
        float half = PoiseMath.current(s, 500 + PoiseMath.DECAY_DELAY + 40, cap);       // 40 ticks of 0.5 a tick
        assertEquals(20f, half, 1e-3);
        assertEquals(0f, PoiseMath.current(s, 500 + PoiseMath.DECAY_DELAY + PoiseMath.DECAY_SPAN, cap), 1e-3, "empty after a full span");
        assertEquals(0f, PoiseMath.current(s, 500 + 10000, cap), 1e-6, "never below zero");
    }

    @Test
    void aShrunkGaugeNeverHoldsMoreThanItsCapacity() {
        State s = new State(70f, 10, 0, 0);
        assertEquals(30f, PoiseMath.current(s, 10, 30f), 1e-6, "the foe's health fell, its gauge shrank with it");
    }

    @Test
    void aBossBreaksBrieflyAndRecoversSlowly() {
        float cap = 100f;
        Result r = PoiseMath.add(State.EMPTY, 120f, 0, cap, true);
        assertTrue(r.broke());
        assertEquals(PoiseMath.BREAK_TICKS_BOSS, r.state().brokenUntil());
        assertEquals(PoiseMath.BREAK_TICKS_BOSS + PoiseMath.LOCKOUT_BOSS, r.state().lockUntil());
    }

    @Test
    void noOrNegativePoiseChangesNothingAndABackwardsClockIsSafe() {
        State s = new State(5f, 100, 0, 0);
        assertEquals(s, PoiseMath.add(s, 0f, 110, 30f, false).state());
        assertEquals(s, PoiseMath.add(s, -4f, 110, 30f, false).state());
        State odd = new State(5f, 100, 5000, 5000);
        assertFalse(PoiseMath.locked(odd, 100), "a lock thousands of ticks ahead is a clock that went backwards");
        assertFalse(PoiseMath.broken(odd, 100));
        assertEquals(5f, PoiseMath.current(new State(5f, 900, 0, 0), 100, 30f), 1e-6, "a stamp in the future counts as no idle time");
    }

    @Test
    void fillIsAShareOfTheCapacity() {
        State s = PoiseMath.add(State.EMPTY, 15f, 0, 30f, false).state();
        assertEquals(0.5f, PoiseMath.fill(s, 0, 30f), 1e-6);
        assertEquals(0f, PoiseMath.fill(s, 0, 0f), 1e-6);
    }
}
