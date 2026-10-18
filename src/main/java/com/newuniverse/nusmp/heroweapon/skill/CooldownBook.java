package com.newuniverse.nusmp.heroweapon.skill;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the dispatcher owns every technique's cooldown ("a skill never starts its own cooldown"), so there is exactly one place where "when may this player cast
 * this skill again" is kept: per player, per skill id, in absolute game time, plus one global lock that spaces any two techniques of the same player. It is not
 * saved (a cooldown that outlives a restart would only annoy; the dispatcher clears the book when a player leaves). The book is pure and takes the time as an
 * argument so the tests can drive it; a ready time further ahead than any cooldown can be (the world's clock was set back) counts as ready, like the Djinn
 * weapons' {@code ActClock}, so a rewound world never locks a weapon for good.
 */
public final class CooldownBook {
    /** No cooldown is longer than this (20 minutes): a ready time further ahead than this is a clock that went backwards. */
    public static final int MAX_TICKS = 24000;

    private final Map<UUID, Map<String, Long>> ready = new HashMap<>();
    private final Map<UUID, Long> lockUntil = new HashMap<>();

    /** Ticks left of a skill's cooldown for a player at {@code now}; 0 when it is ready. */
    public int left(UUID player, String skill, long now) {
        Map<String, Long> m = ready.get(player);
        Long at = m == null ? null : m.get(skill);
        return at == null ? 0 : remaining(at, now);
    }

    public boolean ready(UUID player, String skill, long now) { return left(player, skill, now) == 0; }

    /** Starts a skill's cooldown ({@code ticks} is clamped to 0..{@link #MAX_TICKS}). */
    public void start(UUID player, String skill, long now, int ticks) {
        int t = Math.max(0, Math.min(MAX_TICKS, ticks));
        if (t == 0) {
            Map<String, Long> m = ready.get(player);
            if (m != null) m.remove(skill);
            return;
        }
        ready.computeIfAbsent(player, k -> new HashMap<>()).put(skill, now + t);
    }

    /** Ticks left of the global lock between two techniques of a player at {@code now}. */
    public int lockLeft(UUID player, long now) {
        Long at = lockUntil.get(player);
        return at == null ? 0 : remaining(at, now);
    }

    /** Starts the global lock ({@code ticks} clamped like a cooldown). */
    public void lock(UUID player, long now, int ticks) {
        int t = Math.max(0, Math.min(MAX_TICKS, ticks));
        if (t == 0) lockUntil.remove(player);
        else lockUntil.put(player, now + t);
    }

    /** The cooldowns still running for a player at {@code now}, skill id to ticks left, in no particular order (for the status command and the tests). */
    public Map<String, Integer> running(UUID player, long now) {
        Map<String, Integer> out = new LinkedHashMap<>();
        Map<String, Long> m = ready.get(player);
        if (m == null) return out;
        for (Map.Entry<String, Long> e : m.entrySet()) {
            int left = remaining(e.getValue(), now);
            if (left > 0) out.put(e.getKey(), left);
        }
        return out;
    }

    /** Forgets everything about one player (they left, or their weapon was released). */
    public void forget(UUID player) {
        ready.remove(player);
        lockUntil.remove(player);
    }

    /** Forgets everything (the server stopped). */
    public void clear() {
        ready.clear();
        lockUntil.clear();
    }

    private static int remaining(long at, long now) {
        long d = at - now;
        if (d <= 0 || d > MAX_TICKS) return 0;
        return (int) d;
    }
}
