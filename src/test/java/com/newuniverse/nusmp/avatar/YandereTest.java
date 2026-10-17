package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.yandere.YandereFrenzyState;
import com.newuniverse.nusmp.avatar.yandere.YandereMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Yandere promises: it warns first, it never kills itself, it never kills a player, the frenzy always ends. */
class YandereTest {
    // ---------------------------------------------------------------- jealousy timeline
    @Test
    void itAlwaysWarnsBeforeAnythingElse() {
        assertEquals(YandereMath.Phase.NONE, YandereMath.phase(0, false));
        assertEquals(YandereMath.Phase.NONE, YandereMath.phase(-5, true));
        for (int t = 1; t < YandereMath.WARN_TICKS; t++) {
            assertEquals(YandereMath.Phase.WARN, YandereMath.phase(t, false), "tick " + t);
            assertEquals(YandereMath.Phase.WARN, YandereMath.phase(t, true), "even a hostile intruder is warned first, tick " + t);
        }
    }

    @Test
    void thenItBlocksAndOnlyAttacksForHostilityOrThreeSeconds() {
        assertEquals(YandereMath.Phase.BLOCK, YandereMath.phase(YandereMath.WARN_TICKS, false));
        assertEquals(YandereMath.Phase.BLOCK, YandereMath.phase(YandereMath.STAY_TICKS - 1, false));
        assertEquals(YandereMath.Phase.ATTACK, YandereMath.phase(YandereMath.STAY_TICKS, false), "three seconds");
        assertEquals(YandereMath.Phase.ATTACK, YandereMath.phase(YandereMath.WARN_TICKS, true), "hostile after the warning");
        assertEquals(60, YandereMath.STAY_TICKS);
        assertEquals(20, YandereMath.WARN_TICKS);
    }

    // ---------------------------------------------------------------- never lethal
    @Test
    void interceptingNeverKillsTheAvatar() {
        assertEquals(7f, YandereMath.interceptedPart(10f, 0.7, 100f), 1e-5f);
        assertEquals(4f, YandereMath.interceptedPart(10f, 0.7, 5f), 1e-5f, "capped at health - 1");
        assertEquals(0f, YandereMath.interceptedPart(10f, 0.7, 1f), 1e-5f);
        assertEquals(0f, YandereMath.interceptedPart(10f, 0.7, 0.5f), 1e-5f);
        assertEquals(0f, YandereMath.interceptedPart(10f, 0.0, 100f), 1e-5f);
        assertEquals(10f, YandereMath.interceptedPart(10f, 5.0, 100f), 1e-5f, "a share above 1 is a share of 1");
        assertEquals(0f, YandereMath.interceptedPart(-3f, 0.7, 100f), 1e-5f);
    }

    @Test
    void frenzyBurnFloorsAtOneHealth() {
        assertEquals(2f, YandereMath.burn(20f, 2f), 1e-5f);
        assertEquals(1f, YandereMath.burn(2f, 2f), 1e-5f);
        assertEquals(0f, YandereMath.burn(1f, 2f), 1e-5f);
        assertEquals(0f, YandereMath.burn(0.2f, 2f), 1e-5f);
        float hp = 20f;
        for (int i = 0; i < 100; i++) hp -= YandereMath.burn(hp, 3f);
        assertEquals(1f, hp, 1e-5f, "a hundred frenzy casts leave it alive at 1");
    }

    @Test
    void frenzyDrainsTheMaximumButNeverBelowOne() {
        assertEquals(2.0, YandereMath.drainStep(20.0, 0.0, 2.0), 1e-9);
        assertEquals(2.0, YandereMath.drainStep(20.0, 10.0, 2.0), 1e-9);
        assertEquals(1.0, YandereMath.drainStep(20.0, 18.0, 2.0), 1e-9, "only what leaves a maximum of one");
        assertEquals(0.0, YandereMath.drainStep(20.0, 19.0, 2.0), 1e-9);
        assertEquals(0.0, YandereMath.drainStep(20.0, 25.0, 2.0), 1e-9);
        assertEquals(0.0, YandereMath.drainStep(20.0, 0.0, 0.0), 1e-9);
        assertEquals(0.0, YandereMath.drainStep(20.0, 0.0, -3.0), 1e-9);
        double drained = 0.0, max = 40.0;
        for (int i = 0; i < 1000; i++) drained += YandereMath.drainStep(max, drained, 3.0);
        assertEquals(max - 1.0, drained, 1e-9, "a thousand casts leave the maximum at one, never zero");
    }

