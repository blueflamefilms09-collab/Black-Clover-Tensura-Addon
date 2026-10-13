package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The charge pools of the wave 6 armours: dawn refills, spending, the dice (docs/doc_wave6.md). */
class Wave6PoolTest {
    @Test
    void aFreshPoolIsFullAndSpendingNeverGoesBelowZero() {
        Wave6Pool.State s = Wave6Pool.State.fresh(10, 5);
        assertEquals(10, s.charges());
        assertTrue(Wave6Pool.canSpend(s, 6));
        Wave6Pool.State left = Wave6Pool.spend(s, 6);
        assertEquals(4, left.charges());
        assertFalse(Wave6Pool.canSpend(left, 5));
        assertSame(left, Wave6Pool.spend(left, 5), "a pool that cannot pay is returned unchanged");
        assertFalse(Wave6Pool.canSpend(left, 0), "spending nothing is not a spend");
        assertEquals(0, new Wave6Pool.State(-3, 0).charges());
        Wave6Pool.State five = new Wave6Pool.State(5, 0);
        assertTrue(Wave6Pool.canSpend(five, 5), "the last charge can be spent");
        assertEquals(0, Wave6Pool.spend(five, 5).charges());
    }

    @Test
    void dawnRefillAddsTheGainOncePerMissedDayUpToTheMaximumAndFourDays() {
        Wave6Pool.State s = new Wave6Pool.State(0, 10);
        assertSame(s, Wave6Pool.rollDay(s, 10, 10, () -> 5), "the same day changes nothing");
        assertSame(s, Wave6Pool.rollDay(s, 9, 10, () -> 5), "a clock that runs backwards changes nothing");
        assertEquals(new Wave6Pool.State(5, 11), Wave6Pool.rollDay(s, 11, 10, () -> 5));
        assertEquals(new Wave6Pool.State(10, 12), Wave6Pool.rollDay(s, 12, 10, () -> 5), "two dawns, capped at the maximum");
        assertEquals(new Wave6Pool.State(4, 400), Wave6Pool.rollDay(s, 400, 10, () -> 1), "a long absence pays at most four dawns");
        assertEquals(new Wave6Pool.State(3, 11), Wave6Pool.rollDay(new Wave6Pool.State(3, 10), 11, 3, () -> 3), "a full pool stays full");
    }

    @Test
    void addNeverPassesTheMaximum() {
        assertEquals(5, Wave6Pool.add(new Wave6Pool.State(3, 0), 9, 5).charges());
        assertEquals(3, Wave6Pool.add(new Wave6Pool.State(3, 0), -4, 5).charges());
    }

    @Test
    void diceFollowTheRollerAndStayInRange() {
        assertEquals(3, Wave6Pool.dice(3, 6, sides -> 0), "three dice that all roll the lowest face");
        assertEquals(18, Wave6Pool.dice(3, 6, sides -> sides - 1));
        assertEquals(6, Wave6Pool.dice(1, 6, sides -> 99), "a roller that answers too high is clamped to the top face");
        assertEquals(1, Wave6Pool.dice(1, 6, sides -> -4));
    }

    @Test
    void everyPoolHasASaneMaximumAndADawnGain() {
        for (Wave6Pools p : Wave6Pools.values()) {
            assertTrue(p.max() >= 2 && p.max() <= 10, p.name());
            for (int face = 0; face < 20; face++) {
                int f = face;
                int g = p.gain(n -> Math.min(f, n - 1));
                assertTrue(g >= 1 && g <= p.max() + 2, p.name() + " gain " + g);
            }
        }
        assertEquals(10, Wave6Pools.SKY.max());
        assertEquals(3, Wave6Pools.DAWN.max());
        assertEquals(5, Wave6Pools.MIST.max());
    }
}
