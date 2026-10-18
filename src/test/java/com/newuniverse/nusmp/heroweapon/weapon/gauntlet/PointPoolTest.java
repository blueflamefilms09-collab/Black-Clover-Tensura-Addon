package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Why: the skill points are the Gauntlet's whole rhythm (fight to fill them, spend them on the next technique), so the arithmetic is pinned: a new pool is
 * empty, gains stop at the cap, a spend takes what is there and no more, the points fade one at a time only after the idle time and a gain or a spend restarts
 * that clock, and clearing empties the pool.
 */
class PointPoolTest {
    @Test
    void gainsStopAtTheCapAndSpendsTakeOnlyWhatIsThere() {
        PointPool p = new PointPool();
        assertEquals(0, p.points(0));
        assertEquals(0, p.spend(3, 0), "an empty pool spends nothing");
        p.gain(4, 0);
        assertEquals(4, p.points(0));
        p.gain(50, 0);
        assertEquals(PointPool.MAX, p.points(0));
        assertEquals(3, p.spend(3, 0));
        assertEquals(PointPool.MAX - 3, p.points(0));
        assertEquals(PointPool.MAX - 3, p.spend(99, 0), "a spend never takes more than the pool holds");
        assertEquals(0, p.points(0));
        p.gain(0, 0);
        p.gain(-2, 0);
        assertEquals(0, p.points(0), "a zero or negative gain does nothing");
    }

    @Test
    void pointsFadeOneAtATimeOnlyAfterTheIdleTime() {
        PointPool p = new PointPool();
        p.gain(5, 1000);
        assertEquals(5, p.points(1000 + PointPool.IDLE), "at the end of the idle time nothing has faded yet");
        assertEquals(5, p.points(1000 + PointPool.IDLE + PointPool.FADE_EVERY - 1));
        assertEquals(4, p.points(1000 + PointPool.IDLE + PointPool.FADE_EVERY));
        assertEquals(3, p.points(1000 + PointPool.IDLE + 2 * PointPool.FADE_EVERY));
        assertEquals(0, p.points(1000 + PointPool.IDLE + 99 * PointPool.FADE_EVERY), "they fade to zero and no further");
    }

    @Test
    void aGainOrASpendRestartsTheIdleClock() {
        PointPool p = new PointPool();
        p.gain(5, 0);
        long late = PointPool.IDLE + 2 * PointPool.FADE_EVERY;
        assertEquals(3, p.points(late));
        p.gain(1, late);
        assertEquals(4, p.points(late), "the faded points are gone for good and the new one is added");
        assertEquals(4, p.points(late + PointPool.IDLE), "the clock restarted at the gain");
        assertEquals(3, p.points(late + PointPool.IDLE + PointPool.FADE_EVERY));
        p.spend(1, late + 30);
        assertEquals(3, p.points(late + 30 + PointPool.IDLE), "a spend also restarts the clock");
    }

    @Test
    void clearEmptiesThePool() {
        PointPool p = new PointPool();
        p.gain(8, 10);
        p.clear();
        assertEquals(0, p.points(10));
        p.gain(2, 11);
        assertEquals(2, p.points(11));
    }
}
