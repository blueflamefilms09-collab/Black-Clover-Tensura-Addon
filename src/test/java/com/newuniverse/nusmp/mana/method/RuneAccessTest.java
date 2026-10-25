package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneAccess.Cast;
import com.newuniverse.nusmp.mana.method.RuneAccess.Mode;
import com.newuniverse.nusmp.mana.method.RuneAccess.Start;
import com.newuniverse.nusmp.mana.method.RuneAccess.Stamps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 3.8.0: the access decision (all three modes) and the TIMED timeline, including the cooldown across a relog. */
class RuneAccessTest {
    private static final int DUR = 480, CD = 600;                                   // the defaults: 8 minutes on, 10 minutes cooldown
    private static final long DUR_T = 20L * DUR, CD_T = 20L * CD;

    // ---------------------------------------------------------------- the decision
    @Test
    void theDefaultsAreTheOwnersNumbers() {
        assertEquals(0.7, RuneAccess.DEFAULT_MASTERY);
        assertEquals(480, RuneAccess.DEFAULT_SECONDS);
        assertEquals(600, RuneAccess.DEFAULT_COOLDOWN);
        assertEquals(Mode.PASSIVE, Mode.values()[0], "PASSIVE is the default and the first entry; the entries are only ever appended");
        assertEquals(Mode.TIMED, Mode.values()[1]);
        assertEquals(Mode.MANA_ZONE_GATE, Mode.values()[2]);
    }

    @Test
    void passiveOpensAtTheMasteryAndNeverBefore() {
        assertFalse(RuneAccess.allowed(Mode.PASSIVE, false, 0.0, 0.7, false, true), "the gate does not matter in PASSIVE");
        assertFalse(RuneAccess.allowed(Mode.PASSIVE, false, 0.6999, 0.7, true, true), "the state does not matter in PASSIVE");
        assertTrue(RuneAccess.allowed(Mode.PASSIVE, false, 0.7, 0.7, false, false));
        assertTrue(RuneAccess.allowed(Mode.PASSIVE, false, 1.0, 0.7, false, false));
        assertFalse(RuneAccess.allowed(Mode.PASSIVE, false, Double.NaN, 0.7, false, false));
        assertTrue(RuneAccess.allowed(Mode.PASSIVE, false, 0.0, 0.0, false, false), "a threshold of 0 opens at once");
        assertFalse(RuneAccess.allowed(Mode.PASSIVE, false, 0.999, 1.0, false, false), "a threshold of 1 needs full mastery");
        assertTrue(RuneAccess.allowed(Mode.PASSIVE, false, 1.0, 1.0, false, false));
        assertFalse(RuneAccess.allowed(Mode.PASSIVE, false, 0.5, 9.0, false, false), "an out of range threshold is clamped to 1, not read as 'always open'");
    }

    @Test
    void timedNeedsOnlyTheStateNotMasteryNotTheGate() {
        assertFalse(RuneAccess.allowed(Mode.TIMED, false, 1.0, 0.7, false, true), "full mastery and an open gate do not help while the state is off");
        assertTrue(RuneAccess.allowed(Mode.TIMED, false, 0.0, 0.7, true, false), "the state on is enough: no mastery, no gate");
    }

    @Test
    void theGateModeIsExactlyTheOldGate() {
        assertTrue(RuneAccess.allowed(Mode.MANA_ZONE_GATE, false, 0.0, 0.7, false, true));
        assertFalse(RuneAccess.allowed(Mode.MANA_ZONE_GATE, false, 1.0, 0.7, true, false), "mastery and the state do not open the old gate");
        assertTrue(RuneAccess.needsGate(Mode.MANA_ZONE_GATE));
        assertFalse(RuneAccess.needsGate(Mode.PASSIVE) || RuneAccess.needsGate(Mode.TIMED), "the gate is only evaluated in its own mode");
    }

    @Test
    void theStatePageIsUsableInTimedOnly() {
        for (Mode m : Mode.values())
            for (boolean on : new boolean[]{false, true})
                for (boolean gate : new boolean[]{false, true})
                    assertEquals(m == Mode.TIMED, RuneAccess.allowed(m, true, 1.0, 0.7, on, gate), "the Rune State page in " + m);
    }

