package com.newuniverse.nusmp.heroweapon.rules;

/**
 * Why: several things in the core must not repeat faster than a gap (a hero key pressed a hundred times a second by a macro, the shock's damage, the "rejects
 * the weapon" line, the "repel each other" notice), and each of them stores "the game time it last happened". One tiny pure rule, tested once, answers
 * "is it due again" for all of them, including the two traps of a time stamp: a thing that never happened, and a world clock that was set back.
 */
public final class Spacing {
    private Spacing() {}

    /** The "never recorded" value of a last-time slot: far enough in the past that {@link #due} is true at any game time. */
    public static final long LONG_AGO = Long.MIN_VALUE / 4;

    /**
     * Is something that last happened at {@code last} due again at {@code now}, with at least {@code gap} ticks between two? A {@code last} in the future
     * (the world's clock was set back) counts as due, so a rewound level never silences it for good.
     */
    public static boolean due(long now, long last, int gap) { return last > now || now - last >= Math.max(0, gap); }
}
