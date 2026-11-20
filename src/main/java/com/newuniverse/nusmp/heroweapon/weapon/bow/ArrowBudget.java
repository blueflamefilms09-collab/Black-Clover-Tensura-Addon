package com.newuniverse.nusmp.heroweapon.weapon.bow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a public server must never be flooded with arrow entities, whatever a modified client or a rain of waves asks for. Every hero arrow takes a place here
 * when it is spawned (per wielder, keyed by the player's UUID, and for the whole server) and gives it back when it is removed for any reason (a hit, the end of
 * its flight, logout, death, a changed world, an unloaded chunk). A request over either cap is refused and the shot simply does not leave the string. Pure,
 * no game classes; used on the server thread, but synchronized anyway so a stray caller cannot corrupt the counts.
 */
public final class ArrowBudget {
    private ArrowBudget() {}

    private static final Map<UUID, Integer> PER = new HashMap<>();
    private static int total;

    /** Takes a place for {@code owner}; false (and nothing taken) when the owner is at {@code perCap} or the server at {@code globalCap}. */
    public static synchronized boolean tryAcquire(UUID owner, int perCap, int globalCap) {
        if (owner == null || total >= Math.max(0, globalCap)) return false;
        int n = PER.getOrDefault(owner, 0);
        if (n >= Math.max(0, perCap)) return false;
        PER.put(owner, n + 1);
        total++;
        return true;
    }

    /** Gives one place back (never below zero). */
    public static synchronized void release(UUID owner) {
        Integer n = owner == null ? null : PER.get(owner);
        if (n == null) return;
        if (n <= 1) PER.remove(owner);
        else PER.put(owner, n - 1);
        if (total > 0) total--;
    }

    public static synchronized int count(UUID owner) { return PER.getOrDefault(owner, 0); }

    public static synchronized int total() { return total; }

    /** Forgets everything (a stopped server must not carry counts into the next world). */
    public static synchronized void clear() {
        PER.clear();
        total = 0;
    }
}
