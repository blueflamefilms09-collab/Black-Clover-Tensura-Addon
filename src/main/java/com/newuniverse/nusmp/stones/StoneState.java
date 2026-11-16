package com.newuniverse.nusmp.stones;

import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the few things the stones remember between ticks (the rewind samples, the open Soulworld, the cooldown of Foresight and of the Soul anchor, a stone waiting for its bearer's respawn) in one
 * place, every table keyed by the player's UUID and emptied on logout, death and dimension change, so nothing grows with the server's age. Server thread only (no locking). Size: at most
 * {@link #SAMPLES} samples per bearer, one zone per bearer (at most {@link #MAX_ZONES} in all), one pending stone per dead bearer.
 */
final class StoneState {
    private StoneState() {}

    static final int SAMPLES = 6, MAX_ZONES = 64;
    /** The rewind goes back to the oldest sample, which is about five seconds old once the ring is full. */
    static final int REWIND_MIN_SAMPLES = 5;

    record Sample(String dim, double x, double y, double z, float health) {}

    record Zone(String dim, double x, double y, double z, long until) {}

    record Pending(ItemStack stack, String dim, double x, double y, double z) {}

    private static final Map<UUID, ArrayDeque<Sample>> RING = new HashMap<>();
    private static final Map<UUID, Zone> ZONES = new HashMap<>();
    private static final Map<UUID, Long> FORESIGHT = new HashMap<>(), ANCHOR = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    // ---------------------------------------------------------------- rewind
    static void sample(ServerPlayer p) {
        ArrayDeque<Sample> q = RING.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        q.addLast(new Sample(p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ(), p.getHealth()));
        while (q.size() > SAMPLES) q.pollFirst();
    }

    /** The oldest sample once the ring is deep enough, or null. */
    static Sample rewindTarget(UUID id) {
        ArrayDeque<Sample> q = RING.get(id);
        return q == null || q.size() < REWIND_MIN_SAMPLES ? null : q.peekFirst();
    }

    // ---------------------------------------------------------------- Soulworld
    static boolean openZone(UUID id, Zone z) {
        if (!ZONES.containsKey(id) && ZONES.size() >= MAX_ZONES) return false;
        ZONES.put(id, z);
        return true;
    }

    static Zone zone(UUID id) { return ZONES.get(id); }

    static void closeZone(UUID id) { ZONES.remove(id); }

    static int zones() { return ZONES.size(); }

    // ---------------------------------------------------------------- cooldowns
    static long foresightReady(UUID id) { return FORESIGHT.getOrDefault(id, 0L); }

    static void foresightUsed(UUID id, long readyAt) { FORESIGHT.put(id, readyAt); }

    static long anchorReady(UUID id) { return ANCHOR.getOrDefault(id, 0L); }

    static void anchorUsed(UUID id, long readyAt) { ANCHOR.put(id, readyAt); }

    // ---------------------------------------------------------------- a stone kept through a death
    static void keep(UUID id, Pending p) { PENDING.put(id, p); }

    static Pending takePending(UUID id) { return PENDING.remove(id); }

    static Map<UUID, Pending> allPending() { return PENDING; }

    // ---------------------------------------------------------------- cleanup
    /** Logout, death and dimension change: what the player had in motion ends. The cooldown tables are kept on a dimension change (they are timers, not places). */
    static void forgetMotion(UUID id) {
        RING.remove(id);
        ZONES.remove(id);
    }

    static void forgetAll(UUID id) {
        forgetMotion(id);
        FORESIGHT.remove(id);
        ANCHOR.remove(id);
    }

    static void clear() {
        RING.clear();
        ZONES.clear();
        FORESIGHT.clear();
        ANCHOR.clear();
        PENDING.clear();
    }

    static int ringSize(UUID id) { ArrayDeque<Sample> q = RING.get(id); return q == null ? 0 : q.size(); }
}
