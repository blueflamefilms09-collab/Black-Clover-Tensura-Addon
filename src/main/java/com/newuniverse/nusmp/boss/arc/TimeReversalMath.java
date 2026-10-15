package com.newuniverse.nusmp.boss.arc;

/**
 * Why: in the final arc Lucius has no regeneration; when he is hurt badly he rewinds the damage, and the party must stop him. The
 * owner made this the one exception to "bosses never heal", so it has to be tightly bounded and testable: what counts as a burst, how
 * long the channel is, what interrupts it, how high the restore may go, and the cooldown. This pure class holds those numbers and
 * the state machine (idle, channelling, staggered) with a save/restore pair; the entity glue only supplies health and the facts
 * about each hit.
 * <ul>
 *   <li><b>Burst</b>: he has lost {@value #BURST_PERCENT}% of his maximum health within {@value #BURST_WINDOW} ticks. {@link Window}
 *       remembers the health of those ticks; its highest value is where the rewind would take him back to.</li>
 *   <li><b>Channel</b>: {@value #CHANNEL_TICKS} ticks (3 s). He is rooted and takes {@value #DAMAGE_PERCENT}% damage meanwhile.</li>
 *   <li><b>Interrupt</b>: an anti-magic hit, a heavy hit ({@link #heavy}), or a lock on him. Then he is staggered for
 *       {@value #STAGGER_TICKS} ticks and the damage stays.</li>
 *   <li><b>Restore</b>: only if the channel completes, to the health at the start of the burst window, never above it, never above the
 *       top of his current act, never below his current health.</li>
 *   <li><b>Cooldown</b>: {@value #COOLDOWN_TICKS} ticks from the end of a channel, completed or broken.</li>
 * </ul>
 */
public final class TimeReversalMath {
    private TimeReversalMath() {}

    public static final int CHANNEL_TICKS = 60, BURST_WINDOW = 100, STAGGER_TICKS = 120, COOLDOWN_TICKS = 500;
    public static final int BURST_PERCENT = 18, DAMAGE_PERCENT = 125;
    /** A single hit of this share of his maximum health staggers him; a hit by a wearer of a relic of grade LEGEND or better needs half of it. */
    public static final double HEAVY_FRACTION = 0.06, RELIC_HEAVY_FRACTION = 0.03;
    /** Health is sampled every this many ticks. */
    public static final int SAMPLE_EVERY = 2;

    public enum Interrupt { ANTI_MAGIC, HEAVY_HIT, LOCK, PHASE_CHANGE }

    /** Is this single hit heavy enough to break the channel? */
    public static boolean heavy(float amount, float maxHealth, boolean relicHeavy) {
        if (!(amount > 0f) || !(maxHealth > 0f)) return false;
        return amount >= maxHealth * (relicHeavy ? RELIC_HEAVY_FRACTION : HEAVY_FRACTION);
    }

    /** The health the rewind returns him to: his health at the start of the burst, capped by the top of his act, never lower than now. */
    public static float restoreTarget(float startHealth, float current, float maxHealth, float actCeiling) {
        float cap = Math.min(startHealth, Math.min(maxHealth, actCeiling));
        return Math.max(current, cap);
    }

    /** The health samples of the last {@link #BURST_WINDOW} ticks. */
    public static final class Window {
        private final float[] hp = new float[BURST_WINDOW / SAMPLE_EVERY + 2];
        private final long[] at = new long[hp.length];
        private int n, head;

        public void clear() { n = 0; head = 0; }

        /** Records the health of this tick (every {@value #SAMPLE_EVERY}th call is kept). */
        public void record(long now, float health) {
            if (n > 0 && now - at[(head + n - 1) % hp.length] < SAMPLE_EVERY) return;
            if (n == hp.length) { head = (head + 1) % hp.length; n--; }
            int i = (head + n) % hp.length;
            hp[i] = health;
            at[i] = now;
            n++;
        }

        /** The highest health seen within the window ending now (NaN if nothing was recorded). */
        public float highest(long now) {
            float best = Float.NaN;
            for (int k = 0; k < n; k++) {
                int i = (head + k) % hp.length;
                if (now - at[i] > BURST_WINDOW) continue;
                if (Float.isNaN(best) || hp[i] > best) best = hp[i];
            }
            return best;
        }

        /** Has he lost a burst's worth of his maximum health inside the window? */
        public boolean burst(long now, float current, float maxHealth) {
            float top = highest(now);
            return !Float.isNaN(top) && maxHealth > 0f && top - current >= maxHealth * BURST_PERCENT / 100f;
        }
    }

    public enum State { IDLE, CHANNELING, STAGGERED }

    /** The channel's state machine. */
    public static final class Channel {
        private State state = State.IDLE;
        private long endsAt, cooldownUntil, staggerUntil;
        private float startHealth;

        public State state() { return state; }

        public boolean channeling() { return state == State.CHANNELING; }

        public boolean staggered(long now) { return state == State.STAGGERED && now < staggerUntil; }

        public float startHealth() { return startHealth; }

        public long endsAt() { return endsAt; }

        /** Ticks left of the channel (0 when idle). */
        public int left(long now) { return state == State.CHANNELING ? (int) Math.max(0, endsAt - now) : 0; }

        public boolean ready(long now) { return state == State.IDLE && now >= cooldownUntil; }

        /** Begins a channel that will restore 'startHealth'. False if it may not start now. */
        public boolean begin(long now, float startHealth) {
            if (!ready(now)) return false;
            state = State.CHANNELING;
            endsAt = now + CHANNEL_TICKS;
            this.startHealth = startHealth;
            return true;
        }

        /** Breaks a running channel: staggered, cooldown from now. False if none was running. */
        public boolean interrupt(long now) {
            if (state != State.CHANNELING) return false;
            state = State.STAGGERED;
            staggerUntil = now + STAGGER_TICKS;
            cooldownUntil = now + COOLDOWN_TICKS;
            return true;
        }

        /**
         * One tick. Returns true on the tick the channel completes (the caller applies the restore); a completed channel starts the
         * cooldown. A stagger that has run out ends here.
         */
        public boolean tick(long now) {
            if (state == State.CHANNELING && now >= endsAt) {
                state = State.IDLE;
                cooldownUntil = now + COOLDOWN_TICKS;
                return true;
            }
            if (state == State.STAGGERED && now >= staggerUntil) state = State.IDLE;
            return false;
        }

        /** Forget everything (a new fight). */
        public void reset() {
            state = State.IDLE;
            endsAt = cooldownUntil = staggerUntil = 0;
            startHealth = 0;
        }

        /** {state, endsAt, cooldownUntil, staggerUntil} as longs, and the start health. */
        public long[] save() { return new long[]{state.ordinal(), endsAt, cooldownUntil, staggerUntil, Float.floatToIntBits(startHealth)}; }

        public void restore(long[] s) {
            reset();
            if (s == null || s.length < 5) return;
            int o = (int) Math.max(0, Math.min(State.values().length - 1, s[0]));
            state = State.values()[o];
            endsAt = s[1];
            cooldownUntil = s[2];
            staggerUntil = s[3];
            startHealth = Float.intBitsToFloat((int) s[4]);
        }
    }
}
