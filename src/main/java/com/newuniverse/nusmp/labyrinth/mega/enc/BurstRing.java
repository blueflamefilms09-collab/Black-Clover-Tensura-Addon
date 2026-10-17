package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Heard;
import com.newuniverse.nusmp.labyrinth.mega.enc.WandererRules.Tuning;

/**
 * Why: a wandering boss hears SUDDEN loud things (a block broken, a blast, a fight), and it hears them a moment after they happen, on its next
 * pass, not in the same tick. The noise meters keep a smoothed level per player, which is the right thing for the Dungeon Master's dice but
 * hides WHERE and WHEN a loud thing happened. So the loud events are also remembered, briefly, in a small fixed ring of plain numbers (position,
 * loudness, tick): no object per event, the oldest is overwritten, and a pass asks "what is the most attractive thing I could hear that
 * happened after my last pass". Nothing is ever allocated on the hot path of a game event, and the ring cannot grow however long a fight lasts.
 * Pure.
 */
public final class BurstRing {
    private final double[] x, y, z, loud;
    private final long[] tick;
    private int head, size;

    public BurstRing(int capacity) {
        int n = Math.max(4, capacity);
        x = new double[n];
        y = new double[n];
        z = new double[n];
        loud = new double[n];
        tick = new long[n];
    }

    public int size() { return size; }

    public int capacity() { return x.length; }

    /** Remembers a loud event, overwriting the oldest when the ring is full. */
    public void add(double ex, double ey, double ez, double loudness, long now) {
        x[head] = ex;
        y[head] = ey;
        z[head] = ez;
        loud[head] = loudness;
        tick[head] = now;
        head = (head + 1) % x.length;
        if (size < x.length) size++;
    }

    public void clear() {
        head = 0;
        size = 0;
    }

    /**
     * The best sound a listener at (lx, ly, lz) could hear among those that happened AFTER tick {@code after} (the tick of the listener's last
     * pass: everything of that tick was visible to that pass already, and the listener's own roar, made after it, is not heard again): the one
     * with the highest {@link WandererRules#score} among those {@link WandererRules#hears}; null when none qualifies.
     */
    public Heard loudest(double lx, double ly, double lz, long after, Tuning t) {
        Heard best = null;
        double bestScore = 0.0;
        for (int i = 0; i < size; i++) {
            if (tick[i] <= after) continue;
            double dx = x[i] - lx, dy = y[i] - ly, dz = z[i] - lz, d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (!WandererRules.hears(t, loud[i], d)) continue;
            double s = WandererRules.score(loud[i], d);
            if (best == null || s > bestScore) {
                bestScore = s;
                best = new Heard(x[i], y[i], z[i], d, loud[i]);
            }
        }
        return best;
    }
}
