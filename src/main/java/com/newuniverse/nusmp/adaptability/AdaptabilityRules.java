package com.newuniverse.nusmp.adaptability;

import java.util.Map;

/**
 * Why: the maths of the Adaptability skill ({@code docs/adaptability.md}, 3.70.0) with no Minecraft class in it, so a unit test and the CI self-test can pin it. Adaptability "evolves through
 * punishment": every hit of one kind of damage adds a stack for that kind, each stack takes a little off the next hit of that kind, up to a floor (the damage never drops below
 * {@code minimumMultiplier}). Severance is learned apart: enough hits of it and the body is immune. Per player at most {@link #MAX_TYPES} kinds are remembered, so the record has a fixed size.
 */
public final class AdaptabilityRules {
    private AdaptabilityRules() {}

    /** The most damage kinds one player's skill remembers (the weakest is forgotten for a new one). */
    public static final int MAX_TYPES = 16;
    /** The longest damage-kind key kept (the keys are registry ids, so this is generous). */
    public static final int MAX_KEY = 64;

    /** Clamps the key to something that is safe to store: blank becomes "unknown", over-long is cut. */
    public static String key(String raw) {
        if (raw == null || raw.isBlank()) return "unknown";
        return raw.length() > MAX_KEY ? raw.substring(0, MAX_KEY) : raw;
    }

    /** The share of a hit taken away after {@code stacks} stacks: {@code stacks * perHit}, never more than {@code 1 - minimumMultiplier}, never negative. */
    public static double reduction(int stacks, double perHit, double minimumMultiplier) {
        double cap = Math.max(0.0, 1.0 - clamp(minimumMultiplier, 0.0, 1.0));
        double r = Math.max(0, stacks) * Math.max(0.0, perHit);
        return Math.min(cap, r);
    }

    /** The factor the damage is multiplied by ({@code 1 - reduction}), always in [minimumMultiplier, 1]. */
    public static double multiplier(int stacks, double perHit, double minimumMultiplier) {
        return 1.0 - reduction(stacks, perHit, minimumMultiplier);
    }

    public static int maxStacks(boolean mastered, int normal, int masteredMax) { return Math.max(0, mastered ? masteredMax : normal); }

    /**
     * Adds one stack for {@code key} in {@code stacks} (at most {@code maxStacks}). If the key is new and the table is full, the kind with the fewest stacks is forgotten first (the
     * first of equals). Returns the first forgotten key, or null if none was (a corrupted table over the limit is trimmed the same way). The caller reads the new count from the map afterwards.
     */
    public static String bump(Map<String, Integer> stacks, String key, int maxStacks) {
        String gone = null;
        while (!stacks.containsKey(key) && stacks.size() >= MAX_TYPES) {
            int low = Integer.MAX_VALUE;
            String weakest = null;
            for (Map.Entry<String, Integer> e : stacks.entrySet()) if (e.getValue() < low) { low = e.getValue(); weakest = e.getKey(); }
            if (weakest == null) break;
            stacks.remove(weakest);
            if (gone == null) gone = weakest;
        }
        int now = stacks.getOrDefault(key, 0);
        stacks.put(key, Math.min(Math.max(1, maxStacks), now + 1));
        return gone;
    }

    /** True while one more hit of this kind would still add a stack (the skill is still learning it). */
    public static boolean learning(int stacks, int maxStacks) { return stacks < maxStacks; }

    /** Severance: true once {@code hits} severance hits have been taken (at least one is always needed). */
    public static boolean severanceImmune(int hits, int needed) { return hits >= Math.max(1, needed); }

    /** The self-mastery the skill gives itself when it learns something: base, plus the mastered bonus, never more than {@code cap}. */
    public static double selfMastery(boolean mastered, double base, double masteredBonus, double cap) {
        return clamp(Math.max(0.0, base) + (mastered ? Math.max(0.0, masteredBonus) : 0.0), 0.0, Math.max(0.0, cap));
    }

    /** What a resistance skill's learning points are multiplied by while Adaptability is on (never below 1, and points of zero or less stay as they are). */
    public static double learningPoints(double points, boolean mastered, double normal, double masteredMultiplier) {
        if (!(points > 0.0)) return points;
        return points * Math.max(1.0, mastered ? masteredMultiplier : normal);
    }

    /** The mastery every learned skill gets each interval. */
    public static double globalBonus(boolean mastered, double normal, double masteredBonus) { return Math.max(0.0, mastered ? masteredBonus : normal); }

    /** True on the ticks the global mastery is paid: every {@code interval} ticks (at least 20). */
    public static boolean globalDue(int tickCount, int interval) { return tickCount % Math.max(20, interval) == 0; }

    /** Which learned skills one pass pays: the window of at most {@code cap} starting at a rotating offset, so a long list is covered over several passes. Returns {start, count}. */
    public static int[] window(int size, int pass, int cap) {
        if (size <= 0 || cap <= 0) return new int[] {0, 0};
        int count = Math.min(size, cap);
        return new int[] {(int) (((long) Math.max(0, pass) * count) % size), count};
    }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
