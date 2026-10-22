package com.newuniverse.nusmp.fairytail.storm;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: every spell of the staff has its own cooldown (a Gale Front is back in 7 seconds, an Eye of the Storm in 45), per player, so they live here keyed by the player's UUID (as a
 * string, so the class has no game type and is unit tested). Never a static "current player". The server thread owns it; a player's entry is dropped when they leave.
 */
public final class StormCooldowns {
    private final Map<String, long[]> until = new HashMap<>();

    /** Ticks left before this player may cast this spell again (0 = ready). */
    public int left(String player, int spell, long now) {
        long[] u = until.get(player);
        if (u == null || spell < 0 || spell >= u.length) return 0;
        return (int) Math.max(0L, u[spell] - now);
    }

    public boolean ready(String player, int spell, long now) { return left(player, spell, now) == 0; }

    /** Starts the cooldown of a spell for a player. */
    public void start(String player, int spell, long now, int ticks) {
        if (spell < 0 || spell >= StormSpell.count()) return;
        long[] u = until.computeIfAbsent(player, k -> new long[StormSpell.count()]);
        u[spell] = now + Math.max(0, ticks);
    }

    public void forget(String player) { until.remove(player); }

    public void clear() { until.clear(); }

    public int players() { return until.size(); }
}
