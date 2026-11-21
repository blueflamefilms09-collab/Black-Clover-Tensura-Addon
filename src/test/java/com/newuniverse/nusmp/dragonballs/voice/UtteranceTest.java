package com.newuniverse.nusmp.dragonballs.voice;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UtteranceTest {
    private static final short[] LOUD = filled(320, 8000), QUIET = new short[320];

    private static short[] filled(int n, int v) { short[] a = new short[n]; Arrays.fill(a, (short) v); return a; }

    @Test
    void silenceBeforeSpeechIsNotKept() {
        Utterance u = new Utterance();
        for (int i = 0; i < 100; i++) assertEquals(Utterance.Action.NONE, u.add(QUIET, 0f));
        assertEquals(Utterance.Action.NONE, u.flush());
        assertNull(u.take());
    }

    @Test
    void anUtteranceEndsAfterAboutSixTenthsOfASecondOfSilence() {
        Utterance u = new Utterance();
        for (int i = 0; i < 20; i++) assertEquals(Utterance.Action.NONE, u.add(LOUD, 0.25f));    // 0.4 s of voice
        Utterance.Action last = Utterance.Action.NONE;
        int frames = 0;
        while (last == Utterance.Action.NONE && frames < 100) { last = u.add(QUIET, 0f); frames++; }
        assertEquals(Utterance.Action.END_SILENCE, last);
        assertEquals(30, frames, "0.6 s of 20 ms frames");
        short[] audio = u.take();
        assertNotNull(audio);
        assertEquals(20 * 320 + 30 * 320, audio.length);
        assertNull(u.take(), "a fresh start");
    }

    @Test
    void aClickIsThrownAway() {
        Utterance u = new Utterance();
        u.add(LOUD, 0.25f);                                        // 20 ms of voice only
        assertEquals(Utterance.Action.END_SILENCE, u.flush());
        assertNull(u.take());
    }

    @Test
    void neverMoreThanEightSecondsOfAudio() {
        Utterance u = new Utterance();
        Utterance.Action a = Utterance.Action.NONE;
        int frames = 0;
        while (a == Utterance.Action.NONE && frames < 10_000) { a = u.add(LOUD, 0.25f); frames++; }
        assertEquals(Utterance.Action.END_CAP, a);
        assertEquals(Utterance.MAX_SECONDS * 50, frames);
        assertEquals(Utterance.MAX_SAMPLES, u.take().length);
    }

    @Test
    void theTransmissionEndFlushesAnOpenUtterance() {
        Utterance u = new Utterance();
        for (int i = 0; i < 20; i++) u.add(LOUD, 0.25f);
        assertEquals(Utterance.Action.END_SILENCE, u.flush());
        assertNotNull(u.take());
    }

    @Test
    void atMostFourWaitPerPlayer() {
        assertTrue(Utterance.admit(0) && Utterance.admit(3));
        assertFalse(Utterance.admit(4));
        assertFalse(Utterance.admit(99));
    }

    @Test
    void resamplerTurns48kInto16k() {
        short[] in = new short[960];
        for (int i = 0; i < in.length; i++) in[i] = (short) (i % 3 == 0 ? 3000 : (i % 3 == 1 ? 6000 : 9000));
        short[] out = Resampler.to16k(in, 48000);
        assertEquals(320, out.length);
        assertEquals(6000, out[0], "the average of three samples");
        assertSame(in, Resampler.to16k(in, 16000));
        assertEquals(0, Resampler.to16k(null, 48000).length);
        assertEquals(0, Resampler.to16k(in, 0).length);
    }

    @Test
    void resamplerHandlesAnOddRate() {
        short[] in = new short[441];
        Arrays.fill(in, (short) 1000);
        short[] out = Resampler.to16k(in, 44100);
        assertEquals(160, out.length);
        for (short s : out) assertEquals(1000, s);
    }
}
