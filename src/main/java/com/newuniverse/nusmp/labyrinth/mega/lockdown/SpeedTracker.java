package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Why: "running through the corridors faster than a speed threshold" needs a speed that cannot be fooled in either direction. One tick's displacement is noise
 * (a knock-back, a lag spike, a step up a stair), and a teleport is a huge displacement that is not speed at all, so the tracker keeps the samples of the last
 * window (the director takes one every ten ticks), adds up the HORIZONTAL path between consecutive samples, and reports blocks per second over the window. A step
 * longer than the teleport distance is not walked: it is reported as a teleport and the window starts again from the landing, so one teleport cannot read as a
 * ten-second sprint afterwards. The window must be at least 60 percent covered before a speed is reported (a player who has just appeared has no speed yet). Pure.
 */
public final class SpeedTracker {
    /** What the latest sample was. */
    public enum Move { FIRST, WALK, TELEPORT }

    private record Sample(long tick, double x, double z) {}

    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private double lastStep;

    /** Adds a sample. {@code teleportBlocks} is the length of one step that counts as a teleport. */
    public Move add(long tick, double x, double z, double teleportBlocks) {
        Sample last = samples.peekLast();
        Move m = Move.FIRST;
        if (last != null) {
            if (tick <= last.tick()) return Move.WALK;                                  // a second sample in the same tick tells nothing
            lastStep = Math.hypot(x - last.x(), z - last.z());
            if (lastStep > teleportBlocks) {
                samples.clear();
                m = Move.TELEPORT;
            } else {
                m = Move.WALK;
            }
        } else {
            lastStep = 0.0;
        }
        samples.addLast(new Sample(tick, x, z));
        while (samples.size() > 64) samples.removeFirst();
        return m;
    }

    /** The length of the latest step in blocks (for a teleport: how far it jumped). */
    public double lastStep() { return lastStep; }

    /**
     * Blocks per second over the last {@code windowTicks} ticks ending at {@code now}, or 0 when the samples do not cover at least 60 percent of the window.
     */
    public double speed(long now, int windowTicks) {
        double path = 0.0;
        Sample prev = null, first = null;
        Iterator<Sample> it = samples.iterator();
        while (it.hasNext()) {
            Sample s = it.next();
            if (now - s.tick() > windowTicks) continue;
            if (first == null) first = s;
            if (prev != null) path += Math.hypot(s.x() - prev.x(), s.z() - prev.z());
            prev = s;
        }
        if (first == null || prev == null || prev == first) return 0.0;
        long span = prev.tick() - first.tick();
        if (span < windowTicks * 0.6) return 0.0;
        return path / (span / 20.0);
    }

    public void clear() {
        samples.clear();
        lastStep = 0.0;
    }

    public int size() { return samples.size(); }
}
