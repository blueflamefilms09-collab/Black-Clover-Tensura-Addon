package com.newuniverse.nusmp.spiritdark;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: every live thing the Spirit of Darkness puts in the world (a companion, a powered form) is capped per player and across the whole server so a crowd of casters can never
 * pile up entities. This is the counting part, pure and unit tested: {@link #acquire} says yes only while both caps hold and counts it, {@link #release} gives it back (clamped at zero),
 * {@link #forget} drops a player (logout, death, dimension change). Single threaded (the server thread); never keyed by anything but the player's UUID.
 */
public final class DarkCapLedger {
    private final Map<UUID, Integer> perPlayer = new HashMap<>();
    private int total;

    /** Takes one slot for the player if the player has fewer than {@code perPlayerCap} and the server fewer than {@code globalCap}. */
    public boolean acquire(UUID id, int perPlayerCap, int globalCap) {
        if (id == null || perPlayerCap <= 0 || globalCap <= 0) return false;
        int mine = perPlayer.getOrDefault(id, 0);
        if (mine >= perPlayerCap || total >= globalCap) return false;
        perPlayer.put(id, mine + 1);
        total++;
        return true;
    }

    /** Gives one slot back. */
    public void release(UUID id) {
        Integer mine = perPlayer.get(id);
        if (mine == null) return;
        if (mine <= 1) perPlayer.remove(id); else perPlayer.put(id, mine - 1);
        if (total > 0) total--;
    }

    /** Drops every slot of a player. */
    public void forget(UUID id) {
        Integer mine = perPlayer.remove(id);
        if (mine != null) total = Math.max(0, total - mine);
    }

    public int count(UUID id) { return perPlayer.getOrDefault(id, 0); }

    public int total() { return total; }

    public void clear() { perPlayer.clear(); total = 0; }
}
