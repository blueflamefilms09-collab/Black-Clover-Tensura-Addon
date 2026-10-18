package com.newuniverse.nusmp.heroweapon.forms;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the owner's design says the form cannot be changed within two seconds of a technique, so a player cannot swap forms between the wind-up and the effect to
 * get both forms' benefits (design.md section 6, the "global 2 s lock"). The forms system is told about each technique by {@link FormMastery#skill} and must remember
 * when. This class is that memory plus the arithmetic of the question, with no Minecraft types. The map outlives a server only as a cache of small numbers; the
 * game classes clear it when a player leaves and when the server stops.
 */
public final class FormLocks {
    private FormLocks() {}

    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** Notes that the player cast a technique at game time {@code now}. */
    public static synchronized void technique(UUID player, long now) {
        if (player != null) LAST.put(player, now);
    }

    /** Ticks still to wait, 0 when the form may be changed. */
    public static synchronized int remaining(UUID player, long now, int lockTicks) {
        Long last = LAST.get(player);
        return last == null ? 0 : remaining(last, now, lockTicks);
    }

    /** The same arithmetic on plain numbers: a technique at {@code last}, now {@code now}, a lock of {@code lockTicks}. A clock that went backwards unlocks. */
    public static int remaining(long last, long now, int lockTicks) {
        if (lockTicks <= 0 || now < last) return 0;
        return (int) Math.max(0, last + lockTicks - now);
    }

    public static synchronized void forget(UUID player) { LAST.remove(player); }

    public static synchronized void clear() { LAST.clear(); }
}
