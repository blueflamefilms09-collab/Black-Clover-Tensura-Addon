package com.newuniverse.nusmp.relics.wave;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Wave 4: the bookkeeping of the Pendant of Echoed Souls. When the pendant turns a death into 1 hit point it calls spectral
 * warriors, and until they are all gone (at most a minute) nothing can target the wearer or hurt them: a floor of five seconds
 * holds even when no warrior could be called. Holds ids only. Pure Java.
 */
final class Wave4Echo {
    private Wave4Echo() {}

    /** The pendant works once per game day. */
    static final int COOLDOWN = 24000;
    static final int LIFE = 1200;
    static final int FLOOR = 100;
    static final int MAX_WARRIORS = 4;
    private static final int CAP = 64;

    private static final class Guard {
        final long floorUntil, until;
        final Set<UUID> warriors;
        Guard(long floorUntil, long until, Set<UUID> warriors) { this.floorUntil = floorUntil; this.until = until; this.warriors = warriors; }
    }

    private static final Map<UUID, Guard> GUARDS = new HashMap<>();
    private static final Set<UUID> ALL = new HashSet<>();

    /** 1d4 spectral warriors. */
    static int count(int d4) { return Math.max(1, Math.min(MAX_WARRIORS, d4)); }

    /** Once per day: true when the last use is a full day behind, never was (a negative stamp) or lies in the future (a restored world). */
    static boolean ready(long lastUse, long now) { return Wave4Charges.ready(lastUse, now, COOLDOWN); }

    static void start(UUID player, long now, Set<UUID> warriors) {
        end(player);
        if (GUARDS.size() >= CAP) GUARDS.entrySet().removeIf(e -> e.getValue().until <= now);
        if (GUARDS.size() >= CAP) { ALL.clear(); GUARDS.clear(); }
        Set<UUID> w = new HashSet<>(warriors);
        GUARDS.put(player, new Guard(now + FLOOR, now + LIFE, w));
        ALL.addAll(w);
    }

    /** True while the wearer cannot be targeted or hurt: inside the floor, or while any warrior lives and the minute is not up. */
    static boolean guarded(UUID player, long now, Predicate<UUID> alive) {
        Guard g = GUARDS.get(player);
        if (g == null) return false;
        if (now >= g.until) { end(player); return false; }
        if (now < g.floorUntil) return true;
        for (UUID w : g.warriors) if (alive.test(w)) return true;
        end(player);
        return false;
    }

    static boolean has(UUID player) { return GUARDS.containsKey(player); }

    static boolean isWarrior(UUID mob) { return !ALL.isEmpty() && ALL.contains(mob); }

    static Set<UUID> warriors(UUID player) {
        Guard g = GUARDS.get(player);
        return g == null ? Set.of() : new HashSet<>(g.warriors);
    }

    static void end(UUID player) {
        Guard g = GUARDS.remove(player);
        if (g != null) ALL.removeAll(g.warriors);
    }

    static void clear() { GUARDS.clear(); ALL.clear(); }

    static int size() { return GUARDS.size(); }
}
