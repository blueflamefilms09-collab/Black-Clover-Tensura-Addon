package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: "at most three shadows; raising a fourth releases the oldest" is bookkeeping per owner that has to survive the shadows being scattered over loaded
 * chunks and several dimensions, so it cannot be a question put to the world ("how many zombies have my tag?"). The roster is the owner's ordered list of
 * shadow ids, oldest first. Adding a shadow returns the ids that must be released to stay under the cap, so the caller (which knows how to dismiss an entity)
 * stays simple, and removing is by shadow id alone (a shadow that dies or is discarded does not know its owner's list). Holds ids, never entities. Pure.
 */
public final class MinionRoster {
    private final Map<UUID, ArrayDeque<UUID>> byOwner = new LinkedHashMap<>();

    /**
     * Adds a shadow to its owner's list (nothing happens if it is already there) and returns the shadows to release, oldest first, so that at most {@code cap}
     * remain. With a cap of 0 or less the new shadow itself is in the returned list.
     */
    public List<UUID> add(UUID owner, UUID minion, int cap) {
        ArrayDeque<UUID> q = byOwner.computeIfAbsent(owner, k -> new ArrayDeque<>());
        if (!q.contains(minion)) q.addLast(minion);
        List<UUID> out = new ArrayList<>();
        int keep = Math.max(0, cap);
        while (q.size() > keep) out.add(q.pollFirst());
        if (q.isEmpty()) byOwner.remove(owner);
        return out;
    }

    /** Removes a shadow from whichever owner's list holds it. Returns whether it was listed. */
    public boolean remove(UUID minion) {
        boolean found = false;
        var it = byOwner.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue().remove(minion)) found = true;
            if (e.getValue().isEmpty()) it.remove();
        }
        return found;
    }

    /** The owner's shadows, oldest first. */
    public List<UUID> of(UUID owner) {
        ArrayDeque<UUID> q = byOwner.get(owner);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    public int count(UUID owner) {
        ArrayDeque<UUID> q = byOwner.get(owner);
        return q == null ? 0 : q.size();
    }

    public boolean contains(UUID minion) {
        for (ArrayDeque<UUID> q : byOwner.values()) if (q.contains(minion)) return true;
        return false;
    }

    /** Every owner that has at least one shadow listed. */
    public List<UUID> owners() { return new ArrayList<>(byOwner.keySet()); }

    /** Empties one owner's list and returns what was in it. */
    public List<UUID> clearOwner(UUID owner) {
        ArrayDeque<UUID> q = byOwner.remove(owner);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    public void clear() { byOwner.clear(); }

    public boolean isEmpty() { return byOwner.isEmpty(); }
}
