package com.newuniverse.nusmp.book;

/**
 * Why: Memory Magic's Recall puts a caster back where and how they were a few seconds ago. That needs a short history of the caster (a sample every second: where they stood,
 * how healthy they were) kept in a small ring, and a rule for which sample to go back to. The ring and the rule are pure so a unit test can pin them: 8 samples, one per 20 ticks,
 * Recall goes back to the oldest sample that is at least 60 ticks (3 s) old, and refuses when the history has no sample that old (a fresh caster has nothing to remember).
 * Archive's tally (the damage a marked foe takes while it is archived, replayed at 60 percent, capped) lives here too.
 */
public final class MemoryRewind {
    private MemoryRewind() {}

    public static final int SAMPLES = 8, EVERY = 20, MIN_AGE = 60;
    /** Archive: the share of the recorded damage that is replayed, and the cap as a share of the foe's maximum health (bosses half of it). */
    public static final double REPLAY = 0.6, CAP_SHARE = 0.35;

    /** One remembered moment. */
    public record Sample(long tick, double x, double y, double z, float health, String dimension) {}

    /** The ring of the last {@link #SAMPLES} samples of one caster. */
    public static final class Ring {
        private final Sample[] s = new Sample[SAMPLES];
        private int next, count;

        /** Records a sample when {@link #EVERY} ticks have passed since the last one (returns true when it did). */
        public boolean offer(Sample sample) {
            Sample last = latest();
            if (last != null && sample.tick() - last.tick() < EVERY && sample.tick() >= last.tick()) return false;
            s[next] = sample;
            next = (next + 1) % SAMPLES;
            count = Math.min(SAMPLES, count + 1);
            return true;
        }

        public Sample latest() { return count == 0 ? null : s[(next + SAMPLES - 1) % SAMPLES]; }

        public int size() { return count; }

        public void clear() { count = 0; next = 0; }

        /** The sample to go back to at {@code now}: the oldest one that is at least {@link #MIN_AGE} ticks old and in the same dimension, or null. */
        public Sample recall(long now, String dimension) {
            Sample best = null;
            for (int k = 0; k < count; k++) {
                Sample c = s[(next + SAMPLES - 1 - k) % SAMPLES];
                if (c == null || !c.dimension().equals(dimension) || now - c.tick() < MIN_AGE) continue;
                if (best == null || c.tick() < best.tick()) best = c;
            }
            return best;
        }
    }

    /** Archive: the damage replayed on a foe whose archive recorded {@code recorded}: 60 percent, capped at 35 percent of its maximum health (half of that for a boss). */
    public static float replay(double recorded, float maxHealth, boolean boss) {
        double cap = maxHealth * CAP_SHARE * (boss ? 0.5 : 1.0);
        return (float) Math.max(0, Math.min(recorded * REPLAY, cap));
    }
}
