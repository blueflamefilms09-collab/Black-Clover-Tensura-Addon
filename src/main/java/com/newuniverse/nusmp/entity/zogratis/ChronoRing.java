package com.newuniverse.nusmp.entity.zogratis;

/**
 * Lucius's memory of the last few seconds (pure): a fixed ring of snapshots (tick, position, health) that Time Reversal reads to put
 * him back where, and as how healthy, he was a few seconds ago. Old snapshots are overwritten, so the memory never grows. Ticks must
 * not run backwards: a snapshot older than the newest one means time was rewound, so everything newer than it is dropped first.
 */
public final class ChronoRing {
    /** One remembered moment. */
    public record Snap(long tick, double x, double y, double z, float health) {}

    private final Snap[] ring;
    private int next, size;

    public ChronoRing(int capacity) { ring = new Snap[Math.max(2, capacity)]; }

    public int capacity() { return ring.length; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        java.util.Arrays.fill(ring, null);
        next = 0;
        size = 0;
    }

    /** Remembers a moment. A tick older than the newest snapshot drops everything newer than it (time went backwards). */
    public void add(long tick, double x, double y, double z, float health) {
        Snap last = newest();
        if (last != null && tick < last.tick()) truncateAfter(tick);
        ring[next] = new Snap(tick, x, y, z, health);
        next = (next + 1) % ring.length;
        if (size < ring.length) size++;
    }

    /** The newest snapshot, or null. */
    public Snap newest() { return size == 0 ? null : ring[Math.floorMod(next - 1, ring.length)]; }

    /** The oldest snapshot still remembered, or null. */
    public Snap oldest() { return size == 0 ? null : ring[Math.floorMod(next - size, ring.length)]; }

    /** The newest snapshot taken at or before 'tick', or null if everything remembered is newer. */
    public Snap at(long tick) {
        for (int i = 0; i < size; i++) {
            Snap s = ring[Math.floorMod(next - 1 - i, ring.length)];
            if (s.tick() <= tick) return s;
        }
        return null;
    }

    /**
     * The moment 'ticksBack' ticks before 'now': the newest snapshot at or before it, or the oldest one remembered if the ring does not
     * reach that far (so the rewind goes as far back as he can remember). Null only when nothing is remembered.
     */
    public Snap rewind(long now, int ticksBack) {
        Snap s = at(now - Math.max(0, ticksBack));
        return s != null ? s : oldest();
    }

    /** Forgets every snapshot newer than 'tick' (after a rewind the future that was undone is not remembered). */
    public void truncateAfter(long tick) {
        while (size > 0) {
            Snap s = newest();
            if (s.tick() <= tick) break;
            next = Math.floorMod(next - 1, ring.length);
            ring[next] = null;
            size--;
        }
    }

    /**
     * The health he comes back with: what the snapshot says, but never more than 'capFraction' of his maximum above what he has now,
     * and never less than he has now (a rewind to a worse moment keeps the better health).
     */
    public static float rewoundHealth(float current, float remembered, float maxHealth, double capFraction) {
        if (!(remembered > current)) return current;
        float room = (float) Math.max(0, capFraction) * maxHealth;
        return Math.min(Math.min(maxHealth, remembered), current + room);
    }
}
