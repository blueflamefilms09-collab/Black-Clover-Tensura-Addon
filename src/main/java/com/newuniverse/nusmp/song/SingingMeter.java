package com.newuniverse.nusmp.song;

/**
 * Why: one player's "singing level" has to survive the gaps between voice frames and the pauses between words. This small state
 * machine takes the loudness of each frame (any thread, one writer) and answers "how loud is the player singing right now",
 * smoothing the rise, holding the level for a short time after the last loud frame and then letting it fall. Time is passed in,
 * so it never reads a clock and tests are exact.
 */
public final class SingingMeter {
    public static final float ATTACK = 0.6f, RELEASE = 0.15f;

    private float level;
    private long lastLoudMs = Long.MIN_VALUE / 2;

    /** Feeds one frame's loudness (already gated, 0..1) at time nowMs. */
    public synchronized void feed(float loudness, long nowMs, long holdMs) {
        if (loudness > 0f) {
            lastLoudMs = nowMs;
            level = SongMath.smooth(level, loudness, ATTACK, RELEASE);
        } else if (nowMs - lastLoudMs > holdMs) {
            level = SongMath.smooth(level, 0f, ATTACK, RELEASE);
            if (level < 0.01f) level = 0f;
        }
    }

    /** Forces a steady level (the key fallback), as if loud frames kept arriving. */
    public synchronized void force(float value, long nowMs) {
        level = Math.max(0f, Math.min(1f, value));
        lastLoudMs = nowMs;
    }

    /**
     * The level as seen at nowMs. If no frame has arrived for a while (the player stopped transmitting, which is silence on the
     * wire, or left) the level falls to 0 after the hold time plus a short tail, so a dropped connection cannot sing forever.
     */
    public synchronized float level(long nowMs, long holdMs) {
        long idle = nowMs - lastLoudMs;
        if (idle > holdMs + 300) return 0f;
        if (idle > holdMs) return level * Math.max(0f, 1f - (idle - holdMs) / 300f);
        return level;
    }
}
