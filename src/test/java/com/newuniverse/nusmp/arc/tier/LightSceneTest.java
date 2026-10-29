package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 3.36.0: the pure state machine of Light Yagami's Death Note scene: the beats, the timing, what cancels it, the lethal setting, creative skip. */
class LightSceneTest {
    private static final LightScene.Obs FINE = new LightScene.Obs(true, 0.0, true, true, 6.0, true);

    private static LightScene scene() { return new LightScene(UUID.randomUUID(), UUID.randomUUID()); }

    private static LightScene.Obs with(boolean lightAlive, double moved, boolean present, boolean eligible, double dist, boolean lethal) {
        return new LightScene.Obs(lightAlive, moved, present, eligible, dist, lethal);
    }

    @Test
    void theBeatsFollowEachOtherAtTheStatedTicks() {
        LightScene s = scene();
        assertEquals(LightScene.Phase.CHIP, s.phase());
        int raiseAt = -1, writeAt = -1;
        for (int i = 1; i < LightScene.TOTAL_TICKS; i++) {
            LightScene.Step step = s.tick(FINE);
            if (step == LightScene.Step.PHASE_CHANGED) {
                if (s.phase() == LightScene.Phase.RAISE) raiseAt = i;
                if (s.phase() == LightScene.Phase.WRITE) writeAt = i;
            } else {
                assertEquals(LightScene.Step.CONTINUE, step, "tick " + i);
            }
        }
        assertEquals(LightScene.CHIP_TICKS, raiseAt);
        assertEquals(LightScene.CHIP_TICKS + LightScene.RAISE_TICKS, writeAt);
        assertEquals(LightScene.Phase.WRITE, s.phase());
        assertTrue(s.running());
    }

    @Test
    void theWritingTakesAFewSecondsAndTheWholeSceneUnderTenSeconds() {
        assertEquals(80, LightScene.WRITE_TICKS);
        assertTrue(LightScene.WRITE_TICKS >= 60 && LightScene.WRITE_TICKS <= 120, "a few seconds of writing");
        assertTrue(LightScene.TOTAL_TICKS <= 200, "a scene of a few seconds, not a cutscene that locks the player");
        assertEquals(LightScene.TOTAL_TICKS, LightScene.CHIP_TICKS + LightScene.RAISE_TICKS + LightScene.WRITE_TICKS);
    }

    @Test
    void finishingKillsWhenTheSettingIsOn() {
        LightScene s = scene();
        LightScene.Step last = LightScene.Step.CONTINUE;
        int n = 0;
        while (s.running()) { last = s.tick(FINE); n++; assertTrue(n <= LightScene.TOTAL_TICKS); }
        assertEquals(LightScene.Step.FINISH_KILL, last);
        assertEquals(LightScene.TOTAL_TICKS, n);
        assertEquals(LightScene.Phase.DONE, s.phase());
        assertEquals(LightScene.Cancel.NONE, s.cancelReason());
    }

    @Test
    void settingOffMeansAHarmlessJokeEnding() {
        LightScene s = scene();
        LightScene.Step last = LightScene.Step.CONTINUE;
        while (s.running()) last = s.tick(with(true, 0, true, true, 6, false));
        assertEquals(LightScene.Step.FINISH_JOKE, last);
    }

    @Test
    void theSettingIsReadOnTheLastTickNotWhenTheSceneStarted() {
        LightScene s = scene();
        for (int i = 0; i < LightScene.TOTAL_TICKS - 1; i++) assertNotEquals(LightScene.Step.CANCELLED, s.tick(with(true, 0, true, true, 6, true)));
        assertEquals(LightScene.Step.FINISH_JOKE, s.tick(with(true, 0, true, true, 6, false)));        // an operator turned it off mid-scene
    }

    @Test
    void killingLightCancelsAtAnyMomentBeforeTheEnd() {
        for (int at : new int[]{0, 10, LightScene.CHIP_TICKS, LightScene.CHIP_TICKS + 5, LightScene.TOTAL_TICKS - 1}) {
            LightScene s = scene();
            for (int i = 0; i < at; i++) s.tick(FINE);
            assertEquals(LightScene.Step.CANCELLED, s.tick(with(false, 0, true, true, 6, true)), "at " + at);
            assertEquals(LightScene.Cancel.LIGHT_DIED, s.cancelReason());
            assertFalse(s.running());
        }
    }

