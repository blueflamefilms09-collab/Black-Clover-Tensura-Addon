package com.newuniverse.nusmp.anim;

import com.newuniverse.nusmp.anim.CastHold.Kind;
import com.newuniverse.nusmp.anim.CastHold.State;
import com.newuniverse.nusmp.anim.CastHold.Step;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The held cast key is one continuous "active casting" state, not a stream of new circles. */
class CastHoldTest {
    private static final int CAST = 20;

    /** Plays a hold of 'ticks' ticks and counts the messages by kind (index = Kind ordinal). */
    private static int[] hold(int ticks, boolean allowed) {
        int[] n = new int[Kind.values().length];
        State s = CastHold.press();
        for (int t = 1; t <= ticks; t++) {
            Step st = CastHold.held(s, 1000 + t, Math.min(CAST, t), CAST, allowed);
            n[st.kind().ordinal()]++;
            s = st.next();
        }
        return n;
    }

    @Test
    void aLongHoldStartsOnceAndNeverRespawns() {
        int[] n = hold(200, true);                                          // the maximum hold of a grimoire page
        assertEquals(1, n[Kind.START.ordinal()], "one circle for the whole hold");
        assertEquals(CastHold.LEVELS - 1, n[Kind.LEVEL.ordinal()], "the charge steps grow the same circle");
        assertEquals((200 - CAST) / CastHold.RENEW_EVERY, n[Kind.RENEW.ordinal()] , "then only the keep-alive");
        int messages = n[Kind.START.ordinal()] + n[Kind.LEVEL.ordinal()] + n[Kind.RENEW.ordinal()];
        assertTrue(messages <= 12, "at most a dozen messages in ten seconds, not one per four ticks (50): " + messages);
    }

    @Test
    void aBlockedPageNeverShowsACircle() {
        int[] n = hold(200, false);
        assertEquals(0, n[Kind.START.ordinal()] + n[Kind.LEVEL.ordinal()] + n[Kind.RENEW.ordinal()] + n[Kind.END.ordinal()]);
        Step tap = CastHold.release(CastHold.IDLE, 5000, false);
        assertEquals(Kind.NONE, tap.kind(), "a release on cooldown shows no release circle either");
    }

    @Test
    void theFirstTickIsNotACircleYet() {
        assertEquals(Kind.NONE, CastHold.held(CastHold.IDLE, 10, 0, CAST, true).kind());
        assertEquals(Kind.NONE, CastHold.held(CastHold.IDLE, 10, 1, CAST, true).kind(), "a one tick tap is not a hold");
        Step first = CastHold.held(CastHold.IDLE, 10, 2, CAST, true);
        assertEquals(Kind.START, first.kind());
        assertEquals(1, first.level(), "it opens at the smallest size");
    }

    @Test
    void releaseEndsALiveHoldAndTapsGetTheirOneCircle() {
        State s = CastHold.held(CastHold.IDLE, 100, 6, CAST, true).next();
        Step end = CastHold.release(s, 110, true);
        assertEquals(Kind.END, end.kind());
        assertFalse(end.next().active());
        assertEquals(Kind.END, CastHold.release(s, 110, false).kind(), "the hold ends even while an earlier cast is winding up");
        assertEquals(Kind.TAP, CastHold.release(CastHold.IDLE, 110, true).kind(), "a tap keeps its single release circle");
        assertEquals(Kind.NONE, CastHold.release(CastHold.IDLE, 110, false).kind());
        assertEquals(Kind.NONE, CastHold.release(end.next(), 111, false).kind(), "a second release is silent");
    }

    @Test
    void aPageThatBecomesBlockedMidHoldEndsTheCircleOnce() {
        State s = CastHold.held(CastHold.IDLE, 100, 6, CAST, true).next();
        Step a = CastHold.held(s, 101, 7, CAST, false);
        assertEquals(Kind.END, a.kind());
        assertEquals(Kind.NONE, CastHold.held(a.next(), 102, 8, CAST, false).kind());
    }

    @Test
    void aStateTheClientHasAlreadyDroppedStartsAgain() {
        State s = CastHold.held(CastHold.IDLE, 100, 6, CAST, true).next();
        Step later = CastHold.held(s, 100 + CastHold.LIFE + 1, 20, CAST, true);
        assertEquals(Kind.START, later.kind(), "no news for LIFE ticks: the client dropped it, so it is opened again");
        assertEquals(Kind.TAP, CastHold.release(s, 100 + CastHold.LIFE + 1, true).kind(), "a stale hold is not ended, it is a tap");
    }

    @Test
    void keepAliveComesWellBeforeTheClientGivesUp() {
        assertTrue(CastHold.RENEW_EVERY + 5 < CastHold.LIFE);
        State s = CastHold.held(CastHold.IDLE, 100, CAST, CAST, true).next();      // already full
        assertEquals(Kind.NONE, CastHold.held(s, 100 + CastHold.RENEW_EVERY - 1, CAST, CAST, true).kind());
        Step r = CastHold.held(s, 100 + CastHold.RENEW_EVERY, CAST, CAST, true);
        assertEquals(Kind.RENEW, r.kind());
        assertEquals(100 + CastHold.RENEW_EVERY, r.next().sentAt());
    }

    @Test
    void levelsAreRoundedUpAndClamped() {
        assertEquals(0, CastHold.levelOf(0, CAST));
        assertEquals(1, CastHold.levelOf(1, CAST));
        assertEquals(1, CastHold.levelOf(5, CAST));
        assertEquals(2, CastHold.levelOf(6, CAST));
        assertEquals(4, CastHold.levelOf(20, CAST));
        assertEquals(4, CastHold.levelOf(500, CAST));
        assertEquals(0, CastHold.levelOf(5, 0), "a zero cast time never divides by zero");
        assertTrue(CastHold.power(1) < CastHold.power(4));
        assertEquals(1.15f, CastHold.power(4), 1e-6f);
        assertEquals(CastHold.power(4), CastHold.power(99), "out of range is clamped");
    }

    @Test
    void aRepeatedPressDoesNotRestartTheChant() {
        assertFalse(CastHold.pressAccepted(100, 101));
        assertFalse(CastHold.pressAccepted(100, 102));
        assertTrue(CastHold.pressAccepted(100, 103));
        assertTrue(CastHold.pressAccepted(Long.MIN_VALUE / 4, 5));
        assertEquals(13, CastHold.endDuration(7), "the 7 tick wind-up plus a 6 tick fade");
        assertEquals(7, CastHold.endDuration(0));
        assertEquals(6, CastHold.END_TAIL, "CastHoldLayer fades over exactly this");
    }
}
