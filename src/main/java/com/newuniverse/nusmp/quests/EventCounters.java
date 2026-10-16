package com.newuniverse.nusmp.quests;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the custom advancement trigger needs "how many times has this player done X" so an advancement can ask for "cast 50 spells" or
 * "defeat 3 bosses". The arithmetic (which counter keys one event bumps, how a copy is made on death) is plain map logic with no Minecraft
 * types, so it is kept here where JUnit can test it; {@link NusmpEvents} only moves the map in and out of the player's NBT.
 *
 * <p>Two counters are kept per event: the plain key (every use of the event, any argument) and, when the event has an argument, the
 * {@code key|arg} counter (this exact argument). A quest that wants "any spell" reads the plain key, one that wants "a thread spell" reads
 * {@code spell.cast|thread}.
 */
public final class EventCounters {
    private EventCounters() {}

    /** Separator between key and argument in a counter name. A key never contains it. */
    public static final char SEP = '|';

    /** Counter name for an event: {@code key} alone, or {@code key|arg} when an argument is given. */
    public static String name(String key, String arg) {
        return arg == null || arg.isEmpty() ? key : key + SEP + arg;
    }

    /**
     * Counts one event: bumps the plain key and, when the event has an argument, the {@code key|arg} counter too.
     * Counters stop at {@link Integer#MAX_VALUE} instead of wrapping.
     */
    public static void increment(Map<String, Integer> counts, String key, String arg) {
        bump(counts, key);
        if (arg != null && !arg.isEmpty()) bump(counts, name(key, arg));
    }

    private static void bump(Map<String, Integer> counts, String name) {
        counts.merge(name, 1, (a, b) -> a == Integer.MAX_VALUE ? a : a + b);
    }

    /** The counter that decides a trigger instance: the exact {@code key|arg} one when the instance names an argument, else the plain key. */
    public static int read(Map<String, Integer> counts, String key, String instanceArg) {
        Integer v = counts.get(name(key, instanceArg));
        return v == null ? 0 : v;
    }

    /** An independent copy (used when a player respawns or returns from the End and the old player's counters must survive). */
    public static Map<String, Integer> copy(Map<String, Integer> source) {
        return new HashMap<>(source);
    }
}
