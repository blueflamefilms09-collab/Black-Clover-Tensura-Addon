package com.newuniverse.nusmp.dungeon.nexus.vault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A spatial hash of block positions for "which is the nearest?" queries (the Descent Gates, for the Ki-Resonance Lantern and the admin
 * command): positions are kept in buckets of 32 blocks in every direction, and a query looks only in the buckets the search sphere reaches.
 * Pure.
 */
public final class GateGrid {
    /** A block position. */
    public record P(int x, int y, int z) {}

    private static final int BUCKET = 32;

    private final Map<Long, List<P>> buckets = new HashMap<>();
    private int size;

    private static long key(int bx, int by, int bz) {
        return (bx & 0x1FFFFFL) | (by & 0x1FFFFFL) << 21 | (bz & 0x1FFFFFL) << 42;
    }

    private static long keyOf(int x, int y, int z) { return key(Math.floorDiv(x, BUCKET), Math.floorDiv(y, BUCKET), Math.floorDiv(z, BUCKET)); }

    /** Adds a position. False if it was there already. */
    public boolean add(int x, int y, int z) {
        List<P> b = buckets.computeIfAbsent(keyOf(x, y, z), k -> new ArrayList<>());
        P p = new P(x, y, z);
        if (b.contains(p)) return false;
        b.add(p);
        size++;
        return true;
    }

    /** Removes a position. False if it was not there. */
    public boolean remove(int x, int y, int z) {
        long k = keyOf(x, y, z);
        List<P> b = buckets.get(k);
        if (b == null || !b.remove(new P(x, y, z))) return false;
        if (b.isEmpty()) buckets.remove(k);
        size--;
        return true;
    }

    public boolean contains(int x, int y, int z) {
        List<P> b = buckets.get(keyOf(x, y, z));
        return b != null && b.contains(new P(x, y, z));
    }

    public int size() { return size; }

    /** Every position (in no particular order). */
    public List<P> all() {
        List<P> out = new ArrayList<>(size);
        for (List<P> b : buckets.values()) out.addAll(b);
        return out;
    }

    /** The position nearest to (x, y, z) within `radius` blocks (distance in three dimensions), or null. Ties go to the lowest x, then y, then z. */
    public P nearest(int x, int y, int z, int radius) {
        if (size == 0 || radius < 0) return null;
        long r2 = (long) radius * radius;
        int bx0 = Math.floorDiv(x - radius, BUCKET), bx1 = Math.floorDiv(x + radius, BUCKET);
        int by0 = Math.floorDiv(y - radius, BUCKET), by1 = Math.floorDiv(y + radius, BUCKET);
        int bz0 = Math.floorDiv(z - radius, BUCKET), bz1 = Math.floorDiv(z + radius, BUCKET);
        long reach = (long) (bx1 - bx0 + 1) * (by1 - by0 + 1) * (bz1 - bz0 + 1);
        P best = null;
        long bestD = Long.MAX_VALUE;
        if (reach > buckets.size()) {                                          // a sphere wider than the whole map: look at what there is
            for (List<P> b : buckets.values()) {
                for (P p : b) {
                    long d = dist2(p, x, y, z);
                    if (d <= r2 && (d < bestD || (d == bestD && before(p, best)))) {
                        bestD = d;
                        best = p;
                    }
                }
            }
            return best;
        }
        for (int bx = bx0; bx <= bx1; bx++) {
            for (int by = by0; by <= by1; by++) {
                for (int bz = bz0; bz <= bz1; bz++) {
                    List<P> b = buckets.get(key(bx, by, bz));
                    if (b == null) continue;
                    for (P p : b) {
                        long d = dist2(p, x, y, z);
                        if (d <= r2 && (d < bestD || (d == bestD && before(p, best)))) {
                            bestD = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static long dist2(P p, int x, int y, int z) {
        long dx = p.x() - x, dy = p.y() - y, dz = p.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean before(P a, P b) {
        if (a.x() != b.x()) return a.x() < b.x();
        if (a.y() != b.y()) return a.y() < b.y();
        return a.z() < b.z();
    }
}
