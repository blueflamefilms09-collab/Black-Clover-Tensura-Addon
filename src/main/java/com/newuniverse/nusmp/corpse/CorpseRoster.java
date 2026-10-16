package com.newuniverse.nusmp.corpse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: "at most N Soul Corpses per caster" is a promise about <i>live</i> bodies, so the count must follow the bodies: it grows when
 * one is raised, shrinks when one crumbles, dies, leaves or is discarded, and when a new one would be the N+1th the OLDEST of that
 * caster goes (the player keeps the spell working instead of being refused). It is a generic class without game types (the key is a
 * UUID in the game, a plain value in the tests); the glue removes the entities it is told to evict.
 *
 * @param <O> the owner id type
 * @param <K> the body id type
 */
public final class CorpseRoster<O, K> {
    private final Map<O, ArrayDeque<K>> byOwner = new HashMap<>();

    /** Adds a body of 'owner' and returns the bodies that must go to stay within 'cap' (oldest first; never the new one). */
    public List<K> add(O owner, K body, int cap) {
        ArrayDeque<K> mine = byOwner.computeIfAbsent(owner, k -> new ArrayDeque<>());
        List<K> evicted = new ArrayList<>();
        mine.remove(body);
        while (mine.size() >= Math.max(1, cap)) evicted.add(mine.pollFirst());
        mine.addLast(body);
        return evicted;
    }

    public boolean remove(O owner, K body) {
        ArrayDeque<K> mine = byOwner.get(owner);
        if (mine == null) return false;
        boolean had = mine.remove(body);
        if (mine.isEmpty()) byOwner.remove(owner);
        return had;
    }

    public int count(O owner) {
        ArrayDeque<K> mine = byOwner.get(owner);
        return mine == null ? 0 : mine.size();
    }

    public List<K> bodies(O owner) {
        ArrayDeque<K> mine = byOwner.get(owner);
        return mine == null ? List.of() : new ArrayList<>(mine);
    }

    public List<O> owners() { return new ArrayList<>(byOwner.keySet()); }

    public boolean isEmpty() { return byOwner.isEmpty(); }

    public void clear() { byOwner.clear(); }
}
