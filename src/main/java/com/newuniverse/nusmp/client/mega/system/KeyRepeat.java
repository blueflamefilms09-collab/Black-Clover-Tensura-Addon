package com.newuniverse.nusmp.client.mega.system;

/**
 * Why: in edit mode the player nudges the window with the arrow keys, and a held key must keep moving it: one step on the press, a short pause so a tap does not
 * overshoot, then a steady stream. The game gives a key mapping two facts per tick (was it clicked since the last look, is it held now) and no repeat of its own for
 * a mapping outside a screen, so the timing is counted here, per key, in client ticks. Keeping it a tiny pure object means the rule (a tap is exactly one step,
 * a hold waits {@link #DELAY} ticks and then steps every tick, letting go resets everything, two taps inside one tick are two steps) is pinned by a unit test
 * instead of being felt in the game. Pure.
 */
public final class KeyRepeat {
    /** Ticks a key must stay held after the first step before it repeats (half a second). */
    public static final int DELAY = 10;

    private int held;

    /**
     * Advances one client tick and returns how many steps are due now.
     *
     * @param pressed the mapping was clicked at least once since the last tick (a tap that came and went between two ticks still counts)
     * @param down    the key is held at this moment
     */
    public int step(boolean pressed, boolean down) {
        if (!down) {
            held = 0;
            return pressed ? 1 : 0;
        }
        held++;
        if (held == 1) return 1;                                   // the press itself
        int repeat = held > DELAY ? 1 : 0;
        return pressed ? Math.max(repeat, 1) : repeat;             // a second tap between two ticks is a step of its own
    }

    /** Forgets that the key was held (edit mode ended, a screen opened). */
    public void reset() { held = 0; }

    /** How many ticks the key has been held, for tests. */
    int held() { return held; }
}
