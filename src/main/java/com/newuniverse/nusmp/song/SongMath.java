package com.newuniverse.nusmp.song;

/**
 * Why: the voice bridge hands us raw microphone samples on a network thread, and the rest of the mod must see a single steady
 * number between 0 and 1 ("how loudly is this player singing"). Keeping the arithmetic here, free of Minecraft and of the voice
 * mod, means it can be unit tested and means nothing on the audio thread ever touches the game.
 */
public final class SongMath {
    private SongMath() {}

    /** Root-mean-square of 16-bit PCM, scaled to 0..1 (1 = a full-scale square wave). Empty or null input is silence. */
    public static float rms(short[] pcm) {
        if (pcm == null || pcm.length == 0) return 0f;
        double sum = 0;
        for (short s : pcm) {
            double v = s / 32768.0;
            sum += v * v;
        }
        return (float) Math.min(1.0, Math.sqrt(sum / pcm.length));
    }

    /**
     * Noise gate and scale: below the gate the voice counts as silence; between the gate and the ceiling the loudness rises to 1.
     * The ceiling is below 1 on purpose, a person shouting into a headset rarely goes past an RMS of about 0.35.
     */
    public static float loudness(float rms, float gate, float ceiling) {
        if (!(rms > gate)) return 0f;
        float top = Math.max(gate + 1e-4f, ceiling);
        return Math.min(1f, (rms - gate) / (top - gate));
    }

    /** One smoothing step: quick to rise (attack), slower to fall (release), so a breath between two notes does not stutter the spell. */
    public static float smooth(float previous, float target, float attack, float release) {
        float k = target > previous ? attack : release;
        return previous + (target - previous) * Math.max(0f, Math.min(1f, k));
    }

    /** Spell strength for a singing level: 0.3 at the gate up to 1.0 at full voice. */
    public static float strength(float level) {
        return 0.3f + 0.7f * Math.max(0f, Math.min(1f, level));
    }
}
