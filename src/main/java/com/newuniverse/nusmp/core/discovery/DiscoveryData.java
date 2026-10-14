package com.newuniverse.nusmp.core.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One player's discovered set: the keys they have found, and the data version (0 = the grandfathering pass has not run yet). The player
 * attachment holds one of these ({@link DiscoveryAttachments}); it is saved with the player and copied on death.
 */
public final class DiscoveryData {
    private int version;
    private final Set<String> keys = new LinkedHashSet<>();

    public DiscoveryData() {}

    public DiscoveryData(int version, Collection<String> stored) {
        this.version = version;
        for (String k : stored) add(k);
    }

    public int version() { return version; }

    public void setVersion(int v) { this.version = v; }

    public boolean has(String key) { return keys.contains(key); }

    public int size() { return keys.size(); }

    public boolean isEmpty() { return keys.isEmpty(); }

    /** A read-only view (live). */
    public Set<String> keys() { return Collections.unmodifiableSet(keys); }

    /** A copy of the keys in the order they were found. */
    public List<String> snapshot() { return new ArrayList<>(keys); }

    /** Adds one key. False if it was invalid, already there, or the set is full. */
    public boolean add(String key) {
        if (!DiscoveryRules.validKey(key) || keys.size() >= DiscoveryRules.MAX_KEYS) return false;
        return keys.add(key);
    }

    /** Adds every key; returns the ones that were new. */
    public List<String> addAll(Collection<String> more) {
        List<String> added = new ArrayList<>();
        for (String k : more) if (add(k)) added.add(k);
        return added;
    }

    /** Union with another player's set (a clone, a copy on death). Returns the keys that were new here. */
    public List<String> merge(DiscoveryData other) { return other == null ? List.of() : addAll(other.keys); }
}
