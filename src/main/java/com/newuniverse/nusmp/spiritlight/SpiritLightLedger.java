package com.newuniverse.nusmp.spiritlight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: every live thing the Spirit of Light puts in the world is capped per player and across the whole server so a crowd of casters can never pile up entities. This is the counting part, pure and
 * unit tested. Two kinds: {@link Counted} for things that live until somebody ends them (a linked cub, a risen seraph: a slot is taken at the start and given back at the end), and {@link Timed} for
 * things that end on their own after a known time (a beam, a pulse ring: the slot expires with the tick, so a prop that vanishes without telling anybody can never leak a slot).
 * Single threaded (the server thread); keyed by the player's UUID only, never by anything global.
 */
public final class SpiritLightLedger {
    private SpiritLightLedger() {}

    /** Slots that are taken and given back. */
    public static final class Counted {
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

        /** Gives one slot back (clamped at zero). */
        public void release(UUID id) {
            Integer mine = perPlayer.get(id);
            if (mine == null) return;
            if (mine <= 1) perPlayer.remove(id); else perPlayer.put(id, mine - 1);
            if (total > 0) total--;
        }

        /** Drops every slot of a player (logout, death, dimension change). */
        public void forget(UUID id) {
            Integer mine = perPlayer.remove(id);
            if (mine != null) total = Math.max(0, total - mine);
        }

        public int count(UUID id) { return perPlayer.getOrDefault(id, 0); }

        public int total() { return total; }

        public void clear() { perPlayer.clear(); total = 0; }
    }

    /** Slots that expire by themselves: each is taken for a number of ticks. The list is tiny (bounded by the global cap), so the purge on every ask is a few comparisons. */
    public static final class Timed {
        private final java.util.ArrayList<UUID> owners = new java.util.ArrayList<>();
        private final java.util.ArrayList<Long> until = new java.util.ArrayList<>();

        private void purge(long now) {
            for (int i = until.size() - 1; i >= 0; i--) if (until.get(i) <= now) { until.remove(i); owners.remove(i); }
        }

        /** Takes a slot for {@code life} ticks if the player has fewer than {@code perPlayerCap} alive and the server fewer than {@code globalCap}. */
        public boolean acquire(UUID id, long now, int life, int perPlayerCap, int globalCap) {
            if (id == null || life <= 0 || perPlayerCap <= 0 || globalCap <= 0) return false;
            purge(now);
            int mine = 0;
            for (UUID o : owners) if (o.equals(id)) mine++;
            if (mine >= perPlayerCap || owners.size() >= globalCap) return false;
            owners.add(id);
            until.add(now + life);
            return true;
        }

        public int count(UUID id, long now) {
            purge(now);
            int mine = 0;
            for (UUID o : owners) if (o.equals(id)) mine++;
            return mine;
        }

        public int total(long now) {
            purge(now);
            return owners.size();
        }

        public void forget(UUID id) {
            for (int i = owners.size() - 1; i >= 0; i--) if (owners.get(i).equals(id)) { owners.remove(i); until.remove(i); }
        }

        public void clear() { owners.clear(); until.clear(); }
    }
}
