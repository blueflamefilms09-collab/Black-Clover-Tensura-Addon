package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.ui.theme.UiMath;

/**
 * Why: the hub's motion as pure functions of an age in ticks (20 a second), so it can be tested and so the screen stays a thin drawing layer. The intro runs {@link #INTRO} ticks (about 6.4 seconds): the core
 * circle builds, the rings expand, the nodes light one by one, then the labels and the lines of text fade in. The open sweep ({@link #OPEN}, 320 ms) and the click ripple ({@link #RIPPLE}, 520 ms) use the same
 * timings as the other menus of the reference mods, written here from scratch. Every value is 0..1 and depends on the age only; with motion off the screen passes an age past the end, so everything is simply
 * "done" and the picture is the finished one.
 */
public final class HubTimeline {
    private HubTimeline() {}

    public static final float INTRO = 128f, OPEN = 6.4f, RIPPLE = 10.4f;
    /** The first tick of the rings, of the first lit node, the gap between nodes, and the start of the label fade. */
    public static final float RING_START = 14f, NODE_START = 40f, NODE_STEP = 9f, NODE_FADE = 10f, LABEL_START = 96f;
    /** The ticks at which the two intro sounds play. */
    public static final float SOUND_A = 2f, SOUND_B = NODE_START;

    public static boolean introDone(float age) { return age >= INTRO; }

    /** The core circle's build-up (an eased overshoot, settles at 1). */
    public static float core(float age) { return UiMath.easeOutBack(age / 30f); }

    /** How far the rings have expanded, 0..1. */
    public static float rings(float age) { return UiMath.smooth((age - RING_START) / 40f); }

    /** Node {@code i} lighting up, 0..1. */
    public static float node(float age, int i) { return UiMath.smooth((age - NODE_START - i * NODE_STEP) / NODE_FADE); }

    /** The labels and the text lines fading in, 0..1. */
    public static float labels(float age) { return UiMath.smooth((age - LABEL_START) / 24f); }

    /** The fade-in of a plain open (no intro): the whole stage, over {@link #OPEN} ticks. */
    public static float open(float age) { return UiMath.smooth(age / OPEN); }

    /** The sweep line's position as a fraction of the stage height while it runs, or -1 when it is not running. */
    public static float sweep(float age) { return age < 0f || age >= OPEN ? -1f : age / OPEN; }

    /** The ripple's progress 0..1 while it runs, or -1 when it is over. */
    public static float ripple(float age) { return age < 0f || age >= RIPPLE ? -1f : age / RIPPLE; }

    /** True when {@code at} lies in {@code (prev, now]}: the frame in which an event at a fixed tick happened. */
    public static boolean crossed(float prev, float now, float at) { return prev < at && now >= at; }
}