    @Test
    void lightDyingWinsOverEverythingElse() {
        LightScene s = scene();
        s.tick(with(false, 99, false, false, 99, true));
        assertEquals(LightScene.Cancel.LIGHT_DIED, s.cancelReason());
    }

    @Test
    void knockingLightAwayCancels() {
        LightScene s = scene();
        for (int i = 0; i < 100; i++) s.tick(FINE);
        assertEquals(LightScene.Step.CANCELLED, s.tick(with(true, LightScene.KNOCK_BLOCKS + 0.1, true, true, 6, true)));
        assertEquals(LightScene.Cancel.KNOCKED_AWAY, s.cancelReason());
        // a small shuffle is not a knock
        LightScene t = scene();
        assertNotEquals(LightScene.Step.CANCELLED, t.tick(with(true, LightScene.KNOCK_BLOCKS - 0.1, true, true, 6, true)));
    }

    @Test
    void aHardHitStaggersAndAScratchDoesNot() {
        assertTrue(LightScene.staggers(6f, 28f));
        assertFalse(LightScene.staggers(2f, 28f));
        assertFalse(LightScene.staggers(5f, 0f));
        LightScene s = scene();
        s.tick(FINE);
        s.interrupt(LightScene.Cancel.KNOCKED_AWAY);
        assertEquals(LightScene.Phase.CANCELLED, s.phase());
        assertEquals(LightScene.Step.CONTINUE, s.tick(FINE), "a cancelled scene does nothing more");
    }

    @Test
    void theTargetLeavingCancels() {
        LightScene gone = scene();
        assertEquals(LightScene.Step.CANCELLED, gone.tick(with(true, 0, false, false, 0, true)));
        assertEquals(LightScene.Cancel.TARGET_GONE, gone.cancelReason());
        LightScene far = scene();
        assertEquals(LightScene.Step.CANCELLED, far.tick(with(true, 0, true, true, LightScene.MAX_DISTANCE + 1, true)));
        assertEquals(LightScene.Cancel.TARGET_ESCAPED, far.cancelReason());
    }

    @Test
    void aCreativeOrSpectatorTargetNeverGetsToTheEnd() {
        LightScene s = scene();
        assertEquals(LightScene.Step.CANCELLED, s.tick(with(true, 0, true, false, 6, true)));
        assertEquals(LightScene.Cancel.TARGET_INELIGIBLE, s.cancelReason());
        LightScene late = scene();
        for (int i = 0; i < LightScene.TOTAL_TICKS - 1; i++) late.tick(FINE);
        assertEquals(LightScene.Step.CANCELLED, late.tick(with(true, 0, true, false, 6, true)), "switching to creative on the last tick still saves the player");
    }

    @Test
    void anOutsideInterruptEndsTheSceneOnceAndKeepsTheFirstReason() {
        LightScene s = scene();
        s.interrupt(LightScene.Cancel.TARGET_GONE);
        s.interrupt(LightScene.Cancel.LIGHT_DIED);
        assertEquals(LightScene.Cancel.TARGET_GONE, s.cancelReason());
        LightScene done = scene();
        while (done.running()) done.tick(FINE);
        done.interrupt(LightScene.Cancel.TARGET_GONE);
        assertEquals(LightScene.Phase.DONE, done.phase(), "a scene that finished cannot be cancelled afterwards");
        assertEquals(LightScene.Cancel.NONE, done.cancelReason());
    }

    @Test
    void theClientTimelineAgreesWithTheServerOne() {
        assertEquals(LightScene.Phase.CHIP, LightScene.phaseAt(0));
        assertEquals(LightScene.Phase.CHIP, LightScene.phaseAt(LightScene.CHIP_TICKS - 1));
        assertEquals(LightScene.Phase.RAISE, LightScene.phaseAt(LightScene.CHIP_TICKS));
        assertEquals(LightScene.Phase.WRITE, LightScene.phaseAt(LightScene.CHIP_TICKS + LightScene.RAISE_TICKS));
        assertEquals(LightScene.Phase.DONE, LightScene.phaseAt(LightScene.TOTAL_TICKS));
        assertEquals(0f, LightScene.writeProgress(0));
        assertEquals(0f, LightScene.writeProgress(LightScene.CHIP_TICKS + LightScene.RAISE_TICKS));
        assertEquals(1f, LightScene.writeProgress(LightScene.TOTAL_TICKS));
        assertEquals(1f, LightScene.writeProgress(10_000));
    }
}
