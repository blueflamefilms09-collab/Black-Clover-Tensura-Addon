package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Active Time Reversal: the burst window, the channel, the interrupt, the restore bounds and the cooldown. */
class TimeReversalMathTest {
    private static final float MAX = 1000f;

    @Test
    void aBurstIsEighteenPercentInOneHundredTicks() {
        TimeReversalMath.Window w = new TimeReversalMath.Window();
        for (int t = 0; t < 100; t += 2) w.record(t, 600f - t);       // a slow loss: 100 hp in 100 ticks
        assertFalse(w.burst(100, 500f, MAX));
        assertEquals(600f, w.highest(100));
        w.record(100, 410f);
        assertTrue(w.burst(100, 410f, MAX), "190 lost");
        assertFalse(w.burst(100, 430f, MAX), "170 lost is not enough");
        assertTrue(w.burst(100, 420f, MAX), "exactly 18% is a burst");
        assertFalse(w.burst(100, 421f, MAX), "just under 18% is not");
    }

    @Test
    void theWindowForgetsOldHealth() {
        TimeReversalMath.Window w = new TimeReversalMath.Window();
        w.record(0, 900f);
        for (int t = 2; t <= 300; t += 2) w.record(t, 700f);
        assertEquals(700f, w.highest(300), "the 900 was more than 100 ticks ago");
        assertFalse(w.burst(300, 650f, MAX));
    }

    @Test
    void anEmptyWindowIsNoBurst() {
        TimeReversalMath.Window w = new TimeReversalMath.Window();
        assertTrue(Float.isNaN(w.highest(5)));
        assertFalse(w.burst(5, 0f, MAX));
    }

    @Test
    void heavyHitsBreakTheChannel() {
        assertTrue(TimeReversalMath.heavy(60f, MAX, false));
        assertFalse(TimeReversalMath.heavy(59f, MAX, false));
        assertTrue(TimeReversalMath.heavy(30f, MAX, true), "a Legend relic needs half as much");
        assertFalse(TimeReversalMath.heavy(29f, MAX, true));
        assertFalse(TimeReversalMath.heavy(0f, MAX, true));
        assertFalse(TimeReversalMath.heavy(100f, 0f, true));
    }

    @Test
    void restoreIsBoundedByTheStartTheActAndTheCurrentHealth() {
        assertEquals(700f, TimeReversalMath.restoreTarget(700f, 500f, MAX, 640f + 100f));
        assertEquals(640f, TimeReversalMath.restoreTarget(700f, 500f, MAX, 640f), "never above the act's top");
        assertEquals(550f, TimeReversalMath.restoreTarget(500f, 550f, MAX, 640f), "never lower than now");
        assertEquals(MAX, TimeReversalMath.restoreTarget(5000f, 10f, MAX, 5000f), "never above the maximum");
    }

    @Test
    void aCompletedChannelStartsTheCooldown() {
        TimeReversalMath.Channel c = new TimeReversalMath.Channel();
        assertTrue(c.ready(0));
        assertTrue(c.begin(100, 700f));
        assertTrue(c.channeling());
        assertFalse(c.begin(101, 700f), "one at a time");
        assertEquals(60, c.left(100));
        for (long t = 101; t < 160; t++) assertFalse(c.tick(t), "tick " + t);
        assertTrue(c.tick(160), "completes after 60 ticks");
        assertFalse(c.channeling());
        assertFalse(c.ready(160 + 499));
        assertTrue(c.ready(160 + 500));
    }

    @Test
    void anInterruptStaggersForSixSecondsAndTheDamageStays() {
        TimeReversalMath.Channel c = new TimeReversalMath.Channel();
        c.begin(100, 700f);
        assertTrue(c.interrupt(130));
        assertTrue(c.staggered(131));
        assertTrue(c.staggered(130 + 119));
        assertFalse(c.tick(131));
        assertFalse(c.tick(130 + 120));
        assertFalse(c.staggered(130 + 120));
        assertFalse(c.ready(130 + 499));
        assertTrue(c.ready(130 + 500));
        assertFalse(c.interrupt(200), "nothing to interrupt");
    }

    @Test
    void cannotBeginOnCooldown() {
        TimeReversalMath.Channel c = new TimeReversalMath.Channel();
        c.begin(0, 1f);
        c.interrupt(10);
        assertFalse(c.begin(100, 1f));
    }

    @Test
    void saveAndRestoreKeepEverything() {
        TimeReversalMath.Channel c = new TimeReversalMath.Channel();
        c.begin(100, 612.5f);
        TimeReversalMath.Channel d = new TimeReversalMath.Channel();
        d.restore(c.save());
        assertTrue(d.channeling());
        assertEquals(612.5f, d.startHealth());
        assertEquals(c.endsAt(), d.endsAt());
        assertTrue(d.tick(160));
        TimeReversalMath.Channel e = new TimeReversalMath.Channel();
        e.restore(new long[]{9, 1, 2});
        assertEquals(TimeReversalMath.State.IDLE, e.state());
    }
}
