package com.newuniverse.nusmp.client.heroweapon.curse;

/**
 * Why: the curse is felt on the screen before it is read: the edges of the view darken in the colour of the sin as corruption rises, pulse with a heartbeat when it is
 * high, glow faintly while the weapon is whispering its offer, and flood when the weapon holds the wielder. How strong that is, how many bands the vignette is drawn
 * in, how opaque each band is, how fast the heartbeat goes and how a whisper line fades in and out are all plain arithmetic, kept apart from the drawing so they
 * are pinned by tests (the tint never exceeds the configured cap, never goes negative, the outermost band is the strongest, the heartbeat quickens as corruption
 * rises, a whisper fades in, holds, fades out and is silent between). No Minecraft types.
 */
public final class VignetteMath {
    private VignetteMath() {}

    /** The tint never gets above this even if the config asks for more. */
    public static final float HARD_CAP = 0.92f;

    private static float clamp01(float v) { return Float.isNaN(v) ? 0f : Math.max(0f, Math.min(1f, v)); }

    /**
     * The strength of the vignette, 0 to {@code cap}: it grows with corruption (slowly at first, then quickly), glows a little while an offer is open, floods during
     * an Overrun, and breathes with the heartbeat once corruption is high. {@code seconds} is a running clock for the pulses.
     */
    public static float strength(int corruption, boolean offerOpen, boolean overrun, double seconds, float cap) {
        float limit = Math.max(0f, Math.min(HARD_CAP, Float.isNaN(cap) ? 0f : cap));
        float c = clamp01(corruption / 1000f);
        float s = (float) (0.80 * Math.pow(c, 1.5));
        if (offerOpen) s = Math.max(s, (float) (0.16 + 0.05 * Math.sin(seconds * 1.6)));
        if (corruption >= 600) s *= (float) (1.0 + 0.12 * pulse(seconds, beatsPerMinute(corruption)));
        if (overrun) s = (float) (0.88 + 0.06 * Math.sin(seconds * 9.0));
        return Math.max(0f, Math.min(limit, s));
    }

    /** How many bands the vignette is drawn in: more for a stronger tint, and more again when the sight narrows (Pride). */
    public static int rings(float strength, boolean narrow) { return 12 + Math.round(10f * clamp01(strength)) + (narrow ? 8 : 0); }

    /** Every band of the design is drawn as this many thinner ones, so the tint falls off smoothly and not in visible steps. */
    public static final int SMOOTH = 3;

    /** The nested frames of a vignette on one screen: {@code rings} frames, each {@code band} pixels thick, the outermost at the edge of the screen. */
    public record Layout(int rings, int band) {
        /** How far in from the edge of the screen the innermost frame ends, in pixels. */
        public int depth() { return rings * band; }
    }

    /**
     * The frames for a screen of {@code w} by {@code h} pixels: the design's band (a 44th of the shorter side, a 34th for a narrowed sight) cut into {@link #SMOOTH}
     * thinner ones, and {@link #rings} times {@link #SMOOTH} of them. They never go in past the middle of the screen: frames that did would leave a bare strip
     * across the middle, plainly seen as a line, so the bands are made thinner (down to one pixel) and, when even that is not enough, fewer, until they fit. The
     * faint innermost frame then ends at the middle, where the tint has fallen to nothing.
     */
    public static Layout layout(int w, int h, float strength, boolean narrow) {
        int shorter = Math.max(2, Math.min(w, h));
        int reach = Math.max(1, shorter / 2 - 1);
        int rings = rings(strength, narrow) * SMOOTH;
        int band = Math.max(1, Math.max(2, shorter / (narrow ? 34 : 44)) / SMOOTH);
        if (rings * band > reach) band = Math.max(1, reach / rings);
        if (rings * band > reach) rings = reach / band;
        return new Layout(Math.max(1, rings), band);
    }

    /** The opacity of band {@code ring} (0 is the outermost): the full strength at the edge, falling smoothly to nothing at the inner band. */
    public static float ringAlpha(int ring, int rings, float strength) {
        if (rings <= 0 || ring < 0 || ring >= rings) return 0f;
        float k = 1f - ring / (float) rings;
        return clamp01(strength) * (float) Math.pow(k, 2.2);
    }

    /** An ARGB colour from an RGB colour and an opacity (0 to 1). */
    public static int argb(int rgb, float alpha) { return (Math.round(clamp01(alpha) * 255f) << 24) | (rgb & 0xFFFFFF); }

    /** Heartbeats per minute: none below 600 corruption, 60 at 600, rising to 150 at 1000. */
    public static double beatsPerMinute(int corruption) {
        if (corruption < 600) return 0.0;
        return 60.0 + 90.0 * Math.min(1.0, (corruption - 600) / 400.0);
    }

    /** Ticks between two heartbeat sounds at this corruption; 0 when there is no heartbeat. */
    public static int heartbeatPeriodTicks(int corruption) {
        double bpm = beatsPerMinute(corruption);
        return bpm <= 0.0 ? 0 : (int) Math.round(1200.0 / bpm);
    }

    /** A beat that rises sharply and falls away, 0 to 1, repeating {@code bpm} times a minute; 0 for no rate. */
    public static float pulse(double seconds, double bpm) {
        if (bpm <= 0.0) return 0f;
        double phase = (seconds * bpm / 60.0) % 1.0;
        if (phase < 0) phase += 1.0;
        return (float) Math.exp(-6.0 * phase);
    }

    /**
     * The opacity of a whisper line {@code age} ticks after it began: it fades in over 20 ticks, holds, fades out over the last 20 of {@code visible} ticks, and is
     * 0 before it begins and after it ends.
     */
    public static float whisperAlpha(int age, int visible) {
        if (age < 0 || visible <= 0 || age >= visible) return 0f;
        int fade = Math.min(20, visible / 2);
        if (fade <= 0) return 1f;
        if (age < fade) return age / (float) fade;
        if (age >= visible - fade) return (visible - age) / (float) fade;
        return 1f;
    }

    /** True while a whisper is on screen in a window of {@code period} ticks that shows it for the first {@code visible} of them. */
    public static boolean whisperVisible(long clock, int period, int visible) {
        if (period <= 0) return false;
        return Math.floorMod(clock, (long) period) < visible;
    }

    /** The age of the whisper in its window (see {@link #whisperVisible}). */
    public static int whisperAge(long clock, int period) { return period <= 0 ? 0 : (int) Math.floorMod(clock, (long) period); }

    /** The fraction of the three-second hold that has been done, 0 to 1. */
    public static float holdProgress(int heldTicks, int needTicks) { return needTicks <= 0 ? 1f : clamp01(heldTicks / (float) needTicks); }
}
