package com.newuniverse.nusmp.dragonballs.voice;

import java.util.Arrays;

/**
 * Why: speech arrives as a stream of 20 ms frames; the recogniser wants one phrase at a time. This cuts the stream into utterances: it starts at the first loud frame, ends after about 0.6 s of
 * silence or at 8 s of audio, and a click shorter than 0.15 s of voice is thrown away. At most {@link #MAX_QUEUED} utterances of one player may wait for the recogniser. Pure and bounded: the buffer
 * never grows past {@link #MAX_SAMPLES} (256 KB). Not thread safe: the caller locks it.
 */
public final class Utterance {
    public static final int RATE = 16000, MAX_SECONDS = 8, MAX_QUEUED = 4, SILENCE_MS = 600, MIN_VOICED_MS = 150;
    public static final int MAX_SAMPLES = RATE * MAX_SECONDS;
    /** RMS (0..1) from which a frame counts as voice; low on purpose so a whisper still counts. */
    public static final float VOICE_RMS = 0.01f;

    public enum Action { NONE, END_SILENCE, END_CAP }

    private short[] buf = new short[RATE];
    private int n, voiced, trailing;
    private boolean started;

    /** May one more utterance of this player wait, given how many already do? */
    public static boolean admit(int queued) { return queued < MAX_QUEUED; }

    public Action add(short[] pcm16k, float rms) {
        if (pcm16k == null || pcm16k.length == 0) return Action.NONE;
        boolean loud = rms >= VOICE_RMS;
        if (!started) {
            if (!loud) return Action.NONE;
            started = true;
        }
        int take = Math.min(pcm16k.length, MAX_SAMPLES - n);
        if (take > 0) {
            if (n + take > buf.length) buf = Arrays.copyOf(buf, Math.min(MAX_SAMPLES, Math.max(buf.length * 2, n + take)));
            System.arraycopy(pcm16k, 0, buf, n, take);
            n += take;
        }
        if (loud) { voiced += pcm16k.length; trailing = 0; } else trailing += pcm16k.length;
        if (n >= MAX_SAMPLES) return Action.END_CAP;
        return trailing >= RATE * SILENCE_MS / 1000 ? Action.END_SILENCE : Action.NONE;
    }

    /** The transmission ended (the voice mod sent its end-of-speech frame): close the utterance if one is open. */
    public Action flush() { return started ? Action.END_SILENCE : Action.NONE; }

    /** The audio of the finished utterance (null when it was only a click) and a fresh start. */
    public short[] take() {
        short[] out = voiced >= RATE * MIN_VOICED_MS / 1000 ? Arrays.copyOf(buf, n) : null;
        n = 0; voiced = 0; trailing = 0; started = false;
        return out;
    }
}
