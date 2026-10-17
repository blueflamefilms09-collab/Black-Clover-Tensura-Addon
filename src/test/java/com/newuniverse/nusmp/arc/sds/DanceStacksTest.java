package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Drole's Dance makes Diane stronger, so the cap and the fade of that power are held where a change that made it permanent cannot slip in. */
class DanceStacksTest {
    @Test
    void everyBeatAddsAStackUpToTheMaximum() {
        DanceStacks d = new DanceStacks();
        assertEquals(0, d.stacks(0));
        assertEquals(1f, d.multiplier(0), 1e-6f);
        for (int i = 1; i <= 5; i++) assertEquals(i, d.add(i * 20, 1));
        assertEquals(DanceStacks.MAX, d.add(120, 3), "capped");
        assertEquals(1f + DanceStacks.PER_STACK * DanceStacks.MAX, d.multiplier(120), 1e-6f);
        assertTrue(d.multiplier(120) <= 1.35f, "the dance never more than doubles anything: at most +30%");
    }

    @Test
    void theStacksFadeOneAtATimeAfterTheLastBeat() {
        DanceStacks d = new DanceStacks();
        d.add(100, 3);
        assertEquals(3, d.stacks(100 + DanceStacks.DECAY_TICKS - 1));
        assertEquals(2, d.stacks(100 + DanceStacks.DECAY_TICKS));
        assertEquals(1, d.stacks(100 + 2 * DanceStacks.DECAY_TICKS));
        assertEquals(0, d.stacks(100 + 3 * DanceStacks.DECAY_TICKS));
        assertEquals(0, d.stacks(100 + 50 * DanceStacks.DECAY_TICKS));
        assertEquals(1f, d.multiplier(100 + 50 * DanceStacks.DECAY_TICKS), 1e-6f);
    }

    @Test
    void aNewBeatRestartsTheFade() {
        DanceStacks d = new DanceStacks();
        d.add(0, 2);
        d.add(80, 1);
        assertEquals(3, d.stacks(100), "the first fade would have been at 100");
        assertEquals(3, d.stacks(179));
        assertEquals(2, d.stacks(180));
    }

    @Test
    void aBrokenDanceTakesBackItsStacksAndNoMoreThanSheHas() {
        DanceStacks d = new DanceStacks();
        d.add(0, 2);
        d.add(10, 3);
        assertEquals(5, d.stacks(10));
        assertEquals(2, d.remove(11, 3));
        assertEquals(0, d.remove(12, 9), "never below nothing");
        assertEquals(0, d.stacks(13));
        d.add(20, 4);
        d.clear();
        assertEquals(0, d.stacks(21));
    }

    @Test
    void addingNothingChangesNothing() {
        DanceStacks d = new DanceStacks();
        assertEquals(0, d.add(5, 0));
        assertEquals(0, d.add(5, -4));
        assertEquals(0, d.stacks(6));
    }
}
