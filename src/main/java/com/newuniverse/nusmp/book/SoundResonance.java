package com.newuniverse.nusmp.book;

/**
 * Why: Sound Magic works by RESONANCE: every sonic hit leaves a mark on the foe, marks stack for a while, and the Resonant Shatter page rings them all at once. The decisions are pure
 * numbers so they can be pinned by a unit test: a mark lasts 10 seconds from the latest hit, at most 5 stack, and a full ring deals the page's raw damage times the stacks (the
 * first mark counts one, the fifth five), halved against bosses by the caller.
 */
public final class SoundResonance {
    private SoundResonance() {}

    public static final int MAX_STACKS = 5, WINDOW_TICKS = 200;

    /** The stacks after one more hit at game time {@code now}; {@code last} is when the previous mark landed (the stacks reset when the window ran out). */
    public static int next(int stacks, long last, long now) {
        if (stacks <= 0 || now - last > WINDOW_TICKS || now < last) return 1;
        return Math.min(MAX_STACKS, stacks + 1);
    }

    /** The stacks that are still alive at {@code now}. */
    public static int alive(int stacks, long last, long now) { return stacks > 0 && now >= last && now - last <= WINDOW_TICKS ? Math.min(MAX_STACKS, stacks) : 0; }

    /** Raw damage of ringing {@code stacks} marks with a page worth {@code perStack} each. */
    public static float ring(int stacks, float perStack) { return Math.max(0, Math.min(MAX_STACKS, stacks)) * perStack; }
}
