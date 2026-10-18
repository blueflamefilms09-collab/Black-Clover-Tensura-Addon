package com.newuniverse.nusmp.heroweapon.weapon.instrument;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the rhythm input is the heart of the Musical Instrument and is pure arithmetic on the game clock, so whole phrases are played here: in-time techniques build harmony, a lapsed pocket starts the phrase again, a refused cast counts for nothing. */
class TempoTest {
    @Test
    void theFirstTechniqueOfAPhraseHasNoHarmony() {
        Tempo t = new Tempo();
        assertEquals(0, t.preview(1000, false));
        assertEquals(0, t.commit(1000, false));
    }

    @Test
    void eachTechniqueInTheBeatAddsOneUpToFour() {
        Tempo t = new Tempo();
        long now = 1000;
        int[] want = {0, 1, 2, 3, 4, 4, 4};
        for (int w : want) {
            assertEquals(w, t.commit(now, false), "at " + now);
            now += 60;
        }
    }

    @Test
    void thePocketIsTwentyToOneHundredTicks() {
        Tempo t = new Tempo();
        t.commit(0, false);
        assertEquals(1, t.preview(20, false), "exactly the early edge is in time");
        assertEquals(1, t.preview(100, false), "exactly the late edge is in time");
        assertEquals(0, t.preview(101, false), "one tick later the phrase has lapsed");
    }

    @Test
    void aRushedTechniqueKeepsTheHarmonyButAddsNone() {
        Tempo t = new Tempo();
        t.commit(0, false);
        t.commit(50, false);
        assertEquals(1, t.current(50, false));
        assertEquals(1, t.commit(60, false), "10 ticks after the last: rushed");
        assertEquals(2, t.commit(110, false), "50 ticks after: in time again");
    }

    @Test
    void aLapsedPhraseStartsAgainFromNothing() {
        Tempo t = new Tempo();
        t.commit(0, false);
        t.commit(50, false);
        t.commit(100, false);
        assertEquals(2, t.current(120, false));
        assertEquals(0, t.current(300, false), "the display forgets a lapsed phrase");
        assertEquals(0, t.commit(300, false), "and the next technique begins a new one");
    }

    @Test
    void previewDoesNotChangeAnything() {
        Tempo t = new Tempo();
        t.commit(0, false);
        for (int i = 0; i < 5; i++) assertEquals(1, t.preview(50, false));
        assertEquals(0, t.current(0, false));
    }

    @Test
    void steadyBeatWidensThePocketByTwentyTicks() {
        Tempo t = new Tempo();
        t.commit(0, true);
        assertEquals(1, t.preview(120, true));
        assertEquals(0, t.preview(121, true));
        Tempo plain = new Tempo();
        plain.commit(0, false);
        assertEquals(0, plain.preview(120, false));
    }

    @Test
    void resetSpendsThePhrase() {
        Tempo t = new Tempo();
        t.commit(0, false);
        t.commit(50, false);
        t.reset();
        assertEquals(0, t.current(60, false));
        assertEquals(0, t.preview(60, false));
    }
}
