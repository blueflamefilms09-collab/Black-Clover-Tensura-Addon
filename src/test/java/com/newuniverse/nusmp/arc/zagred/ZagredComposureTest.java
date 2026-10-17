package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the rule that keeps him from being stun-locked (a cut buys one window and then locks the cut out) is the fairness of the whole fight, so it is tested on its own. */
class ZagredComposureTest {
    @Test
    void aCutSilencesHimForThreeSeconds() {
        ZagredComposure c = new ZagredComposure();
        assertFalse(c.silenced(0));
        assertTrue(c.cut(100));
        assertTrue(c.silenced(100));
        assertTrue(c.silenced(100 + ZagredComposure.CUT_SILENCE - 1));
        assertFalse(c.silenced(100 + ZagredComposure.CUT_SILENCE));
        assertEquals(ZagredComposure.CUT_SILENCE, c.silenceLeft(100));
    }

    @Test
    void aSecondCutIsLockedOutUntilTheLockoutEnds() {
        ZagredComposure c = new ZagredComposure();
        assertTrue(c.cut(0));
        long end = ZagredComposure.CUT_SILENCE + ZagredComposure.LOCKOUT;
        assertFalse(c.cut(10), "inside the silence");
        assertFalse(c.cut(ZagredComposure.CUT_SILENCE + 5), "he has recovered but is still locked out");
        assertTrue(c.locked(end - 1));
        assertFalse(c.locked(end));
        assertTrue(c.cut(end), "after the lockout a cut works again");
    }

    @Test
    void aFailedCutChangesNothing() {
        ZagredComposure c = new ZagredComposure();
        c.cut(0);
        long left = c.silenceLeft(20);
        assertFalse(c.cut(20));
        assertEquals(left, c.silenceLeft(20));
    }

    @Test
    void anEmptiedWardSilencesHimEvenWhenLockedOutAndLocksTheNextCut() {
        ZagredComposure c = new ZagredComposure();
        c.cut(0);
        c.unwrite(30, ZagredWard.REFORM_TICKS);
        assertTrue(c.silenced(30 + ZagredWard.REFORM_TICKS - 1));
        assertFalse(c.silenced(30 + ZagredWard.REFORM_TICKS));
        assertFalse(c.cut(30 + ZagredWard.REFORM_TICKS + 10), "the lockout follows the unwritten window");
        assertTrue(c.cut(30 + ZagredWard.REFORM_TICKS + ZagredComposure.LOCKOUT));
    }

    @Test
    void resetClearsEverything() {
        ZagredComposure c = new ZagredComposure();
        c.cut(0);
        c.reset();
        assertFalse(c.silenced(1));
        assertFalse(c.locked(1));
        assertTrue(c.cut(1));
    }
}
