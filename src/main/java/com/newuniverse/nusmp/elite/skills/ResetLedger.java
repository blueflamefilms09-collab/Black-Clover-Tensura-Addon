package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the {@code reset_count} evolution trigger and other systems ask "how many times has this player reset a skill or a class", and this subsystem OWNS that number. A reset is recorded by
 * whoever performs one ({@code ResetLedger.record(player, reason)}: Ouroboros' Cycle does, and any class or skill reset of another system may call it), the count is stored in the player's own
 * persistent data (so it survives relogs and respawns), and it is mirrored in a small cache keyed by UUID so that it can also answer for a player who is offline (or just logged out) and be read
 * every tick from {@code EliteHooks.resetCount} without touching NBT. {@link #install()} sets that hook.
 */
public final class ResetLedger {
    private ResetLedger() {}

    public static final String KEY = "nusmp_elite_resets";
    private static final Map<UUID, Integer> CACHE = new ConcurrentHashMap<>();

    /** Sets {@code EliteHooks.resetCount} (once, at init). */
    public static void install() { EliteHooks.resetCount = ResetLedger::count; }

    /** The recorded reset count (0 for a player never seen this session until they log in). */
    public static int count(UUID id) { return id == null ? 0 : CACHE.getOrDefault(id, 0); }

    /** Records one reset for the player and returns the new count. {@code reason} is only for the log line / debugging. */
    public static int record(ServerPlayer player, String reason) {
        if (player == null) return 0;
        int next = Math.max(count(player.getUUID()), player.getPersistentData().getInt(KEY)) + 1;
        player.getPersistentData().putInt(KEY, next);
        CACHE.put(player.getUUID(), next);
        return next;
    }

    /** Loads the stored number into the cache (on login). */
    public static void load(ServerPlayer player) {
        if (player != null) CACHE.put(player.getUUID(), player.getPersistentData().getInt(KEY));
    }

    /** Test / admin helper: forces a value into the player's data and the cache. */
    public static void set(ServerPlayer player, int value) {
        if (player == null) return;
        player.getPersistentData().putInt(KEY, Math.max(0, value));
        CACHE.put(player.getUUID(), Math.max(0, value));
    }

    static void clearCache() { CACHE.clear(); }
}
