package com.newuniverse.nusmp.heroweapon.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a hero key is a press (cast the technique), but another system wants to know how long it is HELD: the Curse Series asks the wielder to hold sneak and the
 * third hero key for three seconds to accept the curse. The client sends a heartbeat every few ticks while a key is down, so the server can know it is still
 * held without a packet per tick, and a key whose release packet was lost (a screen opened, the connection hiccuped) expires on its own once the heartbeats
 * stop. This is that bookkeeping, pure, with the time passed in. Slots are 0 to 2 (keys one to three).
 */
public final class KeyHold {
    /** A key with no heartbeat for this many ticks counts as released (the client sends one every 10). */
    public static final int STALE_TICKS = 25;

    private static final int SLOTS = 3;

    private static final class Held {
        final long[] since = new long[SLOTS], beat = new long[SLOTS];
        final boolean[] down = new boolean[SLOTS];
    }

    private final Map<UUID, Held> held = new HashMap<>();

    /** The key went down at {@code now}. */
    public void press(UUID player, int slot, long now) {
        if (slot < 0 || slot >= SLOTS) return;
        Held h = held.computeIfAbsent(player, k -> new Held());
        h.down[slot] = true;
        h.since[slot] = now;
        h.beat[slot] = now;
    }

    /** The client says the key is still down. A heartbeat for a key that was never pressed (the press packet was lost) starts the hold now. */
    public void beat(UUID player, int slot, long now) {
        if (slot < 0 || slot >= SLOTS) return;
        Held h = held.computeIfAbsent(player, k -> new Held());
        if (!h.down[slot]) {
            h.down[slot] = true;
            h.since[slot] = now;
        }
        h.beat[slot] = now;
    }

    /** The key went up. */
    public void release(UUID player, int slot) {
        Held h = held.get(player);
        if (h != null && slot >= 0 && slot < SLOTS) h.down[slot] = false;
    }

    /** Is the key held at {@code now}: down, and a heartbeat (or the press itself) no more than {@link #STALE_TICKS} ago? */
    public boolean isHeld(UUID player, int slot, long now) {
        Held h = held.get(player);
        return h != null && slot >= 0 && slot < SLOTS && h.down[slot] && h.beat[slot] <= now && now - h.beat[slot] <= STALE_TICKS;
    }

    /** For how many ticks the key has been held at {@code now}; 0 when it is up or its heartbeats stopped. A key pressed this very tick has been held for 0 ticks. */
    public int heldTicks(UUID player, int slot, long now) {
        if (!isHeld(player, slot, now)) return 0;
        long d = now - held.get(player).since[slot];
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, d));
    }

    /** Forgets one player (they left). */
    public void forget(UUID player) { held.remove(player); }

    /** Forgets everyone (the server stopped). */
    public void clear() { held.clear(); }
}
