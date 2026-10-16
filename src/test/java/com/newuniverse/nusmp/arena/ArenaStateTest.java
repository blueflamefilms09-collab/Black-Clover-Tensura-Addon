package com.newuniverse.nusmp.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The arena's ready / fighting / cooldown machine: the start timers, the abandon rule, the defeat rest, the save. */
class ArenaStateTest {
    @Test
    void nobodyHereMeansNothingHappens() {
        ArenaState s = new ArenaState();
        for (int i = 0; i < 100; i++) assertEquals(ArenaState.Start.NOTHING, s.tickReady(10, 0, 0));
        assertEquals(ArenaState.Phase.READY, s.phase());
        assertEquals(0, s.countdownLeft());
    }

    @Test
    void theFirstArrivalStartsTenSecondsAndTheBossComesAtTheEnd() {
        ArenaState s = new ArenaState();
        assertEquals(ArenaState.Start.COUNTDOWN_BEGAN, s.tickReady(10, 1, 0));
        int ticks = 10;
        ArenaState.Start last = ArenaState.Start.NOTHING;
        while (ticks < 400 && last != ArenaState.Start.SPAWN) {
            last = s.tickReady(10, 1, 0);
            ticks += 10;
            if (last != ArenaState.Start.SPAWN) assertEquals(ArenaState.Start.NOTHING, last, "the title is announced once");
        }
        assertEquals(ArenaState.Start.SPAWN, last);
        assertEquals(ArenaState.COUNTDOWN_TICKS, ticks);
        assertEquals(ArenaState.Phase.FIGHTING, s.phase());
    }

    @Test
    void standingOnTheDaisForFiveSecondsStartsItSooner() {
        ArenaState s = new ArenaState();
        assertEquals(ArenaState.Start.COUNTDOWN_BEGAN, s.tickReady(10, 1, 1));
        int ticks = 10;
        while (s.tickReady(10, 1, 1) != ArenaState.Start.SPAWN) ticks += 10;
        assertEquals(ArenaState.DAIS_TICKS - 10, ticks);
        assertTrue(ArenaState.DAIS_TICKS < ArenaState.COUNTDOWN_TICKS);
    }

    @Test
    void leavingTheDaisStartsTheFiveSecondsAgain() {
        ArenaState s = new ArenaState();
        for (int i = 0; i < 8; i++) s.tickReady(10, 1, 1);
        s.tickReady(10, 1, 0);
        for (int i = 0; i < 8; i++) assertNotEquals(ArenaState.Start.SPAWN, s.tickReady(10, 1, 1));
    }

    @Test
    void everybodyLeavingDuringTheCountdownResetsIt() {
        ArenaState s = new ArenaState();
        for (int i = 0; i < 15; i++) s.tickReady(10, 1, 0);
        assertTrue(s.countdownLeft() > 0);
        s.tickReady(10, 0, 0);
        assertEquals(0, s.countdownLeft());
        assertEquals(ArenaState.Start.COUNTDOWN_BEGAN, s.tickReady(10, 1, 0), "a new arrival begins again");
    }

    @Test
    void anEmptyArenaAbandonsTheFightAfterTwoSeconds() {
        ArenaState s = fighting();
        assertEquals(ArenaState.Watch.CONTINUE, s.tickFighting(10, 0, true));
        assertEquals(ArenaState.Watch.CONTINUE, s.tickFighting(10, 2, true), "somebody came back: the empty clock restarts");
        for (int i = 0; i < 3; i++) assertEquals(ArenaState.Watch.CONTINUE, s.tickFighting(10, 0, true));
        assertEquals(ArenaState.Watch.ABANDONED, s.tickFighting(10, 0, true));
    }

    @Test
    void aBossThatVanishedWithoutBeingDefeatedIsAnAbandonedFight() {
        ArenaState s = fighting();
        assertEquals(ArenaState.Watch.ABANDONED, s.tickFighting(10, 3, false));
    }

    @Test
    void anAbandonedFightRestsSixtySecondsAndThenIsReadyWithNoDefeat() {
        ArenaState s = fighting();
        s.abandon(1000);
        assertEquals(ArenaState.Phase.COOLDOWN, s.phase());
        assertFalse(s.defeated());
        assertEquals(60, s.cooldownSecondsLeft(1000));
        assertFalse(s.tickCooldown(1000 + ArenaState.RESET_TICKS - 1));
        assertTrue(s.tickCooldown(1000 + ArenaState.RESET_TICKS));
        assertEquals(ArenaState.Phase.READY, s.phase());
        assertEquals(ArenaState.Start.COUNTDOWN_BEGAN, s.tickReady(10, 1, 0), "a fresh fight can begin");
    }

    @Test
    void aDefeatRestsForTheConfiguredMinutesAndNoBossComesMeanwhile() {
        ArenaState s = fighting();
        s.defeat(5000, 20L * 60 * 20);
        assertEquals(ArenaState.Phase.COOLDOWN, s.phase());
        assertTrue(s.defeated());
        assertTrue(s.sigilOpen());
        for (int i = 0; i < 50; i++) assertEquals(ArenaState.Start.NOTHING, s.tickReady(10, 4, 4), "tickReady does nothing outside READY");
        assertEquals(ArenaState.Phase.COOLDOWN, s.phase());
        assertEquals(20 * 60, s.cooldownSecondsLeft(5000));
        assertFalse(s.tickCooldown(5000 + 24000 - 1));
        assertTrue(s.tickCooldown(5000 + 24000));
        assertFalse(s.defeated());
    }

    @Test
    void theSigilIsDormantWhileTheBossFights() {
        ArenaState s = fighting();
        assertFalse(s.sigilOpen());
        assertTrue(new ArenaState().sigilOpen());
    }

    @Test
    void tickFightingDoesNothingOutsideAFight() {
        ArenaState s = new ArenaState();
        assertEquals(ArenaState.Watch.CONTINUE, s.tickFighting(10, 0, false));
        assertEquals(ArenaState.Phase.READY, s.phase());
    }

    @Test
    void theStateSavesAndRestores() {
        ArenaState s = fighting();
        s.defeat(100, 500);
        ArenaState t = new ArenaState();
        t.restore(s.save());
        assertEquals(ArenaState.Phase.COOLDOWN, t.phase());
        assertTrue(t.defeated());
        assertEquals(600, t.cooldownUntil());
        ArenaState u = new ArenaState();
        u.restore(fighting().save());
        assertEquals(ArenaState.Phase.FIGHTING, u.phase(), "a fight that was running when the server stopped is checked by the director on its first look");
    }

    @Test
    void garbageInASaveIsMadeSafe() {
        ArenaState s = new ArenaState();
        s.restore(new long[]{99, -5, 7});
        assertEquals(ArenaState.Phase.READY, s.phase(), "the last phase with no end time is no cooldown");
        s.restore(null);
        assertEquals(ArenaState.Phase.READY, s.phase());
        s.restore(new long[]{1});
        assertEquals(ArenaState.Phase.READY, s.phase());
        s.restore(new long[]{2, 0, 0});
        assertEquals(ArenaState.Phase.READY, s.phase(), "a cooldown with no end is no cooldown");
    }

    @Test
    void resetIsAnOperatorsWayBackToReady() {
        ArenaState s = fighting();
        s.defeat(0, 99999);
        s.reset();
        assertEquals(ArenaState.Phase.READY, s.phase());
        assertEquals(0, s.cooldownSecondsLeft(1));
    }

    private static ArenaState fighting() {
        ArenaState s = new ArenaState();
        while (s.tickReady(10, 1, 0) != ArenaState.Start.SPAWN) { /* count down */ }
        return s;
    }
}
