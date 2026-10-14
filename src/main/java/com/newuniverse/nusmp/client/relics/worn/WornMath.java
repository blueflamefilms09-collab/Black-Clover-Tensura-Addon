package com.newuniverse.nusmp.client.relics.worn;

/**
 * The maths of the worn boss relics, free of Minecraft types so it can be tested: orbit angles and bobbing of the crown's shards, the pull-in and
 * burst of the shards when the well fires, the spin and flare of the ring, and the Mimic's clip choice and clock. Times are in ticks since a cue
 * (partial ticks included), angles in degrees.
 */
public final class WornMath {
    private WornMath() {}

    /** Beat lengths in ticks; the Mimic's must match the animation file (tools/relics/waveboss_mimic_geo.py writes both). */
    public static final int WELL_TICKS = 40, STEP_TICKS = 16, GATES_TICKS = 44, BITE_TICKS = 28, YAWN_TICKS = 24;
    /** The Mimic's clip names (the animation file's clip names). */
    public static final String IDLE = "idle", BITE = "bite", YAWN = "yawn";

    // ---------------------------------------------------------------- angles
    public static float wrap(float deg) {
        float d = deg % 360f;
        if (d < 0f) d += 360f;
        return d;
    }

    /** The angle of orbiter {@code index} of {@code count}: a steady turn of {@code degPerTick} from an even start spread. */
    public static float orbit(float age, int index, int count, float degPerTick) {
        int n = Math.max(1, count);
        return wrap(age * degPerTick + index * (360f / n));
    }

    /** A gentle vertical bob (in the unit of {@code amp}) that is out of step from one orbiter to the next. */
    public static float bob(float age, int index, float amp) { return amp * (float) Math.sin(age * 0.07 + index * 1.7); }

    /** The way between two angles by the shorter turn, {@code t} from 0 to 1 (partial-tick interpolation of a rotation). */
    public static float lerpAngle(float from, float to, float t) {
        float d = ((to - from) % 360f + 540f) % 360f - 180f;
        return from + d * t;
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static float clamp01(float t) { return t < 0f ? 0f : t > 1f ? 1f : t; }

    public static float smooth(float t) {
        float x = clamp01(t);
        return x * x * (3f - 2f * x);
    }

    /** Samples a keyframed curve (times ascending), each span eased: held before the first and after the last key. */
    public static float curve(float[] times, float[] values, float t) {
        int n = times.length;
        if (n == 0) return 0f;
        if (t <= times[0]) return values[0];
        if (t >= times[n - 1]) return values[n - 1];
        int i = 0;
        while (i + 2 < n && t >= times[i + 1]) i++;
        float span = times[i + 1] - times[i];
        return lerp(values[i], values[i + 1], span <= 1e-6f ? 1f : smooth((t - times[i]) / span));
    }

    // ---------------------------------------------------------------- cues
    /** Ticks since the cue started (the cue's game time to now, plus the partial tick); negative if there is none. */
    public static float since(long now, long start, float partial) { return start < 0 ? -1f : (now - start) + partial; }

    public static boolean active(float t, int duration) { return t >= 0f && t < duration; }

    // ---------------------------------------------------------------- Dante's Gravity Crown
    private static final float[] WELL_T = {0f, 14f, 18f, 26f, 40f}, WELL_R = {1f, 0.35f, 0.35f, 1.5f, 1f}, WELL_G = {1f, 2.2f, 2.4f, 2.0f, 1f};

    /** The orbit radius of the shards as a factor: they are pulled in for 14 ticks, hold, burst out and settle (1 when no cue is running). */
    public static float wellRadius(float t) { return active(t, WELL_TICKS) ? curve(WELL_T, WELL_R, t) : 1f; }

    /** The brightness factor of the crown's glow over the same beat. */
    public static float wellGlow(float t) { return active(t, WELL_TICKS) ? curve(WELL_T, WELL_G, t) : 1f; }

    /** The extra turn (degrees) the shards have gathered by {@code t}: one whole turn over the beat, so it ends exactly where it began (no jump). */
    public static float wellTurn(float t) {
        if (t < 0f) return 0f;
        if (t >= WELL_TICKS) return 360f;
        return 360f * smooth(t / WELL_TICKS);
    }

    // ---------------------------------------------------------------- Zenon's Spatial Ring
    /** The beat length of a ring cue (kind 1 = step, 2 = gates), 0 for none. */
    public static int ringTicks(int kind) { return kind == WaveBossKinds.GATES ? GATES_TICKS : kind == WaveBossKinds.STEP ? STEP_TICKS : 0; }

    /** The rift ring's size as a factor: it flares to nearly double and settles (1 when idle). */
    public static float ringExpand(float t, int kind) {
        int len = ringTicks(kind);
        if (len == 0 || !active(t, len)) return 1f;
        float k = t / len;
        return 1f + 0.9f * (float) Math.sin(Math.PI * Math.pow(k, 0.6));
    }

    /** The extra spin (degrees) of the ring over a beat: one whole turn for a step, two for the gates, so the ring ends where it began. */
    public static float ringTurn(float t, int kind) {
        int len = ringTicks(kind);
        if (len == 0 || t < 0f) return 0f;
        float turns = kind == WaveBossKinds.GATES ? 720f : 360f;
        return t >= len ? turns : turns * smooth(t / len);
    }

    /** The brightness factor of the ring's core over its beat. */
    public static float ringGlow(float t, int kind) {
        int len = ringTicks(kind);
        if (len == 0 || !active(t, len)) return 1f;
        return 1f + 1.6f * (float) Math.sin(Math.PI * (t / len));
    }

    // ---------------------------------------------------------------- the Artifact Mimic
    /** The clip to play for the Mimic: its bite or yawn while that beat runs, else the idle loop. */
    public static String mimicClip(int kind, float t) {
        if (kind == WaveBossKinds.BITE && active(t, BITE_TICKS)) return BITE;
        if (kind == WaveBossKinds.YAWN && active(t, YAWN_TICKS)) return YAWN;
        return IDLE;
    }

    /** The clip clock in seconds: the beat's own time for bite and yawn, the age of the wearer for the idle loop. */
    public static float mimicSeconds(String clip, float t, float age) { return IDLE.equals(clip) ? age / 20f : Math.max(0f, t) / 20f; }

    /** The Mimic's clip length in ticks (0 for the loop). */
    public static int mimicTicks(String clip) { return BITE.equals(clip) ? BITE_TICKS : YAWN.equals(clip) ? YAWN_TICKS : 0; }
}
