package com.newuniverse.nusmp.star;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Star Magic nodes: the pure bookkeeping of who has which stars (docs/vfx_remake/star_ingame.md). No Minecraft class; every method is O(stars of one player) except the server-wide eviction, which looks at
 * the oldest star of each player (at most one entry per player online). Keyed by the owner's UUID, never a static "current player". All calls come from the server thread.
 *
 * <p>Caps (rule 11): at most {@code perPlayerCap} stars per owner (the OLDEST of that owner goes first), at most {@code serverCap} stars on the whole server (the oldest star of anyone goes first). A star
 * past its expiry tick is never returned by {@link #live}; it is dropped lazily when its owner asks, and the entity's own life counter removes the body, so a missed event costs at most one life.
 */
public final class StarNodes {
    /** One star: the entity that draws it, where it hangs, in which dimension, when it was placed (a strictly rising number) and when it dies (game tick). */
    public record Node(UUID entity, String dim, double x, double y, double z, long seq, long expiry) {
        public StarMath.P pos() { return new StarMath.P(x, y, z); }
    }

    private final Map<UUID, ArrayDeque<Node>> byOwner = new HashMap<>();
    private int total;
    private long counter;

    /** The next placement number (rises by one on every call). */
    public long nextSeq() { return ++counter; }

    public int total() { return total; }

    public int count(UUID owner) {
        ArrayDeque<Node> d = byOwner.get(owner);
        return d == null ? 0 : d.size();
    }

    /**
     * Adds a star and returns the stars that had to go to make room (so the caller discards their bodies): the owner's oldest while the owner is over {@code perPlayerCap}, then the oldest star of
     * anyone while the server is over {@code serverCap}. The new star itself is never evicted by the caps unless {@code serverCap} is below one.
     */
    public List<Node> add(UUID owner, Node n, int perPlayerCap, int serverCap) {
        List<Node> out = new ArrayList<>(2);
        ArrayDeque<Node> d = byOwner.computeIfAbsent(owner, k -> new ArrayDeque<>());
        d.addLast(n);
        total++;
        while (d.size() > Math.max(1, perPlayerCap)) { out.add(d.pollFirst()); total--; }
        while (total > Math.max(1, serverCap)) {
            UUID oldestOwner = null;
            long best = Long.MAX_VALUE;
            for (Map.Entry<UUID, ArrayDeque<Node>> e : byOwner.entrySet()) {
                Node head = e.getValue().peekFirst();
                if (head != null && head.seq() < best) { best = head.seq(); oldestOwner = e.getKey(); }
            }
            if (oldestOwner == null) break;
            ArrayDeque<Node> od = byOwner.get(oldestOwner);
            out.add(od.pollFirst());
            total--;
            if (od.isEmpty()) byOwner.remove(oldestOwner);
        }
        return out;
    }

    /** The owner's stars that are still alive in this dimension, oldest first (newest last). Expired stars are dropped and returned through {@code expiredOut} when it is not null. */
    public List<Node> live(UUID owner, String dim, long now, List<Node> expiredOut) {
        ArrayDeque<Node> d = byOwner.get(owner);
        List<Node> out = new ArrayList<>();
        if (d == null) return out;
        for (Iterator<Node> it = d.iterator(); it.hasNext(); ) {
            Node n = it.next();
            if (n.expiry() <= now) {
                it.remove();
                total--;
                if (expiredOut != null) expiredOut.add(n);
            } else if (n.dim().equals(dim)) out.add(n);
        }
        if (d.isEmpty()) byOwner.remove(owner);
        return out;
    }

    /** Removes one star of the owner (it was used up, or its body is gone). True when it was there. */
    public boolean remove(UUID owner, UUID entity) {
        ArrayDeque<Node> d = byOwner.get(owner);
        if (d == null) return false;
        for (Iterator<Node> it = d.iterator(); it.hasNext(); ) {
            if (it.next().entity().equals(entity)) {
                it.remove();
                total--;
                if (d.isEmpty()) byOwner.remove(owner);
                return true;
            }
        }
        return false;
    }

    /** Removes every star of the owner and returns them (logout, death, dimension change, server stop). */
    public List<Node> clear(UUID owner) {
        ArrayDeque<Node> d = byOwner.remove(owner);
        if (d == null) return List.of();
        total -= d.size();
        return new ArrayList<>(d);
    }

    /** Removes the owner's stars of one dimension (the owner changed dimension). */
    public List<Node> clearDimension(UUID owner, String dim) {
        ArrayDeque<Node> d = byOwner.get(owner);
        List<Node> out = new ArrayList<>();
        if (d == null) return out;
        for (Iterator<Node> it = d.iterator(); it.hasNext(); ) {
            Node n = it.next();
            if (n.dim().equals(dim)) { out.add(n); it.remove(); total--; }
        }
        if (d.isEmpty()) byOwner.remove(owner);
        return out;
    }

    /** Everything, for a server stop. */
    public List<Node> clearAll() {
        List<Node> out = new ArrayList<>();
        for (ArrayDeque<Node> d : byOwner.values()) out.addAll(d);
        byOwner.clear();
        total = 0;
        return out;
    }

    /** A star with the UUID of the mage who owns it (for a read across every owner). */
    public record Owned(UUID owner, Node node) {}

    /**
     * Stars of ANY owner in this dimension that hang within {@code r} of the point and have not expired, at most {@code limit} of them (the first found, no sorting). One pass over the registry, which holds at
     * most {@code maxOnServer} (512) stars; nothing is removed or changed. Used by the dispel hook ({@link StarDispel}).
     */
    public List<Owned> near(String dim, double cx, double cy, double cz, double r, long now, int limit) {
        List<Owned> out = new ArrayList<>();
        if (total == 0 || limit <= 0) return out;
        double r2 = r * r;
        for (Map.Entry<UUID, ArrayDeque<Node>> e : byOwner.entrySet()) {
            for (Node n : e.getValue()) {
                if (n.expiry() <= now || !n.dim().equals(dim)) continue;
                double dx = n.x() - cx, dy = n.y() - cy, dz = n.z() - cz;
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                out.add(new Owned(e.getKey(), n));
                if (out.size() >= limit) return out;
            }
        }
        return out;
    }

    /** Owners that have at least one star (for the self-test). */
    public int owners() { return byOwner.size(); }
}
