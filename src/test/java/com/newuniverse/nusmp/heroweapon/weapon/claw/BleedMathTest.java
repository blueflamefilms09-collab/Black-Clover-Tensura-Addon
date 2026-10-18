package com.newuniverse.nusmp.heroweapon.weapon.claw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.claw.BleedMath.State;
import org.junit.jupiter.api.Test;

/**
 * Why: the bleed is the Claw's core loop, so its arithmetic is pinned: wounds add up to the limit, a new wound renews the time, a closed wound counts as none, the pulse and the
 * burst grow with the wounds, and a clock that went backwards cannot keep a foe bleeding for ever.
 */
class BleedMathTest {
    @Test
    void woundsAddUpToTheLimit() {
        State s = BleedMath.add(State.EMPTY, 1, BleedMath.MAX_STACKS, 100, BleedMath.TICKS);
        assertEquals(1, BleedMath.stacks(s, 100));
        s = BleedMath.add(s, 2, BleedMath.MAX_STACKS, 110, BleedMath.TICKS);
        assertEquals(3, BleedMath.stacks(s, 110));
        s = BleedMath.add(s, 9, BleedMath.MAX_STACKS, 120, BleedMath.TICKS);
        assertEquals(BleedMath.MAX_STACKS, BleedMath.stacks(s, 120), "capped");
        s = BleedMath.add(s, 9, BleedMath.MAX_STACKS_FANGS, 125, BleedMath.TICKS_FANGS);
        assertEquals(BleedMath.MAX_STACKS_FANGS, BleedMath.stacks(s, 125), "the Dragon Claws hold more");
    }

    @Test
    void aNewWoundRenewsTheTimeAndAClosedWoundIsNone() {
        State s = BleedMath.add(State.EMPTY, 2, 5, 0, 120);
        assertTrue(BleedMath.active(s, 119));
        assertFalse(BleedMath.active(s, 120));
        assertEquals(0, BleedMath.stacks(s, 500));
        State renewed = BleedMath.add(s, 1, 5, 100, 120);
        assertEquals(220, renewed.until());
        assertEquals(3, BleedMath.stacks(renewed, 150));
        State afterClosing = BleedMath.add(s, 1, 5, 300, 120);
        assertEquals(1, BleedMath.stacks(afterClosing, 300), "the old wounds closed: only the new one is open");
    }

    @Test
    void addingNothingChangesNothing() {
        State s = BleedMath.add(State.EMPTY, 2, 5, 0, 120);
        assertEquals(s, BleedMath.add(s, 0, 5, 50, 120));
        assertEquals(s, BleedMath.add(s, -3, 5, 50, 120));
    }

    @Test
    void thePulseAndTheBurstGrowWithTheWounds() {
        assertEquals(0f, BleedMath.pulse(0), 1e-6);
        assertEquals(0f, BleedMath.pulse(-2), 1e-6);
        assertEquals(4.5f, BleedMath.pulse(5), 1e-5);
        assertEquals(0f, BleedMath.burst(0), 1e-6);
        assertEquals(4.6f, BleedMath.burst(1), 1e-5);
        assertEquals(15f, BleedMath.burst(5), 1e-5, "2 + 2.6 x 5 = 15");
        assertEquals(15f, BleedMath.burst(7), 1e-5, "capped");
    }

    @Test
    void aClockThatWentBackwardsDoesNotKeepAFoeBleeding() {
        State odd = new State(3, 5000);
        assertFalse(BleedMath.active(odd, 100), "until is thousands of ticks ahead");
        assertEquals(0, BleedMath.stacks(odd, 100));
    }

    @Test
    void aChainRunsOutAfterItsLimit() {
        assertFalse(BleedMath.chainOver(1000, 1000 + BleedMath.CHAIN_MAX - 1));
        assertTrue(BleedMath.chainOver(1000, 1000 + BleedMath.CHAIN_MAX));
    }
}
