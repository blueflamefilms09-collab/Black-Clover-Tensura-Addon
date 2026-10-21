package com.newuniverse.nusmp.vfx.deck;

/**
 * Why: the card summon of the Deck of Many Things (the top card slides out of the deck slot of the worn duel disk, a flash of short light streaks, the card is slapped on a card zone with a
 * shockwave ring, it flips face up, a column of light in the colour of its alignment rises, the card dissolves into sparks) is a client animation of about two seconds. Its clock is pure
 * arithmetic, so the phases, their lengths and the monotonic progress are checked by tests ({@code DeckSummonTimelineTest}) and the renderer only reads the numbers. No Minecraft types.
 * Times are client ticks since the effect started (with the partial tick); {@link #TOTAL} ticks in all (40 = two seconds).
 */
public final class DeckSummonTimeline {
    private DeckSummonTimeline() {}

    /** The phases in the order they start. They may overlap by a few ticks (the flash starts while the card is still sliding). */
    public enum Phase { SLIDE, FLASH, SLAP, FLIP, COLUMN, DISSOLVE }

    /** Length of the whole effect in ticks (two seconds). */
    public static final int TOTAL = 40;

    private static final int[] START = {0, 6, 8, 14, 20, 30};
    private static final int[] END = {8, 10, 14, 22, 32, 40};

    public static int start(Phase p) { return START[p.ordinal()]; }

    public static int end(Phase p) { return END[p.ordinal()]; }

    public static int length(Phase p) { return END[p.ordinal()] - START[p.ordinal()]; }

    /** Progress 0..1 of a phase at time {@code t}: 0 before it starts, 1 after it ends, linear in between. Never decreases as {@code t} grows. */
    public static float progress(Phase p, float t) {
        float a = START[p.ordinal()], b = END[p.ordinal()];
        if (t <= a) return 0f;
        if (t >= b) return 1f;
        return (t - a) / (b - a);
    }

    /** Is the phase running at {@code t}? */
    public static boolean active(Phase p, float t) { return t >= START[p.ordinal()] && t < END[p.ordinal()]; }

    public static float ease(float x) { return x * x * (3f - 2f * x); }

    /** How far the card has travelled from the deck slot to the hover point above the zone (0 at the slot, 1 hovering), then (SLAP) down onto the zone: 0..2 in all. */
    public static float travel(float t) { return ease(progress(Phase.SLIDE, t)) + progress(Phase.SLAP, t) * progress(Phase.SLAP, t); }

    /** The turn of the card about its long axis in degrees: 0 back up, 180 face up (the flip phase), and it stays face up. */
    public static float flipDegrees(float t) { return 180f * ease(progress(Phase.FLIP, t)); }

    /** Is the face of the card towards the viewer standing outside (the card has turned past 90 degrees)? */
    public static boolean faceUp(float t) { return flipDegrees(t) >= 90f; }

    /** The scale of the card: small in the slot, larger while it hovers, a little smaller on the zone. */
    public static float cardScale(float t) {
        float s = 0.55f + 0.45f * ease(progress(Phase.SLIDE, t));
        return s - 0.18f * progress(Phase.SLAP, t);
    }

    /** The opacity of the card: 1 until the dissolve, then down to 0. */
    public static float cardAlpha(float t) { return 1f - ease(progress(Phase.DISSOLVE, t)); }

    /** 0..1 strength of the radial light streaks: a quick rise and fall over the flash phase. */
    public static float flash(float t) {
        float p = progress(Phase.FLASH, t);
        return p <= 0f || p >= 1f ? 0f : (float) Math.sin(Math.PI * p);
    }

    /** Radius (blocks) of the shockwave ring on the zone: it grows from the tick of the impact over six ticks. 0 before the impact. */
    public static float shockwaveRadius(float t) {
        float k = (t - END[Phase.SLAP.ordinal()]) / 6f;
        return k <= 0f ? 0f : 0.55f * Math.min(1f, k);
    }

    /** Opacity of the shockwave ring: bright at the impact, gone six ticks later. */
    public static float shockwaveAlpha(float t) {
        float k = (t - END[Phase.SLAP.ordinal()]) / 6f;
        return k <= 0f || k >= 1f ? 0f : 1f - k;
    }

    /** Height (blocks) of the column of light: it rises over the first half of its phase, holds, and thins out at the end. */
    public static float columnHeight(float t) {
        float p = progress(Phase.COLUMN, t);
        return p <= 0f ? 0f : 1.5f * ease(Math.min(1f, p * 2f));
    }

    /** Opacity of the column: in and out over its phase (it is gone when the phase ends). */
    public static float columnAlpha(float t) {
        float p = progress(Phase.COLUMN, t);
        return p <= 0f || p >= 1f ? 0f : (float) Math.sin(Math.PI * p);
    }

    /** The whole effect is over. */
    public static boolean finished(float t) { return t >= TOTAL; }

    /** Sparks to spawn in the client tick {@code tick} (an integer age): a burst at the impact, a few every tick while the card dissolves, none otherwise. */
    public static int sparks(int tick) {
        if (tick == END[Phase.SLAP.ordinal()]) return 8;
        if (tick >= START[Phase.DISSOLVE.ordinal()] && tick < END[Phase.DISSOLVE.ordinal()]) return 3;
        return 0;
    }

    /** The most sparks one summon spawns in all (the tests pin it; the client particle budget decides how many of them really appear). */
    public static int maxSparks() {
        int n = 0;
        for (int i = 0; i <= TOTAL; i++) n += sparks(i);
        return n;
    }
}
