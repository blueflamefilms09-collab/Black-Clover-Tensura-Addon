package com.newuniverse.nusmp.avatar.ritual;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The ritual timeline walked tick by tick: charge, open, timeout, accept, the birth beats in order and exactly once, and a cancel from anywhere. */
class RitualClockTest {
    private static RitualClock clock() { return new RitualClock(80, 1200, RitualClock.Birth.of(50)); }

    private static List<RitualClock.Event> run(RitualClock c, int ticks) {
        List<RitualClock.Event> out = new ArrayList<>();
        for (int i = 0; i < ticks; i++) {
            RitualClock.Event e = c.tick();
            if (e != RitualClock.Event.NONE) out.add(e);
        }
        return out;
    }

    @Test
    void theScreenOpensExactlyWhenTheChargeIsDone() {
        RitualClock c = clock();
        assertEquals(RitualClock.Phase.CHARGING, c.phase());
        assertEquals(List.of(), run(c, 79));
        assertEquals(RitualClock.Phase.CHARGING, c.phase());
        assertEquals(RitualClock.Event.OPEN_MENU, c.tick());
        assertEquals(RitualClock.Phase.OPEN, c.phase());
        assertEquals(0, c.age());
    }

    @Test
    void anOpenScreenTimesOutAndCancels() {
        RitualClock c = clock();
        run(c, 80);
        assertEquals(List.of(), run(c, 1199));
        assertEquals(RitualClock.Event.TIMEOUT, c.tick());
        assertEquals(RitualClock.Phase.CANCELLED, c.phase());
        assertTrue(c.finished());
        assertEquals(RitualClock.Event.NONE, c.tick());
    }

    @Test
    void acceptOnlyWorksWhileTheScreenIsOpen() {
        RitualClock c = clock();
        assertFalse(c.accept(), "still charging");
        run(c, 80);
        assertTrue(c.accept());
        assertEquals(RitualClock.Phase.SPECTACLE, c.phase());
        assertFalse(c.accept(), "only once");
    }

    @Test
    void theBirthBeatsComeInOrderAndOnce() {
        RitualClock c = clock();
        run(c, 80);
        c.accept();
        List<RitualClock.Event> got = run(c, 200);
        assertEquals(List.of(RitualClock.Event.TELL, RitualClock.Event.STRIKE, RitualClock.Event.SPAWN, RitualClock.Event.NOVA, RitualClock.Event.FINISH), got);
        assertEquals(RitualClock.Phase.DONE, c.phase());
        assertTrue(c.finished());
    }

    @Test
    void theBeatsLandOnTheirTicks() {
        RitualClock.Birth b = RitualClock.Birth.of(50);
        assertEquals(0, b.tell());
        assertEquals(18, b.strike());
        assertEquals(24, b.spawn());
        assertEquals(28, b.nova());
        assertEquals(50, b.end());
        RitualClock c = clock();
        run(c, 80);
        c.accept();
        for (int t = 0; t <= b.end(); t++) {
            RitualClock.Event e = c.tick();
            if (t == b.tell()) assertEquals(RitualClock.Event.TELL, e);
            else if (t == b.strike()) assertEquals(RitualClock.Event.STRIKE, e);
            else if (t == b.spawn()) assertEquals(RitualClock.Event.SPAWN, e);
            else if (t == b.nova()) assertEquals(RitualClock.Event.NOVA, e);
            else if (t == b.end()) assertEquals(RitualClock.Event.FINISH, e);
            else assertEquals(RitualClock.Event.NONE, e, "tick " + t);
        }
    }

    @Test
    void everyBirthLengthKeepsTheBeatsStrictlyIncreasing() {
        for (int total = -5; total <= 400; total++) {
            RitualClock.Birth b = RitualClock.Birth.of(total);
            assertTrue(b.tell() < b.strike() && b.strike() < b.spawn() && b.spawn() < b.nova() && b.nova() < b.end(), "total " + total);
            assertTrue(b.end() >= 24 && b.end() <= 240 + 4, "total " + total + " end " + b.end());
        }
    }

    @Test
    void aBirthWithBeatsOutOfOrderIsRefused() {
        try {
            new RitualClock.Birth(0, 5, 5, 9, 12);
            assertTrue(false, "equal beats must be refused");
        } catch (IllegalArgumentException expected) { /* good */ }
    }

    @Test
    void cancelWorksFromEveryPhaseOnceAndNeverAfterTheEnd() {
        for (int phase = 0; phase < 3; phase++) {
            RitualClock c = clock();
            if (phase >= 1) run(c, 80);
            if (phase >= 2) c.accept();
            assertTrue(c.cancel(), "phase " + phase);
            assertEquals(RitualClock.Phase.CANCELLED, c.phase());
            assertFalse(c.cancel());
            assertEquals(RitualClock.Event.NONE, c.tick());
            assertFalse(c.accept());
        }
        RitualClock done = clock();
        run(done, 80);
        done.accept();
        run(done, 100);
        assertEquals(RitualClock.Phase.DONE, done.phase());
        assertFalse(done.cancel(), "a finished ritual cannot be cancelled");
        assertEquals(RitualClock.Phase.DONE, done.phase());
    }

    @Test
    void nonsenseLengthsAreClampedToOneTick() {
        RitualClock c = new RitualClock(0, -4, RitualClock.Birth.of(50));
        assertEquals(RitualClock.Event.OPEN_MENU, c.tick());
        assertEquals(RitualClock.Event.TIMEOUT, c.tick());
    }
}
