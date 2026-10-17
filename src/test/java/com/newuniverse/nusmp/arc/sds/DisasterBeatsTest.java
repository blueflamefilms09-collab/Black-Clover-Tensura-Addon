package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the player's whole answer to a field is made inside its warning, so the timeline of a beat (when the gardens show, when the pulse lands, what is left of the warning) is held to the tick. */
class DisasterBeatsTest {
    private static final DisasterBeats.Spec S = new DisasterBeats.Spec(5, 32, 24);

    @Test
    void aSpecIsValidOnlyWithAReadableWarningThatEndsBeforeTheNextBeat() {
        assertTrue(S.valid());
        assertFalse(new DisasterBeats.Spec(5, 32, 11).valid(), "under 12 ticks is not a warning");
        assertFalse(new DisasterBeats.Spec(5, 32, 32).valid(), "the pulse must come before the next beat");
        assertFalse(new DisasterBeats.Spec(0, 32, 24).valid());
        assertEquals(160, S.total());
    }

    @Test
    void theGardensShowAtTheStartOfEveryBeatAndOnlyThere() {
        int shows = 0;
        for (int age = -3; age < 200; age++) if (DisasterBeats.showsAt(age, S)) shows++;
        assertEquals(5, shows);
        assertTrue(DisasterBeats.showsAt(0, S) && DisasterBeats.showsAt(32, S) && DisasterBeats.showsAt(128, S));
        assertFalse(DisasterBeats.showsAt(160, S), "the field is over");
        assertFalse(DisasterBeats.showsAt(-32, S));
    }

    @Test
    void thePulseLandsTheWarningAfterTheShow() {
        int pulses = 0;
        for (int age = -3; age < 200; age++) if (DisasterBeats.pulsesAt(age, S)) pulses++;
        assertEquals(DisasterBeats.pulses(S), pulses);
        assertTrue(DisasterBeats.pulsesAt(24, S) && DisasterBeats.pulsesAt(56, S) && DisasterBeats.pulsesAt(152, S));
        assertFalse(DisasterBeats.pulsesAt(184, S), "no pulse after the last beat");
        assertFalse(DisasterBeats.pulsesAt(23, S));
    }

    @Test
    void theBeatAndTheWarningLeftFollowTheClock() {
        assertEquals(0, DisasterBeats.beatOf(0, S));
        assertEquals(0, DisasterBeats.beatOf(31, S));
        assertEquals(1, DisasterBeats.beatOf(32, S));
        assertEquals(4, DisasterBeats.beatOf(159, S));
        assertEquals(-1, DisasterBeats.beatOf(160, S));
        assertEquals(-1, DisasterBeats.beatOf(-1, S));
        assertEquals(24, DisasterBeats.warnLeft(0, S));
        assertEquals(1, DisasterBeats.warnLeft(23, S));
        assertEquals(0, DisasterBeats.warnLeft(24, S), "it has pulsed");
        assertEquals(0, DisasterBeats.warnLeft(31, S));
        assertEquals(24, DisasterBeats.warnLeft(32, S), "the next beat's warning starts");
        assertEquals(0, DisasterBeats.warnLeft(400, S));
    }
}
