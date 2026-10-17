package com.newuniverse.nusmp.labyrinth.mega.flow;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the tracker is what turns "a hit landed" into "a hit that came from that cast", so its window must be exact at both ends (the tick of the cast
 * counts, the tick after the window does not), a later cast must replace an earlier one (a NEUTRAL spell ends the Light window), players must never see
 * each other's casts, and the cooldowns must hold for one player without touching another. Forgetting on logout and clearing on stop are pinned too,
 * because a stale cast kept past a relog would boost a hit nobody cast.
 */
class FlowTrackerTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a"), B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void aHitIsFromTheCastOnlyInsideTheWindow() {
        FlowTracker t = new FlowTracker();
        t.recordCast(A, Alignment.LIGHT, 100);
        assertEquals(Alignment.LIGHT, t.recentCast(A, 100, 30), "the tick of the cast counts");
        assertEquals(Alignment.LIGHT, t.recentCast(A, 115, 30));
        assertEquals(Alignment.LIGHT, t.recentCast(A, 130, 30), "exactly the window after the cast still counts");
        assertNull(t.recentCast(A, 131, 30), "one tick later it does not");
        assertNull(t.recentCast(A, 99, 30), "a cast stamped in the future (the clock was set back) is not recent");
        assertEquals(Alignment.LIGHT, t.recentCast(A, 100, 0), "a window of zero is the tick of the cast only");
        assertNull(t.recentCast(A, 101, 0));
        assertEquals(Alignment.LIGHT, t.recentCast(A, 100, -5), "a negative window behaves as zero");
        assertNull(t.recentCast(A, 101, -5));
    }

    @Test
    void aSpellThatHurtsInstantlyCountsForItsOwnTickOnly() {
        FlowTracker t = new FlowTracker();
        t.beginCast(A, Alignment.LIGHT, 200);
        assertEquals(Alignment.LIGHT, t.recentCast(A, 200, 30), "the damage of the tick the spell runs in comes from it");
        assertNull(t.recentCast(A, 201, 30), "but a spell that never reported success opens no window");
        assertNull(t.lastCast(A), "and remembers nothing");
        t.recordCast(A, Alignment.LIGHT, 200);
        assertEquals(Alignment.LIGHT, t.recentCast(A, 201, 30), "the success opens the window");
        assertEquals(Alignment.LIGHT, t.recentCast(A, 230, 30));
    }

    @Test
    void aRunningSpellOutranksAnOlderFinishedOne() {
        FlowTracker t = new FlowTracker();
        t.recordCast(A, Alignment.LIGHT, 100);
        t.beginCast(A, Alignment.NEUTRAL, 110);
        assertEquals(Alignment.NEUTRAL, t.recentCast(A, 110, 30), "a plain spell's instant damage is not the earlier Light cast's");
        assertEquals(Alignment.LIGHT, t.recentCast(A, 111, 30), "the failed plain spell changed nothing for the next tick");
        t.beginCast(A, Alignment.DARK, 112);
        t.recordCast(A, Alignment.DARK, 112);
        assertEquals(Alignment.DARK, t.recentCast(A, 112, 30), "the finished cast replaces the running one");
        assertEquals(Alignment.DARK, t.recentCast(A, 120, 30));
    }

    @Test
    void aRunningSpellBelongsToItsPlayerAndIsForgotten() {
        FlowTracker t = new FlowTracker();
        t.beginCast(A, Alignment.DARK, 50);
        assertNull(t.recentCast(B, 50, 30), "another player's damage is not from it");
        t.forget(A);
        assertNull(t.recentCast(A, 50, 30), "a relog forgets a running spell");
        t.beginCast(B, Alignment.LIGHT, 50);
        t.clear();
        assertNull(t.recentCast(B, 50, 30), "and so does a server stop");
        t.beginCast(null, Alignment.LIGHT, 50);
        t.beginCast(A, null, 50);
        assertNull(t.recentCast(A, 50, 30), "nothing is recorded for a null player or alignment");
        assertNull(t.recentCast(null, 50, 30));
    }

    @Test
    void aLaterCastReplacesAnEarlierOne() {
        FlowTracker t = new FlowTracker();
        t.recordCast(A, Alignment.LIGHT, 100);
        t.recordCast(A, Alignment.NEUTRAL, 110);
        assertEquals(Alignment.NEUTRAL, t.recentCast(A, 112, 30), "a neutral spell ends the light window");
        t.recordCast(A, Alignment.DARK, 120);
        FlowTracker.Cast last = t.lastCast(A);
        assertEquals(Alignment.DARK, last.alignment());
        assertEquals(120L, last.tick());
    }

    @Test
    void playersDoNotSeeEachOthersCasts() {
        FlowTracker t = new FlowTracker();
        t.recordCast(A, Alignment.LIGHT, 100);
        assertNull(t.recentCast(B, 101, 30));
        assertNull(t.lastCast(B));
        t.recordCast(B, Alignment.DARK, 101);
        assertEquals(Alignment.LIGHT, t.recentCast(A, 102, 30));
        assertEquals(Alignment.DARK, t.recentCast(B, 102, 30));
        assertEquals(2, t.size());
    }

    @Test
    void nullInputsAreHarmless() {
        FlowTracker t = new FlowTracker();
        t.recordCast(null, Alignment.LIGHT, 1);
        t.recordCast(A, null, 1);
        assertEquals(0, t.size());
        assertNull(t.recentCast(null, 1, 30));
        assertNull(t.lastCast(null));
        assertFalse(t.tryReject(null, 1, 20));
        assertFalse(t.effectDue(null, 1, 4));
        assertFalse(t.noticeDue(null, 1, 4));
        t.forget(null);
    }

    @Test
    void theRejectionCooldownHoldsPerPlayer() {
        FlowTracker t = new FlowTracker();
        assertTrue(t.tryReject(A, 1000, 20), "the first rejection is due");
        assertFalse(t.tryReject(A, 1000, 20), "the same tick: not again");
        assertFalse(t.tryReject(A, 1019, 20));
        assertTrue(t.tryReject(B, 1005, 20), "another player has a cooldown of their own");
        assertTrue(t.tryReject(A, 1020, 20), "exactly the cooldown later it is due again");
        assertFalse(t.tryReject(A, 1021, 20), "and the new rejection restarted the cooldown");
        assertTrue(t.tryReject(A, 1021, 0), "a zero cooldown never blocks");
    }

    @Test
    void effectsAndNoticesHaveTheirOwnPauses() {
        FlowTracker t = new FlowTracker();
        assertTrue(t.effectDue(A, 50, 4));
        assertFalse(t.effectDue(A, 53, 4));
        assertTrue(t.effectDue(A, 54, 4));
        assertTrue(t.noticeDue(A, 50, 100), "the notice pause is separate from the effect pause");
        assertFalse(t.noticeDue(A, 149, 100));
        assertTrue(t.noticeDue(A, 150, 100));
        assertTrue(t.tryReject(A, 50, 20), "and separate from the rejection cooldown");
    }

    @Test
    void forgetAndClearEmptyTheMemory() {
        FlowTracker t = new FlowTracker();
        t.recordCast(A, Alignment.LIGHT, 100);
        t.recordCast(B, Alignment.DARK, 100);
        assertTrue(t.tryReject(A, 100, 20));
        assertTrue(t.effectDue(A, 100, 4));
        assertTrue(t.noticeDue(A, 100, 100));
        t.forget(A);
        assertNull(t.lastCast(A));
        assertNull(t.recentCast(A, 101, 30), "a relog forgets the last cast");
        assertTrue(t.tryReject(A, 101, 20), "and the rejection cooldown");
        assertTrue(t.effectDue(A, 101, 4));
        assertTrue(t.noticeDue(A, 101, 100));
        assertNotNull(t.lastCast(B), "forgetting one player leaves the others");
        t.clear();
        assertEquals(0, t.size());
        assertNull(t.lastCast(B));
        assertTrue(t.tryReject(B, 101, 20));
    }

    @Test
    void theSharedInstanceIsOneObject() {
        assertSame(FlowTracker.shared(), FlowTracker.shared());
    }
}