    @Test
    void nonLethalNeverTakesAPlayerBelowOneHealth() {
        assertEquals(5f, YandereMath.nonLethal(5f, 20f, 0f), 1e-5f);
        assertEquals(19f, YandereMath.nonLethal(100f, 20f, 0f), 1e-5f);
        assertEquals(0f, YandereMath.nonLethal(100f, 1f, 0f), 1e-5f);
        assertEquals(4f, YandereMath.nonLethal(100f, 3f, 2f), 1e-5f, "absorption counts as health");
        assertEquals(0f, YandereMath.nonLethal(100f, 0.5f, 0f), 1e-5f);
    }

    // ---------------------------------------------------------------- frenzy state machine
    @Test
    void frenzyRunsThenExhaustsThenEnds() {
        YandereFrenzyState s = new YandereFrenzyState(0, 0, 5);
        assertEquals(YandereFrenzyState.Phase.IDLE, s.phase());
        assertEquals(YandereFrenzyState.Change.NONE, s.tick(), "idle stays idle");
        s.trigger(3);
        assertEquals(YandereFrenzyState.Phase.FRENZY, s.phase());
        assertEquals(YandereFrenzyState.Change.NONE, s.tick());
        assertEquals(YandereFrenzyState.Change.NONE, s.tick());
        assertEquals(YandereFrenzyState.Change.ENTERED_EXHAUSTION, s.tick());
        assertEquals(YandereFrenzyState.Phase.EXHAUSTED, s.phase());
        for (int i = 0; i < 4; i++) assertEquals(YandereFrenzyState.Change.NONE, s.tick());
        assertEquals(YandereFrenzyState.Change.ENDED, s.tick());
        assertEquals(YandereFrenzyState.Phase.IDLE, s.phase());
    }

    @Test
    void frenzyAlwaysEndsEvenWithoutExhaustion() {
        YandereFrenzyState s = new YandereFrenzyState(0, 0, 0);
        s.trigger(2);
        assertEquals(YandereFrenzyState.Change.NONE, s.tick());
        assertEquals(YandereFrenzyState.Change.ENDED, s.tick());
        assertEquals(YandereFrenzyState.Phase.IDLE, s.phase());
    }

    @Test
    void anotherHitRefreshesButNeverStacksBeyondTheDuration() {
        YandereFrenzyState s = new YandereFrenzyState(0, 0, 4);
        s.trigger(300);
        for (int i = 0; i < 100; i++) s.tick();
        assertEquals(200, s.frenzyLeft());
        s.trigger(300);
        assertEquals(300, s.frenzyLeft(), "refreshed to the full duration");
        s.trigger(50);
        assertEquals(300, s.frenzyLeft(), "a shorter trigger never shortens it");
        for (int i = 0; i < 10_000 && s.phase() != YandereFrenzyState.Phase.IDLE; i++) s.tick();
        assertEquals(YandereFrenzyState.Phase.IDLE, s.phase(), "it ends within a finite time");
    }

    @Test
    void aNewHitDuringExhaustionStartsAFreshFrenzy() {
        YandereFrenzyState s = new YandereFrenzyState(0, 3, 5);
        assertEquals(YandereFrenzyState.Phase.EXHAUSTED, s.phase());
        s.trigger(10);
        assertEquals(YandereFrenzyState.Phase.FRENZY, s.phase());
        assertEquals(0, s.exhaustLeft());
    }

    @Test
    void savedValuesRestoreTheSamePhase() {
        YandereFrenzyState a = new YandereFrenzyState(7, 0, 5), b = new YandereFrenzyState(0, 4, 5), c = new YandereFrenzyState(-3, -3, -3);
        assertEquals(YandereFrenzyState.Phase.FRENZY, a.phase());
        assertEquals(YandereFrenzyState.Phase.EXHAUSTED, b.phase());
        assertEquals(YandereFrenzyState.Phase.IDLE, c.phase(), "garbage from a bad save reads as idle");
    }
}
