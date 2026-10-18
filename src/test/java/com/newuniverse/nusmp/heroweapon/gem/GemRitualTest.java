package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the Choosing is a channel with several ways to end, and a wrong answer here is a player who loses a gem worth a nether star or one who is bound to a
 * weapon after being struck. The state machine is pure, so every ending is pinned: it completes on exactly the configured tick, a blow beats a step away
 * beats a covered sky, the tolerance is inclusive, and a result, once given, never changes.
 */
class GemRitualTest {
    private static GemRitual fresh() { return new GemRitual(100, 1.5); }

    private static void channel(GemRitual r, int ticks) {
        for (int i = 0; i < ticks; i++) assertEquals(GemRitual.Step.CONTINUE, r.tick(0.0, false, true), "tick " + (i + 1));
    }

    @Test
    void completesOnExactlyTheConfiguredTick() {
        GemRitual r = fresh();
        channel(r, 99);
        assertEquals(99, r.elapsed());
        assertEquals(GemRitual.State.CHANNELING, r.state());
        assertEquals(GemRitual.Step.COMPLETE, r.tick(0.0, false, true));
        assertEquals(GemRitual.State.COMPLETED, r.state());
        assertEquals(100, r.elapsed());
        assertEquals(0, r.remaining());
        assertEquals(0, r.secondsLeft());
        assertNull(r.reason());
        assertTrue(r.ended());
    }

    @Test
    void aCompletedRitualStaysCompletedWhateverHappensNext() {
        GemRitual r = fresh();
        channel(r, 99);
        r.tick(0.0, false, true);
        assertEquals(GemRitual.Step.COMPLETE, r.tick(50.0, true, false), "a late blow cannot undo it");
        assertEquals(GemRitual.Step.COMPLETE, r.interrupt(GemRitual.Interrupt.RELEASED), "a late release cannot undo it");
        assertEquals(GemRitual.State.COMPLETED, r.state());
        assertEquals(100, r.elapsed(), "and it does not count further");
    }

    @Test
    void aBlowEndsItEvenOnTheLastTickAndTheTickIsNotCounted() {
        GemRitual r = fresh();
        channel(r, 99);
        assertEquals(GemRitual.Step.INTERRUPT, r.tick(0.0, true, true));
        assertEquals(GemRitual.Interrupt.HURT, r.reason());
        assertEquals(99, r.elapsed());
        assertEquals(GemRitual.State.INTERRUPTED, r.state());
    }

    @Test
    void theChecksHaveAFixedOrder() {
        GemRitual all = fresh();
        assertEquals(GemRitual.Step.INTERRUPT, all.tick(99.0, true, false));
        assertEquals(GemRitual.Interrupt.HURT, all.reason(), "a blow first");
        GemRitual walk = fresh();
        walk.tick(99.0, false, false);
        assertEquals(GemRitual.Interrupt.MOVED, walk.reason(), "then a step away");
        GemRitual sky = fresh();
        sky.tick(0.0, false, false);
        assertEquals(GemRitual.Interrupt.CLOSED, sky.reason(), "then a covered sky");
    }

    @Test
    void theToleranceIsInclusive() {
        GemRitual r = fresh();
        assertEquals(GemRitual.Step.CONTINUE, r.tick(1.5, false, true), "exactly on the edge is still inside");
        assertEquals(GemRitual.Step.INTERRUPT, r.tick(1.5000001, false, true));
        assertEquals(GemRitual.Interrupt.MOVED, r.reason());
    }

    @Test
    void anInterruptedRitualNeverResumes() {
        GemRitual r = fresh();
        channel(r, 10);
        assertEquals(GemRitual.Step.INTERRUPT, r.interrupt(GemRitual.Interrupt.RELEASED));
        assertEquals(GemRitual.Interrupt.RELEASED, r.reason());
        for (int i = 0; i < 200; i++) assertEquals(GemRitual.Step.INTERRUPT, r.tick(0.0, false, true));
        assertEquals(10, r.elapsed());
        assertEquals(GemRitual.Step.INTERRUPT, r.interrupt(GemRitual.Interrupt.LOST), "and the first reason is kept");
        assertEquals(GemRitual.Interrupt.RELEASED, r.reason());
    }

    @Test
    void theCountdownRoundsUpToWholeSeconds() {
        GemRitual r = fresh();
        assertEquals(5, r.secondsLeft());
        channel(r, 1);
        assertEquals(5, r.secondsLeft());
        channel(r, 19);
        assertEquals(4, r.secondsLeft(), "twenty ticks in, four whole seconds left");
        channel(r, 60);
        assertEquals(1, r.secondsLeft());
        channel(r, 19);
        assertEquals(1, r.secondsLeft(), "one tick left still shows 1");
    }

    @Test
    void progressRisesFromZeroToOne() {
        GemRitual r = new GemRitual(4, 1.0);
        assertEquals(0.0, r.progress(), 1e-9);
        r.tick(0.0, false, true);
        assertEquals(0.25, r.progress(), 1e-9);
        r.tick(0.0, false, true);
        r.tick(0.0, false, true);
        r.tick(0.0, false, true);
        assertEquals(1.0, r.progress(), 1e-9);
    }

    @Test
    void badNumbersAreClamped() {
        GemRitual r = new GemRitual(0, -3.0);
        assertEquals(1, r.total());
        assertEquals(0.0, r.tolerance(), 1e-9);
        assertEquals(GemRitual.Step.COMPLETE, r.tick(0.0, false, true), "a one-tick ritual completes at once");
        GemRitual still = new GemRitual(5, 0.0);
        assertEquals(GemRitual.Step.CONTINUE, still.tick(0.0, false, true), "a zero tolerance means exactly in place");
        assertEquals(GemRitual.Step.INTERRUPT, still.tick(0.01, false, true));
    }

    @Test
    void everyInterruptReasonHasAKey() {
        for (GemRitual.Interrupt i : GemRitual.Interrupt.values()) assertTrue(!i.key().isEmpty() && i.key().equals(i.key().toLowerCase()), "key of " + i);
    }
}
