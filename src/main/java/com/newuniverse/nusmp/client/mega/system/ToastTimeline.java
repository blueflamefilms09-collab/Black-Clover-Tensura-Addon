package com.newuniverse.nusmp.client.mega.system;

/**
 * Why: the System's pop-up lines have to feel like a status window talking to you: a line SLIDES in from the left, its words are TYPED one character after
 * another, it holds long enough to read, then FADES. All of that is a function of one number, the toast's age in ticks, and keeping it as plain arithmetic
 * (no Minecraft type, no clock) means the timing is unit tested: the slide ends before the typing finishes, the text is complete before the fade begins, the
 * alpha never leaves 0..1, and a toast is done exactly when its life is over. The overlay only reads these numbers and draws. Ages are in game ticks plus the
 * partial tick, so the animation is smooth at any frame rate. Pure.
 */
public final class ToastTimeline {
    private ToastTimeline() {}

    /** Ticks the slide-in takes. */
    public static final float SLIDE_TICKS = 7f;
    /** Ticks a toast lives in all. */
    public static final float LIFE_TICKS = 110f;
    /** Ticks the fade at the end takes. */
    public static final float FADE_TICKS = 16f;
    /** Ticks after the start at which typing begins (the toast is almost in). */
    public static final float TYPE_START = 4f;
    /** Characters revealed per tick (about 34 a second). */
    public static final float CHARS_PER_TICK = 1.7f;

    /** Age of a toast that started at {@code startTicks}, at {@code nowTicks} (both game time plus partial tick). */
    public static float age(float nowTicks, float startTicks) { return nowTicks - startTicks; }

    /** Is the toast over? */
    public static boolean done(float age) { return age >= LIFE_TICKS; }

    /** 0 = fully off to the left, 1 = fully in; an ease-out curve (fast, then settling). */
    public static float slide(float age) {
        float t = Math.max(0f, Math.min(1f, age / SLIDE_TICKS));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Opacity: a quick fade in over two ticks, full until the last {@link #FADE_TICKS}, then down to 0. */
    public static float alpha(float age) {
        if (age <= 0f) return 0f;
        float in = Math.min(1f, age / 2f);
        float out = Math.max(0f, Math.min(1f, (LIFE_TICKS - age) / FADE_TICKS));
        return Math.min(in, out);
    }

    /** How many of {@code total} characters are visible (the typewriter). */
    public static int visibleChars(float age, int total) {
        if (total <= 0) return 0;
        int n = (int) Math.floor((age - TYPE_START) * CHARS_PER_TICK);
        return Math.max(0, Math.min(total, n));
    }

    /** Age at which the last character of a text of this length appears. */
    public static float typedAt(int total) { return TYPE_START + total / CHARS_PER_TICK; }

    /** Does the blinking cursor show at the end of the typed text? It blinks while typing and a few times after, then stays off. */
    public static boolean cursor(float age, int total) {
        if (age > typedAt(total) + 24f) return false;
        return ((int) (age / 3f)) % 2 == 0;
    }
}
