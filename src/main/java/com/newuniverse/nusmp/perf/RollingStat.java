package com.newuniverse.nusmp.perf;

/**
 * Why: the probe wants "what does this section cost now", not "what did it cost since the game started": a ring of the last N samples gives a rolling average and a rolling maximum, next to the
 * totals since the last reset. Pure (no game classes). Not synchronized: the owner ({@link PerfProbe.Section}) locks around it.
 */
public final class RollingStat {
    private final long[] ring;
    private int next, filled;
    private long ringSum;
    private long total, count, max;

    public RollingStat(int window) { this.ring = new long[Math.max(1, window)]; }

    public void add(long nanos) {
        long v = Math.max(0L, nanos);
        ringSum -= ring[next];
        ring[next] = v;
        ringSum += v;
        next = (next + 1) % ring.length;
        if (filled < ring.length) filled++;
        total += v;
        count++;
        if (v > max) max = v;
    }

    /** Samples since the last reset. */
    public long count() { return count; }

    /** The sum of all samples since the last reset (ns). */
    public long totalNanos() { return total; }

    /** The largest sample since the last reset (ns). */
    public long maxNanos() { return max; }

    /** The average of the last {@code window} samples (ns); 0 when there is none. */
    public double rollingAvgNanos() { return filled == 0 ? 0.0 : (double) ringSum / filled; }

    /** The largest of the samples still in the ring (ns). */
    public long rollingMaxNanos() {
        long m = 0;
        for (int i = 0; i < filled; i++) if (ring[i] > m) m = ring[i];
        return m;
    }

    /** The average of all samples since the last reset (ns). */
    public double avgNanos() { return count == 0 ? 0.0 : (double) total / count; }

    public void reset() {
        java.util.Arrays.fill(ring, 0L);
        next = 0;
        filled = 0;
        ringSum = 0;
        total = 0;
        count = 0;
        max = 0;
    }
}
