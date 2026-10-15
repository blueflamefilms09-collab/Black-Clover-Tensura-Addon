package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The three acts: lines, hysteresis, once-only events, save and restore. */
class LuciusArcTest {
    @Test
    void rawPhasesFollowTheLines() {
        assertEquals(LuciusArc.MESSIAH, LuciusArc.raw(1.0f));
        assertEquals(LuciusArc.MESSIAH, LuciusArc.raw(0.65f), "65% itself is still act 1");
        assertEquals(LuciusArc.SOUL_CORRUPTOR, LuciusArc.raw(0.649f));
        assertEquals(LuciusArc.SOUL_CORRUPTOR, LuciusArc.raw(0.30f), "30% itself is still act 2");
        assertEquals(LuciusArc.CLONES, LuciusArc.raw(0.299f));
        assertEquals(LuciusArc.CLONES, LuciusArc.raw(0f));
    }

    @Test
    void theFightOpensInActOneAndEachActIsAnnouncedOnce() {
        LuciusArc a = new LuciusArc();
        assertEquals(LuciusArc.NONE, a.phase());
        assertEquals(LuciusArc.MESSIAH, a.update(1.0f), "first update enters act 1");
        assertEquals(LuciusArc.NONE, a.update(0.9f), "no change, no event");
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.update(0.64f));
        assertEquals(LuciusArc.NONE, a.update(0.64f));
        assertEquals(LuciusArc.CLONES, a.update(0.2f));
        assertEquals(LuciusArc.NONE, a.update(0.1f));
    }

    @Test
    void aBigHitStillAnnouncesEveryActInOrder() {
        LuciusArc a = new LuciusArc();
        assertEquals(LuciusArc.MESSIAH, a.update(0.9f));
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.update(0.1f), "one phase per call");
        assertEquals(LuciusArc.CLONES, a.update(0.1f));
        assertEquals(LuciusArc.CLONES, a.phase());
    }

    @Test
    void goingBackNeedsTheMarginAndNeverAnnouncesAgain() {
        LuciusArc a = new LuciusArc();
        a.update(1f);
        a.update(0.5f);
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.phase());
        assertEquals(LuciusArc.NONE, a.update(0.66f), "just over the line is not enough");
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.phase());
        assertEquals(LuciusArc.NONE, a.update(0.69f), "back in act 1: no event");
        assertEquals(LuciusArc.MESSIAH, a.phase());
        assertEquals(LuciusArc.NONE, a.update(0.60f), "act 2 was announced before: silent");
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.phase());
    }

    @Test
    void hysteresisStopsFlapping() {
        LuciusArc a = new LuciusArc();
        a.update(1f);
        a.update(0.64f);
        int changes = 0;
        int last = a.phase();
        for (int i = 0; i < 100; i++) {
            a.update(i % 2 == 0 ? 0.64f : 0.66f);
            if (a.phase() != last) { changes++; last = a.phase(); }
        }
        assertEquals(0, changes);
    }

    @Test
    void saveAndRestoreRoundTrip() {
        LuciusArc a = new LuciusArc();
        a.update(1f);
        a.update(0.5f);
        int[] s = a.save();
        LuciusArc b = LuciusArc.restore(s[0], s[1]);
        assertEquals(LuciusArc.SOUL_CORRUPTOR, b.phase());
        assertTrue(b.entered(LuciusArc.MESSIAH));
        assertTrue(b.entered(LuciusArc.SOUL_CORRUPTOR));
        assertFalse(b.entered(LuciusArc.CLONES));
        assertEquals(LuciusArc.NONE, b.update(0.5f), "a restored fight does not re-announce its act");
        assertEquals(LuciusArc.CLONES, b.update(0.1f));
    }

    @Test
    void restoreOfGarbageIsSafe() {
        LuciusArc a = LuciusArc.restore(99, -1);
        assertEquals(LuciusArc.CLONES, a.phase());
        assertTrue(a.entered(LuciusArc.MESSIAH) && a.entered(LuciusArc.SOUL_CORRUPTOR) && a.entered(LuciusArc.CLONES));
        LuciusArc b = LuciusArc.restore(-5, 0);
        assertEquals(LuciusArc.NONE, b.phase());
        assertEquals(LuciusArc.NONE, b.update(Float.NaN));
    }

    @Test
    void resetStartsAFreshFight() {
        LuciusArc a = new LuciusArc();
        a.update(1f);
        a.update(0.1f);
        a.reset();
        assertEquals(LuciusArc.NONE, a.phase());
        assertEquals(LuciusArc.MESSIAH, a.update(1f));
    }
}
