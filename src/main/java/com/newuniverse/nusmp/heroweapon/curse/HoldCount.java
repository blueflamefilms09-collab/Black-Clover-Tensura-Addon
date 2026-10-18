package com.newuniverse.nusmp.heroweapon.curse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Embrace by the core's third hero key is "hold Sneak and that key for three seconds", and the server only sees the keys through a look every half second, so
 * the hold is counted in looks: this counts, for each player, the looks IN A ROW in which the gesture was down. The caller looks only while an offer is open, so the
 * count starts at the first look after the weapon began to whisper and not when the key went down: a key that was already down then (a technique the wielder is
 * channelling on it) has to stay down, with Sneak, for three more seconds before it counts, and a single look without the gesture starts the count over. No Minecraft
 * types, the time is the caller's, so it is tested with plain numbers.
 */
final class HoldCount {
    /** Ticks between two looks (the server looks at its wielders every ten ticks). */
    static final int LOOK_TICKS = 10;

    /** Looks in a row that make three seconds: the first and the seventh are sixty ticks apart. */
    static final int LOOKS = 7;

    private final Map<UUID, Integer> counts = new HashMap<>();

    /**
     * One look at a player. {@code down} is whether the gesture is held now. Returns true on the look that makes {@code need} in a row (the count then starts over, so a
     * hold that goes on does not accept twice); a look without the gesture forgets the count.
     */
    boolean step(UUID player, boolean down, int need) {
        if (!down) {
            counts.remove(player);
            return false;
        }
        int n = counts.merge(player, 1, Integer::sum);
        if (n < Math.max(1, need)) return false;
        counts.remove(player);
        return true;
    }

    /** The looks counted so far for a player (0 when none). */
    int count(UUID player) { return counts.getOrDefault(player, 0); }

    /** Forgets one player (they left, or no offer is open any more). */
    void forget(UUID player) { counts.remove(player); }

    /** Forgets everyone (the server stopped). */
    void clear() { counts.clear(); }
}
