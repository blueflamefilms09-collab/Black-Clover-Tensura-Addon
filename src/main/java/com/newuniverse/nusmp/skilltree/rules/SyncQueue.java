package com.newuniverse.nusmp.skilltree.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: a single click can change a player's tree several times (the SP, the node level, the class, a learned skill), and a command can touch it for many players, yet the client only ever
 * needs the FINAL state once. The design asks for "one sync per tick per player", so a change only MARKS the player here, and one flush at the end of the server tick sends each marked player
 * once, in the order they were first marked. Draining takes a snapshot and empties the queue BEFORE anything is sent, so a send that marks the same player again (or another) lands in the
 * next tick's flush instead of being lost or looping. Pure, so the coalescing and the re-entrancy are tested without a server.
 *
 * @param <K> what is queued, in practice a player's UUID (never the player object, which can be gone by the time the flush runs)
 */
public final class SyncQueue<K> {
    private final Set<K> pending = new LinkedHashSet<>();

    /** Queues a key; true when it was not already queued. */
    public boolean mark(K key) { return key != null && pending.add(key); }

    /** Takes everything queued, oldest first, and empties the queue. */
    public List<K> drain() {
        if (pending.isEmpty()) return List.of();
        List<K> out = new ArrayList<>(pending);
        pending.clear();
        return out;
    }

    public void forget(K key) { pending.remove(key); }

    public void clear() { pending.clear(); }

    public boolean isEmpty() { return pending.isEmpty(); }

    public int size() { return pending.size(); }
}
