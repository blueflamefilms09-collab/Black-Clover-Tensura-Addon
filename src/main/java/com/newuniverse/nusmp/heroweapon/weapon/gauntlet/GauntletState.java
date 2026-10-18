package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Gauntlet keeps two short-lived things per wielder (the skill points and the iron guard stance) and one display detail (the last number shown on the
 * action bar). They belong to the wielder, not to the item, they end by themselves (the points fade, the stance has an end time) and they are kept in memory
 * only, keyed by the wielder's UUID, cleared on logout, when the weapon is put away and when the server stops. Server thread only.
 */
final class GauntletState {
    private GauntletState() {}

    /** The iron guard stance: until when, how many blows it has absorbed and whether it has already answered one. */
    static final class Guard {
        final long until;
        int absorbed;
        boolean countered;

        Guard(long until) { this.until = until; }
    }

    private static final Map<UUID, PointPool> POOLS = new HashMap<>();
    private static final Map<UUID, Guard> GUARDS = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN = new HashMap<>();

    static PointPool pool(UUID who) { return POOLS.computeIfAbsent(who, k -> new PointPool()); }

    static void setGuard(UUID who, Guard g) { GUARDS.put(who, g); }

    /** The wielder's guard when it is still up, else null. */
    static Guard guard(UUID who, long now) {
        Guard g = GUARDS.get(who);
        if (g == null) return null;
        if (now < g.until) return g;
        GUARDS.remove(who);
        return null;
    }

    /** The number last shown on the action bar (-1 when nothing was shown). */
    static int shown(UUID who) { return SHOWN.getOrDefault(who, -1); }

    static void setShown(UUID who, int n) { SHOWN.put(who, n); }

    static void forget(UUID who) {
        POOLS.remove(who);
        GUARDS.remove(who);
        SHOWN.remove(who);
    }

    static void clear() {
        POOLS.clear();
        GUARDS.clear();
        SHOWN.clear();
    }
}
