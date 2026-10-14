package com.newuniverse.nusmp.client.ui.theme;

/**
 * The pure timeline of the page-unlock toast, in ticks since it appeared: it slides in, holds while its circle builds and its text types,
 * then fades out. All values are 0..1 or in ticks and depend on the age only, so the overlay stays a thin drawing layer.
 */
public final class ToastTimeline {
    private ToastTimeline() {}

    public static final float IN = 14f, HOLD = 110f, OUT = 18f, TOTAL = IN + HOLD + OUT;
    /** The circle builds up over its first ticks; the text starts typing when the panel is mostly in. */
    public static final float BUILD = 26f, TEXT_START = 8f;

    /** Slide-in progress, 0 before it starts, 1 once it is in (eased out). */
    public static float slide(float age) { return UiMath.easeOutCubic(age / IN); }

    /** Slide-out progress, 0 until the hold ends, 1 when it has left. */
    public static float leave(float age) { return UiMath.smooth((age - IN - HOLD) / OUT); }

    /** Overall opacity (fades in with the slide, out with the leave). */
    public static float alpha(float age) { return UiMath.clamp01(age / (IN * 0.6f)) * (1f - leave(age)); }

    /** The magic circle's build-up, 0..1. */
    public static float build(float age) { return UiMath.easeOutBack(age / BUILD); }

    /** The age the typewriter sees (0 until TEXT_START). */
    public static float textAge(float age) { return Math.max(0f, age - TEXT_START); }

    public static boolean done(float age) { return age >= TOTAL; }
}
