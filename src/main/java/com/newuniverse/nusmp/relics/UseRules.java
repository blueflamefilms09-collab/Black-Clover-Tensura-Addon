package com.newuniverse.nusmp.relics;

/**
 * The pure arithmetic of using a relic: cooldown clocks and the "per day" counters that stand in for D&amp;D's long rest (one Minecraft day,
 * 24000 ticks, is one rest). No Minecraft types, so it is unit-tested.
 */
public final class UseRules {
    private UseRules() {}

    public static final int DAY = 24000;

    /** The day number of a game time (the counter that resets the uses). */
    public static long day(long gameTime) { return Math.floorDiv(gameTime, (long) DAY); }

    public static long readyAt(long now, int cooldownTicks) { return now + Math.max(0, cooldownTicks); }

    public static boolean ready(long now, long readyAt) { return now >= readyAt; }

    /** Ticks left on a cooldown (0 when ready). */
    public static int remaining(long now, long readyAt) {
        long left = readyAt - now;
        return left <= 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, left);
    }

    /** Uses left today for an act limited to {@code perDay} (0 means unlimited: Integer.MAX_VALUE). */
    public static int usesLeft(long storedDay, int storedUsed, long today, int perDay) {
        if (perDay <= 0) return Integer.MAX_VALUE;
        int used = storedDay == today ? Math.max(0, storedUsed) : 0;
        return Math.max(0, perDay - used);
    }

    /** The counter after one more use today. */
    public static int usedAfter(long storedDay, int storedUsed, long today) { return (storedDay == today ? Math.max(0, storedUsed) : 0) + 1; }

    /** "12 s", "1 m 5 s", "2 m". */
    public static String span(int ticks) {
        int s = (Math.max(0, ticks) + 19) / 20;
        if (s < 60) return s + " s";
        int m = s / 60, r = s % 60;
        return r == 0 ? m + " m" : m + " m " + r + " s";
    }
}