    @Test
    void aMissingModeReadsAsPassive() {
        assertTrue(RuneAccess.allowed(null, false, 0.9, 0.7, false, false));
        assertFalse(RuneAccess.allowed(null, false, 0.1, 0.7, true, true));
    }

    @Test
    void theSettingsAreClampedToTheirRanges() {
        assertEquals(0.0, RuneAccess.mastery(-3));
        assertEquals(1.0, RuneAccess.mastery(7));
        assertEquals(RuneAccess.DEFAULT_MASTERY, RuneAccess.mastery(Double.NaN));
        assertEquals(30, RuneAccess.seconds(0));
        assertEquals(3600, RuneAccess.seconds(999999));
        assertEquals(0, RuneAccess.cooldownSeconds(-5));
        assertEquals(7200, RuneAccess.cooldownSeconds(999999));
    }

    // ---------------------------------------------------------------- the timeline (the owner's 8 minutes on, 10 minutes cooldown)
    @Test
    void theExampleTimeline() {
        long t0 = 1000;
        Cast first = RuneAccess.cast(Stamps.NONE, t0, DUR, CD);
        assertEquals(Start.STARTED, first.result());
        Stamps s = first.stamps();
        assertEquals(t0 + DUR_T, s.onUntil(), "ON for 8 minutes");
        assertEquals(t0 + DUR_T + CD_T, s.cooldownUntil(), "then 10 minutes of cooldown, counted from the END of the ON state");

        // during the ON state
        assertTrue(RuneAccess.on(s, t0 + 1));
        assertTrue(RuneAccess.on(s, t0 + DUR_T - 1));
        assertEquals(Start.ALREADY_ON, RuneAccess.cast(s, t0 + 100, DUR, CD).result(), "a second cast while ON does nothing");
        assertSame(s, RuneAccess.cast(s, t0 + 100, DUR, CD).stamps(), "and changes nothing");

        // the 30 second warning
        assertFalse(RuneAccess.warnDue(s, t0 + DUR_T - 601, false));
        assertTrue(RuneAccess.warnDue(s, t0 + DUR_T - 600, false), "30 s before the end");
        assertFalse(RuneAccess.warnDue(s, t0 + DUR_T - 300, true), "warned once only");
        assertFalse(RuneAccess.warnDue(s, t0 + DUR_T, false), "no warning after the end");

        // the end, then the cooldown
        long off = t0 + DUR_T;
        assertFalse(RuneAccess.on(s, off));
        Cast refused = RuneAccess.cast(s, off, DUR, CD);
        assertEquals(Start.COOLING, refused.result());
        assertEquals(CD_T, RuneAccess.cooldownLeft(s, off), "the whole cooldown is left when the state ends");
        assertEquals(Start.COOLING, RuneAccess.cast(s, off + CD_T - 1, DUR, CD).result(), "one tick before the cooldown ends");
        Cast again = RuneAccess.cast(s, off + CD_T, DUR, CD);
        assertEquals(Start.STARTED, again.result(), "the first tick after the cooldown");
        assertEquals(off + CD_T + DUR_T, again.stamps().onUntil());
    }

    @Test
    void aCooldownOfZeroAllowsTheNextCastAtOnce() {
        Stamps s = RuneAccess.cast(Stamps.NONE, 0, 30, 0).stamps();
        assertEquals(Start.ALREADY_ON, RuneAccess.cast(s, 599, 30, 0).result());
        assertEquals(Start.STARTED, RuneAccess.cast(s, 600, 30, 0).result());
    }

