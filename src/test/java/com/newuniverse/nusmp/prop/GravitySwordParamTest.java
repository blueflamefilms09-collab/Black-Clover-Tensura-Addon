package com.newuniverse.nusmp.prop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 1.93.0 Gravity Magic rebuild: the sword prop's param packs its stage and its pitch and gives both back (the server behaviour, the spell and the painter share it). */
class GravitySwordParamTest {
    @Test
    void stageAndPitchRoundTrip() {
        for (int stage = 0; stage <= 2; stage++) {
            for (float pitch = -90f; pitch <= 90f; pitch += 15f) {
                int p = GravitySwordParam.of(stage, pitch);
                assertEquals(stage, GravitySwordParam.stageOf(p));
                assertEquals(pitch, GravitySwordParam.pitchOf(p), 0.001f);
            }
        }
    }

    @Test
    void pitchIsClampedToARightAngle() {
        assertEquals(90f, GravitySwordParam.pitchOf(GravitySwordParam.of(1, 400f)), 0.001f);
        assertEquals(-90f, GravitySwordParam.pitchOf(GravitySwordParam.of(1, -400f)), 0.001f);
    }

    @Test
    void aPlainZeroParamIsAFormingSwordPointingDown() {
        assertEquals(GravitySwordParam.FORMING, GravitySwordParam.stageOf(0));
        assertEquals(-90f, GravitySwordParam.pitchOf(0), 0.001f);
    }
}
