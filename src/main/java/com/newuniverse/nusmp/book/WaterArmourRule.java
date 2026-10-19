package com.newuniverse.nusmp.book;

/**
 * 1.93.0: the pure rule of {@link WaterArmour} (no game classes, unit-tested by WaterRebuildTest): when a cast puts the temporary armour on, what end time it gets.
 */
public final class WaterArmourRule {
    private WaterArmourRule() {}

    /** An ordinary cast: 5 seconds. */
    public static final int CAST_TICKS = 100;

    /**
     * The new end time (game time) of the armour, or -1 when the armour that is on already lasts at least as long and has the same style (it is left alone: a short cast never cuts a
     * long armour short). A different style, or no armour on, always gets {@code now + ticks}.
     */
    public static long nextEnd(long now, long currentEnd, int ticks, boolean running, boolean sameStyle) {
        long end = now + ticks;
        return running && sameStyle && currentEnd >= end ? -1L : end;
    }
}