    @Test
    void theCooldownSurvivesLoggingOutAndIn() {
        long t0 = 50_000;
        Stamps on = RuneAccess.cast(Stamps.NONE, t0, DUR, CD).stamps();
        // the player logs out 100 s into the state: the state ends, the cooldown starts there
        long logout = t0 + 2000;
        Stamps out = RuneAccess.endNow(on, logout, CD);
        assertFalse(RuneAccess.on(out, logout));
        assertEquals(logout + CD_T, out.cooldownUntil(), "the cooldown starts when the state is cut");
        assertTrue(out.cooldownUntil() < on.cooldownUntil(), "cutting it short never keeps the long cooldown, and never gains time: the rest of the ON time is lost");
        // logging back in at once does not reset the cooldown, however often
        for (int relog = 0; relog < 5; relog++) {
            long back = logout + 20L * (relog + 1);
            assertEquals(Start.COOLING, RuneAccess.cast(out, back, DUR, CD).result(), "relog " + relog + " cannot reset the cooldown");
            assertEquals(logout + CD_T - back, RuneAccess.cooldownLeft(out, back));
        }
        // a logout does not change a cooldown that is already running
        assertSame(out, RuneAccess.endNow(out, logout + 40, CD), "ending a state that is already off changes nothing");
        // and after the cooldown the page works again
        assertEquals(Start.STARTED, RuneAccess.cast(out, logout + CD_T, DUR, CD).result());
    }

    @Test
    void aStateLeftOnInTheFileIsEndedAtLogin() {
        // the server stopped without a logout: the file still says ON until t0 + 8 minutes; the player comes back 2 minutes later
        long t0 = 70_000;
        Stamps file = RuneAccess.cast(Stamps.NONE, t0, DUR, CD).stamps();
        long login = t0 + 2400;
        assertTrue(RuneAccess.on(file, login));
        Stamps fixed = RuneAccess.endNow(file, login, CD);
        assertFalse(RuneAccess.on(fixed, login));
        assertEquals(login + CD_T, fixed.cooldownUntil());
    }

    @Test
    void dyingAndChangingDimensionEndTheStateTheSameWay() {
        Stamps on = RuneAccess.cast(Stamps.NONE, 0, DUR, CD).stamps();
        for (long at : new long[]{1, 100, DUR_T - 1}) {
            Stamps cut = RuneAccess.endNow(on, at, CD);
            assertEquals(at, cut.onUntil());
            assertEquals(at + CD_T, cut.cooldownUntil());
            assertEquals(Start.COOLING, RuneAccess.cast(cut, at + 1, DUR, CD).result());
        }
        // a state that already ended naturally is not "ended" again: the cooldown is not pushed back
        Stamps over = on;
        assertSame(over, RuneAccess.endNow(over, DUR_T + 500, CD));
    }

    @Test
    void theCooldownReadsZeroWhenOverAndNeverNegative() {
        Stamps s = new Stamps(100, 200);
        assertEquals(0, RuneAccess.cooldownLeft(s, 200));
        assertEquals(0, RuneAccess.cooldownLeft(s, 99999));
        assertEquals(0, RuneAccess.onLeft(s, 100));
        assertEquals(60, RuneAccess.onLeft(s, 40));
    }

    @Test
    void theDurationsAreClampedInTheCast() {
        Stamps s = RuneAccess.cast(Stamps.NONE, 0, 1, -50).stamps();
        assertEquals(20L * 30, s.onUntil(), "at least 30 s");
        assertEquals(s.onUntil(), s.cooldownUntil(), "a negative cooldown is 0");
        Stamps big = RuneAccess.cast(Stamps.NONE, 0, 99999, 99999).stamps();
        assertEquals(20L * 3600, big.onUntil());
        assertEquals(20L * (3600 + 7200), big.cooldownUntil());
    }

    @Test
    void theClockReadsMinutesAndSeconds() {
        assertEquals("8:00", RuneAccess.clock(DUR_T));
        assertEquals("10:00", RuneAccess.clock(CD_T));
        assertEquals("0:30", RuneAccess.clock(600));
        assertEquals("0:01", RuneAccess.clock(1));
        assertEquals("0:00", RuneAccess.clock(0));
        assertEquals("0:00", RuneAccess.clock(-40));
        assertEquals("0:05", RuneAccess.clock(81), "a started second counts as a whole one");
    }

    @Test
    void twoPlayersNeverShareAStamp() {
        // the stamps are plain values: one record per player, nothing static
        Stamps a = RuneAccess.cast(Stamps.NONE, 0, DUR, CD).stamps();
        Stamps b = Stamps.NONE;
        assertTrue(RuneAccess.on(a, 10));
        assertFalse(RuneAccess.on(b, 10));
        assertEquals(Start.STARTED, RuneAccess.cast(b, 10, DUR, CD).result(), "another player's state does not block this one");
    }
}
