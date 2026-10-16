package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 1.45.0: the stage-driven mode of the acts. */
class LuciusArcStageTest {
    @Test
    void theActIsTheStageAndEachIsAnnouncedOnce() {
        LuciusArc a = new LuciusArc();
        assertEquals(LuciusArc.MESSIAH, a.enterStage(1));
        assertEquals(LuciusArc.NONE, a.enterStage(1));
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.enterStage(2));
        assertEquals(LuciusArc.CLONES, a.enterStage(3));
        assertEquals(LuciusArc.CLONES, a.phase());
    }

    @Test
    void fullHealthInTheNewStageDoesNotPullTheActBack() {
        LuciusArc a = new LuciusArc();
        a.enterStage(1);
        a.enterStage(2);
        assertEquals(LuciusArc.NONE, a.enterStage(2), "a refilled bar at 100% is still stage 2");
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.phase());
    }

    @Test
    void goingBackIsAnnouncedOnlyIfNeverEnteredAndOutOfRangeIsClamped() {
        LuciusArc a = new LuciusArc();
        a.enterStage(1);
        a.enterStage(3);
        assertEquals(LuciusArc.NONE, a.enterStage(1), "a regress to an act already announced is silent");
        assertEquals(LuciusArc.NONE, a.enterStage(3), "and so is the way forward again");
        a.reset();
        assertEquals(LuciusArc.CLONES, a.enterStage(9));
        a.reset();
        assertEquals(LuciusArc.MESSIAH, a.enterStage(-4));
    }

    @Test
    void theThresholdModeIsUntouched() {
        LuciusArc a = new LuciusArc();
        assertEquals(LuciusArc.MESSIAH, a.update(1f));
        assertEquals(LuciusArc.SOUL_CORRUPTOR, a.update(0.5f));
    }

    @Test
    void saveAndRestoreWorkForStageMode() {
        LuciusArc a = new LuciusArc();
        a.enterStage(1);
        a.enterStage(2);
        int[] s = a.save();
        LuciusArc b = LuciusArc.restore(s[0], s[1]);
        assertEquals(LuciusArc.SOUL_CORRUPTOR, b.phase());
        assertTrue(b.entered(LuciusArc.MESSIAH) && b.entered(LuciusArc.SOUL_CORRUPTOR));
        assertFalse(b.entered(LuciusArc.CLONES));
    }
}
