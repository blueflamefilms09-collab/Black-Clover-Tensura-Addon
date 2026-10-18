package com.newuniverse.nusmp.heroweapon.weapon.spear;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the Legendary Spear's time reversal needs the wielder's past, and the past has to be small, bounded and honest: one snapshot a second for the last
 * few seconds, kept per player, thrown away when it is too old. This is that ring buffer, with no game type in it (a snapshot holds plain numbers and the
 * ids of the harmful effects the wielder carried), so the rules that matter are tested: the oldest snapshot is overwritten when the buffer is full, a
 * clock that jumps backwards (a reloaded world, {@code /time set}) empties it instead of mixing two timelines, and the snapshots come out oldest first.
 */
public final class RewindBuffer {
    /** The wielder at one moment: where, how healthy, in which dimension, burning or not, and the ids of the harmful effects they carried. */
    public record Snap(long tick, double x, double y, double z, float health, String dim, boolean onFire, Set<String> harmful) {
        public Snap {
            dim = dim == null ? "" : dim;
            harmful = harmful == null ? Set.of() : Set.copyOf(harmful);
        }
    }

    private final Snap[] ring;
    private int first;
    private int size;

    public RewindBuffer(int capacity) { ring = new Snap[Math.max(2, capacity)]; }

    public int size() { return size; }

    public int capacity() { return ring.length; }

    /** The snapshot {@code i} places from the oldest (0 is the oldest, {@code size() - 1} the newest); null when out of range. */
    public Snap get(int i) { return i < 0 || i >= size ? null : ring[(first + i) % ring.length]; }

    public Snap newest() { return get(size - 1); }

    /** True when it is time for a new snapshot: the buffer is empty, or {@code step} ticks have passed since the newest, or the clock went backwards. */
    public boolean due(long now, int step) {
        Snap n = newest();
        return n == null || now < n.tick() || now - n.tick() >= Math.max(1, step);
    }

    /** Adds a snapshot, overwriting the oldest when full. A snapshot older than the newest means the clock went backwards: everything kept is dropped first. */
    public void push(Snap s) {
        if (s == null) return;
        Snap n = newest();
        if (n != null && s.tick() < n.tick()) clear();
        if (size == ring.length) {
            ring[first] = s;
            first = (first + 1) % ring.length;
        } else {
            ring[(first + size) % ring.length] = s;
            size++;
        }
    }

    /** The snapshots, oldest first. */
    public List<Snap> snapshots() {
        List<Snap> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) out.add(get(i));
        return out;
    }

    public void clear() {
        java.util.Arrays.fill(ring, null);
        first = 0;
        size = 0;
    }
}
