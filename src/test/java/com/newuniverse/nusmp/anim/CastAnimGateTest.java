package com.newuniverse.nusmp.anim;

import com.newuniverse.nusmp.anim.CastAnimGate.Decision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Naming the clip that already plays must not restart it. */
class CastAnimGateTest {
    private static final float NO_STOP = Float.NaN;

    @Test
    void anotherClipOrNothingPlayingStarts() {
        assertEquals(Decision.START, CastAnimGate.decide(null, false, 0, NO_STOP, "cast_chant", 10));
        assertEquals(Decision.START, CastAnimGate.decide("cast_chant", true, 0, NO_STOP, "cast_fail", 10));
    }

    @Test
    void theLoopingChantIsNeverRestartedWhileItPlays() {
        for (int t = 1; t < 400; t++) assertEquals(Decision.IGNORE, CastAnimGate.decide("cast_chant", true, 0, NO_STOP, "cast_chant", t), "tick " + t);
    }

    @Test
    void aChantThatWasFadingOutResumesInsteadOfSnapping() {
        assertEquals(Decision.RESUME, CastAnimGate.decide("cast_chant", true, 0, 100, "cast_chant", 102));
        assertEquals(Decision.START, CastAnimGate.decide("cast_chant", true, 0, 100, "cast_chant", 100 + CastAnimGate.FADE_OUT),
                "once it has faded away it is a fresh start");
    }

    @Test
    void aOneShotNamedAgainRightAwayIsTheSameCast() {
        assertEquals(Decision.IGNORE, CastAnimGate.decide("cast_fail", false, 100, NO_STOP, "cast_fail", 102), "a fizzle spammed by a held key");
        assertEquals(Decision.START, CastAnimGate.decide("cast_fail", false, 100, NO_STOP, "cast_fail", 100 + CastAnimGate.REPEAT_WINDOW),
                "a real second cast plays");
        assertEquals(Decision.START, CastAnimGate.decide("cast_release_up", false, 100, 103, "cast_release_up", 104),
                "a one-shot that was stopped is not resumed");
    }
}
