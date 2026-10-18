package com.newuniverse.nusmp.elite.skills.rules;

/**
 * Why: Ouroboros rewinds its wearer to where and how they were a few seconds ago. That needs a short memory: one sample a second, kept in a ring so it never grows. The ring is
 * Minecraft-free (a sample is health plus a position and a dimension id) so the "which sample is 10 seconds old, and is it still valid" logic is unit tested. Samples older
 * than the ring are overwritten; a sample from before a dimension change is still returned (the caller decides whether it may use it).
 */
public final class SnapshotRing {
    /** One remembered moment. {@code dim} is an opaque id the caller compares (the dimension's location string). */
    public record Sample(long tick, double health, double x, double y, double z, String dim) {}

    private final Sample[] ring;
    private int head;
    private int size;

    public SnapshotRing(int capacity) {
        this.ring = new Sample[Math.max(2, capacity)];
    }

    public int capacity() { return ring.length; }

    public int size() { return size; }

    public void clear() { head = 0; size = 0; java.util.Arrays.fill(ring, null); }

    /** Records a sample if it is at least {@code minGap} ticks after the newest one (so calling every tick is safe). */
    public boolean add(Sample s, long minGap) {
        if (s == null) return false;
        Sample last = newest();
        if (last != null && s.tick() - last.tick() < minGap) return false;
        ring[head] = s;
        head = (head + 1) % ring.length;
        if (size < ring.length) size++;
        return true;
    }

    public Sample newest() { return size == 0 ? null : ring[(head - 1 + ring.length) % ring.length]; }

    /**
     * The newest sample that is at least {@code ago} ticks older than {@code now}, or the oldest one in the ring if every sample is younger (the wearer has not
     * been around for {@code ago} ticks yet), or null for an empty ring.
     */
    public Sample at(long now, long ago) {
        if (size == 0) return null;
        Sample oldest = null;
        for (int i = 0; i < size; i++) {
            Sample s = ring[(head - 1 - i + ring.length * 2) % ring.length];
            if (s == null) continue;
            oldest = s;
            if (now - s.tick() >= ago) return s;
        }
        return oldest;
    }
}
